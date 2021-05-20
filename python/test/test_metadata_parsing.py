import unittest
import sys
from pathlib import Path
import json
sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


class PageMetadataParsingTest(unittest.TestCase):

    def test_parse_metadata_notObject(self):
        metadata, errors = parse_metadata('[]')
        self.assertIsNone(metadata)
        self.assertTrue(errors)
        self.assertTrue(isinstance(errors, list))

    def test_parse_metadata_emptyString(self):
        metadata, errors = parse_metadata('')
        self.assertIsNone(metadata)
        self.assertTrue(errors)
        self.assertTrue(isinstance(errors, list))

    def test_parse_metadata_noneInput(self):
        metadata, errors = parse_metadata(None)
        self.assertIsNone(metadata)
        self.assertTrue(errors)
        self.assertTrue(isinstance(errors, list))

    def test_parse_metadata_malformedJson(self):
        metadata, errors = parse_metadata('not a Json')
        self.assertIsNone(metadata)
        self.assertTrue(errors)
        self.assertTrue(isinstance(errors, list))

    def test_parse_metadata_emptyObject(self):
        metadata, errors = parse_metadata('{}')
        self.assertTrue(isinstance(metadata, dict))
        self.assertFalse('title' in metadata)
        self.assertFalse('custom_template_placeholders' in metadata)
        self.assertFalse(errors)
        self.assertTrue(isinstance(errors, list))

    def test_parse_metadata_unexpectedItems(self):
        metadata, errors = parse_metadata('{"unexpectedItem": "correct value"}')
        self.assertTrue(isinstance(metadata, dict))
        self.assertNotIn('unexpectedItem', metadata)
        self.assertFalse(errors)

    def test_parse_metadata_correctTitle(self):
        metadata, errors = parse_metadata('{"title": "My title"}')
        self.assertEqual('My title', metadata['title'])
        self.assertFalse(errors)

    def test_parse_metadata_emptyTitle(self):
        metadata, errors = parse_metadata('{"title": ""}')
        self.assertEqual('', metadata['title'])
        self.assertFalse(errors)

    def test_parse_metadata_unicodeEntitiesInStrings(self):
        metadata, errors = parse_metadata('{\"title\":\"<!\\u002D-value-\\u002D>\"}')
        self.assertEqual('<!--value-->', metadata['title'])
        self.assertFalse(errors)

    def test_parse_metadata_incorrectTitleValue(self):
        metadata, errors = parse_metadata('{"title": 150}')
        self.assertNotIn('title', metadata)
        self.assertTrue(errors)

    def test_parse_metadata_incorrectTitleKey(self):
        metadata, errors = parse_metadata('{"Title": "correct title value"}')
        self.assertNotIn('Title', metadata)
        self.assertNotIn('title', metadata)
        self.assertFalse(errors)

    def test_parse_metadata_customTemplatePlaceholders_empty(self):
        metadata, errors = parse_metadata('{ "custom_template_placeholders": {} }')
        self.assertIn('custom_template_placeholders', metadata)
        self.assertFalse(errors)

    def test_parse_metadata_customTemplatePlaceholders_wrong(self):
        metadata, errors = parse_metadata('{ "custom_template_placeholders": "not dict" }')
        self.assertNotIn('custom_template_placeholders', metadata)
        self.assertTrue(errors)

    def test_parse_metadata_customTemplatePlaceholders_correctItems(self):
        metadata, errors = parse_metadata('{ "custom_template_placeholders": {"ph1": "val1", "ph2": "val2"} }')
        self.assertIn('custom_template_placeholders', metadata)
        cph = metadata['custom_template_placeholders']
        self.assertEqual('val1', cph['ph1'])
        self.assertEqual('val2', cph['ph2'])
        self.assertFalse(errors)

    def test_parse_metadata_customTemplatePlaceholders_incorrectItems(self):
        metadata, errors = parse_metadata('{ "custom_template_placeholders": {"ph1": 101, "ph2": "val2"} }')
        self.assertIn('custom_template_placeholders', metadata)
        cph = metadata['custom_template_placeholders']
        self.assertNotIn('ph1', cph)
        self.assertEqual('val2', cph['ph2'])
        self.assertTrue(errors)


if __name__ == '__main__':
    unittest.main()
