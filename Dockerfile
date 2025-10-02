# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jre-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the fat JAR from the build stage to the container
COPY target/icars-powertrain-bot.jar .

# Make port 8080 available to the world outside this container (if needed for health checks in the future)
# EXPOSE 8080

# Define environment variables with defaults.
# These will be overridden by docker-compose.yml values.
ENV TG_BOT_TOKEN=""
ENV BOT_USERNAME="ICarsPowertrainBot"
ENV OPS_CHAT_ID=""
ENV ADMIN_TG_IDS=""
ENV DB_URL="jdbc:postgresql://db:5432/icars"
ENV DB_USER="postgres"
ENV DB_PASS="postgres"
ENV LOG_LEVEL="INFO"

# Run the JAR file
# Note: The bot will wait for the database to be available.
# In a real production setup, a wait-script or health check dependency would be used.
CMD ["java", "-jar", "icars-powertrain-bot.jar"]
