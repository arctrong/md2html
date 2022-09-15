import os
import re
from datetime import datetime
from pathlib import Path

import chevron

from constants import EXEC_NAME, EXEC_VERSION
from utils import relativize_relative_resource, read_lines_from_cached_file, UserError, \
    read_lines_from_file

LEGACY_PLACEHOLDERS_UNESCAPED_REPLACEMENT_PATTERN = re.compile(r'(^|[^$])\${(styles|content)}')
LEGACY_PLACEHOLDERS_REPLACEMENT_PATTERN = re.compile(r'(^|[^$])\${([^}]+)}')

CACHED_FILES = {}


def read_lines_from_cached_file_legacy(template_file):
    lines = CACHED_FILES.get(template_file)
    if lines is None:
        lines = read_lines_from_file(template_file)
        lines = re.sub(LEGACY_PLACEHOLDERS_UNESCAPED_REPLACEMENT_PATTERN, r'\1{{{\2}}}', lines)
        lines = re.sub(LEGACY_PLACEHOLDERS_REPLACEMENT_PATTERN, r'\1{{\2}}', lines)
        CACHED_FILES[template_file] = lines
    return lines


def output_page(document, plugins: list, substitutions: dict, options):

    substitutions = substitutions.copy()

    current_time = datetime.today()
    substitutions.update({'title': document.title,
                          'exec_name': EXEC_NAME, 'exec_version': EXEC_VERSION,
                          'generation_date': current_time.strftime('%Y-%m-%d'),
                          'generation_time': current_time.strftime('%H:%M:%S')})

    styles = []
    if document.link_css:
        # TODO Consider applying HTML encoding to the `href` value. Not sure
        #  it's required.
        styles.extend([f'<link rel="stylesheet" type="text/css" '
                       f'href="{relativize_relative_resource(css, document.output_file)}">'
                       for css in document.link_css])
    if document.include_css:
        styles.extend(['<style>\n' + read_lines_from_cached_file(css) + '\n</style>'
                       for css in document.include_css])
    substitutions['styles'] = '\n'.join(styles) if styles else ''

    for plugin in plugins:
        substitutions.update(plugin.variables(document))

    if options.legacy_mode:
        placeholders = substitutions.get('placeholders')
        if placeholders is not None:
            del substitutions['placeholders']
            substitutions.update(placeholders)
        template = read_lines_from_cached_file_legacy(document.template)
    else:
        template = read_lines_from_cached_file(document.template)

    if substitutions['title'] is None:
        substitutions['title'] = ''

    try:
        result = chevron.render(template, substitutions)
    except chevron.ChevronError as e:
        raise UserError(f"Error processing template: {type(e).__name__}: {e}")

    output_dir_path = Path(document.output_file).resolve().parent
    if not output_dir_path.exists():
        os.makedirs(output_dir_path)

    with open(document.output_file, 'w') as result_file:

        # TODO Consider adding `encoding` parameter.

        result_file.write(result)
