import json
from json import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import UserError, reduce_json_validation_error_message, first_not_none

MODULE_DIR = Path(__file__).resolve().parent


class PageVariablesPlugin(Md2HtmlPlugin):
    def __init__(self):
        super().__init__()
        self.data = {}
        self.page_metadata_handler = PageVariablesCollectingMetadataHandler()

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('page_variables_schema.json'))
        if data:
            self.data.update({k.upper(): v for k, v in data.items()})
        if not self.data:
            self.data = {"VARIABLES": {"only-at-page-start": True}}

    def is_blank(self) -> bool:
        return not bool(self.data)

    def add_if_not_present(self, data):
        self.validate_data_with_file(data, MODULE_DIR.joinpath('page_variables_schema.json'))
        for k, v in data:
            self.data.setdefault(k, v)

    def page_metadata_handlers(self):
        result = []
        for k, v in self.data.items():
            result.append((self.page_metadata_handler, k,
                           first_not_none(v.get("only-at-page-start"), True)))
        return result

    def variables(self, doc: Document) -> dict:
        return self.page_metadata_handler.variables()

    def new_page(self, doc: Document):
        self.page_metadata_handler.reset()


class PageVariablesCollectingMetadataHandler:
    def __init__(self):
        self.page_variables = {}
        with open(MODULE_DIR.joinpath('page_variables_metadata_schema.json'), 'r') as schema_file:
            self.metadata_schema = json.load(schema_file)

    def accept_page_metadata(self, doc: dict, marker: str, metadata_str: str, metadata_section):
        try:
            metadata = json.loads(metadata_str)
            validate(instance=metadata, schema=self.metadata_schema)
        except JSONDecodeError as e:
            raise UserError(f"Incorrect JSON in page metadata: {type(e).__name__}: {str(e)}")
        except ValidationError as e:
            raise UserError(f"Error validating page metadata: {type(e).__name__}: " +
                            reduce_json_validation_error_message(str(e)))
        self.page_variables.update(metadata)
        return ''

    def variables(self) -> dict:
        return self.page_variables

    def reset(self):
        self.page_variables = {}
