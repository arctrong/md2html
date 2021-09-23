from md2html_plugin import Md2HtmlPlugin
from md2html_plugin import PluginDataError
from utils import relativize_relative_resource_path


class RelativePathsPlugin(Md2HtmlPlugin):

    def __init__(self):
        self.activated = False
        # noinspection PyTypeChecker
        self.data: dict = None

    def accept_data(self, data):
        if not isinstance(data, dict):
            raise PluginDataError(f"Plugin data is of type '{type(data).__name__}', not a dict.")
        self.data = data
        self.activated = True

    def variables(self, doc: dict) -> dict:
        result = {}
        output_file = str(doc['output_file'])
        for k, v in self.data.items():
            result[k] = relativize_relative_resource_path(v, output_file)
        return result
