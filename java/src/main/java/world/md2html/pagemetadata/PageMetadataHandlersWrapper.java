package world.md2html.pagemetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import world.md2html.options.model.Document;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageMetadataHandler;
import world.md2html.plugins.PageMetadataHandlerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageMetadataHandlersWrapper {

    private static final Pattern METADATA_PATTERN =
            Pattern.compile("^([\\w_][\\w\\d_]*)([^\\w\\d_]*.*)$", Pattern.DOTALL);

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

    public String applyMetadataHandlers(String text, Document document) {

        StringBuilder newText = new StringBuilder();
        int lastPos = 0;
        boolean replacementDone = false;
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
                    replacement = h.acceptPageMetadata(document, matchObj.marker,
                            matchObj.metadata, matchObj.metadataBlock);
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
    private static class MetadataMatchObject {
        private final String before;
        private final String marker;
        private final String metadata;
        private final String metadataBlock;
        private final int endPos;
    }

    private Iterator<MetadataMatchObject> metadataFinder(String text) {

        return new Iterator<MetadataMatchObject>() {

            private int current = 0;
            private int done = 0;
            private MetadataMatchObject metadataMatchObject;

            @Override
            public boolean hasNext() {
                metadataMatchObject = null;
                int begin;
                do {
                    begin = text.indexOf("<!--", current);
                    if (begin >= 0) {
                        int end = text.indexOf("-->", begin + 4);
                        if (end >= 0) {
                            Matcher matcher = METADATA_PATTERN.matcher(text.substring(begin + 4, end));
                            if (matcher.find()) {
                                metadataMatchObject =
                                        new MetadataMatchObject(text.substring(done, begin),
                                                matcher.group(1), matcher.group(2),
                                                text.substring(begin, end + 3), end + 3);
                                done = end + 3;
                            }
                            current = end + 3;
                        }
                    }
                } while (begin >= 0 && metadataMatchObject == null);
                return metadataMatchObject != null;
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
