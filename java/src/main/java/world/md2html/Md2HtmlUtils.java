package world.md2html;

import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.file.Paths;
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
}
