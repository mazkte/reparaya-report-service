FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn dependency:go-offline -B
COPY src ./src
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="mazkte" service="report-service" version="1.0.0"
RUN addgroup -S reparaya && adduser -S reparaya -G reparaya
WORKDIR /app
COPY --from=builder /build/target/reparaya-report-service-*.jar app.jar
USER reparaya
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health || exit 1
ENV JAVA_OPTS="-Xms256m -Xmx512m" SPRING_PROFILES_ACTIVE="prod"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
