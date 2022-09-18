import json
from abc import ABC
from typing import Any, Dict, List

from jsonschema import validate, ValidationError

from cli_arguments_utils import CliArgDataObject
from models.document import Document
from models.options import Options
from utils import UserError, reduce_json_validation_error_message


class PluginDataUserError(UserError):
    pass


class Md2HtmlPlugin(ABC):

    def __init__(self):
        self.data_accepted = False

    def accept_data(self, data):
        """
        Accepts plugin configuration data. Some plugins may be able to accept data several times.
        """
        pass

    def pre_initialize(self, argument_file_dict: dict, cli_args: CliArgDataObject,
                       plugins: dict) -> Dict[str, Any]:
        """
        Returns extra plugins data.
        """
        return {}

    def initialize(self, extra_plugin_data):
        """
        This method will be called after all plugins are `pre_initialized` and before the
        documents processing.
        """
        pass

    def accept_document_list(self, docs: List[Document]):
        """
        This method is called after all plugins are initialized and all documents are defined.
        The list of all documents is sent to the method.
        """
        pass

    def is_blank(self) -> bool:
        """
        If a plugin is blank its usage will have no effect. This method allows removing such
        plugins from consideration.
        """
        return True

    def page_metadata_handlers(self) -> list:
        # TODO Consider returning an object
        """
        Returns a list of tuples:
        - page metadata handler that must have the method `accept_page_metadata`;
        - marker that the handler must accept;
        - the boolean value that states if the handler accepts only the metadata sections
            that are the first non-blank content on the page, `False` means that the handler
            accepts all metadata on the page.
        """
        return []

    def accept_page_metadata(self, doc: Document, marker: str, metadata,
                             metadata_section) -> str:
        """
        Accepts document `doc` where the `metadata` was found, the metadata marker, the
        `metadata` itself (as a string) and the whole section `metadata_section` from
        which the `metadata` was extracted.
        Adjusts the plugin's internal state accordingly, and returns the text that must replace
        the metadata section in the source text.
        """
        return metadata_section

    def variables(self, doc: Document) -> dict:
        return {}

    def new_page(self, doc: Document):
        """
        Reacts on a new page. May be used to reset the plugins state (or a part of the plugin
        state) when a new page comes into processing.
        """
        pass

    def finalize(self, plugins: list, options: Options):
        """
        Executes after all pages processed.
        """
        pass

    def assure_accept_data_once(self):
        if self.data_accepted:
            raise Exception("Trying to accept data again.")
        self.data_accepted = True

    def validate_data_with_file(self, data, schema_file):
        with open(schema_file, 'r', encoding="utf-8") as schema_file:
            schema = json.load(schema_file)
        self.validate_data_with_schema(data, schema)

    def validate_data_with_schema(self, data, schema):
        try:
            validate(instance=data, schema=schema)
        except ValidationError as e:
            raise UserError(f"Error validating plugin data: {type(self).__name__}: "
                            f"{type(e).__name__}: " + reduce_json_validation_error_message(str(e)))
