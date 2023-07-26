package world.md2html.plugins;

import org.junit.jupiter.api.Test;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.testutils.PluginTestUtils;
import world.md2html.utils.UserError;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.TestUtils.relativeToCurrentDir;

class IncludeFilePluginTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private static final String THIS_DIR = relativeToCurrentDir(new File(IncludeFilePluginTest
            .class.getProtectionDomain().getCodeSource().getLocation().getPath()).toPath());

    private IncludeFilePlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findFirstElementOfType(plugins, IncludeFilePlugin.class);
    }

    @Test
    public void test_notActivated_no_plugin_def() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], \"plugins\": {}}",
                DUMMY_CLI_OPTIONS).getPlugins();
        IncludeFilePlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void test_with_empty_plugin_def_must_raise_error() {
        ArgFileParseException e = assertThrows(ArgFileParseException.class,
                () -> parseArgumentFile(
                        "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                                "\"plugins\": {\"include-file\": [] }}",
                        DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().contains("IncludeFilePlugin"));
        assertTrue(e.getMessage().contains("validation"));
    }

    @Test
    public void test_minimal() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                        "\"plugins\": {" +
                        "\"include-file\": [" +
                        "    {\"markers\": [\"marker1\"], " +
                        "     \"root-dir\": \"" + THIS_DIR + "for_include_file_plugin_test/\"}" +
                        "]}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String  pageText = "before <!--marker1  code1.txt --> after";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before Sample text 1 after", processedPage);
    }

    @Test
    public void test_with_untrimmed() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                        "\"plugins\": {" +
                        "\"include-file\": [" +
                        "    {\"markers\": [\"marker1\"], " +
                        "     \"root-dir\": \"" + THIS_DIR + "for_include_file_plugin_test/\"," +
                        "     \"trim\": false" +
                        "    }" +
                        "]}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());
        String  pageText = "before <!--marker1  code1.txt --> after";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before \nSample text 1\n\n after", processedPage
                .replace("\r\n", "\n")
                .replace("\r", "\n"));
    }

    @Test
    public void test_several_markers() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                        "\"plugins\": {" +
                        "\"include-file\": [" +
                        "    {\"markers\": [\"marker1\", \"marker2\"], " +
                        "     \"root-dir\": \"" + THIS_DIR + "for_include_file_plugin_test/\"}" +
                        "]}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String  pageText = "before <!--marker1  code1.txt --> after";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before Sample text 1 after", processedPage);

         pageText = "before <!--marker2  code1.txt --> after";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before Sample text 1 after", processedPage);
    }

    @Test
    public void test_several_root_dirs() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                        "\"plugins\": {" +
                        "\"include-file\": [" +
                        "    {\"markers\": [\"marker1\"], " +
                        "     \"root-dir\": \"" + THIS_DIR + "for_include_file_plugin_test/\"" +
                        "    }," +
                        "    {\"markers\": [\"marker2\"], " +
                        "     \"root-dir\": \"" + THIS_DIR + "for_include_file_plugin_test/folder1/\"" +
                        "    }" +
                        "]}}", DUMMY_CLI_OPTIONS);
        Document doc = argFile.getDocuments().get(0);
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        String  pageText = "before <!--marker1  code1.txt --> after";
        String processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before Sample text 1 after", processedPage);

         pageText = "before <!--marker2  code2.txt --> after";
        processedPage = metadataHandlers.applyMetadataHandlers(pageText, doc);
        assertEquals("before Sample text 2 after", processedPage);
    }

    @Test
    public void test_with_duplicate_markers_must_raise_error() throws ArgFileParseException {
        UserError e = assertThrows(UserError.class,
                () -> parseArgumentFile(
                        "{\"documents\": [{\"input\": \"whatever.txt\"}], " +
                                "\"plugins\": {" +
                                "\"include-file\": [" +
                                "    {\"markers\": [\"marker1\"], \"root-dir\": \"whatever/path1\" }," +
                                "    {\"markers\": [\"marker2\", \"Marker1\"], \"root-dir\": \"whatever/path2\" }" +
                                "]}}", DUMMY_CLI_OPTIONS));
        assertTrue(e.getMessage().contains("duplication"));
        assertTrue(e.getMessage().contains("MARKER1"));
    }
}
