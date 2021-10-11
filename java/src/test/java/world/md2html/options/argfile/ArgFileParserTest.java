package world.md2html.options.argfile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import world.md2html.options.model.ArgFileOptions;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageVariablesPlugin;
import world.md2html.testutils.PluginTestUtils;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.testutils.PluginTestUtils.*;
import static world.md2html.testutils.PluginTestUtils.ANY_DOCUMENT;

public class ArgFileParserTest {

    @Test
    public void emptyFile_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("", null));
    }

    @Test
    public void rootElementIsNotObject_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("[1, 2]", null));
    }

    @Test
    public void defaultElementIsNotObject_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"default\": []}", null));
    }

    @Test
    public void noDefaultElement_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": []}", null);
        assertEquals(0, argFileOptions.getDocuments().size());
    }

    @Test
    public void allDefaultParameters_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", null);
        Document doc = argFileOptions.getDocuments().get(0);
        assertEquals("index.txt", doc.getInputLocation());
        assertEquals("index.html", doc.getOutputLocation());
        assertNull(doc.getTitle());
        // Template path depends on the environment and is not checked here.
        assertFalse(doc.isNoCss());
        assertEquals(0, doc.getLinkCss().size());
        assertEquals(1, doc.getIncludeCss().size());
        assertFalse(doc.isForce());
        assertFalse(doc.isVerbose());
    }

    @Test
    public void allParametersFromDefaultSection_PositiveScenario()
            throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"default\": {\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], \"include-css\": [\"include.css\"], " +
                        "\"force\": true, \"verbose\": true}, \"documents\": [{}]}", null);
        Document doc = argFileOptions.getDocuments().get(0);
        assertEquals("index.txt", doc.getInputLocation());
        assertEquals("index.html", doc.getOutputLocation());
        assertEquals("some title", doc.getTitle());
        assertEquals(Paths.get("path/templates/custom.html"), doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertIterableEquals(Arrays.asList("link1.css", "link2.css"), doc.getLinkCss());
        assertIterableEquals(Arrays.asList("link1.css", "link2.css"), doc.getLinkCss());
        assertIterableEquals(Collections.singletonList(Paths.get("include.css")),
                doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
    }

    @Test
    public void noDocumentsElement_NegativeScenario() {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"default\": {}}", null));
        assertTrue(e.getMessage().contains("documents"));
    }

    @Test
    public void documentsElementIsNotList_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"documents\": \"not a list\"}", null));
    }

    @Test
    public void emptyDocumentsElement_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": []}", null);
        assertEquals(0, argFileOptions.getDocuments().size());
    }

    @ParameterizedTest
    @CsvSource({"link-css", "include-css"})
    public void defaultElementNoCssWithCssDefinitions_NegativeScenario(String cssType) {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"default\": {\"no-css\": true, \"" + cssType +
                        "\": [\"some.css\"]}, \"documents\": []}", null));
        assertTrue(e.getMessage().contains("no-css"));
        assertTrue(e.getMessage().contains(cssType));
    }

    @Test
    public void minimalDocument_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", null);
        Document doc = argFileOptions.getDocuments().get(0);
        assertEquals("index.txt", doc.getInputLocation());
        assertTrue(doc.getOutputLocation().contains("index"));
    }

    @Test
    public void severalDocuments_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}, {\"input\": \"about.txt\"}], " +
                        "\"default\": {\"template\": \"common_template.html\"}}", null);
        Document doc = argFileOptions.getDocuments().get(0);
        assertEquals("index.txt", doc.getInputLocation());
        assertEquals(Paths.get("common_template.html"), doc.getTemplate());
        assertTrue(doc.getOutputLocation().contains("index"));
        doc = argFileOptions.getDocuments().get(1);
        assertEquals("about.txt", doc.getInputLocation());
        assertEquals(Paths.get("common_template.html"), doc.getTemplate());
        assertTrue(doc.getOutputLocation().contains("about"));
    }

    @Test
    public void fullDocument_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], " +
                        "\"add-link-css\": [\"add_link.css\"], " +
                        "\"include-css\": [\"include.css\"], " +
                        "\"add-include-css\": [\"add_include1.css\", \"add_include1.css\"], " +
                        "\"force\": true, \"verbose\": true}]}", null);
        Document doc = argFileOptions.getDocuments().get(0);
        assertEquals("index.txt", doc.getInputLocation());
        assertEquals("index.html", doc.getOutputLocation());
        assertEquals("some title", doc.getTitle());
        assertEquals(Paths.get("path/templates/custom.html"), doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertEquals("some title", doc.getTitle());
        assertIterableEquals(Arrays.asList("link1.css", "link2.css", "add_link.css"),
                doc.getLinkCss());
        assertIterableEquals(Arrays.asList(Paths.get("include.css"), Paths.get("add_include1.css"),
                Paths.get("add_include1.css")),
                doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
    }

    @Test
    public void documentsElementNoInputFile_NegativeScenario() {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"documents\": [{\"output\": \"index.html\"}]}", null));
        assertTrue(e.getMessage().toUpperCase().contains("INPUT"));
    }

    @ParameterizedTest
    @CsvSource({"link-css", "include-css", "add-link-css", "add-include-css"})
    public void documentNoCssWithCssDefinitions_NegativeScenario(String cssType) {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"documents\": [{\"no-css\": true, \"" +
                        cssType + "\": [\"some.css\"]}]}", null));
        assertTrue(e.getMessage().contains("no-css"));
        assertTrue(e.getMessage().contains(cssType));
    }

    @Test
    public void documentVerboseAndReportFlags_NegativeScenario() {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> ArgFileParser.parse("{\"documents\": [{\"output\": \"index.html\", " +
                        "\"verbose\": true, \"report\": true}]}", null));
        assertTrue(e.getMessage().contains("verbose"));
        assertTrue(e.getMessage().contains("report"));
    }

    @Test
    public void overridingWithCliArgs_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], " +
                        "\"add-link-css\": [\"add_link.css\"], " +
                        "\"include-css\": [\"include.css\"], " +
                        "\"add-include-css\": [\"add_include1.css\", \"add_include1.css\"], " +
                        "\"force\": false, \"verbose\": false}]}",
                new CliOptions(null, "cli_index.txt", "cli_index.html",
                        "cli_title", Paths.get("cli/custom.html"),
                        Arrays.asList(Paths.get("cli_include1.css"), Paths.get("cli_include2.css")),
                        Arrays.asList("cli_link1.css", "cli_link2.css"), false, true, true,
                        false, true));
        Document doc = argFileOptions.getDocuments().get(0);
        assertEquals("cli_index.txt", doc.getInputLocation());
        assertEquals("cli_index.html", doc.getOutputLocation());
        assertEquals("cli_title", doc.getTitle());
        assertEquals(Paths.get("cli/custom.html"), doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertIterableEquals(Arrays.asList("cli_link1.css", "cli_link2.css"), doc.getLinkCss());
        assertIterableEquals(Arrays.asList(Paths.get("cli_include1.css"),
                Paths.get("cli_include2.css")), doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
        SessionOptions options = argFileOptions.getOptions();
        assertTrue(options.isLegacyMode());
        assertTrue(options.isVerbose());
    }

    @Test
    public void defaultOptions_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", null);
        SessionOptions options = argFileOptions.getOptions();
        assertFalse(options.isLegacyMode());
        assertFalse(options.isVerbose());
    }

    @Test
    public void fullOptions_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"options\": {\"verbose\": true, \"legacy-mode\": true}, " +
                        "\"documents\": [{\"input\": \"index.txt\"}]}", null);
        SessionOptions options = argFileOptions.getOptions();
        assertTrue(options.isLegacyMode());
        assertTrue(options.isVerbose());
    }

    @Test
    public void legacyMode_inCommandLine_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"options\": {\"verbose\": true}, " +
                        "\"documents\": [{\"input\": \"index.txt\"}]}",
                new CliOptions(Paths.get("unknown_arg_file.json"), "input.txt", "output.html",
                        "title", Paths.get("unknown_template.html"), Collections.emptyList(),
                        Collections.emptyList(), true, false, false, false, true));
        assertTrue(argFileOptions.getOptions().isLegacyMode());
        PageVariablesPlugin plugin = (PageVariablesPlugin) findSinglePlugin(argFileOptions
                .getPlugins(), PageVariablesPlugin.class);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());
        String pageContent = "<!--METADATA {\"key\": \"value\"}-->";
        metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
    }

    @Test
    public void noPlugins_PositiveScenario() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", null);
        List<Md2HtmlPlugin> plugins = argFileOptions.getPlugins();
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void allPlugins_PositiveScenario() throws ArgFileParseException {
        // Adding minimum plugin data to make the plugins declare themselves activated.
        // The specific plugins behavior is going to be tested in separate tests.
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": " +
                "[{\"input\": \"index.txt\"}], \"plugins\": " +
                "{\"relative-paths\": {\"rel_path\": \"/doc\"}, " +
                "\"page-flows\": {\"sections\": [{\"link\": \"doc/about.html\", " +
                "\"title\": \"About\"}]}, " +
                "\"page-variables\":{\"v\": {}}, " +
                "\"variables\": {\"logo\": \"THE GREATEST SITE EVER!\"}}}", null);
        List<Md2HtmlPlugin> plugins = argFileOptions.getPlugins();
        assertEquals(4, plugins.size());
    }

}
