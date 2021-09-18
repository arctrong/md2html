import unittest
import sys
from pathlib import Path
sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *
from .utils import *

# WORKING_DIR = Path(__file__).parent

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

    def test_read_lines_from_commented_file_simple(self):
        try:
            f = create_temp_file('line1\n#comment\nline2')
            self.assertEqual(read_lines_from_commented_file(f), 'line1\n\nline2')
        finally:
            os.remove(f)

    def test_read_lines_from_commented_file_comment_starts_not_from_beginning_of_line(self):
        try:
            f = create_temp_file('line1\n #comment\nline2')
            self.assertEqual(read_lines_from_commented_file(f), 'line1\n\nline2')
        finally:
            os.remove(f)

    def test_read_lines_from_commented_file_comment_in_first_line(self):
        try:
            f = create_temp_file('#comment\nline2')
            self.assertEqual(read_lines_from_commented_file(f), '\nline2')
        finally:
            os.remove(f)

    def test_read_lines_from_commented_file_comment_in_last_line(self):
        try:
            f = create_temp_file('line1\n#comment')
            self.assertEqual(read_lines_from_commented_file(f), 'line1\n')
        finally:
            os.remove(f)

    def test_read_lines_from_commented_file_two_comments_in_a_row(self):
        try:
            f = create_temp_file('line1\n#comment1\n#comment2\nline2')
            self.assertEqual(read_lines_from_commented_file(f), 'line1\n\n\nline2')
        finally:
            os.remove(f)

    def test_read_lines_from_commented_file_two_comments_in_a_distance(self):
        try:
            f = create_temp_file('line1\n#comment1\nline2\n#comment2\nline3')
            self.assertEqual(read_lines_from_commented_file(f), 'line1\n\nline2\n\nline3')
        finally:
            os.remove(f)

    def test_read_lines_from_commented_file_empty_comment(self):
        try:
            f = create_temp_file('line1\n#\nline2')
            self.assertEqual(read_lines_from_commented_file(f), 'line1\n\nline2')
        finally:
            os.remove(f)

    def test_first_not_none(self):
        self.assertEqual(first_not_none(None, 1, 2), 1)
        self.assertEqual(first_not_none(None, None, None), None)
        self.assertEqual(first_not_none(), None)
        self.assertEqual(first_not_none([], [1, 2]), [])
        d = {'a': 1}
        self.assertEqual(first_not_none(d.get('b'), d.get('a')), 1)



if __name__ == '__main__':
    unittest.main()
