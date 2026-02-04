# Step 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run
FROM eclipse-temurin:21-jdk-alpine
COPY --from=build /target/digital-judge.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]