package world.md2html.pagemetadata.legacy;

public class PageMetadataExtractionResult {

    private final boolean success;
    private final String metadata;
    private final int start;
    private final int end;

    public PageMetadataExtractionResult(boolean success, String metadata, int start, int end) {
        this.success = success;
        this.metadata = metadata;
        this.start = start;
        this.end = end;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMetadata() {
        return metadata;
    }

    public int getEnd() {
        return end;
    }

    public int getStart() {
        return start;
    }

}
