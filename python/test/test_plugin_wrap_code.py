import unittest

from md2html import *
from plugins.wrap_code_plugin import WrapCodePlugin
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _find_single_plugin(plugins) -> WrapCodePlugin:
    return find_single_instance_of_type(plugins, WrapCodePlugin)


class WrapCodePluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_notActivated_with_empty_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {"wrap-code": {} }}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"wrap-code": {'
            '    "marker1": {"input-root": "input/path/", "output-root": "output/path/"}'
            '}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        plugin.dry_run = True

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  path/to/file.csv --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before output/path/path/to/file.csv.html after", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"wrap-code": {'
            '    "marker1": {"input-root": "input/path1/", "output-root": "output/path1/"},'
            '    "marker2": {"input-root": "input/path2/", "output-root": "output/path2/"}'
            '}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        plugin.dry_run = True

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  path/to/file1.csv --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before output/path1/path/to/file1.csv.html after", processed_page)

        page_text = "before <!--marker2  path/to/file2.csv --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before output/path2/path/to/file2.csv.html after", processed_page)

    def test_repeated_source(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"wrap-code": {'
            '    "marker1": {"input-root": "input/path/", "output-root": "output/path/"}'
            '}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        plugin.dry_run = True

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "<!--marker1 path/to/file.csv--> <!--marker1 path/to/file.csv-->"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("output/path/path/to/file.csv.html output/path/path/to/file.csv.html", processed_page)
