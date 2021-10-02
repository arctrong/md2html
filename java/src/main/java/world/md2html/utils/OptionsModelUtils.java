package world.md2html.utils;

import world.md2html.Constants;
import world.md2html.options.cli.ClilOptions;
import world.md2html.options.model.Document;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static world.md2html.utils.Utils.isNullOrEmpty;

public class OptionsModelUtils {

    private static final String DEFAULT_TEMPLATE_FILE = "doc_src/templates/default.html";
    private static final String DEFAULT_CSS_FILE = "doc/styles.css";

    private OptionsModelUtils() {
    }

    public static Document enrichDocumentMd2HtmlOptions(Document document) {
        return new Document(document.getInputLocation(),
                document.getOutputLocation() == null ?
                        Utils.stripExtension(document.getInputLocation()) + ".html" :
                        document.getOutputLocation(),
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

//    public static List<ClilOptions> enrichDocumentMd2HtmlOptionsList(
//            List<ClilOptions> optionsList) {
//        List<ClilOptions> result = new ArrayList<>();
//        for (ClilOptions opt : optionsList) {
//            result.add(OptionsModelUtils.enrichDocumentMd2HtmlOptions(opt));
//        }
//        return result;
//    }

}
