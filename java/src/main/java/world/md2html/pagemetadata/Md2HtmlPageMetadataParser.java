package world.md2html.pagemetadata;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Md2HtmlPageMetadataParser {

    /**
     * Private constructor prevent instantiation.
     */
    private Md2HtmlPageMetadataParser() {
    }

    public static PageMetadataParsingResult parse(String metadataSection) {

        List<String> errors = new ArrayList<>();

        if (metadataSection == null) {
            return new PageMetadataParsingResult(false, null, errors);
        }

        JsonObject jsonObject;
        try {
            jsonObject = Json.parse(metadataSection).asObject();
        } catch (ParseException | UnsupportedOperationException e) {
                errors.add("Skipping because page metadata cannot be parsed: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                return new PageMetadataParsingResult(false, null, errors);
        }
        if (jsonObject == null) {
            errors.add("Page metadata is a null object, skipping");
            return new PageMetadataParsingResult(false, null, errors);
        }

        String title = null;
        Map<String, String> customTemplatePlaceholders = new HashMap<>();

        JsonValue titleObject = jsonObject.get("title");
        if (titleObject != null) {
            try {
                title = titleObject.asString();
            } catch (UnsupportedOperationException e) {
                errors.add("Title in page metadata cannot be taken as a string: " +
                        e.getMessage());
            }
        }

        JsonValue placeholdersValue = jsonObject.get("custom_template_placeholders");
        if (placeholdersValue != null) {
            JsonObject placeholdersObject = null;
            try {
                placeholdersObject = placeholdersValue.asObject();
            } catch (UnsupportedOperationException e) {
                errors.add("Custom template placeholders cannot be taken from page metadata: "
                        + e.getMessage());
            }
            if (placeholdersObject != null) {
                for (JsonObject.Member member : placeholdersObject) {
                    String placeholderValue;
                    try {
                        placeholderValue = member.getValue().asString();
                    } catch (UnsupportedOperationException e) {
                        errors.add("Custom template placeholder '" + member.getName() +
                                "' in page metadata cannot be taken as a string: "
                                + e.getMessage());
                        continue;
                    }
                    customTemplatePlaceholders.put(member.getName(), placeholderValue);
                }
            }
        }
        return new PageMetadataParsingResult(true, new PageMetadata(title,
                customTemplatePlaceholders), errors);
    }

}
