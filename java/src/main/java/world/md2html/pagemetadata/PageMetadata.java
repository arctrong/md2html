package world.md2html.pagemetadata;

import java.util.Map;

public class PageMetadata {

    private String title;
    private Map<String, String> customTemplatePlaceholders;

    public PageMetadata(String title,
            Map<String, String> customTemplatePlaceholders) {
        this.title = title;
        this.customTemplatePlaceholders = customTemplatePlaceholders;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, String> getCustomTemplatePlaceholders() {
        return customTemplatePlaceholders;
    }
}
