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
from plugins.back_references_plugin import BackReferencesPlugin, _DefMetadataHandler, \
    _RefMetadataHandler, _normalize_loaded_cache, PageLocation, Reference, \
    _build_reverse_map_references_by_page_from_cache, _prepare_cache_for_save
from utils import UserError, VariableReplacerError
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


def _def_handler(plugin, marker='REFDEF'):
    marker = marker.upper()
    for fmt in plugin.def_formats:
        if marker in fmt.markers:
            return _DefMetadataHandler(plugin, fmt)
    raise ValueError(f"No def-format registered for marker '{marker}'")


def _ref_handler(plugin, marker='REF'):
    marker = marker.upper()
    for fmt in plugin.ref_formats:
        if marker in fmt.markers:
            return _RefMetadataHandler(plugin, fmt)
    raise ValueError(f"No ref-format registered for marker '{marker}'")


def _accept_def_metadata(plugin, doc, metadata, metadata_section='<!--REFDEF-->', phase=1,
                         data_from_prev_phase=None):
    return _def_handler(plugin).accept_page_metadata(
        doc, 'REFDEF', metadata, metadata_section, phase=phase,
        data_from_prev_phase=data_from_prev_phase)


def _accept_ref_metadata(plugin, doc, metadata, metadata_section='<!--REF-->', phase=1,
                         data_from_prev_phase=None):
    return _ref_handler(plugin).accept_page_metadata(
        doc, 'REF', metadata, metadata_section, phase=phase,
        data_from_prev_phase=data_from_prev_phase)


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
        _accept_ref_metadata(
            plugin, page1_doc, 'foo', '<!--REF foo-->', phase=1)

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
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.new_page(page_doc)
        _accept_ref_metadata(plugin, page_doc, 'foo', '<!--REF foo-->', phase=1)

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
        _accept_ref_metadata(plugin, page_doc, 'foo', '<!--REF foo-->', phase=1)
        _accept_ref_metadata(plugin, page_doc, 'foo', '<!--REF foo-->', phase=1)

        record = plugin.backrefs_cache['foo']['page_01.txt']
        self.assertEqual(record['input_file'], 'page_01.txt')
        self.assertEqual(len(record['anchor_ids']), 2)

    def test_prepare_cache_for_save_sorts_sources_and_pages(self):
        saved = _prepare_cache_for_save({
            'zebra': {
                'page_b.txt': {
                    'input_file': 'page_b.txt',
                    'output_file': 'doc/page_b.html',
                    'anchor_ids': ['ref_b1', 'ref_b2'],
                },
                'page_a.txt': {
                    'input_file': 'page_a.txt',
                    'output_file': 'doc/page_a.html',
                    'anchor_ids': ['ref_a1'],
                },
            },
            'alpha': {
                'page_02.txt': {
                    'input_file': 'page_02.txt',
                    'output_file': 'doc/page_02.html',
                    'anchor_ids': ['ref_2'],
                },
                'page_01.txt': {
                    'input_file': 'page_01.txt',
                    'output_file': 'doc/page_01.html',
                    'anchor_ids': ['ref_1a', 'ref_1b'],
                },
            },
        })

        self.assertEqual(list(saved.keys()), ['alpha', 'zebra'])
        self.assertEqual(
            [record['input_file'] for record in saved['alpha']],
            ['page_01.txt', 'page_02.txt'],
        )
        self.assertEqual(
            [record['input_file'] for record in saved['zebra']],
            ['page_a.txt', 'page_b.txt'],
        )
        self.assertEqual(saved['alpha'][0]['anchor_ids'], ['ref_1a', 'ref_1b'])
        self.assertEqual(saved['zebra'][0]['anchor_ids'], ['ref_a1'])
        self.assertEqual(saved['zebra'][1]['anchor_ids'], ['ref_b1', 'ref_b2'])

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
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        _accept_def_metadata(
            plugin, refs_doc, 'bar Bar display', '<!--REFDEF bar Bar display-->', phase=1)

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
        result = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        html = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->',
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

    def test_duplicate_markers_must_raise_error(self):
        for plugin_definition in [
            {'def-formats': [{'markers': ['REFDEF', 'refdef']}]},
            {'ref-formats': [{'markers': ['REF', 'ref']}]},
            {'def-formats': [{'markers': ['REFDEF']}],
             'ref-formats': [{'markers': ['refdef']}]},
        ]:
            with self.subTest(plugin_definition):
                plugin = BackReferencesPlugin()
                with self.assertRaises(UserError) as cm:
                    plugin.accept_data(plugin_definition)
                self.assertIn('duplication', str(cm.exception).lower())

    def test_ref_metadata_with_format_field_must_raise_error(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({})
        page_doc = Document(input_file='page.txt', output_file='page.html')
        plugin.new_page(page_doc)
        with self.assertRaises(UserError) as cm:
            _accept_ref_metadata(plugin, page_doc, 'foo compact', '<!--REF foo compact-->', phase=1)
        self.assertIn('exactly one field', str(cm.exception))

    def test_custom_back_ref_delimiter(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'def-formats': [{
                'markers': ['REFDEF'],
                'back-ref-delimiter': '; ',
            }],
        })
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page1_doc = Document(input_file='page_01.txt', output_file='doc/page_01.html')
        page2_doc = Document(input_file='page_02.txt', output_file='doc/page_02.html')
        plugin.accept_document_list([refs_doc, page1_doc, page2_doc])
        plugin.references['foo'] = {
            'page_01.txt': [Reference(
                PageLocation('page_01.txt', 'doc/page_01.html'), 'backref_ref_foo')],
            'page_02.txt': [Reference(
                PageLocation('page_02.txt', 'doc/page_02.html'), 'backref_ref_foo_2')],
        }

        plugin.new_page(refs_doc)
        result = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        html = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->',
            phase=2, data_from_prev_phase=result.result).result

        self.assertIn('; ', html)
        self.assertNotIn(', ', html.split('Foo display')[-1])

    def test_custom_ref_template_uses_named_placeholders_from_config(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'ref-formats': [{
                'markers': ['REF'],
                'template': (
                    '<span data-code="${code}" data-anchor="${anchor}" '
                    'href="${href}">${content}</span>'),
            }],
        })
        refs_doc = Document(input_file='refs.txt', output_file='refs.html')
        page_doc = Document(input_file='page.txt', output_file='page.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin.new_page(refs_doc)
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.new_page(page_doc)
        html = _accept_ref_metadata(
            plugin, page_doc, 'foo', '<!--REF foo-->', phase=1).result

        self.assertIn('data-code="foo"', html)
        self.assertIn('data-anchor="backref_ref_foo"', html)
        self.assertIn('href="refs.html#backref_def_foo"', html)
        self.assertIn('>Foo display</span>', html)

    def test_custom_def_template_uses_named_placeholders_from_config(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'def-formats': [{
                'markers': ['REFDEF'],
                'template': (
                    '<div data-code="${code}" id="${anchor}">${content}'
                    '<sup>${back_refs_html}</sup></div>'),
                'back-ref-template': '<a href="${href}">${link_text}</a>',
            }],
        })
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page_doc = Document(input_file='page.txt', output_file='doc/page.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin.references['foo'] = {
            'page.txt': [Reference(
                PageLocation('page.txt', 'doc/page.html'), 'backref_ref_foo')],
        }

        plugin.new_page(refs_doc)
        phase1 = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        html = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->',
            phase=2, data_from_prev_phase=phase1.result).result

        self.assertIn('data-code="foo"', html)
        self.assertIn('id="backref_def_foo"', html)
        self.assertIn('>Foo display<sup>', html)
        self.assertIn('href="page.html#backref_ref_foo">1</a>', html)

    def test_code_prefix_from_config(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({'code-prefix': 'xref_'})
        refs_doc = Document(input_file='refs.txt', output_file='refs.html')
        page_doc = Document(input_file='page.txt', output_file='page.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin.new_page(refs_doc)
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.new_page(page_doc)
        html = _accept_ref_metadata(
            plugin, page_doc, 'foo', '<!--REF foo-->', phase=1).result

        self.assertIn('xref_ref_foo', html)
        self.assertIn('refs.html#xref_def_foo', html)

    def test_multiple_ref_formats_use_respective_templates(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'def-formats': [{'markers': ['REFDEF']}],
            'ref-formats': [
                {'markers': ['REF'], 'template': '<ref>${code}</ref>'},
                {'markers': ['CITEREF'], 'template': '<cite>${code}</cite>'},
            ],
        })
        refs_doc = Document(input_file='refs.txt', output_file='refs.html')
        page_ref_doc = Document(input_file='page_ref.txt', output_file='page_ref.html')
        page_cite_doc = Document(input_file='page_cite.txt', output_file='page_cite.html')
        plugin.accept_document_list([refs_doc, page_ref_doc, page_cite_doc])
        plugin.new_page(refs_doc)
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo', '<!--REFDEF foo Foo-->', phase=1)
        _accept_def_metadata(
            plugin, refs_doc, 'bar Bar', '<!--REFDEF bar Bar-->', phase=1)

        plugin.new_page(page_ref_doc)
        ref_html = _ref_handler(plugin, 'REF').accept_page_metadata(
            page_ref_doc, 'REF', 'foo', '<!--REF foo-->', phase=1).result
        plugin.new_page(page_cite_doc)
        cite_html = _ref_handler(plugin, 'CITEREF').accept_page_metadata(
            page_cite_doc, 'CITEREF', 'bar', '<!--CITEREF bar-->', phase=1).result

        self.assertEqual('<ref>foo</ref>', ref_html)
        self.assertEqual('<cite>bar</cite>', cite_html)

    def test_positional_placeholders_in_custom_template_are_not_substituted(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'ref-formats': [{
                'markers': ['REF'],
                'template': 'pos=${1} named=${code}',
            }],
        })
        refs_doc = Document(input_file='refs.txt', output_file='refs.html')
        page_doc = Document(input_file='page.txt', output_file='page.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin.new_page(refs_doc)
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.new_page(page_doc)
        html = _accept_ref_metadata(
            plugin, page_doc, 'foo', '<!--REF foo-->', phase=1).result

        self.assertEqual('pos= named=foo', html)

    def test_custom_templates_loaded_from_argument_file(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '  {"input": "refs.txt", "output": "refs.html"},'
            '  {"input": "page.txt", "output": "page.html"}'
            '], "plugins": {"back-references": {'
            '  "code-prefix": "cfg_",'
            '  "ref-formats": [{"markers": ["REF"], "template": "<r code=\\"${code}\\">${content}</r>"}]'
            '}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        metadata_handlers = register_page_metadata_handlers(args.plugins)

        refs_doc = args.documents[0]
        page_doc = args.documents[1]
        plugin.new_page(refs_doc)
        apply_metadata_handlers(
            '<!--REFDEF foo Foo display-->', metadata_handlers, refs_doc)
        plugin.new_page(page_doc)
        page_result = apply_metadata_handlers(
            'See <!--REF foo-->', metadata_handlers, page_doc)
        html = join_parsing_results(page_result.parsingResults, metadata_handlers, page_doc)

        self.assertEqual('cfg_', plugin.code_prefix)
        self.assertIn('<r code="foo">Foo display</r>', html)

    def test_invalid_template_in_config_raises_at_accept_data(self):
        plugin = BackReferencesPlugin()
        with self.assertRaises(VariableReplacerError):
            plugin.accept_data({
                'ref-formats': [{'markers': ['REF'], 'template': 'start${}end'}],
            })

    def test_def_format_partial_entry_inherits_default_back_ref_template(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'def-formats': [{
                'markers': ['REFDEF'],
                'template': '<wrap>${content}<sup>${back_refs_html}</sup></wrap>',
            }],
        })
        refs_doc = Document(input_file='refs.txt', output_file='doc/refs.html')
        page_doc = Document(input_file='page.txt', output_file='doc/page.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin.references['foo'] = {
            'page.txt': [Reference(
                PageLocation('page.txt', 'doc/page.html'), 'backref_ref_foo')],
        }

        plugin.new_page(refs_doc)
        phase1 = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        html = _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->',
            phase=2, data_from_prev_phase=phase1.result).result

        self.assertIn('<wrap>Foo display<sup>', html)
        self.assertIn('class="ref" href="page.html#backref_ref_foo">1</a>', html)

    def test_omitted_format_arrays_use_defaults(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({})
        self.assertEqual(plugin.def_formats[0].markers, ['REFDEF'])
        self.assertEqual(plugin.ref_formats[0].markers, ['REF'])

    def test_empty_ref_formats_disables_ref_side(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({'ref-formats': []})
        self.assertEqual(len(plugin.ref_formats), 0)
        self.assertEqual(plugin.def_formats[0].markers, ['REFDEF'])
        self.assertFalse(plugin.is_blank())
        handlers = plugin.page_metadata_handlers()
        self.assertEqual([marker for _, marker, _ in handlers], ['REFDEF'])

    def test_empty_def_formats_disables_def_side(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({'def-formats': []})
        self.assertEqual(len(plugin.def_formats), 0)
        self.assertEqual(plugin.ref_formats[0].markers, ['REF'])
        self.assertFalse(plugin.is_blank())

    def test_both_format_arrays_empty_makes_plugin_blank(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({'def-formats': [], 'ref-formats': []})
        self.assertTrue(plugin.is_blank())
        self.assertEqual(plugin.page_metadata_handlers(), [])

    def test_empty_ref_template_renders_nothing(self):
        plugin = BackReferencesPlugin()
        plugin.accept_data({
            'ref-formats': [{'markers': ['REF'], 'template': ''}],
        })
        refs_doc = Document(input_file='refs.txt', output_file='refs.html')
        page_doc = Document(input_file='page.txt', output_file='page.html')
        plugin.accept_document_list([refs_doc, page_doc])
        plugin.new_page(refs_doc)
        _accept_def_metadata(
            plugin, refs_doc, 'foo Foo display', '<!--REFDEF foo Foo display-->', phase=1)
        plugin.new_page(page_doc)
        result = _accept_ref_metadata(plugin, page_doc, 'foo', '<!--REF foo-->', phase=1)
        self.assertEqual(result.result, '')


if __name__ == '__main__':
    unittest.main()
