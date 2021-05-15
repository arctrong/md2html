package world.md2html;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Md2HtmlUtils {

    /**
     * Private constructor prevent instantiation.
     */
    private Md2HtmlUtils() {
    }

    public static Optional<String> extractPageMetadataSection(String pageText) {
        if (pageText == null) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("^\\s*<!--METADATA(.*?)-->",
                Pattern.CASE_INSENSITIVE + Pattern.DOTALL)
                .matcher(pageText);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }

}
