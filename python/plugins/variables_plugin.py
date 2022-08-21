from pathlib import Path

from plugins.md2html_plugin import Md2HtmlPlugin, validate_data_with_file

MODULE_DIR = Path(__file__).resolve().parent


class VariablesPlugin(Md2HtmlPlugin):

    def __init__(self):
        # noinspection PyTypeChecker
        self.data: dict = None

    def accept_data(self, data):
        # Yes, this plugin uses JSON schema of the other plugin as they are the same.
        validate_data_with_file(data, MODULE_DIR.joinpath('relative_paths_schema.json'))
        self.data = data

    def is_blank(self) -> bool:
        return not bool(self.data)

    def variables(self, doc: dict) -> dict:
        return self.data
