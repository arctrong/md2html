package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.model.ArgFileOptions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static world.md2html.plugins.PluginTestUtils.documentWithOutputLocation;

class RelativePathsPluginTest {

    private RelativePathsPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return (RelativePathsPlugin) PluginTestUtils.findSinglePlugin(plugins,
                RelativePathsPlugin.class);
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {}}", null);
        RelativePathsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void relativization() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {\"relative-paths\": { " +
                "\"down1\": \"down1/\", \"down11\": \"down1/down11/\", " +
                "\"down2\": \"down2/\", \"down22\": \"down2/down22/\", " +
                "\"root\": \"\", \"up1\": \"../\", \"up2\": \"../../\" }}}", null);
        RelativePathsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

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
