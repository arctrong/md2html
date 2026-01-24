import json
import os
import tempfile
import unittest
from unittest.mock import patch

from build_cache import _BuildCacheManager, get_empty_build_cache
from models.document import Document
from .utils_for_tests import *


class BuildCacheLoadingAndForcingAllTest(unittest.TestCase):

    def setUp(self):
        self.cache_manager = _BuildCacheManager()
        # TODO Find other tests that use temporary files and consider using the `tempfile`
        #  solution
        self._temp_dir = tempfile.TemporaryDirectory()
        self.temp_dir = self._temp_dir.name
        self.cache_file = Path(self.temp_dir).joinpath('test_cache.json')

    def tearDown(self):
        if os.path.exists(self.cache_file):
            os.remove(self.cache_file)
        self._temp_dir.cleanup()

    @patch('os.path.getmtime')
    def test_get_force_all_depending_on_cache_file_and_arg_file(self, mock_mtime):
        mock_mtime.return_value = 280
        arg_file_mtime = 280

        documents = [Document(input_file='input1.txt', output_file='output1.html')]

        for cache_file_exists, arg_file_modified, should_force_all in [
            (True, False, False), (True, True, True), (False, False, True),
        ]:
            with self.subTest(f"{cache_file_exists}, {arg_file_modified}, {should_force_all}"):
                if arg_file_modified:
                    arg_file_mtime = 270
                if cache_file_exists:
                    test_cache = {
                        'arg_file_mtime': arg_file_mtime,
                        'primary_documents': {'input1.txt': {'output_file': 'output1.html'}}
                    }
                    with open(self.cache_file, 'w') as f:
                        json.dump(test_cache, f)

                self.cache_manager.load_build_cache(self.cache_file, '/path/to/args.json')

                self.assertEqual(self.cache_manager.get_force_all(documents), should_force_all)

    @patch('os.path.getmtime')
    def test_get_force_all_depending_on_document_set(self, mock_mtime):
        mock_mtime.return_value = 280

        documents = [Document(input_file='input1.txt', output_file='output1.html')]
        test_cache = {
            'arg_file_mtime': 280,
            'primary_documents': {'input1.txt': {'output_file': 'output1.html'}}
        }
        with open(self.cache_file, 'w') as f:
            json.dump(test_cache, f)

        for new_docs_appeared, some_docs_removed, should_force_all in [
            (False, False, False), (True, False, True), (False, True, True),
        ]:
            with self.subTest(f"{new_docs_appeared}, {some_docs_removed}, {should_force_all}"):
                if new_docs_appeared:
                    documents.append(Document(input_file='input2.txt', output_file='output2.html'))
                if some_docs_removed:
                    documents.clear()

                self.cache_manager.load_build_cache(self.cache_file, '/path/to/args.json')

                self.assertEqual(self.cache_manager.get_force_all(documents), should_force_all)


class BuildCacheRecordingTest(unittest.TestCase):

    def setUp(self):
        self.cache_manager = _BuildCacheManager()
        self.cache_manager.previous_cache = get_empty_build_cache(230)
        self.cache_manager.previous_cache['primary_documents']['input.md'] = {
            'output_file': 'output_OLD.html',
            'derived_documents': {'derived.html'}
        }
        self.cache_manager.current_cache = get_empty_build_cache(230)

    def test_record_primary_document_not_skipped(self):
        self.cache_manager.record_primary_document('input.md', 'output.html', False)

        self.assertDictEqual(self.cache_manager.current_cache['primary_documents']['input.md'],
                         {'output_file': 'output.html'})

    def test_record_primary_document_skipped(self):        
        self.cache_manager.record_primary_document('input.md', 'output.html', True)

        self.assertDictEqual(self.cache_manager.current_cache['primary_documents']['input.md'],
                         {'output_file': 'output.html', 'derived_documents': {'derived.html'}})

    def test_record_derived_document_for_primary(self):
        self.cache_manager.record_derived_document_for_primary('input.md', 'generated.html')

        primary_entry = self.cache_manager.current_cache['primary_documents']['input.md']
        self.assertIn('generated.html', primary_entry['derived_documents'])

    def test_record_standalone_derived_document(self):
        self.cache_manager.record_standalone_derived_document('index.html')

        self.assertIn('index.html', 
                      self.cache_manager.current_cache['standalone_derived_documents'])

    def test_no_op_when_cache_not_used(self):
        self.cache_manager.previous_cache = None
        self.cache_manager.current_cache = None

        self.cache_manager.record_primary_document('input.md', 'output.html', False)
        self.cache_manager.record_derived_document_for_primary('input.md', 'generated.html')
        self.cache_manager.record_standalone_derived_document('index.html')
        
        self.assertIsNone(self.cache_manager.previous_cache)
        self.assertIsNone(self.cache_manager.current_cache)


class BuildCacheFinalizationTest(unittest.TestCase):

    def setUp(self):
        self.cache_manager = _BuildCacheManager()
        self._temp_dir = tempfile.TemporaryDirectory()
        self.temp_dir = self._temp_dir.name
        self.cache_file = Path(self.temp_dir).joinpath('test_cache.json')
        self.cache_manager.build_cache_file = self.cache_file

        self.cache_manager.previous_cache = {
            'arg_file_mtime': 1234567890,
            'primary_documents': {
                'input.md': {
                    'output_file': 'output.html',
                    'derived_documents': ['keep.html']
                },
                'obsolete.md': {
                    'output_file': 'obsolete.html',
                    'derived_documents': ['remove.html']
                }
            },
            'standalone_derived_documents': ['keep_standalone.html', 'remove_standalone.html']
        }
        self.cache_manager.current_cache = {
            'arg_file_mtime': 1234567890,
            'primary_documents': {
                'input.md': {
                    'output_file': 'output.html',
                    'derived_documents': ['keep.html']
                }
            },
            'standalone_derived_documents': ['keep_standalone.html']
        }

    def tearDown(self):
        if os.path.exists(self.cache_file):
            os.remove(self.cache_file)
        self._temp_dir.cleanup()

    @patch('os.path.exists')
    @patch('os.remove')
    def test_delete_obsolete_files(self, mock_remove, mock_exists):
        mock_exists.return_value = True

        self.cache_manager.delete_obsolete_files()

        self.assertEqual(mock_remove.call_count, 3)
        mock_remove.assert_any_call('obsolete.html')
        mock_remove.assert_any_call('remove.html')
        mock_remove.assert_any_call('remove_standalone.html')

    def test_save_build_cache(self):
        self.cache_manager.save_build_cache()

        self.assertTrue(os.path.exists(self.cache_file))

        with open(self.cache_file, 'r') as f:
            saved_cache = json.load(f)

        self.assertEqual(saved_cache['arg_file_mtime'], 1234567890)
        self.assertIn('input.md', saved_cache['primary_documents'])


class BuildCacheIntegrationTest(unittest.TestCase):

    def test_full_cache_workflow(self):
        cache_manager = _BuildCacheManager()

        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_dir_path = Path(tmp_dir)
            cache_file = str(tmp_dir_path.joinpath("my_build_cache.json"))
            args_file_path = tmp_dir_path.joinpath("args.json")
            args_file_path.touch()
            args_file = str(args_file_path)

            cache_manager.load_build_cache(cache_file, args_file)

            cache_manager.record_primary_document('doc1.md', 'doc1.html', False)
            cache_manager.record_derived_document_for_primary('doc1.md', 'my_code.html')

            cache_manager.delete_obsolete_files()
            cache_manager.save_build_cache()

            self.assertTrue(os.path.exists(cache_file))
            with open(cache_file, 'r') as f:
                final_cache = json.load(f)

            self.assertIn('doc1.md', final_cache['primary_documents'])
            primary_doc = final_cache['primary_documents']['doc1.md']
            self.assertIn('derived_documents', primary_doc)
            self.assertIn('my_code.html', primary_doc['derived_documents'])


if __name__ == '__main__':
    unittest.main()
