import sys
import unittest
from pathlib import Path

sys.path.append(Path(__file__).resolve().parent.parent)
from utils import *


class UtilTest(unittest.TestCase):

    def test_strip_extension(self):
        self.assertEqual('file_with_one', strip_extension('file_with_one.extension'))
        self.assertEqual('file_with.two', strip_extension('file_with.two.extensions'))
        self.assertEqual('file_without_extension', strip_extension('file_without_extension'))
        self.assertEqual('.file_without_name', strip_extension('.file_without_name'))
        self.assertEqual('a', strip_extension('a.file_with_short_name'))
        self.assertEqual('file/with/path', strip_extension('file/with/path.txt'))
        self.assertEqual('with\\path_without_ext', strip_extension('with\\path_without_ext'))
        self.assertEqual('with/dotted.path/name', strip_extension('with/dotted.path/name.ext'))
        self.assertEqual('with\\dotted.path/name', strip_extension('with\\dotted.path/name'))
        self.assertEqual('with/dotted.path/.name', strip_extension('with/dotted.path/.name'))
        self.assertEqual('with\\path/a', strip_extension('with\\path/a.ext'))

    def test_strip_extension_Path_input(self):
        self.assertEqual('file_with.two', strip_extension('file_with.two.extensions'))

    def test_strip_extension_special_cases(self):
        with self.assertRaises(TypeError):
            strip_extension(None)
        self.assertEqual(Path(''), Path(strip_extension(Path(''))))
        self.assertEqual('file_with.two', strip_extension(Path('file_with.two.extensions')))
        self.assertEqual(Path('file/with/path'), Path(strip_extension(Path('file/with/path.txt'))))
        self.assertEqual(Path('file\\with\\path'), Path(strip_extension(Path('file\\with\\path.txt'))))

    def test_blank_comment_line(self):
        self.assertEqual('        ', blank_comment_line('#comment', '#'))
        self.assertEqual('        \n', blank_comment_line('#comment\n', '#'))
        self.assertEqual('        \r\n', blank_comment_line('#comment\r\n', '#'))
        self.assertEqual('         ', blank_comment_line(' #comment', '#'))
        self.assertEqual('not comment', blank_comment_line('not comment', '#'))
        self.assertEqual('not # comment', blank_comment_line('not # comment', '#'))
        self.assertEqual(' ', blank_comment_line('#', '#'))
        self.assertEqual(' \n', blank_comment_line('#\n', '#'))

    def test_first_not_none(self):
        self.assertEqual(1, first_not_none(None, 1, 2))
        self.assertEqual(None, first_not_none(None, None, None))
        self.assertEqual(None, first_not_none())
        self.assertEqual([], first_not_none([], [1, 2]))
        d = {'a': 1}
        self.assertEqual(1, first_not_none(d.get('b'), d.get('a')))

    def test_relativize_relative_resource_path(self):
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc/', '')
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc/', 'path/')
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc/', None)
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc', 'index.html')
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('/', 'index.html')

        self.assertEqual('../', relativize_relative_resource_path('../', 'index.html'))
        self.assertEqual('', relativize_relative_resource_path('', 'index.html'))
        self.assertEqual('doc/', relativize_relative_resource_path('doc/', 'index.html'))
        self.assertEqual('doc/pict/', relativize_relative_resource_path('doc/pict/', 'index.html'))
        
        self.assertEqual('../../', relativize_relative_resource_path('../', 'doc/index.html'))
        self.assertEqual('../', relativize_relative_resource_path('', 'doc/index.html'))
        self.assertEqual('', relativize_relative_resource_path('doc/', 'doc/index.html'))
        self.assertEqual('pict/', relativize_relative_resource_path('doc/pict/', 'doc/index.html'))

        self.assertEqual('../pict/', relativize_relative_resource_path('pict/', 'doc/index.html'))
        self.assertEqual('../pict/doc/', relativize_relative_resource_path('pict/doc/', 'doc/index.html'))
        self.assertEqual('../../pict/', relativize_relative_resource_path('pict/', 'doc/chapter01/index.html'))
        self.assertEqual('../../pict/doc/', relativize_relative_resource_path('pict/doc/', 'doc/chapter01/index.html'))
        
        self.assertEqual('', relativize_relative_resource_path('./', 'index.html'))
        self.assertEqual('../', relativize_relative_resource_path('./', 'doc/index.html'))

    def test_relativize_relative_resource(self):
        with self.assertRaises(ValueError):
            relativize_relative_resource('styles.css', '')
        with self.assertRaises(ValueError):
            relativize_relative_resource('styles.css', 'doc/')
        with self.assertRaises(ValueError):
            relativize_relative_resource('styles.css', None)
        with self.assertRaises(ValueError):
            relativize_relative_resource('', 'index.html')
        with self.assertRaises(ValueError):
            relativize_relative_resource('doc/', 'index.html')
        with self.assertRaises(ValueError):
            relativize_relative_resource(None, 'index.html')

        self.assertEqual('styles.css', relativize_relative_resource('styles.css', 'index.html'))
        self.assertEqual('doc/styles.css', relativize_relative_resource('doc/styles.css', 'index.html'))
        self.assertEqual('doc/pict/logo.png', relativize_relative_resource('doc/pict/logo.png', 'index.html'))
        
        self.assertEqual('../../logo.png', relativize_relative_resource('../logo.png', 'doc/index.html'))
        self.assertEqual('../logo.png', relativize_relative_resource('logo.png', 'doc/index.html'))
        self.assertEqual('logo.png', relativize_relative_resource('doc/logo.png', 'doc/index.html'))
        self.assertEqual('pict/logo.png', relativize_relative_resource('doc/pict/logo.png', 'doc/index.html'))

        self.assertEqual('../pict/logo.png', relativize_relative_resource('pict/logo.png', 'doc/index.html'))
        self.assertEqual('../pict/doc/logo.png', relativize_relative_resource('pict/doc/logo.png', 'doc/index.html'))
        self.assertEqual('../../pict/logo.png', relativize_relative_resource('pict/logo.png', 'doc/chapter01/index.html'))
        self.assertEqual('../../pict/doc/logo.png', relativize_relative_resource('pict/doc/logo.png', 'doc/chapter01/index.html'))
        
        self.assertEqual('logo.png', relativize_relative_resource('./logo.png', 'index.html'))
        self.assertEqual('../logo.png', relativize_relative_resource('./logo.png', 'doc/index.html'))


if __name__ == '__main__':
    unittest.main()
