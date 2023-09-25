package world.md2html.plugins;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.utils.UserError;
import world.md2html.utils.VariableReplacer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static world.md2html.plugins.PluginUtils.listFromStringOrArray;
import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;
import static world.md2html.utils.JsonUtils.deJson;

public class ReplacePlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    @AllArgsConstructor
    private static class Replacement {
        public VariableReplacer replacer;
        public boolean recursive;
    }

    private final List<PageMetadataHandlerInfo> pageLinksHandlers = new ArrayList<>();
    private final Map<String, Replacement> replacements = new HashMap<>();
    private PageMetadataHandlersWrapper metadataHandlers;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/replace_schema.json");

        for (JsonNode node : data) {
            JsonNode markersNode = node.get("markers");
            ObjectReader reader = OBJECT_MAPPER.readerFor(new TypeReference<List<String>>() {
            });
            ArrayList<String> markers;
            try {
                markers = reader.readValue(markersNode);
            } catch (IOException e) {
                throw new ArgFileParseException("Error reading plugin '" +
                        this.getClass().getSimpleName() + "' data: " + node);
            }
            String replaceWith = node.get("replace-with").asText();
            VariableReplacer replacer;
            for (String marker : markers) {
                try {
                    replacer = new VariableReplacer(replaceWith);
                } catch (VariableReplacer.VariableReplacerException e) {
                    throw new UserError(e.getMessage() + " The template is: " + replaceWith);
                }
                boolean recursive = false;
                JsonNode recursiveNode = node.get("recursive");
                if (recursiveNode != null) {
                    recursive = recursiveNode.asBoolean(false);
                }
                replacements.put(marker.toUpperCase(), new Replacement(replacer, recursive));
            }
            this.pageLinksHandlers.addAll(markers.stream()
                    .map(m -> new PageMetadataHandlerInfo(this, m, false))
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public boolean isBlank() {
        return this.pageLinksHandlers.isEmpty();
    }

    @Override
    public void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins,
                              PageMetadataHandlersWrapper metadataHandlers) {
        this.metadataHandlers = metadataHandlers;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.pageLinksHandlers;
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
                                     String metadataSection, Set<String> visitedMarkers
    ) throws PageMetadataException {

        String metadataStr = StringUtils.stripStart(metadata, null);

        List<String> values;
        if (metadataStr.startsWith("[")) {
            //noinspection unchecked
            values = (List<String>) deJson(listFromStringOrArray(document, metadataStr));
        } else {
            values = Collections.singletonList(metadataStr);
        }
        Replacement replacement = replacements.get(marker.toUpperCase());
        String result = replacement.replacer.replace(values);

        return replacement.recursive ?
                metadataHandlers.applyMetadataHandlers(result, document, visitedMarkers) :
                result;
    }
}
