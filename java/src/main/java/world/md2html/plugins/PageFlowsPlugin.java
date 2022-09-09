package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import lombok.Getter;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.util.*;

import static world.md2html.utils.JsonUtils.NODE_FACTORY;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;
import static world.md2html.utils.Utils.relativizeRelativeResource;

public class PageFlowsPlugin extends AbstractMd2HtmlPlugin {

    private final JsonSchema pluginDataSchema =
            loadJsonSchemaFromResource("plugins/page_flows_schema.json");

    private Map<String, List<Map<String, Object>>> data = null;
    private ObjectNode jsonData = null;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        validateInputDataAgainstSchema(data, pluginDataSchema);
        if (this.jsonData == null) {
            this.jsonData = (ObjectNode) data;
        } else {
            addToStart((ObjectNode) data);
        }
    }

    private void addToStart(ObjectNode data) {
        if (data == null) {
            return;
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> dataEntry = it.next();
            ArrayNode existingItems = (ArrayNode) this.jsonData.get(dataEntry.getKey());
            if (existingItems == null) {
                existingItems = new ArrayNode(NODE_FACTORY);
                this.jsonData.set(dataEntry.getKey(), existingItems);
            }
            ArrayNode newItems = dataEntry.getValue().deepCopy();
            for (Iterator<JsonNode> it1 = existingItems.elements(); it1.hasNext(); ) {
                JsonNode existingItem = it1.next();
                newItems.add(existingItem);
            }
            this.jsonData.set(dataEntry.getKey(), newItems);
        }
    }

    private void addToEnd(ObjectNode data) {
        if (data == null) {
            return;
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> dataEntry = it.next();
            ArrayNode existingItems = (ArrayNode) this.jsonData
                    .putIfAbsent(dataEntry.getKey(), new ArrayNode(NODE_FACTORY));
            for (Iterator<JsonNode> it1 = dataEntry.getValue().elements(); it1.hasNext(); ) {
                existingItems.add(it1.next());
            }
        }
    }

    @Override
    public void initialize(JsonNode extraPluginData) throws ArgFileParseException {
        assureInitializeOnce();
        if (extraPluginData != null) {
            validateInputDataAgainstSchema(extraPluginData, pluginDataSchema);
            addToEnd((ObjectNode) extraPluginData);
        }
        this.data = parsePluginData(this.jsonData);
    }

    private Map<String, List<Map<String, Object>>> parsePluginData(JsonNode data) {

        Map<String, List<Map<String, Object>>> pluginData = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> pageFlowEntry = it.next();
            List<Map<String, Object>> pages = new ArrayList<>();
            Map<String, Object> page = new HashMap<>();
            boolean isFirst = true;
            for (Iterator<JsonNode> it1 = pageFlowEntry.getValue().elements(); it1.hasNext(); ) {
                ObjectNode pageNode = (ObjectNode) it1.next();
                page = new HashMap<>();
                for (Iterator<Map.Entry<String, JsonNode>> it2 = pageNode.fields();
                     it2.hasNext(); ) {
                    Map.Entry<String, JsonNode> fieldEntry = it2.next();
                    page.put(fieldEntry.getKey(), JsonUtils.deJson(fieldEntry.getValue()));
                }
                enrichPage(page);
                page.put("first", isFirst);
                page.put("last", false);
                isFirst = false;
                pages.add(page);
            }
            page.put("last", true);
            pluginData.put(pageFlowEntry.getKey(), pages);
        }
        return pluginData;
    }

    @Override
    public boolean isBlank() {
        return this.data.isEmpty();
    }

    public Map<String, Object> variables(Document document) {
        Map<String, Object> pageVariables = new HashMap<>();
        this.data.forEach((k, v) -> {
            try {
                pageVariables.put(k, processPageFlows(v, document.getOutput()));
            } catch (CheckedIllegalArgumentException e) {
                throw new UserError("Error recalculating relative links '" + v +
                        "' of page flow '" + k + "' for page '" + document.getOutput() +
                        "': " + e.getMessage());
            }
        });
        return pageVariables;
    }

    private static PageFlow processPageFlows(List<Map<String, Object>> pages, String outputFile)
            throws CheckedIllegalArgumentException {

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> previous = null;
        Map<String, Object> current = null;
        Map<String, Object> next = null;

        for (Map<String, Object> page : pages) {
            Map<String, Object> newPage = new HashMap<>(page);
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

    private static void enrichPage(Map<String, Object> page) {
        page.putIfAbsent("external", false);
        page.putIfAbsent("current", false);
    }

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
