from pathlib import Path

from cli_arguments_utils import CliArgDataObject
from md2html import parse_argument_file
from models.arguments import Arguments


def find_single_instance_of_type(instances: list, instance_type: type):
    result = None
    for instance in instances:
        if isinstance(instance, instance_type):
            if result is None:
                result = instance
            else:
                raise Exception(f"More than one instances of "
                                f"type '{type(instance).__name__}' found.")
    return result


def relative_to_current_dir(path: Path):
    from_curr_dir = str(path.resolve().relative_to(Path("").absolute())).replace("\\", "/")
    if not from_curr_dir or from_curr_dir in ['./', '.']:
        return ''
    else:
        return from_curr_dir if from_curr_dir.endswith('/') else from_curr_dir + '/'


def parse_argument_file_for_test(argument_file_dict: dict, cli_args: CliArgDataObject) -> Arguments:
    return parse_argument_file(argument_file_dict, cli_args)
