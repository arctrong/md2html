package world.md2html.options.argfile;

import org.junit.jupiter.api.Test;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.TestUtils.relativeToCurrentDir;

public class GlobsProcessingTest {

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

    private static final String THIS_DIR = relativeToCurrentDir(new File(GlobsProcessingTest
            .class.getProtectionDomain().getCodeSource().getLocation().getPath()).toPath());

    @Test
    public void test_minimal_scenario() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(
                "{\"documents\": [{\"input-glob\": \"" + THIS_DIR +
                        "for_globs_processing_test/*.txt\"}]}", DUMMY_CLI_OPTIONS);
        List<String> inputFilesToCheck = argFile.getDocuments().stream()
                .map(d -> d.getInput().substring(THIS_DIR.length())).collect(Collectors.toList());
        assertThat(inputFilesToCheck, containsInAnyOrder(
                "for_globs_processing_test/file02.txt",
                "for_globs_processing_test/file01.txt"));
    }

    @Test
    public void test_recursive() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input-glob\": \"" + THIS_DIR +
                        "for_globs_processing_test/**/*.txt\"}]}", DUMMY_CLI_OPTIONS);
        List<String> inputFilesToCheck = argFile.getDocuments().stream()
                .map(d -> d.getInput().substring(THIS_DIR.length())).collect(Collectors.toList());
        assertThat(inputFilesToCheck, containsInAnyOrder(
                "for_globs_processing_test/recursive/recursive_file01.txt",
                "for_globs_processing_test/file02.txt",
                "for_globs_processing_test/file01.txt"));
    }

    @Test
    public void test_sort_by_file_path() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input-glob\": \"" + THIS_DIR +
                        "for_globs_processing_test/**/*.txt\", " +
                        "\"sort-by-file-path\": true}]}", DUMMY_CLI_OPTIONS);
        List<String> inputFilesToCheck = argFile.getDocuments().stream()
                .map(d -> d.getInput().substring(THIS_DIR.length())).collect(Collectors.toList());
        assertThat(inputFilesToCheck, contains(
                "for_globs_processing_test/file01.txt",
                "for_globs_processing_test/file02.txt",
                "for_globs_processing_test/recursive/recursive_file01.txt"));
    }

    @Test
    public void test_sort_by_title() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input-glob\": \"" + THIS_DIR +
                "for_globs_processing_test/**/*.txt\", \n" +
                "    \"title-from-variable\": \"title\", \n" +
                "    \"sort-by-title\": true}], \n" +
                "\"plugins\": {\"page-variables\": {}} \n" +
                "}", DUMMY_CLI_OPTIONS);
        List<String> inputFilesToCheck = argFile.getDocuments().stream()
                .map(d -> d.getInput().substring(THIS_DIR.length())).collect(Collectors.toList());
        assertThat(inputFilesToCheck, contains("for_globs_processing_test/file02.txt",
                "for_globs_processing_test/recursive/recursive_file01.txt",
                "for_globs_processing_test/file01.txt"));
    }

    @Test
    public void test_sort_by_variable() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input-glob\": \"" + THIS_DIR +
                "for_globs_processing_test/**/*.txt\", \n" +
                "    \"sort-by-variable\": \"SORTORDER\"}], \n" +
                "\"plugins\": {\"page-variables\": {}} \n" +
                "}", DUMMY_CLI_OPTIONS);
        List<String> inputFilesToCheck = argFile.getDocuments().stream()
                .map(d -> d.getInput().substring(THIS_DIR.length())).collect(Collectors.toList());
        assertThat(inputFilesToCheck, contains(
                "for_globs_processing_test/file02.txt",
                "for_globs_processing_test/file01.txt",
                "for_globs_processing_test/recursive/recursive_file01.txt"));
    }

    @Test
    public void test_with_root_paths() throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile("{\"documents\": [{\"input-root\": \"" + THIS_DIR +
                "for_globs_processing_test\", " +
                "\"output-root\": \"dst_root\", \n" +
                "    \"input-glob\": \"**/*.txt\" \n" +
                "    }], \n" +
                "\"plugins\": {\"page-variables\": {}} \n" +
                "}", DUMMY_CLI_OPTIONS);

        List<String> inputFilesToCheck = argFile.getDocuments().stream()
                .map(d -> d.getInput().substring(THIS_DIR.length())).collect(Collectors.toList());
        assertThat(inputFilesToCheck, containsInAnyOrder(
                "for_globs_processing_test/file02.txt",
                "for_globs_processing_test/file01.txt",
                "for_globs_processing_test/recursive/recursive_file01.txt"));

        List<String> outputFilesToCheck = argFile.getDocuments().stream()
                .map(Document::getOutput).collect(Collectors.toList());
        assertThat(outputFilesToCheck, containsInAnyOrder(
                "dst_root/file02.html",
                "dst_root/file01.html",
                "dst_root/recursive/recursive_file01.html"));
    }

}
