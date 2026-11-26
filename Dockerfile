# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system udpchat && useradd --system --gid udpchat --create-home udpchat \
    && mkdir -p /app/data \
    && chown -R udpchat:udpchat /app

COPY --from=build --chown=udpchat:udpchat /workspace/target/UDPmultiChat-1.0-SNAPSHOT.jar /app/app.jar

ENV PORT=8080 \
    BIND_ADDRESS=0.0.0.0 \
    CHAT_DATA_DIR=/app/data \
    COOKIE_SECURE=true \
    SERVER_FORWARD_HEADERS_STRATEGY=framework \
    SERVER_ERROR_INCLUDE_STACKTRACE=never \
    SERVER_ERROR_INCLUDE_BINDING_ERRORS=never \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

EXPOSE 8080

VOLUME ["/app/data"]

ENTRYPOINT ["sh", "-c", "mkdir -p \"${CHAT_DATA_DIR}\" && chown -R udpchat:udpchat \"${CHAT_DATA_DIR}\" && HOME=/home/udpchat exec setpriv --reuid=\"$(id -u udpchat)\" --regid=\"$(id -g udpchat)\" --clear-groups java ${JAVA_OPTS:--XX:MaxRAMPercentage=70.0} -jar /app/app.jar"]
