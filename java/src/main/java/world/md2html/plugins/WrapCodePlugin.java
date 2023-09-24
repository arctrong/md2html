package world.md2html.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import world.md2html.Md2Html;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.options.model.raw.ArgFileDocumentRaw;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.utils.CheckedIllegalArgumentException;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static world.md2html.Md2HtmlUtils.generateHtml;
import static world.md2html.options.argfile.ArgFileParsingHelper.completeArgFileProcessing;
import static world.md2html.options.argfile.ArgFileParsingHelper.mergeAndCanonizeArgFileRaw;
import static world.md2html.utils.JsonUtils.OBJECT_MAPPER_FOR_BUILDERS;
import static world.md2html.utils.JsonUtils.deJson;
import static world.md2html.utils.Utils.getCachedString;
import static world.md2html.utils.Utils.relativizeRelativeResource;
import static world.md2html.utils.Utils.supplyWithFileExceptionAsUserError;

public class WrapCodePlugin extends AbstractMd2HtmlPlugin implements PageMetadataHandler {

    private static class WrapCodeData {
        private String style = "";
        private Map<String, Object> variables = new HashMap<>();
        private ObjectNode documentJson;
        private Document documentObj;
    }

    private Map<String, WrapCodeData> data;
    private final Map<String, String> processedCache = new HashMap<>();

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    private boolean dryRun;

    private SessionOptions options;
    private List<Md2HtmlPlugin> plugins;

    @Override
    public void acceptData(JsonNode data) throws ArgFileParseException {

        assureAcceptDataOnce();
        validateInputDataAgainstSchemaFromResource(data, "plugins/wrap_code_schema.json");

        Map<String, WrapCodeData> dataMap = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = data.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
            ObjectNode markerDataNode = fieldEntry.getValue().deepCopy();

            WrapCodeData wrapCodeData = new WrapCodeData();
            wrapCodeData.documentJson = markerDataNode;

            ValueNode styleNode = (ValueNode) markerDataNode.get("style");
            if (styleNode != null) {
                wrapCodeData.style = styleNode.asText();
            }
            ObjectNode variablesNode = (ObjectNode) markerDataNode.get("variables");
            if (variablesNode != null) {
                //noinspection unchecked
                wrapCodeData.variables = (Map<String, Object>) deJson(variablesNode);
            }
            markerDataNode.remove("style");
            markerDataNode.remove("variables");
            dataMap.put(fieldEntry.getKey().toUpperCase(), wrapCodeData);
        }

        this.data = dataMap;
    }

    @Override
    public boolean isBlank() {
        return data.isEmpty();
    }

    @Override
    public Map<String, JsonNode> preInitialize(ArgFileRaw argFileRaw, CliOptions cliOptions,
                                               Map<String, Md2HtmlPlugin> plugins) {

        for (WrapCodeData wrapCodeData : data.values()) {

            ArgFileDocumentRaw argFileDocumentRaw;
            try {
                argFileDocumentRaw = OBJECT_MAPPER_FOR_BUILDERS
                        .treeToValue(wrapCodeData.documentJson, ArgFileDocumentRaw.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            ArgFileRaw newArgFileRaw = argFileRaw.toBuilder().documents(Collections.singletonList(
                    argFileDocumentRaw.toBuilder()
                            .input("fictional")
                            .output("fictional")
                            .build())
            ).build();

            ArgFileRaw canonizedArgFileRaw =
                    mergeAndCanonizeArgFileRaw(newArgFileRaw, cliOptions);
            ArgFile arguments =
                    completeArgFileProcessing(canonizedArgFileRaw, plugins).getValue0();

            Document documentObj = arguments.getDocuments().get(0);
            String newInput = documentObj.getInput();
            String newOutput = documentObj.getOutput();
            wrapCodeData.documentObj = documentObj.toBuilder()
                    .input(newInput.substring(0, newInput.length() - 9))
                    .output(newOutput.substring(0, newOutput.length() - 9))
                    .build();
        }
        return Collections.emptyMap();
    }

    @Override
    public void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins,
                              PageMetadataHandlersWrapper metadataHandlers) {
        this.options = options;
        this.plugins = plugins;
    }

    @Override
    public List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return this.data.keySet().stream().map(marker ->
                new PageMetadataHandlerInfo(this, marker, false)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> variables(Document document) {
        return Collections.emptyMap();
    }

    @Override
    public String acceptPageMetadata(Document document, String marker, String metadata,
                                     String metadataSection) throws PageMetadataException {

        marker = marker.toUpperCase();
        WrapCodeData markerData = this.data.get(marker);
        metadata = metadata.trim();
        Document documentObj = markerData.documentObj;

        Path inputFile = Paths.get(documentObj.getInput(), metadata);
        String inputFileStr = inputFile.toString().replace("\\", "/");
        String cacheKey = marker + "|" + inputFileStr;

        String outputFileStr = this.processedCache.get(cacheKey);
        if (outputFileStr == null) {
            Path outputFile = Paths.get(documentObj.getOutput(), metadata + ".html");
            outputFileStr = outputFile.toString().replace("\\", "/");

            boolean needToGenerate = true;
            if (!documentObj.isForce() && Files.exists(outputFile)) {
                FileTime inputFileTime;
                FileTime outputFileTime;
                try {
                    inputFileTime = Files.getLastModifiedTime(inputFile);
                    outputFileTime = Files.getLastModifiedTime(outputFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (outputFileTime.compareTo(inputFileTime) > 0) {
                    if (document.isVerbose()) {
                        System.out.println("Wrapped output file is up-to-date. Skipping: "
                                + document.getOutput());
                        needToGenerate = false;
                    }
                }
            }

            if (needToGenerate && !this.dryRun) {
                documentObj = documentObj.toBuilder()
                        .input(inputFileStr)
                        .output(outputFileStr)
                        .build();

                String content = supplyWithFileExceptionAsUserError(
                        () -> getCachedString(inputFile, Utils::readStringFromUtf8File),
                        "Error processing page metadata block"
                );
                String docContent = "````" + markerData.style + "\n" +
                         content + "\n" + "````";

                Map<String, Object> substitutions = new HashMap<>();
                substitutions.put("content", generateHtml(docContent));

                Map<String, Object> variables = new HashMap<>(markerData.variables);
                String fileName = Paths.get(metadata).getFileName().toString();
                variables.put("title", fileName);
                variables.put("wrap_code_path", metadata);
                variables.put("wrap_code_file_name", fileName);

                Md2Html.outputPage(documentObj, this.plugins, substitutions, this.options,
                        variables);

                if (documentObj.isVerbose()) {
                    System.out.println("Wrapped output file generated: " + documentObj.getOutput());
                }
                if (documentObj.isReport()) {
                    System.out.println(documentObj.getOutput());
                }
            }
            this.processedCache.put(cacheKey, outputFileStr);
        }

        try {
            return relativizeRelativeResource(outputFileStr, document.getOutput());
        } catch (CheckedIllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}
