package world.md2html.buildcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import world.md2html.options.model.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.OBJECT_WRITER;

class BuildCacheManagerTest {

    @TempDir
    Path tempDir;

    private BuildCacheManager cacheManager;
    private Path cacheFile;
    private Path argsFile;

    @BeforeEach
    void setUp() throws IOException {
        cacheManager = new BuildCacheManager();
        cacheFile = tempDir.resolve("test_cache.json");
        argsFile = tempDir.resolve("args.json");
        Files.createFile(argsFile);
        Files.setLastModifiedTime(argsFile, FileTime.fromMillis(500_000L));
    }

    @Test
    void noOp_whenDisabled() {
        cacheManager.initializeDisabled();
        cacheManager.recordPrimaryDocument("input.md", "output.html", false);
        cacheManager.recordDerivedDocumentForPrimary("input.md", "generated.html");
        cacheManager.recordStandaloneDerivedDocument("index.html");

        // Should not throw exceptions and should not create cache file
        assertFalse(Files.exists(cacheFile));
    }

    @Test
    void shouldRaiseException_whenUninitialized() {
        assertThrows(IllegalStateException.class, () ->
                cacheManager.recordPrimaryDocument("input.md", "output.html", false));
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, false",
            "true, true, true",
            "false, false, true"
    })
    void getForceAll_dependingOnCacheFileAndArgFile(boolean cacheFileExists,
            boolean argFileModified, boolean shouldForceAll) throws IOException {
        List<Document> documents = new ArrayList<>();
        documents.add(Document.builder().input("input1.txt").output("output1.html").build());

        if (cacheFileExists) {
            BuildCache testCache = BuildCache.builder()
                    .argFileMtime(argFileModified ? 490.0 : 500.0)
                    .build();
            testCache.getPrimaryDocuments().put("input1.txt",
                    PrimaryDocumentInfo.builder().outputFile("output1.html").build());
            OBJECT_WRITER.writeValue(cacheFile.toFile(), testCache);
        }

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        assertEquals(shouldForceAll, cacheManager.getForceAll(documents));
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, false",
            "true, false, true",
            "false, true, true"
    })
    void getForceAll_dependingOnDocumentSet(boolean newDocsAppeared, boolean someDocsRemoved,
            boolean shouldForceAll) throws IOException {
        BuildCache testCache = BuildCache.builder().argFileMtime(500).build();
        testCache.getPrimaryDocuments().put("input1.txt",
                PrimaryDocumentInfo.builder().outputFile("output1.html").build());

        OBJECT_WRITER.writeValue(cacheFile.toFile(), testCache);

        List<Document> testDocuments = new ArrayList<>();
        if (!someDocsRemoved) {
            testDocuments.add(Document.builder().input("input1.txt").output("output1.html")
                    .build());
        }
        if (newDocsAppeared) {
            testDocuments.add(Document.builder().input("input2.txt").output("output2.html")
                    .build());
        }

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        assertEquals(shouldForceAll, cacheManager.getForceAll(testDocuments));
    }

    @Test
    void recordPrimaryDocument_notSkipped() throws IOException {
        BuildCache emptyCache = BuildCache.builder().argFileMtime(500).build();
        OBJECT_WRITER.writeValue(cacheFile.toFile(), emptyCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        cacheManager.recordPrimaryDocument("input.md", "output.html", false);
        cacheManager.recordDependency("input.md", "included.txt");

        cacheManager.saveBuildCache();
        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        assertTrue(savedCache.getPrimaryDocuments().containsKey("input.md"));
        PrimaryDocumentInfo doc = savedCache.getPrimaryDocuments().get("input.md");
        assertEquals("output.html", doc.getOutputFile());
        assertTrue(doc.getDerivedDocuments().isEmpty());
        assertEquals(new HashSet<>(Arrays.asList("included.txt")), doc.getDependencies());
    }

    @Test
    void recordPrimaryDocument_skipped() throws IOException {
        BuildCache previousCache = BuildCache.builder().argFileMtime(500).build();
        PrimaryDocumentInfo prevDoc =
                PrimaryDocumentInfo.builder().outputFile("output_OLD.html").build();
        prevDoc.getDerivedDocuments().add("derived.html");
        prevDoc.getDependencies().add("included.txt");
        previousCache.getPrimaryDocuments().put("input.md", prevDoc);

        OBJECT_WRITER.writeValue(cacheFile.toFile(), previousCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        cacheManager.recordPrimaryDocument("input.md", "output.html", true);

        cacheManager.saveBuildCache();
        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        PrimaryDocumentInfo doc = savedCache.getPrimaryDocuments().get("input.md");
        assertEquals("output.html", doc.getOutputFile());
        assertTrue(doc.getDerivedDocuments().contains("derived.html"));
        assertEquals(new HashSet<>(Arrays.asList("included.txt")), doc.getDependencies());
    }

    @Test
    void recordDerivedDocumentForPrimary() throws IOException {
        BuildCache emptyCache = BuildCache.builder().argFileMtime(500).build();
        OBJECT_WRITER.writeValue(cacheFile.toFile(), emptyCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        cacheManager.recordDerivedDocumentForPrimary("input.md", "generated.html");

        cacheManager.saveBuildCache();
        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        PrimaryDocumentInfo primary = savedCache.getPrimaryDocuments().get("input.md");
        assertNotNull(primary);
        assertTrue(primary.getDerivedDocuments().contains("generated.html"));
    }

    @Test
    void recordStandaloneDerivedDocument() throws IOException {
        BuildCache emptyCache = BuildCache.builder().argFileMtime(500.0).build();
        OBJECT_WRITER.writeValue(cacheFile.toFile(), emptyCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        cacheManager.recordStandaloneDerivedDocument("index.html");

        cacheManager.saveBuildCache();
        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        assertTrue(savedCache.getStandaloneDerivedDocuments().contains("index.html"));
    }

    @Test
    void deleteObsoleteFiles() throws IOException {
        Path obsoleteFile = tempDir.resolve("obsolete.html");
        Path keepFile = tempDir.resolve("keep.html");
        Path obsoleteStandaloneFile = tempDir.resolve("obsolete_standalone.html");

        Files.createFile(obsoleteFile);
        Files.createFile(keepFile);
        Files.createFile(obsoleteStandaloneFile);

        BuildCache previousCache = BuildCache.builder().argFileMtime(500).build();
        previousCache.getPrimaryDocuments().put("input.md",
                PrimaryDocumentInfo.builder()
                        .outputFile(keepFile.toString())
                        .build());
        previousCache.getPrimaryDocuments().put("obsolete.md",
                PrimaryDocumentInfo.builder()
                        .outputFile(obsoleteFile.toString())
                        .build());
        previousCache.getStandaloneDerivedDocuments().add(obsoleteStandaloneFile.toString());

        OBJECT_WRITER.writeValue(cacheFile.toFile(), previousCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());
        cacheManager.recordPrimaryDocument("input.md", keepFile.toString(), false);

        cacheManager.deleteObsoleteFiles();

        assertFalse(Files.exists(obsoleteFile));
        assertTrue(Files.exists(keepFile));
        assertFalse(Files.exists(obsoleteStandaloneFile));
    }

    @Test
    void saveBuildCache() throws IOException {
        cacheManager.initialize(cacheFile.toString(), argsFile.toString());
        cacheManager.recordPrimaryDocument("doc1.md", "doc1.html", false);
        cacheManager.recordDerivedDocumentForPrimary("doc1.md", "my_code.html");

        cacheManager.saveBuildCache();

        assertTrue(Files.exists(cacheFile));
        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);
        assertEquals(500, savedCache.getArgFileMtime());
        assertTrue(savedCache.getPrimaryDocuments().containsKey("doc1.md"));
        PrimaryDocumentInfo primary = savedCache.getPrimaryDocuments().get("doc1.md");
        assertEquals("doc1.html", primary.getOutputFile());
        assertTrue(primary.getDerivedDocuments().contains("my_code.html"));
    }

    @Test
    void fullCacheWorkflow_simple() throws IOException {
        cacheManager.initialize(cacheFile.toString(), argsFile.toString());
        cacheManager.recordPrimaryDocument("doc1.md", "doc1.html", false);
        cacheManager.recordDerivedDocumentForPrimary("doc1.md", "my_code.html");
        cacheManager.deleteObsoleteFiles();
        cacheManager.saveBuildCache();

        assertTrue(Files.exists(cacheFile));
        BuildCache finalCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        assertTrue(finalCache.getPrimaryDocuments().containsKey("doc1.md"));
        PrimaryDocumentInfo primaryDoc = finalCache.getPrimaryDocuments().get("doc1.md");
        assertEquals("doc1.html", primaryDoc.getOutputFile());
        assertTrue(primaryDoc.getDerivedDocuments().contains("my_code.html"));
    }

    @Test
    void fullCacheWorkflow_withObsoleteFileCleanup() throws IOException {
        Path obsoleteFile = tempDir.resolve("old_doc.html");
        Path obsoleteStandalone = tempDir.resolve("old_standalone.html");
        Files.createFile(obsoleteFile);
        Files.createFile(obsoleteStandalone);

        BuildCache previousCache = BuildCache.builder().argFileMtime(500.0).build();
        previousCache.getPrimaryDocuments().put("old_doc.md",
                PrimaryDocumentInfo.builder().outputFile(obsoleteFile.toString()).build());
        previousCache.getStandaloneDerivedDocuments().add(obsoleteStandalone.toString());

        OBJECT_WRITER.writeValue(cacheFile.toFile(), previousCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        cacheManager.recordPrimaryDocument("doc1.md", "doc1.html", false);
        cacheManager.recordDerivedDocumentForPrimary("doc1.md", "my_code.html");

        cacheManager.deleteObsoleteFiles();

        assertFalse(Files.exists(obsoleteFile));
        assertFalse(Files.exists(obsoleteStandalone));

        cacheManager.saveBuildCache();

        assertTrue(Files.exists(cacheFile));
        BuildCache finalCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        assertEquals(1, finalCache.getPrimaryDocuments().size());
        assertTrue(finalCache.getPrimaryDocuments().containsKey("doc1.md"));
        assertFalse(finalCache.getPrimaryDocuments().containsKey("old_doc.md"));

        PrimaryDocumentInfo primaryDoc = finalCache.getPrimaryDocuments().get("doc1.md");
        assertEquals("doc1.html", primaryDoc.getOutputFile());
        assertTrue(primaryDoc.getDerivedDocuments().contains("my_code.html"));
    }

    @Test
    void recordDependency_writesIntoDocumentRecord() throws IOException {
        cacheManager.initialize(cacheFile.toString(), argsFile.toString());
        cacheManager.recordDependency("input.md", "dep1.txt");
        cacheManager.recordDependency("input.md", "dep2.txt");
        cacheManager.recordDependency("other.md", "other_dep.txt");

        cacheManager.saveBuildCache();
        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);

        assertEquals(new HashSet<>(Arrays.asList("dep1.txt", "dep2.txt")),
                savedCache.getPrimaryDocuments().get("input.md").getDependencies());
        assertEquals(new HashSet<>(Arrays.asList("other_dep.txt")),
                savedCache.getPrimaryDocuments().get("other.md").getDependencies());
    }

    @Test
    void hasStaleDependencies_whenDepIsNewer() throws IOException {
        Path depFile = tempDir.resolve("dep.txt");
        Files.createFile(depFile);
        Files.setLastModifiedTime(depFile, FileTime.from(Instant.ofEpochSecond(300)));

        BuildCache previousCache = BuildCache.builder().argFileMtime(500).build();
        PrimaryDocumentInfo prevDoc = PrimaryDocumentInfo.builder().outputFile("output.html")
                .build();
        prevDoc.getDependencies().add(depFile.toString().replace("\\", "/"));
        previousCache.getPrimaryDocuments().put("input.md", prevDoc);
        OBJECT_WRITER.writeValue(cacheFile.toFile(), previousCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        assertTrue(cacheManager.hasStaleDependencies("input.md", 200));
    }

    @Test
    void hasStaleDependencies_whenAllDepsAreOlder() throws IOException {
        Path depFile = tempDir.resolve("dep.txt");
        Files.createFile(depFile);
        Files.setLastModifiedTime(depFile, FileTime.from(Instant.ofEpochSecond(100)));

        BuildCache previousCache = BuildCache.builder().argFileMtime(500).build();
        PrimaryDocumentInfo prevDoc = PrimaryDocumentInfo.builder().outputFile("output.html")
                .build();
        prevDoc.getDependencies().add(depFile.toString().replace("\\", "/"));
        previousCache.getPrimaryDocuments().put("input.md", prevDoc);
        OBJECT_WRITER.writeValue(cacheFile.toFile(), previousCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        assertFalse(cacheManager.hasStaleDependencies("input.md", 200));
    }

    @Test
    void hasStaleDependencies_falseWhenNoDependencies() throws IOException {
        BuildCache previousCache = BuildCache.builder().argFileMtime(500).build();
        previousCache.getPrimaryDocuments().put("input.md",
                PrimaryDocumentInfo.builder().outputFile("output.html").build());
        OBJECT_WRITER.writeValue(cacheFile.toFile(), previousCache);

        cacheManager.initialize(cacheFile.toString(), argsFile.toString());

        assertFalse(cacheManager.hasStaleDependencies("input.md", 200));
    }

    @Test
    void hasStaleDependencies_noOpWithoutCache() {
        cacheManager.initializeDisabled();

        assertFalse(cacheManager.hasStaleDependencies("input.md", 200));
    }

    @Test
    void saveBuildCache_sortsDependencies() throws IOException {
        cacheManager.initialize(cacheFile.toString(), argsFile.toString());
        cacheManager.recordPrimaryDocument("input.md", "output.html", false);
        cacheManager.recordDependency("input.md", "b.txt");
        cacheManager.recordDependency("input.md", "a.txt");

        cacheManager.saveBuildCache();

        BuildCache savedCache = OBJECT_MAPPER.readValue(cacheFile.toFile(), BuildCache.class);
        SortedSet<String> dependencies =
                savedCache.getPrimaryDocuments().get("input.md").getDependencies();
        assertEquals(Arrays.asList("a.txt", "b.txt"), new ArrayList<>(dependencies));
    }
}
