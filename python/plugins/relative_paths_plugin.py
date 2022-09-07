from pathlib import Path

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin, validate_data_with_file
from utils import relativize_relative_resource_path

MODULE_DIR = Path(__file__).resolve().parent


class RelativePathsPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        # noinspection PyTypeChecker
        self.data: dict = None

    def accept_data(self, data):
        self.assure_accept_data_once()
        validate_data_with_file(data, MODULE_DIR.joinpath('relative_paths_schema.json'))
        self.data = data

    def is_blank(self) -> bool:
        return not bool(self.data)

    def variables(self, doc: Document) -> dict:
        result = {}
        for k, v in self.data.items():
            result[k] = relativize_relative_resource_path(v, doc.output_file)
        return result
