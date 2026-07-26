package world.md2html.pagemetadata;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ParsingResultItem {

    private final Object result;
    private final String marker;
    private final String metadata;
    private final String metadataSection;
    private final PageMetadataHandlersWrapper.MarkerKey markerKey;

    public static ParsingResultItem text(String result) {
        return new ParsingResultItem(result, null, null, null, null);
    }

    public static ParsingResultItem deferred(Object phase2Data, String marker, String metadata,
            String metadataSection, PageMetadataHandlersWrapper.MarkerKey markerKey) {
        return new ParsingResultItem(phase2Data, marker, metadata, metadataSection, markerKey);
    }

    public boolean isDeferred() {
        return markerKey != null;
    }
}
