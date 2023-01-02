package world.md2html.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.NullNode;
import org.apache.commons.lang3.StringUtils;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;

public class IgnorePlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private List<String> markers;
    private List<PageMetadataHandlerInfo> pageLinksHandlers = new ArrayList<>();

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/ignore_schema.json");
        JsonNode markersNode = data.get("markers");
        if (markersNode == null || markersNode instanceof NullNode) {
            this.markers = null;
        } else {
            ObjectReader reader = OBJECT_MAPPER.readerFor(new TypeReference<List<String>>() {});
            ArrayList<String> list;
            try {
                list = reader.readValue(markersNode);
            } catch (IOException e) {
                throw new ArgFileParseException("Error reading plugin '" +
                        this.getClass().getSimpleName() + "' data: " + data);
            }
            this.markers = list;
        }
        if (this.markers == null || this.markers.isEmpty()) {
            this.markers = Collections.singletonList("ignore");
        }

        this.pageLinksHandlers = this.markers.stream()
                .map(m -> new PageMetadataHandlerInfo(this, m, false))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isBlank() {
        return this.pageLinksHandlers.isEmpty();
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.pageLinksHandlers;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
            String metadataSection) throws PageMetadataException {

        int contentStart = metadataSection.indexOf(metadata);
        String prefix = metadataSection.substring(0, contentStart - marker.length());
        String suffix = metadataSection.substring(contentStart + metadata.length());
        return prefix + StringUtils.stripStart(metadata, null) + suffix;
    }
}
