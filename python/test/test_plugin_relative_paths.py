import sys
import unittest
from pathlib import Path

from .utils_for_tests import *

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *
from plugins.relative_paths_plugin import *


class RelativePathsPluginTest(unittest.TestCase):

    def _find_single_plugin(self, plugins):
        return find_single_instance_of_type(plugins, RelativePathsPlugin)

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], "plugins": {}}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = self._find_single_plugin(plugins.values())
        self.assertIsNone(plugin)
        
    def test_relativisation(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {"relative-paths": { "down1": "down1/", "down11": "down1/down11/", '
            '"down2": "down2/", "down22": "down2/down22/", "root": "", '
            '"up1": "../", "up2": "../../" }}}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = self._find_single_plugin(plugins.values())
        
        rel_paths = plugin.variables({'output': "root.html"})
        self.assertDictEqual({"down1": "down1/", "down11": "down1/down11/", 
                              "down2": "down2/", "down22": "down2/down22/", 
                              "root": "", "up1": "../", "up2": "../../" }, rel_paths)
        
        rel_paths = plugin.variables({'output': "down1/doc.html"})
        self.assertDictEqual({"down1": "", "down11": "down11/", 
                              "down2": "../down2/", "down22": "../down2/down22/", 
                              "root": "../", "up1": "../../", "up2": "../../../" }, rel_paths)
        
        rel_paths = plugin.variables({'output': "down2/down22/doc.html"})
        self.assertDictEqual({"down1": "../../down1/", "down11": "../../down1/down11/", 
                              "down2": "../", "down22": "", 
                              "root": "../../", "up1": "../../../", "up2": "../../../../" }, rel_paths)


if __name__ == '__main__':
    unittest.main()
