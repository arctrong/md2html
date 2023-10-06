package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import world.md2html.options.model.Document;
import world.md2html.utils.JsonUtils;

import java.util.Collections;

import static world.md2html.utils.JsonUtils.NODE_FACTORY;
import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;
import static world.md2html.utils.JsonUtils.validateJson;

public class PluginUtils {

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private static final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/string_or_array_schema.json");

    public static ArrayNode listFromStringOrArray(Document document, String metadata) {
        if (metadata.startsWith("[")) {
            JsonNode metadataJsonNode;
            try {
                metadataJsonNode = OBJECT_MAPPER.readTree(metadata);
            } catch (JsonProcessingException e) {
                throw new PageMetadataHandler.PageMetadataException("Incorrect JSON. Class '" +
                        e.getClass().getSimpleName() + "', page '" + document.getInput()
                        + "', error: " + e.getMessage());
            }
            try {
                validateJson(metadataJsonNode, metadataSchema);
            } catch (JsonUtils.JsonValidationException e) {
                throw new PageMetadataHandler.PageMetadataException("Validation error. Class '" +
                        e.getClass().getSimpleName() + "', page '" + document.getInput() +
                        "', error: " + e.getMessage());
            }
            return (ArrayNode) metadataJsonNode;
        } else {
            return new ArrayNode(NODE_FACTORY,
                    Collections.singletonList(new TextNode(metadata)));
        }
    }

    public static ObjectNode mapFromStringOrObject(String metadata, String key) {
        return mapFromStringOrObject(metadata, key, null);
    }

    public static ObjectNode mapFromStringOrObject(String metadata,
                                                   String key, JsonSchema schema) {
        if (metadata.startsWith("{")) {
            JsonNode metadataJsonNode;
            try {
                metadataJsonNode = OBJECT_MAPPER.readTree(metadata);
            } catch (JsonProcessingException e) {
                throw new PageMetadataHandler.PageMetadataException("Incorrect JSON. Class '" +
                        e.getClass().getSimpleName() + "', error: " + e.getMessage());
            }
            if (schema != null) {
                try {
                    validateJson(metadataJsonNode, schema);
                } catch (JsonUtils.JsonValidationException e) {
                    throw new PageMetadataHandler.PageMetadataException("Validation error. Class '" +
                            e.getClass().getSimpleName() + "', error: " + e.getMessage());
                }
            }
            return (ObjectNode) metadataJsonNode;
        } else {
            return new ObjectNode(NODE_FACTORY,
                    Collections.singletonMap(key, new TextNode(metadata)));
        }
    }
}
