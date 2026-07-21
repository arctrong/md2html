import json
import os
import unittest
from pathlib import Path

from bs4 import BeautifulSoup

import helpers as h

@unittest.skipUnless(
    os.getenv("IMPLEMENTATION") == "py",
    "Skipping class because IMPLEMENTATION is not 'py'. Java version is not implemented yet.",
)
class BackReferencesPluginContentExtendedE2eTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('BackReferencesPluginContentExtendedTest')
        # This test class uses the strategy: generate all pages at once then test everything.
        # So a cache usage is acceptable. All test methods only read the output files.
        cls._output_pages_cache = {}
        h.run_with_parameters([
            '--input-root', str(cls.INPUT_DIR),
            '--output-root', cls.OUTPUT_DIR,
            '--argument-file', str(cls.INPUT_DIR.joinpath('md2html_args.json')),
        ])

    def _read_cached_output(self, file_name):
        if file_name not in self._output_pages_cache:
            path = Path(self.OUTPUT_DIR).joinpath(file_name)
            with open(path, encoding='utf-8') as html_file:
                self._output_pages_cache[file_name] = BeautifulSoup(html_file, 'html.parser')
        return self._output_pages_cache[file_name]

    def _assert_input_listed_before(self, earlier_input, later_input):
        arg_file = self.INPUT_DIR.joinpath('md2html_args.json')
        with open(arg_file, encoding='utf-8') as file:
            documents = json.load(file)['documents']
        inputs = [entry['input'] for entry in documents]
        self.assertLess(
            inputs.index(earlier_input), inputs.index(later_input),
            f'{earlier_input!r} must appear before {later_input!r} in documents')

    def test_refs_before_definition(self):
        self._assert_input_listed_before(
            'extended_ref_first.txt', 'extended_def_com.txt')

        def_com_page = self._read_cached_output('extended_def_com.html')
        def_com_anchor = def_com_page.body.p.a['name']

        ref_page = self._read_cached_output('extended_ref_first.html')
        forward_link = ref_page.body.p.sup.select('a.ref')[0]
        self.assertEqual(
            f'extended_def_com.html#{def_com_anchor}', forward_link['href'])

    def test_multi_referencer_back_links(self):
        def_page = self._read_cached_output('extended_def_com.html')
        self.assertEqual('Definition for example.com', def_page.body.h1.text)

        def_paragraph = def_page.body.p
        def_span = def_paragraph.span
        self.assertEqual('[example_com]', def_span.text)
        self.assertEqual(['ref-def'], def_span['class'])
        display_link = def_paragraph.select('a:nth-of-type(2)')[0]
        self.assertEqual('Example.com', display_link.text)

        ref_first_page = self._read_cached_output('extended_ref_first.html')
        ref_first_anchor = ref_first_page.body.p.sup.a['name']
        ref_second_page = self._read_cached_output('extended_ref_second.html')
        ref_second_anchor = ref_second_page.body.p.sup.a['name']

        back_links = def_paragraph.sup.find_all('a', class_='ref')
        hrefs = [link['href'] for link in back_links]
        self.assertIn(
            f'extended_ref_first.html#{ref_first_anchor}', hrefs)
        self.assertIn(
            f'extended_ref_second.html#{ref_second_anchor}', hrefs)

    def test_multiple_refs_same_page(self):
        multi_ref_page = self._read_cached_output('extended_multi_ref.html')
        ref_anchor_names = {sup.a['name'] for sup in multi_ref_page.body.find_all('sup')}
        self.assertEqual(2, len(ref_anchor_names))

        def_page = self._read_cached_output('extended_multi_ref_def.html')
        back_links = def_page.body.p.sup.find_all('a', class_='ref')
        self.assertEqual(2, len(back_links))
        for link in back_links:
            self.assertTrue(link['href'].startswith('extended_multi_ref.html#'))
        def_anchor_names = {link['href'].split('#', 1)[1] for link in back_links}
        self.assertEqual(ref_anchor_names, def_anchor_names)

    def test_definition_page_references_other_source(self):
        def_net_page = self._read_cached_output('extended_def_net.html')
        def_net_paragraph = def_net_page.body.find_all('p')[0]
        def_net_anchor = def_net_paragraph.a['name']
        self.assertEqual('[example_net]', def_net_paragraph.span.text)

        cross_ref_paragraph = def_net_page.body.find_all('p')[1]
        self.assertEqual('See also ', cross_ref_paragraph.contents[0])
        self.assertEqual('Example.com', cross_ref_paragraph.a.text)
        cross_ref_anchor = cross_ref_paragraph.sup.a['name']
        cross_ref_link = cross_ref_paragraph.sup.select('a.ref')[0]
        self.assertEqual('[example_com]', cross_ref_link.text)

        def_com_page = self._read_cached_output('extended_def_com.html')
        def_com_anchor = def_com_page.body.p.a['name']
        self.assertEqual(
            f'extended_def_com.html#{def_com_anchor}', cross_ref_link['href'])

        back_link_hrefs = [
            link['href']
            for link in def_com_page.body.p.sup.find_all('a', class_='ref')]
        self.assertIn(
            f'extended_def_net.html#{cross_ref_anchor}', back_link_hrefs)

        ref_net_page = self._read_cached_output('extended_ref_net.html')
        ref_net_forward_link = ref_net_page.body.p.sup.select('a.ref')[0]
        self.assertEqual(
            f'extended_def_net.html#{def_net_anchor}', ref_net_forward_link['href'])

    def test_self_reference_on_definition_page(self):
        def_com_page = self._read_cached_output('extended_def_com.html')
        def_paragraph = def_com_page.body.find_all('p')[0]
        def_com_anchor = def_paragraph.a['name']

        self_ref_paragraph = def_com_page.body.find_all('p')[1]
        self.assertEqual('Also see ', self_ref_paragraph.contents[0])
        self_ref_anchor = self_ref_paragraph.sup.a['name']
        self_ref_link = self_ref_paragraph.sup.select('a.ref')[0]
        self.assertEqual('[example_com]', self_ref_link.text)
        self.assertEqual(
            f'extended_def_com.html#{def_com_anchor}', self_ref_link['href'])

        back_link_hrefs = [
            link['href']
            for link in def_paragraph.sup.find_all('a', class_='ref')]
        self.assertIn(
            f'extended_def_com.html#{self_ref_anchor}', back_link_hrefs)

    def test_relative_paths(self):
        def_page = self._read_cached_output('defs/extended_subdir_def.html')
        def_paragraph = def_page.body.p
        def_anchor = def_paragraph.a['name']
        self.assertEqual('[subdir_example]', def_paragraph.span.text)

        ref_page = self._read_cached_output('pages/extended_subdir_ref.html')
        ref_anchor = ref_page.body.p.sup.a['name']
        forward_link = ref_page.body.p.sup.select('a.ref')[0]
        self.assertEqual('[subdir_example]', forward_link.text)
        self.assertEqual(
            f'../defs/extended_subdir_def.html#{def_anchor}', forward_link['href'])

        back_link_hrefs = [
            link['href']
            for link in def_paragraph.sup.find_all('a', class_='ref')]
        self.assertIn(
            f'../pages/extended_subdir_ref.html#{ref_anchor}', back_link_hrefs)


if __name__ == '__main__':
    unittest.main()
