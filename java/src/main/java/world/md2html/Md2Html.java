package world.md2html;

import com.github.mustachejava.Mustache;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import world.md2html.extentions.admonition.PythonMarkdownCompatibleAdmonitionExtension;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.utils.MustacheUtils;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Md2Html {

    private static final String TITLE_PLACEHOLDER = "title";
    private static final String STYLES_PLACEHOLDER = "styles";
    private static final String CONTENT_PLACEHOLDER = "content";
    private static final String EXEC_NAME_PLACEHOLDER = "exec_name";
    private static final String EXEC_VERSION_PLACEHOLDER = "exec_version";
    private static final String GENERATION_DATE_PLACEHOLDER = "generation_date";
    private static final String GENERATION_TIME_PLACEHOLDER = "generation_time";

    public static void execute(SessionOptions options, Document document,
            List<Md2HtmlPlugin> plugins, PageMetadataHandlersWrapper metadataHandlersWrapper)
            throws IOException, UserError {

        Path outputFile = Paths.get(document.getOutputLocation());
        Path inputFile = Paths.get(document.getInputLocation());

        if (!document.isForce() && Files.exists(outputFile)) {
            FileTime inputFileTime = Files.getLastModifiedTime(inputFile);
            FileTime outputFileTime = Files.getLastModifiedTime(outputFile);
            if (outputFileTime.compareTo(inputFileTime) > 0) {
                if (document.isVerbose()) {
                    System.out.println("The output file is up-to-date. Skipping: "
                            + document.getOutputLocation());
                }
                return;
            }
        }

        String mdText = Utils.readStringFromUtf8File(inputFile);

        plugins.forEach(plugin -> plugin.newPage(document));
        mdText = metadataHandlersWrapper.applyMetadataHandlers(mdText, document);

        Map<String, Object> substitutions = new HashMap<>();
        String title = document.getTitle();

        if (title == null) {
            title = "";
        }

        String htmlText = generateHtml(mdText);

        substitutions.put(TITLE_PLACEHOLDER, title);
        substitutions.put(CONTENT_PLACEHOLDER, htmlText);

        substitutions.put(STYLES_PLACEHOLDER, Md2HtmlUtils.generateDocumentStyles(document));

        substitutions.put(EXEC_NAME_PLACEHOLDER, Constants.EXEC_NAME);
        substitutions.put(EXEC_VERSION_PLACEHOLDER, Constants.EXEC_VERSION);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        substitutions.put(GENERATION_DATE_PLACEHOLDER, dateTime.format(dateFormatter));
        substitutions.put(GENERATION_TIME_PLACEHOLDER, dateTime.format(timeFormatter));

        for (Md2HtmlPlugin plugin : plugins) {
            substitutions.putAll(plugin.variables(document));
        }

        if (options.isLegacyMode()) {
            Map<String, Object> placeholders = null;
            try {
                //noinspection unchecked
                placeholders = (Map<String, Object>) substitutions.get("placeholders");
            } catch (Exception e) {
                // Deliberate ignore.
            }
            if (placeholders != null) {
                substitutions.remove("placeholders");
                substitutions.putAll(placeholders);
            }
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile.toFile()), StandardCharsets.UTF_8))) {
            Mustache mustache;
            try {
                if (options.isLegacyMode()) {
                    mustache = MustacheUtils.createCachedMustacheRendererLegacy(document.getTemplate());
                } else {
                    mustache = MustacheUtils.createCachedMustacheRenderer(document.getTemplate());
                }
            } catch (FileNotFoundException e) {
                throw new UserError(String.format("Error reading template file '%s': %s: %s",
                        document.getTemplate().toString(), e.getClass().getSimpleName(),
                        e.getMessage()));
            }
            mustache.execute(writer, substitutions);
            writer.flush();
        }

        if (document.isVerbose()) {
            System.out.println("Output file generated: " + document.getOutputLocation());
        }
        if (document.isReport()) {
            System.out.println(document.getOutputLocation());
        }
    }

    private static String generateHtml(String mdText) {

        MutableDataSet options = new MutableDataSet();
        options.set(
                Parser.EXTENSIONS,
                Arrays.asList(
                        TablesExtension.create(),
                        //StrikethroughExtension.create(),
                        TocExtension.create(),
                        //InsExtension.create(),
                        TypographicExtension.create(),
                        PythonMarkdownCompatibleAdmonitionExtension.create()
                ));
        options.set(TocExtension.LEVELS, 126); // generate ToC for all header levels

        // TODO Try to wrap table of contents into a `div` block.
        //  It's really unclear how to do it.
        //  See https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension

        //options.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(mdText);
        return renderer.render(document);
    }

}
