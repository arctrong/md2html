package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.UserError;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;

import java.util.HashMap;
import java.util.Map;

import static world.md2html.utils.Utils.relativizeRelativePath;

public class RelativePathsPlugin extends AbstractMd2HtmlPlugin {

    private Map<String, String> data = null;

    @Override
    public boolean acceptData(JsonNode data) throws ArgFileParseException {
        doStandardJsonInputDataValidation(data, "plugins/relative_paths_schema.json");
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
            } catch (CheckedIllegalArgumentException e) {
                throw new UserError("Error recalculating relative path '" + k + "'='" + v +
                        "' for page '" + document.getOutputLocation() + "': " + e.getMessage());
            }
        });
        return variables;
    }
}
