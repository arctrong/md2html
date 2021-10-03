package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import world.md2html.UserError;
import world.md2html.options.model.Document;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.Utils;

import java.util.*;

import static world.md2html.utils.Utils.ResourceLocationException;
import static world.md2html.utils.Utils.relativizeRelativeResource;

public class PageFlowsPlugin implements Md2HtmlPlugin {

    private Map<String, List<Map<String, Object>>> data = null;

    @Override
    public boolean acceptData(JsonNode data) {
        try {
            JsonUtils.validateJsonAgainstSchemaFromResource(data, "plugins/page_flows_schema.json");
        } catch (JsonUtils.JsonValidationException e) {
            throw new PluginDataUserError("Plugin '" + this.getClass().getSimpleName() +
                    "' data error: " + e.getMessage());
        }
        Map<String, List<Map<String, Object>>> pluginData = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> pageFlowEntry = it.next();
            List<Map<String, Object>> pages = new ArrayList<>();
            for (Iterator<JsonNode> it1 = pageFlowEntry.getValue().elements(); it1.hasNext(); ) {
                ObjectNode pageNode = (ObjectNode) it1.next();
                Map<String, Object> page = new HashMap<>();
                pageNode.fields().forEachRemaining(fieldEntry -> page.put(fieldEntry.getKey(),
                        Utils.deJson(fieldEntry.getValue())));
                Map<String, Object> enrichedPage = enrichPage(page);
                pages.add(enrichedPage);
            }
            pluginData.put(pageFlowEntry.getKey(), pages);
        }
        this.data = pluginData;
        return !this.data.isEmpty();
    }

    public Map<String, Object> variables(Document document) {
        Map<String, Object> pageVariables = new HashMap<>();
        this.data.forEach((k, v) -> {
            try {
                pageVariables.put(k, processPageFlows(v, document.getOutputLocation()));
            } catch (ResourceLocationException e) {
                throw new UserError("Error recalculating relative links '" + v +
                        "' of page flow '" + k + "' for page '" + document.getOutputLocation() +
                        "': " + e.getMessage());
            }
        });
        return pageVariables;
    }

    private static PageFlow processPageFlows(List<Map<String, Object>> pages, String outputFile)
            throws ResourceLocationException {

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> previous = null;
        Map<String, Object> current = null;
        Map<String, Object> next = null;

        for (Map<String, Object> page : pages) {
            Map<String, Object> newPage = enrichPage(page);
            if (Utils.isNullOrFalse(page.get("external"))) {
                result.add(newPage);
            } else {
                String link = (String) page.get("link");
                boolean isCurrent = link.equals(outputFile);
                newPage.put("link", relativizeRelativeResource(link, outputFile));
                newPage.put("current", isCurrent);
                result.add(newPage);
                if (current == null) {
                    if (isCurrent) {
                        current = newPage;
                    } else {
                        previous = newPage;
                    }
                } else if (next == null) {
                    next = newPage;
                }
            }
        }
        if (current == null) {
            previous = null;
        }
        return new PageFlow(result, previous, current, next);
    }

    private static Map<String, Object> enrichPage(Map<String, Object> page) {
        HashMap<String, Object> newPage = new HashMap<>(page);
        newPage.putIfAbsent("external", false);
        newPage.putIfAbsent("current", false);
        return newPage;
    }

//    @Getter
//    private static class Page {
//        private final String link;
//        private final String title;
//        private final boolean external;
//        private final boolean current;
//
//        public Page(String link, String title, Boolean external) {
//            this.link = link;
//            this.title = title;
//            this.current = false;
//            this.external = external != null && external;
//        }
//
//        public Page(String link, String title, boolean external, boolean current) {
//            this.link = link;
//            this.title = title;
//            this.current = current;
//            this.external = external;
//        }
//    }

    private static class PageFlow implements Iterable<Map<String, Object>> {

        @Getter private final List<Map<String, Object>> pages;
        @Getter private final Map<String, Object> previous;
        @Getter private final Map<String, Object> current;
        @Getter private final Map<String, Object> next;
        // For a logic-less template like Mustache the following calculated fields will help a lot.
        @Getter private final boolean has_navigation;
        @Getter private final boolean not_empty;

        private PageFlow(List<Map<String, Object>> pages, Map<String, Object> previous,
                Map<String, Object> current, Map<String, Object> next) {
            this.pages = pages;
            this.previous = previous;
            this.current = current;
            this.next = next;
            this.has_navigation = this.previous != null || this.next != null;
            this.not_empty = this.pages != null && !this.pages.isEmpty();
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return pages.iterator();
        }
    }

}
