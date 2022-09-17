import unittest

from md2html import *
from plugins.page_links_plugin import PageLinksPlugin
from .utils_for_tests import find_single_instance_of_type


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, PageLinksPlugin)


class PageLinksPluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_notActivated_with_no_page_code(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], '
            '"plugins": {"page-links": {} }}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt", "code": "page1"} \n'
            '], \n'
            '"plugins": { \n'
            '    "page-links": {} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "[](<!--page page1-->#anchor)"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("[](page1.html#anchor)", processed_page)

    def test_different_paths(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt", "code": "page1"}, \n'
            '    {"input": "subdir/page2.txt", "code": "page2"} \n'
            '], \n'
            '"plugins": { \n'
            '    "page-links": {} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc1 = args.documents[0]
        doc2 = args.documents[1]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "[](<!--page page2-->#anchor)"
        plugin.new_page(doc1)
        plugin.new_page(doc2)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc1)
        self.assertEqual("[](subdir/page2.html#anchor)", processed_page)

        page_text = "[](<!--page page1-->#anchor)"
        plugin.new_page(doc1)
        plugin.new_page(doc2)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc2)
        self.assertEqual("[](../page1.html#anchor)", processed_page)

    def test_no_page_code_must_ignore(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt", "code": "page1"}, \n'
            '    {"input": "page2.txt"} \n'
            '], \n'
            '"plugins": { \n'
            '    "page-links": {} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc1 = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "[](<!--page page2-->#anchor)"
        plugin.new_page(doc1)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc1)
        self.assertEqual("[](<!--page page2-->#anchor)", processed_page)

    def test_non_default_marker(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt", "code": "page1"}, \n'
            '    {"input": "page2.txt", "code": "page2"}], \n'
            '"plugins": { \n'
            '    "page-links": {"markers": ["marker1"]} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "[](<!--marker1 page2-->#anchor)"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("[](page2.html#anchor)", processed_page)

        page_text = "[](<!--page page2-->#anchor)"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("[](<!--page page2-->#anchor)", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt", "code": "page1"}, \n'
            '    {"input": "page2.txt", "code": "page2"}], \n'
            '"plugins": { \n'
            '    "page-links": {"markers": ["marker1", "marker2"]} \n'
            '}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "[](<!--marker1 page2-->#anchor)"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("[](page2.html#anchor)", processed_page)

        page_text = "[](<!--marker2 page2-->#anchor)"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("[](page2.html#anchor)", processed_page)

        page_text = "[](<!--page page2-->#anchor)"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("[](<!--page page2-->#anchor)", processed_page)

