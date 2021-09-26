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

    def metadata_handler_registration_info(self):
        """
        Returns a triple:
        - the handler that has a method `accept_page_metadata`;
        - the list of markers this handler must accept;
        - `True` if the handler accepts only the metadata sections that are the first non-blank
            content on the page, `False` if the handler accepts all metadata on the page.
        """
        return None, None, False

    def accept_page_metadata(self, output_file: str, marker: str, metadata, metadata_section):
        """
        Accepts `output_file` where the `metadata` was found, the metadata marker, the
        `metadata` itself (as a string) and the whole section `metadata_section` from
        which the `metadata` was extracted.
        Adjusts its internal state accordingly, and returns the text that must replace
        the metadata section in the source text.
        """
        return metadata_section

    def variables(self, doc: dict) -> dict:
        return {}

    def new_page(self):
        """
        Reacts on a new page. May be used to reset the plugins state (or a part of the plugin
        state) when a new page comes to be processed.
        """
        pass
