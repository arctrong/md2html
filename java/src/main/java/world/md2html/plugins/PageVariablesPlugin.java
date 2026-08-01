package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.MetadataProcessingResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.deJson;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;

public class PageVariablesPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private List<PageMetadataHandlerInfo> handlers = new ArrayList<>();
    private Map<String, Map<String, Object>> variablesByInputFile = new HashMap<>();
    private Map<String, Object> variablesWithoutInputFile = new HashMap<>();

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/page_variables_metadata_schema.json");

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
    public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
            String metadata, String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException {
        ObjectNode metadataNode;
        try {
            metadataNode = parseAndValidatePageVariableMetadata(metadata);
        } catch (ArgFileParseException e) {
            throw new PageMetadataException(e.getMessage());
        }
        //noinspection unchecked
        Map<String, Object> metadataMap = (Map<String, Object>) deJson(metadataNode);
        this.variablesWithoutInputFile.putAll(metadataMap);
        String inputFile = documentInputFile(document);
        if (inputFile != null) {
            this.variablesByInputFile.computeIfAbsent(inputFile, k -> new HashMap<>())
                    .putAll(metadataMap);
        }
        return MetadataProcessingResult.immediate("");
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
        String inputFile = documentInputFile(document);
        if (inputFile != null) {
            return this.variablesByInputFile.getOrDefault(inputFile, Collections.emptyMap());
        }
        return this.variablesWithoutInputFile;
    }

    @Override
    public void newPage(Document document) {
        this.variablesWithoutInputFile = new HashMap<>();
        String inputFile = documentInputFile(document);
        if (inputFile != null) {
            this.variablesByInputFile.put(inputFile, new HashMap<>());
        }
    }

    private static String documentInputFile(Document document) {
        return document == null ? null : document.getInput();
    }
}
