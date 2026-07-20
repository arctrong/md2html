import os
import unittest
from pathlib import Path

from bs4 import BeautifulSoup

import helpers as h


@unittest.skipUnless(
    os.getenv("IMPLEMENTATION") == "py",
    "Skipping class because IMPLEMENTATION is not 'py'. Java version is not implemented yet.",
)
class BackReferencesPluginContentMinimalE2eTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('BackReferencesPluginContentMinimalTest')
        h.run_with_parameters([
            '--input-root', str(cls.INPUT_DIR),
            '--output-root', cls.OUTPUT_DIR,
            '--argument-file', str(cls.INPUT_DIR.joinpath('md2html_args_minimal.json')),
        ])

    def _read_output(self, file_name):
        with open(Path(self.OUTPUT_DIR).joinpath(file_name), encoding='utf-8') as html_file:
            return BeautifulSoup(html_file, 'html.parser')

    def test_minimal_forward_and_back_reference(self):
        ref_page = self._read_output('minimal_ref.html')

        self.assertEqual("Referencing page", ref_page.body.h1.text)

        ref_paragraph = ref_page.body.p
        self.assertEqual("See", ref_paragraph.contents[0].strip())
        ref_external_link = ref_paragraph.a
        self.assertEqual("Example Domain", ref_external_link.text)

        ref_sup = ref_paragraph.sup
        self.assertEqual("backref_ref_example", ref_sup.a['name'])
        ref_backref_link = ref_sup.select("a:nth-of-type(2)")[0]
        self.assertEqual("[example]", ref_backref_link.text)
        self.assertEqual(["ref"], ref_backref_link['class'])
        self.assertEqual("minimal_def.html#backref_def_example", ref_backref_link['href'])
        
        def_page = self._read_output('minimal_def.html')
        self.assertEqual("Definition page", def_page.body.h1.text)

        def_paragraph = def_page.body.p
        self.assertEqual("backref_def_example", def_paragraph.a['name'])
        def_span = def_paragraph.span
        self.assertEqual("[example]", def_span.text)
        self.assertEqual(["ref-def"], def_span['class'])
        def_external_link = def_paragraph.select("a:nth-of-type(2)")[0]
        self.assertEqual("Example Domain", def_external_link.text)

        def_sup = def_paragraph.sup
        def_backref_link = def_sup.a        
        self.assertEqual(["ref"], def_backref_link['class'])
        self.assertEqual("minimal_ref.html#backref_ref_example", def_backref_link['href'])
        self.assertEqual("1", def_backref_link.text)


if __name__ == '__main__':
    unittest.main()
