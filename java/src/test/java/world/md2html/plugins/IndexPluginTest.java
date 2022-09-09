package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.options.TestUtils.parseArgumentFile;

class IndexPluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private IndexPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findSinglePlugin(plugins, IndexPlugin.class);
    }

    @Test
    public void test_notActivated_no_plugin_def() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {}}",
                DUMMY_CLI_OPTIONS).getPlugins();
        IndexPlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void test_notActivated_with_empty_plugin_def() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {\"index\": {} }}",
                DUMMY_CLI_OPTIONS).getPlugins();
        IndexPlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void test_minimal() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                        "\"plugins\": {" +
                        "\"index\": {\"index\": {\"output\": \"index_page.html\", " +
                        "\"index-cache\": \"index_cache.json\"}}" +
                        "}}", DUMMY_CLI_OPTIONS);
        IndexPlugin plugin = findSinglePlugin(argFile.getPlugins());
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String  pageText = "before <!--index entry 1--> after";
        plugin.newPage(doc);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertNotEquals(processedPage, pageText);

        pageText = "before <!--index1 entry 1--> after";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals(processedPage, pageText);

        pageText = "before <!--index [\"entry 1\", \"entry 2\"] --> after";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertNotEquals(processedPage, pageText);
    }

    @Test
    public void test_several_indexes() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                        "\"plugins\": {" +
                        "\"index\": {\"index1\": {\"output\": \"index_page1.html\", " +
                        "                         \"index-cache\": \"cache1.json\"}, " +
                        "          \"index2\": {\"output\": \"index_page2.html\", " +
                        "                       \"index-cache\": \"cache2.json\"}" +
                        "         }" +
                        "}}", DUMMY_CLI_OPTIONS);
        IndexPlugin plugin = findSinglePlugin(argFile.getPlugins());
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String  pageText = "before <!--index1 entry 1--> after";
        plugin.newPage(doc);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertNotEquals(processedPage, pageText);

         pageText = "before <!--index2 entry 1--> after";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertNotEquals(processedPage, pageText);

        pageText = "before <!--index5 entry 1--> after";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals(processedPage, pageText);
    }

}
