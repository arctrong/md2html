package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.mustachejava.Mustache;
import com.networknt.schema.JsonSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import world.md2html.Constants;
import world.md2html.Md2HtmlUtils;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.model.ArgFileOptions;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.MustacheUtils;
import world.md2html.utils.UserError;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static world.md2html.utils.JsonUtils.*;
import static world.md2html.utils.Utils.relativizeRelativeResource;

public class IndexPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler,
        InitializationAction, FinalizationAction {

    private static final String TITLE_PLACEHOLDER = "title";
    private static final String STYLES_PLACEHOLDER = "styles";
    private static final String CONTENT_PLACEHOLDER = "content";
    private static final String EXEC_NAME_PLACEHOLDER = "exec_name";
    private static final String EXEC_VERSION_PLACEHOLDER = "exec_version";
    private static final String GENERATION_DATE_PLACEHOLDER = "generation_date";
    private static final String GENERATION_TIME_PLACEHOLDER = "generation_time";

    private static final String INDEX_ENTRY_ANCHOR_PREFIX = "index_entry_";
    private static final String INDEX_CONTENT_BLOCK_CLASS = "index-content";
    private static final String INDEX_ENTRY_CLASS = "index-entry";
    private static final String INDEX_LETTER_ID_PREFIX = "index_letter_";
    private static final String INDEX_LETTER_CLASS = "index-letter";
    private static final String INDEX_LETTERS_BLOCK_CLASS = "index_letters";

    // We are going to validate multiple metadata blocks, so preloading the schema.
    private final JsonSchema metadataSchema =
            loadJsonSchemaFromResource("plugins/index_metadata_schema.json");

    private Path indexCacheFile;
    private boolean indexCacheRelative = false;
    private boolean addLetters = false;
    private boolean addLettersBlock = false;

    private ObjectNode documentJson;
    private Document document;
    private List<Md2HtmlPlugin> plugins;

    private String currentLinkPage;
    private int currentAnchorNumber = 0;

    private Map<String, List<IndexEntry>> indexCache;
    private final Set<String> cachedPageResets = new HashSet<>();

    private final List<PageMetadataHandlerInfo> handlers =
            Collections.singletonList(new PageMetadataHandlerInfo(this, "INDEX", false));

    private final List<InitializationAction> initializationActions =
            Collections.singletonList(this);

    private final List<FinalizationAction> finalizationActions =
            Collections.singletonList(this);

    @Override
    public boolean acceptData(JsonNode data) throws ArgFileParseException {
        doStandardJsonInputDataValidation(data, "plugins/index_schema.json");
        this.documentJson = data.deepCopy();
        this.indexCacheFile = Paths.get(data.get("index-cache").asText());
        ObjectNode dataObj = (ObjectNode) data;
        this.indexCacheRelative = jsonObjectBooleanField(dataObj, "index-cache-relative",
                this.indexCacheRelative);
        this.addLetters = jsonObjectBooleanField(dataObj, "letters", this.addLetters);
        this.addLettersBlock = jsonObjectBooleanField(dataObj, "letters-block",
                this.addLettersBlock);
        return true;
    }

    @Override
    public List<InitializationAction> initializationActions() {
        return this.initializationActions;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.handlers;
    }

    @Override
    public void newPage(Document document) {
        this.indexCache.put(document.getOutputLocation(), new ArrayList<>());
        this.cachedPageResets.add(document.getOutputLocation());
        try {
            this.currentLinkPage = relativizeRelativeResource(document.getOutputLocation(),
                    this.document.getOutputLocation());
        } catch (CheckedIllegalArgumentException e) {
            throw new RuntimeException("Could not relativize '" + this.document.getOutputLocation()
                    + "' against '" + document.getOutputLocation() + "': " + e.getMessage(), e);
        }
        this.currentAnchorNumber = 0;
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return Collections.emptyMap();
    }

    @Override
    public void initialize(ObjectNode argFileNode, CliOptions cliOptions,
                List<Md2HtmlPlugin> plugins) {
        argFileNode = argFileNode.deepCopy();
        this.documentJson.put("input", "fictional.txt");
        ArrayNode documents = new ArrayNode(NODE_FACTORY);
        documents.add(this.documentJson);
        argFileNode.set("documents", documents);

        cliOptions = cliOptions.toBuilder().build(); // creating a copy
        cliOptions.setInputFile(null);
        cliOptions.setOutputFile(null);

        ArgFileOptions argFileOptions;
        try {
            argFileOptions = ArgFileParser.parse(argFileNode, cliOptions, false);
        } catch (ArgFileParseException e) {
            throw new RuntimeException("Error initializing plugin '"
                    + this.getClass().getSimpleName() + "': " + e.getMessage(), e);
        }
        this.document = argFileOptions.getDocuments().get(0);
        this.document.setInputLocation(null);

        if (this.indexCacheRelative) {
            this.indexCacheFile = Paths.get(this.document.getOutputLocation()).getParent()
                    .resolve(this.indexCacheFile);
        }
        if (Files.exists(indexCacheFile)) {
            try {
                this.indexCache = MAPPER.readValue(indexCacheFile.toFile(),
                        new TypeReference<Map<String, List<IndexEntry>>>() {});
            } catch (IOException e) {
                throw new RuntimeException("Error reading index cache file '" + indexCacheFile
                        + "': " + e.getMessage(), e);
            }
        } else {
            this.indexCache = new LinkedHashMap<>();
        }

        this.plugins = plugins;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
                                     String metadataSection) throws PageMetadataException {

        metadata = metadata.trim();
        List<String> terms;
        if (metadata.startsWith("[")) {
            //noinspection unchecked
            terms = (List<String>) deJson(parseAndValidateEntryJson(document, metadata));
        } else {
            terms = Collections.singletonList(metadata);
        }

        List<IndexEntry> anchors = this.indexCache.get(document.getOutputLocation());
        String anchorText = "<a name=\"" + INDEX_ENTRY_ANCHOR_PREFIX +
                (++this.currentAnchorNumber) + "\"></a>";

        for (String term : terms) {
            String normalizedTerm = term.trim();
            IndexEntry indexEntry = new IndexEntry(normalizedTerm, this.currentLinkPage + "#"
                    + INDEX_ENTRY_ANCHOR_PREFIX + this.currentAnchorNumber, document.getTitle());
            anchors.add(indexEntry);
        }

        return anchorText;
    }

    @Override
    public List<FinalizationAction> finalizationActions() {
        return this.finalizationActions;
    }

    @Override
    public void finalizePlugin() {

        if (this.cachedPageResets.isEmpty()) {
            if (this.document.isVerbose()) {
                System.out.println("Index file is up-to-date. Skipping: "
                        + this.document.getOutputLocation());
            }
            return;
        }

        Path outputFile = Paths.get(this.document.getOutputLocation());
        
        this.plugins.stream().filter(p -> p != this)
                .forEach(plugin -> plugin.newPage(this.document));

        Map<String, Object> substitutions = new HashMap<>();
        String title = this.document.getTitle();

        if (title == null) {
            title = "";
        }

        String htmlText = generateIndexHtml(this.indexCache, this.addLetters, this.addLettersBlock);

        substitutions.put(TITLE_PLACEHOLDER, title);
        substitutions.put(CONTENT_PLACEHOLDER, htmlText);

        substitutions.put(STYLES_PLACEHOLDER, Md2HtmlUtils.generateDocumentStyles(this.document));

        substitutions.put(EXEC_NAME_PLACEHOLDER, Constants.EXEC_NAME);
        substitutions.put(EXEC_VERSION_PLACEHOLDER, Constants.EXEC_VERSION);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        substitutions.put(GENERATION_DATE_PLACEHOLDER, dateTime.format(dateFormatter));
        substitutions.put(GENERATION_TIME_PLACEHOLDER, dateTime.format(timeFormatter));

        for (Md2HtmlPlugin plugin : plugins) {
            substitutions.putAll(plugin.variables(this.document));
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile.toFile()), StandardCharsets.UTF_8))) {
            Mustache mustache;
            try {
                mustache = MustacheUtils.createCachedMustacheRenderer(this.document.getTemplate());
            } catch (FileNotFoundException e) {
                throw new UserError(String.format("Error reading template file '%s': %s: %s",
                        this.document.getTemplate().toString(), e.getClass().getSimpleName(),
                        e.getMessage()));
            }
            mustache.execute(writer, substitutions);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error opening output file for writing: " + outputFile, e);
        }

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"));
        try {
            mapper.writer(printer).writeValue(this.indexCacheFile.toFile(), this.indexCache);
        } catch (IOException e) {
            throw new RuntimeException("Error opening index cache file for writing: "
                    + this.indexCacheFile, e);
        }

        if (this.document.isVerbose()) {
            System.out.println("Index file generated: " + this.document.getOutputLocation());
        }
        if (this.document.isReport()) {
            System.out.println(this.document.getOutputLocation());
        }
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
                        content.append("<p class=\"" + INDEX_LETTER_CLASS + "\" id=\"")
                                .append(indexLetterId).append("\">")
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
                content.append("<p class=\"" + INDEX_ENTRY_CLASS + "\"><a href=\"").append(indexEntry.getLink())
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

    private ArrayNode parseAndValidateEntryJson(Document document, String metadata) {
        JsonNode metadataJsonNode;
        try {
            metadataJsonNode = MAPPER.readTree(metadata);
        } catch (JsonProcessingException e) {
            throw new PageMetadataException("Incorrect JSON in index entry. Class '" +
                    e.getClass().getSimpleName() + "', page '" + document.getInputLocation()
                    + "', error: " + e.getMessage());
        }
        try {
            validateJson(metadataJsonNode, this.metadataSchema);
        } catch (JsonValidationException e) {
            throw new PageMetadataException("Error validating index entry. Class '" +
                    e.getClass().getSimpleName() + "', page '" + document.getInputLocation()
                    + "', error: " + e.getMessage());
        }
        return (ArrayNode) metadataJsonNode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    static class IndexEntry {
        private String entry;
        private String link;
        private String title;
    }

}
