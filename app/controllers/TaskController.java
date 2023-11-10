package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import filters.JwtAuthorizationFilter;
import models.Task;
import play.libs.Json;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import store.TaskStore;
import utils.ResponseHelper;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;


public class TaskController extends Controller {
    private final ClassLoaderExecutionContext ec;

    private final TaskStore taskStore;

    @Inject
    public TaskController(ClassLoaderExecutionContext ec, TaskStore taskStore) {
        this.taskStore = taskStore;
        this.ec = ec;
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> create(Http.Request request) {
        JsonNode json = request.body().asJson();
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            if (json == null) {
                return badRequest(ResponseHelper.createResponse("Expecting JSON data"));
            }
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            String name = json.findPath("name").textValue();
            String description = json.findPath("description").textValue();
            boolean nameMissing = name == null;
            boolean descriptionMissing = description == null;
            if (nameMissing || descriptionMissing) {
                return badRequest(ResponseHelper.createResponse("Missing parameter(s): "
                        + (nameMissing ? "[name]" : "") + (descriptionMissing ? "[description]" : "")));
            }
            ((ObjectNode)json).put("userId", Integer.parseInt(userId));
            Optional<Task> taskOptional = taskStore.create(Json.fromJson(json, Task.class));

            return taskOptional.map(task -> {
                JsonNode jsonResponse = Json.toJson(task);
                return created(jsonResponse);
            }).orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> retrieveAll(Http.Request request) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            Set<Task> result = taskStore.retrieveAll(Integer.parseInt(userId));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = mapper.convertValue(result, JsonNode.class);
            return ok(jsonData);
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> retrieve(Http.Request request, int id) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            int userIdInt = Integer.parseInt(userId);
            final Optional<Task> taskOptional = taskStore.retrieve(id);
            return taskOptional.map(task -> {
                if (task.getUserId() != userIdInt)
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                JsonNode jsonResponse = Json.toJson(task);
                return ok(jsonResponse);
            }).orElse(notFound(ResponseHelper.createResponse("Task with id:" + id + " not found")));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> update(Http.Request request) {
        JsonNode json = request.body().asJson();
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            if (json == null) {
                return badRequest(ResponseHelper.createResponse("Expecting JSON data"));
            }
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            String id = json.findPath("id").asText(null);
            String name = json.findPath("name").textValue();
            String description = json.findPath("description").textValue();
            boolean idMissing = id == null;
            boolean nameMissing = name == null;
            boolean descriptionMissing = description == null;

            if (idMissing || nameMissing || descriptionMissing) {
                return badRequest(ResponseHelper.createResponse("Missing parameter(s): "
                        + (idMissing ? "[id]" : "")
                        + (nameMissing ? "[name]" : "")
                        + (descriptionMissing ? "[description]" : "")));
            }
            Optional<Task> taskOptionalFromReq = taskStore.retrieve(Integer.parseInt(id));
            if (taskOptionalFromReq.isEmpty()) {
                return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
            }
            Task taskToUpdate = taskOptionalFromReq.get();
            if (taskToUpdate.getUserId() != Integer.parseInt(userId)) {
                return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
            }
            Optional<Task> taskOptional = taskStore.update(Json.fromJson(json, Task.class));
            return taskOptional.map(task -> {
                JsonNode jsonResponse = Json.toJson(task);
                return ok(jsonResponse);
            }).orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> delete(Http.Request request, int id) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            Optional<Task> taskOptional = taskStore.retrieve(id);
            return taskOptional.map(task -> {
                if (task.getUserId() != Integer.parseInt(userId))
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                taskStore.delete(id);
                return ok(ResponseHelper.createResponse("Task with id:" + id + " deleted"));
            }).orElse(notFound(ResponseHelper.createResponse("Task with id:" + id + " not found")));
        }, ec.current());
    }

}