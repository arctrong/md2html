import json
from abc import ABC

from jsonschema import validate


class PluginDataError(ValueError):
    pass


def validate_data(data, schema_file):
    try:
        with open(schema_file, 'r') as schema_file:
            schema = json.load(schema_file)
        validate(instance=data, schema=schema)
    except Exception as e:
        raise PluginDataError(f'Error validating plugin data: {type(e).__name__}: {e}')


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
        - the handler that has a method `accept_page_metadatum`;
        - the list of markers this handler must accept;
        - `True` if the handler accepts only the metadata sections that are the first non-blank
            content on the page, `False` if the handler accepts all metadata on the page.
        """
        return None, None, False

    def accept_page_metadatum(self, output_file: str, marker: str, metadatum, metadatum_section):
        """
        Accepts `output_file` where the `metadatum` was found, the metadatum marker, the
        `metadatum` itself (as a string) and the whole section `metadatum_section` from
        which the `metadatum` was extracted.
        Adjusts its internal state accordingly, and returns the text that must replace
        the metadatum section in the source text.
        """
        return metadatum_section

    def variables(self, doc: dict) -> dict:
        return {}

    def new_page(self):
        """
        Reacts on a new page. May be used to reset the plugins state (or a part of the plugin
        state) when a new page comes to be processed.
        """
        pass
