package world.md2html.options;

import com.eclipsesource.json.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static world.md2html.utils.Utils.*;

public class ArgumentFileParser {

    private static final String[] DEFAULT_ALL_CSS_OPTIONS = {"link-css", "include-css"};
    private static final String[] DOCUMENT_ALL_CSS_OPTIONS =
            {"link-css", "include-css", "add-link-css", "add-include-css"};

    private ArgumentFileParser() {
    }

    public static List<Md2HtmlOptions> parse(String argumentFileContent, Md2HtmlOptions cliOptions)
            throws ArgumentFileParseException {

        List<Md2HtmlOptions> documentList = new ArrayList<>();

        JsonObject jsonObject;
        try {
            jsonObject = Json.parse(argumentFileContent).asObject();
        } catch (ParseException | UnsupportedOperationException e) {
            throw new ArgumentFileParseException("Argument file content cannot be parsed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (jsonObject == null) {
            throw new ArgumentFileParseException("Argument file content is a null object");
        }

        JsonObject defaults = getNotNullJsonObject(jsonObject, "default");

        if (jsonObject.get("documents") == null) {
            throw new ArgumentFileParseException("'documents' section is absent.");
        }

        if (jsonObjectContains(defaults, "no-css") &&
                jsonObjectContainsAny(defaults, DEFAULT_ALL_CSS_OPTIONS)) {
            throw new ArgumentFileParseException("'no-css' parameter incompatible with one of " +
                    "the [" + String.join(", ", DEFAULT_ALL_CSS_OPTIONS) +
                    "] in the 'default' section.");
        }

        if (cliOptions == null) {
            cliOptions = createEmptyMd2HtmlOptions();
        }

        JsonArray documents = getNotNullJsonArray(jsonObject, "documents");

        for (JsonValue documentValue : documents) {

            JsonObject document;
            try {
                document = documentValue.asObject();
            } catch (UnsupportedOperationException e) {
                throw new ArgumentFileParseException("'documents' item JSON object cannot " +
                        "be taken: " + e.getMessage());
            }

            Path inputFile = cliOptions.getInputFile();
            if (inputFile == null) {
                String inputFileString = firstNotNull(getJsonObjectStringMember(document, "input"),
                        getJsonObjectStringMember(defaults, "input"));
                if (inputFileString != null) {
                    inputFile = Paths.get(inputFileString);
                } else {
                    throw new ArgumentFileParseException("Undefined input file for the " +
                            "document: '" + document + "'.");
                }
            }

            Path outputFile = cliOptions.getOutputFile();
            if (outputFile == null) {
                String outputFileString = firstNotNull(getJsonObjectStringMember(document, "output"),
                        getJsonObjectStringMember(defaults, "output"));
                if (outputFileString != null) {
                    outputFile = Paths.get(outputFileString);
                }
            }

            String title = cliOptions.getTitle();
            if (title == null) {
                title = firstNotNull(getJsonObjectStringMember(document, "title"),
                        getJsonObjectStringMember(defaults, "title"));
            }

            Path templateFile = cliOptions.getTemplate();
            if (templateFile == null) {
                String templateFileString = firstNotNull(
                        getJsonObjectStringMember(document, "template"),
                        getJsonObjectStringMember(defaults, "template"));
                if (templateFileString != null) {
                    templateFile = Paths.get(templateFileString);
                }
            }

            List<String> linkCss = firstNotNull(cliOptions.getLinkCss(), new ArrayList<>());
            List<Path> includeCss = firstNotNull(cliOptions.getIncludeCss(), new ArrayList<>());
            boolean noCss = cliOptions.isNoCss();
            assert linkCss != null;
            assert includeCss != null;
            if (!noCss && linkCss.isEmpty() && includeCss.isEmpty()) {

                if (jsonObjectContains(document, "no-css") &&
                        jsonObjectContainsAny(document, DOCUMENT_ALL_CSS_OPTIONS)) {
                    throw new ArgumentFileParseException("'no-css' parameter incompatible with " +
                            "one of the [" + String.join(", ", DOCUMENT_ALL_CSS_OPTIONS) +
                            "] in the document: " + document);
                }
                //noinspection ConstantConditions
                noCss = firstNotNull(getJsonObjectBooleanMember(document, "no-css"),
                        getJsonObjectBooleanMember(defaults, "no-css"), false);

                JsonValue cssList = firstNotNull(document.get("link-css"),
                        defaults.get("link-css"), new JsonArray());
                assert cssList != null;
                linkCss.addAll(jsonArrayToStringList(toJsonArray(cssList)));

                cssList = getNotNullJsonArray(document, "add-link-css");
                assert cssList != null;
                linkCss.addAll(jsonArrayToStringList(toJsonArray(cssList)));

                cssList = firstNotNull(document.get("include-css"),
                        defaults.get("include-css"), new JsonArray());
                assert cssList != null;
                includeCss.addAll(jsonArrayToStringList(toJsonArray(cssList)).stream()
                        .map(Paths::get).collect(Collectors.toList()));

                cssList = getNotNullJsonArray(document, "add-include-css");
                assert cssList != null;
                includeCss.addAll(jsonArrayToStringList(toJsonArray(cssList)).stream()
                        .map(Paths::get).collect(Collectors.toList()));

                if (!linkCss.isEmpty() || !includeCss.isEmpty()) {
                    noCss = false;
                }
            }

            boolean force = cliOptions.isForce() || firstNotNull(document.get("force"),
                    defaults.get("force"), Json.value(false)).asBoolean();
            boolean verbose = cliOptions.isVerbose() || firstNotNull(document.get("verbose"),
                    defaults.get("verbose"), Json.value(false)).asBoolean();
            boolean report = cliOptions.isReport() || firstNotNull(document.get("report"),
                    defaults.get("report"), Json.value(false)).asBoolean();

            if (report && verbose) {
                throw new ArgumentFileParseException("Incompatible 'report' and 'verbose' " +
                        "parameters for 'documents' item: " + document);
            }

            documentList.add(new Md2HtmlOptions(null, inputFile, outputFile, title,
                    templateFile, includeCss, linkCss, noCss, force, verbose, report));
        }

        return documentList;
    }

    private static Md2HtmlOptions createEmptyMd2HtmlOptions() {
        return new Md2HtmlOptions(null, null, null, null, null, null, null, false, false,
                false, false);
    }

    private static JsonArray toJsonArray(JsonValue jsonValue) throws ArgumentFileParseException {
        try {
            return jsonValue.asArray();
        }  catch (UnsupportedOperationException e) {
            throw new ArgumentFileParseException("Cannot convert '" + jsonValue + "' to " +
                    "JSON array: " + e.getMessage());
        }
    }

    private static String getJsonObjectStringMember(JsonObject parent, String memberName) {
        JsonValue jsonValue = parent.get(memberName);
        if (jsonValue == null) {
            return null;
        } else {
            return jsonValue.asString();
        }
    }

    private static Boolean getJsonObjectBooleanMember(JsonObject parent, String memberName) {
        JsonValue jsonValue = parent.get(memberName);
        if (jsonValue == null) {
            return null;
        } else {
            return jsonValue.asBoolean();
        }
    }

    private static List<String> jsonArrayToStringList(JsonArray array)
            throws ArgumentFileParseException {
        List<String> result = new ArrayList<>();
        for (JsonValue jv : array) {
            try {
                result.add(jv.asString());
            }  catch (UnsupportedOperationException e) {
                throw new ArgumentFileParseException("Cannot convert '" + jv + "' to string: " +
                        e.getMessage());
            }
        }
        return result;
    }

    private static JsonObject getNotNullJsonObject(JsonObject parent, String memberName)
            throws ArgumentFileParseException {
        JsonObject result;
        JsonValue defaultSectionValue = parent.get(memberName);
        if (defaultSectionValue != null) {
            try {
                result = defaultSectionValue.asObject();
            } catch (UnsupportedOperationException e) {
                throw new ArgumentFileParseException("'" + memberName + "' JSON object cannot " +
                        "be taken: " + e.getMessage());
            }
        } else {
            result = new JsonObject();
        }
        return result;
    }

    private static JsonArray getNotNullJsonArray(JsonObject parent, String memberName)
            throws ArgumentFileParseException {
        JsonArray result;
        JsonValue defaultSectionValue = parent.get(memberName);
        if (defaultSectionValue != null) {
            try {
                result = defaultSectionValue.asArray();
            } catch (UnsupportedOperationException e) {
                throw new ArgumentFileParseException("'" + memberName + "' JSON array cannot " +
                        "be taken: " + e.getMessage());
            }
        } else {
            result = new JsonArray();
        }
        return result;
    }

}
