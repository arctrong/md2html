package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.utils.JsonUtils;

abstract public class AbstractMd2HtmlPlugin implements Md2HtmlPlugin {

    protected void doStandardJsonInputDataValidation(JsonNode data, String schemaResourceLocation)
            throws ArgFileParseException {
        try {
            JsonUtils.validateJsonAgainstSchemaFromResource(data, schemaResourceLocation);
        } catch (JsonUtils.JsonValidationException e) {
            throw new ArgFileParseException("Plugin '" + this.getClass().getSimpleName() +
                    "' data error: " + e.getMessage());
        }
    }

}
