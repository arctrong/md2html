import os
import re
import sys
import time
from datetime import datetime
from datetime import timedelta
from pathlib import Path

import chevron
import markdown

from argument_file_utils import load_json_argument_file, parse_argument_file_content
from cli_arguments_utils import parse_cli_arguments, CliError
from constants import EXEC_NAME, EXEC_VERSION
from page_metadata_utils import register_page_metadata_handlers, apply_metadata_handlers
from utils import UserError, read_lines_from_file, \
    read_lines_from_commented_json_file, read_lines_from_cached_file

WORKING_DIR = Path(__file__).resolve().parent
MARKDOWN = markdown.Markdown(extensions=["extra", "toc", "mdx_emdash",
                                         "pymdownx.superfences", "admonition"])

LEGACY_PLACEHOLDERS_REPLACEMENT_PATTERN = re.compile(r'(^|[^$])\${([^}]+)}')
LEGACY_PLACEHOLDERS_UNESCAPED_REPLACEMENT_PATTERN = re.compile(r'(^|[^$])\${(styles|content)}')

CACHED_FILES = {}


def read_lines_from_cached_file_legacy(template_file):
    lines = CACHED_FILES.get(template_file)
    if lines is None:
        lines = read_lines_from_file(template_file)
        lines = re.sub(LEGACY_PLACEHOLDERS_UNESCAPED_REPLACEMENT_PATTERN, r'\1{{{\2}}}', lines)
        lines = re.sub(LEGACY_PLACEHOLDERS_REPLACEMENT_PATTERN, r'\1{{\2}}', lines)
        CACHED_FILES[template_file] = lines
    return lines


def md2html(document, plugins, metadata_handlers, options):
    input_location = document['input_file']
    output_location = document['output_file']
    title = document['title']
    template_file = document['template']
    link_css = document['link_css']
    include_css = document['include_css']
    force = document['force']
    verbose = document['verbose']
    report = document['report']

    output_file = Path(output_location)
    input_file = Path(input_location)

    if not force and output_file.exists():
        output_file_mtime = os.path.getmtime(output_file)
        input_file_mtime = os.path.getmtime(input_file)
        if output_file_mtime > input_file_mtime:
            if verbose:
                print(f'The output file is up-to-date. Skipping: {output_location}')
            return

    current_time = datetime.today()
    substitutions = {'title': title, 'exec_name': EXEC_NAME, 'exec_version': EXEC_VERSION,
                     'generation_date': current_time.strftime('%Y-%m-%d'),
                     'generation_time': current_time.strftime('%H:%M:%S')}
    styles = []
    if link_css:
        styles.extend([f'<link rel="stylesheet" type="text/css" href="{item}">'
                       for item in link_css])
    if include_css:
        styles.extend(['<style>\n' + read_lines_from_file(item) + '\n</style>'
                       for item in include_css])
    substitutions['styles'] = '\n'.join(styles) if styles else ''

    md_lines = read_lines_from_file(input_file)
    for plugin in plugins:
        plugin.new_page()
    md_lines = apply_metadata_handlers(md_lines, metadata_handlers, document)

    substitutions['content'] = MARKDOWN.convert(source=md_lines)

    for plugin in plugins:
        substitutions.update(plugin.variables(document))

    if options['legacy_mode']:
        placeholders = substitutions.get('placeholders')
        if placeholders is not None:
            del substitutions['placeholders']
            substitutions.update(placeholders)
        template = read_lines_from_cached_file_legacy(template_file)
    else:
        template = read_lines_from_cached_file(template_file)

    if substitutions['title'] is None:
        substitutions['title'] = ''

    # Methods `markdown.markdownFromFile()` and `Markdown.convertFile()` raise errors from
    # their inside implementation. So we are going to use methods `markdown.markdown()` and
    # `Markdown.convert()` instead. And anyway the first two methods read the md-file
    # completely before conversion, so they give no memory save.

    result = chevron.render(template, substitutions)

    with open(output_file, 'w') as result_file:
        result_file.write(result)

    if verbose:
        print(f'Output file generated: {output_location}')
    if report:
        print(output_location)


def main():
    try:
        start_moment = time.monotonic()

        try:
            cli_args = parse_cli_arguments(sys.argv[1:])
        except CliError:
            sys.exit(1)

        argument_file = cli_args.get('argument_file')
        if argument_file:
            argument_file_string = read_lines_from_commented_json_file(argument_file)
            try:
                argument_file_dict = load_json_argument_file(argument_file_string)
                arguments = parse_argument_file_content(argument_file_dict, cli_args)
            except UserError as e:
                raise UserError(f"Error parsing argument file '{argument_file}': "
                                f"{type(e).__name__}: {e}")
        else:
            page_variables_markers = ["VARIABLES"]
            if cli_args["legacy_mode"]:
                page_variables_markers.append("METADATA")
            argument_file_dict = {"documents": [{}],
                                  # When run without an argument file, need implicitly added
                                  # plugin for extraction of page title from the source text.
                                  "plugins": {"page-variables": {"markers": page_variables_markers,
                                                                 "only-at-page-start": True}}}
            arguments = parse_argument_file_content(argument_file_dict, cli_args)

        metadata_handlers = register_page_metadata_handlers(arguments.plugins)

        for document in arguments.documents:
            md2html(document, arguments.plugins, metadata_handlers, arguments.options)

        if arguments.options["verbose"]:
            end_moment = time.monotonic()
            print('Finished in: ' + str(timedelta(seconds=end_moment - start_moment)))
    except UserError as ue:
        print(str(ue))
        sys.exit(1)


if __name__ == '__main__':
    main()
