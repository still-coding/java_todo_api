package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.User;
import play.libs.Json;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import store.UserStore;
import utils.JwtUtil;
import utils.PasswordHelper;
import utils.ResponseHelper;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class AuthController extends Controller {
    private ClassLoaderExecutionContext ec;

    private UserStore userStore;

    @Inject
    public AuthController(ClassLoaderExecutionContext ec, UserStore userStore) {
        this.userStore = userStore;
        this.ec = ec;
    }

    public CompletionStage<Result> login(Http.Request request) {
        JsonNode json = request.body().asJson();
        return supplyAsync(() -> {
            if (json == null) {
                return badRequest(ResponseHelper.createResponse("Expecting JSON data"));
            }
            User reqUser = Json.fromJson(json, User.class);
            Optional<User> userOptional = userStore.getByUsername(reqUser.getName());

            Result errorResult = unauthorized(ResponseHelper.createResponse("Invalid credentials"));

            return userOptional.map(user -> {
                if (user == null) {
                    return errorResult;
                }
                if (!user.getName().equals(reqUser.getName())) {
                    return errorResult;
                }
                if (!PasswordHelper.verify(user.getPassword(), reqUser.getPassword())) {
                    return errorResult;
                }
                JsonNode jsonObject = JsonNodeFactory.instance.objectNode();
                ((ObjectNode) jsonObject).put("token", JwtUtil.generateToken(user.getId()));
                ((ObjectNode) jsonObject).put("token_type", "bearer");
                return ok(jsonObject);
            }).orElse(errorResult);
        }, ec.current());
    }
}