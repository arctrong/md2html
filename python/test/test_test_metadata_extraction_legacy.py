import unittest
import sys
from pathlib import Path

sys.path.append(Path(__file__).resolve().parent.parent)
from page_metadata_utils import *


class MetadataExtractionLegacyTest(unittest.TestCase):
    """
    Reusing legacy page metadata extraction tests.
    """

    def _assertMetadataMatchObjectsEqual(self, mo1, mo2):
        self.assertEqual(mo1.before, mo2.before)
        self.assertEqual(mo1.metadata, mo2.metadata)
        self.assertEqual(mo1.metadata_block, mo2.metadata_block)
        self.assertEqual(mo1.end_position, mo2.end_position)

    def _extract_metadata_section_legacy(self, page_content):
        """
        Simulates the legacy method of page metadata blocks extraction. Allows legacy tests to be
        reused. Extracts only the blocks that are the first non-blank text on the page and have
        marker "METADATA" case-insensitively.

        Returns a triple of:
        - metadata text;
        - metadata block start position;
        - metadata block end position.
        """
        for matchObject in metadata_finder(page_content):
            if matchObject.before.strip() == '' and \
                    matchObject.metadata_block[4:12].upper() == 'METADATA':
                return (matchObject.metadata, len(matchObject.before),
                        len(matchObject.before) + len(matchObject.metadata_block))
        return None, None, None

    def test_extract_metadata_section_legacy_emptyInput(self):
        self.assertEqual(None, self._extract_metadata_section_legacy('')[0])

    def test_extract_metadata_section_legacy_noMetadata(self):
        self.assertEqual(None, self._extract_metadata_section_legacy('no metadata')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy('<!---->')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy('<!-- -->')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy('not opened -->')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy('<!-- not closed')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy('<!-- not metadata -->')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy('<!--notMetadata -->')[0])
        self.assertEqual(None, self._extract_metadata_section_legacy(
            '<!-- metadata with leading space -->')[0])

    def test_extract_metadata_section_legacy_emptyMetadata(self):
        self.assertEqual('', self._extract_metadata_section_legacy('<!--metadata-->')[0])

    def test_extract_metadata_section_legacy_start(self):
        self.assertEqual(0, self._extract_metadata_section_legacy("<!--metadata-->")[1])
        self.assertEqual(1, self._extract_metadata_section_legacy(" <!--metadata-->")[1])
        self.assertEqual(1, self._extract_metadata_section_legacy("\n<!--metadata-->")[1])
        self.assertEqual(1, self._extract_metadata_section_legacy("\r<!--metadata-->")[1])
        self.assertEqual(2, self._extract_metadata_section_legacy("\r\n<!--metadata-->")[1])
        self.assertEqual(5, self._extract_metadata_section_legacy("\n \t \n<!--metadata-->")[1])

    def test_extract_metadata_section_legacy_end(self):
        self.assertEqual(15, self._extract_metadata_section_legacy("<!--metadata-->")[2])
        self.assertEqual(16, self._extract_metadata_section_legacy(" <!--metadata-->whatever")[2])
        self.assertEqual(16, self._extract_metadata_section_legacy(" <!--metadata-->\n")[2])
        self.assertEqual(16, self._extract_metadata_section_legacy(" <!--metadata-->\nwhatever")[2])

    def test_extract_metadata_section_legacy_prepended(self):
        self.assertEqual(' ', self._extract_metadata_section_legacy(' \t \n <!--metadata -->')[0])
        self.assertEqual(None,
                         self._extract_metadata_section_legacy(' \t a \n <!--METADATA -->')[0])

    def test_extract_metadata_section_legacy_postpended(self):
        self.assertEqual('', self._extract_metadata_section_legacy('<!--metadata-->whatever')[0])

    def test_extract_metadata_section_legacy_caseInsensitive(self):
        self.assertEqual(' ', self._extract_metadata_section_legacy('<!--metadata -->')[0])
        self.assertEqual(' ', self._extract_metadata_section_legacy('<!--meTAdaTA -->')[0])

    def test_extract_metadata_section_legacy_multiline(self):
        self.assertEqual(' L1\nL2 ',
                         self._extract_metadata_section_legacy('<!--metadata L1\nL2 -->')[0])

    def test_extract_metadata_section_legacy_withoutSpaces(self):
        self.assertEqual('{METADATA}',
                         self._extract_metadata_section_legacy('<!--metadata{METADATA}-->')[0])

    def test_extract_metadata_section_legacy_with_literalClose(self):
        # This is an illegal case but in JSON, strings `-->` may be represented as like `-\u002D>`;
        # that will solve the problem.
        self.assertEqual(' \"',
                         self._extract_metadata_section_legacy('<!--metadata \"-->\" -->')[0])


if __name__ == '__main__':
    unittest.main()
