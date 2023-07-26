package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.testutils.PluginTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.PluginTestUtils.ANY_DOCUMENT;

class VariablesPluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private VariablesPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findFirstElementOfType(plugins, VariablesPlugin.class);
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {}}", DUMMY_CLI_OPTIONS);
        VariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void variables() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"whatever.md\"}], " +
                        "\"plugins\": {" +
                        "\"variables\": {\"var1\": \"val1\", \"_var2\": \"val2\", " +
                        "    \"strange\": \"Don't do it yourself! -\\u002D>\" }" +
                        "}}", DUMMY_CLI_OPTIONS);
        VariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("val1", variables.get("var1"));
        assertEquals("val2", variables.get("_var2"));
        assertEquals("Don't do it yourself! -->", variables.get("strange"));
    }

}
