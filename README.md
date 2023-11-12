# java_todo_api

Todo REST API built with Play framework and Java.

## Quickest start

Go to http://91.210.168.40:9000/ to check if API is live.

Import `java-todo-api.postman_collection.json` to Postman and have fun.



## Quick start
Execute:
```shell
sbt Universal/packageZipTarball
docker compose up
```

Go to http://localhost:9000/ to see welcome message.

Use `java-todo-api` Postman Collection to work with API. 



This is my first ever java project so here how it was.

## Dev log

### Day 0

* set up an IDE
* figured out how to launch Play framework examples

### Day 1

* googled just every problem and almost gave up
* figured out how to make basic endpoints
* wrote in memory objects storage
* found out how to add third party libs

### Day 2

* made Postman collection to test API
* made JWT auth - that was a tough one at first
* finished endpoints
* found myself writing big chunks of code without googling
* wrote functional tests
* added tasks sorting
* added .env 

### Day 3

* added MongoDB storage with Morphia
* changed userId to BSON ObjectID/HexString type 
* added docker-compose.yml with Mongo and MongoExpress
* fixed tests
* added task labels
* dockerized app

### Plans

* set up testing workflow with GH Actions
