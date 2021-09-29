import json
from pathlib import Path

from jsonschema import validate, ValidationError

from plugins.md2html_plugin import Md2HtmlPlugin, validate_data
from utils import UserError, reduce_json_validation_error_message, first_not_none

MODULE_DIR = Path(__file__).resolve().parent


class PageVariablesPlugin(Md2HtmlPlugin):
    def __init__(self):
        self.data = None
        self.page_variables = {}
        with open(MODULE_DIR.joinpath('page_variables_metadata_schema.json'), 'r') as schema_file:
            self.metadata_schema = json.load(schema_file)

    def accept_data(self, data):
        validate_data(data, MODULE_DIR.joinpath('page_variables_schema.json'))
        self.data = data
        return bool(self.data)

    def page_metadata_handlers(self):
        result = []
        for k, v in self.data.items():
            result.append((self, k, first_not_none(v.get("only-at-page-start"), True)))
        return result

    def accept_page_metadata(self, doc: dict, marker: str, metadata_str: str, metadata_section):
        try:
            metadata = json.loads(metadata_str)
            validate(instance=metadata, schema=self.metadata_schema)
        except ValidationError as e:
            raise UserError(f"Error validating page metadata: {type(e).__name__}: " +
                            reduce_json_validation_error_message(str(e)))
        self.page_variables.update(metadata)
        return ''

    def variables(self, doc: dict) -> dict:
        return self.page_variables

    def new_page(self):
        self.page_variables = {}
