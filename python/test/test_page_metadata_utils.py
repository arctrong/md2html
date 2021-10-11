import sys
import unittest
from pathlib import Path

sys.path.append(Path(__file__).resolve().parent.parent)
from page_metadata_utils import *


class PageMetadataUtilsTest(unittest.TestCase):

    def _assertMetadataMatchObjectsEqual(self, mo1, mo2):
        self.assertEqual(mo1.before, mo2.before)
        self.assertEqual(mo1.metadata, mo2.metadata)
        self.assertEqual(mo1.metadata_block, mo2.metadata_block)
        self.assertEqual(mo1.end_position, mo2.end_position)
        
    def test_metadata_finder_trivial(self):
        page_content = '    <!--metadata{"key": "value"}--> other ' + \
            'text <!--variables{"question": "answer"} --> some more text'
        matchObjects = [md for md in metadata_finder(page_content)]
        self.assertEqual(2, len(matchObjects))
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject('    ', 'metadata',
            '{"key": "value"}', '<!--metadata{"key": "value"}-->', 35), matchObjects[0])
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(' other text ', 'variables', 
            '{"question": "answer"} ', '<!--variables{"question": "answer"} -->', 86),
                                              matchObjects[1])
            
    def test_metadata_finder_marginPositions(self):
        page_content = '<!--m1 d1--> t2 <!--m2 d2--> t3 <!--m3 d3-->'
        matchObjects = [md for md in metadata_finder(page_content)]
        self.assertEqual(3, len(matchObjects))
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            '', 'm1', ' d1', '<!--m1 d1-->', 12), matchObjects[0])
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            ' t2 ', 'm2', ' d2', '<!--m2 d2-->', 28), matchObjects[1])
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            ' t3 ', 'm3', ' d3', '<!--m3 d3-->', 44), matchObjects[2])


if __name__ == '__main__':
    unittest.main()
