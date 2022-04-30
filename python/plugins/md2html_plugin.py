import json
from abc import ABC

from jsonschema import validate, ValidationError

from utils import UserError, reduce_json_validation_error_message


class PluginDataUserError(UserError):
    pass


def validate_data(data, schema_file):
    with open(schema_file, 'r') as schema_file:
        schema = json.load(schema_file)
    try:
        validate(instance=data, schema=schema)
    except ValidationError as e:
        raise UserError(f"Error validating plugin data: {type(e).__name__}: " +
                        reduce_json_validation_error_message(str(e)))


class Md2HtmlPlugin(ABC):

    def accept_data(self, data) -> bool:
        """
        Returns the plugin activated state. After accepting the data, the plugin may declare
        itself as not activated and return `False`. In this case it should not be used.
        """
        return False

    def page_metadata_handlers(self):
        """
        Returns a list of tuples:
        - page metadata handler that must have the method `accept_page_metadata`;
        - marker that the handler must accept;
        - the boolean value that states if the handler accepts only the metadata sections
            that are the first non-blank content on the page, `False` means that the handler
            accepts all metadata on the page.
        """
        return []

    def accept_page_metadata(self, doc: dict, marker: str, metadata, metadata_section):
        """
        Accepts document `doc` where the `metadata` was found, the metadata marker, the
        `metadata` itself (as a string) and the whole section `metadata_section` from
        which the `metadata` was extracted.
        Adjusts the plugin's internal state accordingly, and returns the text that must replace
        the metadata section in the source text.
        """
        return metadata_section

    def variables(self, doc: dict) -> dict:
        return {}

    def new_page(self, doc: dict):
        """
        Reacts on a new page. May be used to reset the plugins state (or a part of the plugin
        state) when a new page comes into processing.
        """
        pass

    def get_additional_documents(self) -> list:
        """
        A plugin may generate additional documents, like an index file. This method returns  a list of
        such additional document definitions to be processed using the command line arguments and
        the argument file.
        """
        return []

    def set_additional_documents_processed(self, documents, plugins, metadata_handlers, options):
        """
        Accepts additional documents processed using the command line arguments and
        the argument file. Also accepts the contextual information.
        """
        pass

    def after_all_page_processed_actions(self):
        """
        Returns a list of handlers that must have the method `execute_after_all_page_processed`.
        """
        return []

    def execute_after_all_page_processed(self):
        pass
