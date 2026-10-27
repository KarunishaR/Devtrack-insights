# Multi-stage Docker build for DevTrack Insights (Spring Boot + Static Frontend)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Optimize Maven memory for cloud builders (prevents OOM on free tier)
ENV MAVEN_OPTS="-Xmx384m"

# Copy pom.xml and source files
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src

# Build production jar skipping tests
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

# Tune JVM memory for cloud free tier (prevents OOMKilled error)
ENTRYPOINT ["sh", "-c", "java -Xmx350m -Xms128m -Dserver.port=${PORT:-8080} -jar app.jar"]
