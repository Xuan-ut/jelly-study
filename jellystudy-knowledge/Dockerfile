FROM eclipse-temurin:11-jre
WORKDIR /app
COPY target/jellystudy-knowledge-1.0-SNAPSHOT.jar app.jar
EXPOSE 8081 20881
ENTRYPOINT ["java", "-Dnacos.logging.path=/app/logs/nacos", "-jar", "app.jar", "--spring.profiles.active=docker"]
