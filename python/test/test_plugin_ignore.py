import unittest

from md2html import *
from plugins.ignore_plugin import IgnorePlugin
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, IgnorePlugin)


class IgnorePluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "ignore": {} \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning<!--ignore \t  some context-->ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning<!--some context-->ending", processed_page)

    def test_non_default_marker(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "ignore": {"markers": ["marker1"]} \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--marker1 some context--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning <!--some context--> ending", processed_page)

        page_text = "beginning <!--ignore some context--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning <!--ignore some context--> ending", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "ignore": {"markers": ["marker1", "marker2"]} \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--marker1 some context--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning <!--some context--> ending", processed_page)

        page_text = "beginning <!--marker2 some context--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning <!--some context--> ending", processed_page)

        page_text = "beginning <!--ignore some context--> ending"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning <!--ignore some context--> ending", processed_page)
