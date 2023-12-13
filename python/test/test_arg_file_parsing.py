import unittest

from md2html import *
from test.utils_for_tests import parse_argument_file_for_test
from utils import UserError


class ArgFileParseTest(unittest.TestCase):

    def test_emptyFile_NegativeScenario(self):
        with self.assertRaises(UserError):
            load_json_argument_file('')

    def test_rootElementIsNotObject_NegativeScenario(self):
        with self.assertRaises(UserError):
            load_json_argument_file('[1, 2]')

    def test_defaultElementIsNotObject_NegativeScenario(self):
        with self.assertRaises(UserError):
            load_json_argument_file('{"default": []}')

    def test_noDefaultElement_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": []}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertEqual(0, len(args.documents))

    def test_fullDefaultElement_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"default": {"input-root": "doc_src", "output-root": "doc", '
            '"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "include-css": ["include.css"], '
            '"force": true, "verbose": true}, "documents": [{}]}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        doc = args.documents[0]
        self.assertEqual('doc_src/index.txt', doc.input_file)
        self.assertEqual('doc/index.html', doc.output_file)
        self.assertEqual('some title', doc.title)
        self.assertEqual('path/templates/custom.html', doc.template)
        self.assertFalse(doc.no_css)
        self.assertListEqual(["link1.css", "link2.css"], doc.link_css)
        self.assertListEqual(["include.css"], doc.include_css)
        self.assertTrue(doc.force)
        self.assertTrue(doc.verbose)

    def test_noDocumentsElement_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            load_json_argument_file('{"default": {}}')
        self.assertTrue('documents' in str(cm.exception))

    def test_documentsElementIsNotList_NegativeScenario(self):
        with self.assertRaises(UserError):
            load_json_argument_file('{"documents": "not a list"}')

    def test_emptyDocumentsElement_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": []}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertEqual([], args.documents)

    def test_defaultElementNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "include-css"]:
            with self.subTest(css_type=css_type):
                with self.assertRaises(UserError) as cm:
                    argument_file_dict = load_json_argument_file(
                        '{"default": {"no-css": true, "' + css_type +
                        '": ["some.css"]}, "documents": []}')
                    parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
                self.assertTrue('no-css' in str(cm.exception))
                self.assertTrue(css_type in str(cm.exception))

    def test_minimalDocument_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        doc = args.documents[0]
        self.assertEqual('index.txt', doc.input_file)
        self.assertTrue('index.html' in doc.output_file)

    def test_severalDocuments_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}, {"input": "about.txt"}], '
            '"default": {"template": "common_template.html"}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        doc = args.documents[0]
        self.assertEqual('index.txt', doc.input_file)
        self.assertEqual('common_template.html', doc.template)
        self.assertTrue('index.html' in doc.output_file)
        doc = args.documents[1]
        self.assertEqual('about.txt', doc.input_file)
        self.assertEqual('common_template.html', doc.template)
        self.assertTrue('about.html' in doc.output_file)

    def test_fullDocument_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-root": "doc_src", "output-root": "doc", '
            '"input": "index.txt", "output": "index.html", '
            '"title": "some title", "code": "some_code", '
            '"template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], '
            '"add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": true, "verbose": true}]}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        doc = args.documents[0]
        self.assertEqual('doc_src/index.txt', doc.input_file)
        self.assertEqual('doc/index.html', doc.output_file)
        self.assertEqual('some title', doc.title)
        self.assertEqual('some_code', doc.code)
        self.assertEqual('path/templates/custom.html', doc.template)
        self.assertFalse(doc.no_css)
        self.assertListEqual(["link1.css", "link2.css", "add_link.css"], doc.link_css)
        self.assertListEqual(["include.css", "add_include1.css", "add_include1.css"],
                             doc.include_css)
        self.assertTrue(doc.force)
        self.assertTrue(doc.verbose)

    def test_documentsElementNoInputFile_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file(
                '{"documents": [{"output": "index.html"}]}')
            parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertTrue('index.html' in str(cm.exception))
        self.assertTrue('input' in str(cm.exception).lower())

    def test_documentNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "add-link-css", "include-css", "add-include-css"]:
            with self.subTest(css_type=css_type):
                with self.assertRaises(UserError) as cm:
                    argument_file_dict = load_json_argument_file(
                        '{"documents": [{"no-css": true, "' + css_type + '": ["some.css"]}]}')
                    parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
                self.assertTrue('no-css' in str(cm.exception))
                self.assertTrue(css_type in str(cm.exception))

    def test_documentVerboseAndReportFlags_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file(
                '{"documents": [{"output": "index.html", "verbose": true, "report": true}]}')
            parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertTrue('verbose' in str(cm.exception))
        self.assertTrue('report' in str(cm.exception))

    def test_overridingWithCliArgs_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-root": "doc_src", "output-root": "doc", '
            '"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], '
            '"add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": false, "verbose": false}]}')
        cli_args = parse_cli_arguments([
            "--input-root", "cli_doc_src", "--output-root", "cli_doc",
            "-i", "cli_index.txt", "-o", "cli_index.html", "--title", "cli_title",
            "--template", "cli/custom.html",
            "--link-css", "cli_link1.css", "--link-css", "cli_link2.css",
            "--include-css", "cli_include1.css", "--include-css", "cli_include2.css",
            "--force", "--verbose"])
        args = parse_argument_file_for_test(argument_file_dict, cli_args)
        doc = args.documents[0]
        self.assertEqual('cli_doc_src/cli_index.txt', doc.input_file)
        self.assertEqual('cli_doc/cli_index.html', doc.output_file)
        self.assertEqual('cli_title', doc.title)
        self.assertEqual(Path('cli/custom.html'), doc.template)
        self.assertFalse(doc.no_css)
        self.assertListEqual(["cli_link1.css", "cli_link2.css"], doc.link_css)
        self.assertListEqual(["cli_include1.css", "cli_include2.css"], doc.include_css)
        self.assertTrue(doc.force)
        self.assertTrue(doc.verbose)

    def test_nonUniqueDocumentCode_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file(
                '{"documents": [ \n'
                '    {"input": "page1.txt", "code": "page1"}, \n'
                '    {"input": "page2.txt", "code": "page1"}] \n'
                '}')
            parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertTrue('duplicate' in str(cm.exception).lower())
        self.assertTrue('page1' in str(cm.exception))

    def test_caseSensitiveCodes_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [ \n'
            '    {"input": "page1.txt", "code": "page1"}, \n'
            '    {"input": "page2.txt", "code": "paGe1"}] \n'
            '}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertEqual("page1", args.documents[0].code)
        self.assertEqual("paGe1", args.documents[1].code)

    def test_defaultOptions_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        options = args.options
        self.assertFalse(options.verbose)
        self.assertFalse(options.legacy_mode)

    def test_fullOptions_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"options": {"verbose": true, "legacy-mode": true}, '
            '"documents": [{"input": "index.txt"}]}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        options = args.options
        self.assertTrue(options.verbose)
        self.assertTrue(options.legacy_mode)

    def test_noPlugins_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertEqual(0, len(args.plugins))

    def test_allPlugins_PositiveScenario(self):
        # Adding minimum plugin data to make the plugins declare themselves activated.
        # The specific plugins behavior is going to be tested in separate tests.
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt", "code": "index"}], '
            '"plugins": {'
            '"relative-paths": {"rel_path": "/doc"}, '
            '"page-flows": {"sections": [{"link": "doc/about.html", "title": "About"}]}, '
            '"page-variables":{"v": {}}, '
            '"variables": {"logo": "THE GREATEST SITE EVER!"}, '
            '"index": {"index": {"output": "o.html", "index-cache": "ic.json"}}, '
            '"page-links": {} '
            '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertEqual(6, len(args.plugins))

    def test_auto_output_file_with_root_dirs_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '{"input-root": "doc_src/txt", "output-root": "doc/html", "input": "index.txt"}, '
            '{"output-root": "doc/html", "input": "index.txt"}, '
            '{"input-root": "doc_src/txt", "input": "index.txt"}'
            ']}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        doc = args.documents[0]
        self.assertEqual('doc_src/txt/index.txt', doc.input_file)
        self.assertTrue('doc/html/index.html' in doc.output_file)
        doc = args.documents[1]
        self.assertEqual('index.txt', doc.input_file)
        self.assertTrue('doc/html/index.html' in doc.output_file)
        doc = args.documents[2]
        self.assertEqual('doc_src/txt/index.txt', doc.input_file)
        self.assertTrue('index.html' in doc.output_file)
