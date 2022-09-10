import unittest

from md2html import *
from plugins.variables_plugin import *
from .utils_for_tests import *


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, VariablesPlugin)


class VariablesPluginTest(unittest.TestCase):

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], "plugins": {}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        self.assertIsNone(plugin)

    def test_variables(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {"variables": {"var1": "val1", "_var2": "val2", '
            '"strange": "Don\'t do it yourself! -\u002D>" }}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        variables = plugin.variables({})
        self.assertDictEqual({"var1": "val1", "_var2": "val2", 
                              "strange": "Don\'t do it yourself! -->"}, variables)


if __name__ == '__main__':
    unittest.main()
