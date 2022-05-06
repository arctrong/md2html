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

import static world.md2html.utils.JsonUtils.*;

public class PageVariablesPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private List<PageMetadataHandlerInfo> handlers = new ArrayList<>();
    private Map<String, Object> pageVariables;

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/page_variables_metadata_schema.json");

    public PageVariablesPlugin() {
        resetPageVariable();
    }

    @Override
    public boolean acceptData(JsonNode data) throws ArgFileParseException {
        doStandardJsonInputDataValidation(data, "plugins/page_variables_schema.json");
        List<PageMetadataHandlerInfo> handlers = new ArrayList<>();
        data.fields().forEachRemaining(entry -> {
            JsonNode valueNode = entry.getValue().get("only-at-page-start");
            handlers.add(new PageMetadataHandlerInfo(this, entry.getKey().toUpperCase(),
                    valueNode != null && valueNode.asBoolean()));
        });
        this.handlers = handlers;
        if (handlers.isEmpty()) {
            this.handlers.add(new PageMetadataHandlerInfo(this, "VARIABLES", true));
        }
        return !this.handlers.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.handlers;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
            String metadataSection) throws PageMetadataException {

        ObjectNode metadataNode = parseAndValidatePageVariableMetadata(metadata);
        //noinspection unchecked
        this.pageVariables.putAll((Map<String, Object>) deJson(metadataNode));
        return "";
    }

    private ObjectNode parseAndValidatePageVariableMetadata(String metadata) {
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
        return metadataNode;
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return this.pageVariables;
    }

    @Override
    public void newPage(Document document) {
        resetPageVariable();
    }

    private void resetPageVariable() {
        this.pageVariables = new HashMap<>();
    }

}
