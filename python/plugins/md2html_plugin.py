import json
from abc import ABC

from jsonschema import validate, ValidationError

from cli_arguments_utils import CliArgDataObject
from utils import UserError, reduce_json_validation_error_message


class PluginDataUserError(UserError):
    pass


def validate_data_with_file(data, schema_file):
    with open(schema_file, 'r') as schema_file:
        schema = json.load(schema_file)
    validate_data_with_schema(data, schema)


def validate_data_with_schema(data, schema):
    try:
        validate(instance=data, schema=schema)
    except ValidationError as e:
        raise UserError(f"Error validating plugin data: {type(e).__name__}: " +
                        reduce_json_validation_error_message(str(e)))


class Md2HtmlPlugin(ABC):

    def accept_data(self, data):
        """
        Accepts plugin configuration data. Plugin may be asked to accept data several times.
        """
        pass

    def is_blank(self) -> bool:
        """
        If a plugin is blank its usage will have no effect. This method allows removing such
        plugins from consideration.
        """
        return True

    def initialize(self, argument_file_dict: dict, cli_args: CliArgDataObject, plugins: dict):
        """
        This method is going to be called before the documents processing.
        """
        pass

    def page_metadata_handlers(self) -> list:
        """
        Returns a list of tuples:
        - page metadata handler that must have the method `accept_page_metadata`;
        - marker that the handler must accept;
        - the boolean value that states if the handler accepts only the metadata sections
            that are the first non-blank content on the page, `False` means that the handler
            accepts all metadata on the page.
        """
        return []

    def accept_page_metadata(self, doc: dict, marker: str, metadata, metadata_section) -> str:
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

    def finalize(self, argument_file_dict: dict, cli_args: CliArgDataObject, plugins: dict):
        """
        Executes after all page processed.
        """
        pass