package utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

public class ResponseHelper {
    public static ObjectNode createResponse(String message){
        return Json.newObject().put("message", message);
    }
}
