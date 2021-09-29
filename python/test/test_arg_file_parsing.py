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
        parse_argument_file_content(argument_file_dict, {})

    def test_fullDefaultElement_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"default": {"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "include-css": ["include.css"], '
            '"force": true, "verbose": true}, "documents": [{}]}')
        arguments = parse_argument_file_content(argument_file_dict, {})
        doc = arguments.documents[0]
        self.assertEqual('index.txt', doc["input_file"])
        self.assertEqual('index.html', doc["output_file"])
        self.assertEqual('some title', doc["title"])
        self.assertEqual(Path('path/templates/custom.html'), doc["template"])
        self.assertFalse(doc["no_css"])
        self.assertListEqual(["link1.css", "link2.css"], doc["link_css"])
        self.assertListEqual(["include.css"], doc["include_css"])
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
        arguments = parse_argument_file_content(argument_file_dict, {})
        self.assertEqual([], arguments.documents)

    def test_defaultElementNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "include-css"]:
            with self.subTest(css_type=css_type):
                with self.assertRaises(UserError) as cm:
                    argument_file_dict = load_json_argument_file('{"default": {"no-css": true, "' +
                        css_type + '": ["some.css"]}, "documents": []}')
                    parse_argument_file_content(argument_file_dict, {})
                self.assertTrue('no-css' in str(cm.exception))
                self.assertTrue(css_type in str(cm.exception))

    def test_minimalDocument_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        arguments = parse_argument_file_content(argument_file_dict, {})
        doc = arguments.documents[0]
        self.assertEqual('index.txt', doc["input_file"])
        self.assertTrue('index.html' in doc["output_file"])

    def test_severalDocuments_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}, {"input": "about.txt"}], '
            '"default": {"template": "common_template.html"}}')
        arguments = parse_argument_file_content(argument_file_dict, {})
        doc = arguments.documents[0]
        self.assertEqual('index.txt', doc["input_file"])
        self.assertEqual(Path('common_template.html'), doc["template"])
        self.assertTrue('index.html' in doc["output_file"])
        doc = arguments.documents[1]
        self.assertEqual('about.txt', doc["input_file"])
        self.assertEqual(Path('common_template.html'), doc["template"])
        self.assertTrue('about.html' in doc["output_file"])

    def test_fullDocument_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
        '{"documents": [{"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], "add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": true, "verbose": true}]}')
        arguments = parse_argument_file_content(argument_file_dict, {})
        doc = arguments.documents[0]
        self.assertEqual('index.txt', doc["input_file"])
        self.assertEqual('index.html', doc["output_file"])
        self.assertEqual('some title', doc["title"])
        self.assertEqual(Path('path/templates/custom.html'), doc["template"])
        self.assertFalse(doc["no_css"])
        self.assertEqual('some title', doc["title"])
        self.assertListEqual(["link1.css", "link2.css", "add_link.css"], doc["link_css"])
        self.assertListEqual(["include.css", "add_include1.css", "add_include1.css"], doc["include_css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_documentsElementNoInputFile_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file('{"documents": [{"output": "index.html"}]}')
            parse_argument_file_content(argument_file_dict, {})
        self.assertTrue('index.html' in str(cm.exception))
        self.assertTrue('input' in str(cm.exception).lower())

    def test_documentNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "add-link-css", "include-css", "add-include-css"]:
            with self.subTest(css_type=css_type):
                with self.assertRaises(UserError) as cm:
                    argument_file_dict = load_json_argument_file('{"documents": [{"no-css": true, "' + 
                        css_type + '": ["some.css"]}]}')
                    arguments = parse_argument_file_content(argument_file_dict, {})
                self.assertTrue('no-css' in str(cm.exception))
                self.assertTrue(css_type in str(cm.exception))

    def test_documentVerboseAndReportFlags_NegativeScenario(self):
        with self.assertRaises(UserError) as cm:
            argument_file_dict = load_json_argument_file('{"documents": [{"output": "index.html", '
                '"verbose": true, "report": true}]}')
            arguments = parse_argument_file_content(argument_file_dict, {})
        self.assertTrue('verbose' in str(cm.exception))
        self.assertTrue('report' in str(cm.exception))

    def test_overridingWithCliArgs_PositiveScenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], "add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": false, "verbose": false}]}')
        arguments = parse_argument_file_content(argument_file_dict, 
            {"input_file": "cli_index.txt", "output_file": "cli_index.html", "title": "cli_title", 
             "template": "cli/custom.html", 
             "link_css": ["cli_link1.css", "cli_link2.css"],
             "include_css": ["cli_include1.css", "cli_include2.css"], 
             "force": True, "verbose": True
            })
        doc = arguments.documents[0]
        self.assertEqual('cli_index.txt', doc["input_file"])
        self.assertEqual('cli_index.html', doc["output_file"])
        self.assertEqual('cli_title', doc["title"])
        self.assertEqual(Path('cli/custom.html'), doc["template"])
        self.assertFalse(doc["no_css"])
        self.assertListEqual(["cli_link1.css", "cli_link2.css"], doc["link_css"])
        self.assertListEqual(["cli_include1.css", "cli_include2.css"], doc["include_css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_defaultOptions_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        options = parse_argument_file_content(argument_file_dict, {}).options
        self.assertFalse(options["verbose"])
        self.assertFalse(options["legacy_mode"])

    def test_fullOptions_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"options": {"verbose": true, "legacy-mode": true}, '
            '"documents": [{"input": "index.txt"}]}')
        options = parse_argument_file_content(argument_file_dict, {}).options
        self.assertTrue(options["verbose"])
        self.assertTrue(options["legacy_mode"])

    def test_noPlugins_PositiveScenario(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}]}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        self.assertEqual([], plugins)

    def test_allPlugins_PositiveScenario(self):
        # Adding minimum plugin data to make the plugins declare themselves activated.
        # The specific plugins behavior is going to be tested in separate tests.
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}], '
            '"plugins": {"relative-paths": {"rel_path": "/doc"}, '
            '"page-flows": {"sections": [{"link": "doc/about.html", "title": "About"}]}, '
            '"page-variables":{}, '
            '"variables": {"logo": "THE GREATEST SITE EVER!"}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        self.assertEqual(4, len(plugins))


if __name__ == '__main__':
    unittest.main()
