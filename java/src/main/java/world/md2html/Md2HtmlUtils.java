package world.md2html;

import world.md2html.options.model.Document;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.util.function.Consumer;

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
            document.getLinkCss().forEach(item -> styleAppender
                    .accept("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + item + "\">"));
        }
        if (document.getIncludeCss() != null) {
            document.getIncludeCss().forEach(item -> {
                try {
                    styleAppender.accept("<style>\n" + Utils.readStringFromUtf8File(item)
                            + "\n</style>");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return styles.toString();
    }
}
