package world.md2html.pagemetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import world.md2html.options.model.Document;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageMetadataHandler;
import world.md2html.plugins.PageMetadataHandlerInfo;
import world.md2html.utils.UserError;

import java.util.Collections;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageMetadataHandlersWrapper {

    private static final Pattern METADATA_PATTERN =
            Pattern.compile("^([\\w_][\\w\\d_]*)([^\\w\\d_]*.*)$", Pattern.DOTALL);

    private static final String METADATA_START = "<!--";
    private static final String METADATA_END = "-->";
    private final static int METADATA_START_LEN = METADATA_START.length();
    private final static int METADATA_END_LEN = METADATA_END.length();
    private static final Pattern METADATA_DELIMITERS_PATTERN =
            Pattern.compile(METADATA_START.replace("|", "\\|") +  "|" +
                    METADATA_END.replace("|", "\\|"));

    private final static int RECURSIVE_MAX_DEPTH = 100;

    private final Map<MarkerKey, List<PageMetadataHandler>> markerHandlers;
    private final boolean allOnlyAtPageStart;

    private PageMetadataHandlersWrapper(
            Map<MarkerKey, List<PageMetadataHandler>> markerHandlers, boolean allOnlyAtPageStart) {
        this.markerHandlers = markerHandlers;
        this.allOnlyAtPageStart = allOnlyAtPageStart;
    }

    public static PageMetadataHandlersWrapper fromPlugins(List<Md2HtmlPlugin> plugins) {
        Map<MarkerKey, List<PageMetadataHandler>> markerHandlers = new HashMap<>();
        Set<String> registeredMarkers = new HashSet<>();
        boolean allOnlyAtPageStart = true;
        for (Md2HtmlPlugin plugin : plugins) {
            List<PageMetadataHandlerInfo> handlerInfoList = plugin.pageMetadataHandlers();
            if (handlerInfoList != null && !handlerInfoList.isEmpty()) {
                for (PageMetadataHandlerInfo info : handlerInfoList) {
                    if (!info.isOnlyAtPageStart()) {
                        allOnlyAtPageStart = false;
                    }
                    String marker = info.getMarker().toUpperCase();
                    if (!registeredMarkers.add(marker)) {
                        throw new UserError("Marker duplication (case-insensitively): " + marker);
                    }
                    MarkerKey markerKey = new MarkerKey(marker, info.isOnlyAtPageStart());
                    List<PageMetadataHandler> handlers = new ArrayList<>();
                    handlers.add(info.getPageMetadataHandler());
                    markerHandlers.put(markerKey, handlers);
                }
            }
        }
        return new PageMetadataHandlersWrapper(markerHandlers, allOnlyAtPageStart);
    }

    public String applyMetadataHandlers(String text, Document document,
            Set<String> visitedMarkers, String recursiveMarker) {
        MetadataHandlersApplicationResult applicationResult =
                applyMetadataHandlersWithResult(text, document, visitedMarkers, recursiveMarker);
        return joinParsingResults(applicationResult.getParsingResults(), document);
    }

    public String applyAndMergeMetadataHandlers(String text, Document document,
            Set<String> visitedMarkers, String recursiveMarker) {
        return applyMetadataHandlers(text, document, visitedMarkers, recursiveMarker);
    }

    public MetadataHandlersApplicationResult applyMetadataHandlersWithResult(String text,
            Document document) {
        return applyMetadataHandlersWithResult(text, document, null, null);
    }

    public MetadataHandlersApplicationResult applyMetadataHandlersWithResult(String text,
            Document document, Set<String> visitedMarkers, String recursiveMarker) {

        if (recursiveMarker != null) {
            visitedMarkers = visitedMarkers == null ? new LinkedHashSet<>() : visitedMarkers;
            if (visitedMarkers.contains(recursiveMarker)) {
                throw new UserError("Cycle detected at marker: " + recursiveMarker +
                        ", the path is [" + String.join(",", visitedMarkers) + "]");
            }
            visitedMarkers.add(recursiveMarker);
            // Different plugins may have their peculiarities, so we cannot be completely sure
            // that ALL cycles are detected in ALL possible cases.
            if (visitedMarkers.size() > RECURSIVE_MAX_DEPTH) {
                throw new UserError("Cycle SUSPECTED with recursive depth " + RECURSIVE_MAX_DEPTH +
                        "at marker: " + recursiveMarker +
                        ", the path is [" + String.join("\n", visitedMarkers) + "]");
            }
        }

        List<ParsingResultItem> parsingResults = new ArrayList<>();
        int lastPos = 0;
        boolean replacementDone = false;
        boolean deferPage = false;
        Iterator<MetadataMatchObject> it = metadataFinder(text);
        while (it.hasNext()) {
            MetadataMatchObject matchObj = it.next();
            boolean firstNonBlank = matchObj.before.trim().isEmpty();
            lastPos = matchObj.endPos;
            String lookupMarker = matchObj.marker.toUpperCase();
            List<PageMetadataHandler> handlers = this.markerHandlers.get(
                    new MarkerKey(lookupMarker, firstNonBlank));
            if (handlers == null && firstNonBlank) {
                handlers = this.markerHandlers.get(
                        new MarkerKey(lookupMarker, false));
            }
            ParsingResultItem replacement = ParsingResultItem.text(matchObj.metadataBlock);
            if (handlers != null) {
                for (PageMetadataHandler h : handlers) {
                    MetadataProcessingResult acceptResult = h.acceptPageMetadata(
                            document, lookupMarker, matchObj.metadata, matchObj.metadataBlock,
                            visitedMarkers);
                    deferPage |= acceptResult.isDefer();
                    if (acceptResult.isDefer()) {
                        replacement = ParsingResultItem.deferred(acceptResult.getResult(),
                                lookupMarker, matchObj.metadata, matchObj.metadataBlock,
                                new MarkerKey(lookupMarker, false));
                    } else {
                        replacement = ParsingResultItem.text(acceptResult.getResultAsString());
                    }
                    replacementDone = true;
                }
            }
            parsingResults.add(ParsingResultItem.text(matchObj.before));
            parsingResults.add(replacement);
            if (allOnlyAtPageStart) {
                break;
            }
        }

        if (recursiveMarker != null) {
            visitedMarkers.remove(recursiveMarker);
        }

        if (replacementDone) {
            parsingResults.add(ParsingResultItem.text(text.substring(lastPos)));
            return new MetadataHandlersApplicationResult(parsingResults, deferPage);
        } else {
            return new MetadataHandlersApplicationResult(
                    Collections.singletonList(ParsingResultItem.text(text)), false);
        }
    }

    public String joinParsingResults(List<ParsingResultItem> parsingResults, Document document) {
        StringBuilder result = new StringBuilder();
        for (ParsingResultItem item : parsingResults) {
            Object replacement = item.getResult();
            if (item.isDeferred()) {
                List<PageMetadataHandler> handlers = this.markerHandlers.get(item.getMarkerKey());
                if (handlers == null) {
                    throw new IllegalStateException(
                            "Deferred metadata marker '" + item.getMarker() +
                            "' has no handler for phase-2 join (marker key: " +
                            item.getMarkerKey() + "). Check plugin registration, e.g. " +
                            "only-at-page-start mismatch.");
                }
                for (PageMetadataHandler h : handlers) {
                    MetadataProcessingResult acceptResult = h.acceptPageMetadata(
                            document,
                            item.getMarker(),
                            item.getMetadata(),
                            item.getMetadataSection(),
                            null,
                            MetadataProcessingPhase.PHASE_2,
                            item.getResult());
                    if (acceptResult.isDefer()) {
                        throw new UserError(
                                "Deferred result encountered when processing metadata " +
                                "marker '" + item.getMarker() + "' on phase 2. " +
                                "This may mean that this marker cannot be nested " +
                                "inside the other metadata block.");
                    }
                    replacement = acceptResult.getResult();
                }
            }
            result.append(replacement);
        }
        return result.toString();
    }

    public String applyMetadataHandlers(String pageText, Document doc) {
        return applyMetadataHandlers(pageText, doc, null, null);
    }

    @AllArgsConstructor
    @Getter
    public static class MarkerKey {
        private final String marker;
        private final boolean onlyAtPageStart;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MarkerKey markerKey = (MarkerKey) o;
            return onlyAtPageStart == markerKey.onlyAtPageStart &&
                    Objects.equals(marker, markerKey.marker);
        }

        @Override
        public int hashCode() {
            return Objects.hash(marker, onlyAtPageStart);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class MetadataMatchObject {
        private final String before;
        private final String marker;
        private final String metadata;
        private final String metadataBlock;
        private final int endPos;
    }

    public static Iterator<MetadataMatchObject> metadataFinder(String text) {

        return new Iterator<MetadataMatchObject>() {

            private int done = 0;
            private int begin = 0;
            final Deque<Integer> stack = new ArrayDeque<>();
            private MetadataMatchObject metadataMatchObject;
            final Matcher delimiter = METADATA_DELIMITERS_PATTERN.matcher(text);

            @Override
            public boolean hasNext() {
                while (delimiter.find()) {
                    if (delimiter.group().equals(METADATA_START)) {
                        stack.push(delimiter.start());
                    } else if (delimiter.group().equals(METADATA_END)) {
                        if (stack.isEmpty()) {
                            continue;
                        } else {
                            begin = stack.pop();
                        }
                    }
                    if (stack.isEmpty()) {
                        int end = delimiter.end() - METADATA_END_LEN;
                        Matcher matcher = METADATA_PATTERN
                                .matcher(text.substring(begin + METADATA_START_LEN, end));
                        if (matcher.find()) {
                            metadataMatchObject = new MetadataMatchObject(
                                    text.substring(done, begin), matcher.group(1), matcher.group(2),
                                    text.substring(begin, end + METADATA_END_LEN),
                                    end + METADATA_END_LEN);
                            done = end + METADATA_END_LEN;
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public MetadataMatchObject next() {
                if (metadataMatchObject == null) {
                    throw new IllegalStateException();
                }
                return metadataMatchObject;
            }
        };
    }
}
