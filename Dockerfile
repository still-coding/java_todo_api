FROM openjdk:21-slim-bullseye

ADD target/universal/todo-api-1.0-SNAPSHOT.tgz ./
COPY .env /todo-api-1.0-SNAPSHOT

CMD ["/todo-api-1.0-SNAPSHOT/bin/todo-api", "-Dplay.http.secret.key=db9623561f7e5ce3d7b5b193b044c967f4c1e87481b2a5dbe9ba4f3465b739c8"]
