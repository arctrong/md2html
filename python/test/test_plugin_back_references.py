import json
import tempfile
import unittest
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

from cli_arguments_utils import CliArgDataObject
from md2html import load_json_argument_file, register_page_metadata_handlers
from models.options import Options
from page_metadata_utils import apply_metadata_handlers, join_parsing_results
from plugins.back_references_plugin import BackReferencesPlugin
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import UserError
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _find_single_plugin(plugins: List[Md2HtmlPlugin]) -> Optional[BackReferencesPlugin]:
    return find_single_instance_of_type(plugins, BackReferencesPlugin)


def _plugin_from_arg_file(arg_file_str: str) -> Tuple[Optional[BackReferencesPlugin], List[str]]:
    args = parse_argument_file_for_test(
        load_json_argument_file(arg_file_str), CliArgDataObject())
    plugin = _find_single_plugin(args.plugins)
    if plugin is None:
        return None, []
    markers = [marker for _, marker, _ in plugin.page_metadata_handlers()]
    return plugin, markers


def _simulate_build(
        arg_file_str: str,
        pages: Sequence[Tuple[int, str]],
) -> Tuple[BackReferencesPlugin, List[str], Dict[int, str], Dict[int, bool]]:
    args = parse_argument_file_for_test(
        load_json_argument_file(arg_file_str), CliArgDataObject())
    plugin = _find_single_plugin(args.plugins)
    metadata_handlers = register_page_metadata_handlers(args.plugins)
    plugin.accept_document_list(args.documents)
    plugin.accept_app_data([], Options(), metadata_handlers)

    output: Dict[int, str] = {}
    deferred = {}
    deferred_pages: Dict[int, bool] = {}
    for index, text in pages:
        doc = args.documents[index]
        plugin.new_page(doc)
        result = apply_metadata_handlers(text, metadata_handlers, doc)
        deferred_pages[index] = result.deferPage
        if result.deferPage:
            deferred[index] = result
        else:
            output[index] = join_parsing_results(
                result.parsingResults, metadata_handlers, doc)
    for index, result in deferred.items():
        doc = args.documents[index]
        output[index] = join_parsing_results(
            result.parsingResults, metadata_handlers, doc)

    markers = [marker for _, marker, _ in plugin.page_metadata_handlers()]
    return plugin, markers, output, deferred_pages


class BackReferencesPluginTest(unittest.TestCase):

    def setUp(self):
        self._temp_dir = tempfile.TemporaryDirectory()
        self.temp_dir_name = self._temp_dir.name

    def tearDown(self):
        self._temp_dir.cleanup()

    def test_minimal_config(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {}}}')
        plugin, markers, html, _ = _simulate_build(arg_file_str, [
            (0, 'Def <!--refdef example [Example Domain](https://example.com/)-->'),
            (1, 'See <!--ref example-->.'),
        ])

        self.assertFalse(plugin.is_blank())
        self.assertEqual(sorted(markers), ['REF', 'REFDEF'])
        self.assertEqual(
            html[0],
            'Def <a name="backref_def_example"></a><span class="ref-def">[example]</span> '
            '[Example Domain](https://example.com/)'
            '<sup><a class="ref" href="ref.html#backref_ref_example">1</a></sup>')
        self.assertEqual(
            html[1],
            'See [Example Domain](https://example.com/)'
            '<sup><a name="backref_ref_example"></a>'
            '<a class="ref" href="def.html#backref_def_example">[example]</a></sup>.')

    def test_full_config(self):
        cache_file = str(
            Path(self.temp_dir_name).joinpath('full_config_cache.json')).replace('\\', '/')
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {'
            '"code-prefix": "xref_",'
            '"cache": "' + cache_file + '",'
            '"def-formats": [{'
            '    "markers": ["bibdef"],'
            '    "template": "<div id=\\"${anchor}\\">${content}<sup>${back_refs_html}</sup></div>",'
            '    "back-ref-template": "<a class=\\"bib-back\\" href=\\"${href}\\">${link_text}</a>",'
            '    "back-ref-delimiter": "; "'
            '}],'
            '"ref-formats": [{'
            '    "markers": ["citeref"],'
            '    "template": "<cite><a name=\\"${anchor}\\"></a><a href=\\"${href}\\">${code}</a></cite>"'
            '}]'
            '}}}')
        plugin, markers, html, _ = _simulate_build(arg_file_str, [
            (0, 'Book <!--bibdef src1 Some book text-->'),
            (1, 'Cite <!--citeref src1-->.'),
        ])

        self.assertFalse(plugin.is_blank())
        self.assertEqual(sorted(markers), ['BIBDEF', 'CITEREF'])
        self.assertEqual(
            html[0],
            'Book <div id="xref_def_src1">Some book text'
            '<sup><a class="bib-back" href="ref.html#xref_ref_src1">1</a></sup></div>')
        self.assertEqual(
            html[1],
            'Cite <cite><a name="xref_ref_src1"></a>'
            '<a href="def.html#xref_def_src1">src1</a></cite>.')

    def test_invalid_template_syntax(self):
        arg_file_str = (
            '{"documents": [{"input": "page.txt", "output": "page.html"}], '
            '"plugins": {"back-references": {'
            '"ref-formats": [{"markers": ["REF"], "template": "start${}end"}]'
            '}}}')
        with self.assertRaises(UserError) as cm:
            parse_argument_file_for_test(load_json_argument_file(arg_file_str),
                                         CliArgDataObject())
        self.assertIn('empty placeholder', str(cm.exception).lower())

    def test_empty_format_arrays(self):
        page_doc = '{"input": "page.txt", "output": "page.html"}'
        cases = [
            ('{"ref-formats": []}', False, ['REFDEF']),
            ('{"def-formats": []}', False, ['REF']),
            ('{"def-formats": [], "ref-formats": []}', True, []),
        ]
        for plugin_config, blank, expected_markers in cases:
            with self.subTest(plugin_config=plugin_config):
                arg_file_str = (
                    '{"documents": [' + page_doc + '], '
                    '"plugins": {"back-references": ' + plugin_config + '}}')
                plugin, markers = _plugin_from_arg_file(arg_file_str)
                if blank:
                    self.assertIsNone(plugin)
                else:
                    self.assertIsNotNone(plugin)
                    self.assertFalse(plugin.is_blank())
                self.assertEqual(sorted(markers), sorted(expected_markers))

    def test_duplicate_markers(self):
        page_doc = '{"input": "page.txt", "output": "page.html"}'
        plugin_configs = [
            '{"def-formats": [{"markers": ["REFDEF", "refdef"]}]}',
            '{"ref-formats": [{"markers": ["REF", "ref"]}]}',
            '{"def-formats": [{"markers": ["REFDEF"]}], '
            '"ref-formats": [{"markers": ["refdef"]}]}',
        ]
        for plugin_config in plugin_configs:
            with self.subTest(plugin_config=plugin_config):
                arg_file_str = (
                    '{"documents": [' + page_doc + '], '
                    '"plugins": {"back-references": ' + plugin_config + '}}')
                with self.assertRaises(UserError) as cm:
                    parse_argument_file_for_test(
                        load_json_argument_file(arg_file_str), CliArgDataObject())
                self.assertIn('duplication', str(cm.exception).lower())

    def test_refdef_then_ref_with_def_first(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {}}}')
        _, _, html, _ = _simulate_build(arg_file_str, [
            (0, 'Entry <!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
        ])
        self.assertEqual(
            html[0],
            'Entry <a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Foo display'
            '<sup><a class="ref" href="ref.html#backref_ref_foo">1</a></sup>')
        self.assertEqual(
            html[1],
            'See Foo display<sup><a name="backref_ref_foo"></a>'
            '<a class="ref" href="def.html#backref_def_foo">[foo]</a></sup>.')

    def test_ref_then_refdef_with_ref_first_should_defer(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "ref.txt", "output": "ref.html"},'
            '  {"input": "def.txt", "output": "def.html"}'
            '], "plugins": {"back-references": {}}}')
        _, _, html, deferred_pages = _simulate_build(arg_file_str, [
            (0, 'See <!--ref foo-->.'),
            (1, '<!--refdef foo Foo display-->'),
        ])
        self.assertTrue(deferred_pages[0])
        self.assertTrue(deferred_pages[1])
        self.assertEqual(
            html[0],
            'See Foo display<sup><a name="backref_ref_foo"></a>'
            '<a class="ref" href="def.html#backref_def_foo">[foo]</a></sup>.')
        self.assertEqual(
            html[1],
            '<a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Foo display'
            '<sup><a class="ref" href="ref.html#backref_ref_foo">1</a></sup>')

    def test_ref_to_undefined_def_should_fail(self):
        arg_file_base = (
            '{"documents": %s, "plugins": {"back-references": {}}}')
        cases = [
            ('[{"input": "ref.txt", "output": "ref.html"}]', [(0, 'See <!--ref missing-->.')]),
            ('[{"input": "ref.txt", "output": "ref.html"},'
             ' {"input": "other.txt", "output": "other.html"}]',
             [(0, 'See <!--ref missing-->.'), (1, 'Other page.')]),
        ]
        for documents_json, pages in cases:
            with self.subTest(documents_json=documents_json):
                arg_file_str = arg_file_base % documents_json
                with self.assertRaises(UserError) as cm:
                    _simulate_build(arg_file_str, pages)
                message = str(cm.exception).lower()
                self.assertIn('referenced but not defined', message)
                self.assertIn('missing', message)

    def test_duplicate_refdef_should_fail(self):
        arg_file_base = (
            '{"documents": %s, "plugins": {"back-references": {}}}')
        cases = [
            ('[{"input": "def.txt", "output": "def.html"}]',
             [(0, 'A <!--refdef foo One--> B <!--refdef foo Two-->')]),
            ('[{"input": "def1.txt", "output": "def1.html"},'
             ' {"input": "def2.txt", "output": "def2.html"}]',
             [(0, '<!--refdef foo One-->'), (1, '<!--refdef foo Two-->')]),
        ]
        for documents_json, pages in cases:
            with self.subTest(documents_json=documents_json):
                arg_file_str = arg_file_base % documents_json
                with self.assertRaises(UserError) as cm:
                    _simulate_build(arg_file_str, pages)
                message = str(cm.exception).lower()
                self.assertIn('defined multiple times', message)
                self.assertIn('foo', message)

    def test_ref_wrong_field_count_should_fail(self):
        arg_file_str = (
            '{"documents": [{"input": "ref.txt", "output": "ref.html"}], '
            '"plugins": {"back-references": {}}}')
        cases = [
            'See <!--ref foo compact-->.',
            'See <!--ref-->.',
        ]
        for page_text in cases:
            with self.subTest(page_text=page_text):
                with self.assertRaises(UserError) as cm:
                    _simulate_build(arg_file_str, [(0, page_text)])
                self.assertIn('single word', str(cm.exception).lower())

    def test_multiple_refs_to_same_def_on_same_page(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {}}}')
        _, _, html, deferred_pages = _simulate_build(arg_file_str, [
            (0, '<!--refdef code1 [Example.com](https://example.com/)-->'),
            (1, 'First <!--ref code1--> and second <!--ref code1-->.'),
        ])
        self.assertTrue(deferred_pages[0])
        self.assertFalse(deferred_pages[1])
        self.assertEqual(
            html[1],
            'First [Example.com](https://example.com/)'
            '<sup><a name="backref_ref_code1"></a>'
            '<a class="ref" href="def.html#backref_def_code1">[code1]</a></sup>'
            ' and second [Example.com](https://example.com/)'
            '<sup><a name="backref_ref_code1_1"></a>'
            '<a class="ref" href="def.html#backref_def_code1">[code1]</a></sup>.')
        self.assertEqual(
            html[0],
            '<a name="backref_def_code1"></a><span class="ref-def">[code1]</span> '
            '[Example.com](https://example.com/)<sup>'
            '<a class="ref" href="ref.html#backref_ref_code1">1</a>, '
            '<a class="ref" href="ref.html#backref_ref_code1_1">2</a>'
            '</sup>')

    def test_ref_and_def_with_relative_output_paths(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "defs/subdir_def.txt", "output": "defs/subdir_def.html"},'
            '  {"input": "pages/subdir_ref.txt", "output": "pages/subdir_ref.html"}'
            '], "plugins": {"back-references": {}}}')
        _, _, html, deferred_pages = _simulate_build(arg_file_str, [
            (0, '<!--refdef subdir_example [Subdir Example](https://example.org/subdir/)-->'),
            (1, 'See <!--ref subdir_example-->.'),
        ])
        self.assertTrue(deferred_pages[0])
        self.assertFalse(deferred_pages[1])
        self.assertEqual(
            html[1],
            'See [Subdir Example](https://example.org/subdir/)'
            '<sup><a name="backref_ref_subdir_example"></a>'
            '<a class="ref" href="../defs/subdir_def.html#backref_def_subdir_example">'
            '[subdir_example]</a></sup>.')
        self.assertEqual(
            html[0],
            '<a name="backref_def_subdir_example"></a>'
            '<span class="ref-def">[subdir_example]</span> '
            '[Subdir Example](https://example.org/subdir/)'
            '<sup><a class="ref" href="../pages/subdir_ref.html#backref_ref_subdir_example">'
            '1</a></sup>')

    def test_custom_ref_template_and_code_prefix(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {'
            '"code-prefix": "xref_",'
            '"ref-formats": [{'
            '    "markers": ["REF"],'
            '    "template": "<span data-code=\\"${code}\\" data-anchor=\\"${anchor}\\" '
            'href=\\"${href}\\">${content}</span>"'
            '}]'
            '}}}')
        _, _, html, _ = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
        ])
        self.assertEqual(
            html[1],
            'See <span data-code="foo" data-anchor="xref_ref_foo" '
            'href="def.html#xref_def_foo">Foo display</span>.')

    def test_custom_def_templates_and_back_ref_delimiter(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref1.txt", "output": "ref1.html"},'
            '  {"input": "ref2.txt", "output": "ref2.html"}'
            '], "plugins": {"back-references": {'
            '"def-formats": [{'
            '    "markers": ["REFDEF"],'
            '    "template": "<div data-code=\\"${code}\\" id=\\"${anchor}\\">${content}'
            '<sup>${back_refs_html}</sup></div>",'
            '    "back-ref-template": "<a href=\\"${href}\\">${link_text}</a>",'
            '    "back-ref-delimiter": "; "'
            '}]'
            '}}}')
        _, _, html, deferred_pages = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
            (2, 'Also <!--ref foo-->.'),
        ])
        self.assertTrue(deferred_pages[0])
        self.assertFalse(deferred_pages[1])
        self.assertFalse(deferred_pages[2])
        self.assertEqual(
            html[0],
            '<div data-code="foo" id="backref_def_foo">Foo display'
            '<sup><a href="ref1.html#backref_ref_foo">1</a>; '
            '<a href="ref2.html#backref_ref_foo">2</a></sup></div>')

    def test_two_ref_markers_use_respective_templates(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "defs.txt", "output": "defs.html"},'
            '  {"input": "ref.txt", "output": "ref.html"},'
            '  {"input": "cite.txt", "output": "cite.html"}'
            '], "plugins": {"back-references": {'
            '"def-formats": [{"markers": ["REFDEF"]}],'
            '"ref-formats": ['
            '    {"markers": ["REF"], "template": "<ref>${code}</ref>"},'
            '    {"markers": ["CITEREF"], "template": "<cite>${code}</cite>"}'
            ']'
            '}}}')
        _, markers, html, deferred_pages = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo-->\n<!--refdef bar Bar-->'),
            (1, 'Ref <!--ref foo-->.'),
            (2, 'Cite <!--citeref bar-->.'),
        ])
        self.assertCountEqual(markers, ['CITEREF', 'REF', 'REFDEF'])
        self.assertTrue(deferred_pages[0])
        self.assertFalse(deferred_pages[1])
        self.assertFalse(deferred_pages[2])
        self.assertEqual(html[1], 'Ref <ref>foo</ref>.')
        self.assertEqual(html[2], 'Cite <cite>bar</cite>.')

    def test_empty_ref_template_renders_nothing(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {'
            '"ref-formats": [{"markers": ["REF"], "template": ""}]'
            '}}}')
        _, _, html, _ = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
        ])
        self.assertEqual(html[1], 'See .')

    def test_partial_def_format_inherits_default_back_ref_template(self):
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {'
            '"def-formats": [{'
            '    "markers": ["REFDEF"],'
            '    "template": "<wrap>${content}<sup>${back_refs_html}</sup></wrap>"'
            '}]'
            '}}}')
        _, _, html, _ = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
        ])
        self.assertEqual(
            html[0],
            '<wrap>Foo display'
            '<sup><a class="ref" href="ref.html#backref_ref_foo">1</a></sup></wrap>')

    def test_first_run_with_cache_enabled_creates_cache_file(self):
        cache_file = (str(Path(self.temp_dir_name).joinpath('back_refs_cache.json'))
                      .replace('\\', '/'))
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {'
            '"cache": "' + cache_file + '"'
            '}}}')
        plugin, _, _, _ = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
        ])
        plugin.finalize()

        cache_path = Path(cache_file)
        self.assertTrue(cache_path.exists())
        with open(cache_path, 'r', encoding='utf-8') as file:
            saved = json.load(file)
        self.assertIn('foo', saved)
        self.assertEqual(saved['foo'][0]['input_file'], 'ref.txt')
        self.assertEqual(saved['foo'][0]['anchor_ids'], ['backref_ref_foo'])

    def test_second_run_rebuilt_referencer_preserves_skipped_back_links(self):
        cache_file = (str(Path(self.temp_dir_name).joinpath('back_refs_cache.json'))
                      .replace('\\', '/'))
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref1.txt", "output": "ref1.html"},'
            '  {"input": "ref2.txt", "output": "ref2.html"}'
            '], "plugins": {"back-references": {'
            '"cache": "' + cache_file + '"'
            '}}}')
        first_run_pages = [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo--> (one).'),
            (2, 'See <!--ref foo--> (two).'),
        ]
        plugin, _, _, _ = _simulate_build(arg_file_str, first_run_pages)
        plugin.finalize()

        _, _, html, _ = _simulate_build(arg_file_str, [
            (1, 'See <!--ref foo--> (one rebuilt).'),
            (0, '<!--refdef foo Foo display-->'),
        ])
        self.assertEqual(
            html[0],
            '<a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Foo display'
            '<sup><a class="ref" href="ref2.html#backref_ref_foo">1</a>, '
            '<a class="ref" href="ref1.html#backref_ref_foo">2</a></sup>')

    def test_second_run_if_ref_page_gains_second_ref_then_def_lists_both_anchors(self):
        cache_file = (str(Path(self.temp_dir_name).joinpath('back_refs_cache.json'))
                      .replace('\\', '/'))
        arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref.txt", "output": "ref.html"}'
            '], "plugins": {"back-references": {'
            '"cache": "' + cache_file + '"'
            '}}}')
        plugin, _, _, _ = _simulate_build(arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo-->.'),
        ])
        plugin.finalize()

        _, _, html, _ = _simulate_build(arg_file_str, [
            (1, 'First <!--ref foo--> and second <!--ref foo-->.'),
            (0, '<!--refdef foo Foo display-->'),
        ])
        self.assertEqual(
            html[0],
            '<a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Foo display'
            '<sup><a class="ref" href="ref.html#backref_ref_foo">1</a>, '
            '<a class="ref" href="ref.html#backref_ref_foo_1">2</a></sup>')

    def test_second_run_smaller_document_list_drops_removed_referencer_and_prunes_cache(self):
        cache_file = (str(Path(self.temp_dir_name).joinpath('back_refs_cache.json'))
                      .replace('\\', '/'))
        full_arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref1.txt", "output": "ref1.html"},'
            '  {"input": "ref2.txt", "output": "ref2.html"}'
            '], "plugins": {"back-references": {'
            '"cache": "' + cache_file + '"'
            '}}}')
        plugin, _, _, _ = _simulate_build(full_arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
            (1, 'See <!--ref foo--> (one).'),
            (2, 'See <!--ref foo--> (two).'),
        ])
        plugin.finalize()

        smaller_arg_file_str = (
            '{"documents": ['
            '  {"input": "def.txt", "output": "def.html"},'
            '  {"input": "ref1.txt", "output": "ref1.html"}'
            '], "plugins": {"back-references": {'
            '"cache": "' + cache_file + '"'
            '}}}')
        plugin, _, html, _ = _simulate_build(smaller_arg_file_str, [
            (0, '<!--refdef foo Foo display-->'),
        ])
        self.assertEqual(
            html[0],
            '<a name="backref_def_foo"></a><span class="ref-def">[foo]</span> Foo display'
            '<sup><a class="ref" href="ref1.html#backref_ref_foo">1</a></sup>')
        plugin.finalize()

        with open(cache_file, 'r', encoding='utf-8') as file:
            saved = json.load(file)
        self.assertIn('foo', saved)
        input_files = [entry['input_file'] for entry in saved['foo']]
        self.assertEqual(input_files, ['ref1.txt'])


if __name__ == '__main__':
    unittest.main()
