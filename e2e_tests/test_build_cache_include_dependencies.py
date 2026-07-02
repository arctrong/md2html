import os
import shutil
import time
import unittest
from pathlib import Path

from bs4 import BeautifulSoup

import helpers as h


@unittest.skipUnless(h.IMPLEMENTATION == 'py',
                     'Include dependency tracking is Python-only for now')
class BuildCacheIncludeDependencyE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('BuildCacheIncludeDependencyTest')
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.CACHE_FILE = str(Path(cls.OUTPUT_DIR).joinpath('build_cache.json'))

        # Some shared included files need to be modified (touched) during the test, so
        # they are copied to a git-ignored directory to avoid mutating source-controlled files.
        shared_dir = Path(h.WORKING_DIR).joinpath(
            'test_output/common/BuildCacheIncludeDependencyE2eTest')
        h.recreate_directory(shared_dir)
        cls.SHARED_FILE = shared_dir.joinpath('shared.txt')
        cls.SHARED_LEVEL2_FILE = shared_dir.joinpath('shared_level2.txt')
        shutil.copy(str(cls.INPUT_DIR.joinpath('shared.txt')), str(cls.SHARED_FILE))
        shutil.copy(str(cls.INPUT_DIR.joinpath('shared_level2.txt')),
                    str(cls.SHARED_LEVEL2_FILE))

    def run_build(self):
        h.run_with_parameters([
            '--input-root', str(self.INPUT_DIR),
            '--output-root', self.OUTPUT_DIR,
            '--argument-file', str(self.INPUT_DIR.joinpath('md2html_args.json')),
            '--cache-file', self.CACHE_FILE,
        ])

    def get_output_files(self):
        return {str(f.relative_to(self.OUTPUT_DIR)): os.path.getmtime(f)
                for f in Path(self.OUTPUT_DIR).glob('*.*')
                if not f.name.startswith('.')}

    def get_body_paragraphs(self, name):
        with open(Path(self.OUTPUT_DIR).joinpath(name)) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        return [p.text for p in root.body.find_all('p')]

    def _build_and_confirm_stable_rebuild(self):
        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual([
            'build_cache.json',
            'unchanged.html',
            'page_a.html',
            'page_b.html',
        ], output_files.keys())

        self.run_build()
        output_files_after_rebuild = self.get_output_files()
        for name in ('unchanged.html', 'page_a.html', 'page_b.html'):
            self.assertEqual(output_files[name], output_files_after_rebuild[name],
                             f'{name} should not be regenerated on an unchanged rebuild')
        return output_files_after_rebuild

    def test_transitive_include_change_propagates_to_pages(self):
        output_files_before = self._build_and_confirm_stable_rebuild()

        for page, page_token in (('page_a.html', 'PAGE-A'), ('page_b.html', 'PAGE-B')):
            paragraphs = self.get_body_paragraphs(page)
            self.assertEqual(page_token, paragraphs[0])
            self.assertEqual('LEVEL1-CONTENT', paragraphs[1])
            self.assertEqual('LEVEL2-ORIGINAL', paragraphs[2])

        time.sleep(0.2)
        self.SHARED_LEVEL2_FILE.write_text('LEVEL2-UPDATED\n', encoding='utf-8')
        self.run_build()
        output_files_after = self.get_output_files()

        self.assertEqual(output_files_before['unchanged.html'],
                         output_files_after['unchanged.html'])
        self.assertGreater(output_files_after['page_a.html'],
                           output_files_before['page_a.html'])
        self.assertGreater(output_files_after['page_b.html'],
                           output_files_before['page_b.html'])

        for page, page_token in (('page_a.html', 'PAGE-A'), ('page_b.html', 'PAGE-B')):
            paragraphs = self.get_body_paragraphs(page)
            self.assertEqual(page_token, paragraphs[0])
            self.assertEqual('LEVEL1-CONTENT', paragraphs[1])
            self.assertEqual('LEVEL2-UPDATED', paragraphs[2])


if __name__ == '__main__':
    unittest.main()
