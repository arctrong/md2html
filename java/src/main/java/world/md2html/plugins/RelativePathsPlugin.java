package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.UserError;

import java.util.HashMap;
import java.util.Map;

import static world.md2html.utils.Utils.relativizeRelativePath;

public class RelativePathsPlugin extends AbstractMd2HtmlPlugin {

    // TODO Consider extending this plugin for substitution of the paths inside documents.
    //  Like what `PageLinksPlugin` does. Probably need to add parameter `marker` the the
    //  `path` definition.

    private Map<String, String> data = null;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        validateInputDataAgainstSchemaFromResource(data, "plugins/relative_paths_schema.json");
        Map<String, String> pluginData = new HashMap<>();
        data.fields().forEachRemaining(entry -> pluginData.put(entry.getKey(),
                entry.getValue().asText()));
        this.data = pluginData;
    }

    @Override
    public boolean isBlank() {
        return this.data.isEmpty();
    }

    @Override
    public Map<String, Object> variables(Document document) {
        Map<String, Object> variables = new HashMap<>();
        this.data.forEach((k, v) -> {
            try {
                variables.put(k, relativizeRelativePath(v, document.getOutput()));
            } catch (CheckedIllegalArgumentException e) {
                throw new UserError("Error recalculating relative path '" + k + "'='" + v +
                        "' for page '" + document.getOutput() + "': " + e.getMessage());
            }
        });
        return variables;
    }

}
