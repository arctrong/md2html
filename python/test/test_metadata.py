import unittest
import sys
from pathlib import Path
import json
sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


class PageMetadataTest(unittest.TestCase):

    def test_extract_metadata_section_empty_input(self):
        self.assertFalse(extract_metadata_section(''))

    def test_extract_metadata_section_no_metadata(self):
        self.assertFalse(extract_metadata_section('no metadata'))
        self.assertFalse(extract_metadata_section('<!---->'))
        self.assertFalse(extract_metadata_section('<!-- -->'))
        self.assertFalse(extract_metadata_section('not opened -->'))
        self.assertFalse(extract_metadata_section('<!-- not closed'))
        self.assertFalse(extract_metadata_section('<!-- not metadata -->'))
        self.assertFalse(extract_metadata_section('<!--notMetadata -->'))
        self.assertFalse(extract_metadata_section('<!-- metadata with space -->'))

    def test_extract_metadata_section_empty_metadata(self):
        self.assertEqual('', extract_metadata_section('<!--metadata-->'))

    def test_extract_metadata_section_prepended(self):
        self.assertTrue(extract_metadata_section(' \t \n <!--metadata -->'))
        self.assertFalse(extract_metadata_section(' \t a \n <!--METADATA -->'))

    def test_extract_metadata_section_postpended(self):
        self.assertEqual('', extract_metadata_section('<!--metadata-->no_matter_what'))

    def test_extract_metadata_section_case_insensitive(self):
        self.assertEqual(' ', extract_metadata_section('<!--metadata -->'))
        self.assertEqual(' ', extract_metadata_section('<!--meTAdaTA -->'))

    def test_extract_metadata_section_multiline(self):
        self.assertEqual(' line1\nline2 ', extract_metadata_section('<!--metadata line1\nline2 -->'))

    def test_extract_metadata_section_without_spaces(self):
        self.assertEqual('METADATA', extract_metadata_section('<!--metadataMETADATA-->'))

    def test_extract_metadata_section_with_literal_close(self):
        # In JSON strings `-->` may be represented as `-\u002D>`.
        self.assertEqual(' \"', extract_metadata_section('<!--metadata \"-->\" -->'))

    def test_json_parser_unicode_entities_in_strings(self):
        # This is an external library but we need to be sure about this certain case.
        json_object = json.loads('{\"key\":\"<!\\u002D-value-\\u002D>\"}')
        value = json_object.get('key')
        self.assertEqual(value, "<!--value-->")


if __name__ == '__main__':
    unittest.main()
