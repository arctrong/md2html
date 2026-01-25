import unittest
import subprocess
import tempfile
import os
from pathlib import Path

import helpers as h


class VerboseFlagE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.input_file = str(Path(h.INPUT_DIR).joinpath('verbose_test.txt'))

    def test_verbose_output_present(self):
        output_file = str(Path(self.OUTPUT_DIR).joinpath('verbose_test_present.html'))

        returncode, stdout, _ = h.run_with_capture([
            '-i', self.input_file,
            '-o', output_file,
            '-f',
            '-v'
        ])
        self.assertEqual(returncode, 0)
        self.assertIn('Output file generated:', stdout)

    def test_verbose_output_absent(self):
        output_file = str(Path(self.OUTPUT_DIR).joinpath('verbose_test_absent.html'))

        returncode, stdout, _ = h.run_with_capture([
            '-i', self.input_file,
            '-o', output_file,
            '-f'
        ])

        self.assertEqual(returncode, 0)
        self.assertEqual(stdout.strip(), '')


if __name__ == '__main__':
    unittest.main()
