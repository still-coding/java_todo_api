FROM openjdk:21-slim-bullseye

ADD target/universal/todo-api-1.0-SNAPSHOT.tgz ./
COPY .env /todo-api-1.0-SNAPSHOT
