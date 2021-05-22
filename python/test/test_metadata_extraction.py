import unittest
import sys
from pathlib import Path
import json
sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


class PageMetadataExtractionTest(unittest.TestCase):

    def test_extract_metadata_section_empty_input(self):
        self.assertEqual(None, extract_metadata_section('')[0])

    def test_extract_metadata_section_no_metadata(self):
        self.assertEqual(None, extract_metadata_section('no metadata')[0])
        self.assertEqual(None, extract_metadata_section('<!---->')[0])
        self.assertEqual(None, extract_metadata_section('<!-- -->')[0])
        self.assertEqual(None, extract_metadata_section('not opened -->')[0])
        self.assertEqual(None, extract_metadata_section('<!-- not closed')[0])
        self.assertEqual(None, extract_metadata_section('<!-- not metadata -->')[0])
        self.assertEqual(None, extract_metadata_section('<!--notMetadata -->')[0])
        self.assertEqual(None, extract_metadata_section('<!-- metadata with space -->')[0])

    def test_extract_metadata_section_empty_metadata(self):
        self.assertEqual('', extract_metadata_section('<!--metadata-->')[0])

    def test_extract_metadata_section_start(self):
        self.assertEqual(0, extract_metadata_section("<!--metadata-->")[1]);
        self.assertEqual(1, extract_metadata_section(" <!--metadata-->")[1]);
        self.assertEqual(1, extract_metadata_section("\n<!--metadata-->")[1]);
        self.assertEqual(1, extract_metadata_section("\r<!--metadata-->")[1]);
        self.assertEqual(2, extract_metadata_section("\r\n<!--metadata-->")[1]);
        self.assertEqual(5, extract_metadata_section("\n \t \n<!--metadata-->")[1]);

    def test_extract_metadata_section_end(self):
        self.assertEqual(15, extract_metadata_section("<!--metadata-->")[2]);
        self.assertEqual(16, extract_metadata_section(" <!--metadata-->whatever")[2]);
        self.assertEqual(16, extract_metadata_section(" <!--metadata-->\n")[2]);
        self.assertEqual(16, extract_metadata_section(" <!--metadata-->\nwhatever")[2]);

    def test_extract_metadata_section_prepended(self):
        self.assertEqual(' ', extract_metadata_section(' \t \n <!--metadata -->')[0])
        self.assertEqual(None, extract_metadata_section(' \t a \n <!--METADATA -->')[0])

    def test_extract_metadata_section_postpended(self):
        self.assertEqual('', extract_metadata_section('<!--metadata-->no_matter_what')[0])

    def test_extract_metadata_section_case_insensitive(self):
        self.assertEqual(' ', extract_metadata_section('<!--metadata -->')[0])
        self.assertEqual(' ', extract_metadata_section('<!--meTAdaTA -->')[0])

    def test_extract_metadata_section_multiline(self):
        self.assertEqual(' line1\nline2 ', extract_metadata_section('<!--metadata line1\nline2 -->')[0])

    def test_extract_metadata_section_without_spaces(self):
        self.assertEqual('METADATA', extract_metadata_section('<!--metadataMETADATA-->')[0])

    def test_extract_metadata_section_with_literal_close(self):
        # In JSON strings `-->` may be represented as `-\u002D>`.
        self.assertEqual(' \"', extract_metadata_section('<!--metadata \"-->\" -->')[0])


if __name__ == '__main__':
    unittest.main()
