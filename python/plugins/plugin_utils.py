import json
from json.decoder import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from utils import UserError, reduce_json_validation_error_message

MODULE_DIR = Path(__file__).resolve().parent

with open(MODULE_DIR.joinpath('string_or_array_schema.json'), 'r',
          encoding="utf-8") as schema_file:
    STRING_OR_ARRAY_METADATA_SCHEMA = json.load(schema_file)


def list_from_string_or_array(string: str):
    global STRING_OR_ARRAY_METADATA_SCHEMA
    if string.startswith('['):
        try:
            result = json.loads(string)
            validate(instance=result, schema=STRING_OR_ARRAY_METADATA_SCHEMA)
        except JSONDecodeError as e:
            raise UserError(f"Incorrect JSON: {type(e).__name__}: {str(e)}")
        except ValidationError as e:
            raise UserError(f"Validation error: {type(e).__name__}: " +
                            reduce_json_validation_error_message(str(e)))
    else:
        result = [string]
    return result


def dict_from_string_or_object(string: str, key: str, schema=None):
    if string.startswith('{'):
        try:
            result = json.loads(string)
            if schema:
                validate(instance=result, schema=schema)
        except JSONDecodeError as e:
            raise UserError(f"Incorrect JSON: {type(e).__name__}: {str(e)}")
        except ValidationError as e:
            raise UserError(f"Validation error: {type(e).__name__}: " +
                            reduce_json_validation_error_message(str(e)))
    else:
        result = {key: string}
    return result
