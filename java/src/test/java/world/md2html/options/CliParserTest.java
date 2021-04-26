package world.md2html.options;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class CliParserTest {

    @Test
    public void getMd2HtmlOptions_helpRequested() throws Exception {
        testHelp();
        testHelp("-h");
        testHelp("--help");
    }

    @Test
    public void getMd2HtmlOptions_minimalArgumentSet() throws Exception {
        CliParsingResult parsingResult = getParsingResult("-i", "../doc/notes.md");
        assertEquals(CliParsingResultType.SUCCESS, parsingResult.getResultType());
        Md2HtmlOptions options = parsingResult.getOptions();
        assertEquals(Paths.get("../doc/notes.md"), options.getInputFile());
        assertEquals(Paths.get("../doc/notes.html"), options.getOutputFile());
        assertEquals("", options.getTitle());
        // Template path depends on the environment and is not checked here.
        assertEquals("", options.getLinkCss());
        assertFalse(options.isForce());
        assertFalse(options.isVerbose());
        assertFalse(options.isReport());
    }

    @Test
    public void getMd2HtmlOptions_maxArguments_withoutPositional() throws Exception {

        // Short form
        CliParsingResult parsingResult = getParsingResult("-i", "input.md", "-o", "doc/output.htm",
                "-t", "someTitle", "--templates", "../templateDir", "-l", "someStyles.css",
                "-fv");
        assertEquals(CliParsingResultType.SUCCESS, parsingResult.getResultType());
        Md2HtmlOptions options = parsingResult.getOptions();
        assertEquals(Paths.get("input.md"), options.getInputFile());
        assertEquals(Paths.get("doc/output.htm"), options.getOutputFile());
        assertEquals("someTitle", options.getTitle());
        assertEquals(Paths.get("../templateDir"), options.getTemplateDir());
        assertEquals("someStyles.css", options.getLinkCss());
        assertTrue(options.isForce());
        assertTrue(options.isVerbose());
        assertFalse(options.isReport());

        // Long form
        parsingResult = getParsingResult("--input", "input.md", "--output=doc/output.htm",
                "--title", "someTitle", "--templates", "../templateDir",
                "--link-css=someStyles.css", "--force", "--verbose");
        assertEquals(CliParsingResultType.SUCCESS, parsingResult.getResultType());
        Md2HtmlOptions options1 = parsingResult.getOptions();
        assertMd2HtmlOptionsEquals(options, options1);
    }

    @Test
    public void getMd2HtmlOptions_maxArguments_withPositional() throws Exception {
        CliParsingResult parsingResult = getParsingResult("-f", "-r", "--templates",
                "../templates/default", "-l", "styles.css",
                "doc/notes.txt", "index.html", "some_title");
        assertEquals(CliParsingResultType.SUCCESS, parsingResult.getResultType());
        Md2HtmlOptions options = parsingResult.getOptions();
        assertEquals(Paths.get("doc/notes.txt"), options.getInputFile());
        assertEquals(Paths.get("index.html"), options.getOutputFile());
        assertEquals("some_title", options.getTitle());
        assertEquals(Paths.get("../templates/default"), options.getTemplateDir());
        assertEquals("styles.css", options.getLinkCss());
        assertTrue(options.isForce());
        assertFalse(options.isVerbose());
        assertTrue(options.isReport());
    }

    @Test
    public void getMd2HtmlOptions_unknownKeys() {
        assertThrows(ParseException.class, () -> getParsingResult("-u"));
        assertThrows(ParseException.class, () -> getParsingResult("--unknown"));
    }

    @Test
    public void getMd2HtmlOptions_missingKeyArgument() {
        assertThrows(ParseException.class, () -> getParsingResult("-i"));
        assertThrows(ParseException.class, () -> getParsingResult("-o", "-i", "input.txt"));
        assertThrows(ParseException.class,
                () -> getParsingResult("-o", "output.html", "--input=input.txt", "--title"));
    }

    @Test
    public void getMd2HtmlOptions_wrongNumberOfPositionalArguments() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("input.txt"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("input.txt", "output.html"));
    }

    @Test
    public void getMd2HtmlOptions_wrongCombinationOfOfPositionalAndNamedArguments() {
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("--input=readme.md", "readme.txt", "readme.html", "Title"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("--output=readme.htm", "readme.txt", "readme.html",
                        "Title"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("-t", "Overview", "readme.txt", "readme.html",
                        "Title"));
    }

    @Test
    public void getMd2HtmlOptions_wrongVerboseAndReportFlags() {
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("-i", "readme.txt", "-vr"));
    }

    private void assertMd2HtmlOptionsEquals(Md2HtmlOptions o1, Md2HtmlOptions o2) {
        assertEquals(o1.getInputFile(), o2.getInputFile());
        assertEquals(o1.getOutputFile(), o2.getOutputFile());
        assertEquals(o1.getTitle(), o2.getTitle());
        assertEquals(o1.getTemplateDir(), o2.getTemplateDir());
        assertEquals(o1.getLinkCss(), o2.getLinkCss());
        assertEquals(o1.isForce(), o2.isForce());
        assertEquals(o1.isVerbose(), o2.isVerbose());
        assertEquals(o1.isReport(), o2.isReport());
    }

    private CliParsingResult getParsingResult(String... args)
            throws ParseException, CliArgumentsException {
        return new CliParser().getMd2HtmlOptions(args);
    }

    private void testHelp(String... args) throws Exception {
        CliParsingResult parsingResult = getParsingResult(args);
        assertEquals(CliParsingResultType.HELP, parsingResult.getResultType());
        assertNull(parsingResult.getOptions());
    }

}
