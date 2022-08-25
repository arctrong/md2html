import sys
import unittest
from pathlib import Path

from plugins.index_plugin import IndexPlugin
from .utils_for_tests import find_single_instance_of_type

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *
from plugins.page_flows_plugin import *


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, IndexPlugin)


class IndexPluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(plugins))

    def test_notActivated_with_empty_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {"index": {} }}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(plugins))

    def test_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"index": {"index": {"output": "index_page.html", "index-cache": "index_cache.json"}}'
            '}}')
        arguments, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(plugins.values())

        doc = arguments.documents[0]
        metadata_handlers = register_page_metadata_handlers(plugins)

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
        arguments, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(plugins.values())

        doc = arguments.documents[0]
        metadata_handlers = register_page_metadata_handlers(plugins)

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


if __name__ == '__main__':
    unittest.main()
