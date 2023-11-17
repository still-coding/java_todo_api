package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import filters.JwtAuthorizationFilter;
import models.Task;
import models.User;
import org.bson.types.ObjectId;
import play.libs.Json;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import store.ImageStore;
import store.TaskStore;
import store.UserStore;
import utils.PasswordHelper;
import utils.ResponseHelper;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class UserController extends Controller {
    private ClassLoaderExecutionContext ec;
    private UserStore userStore;
    private TaskStore taskStore;
    private ImageStore imageStore;

    @Inject
    public UserController(ClassLoaderExecutionContext ec, UserStore userStore, TaskStore taskStore, ImageStore imageStore) {
        this.ec = ec;
        this.userStore = userStore;
        this.taskStore = taskStore;
        this.imageStore = imageStore;
    }

    public CompletionStage<Result> create(Http.Request request) {
        JsonNode json = request.body().asJson();
        return supplyAsync(() -> {
            if (json == null) {
                return badRequest(ResponseHelper.createResponse("Expecting JSON data"));
            }
            // tried to validate json here with jackson.annotation.JsonProperty and
            // try {} catch (JsonMappingException e) {} but failed miserably
            String userName = json.findPath("name").textValue();
            String password = json.findPath("password").textValue();
            boolean nameMissing = userName == null;
            boolean passwordMissing = password == null;
            if (nameMissing || passwordMissing) {
                return badRequest(ResponseHelper.createResponse("Missing parameter(s): "
                        + (nameMissing ? "[name]" : "") + (passwordMissing ? "[password]" : "")));
            }
            User reqUser = Json.fromJson(json, User.class);
            if (userStore.getByUsername(reqUser.getName()).isPresent())
            {
                return badRequest(ResponseHelper.createResponse("User " + userName + " already exists"));
            }
            if (password.length() < 6) {
                return badRequest(ResponseHelper.createResponse("Password is too short"));
            }
            reqUser.setPassword(PasswordHelper.hash(password));
            Optional<User> userOptional = userStore.create(reqUser);
            return userOptional.map(user -> {
                JsonNode jsonResponse = Json.toJson(user);
                ((ObjectNode)jsonResponse).remove("password");
                return created(jsonResponse);
            }).orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
            // feels like 409 but didn't find it in Results
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> retrieve(Http.Request request) {
        return supplyAsync(() -> {
            String userId = request.attrs().get(Security.USERNAME);
            final Optional<User> userOptional = userStore.retrieve(new ObjectId(userId));
            return userOptional.map(user -> {
                JsonNode jsonResponse = Json.toJson(user);
                ((ObjectNode)jsonResponse).remove("password");
                return ok(jsonResponse);
            }).orElse(notFound(ResponseHelper.createResponse("User not found")));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> delete(Http.Request request) {
        return supplyAsync(() -> {
            String userId = request.attrs().get(Security.USERNAME);
            ObjectId userObjectId = new ObjectId(userId);
            if (userStore.delete(userObjectId))
            {
                List<Task> taskList = taskStore.retrieveAll(userObjectId);
                for (Task t : taskList) {
                    imageStore.deleteList(t.truePdfPages());
                    taskStore.delete(t.trueId());
                }
                return ok(ResponseHelper.createResponse("User with id:" + userId + " deleted"));
            }
            return notFound(ResponseHelper.createResponse("User with id:" + userId + " not found"));
        }, ec.current());
    }
}