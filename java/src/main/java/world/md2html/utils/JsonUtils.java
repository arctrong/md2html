package world.md2html.utils;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonUtils {

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
            throw new JsonValidationException("JASON document validation errors:" +
                    errors.stream().map(e -> "\n" + e.getMessage()).collect(Collectors.joining()));
        }
    }
}
