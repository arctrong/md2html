import os
import shutil
import time
import unittest
from pathlib import Path

from bs4 import BeautifulSoup

import helpers as h


@unittest.skipUnless(
    os.getenv("IMPLEMENTATION") == "py",
    "Skipping class because IMPLEMENTATION is not 'py'. Java version is not implemented yet.",
)
class BackReferencesPluginIncrementalPluginCacheE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath(
            'BackReferencesIncrementalPluginCacheTest')
        cls.SOURCE_DIR = Path(h.WORKING_DIR).joinpath(
            'test_output/common/BackReferencesIncrementalPluginCacheE2eTest')
        h.recreate_directory(cls.SOURCE_DIR)
        cls.DEF_SOURCE = cls.SOURCE_DIR.joinpath('incr_plugin_cache_def.txt')
        cls.REF_FIRST_SOURCE = cls.SOURCE_DIR.joinpath('incr_plugin_cache_ref_first.txt')
        cls.REF_SECOND_SOURCE = cls.SOURCE_DIR.joinpath('incr_plugin_cache_ref_second.txt')
        cls.BACKREFS_CACHE_FILE = cls.SOURCE_DIR.joinpath('backrefs_cache.json')
        for name in (
                'incr_plugin_cache_def.txt',
                'incr_plugin_cache_ref_first.txt',
                'incr_plugin_cache_ref_second.txt'):
            shutil.copy(cls.INPUT_DIR.joinpath(name), cls.SOURCE_DIR.joinpath(name))

        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.CACHE_FILE = str(Path(cls.OUTPUT_DIR).joinpath('build_cache.json'))

    def run_build(self, arg_file_name='md2html_args.json'):
        h.run_with_parameters([
            '--input-root', str(self.SOURCE_DIR),
            '--output-root', self.OUTPUT_DIR,
            '--argument-file', str(self.INPUT_DIR.joinpath(arg_file_name)),
            '--cache-file', self.CACHE_FILE,
        ])

    def get_output_files(self):
        return {str(f.relative_to(self.OUTPUT_DIR)): f.stat().st_mtime_ns
                for f in Path(self.OUTPUT_DIR).glob('*.*')
                if not f.name.startswith('.')}

    def _read_page(self, html_name) -> BeautifulSoup:
        with open(Path(self.OUTPUT_DIR).joinpath(html_name), encoding='utf-8') as html_file:
            page = BeautifulSoup(html_file, 'html.parser')
        return page

    def _read_ref_anchors(self, html_name):
        page = self._read_page(html_name)
        return {sup.a['name'] for sup in page.find_all('sup')}

    def _read_def_back_link_hrefs(self):
        page = self._read_page('incr_plugin_cache_def.html')
        return [link['href']
                for link in page.body.p.sup.find_all('a', class_='ref')]

    def test(self):
        output_files = self.step_initial_cached_build()
        output_files = self.step_stable_no_op_rebuild(output_files)
        output_files = self.step_def_rebuild_refs_skipped(output_files)
        output_files = self.step_one_ref_rebuild_partial_reset(output_files)
        self.step_doc_set_prune(output_files)

    def step_initial_cached_build(self):
        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual([
            'build_cache.json',
            'incr_plugin_cache_def.html',
            'incr_plugin_cache_ref_first.html',
            'incr_plugin_cache_ref_second.html',
        ], output_files.keys())
        self.assertTrue(self.BACKREFS_CACHE_FILE.is_file())
        return output_files

    def step_stable_no_op_rebuild(self, output_files_before):
        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual(output_files_before.keys(), output_files.keys())
        for name in (
                'incr_plugin_cache_def.html',
                'incr_plugin_cache_ref_first.html',
                'incr_plugin_cache_ref_second.html'):
            self.assertEqual(
                output_files_before[name], output_files[name],
                f'{name} should not be regenerated on an unchanged rebuild')
        return output_files

    def step_def_rebuild_refs_skipped(self, output_files_before):
        ref_second_anchors = self._read_ref_anchors('incr_plugin_cache_ref_second.html')
        self.assertEqual(1, len(ref_second_anchors))
        ref_second_anchor = next(iter(ref_second_anchors))

        time.sleep(0.2)
        self.REF_FIRST_SOURCE.write_text(
            self.REF_FIRST_SOURCE.read_text(encoding='utf-8') + '\n\nRef page edited.\n',
            encoding='utf-8')

        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual(output_files_before.keys(), output_files.keys())

        self.assertGreater(
            output_files['incr_plugin_cache_ref_first.html'],
            output_files_before['incr_plugin_cache_ref_first.html'])
        self.assertEqual(
            output_files['incr_plugin_cache_ref_second.html'],
            output_files_before['incr_plugin_cache_ref_second.html'])
        self.assertGreater(
            output_files['incr_plugin_cache_def.html'],
            output_files_before['incr_plugin_cache_def.html'])

        ref_first_anchors = self._read_ref_anchors('incr_plugin_cache_ref_first.html')
        self.assertEqual(1, len(ref_first_anchors))
        ref_first_anchor = next(iter(ref_first_anchors))
        back_link_hrefs = self._read_def_back_link_hrefs()
        self.assertIn(f'incr_plugin_cache_ref_first.html#{ref_first_anchor}',
                      back_link_hrefs)
        self.assertIn(f'incr_plugin_cache_ref_second.html#{ref_second_anchor}',
                      back_link_hrefs)
        return output_files

    def step_one_ref_rebuild_partial_reset(self, output_files_before):
        ref_second_anchors = self._read_ref_anchors('incr_plugin_cache_ref_second.html')
        self.assertEqual(1, len(ref_second_anchors))
        ref_second_anchor = next(iter(ref_second_anchors))

        time.sleep(0.2)
        self.REF_FIRST_SOURCE.write_text(
            self.REF_FIRST_SOURCE.read_text(encoding='utf-8')
            + '\n\nAlso see <!--ref incr_plugin_cache-->.\n', encoding='utf-8')

        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual(output_files_before.keys(), output_files.keys())

        self.assertGreater(
            output_files['incr_plugin_cache_ref_first.html'],
            output_files_before['incr_plugin_cache_ref_first.html'])
        self.assertEqual(
            output_files['incr_plugin_cache_ref_second.html'],
            output_files_before['incr_plugin_cache_ref_second.html'])
        self.assertGreater(
            output_files['incr_plugin_cache_def.html'],
            output_files_before['incr_plugin_cache_def.html'])

        ref_first_anchors = self._read_ref_anchors('incr_plugin_cache_ref_first.html')
        self.assertEqual(2, len(ref_first_anchors))

        expected_back_links = {
            f'incr_plugin_cache_ref_first.html#{anchor}'
            for anchor in ref_first_anchors
        }
        expected_back_links.add(
            f'incr_plugin_cache_ref_second.html#{ref_second_anchor}')
        self.assertEqual(expected_back_links, set(self._read_def_back_link_hrefs()))
        return output_files

    def step_doc_set_prune(self, output_files_before):
        ref_first_anchors = self._read_ref_anchors('incr_plugin_cache_ref_first.html')
        self.assertEqual(2, len(ref_first_anchors))

        self.run_build('md2html_args_step2_pruned.json')
        output_files = self.get_output_files()
        self.assertCountEqual([
            'build_cache.json',
            'incr_plugin_cache_def.html',
            'incr_plugin_cache_ref_first.html',
        ], output_files.keys())
        self.assertTrue(self.BACKREFS_CACHE_FILE.is_file())

        self.assertGreater(
            output_files['incr_plugin_cache_def.html'],
            output_files_before['incr_plugin_cache_def.html'])

        expected_back_links = {
            f'incr_plugin_cache_ref_first.html#{anchor}'
            for anchor in ref_first_anchors
        }
        self.assertEqual(expected_back_links, set(self._read_def_back_link_hrefs()))


if __name__ == '__main__':
    unittest.main()
