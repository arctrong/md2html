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

class IgnorePluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private IgnorePlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findSinglePlugin(plugins, IgnorePlugin.class);
    }

    @Test
    public void notActivated_no_plugin_def() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {}}",
                DUMMY_CLI_OPTIONS);
        IgnorePlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void minimal() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\"} ], \n" +
                "\"plugins\": { \n" +
                "    \"ignore\": {} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning<!--ignore \t  some context-->ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning<!--some context-->ending", processedPage);
    }

    @Test
    public void non_default_marker() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\"} ], \n" +
                "\"plugins\": { \n" +
                "    \"ignore\": {\"markers\": [\"marker1\"]} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document doc = argFile.getDocuments().get(0);

        String pageText = "beginning <!--marker1 some context--> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning <!--some context--> ending", processedPage);

        pageText = "beginning <!--ignore some context--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning <!--ignore some context--> ending", processedPage);
    }

    @Test
    public void several_markers() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [ \n" +
                "    {\"input\": \"page1.txt\"} ], \n" +
                "\"plugins\": { \n" +
                "    \"ignore\": {\"markers\": [\"marker1\", \"marker2\"]} \n" +
                "}}", DUMMY_CLI_OPTIONS);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        Document doc = argFile.getDocuments().get(0);

        String pageText = "beginning <!--marker1 some context--> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning <!--some context--> ending", processedPage);

        pageText = "beginning <!--marker2 some context--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning <!--some context--> ending", processedPage);

        pageText = "beginning <!--ignore some context--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning <!--ignore some context--> ending", processedPage);
    }

}
