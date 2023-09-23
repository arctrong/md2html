package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.networknt.schema.JsonSchema;
import lombok.*;
import org.javatuples.Pair;
import world.md2html.Md2Html;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.options.model.raw.ArgFileDocumentRaw;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.utils.CheckedIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static world.md2html.options.argfile.ArgFileParsingHelper.completeArgFileProcessing;
import static world.md2html.options.argfile.ArgFileParsingHelper.mergeAndCanonizeArgFileRaw;
import static world.md2html.plugins.PluginUtils.listFromStringOrArray;
import static world.md2html.utils.JsonUtils.*;
import static world.md2html.utils.Utils.relativizeRelativeResource;

public class IndexPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    // TODO Consider using Jackson object mapper
    // TODO Remove all getters and setters and use direct field access. This is a nested class
    //  and the outer class anyway has access to its private fields.
    @Getter
    @Setter
    @Builder(toBuilder = true)
    private static class IndexData {
        private Path indexCacheFile;
        private boolean indexCacheRelative;
        // TODO Test this parameter
        private boolean addLetters;
        // TODO Test this parameter
        private boolean addLettersBlock;
        private ObjectNode documentJson;
        private Document document;
        private String currentLinkPage;
        private int currentAnchorNumber;
        private Map<String, List<IndexEntry>> indexCache;
        private final Set<String> cachedPageResets = new HashSet<>();
    }

    private static final String INDEX_ENTRY_ANCHOR_PREFIX = "index_entry_";
    private static final String INDEX_CONTENT_BLOCK_CLASS = "index-content";
    private static final String INDEX_ENTRY_CLASS = "index-entry";
    private static final String INDEX_LETTER_ID_PREFIX = "index_letter_";
    private static final String INDEX_LETTER_CLASS = "index-letter";
    private static final String INDEX_LETTERS_BLOCK_CLASS = "index_letters";

    private Map<String, IndexData> indexData;

    private SessionOptions options;
    private List<Md2HtmlPlugin> plugins;

    private boolean finalizationStarted = false;

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/string_or_array_schema.json");

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {

        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/index_schema.json");
        Map<String, IndexData> indexDataMap = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fieldIterator = data.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
            String marker = fieldEntry.getKey();
            ObjectNode indexData = fieldEntry.getValue().deepCopy();

            IndexData.IndexDataBuilder indexDataBuilder = IndexData.builder();

            indexDataBuilder.documentJson(indexData);
            indexDataBuilder.indexCacheFile(Paths.get(indexData.get("index-cache").asText()));

            ValueNode indexCacheRelativeNode = (ValueNode) indexData.get("index-cache-relative");
            if (indexCacheRelativeNode != null) {
                indexDataBuilder.indexCacheRelative(indexCacheRelativeNode.asBoolean());
            }
            ValueNode addLettersNode = (ValueNode) indexData.get("letters");
            if (addLettersNode != null) {
                indexDataBuilder.addLetters(addLettersNode.asBoolean());
            }
            ValueNode lettersBlock = (ValueNode) indexData.get("letters-block");
            if (lettersBlock != null) {
                indexDataBuilder.addLettersBlock(lettersBlock.asBoolean());
            }

            indexDataMap.put(marker.toUpperCase(), indexDataBuilder.build());

            indexData.remove("index-cache");
            indexData.remove("index-cache-relative");
            indexData.remove("letters");
            indexData.remove("letters-block");
        }
        this.indexData = indexDataMap;
    }

    @Override
    public boolean isBlank() {
        return indexData.isEmpty();
    }

    @Override
    public Map<String, JsonNode> preInitialize(ArgFileRaw argFileRaw, CliOptions cliOptions,
            Map<String, Md2HtmlPlugin> plugins) {

        ArgFileRaw.ArgFileRawBuilder argFileRawBuilder = argFileRaw.toBuilder();
        List<ArgFileDocumentRaw> documentRawList = new ArrayList<>();
        for (IndexData indexData : indexData.values()) {
            ArgFileDocumentRaw argFileDocumentRaw;
            try {
                argFileDocumentRaw = OBJECT_MAPPER_FOR_BUILDERS
                        .treeToValue(indexData.getDocumentJson(), ArgFileDocumentRaw.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            documentRawList.add(argFileDocumentRaw.toBuilder().input("fictional.txt").build());
        }
        argFileRawBuilder.documents(documentRawList);

        ArgFileRaw canonizedArgFileRaw = mergeAndCanonizeArgFileRaw(argFileRawBuilder.build(),
                cliOptions);

        Pair<ArgFile, Map<String, JsonNode>> processingResult =
                completeArgFileProcessing(canonizedArgFileRaw, plugins);
        ArgFile arguments = processingResult.getValue0();
        Map<String, JsonNode> extraPluginData = processingResult.getValue1();

        Map<String, IndexData> newIndexData = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, IndexData> indexDataEntry : this.indexData.entrySet()) {
            String marker = indexDataEntry.getKey();
            IndexData indexData = indexDataEntry.getValue();
            IndexData.IndexDataBuilder indexDataBuilder = indexData.toBuilder();
            Document.DocumentBuilder documentBuilder = arguments.getDocuments().get(i).toBuilder();
            i++;
            documentBuilder.input(null);
            Document document = documentBuilder.build();
            indexDataBuilder.document(document);

            Path indexCacheFile = indexData.indexCacheRelative ? Paths.get(document.getOutput())
                    .getParent().resolve(indexData.getIndexCacheFile()) :
                    indexData.getIndexCacheFile();
            indexDataBuilder.indexCacheFile(indexCacheFile);

            if (Files.exists(indexCacheFile)) {
                try {
                    indexDataBuilder.indexCache(OBJECT_MAPPER.readValue(indexCacheFile.toFile(),
                            new TypeReference<Map<String, List<IndexEntry>>>() {
                            }));
                } catch (IOException e) {
                    throw new RuntimeException("Error reading index cache file '" + indexCacheFile
                            + "': " + e.getMessage(), e);
                }
            } else {
                indexDataBuilder.indexCache(new LinkedHashMap<>());
            }
            newIndexData.put(marker, indexDataBuilder.build());
        }
        this.indexData = newIndexData;

        return extraPluginData;
    }

    @Override
    public void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins) {
        this.options = options;
        this.plugins = plugins;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.indexData.keySet().stream().map(marker ->
                new PageMetadataHandlerInfo(this, marker, false)).collect(Collectors.toList());
    }

    @Override
    public void newPage(Document document) {
        if (this.finalizationStarted) {
            return;
        }
        for (IndexData indexData : this.indexData.values()) {
            indexData.getIndexCache().put(document.getOutput(), new ArrayList<>());
            indexData.getCachedPageResets().add(document.getOutput());
            try {
                indexData.setCurrentLinkPage(relativizeRelativeResource(document.getOutput(),
                        indexData.getDocument().getOutput()));
            } catch (CheckedIllegalArgumentException e) {
                throw new RuntimeException("Could not relativize '" +
                        indexData.getDocument().getOutput() + "' against '" +
                        document.getOutput() + "': " + e.getMessage(), e);
            }
            indexData.setCurrentAnchorNumber(0);
        }
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return Collections.emptyMap();
    }

    public void finalizePlugin() {

        this.finalizationStarted = true;

        for (IndexData indexData : this.indexData.values()) {
            if (indexData.getCachedPageResets().isEmpty()) {
                if (indexData.getDocument().isVerbose()) {
                    System.out.println("Index file is up-to-date. Skipping: "
                            + indexData.getDocument().getOutput());
                }
                return;
            }
            for (Md2HtmlPlugin plugin : this.plugins) {
                plugin.newPage(indexData.getDocument());
            }

            Map<String, Object> substitutions = new HashMap<>();
            substitutions.put("content", generateIndexHtml(indexData.getIndexCache(),
                    indexData.isAddLetters(), indexData.isAddLettersBlock()));

            Md2Html.outputPage(indexData.getDocument(), this.plugins, substitutions, this.options,
                    null);

            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                    .withObjectIndenter(new DefaultIndenter("  ", "\n"));
            try {
                mapper.writer(printer).writeValue(indexData.getIndexCacheFile().toFile(),
                        indexData.getIndexCache());
            } catch (IOException e) {
                throw new RuntimeException("Error opening index cache file for writing: "
                        + indexData.getIndexCacheFile(), e);
            }

            if (indexData.getDocument().isVerbose()) {
                System.out.println("Index file generated: " +
                        indexData.getDocument().getOutput());
            }
            if (indexData.getDocument().isReport()) {
                System.out.println(indexData.getDocument().getOutput());
            }
        }
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
                                     String metadataSection) throws PageMetadataException {

        IndexData indexData = this.indexData.get(marker.toUpperCase());
        Map<String, List<IndexEntry>> indexCache = indexData.getIndexCache();

        metadata = metadata.trim();
        List<String> terms;
        if (metadata.startsWith("[")) {
            //noinspection unchecked
            terms = (List<String>) deJson(listFromStringOrArray(document, metadata));
        } else {
            terms = Collections.singletonList(metadata);
        }

        List<IndexEntry> anchors = indexCache.get(document.getOutput());
        int currentAnchorNumber = indexData.getCurrentAnchorNumber() + 1;
        String anchorName = INDEX_ENTRY_ANCHOR_PREFIX + marker.toLowerCase() + "_" +
                currentAnchorNumber;
        String anchorText = "<a name=\"" + anchorName + "\"></a>";
        indexData.setCurrentAnchorNumber(currentAnchorNumber);

        for (String term : terms) {
            String normalizedTerm = term.trim();
            IndexEntry indexEntry = new IndexEntry(normalizedTerm,
                    indexData.getCurrentLinkPage() + "#" + anchorName, document.getTitle());
            anchors.add(indexEntry);
        }

        return anchorText;
    }

    private static String generateIndexHtml(Map<String, List<IndexEntry>> indexCache,
                                            boolean addLetters, boolean addLettersBlock) {

        Map<String, List<IndexEntry>> terms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (List<IndexEntry> entries : indexCache.values()) {
            for (IndexEntry entry : entries) {
                terms.computeIfAbsent(entry.entry, (k) -> new ArrayList<>()).add(entry);
            }
        }

        StringBuilder content = new StringBuilder();
        StringBuilder letterLinks = new StringBuilder();
        String currentLetter = "";
        for (Map.Entry<String, List<IndexEntry>> termEntry : terms.entrySet()) {
            String term = termEntry.getKey();
            List<IndexEntry> links = termEntry.getValue();

            if (addLetters || addLettersBlock) {
                String letter = term == null || term.isEmpty() ? "" :
                        term.substring(0, 1).toUpperCase();
                if (!letter.equals(currentLetter)) {
                    currentLetter = letter;
                    String indexLetterId = INDEX_LETTER_ID_PREFIX + currentLetter;
                    if (addLetters) {
                        content.append("<p class=\"" + INDEX_LETTER_CLASS + "\">")
                                .append("<a id=\"").append(indexLetterId).append("\"></a>")
                                .append(currentLetter).append("</p>\n");
                    } else {
                        content.append("<a name=\"").append(indexLetterId).append("\">")
                                .append("</a>\n");
                    }
                    if (addLettersBlock) {
                        letterLinks.append("<a href=\"#").append(indexLetterId).append("\">")
                                .append(currentLetter).append("</a>").append(" ");
                    }
                }
            }

            if (links.size() > 1) {
                StringBuilder linksString = new StringBuilder();
                int count = 0;
                for (IndexEntry indexEntry : links) {
                    count++;
                    linksString.append(count > 1 ? ", " : "").append("<a href=\"")
                            .append(indexEntry.getLink()).append("\"")
                            .append(createTitleAttr(indexEntry.getTitle())).append(">")
                            .append(count).append("</a>");
                }
                content.append("<p class=\"" + INDEX_ENTRY_CLASS + "\">").append(term).append(": ")
                        .append(linksString).append("</p>\n");
            } else {
                IndexEntry indexEntry = links.get(0);
                content.append("<p class=\"" + INDEX_ENTRY_CLASS + "\"><a href=\"")
                        .append(indexEntry.getLink())
                        .append("\"").append(createTitleAttr(indexEntry.getTitle())).append(">")
                        .append(term).append("</a></p>\n");
            }
        }

        return (letterLinks.length() > 0 ?
                "<p class=\"" + INDEX_LETTERS_BLOCK_CLASS + "\">" + letterLinks + "</p>\n" : "") +
                "<div class=\"" + INDEX_CONTENT_BLOCK_CLASS + "\">\n" + content + "\n</div>";
    }

    private static String createTitleAttr(String title) {
        return title == null || title.isEmpty() ? "" : " title=\"" + escapeHtml4(title) + "\"";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    static class IndexEntry {
        private String entry;
        private String link;
        private String title;
    }

}
