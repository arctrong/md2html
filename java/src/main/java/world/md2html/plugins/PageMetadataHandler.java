package world.md2html.plugins;

import world.md2html.options.model.Document;
import world.md2html.pagemetadata.MetadataProcessingPhase;
import world.md2html.pagemetadata.MetadataProcessingResult;
import world.md2html.utils.UserError;

import java.util.Set;

public interface PageMetadataHandler {

    /**
     * Accepts document {@code document} where the {@code metadata} was found, the metadata
     * {@code marker}, the {@code metadata} itself and the whole section {@code metadataSection}
     * from which the {@code metadata} was extracted.
     * Returns the processing result that must replace the metadata section in the source text.
     * If the plugin itself processes metadata in its own content, it must send forward the
     * provided {@code visitedMarkers} collection and state the key that must be used for cycle
     * detection (see the existing plugins for examples).
     */
    MetadataProcessingResult acceptPageMetadata(Document document, String marker, String metadata,
            String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException;

    default MetadataProcessingResult acceptPageMetadata(Document document, String marker,
            String metadata, String metadataSection, Set<String> visitedMarkers,
            MetadataProcessingPhase phase, Object dataFromPrevPhase
    ) throws PageMetadataException {
        if (phase == MetadataProcessingPhase.PHASE_2) {
            throw new PageMetadataException(
                    "Deferred result encountered when processing metadata marker '" + marker +
                    "' on phase 2. This may mean that this marker cannot be nested " +
                    "inside the other metadata block.");
        }
        return acceptPageMetadata(document, marker, metadata, metadataSection, visitedMarkers);
    }

    class PageMetadataException extends UserError {
        public PageMetadataException(String message) {
            super(message);
        }
    }

}
