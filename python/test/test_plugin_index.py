import unittest

from md2html import *
from plugins.index_plugin import IndexPlugin
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, IndexPlugin)


class IndexPluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_notActivated_with_empty_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {"index": {} }}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"index": {"index": {"output": "index_page.html", "index-cache": "index_cache.json"}}'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--index entry 1--> after"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertNotEqual(processed_page, page_text)

        page_text = "before <!--index1 entry 1--> after"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual(processed_page, page_text)

        page_text = 'before <!--index ["entry 1", "entry 2"] --> after'
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertNotEqual(processed_page, page_text)

    def test_several_indexes(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"index": {"index1": {"output": "index_page1.html", "index-cache": "cache1.json"}, '
            '          "index2": {"output": "index_page2.html", "index-cache": "cache2.json"}'
            '         }'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--index1 entry 1--> after"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertNotEqual(processed_page, page_text)

        page_text = "before <!--index2 entry 1--> after"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertNotEqual(processed_page, page_text)

        page_text = "before <!--index5 entry 1--> after"
        plugin.new_page(doc)
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual(processed_page, page_text)

# TODO Also test page flows with index.
