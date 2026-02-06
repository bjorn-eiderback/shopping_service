# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY gateway-service/src gateway-service/src
COPY user-service/src user-service/src
COPY catalog-service/src catalog-service/src
COPY order-service/src order-service/src

ARG SERVICE
ENV SERVICE=order-service
RUN mvn -q -pl ${SERVICE} -am -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
ARG SERVICE
ENV SERVICE=order-service
COPY --from=build /workspace/${SERVICE}/target/*.jar /app/app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
