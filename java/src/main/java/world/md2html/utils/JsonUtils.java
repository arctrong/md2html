package world.md2html.utils;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JsonUtils {

    public static String jsonObjectStringField(ObjectNode objectNode, String fieldName) {
        return Optional.ofNullable(objectNode.get(fieldName)).map(JsonNode::asText).orElse(null);
    }

    public static Boolean jsonObjectBooleanField(ObjectNode objectNode, String fieldName) {
        return Optional.ofNullable(objectNode.get(fieldName)).map(JsonNode::asBoolean).orElse(null);
    }

    public static Path jsonObjectPathField(ObjectNode objectNode, String fieldName) {
        return Optional.ofNullable(objectNode.get(fieldName))
                .map(JsonNode::asText).map(Paths::get).orElse(null);
    }

    public static List<String> jsonArrayToStringList(ArrayNode array) {
        List<String> result = new ArrayList<>();
        for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
            result.add(it.next().asText());
        }
        return result;
    }

    public static class JsonValidationException extends Exception {
        public JsonValidationException(String message) {
            super(message);
        }
    }

    private JsonUtils() {}

    public static String formatJsonProcessingException(JsonProcessingException e) {
        JsonLocation location = e.getLocation();
        return e.getOriginalMessage() + ". Line: " + location.getLineNr() + ", column: " +
                location.getColumnNr() + ", offset: " + location.getCharOffset();
    }

    public static void validateJsonAgainstSchemaFromResource(JsonNode node,
            String schemaResourceLocation) throws JsonValidationException {

        String argumentFileSchema;
        try {
            argumentFileSchema = Utils.readStringFromResource(schemaResourceLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema(argumentFileSchema);

        Set<ValidationMessage> errors = schema.validate(node);
        if (!errors.isEmpty()) {
            throw new JsonValidationException("JSON document validation errors:" +
                    errors.stream().map(e -> "\n" + e.getMessage()).collect(Collectors.joining()));
        }
    }
}
