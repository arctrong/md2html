"""E2E: footnote-style delivery via recursive replace + back-references.

Regression for the writing-project bug where nested ref/refdef markers inside
recursive replace were merged in phase 1 before definitions existed on later pages.
"""

import json
import unittest
from pathlib import Path

import helpers as h

INPUT_DIR = Path(h.INPUT_DIR).joinpath('BackReferencesRecursiveReplaceTest')


def _document_inputs(input_dir, arg_file_name):
    arg_file = input_dir.joinpath(arg_file_name)
    with open(arg_file, encoding='utf-8') as file:
        return [entry['input'] for entry in json.load(file)['documents']]


class BackReferencesRecursiveReplaceRefBeforeDefE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.html = h.HtmlOutputCache(cls.OUTPUT_DIR)
        h.run_with_parameters([
            '--input-root', str(INPUT_DIR),
            '--output-root', cls.OUTPUT_DIR,
            '--argument-file', str(INPUT_DIR.joinpath('md2html_args_ref_before_def.json')),
        ])

    def test_ref_before_def_via_recursive_replace(self):
        self.assertEqual(
            ['ref.txt', 'def.txt'],
            _document_inputs(INPUT_DIR, 'md2html_args_ref_before_def.json'),
        )

        ref_page = self.html.read('ref.html')
        self.assertEqual('Referencing page', ref_page.body.h1.text)

        ref_paragraph = ref_page.body.p
        ref_sup = ref_paragraph.sup
        self.assertEqual('See Shared display', ref_sup.previous_sibling.strip())
        self.assertIn('note text', ref_paragraph.get_text())

        ref_anchor = ref_sup.a['name']
        forward_link = ref_sup.select('a.ref')[0]
        self.assertEqual('[foo]', forward_link.text)

        def_page = self.html.read('def.html')
        self.assertEqual('Definitions', def_page.body.h1.text)

        def_paragraph = def_page.body.p
        def_anchor = def_paragraph.a['name']
        self.assertEqual(f'def.html#{def_anchor}', forward_link['href'])

        def_span = def_paragraph.span
        self.assertEqual('[foo]', def_span.text)
        self.assertEqual(['ref-def'], def_span['class'])
        self.assertIn('Shared display', def_paragraph.get_text())

        back_link = def_paragraph.sup.a
        self.assertEqual(['ref'], back_link['class'])
        self.assertEqual(f'ref.html#{ref_anchor}', back_link['href'])
        self.assertEqual('1', back_link.text)


class BackReferencesRecursiveReplaceMultiNoteE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.html = h.HtmlOutputCache(cls.OUTPUT_DIR)
        h.run_with_parameters([
            '--input-root', str(INPUT_DIR),
            '--output-root', cls.OUTPUT_DIR,
            '--argument-file', str(INPUT_DIR.joinpath('md2html_args_multi_note.json')),
        ])

    def test_multiple_notes_one_definition_via_recursive_replace(self):
        self.assertEqual(
            ['ref_first.txt', 'ref_second.txt', 'def.txt'],
            _document_inputs(INPUT_DIR, 'md2html_args_multi_note.json'),
        )

        def_page = self.html.read('def.html')
        def_paragraph = def_page.body.p
        def_anchor = def_paragraph.a['name']
        back_links = def_paragraph.sup.find_all('a', class_='ref')
        self.assertEqual(2, len(back_links))

        ref_first_page = self.html.read('ref_first.html')
        ref_first_anchor = ref_first_page.body.p.sup.a['name']
        ref_second_page = self.html.read('ref_second.html')
        ref_second_anchor = ref_second_page.body.p.sup.a['name']

        hrefs = {link['href'] for link in back_links}
        texts = {link.text for link in back_links}
        self.assertIn(f'ref_first.html#{ref_first_anchor}', hrefs)
        self.assertIn(f'ref_second.html#{ref_second_anchor}', hrefs)
        self.assertEqual({'1', '2'}, texts)

        for page_name in ('ref_first.html', 'ref_second.html'):
            ref_page = self.html.read(page_name)
            ref_paragraph = ref_page.body.p
            self.assertIn('Shared display', ref_paragraph.get_text())
            forward_link = ref_paragraph.sup.select('a.ref')[0]
            self.assertEqual('[foo]', forward_link.text)
            self.assertEqual(f'def.html#{def_anchor}', forward_link['href'])


if __name__ == '__main__':
    unittest.main()
