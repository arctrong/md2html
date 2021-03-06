package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.Document;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Md2HtmlPlugin {

    /**
     * Returns the plugin activated state. After accepting the given data, the plugin may declare
     * itself as not activated and return `False`. In this case it should not be used.
     */
    boolean acceptData(JsonNode data) throws ArgFileParseException;

    /**
     * Returns a list of actions that are going to be fulfilled before the documents processing.
     */
    default List<InitializationAction> initializationActions() {
        return Collections.emptyList();
    }

    default List<PageMetadataHandlerInfo> pageMetadataHandlers() {
        return Collections.emptyList();
    }

    /**
     * Reacts to a new page. May be used to reset the plugin's state when a new page comes
     * into processing.
     */
    default void newPage(Document document) {
    }

    default Map<String, Object> variables(Document document) {
        return Collections.emptyMap();
    }

    /**
     * Returns a list of actions that are going to be fulfilled after all documents are processed.
     */
    default List<FinalizationAction> finalizationActions() {
        return Collections.emptyList();
    }

}
