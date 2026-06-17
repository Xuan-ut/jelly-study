FROM eclipse-temurin:11-jre
WORKDIR /app
COPY target/jellystudy-studyplan-1.0-SNAPSHOT.jar app.jar
EXPOSE 8084 20885
ENTRYPOINT ["java", "-Dnacos.logging.path=/app/logs/nacos", "-jar", "app.jar", "--spring.profiles.active=docker"]
