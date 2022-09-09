package world.md2html.options.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import world.md2html.options.cli.CliArgumentsException.CliParsingExceptionType;
import world.md2html.options.model.CliOptions;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CliParserTest {

    private void assertMd2HtmlOptionsEquals(CliOptions o1, CliOptions o2) {
        assertEquals(o1.getInputRoot(), o2.getInputRoot());
        assertEquals(o1.getOutputRoot(), o2.getOutputRoot());
        assertEquals(o1.getInput(), o2.getInput());
        assertEquals(o1.getOutput(), o2.getOutput());
        assertEquals(o1.getTitle(), o2.getTitle());
        assertEquals(o1.getTemplate(), o2.getTemplate());
        assertEquals(o1.getIncludeCss(), o2.getIncludeCss());
        assertEquals(o1.getLinkCss(), o2.getLinkCss());
        assertEquals(o1.isForce(), o2.isForce());
        assertEquals(o1.isVerbose(), o2.isVerbose());
        assertEquals(o1.isReport(), o2.isReport());
    }

    private CliOptions getParsingResult(String... args) throws CliArgumentsException {
        CliParser cliParser = new CliParser("whatever");
        return cliParser.parse(args);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> helpRequested() {
        return Stream.of(
                Arguments.of((Object) new String[] {"-h"}),
                Arguments.of((Object) new String[] {"--help"}),
                Arguments.of((Object) new String[] {"-t", "\"whatever\"", "--help"})
        );
    }

    @ParameterizedTest
    @MethodSource
    public void helpRequested(String... args) {
        CliArgumentsException e = assertThrows(CliArgumentsException.class,
                () -> getParsingResult(args));
        assertEquals(CliParsingExceptionType.HELP, e.getExceptionType());
    }

    @Test
    public void minimalArgumentSet() throws Exception {
        CliOptions options = getParsingResult("-i", "../doc/notes.md");
        assertEquals("../doc/notes.md", options.getInput());
        assertNull(options.getArgumentFile());
        assertNull(options.getTitle());
        assertNull(options.getLinkCss());
        assertFalse(options.isForce());
        assertFalse(options.isVerbose());
        assertFalse(options.isReport());
        assertFalse(options.isLegacyMode());
    }

    @Test
    public void argumentFile() throws Exception {
        CliOptions options = getParsingResult("--argument-file", "md2html_args.json");
        assertEquals("md2html_args.json", options.getArgumentFile());
    }

    @Test
    public void noInputFileWithArgumentFile() throws Exception {
        getParsingResult("-t", "whatever", "--argument-file=md2html_args.json");
    }

    @Test
    public void maxArguments() throws Exception {

        // Short form
        CliOptions options = getParsingResult(
                "--input-root", "input/root", "--output-root", "output/root",
                "-i", "input.md", "-o", "doc/output.htm",
                "-t", "someTitle", "--template", "../templateDir", "--link-css=someStyles.css",
                "-fv");
        assertEquals("input.md", options.getInput());
        assertEquals("doc/output.htm", options.getOutput());
        assertEquals("someTitle", options.getTitle());
        assertEquals("../templateDir", options.getTemplate());
        assertEquals(1, options.getLinkCss().size());
        assertEquals("someStyles.css", options.getLinkCss().get(0));
        assertNull(options.getIncludeCss());
        assertTrue(options.isForce());
        assertTrue(options.isVerbose());
        assertFalse(options.isReport());

        // Long form
        CliOptions options1 = getParsingResult(
                "--input-root", "input/root", "--output-root", "output/root",
                "--input", "input.md", "--output=doc/output.htm",
                "--title", "someTitle", "--template", "../templateDir",
                "--link-css", "someStyles.css", "--force", "--verbose");
        assertMd2HtmlOptionsEquals(options, options1);
    }

    @Test
    public void includeCss() throws Exception {
        CliOptions options = getParsingResult("-i", "input.md", "--include-css=styles1.css",
                "--include-css=styles2.css");
        assertEquals(2, options.getIncludeCss().size());
        assertTrue(options.getIncludeCss().contains("styles1.css"));
        assertTrue(options.getIncludeCss().contains("styles2.css"));
        assertTrue(options.getLinkCss() == null || options.getLinkCss().isEmpty());
    }

    @Test
    public void linkCss() throws Exception {
        CliOptions options = getParsingResult("-i", "input.md", "--link-css=styles1.css",
                "--link-css=styles2.css");
        assertEquals(2, options.getLinkCss().size());
        assertTrue(options.getLinkCss().contains("styles1.css"));
        assertTrue(options.getLinkCss().contains("styles2.css"));
        assertTrue(options.getIncludeCss() == null || options.getIncludeCss().isEmpty());
    }

    @Test
    public void linkAndIncludeCss() throws Exception {
        CliOptions options = getParsingResult("-i", "input.md", "--link-css=styles1.css",
                "--include-css=styles2.css");
        assertEquals(1, options.getLinkCss().size());
        assertEquals(1, options.getIncludeCss().size());
        assertTrue(options.getLinkCss().contains("styles1.css"));
        assertTrue(options.getIncludeCss().contains("styles2.css"));
    }

    @Test
    public void noCss() throws Exception {
        CliOptions options = getParsingResult("-i", "input.md", "--no-css");
        assertTrue(options.getLinkCss() == null || options.getLinkCss().isEmpty());
        assertTrue(options.getIncludeCss() == null || options.getIncludeCss().isEmpty());
    }

    @Test
    public void unknownKeys() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-u"));
        assertThrows(CliArgumentsException.class, () -> getParsingResult("--unknown"));
    }

    @Test
    public void missingKeyArgument() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-i"));
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-o", "-i", "input.txt"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("-o", "output.html", "--input=input.txt", "--title"));
    }

    @Test
    public void positionalArguments_mustFail() {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("input.txt"));
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("input.txt", "output.html"));
    }

    @Test
    public void wrongVerboseAndReportFlags() {
        assertThrows(CliArgumentsException.class,
                () -> getParsingResult("-i", "readme.txt", "-vr"));
    }

    @ParameterizedTest
    @CsvSource({"--include-css", "--link-css"})
    public void wrongNoCssAndCss(String cssOption) {
        assertThrows(CliArgumentsException.class, () -> getParsingResult("-i", "readme.txt",
                "--no-css", cssOption, "styles.css"));
    }

    @Test
    public void legacyMode() throws CliArgumentsException {
        CliOptions options = getParsingResult("-i", "input.md", "--legacy-mode");
        assertTrue(options.isLegacyMode());
    }

}
