package world.md2html.options.argfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import world.md2html.options.cli.ClilOptions;
import world.md2html.options.model.Document;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.OptionsModelUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static world.md2html.utils.Utils.firstNotNull;

public class ArgFileParser {

    private static final String[] DEFAULT_ALL_CSS_OPTIONS = {"link-css", "include-css"};
    private static final String[] DOCUMENT_ALL_CSS_OPTIONS =
            {"link-css", "include-css", "add-link-css", "add-include-css"};

    private ArgFileParser() {
    }

    public static ArgFileOptions parse(String argumentFileContent, ClilOptions cliOptions)
            throws ArgFileParseException {

        List<Document> documentList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        JsonNodeFactory nodeFactory = mapper.getNodeFactory();

        ObjectNode argFileNode;
        try {
            argFileNode = (ObjectNode) mapper.readTree(argumentFileContent);
        } catch (JsonProcessingException e) {
            throw new ArgFileParseException("Argument file content cannot be parsed: "
                    + e.getClass().getSimpleName() + ": " +
                    JsonUtils.formatJsonProcessingException(e));
        }

        try {
            JsonUtils.validateJsonAgainstSchemaFromResource(argFileNode, "args_file_schema.json");
        } catch (JsonUtils.JsonValidationException e) {
            throw new ArgFileParseException(e.getMessage());
        }



//        JsonObject jsonObject;
//        try {
//            jsonObject = Json.parse(argumentFileContent).asObject();
//        } catch (ParseException | UnsupportedOperationException e) {
//            throw new ArgFileParseException("Argument file content cannot be parsed: "
//                    + e.getClass().getSimpleName() + ": " + e.getMessage());
//        }
//        if (jsonObject == null) {
//            throw new ArgFileParseException("Argument file content is a null object");
//        }

        ObjectNode defaultNode = Optional.ofNullable((ObjectNode) argFileNode.get("default"))
                .orElse(new ObjectNode(nodeFactory));

        BooleanNode defaultNoCssNode = (BooleanNode) defaultNode.get("no-css");
        ArrayNode defaultIncludeCssNode = (ArrayNode) defaultNode.get("include-css");
        ArrayNode defaultLinkCssNode = (ArrayNode) defaultNode.get("link-css");

        if (defaultNoCssNode != null && (defaultIncludeCssNode != null ||
                defaultLinkCssNode != null)) {
            throw new ArgFileParseException("'no-css' parameter incompatible with one of " +
                    "the [" + String.join(", ", DEFAULT_ALL_CSS_OPTIONS) +
                    "] in the 'default' section.");
        }

        ArrayNode documentsNode = (ArrayNode) argFileNode.get("documents");

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

                noCss = Optional.ofNullable(firstNotNull(
                        jsonObjectBooleanField(documentNode, "no-css"),
                        jsonObjectBooleanField(defaultNode, "no-css"))).orElse(false);

                ArrayNode cssList = Optional.ofNullable(firstNotNull(documentLinkCssNode,
                        defaultLinkCssNode)).orElse(new ArrayNode(nodeFactory));
                linkCss.addAll(jsonArrayToStringList(cssList));
                Optional.ofNullable(documentAddLinkCssNode)
                        .ifPresent(list -> linkCss.addAll(jsonArrayToStringList(list)));

                cssList = Optional.ofNullable(firstNotNull(documentIncludeCssNode,
                        defaultIncludeCssNode)).orElse(new ArrayNode(nodeFactory));
                includeCss.addAll(jsonArrayToStringList(cssList).stream()
                        .map(Paths::get).collect(Collectors.toList()));
                Optional.ofNullable(documentAddIncludeCssNode)
                        .ifPresent(list -> includeCss.addAll(jsonArrayToStringList(list).stream()
                                .map(Paths::get).collect(Collectors.toList())));

                if (!linkCss.isEmpty() || !includeCss.isEmpty()) {
                    noCss = false;
                }
            }

            boolean force = cliOptions.isForce() || Optional.ofNullable(
                    firstNotNull(documentNode.get("force"), defaultNode.get("force")))
                        .map(JsonNode::asBoolean).orElse(false);
            boolean verbose = cliOptions.isVerbose() || Optional.ofNullable(
                    firstNotNull(documentNode.get("verbose"), defaultNode.get("verbose")))
                    .map(JsonNode::asBoolean).orElse(false);
            boolean report = cliOptions.isReport() || Optional.ofNullable(
                    firstNotNull(documentNode.get("report"), defaultNode.get("report")))
                    .map(JsonNode::asBoolean).orElse(false);

            if (report && verbose) {
                throw new ArgFileParseException("Incompatible 'report' and 'verbose' " +
                        "parameters for 'documents' item: " + documentNode);
            }

            documentList.add(OptionsModelUtils.enrichDocumentMd2HtmlOptions(
                    new Document(inputFile, outputFile, title, templateFile, includeCss,
                            linkCss, noCss, force, verbose, report)));
        }

        return new ArgFileOptions(null, documentList, null);
    }

//    private static ClilOptions createEmptyMd2HtmlOptions() {
//        return new ClilOptions(null, null, null, null, null, null, null, false, false,
//                false, false);
//    }

//    private static JsonArray toJsonArray(JsonValue jsonValue) throws ArgFileParseException {
//        try {
//            return jsonValue.asArray();
//        }  catch (UnsupportedOperationException e) {
//            throw new ArgFileParseException("Cannot convert '" + jsonValue + "' to " +
//                    "JSON array: " + e.getMessage());
//        }
//    }

    private static String jsonObjectStringField(ObjectNode objectNode, String fieldName) {
        return Optional.ofNullable(objectNode.get(fieldName)).map(JsonNode::asText).orElse(null);
    }

    private static Path jsonObjectPathField(ObjectNode objectNode, String fieldName) {
        return Optional.ofNullable(objectNode.get(fieldName))
                .map(JsonNode::asText).map(Paths::get).orElse(null);
    }

    private static Boolean jsonObjectBooleanField(ObjectNode objectNode, String fieldName) {
        return Optional.ofNullable(objectNode.get(fieldName)).map(JsonNode::asBoolean).orElse(null);
    }

    private static List<String> jsonArrayToStringList(ArrayNode array) {
        List<String> result = new ArrayList<>();
        for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
            result.add(it.next().asText());
        }
        return result;
    }

//    private static JsonObject getNotNullJsonObject(JsonObject parent, String memberName)
//            throws ArgFileParseException {
//        JsonObject result;
//        JsonValue defaultSectionValue = parent.get(memberName);
//        if (defaultSectionValue != null) {
//            try {
//                result = defaultSectionValue.asObject();
//            } catch (UnsupportedOperationException e) {
//                throw new ArgFileParseException("'" + memberName + "' JSON object cannot " +
//                        "be taken: " + e.getMessage());
//            }
//        } else {
//            result = new JsonObject();
//        }
//        return result;
//    }

//    private static JsonArray getNotNullJsonArray(JsonObject parent, String memberName)
//            throws ArgFileParseException {
//        JsonArray result;
//        JsonValue defaultSectionValue = parent.get(memberName);
//        if (defaultSectionValue != null) {
//            try {
//                result = defaultSectionValue.asArray();
//            } catch (UnsupportedOperationException e) {
//                throw new ArgFileParseException("'" + memberName + "' JSON array cannot " +
//                        "be taken: " + e.getMessage());
//            }
//        } else {
//            result = new JsonArray();
//        }
//        return result;
//    }

}
