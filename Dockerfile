FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y git

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar ./app.jar

ENTRYPOINT java -jar app.jar