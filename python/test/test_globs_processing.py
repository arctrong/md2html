import sys
import unittest
from pathlib import Path

from test.utils_for_tests import relative_to_current_dir

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


# This is required to make it work when running from different directories
THIS_DIR = relative_to_current_dir(Path(__file__).parent)


class GlobsProcessingTest(unittest.TestCase):

    def test_minimal_scenario(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-glob": "' + THIS_DIR + 'for_globs_processing_test/*.txt"}]}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        input_files_to_check = [d.input_file[len(THIS_DIR):] for d in args.documents]
        # Here the order is undefined.
        self.assertCountEqual(['for_globs_processing_test/file02.txt',
                               'for_globs_processing_test/file01.txt'],
                              input_files_to_check)

    def test_recursive(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-glob": "' + THIS_DIR + 'for_globs_processing_test/**/*.txt"}]}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        input_files_to_check = [d.input_file[len(THIS_DIR):] for d in args.documents]
        # Here the order is undefined.
        self.assertCountEqual(['for_globs_processing_test/recursive/recursive_file01.txt',
                               'for_globs_processing_test/file02.txt',
                               'for_globs_processing_test/file01.txt'],
                              input_files_to_check)

    def test_sort_by_file_path(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-glob": "' + THIS_DIR +
            'for_globs_processing_test/**/*.txt", '
            '"sort-by-file-path": true}]}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        input_files_to_check = [d.input_file[len(THIS_DIR):] for d in args.documents]
        self.assertListEqual(['for_globs_processing_test/file01.txt',
                              'for_globs_processing_test/file02.txt',
                              'for_globs_processing_test/recursive/recursive_file01.txt'],
                             input_files_to_check)

    def test_sort_by_title(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-glob": "' + THIS_DIR + 'for_globs_processing_test/**/*.txt", \n'
            '    "title-from-variable": "title", \n'
            '    "sort-by-title": true}], \n'
            '"plugins": {"page-variables": {}} \n'
            '}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        input_files_to_check = [d.input_file[len(THIS_DIR):] for d in args.documents]
        self.assertListEqual(['for_globs_processing_test/file02.txt',
                              'for_globs_processing_test/recursive/recursive_file01.txt',
                              'for_globs_processing_test/file01.txt'],
                             input_files_to_check)

    def test_sort_by_variable(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-glob": "' + THIS_DIR + 'for_globs_processing_test/**/*.txt", \n'
            '    "sort-by-variable": "SORTORDER"}], \n'
            '"plugins": {"page-variables": {}} \n'
            '}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        input_files_to_check = [d.input_file[len(THIS_DIR):] for d in args.documents]
        self.assertListEqual(['for_globs_processing_test/file02.txt',
                              'for_globs_processing_test/file01.txt',
                              'for_globs_processing_test/recursive/recursive_file01.txt'],
                             input_files_to_check)

    def test_with_root_paths(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input-root": "' + THIS_DIR + 'for_globs_processing_test", '
                                                           '"output-root": "dst_root", \n'
            '    "input-glob": "**/*.txt" \n'
            '    }], \n'
            '"plugins": {"page-variables": {}} \n'
            '}')
        args = parse_argument_file(argument_file_dict, CliArgDataObject())
        input_files_to_check = [d.input_file[len(THIS_DIR):] for d in args.documents]
        # Here the order is undefined.
        self.assertCountEqual(['for_globs_processing_test/file02.txt',
                               'for_globs_processing_test/file01.txt',
                               'for_globs_processing_test/recursive/recursive_file01.txt'],
                              input_files_to_check)
        output_files_to_check = [d.output_file for d in args.documents]
        # Here the order is undefined.
        self.assertCountEqual(['dst_root/file02.html',
                               'dst_root/file01.html',
                               'dst_root/recursive/recursive_file01.html'],
                              output_files_to_check)


if __name__ == '__main__':
    unittest.main()
