import unittest

from md2html import *
from plugins.relative_paths_plugin import *
from .utils_for_tests import *


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, RelativePathsPlugin)


class RelativePathsPluginTest(unittest.TestCase):

    def test_notDefined(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], "plugins": {}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        self.assertIsNone(plugin)

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {'
            '    "relative-paths": {}'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        self.assertIsNone(plugin)

    def test_notActivated_newSyntax(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {'
            '    "relative-paths": {"markers": ["p1", "p2"], "paths": {}}'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        self.assertIsNone(plugin)

    def test_minimal_newSyntax(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.md"}], '
            '"plugins": {'
            '    "relative-paths": {"markers": ["path"], "paths": {"pict": "doc/pict/"}}'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        rel_paths = plugin.variables(Document(output_file="root.html"))
        self.assertDictEqual({"pict": "doc/pict/"}, rel_paths)

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
                              "root": "", "up1": "../", "up2": "../../"}, rel_paths)
        
        rel_paths = plugin.variables(Document(output_file="down1/doc.html"))
        self.assertDictEqual({"down1": "", "down11": "down11/", 
                              "down2": "../down2/", "down22": "../down2/down22/", 
                              "root": "../", "up1": "../../", "up2": "../../../" }, rel_paths)
        
        rel_paths = plugin.variables(Document(output_file="down2/down22/doc.html"))
        self.assertDictEqual({"down1": "../../down1/", "down11": "../../down1/down11/", 
                              "down2": "../", "down22": "", 
                              "root": "../../", "up1": "../../../", "up2": "../../../../" },
                             rel_paths)

    def test_substitution_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "relative-paths": {"markers": ["path1"], "paths": {"pict1": "doc/pict/"}} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "![](<!--path1 pict1-->img1.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("![](doc/pict/img1.png)", processed_page)

    def test_different_paths(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt"}, \n'
            '    {"input": "subdir/page2.txt"} \n'
            '], \n'
            '"plugins": { \n'
            '    "relative-paths": {"markers": ["path2"], "paths": {"pict2": "doc/pict/"}} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc1 = args.documents[0]
        doc2 = args.documents[1]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "![](<!--path2 pict2-->img2.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc1)
        self.assertEqual("![](doc/pict/img2.png)", processed_page)

        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc2)
        self.assertEqual("![](../doc/pict/img2.png)", processed_page)

    def test_unknown_path_must_ignore(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "relative-paths": {"markers": ["path2"], "paths": {"pict2": "doc/pict/"}} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc1 = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "![](<!--path2 unknown-->img2.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc1)
        self.assertEqual("![](<!--path2 unknown-->img2.png)", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "relative-paths": {"markers": ["p1", "p2"], \n'
            '        "paths": {"pict2": "doc/pict/", "pict3": "doc/layout/pict/"}} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "![](<!--p1 pict2-->img.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("![](doc/pict/img.png)", processed_page)

        page_text = "![](<!--p2 pict2-->img.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("![](doc/pict/img.png)", processed_page)

        page_text = "![](<!--p1 pict3-->img.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("![](doc/layout/pict/img.png)", processed_page)

        page_text = "![](<!--p2 pict3-->img.png)"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("![](doc/layout/pict/img.png)", processed_page)
