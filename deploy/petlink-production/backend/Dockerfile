FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY petlink-server.jar /app/petlink-server.jar

RUN useradd --system --uid 10001 --create-home petlink \
    && mkdir -p /app/data/uploads \
    && chown -R 10001:10001 /app

USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
ENTRYPOINT ["java", "-jar", "/app/petlink-server.jar"]
