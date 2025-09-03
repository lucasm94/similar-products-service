# Imagen base con Java 21
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/similar-products-service-*.jar app.jar

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "app.jar"]
