package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import world.md2html.buildcache.BuildCacheManager;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.MetadataProcessingPhase;
import world.md2html.pagemetadata.MetadataProcessingResult;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.PageMetadataHandler.PageMetadataException;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.UniqueIndexer;
import world.md2html.utils.UserError;
import world.md2html.utils.VariableReplacer;
import world.md2html.utils.VariableReplacer.VariableReplacerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;

public class BackReferencesPlugin extends AbstractMd2HtmlPlugin {

    private static final Pattern DEF_METADATA_PATTERN = Pattern.compile("([^\\s]+)\\s+(.*)",
            Pattern.DOTALL);

    private static final Map<String, Object> DEFAULT_DEF_FORMAT = defFormatDefaults();
    private static final Map<String, Object> DEFAULT_REF_FORMAT = refFormatDefaults();
    private static final String DEFAULT_CODE_PREFIX = "backref_";

    private static Map<String, Object> defFormatDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("markers", Collections.singletonList("REFDEF"));
        defaults.put("template",
                "<a name=\"${anchor}\"></a><span class=\"ref-def\">[${code}]</span> ${content}"
                        + "<sup>${back_refs_html}</sup>");
        defaults.put("back-ref-template",
                "<a class=\"ref\" href=\"${href}\">${link_text}</a>");
        defaults.put("back-ref-delimiter", ", ");
        return defaults;
    }

    private static Map<String, Object> refFormatDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("markers", Collections.singletonList("REF"));
        defaults.put("template",
                "${content}<sup><a name=\"${anchor}\"></a><a class=\"ref\" "
                        + "href=\"${href}\">[${code}]</a></sup>");
        return defaults;
    }

    @Getter
    @RequiredArgsConstructor
    private static class DefFormatConfig {
        private final List<String> markers;
        private final VariableReplacer template;
        private final VariableReplacer backRefTemplate;
        private final String backRefDelimiter;
    }

    @Getter
    @RequiredArgsConstructor
    private static class RefFormatConfig {
        private final List<String> markers;
        private final VariableReplacer template;
    }

    @Getter
    @RequiredArgsConstructor
    private static class PageLocation {
        private final String inputFile;
        private final String outputFile;
    }

    @Getter
    @RequiredArgsConstructor
    private static class Definition {
        private final PageLocation page;
        private final String anchorId;
        private final String refContent;
        private final DefFormatConfig defFormat;
    }

    @Getter
    @RequiredArgsConstructor
    private static class Reference {
        private final PageLocation page;
        private final String anchorId;
    }

    private final BuildCacheManager buildCacheManager;

    private final Map<String, Definition> definitions = new HashMap<>();
    private final Map<String, Map<String, List<Reference>>> references = new HashMap<>();
    private List<DefFormatConfig> defFormats;
    private List<RefFormatConfig> refFormats;
    private String codePrefix;
    private UniqueIndexer uniqueIndexer;
    private String backrefsCacheFile;
    private Map<String, Map<String, Map<String, Object>>> backrefsCache;
    private Set<String> documentInputs;
    private final Map<String, Set<String>> reverseMapDefinitionsByPage = new HashMap<>();
    private final Map<String, Set<String>> reverseMapReferencesByPage = new HashMap<>();
    private final List<PageMetadataHandlerInfo> handlerInfos = new ArrayList<>();

    public BackReferencesPlugin(BuildCacheManager buildCacheManager) {
        this.buildCacheManager = buildCacheManager;
    }

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/back_references_schema.json");

        codePrefix = DEFAULT_CODE_PREFIX;
        defFormats = parseDefFormats(data);
        refFormats = parseRefFormats(data);

        JsonNode codePrefixNode = data.get("code-prefix");
        if (codePrefixNode != null && !codePrefixNode.isNull()) {
            codePrefix = codePrefixNode.asText();
        }

        JsonNode cacheNode = data.get("cache");
        if (cacheNode != null && !cacheNode.isNull()) {
            backrefsCacheFile = cacheNode.asText().replace("\\", "/");
        }

        for (DefFormatConfig format : defFormats) {
            DefMetadataHandler handler = new DefMetadataHandler(format);
            for (String marker : format.getMarkers()) {
                handlerInfos.add(new PageMetadataHandlerInfo(handler, marker, false));
            }
        }
        for (RefFormatConfig format : refFormats) {
            RefMetadataHandler handler = new RefMetadataHandler(format);
            for (String marker : format.getMarkers()) {
                handlerInfos.add(new PageMetadataHandlerInfo(handler, marker, false));
            }
        }
    }

    @Override
    public boolean isBlank() {
        return (defFormats == null || defFormats.isEmpty())
                && (refFormats == null || refFormats.isEmpty());
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return handlerInfos;
    }

    @Override
    public void acceptDocumentList(List<Document> documents) {
        documentInputs = documents.stream()
                .map(doc -> normalizeDependencyPath(doc.getInput()))
                .collect(Collectors.toSet());
    }

    @Override
    public void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins,
                              PageMetadataHandlersWrapper metadataHandlers) {
        if (isBackrefsCacheEnabled()) {
            loadBackrefsCache();
            pruneBackrefsCache();
            reverseMapReferencesByPage.clear();
            reverseMapReferencesByPage.putAll(
                    buildReverseMapReferencesByPageFromCache(backrefsCache));
            populateReferencesFromCache(references);
        }
    }

    @Override
    public void newPage(Document document) {
        uniqueIndexer = new UniqueIndexer();
        if (document == null || document.getInput() == null) {
            return;
        }
        String pageInput = normalizeDependencyPath(document.getInput());
        removePageFromDefinitions(pageInput);
        removeReferencingPage(pageInput);
    }

    private class DefMetadataHandler implements PageMetadataHandler {

        private final DefFormatConfig format;

        private DefMetadataHandler(DefFormatConfig format) {
            this.format = format;
        }

        @Override
        public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
                String metadata, String metadataSection, Set<String> visitedMarkers
        ) throws PageMetadataException {
            return acceptPageMetadata(document, marker, metadata, metadataSection, visitedMarkers,
                    MetadataProcessingPhase.PHASE_1, null);
        }

        @Override
        public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
                String metadata, String metadataSection, Set<String> visitedMarkers,
                MetadataProcessingPhase phase, Object dataFromPrevPhase
        ) throws PageMetadataException {
            String[] parsed = parseDefMetadata(metadata);
            String sourceCode = parsed[0];
            String refContent = parsed[1];
            if (phase == MetadataProcessingPhase.PHASE_1) {
                if (definitions.containsKey(sourceCode)) {
                    throw new PageMetadataException(
                            "Source '" + sourceCode + "' is defined multiple times");
                }
                String anchorId = codePrefix + "def_" + sourceCode;
                Definition definition = new Definition(
                        pageLocationFromDoc(document), anchorId, refContent, format);
                addDefinition(definition, sourceCode);
                return MetadataProcessingResult.deferred(anchorId);
            }
            if (phase == MetadataProcessingPhase.PHASE_2) {
                String anchorId = (String) dataFromPrevPhase;
                DefFormatConfig defFormat = definitions.get(sourceCode).getDefFormat();
                List<String> backRefList = new ArrayList<>();
                int backRefIndex = 0;
                Map<String, List<Reference>> refsByPage = references.get(sourceCode);
                if (refsByPage != null) {
                    for (List<Reference> refs : refsByPage.values()) {
                        for (Reference ref : refs) {
                            backRefIndex++;
                            String refLink = relativizeRelativeResource(
                                    ref.getPage().getOutputFile(), document.getOutput());
                            backRefList.add(defFormat.getBackRefTemplate().replace(namedMap(
                                    "href", refLink + "#" + ref.getAnchorId(),
                                    "link_text", String.valueOf(backRefIndex))));
                        }
                    }
                }
                String backRefHtml = String.join(defFormat.getBackRefDelimiter(), backRefList);
                return MetadataProcessingResult.immediate(defFormat.getTemplate().replace(namedMap(
                        "code", sourceCode,
                        "anchor", anchorId,
                        "back_refs_html", backRefHtml,
                        "content", refContent)));
            }
            throw new IllegalStateException("Unknown phase: " + phase);
        }
    }

    private class RefMetadataHandler implements PageMetadataHandler {

        private final RefFormatConfig format;

        private RefMetadataHandler(RefFormatConfig format) {
            this.format = format;
        }

        @Override
        public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
                String metadata, String metadataSection, Set<String> visitedMarkers
        ) throws PageMetadataException {
            return acceptPageMetadata(document, marker, metadata, metadataSection, visitedMarkers,
                    MetadataProcessingPhase.PHASE_1, null);
        }

        @Override
        public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
                String metadata, String metadataSection, Set<String> visitedMarkers,
                MetadataProcessingPhase phase, Object dataFromPrevPhase
        ) throws PageMetadataException {
            String sourceCode = parseRefMetadata(metadata);
            if (phase == MetadataProcessingPhase.PHASE_1) {
                String refAnchorId = codePrefix + "ref_" + sourceCode;
                refAnchorId = uniqueIndexer.getUnique(refAnchorId);
                PageLocation refPage = pageLocationFromDoc(document);
                addReference(sourceCode, refPage, refAnchorId);
                if (definitions.containsKey(sourceCode)) {
                    return MetadataProcessingResult.immediate(
                            generateRefHtml(sourceCode, refAnchorId, document));
                }
                return MetadataProcessingResult.deferred(refAnchorId);
            }
            if (phase == MetadataProcessingPhase.PHASE_2) {
                String refAnchorId = (String) dataFromPrevPhase;
                if (!definitions.containsKey(sourceCode)) {
                    throw new PageMetadataException(
                            "Source '" + sourceCode + "' referenced but not defined");
                }
                return MetadataProcessingResult.immediate(
                        generateRefHtml(sourceCode, refAnchorId, document));
            }
            throw new IllegalStateException("Unknown phase: " + phase);
        }

        private String generateRefHtml(String sourceCode, String refAnchorId, Document document) {
            Definition definition = definitions.get(sourceCode);
            String link = relativizeRelativeResource(
                    definition.getPage().getOutputFile(), document.getOutput());
            return format.getTemplate().replace(namedMap(
                    "code", sourceCode,
                    "anchor", refAnchorId,
                    "href", link + "#" + definition.getAnchorId(),
                    "content", definition.getRefContent()));
        }
    }

    private static String parseRefMetadata(String metadata) throws PageMetadataException {
        String[] fields = metadata.trim().split("\\s+");
        if (fields.length != 1) {
            throw new PageMetadataException("Metadata error: '" + metadata.trim()
                    + "' - the source code must be a single word (no spaces).");
        }
        return fields[0];
    }

    private static String[] parseDefMetadata(String metadata) throws PageMetadataException {
        Matcher matcher = DEF_METADATA_PATTERN.matcher(metadata.trim());
        if (!matcher.matches()) {
            throw new PageMetadataException("Metadata error: '" + metadata.trim()
                    + "' - should contain source code and reference display value separated "
                    + "by spaces.");
        }
        return new String[]{matcher.group(1), matcher.group(2)};
    }

    private static String normalizeDependencyPath(String path) {
        return path.replace("\\", "/");
    }

    private static PageLocation pageLocationFromDoc(Document document) {
        return new PageLocation(
                normalizeDependencyPath(document.getInput()),
                normalizeDependencyPath(document.getOutput()));
    }

    private static VariableReplacer parseTemplate(String template, String fieldName) {
        try {
            return new VariableReplacer(template);
        } catch (VariableReplacerException e) {
            throw new UserError("Invalid " + fieldName + ": " + e.getMessage());
        }
    }

    private static DefFormatConfig parseDefFormat(JsonNode entry) {
        Map<String, Object> merged = new HashMap<>(DEFAULT_DEF_FORMAT);
        merged.putAll(formatEntryAsMap(entry));
        List<String> markers = markerList(merged.get("markers"));
        if (markers.isEmpty()) {
            throw new UserError("Each def-format entry must declare at least one marker.");
        }
        return new DefFormatConfig(
                markers,
                parseTemplate((String) merged.get("template"), "template"),
                parseTemplate((String) merged.get("back-ref-template"), "back-ref-template"),
                (String) merged.get("back-ref-delimiter"));
    }

    private static RefFormatConfig parseRefFormat(JsonNode entry) {
        Map<String, Object> merged = new HashMap<>(DEFAULT_REF_FORMAT);
        merged.putAll(formatEntryAsMap(entry));
        List<String> markers = markerList(merged.get("markers"));
        if (markers.isEmpty()) {
            throw new UserError("Each ref-format entry must declare at least one marker.");
        }
        return new RefFormatConfig(
                markers,
                parseTemplate((String) merged.get("template"), "template"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> formatEntryAsMap(JsonNode entry) {
        return OBJECT_MAPPER.convertValue(entry, Map.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> markerList(Object rawMarkers) {
        List<String> markers = new ArrayList<>();
        for (String marker : (List<String>) rawMarkers) {
            markers.add(marker.toUpperCase());
        }
        return markers;
    }

    private static List<DefFormatConfig> parseDefFormats(JsonNode data) {
        JsonNode rawFormats = data.get("def-formats");
        if (rawFormats == null || rawFormats.isNull()) {
            return Collections.singletonList(parseDefFormat(OBJECT_MAPPER.createObjectNode()));
        }
        List<DefFormatConfig> formats = new ArrayList<>();
        for (JsonNode entry : rawFormats) {
            formats.add(parseDefFormat(entry));
        }
        return formats;
    }

    private static List<RefFormatConfig> parseRefFormats(JsonNode data) {
        JsonNode rawFormats = data.get("ref-formats");
        if (rawFormats == null || rawFormats.isNull()) {
            return Collections.singletonList(parseRefFormat(OBJECT_MAPPER.createObjectNode()));
        }
        List<RefFormatConfig> formats = new ArrayList<>();
        for (JsonNode entry : rawFormats) {
            formats.add(parseRefFormat(entry));
        }
        return formats;
    }

    private void addDefinition(Definition definition, String sourceCode) {
        definitions.put(sourceCode, definition);
        reverseMapDefinitionsByPage
                .computeIfAbsent(definition.getPage().getInputFile(), k -> new HashSet<>())
                .add(sourceCode);
    }

    private void addReference(String sourceCode, PageLocation refPage, String refAnchorId) {
        references.computeIfAbsent(sourceCode, k -> new LinkedHashMap<>())
                .computeIfAbsent(refPage.getInputFile(), k -> new ArrayList<>())
                .add(new Reference(refPage, refAnchorId));
        reverseMapReferencesByPage
                .computeIfAbsent(refPage.getInputFile(), k -> new HashSet<>())
                .add(sourceCode);
        if (isBackrefsCacheEnabled()) {
            Map<String, Map<String, Object>> referencingPages =
                    backrefsCache.computeIfAbsent(sourceCode, k -> new LinkedHashMap<>());
            Map<String, Object> record = referencingPages.get(refPage.getInputFile());
            if (record == null) {
                Map<String, Object> newRecord = new HashMap<>();
                newRecord.put("input_file", refPage.getInputFile());
                newRecord.put("output_file", refPage.getOutputFile());
                newRecord.put("anchor_ids", new ArrayList<>(
                        Collections.singletonList(refAnchorId)));
                referencingPages.put(refPage.getInputFile(), newRecord);
            } else {
                @SuppressWarnings("unchecked")
                List<String> anchorIds = (List<String>) record.get("anchor_ids");
                anchorIds.add(refAnchorId);
            }
        }
    }

    private boolean isBackrefsCacheEnabled() {
        return backrefsCacheFile != null;
    }

    private void loadBackrefsCache() {
        Path cacheFile = Paths.get(backrefsCacheFile);
        if (Files.exists(cacheFile)) {
            try {
                backrefsCache = normalizeLoadedCache(
                        OBJECT_MAPPER.readValue(cacheFile.toFile(), Map.class));
            } catch (IOException e) {
                throw new RuntimeException("Error reading backrefs cache file '" +
                        backrefsCacheFile + "'", e);
            }
        } else {
            backrefsCache = new HashMap<>();
        }
    }

    private void pruneBackrefsCache() {
        Map<String, Map<String, Map<String, Object>>> pruned = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : backrefsCache.entrySet()) {
            Map<String, Map<String, Object>> kept = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> pageEntry : entry.getValue().entrySet()) {
                if (documentInputs.contains(pageEntry.getKey())) {
                    kept.put(pageEntry.getKey(), pageEntry.getValue());
                }
            }
            if (!kept.isEmpty()) {
                pruned.put(entry.getKey(), kept);
            }
        }
        backrefsCache = pruned;
    }

    private void populateReferencesFromCache(Map<String, Map<String, List<Reference>>> references) {
        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : backrefsCache.entrySet()) {
            String sourceCode = entry.getKey();
            Map<String, List<Reference>> byPage =
                    references.computeIfAbsent(sourceCode, k -> new LinkedHashMap<>());
            for (Map<String, Object> referencingPage : entry.getValue().values()) {
                PageLocation page = pageLocationFromCacheRefItem(referencingPage);
                @SuppressWarnings("unchecked")
                List<String> anchorIds = (List<String>) referencingPage.get("anchor_ids");
                List<Reference> refs = new ArrayList<>();
                for (String anchorId : anchorIds) {
                    refs.add(new Reference(page, anchorId));
                }
                byPage.put(page.getInputFile(), refs);
            }
        }
    }

    private void removePageFromDefinitions(String pageInput) {
        Set<String> sourceCodes = reverseMapDefinitionsByPage.remove(pageInput);
        if (sourceCodes != null) {
            for (String sourceCode : sourceCodes) {
                definitions.remove(sourceCode);
            }
        }
    }

    private void removeReferencingPage(String pageInput) {
        Set<String> sourceCodes = reverseMapReferencesByPage.remove(pageInput);
        if (sourceCodes != null) {
            for (String sourceCode : sourceCodes) {
                removeReferencingPageFromSource(sourceCode, pageInput);
            }
        }
    }

    private void removeReferencingPageFromSource(String sourceCode, String pageInput) {
        Map<String, List<Reference>> refsByPage = references.get(sourceCode);
        if (refsByPage != null) {
            refsByPage.remove(pageInput);
            if (refsByPage.isEmpty()) {
                references.remove(sourceCode);
            }
        }
        if (isBackrefsCacheEnabled()) {
            Map<String, Map<String, Object>> cacheReferences = backrefsCache.get(sourceCode);
            if (cacheReferences != null) {
                cacheReferences.remove(pageInput);
                if (cacheReferences.isEmpty()) {
                    backrefsCache.remove(sourceCode);
                }
            }
        }
    }

    private static PageLocation pageLocationFromCacheRefItem(Map<String, Object> record) {
        return new PageLocation(
                normalizeDependencyPath((String) record.get("input_file")),
                normalizeDependencyPath((String) record.get("output_file")));
    }

    private static Map<String, Map<String, Map<String, Object>>> normalizeLoadedCache(
            Map<String, ?> data) {
        Map<String, Map<String, Map<String, Object>>> normalized = new HashMap<>();
        for (Map.Entry<String, ?> sourceEntry : data.entrySet()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> referencingPages =
                    (List<Map<String, Object>>) sourceEntry.getValue();
            Map<String, Map<String, Object>> byInputFile = new LinkedHashMap<>();
            for (Map<String, Object> referencingPage : referencingPages) {
                String inputFile = normalizeDependencyPath((String) referencingPage.get("input_file"));
                Map<String, Object> normalizedPage = new HashMap<>();
                normalizedPage.put("input_file", inputFile);
                normalizedPage.put("output_file",
                        normalizeDependencyPath((String) referencingPage.get("output_file")));
                normalizedPage.put("anchor_ids",
                        new ArrayList<>((List<String>) referencingPage.get("anchor_ids")));
                byInputFile.put(inputFile, normalizedPage);
            }
            if (!byInputFile.isEmpty()) {
                normalized.put(sourceEntry.getKey(), byInputFile);
            }
        }
        return normalized;
    }

    private static Map<String, Set<String>> buildReverseMapReferencesByPageFromCache(
            Map<String, Map<String, Map<String, Object>>> backrefsCache) {
        Map<String, Set<String>> reverseMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : backrefsCache.entrySet()) {
            for (String pageInput : entry.getValue().keySet()) {
                reverseMap.computeIfAbsent(pageInput, k -> new HashSet<>()).add(entry.getKey());
            }
        }
        return reverseMap;
    }

    private static Map<String, String> namedMap(String... keysAndValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static String relativizeRelativeResource(String resource, String page) {
        try {
            return world.md2html.utils.Utils.relativizeRelativeResource(resource, page);
        } catch (CheckedIllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}
