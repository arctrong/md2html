package world.md2html.plugins;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.MetadataHandlersApplicationResult;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;
import world.md2html.utils.UserError;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.options.TestUtils.parseArgumentFile;

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
        assertEquals(Arrays.asList("REF", "REFDEF"), sorted(result.markers));
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
        assertEquals(Arrays.asList("BIBDEF", "CITEREF"), sorted(result.markers));
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
                Arguments.of("{\"ref-formats\": []}", false, Arrays.asList("REFDEF")),
                Arguments.of("{\"def-formats\": []}", false, Arrays.asList("REF")),
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
        assertEquals(sorted(expectedMarkers), sorted(result.markers));
    }

    private static List<String> sorted(List<String> markers) {
        List<String> copy = new ArrayList<>(markers);
        Collections.sort(copy);
        return copy;
    }

    private static class PluginMarkers {
        private final BackReferencesPlugin plugin;
        private final List<String> markers;

        private PluginMarkers(BackReferencesPlugin plugin, List<String> markers) {
            this.plugin = plugin;
            this.markers = markers;
        }
    }

    private static class SimulateBuildResult {
        private final BackReferencesPlugin plugin;
        private final List<String> markers;
        private final Map<Integer, String> html;
        private final Map<Integer, Boolean> deferredPages;

        private SimulateBuildResult(BackReferencesPlugin plugin, List<String> markers,
                Map<Integer, String> html, Map<Integer, Boolean> deferredPages) {
            this.plugin = plugin;
            this.markers = markers;
            this.html = html;
            this.deferredPages = deferredPages;
        }
    }
}
