package world.md2html;

import com.github.mustachejava.Mustache;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static world.md2html.Md2HtmlUtils.generateDocumentStyles;
import static world.md2html.Md2HtmlUtils.generateHtml;
import static world.md2html.utils.MustacheUtils.createCachedMustacheRenderer;
import static world.md2html.utils.MustacheUtils.createCachedMustacheRendererLegacy;
import static world.md2html.utils.Utils.firstNotNull;
import static world.md2html.utils.Utils.getCachedString;
import static world.md2html.utils.Utils.relativizeRelativeResource;
import static world.md2html.utils.Utils.supplyWithFileExceptionAsUserError;

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

        String mdText = supplyWithFileExceptionAsUserError(
                () -> getCachedString(inputFile, Utils::readStringFromUtf8File),
                "Error processing page"
        );

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

        outputPage(document, plugins, substitutions, options, null);

        if (document.isVerbose()) {
            System.out.println("Output file generated: " + document.getOutput());
        }
        if (document.isReport()) {
            System.out.println(document.getOutput());
        }
    }

    public static void outputPage(Document document, List<Md2HtmlPlugin> plugins,
                                  Map<String, Object> substitutions, SessionOptions options,
                                  Map<String, Object> overrideSubstitutions) {

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

        substitutions.put(STYLES_PLACEHOLDER, generateDocumentStyles(document));

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

        if (overrideSubstitutions != null) {
            substitutions.putAll(overrideSubstitutions);
        }

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
                Files.newOutputStream(Paths.get(document.getOutput())), StandardCharsets.UTF_8))) {
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

}
