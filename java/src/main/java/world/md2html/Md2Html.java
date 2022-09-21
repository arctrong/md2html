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
import world.md2html.utils.CheckedIllegalArgumentException;
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

import static world.md2html.utils.MustacheUtils.createCachedMustacheRenderer;
import static world.md2html.utils.MustacheUtils.createCachedMustacheRendererLegacy;
import static world.md2html.utils.Utils.*;

public class Md2Html {

    private static final String TITLE_PLACEHOLDER = "title";
    private static final String STYLES_PLACEHOLDER = "styles";
    private static final String CONTENT_PLACEHOLDER = "content";
    private static final String EXEC_NAME_PLACEHOLDER = "exec_name";
    private static final String EXEC_VERSION_PLACEHOLDER = "exec_version";
    private static final String GENERATION_DATE_PLACEHOLDER = "generation_date";
    private static final String GENERATION_TIME_PLACEHOLDER = "generation_time";
    private static final String SOURCE_FILE_PLACEHOLDER = "source_file";

    public static void execute(Document document, List<Md2HtmlPlugin> plugins,
            PageMetadataHandlersWrapper metadataHandlersWrapper, SessionOptions options)
            throws IOException, UserError {

        Path outputFile = Paths.get(document.getOutput());
        Path inputFile = Paths.get(document.getInput());

        if (!document.isForce() && Files.exists(outputFile)) {
            FileTime inputFileTime = Files.getLastModifiedTime(inputFile);
            FileTime outputFileTime = Files.getLastModifiedTime(outputFile);
            if (outputFileTime.compareTo(inputFileTime) > 0) {
                if (document.isVerbose()) {
                    System.out.println("The output file is up-to-date. Skipping: "
                            + document.getOutput());
                }
                return;
            }
        }

        for (Md2HtmlPlugin plugin : plugins) {
            plugin.newPage(document);
        }

        String mdText = getCachedString(inputFile, Utils::readStringFromUtf8File);
        mdText = metadataHandlersWrapper.applyMetadataHandlers(mdText, document);

        Map<String, Object> substitutions = new HashMap<>();

        String htmlText = generateHtml(mdText);

        substitutions.put(CONTENT_PLACEHOLDER, htmlText);
        try {
            substitutions.put(SOURCE_FILE_PLACEHOLDER,
                    relativizeRelativeResource(document.getInput(), document.getOutput()));
        } catch (CheckedIllegalArgumentException e) {
            throw new RuntimeException(e);
        }

        outputPage(document, plugins, substitutions, options);

        if (document.isVerbose()) {
            System.out.println("Output file generated: " + document.getOutput());
        }
        if (document.isReport()) {
            System.out.println(document.getOutput());
        }
    }

    public static void outputPage(Document document, List<Md2HtmlPlugin> plugins,
            Map<String, Object> substitutions, SessionOptions options) {

        // TODO Probably move to `Md2HtmlUtils`.

        substitutions = new HashMap<>(substitutions);

        substitutions.put(TITLE_PLACEHOLDER, firstNotNull(document.getTitle(), ""));
        substitutions.put(EXEC_NAME_PLACEHOLDER, Constants.EXEC_NAME);
        substitutions.put(EXEC_VERSION_PLACEHOLDER, Constants.EXEC_VERSION);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        substitutions.put(GENERATION_DATE_PLACEHOLDER, dateTime.format(dateFormatter));
        substitutions.put(GENERATION_TIME_PLACEHOLDER, dateTime.format(timeFormatter));

        substitutions.put(STYLES_PLACEHOLDER, Md2HtmlUtils.generateDocumentStyles(document));

        for (Md2HtmlPlugin plugin : plugins) {
            substitutions.putAll(plugin.variables(document));
        }

        if (options.isLegacyMode()) {
            Map<String, Object> placeholders = null;
            try {
                //noinspection unchecked
                placeholders = (Map<String, Object>) substitutions.get("placeholders");
            } catch (Exception e) {
                // Intentional ignore.
            }
            if (placeholders != null) {
                substitutions.remove("placeholders");
                substitutions.putAll(placeholders);
            }
        }

        // TODO Decide whether it's required.
        substitutions.putIfAbsent(TITLE_PLACEHOLDER, "");

        Path  outputDirPath = Paths.get((document.getOutput())).normalize().getParent();
        if (outputDirPath != null && !Files.exists(outputDirPath)) {
            try {
                Files.createDirectories(outputDirPath);
            } catch (IOException e) {
                throw new RuntimeException("Could not create output file directory structure: " +
                        outputDirPath, e);
            }
        }
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(document.getOutput()), StandardCharsets.UTF_8))) {
            Mustache mustache;
            try {
                if (options.isLegacyMode()) {
                    mustache = createCachedMustacheRendererLegacy(Paths.get(document.getTemplate()));
                } else {
                    mustache = createCachedMustacheRenderer(Paths.get(document.getTemplate()));
                }
            } catch (FileNotFoundException e) {
                throw new UserError(String.format("Error reading template file '%s': %s: %s",
                        document.getTemplate(), e.getClass().getSimpleName(),
                        e.getMessage()));
            }
            mustache.execute(writer, substitutions);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateHtml(String mdText) {
        MutableDataSet options = new MutableDataSet()
                .set(
                        Parser.EXTENSIONS,
                        Arrays.asList(
                                TablesExtension.create(),
                                //StrikethroughExtension.create(),
                                TocExtension.create(),
                                //InsExtension.create(),
                                TypographicExtension.create(),
                                PythonMarkdownCompatibleAdmonitionExtension.create()
                        ))
                .set(TocExtension.LEVELS, 126) // generate ToC for all header levels
                // TODO looks like the following option doesn't work. Probably need to
                //  rewrite to `TypographicExtension` in order to use only `EM_DASH`.
                .set(TypographicExtension.EN_DASH, "--")
                .set(TypographicExtension.ENABLE_QUOTES, false); // disable &ndash;

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
