import unittest
from pathlib import Path

import helpers as h


class BackReferencesPluginContentFormatsE2eTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('BackReferencesPluginContentFormatsTest')
        # This test class uses the strategy: generate all pages at once then test everything.
        # So a cache usage is acceptable. All test methods only read the output files.
        cls.html = h.HtmlOutputCache(cls.OUTPUT_DIR)
        h.run_with_parameters([
            '--input-root', str(cls.INPUT_DIR),
            '--output-root', cls.OUTPUT_DIR,
            '--argument-file', str(cls.INPUT_DIR.joinpath('md2html_args.json')),
        ])

    def test_two_ref_formats_same_source(self):
        def_page = self.html.read('formats_def_standard.html')
        def_paragraph = def_page.body.p
        def_anchor = def_paragraph.a['name']
        self.assertEqual('[format_dual]', def_paragraph.span.text)
        self.assertEqual(['ref-def'], def_paragraph.span['class'])
        display_link = def_paragraph.select('a:nth-of-type(2)')[0]
        self.assertEqual('Format Dual', display_link.text)

        ref_standard_page = self.html.read('formats_ref_standard.html')
        ref_standard_paragraph = ref_standard_page.body.p
        self.assertEqual('Format Dual', ref_standard_paragraph.a.text)
        ref_standard_anchor = ref_standard_paragraph.sup.a['name']
        ref_standard_link = ref_standard_paragraph.sup.select('a.ref')[0]
        self.assertEqual('[format_dual]', ref_standard_link.text)
        self.assertEqual(
            f'formats_def_standard.html#{def_anchor}', ref_standard_link['href'])

        ref_compact_page = self.html.read('formats_ref_compact.html')
        ref_compact_paragraph = ref_compact_page.body.p
        ref_compact_cite = ref_compact_paragraph.cite
        self.assertEqual(['compact-ref'], ref_compact_cite['class'])
        compact_anchors = ref_compact_cite.find_all('a')
        self.assertEqual(2, len(compact_anchors))
        ref_compact_anchor = compact_anchors[0]['name']
        ref_compact_link = compact_anchors[1]
        self.assertEqual('format_dual', ref_compact_link.text)
        self.assertNotIn('class', ref_compact_link.attrs)
        self.assertEqual(
            f'formats_def_standard.html#{def_anchor}', ref_compact_link['href'])

        back_link_hrefs = [
            link['href']
            for link in def_paragraph.sup.find_all('a', class_='ref')]
        self.assertIn(
            f'formats_ref_standard.html#{ref_standard_anchor}', back_link_hrefs)
        self.assertIn(
            f'formats_ref_compact.html#{ref_compact_anchor}', back_link_hrefs)

    def test_two_def_formats_different_sources(self):
        standard_page = self.html.read('formats_def_standard.html')
        self.assertIsNone(standard_page.body.find('div', class_='bib-def'))

        bib_page = self.html.read('formats_def_bib.html')
        bib_def = bib_page.body.find('div', class_='bib-def')
        self.assertIsNotNone(bib_def)
        bib_anchor = bib_def['id']
        self.assertIsNone(bib_def.find('span', class_='ref-def'))
        bib_content = bib_def.contents[0].strip()
        self.assertEqual('Some book (without a link)', bib_content)

        ref_bib_page = self.html.read('formats_ref_bib.html')
        ref_bib_anchor = ref_bib_page.body.p.sup.a['name']
        forward_link = ref_bib_page.body.p.sup.select('a.ref')[0]
        self.assertEqual(
            f'formats_def_bib.html#{bib_anchor}', forward_link['href'])

        bib_back_link_hrefs = [
            link['href'] for link in bib_def.sup.find_all('a', class_='bib-back')]
        self.assertIn(
            f'formats_ref_bib.html#{ref_bib_anchor}', bib_back_link_hrefs)

    def test_bibdef_back_ref_delimiter(self):
        ref_first_page = self.html.read('formats_ref_bib.html')
        ref_first_anchor = ref_first_page.body.p.sup.a['name']
        ref_second_page = self.html.read('formats_ref_bib_second.html')
        ref_second_anchor = ref_second_page.body.p.sup.a['name']

        bib_page = self.html.read('formats_def_bib.html')
        bib_sup = bib_page.body.find('div', class_='bib-def').sup
        back_links = bib_sup.find_all('a', class_='bib-back')
        self.assertEqual(3, len(back_links))

        hrefs = [link['href'] for link in back_links]
        self.assertIn(f'formats_ref_bib.html#{ref_first_anchor}', hrefs)
        self.assertIn(f'formats_ref_bib_second.html#{ref_second_anchor}', hrefs)

        sup_children = list(bib_sup.children)
        self.assertEqual(5, len(sup_children))
        self.assertEqual('; ', str(sup_children[1]))
        self.assertEqual('; ', str(sup_children[3]))

    def test_mixed_markers_cross_link(self):
        def_standard_page = self.html.read('formats_def_standard.html')
        def_standard_anchor = def_standard_page.body.p.a['name']

        ref_standard_page = self.html.read('formats_ref_standard.html')
        ref_standard_forward = ref_standard_page.body.p.sup.select('a.ref')[0]
        self.assertEqual(
            f'formats_def_standard.html#{def_standard_anchor}',
            ref_standard_forward['href'])

        def_bib_page = self.html.read('formats_def_bib.html')
        bib_def = def_bib_page.body.find('div', class_='bib-def')
        bib_anchor = bib_def['id']

        ref_bib_compact_page = self.html.read('formats_ref_bib_compact.html')
        ref_bib_compact_cite = ref_bib_compact_page.body.p.cite
        self.assertEqual(['compact-ref'], ref_bib_compact_cite['class'])
        compact_anchors = ref_bib_compact_cite.find_all('a')
        ref_bib_compact_anchor = compact_anchors[0]['name']
        ref_bib_compact_forward = compact_anchors[1]
        self.assertEqual('format_bib', ref_bib_compact_forward.text)
        self.assertEqual(
            f'formats_def_bib.html#{bib_anchor}', ref_bib_compact_forward['href'])

        bib_back_link_hrefs = [
            link['href'] for link in bib_def.sup.find_all('a', class_='bib-back')]
        self.assertIn(
            f'formats_ref_bib_compact.html#{ref_bib_compact_anchor}',
            bib_back_link_hrefs)


if __name__ == '__main__':
    unittest.main()
