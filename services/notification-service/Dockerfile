# Build stage: uses the project's Maven Wrapper so the image builds with the same Maven version as local builds.
FROM eclipse-temurin:21.0.10_7-jdk-noble AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package \
    && cp target/notification-service-*.jar /workspace/app.jar

# Runtime stage: JRE only, non-root user, no build tools or sources.
FROM eclipse-temurin:21.0.10_7-jre-noble
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8089
# MONGODB_URI, AUTH_JWT_* and the JWT public key (mounted file) are supplied at runtime, never baked into the image.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
