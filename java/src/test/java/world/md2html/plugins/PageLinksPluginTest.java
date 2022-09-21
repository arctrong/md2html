package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static world.md2html.options.TestUtils.parseArgumentFile;

class PageLinksPluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private PageLinksPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findSinglePlugin(plugins, PageLinksPlugin.class);
    }

    @Test
    public void notActivated_no_plugin_def() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {}}",
                DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void notActivated_with_no_page_code() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {\"page-links\": {} }}",
                DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void minimal() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\", \"code\": \"page1\"} \n" +
                "], \n" +
                "\"plugins\": { \n" +
                "    \"page-links\": {} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "[](<!--page page1-->#anchor)";
        plugin.newPage(doc);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("[](page1.html#anchor)", processedPage);
    }

    @Test
    public void different_paths() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\", \"code\": \"page1\"}, \n" +
                "    {\"input\": \"subdir/page2.txt\", \"code\": \"page2\"} \n" +
                "], \n" +
                "\"plugins\": { \n" +
                "    \"page-links\": {} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document doc1 = argFile.getDocuments().get(0);
        Document doc2 = argFile.getDocuments().get(1);

        String pageText = "[](<!--page page2-->#anchor)";
        plugin.newPage(doc1);
        plugin.newPage(doc2);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("[](subdir/page2.html#anchor)", processedPage);

        pageText = "[](<!--page page1-->#anchor)";
        plugin.newPage(doc1);
        plugin.newPage(doc2);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc2);
        assertEquals("[](../page1.html#anchor)", processedPage);
    }

    @Test
    public void no_page_code_must_ignore() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\", \"code\": \"page1\"}, \n" +
                "    {\"input\": \"page2.txt\"} \n" +
                "], \n" +
                "\"plugins\": { \n" +
                "    \"page-links\": {} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document doc1 = argFile.getDocuments().get(0);

        String pageText = "[](<!--page page2-->#anchor)";
        plugin.newPage(doc1);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc1);
        assertEquals("[](<!--page page2-->#anchor)", processedPage);
    }

    @Test
    public void non_default_marker() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\", \"code\": \"page1\"}, \n" +
                "    {\"input\": \"page2.txt\", \"code\": \"page2\"}], \n" +
                "\"plugins\": { \n" +
                "    \"page-links\": {\"markers\": [\"marker1\"]} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document doc = argFile.getDocuments().get(0);

        String pageText = "[](<!--marker1 page2-->#anchor)";
        plugin.newPage(doc);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("[](page2.html#anchor)", processedPage);

        pageText = "[](<!--page page2-->#anchor)";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("[](<!--page page2-->#anchor)", processedPage);
    }

    @Test
    public void several_markers() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\", \"code\": \"page1\"}, \n" +
                "    {\"input\": \"page2.txt\", \"code\": \"page2\"}], \n" +
                "\"plugins\": { \n" +
                "    \"page-links\": {\"markers\": [\"marker1\", \"marker2\"]} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageLinksPlugin plugin = findSinglePlugin(argFile.getPlugins());
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document doc = argFile.getDocuments().get(0);

        String pageText = "[](<!--marker1 page2-->#anchor)";
        plugin.newPage(doc);
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("[](page2.html#anchor)", processedPage);

        pageText = "[](<!--marker2 page2-->#anchor)";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("[](page2.html#anchor)", processedPage);

        pageText = "[](<!--page page2-->#anchor)";
        plugin.newPage(doc);
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("[](<!--page page2-->#anchor)", processedPage);
    }

}
