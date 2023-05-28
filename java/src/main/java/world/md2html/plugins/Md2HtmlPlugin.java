package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.SessionOptions;
import world.md2html.options.model.raw.ArgFileRaw;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Md2HtmlPlugin {

    /**
     * Accepts plugin configuration data. Some plugins may be able to accept data several times.
     */
    void acceptData(JsonNode data) throws ArgFileParseException;

    /**
     * If a plugin is blank its usage will have no effect. This method allows removing such
     * plugins from consideration.
     */
    boolean isBlank();

    /**
     * @return Extra plugins data.
     */
    default Map<String, JsonNode> preInitialize(ArgFileRaw argFileRaw, CliOptions cliOptions,
            Map<String, Md2HtmlPlugin> plugins) {
        return Collections.emptyMap();
    }

    /**
     * This method will be called after all plugins are pre-initialized and before the
     * documents processing.
     */
    default void initialize(JsonNode extraPluginData) throws ArgFileParseException {
    }

    default void acceptAppData(SessionOptions options, List<Md2HtmlPlugin> plugins) {
    }

    /**
     * This method is called after all plugins are initialized and all documents are defined.
     * The list of all documents is sent to the method.
     */
    default void acceptDocumentList(List<Document> documents) {
    }

    /**
     * Reacts to a new page. May be used to reset the plugin's state when a new page comes
     * into processing.
     */
    default void newPage(Document document) {
    }

    default List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return Collections.emptyList();
    }

    default Map<String, Object> variables(Document document) {
        return Collections.emptyMap();
    }

    /**
     * Executes after all pages processed.
     */
    default void finalizePlugin() {
    }

}
