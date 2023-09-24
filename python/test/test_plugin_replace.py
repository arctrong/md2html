import unittest

from md2html import *
from plugins.replace_plugin import ReplacePlugin
from .utils_for_tests import find_single_instance_of_type


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, ReplacePlugin)


class ReplacePluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_single_value(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [{"markers": ["marker1"], "replace-with": "[[${1}]]"}] \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--MARKER1  some context  --> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[some context  ]] ending", processed_page)

    def test_several_values(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [{"markers": ["marker1"], "replace-with": "[[${1}-${2}]]"}] \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = 'beginning <!--marker1 ["A", "B"]--> ending'
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[A-B]] ending", processed_page)

        page_text = 'beginning <!--marker1 ["C", "D"]--> ending'
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[C-D]] ending", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [{"markers": ["marker1", "marker2"], "replace-with": "[[${1}]]"}] \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--marker1 some-value--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[some-value]] ending", processed_page)

        page_text = "beginning <!--marker2 some-value--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[some-value]] ending", processed_page)

    def test_several_instances(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [\n'
            '        {"markers": ["marker1"], "replace-with": "s1 ${1} e1"},\n'
            '        {"markers": ["marker2"], "replace-with": "s2 ${1} e2"}\n'
            '    ] \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--marker1 VALUE--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning s1 VALUE e1 ending", processed_page)

        page_text = "beginning <!--marker2 VALUE--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning s2 VALUE e2 ending", processed_page)

    def test_recursive(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [\n'
            '        {"markers": ["m1"], "replace-with": "${1} m1", "recursive": false},\n'
            '        {"markers": ["m2"], "replace-with": "${1} m2 <!--m1 v1-->", "recursive": true},\n'
            '        {"markers": ["m3"], "replace-with": "${1} m3 <!--m1 v1-->"}\n'
            '    ] \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--m2 V2--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning V2 m2 v1 m1 ending", processed_page)

        page_text = "beginning <!--m3 V3--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning V3 m3 <!--m1 v1--> ending", processed_page)
