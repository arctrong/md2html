package world.md2html;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import world.md2html.extentions.admonition.PythonMarkdownCompatibleAdmonitionExtension;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;

import static world.md2html.utils.Utils.relativizeRelativeResource;

public class Md2HtmlUtils {

    private Md2HtmlUtils() {
    }

    public static String generateDocumentStyles(Document document) {
        StringBuilder styles = new StringBuilder();
        boolean[] firstStyle = {true};

        Consumer<String> styleAppender = item -> {
            if (!firstStyle[0]) {
                styles.append("\n");
            }
            styles.append(item);
            firstStyle[0] = false;
        };

        if (document.getLinkCss() != null) {
            document.getLinkCss().forEach(css -> {
                String relativizedCss;
                try {
                    relativizedCss = relativizeRelativeResource(css, document.getOutput());
                } catch (CheckedIllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
                // TODO Consider applying HTML encoding to the `href` value. Not sure
                //  it's required.
                styleAppender.accept("<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                                relativizedCss + "\">");
            });
        }
        if (document.getIncludeCss() != null) {
            document.getIncludeCss().forEach(item -> {
                try {
                    styleAppender.accept("<style>\n" +
                            Utils.readStringFromUtf8File(Paths.get(item)) + "\n</style>");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return styles.toString();
    }

    public static String generateHtml(String mdText) {
        MutableDataSet options = new MutableDataSet()
                .set(
                        Parser.EXTENSIONS,
                        Arrays.asList(
                                TablesExtension.create(),
                                //StrikethroughExtension.create(),
                                TocExtension.create(),
                                //InsExtension.create(),
                                TypographicExtension.create(),
                                PythonMarkdownCompatibleAdmonitionExtension.create(),
                                AttributesExtension.create()
                        ))
                .set(TocExtension.LEVELS, 126) // generate ToC for all header levels
                // TODO looks like the following option doesn't work. Probably need to
                //  rewrite to `TypographicExtension` in order to use only `EM_DASH`.
                .set(TypographicExtension.EN_DASH, "--")
                .set(TypographicExtension.ENABLE_QUOTES, false) // disable &ndash;
                .set(AttributesExtension.ASSIGN_TEXT_ATTRIBUTES, false);

        // TODO Try to wrap table of contents into a `div` block.
        //  It's really unclear how to do it.
        //  See https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(mdText);
        return renderer.render(document);
    }
}
