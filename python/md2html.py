import os
import sys
import time
from datetime import timedelta
from pathlib import Path

import markdown

from output_utils import output_page
from plugins_utils import process_plugins, filter_non_blank_plugins
from argument_file_utils import load_json_argument_file, complete_argument_file_processing, \
    merge_and_canonize_argument_file
from models import Arguments
from cli_arguments_utils import parse_cli_arguments, CliError, CliArgDataObject
from page_metadata_utils import register_page_metadata_handlers, apply_metadata_handlers
from utils import UserError, read_lines_from_commented_json_file, read_lines_from_cached_file, \
    relativize_relative_resource

WORKING_DIR = Path(__file__).resolve().parent
MARKDOWN = markdown.Markdown(extensions=["extra", "toc", "mdx_emdash",
                                         "pymdownx.superfences", "admonition"])


def md2html(document, plugins, metadata_handlers, options):

    input_path = Path(document.input_file)
    output_path = Path(document.output_file)
    if not document.force and output_path.exists():
        output_file_mtime = os.path.getmtime(output_path)
        input_file_mtime = os.path.getmtime(input_path)
        if output_file_mtime > input_file_mtime:
            if document.verbose:
                print(f'The output file is up-to-date. Skipping: {document.output_file}')
            return

    for plugin in plugins.values():
        plugin.new_page(document)

    md_lines = read_lines_from_cached_file(document.input_file)
    md_lines = apply_metadata_handlers(md_lines, metadata_handlers, document)
    substitutions = {'content': MARKDOWN.convert(source=md_lines),
                     'source_file': relativize_relative_resource(document.input_file,
                                                                 document.output_file)}

    output_page(document, plugins, substitutions, options)


def parse_argument_file(argument_file_dict: dict, cli_args: CliArgDataObject) -> (Arguments, dict):

    canonized_argument_file = merge_and_canonize_argument_file(argument_file_dict, cli_args)
    plugins = process_plugins(canonized_argument_file['plugins'])
    arguments, document_plugins_items = complete_argument_file_processing(
        canonized_argument_file, plugins)

    # TODO Consider moving this logic inside `complete_argument_file_processing`
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
            # This exception is also raised when the user asks for the help info that may look
            # illogical. But when this program is run from inside a script, this situation must
            # be considered as an error so the error code >0 is always returned.
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
                error_input_file = document.input_file
                raise UserError(f"Error processing input file '{error_input_file}': "
                                f"{type(e).__name__}: {e}")

        for plugin in plugins.values():
            try:
                plugin.finalize(plugins, arguments.options)
            except UserError as e:
                raise UserError(f"Error in after-all-pages-processed action: "
                                f"{type(e).__name__}: {e}")

        if arguments.options.verbose:
            end_moment = time.monotonic()
            print('Finished in: ' + str(timedelta(seconds=end_moment - start_moment)))
    except UserError as ue:
        print(str(ue))
        sys.exit(1)


if __name__ == '__main__':
    main()
