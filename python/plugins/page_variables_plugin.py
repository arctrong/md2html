import json
from pathlib import Path

from jsonschema import validate

from plugins.md2html_plugin import Md2HtmlPlugin, validate_data
from plugins.md2html_plugin import PluginDataError

MODULE_DIR = Path(__file__).resolve().parent


class PageVariablesPlugin(Md2HtmlPlugin):

    def __init__(self):
        self.markers = None
        self.only_at_page_start = False
        self.page_variables = {}
        with open(MODULE_DIR.joinpath('page_variables_metadatum_schema.json'), 'r') as schema_file:
            self.metadata_schema = json.load(schema_file)

    def accept_data(self, data):
        validate_data(data, MODULE_DIR.joinpath('page_variables_schema.json'))
        self.markers = data["markers"]
        self.only_at_page_start = bool(data.get("only-at-page-start"))
        return bool(self.markers)

    def metadata_handler_registration_info(self):
        return self, self.markers, self.only_at_page_start

    def accept_page_metadatum(self, output_file: str, marker: str, metadata: str,
                              metadata_section):
        try:
            metadata = json.loads(metadata)
            validate(instance=metadata, schema=self.metadata_schema)
        except Exception as e:
            raise PluginDataError(f'Error validating page metadata: {type(e).__name__}: {e}')
        self.page_variables = metadata
        return ''

    def variables(self, doc: dict) -> dict:
        return self.page_variables

    def new_page(self):
        self.page_variables = {}
