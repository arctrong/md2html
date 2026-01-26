package world.md2html.buildcache;

import world.md2html.options.model.Document;
import world.md2html.utils.Logging;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.OBJECT_WRITER;

public class BuildCacheManager {

    private static final Logger log = Logging.getLogger();

    private boolean initialized = false;
    private BuildCache previousCache;
    private BuildCache currentCache;
    private double currentArgFileMTime = -1.0;
    private String buildCacheFile = "";
    private boolean buildCacheFileExistedOnStart = true;

    private boolean isDisabled() {
        if (!initialized) {
            throw new IllegalStateException("BuildCacheManager used before initialization.");
        }
        return previousCache == null;
    }

    public void initializeDisabled() {
        // This trick is used to fail-fast when the predefined execution order is broken.
        // Plugins and probably some other code may potentially start using the instance
        // of this class before it's initialized. As an uninitialized instance is fully
        // functional, the error may be raised too late or even cause silent incorrect
        // behavior.
        // TODO Revise the application workflow to provide a clean separation of
        //  the context setup (CLI and arg file parsing) and the main work
        //  (docs generation).
        initialized = true;
    }

    public void initialize(String buildCacheFile, String argumentFile) throws IOException {
        if (initialized) {
            throw new IllegalStateException("BuildCacheManager is already initialized.");
        }
        currentArgFileMTime = Utils.pythonLikeFileMTime(Paths.get(argumentFile));
        this.buildCacheFile = buildCacheFile;
        Path buildCacheFilePath = Paths.get(buildCacheFile);
        
        if (Files.exists(buildCacheFilePath)) {
            this.previousCache = OBJECT_MAPPER.readValue(buildCacheFilePath.toFile(),
                    BuildCache.class);
        } else {
            this.previousCache = getEmptyBuildCache(this.currentArgFileMTime);
            this.buildCacheFileExistedOnStart = false;
            log.info("Build cache file does not exist - all documents will be regenerated");
        }
        this.currentCache = getEmptyBuildCache(this.currentArgFileMTime);
        initialized = true;
    }

    public boolean getForceAll(List<Document> documents) {
        boolean forceAll = !buildCacheFileExistedOnStart;

        if (currentArgFileMTime > previousCache.getArgFileMtime()) {
            forceAll = true;
            log.info("Argument file changed - all documents will be regenerated");
        }

        Map<String, PrimaryDocumentInfo> prevDocuments = previousCache.getPrimaryDocuments();
        Map<String, Document> newDocuments = new LinkedHashMap<>();
        for (Document doc : documents) {
            newDocuments.put(doc.getInput(), doc);
        }
        Set<String> removedInputFiles = new LinkedHashSet<>(prevDocuments.keySet());

        for (String inputFile : prevDocuments.keySet()) {
            newDocuments.remove(inputFile);
        }
        for (Document document : documents) {
            removedInputFiles.remove(document.getInput());
        }

        if (!newDocuments.isEmpty()) {
            forceAll = true;
            if (log.isLoggable(Level.INFO)) {
                log.info("New source files appeared - all documents will be regenerated");
            }
        }
        if (!removedInputFiles.isEmpty()) {
            forceAll = true;
            if (log.isLoggable(Level.INFO)) {
                log.info("Some source files deleted - all documents will be regenerated");
            }
        }

        return forceAll;
    }

    public void recordPrimaryDocument(String inputFile, String outputFile, boolean skipped) {
        if (isDisabled()) {
            return;
        }
        PrimaryDocumentInfo newDoc = currentCache.getPrimaryDocuments().computeIfAbsent(inputFile,
                (k) -> PrimaryDocumentInfo.builder().outputFile(outputFile).build());
        newDoc.setOutputFile(outputFile);
        if (skipped) {
            PrimaryDocumentInfo oldDoc = previousCache.getPrimaryDocuments().get(inputFile);
            if (oldDoc != null && oldDoc.getDerivedDocuments() != null) {
                newDoc.getDerivedDocuments().addAll(oldDoc.getDerivedDocuments());
            }
        }
    }

    public void recordDerivedDocumentForPrimary(String primaryInput, String derivedOutput) {
        if (isDisabled()) {
            return;
        }
        PrimaryDocumentInfo newDoc =
                currentCache.getPrimaryDocuments().computeIfAbsent(primaryInput,
                        (k) -> PrimaryDocumentInfo.builder().outputFile("").build());
        newDoc.getDerivedDocuments().add(derivedOutput);
    }

    public void recordStandaloneDerivedDocument(String derivedOutput) {
        if (isDisabled()) {
            return;
        }
        currentCache.getStandaloneDerivedDocuments().add(derivedOutput);
    }

    public void deleteObsoleteFiles() throws IOException {
        if (isDisabled()) {
            return;
        }

        Set<String> newPrimaryOutputs = new HashSet<>();
        Set<String> newDerivedOutputs = new HashSet<>();
        for (PrimaryDocumentInfo docInfo : currentCache.getPrimaryDocuments().values()) {
            newPrimaryOutputs.add(docInfo.getOutputFile());
            newDerivedOutputs.addAll(docInfo.getDerivedDocuments());
        }

        Set<String> oldDerivedOutputs = new HashSet<>();
        for (PrimaryDocumentInfo prevDocInfo : previousCache.getPrimaryDocuments().values()) {
            if (prevDocInfo.getDerivedDocuments() != null) {
                oldDerivedOutputs.addAll(prevDocInfo.getDerivedDocuments());
            }
            String outputFile = prevDocInfo.getOutputFile();
            if (!newPrimaryOutputs.contains(outputFile)) {
                if (deleteFileIfExists(outputFile) && log.isLoggable(Level.INFO)) {
                    log.info("Unused primary file deleted: " + outputFile);
                }
            }
        }

        for (String oldDerivedOutput : oldDerivedOutputs) {
            if (!newDerivedOutputs.contains(oldDerivedOutput)) {
                if (deleteFileIfExists(oldDerivedOutput) && log.isLoggable(Level.INFO)) {
                    log.info("Obsolete derived file deleted: " + oldDerivedOutput);
                }
            }
        }

        if (previousCache.getStandaloneDerivedDocuments() != null) {
            Set<String> newStandaloneFiles = currentCache.getStandaloneDerivedDocuments();
            for (String oldStandaloneFile : previousCache.getStandaloneDerivedDocuments()) {
                if (!newStandaloneFiles.contains(oldStandaloneFile)) {
                    if (deleteFileIfExists(oldStandaloneFile) && log.isLoggable(Level.INFO)) {
                        log.info("Obsolete standalone derived file deleted: " + oldStandaloneFile);
                    }
                }
            }
        }
    }

    private boolean deleteFileIfExists(String file) throws IOException {
        Path path = Paths.get(file);
        if (Files.exists(path)) {
            Files.delete(path);
            return true;
        } else {
            return false;
        }
    }

    public void saveBuildCache() throws IOException {
        if (isDisabled()) {
            return;
        }
        OBJECT_WRITER.writeValue(Paths.get(buildCacheFile).toFile(), currentCache);
        if (log.isLoggable(Level.INFO)) {
            log.info("Build cache saved: " + buildCacheFile);
        }
    }

    private BuildCache getEmptyBuildCache(double argFileMtime) {
        return BuildCache.builder().argFileMtime(argFileMtime).build();
    }
}
