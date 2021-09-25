import json
from abc import ABC

from jsonschema import validate


class PluginDataError(ValueError):
    pass


def validate_data(data, schema_file):
    try:
        with open(schema_file, 'r') as schema_file:
            schema = json.load(schema_file)
        validate(instance=data, schema=schema)
    except Exception as e:
        raise PluginDataError(f'Error validating plugin data: {type(e).__name__}: {e}')


class Md2HtmlPlugin(ABC):

    def accept_data(self, data):
        pass

    def variables(self, doc: dict) -> dict:
        return {}
