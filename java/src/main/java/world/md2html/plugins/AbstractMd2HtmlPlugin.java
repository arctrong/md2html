package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import world.md2html.options.argfile.ArgFileParseException;

import static world.md2html.utils.JsonUtils.JsonValidationException;
import static world.md2html.utils.JsonUtils.validateJson;
import static world.md2html.utils.JsonUtils.validateJsonAgainstSchemaFromResource;

abstract public class AbstractMd2HtmlPlugin implements Md2HtmlPlugin {

    protected boolean dataAccepted = false;
    protected boolean initialized = false;

    protected void validateInputDataAgainstSchemaFromResource(JsonNode data,
            String schemaResourceLocation) throws ArgFileParseException {
        try {
            validateJsonAgainstSchemaFromResource(data, schemaResourceLocation);
        } catch (JsonValidationException e) {
            throwValidationException(e);
        }
    }

    protected void validateInputDataAgainstSchema(JsonNode data, JsonSchema schema)
            throws ArgFileParseException {
        try {
            validateJson(data, schema);
        } catch (JsonValidationException e) {
            throwValidationException(e);
        }
    }

    private void throwValidationException(JsonValidationException e) throws ArgFileParseException {
        throw new ArgFileParseException("Plugin '" + this.getClass().getSimpleName() +
                "' data error: " + e.getMessage());
    }

    protected void assureAcceptDataOnce() {
        if (dataAccepted) {
            throw new IllegalStateException("Trying to accept data again.");
        }
        dataAccepted = true;
    }

    protected void assureInitializeOnce() {
        if (initialized) {
            throw new IllegalStateException("Trying to initialize again.");
        }
        initialized = true;
    }

}
