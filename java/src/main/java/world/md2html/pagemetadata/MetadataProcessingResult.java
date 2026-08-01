package world.md2html.pagemetadata;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MetadataProcessingResult {

    private final Object result;
    private final boolean defer;

    public static MetadataProcessingResult immediate(String text) {
        return new MetadataProcessingResult(text, false);
    }

    public static MetadataProcessingResult deferred(Object dataForPhase2) {
        return new MetadataProcessingResult(dataForPhase2, true);
    }

    public String getResultAsString() {
        return (String) result;
    }
}
