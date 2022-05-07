package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.model.ArgFileOptions;
import world.md2html.testutils.PluginTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static world.md2html.testutils.PluginTestUtils.ANY_DOCUMENT;

class VariablesPluginTest {

    private VariablesPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return (VariablesPlugin) PluginTestUtils.findSinglePlugin(plugins,
                VariablesPlugin.class);
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.readAndParse("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {}}", null);
        VariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void variables() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.readAndParse("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {\"variables\": " +
                "{\"var1\": \"val1\", \"_var2\": \"val2\", " +
                "\"strange\": \"Don't do it yourself! -\\u002D>\" }}}", null);
        VariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("val1", variables.get("var1"));
        assertEquals("val2", variables.get("_var2"));
        assertEquals("Don't do it yourself! -->", variables.get("strange"));
    }

}
