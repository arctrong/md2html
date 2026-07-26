package world.md2html.pagemetadata;

import org.junit.jupiter.api.Test;
import world.md2html.options.model.Document;
import world.md2html.plugins.testsupport.DeferPhaseTestPlugin;
import world.md2html.testutils.PluginTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataPhase2Test {

    private PageMetadataHandlersWrapper createWrapper() {
        DeferPhaseTestPlugin plugin = new DeferPhaseTestPlugin();
        plugin.activate();
        return PageMetadataHandlersWrapper.fromPlugins(
                Collections.singletonList(plugin));
    }

    @Test
    void immediateMetadataIsProcessedInOnePass() {
        PageMetadataHandlersWrapper wrapper = createWrapper();
        Document doc = PluginTestUtils.ANY_DOCUMENT;

        MetadataHandlersApplicationResult result = wrapper.applyMetadataHandlersWithResult(
                "before <!--DEFER_TEST immediate--> after", doc);

        assertFalse(result.isDeferPage());
        assertEquals("before immediate after",
                wrapper.joinParsingResults(result.getParsingResults(), doc));
    }

    @Test
    void deferredMetadataDefersPageUntilJoin() {
        PageMetadataHandlersWrapper wrapper = createWrapper();
        Document doc = PluginTestUtils.ANY_DOCUMENT;

        MetadataHandlersApplicationResult result = wrapper.applyMetadataHandlersWithResult(
                "before <!--DEFER_TEST defer--> after", doc);

        assertTrue(result.isDeferPage());
        assertEquals("before resolved:defer after",
                wrapper.joinParsingResults(result.getParsingResults(), doc));
    }

    @Test
    void applyMetadataHandlersJoinsImmediatelyWhenNotDeferred() {
        PageMetadataHandlersWrapper wrapper = createWrapper();
        Document doc = PluginTestUtils.ANY_DOCUMENT;

        String processed = wrapper.applyMetadataHandlers(
                "before <!--DEFER_TEST immediate--> after", doc);

        assertEquals("before immediate after", processed);
    }
}
