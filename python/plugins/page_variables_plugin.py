import json
from json import JSONDecodeError
from pathlib import Path
from typing import Union, Dict, Optional

from jsonschema import validate, ValidationError

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin, MetadataProcessingResult
from utils import UserError, reduce_json_validation_error_message, first_not_none

MODULE_DIR = Path(__file__).resolve().parent


class PageVariablesPlugin(Md2HtmlPlugin):
    def __init__(self):
        super().__init__()
        self.data = {}
        self._variables_by_input_file: Dict[str, dict] = {}
        self._variables_without_input_file: dict = {}
        with open(MODULE_DIR.joinpath('page_variables_metadata_schema.json'), 'r',
                  encoding="utf-8") as schema_file:
            self._metadata_schema = json.load(schema_file)

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
        return [(self, k, first_not_none(v.get("only-at-page-start"), True))
                for k, v in self.data.items()]

    def accept_page_metadata(self, doc: Optional[Document], marker: str, metadata_str: str,
                             metadata_section, visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        metadata = self._parse_and_validate_metadata(metadata_str)
        self._variables_without_input_file.update(metadata)
        input_file = _document_input_file(doc)
        if input_file:
            self._variables_by_input_file.setdefault(input_file, {}).update(metadata)
        return MetadataProcessingResult('')

    def variables(self, doc: Optional[Document]) -> dict:
        input_file = _document_input_file(doc)
        return (self._variables_by_input_file.get(input_file, {}) if input_file 
                else self._variables_without_input_file)

    def new_page(self, doc: Optional[Document]):
        self._variables_without_input_file = {}
        input_file = _document_input_file(doc)
        if input_file:
            self._variables_by_input_file[input_file] = {}

    def _parse_and_validate_metadata(self, metadata_str: str) -> dict:
        try:
            metadata = json.loads(metadata_str)
            validate(instance=metadata, schema=self._metadata_schema)
        except JSONDecodeError as e:
            raise UserError(f"Incorrect JSON in page metadata: {type(e).__name__}: {str(e)}")
        except ValidationError as e:
            raise UserError(f"Error validating page metadata: {type(e).__name__}: " +
                            reduce_json_validation_error_message(str(e)))
        return metadata


def _document_input_file(doc: Optional[Document]) -> Optional[str]:
    return None if doc is None else doc.input_file
