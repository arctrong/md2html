import unittest
import sys
from pathlib import Path
sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


class UtilTest(unittest.TestCase):

    def test_strip_extension(self):
        self.assertEqual(strip_extension('file_with_one.extension'), 'file_with_one')
        self.assertEqual(strip_extension('file_with.two.extensions'), 'file_with.two')
        self.assertEqual(strip_extension('file_without_extension'), 'file_without_extension')
        self.assertEqual(strip_extension('.file_without_name'), '.file_without_name')
        self.assertEqual(strip_extension('a.file_with_short_name'), 'a')
        self.assertEqual(strip_extension('file/with/path.txt'), 'file/with/path')
        self.assertEqual(strip_extension('with\\path_without_ext'), 'with\\path_without_ext')
        self.assertEqual(strip_extension('with/dotted.path/name.ext'), 'with/dotted.path/name')
        self.assertEqual(strip_extension('with\\dotted.path/name'), 'with\\dotted.path/name')
        self.assertEqual(strip_extension('with/dotted.path/.name'), 'with/dotted.path/.name')
        self.assertEqual(strip_extension('with\\path/a.ext'), 'with\\path/a')

    def test_strip_extension_Path_input(self):
        self.assertEqual(strip_extension('file_with.two.extensions'), 'file_with.two')

    def test_strip_extension_special_cases(self):
        with self.assertRaises(TypeError):
            strip_extension(None)
        self.assertEqual(Path(strip_extension(Path(''))), Path(''))
        self.assertEqual(strip_extension(Path('file_with.two.extensions')), 'file_with.two')
        self.assertEqual(Path(strip_extension(Path('file/with/path.txt'))), Path('file/with/path'))
        self.assertEqual(Path(strip_extension(Path('file\\with\\path.txt'))), Path('file\\with\\path'))


if __name__ == '__main__':
    unittest.main()
