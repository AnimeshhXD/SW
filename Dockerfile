# Replace or create Dockerfile to honor Render's PORT env var and keep sensible defaults
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the jar produced by `mvn package` (assumes target/*.jar)
COPY target/*.jar app.jar

# Tunable JVM options (override at runtime if needed)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Expose the common port (Render will provide PORT env at runtime)
EXPOSE 8080

# Use the Port provided by the platform (Render sets $PORT). Fall back to 8080 locally.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
