package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;

import java.util.Map;

import static world.md2html.utils.JsonUtils.deJson;

public class VariablesPlugin extends AbstractMd2HtmlPlugin {

    private Map<String, Object> pageVariables;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        validateInputDataAgainstSchemaFromResource(data, "plugins/variables_schema.json");
        //noinspection unchecked
        this.pageVariables = (Map<String, Object>) deJson(data);
    }

    @Override
    public boolean isBlank() {
        return this.pageVariables.isEmpty();
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return this.pageVariables;
    }

}
