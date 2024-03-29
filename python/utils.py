import os
import re
from pathlib import Path


class UserError(Exception):
    """
    Error that was caused by correct processing of incorrect user input or actions.
    The message must be more user-friendly, and the application output may be simpler, e.g.
    without a stack trace.
    """
    pass


CACHED_FILES = {}
JSON_COMMENT_BLANKING_PATTERN = re.compile(r'[^\s]')
REGEX_MASKING_PATTERN = re.compile("([?^\\\\$.|*+\\[\\](){}])")


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
    relative location that being applied on the HTML page `page` will resolve to `resource`.

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
    with open(file, 'r', encoding="utf-8") as file_handler:
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
    with open(file, 'r', encoding="utf-8") as file_handler:
        for line in file_handler:
            lines.append(blank_comment_line(line, comment_char))
    return ''.join(lines)


def strip_extension(path):
    return os.path.splitext(path)[0]


def first_not_none(*values):
    return next((v for v in values if v is not None), None)


def read_lines_from_cached_file(file):

    # TODO Consider adding `encoding` parameter.

    lines = CACHED_FILES.get(file)
    if lines is None:
        lines = read_lines_from_file(file)
        CACHED_FILES[file] = lines
    return lines


class VariableReplacerError(Exception):
    pass


class VariableReplacer:

    TOKEN_MARKER = "$"
    TOKEN_START = "{"
    TOKEN_END = "}"

    def __init__(self, template: str):
        self.parts = []

        state = 0
        token = []
        for char in template:
            if state == 0:
                if char == self.TOKEN_MARKER:
                    state = 1
                else:
                    token.append(char)
            elif state == 1:
                if char == self.TOKEN_MARKER:
                    token.append(self.TOKEN_MARKER)
                    state = 0
                elif char == self.TOKEN_START:
                    self.parts.append("".join(token))
                    token = []
                    state = 2
                else:
                    token.append(char)
                    state = 0
            elif state == 2:
                if char == self.TOKEN_END:
                    index = "".join(token).strip()
                    if not index.isdigit():
                        raise VariableReplacerError(f"Replacement position is not a number: {index}")
                    index = int(index)
                    if index < 1:
                        raise VariableReplacerError(f"Replacement position is less that 1: {index}")
                    self.parts.append(index)
                    token = []
                    state = 0
                else:
                    token.append(char)
        if state > 1:
            raise VariableReplacerError("Matching closing brace not found: " + self.TOKEN_END)
        if state == 1:
            token.append(self.TOKEN_MARKER)
        self.parts.append("".join(token))

    def replace(self, substitutions: list):
        result = []
        for part in self.parts:
            if type(part) == int:
                if len(substitutions) >= part:
                    result.append(substitutions[part - 1])
            else:
                result.append(part)
        return "".join(result)


def mask_regex_chars(string):
    return REGEX_MASKING_PATTERN.sub("\\\\\\1", string)


class SmartSubstringer:

    def __init__(self, start_with, end_with, start_marker, end_marker):
        self.empty = False
        self.start_with = start_with
        self.end_with = end_with
        self.start_marker = start_marker
        self.end_marker = end_marker
        if not (start_with or end_with or start_marker or end_marker):
            self.empty = True
            return
        delimiters = (self.start_with, self.end_with, self.start_marker, self.end_marker)
        delimiters = (delimiter for delimiter in delimiters if delimiter)
        self.pattern = re.compile("|".join(mask_regex_chars(delimiter) for delimiter in delimiters))

    def smart_copy(self, start_with, end_with, start_marker, end_marker):
        start_with = self.start_with if start_with is None else start_with
        end_with = self.end_with if end_with is None else end_with
        start_marker = self.start_marker if start_marker is None else start_marker
        end_marker = self.end_marker if end_marker is None else end_marker
        if (start_with == self.start_with and end_with == self.end_with and
                start_marker == self.start_marker and end_marker == self.end_marker):
            return self
        else:
            return SmartSubstringer(start_with, end_with, start_marker, end_marker)

    def substring(self, string):
        if self.empty:
            return string

        start_position = 0
        end_position = len(string)
        start_with_found = False
        end_with_found = False
        start_marker_found = False
        end_marker_found = False

        for found in self.pattern.finditer(string):
            if found[0] == self.start_with and not start_with_found and not start_marker_found:
                start_with_found = True
                start_position = found.start()
            elif found[0] == self.end_with and not end_with_found and not end_marker_found:
                end_with_found = True
                end_position = found.end()
            elif found[0] == self.start_marker and not start_with_found and not start_marker_found:
                start_marker_found = True
                start_position = found.end()
            elif found[0] == self.end_marker and not end_with_found and not end_marker_found:
                end_marker_found = True
                end_position = found.start()

        if (self.start_with or self.start_marker) and not (start_with_found or start_marker_found):
            return ""
        if start_position > 0 or end_position < len(string):
            return string[start_position:end_position]
        else:
            return string


def _find_first_non_empty_line(string: str):
    start = 0
    pos = -1
    for c in string:
        pos += 1
        if c in ["\r", "\n"]:
            start = pos + 1
        elif not c.isspace():
            return start
    return len(string)


def _find_last_non_empty_line(string: str):
    end = len(string)
    for pos in range(len(string) - 1, -1, -1):
        c = string[pos]
        if c in ["\r", "\n"]:
            end = pos
        elif not c.isspace():
            return end
    return 0


def strip_empty_lines(string: str):
    return string[_find_first_non_empty_line(string):_find_last_non_empty_line(string)]





