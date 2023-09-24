package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import lombok.Getter;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static world.md2html.utils.JsonUtils.NODE_FACTORY;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;
import static world.md2html.utils.Utils.relativizeRelativeResource;

public class PageFlowsPlugin extends AbstractMd2HtmlPlugin {

    private final JsonSchema pluginDataSchema =
            loadJsonSchemaFromResource("plugins/page_flows_schema.json");

    private Map<String, PageFlowRaw> data = null;
    private ObjectNode jsonData = null;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {
        validateInputDataAgainstSchema(data, pluginDataSchema);
        data = unifyData(data);
        if (this.jsonData == null) {
            this.jsonData = (ObjectNode) data;
        } else {
            addToStart((ObjectNode) data);
        }
    }

    private JsonNode unifyData(JsonNode data) {
        ObjectNode newData = new ObjectNode(NODE_FACTORY);
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = data.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
            if (fieldEntry.getValue().isArray()) {
                ObjectNode pageFlow = new ObjectNode(NODE_FACTORY);
                pageFlow.set("items", fieldEntry.getValue());
                newData.set(fieldEntry.getKey(), unifyPageFlow(pageFlow));
            } else if (fieldEntry.getValue().isObject()) {
                newData.set(fieldEntry.getKey(), unifyPageFlow((ObjectNode) fieldEntry.getValue()));
            } else {
                throw new RuntimeException("Wrong value type: " + fieldEntry.getClass().getSimpleName());
            }
        }
        return newData;
    }

    private ObjectNode unifyPageFlow(ObjectNode pageFlow) {
        ObjectNode newPageFlow = new ObjectNode(NODE_FACTORY);
        newPageFlow.set("title", pageFlow.has("title") ? pageFlow.get("title") : new TextNode(""));
        newPageFlow.set("groups", pageFlow.has("groups") ? pageFlow.get("groups") : new ArrayNode(NODE_FACTORY));
        newPageFlow.set("items", pageFlow.has("items") ? pageFlow.get("items") : new ArrayNode(NODE_FACTORY));
        return newPageFlow;
    }

    private void addToStart(ObjectNode data) {
        if (data == null) {
            return;
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> dataEntry = it.next();
            ObjectNode existingPageFlow = (ObjectNode) this.jsonData.get(dataEntry.getKey());
            if (existingPageFlow == null) {
                this.jsonData.set(dataEntry.getKey(), dataEntry.getValue());
            } else {
                ArrayNode existingPages = (ArrayNode) existingPageFlow.get("items");
                ArrayNode newPages = (ArrayNode) existingPageFlow
                        .set("items", new ArrayNode(NODE_FACTORY)).get("items");
                newPages.addAll((ArrayNode) dataEntry.getValue().get("items"));
                newPages.addAll(existingPages);
            }
        }
    }

    private void addToEnd(ObjectNode data) {
        if (data == null) {
            return;
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> dataEntry = it.next();
            ObjectNode existingPageFlow = (ObjectNode) this.jsonData.get(dataEntry.getKey());
            if (existingPageFlow == null) {
                this.jsonData.set(dataEntry.getKey(), dataEntry.getValue());
            } else {
                ArrayNode existingPages = (ArrayNode) existingPageFlow.get("items");
                existingPages.addAll((ArrayNode) dataEntry.getValue().get("items"));
            }
        }
    }

    @Override
    public void initialize(JsonNode extraPluginData) throws ArgFileParseException {
        assureInitializeOnce();
        if (extraPluginData != null) {
            validateInputDataAgainstSchema(extraPluginData, pluginDataSchema);
            addToEnd((ObjectNode) unifyData(extraPluginData));
        }
        parsePluginData(this.jsonData);
    }

    private static class PageFlowRaw {
        @Getter private final String title;
        @Getter private final List<String> groups;
        @Getter private final List<Map<String, Object>> pages;

        public PageFlowRaw(String title, List<String> groups, List<Map<String, Object>> pages) {
            this.title = title;
            this.groups = groups;
            this.pages = pages;
        }
    }

    private void parsePluginData(JsonNode data) {
        Map<String, PageFlowRaw> pluginData = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> pageFlowEntry = it.next();
            List<Map<String, Object>> pages = new ArrayList<>();
            Map<String, Object> page = new HashMap<>();
            boolean isFirst = true;
            JsonNode pageFlow = pageFlowEntry.getValue();
            for (Iterator<JsonNode> it1 = pageFlow.get("items").elements(); it1.hasNext(); ) {
                ObjectNode pageNode = (ObjectNode) it1.next();
                page = new HashMap<>();
                for (Iterator<Map.Entry<String, JsonNode>> it2 = pageNode.fields(); it2.hasNext(); ) {
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
            List<String> groups = new ArrayList<>(pageFlow.get("groups").size());
            for (Iterator<JsonNode> it3 = pageFlow.get("groups").elements(); it3.hasNext(); ) {
                groups.add(it3.next().asText());
            }

            pluginData.put(pageFlowEntry.getKey(), new PageFlowRaw(pageFlow.get("title").asText(),
                    groups, pages));
        }
        this.data = pluginData;
    }

    @Override
    public boolean isBlank() {
        return this.data.isEmpty();
    }

    public Map<String, Object> variables(Document document) {
        Map<String, PageFlow> pageFlowVariables = new LinkedHashMap<>();
        Map<String, List<PageFlow>> pageFlowGroupVariables = new HashMap<>();
        this.data.forEach((k, v) -> {
            PageFlow pageFlow;
            try {
                pageFlow = processPageFlow(v, document.getOutput());
            } catch (CheckedIllegalArgumentException e) {
                throw new UserError("Error recalculating relative links '" + v +
                        "' of page flow '" + k + "' for page '" + document.getOutput() +
                        "': " + e.getMessage());
            }
            pageFlowVariables.put(k, pageFlow);
            v.getGroups().forEach(g -> {
                pageFlowGroupVariables.putIfAbsent(g, new ArrayList<>());
                List<PageFlow> groupPageFlows = pageFlowGroupVariables.get(g);
                groupPageFlows.add(pageFlow);
            });
        });
        pageFlowGroupVariables.keySet().forEach(key -> {
            if (pageFlowVariables.containsKey(key)) {
                throw new UserError("Variable duplication error in plugin '" +
                        this.getClass().getSimpleName() + "': group name is '" + key + "'");
            }
        });
        Map<String, Object> variables = new HashMap<>(pageFlowVariables);
        variables.putAll(pageFlowGroupVariables);
        return variables;
    }

    private static PageFlow processPageFlow(PageFlowRaw pageFlowRaw, String outputFile)
            throws CheckedIllegalArgumentException {

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> previous = null;
        Map<String, Object> current = null;
        Map<String, Object> next = null;

        for (Map<String, Object> page : pageFlowRaw.getPages()) {
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
        return new PageFlow(pageFlowRaw.getTitle(), result, previous, current, next);
    }

    private static void enrichPage(Map<String, Object> page) {
        page.putIfAbsent("external", false);
        page.putIfAbsent("current", false);
    }

    private static class PageFlow implements Iterable<Map<String, Object>> {

        @Getter private final String title;
        @Getter private final List<Map<String, Object>> pages;
        @Getter private final Map<String, Object> previous;
        @Getter private final Map<String, Object> current;
        @Getter private final Map<String, Object> next;
        // For a logic-less template like Mustache the following calculated fields will help a lot.
        @Getter private final boolean has_navigation;
        @Getter private final boolean not_empty;

        private PageFlow(String title, List<Map<String, Object>> pages,
                Map<String, Object> previous,
                Map<String, Object> current, Map<String, Object> next) {
            this.title = title;
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
