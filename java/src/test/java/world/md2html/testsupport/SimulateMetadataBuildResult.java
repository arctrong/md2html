package world.md2html.testsupport;

import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.List;
import java.util.Map;

public final class SimulateMetadataBuildResult {

    private final Map<Integer, String> output;
    private final Map<Integer, Boolean> deferredPages;
    private final List<Md2HtmlPlugin> plugins;
    private final PageMetadataHandlersWrapper metadataHandlers;
    private final List<Document> documents;

    SimulateMetadataBuildResult(Map<Integer, String> output,
            Map<Integer, Boolean> deferredPages,
            List<Md2HtmlPlugin> plugins,
            PageMetadataHandlersWrapper metadataHandlers,
            List<Document> documents) {
        this.output = output;
        this.deferredPages = deferredPages;
        this.plugins = plugins;
        this.metadataHandlers = metadataHandlers;
        this.documents = documents;
    }

    public Map<Integer, String> getOutput() {
        return output;
    }

    public Map<Integer, Boolean> getDeferredPages() {
        return deferredPages;
    }

    public List<Md2HtmlPlugin> getPlugins() {
        return plugins;
    }

    public PageMetadataHandlersWrapper getMetadataHandlers() {
        return metadataHandlers;
    }

    public List<Document> getDocuments() {
        return documents;
    }
}
