package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.PluginTestUtils.ANY_DOCUMENT;

class PageVariablesPluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private PageVariablesPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findFirstElementOfType(plugins, PageVariablesPlugin.class);
    }

    private ArgFile parsePluginData(String pluginData) throws ArgFileParseException {
        return parseArgumentFile("{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-variables\": " + pluginData + "}}",
                DUMMY_CLI_OPTIONS);
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"variables\": {}}}", DUMMY_CLI_OPTIONS);
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void activated_withDefaultMarker() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-variables\": {}}}", DUMMY_CLI_OPTIONS);
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNotNull(plugin);
    }

    @Test
    public void singleBlock_complexTest() throws ArgFileParseException {
        ArgFile argFile = parsePluginData("{\"METADATA\": { }}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageContent = "<!--METADATA {\"title\": \"About\"}-->other content";
        plugin.newPage(null);
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("About", variables.get("title"));
        assertEquals("other content", result);

        pageContent = "  \r\n \t \n   <!--METADATA{\"title\":\"About1\" } -->";
        plugin.newPage(null);
        result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("About1", variables.get("title"));
        assertEquals("  \r\n \t \n   ", result);

        pageContent = "  \r\n \t \n  no metadata blocks  ";
        result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("About1", variables.get("title")); // that"s because the plugin was not reset
        assertEquals("  \r\n \t \n  no metadata blocks  ", result);
        plugin.newPage(null); // reset
        result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        variables = plugin.variables(ANY_DOCUMENT);
        assertNull(variables.get("title"));
        assertEquals("  \r\n \t \n  no metadata blocks  ", result);
    }

    @Test
    public void caseInsensitive() throws ArgFileParseException {
        ArgFile argFile = parsePluginData("{\"VariaBLEs\": {}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageContent = "<!--variables{ \"key\":\"value\" }-->other content";
        plugin.newPage(null);
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
        assertEquals("other content", result);
    }

    @Test
    public void inPageMiddle() throws ArgFileParseException {
        ArgFile argFile = parsePluginData(
                "{\"metadata\": {\"only-at-page-start\": false}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageContent = "start text <!--metadata{ \"logo\":\"COOL!\" }-->other content";
        plugin.newPage(null);
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("COOL!", variables.get("logo"));
        assertEquals("start text other content", result);
    }

    @Test
    public void multiline() throws ArgFileParseException {
        ArgFile argFile = parsePluginData(
                "{\"variables\": {\"only-at-page-start\": false}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageContent = "start text <!--variables\n{\"key\": \"value\"}\r\n-->\n other content";
        plugin.newPage(null);
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
        assertEquals("start text \n other content", result);
    }

    @Test
    public void wrongMarker() throws ArgFileParseException {
        ArgFile argFile = parsePluginData(
                "{\"variables\": {\"only-at-page-start\": false}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());
        String pageContent = "start text<!--metadata{\"key\":\"value\"}-->";
        plugin.newPage(null);
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertTrue(variables.isEmpty());
        assertEquals("start text<!--metadata{\"key\":\"value\"}-->", result);
    }

    @Test
    public void severalBlocks() throws ArgFileParseException {
        ArgFile argFile = parsePluginData(
                "{\"variables1\": {\"only-at-page-start\": false}, \"metadata1\": {}}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageContent = "    <!--metadata1{\"key\": \"value\"}--> other " +
                "text <!--variables1{\"question\": \"answer\"} --> some more text";
        plugin.newPage(null);
        String result = metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
        assertEquals("answer", variables.get("question"));
        assertEquals("     other text  some more text", result);
    }

    /**
     * Regression: {@code variables(document)} after phase 1 must not leak across documents
     * (deferred build).
     */
    @Test
    public void variablesPerDocumentAfterMultiplePages() throws ArgFileParseException {
        ArgFile argFile = parsePluginData("{}");
        PageVariablesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document docReferencer = Document.builder()
                .input("referencer.txt")
                .output("referencer.html")
                .title("Referencing page")
                .build();
        Document docDefinition = Document.builder()
                .input("definition.txt")
                .output("definition.html")
                .title("Definition page")
                .build();
        Document docPlain = Document.builder()
                .input("plain.txt")
                .output("plain.html")
                .title("Plain page")
                .build();

        plugin.newPage(docReferencer);
        metadataHandlers.applyMetadataHandlers(
                "<!--VARIABLES {\"title\": \"Referencing page\"}-->\n# Referencer",
                docReferencer);

        plugin.newPage(docDefinition);
        metadataHandlers.applyMetadataHandlers(
                "<!--VARIABLES {\"title\": \"Definition page\"}-->\n# Definition",
                docDefinition);

        plugin.newPage(docPlain);
        metadataHandlers.applyMetadataHandlers(
                "<!--VARIABLES {\"title\": \"Plain page\"}-->\n# Plain",
                docPlain);

        Map<String, Object> referencerVars = plugin.variables(docReferencer);
        Map<String, Object> definitionVars = plugin.variables(docDefinition);
        Map<String, Object> plainVars = plugin.variables(docPlain);

        assertEquals("Referencing page", referencerVars.get("title"));
        assertEquals("Definition page", definitionVars.get("title"));
        assertEquals("Plain page", plainVars.get("title"));

        Map<String, Object> substitutions = new HashMap<>();
        substitutions.put("title", docReferencer.getTitle());
        substitutions.putAll(plugin.variables(docReferencer));
        assertEquals("Referencing page", substitutions.get("title"));
    }
}
