import unittest

from md2html import *
from page_metadata_utils import apply_and_merge_metadata_handlers
from plugins.replace_plugin import ReplacePlugin
from .testsupport.simulate_metadata_build import simulate_metadata_build_from_arg_file
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, ReplacePlugin)


class ReplacePluginTest(unittest.TestCase):

    def test_notActivated_no_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_single_value(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [{"markers": ["marker1"], "replace-with": "[[${1}]]"}] \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--MARKER1  some context  --> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[some context  ]] ending", processed_page)

    def test_several_values(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [{"markers": ["marker1"], "replace-with": "[[${1}-${2}]]"}] \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = 'beginning <!--marker1 ["A", "B"]--> ending'
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[A-B]] ending", processed_page)

        page_text = 'beginning <!--marker1 ["C", "D"]--> ending'
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[C-D]] ending", processed_page)

    def test_several_markers(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [{"markers": ["marker1", "marker2"], "replace-with": "[[${1}]]"}] \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--marker1 some-value--> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning [[some-value]] ending", processed_page)

        page_text = "beginning <!--marker2 some-value--> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
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
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--marker1 VALUE--> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning s1 VALUE e1 ending", processed_page)

        page_text = "beginning <!--marker2 VALUE--> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
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
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--m2 V2--> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning V2 m2 v1 m1 ending", processed_page)

        page_text = "beginning <!--m3 V3--> ending"
        processed_page = apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        self.assertEqual("beginning V3 m3 <!--m1 v1--> ending", processed_page)

    def test_recursive_direct_cycle_must_fail(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [\n'
            '        {"markers": ["m1"], "replace-with": "${1} <!--m1 v1-->", "recursive": true}\n'
            '    ] \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--m1 V1--> ending"
        with self.assertRaises(UserError) as cm:
            apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        message = str(cm.exception).upper()
        self.assertIn("CYCLE", message)
        self.assertIn("M1", message)

    def test_recursive_indirect_cycle_must_fail(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page1.txt"}], \n'
            '"plugins": { \n'
            '    "replace": [\n'
            '        {"markers": ["m1"], "replace-with": "${1} <!--m2 v2-->", "recursive": true},\n'
            '        {"markers": ["m2"], "replace-with": "${1} <!--m3 v3-->", "recursive": true},\n'
            '        {"markers": ["m3"], "replace-with": "${1} <!--m1 v1-->", "recursive": true}\n'
            '    ] \n'
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())

        doc = args.documents[0]
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        page_text = "beginning <!--m1 V1--> ending"
        with self.assertRaises(UserError) as cm:
            apply_and_merge_metadata_handlers(page_text, metadata_handlers, doc)
        message = str(cm.exception).upper()
        self.assertIn("CYCLE", message)
        self.assertIn("M1,M2,M3", message)

    def test_recursive_replace_back_references_ref_before_def(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "ref.txt", "output": "ref.html"},'
            '  {"input": "def.txt", "output": "def.html"}'
            '], "plugins": {'
            '"replace": ['
            '  {"markers": ["note"],'
            '   "replace-with": "<!--ref ${1}-->note text",'
            '   "recursive": true},'
            '  {"markers": ["notedef"],'
            '   "replace-with": "<!--refdef ${1} ${2}-->",'
            '   "recursive": true}'
            '],'
            '"back-references": {}'
            '}}')
        build = simulate_metadata_build_from_arg_file(arg_file_str, [
            (0, 'See <!--note foo-->.'),
            (1, 'Entry <!--notedef foo Foo display-->'),
        ])
        self.assertTrue(build.deferred_pages[0])
        self.assertTrue(build.deferred_pages[1])
        self.assertEqual(
            build.output[0],
            'See Foo display<sup><a name="backref_ref_foo"></a>'
            '<a class="ref" href="def.html#backref_def_foo">[foo]</a></sup>note text.')
        self.assertEqual(
            build.output[1],
            'Entry <a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Foo display'
            '<sup><a class="ref" href="ref.html#backref_ref_foo">1</a></sup>')

    def test_recursive_replace_back_references_multiple_notes_one_def(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "ref1.txt", "output": "ref1.html"},'
            '  {"input": "ref2.txt", "output": "ref2.html"},'
            '  {"input": "def.txt", "output": "def.html"}'
            '], "plugins": {'
            '"replace": ['
            '  {"markers": ["note"],'
            '   "replace-with": "<!--ref ${1}-->",'
            '   "recursive": true},'
            '  {"markers": ["notedef"],'
            '   "replace-with": "<!--refdef ${1} ${2}-->",'
            '   "recursive": true}'
            '],'
            '"back-references": {}'
            '}}')
        build = simulate_metadata_build_from_arg_file(arg_file_str, [
            (0, 'First <!--note foo-->.'),
            (1, 'Second <!--note foo-->.'),
            (2, '<!--notedef foo Shared display-->'),
        ])
        self.assertTrue(all(build.deferred_pages.values()))
        self.assertEqual(
            build.output[0],
            'First Shared display<sup><a name="backref_ref_foo"></a>'
            '<a class="ref" href="def.html#backref_def_foo">[foo]</a></sup>.')
        self.assertEqual(
            build.output[1],
            'Second Shared display<sup><a name="backref_ref_foo"></a>'
            '<a class="ref" href="def.html#backref_def_foo">[foo]</a></sup>.')
        self.assertEqual(
            build.output[2],
            '<a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Shared display'
            '<sup><a class="ref" href="ref1.html#backref_ref_foo">1</a>, '
            '<a class="ref" href="ref2.html#backref_ref_foo">2</a></sup>')

    def test_recursive_replace_back_references_missing_def_should_fail(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {'
            '"replace": ['
            '  {"markers": ["note"],'
            '   "replace-with": "<!--ref ${1}-->",'
            '   "recursive": true}'
            '],'
            '"back-references": {}'
            '}}')
        with self.assertRaises(UserError) as cm:
            simulate_metadata_build_from_arg_file(arg_file_str, [
                (0, 'See <!--note missing-->.'),
            ])
        self.assertIn('referenced but not defined', str(cm.exception).lower())

