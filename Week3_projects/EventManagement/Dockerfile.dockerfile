FROM hseeberger/scala-sbt:latest AS builder
WORKDIR /app
COPY . .
RUN sbt clean stage

FROM eclipse-temurin:11-jre-jammy
WORKDIR /opt/app
COPY --from=builder /app/target/universal/stage /opt/app
EXPOSE 9001
ENV JAVA_OPTS="-Dplay.server.http.address=0.0.0.0 -Dplay.server.http.port=9001"
ENTRYPOINT ["bin/EventManagement"]