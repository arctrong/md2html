package world.md2html.pagemetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import world.md2html.options.model.Document;

@Getter
@AllArgsConstructor
public class DeferredPage {

    private final Document document;
    private final MetadataHandlersApplicationResult applicationResult;
}
