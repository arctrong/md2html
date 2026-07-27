package world.md2html.pagemetadata;

import org.junit.jupiter.api.Test;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.testsupport.StubMetadataPlugin;
import world.md2html.utils.UserError;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static world.md2html.plugins.testsupport.StubMetadataPlugin.MarkerSpec.anywhere;
import static world.md2html.plugins.testsupport.StubMetadataPlugin.MarkerSpec.atPageStart;

class PageMetadataHandlersWrapperRegistrationTest {

    @Test
    void duplicateMarkersWithinOnePlugin() {
        UserError error = assertThrows(UserError.class, () -> register(
                new StubMetadataPlugin(anywhere("marker1"), anywhere("marker2"), anywhere("Marker1"))));
        assertDuplicateMarker(error, "MARKER1");
    }

    @Test
    void duplicateMarkersAcrossPlugins() {
        UserError error = assertThrows(UserError.class, () -> register(
                new StubMetadataPlugin(anywhere("shared")),
                new StubMetadataPlugin(anywhere("SHARED"))));
        assertDuplicateMarker(error, "SHARED");
    }

    @Test
    void duplicateMarkersCaseInsensitivelyWithinOnePlugin() {
        UserError error = assertThrows(UserError.class, () -> register(
                new StubMetadataPlugin(anywhere("red"), anywhere("RED"))));
        assertDuplicateMarker(error, "RED");
    }

    @Test
    void duplicateSameMarkerDifferentOnlyAtPageStart() {
        UserError error = assertThrows(UserError.class, () -> register(
                new StubMetadataPlugin(
                        atPageStart("VARIABLES"),
                        anywhere("variables"))));
        assertDuplicateMarker(error, "VARIABLES");
    }

    @Test
    void distinctMarkersAllowed() {
        assertDoesNotThrow(() -> register(
                new StubMetadataPlugin(anywhere("marker1"), anywhere("marker2"))));
    }

    private static void register(Md2HtmlPlugin... plugins) {
        PageMetadataHandlersWrapper.fromPlugins(Arrays.asList(plugins));
    }

    private static void assertDuplicateMarker(UserError error, String marker) {
        assertThat(error.getMessage(), allOf(
                containsStringIgnoringCase("duplication"),
                containsString(marker)));
    }
}
