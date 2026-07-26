package world.md2html.plugins.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.MetadataProcessingPhase;
import world.md2html.pagemetadata.MetadataProcessingResult;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageMetadataHandler;
import world.md2html.plugins.PageMetadataHandlerInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DeferPhaseTestPlugin implements Md2HtmlPlugin, PageMetadataHandler {

    public static final String MARKER = "DEFER_TEST";

    private boolean active;

    public void activate() {
        this.active = true;
    }

    @Override
    public void acceptData(JsonNode data) {
        activate();
    }

    @Override
    public boolean isBlank() {
        return !active;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return Collections.singletonList(new PageMetadataHandlerInfo(this, MARKER, false));
    }

    @Override
    public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
            String metadata, String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException {
        return acceptPageMetadata(document, marker, metadata, metadataSection, visitedMarkers,
                MetadataProcessingPhase.PHASE_1, null);
    }

    @Override
    public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
            String metadata, String metadataSection, Set<String> visitedMarkers,
            MetadataProcessingPhase phase, Object dataFromPrevPhase
    ) throws PageMetadataException {
        if (phase == MetadataProcessingPhase.PHASE_1) {
            String trimmed = metadata.trim();
            if ("defer".equals(trimmed)) {
                return MetadataProcessingResult.deferred(trimmed);
            }
            return MetadataProcessingResult.immediate(trimmed);
        }
        return MetadataProcessingResult.immediate("resolved:" + dataFromPrevPhase);
    }
}
