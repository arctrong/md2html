package world.md2html.options;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class CliParserHelperTest {

    @Test
    public void getMd2HtmlOptions_helpRequested() {
        testHelp();
        testHelp("-h");
        testHelp("--help");
        testHelp("-t \"no_matter_what\"", "--help");
    }

    @Test
    public void getMd2HtmlOptions_minimalArgumentSet() throws Exception {
        Md2HtmlOptions options = getParsingResult("-i", "../doc/notes.md");
        assertEquals(Paths.get("../doc/notes.md"), options.getInputFile());
        assertEquals(Paths.get("../doc/notes.html"), options.getOutputFile());
        assertNull(options.getTitle());
        // Template path depends on the environment and is not checked here.
        assertNull(options.getLinkCss());
        assertNotNull(options.getIncludeCss());
        assertFalse(options.isForce());
        assertFalse(options.isVerbose());
        assertFalse(options.isReport());
    }

    @Test
    public void getMd2HtmlOptions_maxArguments() throws Exception {

        // Short form
        Md2HtmlOptions options = getParsingResult("-i", "input.md", "-o", "doc/output.htm",
                "-t", "someTitle", "--template", "../templateDir", "--link-css=someStyles.css",
                "-fv");
        assertEquals(Paths.get("input.md"), options.getInputFile());
        assertEquals(Paths.get("doc/output.htm"), options.getOutputFile());
        assertEquals("someTitle", options.getTitle());
        assertEquals(Paths.get("../templateDir"), options.getTemplateDir());
        assertEquals(1, options.getLinkCss().size());
        assertEquals("someStyles.css", options.getLinkCss().get(0));
        assertNull(options.getIncludeCss());
        assertTrue(options.isForce());
        assertTrue(options.isVerbose());
        assertFalse(options.isReport());

        // Long form
        Md2HtmlOptions options1 = getParsingResult("--input", "input.md", "--output=doc/output.htm",
                "--title", "someTitle", "--template", "../templateDir",
                "--link-css", "someStyles.css", "--force", "--verbose");
        assertMd2HtmlOptionsEquals(options, options1);
    }

    @Test
    public void getMd2HtmlOptions_includeCss() throws Exception {
        Md2HtmlOptions options = getParsingResult("-i", "input.md", "--include-css=styles1.css",
                "--include-css=styles2.css");
        assertEquals(2, options.getIncludeCss().size());
        assertTrue(options.getIncludeCss().contains(Paths.get("styles1.css")));
        assertTrue(options.getIncludeCss().contains(Paths.get("styles2.css")));
        assertTrue(options.getLinkCss() == null || options.getLinkCss().isEmpty());
    }

    @Test
    public void getMd2HtmlOptions_linkCss() throws Exception {
        Md2HtmlOptions options = getParsingResult("-i", "input.md", "--link-css=styles1.css",
                "--link-css=styles2.css");
        assertEquals(2, options.getLinkCss().size());
        assertTrue(options.getLinkCss().contains("styles1.css"));
        assertTrue(options.getLinkCss().contains("styles2.css"));
        assertTrue(options.getIncludeCss() == null || options.getIncludeCss().isEmpty());
    }

    @Test
    public void getMd2HtmlOptions_linkAndIncludeCss() throws Exception {
        Md2HtmlOptions options = getParsingResult("-i", "input.md", "--link-css=styles1.css",
                "--include-css=styles2.css");
        assertEquals(1, options.getLinkCss().size());
        assertEquals(1, options.getIncludeCss().size());
        assertTrue(options.getLinkCss().contains("styles1.css"));
        assertTrue(options.getIncludeCss().contains(Paths.get("styles2.css")));
    }

    @Test
    public void getMd2HtmlOptions_defaultCss() throws Exception {
        Md2HtmlOptions options = getParsingResult("-i", "input.md");
        assertTrue(options.getLinkCss() == null || options.getLinkCss().isEmpty());
        assertEquals(1, options.getIncludeCss().size());
    }

    @Test
    public void getMd2HtmlOptions_noCss() throws Exception {
        Md2HtmlOptions options = getParsingResult("-i", "input.md", "--no-css");
        assertTrue(options.getLinkCss() == null || options.getLinkCss().isEmpty());
        assertTrue(options.getIncludeCss() == null || options.getIncludeCss().isEmpty());
    }

    @Test
    public void getMd2HtmlOptions_unknownKeys() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-u"));
        assertThrows(CliArgumentsException.class, () -> getParsingResult("--unknown"));
    }

    @Test
    public void getMd2HtmlOptions_missingKeyArgument() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-i"));
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-o", "-i", "input.txt"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("-o", "output.html", "--input=input.txt", "--title"));
    }

    @Test
    public void getMd2HtmlOptions_positionalArguments_mustFail() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("input.txt"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("input.txt", "output.html"));
    }

    @Test
    public void getMd2HtmlOptions_wrongVerboseAndReportFlags() {
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("-i", "readme.txt", "-vr"));
    }

    @Test
    public void getMd2HtmlOptions_wrongNoCssAndCss() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-i", "readme.txt",
                "--no-css", "--include-css", "styles.css"));
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-i", "readme.txt",
                "--no-css", "--link-css", "styles.css"));
    }

    private void assertMd2HtmlOptionsEquals(Md2HtmlOptions o1, Md2HtmlOptions o2) {
        assertEquals(o1.getInputFile(), o2.getInputFile());
        assertEquals(o1.getOutputFile(), o2.getOutputFile());
        assertEquals(o1.getTitle(), o2.getTitle());
        assertEquals(o1.getTemplateDir(), o2.getTemplateDir());
        assertEquals(o1.getIncludeCss(), o2.getIncludeCss());
        assertEquals(o1.getLinkCss(), o2.getLinkCss());
        assertEquals(o1.isForce(), o2.isForce());
        assertEquals(o1.isVerbose(), o2.isVerbose());
        assertEquals(o1.isReport(), o2.isReport());
    }

    private Md2HtmlOptions getParsingResult(String... args) throws CliArgumentsException {
        CliParserHelper cliParserHelper = new CliParserHelper("no_matter_what");
        return cliParserHelper.parse(args);
    }

    private void testHelp(String... args) {
        String exceptionClass = CliArgumentsException.class.getName();
        try {
            getParsingResult(args);
            fail(exceptionClass + " was expected but nothing was thrown");
        } catch (CliArgumentsException e) {
            assertEquals(CliArgumentsException.CliParsingExceptionType.HELP, e.getExceptionType());
        } catch (Exception e) {
            fail(exceptionClass + " was expected but " + e.getClass().getName() + " was thrown");
        }
    }

}
