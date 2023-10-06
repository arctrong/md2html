import unittest

from md2html import *
from plugins.include_file_plugin import IncludeFilePlugin
from .utils_for_tests import find_single_instance_of_type, relative_to_current_dir

THIS_DIR = relative_to_current_dir(Path(__file__).parent)


def _find_single_plugin(plugins) -> IncludeFilePlugin:
    return find_single_instance_of_type(plugins, IncludeFilePlugin)


class IncludeFilePluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], "plugins": {}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_with_empty_plugin_def_must_raise_error(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], "plugins": {"include-file": [] }}')
        with self.assertRaises(UserError) as cm:
            parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertTrue('IncludeFilePlugin' in str(cm.exception))
        self.assertTrue('ValidationError' in str(cm.exception))

    def test_minimal(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["marker1"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/"}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  code1.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before Sample text 1 after", processed_page)

    def test_with_untrimmed(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["marker1"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "trim": "none"'
            '    }'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  code1.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before \nSample text 1\n\n after", processed_page)

    def test_with_trimmed_empty_lines(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["marker1"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "trim": "empty-lines"'
            '    }'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before<!--marker1  trim_empty_lines.txt -->after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before   Sample text 1   after", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["marker1", "marker2"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/"}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  code1.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before Sample text 1 after", processed_page)

        page_text = "before <!--marker2  code1.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before Sample text 1 after", processed_page)

    def test_several_root_dirs(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["marker1"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/"'
            '    },'
            '    {"markers": ["marker2"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/folder1/"'
            '    }'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  code1.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before Sample text 1 after", processed_page)

        page_text = "before <!--marker2  code2.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before Sample text 2 after", processed_page)

    def test_with_duplicate_markers_must_raise_error(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["marker1"], "root-dir": "whatever/path1" },'
            '    {"markers": ["marker2", "Marker1"], "root-dir": "whatever/path2" }'
            ']}}')
        with self.assertRaises(UserError) as cm:
            parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertTrue('duplication' in str(cm.exception))
        self.assertTrue('MARKER1' in str(cm.exception))

    def test_recursive(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"replace": [{"markers": ["replace"], "replace-with": "[[${1}]]"}],'
            '"include-file": ['
            '    {"markers": ["marker1"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "recursive": true}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1 recursive.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before text 1, [[text 2]] after", processed_page)

    def test_recursive_cycle_detection(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"replace": [{"markers": ["replace"], "replace-with": '
            '    "[[${1}<!--marker1 recursive.txt -->]]", "recursive": true}],'
            '"include-file": ['
            '    {"markers": ["marker1"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "recursive": true}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  recursive.txt --> after"
        with self.assertRaises(UserError) as cm:
            apply_metadata_handlers(page_text, metadata_handlers, doc)
        message = str(cm.exception).upper()
        self.assertIn("CYCLE", message)
        self.assertIn("INCLUDE_FILE_PLUGIN", message)
        self.assertIn("RECURSIVE.TXT", message)

    def test_recursive_cycle_detection_with_different_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"replace": [{"markers": ["replace"], "replace-with": '
            '    "[[${1}<!--marker2 recursive.txt -->]]", "recursive": true}],'
            '"include-file": ['
            '    {"markers": ["marker1", "marker2"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "recursive": true}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--marker1  recursive.txt --> after"
        with self.assertRaises(UserError) as cm:
            apply_metadata_handlers(page_text, metadata_handlers, doc)
        message = str(cm.exception).upper()
        self.assertIn("CYCLE", message)
        self.assertIn("INCLUDE_FILE_PLUGIN", message)
        self.assertIn("RECURSIVE.TXT", message)

    def test_recursive_no_cycles_with_different_files(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"replace": [{"markers": ["replace"], "replace-with": "[[${1}]]"}],'
            '"include-file": ['
            '    {"markers": ["include"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "recursive": true}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--include recursive1.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before text 3, text 1, [[text 2]] after", processed_page)

    def test_substring(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["include_text"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "start-with": "<body>", "end-with": "</body>"},'
            '    {"markers": ["include_marker"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "start-marker": "<body>", "end-marker": "</body>"}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "before <!--include_text substrings.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before <body>BODY</body> after", processed_page)

        page_text = "before <!--include_marker substrings.txt --> after"
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before BODY after", processed_page)

    def test_substring_with_per_file_delimiters(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "whatever.txt"}], '
            '"plugins": {'
            '"include-file": ['
            '    {"markers": ["include"], '
            '     "root-dir": "' + THIS_DIR + 'for_include_file_plugin_test/",'
            '     "start-marker": "// START HERE", "end-marker": "// END HERE"}'
            ']}}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = ('before <!--include {"file": "per_file_delimiters.txt", "start-marker": "", '
                     '"start-with": "<body>", "end-with": "</body>"}--> after')
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before <body>BODY</body> after", processed_page)

        page_text = ('before <!--include {"file": "per_file_delimiters.txt", '
                     '"start-marker": "<body>", "end-marker": "</body>"}--> after')
        processed_page = apply_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("before BODY after", processed_page)
