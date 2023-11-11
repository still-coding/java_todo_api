package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.libs.Json;
import play.mvc.Result;
import play.test.WithApplication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;
import static play.test.Helpers.route;

public class UserControllerTest extends WithApplication {
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    private final JsonNode jsonNode = Json.newObject();


    @Before
    public void setUp() {
        ((ObjectNode) jsonNode).put("name", "John");
        ((ObjectNode) jsonNode).put("password", "1q2w3e4r");

    }

    @After
    public void tearDown() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .bodyJson(jsonNode)
                .uri("/login");

        Result result = route(app, request);
        if (result.status() == OK)
        {
            request = new Http.RequestBuilder()
                    .method(DELETE)
                    .header("Authorization", "Bearer " + Json.parse(contentAsString(result)).get("token").asText())
                    .uri("/users");
            result = route(app, request);
            assertEquals(OK, result.status());
        }

    }

    @Test
    public void canCreateUser() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(jsonNode)
                .uri("/users");

        Result result = route(app, request);
        assertEquals(CREATED, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("name").asText(), "John");
        assertEquals(jsonResult.get("password"), null);

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertTrue(jsonResult.get("createdAt").asText().contains(formatter.format(new Date()).substring(0, 18)));

    }

    @Test
    public void cantCreateUserWithoutName() {
        ((ObjectNode) jsonNode).remove("name");
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(jsonNode)
                .uri("/users");

        Result result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
        ((ObjectNode) jsonNode).put("name", "John");
    }

    @Test
    public void cantCreateUserWithoutPassword() {
        ((ObjectNode) jsonNode).remove("password");

        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(jsonNode)
                .uri("/users");

        Result result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
        ((ObjectNode) jsonNode).put("password", "1q2w3e4r");
    }


    @Test
    public void cantRetrieveWithoutLogin() {
        final JsonNode jsonNode = Json.newObject();
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .bodyJson(jsonNode)
                .uri("/users");

        Result result = route(app, request);
        assertEquals(UNAUTHORIZED, result.status());
    }

    @Test
    public void cantCreateSameUserTwice() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(jsonNode)
                .uri("/users");

        Result result = route(app, request);
        assertEquals(CREATED, result.status());

        result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }
}
