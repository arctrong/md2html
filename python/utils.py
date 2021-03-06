import os
import re
from pathlib import Path


class UserError(Exception):
    """
    Error that was caused by correct processing of incorrect user input or actions.
    The message must be more user friendly, and the application output may be simpler, e.g.
    without a stack trace.
    """
    pass


CACHED_FILES = {}
JSON_COMMENT_BLANKING_PATTERN = re.compile(r'[^\s]')


def reduce_json_validation_error_message(error_message: str) -> str:
    return error_message.splitlines()[0]


def relativize_relative_resource_path(path: str, page: str):
    """
    The `page` argument is an HTML page, so it cannot be empty or end with a '/'.
    The `path` argument is a relative path to a place where HTML page resources (like other pages,
    pictures, CSS files etc.) can be allocated. So the `path` argument must end with '/' or be
    empty so that it can be used in substitutions like `f'{path}styles.css'`.

    The method considers the both arguments being relative to the same location. It returns the
    path that being applied from the HTML page `page` will lead to `path`. The result will match
    the same requirements as the path` argument matches, i.e. it will be empty or end with '/'.

    ATTENTION! This method wasn't tested with ABSOLUTE paths as any of the arguments.
    """
    page = (page or '').replace('\\', '/')
    if not page or page.endswith('/'):
        raise ValueError(f"'page' argument is not a relatively located resource: {page}")
    path = (path or '').replace('\\', '/')
    if path and not path.endswith('/') or path == '/':
        raise ValueError(f"'path' argument is not a relative resource path: {path}")

    base_path = Path(page).resolve()
    if len(base_path.parents) < 1:
        return path
    relative_path = str(os.path.relpath(Path(path), base_path.parent)).replace('\\', '/')
    if not relative_path or relative_path in ['./', '.']:
        return ''
    return relative_path if relative_path.endswith('/') else relative_path + '/'


def relativize_relative_resource(resource, page):
    """
    The `page` argument is an HTML page.
    The `resource` argument is a relative location of an HTML page resource (like another page,
    a picture, a CSS file etc.). So the both arguments cannot be empty or end with a '/'.

    The method considers the both arguments being relative to the same location. It returns the
    relative location that being applied on the HTML page `page` will resolve to `path`.

    ATTENTION! This method wasn't tested with ABSOLUTE paths as any of the arguments.
    """
    page = (page or '').replace('\\', '/')
    if not page or page.endswith('/'):
        raise ValueError(f"'page' argument is not a relatively located resource: {page}")
    resource = (resource or '').replace('\\', '/')
    if not resource or resource.endswith('/'):
        raise ValueError(f"'page' argument is not a relatively located resource: {resource}")

    base_path = Path(page).resolve()
    if len(base_path.parents) < 1:
        return resource
    return str(os.path.relpath(resource, base_path.parent)).replace('\\', '/')


def read_lines_from_file(file):
    with open(file, 'r') as file_handler:
        return file_handler.read()


def blank_comment_line(line, comment_char):
    if line.strip().startswith(comment_char):
        return JSON_COMMENT_BLANKING_PATTERN.sub(' ', line)
    else:
        return line


def read_lines_from_commented_json_file(file, comment_char='#'):
    """
    When reading replaces with spaces the content of those lines whose first non-blank symbol is
    `comment_char`.
    Then, when a parser points at an error, this error will be found at the
    pointed line and at the pointed position in the initial (commented) file.

    NOTE. JSON syntax does not allow comments. In this application, this function was added
    for convenience.
    """
    lines = []
    with open(file, 'r') as file_handler:
        for line in file_handler:
            lines.append(blank_comment_line(line, comment_char))
    return ''.join(lines)


def strip_extension(path):
    return os.path.splitext(path)[0]


def first_not_none(*values):
    return next((v for v in values if v is not None), None)


def read_lines_from_cached_file(file):
    lines = CACHED_FILES.get(file)
    if lines is None:
        lines = read_lines_from_file(file)
        CACHED_FILES[file] = lines
    return lines
