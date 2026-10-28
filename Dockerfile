FROM eclipse-temurin:25-jre

# Run as non-root user (uid 10001)
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /sbin/nologin -M appuser

WORKDIR /app

# Pre-built jar copied from target/ (placed there by CI artifact download)
COPY target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
