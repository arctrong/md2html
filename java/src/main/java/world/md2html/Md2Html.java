package world.md2html;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.cli.ParseException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import world.md2html.options.*;
import world.md2html.utils.Utils;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;

public class Md2Html {

    private static final String TEMPLATE_FILE_NAME = "template.html";
    private static final String CSS_FILE_NAME = "styles.css";

    private static final String TITLE_PLACEHOLDER = "title";
    private static final String STYLES_PLACEHOLDER = "styles";
    private static final String CONTENT_PLACEHOLDER = "content";

    public static void main(String[] args) throws Exception {

        CliParser cliParser = new CliParser();
        CliParsingResult cliParsingResult = null;
        try {
            cliParsingResult = cliParser.getMd2HtmlOptions(args);
        } catch (CliArgumentsException | ParseException e) {
            cliParser.getFeedbackHelper().printError(e.getMessage());
            System.exit(1);
        }
        if (cliParsingResult.getResultType() == CliParsingResultType.HELP) {
            cliParser.getFeedbackHelper().printHelp();
        } else if (cliParsingResult.getResultType() == CliParsingResultType.SUCCESS) {
            md2html(cliParsingResult.getOptions());
        }
    }

    public static void md2html(Md2HtmlOptions options) throws Exception {

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
        String htmlText = generateHtml(mdText);

        VelocityEngine velocityEngine = new VelocityEngine();
        String fileResourceLoaderPath = options.getTemplateDir().toUri().getPath();
        velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
                fileResourceLoaderPath);
        velocityEngine.init();
        Template velocityTemplate = velocityEngine.getTemplate(TEMPLATE_FILE_NAME);
        VelocityContext context = new VelocityContext();
        context.put(TITLE_PLACEHOLDER, options.getTitle());

        if (options.getLinkCss() != null && !options.getLinkCss().isEmpty()) {
            context.put(STYLES_PLACEHOLDER, "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                    options.getLinkCss() + "\">");
        } else {
            context.put(STYLES_PLACEHOLDER, "<style>\n"
                    + Utils.readStringFromUtf8File(options.getTemplateDir().resolve(CSS_FILE_NAME))
                    + "\n</style>");
        }
        context.put(CONTENT_PLACEHOLDER, htmlText);

        try (Writer out = Files.newBufferedWriter(options.getOutputFile())) {
            velocityTemplate.merge(context, out);
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
                        StrikethroughExtension.create(),
                        TocExtension.create(),
                        InsExtension.create(),
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
