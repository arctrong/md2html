package world.md2html.utils;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final JsonNodeFactory NODE_FACTORY = OBJECT_MAPPER.getNodeFactory();
    public static final ObjectMapper OBJECT_MAPPER_FOR_BUILDERS = new ObjectMapper();

    static {
        OBJECT_MAPPER_FOR_BUILDERS.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
                if (ac.hasAnnotation(JsonPOJOBuilder.class)) {
                    return super.findPOJOBuilderConfig(ac);
                }
                return new JsonPOJOBuilder.Value("build", "");
            }
        });
    }

    public static JsonNode objectNodeSetDefault(ObjectNode node, String propertyName,
            JsonNode value) {
        JsonNode result = node.get(propertyName);
        if (result == null) {
            result = value;
            node.set(propertyName, result);
        }
        return result;
    }

    /**
     * This function converts the given `JsonNode` into native Java representation. To reflect
     * JSON types and structures it uses certain Java types that are suitable in context of this
     * program. May be not suitable in other contexts.
     */
    public static Object deJson(JsonNode value) {
        if (value == null) {
            return null;
        }
        switch (value.getNodeType()) {
            case ARRAY:
                List<Object> list = new ArrayList<>();
                value.forEach(item -> list.add(deJson(item)));
                return list;
            case NULL:
            case BINARY:
            case MISSING:
            case POJO:
                return null;
            case BOOLEAN:
                return value.asBoolean();
            case NUMBER:
                if (value.canConvertToExactIntegral()) {
                    return value.asInt();
                } else {
                    return value.asDouble();
                }
            case OBJECT:
                Map<String, Object> map = new LinkedHashMap<>();
                value.fields().forEachRemaining(entry -> map.put(entry.getKey(),
                        deJson(entry.getValue())));
                return map;
            case STRING:
                return value.asText();
        }
        return null;
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
        JsonSchema schema = loadJsonSchemaFromResource(schemaResourceLocation);
        validateJson(node, schema);
    }

    public static void validateJson(JsonNode node, JsonSchema schema)
            throws JsonValidationException {
        Set<ValidationMessage> errors = schema.validate(node);
        if (!errors.isEmpty()) {
            throw new JsonValidationException("JSON document validation errors:" +
                    errors.stream().map(e -> "\n" + e.getMessage()).collect(Collectors.joining()));
        }
    }

    public static JsonSchema loadJsonSchemaFromResource(String schemaResourceLocation) {
        String argumentFileSchema;
        try {
            argumentFileSchema = Utils.readStringFromResource(schemaResourceLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        return factory.getSchema(argumentFileSchema);
    }

}
