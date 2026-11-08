#!/usr/bin/env bash
# Verifies issue #58 acceptance criterion: the existing services register with discovery-service and show UP.
#
# Runs, with NO source changes: discovery-service + auth, user, mentor (MySQL 8.4) and booking, payment,
# review (PostgreSQL, as on develop). Clients are switched on purely through their existing environment
# variables (EUREKA_ENABLED / EUREKA_URL). Every password is random per run and never written to disk.
#
# Requirements: bash, docker (daemon running), curl, Java 21. Ports 8761 and 8081-8086 must be free.
# Usage (from anywhere):  bash services/discovery-service/verification/verify-client-registration.sh
# Exit code 0 = all six services registered and UP.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUN_ID="mm-eureka-verify-$$"
WORK_DIR="$(mktemp -d)"
MYSQL_CONTAINER="$RUN_ID-mysql"
POSTGRES_CONTAINER="$RUN_ID-postgres"
MYSQL_PORT="${MYSQL_PORT:-33306}"
POSTGRES_PORT="${POSTGRES_PORT:-55432}"
DISCOVERY_PORT=8761
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-240}"

MYSQL_SERVICES=(auth-service user-service mentor-service)
POSTGRES_SERVICES=(booking-service payment-service review-service)
ALL_SERVICES=("${MYSQL_SERVICES[@]}" "${POSTGRES_SERVICES[@]}")

random_secret() { echo "v-$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"; }
native_path() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else echo "$1"; fi; }
log() { printf '[verify] %s\n' "$*"; }

DISCOVERY_USER="discovery"
DISCOVERY_PASSWORD="$(random_secret)"
DB_ROOT_PASSWORD="$(random_secret)"
DB_PASSWORD="$(random_secret)"
SECRETS=("$DISCOVERY_PASSWORD" "$DB_ROOT_PASSWORD" "$DB_PASSWORD")
PIDS=()

cleanup() {
  log "cleaning up processes and containers"
  for pid in "${PIDS[@]:-}"; do [ -n "$pid" ] && kill "$pid" 2>/dev/null || true; done
  for pid in "${PIDS[@]:-}"; do [ -n "$pid" ] && wait "$pid" 2>/dev/null || true; done
  docker rm -f "$MYSQL_CONTAINER" "$POSTGRES_CONTAINER" >/dev/null 2>&1 || true
  for port in "$DISCOVERY_PORT" 8081 8082 8083 8084 8085 8086; do
    if curl -s -o /dev/null --max-time 1 "http://localhost:$port"; then
      log "WARNING: something is still listening on port $port after cleanup"
    fi
  done
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

for port in "$DISCOVERY_PORT" 8081 8082 8083 8084 8085 8086; do
  if curl -s -o /dev/null --max-time 1 "http://localhost:$port"; then
    echo "Port $port is already in use; stop the process using it and retry." >&2; exit 2
  fi
done

log "starting MySQL 8.4 and PostgreSQL 16 containers"
docker pull -q mysql:8.4 >/dev/null
docker pull -q postgres:16-alpine >/dev/null
docker run -d --name "$MYSQL_CONTAINER" -p "$MYSQL_PORT:3306" -e MYSQL_ROOT_PASSWORD="$DB_ROOT_PASSWORD" mysql:8.4 >/dev/null
docker run -d --name "$POSTGRES_CONTAINER" -p "$POSTGRES_PORT:5432" -e POSTGRES_PASSWORD="$DB_ROOT_PASSWORD" postgres:16-alpine >/dev/null

# Test sources are not compiled (-Dmaven.test.skip): this script checks runtime registration only, and
# some services' own test suites are outside the scope of #58.
log "building discovery-service and the six client services (sources unchanged, tests skipped)"
for service in discovery-service "${ALL_SERVICES[@]}"; do
  (cd "$SERVICES_DIR/$service" && ./mvnw -q -B -ntp -Dmaven.test.skip=true package) || { echo "build failed: $service" >&2; exit 1; }
done

wait_until() { # wait_until <description> <command...>
  local description="$1"; shift
  for _ in $(seq 1 "$TIMEOUT_SECONDS"); do "$@" >/dev/null 2>&1 && return 0; sleep 1; done
  echo "timed out waiting for $description" >&2; return 1
}

wait_until "MySQL" docker exec -e MYSQL_PWD="$DB_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql -uroot -e "SELECT 1"
wait_until "PostgreSQL" docker exec "$POSTGRES_CONTAINER" pg_isready -U postgres

log "creating one database and one user per service"
for service in "${MYSQL_SERVICES[@]}"; do
  db="mm_${service%-service}"
  docker exec -e MYSQL_PWD="$DB_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql -uroot -e \
    "CREATE DATABASE $db; CREATE USER '$db'@'%' IDENTIFIED BY '$DB_PASSWORD'; GRANT ALL ON $db.* TO '$db'@'%';"
done
for service in "${POSTGRES_SERVICES[@]}"; do
  db="mm_${service%-service}"
  docker exec "$POSTGRES_CONTAINER" psql -q -U postgres -c "CREATE USER $db PASSWORD '$DB_PASSWORD';" -c "CREATE DATABASE $db OWNER $db;"
done

start_jar() { # start_jar <service> [ENV=VALUE ...]
  local service="$1"; shift
  local jar; jar="$(ls "$SERVICES_DIR/$service"/target/"$service"-*.jar | grep -v '\.original$' | head -1)"
  # exec: the recorded PID is the JVM itself, so cleanup really stops it.
  (cd "$SERVICES_DIR/$service" && exec env "$@" java -jar "$jar" >"$WORK_DIR/$service.log" 2>&1) &
  PIDS+=("$!")
}

log "starting discovery-service on :$DISCOVERY_PORT"
start_jar discovery-service DISCOVERY_USERNAME="$DISCOVERY_USER" DISCOVERY_PASSWORD="$DISCOVERY_PASSWORD"
wait_until "discovery-service health" curl -sf "http://localhost:$DISCOVERY_PORT/actuator/health"

EUREKA_ENV=(
  EUREKA_ENABLED=true
  "EUREKA_URL=http://$DISCOVERY_USER:$DISCOVERY_PASSWORD@localhost:$DISCOVERY_PORT/eureka/"
  # Keeps the registry URL (with credentials) out of client logs on connection errors; see README §8.
  LOGGING_LEVEL_COM_NETFLIX_DISCOVERY_SHARED_TRANSPORT_DECORATOR_REDIRECTINGEUREKAHTTPCLIENT=WARN
)
PUBLIC_KEY="file:$(native_path "$SERVICES_DIR/auth-service/jwt-keys/public.pem")"
PRIVATE_KEY="$(native_path "$SERVICES_DIR/auth-service/jwt-keys/private.pem")"

log "starting the six client services with Eureka enabled"
for service in "${MYSQL_SERVICES[@]}"; do
  db="mm_${service%-service}"
  extra=()
  case "$service" in
    auth-service) extra=(AUTH_JWT_AUDIENCE=mentor-matching-api "AUTH_JWT_PRIVATE_KEY_PATH=$PRIVATE_KEY") ;;
    user-service) extra=(AUTH_JWT_AUDIENCE=mentor-matching-api "AUTH_JWT_PUBLIC_KEY_LOCATION=$PUBLIC_KEY") ;;
  esac
  start_jar "$service" "${EUREKA_ENV[@]}" "${extra[@]}" \
    "DB_URL=jdbc:mysql://localhost:$MYSQL_PORT/$db" "DB_USERNAME=$db" "DB_PASSWORD=$DB_PASSWORD"
done
for service in "${POSTGRES_SERVICES[@]}"; do
  db="mm_${service%-service}"
  start_jar "$service" "${EUREKA_ENV[@]}" \
    "DB_URL=jdbc:postgresql://localhost:$POSTGRES_PORT/$db" "DB_USERNAME=$db" "DB_PASSWORD=$DB_PASSWORD"
done

registry() {
  curl -sf -u "$DISCOVERY_USER:$DISCOVERY_PASSWORD" -H 'Accept: application/json' "http://localhost:$DISCOVERY_PORT/eureka/apps"
}
all_up() {
  local apps; apps="$(registry)" || return 1
  for service in "${ALL_SERVICES[@]}"; do
    local name; name="$(echo "$service" | tr '[:lower:]' '[:upper:]')"
    echo "$apps" | tr -d ' \n' | grep -q "\"app\":\"$name\",[^}]*\"status\":\"UP\"" || return 1
  done
}

log "waiting (up to ${TIMEOUT_SECONDS}s) for all six services to be UP in the registry"
if ! wait_until "six services UP in Eureka" all_up; then
  log "registry content:"; registry || true
  for service in "${ALL_SERVICES[@]}"; do echo "--- $service (last lines)"; tail -n 15 "$WORK_DIR/$service.log"; done
  exit 1
fi

apps_json="$(registry | tr -d ' \n')"
printf '\n%-18s %-36s %-8s %s\n' "SERVICE" "INSTANCE" "EUREKA" "HEALTH"
port=8081
for service in "${ALL_SERVICES[@]}"; do
  name="$(echo "$service" | tr '[:lower:]' '[:upper:]')"
  instance="$(echo "$apps_json" | grep -o "\"instanceId\":\"[^\"]*\",[^}]*\"app\":\"$name\"" | head -1 | sed 's/"instanceId":"\([^"]*\)".*/\1/')"
  health="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/actuator/health")"
  printf '%-18s %-36s %-8s %s\n' "$service" "${instance:-?}" "UP" "$health"
  port=$((port + 1))
done

leaks=0
for secret in "${SECRETS[@]}"; do
  leaks=$((leaks + $(cat "$WORK_DIR"/*.log | grep -c -F "$secret" || true)))
done
echo
log "occurrences of the generated passwords in all service logs: $leaks"
[ "$leaks" -eq 0 ] || { echo "credential leak detected in logs" >&2; exit 1; }
log "PASS: all six existing services registered with discovery-service and are UP"
