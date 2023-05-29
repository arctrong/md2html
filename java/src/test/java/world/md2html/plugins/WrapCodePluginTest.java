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

class WrapCodePluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private WrapCodePlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findSinglePlugin(plugins, WrapCodePlugin.class);
    }

    @Test
    public void test_notActivated_no_plugin_def() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {}}",
                DUMMY_CLI_OPTIONS).getPlugins();
        WrapCodePlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void test_notActivated_with_empty_plugin_def() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {\"wrap-code\": {} }}",
                DUMMY_CLI_OPTIONS).getPlugins();
        WrapCodePlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void test_minimal() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                "\"plugins\": {" +
                "\"wrap-code\": {" +
                "    \"marker1\": {\"input-root\": \"input/path/\", " +
                "                  \"output-root\": \"output/path/\"}" +
                "}}}", DUMMY_CLI_OPTIONS);
        WrapCodePlugin plugin = findSinglePlugin(argFile.getPlugins());
        plugin.setDryRun(true);

        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String  pageText = "before <!--marker1  path/to/file.csv --> after";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before output/path/path/to/file.csv.html after", processedPage);
    }

    @Test
    public void test_several_markers() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                "\"plugins\": {" +
                "\"wrap-code\": {" +
                "    \"marker1\": {\"input-root\": \"input/path1/\", " +
                "                  \"output-root\": \"output/path1/\"}," +
                "    \"marker2\": {\"input-root\": \"input/path2/\", " +
                "                  \"output-root\": \"output/path2/\"}" +
                "}}}", DUMMY_CLI_OPTIONS);
        WrapCodePlugin plugin = findSinglePlugin(argFile.getPlugins());
        plugin.setDryRun(true);

        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "before <!--marker1  path/to/file1.csv --> after";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before output/path1/path/to/file1.csv.html after", processedPage);

        pageText = "before <!--marker2  path/to/file2.csv --> after";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before output/path2/path/to/file2.csv.html after", processedPage);
    }

    @Test
    public void test_repeated_source() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                "\"plugins\": {" +
                "\"wrap-code\": {" +
                "    \"marker1\": {\"input-root\": \"input/path/\", " +
                "                  \"output-root\": \"output/path/\"}" +
                "}}}", DUMMY_CLI_OPTIONS);
        WrapCodePlugin plugin = findSinglePlugin(argFile.getPlugins());
        plugin.setDryRun(true);

        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "<!--marker1 path/to/file.csv--> <!--marker1 path/to/file.csv-->";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("output/path/path/to/file.csv.html output/path/path/to/file.csv.html",
                processedPage);
    }
}
