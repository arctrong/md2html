package world.md2html.options.argfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import world.md2html.Constants;
import world.md2html.options.cli.ClilOptions;
import world.md2html.options.model.Document;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.utils.OptionsModelUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static world.md2html.Constants.MAPPER;
import static world.md2html.Constants.NODE_FACTORY;
import static world.md2html.utils.JsonUtils.*;
import static world.md2html.utils.Utils.firstNotNull;
import static world.md2html.utils.Utils.firstNotNullOptional;

public class ArgFileParser {

    private static final String[] DEFAULT_ALL_CSS_OPTIONS = {"link-css", "include-css"};
    private static final String[] DOCUMENT_ALL_CSS_OPTIONS =
            {"link-css", "include-css", "add-link-css", "add-include-css"};

    private ArgFileParser() {
    }

    public static ArgFileOptions parse(String argumentFileContent, ClilOptions cliOptions)
            throws ArgFileParseException {

        ObjectNode argFileNode;
        try {
            argFileNode = (ObjectNode) MAPPER.readTree(argumentFileContent);
        } catch (JsonProcessingException e) {
            throw new ArgFileParseException("Argument file content cannot be parsed: "
                    + e.getClass().getSimpleName() + ": " +
                    formatJsonProcessingException(e));
        }

        try {
            validateJsonAgainstSchemaFromResource(argFileNode, "args_file_schema.json");
        } catch (JsonValidationException e) {
            throw new ArgFileParseException("Argument file validation error: " + e.getMessage());
        }

        ObjectNode pluginsNode = (ObjectNode) Optional.ofNullable(argFileNode.get("plugins"))
                .orElse(new ObjectNode(NODE_FACTORY));
        ObjectNode pageFlowsPluginNode = (ObjectNode) pluginsNode.get("page-flows");
        ObjectNode documentsPageFlowsNode = new ObjectNode(NODE_FACTORY);

        ObjectNode defaultNode = Optional.ofNullable((ObjectNode) argFileNode.get("default"))
                .orElse(new ObjectNode(NODE_FACTORY));

        BooleanNode defaultNoCssNode = (BooleanNode) defaultNode.get("no-css");
        ArrayNode defaultIncludeCssNode = (ArrayNode) defaultNode.get("include-css");
        ArrayNode defaultLinkCssNode = (ArrayNode) defaultNode.get("link-css");

        if (defaultNoCssNode != null && (defaultIncludeCssNode != null ||
                defaultLinkCssNode != null)) {
            throw new ArgFileParseException("'no-css' parameter incompatible with one of " +
                    "the [" + String.join(", ", DEFAULT_ALL_CSS_OPTIONS) +
                    "] in the 'default' section.");
        }

        ObjectNode optionsNode = (ObjectNode) Optional.ofNullable(argFileNode.get("options"))
                .orElse(new ObjectNode(NODE_FACTORY));
        boolean optionsVerbose = Optional.ofNullable(optionsNode.get("options"))
                .map(JsonNode::asBoolean).orElse(false);
        if (optionsVerbose && cliOptions.isReport()) {
            throw new ArgFileParseException("'verbose' parameter in 'options' section is " +
                    "incompatible with '--report' command line argument.");
        }
        boolean optionsLegacyMode = Optional.ofNullable(optionsNode.get("legacy-mode"))
                .map(JsonNode::asBoolean).orElse(false);
        SessionOptions options = new SessionOptions(cliOptions.isVerbose() || optionsVerbose,
                cliOptions.isLegacyMode() || optionsLegacyMode);

        ArrayNode documentsNode = (ArrayNode) argFileNode.get("documents");
        List<Document> documentList = new ArrayList<>();
        for (Iterator<JsonNode> it = documentsNode.elements(); it.hasNext(); ) {
            ObjectNode documentNode = (ObjectNode) it.next();

            String inputFile = firstNotNull(cliOptions.getInputFile(),
                    jsonObjectStringField(documentNode, "input"),
                    jsonObjectStringField(defaultNode, "input"));
            if (inputFile == null) {
                throw new ArgFileParseException("Undefined input file for the document: '" +
                        documentNode + "'.");
            }

            String outputFile = firstNotNull(cliOptions.getOutputFile(),
                    jsonObjectStringField(documentNode, "output"),
                    jsonObjectStringField(defaultNode, "output"));

            String title = firstNotNull(cliOptions.getTitle(),
                    jsonObjectStringField(documentNode, "title"),
                    jsonObjectStringField(defaultNode, "title"));

            Path templateFile = firstNotNull(cliOptions.getTemplate(),
                    jsonObjectPathField(documentNode, "template"),
                    jsonObjectPathField(defaultNode, "template"));

            List<String> linkCss = firstNotNull(cliOptions.getLinkCss(), new ArrayList<>());
            List<Path> includeCss = firstNotNull(cliOptions.getIncludeCss(), new ArrayList<>());
            boolean noCss = cliOptions.isNoCss();
            assert linkCss != null;
            assert includeCss != null;
            if (!noCss && linkCss.isEmpty() && includeCss.isEmpty()) {

                BooleanNode documentNoCssNode = (BooleanNode) documentNode.get("no-css");
                ArrayNode documentIncludeCssNode = (ArrayNode) documentNode.get("include-css");
                ArrayNode documentLinkCssNode = (ArrayNode) documentNode.get("link-css");
                ArrayNode documentAddLinkCssNode = (ArrayNode) documentNode.get("add-link-css");
                ArrayNode documentAddIncludeCssNode =
                        (ArrayNode) documentNode.get("add-include-css");

                if (documentNoCssNode != null && firstNotNull(documentIncludeCssNode,
                        documentLinkCssNode, documentAddLinkCssNode,
                        documentAddIncludeCssNode) != null) {
                    throw new ArgFileParseException("'no-css' parameter incompatible with " +
                            "one of the [" + String.join(", ", DOCUMENT_ALL_CSS_OPTIONS) +
                            "] in the document: " + documentNode);
                }

                noCss = firstNotNullOptional(
                        jsonObjectBooleanField(documentNode, "no-css"),
                        jsonObjectBooleanField(defaultNode, "no-css")).orElse(false);

                ArrayNode cssList = firstNotNullOptional(documentLinkCssNode,
                        defaultLinkCssNode).orElse(new ArrayNode(NODE_FACTORY));
                linkCss.addAll(jsonArrayToStringList(cssList));
                Optional.ofNullable(documentAddLinkCssNode)
                        .ifPresent(list -> linkCss.addAll(jsonArrayToStringList(list)));

                cssList = firstNotNullOptional(documentIncludeCssNode,
                        defaultIncludeCssNode).orElse(new ArrayNode(NODE_FACTORY));
                includeCss.addAll(jsonArrayToStringList(cssList).stream()
                        .map(Paths::get).collect(Collectors.toList()));
                Optional.ofNullable(documentAddIncludeCssNode)
                        .ifPresent(list -> includeCss.addAll(jsonArrayToStringList(list).stream()
                                .map(Paths::get).collect(Collectors.toList())));

                if (!linkCss.isEmpty() || !includeCss.isEmpty()) {
                    noCss = false;
                }
            }

            boolean force = cliOptions.isForce() || firstNotNullOptional(
                    documentNode.get("force"), defaultNode.get("force"))
                        .map(JsonNode::asBoolean).orElse(false);
            boolean verbose = cliOptions.isVerbose() || firstNotNullOptional(
                    documentNode.get("verbose"), defaultNode.get("verbose"))
                    .map(JsonNode::asBoolean).orElse(false);
            boolean report = cliOptions.isReport() || firstNotNullOptional(
                    documentNode.get("report"), defaultNode.get("report"))
                    .map(JsonNode::asBoolean).orElse(false);

            if (report && verbose) {
                throw new ArgFileParseException("Incompatible 'report' and 'verbose' " +
                        "parameters for 'documents' item: " + documentNode);
            }

            Document document = OptionsModelUtils.enrichDocumentMd2HtmlOptions(
                    new Document(inputFile, outputFile, title, templateFile, includeCss,
                            linkCss, noCss, force, verbose, report));
            documentList.add(document);

            // Even if all page flows are defined in the 'documents' section, at least empty
            // 'page-flows' plugin must be defined in order to activate page flows processing.
            if (pageFlowsPluginNode != null) {

                ArrayNode documentPageFlowsNode = (ArrayNode) documentNode.get("page-flows");
                ArrayNode documentAddPageFlowsNode = (ArrayNode) documentNode.get("add-page-flows");

                if (documentPageFlowsNode != null && documentAddPageFlowsNode != null) {
                    throw new ArgFileParseException("Incompatible 'page-flows' " +
                            "and 'add-page-flows' parameters for 'documents' item: "
                            + documentNode);
                }

                documentPageFlowsNode = firstNotNullOptional(documentPageFlowsNode,
                        (ArrayNode) defaultNode.get("page-flows"))
                        .orElse(new ArrayNode(NODE_FACTORY));
                if (documentAddPageFlowsNode != null) {
                    documentPageFlowsNode.addAll(documentAddPageFlowsNode);
                }
                documentPageFlowsNode.forEach(pageFlowNameNode -> {
                    ArrayNode pageFlowItemsNode = (ArrayNode) documentsPageFlowsNode
                            .putIfAbsent(pageFlowNameNode.asText(), new ArrayNode(NODE_FACTORY));
                    if (pageFlowItemsNode == null) {
                        pageFlowItemsNode = (ArrayNode) documentsPageFlowsNode
                                .get(pageFlowNameNode.asText());
                    }
                    ObjectNode pageFlowNode = new ObjectNode(NODE_FACTORY);
                    pageFlowNode.put("link", document.getOutputLocation());
                    pageFlowNode.put("title", document.getTitle());
                    pageFlowItemsNode.add(pageFlowNode);
                });
            }
        }

        if (!documentsPageFlowsNode.isEmpty() && pageFlowsPluginNode != null) {
            documentsPageFlowsNode.fields().forEachRemaining(entry -> {
                ArrayNode pageFlowItemsNode = (ArrayNode) pageFlowsPluginNode
                        .putIfAbsent(entry.getKey(), new ArrayNode(NODE_FACTORY));
                if (pageFlowItemsNode == null) {
                    pageFlowItemsNode = (ArrayNode) pageFlowsPluginNode.get(entry.getKey());
                }
                ArrayNode newItemsNode = entry.getValue().deepCopy();
                newItemsNode.addAll(pageFlowItemsNode);
                pageFlowsPluginNode.replace(entry.getKey(), newItemsNode);
            });
        }

        List<Md2HtmlPlugin> plugins = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = pluginsNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> pluginEntry = it.next();
            Md2HtmlPlugin plugin = Constants.PLUGINS.get(pluginEntry.getKey());
            if (plugin != null) {
                try {
                    if (plugin.acceptData(pluginEntry.getValue())) {
                        plugins.add(plugin);
                    }
                } catch (Exception e) {
                    throw new ArgFileParseException("Error initializing plugin '" +
                            pluginEntry.getKey() + "': " + e.getClass().getSimpleName() +
                            ": " + e.getMessage());
                }
            }
        }

        return new ArgFileOptions(options, documentList, plugins);
    }

}
