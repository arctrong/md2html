package world.md2html.pagemetadata.legacy;

import java.util.List;

public class PageMetadataParsingResult {

    private final boolean success;
    private final PageMetadata pageMetadata;
    private final List<String> errors;

    public PageMetadataParsingResult(boolean success, PageMetadata pageMetadata,
            List<String> errors) {
        this.pageMetadata = pageMetadata;
        this.success = success;
        this.errors = errors;
    }

    public PageMetadata getPageMetadata() {
        return pageMetadata;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrors() {
        return errors;
    }

}
