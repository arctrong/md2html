package world.md2html.plugins;

import world.md2html.options.model.Document;
import world.md2html.utils.UserError;

public interface PageMetadataHandler {

    /**
     * Accepts document `doc` where the `metadata` was found, the metadata `marker`, the
     * `metadata` itself and the whole section `metadata_section` from which the `metadata`
     * was extracted.
     * Returns the text that must replace the metadata section in the source text.
     */
    String acceptPageMetadata(Document document, String marker, String metadata,
            String metadataSection) throws PageMetadataException;

    class PageMetadataException extends UserError {
        public PageMetadataException(String message) {
            super(message);
        }
    }

}
