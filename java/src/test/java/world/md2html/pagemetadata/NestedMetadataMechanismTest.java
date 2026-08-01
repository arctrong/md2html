package world.md2html.pagemetadata;

import org.junit.jupiter.api.Test;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.testsupport.DeferPhaseTestPlugin;
import world.md2html.plugins.testsupport.RecursiveExpandTestPlugin;
import world.md2html.testsupport.SimulateMetadataBuild;
import world.md2html.testsupport.SimulateMetadataBuildResult;
import world.md2html.testutils.PluginTestUtils;
import world.md2html.utils.UserError;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedMetadataMechanismTest {

    private static List<Md2HtmlPlugin> mechanismPlugins() {
        return Arrays.asList(new RecursiveExpandTestPlugin(), new DeferPhaseTestPlugin());
    }

    private static Map<Integer, String> runMechanismBuild(List<Map.Entry<Integer, String>> pages) {
        List<Integer> indices = new ArrayList<>();
        for (Map.Entry<Integer, String> page : pages) {
            indices.add(page.getKey());
        }
        SimulateMetadataBuildResult result = SimulateMetadataBuild.simulateMetadataBuild(
                mechanismPlugins(),
                pages,
                SimulateMetadataBuild.documentsForPageIndices(indices));
        return result.getOutput();
    }

    @Test
    void oneRecursionLevelThenDefer() {
        Map<Integer, String> output = runMechanismBuild(Collections.singletonList(
                new AbstractMap.SimpleEntry<>(0, "before <!--EXPAND foo--> after")));
        assertEquals("before resolved:foo after", output.get(0));
    }

    @Test
    void twoRecursionLevelsThenDefer() {
        Map<Integer, String> output = runMechanismBuild(Collections.singletonList(
                new AbstractMap.SimpleEntry<>(0, "<!--OUTER x-->")));
        assertEquals("A [C (resolved:x) D] B", output.get(0));
    }

    @Test
    void sameLevelTwoDeferredMarkers() {
        Map<Integer, String> output = runMechanismBuild(Collections.singletonList(
                new AbstractMap.SimpleEntry<>(0, "<!--TWIN [\"a\", \"b\"]-->")));
        assertEquals("start resolved:a mid resolved:b end", output.get(0));
    }

    @Test
    void sameLevelTwoDeferredMarkersOnPage() {
        List<Integer> indices = Collections.singletonList(0);
        SimulateMetadataBuildResult result = SimulateMetadataBuild.simulateMetadataBuild(
                Collections.singletonList(new DeferPhaseTestPlugin()),
                Collections.singletonList(new AbstractMap.SimpleEntry<>(
                        0, "start <!--DEFER a--> mid <!--DEFER b--> end")),
                SimulateMetadataBuild.documentsForPageIndices(indices));
        assertEquals("start resolved:a mid resolved:b end", result.getOutput().get(0));
    }

    @Test
    void unresolvedDeferRaisesOnPhase2Join() {
        UserError error = assertThrows(UserError.class, () -> runMechanismBuild(
                Collections.singletonList(new AbstractMap.SimpleEntry<>(
                        0, "See <!--EXPAND missing-->.") )));
        assertTrue(error.getMessage().toLowerCase().contains("referenced but not defined"));
    }

    @Test
    void deferredPageWithoutPhase2JoinStaysUnresolved() {
        MetadataHandlersApplicationResult result =
                SimulateMetadataBuild.wirePlugins(mechanismPlugins())
                        .applyMetadataHandlersWithResult(
                                "before <!--EXPAND foo--> after", PluginTestUtils.ANY_DOCUMENT);
        assertTrue(result.isDeferPage());
        ParsingResultItem deferredItem = result.getParsingResults().get(1);
        assertEquals("EXPAND", deferredItem.getMarker());
        assertTrue(deferredItem.getResult() instanceof MetadataHandlersApplicationResult);
        MetadataHandlersApplicationResult innerResult =
                (MetadataHandlersApplicationResult) deferredItem.getResult();
        long innerDeferredCount = innerResult.getParsingResults().stream()
                .filter(ParsingResultItem::isDeferred)
                .count();
        assertEquals(1, innerDeferredCount);
        assertEquals(DeferPhaseTestPlugin.MARKER, innerResult.getParsingResults().stream()
                .filter(ParsingResultItem::isDeferred)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected deferred inner marker"))
                .getMarker());
    }
}
