package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
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
        return PluginTestUtils.findFirstElementOfType(plugins, RelativePathsPlugin.class);
    }

    @Test
    public void notDefined() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": " +
                "[{\"input\": \"whatever.md\"}], \"plugins\": {}}", DUMMY_CLI_OPTIONS);
        RelativePathsPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void notActivated_newSyntax() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"whatever.md\"}], " +
                "\"plugins\": {" +
                "    \"relative-paths\": {\"markers\": [\"p1\", \"p2\"], \"paths\": {}}" +
                "}}", DUMMY_CLI_OPTIONS);
        RelativePathsPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void minimal_newSyntax() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"whatever.md\"}], " +
                "\"plugins\": {" +
                "    \"relative-paths\": {\"markers\": [\"path\"], " +
                "        \"paths\": {\"pict\": \"doc/pict/\"}}" +
                "}}", DUMMY_CLI_OPTIONS);
        RelativePathsPlugin plugin = findSinglePlugin(argFile.getPlugins());
        Map<String, Object> relPaths = plugin.variables(documentWithOutputLocation("root.html"));
        assertEquals("doc/pict/", relPaths.get("pict"));
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

    @Test
    public void substitution_minimal() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                "\"plugins\": { \n" +
                "    \"relative-paths\": {\"markers\": [\"path1\"], " +
                "        \"paths\": {\"pict1\": \"doc/pict/\"}} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "![](<!--path1 pict1-->img1.png)";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("![](doc/pict/img1.png)", processedPage);
    }

    @Test
    public void different_paths() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\"}, \n" +
                "    {\"input\": \"subdir/page2.txt\"} \n" +
                "], \n" +
                "\"plugins\": { \n" +
                "    \"relative-paths\": {\"markers\": [\"path2\"], " +
                "        \"paths\": {\"pict2\": \"doc/pict/\"}} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        Document doc1 = argFile.getDocuments().get(0);
        Document doc2 = argFile.getDocuments().get(1);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "![](<!--path2 pict2-->img2.png)";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("![](doc/pict/img2.png)", processedPage);

        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc2);
        assertEquals("![](../doc/pict/img2.png)", processedPage);
    }

    @Test
    public void unknown_path_must_ignore() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                "\"plugins\": { \n" +
                "    \"relative-paths\": {\"markers\": [\"path2\"], " +
                "        \"paths\": {\"pict2\": \"doc/pict/\"}} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        Document doc1 = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "![](<!--path2 unknown-->img2.png)";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("![](<!--path2 unknown-->img2.png)", processedPage);
    }

    @Test
    public void several_markers() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                "\"plugins\": { \n" +
                "    \"relative-paths\": {\"markers\": [\"p1\", \"p2\"], \n" +
                "        \"paths\": {\"pict2\": \"doc/pict/\", " +
                "                    \"pict3\": \"doc/layout/pict/\"}} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        Document doc1 = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "![](<!--p1 pict2-->img.png)";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("![](doc/pict/img.png)", processedPage);

        pageText = "![](<!--p2 pict2-->img.png)";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("![](doc/pict/img.png)", processedPage);

        pageText = "![](<!--p1 pict3-->img.png)";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("![](doc/layout/pict/img.png)", processedPage);

        pageText = "![](<!--p2 pict3-->img.png)";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("![](doc/layout/pict/img.png)", processedPage);
    }
}
