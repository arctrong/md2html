import json
import unittest

from plugins.plugin_utils import list_from_string_or_array, dict_from_string_or_object
from utils import UserError


class PluginUtilTest(unittest.TestCase):

    def test_list_from_string_or_array(self):
        for case in (
            ("empty string", "", [""]),
            ("value as string", " some string ", [" some string "]),
            ("value empty list", "[]", []),
            ("list of one value", "[\"one value\"]", ["one value"]),
            ("list of several values", "[\"value1\", \"value2\"]", ["value1", "value2"]),
        ):
            with self.subTest(test_name=case[0]):
                self.assertListEqual(case[2], list_from_string_or_array(case[1]))

    def test_dict_from_string_or_array(self):
        for case in (
            ("empty string", "", "key", {"key": ""}),
            ("value as string", " some string ", "key", {"key": " some string "}),
            ("value empty dict", "{}", "key", {}),
            ("dict of one value", "{\"key\": \"value\"}", "key", {"key": "value"}),
            ("dict of several values", "{\"k1\": \"v1\", \"k2\": 2, \"k3\": true}", "key",
             {"k1": "v1", "k2": 2, "k3": True}),
        ):
            with self.subTest(test_name=case[0]):
                self.assertDictEqual(case[3], dict_from_string_or_object(case[1], case[2]))

    schema = json.loads("""
            {
                "$schema": "http://json-schema.org/draft-04/schema#",
                "type": "object",
                "properties": {
                    "file": { "type": "string" }
                },
                "required": ["file"]
            }
        """)

    def test_dict_from_string_or_array_with_schema_positive(self):
        self.assertDictEqual({"file": "readme.txt"},
                             dict_from_string_or_object("{\"file\": \"readme.txt\"}", "",
                                                        self.schema))

    def test_dict_from_string_or_array_with_schema_negative(self):
        with self.assertRaises(UserError) as cm:
            dict_from_string_or_object("{\"filename\": \"readme.txt\"}", "", self.schema)
        self.assertIn('Validation error', str(cm.exception))
