package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.model.ArgFileOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.testutils.PluginTestUtils.ANY_DOCUMENT;

class PageVariablesPluginTest {

    private PageVariablesPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return (PageVariablesPlugin) PluginTestUtils.findSinglePlugin(plugins,
                PageVariablesPlugin.class);
    }

    private ArgFileOptions parsePluginData(String pluginData) throws ArgFileParseException {
        return ArgFileParser.parse("{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-variables\": " + pluginData + "}}", null);
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"variables\": {}}}", null);
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void activated_withDefaultMarker() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-variables\": {}}}", null);
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        assertNotNull(plugin);
    }

    @Test
    public void singleBlock_complexTest() throws ArgFileParseException {
        ArgFileOptions argFileOptions = parsePluginData("{\"METADATA\": { }}");
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());

        String pageContent = "<!--METADATA {\"title\": \"About\"}-->other content";
        plugin.newPage();
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("About", variables.get("title"));
        assertEquals("other content", result);

        pageContent = "  \r\n \t \n   <!--METADATA{\"title\":\"About1\" } -->";
        plugin.newPage();
        result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("About1", variables.get("title"));
        assertEquals("  \r\n \t \n   ", result);

        pageContent = "  \r\n \t \n  no metadata blocks  ";
        result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("About1", variables.get("title")); // that"s because the plugin was not reset
        assertEquals("  \r\n \t \n  no metadata blocks  ", result);
        plugin.newPage(); // reset
        result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        variables = plugin.variables(ANY_DOCUMENT);
        assertNull(variables.get("title"));
        assertEquals("  \r\n \t \n  no metadata blocks  ", result);
    }

    @Test
    public void caseInsensitive() throws ArgFileParseException {
        ArgFileOptions argFileOptions = parsePluginData("{\"VariaBLEs\": {}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());

        String pageContent = "<!--variables{ \"key\":\"value\" }-->other content";
        plugin.newPage();
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
        assertEquals("other content", result);
    }

    @Test
    public void inPageMiddle() throws ArgFileParseException {
        ArgFileOptions argFileOptions = parsePluginData(
                "{\"metadata\": {\"only-at-page-start\": false}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());

        String pageContent = "start text <!--metadata{ \"logo\":\"COOL!\" }-->other content";
        plugin.newPage();
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("COOL!", variables.get("logo"));
        assertEquals("start text other content", result);
    }

    @Test
    public void multiline() throws ArgFileParseException {
        ArgFileOptions argFileOptions = parsePluginData(
                "{\"variables\": {\"only-at-page-start\": false}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());

        String pageContent = "start text <!--variables\n{\"key\": \"value\"}\r\n-->\n other content";
        plugin.newPage();
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
        assertEquals("start text \n other content", result);
    }

    @Test
    public void wrongMarker() throws ArgFileParseException {
        ArgFileOptions argFileOptions = parsePluginData(
                "{\"variables\": {\"only-at-page-start\": false}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());
        String pageContent = "start text<!--metadata{\"key\":\"value\"}-->";
        plugin.newPage();
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertTrue(variables.isEmpty());
        assertEquals("start text<!--metadata{\"key\":\"value\"}-->", result);
    }

    @Test
    public void severalBlocks() throws ArgFileParseException {
        ArgFileOptions argFileOptions = parsePluginData(
                "{\"variables1\": {\"only-at-page-start\": false}, \"metadata1\": {}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());

        String pageContent = "    <!--metadata1{\"key\": \"value\"}--> other " +
                "text <!--variables1{\"question\": \"answer\"} --> some more text";
        plugin.newPage();
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
        assertEquals("answer", variables.get("question"));
        assertEquals("     other text  some more text", result);
    }

}
