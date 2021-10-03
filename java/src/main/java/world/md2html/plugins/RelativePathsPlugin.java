package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.UserError;
import world.md2html.options.model.Document;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static world.md2html.utils.Utils.relativizeRelativePath;

public class RelativePathsPlugin implements Md2HtmlPlugin {

    private Map<String, String> data = null;

    @Override
    public boolean acceptData(JsonNode data) {
        try {
            JsonUtils.validateJsonAgainstSchemaFromResource(data, "plugins/relative_paths_schema.json");
        } catch (JsonUtils.JsonValidationException e) {
            throw new PluginDataUserError("Plugin '" + this.getClass().getSimpleName() +
                    "' data error: " + e.getMessage());
        }
        Map<String, String> pluginData = new HashMap<>();
        data.fields().forEachRemaining(entry -> pluginData.put(entry.getKey(),
                entry.getValue().asText()));
        this.data = pluginData;
        return !this.data.isEmpty();
    }

    @Override
    public Map<String, Object> variables(Document document) {
        Map<String, Object> variables = new HashMap<>();
        this.data.forEach((k, v) -> {
            try {
                variables.put(k, relativizeRelativePath(v, document.getOutputLocation()));
            } catch (Utils.ResourceLocationException e) {
                throw new UserError("Error recalculating relative path '" + k + "'='" + v +
                        "' for page '" + document.getOutputLocation() + "': " + e.getMessage());
            }
        });
        return variables;
    }
}
