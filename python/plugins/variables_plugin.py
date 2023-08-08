from pathlib import Path

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin

MODULE_DIR = Path(__file__).resolve().parent


class VariablesPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        # noinspection PyTypeChecker
        self.data: dict = None

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('variables_plugin_schema.json'))
        self.data = data

    def is_blank(self) -> bool:
        return not bool(self.data)

    def variables(self, doc: Document) -> dict:
        return self.data
