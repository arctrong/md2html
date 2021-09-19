package world.md2html.options;

import world.md2html.Constants;
import world.md2html.utils.Utils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static world.md2html.utils.Utils.isNullOrEmpty;

public class Md2HtmlOptionUtils {

    private static final String DEFAULT_TEMPLATE_FILE = "doc_src/templates/default.html";
    private static final String DEFAULT_CSS_FILE = "doc/styles.css";

    private Md2HtmlOptionUtils() {
    }

    public static Md2HtmlOptions enrichDocumentMd2HtmlOptions(Md2HtmlOptions options) {
        return new Md2HtmlOptions(options.getArgumentFile(), options.getInputFile(),
                options.getOutputFile() == null ?
                        Paths.get(
                                Utils.stripExtension(options.getInputFile().toString()) + ".html") :
                        options.getOutputFile(),
                options.getTitle(),
                options.getTemplate() == null ? Constants.WORKING_DIR.resolve(
                        DEFAULT_TEMPLATE_FILE) :
                        options.getTemplate(),
                isNullOrEmpty(options.getIncludeCss()) && isNullOrEmpty(options.getLinkCss()) &&
                        !options.isNoCss() ?
                        Collections.singletonList(Constants.WORKING_DIR.resolve(DEFAULT_CSS_FILE)) :
                        options.getIncludeCss(),
                options.getLinkCss(), options.isNoCss(), options.isForce(), options.isVerbose(),
                options.isReport());
    }

    public static List<Md2HtmlOptions> enrichDocumentMd2HtmlOptionsList(
            List<Md2HtmlOptions> optionsList) {
        List<Md2HtmlOptions> result = new ArrayList<>();
        for (Md2HtmlOptions opt : optionsList) {
            result.add(Md2HtmlOptionUtils.enrichDocumentMd2HtmlOptions(opt));
        }
        return result;
    }

}
