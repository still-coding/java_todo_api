package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import filters.JwtAuthorizationFilter;
import models.User;
import play.libs.Json;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import store.UserStore;
import utils.PasswordHelper;
import utils.ResponseHelper;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class UserController extends Controller {
    private ClassLoaderExecutionContext ec;
    private UserStore userStore;

    @Inject
    public UserController(ClassLoaderExecutionContext ec, UserStore userStore) {
        this.userStore = userStore;
        this.ec = ec;
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

            Optional<User> userOptional = userStore.create(Json.fromJson(json, User.class));
            if (userOptional.isEmpty()) {
                // feels like 409 but didn't find it in Results
                return badRequest(ResponseHelper.createResponse("User " + userName + " already exists"));
            }

            if (password == null || password.length() < 6) {
                return unauthorized(ResponseHelper.createResponse("Password is too short"));
            }

            String hash = PasswordHelper.hash(password);
            return userOptional.map(user -> {
                user.setPassword(hash);
                JsonNode jsonResponse = Json.toJson(user);
                ((ObjectNode)jsonResponse).remove("password");
                return created(jsonResponse);
            }).orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> retrieve(Http.Request request) {
        return supplyAsync(() -> {
            String userId = request.attrs().get(Security.USERNAME);
            final Optional<User> userOptional = userStore.retrieve(Integer.parseInt(userId));
            return userOptional.map(user -> {
                JsonNode jsonResponse = Json.toJson(user);
                ((ObjectNode)jsonResponse).remove("password");
                return ok(jsonResponse);
            }).orElse(notFound(ResponseHelper.createResponse("User not found")));
        }, ec.current());
    }
}