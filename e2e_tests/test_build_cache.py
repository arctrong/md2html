import os
import unittest
from pathlib import Path

import helpers as h


class BuildCacheE2eTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('BuildCacheTest')
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.CACHE_FILE = str(Path(cls.OUTPUT_DIR).joinpath('build_cache.json'))

    def run_build(self, arg_file_name, add_cache_file: bool):
        args = [
            '--input-root', str(self.INPUT_DIR),
            '--output-root', self.OUTPUT_DIR,
            '--argument-file', str(self.INPUT_DIR.joinpath(arg_file_name))
        ]
        if add_cache_file:
            args.extend(['--cache-file', self.CACHE_FILE])
        h.run_with_parameters(args)

    def get_output_files(self):
        return {str(f.relative_to(self.OUTPUT_DIR)): os.path.getmtime(f)
                for f in Path(self.OUTPUT_DIR).glob('*.*')
                if not f.name.startswith('.')}

    def test(self):
        output_files = self.step_without_build_cache()
        output_files = self.step_with_build_cache_should_regenerate_all(output_files)
        output_files = self.step_rebuild_should_not_regenerate(output_files)
        output_files = self.step_doc_set_changed_should_regenerate_all(output_files)

    def step_without_build_cache(self):
        self.run_build('md2html_args_step1_initial.json', False)
        output_files = self.get_output_files()
        self.assertCountEqual([
            "unchanged.html",
            "removed_with_wrap.html",
            "CodeToRemove.java.html",
            "index_page.html",
            "index_cache.json",
            ], output_files.keys())
        return output_files

    def step_with_build_cache_should_regenerate_all(self, output_files_before):
        self.run_build('md2html_args_step1_initial.json', True)
        output_files = self.get_output_files()
        self.assertIn("build_cache.json", output_files.keys())
        self.assertGreater(output_files["unchanged.html"], output_files_before["unchanged.html"])
        self.assertGreater(output_files["removed_with_wrap.html"], 
                           output_files_before["removed_with_wrap.html"])
        return output_files

    def step_rebuild_should_not_regenerate(self, output_files_before):
        self.run_build('md2html_args_step1_initial.json', True)
        output_files = self.get_output_files()
        self.assertIn("build_cache.json", output_files.keys())
        self.assertEqual(output_files["unchanged.html"], output_files_before["unchanged.html"])
        self.assertEqual(output_files["removed_with_wrap.html"], 
                         output_files_before["removed_with_wrap.html"])
        return output_files

    def step_doc_set_changed_should_regenerate_all(self, output_files_before):
        self.run_build('md2html_args_step2_doc_set_changed.json', True)
        output_files = self.get_output_files()
        self.assertCountEqual([
            "build_cache.json",
            "unchanged.html",
            "added_with_wrap.html",
            "CodeToPreserve.java.html",
            ], output_files.keys())
        self.assertGreater(output_files["unchanged.html"], 
                           output_files_before["unchanged.html"])
        return output_files


if __name__ == '__main__':
    unittest.main()