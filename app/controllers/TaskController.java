package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import filters.JwtAuthorizationFilter;
import models.Task;
import org.bson.types.ObjectId;
import org.javatuples.Pair;
import play.libs.Files;
import play.libs.Files.TemporaryFile;
import play.libs.Json;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import store.ImageStore;
import store.TaskStore;
import utils.PdfImageUtil;
import utils.ResponseHelper;
import utils.ZipUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.util.concurrent.CompletableFuture.supplyAsync;


public class TaskController extends Controller {
    private final ClassLoaderExecutionContext ec;
    private final TaskStore taskStore;
    private final ImageStore imageStore;
    @Inject
    public TaskController(ClassLoaderExecutionContext ec, TaskStore taskStore, ImageStore imageStore) {
        this.ec = ec;
        this.taskStore = taskStore;
        this.imageStore = imageStore;
    }

    private final Result correctJson = ok("Correct JSON");
    private Result checkJsonFields(JsonNode json, String[] fields) {
        HashMap<String, Boolean> checkMap = new HashMap<String, Boolean>();
        for (String field : fields) {
            checkMap.put(field, json.findValue(field) == null);
        }
        StringBuffer resultMessage = new StringBuffer();
        for (Map.Entry<String, Boolean> nullEntry : checkMap.entrySet()) {
            if (nullEntry.getValue()) {
                resultMessage.append("[");
                resultMessage.append(nullEntry.getKey());
                resultMessage.append("], ");
            }
        }
        if (resultMessage.isEmpty()) {
            return correctJson;
        }
        resultMessage.insert(0, "Missing parameter(s): ");
        return badRequest(ResponseHelper.createResponse(resultMessage.substring(0, resultMessage.length() - 2)));
    }

    private Optional<Task> createTaskFromJson(JsonNode json, String userId) {
        ((ObjectNode)json).put("userId", userId);
        return taskStore.create(Json.fromJson(json, Task.class));
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> create(Http.Request request) {
        return supplyAsync(() -> {
            JsonNode jsonBody = request.body().asJson();
            String userId = request.attrs().get(Security.USERNAME);
            Http.MultipartFormData<TemporaryFile> multiBody = request.body().asMultipartFormData();
            String[] requiredFields = {"name", "description"};
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            if (jsonBody != null) {
                Result jsonCheckResult = checkJsonFields(jsonBody, requiredFields);
                if (!jsonCheckResult.equals(correctJson)) {
                    return jsonCheckResult;
                }
                Optional<Task> taskOptional = createTaskFromJson(jsonBody, userId);
                return taskOptional.map(task -> created(Json.toJson(task)))
                        .orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
            }
            if (multiBody != null) {
                String[] jsons = multiBody.asFormUrlEncoded().get("json");
                if (jsons == null) {
                    return badRequest(ResponseHelper.createResponse("Multipart [json] field required"));
                }
                Http.MultipartFormData.FilePart<TemporaryFile> doc = multiBody.getFile("file");
                if (doc == null) {
                    return badRequest(ResponseHelper.createResponse("Multipart [file] field required"));
                }

                String fileName = doc.getFilename();
                TemporaryFile file = doc.getRef();
                if (!PdfImageUtil.isPdf(file)) {
                    return badRequest(ResponseHelper.createResponse("PDF file required"));
                }
                List<BufferedImage> imageList = PdfImageUtil.convertPdfToImages(file);
                if (imageList.isEmpty()) {
                    return internalServerError(ResponseHelper.createResponse("Failed to convert PDF pages to images"));
                }
                List<ObjectId> taskDocPages = imageStore.createList(imageList, fileName);
                JsonNode json = Json.parse(jsons[0]);
                Result jsonCheckResult = checkJsonFields(json, requiredFields);
                if (!jsonCheckResult.equals(correctJson)) {
                    return jsonCheckResult;
                }
                ((ObjectNode)json).put("pdfName", fileName);
                ((ObjectNode)json).set("pdfPages", Json.toJson(taskDocPages.stream().map(ObjectId::toHexString).collect(Collectors.toList())));
                Optional<Task> taskOptional = createTaskFromJson(json, userId);

                return taskOptional.map(task -> {
                    return created(Json.toJson(task));
                }).orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
            }
            return badRequest(ResponseHelper.createResponse("Expecting JSON data or Multipart data"));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> retrieveAll(Http.Request request, Optional<String> sort, List<String> labels) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            List<Task> result = taskStore.retrieveAll(new ObjectId(userId));

            if (!labels.isEmpty()) {
                result = result.stream()
                        .filter(task -> !Collections.disjoint(task.getLabels(), labels))
                        .collect(Collectors.toList());
            }

            if (sort.isPresent())
            {
                switch (sort.get()) {
                    case "created" -> result.sort(Comparator.comparing(Task::getCreatedAt));
                    case "-created" -> result.sort(Comparator.comparing(Task::getCreatedAt).reversed());
                    case "name" -> result.sort(Comparator.comparing(Task::getName));
                    case "-name" -> result.sort(Comparator.comparing(Task::getName).reversed());
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = mapper.convertValue(result, JsonNode.class);
            return ok(jsonData);
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> retrieve(Http.Request request, String id) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            final Optional<Task> taskOptional = taskStore.retrieve(new ObjectId(id));
            return taskOptional.map(task -> {
                if (!Objects.equals(task.getUserId(), userId))
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                JsonNode jsonResponse = Json.toJson(task);
                return ok(jsonResponse);
            }).orElse(notFound(ResponseHelper.createResponse("Task with id:" + id + " not found")));
        }, ec.current());
    }

    // TODO: too WET code
    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> update(Http.Request request) {
        String userId = request.attrs().get(Security.USERNAME);
        JsonNode jsonBody = request.body().asJson();
        Http.MultipartFormData<TemporaryFile> multiBody = request.body().asMultipartFormData();
        String[] requiredFields = {"id", "name", "description"};
        return supplyAsync(() -> {
            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            if (jsonBody != null) {
                Result jsonCheckResult = checkJsonFields(jsonBody, requiredFields);
                if (!jsonCheckResult.equals(correctJson)) {
                    return jsonCheckResult;
                }
                String id = jsonBody.findPath("id").asText(null);
                Optional<Task> taskOptionalFromReq = taskStore.retrieve(new ObjectId(id));
                if (taskOptionalFromReq.isEmpty()) {
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                }
                Task taskToUpdate = taskOptionalFromReq.get();
                if (!userId.equals(taskToUpdate.getUserId())) {
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                }
                Optional<Task> taskOptional = taskStore.update(Json.fromJson(jsonBody, Task.class));
                return taskOptional.map(task -> {
                    imageStore.deleteList(taskToUpdate.truePdfPages());
                    return ok(Json.toJson(task));
                }).orElse(internalServerError(ResponseHelper.createResponse("Could not update data")));
            }
            if (multiBody != null) {
                String[] jsons = multiBody.asFormUrlEncoded().get("json");
                if (jsons == null) {
                    return badRequest(ResponseHelper.createResponse("Multipart [json] field required"));
                }
                Http.MultipartFormData.FilePart<TemporaryFile> doc = multiBody.getFile("file");
                if (doc == null) {
                    return badRequest(ResponseHelper.createResponse("Multipart [file] field required"));
                }

                JsonNode json = Json.parse(jsons[0]);
                Result jsonCheckResult = checkJsonFields(json, requiredFields);
                if (!jsonCheckResult.equals(correctJson)) {
                    return jsonCheckResult;
                }
                String id = json.findPath("id").asText(null);
                Optional<Task> taskOptionalFromReq = taskStore.retrieve(new ObjectId(id));
                if (taskOptionalFromReq.isEmpty()) {
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                }
                Task taskToUpdate = taskOptionalFromReq.get();
                if (!userId.equals(taskToUpdate.getUserId())) {
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                }
                String fileName = doc.getFilename();
                TemporaryFile file = doc.getRef();
                if (!PdfImageUtil.isPdf(file)) {
                    return badRequest(ResponseHelper.createResponse("PDF file required"));
                }
                List<BufferedImage> imageList = PdfImageUtil.convertPdfToImages(file);
                if (imageList.isEmpty()) {
                    return internalServerError(ResponseHelper.createResponse("Failed to convert PDF pages to images"));
                }
                imageStore.deleteList(taskToUpdate.truePdfPages());
                List<ObjectId> taskDocPages = imageStore.createList(imageList, fileName);
                ((ObjectNode)json).put("pdfName", fileName);
                ((ObjectNode)json).set("pdfPages", Json.toJson(taskDocPages.stream().map(ObjectId::toHexString).collect(Collectors.toList())));

                Optional<Task> taskOptional = taskStore.update(Json.fromJson(json, Task.class));
                return taskOptional.map(task -> {
                    return ok(Json.toJson(task));
                }).orElse(internalServerError(ResponseHelper.createResponse("Could not update data")));
            }
            return badRequest(ResponseHelper.createResponse("Expecting JSON data or Multipart data"));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> delete(Http.Request request, String id) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            ObjectId taskId = new ObjectId(id);
            Optional<Task> taskOptional = taskStore.retrieve(taskId);
            return taskOptional.map(task -> {
                if (!userId.equals(task.getUserId()))
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));
                imageStore.deleteList(task.truePdfPages());
                taskStore.delete(taskId);
                return ok(ResponseHelper.createResponse("Task with id:" + id + " deleted"));
            }).orElse(notFound(ResponseHelper.createResponse("Task with id:" + id + " not found")));
        }, ec.current());
    }


    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> export(Http.Request request, String id) {
        String userId = request.attrs().get(Security.USERNAME);
        return supplyAsync(() -> {
            ObjectId taskId = new ObjectId(id);
            Optional<Task> taskOptional = taskStore.retrieve(taskId);
            return taskOptional.map(task -> {
                if (!userId.equals(task.getUserId()))
                    return notFound(ResponseHelper.createResponse("Task with id:" + id + " not found"));

                Optional<File> zipOptional = ZipUtil.zip(task.getId(), ".zip", Json.toJson(task), task.truePdfPages());
                if (zipOptional.isEmpty())
                    return internalServerError(ResponseHelper.createResponse("Failed to create zip"));

                return ok(zipOptional.get()).as("application/zip")
                        .withHeader("Content-Disposition", "attachment; filename=task_" + task.getId() +  ".zip");
            }).orElse(notFound(ResponseHelper.createResponse("Task with id:" + id + " not found")));
        }, ec.current());
    }

    @Security.Authenticated(JwtAuthorizationFilter.class)
    public CompletionStage<Result> importTask(Http.Request request) {
        return supplyAsync(() -> {
            String userId = request.attrs().get(Security.USERNAME);
            Http.MultipartFormData<TemporaryFile> multiBody = request.body().asMultipartFormData();

            if (userId == null) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            if (multiBody == null) {
                return badRequest(ResponseHelper.createResponse("Expecting Multipart data"));
            }
            Http.MultipartFormData.FilePart<TemporaryFile> zip = multiBody.getFile("file");
            if (zip == null) {
                return badRequest(ResponseHelper.createResponse("Multipart [file] field required"));
            }
            TemporaryFile tempZipFile = zip.getRef();
            if (!PdfImageUtil.isZip(tempZipFile)) {
                return badRequest(ResponseHelper.createResponse("ZIP file required"));
            }
            Pair<Optional<JsonNode>, Optional<List<byte[]>>> jsonImagesOptionalPair = ZipUtil.unzip(tempZipFile.path().toFile());

            if (jsonImagesOptionalPair.getValue0().isEmpty() && jsonImagesOptionalPair.getValue1().isEmpty()) {
                return internalServerError(ResponseHelper.createResponse("Failed to unpack zip"));
            }

            if (jsonImagesOptionalPair.getValue0().isEmpty()) {
                return badRequest(ResponseHelper.createResponse("No json inside ZIP file"));
            }

            JsonNode jsonTask = jsonImagesOptionalPair.getValue0().get();
            List<byte[]> images = jsonImagesOptionalPair.getValue1().get();

            String[] requiredFields = {"userId", "name", "description", "labels", "pdfName", "pdfPages", "createdAt"};
            Result jsonCheckResult = checkJsonFields(jsonTask, requiredFields);
            if (!jsonCheckResult.equals(correctJson)) {
                return jsonCheckResult;
            }
            if (!userId.equals(jsonTask.findValue("userId").asText())) {
                return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
            }
            boolean continu = (jsonTask.findValue("pdfName").asText() != null) == jsonTask.findValue("pdfPages").elements().hasNext() == !images.isEmpty();

            if (!continu) {
                return badRequest(ResponseHelper.createResponse("Inconsistent data about pdf pages"));
            }
            ((ObjectNode)jsonTask).remove("id");
            ((ObjectNode)jsonTask).remove("pdfPages");
            if (!images.isEmpty()) {
                List<ObjectId> taskDocPages = imageStore.createList(images, jsonTask.get("pdfName").asText());
                ((ObjectNode)jsonTask).set("pdfPages", Json.toJson(taskDocPages.stream().map(ObjectId::toHexString).collect(Collectors.toList())));
            }
            Optional<Task> taskOptional = taskStore.create(Json.fromJson(jsonTask, Task.class));

            return taskOptional.map(task -> {
                return created(Json.toJson(task));
            }).orElse(internalServerError(ResponseHelper.createResponse("Could not create data")));
        }, ec.current());
    }

}