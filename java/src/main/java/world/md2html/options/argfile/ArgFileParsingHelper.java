package world.md2html.options.argfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.javatuples.Pair;
import world.md2html.Constants;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.options.model.raw.ArgFileDocumentRaw;
import world.md2html.options.model.raw.ArgFileOptionsRaw;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static world.md2html.utils.JsonUtils.*;
import static world.md2html.utils.Utils.*;

public class ArgFileParsingHelper {

    private static final String DEFAULT_TEMPLATE_FILE = "doc_src/templates/default.html";
    private static final String DEFAULT_CSS_FILE = "doc/layout/styles.css";

    private ArgFileParsingHelper() {
    }

    public static ArgFileRaw readArgumentFileNode(String argumentFileContent)
            throws ArgFileParseException {
        JsonNode argFileJsonNode;
        try {
            argFileJsonNode = OBJECT_MAPPER.readTree(argumentFileContent);
        } catch (JsonProcessingException e) {
            throw new ArgFileParseException("Argument file content cannot be parsed: "
                    + e.getClass().getSimpleName() + ": " + formatJsonProcessingException(e));
        }
        ObjectNode argFileNode;
        try {
            argFileNode = (ObjectNode) argFileJsonNode;
        } catch (ClassCastException e) {
            throw new ArgFileParseException("Argument file content is not a JSON object: " +
                    argFileJsonNode);
        }
        try {
            validateJsonAgainstSchemaFromResource(argFileNode, "args_file_schema.json");
        } catch (JsonValidationException e) {
            throw new ArgFileParseException("Argument file validation error: " + e.getMessage());
        }
        ArgFileRaw argFileRaw;
        try {
            argFileRaw = OBJECT_MAPPER_FOR_BUILDERS.treeToValue(argFileJsonNode, ArgFileRaw.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return argFileRaw;
    }

    /**
     * <br />Brings the argument file to a more canonical form:
     * <br />
     * <br />- merges arguments from the command line into the argument file;
     * <br />- applies the arguments from the default section to the documents;
     * <br />- canonizes some parameters that may be defined in different ways;
     * <br />- explicitly sets defaults values;
     * <br />- creates some default structures like empty collections;
     * <br />- doesn't change the plugins.
     * <br />
     * <br />This method works only with the argument file's content and doesn't use the context.
     * <br />So it, for example, doesn't resolve the GLOBs as it would need to read the file system.
     * <br />
     * <br />This method does not instantiate plugins.
     */
    public static ArgFileRaw mergeAndCanonizeArgFileRaw(ArgFileRaw argFileRaw,
            CliOptions cliOptions) {

        ArgFileRaw.ArgFileRawBuilder argFileRawBuilder = ArgFileRaw.builder();
        ArgFileDocumentRaw defaults = firstNotNull(argFileRaw.getDefaultSection(),
                ArgFileDocumentRaw.builder().build());

        if (defaults.isNoCss() && (!isNullOrEmpty(defaults.getLinkCss()) ||
                !isNullOrEmpty(defaults.getIncludeCss()))) {
          throw new UserError("'no-css' parameter incompatible with one of the " +
                  "['link-css', 'include-css'] in the 'default' section.");
        }

        List<ArgFileDocumentRaw> newDocumentsRaw = new ArrayList<>();
        for (ArgFileDocumentRaw newDocumentRaw : argFileRaw.getDocuments()) {
            newDocumentsRaw.add(mergeAndCanonizeDocumentRaw(newDocumentRaw, defaults, cliOptions));
        }
        argFileRawBuilder.documents(newDocumentsRaw);

        ArgFileOptionsRaw options = mergeArgFileOptionsRaw(argFileRaw, cliOptions);
        argFileRawBuilder.options(options);
        Map<String, JsonNode> plugins = firstNotNull(argFileRaw.getPlugins(),
                new LinkedHashMap<>());
        argFileRawBuilder.plugins(plugins);

        if (options.isLegacyMode()) {
            JsonNode pageVariablesNode = plugins.computeIfAbsent("page-variables",
                    key -> new ObjectNode(NODE_FACTORY));
            if (!pageVariablesNode.has("METADATA")) {
                ObjectNode metadataNode = new ObjectNode(NODE_FACTORY);
                metadataNode.set("only-at-page-start", BooleanNode.getTrue());
                ((ObjectNode) pageVariablesNode).set("METADATA", metadataNode);
            }
        }

        return argFileRawBuilder.build();
    }

    private static ArgFileOptionsRaw mergeArgFileOptionsRaw(ArgFileRaw argFileRaw,
            CliOptions cliOptions) {

        ArgFileOptionsRaw options = firstNotNull(argFileRaw.getOptions(),
                ArgFileOptionsRaw.builder().build());
        boolean verbose = cliOptions.isVerbose() || options.isVerbose();
        boolean legacyMode = cliOptions.isLegacyMode() || options.isLegacyMode();

        if (verbose && cliOptions.isReport()) {
            throw new UserError("'verbose' parameter in 'options' section is incompatible " +
                    "with '--report' command line argument.");
        }

        return ArgFileOptionsRaw.builder()
                .verbose(verbose)
                .legacyMode(legacyMode)
                .build();
    }

    private static ArgFileDocumentRaw mergeAndCanonizeDocumentRaw(ArgFileDocumentRaw documentRaw,
            ArgFileDocumentRaw defaults, CliOptions cliOptions) {

        ArgFileDocumentRaw.ArgFileDocumentRawBuilder argFileDocumentRawBuilder =
                ArgFileDocumentRaw.builder();

        // TODO Consider simplification. Probably defaults may be applied first to the documents
        //  (maybe it's easier to do in the Jackson `ObjectNode`), then command line arguments
        //  may override the options.

        String inputFile = firstNotNull(cliOptions.getInput(), documentRaw.getInput(),
                defaults.getInput());
        String inputGlob = firstNotNull(cliOptions.getInputGlob(), documentRaw.getInputGlob(),
                defaults.getInputGlob());

        if (inputFile != null && inputGlob != null) {
            throw new UserError("Both input file GLOB and input file name are defined " +
                    "for 'documents' item: " + refineToString(documentRaw));
        } else if (inputFile == null && inputGlob == null) {
            throw new UserError("None of the input file name or input file GLOB is specified " +
                    "for 'documents' item: " + refineToString(documentRaw));
        }

        argFileDocumentRawBuilder.input(inputFile);
        argFileDocumentRawBuilder.inputGlob(inputGlob);

        if (inputGlob != null) {
            boolean sortByFilePath = cliOptions.isSortByFilePath() ||
                    documentRaw.isSortByFilePath() || defaults.isSortByFilePath();
            String sortByVariable = firstNotNull(cliOptions.getSortByVariable(),
                    documentRaw.getSortByVariable(), defaults.getSortByVariable());
            boolean sortByTitle = cliOptions.isSortByTitle() ||
                    documentRaw.isSortByTitle() || defaults.isSortByTitle();

            List<String> sorts = new ArrayList<>();
            if (sortByFilePath) {
                sorts.add("'sort-by-file-path'");
            }
            if (sortByVariable != null) {
                sorts.add("'sort-by-variable'");
            }
            if (sortByTitle) {
                sorts.add("'sort-by-title'");
            }
            if (sorts.size() > 1) {
                throw new UserError("Incompatible sort options " + String.join(", ", sorts) +
                        " for 'documents' item: " + refineToString(documentRaw));
            }
            argFileDocumentRawBuilder.sortByFilePath(sortByFilePath);
            argFileDocumentRawBuilder.sortByVariable(sortByVariable);
            argFileDocumentRawBuilder.sortByTitle(sortByTitle);
        }

        String output = firstNotNull(cliOptions.getOutput(), documentRaw.getOutput(),
                defaults.getOutput());
        argFileDocumentRawBuilder.output(output);
        String inputRoot = firstNotNull(cliOptions.getInputRoot(), documentRaw.getInputRoot(),
                defaults.getInputRoot(), "");
        argFileDocumentRawBuilder.inputRoot(inputRoot);
        String outputRoot = firstNotNull(cliOptions.getOutputRoot(), documentRaw.getOutputRoot(),
                defaults.getOutputRoot(), "");
        argFileDocumentRawBuilder.outputRoot(outputRoot);

        String title = firstNotNull(cliOptions.getTitle(), documentRaw.getTitle(),
                defaults.getTitle());
        argFileDocumentRawBuilder.title(title);
        String titleFromVariable = firstNotNull(cliOptions.getTitleFromVariable(),
                documentRaw.getTitleFromVariable(), defaults.getTitleFromVariable());
        argFileDocumentRawBuilder.titleFromVariable(titleFromVariable);

        String templateFile = firstNotNull(cliOptions.getTemplate(), documentRaw.getTemplate(),
                defaults.getTemplate());
        argFileDocumentRawBuilder.template(templateFile);

        List<String> linkCss = new ArrayList<>();
        List<String> includeCss = new ArrayList<>();
        boolean noCss = false;
        // TODO Looks like if any of the CSS options is defined in the command line then
        //  all CSS options are taken from the command line. Need to check whether it's correct.
        if (cliOptions.isNoCss() || cliOptions.getLinkCss() != null ||
                cliOptions.getIncludeCss() != null) {
            if (cliOptions.isNoCss()) {
                noCss = true;
            } else {
                if (cliOptions.getLinkCss() != null) {
                    linkCss = cliOptions.getLinkCss();
                }
                if (cliOptions.getIncludeCss() != null) {
                    includeCss = cliOptions.getIncludeCss();
                }
            }
        } else {
            List<String> cssOptions = new ArrayList<>();
            if (documentRaw.getLinkCss() != null) {
                cssOptions.add("link-css");
            }
            if (documentRaw.getAddLinkCss() != null) {
                cssOptions.add("add-link-css");
            }
            if (documentRaw.getIncludeCss() != null) {
                cssOptions.add("include-css");
            }
            if (documentRaw.getAddIncludeCss() != null) {
                cssOptions.add("add-include-css");
            }
            if (documentRaw.isNoCss() && cssOptions.size() > 0) {
                throw new UserError("'no-css' parameter incompatible with any of [" +
                        cssOptions.stream().map(a -> "'" + a + "'")
                                .collect(Collectors.joining(", ")) +
                        "] in `documents` item: " + refineToString(documentRaw));
            }
            noCss = documentRaw.isNoCss() || defaults.isNoCss();

            Optional.ofNullable(firstNotNull(documentRaw.getLinkCss(), defaults.getLinkCss()))
                    .ifPresent(linkCss::addAll);
            Optional.ofNullable(firstNotNull(documentRaw.getAddLinkCss()))
                    .ifPresent(linkCss::addAll);

            Optional.ofNullable(firstNotNull(documentRaw.getIncludeCss(), defaults.getIncludeCss()))
                    .ifPresent(includeCss::addAll);
            Optional.ofNullable(firstNotNull(documentRaw.getAddIncludeCss()))
                    .ifPresent(includeCss::addAll);

            if (!linkCss.isEmpty() || !includeCss.isEmpty()) {
                noCss = false;
            }
        }
        argFileDocumentRawBuilder.linkCss(linkCss);
        argFileDocumentRawBuilder.includeCss(includeCss);
        argFileDocumentRawBuilder.noCss(noCss);

        argFileDocumentRawBuilder.force(cliOptions.isForce() || documentRaw.isForce() ||
                defaults.isForce());

        boolean verbose = cliOptions.isVerbose() || documentRaw.isVerbose() ||
                defaults.isVerbose();
        boolean report = cliOptions.isReport() || documentRaw.isReport() ||
                defaults.isReport();
        if (verbose && report) {
            throw new UserError("Incompatible 'report' and 'verbose' parameters for 'documents " +
                    "item: " + refineToString(documentRaw));
        }
        argFileDocumentRawBuilder.verbose(verbose);
        argFileDocumentRawBuilder.report(report);

        //  Page flows must be ignored if the 'page-flows' plugin is not defined.
        //  But this is not checked here and must be checked at the following steps.
        if (documentRaw.getPageFlows() != null && documentRaw.getAddPageFlows() != null) {
            throw new UserError("Incompatible 'page-flows' and 'add-page-flows' parameters " +
                    "in the 'documents' item: " + refineToString(documentRaw));
        }
        List<String> pageFlows = firstNotNull(documentRaw.getPageFlows(),
                defaults.getPageFlows(), new ArrayList<>());
        if (documentRaw.getAddPageFlows() != null) {
            Objects.requireNonNull(pageFlows).addAll(documentRaw.getAddPageFlows());
        }
        argFileDocumentRawBuilder.pageFlows(pageFlows);

        return argFileDocumentRawBuilder.build();
    }

    /**
     * <br /> Returns a tuple:
     * <br />
     * <br /> - Arguments data object without plugins;
     * <br /> - Definitions of the plugins that are defined outside the `plugins` section.
     * <br />
     * <br /> This method does not instantiate plugins.
     */
    public static Pair<ArgFile, Map<String, JsonNode>> completeArgFileProcessing(
            ArgFileRaw canonizedArgFileRaw, Map<String, Md2HtmlPlugin> plugins) {

        // TODO Consider removing "This method does not instantiate plugins" from Javadoc.

        ArgFileOptionsRaw optionsRaw = canonizedArgFileRaw.getOptions();
        ObjectNode documentsPageFlowsPlugin = new ObjectNode(NODE_FACTORY);
        Map<String, JsonNode> extraPluginData = new HashMap<>();
        extraPluginData.put("page-flows", documentsPageFlowsPlugin);

        List<ArgFileDocumentRaw> documentsRaw;
        try {
            documentsRaw = expandDocumentGlobs(canonizedArgFileRaw.getDocuments(), plugins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<Document> documents = new ArrayList<>(documentsRaw.size());

        for (ArgFileDocumentRaw documentRaw : documentsRaw) {
            // Such check was probably done before but let it stay here as a safeguard.
            if (documentRaw.getInput() == null) {
                throw new UserError("Undefined input file for 'documents' item: " +
                        Utils.refineToString(documentRaw));
            }

            ArgFileDocumentRaw enrichedDocumentRaw = enrichDocument(documentRaw);

            List<String> pageFlows = enrichedDocumentRaw.getPageFlows();
            if (pageFlows != null) {
                for (String pageFlow : pageFlows) {
                    ArrayNode pageFlowList = (ArrayNode) documentsPageFlowsPlugin.get(pageFlow);
                    if (pageFlowList ==  null) {
                        pageFlowList = new ArrayNode(NODE_FACTORY);
                        documentsPageFlowsPlugin.set(pageFlow, pageFlowList);
                    }
                    ObjectNode pageFlowNode = new ObjectNode(NODE_FACTORY);
                    pageFlowNode.put("link", enrichedDocumentRaw.getOutput());
                    pageFlowNode.put("title", enrichedDocumentRaw.getTitle());
                    pageFlowList.add(pageFlowNode);
                }
            }

            documents.add(Document.builder()
                    .input(enrichedDocumentRaw.getInput())
                    .output(enrichedDocumentRaw.getOutput())
                    .title(enrichedDocumentRaw.getTitle())
                    .template(enrichedDocumentRaw.getTemplate())
                    .linkCss(enrichedDocumentRaw.getLinkCss())
                    .includeCss(enrichedDocumentRaw.getIncludeCss())
                    .noCss(enrichedDocumentRaw.isNoCss())
                    .force(enrichedDocumentRaw.isForce())
                    .verbose(enrichedDocumentRaw.isVerbose())
                    .report(enrichedDocumentRaw.isReport())
                    .build());
        }

        ArgFile argFile = ArgFile.builder()
                .options(SessionOptions.builder()
                        .verbose(optionsRaw.isVerbose())
                        .legacyMode(optionsRaw.isLegacyMode())
                        .build())
                .documents(documents)
                .build();

        return new Pair<>(argFile, extraPluginData);
    }

    private static List<ArgFileDocumentRaw> expandDocumentGlobs(
            List<ArgFileDocumentRaw> documentsRaw, Map<String, Md2HtmlPlugin> plugins)
            throws IOException {

        List<ArgFileDocumentRaw> expandedDocumentRawList = new ArrayList<>();

        for (ArgFileDocumentRaw documentRaw : documentsRaw) {
            if (isNullOrEmpty(documentRaw.getInputGlob())) {
                expandedDocumentRawList.add(documentRaw);
            } else {
                Path inputRootPath = Paths.get(documentRaw.getInputRoot());
                // TODO Check whether right slashes are used in Linux and Windows.
                List<Path> globPathList = new ArrayList<>(expandGlob(documentRaw.getInputGlob(),
                        inputRootPath));

                // TODO Consider moving this sort lower where the final input file path is defined.
                if (documentRaw.isSortByFilePath()) {
                    globPathList.sort(Path::compareTo);
                }

                List<ArgFileDocumentRaw> globDocumentRawList =
                        new ArrayList<>(globPathList.size());

                for (Path globPath : globPathList) {
                    ArgFileDocumentRaw.ArgFileDocumentRawBuilder globDocumentRawBuilder =
                            documentRaw.toBuilder();
                    globDocumentRawBuilder.inputGlob(null);
                    globDocumentRawBuilder.input(inputRootPath.toAbsolutePath()
                            .relativize(globPath.toAbsolutePath()).toString());

                    if (documentRaw.getTitleFromVariable() != null ||
                            documentRaw.getSortByVariable() != null) {

                        Md2HtmlPlugin pageVariablesPlugin = plugins.get("page-variables");
                        if (pageVariablesPlugin != null) {
                            pageVariablesPlugin.newPage(null);
                            PageMetadataHandlersWrapper pageMetadataHandlersWrapper =
                                    PageMetadataHandlersWrapper.fromPlugins(
                                    Collections.singletonList(pageVariablesPlugin));
                            String inputFileString = getCachedString(globPath,
                                    Utils::readStringFromUtf8File);
                            pageMetadataHandlersWrapper.applyMetadataHandlers(inputFileString,
                                    null);
                            Map<String, Object> pageVariables =
                                    pageVariablesPlugin.variables(null);

                            Optional.ofNullable(documentRaw.getTitleFromVariable())
                                    .flatMap(titleVar ->
                                            Optional.ofNullable(pageVariables.get(titleVar)))
                                    .ifPresent(title ->
                                            globDocumentRawBuilder.title((String) title));

                            Optional.ofNullable(documentRaw.getSortByVariable())
                                    .ifPresent(sortVar ->
                                            globDocumentRawBuilder.techSortBy(
                                                    (String) pageVariables.get(sortVar)));
                        }
                    }
                    globDocumentRawList.add(globDocumentRawBuilder.build());
                }
                if (documentRaw.getSortByVariable() != null) {
                    globDocumentRawList
                            .sort(Comparator.comparing(ArgFileDocumentRaw::getTechSortBy));
                } else if (documentRaw.isSortByTitle()) {
                    globDocumentRawList
                            .sort(Comparator.comparing(ArgFileDocumentRaw::getTitle));
                }
                expandedDocumentRawList.addAll(globDocumentRawList);
            }
        }
        return expandedDocumentRawList;
    }

    public static List<Path> expandGlob(String glob, Path relativeTo)
            throws IOException {

        List<Path> pathList = new ArrayList<>();
        final PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + glob.replace("**/", "**"));

        Files.walkFileTree(relativeTo, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                Path relativePath = relativeTo.toAbsolutePath().relativize(path.toAbsolutePath());
                if (pathMatcher.matches(relativePath)) {
                    pathList.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return pathList;
    }

    public static ArgFileDocumentRaw enrichDocument(ArgFileDocumentRaw documentRaw) {

        ArgFileDocumentRaw.ArgFileDocumentRawBuilder newDocumentRawBuilder =
                documentRaw.toBuilder();

        if (documentRaw.getTemplate() == null) {
            newDocumentRawBuilder.template(Constants.WORKING_DIR.resolve(
                        DEFAULT_TEMPLATE_FILE).toString());
        }
        String output = documentRaw.getOutput();
        if (output == null) {
            output = (stripExtension(documentRaw.getInput()) + ".html").replace('\\', '/');
        }
        if (documentRaw.getInputRoot() != null) {
            newDocumentRawBuilder.input(Paths.get(documentRaw.getInputRoot())
                    .resolve(documentRaw.getInput()).toString().replace('\\', '/'));
        }
        if (documentRaw.getOutputRoot() != null) {
            output = Paths.get(documentRaw.getOutputRoot()).resolve(output).toString()
                    .replace('\\', '/');
        }
        newDocumentRawBuilder.output(output);

        if (!documentRaw.isNoCss() && isNullOrEmpty(documentRaw.getLinkCss()) &&
                isNullOrEmpty(documentRaw.getIncludeCss())) {
            newDocumentRawBuilder.includeCss(Collections.singletonList(Constants
                    .WORKING_DIR.resolve(DEFAULT_CSS_FILE).toString().replace('\\', '/')));
        }

        return newDocumentRawBuilder.build();
    }

}
