from pathlib import Path
from typing import List

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin
from plugins.plugin_utils import list_from_string_or_array
from utils import UserError, VariableReplacer

MODULE_DIR = Path(__file__).resolve().parent


class ReplacePlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.metadata_handlers = []
        self.replacers = {}

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('replace_schema.json'))

        for datum in data:
            markers = datum.get("markers")
            replace_with = datum.get("replace-with")
            try:
                replacer = VariableReplacer(replace_with)
                self.replacers.update({k.upper(): replacer for k in markers})
            except UserError as e:
                raise UserError(f"{str(e)}, the template is: '{replace_with}'")

            self.metadata_handlers.extend((self, m, False) for m in markers)

    def is_blank(self) -> bool:
        return not bool(self.metadata_handlers)

    def page_metadata_handlers(self):
        return self.metadata_handlers

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str,
                             metadata_section: str):
        # Preserving trailing spaces
        metadata_str = metadata_str.lstrip()

        try:
            metadata = list_from_string_or_array(metadata_str)
        except UserError as e:
            raise UserError(f"Error in replace entry: {str(e)}")

        return self.replacers[marker.upper()].replace(metadata)
