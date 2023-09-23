package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.JsonSchema;
import world.md2html.options.model.Document;
import world.md2html.utils.JsonUtils;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;
import static world.md2html.utils.JsonUtils.validateJson;

public class PluginUtils {

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private static final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/string_or_array_schema.json");

    public static ArrayNode listFromStringOrArray(Document document, String metadata) {
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
                    e.getClass().getSimpleName() + "', page '" + document.getInput()
                    + "', error: " + e.getMessage());
        }
        return (ArrayNode) metadataJsonNode;
    }
}
