
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/javaapp-1.0-SNAPSHOT-shaded.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

