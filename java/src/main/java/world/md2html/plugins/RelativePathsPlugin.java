package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.UserError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static world.md2html.utils.Utils.relativizeRelativePath;

public class RelativePathsPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private Map<String, String> paths;
    private List<String> markers;
    private List<PageMetadataHandlerInfo> handlers;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        validateInputDataAgainstSchemaFromResource(data, "plugins/relative_paths_schema.json");

        JsonNode pathsNode;
        JsonNode markers = data.get("markers");
        if (markers instanceof ArrayNode) {
            List<String> markerList = new ArrayList<>();
            markers.forEach(node -> markerList.add(node.asText()));
            this.markers = markerList;
            pathsNode = data.get("paths");
        } else {
            this.markers = Collections.emptyList();
            pathsNode = data;
        }

        Map<String, String> pluginData = new HashMap<>();
        pathsNode.fields().forEachRemaining(entry -> pluginData.put(entry.getKey(),
                entry.getValue().asText()));
        this.paths = pluginData;

        this.handlers = this.markers.stream()
                .map(m -> new PageMetadataHandlerInfo(this, m, false))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBlank() {
        return this.paths.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.handlers;
    }

    @Override
    public Map<String, Object> variables(Document document) {
        Map<String, Object> variables = new HashMap<>();
        this.paths.forEach((k, v) -> {
            try {
                variables.put(k, relativizeRelativePath(v, document.getOutput()));
            } catch (CheckedIllegalArgumentException e) {
                throw new UserError("Error recalculating relative path '" + k + "'='" + v +
                        "' for page '" + document.getOutput() + "': " + e.getMessage());
            }
        });
        return variables;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
            String metadataSection) throws PageMetadataException {

        String path = this.paths.get(metadata.trim());
        if (path == null) {
            return metadataSection;
        } else {
            try {
                return relativizeRelativePath(path, document.getOutput());
            } catch (CheckedIllegalArgumentException e) {
                throw new PageMetadataException("Plugin '" + this.getClass().getSimpleName() +
                        "': Cannot relativize '" + path + "' against '" +
                        document.getOutput() + "': " + e.getMessage());
            }
        }
    }
}
