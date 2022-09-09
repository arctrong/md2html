package world.md2html.options.argfile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import world.md2html.options.cli.CliParser;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageVariablesPlugin;
import world.md2html.utils.UserError;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.PluginTestUtils.ANY_DOCUMENT;
import static world.md2html.testutils.PluginTestUtils.findSinglePlugin;

public class ArgFileParsingHelperTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();
    private static final CliParser cliParser = new CliParser("whatever");

    @Test
    public void emptyFile_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> parseArgumentFile("", DUMMY_CLI_OPTIONS));
    }

    @Test
    public void rootElementIsNotObject_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> parseArgumentFile("[1, 2]", DUMMY_CLI_OPTIONS));
    }

    @Test
    public void defaultElementIsNotObject_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> parseArgumentFile("{\"default\": []}", DUMMY_CLI_OPTIONS));
    }

    @Test
    public void noDefaultElement_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile("{\"documents\": []}", DUMMY_CLI_OPTIONS);
        assertEquals(0, argFile.getDocuments().size());
    }

    @Test
    public void allDefaultParameters_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        assertEquals("index.txt", doc.getInput());
        assertEquals("index.html", doc.getOutput());
        assertNull(doc.getTitle());
        // Template path depends on the environment and is not checked here.
        assertFalse(doc.isNoCss());
        assertEquals(0, doc.getLinkCss().size());
        assertEquals(1, doc.getIncludeCss().size());
        assertFalse(doc.isForce());
        assertFalse(doc.isVerbose());
    }

    @Test
    public void allParametersFromDefaultSection_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"default\": {\"input-root\": \"doc_src\", \"output-root\": \"doc\", " +
                        "\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], \"include-css\": [\"include.css\"], " +
                        "\"force\": true, \"verbose\": true}, \"documents\": [{}]}",
                DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        assertEquals("doc_src/index.txt", doc.getInput());
        assertEquals("doc/index.html", doc.getOutput());
        assertEquals("some title", doc.getTitle());
        assertEquals("path/templates/custom.html", doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertIterableEquals(Arrays.asList("link1.css", "link2.css"), doc.getLinkCss());
        assertIterableEquals(Arrays.asList("link1.css", "link2.css"), doc.getLinkCss());
        assertIterableEquals(Collections.singletonList("include.css"),
                doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
    }

    @Test
    public void noDocumentsElement_NegativeScenario() {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> parseArgumentFile("{\"default\": {}}", DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().contains("documents"));
    }

    @Test
    public void documentsElementIsNotList_NegativeScenario() {
        assertThrows(ArgFileParseException.class,
                () -> parseArgumentFile("{\"documents\": \"not a list\"}", DUMMY_CLI_OPTIONS));
    }

    @Test
    public void emptyDocumentsElement_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile("{\"documents\": []}", DUMMY_CLI_OPTIONS);
        assertEquals(0, argFile.getDocuments().size());
    }

    @ParameterizedTest
    @CsvSource({"link-css", "include-css"})
    public void defaultElementNoCssWithCssDefinitions_NegativeScenario(String cssType) {
        UserError e = assertThrows(UserError.class,
                () -> parseArgumentFile("{\"default\": {\"no-css\": true, \"" + cssType +
                        "\": [\"some.css\"]}, \"documents\": []}", DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().contains("no-css"));
        assertTrue(e.getMessage().contains(cssType));
    }

    @Test
    public void minimalDocument_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        assertEquals("index.txt", doc.getInput());
        assertTrue(doc.getOutput().contains("index"));
    }

    @Test
    public void severalDocuments_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}, {\"input\": \"about.txt\"}], " +
                        "\"default\": {\"template\": \"common_template.html\"}}",
                DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        assertEquals("index.txt", doc.getInput());
        assertEquals("common_template.html", doc.getTemplate());
        assertTrue(doc.getOutput().contains("index"));
        doc = argFile.getDocuments().get(1);
        assertEquals("about.txt", doc.getInput());
        assertEquals("common_template.html", doc.getTemplate());
        assertTrue(doc.getOutput().contains("about"));
    }

    @Test
    public void fullDocument_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input-root\": \"doc_src\", \"output-root\": \"doc\", " +
                        "\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], " +
                        "\"add-link-css\": [\"add_link.css\"], " +
                        "\"include-css\": [\"include.css\"], " +
                        "\"add-include-css\": [\"add_include1.css\", \"add_include1.css\"], " +
                        "\"force\": true, \"verbose\": true}]}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        assertEquals("doc_src/index.txt", doc.getInput());
        assertEquals("doc/index.html", doc.getOutput());
        assertEquals("some title", doc.getTitle());
        assertEquals("path/templates/custom.html", doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertEquals("some title", doc.getTitle());
        assertIterableEquals(Arrays.asList("link1.css", "link2.css", "add_link.css"),
                doc.getLinkCss());
        assertIterableEquals(Arrays.asList("include.css", "add_include1.css", "add_include1.css"),
                doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
    }

    @Test
    public void documentsElementNoInputFile_NegativeScenario() {
        UserError e = assertThrows(UserError.class,
                () -> parseArgumentFile("{\"documents\": [{\"output\": \"index.html\"}]}",
                        DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().toUpperCase().contains("INPUT"));
    }

    @ParameterizedTest
    @CsvSource({"link-css", "include-css", "add-link-css", "add-include-css"})
    public void documentNoCssWithCssDefinitions_NegativeScenario(String cssType) {
        UserError e = assertThrows(UserError.class,
                () -> parseArgumentFile("{\"documents\": [{\"input\": \"whatever.txt\"," +
                        "\"no-css\": true, \"" + cssType + "\": [\"some.css\"]}]}",
                        DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().contains("no-css"));
        assertTrue(e.getMessage().contains(cssType));
    }

    @Test
    public void documentVerboseAndReportFlags_NegativeScenario() {
        UserError e = assertThrows(UserError.class,
                () -> parseArgumentFile("{\"documents\": [{\"input\": \"index.txt\", " +
                        "\"verbose\": true, \"report\": true}]}", DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().contains("verbose"));
        assertTrue(e.getMessage().contains("report"));
    }

    @Test
    public void overridingWithCliArgs_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input-root\": \"doc_src\", \"output-root\": \"doc\", " +
                        "\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], " +
                        "\"add-link-css\": [\"add_link.css\"], " +
                        "\"include-css\": [\"include.css\"], " +
                        "\"add-include-css\": [\"add_include1.css\", \"add_include1.css\"], " +
                        "\"force\": false, \"verbose\": false}]}",
                cliParser.parse(new String[]{"--input-root", "cli_doc_src",
                        "--output-root", "cli_doc", "-i", "cli_index.txt", "-o", "cli_index.html",
                        "-t", "cli_title", "--template", "cli/custom.html",
                        "--include-css", "cli_include1.css", "--include-css", "cli_include2.css",
                        "--link-css", "cli_link1.css", "--link-css", "cli_link2.css",
                        "-fv", "--legacy-mode"})
        );
        Document doc = argFile.getDocuments().get(0);
        assertEquals("cli_doc_src/cli_index.txt", doc.getInput());
        assertEquals("cli_doc/cli_index.html", doc.getOutput());
        assertEquals("cli_title", doc.getTitle());
        assertEquals("cli/custom.html", doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertIterableEquals(Arrays.asList("cli_link1.css", "cli_link2.css"), doc.getLinkCss());
        assertIterableEquals(Arrays.asList("cli_include1.css", "cli_include2.css"),
                doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
        SessionOptions options = argFile.getOptions();
        assertTrue(options.isLegacyMode());
        assertTrue(options.isVerbose());
    }

    @Test
    public void defaultOptions_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", DUMMY_CLI_OPTIONS);
        SessionOptions options = argFile.getOptions();
        assertFalse(options.isLegacyMode());
        assertFalse(options.isVerbose());
    }

    @Test
    public void fullOptions_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"options\": {\"verbose\": true, \"legacy-mode\": true}, " +
                        "\"documents\": [{\"input\": \"index.txt\"}]}", DUMMY_CLI_OPTIONS)
                ;
        SessionOptions options = argFile.getOptions();
        assertTrue(options.isLegacyMode());
        assertTrue(options.isVerbose());
    }

    @Test
    public void legacyMode_inCommandLine_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile(
                "{\"options\": {\"verbose\": true}, " +
                        "\"documents\": [{\"input\": \"index.txt\"}]}",
                cliParser.parse(new String[]{"--argument-file", "unknown_arg_file.json",
                        "-i", "input.txt", "-o", "output.html",
                        "-t", "title", "--template", "unknown_template.html",
                        "--include-css", "cli_include1.css", "--include-css", "cli_include2.css",
                        "--link-css", "cli_link1.css", "--link-css", "cli_link2.css",
                        "-fv", "--legacy-mode"}));
        List<Md2HtmlPlugin> plugins = argFile.getPlugins();
        assertTrue(argFile.getOptions().isLegacyMode());
        PageVariablesPlugin plugin = findSinglePlugin(plugins, PageVariablesPlugin.class);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(plugins);
        String pageContent = "<!--METADATA {\"key\": \"value\"}-->";
        metadataHandlers.applyMetadataHandlers(pageContent, ANY_DOCUMENT);
        Map<String, Object> variables = plugin.variables(ANY_DOCUMENT);
        assertEquals("value", variables.get("key"));
    }

    @Test
    public void noPlugins_PositiveScenario() throws Exception {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", DUMMY_CLI_OPTIONS).getPlugins();
        assertTrue(plugins.isEmpty());
    }

    @Test
    public void allPlugins_PositiveScenario() throws Exception {
        // Adding minimum plugin data to make the plugins declare themselves activated.
        // The specific plugins behavior is going to be tested in separate tests.
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": " +
                "[{\"input\": \"index.txt\"}], \"plugins\": " +
                "{\"relative-paths\": {\"rel_path\": \"/doc\"}, " +
                "\"page-flows\": {\"sections\": [{\"link\": \"doc/about.html\", " +
                "\"title\": \"About\"}]}, " +
                "\"page-variables\":{\"v\": {}}, " +
                "\"variables\": {\"logo\": \"THE GREATEST SITE EVER!\"}, " +
                "\"index\": {\"index\": {\"output\": \"o.html\", \"index-cache\": \"ic.json\"}} \n" +
                "}}", DUMMY_CLI_OPTIONS).getPlugins();
        assertEquals(5, plugins.size());
    }

    @Test
    public void auto_output_file_with_root_dirs_PositiveScenario() throws Exception {
        ArgFile argFile = parseArgumentFile("{\"documents\": [" +
                "{\"input-root\": \"doc_src/txt\", \"output-root\": \"doc/html\", " +
                "\"input\": \"index.txt\"}, " +
                "{\"output-root\": \"doc/html\", \"input\": \"index.txt\"}, " +
                "{\"input-root\": \"doc_src/txt\", \"input\": \"index.txt\"}" +
                "]}}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        assertEquals("doc_src/txt/index.txt", doc.getInput());
        assertEquals("doc/html/index.html", doc.getOutput());
        doc = argFile.getDocuments().get(1);
        assertEquals("index.txt", doc.getInput());
        assertEquals("doc/html/index.html", doc.getOutput());
        doc = argFile.getDocuments().get(2);
        assertEquals("doc_src/txt/index.txt", doc.getInput());
        assertEquals("index.html", doc.getOutput());
    }

}
