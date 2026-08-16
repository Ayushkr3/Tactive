FROM eclipse-temurin:25-jdk

ENV TZ=UTC

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

COPY docker-compose.yml /app/config/docker-compose.yml

ENTRYPOINT ["java","-jar","app.jar"]