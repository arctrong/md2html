import unittest
from pathlib import Path

from bs4 import BeautifulSoup

import helpers as h


class DeferredBuildPageTitleE2eTest(unittest.TestCase):
    """E2E regression: with a two-phase plugin (back-references), pages that contain
    ``<!--ref ...-->`` may get the wrong HTML ``<title>`` — often the title of
    another page processed later in the same build — while navigation titles from
    the argument file stay correct.
    """

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('DeferredBuildPageTitleTest')
        h.run_with_parameters([
            '--input-root', str(cls.INPUT_DIR),
            '--output-root', cls.OUTPUT_DIR,
            '--argument-file', str(cls.INPUT_DIR.joinpath('md2html_args.json')),
        ])

    def _read_output(self, file_name):
        with open(Path(self.OUTPUT_DIR).joinpath(file_name), encoding='utf-8') as html_file:
            return BeautifulSoup(html_file, 'html.parser')

    def test_referencer_keeps_correct_title(self):
        page = self._read_output('referencer.html')
        self.assertEqual('Referencing page', page.head.title.text)

    def test_definition_keeps_correct_title(self):
        page = self._read_output('definition.html')
        self.assertEqual('Definition page', page.head.title.text)

    def test_plain_keeps_correct_title(self):
        page = self._read_output('plain.html')
        self.assertEqual('Plain page', page.head.title.text)


if __name__ == '__main__':
    unittest.main()
