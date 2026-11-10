#!/usr/bin/env bash
# Verifies issue #58 acceptance criterion: the existing services register with discovery-service and show UP.
#
# Runs, with NO source changes, discovery-service plus every service that ships a Eureka client:
#   auth, user, mentor   -> MySQL 8.4
#   booking, payment, review -> SQL Server 2022 + RabbitMQ (their outbox publishers/listeners)
#   media                -> MongoDB replica set
# Clients are switched on purely through environment variables the services already read
# (EUREKA_ENABLED / EUREKA_URL, DB_*, SPRING_RABBITMQ_*, MONGODB_URI). Every password is random per run
# and never written to disk. api-gateway and catalog have no Eureka client yet (#59, #60).
#
# Requirements: bash, docker (daemon running), curl, Java 21. Ports 8761 and 8081-8086, 8088 must be free.
# Usage (from anywhere):  bash services/discovery-service/verification/verify-client-registration.sh
# Options (env): SKIP_BUILD=1 reuses already built jars; VERIFY_WORK_DIR=<dir> keeps the service logs there.
# Exit code 0 = every listed service registered and UP.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICES_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
RUN_ID="mm-eureka-verify-$$"
# Logs go to VERIFY_WORK_DIR if set (kept afterwards), otherwise to a temp dir removed on exit.
WORK_DIR="${VERIFY_WORK_DIR:-$(mktemp -d)}"
KEEP_WORK_DIR="${VERIFY_WORK_DIR:+1}"
mkdir -p "$WORK_DIR"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-300}"

MYSQL_CONTAINER="$RUN_ID-mysql";       MYSQL_PORT="${MYSQL_PORT:-33306}"
MSSQL_CONTAINER="$RUN_ID-mssql";       MSSQL_PORT="${MSSQL_PORT:-31433}"
MONGO_CONTAINER="$RUN_ID-mongo";       MONGO_PORT="${MONGO_PORT:-27047}"
RABBIT_CONTAINER="$RUN_ID-rabbitmq";   RABBIT_PORT="${RABBIT_PORT:-35672}"
DISCOVERY_PORT=8761

MYSQL_SERVICES=(auth-service user-service mentor-service)
MSSQL_SERVICES=(booking-service payment-service review-service)
MONGO_SERVICES=(media-service)
ALL_SERVICES=("${MYSQL_SERVICES[@]}" "${MSSQL_SERVICES[@]}" "${MONGO_SERVICES[@]}")
declare -A PORT=([auth-service]=8081 [user-service]=8082 [mentor-service]=8083 [booking-service]=8084
                 [payment-service]=8085 [review-service]=8086 [media-service]=8088)

random_hex() { od -An -N16 -tx1 /dev/urandom | tr -d ' \n'; }
native_path() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else echo "$1"; fi; }
log() { printf '[verify] %s\n' "$*"; }
# Git Bash on Windows rewrites absolute paths such as /opt/... in arguments; container paths must reach docker as-is.
dexec() { MSYS_NO_PATHCONV=1 docker exec "$@"; }

DISCOVERY_USER="discovery"
DISCOVERY_PASSWORD="v-$(random_hex)"
MYSQL_ROOT_PASSWORD="v-$(random_hex)"
MYSQL_APP_PASSWORD="v-$(random_hex)"
MSSQL_SA_PASSWORD="Vx9-$(random_hex)"   # SQL Server requires upper, lower, digit and symbol
RABBIT_USER="verify"
RABBIT_PASSWORD="v-$(random_hex)"
SECRETS=("$DISCOVERY_PASSWORD" "$MYSQL_ROOT_PASSWORD" "$MYSQL_APP_PASSWORD" "$MSSQL_SA_PASSWORD" "$RABBIT_PASSWORD")
PIDS=()
declare -A SERVICE_PID

cleanup() {
  log "cleaning up processes and containers"
  for pid in "${PIDS[@]:-}"; do [ -n "$pid" ] && kill "$pid" 2>/dev/null || true; done
  for pid in "${PIDS[@]:-}"; do [ -n "$pid" ] && wait "$pid" 2>/dev/null || true; done
  docker rm -f "$MYSQL_CONTAINER" "$MSSQL_CONTAINER" "$MONGO_CONTAINER" "$RABBIT_CONTAINER" >/dev/null 2>&1 || true
  for port in "$DISCOVERY_PORT" "${PORT[@]}"; do
    if curl -s -o /dev/null --max-time 1 "http://localhost:$port"; then
      log "WARNING: something is still listening on port $port after cleanup"
    fi
  done
  [ -n "$KEEP_WORK_DIR" ] || rm -rf "$WORK_DIR"
}
trap cleanup EXIT

for port in "$DISCOVERY_PORT" "${PORT[@]}"; do
  if curl -s -o /dev/null --max-time 1 "http://localhost:$port"; then
    echo "Port $port is already in use; stop the process using it and retry." >&2; exit 2
  fi
done

wait_until() { # wait_until <description> <command...>
  local description="$1"; shift
  for _ in $(seq 1 "$TIMEOUT_SECONDS"); do "$@" >/dev/null 2>&1 && return 0; sleep 1; done
  echo "timed out waiting for $description" >&2; return 1
}

log "starting MySQL 8.4, SQL Server 2022, MongoDB 8 (replica set) and RabbitMQ containers"
for image in mysql:8.4 mcr.microsoft.com/mssql/server:2022-latest mongo:8.0 rabbitmq:4.1-alpine; do
  docker pull -q "$image" >/dev/null
done
docker run -d --name "$MYSQL_CONTAINER" -p "$MYSQL_PORT:3306" -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" mysql:8.4 >/dev/null
docker run -d --name "$MSSQL_CONTAINER" -p "$MSSQL_PORT:1433" -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD="$MSSQL_SA_PASSWORD" \
  mcr.microsoft.com/mssql/server:2022-latest >/dev/null
docker run -d --name "$MONGO_CONTAINER" -p "$MONGO_PORT:$MONGO_PORT" mongo:8.0 --replSet rs0 --bind_ip_all --port "$MONGO_PORT" >/dev/null
docker run -d --name "$RABBIT_CONTAINER" -p "$RABBIT_PORT:5672" -e RABBITMQ_DEFAULT_USER="$RABBIT_USER" \
  -e RABBITMQ_DEFAULT_PASS="$RABBIT_PASSWORD" rabbitmq:4.1-alpine >/dev/null

# Test sources are not compiled (-Dmaven.test.skip): this script checks runtime registration only, and
# the services' own test suites are outside the scope of #58.
log "building discovery-service and the client services (sources unchanged, tests skipped)"
for service in discovery-service "${ALL_SERVICES[@]}"; do
  if [ "${SKIP_BUILD:-0}" = 1 ] && ls "$SERVICES_DIR/$service"/target/"$service"-*.jar >/dev/null 2>&1; then
    continue
  fi
  (cd "$SERVICES_DIR/$service" && ./mvnw -q -B -ntp -Dmaven.test.skip=true package) || { echo "build failed: $service" >&2; exit 1; }
done

sqlcmd() { dexec "$MSSQL_CONTAINER" /opt/mssql-tools18/bin/sqlcmd -C -b -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -Q "$1"; }
wait_until "MySQL" dexec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql -uroot -e "SELECT 1"
wait_until "SQL Server" sqlcmd "SELECT 1"
wait_until "MongoDB" dexec "$MONGO_CONTAINER" mongosh --port "$MONGO_PORT" --quiet --eval "db.adminCommand({ping:1}).ok"
wait_until "RabbitMQ" dexec "$RABBIT_CONTAINER" rabbitmq-diagnostics -q check_port_connectivity

log "creating one database per service and initiating the MongoDB replica set"
for service in "${MYSQL_SERVICES[@]}"; do
  db="mm_${service%-service}"
  dexec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql -uroot -e \
    "CREATE DATABASE $db; CREATE USER '$db'@'%' IDENTIFIED BY '$MYSQL_APP_PASSWORD'; GRANT ALL ON $db.* TO '$db'@'%';"
done
for service in "${MSSQL_SERVICES[@]}"; do
  sqlcmd "CREATE DATABASE mm_${service%-service};" >/dev/null
done
dexec "$MONGO_CONTAINER" mongosh --port "$MONGO_PORT" --quiet \
  --eval "rs.initiate({_id:'rs0',members:[{_id:0,host:'localhost:$MONGO_PORT'}]})" >/dev/null
wait_until "MongoDB primary" bash -c "[ \"\$(MSYS_NO_PATHCONV=1 docker exec $MONGO_CONTAINER mongosh --port $MONGO_PORT --quiet --eval 'rs.status().myState')\" = 1 ]"

start_jar() { # start_jar <service> [ENV=VALUE ...]
  local service="$1"; shift
  local jar; jar="$(ls "$SERVICES_DIR/$service"/target/"$service"-*.jar | grep -v '\.original$' | head -1)"
  mkdir -p "$WORK_DIR" # temp cleaners may remove an idle temp dir during long runs
  # exec: the recorded PID is the JVM itself, so cleanup really stops it.
  (cd "$SERVICES_DIR/$service" && exec env "$@" java -jar "$jar" >"$WORK_DIR/$service.log" 2>&1) &
  PIDS+=("$!")
  SERVICE_PID[$service]=$!
}

log "starting discovery-service on :$DISCOVERY_PORT"
start_jar discovery-service DISCOVERY_USERNAME="$DISCOVERY_USER" DISCOVERY_PASSWORD="$DISCOVERY_PASSWORD"
wait_until "discovery-service health" curl -sf "http://localhost:$DISCOVERY_PORT/actuator/health"

PUBLIC_KEY="file:$(native_path "$SERVICES_DIR/auth-service/jwt-keys/public.pem")"
PRIVATE_KEY="$(native_path "$SERVICES_DIR/auth-service/jwt-keys/private.pem")"
COMMON_ENV=(
  EUREKA_ENABLED=true
  "EUREKA_URL=http://$DISCOVERY_USER:$DISCOVERY_PASSWORD@localhost:$DISCOVERY_PORT/eureka/"
  # Keeps the registry URL (with credentials) out of client logs on connection errors; see README §7.
  LOGGING_LEVEL_COM_NETFLIX_DISCOVERY_SHARED_TRANSPORT_DECORATOR_REDIRECTINGEUREKAHTTPCLIENT=WARN
  AUTH_JWT_AUDIENCE=mentor-matching-api
)
RABBIT_ENV=(
  SPRING_RABBITMQ_HOST=localhost "SPRING_RABBITMQ_PORT=$RABBIT_PORT"
  "SPRING_RABBITMQ_USERNAME=$RABBIT_USER" "SPRING_RABBITMQ_PASSWORD=$RABBIT_PASSWORD"
)

log "starting the client services with Eureka enabled"
for service in "${MYSQL_SERVICES[@]}"; do
  db="mm_${service%-service}"
  key=("AUTH_JWT_PUBLIC_KEY_LOCATION=$PUBLIC_KEY")
  [ "$service" = auth-service ] && key=("AUTH_JWT_PRIVATE_KEY_PATH=$PRIVATE_KEY")
  start_jar "$service" "${COMMON_ENV[@]}" "${key[@]}" \
    "DB_URL=jdbc:mysql://localhost:$MYSQL_PORT/$db" "DB_USERNAME=$db" "DB_PASSWORD=$MYSQL_APP_PASSWORD"
done
for service in "${MSSQL_SERVICES[@]}"; do
  db="mm_${service%-service}"
  start_jar "$service" "${COMMON_ENV[@]}" "${RABBIT_ENV[@]}" "AUTH_JWT_PUBLIC_KEY_LOCATION=$PUBLIC_KEY" \
    "DB_URL=jdbc:sqlserver://localhost:$MSSQL_PORT;databaseName=$db;encrypt=true;trustServerCertificate=true" \
    DB_USERNAME=sa "DB_PASSWORD=$MSSQL_SA_PASSWORD"
done
start_jar media-service "${COMMON_ENV[@]}" "AUTH_JWT_PUBLIC_KEY_LOCATION=$PUBLIC_KEY" \
  "MONGODB_URI=mongodb://localhost:$MONGO_PORT/mm_media?replicaSet=rs0"

registry() {
  curl -sf -u "$DISCOVERY_USER:$DISCOVERY_PASSWORD" -H 'Accept: application/json' "http://localhost:$DISCOVERY_PORT/eureka/apps"
}
is_up() { # is_up <service> <apps-json-without-whitespace>
  local name; name="$(echo "$1" | tr '[:lower:]' '[:upper:]')"
  echo "$2" | grep -q "\"app\":\"$name\",[^}]*\"status\":\"UP\""
}
is_running() { kill -0 "${SERVICE_PID[$1]}" 2>/dev/null; }
# Settled = every service is either UP in the registry or its JVM has exited (failed to start on its own).
settled() {
  local apps; apps="$(registry | tr -d ' \n')" || return 1
  for service in "${ALL_SERVICES[@]}"; do
    is_up "$service" "$apps" || ! is_running "$service" || return 1
  done
}
startup_error() { # first meaningful failure line of a service log, without timestamps
  grep -m1 -o -E "Schema validation: [^;]{0,120}|APPLICATION FAILED TO START|Caused by: [^:]+: [^;]{0,120}" \
    "$WORK_DIR/$1.log" 2>/dev/null || echo "exited (see its log)"
}

log "waiting (up to ${TIMEOUT_SECONDS}s) for ${#ALL_SERVICES[@]} services to be UP in the registry"
wait_until "services to register or exit" settled || true

apps_json="$(registry | tr -d ' \n' || true)"
registered=0; not_started=(); not_registered=()
printf '\n%-18s %-36s %-8s %s\n' "SERVICE" "INSTANCE" "EUREKA" "HEALTH / NOTE"
for service in "${ALL_SERVICES[@]}"; do
  name="$(echo "$service" | tr '[:lower:]' '[:upper:]')"
  if is_up "$service" "$apps_json"; then
    instance="$(echo "$apps_json" | grep -o "\"instanceId\":\"[^\"]*\",[^}]*\"app\":\"$name\"" | head -1 | sed 's/"instanceId":"\([^"]*\)".*/\1/')"
    health="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${PORT[$service]}/actuator/health")"
    printf '%-18s %-36s %-8s %s\n' "$service" "${instance:-?}" "UP" "$health"
    registered=$((registered + 1))
  elif ! is_running "$service"; then
    printf '%-18s %-36s %-8s %s\n' "$service" "-" "-" "DID NOT START: $(startup_error "$service")"
    not_started+=("$service")
  else
    printf '%-18s %-36s %-8s %s\n' "$service" "-" "MISSING" "running but not registered"
    not_registered+=("$service")
  fi
done

leaks=0
for secret in "${SECRETS[@]}"; do
  leaks=$((leaks + $(cat "$WORK_DIR"/*.log | grep -c -F "$secret" || true)))
done
echo
log "occurrences of the generated passwords in all service logs: $leaks"
[ "$leaks" -eq 0 ] || { echo "credential leak detected in logs" >&2; exit 1; }
if [ "${#not_registered[@]}" -gt 0 ]; then
  log "FAIL: running but not registered: ${not_registered[*]}"; exit 1
fi
if [ "${#not_started[@]}" -gt 0 ]; then
  # A service that cannot boot at all is not a discovery problem; report it separately and fail loudly.
  log "PARTIAL: $registered/${#ALL_SERVICES[@]} registered and UP; did not start on their own: ${not_started[*]}"
  exit 3
fi
log "PASS: all ${#ALL_SERVICES[@]} services with a Eureka client registered with discovery-service and are UP"
