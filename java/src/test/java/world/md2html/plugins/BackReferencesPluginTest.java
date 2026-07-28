package world.md2html.plugins;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.MetadataHandlersApplicationResult;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;
import world.md2html.utils.UserError;

import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;

class BackReferencesPluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    @TempDir
    Path tempDir;

    private BackReferencesPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findFirstElementOfType(plugins, BackReferencesPlugin.class);
    }

    private List<String> markersFromPlugin(BackReferencesPlugin plugin) {
        if (plugin == null) {
            return Collections.emptyList();
        }
        List<String> markers = new ArrayList<>();
        for (PageMetadataHandlerInfo info : plugin.pageMetadataHandlers()) {
            markers.add(info.getMarker());
        }
        return markers;
    }

    private PluginMarkers pluginFromArgFile(String argFileStr) throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(argFileStr, DUMMY_CLI_OPTIONS);
        BackReferencesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        return new PluginMarkers(plugin, markersFromPlugin(plugin));
    }

    private SimulateBuildResult simulateBuild(String argFileStr,
            List<Map.Entry<Integer, String>> pages) throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(argFileStr, DUMMY_CLI_OPTIONS);
        BackReferencesPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers = argFile.getMetadataHandlers();

        Map<Integer, String> output = new HashMap<>();
        Map<Integer, MetadataHandlersApplicationResult> deferred = new HashMap<>();
        Map<Integer, Boolean> deferredPages = new HashMap<>();

        for (Map.Entry<Integer, String> page : pages) {
            int index = page.getKey();
            String text = page.getValue();
            Document doc = argFile.getDocuments().get(index);
            if (plugin != null) {
                plugin.newPage(doc);
            }
            MetadataHandlersApplicationResult result =
                    metadataHandlers.applyMetadataHandlersWithResult(text, doc);
            deferredPages.put(index, result.isDeferPage());
            if (result.isDeferPage()) {
                deferred.put(index, result);
            } else {
                output.put(index, metadataHandlers.joinParsingResults(
                        result.getParsingResults(), doc));
            }
        }
        for (Map.Entry<Integer, MetadataHandlersApplicationResult> entry : deferred.entrySet()) {
            int index = entry.getKey();
            Document doc = argFile.getDocuments().get(index);
            output.put(index, metadataHandlers.joinParsingResults(
                    entry.getValue().getParsingResults(), doc));
        }

        return new SimulateBuildResult(plugin, markersFromPlugin(plugin), output, deferredPages);
    }

    @Test
    void minimalConfig() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0,
                        "Def <!--refdef example [Example Domain](https://example.com/)-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref example-->.")));

        assertFalse(result.plugin.isBlank());
        assertThat(result.markers, containsInAnyOrder("REF", "REFDEF"));
        assertEquals(
                "Def <a name=\"backref_def_example\"></a><span class=\"ref-def\">[example]</span> "
                        + "[Example Domain](https://example.com/)"
                        + "<sup><a class=\"ref\" href=\"ref.html#backref_ref_example\">1</a></sup>",
                result.html.get(0));
        assertEquals(
                "See [Example Domain](https://example.com/)"
                        + "<sup><a name=\"backref_ref_example\"></a>"
                        + "<a class=\"ref\" href=\"def.html#backref_def_example\">[example]</a></sup>.",
                result.html.get(1));
    }

    @Test
    void fullConfig() throws ArgFileParseException {
        String cacheFile = tempDir.resolve("full_config_cache.json").toString().replace('\\', '/');
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"code-prefix\": \"xref_\","
                + "\"cache\": \"" + cacheFile + "\","
                + "\"def-formats\": [{"
                + "    \"markers\": [\"bibdef\"],"
                + "    \"template\": \"<div id=\\\"${anchor}\\\">${content}<sup>${back_refs_html}</sup></div>\","
                + "    \"back-ref-template\": \"<a class=\\\"bib-back\\\" href=\\\"${href}\\\">${link_text}</a>\","
                + "    \"back-ref-delimiter\": \"; \""
                + "}],"
                + "\"ref-formats\": [{"
                + "    \"markers\": [\"citeref\"],"
                + "    \"template\": \"<cite><a name=\\\"${anchor}\\\"></a><a href=\\\"${href}\\\">${code}</a></cite>\""
                + "}]"
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "Book <!--bibdef src1 Some book text-->"),
                new AbstractMap.SimpleEntry<>(1, "Cite <!--citeref src1-->.")));

        assertFalse(result.plugin.isBlank());
        assertThat(result.markers, containsInAnyOrder("BIBDEF", "CITEREF"));
        assertEquals(
                "Book <div id=\"xref_def_src1\">Some book text"
                        + "<sup><a class=\"bib-back\" href=\"ref.html#xref_ref_src1\">1</a></sup></div>",
                result.html.get(0));
        assertEquals(
                "Cite <cite><a name=\"xref_ref_src1\"></a>"
                        + "<a href=\"def.html#xref_def_src1\">src1</a></cite>.",
                result.html.get(1));
    }

    @Test
    void invalidTemplateSyntax() {
        String argFileStr =
                "{\"documents\": [{\"input\": \"page.txt\", \"output\": \"page.html\"}], "
                + "\"plugins\": {\"back-references\": {"
                + "\"ref-formats\": [{\"markers\": [\"REF\"], \"template\": \"start${}end\"}]"
                + "}}}";
        UserError error = assertThrows(UserError.class,
                () -> parseArgumentFile(argFileStr, DUMMY_CLI_OPTIONS));
        assertTrue(error.getMessage().toLowerCase().contains("empty placeholder"));
    }

    static Stream<Arguments> emptyFormatArraysCases() {
        return Stream.of(
                Arguments.of("{\"ref-formats\": []}", false, Collections.singletonList("REFDEF")),
                Arguments.of("{\"def-formats\": []}", false, Collections.singletonList("REF")),
                Arguments.of("{\"def-formats\": [], \"ref-formats\": []}", true,
                        Collections.<String>emptyList())
        );
    }

    @ParameterizedTest
    @MethodSource("emptyFormatArraysCases")
    void emptyFormatArrays(String pluginConfig, boolean blank, List<String> expectedMarkers)
            throws ArgFileParseException {
        String pageDoc = "{\"input\": \"page.txt\", \"output\": \"page.html\"}";
        String argFileStr =
                "{\"documents\": [" + pageDoc + "], "
                + "\"plugins\": {\"back-references\": " + pluginConfig + "}}";
        PluginMarkers result = pluginFromArgFile(argFileStr);
        if (blank) {
            assertNull(result.plugin);
        } else {
            assertFalse(result.plugin.isBlank());
        }
        assertThat(result.markers, containsInAnyOrder(expectedMarkers.toArray(new String[0])));
    }

    @Test
    void refdefThenRef_whenDefFirst() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "Entry <!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->.")));

        assertEquals(
                "Entry <a name=\"backref_def_foo\"></a><span class=\"ref-def\">[foo]</span> Foo display"
                        + "<sup><a class=\"ref\" href=\"ref.html#backref_ref_foo\">1</a></sup>",
                result.html.get(0));
        assertEquals(
                "See Foo display<sup><a name=\"backref_ref_foo\"></a>"
                        + "<a class=\"ref\" href=\"def.html#backref_def_foo\">[foo]</a></sup>.",
                result.html.get(1));
    }

    @Test
    void refThenRefdef_whenRefFirst_shouldDefer() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"},"
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"}"
                + "], \"plugins\": {\"back-references\": {}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "See <!--ref foo-->."),
                new AbstractMap.SimpleEntry<>(1, "<!--refdef foo Foo display-->")));

        assertTrue(result.deferredPages.get(0));
        assertTrue(result.deferredPages.get(1));
        assertEquals(
                "See Foo display<sup><a name=\"backref_ref_foo\"></a>"
                        + "<a class=\"ref\" href=\"def.html#backref_def_foo\">[foo]</a></sup>.",
                result.html.get(0));
        assertEquals(
                "<a name=\"backref_def_foo\"></a><span class=\"ref-def\">[foo]</span> Foo display"
                        + "<sup><a class=\"ref\" href=\"ref.html#backref_ref_foo\">1</a></sup>",
                result.html.get(1));
    }

    static Stream<Arguments> refToUndefinedDefCases() {
        return Stream.of(
                Arguments.of(
                        "[{\"input\": \"ref.txt\", \"output\": \"ref.html\"}]",
                        Collections.singletonList(new AbstractMap.SimpleEntry<>(0,
                                "See <!--ref missing-->."))),
                Arguments.of(
                        "[{\"input\": \"ref.txt\", \"output\": \"ref.html\"},"
                                + " {\"input\": \"other.txt\", \"output\": \"other.html\"}]",
                        Arrays.asList(
                                new AbstractMap.SimpleEntry<>(0, "See <!--ref missing-->."),
                                new AbstractMap.SimpleEntry<>(1, "Other page.")))
        );
    }

    @ParameterizedTest
    @MethodSource("refToUndefinedDefCases")
    void refToUndefinedDef_shouldFail(String documentsJson,
            List<Map.Entry<Integer, String>> pages) {
        String argFileStr = "{\"documents\": " + documentsJson
                + ", \"plugins\": {\"back-references\": {}}}";
        UserError error = assertThrows(UserError.class, () -> simulateBuild(argFileStr, pages));
        String message = error.getMessage().toLowerCase();
        assertThat(message, containsString("referenced but not defined"));
        assertThat(message, containsString("missing"));
    }

    static Stream<Arguments> duplicateRefdefCases() {
        return Stream.of(
                Arguments.of(
                        "[{\"input\": \"def.txt\", \"output\": \"def.html\"}]",
                        Collections.singletonList(new AbstractMap.SimpleEntry<>(0,
                                "A <!--refdef foo One--> B <!--refdef foo Two-->"))),
                Arguments.of(
                        "[{\"input\": \"def1.txt\", \"output\": \"def1.html\"},"
                                + " {\"input\": \"def2.txt\", \"output\": \"def2.html\"}]",
                        Arrays.asList(
                                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo One-->"),
                                new AbstractMap.SimpleEntry<>(1, "<!--refdef foo Two-->")))
        );
    }

    @ParameterizedTest
    @MethodSource("duplicateRefdefCases")
    void duplicateRefdef_shouldFail(String documentsJson,
            List<Map.Entry<Integer, String>> pages) {
        String argFileStr = "{\"documents\": " + documentsJson
                + ", \"plugins\": {\"back-references\": {}}}";
        UserError error = assertThrows(UserError.class, () -> simulateBuild(argFileStr, pages));
        String message = error.getMessage().toLowerCase();
        assertThat(message, containsString("defined multiple times"));
        assertThat(message, containsString("foo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "See <!--ref foo compact-->.",
            "See <!--ref-->."
    })
    void refWrongFieldCount_shouldFail(String pageText) {
        String argFileStr =
                "{\"documents\": [{\"input\": \"ref.txt\", \"output\": \"ref.html\"}], "
                + "\"plugins\": {\"back-references\": {}}}";
        UserError error = assertThrows(UserError.class, () -> simulateBuild(argFileStr,
                Collections.singletonList(new AbstractMap.SimpleEntry<>(0, pageText))));
        assertTrue(error.getMessage().toLowerCase().contains("single word"));
    }

    @Test
    void multipleRefsToSameDefOnSamePage() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0,
                        "<!--refdef code1 [Example.com](https://example.com/)-->"),
                new AbstractMap.SimpleEntry<>(1,
                        "First <!--ref code1--> and second <!--ref code1-->.")));

        assertTrue(result.deferredPages.get(0));
        assertFalse(result.deferredPages.get(1));
        assertEquals(
                "First [Example.com](https://example.com/)"
                        + "<sup><a name=\"backref_ref_code1\"></a>"
                        + "<a class=\"ref\" href=\"def.html#backref_def_code1\">[code1]</a></sup>"
                        + " and second [Example.com](https://example.com/)"
                        + "<sup><a name=\"backref_ref_code1_1\"></a>"
                        + "<a class=\"ref\" href=\"def.html#backref_def_code1\">[code1]</a></sup>.",
                result.html.get(1));
        assertEquals(
                "<a name=\"backref_def_code1\"></a><span class=\"ref-def\">[code1]</span> "
                        + "[Example.com](https://example.com/)<sup>"
                        + "<a class=\"ref\" href=\"ref.html#backref_ref_code1\">1</a>, "
                        + "<a class=\"ref\" href=\"ref.html#backref_ref_code1_1\">2</a>"
                        + "</sup>",
                result.html.get(0));
    }

    @Test
    void refAndDefWithRelativeOutputPaths() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"defs/subdir_def.txt\", \"output\": \"defs/subdir_def.html\"},"
                + "  {\"input\": \"pages/subdir_ref.txt\", \"output\": \"pages/subdir_ref.html\"}"
                + "], \"plugins\": {\"back-references\": {}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0,
                        "<!--refdef subdir_example [Subdir Example](https://example.org/subdir/)-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref subdir_example-->.")));

        assertTrue(result.deferredPages.get(0));
        assertFalse(result.deferredPages.get(1));
        assertEquals(
                "See [Subdir Example](https://example.org/subdir/)"
                        + "<sup><a name=\"backref_ref_subdir_example\"></a>"
                        + "<a class=\"ref\" href=\"../defs/subdir_def.html#backref_def_subdir_example\">"
                        + "[subdir_example]</a></sup>.",
                result.html.get(1));
        assertEquals(
                "<a name=\"backref_def_subdir_example\"></a>"
                        + "<span class=\"ref-def\">[subdir_example]</span> "
                        + "[Subdir Example](https://example.org/subdir/)"
                        + "<sup><a class=\"ref\" href=\"../pages/subdir_ref.html#backref_ref_subdir_example\">"
                        + "1</a></sup>",
                result.html.get(0));
    }

    @Test
    void customRefTemplateAndCodePrefix() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"code-prefix\": \"xref_\","
                + "\"ref-formats\": [{"
                + "    \"markers\": [\"REF\"],"
                + "    \"template\": \"<span data-code=\\\"${code}\\\" data-anchor=\\\"${anchor}\\\" "
                + "href=\\\"${href}\\\">${content}</span>\""
                + "}]"
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->.")));

        assertEquals(
                "See <span data-code=\"foo\" data-anchor=\"xref_ref_foo\" "
                        + "href=\"def.html#xref_def_foo\">Foo display</span>.",
                result.html.get(1));
    }

    @Test
    void customDefTemplatesAndBackRefDelimiter() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref1.txt\", \"output\": \"ref1.html\"},"
                + "  {\"input\": \"ref2.txt\", \"output\": \"ref2.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"def-formats\": [{"
                + "    \"markers\": [\"REFDEF\"],"
                + "    \"template\": \"<div data-code=\\\"${code}\\\" id=\\\"${anchor}\\\">${content}"
                + "<sup>${back_refs_html}</sup></div>\","
                + "    \"back-ref-template\": \"<a href=\\\"${href}\\\">${link_text}</a>\","
                + "    \"back-ref-delimiter\": \"; \""
                + "}]"
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->."),
                new AbstractMap.SimpleEntry<>(2, "Also <!--ref foo-->.")));

        assertTrue(result.deferredPages.get(0));
        assertFalse(result.deferredPages.get(1));
        assertFalse(result.deferredPages.get(2));
        assertEquals(
                "<div data-code=\"foo\" id=\"backref_def_foo\">Foo display"
                        + "<sup><a href=\"ref1.html#backref_ref_foo\">1</a>; "
                        + "<a href=\"ref2.html#backref_ref_foo\">2</a></sup></div>",
                result.html.get(0));
    }

    @Test
    void twoRefMarkersUseRespectiveTemplates() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"defs.txt\", \"output\": \"defs.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"},"
                + "  {\"input\": \"cite.txt\", \"output\": \"cite.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"def-formats\": [{\"markers\": [\"REFDEF\"]}],"
                + "\"ref-formats\": ["
                + "    {\"markers\": [\"REF\"], \"template\": \"<ref>${code}</ref>\"},"
                + "    {\"markers\": [\"CITEREF\"], \"template\": \"<cite>${code}</cite>\"}"
                + "]"
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo-->\n<!--refdef bar Bar-->"),
                new AbstractMap.SimpleEntry<>(1, "Ref <!--ref foo-->."),
                new AbstractMap.SimpleEntry<>(2, "Cite <!--citeref bar-->.")));

        assertThat(result.markers, containsInAnyOrder("CITEREF", "REF", "REFDEF"));
        assertTrue(result.deferredPages.get(0));
        assertFalse(result.deferredPages.get(1));
        assertFalse(result.deferredPages.get(2));
        assertEquals("Ref <ref>foo</ref>.", result.html.get(1));
        assertEquals("Cite <cite>bar</cite>.", result.html.get(2));
    }

    @Test
    void emptyRefTemplate_rendersNothing() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"ref-formats\": [{\"markers\": [\"REF\"], \"template\": \"\"}]"
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->.")));

        assertEquals("See .", result.html.get(1));
    }

    @Test
    void partialDefFormat_inheritsDefaultBackRefTemplate() throws ArgFileParseException {
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"def-formats\": [{"
                + "    \"markers\": [\"REFDEF\"],"
                + "    \"template\": \"<wrap>${content}<sup>${back_refs_html}</sup></wrap>\""
                + "}]"
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->.")));

        assertEquals(
                "<wrap>Foo display"
                        + "<sup><a class=\"ref\" href=\"ref.html#backref_ref_foo\">1</a></sup></wrap>",
                result.html.get(0));
    }

    @Test
    void firstRun_whenCacheEnabled_createsCacheFile() throws Exception {
        String cacheFile = cacheFilePath("back_refs_cache.json");
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"cache\": \"" + cacheFile + "\""
                + "}}}";
        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->.")));
        result.plugin.finalizePlugin();

        Path cachePath = Paths.get(cacheFile);
        assertTrue(Files.exists(cachePath));
        Map<String, List<Map<String, Object>>> saved = OBJECT_MAPPER.readValue(cachePath.toFile(),
                new TypeReference<Map<String, List<Map<String, Object>>>>() {});
        assertThat(saved.keySet(), contains("foo"));
        assertEquals("ref.txt", saved.get("foo").get(0).get("input_file"));
        assertEquals(Collections.singletonList("backref_ref_foo"),
                saved.get("foo").get(0).get("anchor_ids"));
    }

    @Test
    void secondRun_whenRebuiltReferencer_preservesSkippedBackLinks() throws Exception {
        String cacheFile = cacheFilePath("back_refs_cache.json");
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref1.txt\", \"output\": \"ref1.html\"},"
                + "  {\"input\": \"ref2.txt\", \"output\": \"ref2.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"cache\": \"" + cacheFile + "\""
                + "}}}";
        simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo--> (one)."),
                new AbstractMap.SimpleEntry<>(2, "See <!--ref foo--> (two)."))).plugin.finalizePlugin();

        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo--> (one rebuilt)."),
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->")));

        assertEquals(
                "<a name=\"backref_def_foo\"></a><span class=\"ref-def\">[foo]</span> Foo display"
                        + "<sup><a class=\"ref\" href=\"ref2.html#backref_ref_foo\">1</a>, "
                        + "<a class=\"ref\" href=\"ref1.html#backref_ref_foo\">2</a></sup>",
                result.html.get(0));
    }

    @Test
    void secondRun_ifRefPageGainsSecondRef_thenDefListsBothAnchors() throws Exception {
        String cacheFile = cacheFilePath("back_refs_cache.json");
        String argFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref.txt\", \"output\": \"ref.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"cache\": \"" + cacheFile + "\""
                + "}}}";
        simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo-->."))).plugin.finalizePlugin();

        SimulateBuildResult result = simulateBuild(argFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(1, "First <!--ref foo--> and second <!--ref foo-->."),
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->")));

        assertEquals(
                "<a name=\"backref_def_foo\"></a><span class=\"ref-def\">[foo]</span> Foo display"
                        + "<sup><a class=\"ref\" href=\"ref.html#backref_ref_foo\">1</a>, "
                        + "<a class=\"ref\" href=\"ref.html#backref_ref_foo_1\">2</a></sup>",
                result.html.get(0));
    }

    @Test
    void secondRun_whenSmallerDocumentList_dropsRemovedReferencerAndPrunesCache() throws Exception {
        String cacheFile = cacheFilePath("back_refs_cache.json");
        String fullArgFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref1.txt\", \"output\": \"ref1.html\"},"
                + "  {\"input\": \"ref2.txt\", \"output\": \"ref2.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"cache\": \"" + cacheFile + "\""
                + "}}}";
        simulateBuild(fullArgFileStr, Arrays.asList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->"),
                new AbstractMap.SimpleEntry<>(1, "See <!--ref foo--> (one)."),
                new AbstractMap.SimpleEntry<>(2, "See <!--ref foo--> (two)."))).plugin.finalizePlugin();

        String smallerArgFileStr =
                "{\"documents\": ["
                + "  {\"input\": \"def.txt\", \"output\": \"def.html\"},"
                + "  {\"input\": \"ref1.txt\", \"output\": \"ref1.html\"}"
                + "], \"plugins\": {\"back-references\": {"
                + "\"cache\": \"" + cacheFile + "\""
                + "}}}";
        SimulateBuildResult result = simulateBuild(smallerArgFileStr, Collections.singletonList(
                new AbstractMap.SimpleEntry<>(0, "<!--refdef foo Foo display-->")));
        result.plugin.finalizePlugin();

        assertEquals(
                "<a name=\"backref_def_foo\"></a><span class=\"ref-def\">[foo]</span> Foo display"
                        + "<sup><a class=\"ref\" href=\"ref1.html#backref_ref_foo\">1</a></sup>",
                result.html.get(0));

        Map<String, List<Map<String, Object>>> saved = OBJECT_MAPPER.readValue(
                Paths.get(cacheFile).toFile(),
                new TypeReference<Map<String, List<Map<String, Object>>>>() {});
        assertTrue(saved.containsKey("foo"));
        List<String> inputFiles = saved.get("foo").stream()
                .map(entry -> (String) entry.get("input_file"))
                .collect(Collectors.toList());
        assertEquals(Collections.singletonList("ref1.txt"), inputFiles);
    }

    private String cacheFilePath(String fileName) {
        return tempDir.resolve(fileName).toString().replace('\\', '/');
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class PluginMarkers {
        private final BackReferencesPlugin plugin;
        private final List<String> markers;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class SimulateBuildResult {
        private final BackReferencesPlugin plugin;
        private final List<String> markers;
        private final Map<Integer, String> html;
        private final Map<Integer, Boolean> deferredPages;
    }
}
