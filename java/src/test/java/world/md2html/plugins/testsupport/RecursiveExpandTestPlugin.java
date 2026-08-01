package world.md2html.plugins.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.MetadataProcessingPhase;
import world.md2html.pagemetadata.MetadataProcessingResult;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageMetadataHandler;
import world.md2html.plugins.PageMetadataHandlerInfo;
import world.md2html.utils.UserError;
import world.md2html.utils.VariableReplacer;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static world.md2html.plugins.PluginUtils.listFromStringOrArray;
import static world.md2html.utils.JsonUtils.deJson;

public class RecursiveExpandTestPlugin implements Md2HtmlPlugin, PageMetadataHandler {

    private static final Map<String, AbstractMap.SimpleEntry<String, Boolean>> DEFAULT_EXPANSIONS;

    static {
        DEFAULT_EXPANSIONS = new LinkedHashMap<>();
        DEFAULT_EXPANSIONS.put("EXPAND", new AbstractMap.SimpleEntry<>("<!--DEFER ${1}-->", true));
        DEFAULT_EXPANSIONS.put("OUTER", new AbstractMap.SimpleEntry<>("A [<!--INNER ${1}-->] B", true));
        DEFAULT_EXPANSIONS.put("INNER", new AbstractMap.SimpleEntry<>("C (<!--DEFER ${1}-->) D", true));
        DEFAULT_EXPANSIONS.put("TWIN",
                new AbstractMap.SimpleEntry<>("start <!--DEFER ${1}--> mid <!--DEFER ${2}--> end", true));
    }

    private final Map<String, AbstractMap.SimpleEntry<String, Boolean>> expansions;
    private PageMetadataHandlersWrapper metadataHandlers;

    public RecursiveExpandTestPlugin() {
        this(DEFAULT_EXPANSIONS);
    }

    RecursiveExpandTestPlugin(Map<String, AbstractMap.SimpleEntry<String, Boolean>> expansions) {
        this.expansions = expansions;
    }

    @Override
    public void acceptData(JsonNode data) {
    }

    @Override
    public boolean isBlank() {
        return false;
    }

    @Override
    public void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins,
                              PageMetadataHandlersWrapper metadataHandlers) {
        this.metadataHandlers = metadataHandlers;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return expansions.keySet().stream()
                .map(marker -> new PageMetadataHandlerInfo(this, marker, false))
                .collect(Collectors.toList());
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
        String metadataStr = StringUtils.stripStart(metadata, null);
        List<String> values;
        if (metadataStr.startsWith("[")) {
            //noinspection unchecked
            values = (List<String>) deJson(listFromStringOrArray(document, metadataStr));
        } else {
            values = Collections.singletonList(metadataStr);
        }

        AbstractMap.SimpleEntry<String, Boolean> expansion = expansions.get(marker);
        String result;
        try {
            result = expansion.getKey().contains("${")
                    ? new VariableReplacer(expansion.getKey()).replace(values)
                    : expansion.getKey();
        } catch (VariableReplacer.VariableReplacerException e) {
            throw new UserError("Error in recursive expand test entry: " + e.getMessage());
        }

        if (expansion.getValue()) {
            return metadataHandlers.processNestedMetadata(
                    result, document, visitedMarkers, marker);
        }
        return MetadataProcessingResult.immediate(result);
    }
}
