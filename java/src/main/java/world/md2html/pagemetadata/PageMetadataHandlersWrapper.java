package world.md2html.pagemetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import world.md2html.options.model.Document;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageMetadataHandler;
import world.md2html.plugins.PageMetadataHandlerInfo;
import world.md2html.utils.UserError;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
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

    private final Map<MarkerKey, List<PageMetadataHandler>> markerHandlers;
    private final boolean allOnlyAtPageStart;

    private PageMetadataHandlersWrapper(
            Map<MarkerKey, List<PageMetadataHandler>> markerHandlers, boolean allOnlyAtPageStart) {
        this.markerHandlers = markerHandlers;
        this.allOnlyAtPageStart = allOnlyAtPageStart;
    }

    public static PageMetadataHandlersWrapper fromPlugins(List<Md2HtmlPlugin> plugins) {
        Map<MarkerKey, List<PageMetadataHandler>> markerHandlers = new HashMap<>();
        boolean allOnlyAtPageStart = true;
        for (Md2HtmlPlugin plugin : plugins) {
            List<PageMetadataHandlerInfo> handlerInfoList = plugin.pageMetadataHandlers();
            if (handlerInfoList != null && !handlerInfoList.isEmpty()) {
                for (PageMetadataHandlerInfo info : handlerInfoList) {
                    if (!info.isOnlyAtPageStart()) {
                        allOnlyAtPageStart = false;
                    }
                    MarkerKey markerKey = new MarkerKey(info.getMarker().toUpperCase(),
                            info.isOnlyAtPageStart());
                    List<PageMetadataHandler> handlers = new ArrayList<>();
                    List<PageMetadataHandler> OldValue = markerHandlers.putIfAbsent(markerKey,
                            handlers);
                    handlers = OldValue == null ? handlers : OldValue;
                    handlers.add(info.getPageMetadataHandler());
                }
            }
        }
        return new PageMetadataHandlersWrapper(markerHandlers, allOnlyAtPageStart);
    }

    public String applyMetadataHandlers(String text, Document document,
                                        Set<String> visitedMarkers) {

        StringBuilder newText = new StringBuilder();
        int lastPos = 0;
        boolean replacementDone = false;
        visitedMarkers = visitedMarkers == null ? new LinkedHashSet<>() : visitedMarkers;
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
            String replacement = matchObj.metadataBlock;
            if (handlers != null) {
                for (PageMetadataHandler h : handlers) {
                    if (visitedMarkers.contains(lookupMarker)) {
                        throw new UserError("Cycle detected at marker: " + lookupMarker +
                                ", path is [" + String.join(",", visitedMarkers) + "]");
                    }
                    visitedMarkers.add(lookupMarker);
                    replacement = h.acceptPageMetadata(document, lookupMarker,
                            matchObj.metadata, matchObj.metadataBlock, visitedMarkers);
                    visitedMarkers.remove(lookupMarker);
                    replacementDone = true;
                }
            }
            newText.append(matchObj.before);
            newText.append(replacement);
            if (allOnlyAtPageStart) {
                break;
            }
        }
        if (replacementDone) {
            newText.append(text.substring(lastPos));
            return newText.toString();
        } else {
            return text;
        }
    }

    public String applyMetadataHandlers(String pageText, Document doc) {
        return applyMetadataHandlers(pageText, doc, null);
    }

    @AllArgsConstructor
    @Getter
    private static class MarkerKey {
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
                    } else if (delimiter.group().equals(METADATA_END) && !stack.isEmpty()) {
                        begin = stack.pop();
                    }
                    if (stack.isEmpty()) {
                        int end = delimiter.end() - METADATA_END_LEN;
                        Matcher matcher = METADATA_PATTERN
                                .matcher(text.substring(begin + METADATA_START_LEN, end));
                        if (matcher.find()) {
                            metadataMatchObject =
                                    new MetadataMatchObject(text.substring(done, begin),
                                            matcher.group(1), matcher.group(2),
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
