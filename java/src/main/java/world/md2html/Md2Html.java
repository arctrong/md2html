package world.md2html;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.text.StringSubstitutor;
import world.md2html.options.Md2HtmlOptions;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Md2Html {

    private static final String TEMPLATE_FILE_NAME = "template.html";

    private static final String TITLE_PLACEHOLDER = "title";
    private static final String STYLES_PLACEHOLDER = "styles";
    private static final String CONTENT_PLACEHOLDER = "content";
    private static final String EXEC_NAME_PLACEHOLDER = "exec_name";
    private static final String EXEC_VERSION_PLACEHOLDER = "exec_version";
    private static final String GENERATION_DATE_PLACEHOLDER = "generation_date";
    private static final String GENERATION_TIME_PLACEHOLDER = "generation_time";

    public static void execute(Md2HtmlOptions options) throws Exception {

        if (!options.isForce() && Files.exists(options.getOutputFile())) {
            FileTime inputFileTime = Files.getLastModifiedTime(options.getInputFile());
            FileTime outputFileTime = Files.getLastModifiedTime(options.getOutputFile());
            if (outputFileTime.compareTo(inputFileTime) > 0) {
                if (options.isVerbose()) {
                    System.out.println("The output file is up-to-date. Skipping: "
                            + options.getOutputFile());
                }
                return;
            }
        }

        String mdText = Utils.readStringFromUtf8File(options.getInputFile());

        String title = options.getTitle();
        // Trying to get title from metadata.
        // If adding other parameters, need to remove this condition.
        if (title == null) {

            Optional<String> metadata = Md2HtmlUtils.extractPageMetadataSection(mdText);
            if (metadata.isPresent()) {
                JsonObject jsonObject = null;
                try {
                    jsonObject = Json.parse(metadata.get()).asObject();
                } catch (ParseException | UnsupportedOperationException e) {
                    if (options.isVerbose()) {
                        System.out.println("WARNING: Page metadata cannot be parsed: "
                                + e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                }
                if (jsonObject != null) {
                    JsonValue titleObject = jsonObject.get("title");
                    if (titleObject != null) {
                        try {
                            title = titleObject.asString();
                        } catch (UnsupportedOperationException e) {
                            System.out.println("WARNING: Title cannot be taken from page metadata: "
                                    + e.getMessage());
                        }
                    }
                }
            }
        }
        if (title == null) {
            title = "";
        }

        String htmlText = generateHtml(mdText);

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put(TITLE_PLACEHOLDER, title);
        substitutions.put(CONTENT_PLACEHOLDER, htmlText);

        StringBuilder styles = new StringBuilder();
        boolean[] firstStyle = {true};

        Consumer<String> styleAppender = item -> {
            if (!firstStyle[0]) {
                styles.append("\n");
            }
            styles.append(item);
            firstStyle[0] = false;
        };

        if (options.getLinkCss() != null) {
            options.getLinkCss().forEach(item -> styleAppender
                    .accept("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + item + "\">"));
        }
        if (options.getIncludeCss() != null) {
            options.getIncludeCss().forEach(item -> {
                try {
                    styleAppender.accept("<style>\n" + Utils.readStringFromUtf8File(item)
                            + "\n</style>");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        substitutions.put(STYLES_PLACEHOLDER, styles.toString());

        substitutions.put(EXEC_NAME_PLACEHOLDER, Constants.EXEC_NAME);
        substitutions.put(EXEC_VERSION_PLACEHOLDER, Constants.EXEC_VERSION);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        substitutions.put(GENERATION_DATE_PLACEHOLDER, dateTime.format(dateFormatter));
        substitutions.put(GENERATION_TIME_PLACEHOLDER, dateTime.format(timeFormatter));

        StringSubstitutor stringSubstitutor = new StringSubstitutor(substitutions);
        stringSubstitutor.setEnableUndefinedVariableException(false);
        stringSubstitutor.setDisableSubstitutionInValues(true);

        String template = Utils.readStringFromUtf8File(options.getTemplateDir()
                .resolve(TEMPLATE_FILE_NAME));
        try (Writer out = Files.newBufferedWriter(options.getOutputFile())) {
            out.write(stringSubstitutor.replace(template));
        }

        if (options.isVerbose()) {
            System.out.println("Output file generated: " + options.getOutputFile());
        }
        if (options.isReport()) {
            System.out.println(options.getOutputFile().toString());
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
                        TypographicExtension.create()
                ));
        options.set(TocExtension.LEVELS, 7); // generate ToC for all header levels
        options.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(mdText);
        return renderer.render(document);
    }

}
