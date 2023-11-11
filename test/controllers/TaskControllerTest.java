package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;


public class TaskControllerTest extends WithApplication {
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }


    private static String token = null;

    private static ArrayList<String> taskIds = null;

    @Before
    public void setUp() {
        JsonNode userNode = Json.newObject();
        ((ObjectNode) userNode).put("name", "John");
        ((ObjectNode) userNode).put("password", "1q2w3e4r");

        Http.RequestBuilder createRequest = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(userNode)
                .uri("/users");

        Result result = route(app, createRequest);
        assertEquals(CREATED, result.status());

        Http.RequestBuilder loginRequest = new Http.RequestBuilder()
                .method(GET)
                .bodyJson(userNode)
                .uri("/login");

        result = route(app, loginRequest);
        assertEquals(OK, result.status());

        token = Json.parse(contentAsString(result)).get("token").asText();
        taskIds = new ArrayList<>();
    }

    @After
    public void tearDown() {

        Http.RequestBuilder request;
        Result result;
        for (String id: taskIds) {
            request= new Http.RequestBuilder()
                    .method(DELETE)
                    .header("Authorization", "Bearer " + token)
                    .uri("/tasks/" + id);
        }


        request = new Http.RequestBuilder()
                .method(DELETE)
                .header("Authorization", "Bearer " + token)
                .uri("/users");

        result = route(app, request);
        assertEquals(OK, result.status());

        token = null;
        taskIds = null;
    }


    @Test
    public void canCreateTask() {
        final JsonNode jsonNode = Json.newObject();
        ((ObjectNode) jsonNode).put("name", "test task 1");
        ((ObjectNode) jsonNode).put("description", "test task 1 description");

        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(jsonNode)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks");

        Result result = route(app, request);
        assertEquals(CREATED, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("name").asText(), "test task 1");
        assertEquals(jsonResult.get("description").asText(), "test task 1 description");

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertTrue(jsonResult.get("createdAt").asText().contains(formatter.format(new Date()).substring(0, 18)));
        taskIds.add(jsonResult.get("id").asText());
    }

    @Test
    public void canCreateTwoTasks() {
        canCreateTask();
        final JsonNode jsonNode = Json.newObject();
        ((ObjectNode) jsonNode).put("name", "test task 2");
        ((ObjectNode) jsonNode).put("description", "test task 2 description");

        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .bodyJson(jsonNode)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks");

        Result result = route(app, request);
        assertEquals(CREATED, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("name").asText(), "test task 2");
        assertEquals(jsonResult.get("description").asText(), "test task 2 description");

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertTrue(jsonResult.get("createdAt").asText().contains(formatter.format(new Date()).substring(0, 18)));
        taskIds.add(jsonResult.get("id").asText());
    }

    @Test
    public void canUpdateTask() {
        canCreateTask();
        final JsonNode jsonNode = Json.newObject();
        ((ObjectNode) jsonNode).put("id", taskIds.get(0));
        ((ObjectNode) jsonNode).put("name", "test task 2");
        ((ObjectNode) jsonNode).put("description", "test task 2 description");

        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(PUT)
                .bodyJson(jsonNode)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks");

        Result result = route(app, request);
        assertEquals(OK, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("id").asText(), taskIds.get(0));
//        assertEquals(jsonResult.get("userId").asText(), "0");
        assertEquals(jsonResult.get("name").asText(), "test task 2");
        assertEquals(jsonResult.get("description").asText(), "test task 2 description");

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertTrue(jsonResult.get("createdAt").asText().contains(formatter.format(new Date()).substring(0, 18)));
    }

    @Test
    public void canDeleteTask() {
        canCreateTask();
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(DELETE)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks/" + taskIds.get(0));

        Result result = route(app, request);
        assertEquals(OK, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("message").asText(), "Task with id:" + taskIds.get(0) +" deleted");

        request = new Http.RequestBuilder()
                .method(GET)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks/" + taskIds.get(0));
        result = route(app, request);
        assertEquals(NOT_FOUND, result.status());
    }

    @Test
    public void canGetAllTasksOneByOne() {
        canCreateTwoTasks();
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks/" + taskIds.get(0));

        Result result = route(app, request);
        assertEquals(OK, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("name").asText(), "test task 1");

        request = new Http.RequestBuilder()
                .method(GET)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks/" + taskIds.get(1));

        result = route(app, request);
        assertEquals(OK, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        jsonResult = Json.parse(contentAsString(result));
        assertEquals(jsonResult.get("name").asText(), "test task 2");
    }


    @Test
    public void canGetAllTasksAtOnce() {
        canCreateTwoTasks();
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .header("Authorization", "Bearer " + token)
                .uri("/tasks?sort=created");

        Result result = route(app, request);
        assertEquals(OK, result.status());
        assertTrue(result.contentType().isPresent());
        assertEquals("application/json", result.contentType().get());

        JsonNode jsonResult = Json.parse(contentAsString(result));
        System.out.println(jsonResult);
        JsonNode nodeTask1 = jsonResult.get(0);
        assertEquals(nodeTask1.get("name").asText(), "test task 1");
        JsonNode nodeTask2 = jsonResult.get(1);
        assertEquals(nodeTask2.get("name").asText(), "test task 2");
    }
}