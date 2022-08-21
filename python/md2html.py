import os
import re
import sys
import time
from datetime import datetime
from datetime import timedelta
from pathlib import Path

import chevron
import markdown

from argument_file_plugins_utils import process_plugins, filter_non_blank_plugins
from argument_file_utils import load_json_argument_file, complete_argument_file_processing, \
    merge_and_canonize_argument_file, Arguments
from cli_arguments_utils import parse_cli_arguments, CliError, CliArgDataObject
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
    input_location = document['input']
    output_location = document['output']
    title = document['title']
    template_file = document['template']
    link_css = document['link-css']
    include_css = document['include-css']
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

    md_lines = read_lines_from_cached_file(str(input_file))
    for plugin in plugins.values():
        plugin.new_page(document)
    md_lines = apply_metadata_handlers(md_lines, metadata_handlers, document)

    substitutions['content'] = MARKDOWN.convert(source=md_lines)

    for plugin in plugins.values():
        substitutions.update(plugin.variables(document))

    if options['legacy-mode']:
        placeholders = substitutions.get('placeholders')
        if placeholders is not None:
            del substitutions['placeholders']
            substitutions.update(placeholders)
        template = read_lines_from_cached_file_legacy(template_file)
    else:
        template = read_lines_from_cached_file(template_file)

    if substitutions['title'] is None:
        substitutions['title'] = ''

    try:
        result = chevron.render(template, substitutions)
    except chevron.ChevronError as e:
        raise UserError(f"Error processing template: {type(e).__name__}: {e}")

    output_path = Path(output_file).resolve().parent
    if not output_path.exists():
        os.makedirs(output_path)

    with open(output_file, 'w') as result_file:
        result_file.write(result)

    if verbose:
        print(f'Output file generated: {output_location}')
    if report:
        print(output_location)


def parse_argument_file(argument_file_dict: dict, cli_args: CliArgDataObject) -> (Arguments, dict):
    canonized_argument_file = merge_and_canonize_argument_file(argument_file_dict, cli_args)
    if canonized_argument_file["options"]['legacy-mode']:
        # noinspection PyTypeChecker
        page_variables = canonized_argument_file["plugins"].setdefault("page-variables", {})
        page_variables.setdefault("METADATA", {"only-at-page-start": True})

    plugins = process_plugins(canonized_argument_file['plugins'])
    arguments, document_plugins_items = complete_argument_file_processing(
        canonized_argument_file, plugins)

    for plugin_name, plugin_data in document_plugins_items.items():
        plugin = plugins.get(plugin_name)
        if plugin is not None:
            plugin.accept_data(plugin_data)

    for plugin in plugins.values():
        plugin.initialize(argument_file_dict, cli_args, plugins)

    # Removing "blank" plugins may be done only here because at the earlier steps they
    # are not completely defined.
    plugins = filter_non_blank_plugins(plugins)

    return arguments, plugins


def main():
    try:
        start_moment = time.monotonic()

        try:
            cli_args = parse_cli_arguments(sys.argv[1:])
        except CliError:
            # This exception is also raised when the user asks for the help info.
            # But when this program is run from inside a script such situation must be
            # considered as an error so the error code >0 is always returned.
            sys.exit(1)

        if cli_args.argument_file:
            argument_file_string = read_lines_from_commented_json_file(cli_args.argument_file)
            try:
                argument_file_dict = load_json_argument_file(argument_file_string)
            except UserError as e:
                raise UserError(f"Error reading argument file '{cli_args.argument_file}': "
                                f"{type(e).__name__}: {e}")
        else:
            argument_file_dict = {"documents": [{}],
                                  # When run without argument file, need to implicitly add
                                  # plugin for page title extraction from the source text.
                                  "plugins": {"page-variables": {
                                          "VARIABLES": {"only-at-page-start": True}}}
                                  }

        try:
            arguments, plugins = parse_argument_file(argument_file_dict, cli_args)
        except UserError as e:
            raise UserError(f"Error parsing argument file '{cli_args.argument_file}': "
                            f"{type(e).__name__}: {e}")

        metadata_handlers = register_page_metadata_handlers(plugins)

        for document in arguments.documents:
            try:
                md2html(document, plugins, metadata_handlers, arguments.options)
            except UserError as e:
                error_input_file = document["input"]
                raise UserError(f"Error processing input file '{error_input_file}': "
                                f"{type(e).__name__}: {e}")

        for plugin in plugins.values():
            try:
                plugin.finalize(argument_file_dict, cli_args, plugins)
            except UserError as e:
                raise UserError(f"Error in after-all-pages-processed action: "
                                f"{type(e).__name__}: {e}")

        if arguments.options["verbose"]:
            end_moment = time.monotonic()
            print('Finished in: ' + str(timedelta(seconds=end_moment - start_moment)))
    except UserError as ue:
        print(str(ue))
        sys.exit(1)


if __name__ == '__main__':
    main()
