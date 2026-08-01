import shutil
import time
import unittest
from pathlib import Path

import helpers as h


class BackReferencesPluginIncrementalBuildCacheE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath(
            'BackReferencesIncrementalBuildCacheTest')
        cls.SOURCE_DIR = Path(h.WORKING_DIR).joinpath(
            'test_output/common/BackReferencesIncrementalBuildCacheE2eTest')
        h.recreate_directory(cls.SOURCE_DIR)
        cls.DEF_SOURCE = cls.SOURCE_DIR.joinpath('incr_build_cache_def.txt')
        cls.REF_SOURCE = cls.SOURCE_DIR.joinpath('incr_build_cache_ref.txt')
        shutil.copy(cls.INPUT_DIR.joinpath('incr_build_cache_def.txt'), cls.DEF_SOURCE)
        shutil.copy(cls.INPUT_DIR.joinpath('incr_build_cache_ref.txt'), cls.REF_SOURCE)

        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.CACHE_FILE = str(Path(cls.OUTPUT_DIR).joinpath('build_cache.json'))

    def run_build(self):
        h.run_with_parameters([
            '--input-root', str(self.SOURCE_DIR),
            '--output-root', self.OUTPUT_DIR,
            '--argument-file', str(self.INPUT_DIR.joinpath('md2html_args.json')),
            '--cache-file', self.CACHE_FILE,
        ])

    def get_output_files(self):
        return {str(f.relative_to(self.OUTPUT_DIR)): f.stat().st_mtime_ns
                for f in Path(self.OUTPUT_DIR).glob('*.*')
                if not f.name.startswith('.')}

    def test(self):
        output_files = self.step_initial_cached_build()
        output_files = self.step_stable_no_op_rebuild(output_files)
        output_files = self.step_ref_edit_rebuilds_def(output_files)
        self.step_def_edit_rebuilds_ref(output_files)

    def step_initial_cached_build(self):
        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual([
            'build_cache.json',
            'incr_build_cache_def.html',
            'incr_build_cache_ref.html',
        ], output_files.keys())
        return output_files

    def step_stable_no_op_rebuild(self, output_files_before):
        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual(output_files_before.keys(), output_files.keys())
        for name in ('incr_build_cache_def.html', 'incr_build_cache_ref.html'):
            self.assertEqual(
                output_files_before[name], output_files[name],
                f'{name} should not be regenerated on an unchanged rebuild')
        return output_files

    def step_ref_edit_rebuilds_def(self, output_files_before):
        time.sleep(0.2)
        self.REF_SOURCE.write_text(
            self.REF_SOURCE.read_text(encoding='utf-8') + '\n\nRef file edited.\n',
            encoding='utf-8')

        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual(output_files_before.keys(), output_files.keys())
        self.assertGreater(
            output_files['incr_build_cache_ref.html'],
            output_files_before['incr_build_cache_ref.html'])
        self.assertGreater(
            output_files['incr_build_cache_def.html'],
            output_files_before['incr_build_cache_def.html'])
        return output_files

    def step_def_edit_rebuilds_ref(self, output_files_before):
        time.sleep(0.2)
        self.DEF_SOURCE.write_text(
            self.DEF_SOURCE.read_text(encoding='utf-8') + '\n\nDef file edited.\n',
            encoding='utf-8')

        self.run_build()
        output_files = self.get_output_files()
        self.assertCountEqual(output_files_before.keys(), output_files.keys())
        self.assertGreater(
            output_files['incr_build_cache_def.html'],
            output_files_before['incr_build_cache_def.html'])
        self.assertGreater(
            output_files['incr_build_cache_ref.html'],
            output_files_before['incr_build_cache_ref.html'])


if __name__ == '__main__':
    unittest.main()
