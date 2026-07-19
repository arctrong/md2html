import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from cli_arguments_utils import CliArgDataObject
from md2html import load_json_argument_file, register_page_metadata_handlers
from models.document import Document
from models.options import Options
from page_metadata_utils import apply_metadata_handlers, join_parsing_results
from plugins.back_references_plugin import BackReferencesPlugin, _normalize_loaded_cache, \
    _build_reverse_map_references_by_page_from_cache
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, BackReferencesPlugin)


def _plugin_with_cache(temp_dir: str) -> BackReferencesPlugin:
    cache_file = str(Path(temp_dir).joinpath('back_refs_cache.json')).replace('\\', '/')
    plugin = BackReferencesPlugin()
    plugin.accept_data({'cache': cache_file})
    plugin.accept_app_data([], Options(), None)
    return plugin


def _set_cache_referencing_pages(plugin, referencing_pages_by_source):
    plugin.backrefs_cache = _normalize_loaded_cache(referencing_pages_by_source)
    plugin._reverse_map_references_by_page = _build_reverse_map_references_by_page_from_cache(
        plugin.backrefs_cache)    
    plugin._populate_references_from_cache(plugin.references)


class BackReferencesPluginTest(unittest.TestCase):

    def setUp(self):
        self._temp_dir = tempfile.TemporaryDirectory()
        self.temp_dir = self._temp_dir.name

    def tearDown(self):
        self._temp_dir.cleanup()

    def test_activated_with_empty_plugin_def(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "page.txt"}], "plugins": {"back-references": {}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        self.assertIsNotNone(plugin)
        self.assertFalse(plugin.is_blank())

    def test_ref_and_refdef_phase1(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '  {"input": "refs.txt", "output": "refs.html"},'
            '  {"input": "page.txt", "output": "page.html"}'
            '], "plugins": {"back-references": {}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        refs_doc = args.documents[0]
        page_doc = args.documents[1]

        plugin.new_page(refs_doc)
        refs_result = apply_metadata_handlers(
            'Intro <!--REFDEF foo Foo display-->', metadata_handlers, refs_doc)
        self.assertTrue(refs_result.deferPage)

        plugin.new_page(page_doc)
        page_result = apply_metadata_handlers(
            'See <!--REF foo-->', metadata_handlers, page_doc)
        self.assertFalse(page_result.deferPage)
        html = join_parsing_results(page_result.parsingResults, metadata_handlers, page_doc)
        self.assertIn('refs.html#backref_def_foo', html)

    def test_ref_cache_preserves_skipped_referencer(self):
        plugin = _plugin_with_cache(self.temp_dir)
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page1_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        page2_doc = Document(input_file='page_02.txt', output_file='doc/page_02.html')
        plugin.accept_document_list([refs_doc, page1_doc, page2_doc])

        _set_cache_referencing_pages(plugin, {
            'foo': [
                {
                    'input_file': 'page_01.txt',
                    'output_file': 'doc/page_01.html',
                    'anchor_ids': ['backref_ref_foo'],
                },
                {
                    'input_file': 'page_02.txt',
                    'output_file': 'doc/page_02.html',
                    'anchor_ids': ['backref_ref_foo_2'],
                },
            ],
        })

        plugin.new_page(page1_doc)
        plugin.accept_page_metadata(
            page1_doc, 'REF', 'foo', '<!--REF foo-->', phase=1)

        referencer_inputs = set(plugin.references.get('foo', {}).keys())
        self.assertEqual(referencer_inputs, {'page_01.txt', 'page_02.txt'})

    def test_ref_cache_new_page_clears_only_current_page(self):
        plugin = _plugin_with_cache(self.temp_dir)
        page1_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        page2_doc = Document(input_file='page_02.txt', output_file='doc/page_02.html')
        plugin.accept_document_list([page1_doc, page2_doc])
        _set_cache_referencing_pages(plugin, {
            'foo': [
                {
                    'input_file': 'page_01.txt',
                    'output_file': 'doc/page_01.html',
                    'anchor_ids': ['a1'],
                },
                {
                    'input_file': 'page_02.txt',
                    'output_file': 'doc/page_02.html',
                    'anchor_ids': ['a2'],
                },
            ],
        })

        plugin.new_page(page1_doc)

        self.assertNotIn('page_01.txt', plugin.backrefs_cache.get('foo', {}))
        refs_by_page = plugin.references.get('foo', {})
        self.assertEqual(set(refs_by_page.keys()), {'page_02.txt'})
        self.assertEqual(len(refs_by_page['page_02.txt']), 1)

    def test_new_page_with_no_input_file_skips_cache_reset(self):
        plugin = _plugin_with_cache(self.temp_dir)
        _set_cache_referencing_pages(plugin, {
            'foo': [{
                'input_file': 'page_01.txt',
                'output_file': 'doc/page_01.html',
                'anchor_ids': ['a1'],
            }],
        })

        index_doc = Document(input_file=None, output_file='doc/index_page.html')
        plugin.new_page(index_doc)

        self.assertIn('foo', plugin.backrefs_cache)
        self.assertEqual(len(plugin.references.get('foo', {})), 1)

    def test_ref_cache_finalize_writes_file(self):
        plugin = _plugin_with_cache(self.temp_dir)
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        plugin.accept_document_list([refs_doc, page_doc])

        plugin.new_page(refs_doc)
        plugin.accept_page_metadata(
            refs_doc, 'REFDEF', 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.new_page(page_doc)
        plugin.accept_page_metadata(page_doc, 'REF', 'foo', '<!--REF foo-->', phase=1)

        plugin.finalize()

        cache_path = Path(plugin.backrefs_cache_file)
        self.assertTrue(cache_path.exists())
        with open(cache_path, 'r', encoding='utf-8') as file:
            saved = json.load(file)
        self.assertIn('foo', saved)
        self.assertEqual(saved['foo'][0]['input_file'], 'page_01.txt')
        self.assertEqual(saved['foo'][0]['anchor_ids'], ['backref_ref_foo'])

    def test_ref_cache_load_and_prune_removed_documents(self):
        plugin = _plugin_with_cache(self.temp_dir)
        cache_path = Path(plugin.backrefs_cache_file)
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        with open(cache_path, 'w', encoding='utf-8') as file:
            json.dump({
                'foo': [{
                    'input_file': 'page_01.txt',
                    'output_file': 'doc/page_01.html',
                    'anchor_ids': ['a1'],
                }],
                'bar': [{
                    'input_file': 'removed.txt',
                    'output_file': 'doc/removed.html',
                    'anchor_ids': ['a2'],
                }],
            }, file)

        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin._load_backrefs_cache()
        plugin._prune_backrefs_cache()
        plugin._reverse_map_references_by_page = _build_reverse_map_references_by_page_from_cache(
            plugin.backrefs_cache)
        plugin._populate_references_from_cache(plugin.references)

        self.assertIn('foo', plugin.backrefs_cache)
        self.assertNotIn('bar', plugin.backrefs_cache)
        self.assertEqual(len(plugin.references.get('foo', {})), 1)

    def test_ref_cache_groups_multiple_anchors_on_same_page(self):
        plugin = _plugin_with_cache(self.temp_dir)
        page_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        plugin.accept_document_list([page_doc])

        plugin.new_page(page_doc)
        plugin.accept_page_metadata(page_doc, 'REF', 'foo', '<!--REF foo-->', phase=1)
        plugin.accept_page_metadata(page_doc, 'REF', 'foo', '<!--REF foo-->', phase=1)

        record = plugin.backrefs_cache['foo']['page_01.txt']
        self.assertEqual(record['input_file'], 'page_01.txt')
        self.assertEqual(len(record['anchor_ids']), 2)

    def test_reverse_map_references_by_page_after_cache_load(self):
        plugin = _plugin_with_cache(self.temp_dir)
        cache_path = Path(plugin.backrefs_cache_file)
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        with open(cache_path, 'w', encoding='utf-8') as file:
            json.dump({
                'foo': [
                    {
                        'input_file': 'page_01.txt',
                        'output_file': 'doc/page_01.html',
                        'anchor_ids': ['a1'],
                    },
                    {
                        'input_file': 'page_02.txt',
                        'output_file': 'doc/page_02.html',
                        'anchor_ids': ['a2'],
                    },
                ],
            }, file)

        plugin._load_backrefs_cache()
        plugin._reverse_map_references_by_page = _build_reverse_map_references_by_page_from_cache(
            plugin.backrefs_cache)

        self.assertEqual(plugin._reverse_map_references_by_page['page_01.txt'], {'foo'})
        self.assertEqual(plugin._reverse_map_references_by_page['page_02.txt'], {'foo'})

    def test_reverse_map_definitions_by_page_on_new_page(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({})
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')

        plugin.new_page(refs_doc)
        plugin.accept_page_metadata(
            refs_doc, 'REFDEF', 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.accept_page_metadata(
            refs_doc, 'REFDEF', 'bar Bar display', '<!--REFDEF bar Bar display-->', phase=1)

        self.assertEqual(plugin._reverse_map_definitions_by_page['refs.txt'], {'foo', 'bar'})

        plugin.new_page(refs_doc)

        self.assertNotIn('refs.txt', plugin._reverse_map_definitions_by_page)
        self.assertEqual(plugin.definitions, {})

    @patch('plugins.back_references_plugin.build_cache_manager')
    def test_refdef_phase2_uses_cached_references_from_skipped_page(self, mock_cache_manager):
        plugin = _plugin_with_cache(self.temp_dir)
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page1_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        page2_doc = Document(input_file='page_02.txt', output_file='doc/page_02.html')
        plugin.accept_document_list([refs_doc, page1_doc, page2_doc])
        _set_cache_referencing_pages(plugin, {
            'foo': [
                {
                    'input_file': 'page_01.txt',
                    'output_file': 'doc/page_01.html',
                    'anchor_ids': ['backref_ref_foo'],
                },
                {
                    'input_file': 'page_02.txt',
                    'output_file': 'doc/page_02.html',
                    'anchor_ids': ['backref_ref_foo_2'],
                },
            ],
        })

        plugin.new_page(refs_doc)
        result = plugin.accept_page_metadata(
            refs_doc, 'REFDEF', 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        html = plugin.accept_page_metadata(
            refs_doc, 'REFDEF', 'foo Foo display', '<!--REFDEF foo Foo display-->',
            phase=2, data_from_prev_phase=result.result).result

        self.assertIn('page_01.html#backref_ref_foo', html)
        self.assertIn('page_02.html#backref_ref_foo_2', html)

    def test_refdef_phase2_uses_phase1_references_without_new_page(self):
        plugin = _plugin_with_cache(self.temp_dir)
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page1_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        plugin.accept_document_list([refs_doc, page1_doc])
        metadata_handlers = register_page_metadata_handlers([plugin])

        plugin.new_page(page1_doc)
        page1_result = apply_metadata_handlers(
            'See <!--REF foo-->', metadata_handlers, page1_doc)
        self.assertTrue(page1_result.deferPage)

        plugin.new_page(refs_doc)
        refs_result = apply_metadata_handlers(
            '<!--REFDEF foo Foo display-->', metadata_handlers, refs_doc)
        self.assertTrue(refs_result.deferPage)

        refs_html = join_parsing_results(
            refs_result.parsingResults, metadata_handlers, refs_doc)
        self.assertIn('page_01.html#backref_ref_foo', refs_html)

        page1_html = join_parsing_results(
            page1_result.parsingResults, metadata_handlers, page1_doc)
        self.assertIn('refs.html#backref_def_foo', page1_html)

        plugin.finalize()
        with open(plugin.backrefs_cache_file, 'r', encoding='utf-8') as file:
            saved = json.load(file)
        self.assertEqual(saved['foo'][0]['input_file'], 'page_01.txt')
        self.assertEqual(saved['foo'][0]['anchor_ids'], ['backref_ref_foo'])


if __name__ == '__main__':
    unittest.main()
