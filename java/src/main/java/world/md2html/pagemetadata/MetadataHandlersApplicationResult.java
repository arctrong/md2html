package world.md2html.pagemetadata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class MetadataHandlersApplicationResult {
    private final List<ParsingResultItem> parsingResults;
    private final boolean deferPage;
}
