import sys
import unittest
from pathlib import Path

from argument_file_utils import enrich_document

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *

class ArgFileParseTest(unittest.TestCase):

    def test_emptyFile_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content('', {})
        self.assertFalse(success)
    
    def test_rootElementIsNotObject_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content('[1, 2]', {})
        self.assertFalse(success)

    def test_defaultElementIsNotObject_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content('{"default": []}', {})
        self.assertFalse(success)

    def test_noDefaultElement_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content('{"documents": []}', {})
        self.assertTrue(success)

    def test_fullDefaultElement_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"default": {"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "include-css": ["include.css"], '
            '"force": true, "verbose": true}, "documents": [{}]}', {})
        self.assertTrue(success)
        doc = options["document-list"][0]
        self.assertEqual(Path('index.txt'), doc["input_file"])
        self.assertEqual(Path('index.html'), doc["output_file"])
        self.assertEqual('some title', doc["title"])
        self.assertEqual(Path('path/templates/custom.html'), doc["template"])
        self.assertFalse(doc["no_css"])
        self.assertListEqual(["link1.css", "link2.css"], doc["link_css"])
        self.assertListEqual(["include.css"], doc["include_css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_noDocumentsElement_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content('{"default": {}}', {})
        self.assertTrue('documents' in error_message)
        self.assertFalse(success)

    def test_documentsElementIsNotList_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content('{"documents": "not a list"}', {})
        self.assertFalse(success)
        
    def test_emptyDocumentsElement_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content('{"documents": []}', {})
        self.assertTrue(success)
        self.assertEqual([], options["document-list"])

    def test_defaultElementNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "include-css"]:
            with self.subTest(css_type=css_type):
                success, error_message, options = parse_argument_file_content(
                    '{"default": {"no-css": true, "' + css_type + '": ["some.css"]}, "documents": []}', {})
                self.assertFalse(success)
                self.assertTrue('no-css' in error_message)
                self.assertTrue(css_type in error_message)

    def test_minimalDocument_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"documents": [{"input": "index.txt"}]}', {})
        self.assertTrue(success)
        doc = options["document-list"][0]
        self.assertEqual(Path('index.txt'), doc["input_file"])
        enrich_document(options["document-list"])
        self.assertTrue(isinstance(doc["output_file"], Path))
        self.assertTrue('index' in str(doc["output_file"]))

    def test_severalDocuments_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"documents": [{"input": "index.txt"}, {"input": "about.txt"}], '
            '"default": {"template": "common_template.html"}}', {})
        self.assertTrue(success)
        doc = options["document-list"][0]
        self.assertEqual(Path('index.txt'), doc["input_file"])
        self.assertEqual(Path('common_template.html'), doc["template"])
        enrich_document(options["document-list"])
        self.assertTrue(isinstance(doc["output_file"], Path))
        self.assertTrue('index' in str(doc["output_file"]))
        doc = options["document-list"][1]
        self.assertEqual(Path('about.txt'), doc["input_file"])
        self.assertEqual(Path('common_template.html'), doc["template"])
        enrich_document(options["document-list"])
        self.assertTrue(isinstance(doc["output_file"], Path))
        self.assertTrue('about' in str(doc["output_file"]))

    def test_fullDocument_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"documents": [{"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], "add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": true, "verbose": true}]}', {})
        self.assertTrue(success)
        doc = options["document-list"][0]
        self.assertEqual(Path('index.txt'), doc["input_file"])
        self.assertEqual(Path('index.html'), doc["output_file"])
        self.assertEqual('some title', doc["title"])
        self.assertEqual(Path('path/templates/custom.html'), doc["template"])
        self.assertFalse(doc["no_css"])
        self.assertEqual('some title', doc["title"])
        self.assertListEqual(["link1.css", "link2.css", "add_link.css"], doc["link_css"])
        self.assertListEqual(["include.css", "add_include1.css", "add_include1.css"], doc["include_css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])

    def test_documentsElementNoInputFile_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"documents": [{"output": "index.html"}]}', {})
        self.assertFalse(success)
        self.assertTrue('INPUT' in error_message.upper())

    def test_documentNoCssWithCssDefinitions_NegativeScenario(self):
        for css_type in ["link-css", "add-link-css", "include-css", "add-include-css"]:
            with self.subTest(css_type=css_type):
                success, error_message, options = parse_argument_file_content(
                    '{"documents": [{"no-css": true, "' + css_type + '": ["some.css"]}]}', {})
                self.assertFalse(success)
                self.assertTrue('no-css' in error_message)
                self.assertTrue(css_type in error_message)

    def test_documentVerboseAndReportFlags_NegativeScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"documents": [{"output": "index.html", "verbose": true, "report": true}]}', {})
        self.assertFalse(success)
        self.assertTrue('verbose' in error_message)
        self.assertTrue('report' in error_message)

    def test_overridingWithCliArgs_PositiveScenario(self):
        success, error_message, options = parse_argument_file_content(
            '{"documents": [{"input": "index.txt", "output": "index.html", '
            '"title": "some title", "template": "path/templates/custom.html", '
            '"link-css": ["link1.css", "link2.css"], "add-link-css": ["add_link.css"], '
            '"include-css": ["include.css"], "add-include-css": ["add_include1.css", "add_include1.css"], '
            '"force": false, "verbose": false}]}', 
            {"input_file": "cli_index.txt", "output_file": "cli_index.html", "title": "cli_title", 
             "template": "cli/custom.html", 
             "link_css": ["cli_link1.css", "cli_link2.css"],
             "include_css": ["cli_include1.css", "cli_include2.css"], 
             "force": True, "verbose": True
            })
        self.assertTrue(success)
        doc = options["document-list"][0]
        self.assertEqual(Path('cli_index.txt'), doc["input_file"])
        self.assertEqual(Path('cli_index.html'), doc["output_file"])
        self.assertEqual('cli_title', doc["title"])
        self.assertEqual(Path('cli/custom.html'), doc["template"])
        self.assertFalse(doc["no_css"])
        self.assertListEqual(["cli_link1.css", "cli_link2.css"], doc["link_css"])
        self.assertListEqual(["cli_include1.css", "cli_include2.css"], doc["include_css"])
        self.assertTrue(doc["force"])
        self.assertTrue(doc["verbose"])


if __name__ == '__main__':
    unittest.main()
