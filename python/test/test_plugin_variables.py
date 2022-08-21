import sys
import unittest
from pathlib import Path

from .utils_for_tests import *

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *
from plugins.variables_plugin import *


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, VariablesPlugin)


class VariablesPluginTest(unittest.TestCase):

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], "plugins": {}}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(plugins.values())
        self.assertIsNone(plugin)

    def test_variables(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {"variables": {"var1": "val1", "_var2": "val2", '
            '"strange": "Don\'t do it yourself! -\u002D>" }}}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(plugins.values())
        variables = plugin.variables({})
        self.assertDictEqual({"var1": "val1", "_var2": "val2", 
                              "strange": "Don\'t do it yourself! -->"}, variables)


if __name__ == '__main__':
    unittest.main()
