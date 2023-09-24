package world.md2html.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.NullNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.Utils.relativizeRelativeResource;

public class PageLinksPlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private List<String> markers;
    private List<PageMetadataHandlerInfo> pageLinksHandlers = new ArrayList<>();
    private Map<String, String> pages;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/page_links_schema.json");
        JsonNode markersNode = data.get("markers");
        if (markersNode == null) {
            this.markers = Collections.singletonList("page");
        } else if (markersNode instanceof NullNode) {
            this.markers = Collections.emptyList();
        } else {
            ObjectReader reader = OBJECT_MAPPER.readerFor(new TypeReference<List<String>>() {});
            ArrayList<String> list;
            try {
                list = reader.readValue(markersNode);
            } catch (IOException e) {
                throw new ArgFileParseException("Error reading plugin '" +
                        this.getClass().getSimpleName() + "' data: " + data);
            }
            if (list.isEmpty()) {
                this.markers = Collections.singletonList("page");
            } else {
                this.markers = list;
            }
        }
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

        String destinationPageOutput = this.pages.get(metadata.trim());
        if (destinationPageOutput == null) {
            return metadataSection;
        } else {
            try {
                return relativizeRelativeResource(destinationPageOutput, document.getOutput());
            } catch (CheckedIllegalArgumentException e) {
                throw new PageMetadataException("Plugin '" + this.getClass().getSimpleName() +
                        "': Cannot relativize '" + destinationPageOutput + "' against '" +
                        document.getOutput() + "': " + e.getMessage());
            }
        }
    }

    @Override
    public void acceptDocumentList(List<Document> documents) {
        if (this.markers.isEmpty()) {
            return;
        }
        this.pages = documents.stream().filter(d -> !Objects.isNull(d.getCode()))
                .collect(Collectors.toMap(Document::getCode, Document::getOutput));
        if (!this.pages.isEmpty()) {
            this.pageLinksHandlers = this.markers.stream()
                    .map(m -> new PageMetadataHandlerInfo(this, m, false))
                    .collect(Collectors.toList());
        }
    }

}
