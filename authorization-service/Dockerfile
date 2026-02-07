FROM eclipse-temurin:25-jre
ARG SERVICE
ENV SERVICE=${SERVICE}
WORKDIR /app
COPY ${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java","-jar","/app/app.jar"]
