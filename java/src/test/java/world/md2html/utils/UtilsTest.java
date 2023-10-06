package world.md2html.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTest {

    @Test
    void stripExtension() {
        assertEquals("", Utils.stripExtension(""));
        assertEquals("file_with_one", Utils.stripExtension("file_with_one.extension"));
        assertEquals("file_with.two", Utils.stripExtension("file_with.two.extensions"));
        assertEquals("file_without_extension", Utils.stripExtension("file_without_extension"));
        assertEquals(".file_without_name", Utils.stripExtension(".file_without_name"));
        assertEquals("a", Utils.stripExtension("a.file_with_short_name"));
        assertEquals("file/with/path", Utils.stripExtension("file/with/path.txt"));
        assertEquals("with\\path_without_ext", Utils.stripExtension("with\\path_without_ext"));
        assertEquals("with/dotted.path/name", Utils.stripExtension("with/dotted.path/name.ext"));
        assertEquals("with\\dotted.path/name", Utils.stripExtension("with\\dotted.path/name"));
        assertEquals("with/dotted.path/.name", Utils.stripExtension("with/dotted.path/.name"));
        assertEquals("with\\path/a", Utils.stripExtension("with\\path/a.ext"));
    }

    @Test
    void firstNotNull() {
        assertEquals(1, Utils.firstNotNull(null, 1, 2));
        assertNull(Utils.firstNotNull(null, null));
        assertNull(Utils.firstNotNull());
        Object o = new String[0];
        assertEquals(o, Utils.firstNotNull(o, new String[]{"1", "2"}));
    }

    @Test
    void formatNanoSeconds() {
        long m = 1_000_000;
        assertEquals("0 00:00:00.000", Utils.formatNanoSeconds(0));
        assertEquals("0 00:00:00.000", Utils.formatNanoSeconds(1));
        assertEquals("0 00:00:00.000", Utils.formatNanoSeconds(999_999));
        assertEquals("0 00:00:00.001", Utils.formatNanoSeconds(1_000_000));
        assertEquals("0 00:00:00.999", Utils.formatNanoSeconds(999 * m));
        assertEquals("0 00:00:01.000", Utils.formatNanoSeconds(1000 * m));
        assertEquals("0 00:00:59.000", Utils.formatNanoSeconds(59_000 * m));
        assertEquals("0 00:01:00.000", Utils.formatNanoSeconds(60_000 * m));
        assertEquals("0 00:01:01.000", Utils.formatNanoSeconds(61_000 * m));
        assertEquals("0 00:59:00.000", Utils.formatNanoSeconds(59 * 60_000 * m));
        assertEquals("0 01:00:00.000", Utils.formatNanoSeconds(60 * 60_000 * m));
        assertEquals("0 01:01:00.000", Utils.formatNanoSeconds(61 * 60_000 * m));
        assertEquals("0 23:00:00.000", Utils.formatNanoSeconds(23 * 60 * 60_000 * m));
        assertEquals("1 00:00:00.000", Utils.formatNanoSeconds(24 * 60 * 60_000 * m));
        assertEquals("1 01:00:00.000", Utils.formatNanoSeconds(25 * 60 * 60_000 * m));
        assertEquals("125 17:28:58.819", Utils.formatNanoSeconds(
                ((((125L * 24 * 3600) + (17 * 3600) + (28 * 60) + 58) * 1000) + 819) * m + 1));
        assertEquals("90 08:05:04.009", Utils.formatNanoSeconds(
                ((((90L * 24 * 3600) + (8 * 3600) + (5 * 60) + 4) * 1000) + 9) * m + 358));
    }

    @Test
    void blankCommentLine() {
        assertEquals("        ", Utils.blankCommentLine("#comment", "#"));
        assertEquals("        \n", Utils.blankCommentLine("#comment\n", "#"));
        assertEquals("        \r\n", Utils.blankCommentLine("#comment\r\n", "#"));
        assertEquals("         ", Utils.blankCommentLine(" #comment", "#"));
        assertEquals("not comment", Utils.blankCommentLine("not comment", "#"));
        assertEquals("not # comment", Utils.blankCommentLine("not # comment", "#"));
        assertEquals(" ", Utils.blankCommentLine("#", "#"));
        assertEquals(" \n", Utils.blankCommentLine("#\n", "#"));
    }

    @Test
    void relativizeRelativeResource() throws CheckedIllegalArgumentException {

        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativeResource("styles.css", ""));
        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativeResource("styles.css", "doc/"));
        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativeResource("", "index.html"));
        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativeResource("doc/", "index.html"));

        assertEquals("styles.css", Utils.relativizeRelativeResource("styles.css", "index.html"));
        assertEquals("doc/styles.css", Utils.relativizeRelativeResource("doc/styles.css", "index.html"));
        assertEquals("doc/pict/logo.png", Utils.relativizeRelativeResource("doc/pict/logo.png", "index.html"));

        assertEquals("../../logo.png", Utils.relativizeRelativeResource("../logo.png", "doc/index.html"));
        assertEquals("../logo.png", Utils.relativizeRelativeResource("logo.png", "doc/index.html"));
        assertEquals("logo.png", Utils.relativizeRelativeResource("doc/logo.png", "doc/index.html"));
        assertEquals("pict/logo.png", Utils.relativizeRelativeResource("doc/pict/logo.png", "doc/index.html"));

        assertEquals("../pict/logo.png", Utils.relativizeRelativeResource("pict/logo.png", "doc/index.html"));
        assertEquals("../pict/doc/logo.png", Utils.relativizeRelativeResource("pict/doc/logo.png", "doc/index.html"));
        assertEquals("../../pict/logo.png", Utils.relativizeRelativeResource("pict/logo.png", "doc/chapter01/index.html"));
        assertEquals("../../pict/doc/logo.png", Utils.relativizeRelativeResource("pict/doc/logo.png", "doc/chapter01/index.html"));

        assertEquals("logo.png", Utils.relativizeRelativeResource("./logo.png", "index.html"));
        assertEquals("../logo.png", Utils.relativizeRelativeResource("./logo.png", "doc/index.html"));
    }

    @Test
    void relativizeRelativePath() throws CheckedIllegalArgumentException {

        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativePath("doc/", ""));
        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativePath("doc/", "path/"));
        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativePath("doc", "index.html"));
        assertThrows(CheckedIllegalArgumentException.class,
                () -> Utils.relativizeRelativePath("/", "index.html"));

        assertEquals("../", Utils.relativizeRelativePath("../", "index.html"));
        assertEquals("", Utils.relativizeRelativePath("", "index.html"));
        assertEquals("doc/", Utils.relativizeRelativePath("doc/", "index.html"));
        assertEquals("doc/pict/", Utils.relativizeRelativePath("doc/pict/", "index.html"));

        assertEquals("../../", Utils.relativizeRelativePath("../", "doc/index.html"));
        assertEquals("../", Utils.relativizeRelativePath("", "doc/index.html"));
        assertEquals("", Utils.relativizeRelativePath("doc/", "doc/index.html"));
        assertEquals("pict/", Utils.relativizeRelativePath("doc/pict/", "doc/index.html"));

        assertEquals("../pict/", Utils.relativizeRelativePath("pict/", "doc/index.html"));
        assertEquals("../pict/doc/", Utils.relativizeRelativePath("pict/doc/", "doc/index.html"));
        assertEquals("../../pict/", Utils.relativizeRelativePath("pict/", "doc/chapter01/index.html"));
        assertEquals("../../pict/doc/", Utils.relativizeRelativePath("pict/doc/", "doc/chapter01/index.html"));

        assertEquals("", Utils.relativizeRelativePath("./", "index.html"));
        assertEquals("../", Utils.relativizeRelativePath("./", "doc/index.html"));
    }

    @Test
    void refineToString() {
        RefineToStringTestClass object = new RefineToStringTestClass("value1", "value2");
        assertEquals("{field1=value1, field2=value2}", Utils.refineToString(object));
    }

    @Test
    void maskRegexChars() {
        assertEquals("no replacements", Utils.maskRegexChars("no replacements"));
        assertEquals("\\[x\\]", Utils.maskRegexChars("[x]"));
        assertEquals("\\?\\^\\\\\\$\\.\\|\\*\\+\\]\\[\\)\\(\\}\\{",
                Utils.maskRegexChars("?^\\$.|*+][)(}{"));
    }

    public static Stream<Arguments> testSmartSubstringer() {
        return Stream.of(
                Arguments.of(" do not substring ", "", "", "", "", " do not substring "),
                Arguments.of("xxx SW yyy", "SW", "", "", "", "SW yyy"),
                Arguments.of("xxx EW yyy", "", "EW", "", "", "xxx EW"),
                Arguments.of("xxx SM yyy", "", "", "SM", "", " yyy"),
                Arguments.of("xxx EM yyy", "", "", "", "EM", "xxx "),
                Arguments.of("xxx SW yyy EW zzz", "SW", "EW", "", "", "SW yyy EW"),
                Arguments.of("xxx SM yyy EM zzz", "", "", "SM", "EM", " yyy "),
                Arguments.of("aaa SW bbb SM ccc", "SW", "", "SM", "", "SW bbb SM ccc"),
                Arguments.of("aaa SM bbb SW ccc", "SW", "", "SM", "", " bbb SW ccc"),
                Arguments.of("aaa EW bbb EM ccc", "", "EW", "", "EM", "aaa EW"),
                Arguments.of("aaa EM bbb EW ccc", "", "EW", "", "EM", "aaa "),
                Arguments.of("no start_with", "SW", "", "", "", ""),
                Arguments.of("no start_marker", "", "", "SM", "", ""),
                Arguments.of("no end_with", "", "EW", "", "", "no end_with"),
                Arguments.of("no end_marker", "", "", "", "EM", "no end_marker"),
                Arguments.of("SW at start ", "SW", "", "", "", "SW at start "),
                Arguments.of("SM at start ", "", "", "SM", "", " at start "),
                Arguments.of(" at end EW", "", "EW", "", "", " at end EW"),
                Arguments.of(" at end EM", "", "", "", "EM", " at end "),
                Arguments.of("EW at start", "", "EW", "", "", "EW"),
                Arguments.of("EM at start", "", "", "", "EM", ""),
                Arguments.of(" at end SW", "SW", "", "", "", "SW"),
                Arguments.of(" at end SM", "", "", "SM", "", ""),
                Arguments.of("uuu EW after SW www", "SW", "EW", "", "", ""),
                Arguments.of("with limits SW with EW spaces", " SW ", " EW ", "", "",
                        " SW with EW "),
                Arguments.of("markers SM with EM spaces", "", "", " SM ", " EM ", "with"),
                Arguments.of("uuu EM after SM www", "", "", "SM", "EM", ""),
                Arguments.of("start with ?^\\$.|*+][)(}{ all RE chars", "?^\\$.|*+][)(}{",
                        "", "", "", "?^\\$.|*+][)(}{ all RE chars"),
                Arguments.of("a }SM{ some RE chars ]EM[ b", "", "", "}SM{", "]EM[",
                        " some RE chars ")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSmartSubstringer(String input, String startWith, String endWith, String startMarker,
                              String endMarker, String expected) {
        SmartSubstringer substringer = new SmartSubstringer(startWith, endWith, startMarker,
                endMarker);
        assertEquals(expected, substringer.substring(input));
    }

    public static Stream<Arguments> stripEmptyLines() {
        return Stream.of(
                Arguments.of("empty string", "", ""),
                Arguments.of("all spaces", " ", ""),
                Arguments.of("no EOFs nor spaces", "no EOFs", "no EOFs"),
                Arguments.of("no EOFs", "   text   ", "   text   "),
                Arguments.of("different EOFs", "\rline 1\r\nline 2\n\rline 3\n",
                        "line 1\r\nline 2\n\rline 3"),
                Arguments.of("all spaces with EOFs", "   \r\n   ", ""),
                Arguments.of("EOFs at start", "\r\n   text   ", "   text   "),
                Arguments.of("EOFs at end", "   text   \r\n", "   text   "),
                Arguments.of("multiline string", "  \n   line 1 \n line 2   \n   ",
                        "   line 1 \n line 2   ")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void stripEmptyLines(String name, String input, String expected) {
        assertEquals(expected, Utils.stripEmptyLines(input));
    }
}
