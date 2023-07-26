package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static world.md2html.utils.Utils.getCachedString;

public class IncludeFilePlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private static class IncludeFileData {
        private String rootDir = "";
        private boolean trim = true;
    }

    private Map<String, IncludeFileData> data;

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
                IncludeFileData wrapCodeData = new IncludeFileData();
                wrapCodeData.rootDir = itemNode.get("root-dir").asText();
                wrapCodeData.trim = !itemNode.has("trim") || itemNode.get("trim").asBoolean(true);
                dataMap.put(marker, wrapCodeData);
            }
            this.data = dataMap;
        }
    }

    @Override
    public boolean isBlank() {
        return data.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.data.keySet().stream().map(marker ->
                new PageMetadataHandlerInfo(this, marker, false)).collect(Collectors.toList());
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String filePath,
                                     String metadataSection) throws PageMetadataException {
        marker = marker.toUpperCase();
        IncludeFileData markerData = this.data.get(marker);
        filePath = filePath.trim();
        Path includeFile = Paths.get(markerData.rootDir, filePath);
        String content = getCachedString(includeFile, Utils::readStringFromUtf8File);
        if (markerData.trim) {
            content = content.trim();
        }
        return  content;
    }
}
