package world.md2html.pagemetadata;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Md2HtmlPageMetadataExtractor {

    /**
     * Private constructor prevent instantiation.
     */
    private Md2HtmlPageMetadataExtractor() {
    }

    public static PageMetadataExtractionResult extract(String pageText) {
        if (pageText == null) {
            return new PageMetadataExtractionResult(false, null, -1, -1);
        }
        Matcher matcher = Pattern.compile("^\\s*(<!--METADATA(.*?)-->)",
                Pattern.CASE_INSENSITIVE + Pattern.DOTALL)
                .matcher(pageText);
        if (matcher.find()) {
            return new PageMetadataExtractionResult(true, matcher.group(2), matcher.start(1),
                    matcher.end(1));
        } else {
            return new PageMetadataExtractionResult(false, null, -1, -1);
        }
    }

}
