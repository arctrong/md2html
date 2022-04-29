package world.md2html.options.model;

import world.md2html.Constants;
import world.md2html.utils.Utils;

import java.nio.file.Paths;
import java.util.Collections;

import static world.md2html.utils.Utils.isNullOrEmpty;

public class OptionsModelUtils {

    private static final String DEFAULT_TEMPLATE_FILE = "doc_src/templates/default.html";
    private static final String DEFAULT_CSS_FILE = "doc/styles.css";

    private OptionsModelUtils() {
    }

    public static Document enrichDocument(Document document, String inputRoot, String outputRoot) {
        String outputLocation = document.getOutputLocation() == null ?
                Utils.stripExtension(document.getInputLocation()) + ".html" :
                document.getOutputLocation();
        String inputLocation = inputRoot == null ? document.getInputLocation() :
            Paths.get(inputRoot).resolve(document.getInputLocation()).toString()
                    .replace('\\', '/');
        if (outputRoot != null) {
            outputLocation = Paths.get(outputRoot).resolve(outputLocation).toString()
                    .replace('\\', '/');
        }

        return new Document(inputLocation,
                outputLocation,
                document.getTitle(),
                document.getTemplate() == null ? Constants.WORKING_DIR.resolve(
                        DEFAULT_TEMPLATE_FILE) : document.getTemplate(),
                !document.isNoCss() && isNullOrEmpty(document.getIncludeCss()) &&
                        isNullOrEmpty(document.getLinkCss()) ?
                        Collections.singletonList(Constants.WORKING_DIR.resolve(DEFAULT_CSS_FILE)) :
                        document.getIncludeCss(),
                document.getLinkCss(), document.isNoCss(), document.isForce(), document.isVerbose(),
                document.isReport());
    }

}
