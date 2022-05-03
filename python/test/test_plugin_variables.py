import sys
import unittest
from pathlib import Path

from .utils_for_tests import *

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *
from plugins.variables_plugin import *


class VariablesPluginTest(unittest.TestCase):

    def _find_single_plugin(self, plugins):
        return find_single_instance_of_type(plugins, VariablesPlugin)

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "whatever.md"}], '
            '"plugins": {}}')
        parse_argument_file_content(argument_file_dict, {})
        plugins = process_plugins(argument_file_dict['plugins'])
        plugin = self._find_single_plugin(plugins)
        self.assertIsNone(plugin)
        
    def test_variables(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "whatever.md"}], '
            '"plugins": {"variables": {"var1": "val1", "_var2": "val2", '
            '"strange": "Don\'t do it yourself! -\u002D>" }}}')
        parse_argument_file_content(argument_file_dict, {})
        plugins = process_plugins(argument_file_dict['plugins'])
        plugin = self._find_single_plugin(plugins)
        
        variables = plugin.variables({})
        self.assertDictEqual({"var1": "val1", "_var2": "val2", 
                              "strange": "Don\'t do it yourself! -->" }, variables)


if __name__ == '__main__':
    unittest.main()
