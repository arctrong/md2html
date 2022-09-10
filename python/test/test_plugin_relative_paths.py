import unittest

from md2html import *
from plugins.relative_paths_plugin import *
from .utils_for_tests import *


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, RelativePathsPlugin)


class RelativePathsPluginTest(unittest.TestCase):

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], "plugins": {}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        self.assertIsNone(plugin)
        
    def test_relativisation(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {"relative-paths": { "down1": "down1/", "down11": "down1/down11/", '
            '"down2": "down2/", "down22": "down2/down22/", "root": "", '
            '"up1": "../", "up2": "../../" }}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        
        rel_paths = plugin.variables(Document(output_file="root.html"))
        self.assertDictEqual({"down1": "down1/", "down11": "down1/down11/", 
                              "down2": "down2/", "down22": "down2/down22/", 
                              "root": "", "up1": "../", "up2": "../../" }, rel_paths)
        
        rel_paths = plugin.variables(Document(output_file="down1/doc.html"))
        self.assertDictEqual({"down1": "", "down11": "down11/", 
                              "down2": "../down2/", "down22": "../down2/down22/", 
                              "root": "../", "up1": "../../", "up2": "../../../" }, rel_paths)
        
        rel_paths = plugin.variables(Document(output_file="down2/down22/doc.html"))
        self.assertDictEqual({"down1": "../../down1/", "down11": "../../down1/down11/", 
                              "down2": "../", "down22": "", 
                              "root": "../../", "up1": "../../../", "up2": "../../../../" },
                             rel_paths)
