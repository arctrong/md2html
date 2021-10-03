package world.md2html.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PageMetadataHandlerInfo {

    private final PageMetadataHandler pageMetadataHandler;
    private final String marker;
    /**
     * If `true` then the handler accepts only the metadata sections that are the first non-blank
     * content on the page, if `false` then the handler accepts all metadata on the page.
     */
    private final boolean onlyAtPageStart;

}
