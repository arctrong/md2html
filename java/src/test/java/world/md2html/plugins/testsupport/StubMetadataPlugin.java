package world.md2html.plugins.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.MetadataProcessingResult;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageMetadataHandler;
import world.md2html.plugins.PageMetadataHandlerInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Test-only plugin that registers configurable metadata markers without production plugin config.
 */
public class StubMetadataPlugin implements Md2HtmlPlugin, PageMetadataHandler {

    public static final class MarkerSpec {
        private final String marker;
        private final boolean onlyAtPageStart;

        public MarkerSpec(String marker, boolean onlyAtPageStart) {
            this.marker = marker;
            this.onlyAtPageStart = onlyAtPageStart;
        }

        public static MarkerSpec anywhere(String marker) {
            return new MarkerSpec(marker, false);
        }

        public static MarkerSpec atPageStart(String marker) {
            return new MarkerSpec(marker, true);
        }
    }

    private final List<PageMetadataHandlerInfo> handlerInfos;

    public StubMetadataPlugin(MarkerSpec... markerSpecs) {
        List<PageMetadataHandlerInfo> infos = new ArrayList<>();
        for (MarkerSpec spec : markerSpecs) {
            infos.add(new PageMetadataHandlerInfo(this, spec.marker, spec.onlyAtPageStart));
        }
        this.handlerInfos = Collections.unmodifiableList(infos);
    }

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        throw new UnsupportedOperationException("Not used in stub plugin tests");
    }

    @Override
    public boolean isBlank() {
        return handlerInfos.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return handlerInfos;
    }

    @Override
    public MetadataProcessingResult acceptPageMetadata(Document document, String marker,
            String metadata, String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException {
        return MetadataProcessingResult.immediate(metadata);
    }
}
