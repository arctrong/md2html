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
import java.util.Set;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.deJson;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;

public class PageVariablesPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private List<PageMetadataHandlerInfo> handlers = new ArrayList<>();
    private Map<String, Object> pageVariables;

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/page_variables_metadata_schema.json");

    public PageVariablesPlugin() {
        resetPageVariables();
    }

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        validateInputDataAgainstSchemaFromResource(data, "plugins/page_variables_schema.json");
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
    }

    @Override
    public boolean isBlank() {
        return this.handlers.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.handlers;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
                                     String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException {
        ObjectNode metadataNode;
        try {
            metadataNode = parseAndValidatePageVariableMetadata(metadata);
        } catch (ArgFileParseException e) {
            throw new PageMetadataException(e.getMessage());
        }
        //noinspection unchecked
        this.pageVariables.putAll((Map<String, Object>) deJson(metadataNode));
        return "";
    }

    private ObjectNode parseAndValidatePageVariableMetadata(String metadata)
            throws ArgFileParseException {
        ObjectNode metadataNode;
        try {
            metadataNode = (ObjectNode) OBJECT_MAPPER.readTree(metadata);
        } catch (JsonProcessingException e) {
            throw new PageMetadataException("Incorrect JSON in page metadata: " +
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        validateInputDataAgainstSchema(metadataNode, this.metadataSchema);
        return metadataNode;
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return this.pageVariables;
    }

    @Override
    public void newPage(Document document) {
        resetPageVariables();
    }

    private void resetPageVariables() {
        this.pageVariables = new HashMap<>();
    }

}
