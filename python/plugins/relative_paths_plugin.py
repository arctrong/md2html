from pathlib import Path

from plugins.md2html_plugin import Md2HtmlPlugin, validate_data
from plugins.md2html_plugin import PluginDataError
from utils import relativize_relative_resource_path

MODULE_DIR = Path(__file__).resolve().parent


class RelativePathsPlugin(Md2HtmlPlugin):

    def __init__(self):
        # noinspection PyTypeChecker
        self.data: dict = None

    def accept_data(self, data):
        validate_data(data, MODULE_DIR.joinpath('relative_paths_schema.json'))
        if not isinstance(data, dict):
            raise PluginDataError(f"Plugin data is of type '{type(data).__name__}', not a dict.")
        self.data = data

    def variables(self, doc: dict) -> dict:
        result = {}
        output_file = str(doc['output_file'])
        for k, v in self.data.items():
            result[k] = relativize_relative_resource_path(v, output_file)
        return result
