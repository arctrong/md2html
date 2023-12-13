import unittest

from page_metadata_utils import *


def _extract_metadata_section_legacy(page_content):
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


class MetadataExtractionLegacyTest(unittest.TestCase):
    """
    Reusing legacy page metadata extraction tests.
    """

    def _assertMetadataMatchObjectsEqual(self, mo1, mo2):
        self.assertEqual(mo1.before, mo2.before)
        self.assertEqual(mo1.metadata, mo2.metadata)
        self.assertEqual(mo1.metadata_block, mo2.metadata_block)
        self.assertEqual(mo1.end_position, mo2.end_position)

    def test_emptyInput(self):
        self.assertEqual(None, _extract_metadata_section_legacy('')[0])

    def test_noMetadata(self):
        self.assertEqual(None, _extract_metadata_section_legacy('no metadata')[0])
        self.assertEqual(None, _extract_metadata_section_legacy('<!---->')[0])
        self.assertEqual(None, _extract_metadata_section_legacy('<!-- -->')[0])
        self.assertEqual(None, _extract_metadata_section_legacy('not opened -->')[0])
        self.assertEqual(None, _extract_metadata_section_legacy('<!-- not closed')[0])
        self.assertEqual(None, _extract_metadata_section_legacy('<!-- not metadata -->')[0])
        self.assertEqual(None, _extract_metadata_section_legacy('<!--notMetadata -->')[0])
        self.assertEqual(None, _extract_metadata_section_legacy(
            '<!-- metadata with leading space -->')[0])

    def test_emptyMetadata(self):
        self.assertEqual('', _extract_metadata_section_legacy('<!--metadata-->')[0])

    def test_start(self):
        self.assertEqual(0, _extract_metadata_section_legacy("<!--metadata-->")[1])
        self.assertEqual(1, _extract_metadata_section_legacy(" <!--metadata-->")[1])
        self.assertEqual(1, _extract_metadata_section_legacy("\n<!--metadata-->")[1])
        self.assertEqual(1, _extract_metadata_section_legacy("\r<!--metadata-->")[1])
        self.assertEqual(2, _extract_metadata_section_legacy("\r\n<!--metadata-->")[1])
        self.assertEqual(5, _extract_metadata_section_legacy("\n \t \n<!--metadata-->")[1])

    def test_end(self):
        self.assertEqual(15, _extract_metadata_section_legacy("<!--metadata-->")[2])
        self.assertEqual(16, _extract_metadata_section_legacy(" <!--metadata-->whatever")[2])
        self.assertEqual(16, _extract_metadata_section_legacy(" <!--metadata-->\n")[2])
        self.assertEqual(16, _extract_metadata_section_legacy(" <!--metadata-->\nwhatever")[2])

    def test_prepended(self):
        self.assertEqual(' ', _extract_metadata_section_legacy(' \t \n <!--metadata -->')[0])
        self.assertEqual(None,
                         _extract_metadata_section_legacy(' \t a \n <!--METADATA -->')[0])

    def test_postpended(self):
        self.assertEqual('', _extract_metadata_section_legacy('<!--metadata-->whatever')[0])

    def test_caseInsensitive(self):
        self.assertEqual(' ', _extract_metadata_section_legacy('<!--metadata -->')[0])
        self.assertEqual(' ', _extract_metadata_section_legacy('<!--meTAdaTA -->')[0])

    def test_multiline(self):
        self.assertEqual(' L1\nL2 ',
                         _extract_metadata_section_legacy('<!--metadata L1\nL2 -->')[0])

    def test_withoutSpaces(self):
        self.assertEqual('{METADATA}',
                         _extract_metadata_section_legacy('<!--metadata{METADATA}-->')[0])

    def test_with_literalClose(self):
        # This is an illegal case but in JSON, strings `-->` may be represented as like `-\u002D>`;
        # that will solve the problem.
        self.assertEqual(' \"',
                         _extract_metadata_section_legacy('<!--metadata \"-->\" -->')[0])

    def test_severalBlocks(self):
        self.assertEqual('{payload}', _extract_metadata_section_legacy(
            '<!--metadata{payload}-->line1\nline2<!-- something else -->')[0])
