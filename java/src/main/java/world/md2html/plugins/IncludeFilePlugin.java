package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.SmartSubstringer;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static world.md2html.plugins.PluginUtils.mapFromStringOrObject;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;
import static world.md2html.utils.Utils.getCachedString;
import static world.md2html.utils.Utils.supplyWithFileExceptionAsUserError;

public class IncludeFilePlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private static class IncludeFileData {
        private String rootDir = "";
        private String trim = "all";
        private boolean recursive = true;
        private SmartSubstringer substringer;
    }

    private Map<String, IncludeFileData> data;
    private PageMetadataHandlersWrapper metadataHandlers;
    private JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/include_file_metadata_schema.json");

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/include_file_schema.json");

        Map<String, IncludeFileData> dataMap = new HashMap<>();
        for (JsonNode item : data) {
            ObjectNode itemNode = (ObjectNode) item;
            for (JsonNode jsonNode : itemNode.get("markers")) {
                String marker = jsonNode.asText().toUpperCase();
                if (dataMap.containsKey(marker)) {
                    throw new UserError("Marker duplication (case-insensitively): " + marker);
                }
                IncludeFileData includeFileData = new IncludeFileData();
                includeFileData.rootDir = itemNode.get("root-dir").asText();
                includeFileData.trim = itemNode.has("trim") ?
                        itemNode.get("trim").asText("all") : "all";
                includeFileData.recursive = itemNode.has("recursive") &&
                        itemNode.get("recursive").asBoolean();
                includeFileData.substringer = new SmartSubstringer(
                        itemNode.has("start-with") ? itemNode.get("start-with").asText("") : "",
                        itemNode.has("end-with") ? itemNode.get("end-with").asText("") : "",
                        itemNode.has("start-marker") ? itemNode.get("start-marker").asText("") : "",
                        itemNode.has("end-marker") ? itemNode.get("end-marker").asText("") : ""
                );
                dataMap.put(marker, includeFileData);
            }
            this.data = dataMap;
        }
    }

    @Override
    public boolean isBlank() {
        return data.isEmpty();
    }

    @Override
    public void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins,
                              PageMetadataHandlersWrapper metadataHandlers) {
        this.metadataHandlers = metadataHandlers;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.data.keySet().stream().map(marker ->
                new PageMetadataHandlerInfo(this, marker, false)).collect(Collectors.toList());
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
                                     String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException {

        IncludeFileData markerData = this.data.get(marker);
        SmartSubstringer substringer = markerData.substringer;

        Map<String, Object> metadataMap;
        try {
            //noinspection unchecked
            metadataMap = (Map<String, Object>)
                    JsonUtils.deJson(mapFromStringOrObject(metadata.trim(),
                    "file", this.metadataSchema));
        } catch (UserError e) {
            throw new UserError("Error in inclusion: " + e.getMessage() + ", page: '" +
                    document.getInput());
        }

        substringer = substringer.smartCopy(
                (String) metadataMap.get("start-with"),
                (String) metadataMap.get("end-with"),
                (String) metadataMap.get("start-marker"),
                (String) metadataMap.get("end-marker")
        );
        String filePath = ((String) metadataMap.get("file")).trim();
        Path includeFile = Paths.get(markerData.rootDir, filePath);
        String content = supplyWithFileExceptionAsUserError(
                () -> getCachedString(includeFile, Utils::readStringFromUtf8File),
                "Error processing page metadata block"
        );

        content = substringer.substring(content);

        String trim = (String) metadataMap.getOrDefault("trim", markerData.trim);
        if ("all".equals(trim)) {
            content = content.trim();
        } else if ("empty-lines".equals(trim)) {
            content = Utils.stripEmptyLines(content);
        }

        boolean recursive;
        if (metadataMap.containsKey("recursive")) {
            recursive = (Boolean) metadataMap.get("recursive");
        } else {
            recursive = markerData.recursive;
        }
        return recursive ?
                metadataHandlers.applyMetadataHandlers(content, document, visitedMarkers,
                        "INCLUDE_FILE_PLUGIN:" + includeFile) :
                content;
    }
}
