package world.md2html.testsupport;

import lombok.experimental.UtilityClass;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.pagemetadata.MetadataHandlersApplicationResult;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.testutils.PluginTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static world.md2html.options.TestUtils.parseArgumentFile;

@UtilityClass
public final class SimulateMetadataBuild {

    public static PageMetadataHandlersWrapper wirePlugins(List<Md2HtmlPlugin> plugins) {
        return wirePlugins(plugins, null);
    }

    public static PageMetadataHandlersWrapper wirePlugins(List<Md2HtmlPlugin> plugins,
            List<Document> documents) {
        PageMetadataHandlersWrapper metadataHandlers =
                PageMetadataHandlersWrapper.fromPlugins(plugins);
        if (documents != null) {
            for (Md2HtmlPlugin plugin : plugins) {
                plugin.acceptDocumentList(documents);
            }
        }
        SessionOptions options = SessionOptions.builder().build();
        for (Md2HtmlPlugin plugin : plugins) {
            plugin.acceptAppData(options, plugins, metadataHandlers);
        }
        return metadataHandlers;
    }

    public static Map<Integer, Document> documentsForPageIndices(Collection<Integer> indices) {
        Map<Integer, Document> documentsByIndex = new HashMap<>();
        for (Integer index : indices) {
            documentsByIndex.put(index, PluginTestUtils.documentWithOutputLocation(
                    "page" + index + ".html"));
        }
        return documentsByIndex;
    }

    public static SimulateMetadataBuildResult simulateMetadataBuild(
            List<Md2HtmlPlugin> plugins,
            List<Map.Entry<Integer, String>> pages,
            Map<Integer, Document> documentsByIndex) {
        return simulateMetadataBuild(plugins, pages, documentsByIndex, true, null);
    }

    public static SimulateMetadataBuildResult simulateMetadataBuild(
            List<Md2HtmlPlugin> plugins,
            List<Map.Entry<Integer, String>> pages,
            Map<Integer, Document> documentsByIndex,
            boolean runPhase2) {
        return simulateMetadataBuild(plugins, pages, documentsByIndex, runPhase2, null);
    }

    public static SimulateMetadataBuildResult simulateMetadataBuild(
            List<Md2HtmlPlugin> plugins,
            List<Map.Entry<Integer, String>> pages,
            Map<Integer, Document> documentsByIndex,
            boolean runPhase2,
            List<Document> allDocuments) {
        List<Document> documentsForPlugins = allDocuments != null
                ? allDocuments
                : new ArrayList<>(documentsByIndex.values());
        PageMetadataHandlersWrapper metadataHandlers = wirePlugins(plugins, documentsForPlugins);

        Map<Integer, String> output = new HashMap<>();
        Map<Integer, MetadataHandlersApplicationResult> deferred = new HashMap<>();
        Map<Integer, Boolean> deferredPages = new HashMap<>();

        for (Map.Entry<Integer, String> page : pages) {
            int index = page.getKey();
            String text = page.getValue();
            Document doc = documentsByIndex.get(index);
            for (Md2HtmlPlugin plugin : plugins) {
                plugin.newPage(doc);
            }
            MetadataHandlersApplicationResult result =
                    metadataHandlers.applyMetadataHandlersWithResult(text, doc);
            deferredPages.put(index, result.isDeferPage());
            if (result.isDeferPage()) {
                deferred.put(index, result);
            } else {
                output.put(index, metadataHandlers.joinParsingResults(
                        result.getParsingResults(), doc));
            }
        }

        if (runPhase2) {
            for (Map.Entry<Integer, MetadataHandlersApplicationResult> entry : deferred.entrySet()) {
                int index = entry.getKey();
                Document doc = documentsByIndex.get(index);
                output.put(index, metadataHandlers.joinParsingResults(
                        entry.getValue().getParsingResults(), doc));
            }
        }

        List<Document> orderedDocuments = new ArrayList<>();
        for (Map.Entry<Integer, String> page : pages) {
            orderedDocuments.add(documentsByIndex.get(page.getKey()));
        }

        return new SimulateMetadataBuildResult(
                output, deferredPages, plugins, metadataHandlers, orderedDocuments);
    }

    public static SimulateMetadataBuildResult simulateMetadataBuildFromArgFile(
            String argFileStr,
            List<Map.Entry<Integer, String>> pages,
            CliOptions cliOptions) throws ArgFileParseException {
        return simulateMetadataBuildFromArgFile(argFileStr, pages, cliOptions, true);
    }

    public static SimulateMetadataBuildResult simulateMetadataBuildFromArgFile(
            String argFileStr,
            List<Map.Entry<Integer, String>> pages,
            CliOptions cliOptions,
            boolean runPhase2) throws ArgFileParseException {
        ArgFile argFile = parseArgumentFile(argFileStr, cliOptions);
        Map<Integer, Document> documentsByIndex = new HashMap<>();
        for (Map.Entry<Integer, String> page : pages) {
            documentsByIndex.put(page.getKey(), argFile.getDocuments().get(page.getKey()));
        }
        return simulateMetadataBuild(
                argFile.getPlugins(),
                pages,
                documentsByIndex,
                runPhase2,
                argFile.getDocuments());
    }
}
