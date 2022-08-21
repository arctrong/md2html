import sys
import unittest
from pathlib import Path

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


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
        parse_argument_file(argument_file_dict, CliArgDataObject())

    def test_fullDefaultElement_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"default": {"input-root": "doc_src", "output-root": "doc", '
            '"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "include-css": ["include.css"], '
            '"force": true, "verbose": true}, "documents": [{}]}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        doc = arguments.documents[0]
        self.assertEqual('doc_src/index.txt', doc["input"])
        self.assertEqual('doc/index.html', doc["output"])
        self.assertEqual('some title', doc["title"])
        self.assertEqual('path/templates/custom.html', doc["template"])
        self.assertFalse(doc["no-css"])
        self.assertListEqual(["link1.css", "link2.css"], doc["link-css"])
        self.assertListEqual(["include.css"], doc["include-css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_noDocumentsElement_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            load_json_argument_file('{"default": {}}')
        self.assertTrue('documents' in str(cm.exception))

    def test_documentsElementIsNotList_NegativeScenario(self):
        with self.assertRaises(UserError):
            load_json_argument_file('{"documents": "not a list"}')
        
    def test_emptyDocumentsElement_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": []}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertEqual([], arguments.documents)

    def test_defaultElementNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "include-css"]:
            with self.subTest(css_type=css_type):
                with self.assertRaises(UserError) as cm:
                    argument_file_dict = load_json_argument_file(
                        '{"default": {"no-css": true, "' + css_type +
                        '": ["some.css"]}, "documents": []}')
                    _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
                self.assertTrue('no-css' in str(cm.exception))
                self.assertTrue(css_type in str(cm.exception))

    def test_minimalDocument_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        doc = arguments.documents[0]
        self.assertEqual('index.txt', doc["input"])
        self.assertTrue('index.html' in doc["output"])

    def test_severalDocuments_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}, {"input": "about.txt"}], '
            '"default": {"template": "common_template.html"}}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        doc = arguments.documents[0]
        self.assertEqual('index.txt', doc["input"])
        self.assertEqual('common_template.html', doc["template"])
        self.assertTrue('index.html' in doc["output"])
        doc = arguments.documents[1]
        self.assertEqual('about.txt', doc["input"])
        self.assertEqual('common_template.html', doc["template"])
        self.assertTrue('about.html' in doc["output"])

    def test_fullDocument_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-root": "doc_src", "output-root": "doc", '
            '"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], '
            '"add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": true, "verbose": true}]}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        doc = arguments.documents[0]
        self.assertEqual('doc_src/index.txt', doc["input"])
        self.assertEqual('doc/index.html', doc["output"])
        self.assertEqual('some title', doc["title"])
        self.assertEqual('path/templates/custom.html', doc["template"])
        self.assertFalse(doc["no-css"])
        self.assertEqual('some title', doc["title"])
        self.assertListEqual(["link1.css", "link2.css", "add_link.css"], doc["link-css"])
        self.assertListEqual(["include.css", "add_include1.css", "add_include1.css"],
                             doc["include-css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_documentsElementNoInputFile_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file(
                '{"documents": [{"output": "index.html"}]}')
            parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertTrue('index.html' in str(cm.exception))
        self.assertTrue('input' in str(cm.exception).lower())

    def test_documentNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "add-link-css", "include-css", "add-include-css"]:
            with self.subTest(css_type=css_type):
                with self.assertRaises(UserError) as cm:
                    argument_file_dict = load_json_argument_file(
                        '{"documents": [{"no-css": true, "' + css_type + '": ["some.css"]}]}')
                    parse_argument_file(argument_file_dict, CliArgDataObject())
                self.assertTrue('no-css' in str(cm.exception))
                self.assertTrue(css_type in str(cm.exception))

    def test_documentVerboseAndReportFlags_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file(
                '{"documents": [{"output": "index.html", "verbose": true, "report": true}]}')
            parse_argument_file(argument_file_dict, CliArgDataObject())
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
        arguments, _ = parse_argument_file(argument_file_dict, cli_args)
        doc = arguments.documents[0]
        self.assertEqual('cli_doc_src/cli_index.txt', doc["input"])
        self.assertEqual('cli_doc/cli_index.html', doc["output"])
        self.assertEqual('cli_title', doc["title"])
        self.assertEqual(Path('cli/custom.html'), doc["template"])
        self.assertFalse(doc["no-css"])
        self.assertListEqual(["cli_link1.css", "cli_link2.css"], doc["link-css"])
        self.assertListEqual(["cli_include1.css", "cli_include2.css"], doc["include-css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_defaultOptions_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        options = arguments.options
        self.assertFalse(options["verbose"])
        self.assertFalse(options["legacy-mode"])

    def test_fullOptions_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"options": {"verbose": true, "legacy-mode": true}, '
            '"documents": [{"input": "index.txt"}]}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        options = arguments.options
        self.assertTrue(options["verbose"])
        self.assertTrue(options["legacy-mode"])

    def test_noPlugins_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        plugins = process_plugins(argument_file_dict['plugins'])
        self.assertEqual({}, plugins)

    def test_allPlugins_PositiveScenario(self):
        # Adding minimum plugin data to make the plugins declare themselves activated.
        # The specific plugins behavior is going to be tested in separate tests.
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], '
            '"plugins": {"relative-paths": {"rel_path": "/doc"}, '
            '"page-flows": {"sections": [{"link": "doc/about.html", "title": "About"}]}, '
            '"page-variables":{"v": {}}, '
            '"variables": {"logo": "THE GREATEST SITE EVER!"}}}')
        _, plugins = parse_argument_file(argument_file_dict, CliArgDataObject())
        self.assertEqual(4, len(plugins))
        
    def test_auto_output_file_with_root_dirs_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '{"input-root": "doc_src/txt", "output-root": "doc/html", "input": "index.txt"}, '
            '{"output-root": "doc/html", "input": "index.txt"}, '
            '{"input-root": "doc_src/txt", "input": "index.txt"}'
            ']}')
        arguments, _ = parse_argument_file(argument_file_dict, CliArgDataObject())
        doc = arguments.documents[0]
        self.assertEqual('doc_src/txt/index.txt', doc["input"])
        self.assertTrue('doc/html/index.html' in doc["output"])
        doc = arguments.documents[1]
        self.assertEqual('index.txt', doc["input"])
        self.assertTrue('doc/html/index.html' in doc["output"])
        doc = arguments.documents[2]
        self.assertEqual('doc_src/txt/index.txt', doc["input"])
        self.assertTrue('index.html' in doc["output"])


if __name__ == '__main__':
    unittest.main()
