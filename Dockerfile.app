# Stage 1: Build the Java application
FROM maven:3.8.5-openjdk-8 AS build

WORKDIR /app

# Copy the source code and resources into the container
COPY TestHttpServer.java src/main/java/
COPY log4j2.xml src/main/resources/
COPY pom.xml .

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the Java application
FROM openjdk:8-jre-alpine

WORKDIR /usr/src/myapp

# Copy the packaged JAR file from the build stage
COPY --from=build /app/target/rranjan-java-app-1.0-SNAPSHOT.jar rranjan-java-app.jar
COPY --from=docker.elastic.co/observability/apm-agent-java:latest /usr/agent/elastic-apm-agent.jar /usr/src/myapp/elastic-apm-agent.jar

# Expose the port that your Java application will use
EXPOSE 8001

# Run the Java application with the APM agent
CMD ["java", \
    "-javaagent:/usr/src/myapp/elastic-apm-agent.jar", \
    "-Delastic.apm.service_name=rranjan-java-app", \
    "-Delastic.apm.server_urls=<Add your APM URL here>", \
    "-Delastic.apm.secret_token=<Add your APM secret code here", \
    "-Delastic.apm.environment=dev", \
    "-Delastic.apm.application_packages=com.example", \
    "-Delastic.apm.log_ecs_reformatting=OVERRIDE", \
    "-Delastic.apm.log_sending=true", \
    "-Delastic.apm.log_level=DEBUG", \
    "-jar", "rranjan-java-app.jar", \
    "--server.port=8001"]

