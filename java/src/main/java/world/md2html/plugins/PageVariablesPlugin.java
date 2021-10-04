package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static world.md2html.utils.JsonUtils.MAPPER;
import static world.md2html.utils.JsonUtils.*;
import static world.md2html.utils.JsonUtils.deJson;

public class PageVariablesPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private List<PageMetadataHandlerInfo> handlers;
    private Map<String, Object> pageVariables = new HashMap<>();

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/page_variables_metadata_schema.json");

    @Override
    public boolean acceptData(JsonNode data) throws ArgFileParseException {
        doStandardJsonInputDataValidation(data, "plugins/page_variables_schema.json");
        List<PageMetadataHandlerInfo> handlers = new ArrayList<>();
        data.fields().forEachRemaining(entry -> {
            JsonNode valueNode = entry.getValue().get("only-at-page-start");
            handlers.add(new PageMetadataHandlerInfo(this, entry.getKey(),
                    valueNode != null && valueNode.asBoolean()));
        });
        this.handlers = handlers;
        return !this.handlers.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.handlers;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
            String metadataSection) throws PageMetadataException {

        ObjectNode metadataNode;
        try {
            metadataNode = (ObjectNode) MAPPER.readTree(metadata);
        } catch (JsonProcessingException e) {
            throw new PageMetadataException("Incorrect JSON in page metadata: " +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        try {
            validateJson(metadataNode, this.metadataSchema);
        } catch (JsonValidationException e) {
            throw new PageMetadataException("Error validating page metadata: " +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        //noinspection unchecked
        this.pageVariables.putAll((Map<String, Object>) deJson(metadataNode));
        return "";
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return this.pageVariables;
    }

    @Override
    public void newPage() {
        this.pageVariables = new HashMap<>();
    }

}
