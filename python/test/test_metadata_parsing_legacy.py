import unittest

from argument_file_utils import *
from plugins.page_variables_plugin import PageVariablesPlugin
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _parse_metadata(metadata):
    argument_file_dict = load_json_argument_file(
        '{"documents": [{"input": "about.md"}], '
        '"plugins": {"page-variables": {"VARIABLES": {"only-at-page-start": false}}}}')
    args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
    plugin = find_single_instance_of_type(args.plugins, PageVariablesPlugin)
    metadata_handlers = register_page_metadata_handlers(args.plugins)
    page_content = 'text before<!--VARIABLES ' + metadata + '-->text after'
    plugin.new_page({})
    apply_metadata_handlers(page_content, metadata_handlers, args.documents[0])
    return plugin.variables({})


class PageMetadataUtilsTest(unittest.TestCase):
    """
    Reusing legacy page metadata parsing tests.
    """

    def test_notObject(self):
        with self.assertRaises(UserError) as cm:
            _parse_metadata('[]')
        self.assertIn('object', str(cm.exception))

    def test_emptyString(self):
        with self.assertRaises(UserError) as cm:
            _parse_metadata('')
        self.assertIn('JSON', str(cm.exception))

    def test_malformedJson(self):
        with self.assertRaises(UserError) as cm:
            _parse_metadata('not a Json')
        self.assertIn('JSON', str(cm.exception))

    def test_emptyObject(self):
        metadata = _parse_metadata('{}')
        self.assertTrue(isinstance(metadata, dict))
        self.assertNotIn('title', metadata)
        self.assertNotIn('placeholders', metadata)

    def test_correctTitle(self):
        metadata = _parse_metadata('{"title": "My title"}')
        self.assertEqual('My title', metadata['title'])

    def test_emptyTitle(self):
        metadata = _parse_metadata('{"title": ""}')
        self.assertEqual('', metadata['title'])

    def test_unicodeEntitiesInStrings(self):
        metadata = _parse_metadata('{\"title\":\"<!\\u002D-value-\\u002D>\"}')
        self.assertEqual('<!--value-->', metadata['title'])

    def test_keysCaseSensitive(self):
        metadata = _parse_metadata('{"Title": "correct title value"}')
        self.assertIn('Title', metadata)
        self.assertNotIn('title', metadata)

    def test_customTemplatePlaceholders_empty(self):
        metadata = _parse_metadata('{ "placeholders": {} }')
        self.assertDictEqual({"placeholders": {}}, metadata)

    def test_customTemplatePlaceholders_correctItems(self):
        metadata = _parse_metadata('{ "placeholders": {"ph1": "val1", "ph2": "val2"} }')
        self.assertDictEqual({'placeholders': {'ph1': 'val1', 'ph2': 'val2'}}, metadata)
