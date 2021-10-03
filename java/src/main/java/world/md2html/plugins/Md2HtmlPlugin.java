package world.md2html.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.UserError;
import world.md2html.options.model.Document;

import java.util.Map;

public interface Md2HtmlPlugin {

    /**
     * Returns the plugin activated state. After accepting the given data, the plugin may declare
     * itself as not activated and return `False`. In this case it should not be used.
     */
    boolean acceptData(JsonNode data);

    Map<String, Object> variables(Document document);

//    def page_metadata_handlers(self):
//            """
//        Returns a list of tuples:
//        - page metadata handler that must have the method `accept_page_metadata`;
//        - marker that the handler must accept;
//        - the boolean value that states if the handler accepts only the metadata sections
//            that are the first non-blank content on the page, `False` means that the handler
//            accepts all metadata on the page.
//        """
//            return []
//
//    def accept_page_metadata(self, doc: dict, marker: str, metadata, metadata_section):
//            """
//        Accepts document `doc` where the `metadata` was found, the metadata marker, the
//        `metadata` itself (as a string) and the whole section `metadata_section` from
//        which the `metadata` was extracted.
//        Adjusts the plugin's internal state accordingly, and returns the text that must replace
//        the metadata section in the source text.
//        """
//            return metadata_section
//
//    def variables(self, doc: dict) -> dict:
//            return {}
//
//    def new_page(self):
//            """
//        Reacts on a new page. May be used to reset the plugins state (or a part of the plugin
//        state) when a new page comes to be processed.
//        """
//    pass

    class PluginDataUserError extends UserError {
        public PluginDataUserError(String message) {
            super(message);
        }
    }

}
