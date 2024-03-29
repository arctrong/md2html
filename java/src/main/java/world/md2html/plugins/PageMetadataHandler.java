package world.md2html.plugins;

import world.md2html.options.model.Document;
import world.md2html.utils.UserError;

import java.util.Set;

public interface PageMetadataHandler {

    /**
     * Accepts document `doc` where the `metadata` was found, the metadata `marker`, the
     * `metadata` itself and the whole section `metadata_section` from which the `metadata`
     * was extracted.
     * Returns the text that must replace the metadata section in the source text.
     *If the plugin itself processes metadata in its own content, it must:
     *send forward the provided `visited_markers` collection
     *and state the key that must be used for cycle detection
     *(see the existing plugins for examples).
     */
    String acceptPageMetadata(Document document, String marker, String metadata,
                              String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException;

    class PageMetadataException extends UserError {
        public PageMetadataException(String message) {
            super(message);
        }
    }

}
