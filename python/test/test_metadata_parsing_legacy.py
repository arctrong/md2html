import sys
import unittest
from pathlib import Path

from argument_file_plugins_utils import process_plugins
from .utils_for_tests import find_single_instance_of_type

sys.path.append(Path(__file__).resolve().parent.parent)
from page_metadata_utils import *
from argument_file_utils import *
from plugins.page_variables_plugin import PageVariablesPlugin


class PageMetadataUtilsTest(unittest.TestCase):
    """
    Reusing legacy page metadata parsing tests.
    """

    def _parse_metadata(self, metadata):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "about.md"}], '
            '"plugins": {"page-variables": {"VARIABLES": {"only-at-page-start": false}}}}')
        parse_argument_file_content(argument_file_dict, {})
        plugins = process_plugins(argument_file_dict['plugins'])
        metadata_handlers = register_page_metadata_handlers(plugins)
        plugin = find_single_instance_of_type(plugins, PageVariablesPlugin)
        page_content = 'text before<!--VARIABLES ' + metadata + '-->text after'
        plugin.new_page({})
        result = apply_metadata_handlers(page_content, metadata_handlers, {})
        return plugin.variables({})

    def test_notObject(self):
        with self.assertRaises(UserError) as cm:
            metadata = self._parse_metadata('[]')
        self.assertIn('object', str(cm.exception))
  
    def test_emptyString(self):
        with self.assertRaises(UserError) as cm:
            metadata = self._parse_metadata('')
        self.assertIn('JSON', str(cm.exception))

    def test_malformedJson(self):
        with self.assertRaises(UserError) as cm:
            metadata = self._parse_metadata('not a Json')
        self.assertIn('JSON', str(cm.exception))

    def test_emptyObject(self):
        metadata = self._parse_metadata('{}')
        self.assertTrue(isinstance(metadata, dict))
        self.assertNotIn('title', metadata)
        self.assertNotIn('placeholders', metadata)

    def test_correctTitle(self):
        metadata = self._parse_metadata('{"title": "My title"}')
        self.assertEqual('My title', metadata['title'])

    def test_emptyTitle(self):
        metadata = self._parse_metadata('{"title": ""}')
        self.assertEqual('', metadata['title'])

    def test_unicodeEntitiesInStrings(self):
        metadata = self._parse_metadata('{\"title\":\"<!\\u002D-value-\\u002D>\"}')
        self.assertEqual('<!--value-->', metadata['title'])

    def test_keysCaseSensitive(self):
        metadata = self._parse_metadata('{"Title": "correct title value"}')
        self.assertIn('Title', metadata)
        self.assertNotIn('title', metadata)

    def test_customTemplatePlaceholders_empty(self):
        metadata = self._parse_metadata('{ "placeholders": {} }')
        self.assertDictEqual({"placeholders": {}}, metadata)

    def test_customTemplatePlaceholders_correctItems(self):
        metadata = self._parse_metadata('{ "placeholders": {"ph1": "val1", "ph2": "val2"} }')
        self.assertDictEqual({'placeholders': {'ph1': 'val1', 'ph2': 'val2'}}, metadata)


if __name__ == '__main__':
    unittest.main()
