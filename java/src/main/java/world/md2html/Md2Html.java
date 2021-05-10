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

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Md2Html {

    private static final String TEMPLATE_FILE_NAME = "template.html";

    private static final String TITLE_PLACEHOLDER = "title";
    private static final String STYLES_PLACEHOLDER = "styles";
    private static final String CONTENT_PLACEHOLDER = "content";

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

            Matcher matcher = Pattern.compile("^\\s*<!--METADATA\\s+(.*?)\\s*-->",
                    Pattern.CASE_INSENSITIVE + Pattern.DOTALL)
                    .matcher(mdText);
            if (matcher.find()) {
                JsonObject jsonObject = null;
                try {
                    jsonObject = Json.parse(matcher.group(1)).asObject();
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
        String linkCss = options.getLinkCss();
        Path includeCss = options.getIncludeCss();
        if (linkCss != null && !linkCss.isEmpty()) {
            substitutions.put(STYLES_PLACEHOLDER,
                    "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + linkCss + "\">");
        } else if (includeCss != null) {
            substitutions.put(STYLES_PLACEHOLDER, "<style>\n"
                    + Utils.readStringFromUtf8File(includeCss)
                    + "\n</style>");
        } else {
            substitutions.put(STYLES_PLACEHOLDER, "");
        }

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
