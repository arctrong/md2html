import unittest

from md2html import *
from page_metadata_utils import apply_and_merge_metadata_handlers
from plugins.page_variables_plugin import *
from .utils_for_tests import ANY_DOCUMENT, find_single_instance_of_type, \
    parse_argument_file_for_test


class PageVariablesPluginTest(unittest.TestCase):

    @staticmethod
    def _find_single_plugin(plugins):
        return find_single_instance_of_type(plugins, PageVariablesPlugin)

    def _parse_plugin_data(self, plugin_data):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "about.md"}], "plugins": {"page-variables": ' +
            plugin_data + '}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = self._find_single_plugin(args.plugins)
        metadata_handlers = register_page_metadata_handlers(args.plugins)
        return plugin, metadata_handlers

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "about.md"}], "plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = self._find_single_plugin(args.plugins)
        self.assertIsNone(plugin)

    def test_notActivated_withDefaultMarker(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "about.md"}], "plugins": {"page-variables": {}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = self._find_single_plugin(args.plugins)
        self.assertIsNotNone(plugin)

    def test_singleBlock_complexTest(self):
        plugin, metadata_handlers = self._parse_plugin_data(
            '{"METADATA": { }}')  # "only-at-page-start": true by default

        page_content = '<!--METADATA {"title": "About"}-->other content'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({'title': 'About'}, variables)
        self.assertEqual("other content", result)

        page_content = '  \r\n \t \n   <!--METADATA{"title":"About1" } -->'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({'title': 'About1'}, variables)
        self.assertEqual("  \r\n \t \n   ", result)

        page_content = '  \r\n \t \n  no metadata blocks  '
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({'title': 'About1'},
                             variables)  # that's because the plugin wasn't reset
        self.assertEqual("  \r\n \t \n  no metadata blocks  ", result)
        plugin.new_page(None)  # reset
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({}, variables)
        self.assertEqual("  \r\n \t \n  no metadata blocks  ", result)

    def test_caseInsensitive(self):
        plugin, metadata_handlers = self._parse_plugin_data('{"VariaBLEs": {}}')

        page_content = '<!--variables{ "key":"value" }-->other content'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({"key": "value"}, variables)
        self.assertEqual("other content", result)

    def test_inPageMiddle(self):
        plugin, metadata_handlers = self._parse_plugin_data(
            '{"metadata": {"only-at-page-start": false}}')

        page_content = 'start text <!--metadata{ "logo":"COOL!" }-->other content'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({"logo": "COOL!"}, variables)
        self.assertEqual("start text other content", result)

    def test_multiline(self):
        plugin, metadata_handlers = self._parse_plugin_data(
            '{"variables": {"only-at-page-start": false}}')

        page_content = 'start text <!--variables\n{"key": "value"}\r\n-->\n other content'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({"key": "value"}, variables)
        self.assertEqual("start text \n other content", result)

    def test_wrongMarker(self):
        plugin, metadata_handlers = self._parse_plugin_data(
            '{"variables": {"only-at-page-start": false}}')

        page_content = 'start text<!--metadata{"key":"value"}-->'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({}, variables)
        self.assertEqual('start text<!--metadata{"key":"value"}-->', result)

    def test_severalBlocks(self):
        plugin, metadata_handlers = self._parse_plugin_data(
            '{"variables1": {"only-at-page-start": false}, "metadata1": {}}')

        page_content = '    <!--metadata1{"key": "value"}--> other ' + \
            'text <!--variables1{"question": "answer"} --> some more text'
        plugin.new_page(None)
        result = apply_and_merge_metadata_handlers(page_content, metadata_handlers, ANY_DOCUMENT)
        variables = plugin.variables(ANY_DOCUMENT)
        self.assertDictEqual({"key": "value", "question": "answer"}, variables)
        self.assertEqual('     other text  some more text', result)

    def test_variables_per_document_after_multiple_pages(self):
        """Regression: variables(doc) after phase 1 must not leak across documents """
        """(deferred build)."""
        plugin, metadata_handlers = self._parse_plugin_data('{}')

        doc_referencer = Document(
            input_file='referencer.txt', output_file='referencer.html', title='Referencing page')
        doc_definition = Document(
            input_file='definition.txt', output_file='definition.html', title='Definition page')
        doc_plain = Document(
            input_file='plain.txt', output_file='plain.html', title='Plain page')

        plugin.new_page(doc_referencer)
        apply_and_merge_metadata_handlers(
            '<!--VARIABLES {"title": "Referencing page"}-->\n# Referencer',
            metadata_handlers, doc_referencer)

        plugin.new_page(doc_definition)
        apply_and_merge_metadata_handlers(
            '<!--VARIABLES {"title": "Definition page"}-->\n# Definition',
            metadata_handlers, doc_definition)

        plugin.new_page(doc_plain)
        apply_and_merge_metadata_handlers(
            '<!--VARIABLES {"title": "Plain page"}-->\n# Plain',
            metadata_handlers, doc_plain)

        self.assertEqual({'title': 'Referencing page'}, plugin.variables(doc_referencer))
        self.assertEqual({'title': 'Definition page'}, plugin.variables(doc_definition))
        self.assertEqual({'title': 'Plain page'}, plugin.variables(doc_plain))

        substitutions = {'title': doc_referencer.title}
        substitutions.update(plugin.variables(doc_referencer))
        self.assertEqual('Referencing page', substitutions['title'])


if __name__ == '__main__':
    unittest.main()
