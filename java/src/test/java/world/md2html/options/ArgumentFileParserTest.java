package world.md2html.options;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.options.Md2HtmlOptionUtils.enrichDocumentMd2HtmlOptionsList;

public class ArgumentFileParserTest {

    @Test
    public void emptyFile_NegativeScenario() {
        assertThrows(ArgumentFileParseException.class,
                () -> ArgumentFileParser.parse("", null));
    }

    @Test
    public void rootElementIsNotObject_NegativeScenario() {
        assertThrows(ArgumentFileParseException.class,
                () -> ArgumentFileParser.parse("[1, 2]", null));
    }

    @Test
    public void defaultElementIsNotObject_NegativeScenario() {
        assertThrows(ArgumentFileParseException.class,
                () -> ArgumentFileParser.parse("{\"default\": []}", null));
    }

    @Test
    public void noDefaultElement_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse("{\"documents\": []}", null);
        assertEquals(0, options.size());
    }

    @Test
    public void fullDefaultElement_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse(
                "{\"default\": {\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], \"include-css\": [\"include.css\"], " +
                        "\"force\": true, \"verbose\": true}, \"documents\": [{}]}", null);
        options = enrichDocumentMd2HtmlOptionsList(options);
        Md2HtmlOptions doc = options.get(0);
        assertEquals(Paths.get("index.txt"), doc.getInputFile());
        assertEquals(Paths.get("index.html"), doc.getOutputFile());
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
        try {
            ArgumentFileParser.parse("{\"default\": {}}", null);
            fail("exception expected");
        } catch (ArgumentFileParseException e) {
            assertTrue(e.getMessage().contains("documents"));
        }
    }

    @Test
    public void documentsElementIsNotList_NegativeScenario() {
        assertThrows(ArgumentFileParseException.class,
                () -> ArgumentFileParser.parse("{\"documents\": \"not a list\"}", null));
    }

    @Test
    public void emptyDocumentsElement_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse("{\"documents\": []}", null);
        assertEquals(0, options.size());
    }

    @ParameterizedTest
    @CsvSource({"link-css", "include-css"})
    public void defaultElementNoCssWithCssDefinitions_NegativeScenario(String cssType) {
        try {
            ArgumentFileParser.parse("{\"default\": {\"no-css\": true, \"" + cssType +
                    "\": [\"some.css\"]}, \"documents\": []}", null);
            fail("exception expected");
        } catch (ArgumentFileParseException e) {
            assertTrue(e.getMessage().contains("no-css"));
            assertTrue(e.getMessage().contains(cssType));
        }
    }

    @Test
    public void minimalDocument_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}]}", null);
        options = enrichDocumentMd2HtmlOptionsList(options);
        Md2HtmlOptions doc = options.get(0);
        assertEquals(Paths.get("index.txt"), doc.getInputFile());
        assertTrue(doc.getOutputFile().toString().contains("index"));
    }

    @Test
    public void severalDocuments_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}, {\"input\": \"about.txt\"}], " +
                        "\"default\": {\"template\": \"common_template.html\"}}", null);
        options = enrichDocumentMd2HtmlOptionsList(options);
        Md2HtmlOptions doc = options.get(0);
        assertEquals(Paths.get("index.txt"), doc.getInputFile());
        assertEquals(Paths.get("common_template.html"), doc.getTemplate());
        assertTrue(doc.getOutputFile().toString().contains("index"));
        doc = options.get(1);
        assertEquals(Paths.get("about.txt"), doc.getInputFile());
        assertEquals(Paths.get("common_template.html"), doc.getTemplate());
        assertTrue(doc.getOutputFile().toString().contains("about"));
    }

    @Test
    public void fullDocument_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], " +
                        "\"add-link-css\": [\"add_link.css\"], " +
                        "\"include-css\": [\"include.css\"], " +
                        "\"add-include-css\": [\"add_include1.css\", \"add_include1.css\"], " +
                        "\"force\": true, \"verbose\": true}]}", null);
        Md2HtmlOptions doc = options.get(0);
        assertEquals(Paths.get("index.txt"), doc.getInputFile());
        assertEquals(Paths.get("index.html"), doc.getOutputFile());
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
        try {
            ArgumentFileParser.parse(
                    "{\"documents\": [{\"output\": \"index.html\"}]}", null);
            fail("exception expected");
        } catch (ArgumentFileParseException e) {
            assertTrue(e.getMessage().toUpperCase().contains("INPUT"));
        }
    }

    @ParameterizedTest
    @CsvSource({"link-css", "include-css", "add-link-css", "add-include-css"})
    public void documentNoCssWithCssDefinitions_NegativeScenario(String cssType) {
        try {
            ArgumentFileParser.parse("{\"documents\": [{\"no-css\": true, \"" +
                    cssType + "\": [\"some.css\"]}]}", null);
            fail("exception expected");
        } catch (ArgumentFileParseException e) {
            assertTrue(e.getMessage().contains("no-css"));
            assertTrue(e.getMessage().contains(cssType));
        }
    }

    @Test
    public void documentVerboseAndReportFlags_NegativeScenario() {
        try {
            ArgumentFileParser.parse("{\"documents\": [{\"output\": \"index.html\", " +
                    "\"verbose\": true, \"report\": true}]}", null);
            fail("exception expected");
        } catch (ArgumentFileParseException e) {
            assertTrue(e.getMessage().contains("verbose"));
            assertTrue(e.getMessage().contains("report"));
        }
    }

    @Test
    public void overridingWithCliArgs_PositiveScenario() throws ArgumentFileParseException {
        List<Md2HtmlOptions> options = ArgumentFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"some title\", \"template\": \"path/templates/custom.html\", " +
                        "\"link-css\": [\"link1.css\", \"link2.css\"], " +
                        "\"add-link-css\": [\"add_link.css\"], " +
                        "\"include-css\": [\"include.css\"], " +
                        "\"add-include-css\": [\"add_include1.css\", \"add_include1.css\"], " +
                        "\"force\": false, \"verbose\": false}]}",
                new Md2HtmlOptions(null, Paths.get("cli_index.txt"), Paths.get("cli_index.html"),
                        "cli_title", Paths.get("cli/custom.html"),
                        Arrays.asList(Paths.get("cli_include1.css"), Paths.get("cli_include2.css")),
                        Arrays.asList("cli_link1.css", "cli_link2.css"), false, true, true, false));
        Md2HtmlOptions doc = options.get(0);
        assertEquals(Paths.get("cli_index.txt"), doc.getInputFile());
        assertEquals(Paths.get("cli_index.html"), doc.getOutputFile());
        assertEquals("cli_title", doc.getTitle());
        assertEquals(Paths.get("cli/custom.html"), doc.getTemplate());
        assertFalse(doc.isNoCss());
        assertIterableEquals(Arrays.asList("cli_link1.css", "cli_link2.css"), doc.getLinkCss());
        assertIterableEquals(Arrays.asList(Paths.get("cli_include1.css"),
                Paths.get("cli_include2.css")), doc.getIncludeCss());
        assertTrue(doc.isForce());
        assertTrue(doc.isVerbose());
    }

}
