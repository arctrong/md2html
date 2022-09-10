import unittest

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
        match_objects = [md for md in metadata_finder(page_content)]
        self.assertEqual(2, len(match_objects))
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            '    ', 'metadata', '{"key": "value"}', '<!--metadata{"key": "value"}-->', 35),
            match_objects[0])
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            ' other text ', 'variables', '{"question": "answer"} ',
            '<!--variables{"question": "answer"} -->', 86), match_objects[1])
            
    def test_metadata_finder_marginPositions(self):
        page_content = '<!--m1 d1--> t2 <!--m2 d2--> t3 <!--m3 d3-->'
        match_objects = [md for md in metadata_finder(page_content)]
        self.assertEqual(3, len(match_objects))
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            '', 'm1', ' d1', '<!--m1 d1-->', 12), match_objects[0])
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            ' t2 ', 'm2', ' d2', '<!--m2 d2-->', 28), match_objects[1])
        self._assertMetadataMatchObjectsEqual(MetadataMatchObject(
            ' t3 ', 'm3', ' d3', '<!--m3 d3-->', 44), match_objects[2])
