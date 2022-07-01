FROM maven:3-openjdk-11 AS builder
COPY . /crawler
WORKDIR /crawler
RUN mvn clean install assembly:single

FROM openjdk:11-jre-slim
COPY --from=builder /crawler/target/*-jar-with-dependencies.jar /crawler.jar

USER root
ENV TIME_LIMIT 600
ENTRYPOINT ["java", "-jar", "/crawler.jar"]
