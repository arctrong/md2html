package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;
import world.md2html.utils.UserError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.options.TestUtils.parseArgumentFile;

class ReplacePluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private ReplacePlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findFirstElementOfType(plugins, ReplacePlugin.class);
    }

    @Test
    public void notActivated_no_plugin_def() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], \"plugins\": {}}",
                DUMMY_CLI_OPTIONS);
        ReplacePlugin plugin = findSinglePlugin(argFile.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void test_single_value() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                        "\"plugins\": { \n" +
                        "    \"replace\": [\n" +
                        "        {\"markers\": [\"marker1\"], \"replace-with\": \"[[${1}]]\"} \n" +
                        "    ]\n" +
                        "}}", DUMMY_CLI_OPTIONS);

        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--marker1  some context  --> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning [[some context  ]] ending", processedPage);
    }

    @Test
    public void test_several_values() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                        "\"plugins\": { \n" +
                        "    \"replace\": [\n" +
                        "        {\"markers\": [\"marker1\"], \"replace-with\": \"[[${1}-${2}]]\"}\n" +
                        "    ]\n" +
                        "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--marker1 [\"A\", \"B\"]--> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning [[A-B]] ending", processedPage);

        pageText = "beginning <!--marker1 [\"C\", \"D\"]--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning [[C-D]] ending", processedPage);
    }

    @Test
    public void test_several_markers() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                        "\"plugins\": { \n" +
                        "    \"replace\": [\n" +
                        "        {\"markers\": [\"marker1\", \"marker2\"], \"replace-with\": \"[[${1}]]\"}\n" +
                        "    ]\n" +
                        "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--marker1 some-value--> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning [[some-value]] ending", processedPage);

        pageText = "beginning <!--marker2 some-value--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning [[some-value]] ending", processedPage);
    }

    @Test
    public void test_several_instances() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                "\"plugins\": { \n" +
                "    \"replace\": [\n" +
                "        {\"markers\": [\"marker1\"], \"replace-with\": \"s1 ${1} e1\"},\n" +
                "        {\"markers\": [\"marker2\"], \"replace-with\": \"s2 ${1} e2\"}\n" +
                "    ] \n" +
                "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--marker1 VALUE--> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning s1 VALUE e1 ending", processedPage);

        pageText = "beginning <!--marker2 VALUE--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning s2 VALUE e2 ending", processedPage);
    }

    @Test
    public void test_recursive() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                        "\"plugins\": { \n" +
                        "    \"replace\": [\n" +
                        "        {\"markers\": [\"m1\"], \"replace-with\": \"${1} m1\", \"recursive\": false},\n" +
                        "        {\"markers\": [\"m2\"], \"replace-with\": \"${1} m2 <!--m1 v1-->\", \"recursive\": true},\n" +
                        "        {\"markers\": [\"m3\"], \"replace-with\": \"${1} m3 <!--m1 v1-->\"}\n" +
                        "    ] \n" +
                        "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--m2 V2--> ending";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning V2 m2 v1 m1 ending", processedPage);

        pageText = "beginning <!--m3 V3--> ending";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("beginning V3 m3 <!--m1 v1--> ending", processedPage);
    }

    @Test
    public void test_recursive_direct_cycle_must_fail() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                        "\"plugins\": { \n" +
                        "    \"replace\": [\n" +
                        "        {\"markers\": [\"m1\"], \"replace-with\": \"${1} <!--m1 v1-->\", \"recursive\": true}\n" +
                        "    ] \n" +
                        "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--m1 V1--> ending";

        UserError e = assertThrows(UserError.class,
                () -> metadataHandlers.applyMetadataHandlers(pageText, doc));
        String message = e.getMessage().toUpperCase();
        assertTrue(message.contains("CYCLE"));
        assertTrue(message.contains("M1"));
    }

    @Test
    public void test_recursive_indirect_cycle_must_fail() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"page1.txt\"}], \n" +
                        "\"plugins\": { \n" +
                        "    \"replace\": [\n" +
                        "        {\"markers\": [\"m1\"], \"replace-with\": \"${1} <!--m2 v2-->\", \"recursive\": true},\n" +
                        "        {\"markers\": [\"m2\"], \"replace-with\": \"${1} <!--m3 v3-->\", \"recursive\": true},\n" +
                        "        {\"markers\": [\"m3\"], \"replace-with\": \"${1} <!--m1 v1-->\", \"recursive\": true}\n" +
                        "    ] \n" +
                        "}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String pageText = "beginning <!--m1 V1--> ending";

        UserError e = assertThrows(UserError.class,
                () -> metadataHandlers.applyMetadataHandlers(pageText, doc));
        String message = e.getMessage().toUpperCase();
        assertTrue(message.contains("CYCLE"));
        assertTrue(message.contains("M1,M2,M3"));
    }
}
