package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.libs.Json;
import play.mvc.Result;
import play.test.WithApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;
import static play.test.Helpers.route;


public class AuthControllerTest extends WithApplication {
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    private final JsonNode userNode = Json.newObject();

    @Before
    public void createUser() {
        ((ObjectNode) userNode).put("name", "John");
        ((ObjectNode) userNode).put("password", "1q2w3e4r");

        Http.RequestBuilder createRequest = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(userNode)
                .uri("/users");

        Result result = route(app, createRequest);
        assertEquals(CREATED, result.status());
    }

    @Test
    public void canLogin() {
        Http.RequestBuilder loginRequest = new Http.RequestBuilder()
                .method(GET)
                .bodyJson(userNode)
                .uri("/login");

        Result result = route(app, loginRequest);
        assertEquals(OK, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());
        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertTrue(jsonResult.get("token").asText().startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."));
    }
}