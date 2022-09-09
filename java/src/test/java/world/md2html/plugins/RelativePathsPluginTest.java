package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.testutils.PluginTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.PluginTestUtils.documentWithOutputLocation;

class RelativePathsPluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private RelativePathsPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findSinglePlugin(plugins, RelativePathsPlugin.class);
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {}}", DUMMY_CLI_OPTIONS);
        RelativePathsPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void relativization() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {\"relative-paths\": { " +
                "\"down1\": \"down1/\", \"down11\": \"down1/down11/\", " +
                "\"down2\": \"down2/\", \"down22\": \"down2/down22/\", " +
                "\"root\": \"\", \"up1\": \"../\", \"up2\": \"../../\" }}}", DUMMY_CLI_OPTIONS);
        RelativePathsPlugin plugin = findSinglePlugin(argFile.getPlugins());

        Map<String, Object> relPaths = plugin.variables(documentWithOutputLocation("root.html"));
        assertEquals("down1/", relPaths.get("down1"));
        assertEquals("down1/down11/", relPaths.get("down11"));
        assertEquals("down2/", relPaths.get("down2"));
        assertEquals("down2/down22/", relPaths.get("down22"));
        assertEquals("", relPaths.get("root"));
        assertEquals("../", relPaths.get("up1"));
        assertEquals("../../", relPaths.get("up2"));

        relPaths = plugin.variables(documentWithOutputLocation("down1/doc.html"));
        assertEquals("", relPaths.get("down1"));
        assertEquals("down11/", relPaths.get("down11"));
        assertEquals("../down2/", relPaths.get("down2"));
        assertEquals("../down2/down22/", relPaths.get("down22"));
        assertEquals("../", relPaths.get("root"));
        assertEquals("../../", relPaths.get("up1"));
        assertEquals("../../../", relPaths.get("up2"));

        relPaths = plugin.variables(documentWithOutputLocation("down2/down22/doc.html"));
        assertEquals("../../down1/", relPaths.get("down1"));
        assertEquals("../../down1/down11/", relPaths.get("down11"));
        assertEquals("../", relPaths.get("down2"));
        assertEquals("", relPaths.get("down22"));
        assertEquals("../../", relPaths.get("root"));
        assertEquals("../../../", relPaths.get("up1"));
        assertEquals("../../../../", relPaths.get("up2"));
    }

}
