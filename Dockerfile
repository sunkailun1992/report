FROM eclipse-temurin:21-jre
COPY build/libs/report.jar /app/report.jar
WORKDIR /app/
ENTRYPOINT ["java", "-jar", "/app/report.jar"]
