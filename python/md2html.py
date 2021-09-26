import argparse
import json
import os
import re
import sys
import time
from datetime import datetime
from datetime import timedelta
from json.decoder import JSONDecodeError
from pathlib import Path

import chevron
import markdown
from jsonschema import validate, ValidationError

from page_metadata_utils import register_page_metadata_handlers, apply_metadata_handlers
from plugins.page_flows_plugin import PageFlowsPlugin
from plugins.page_variables_plugin import PageVariablesPlugin
from plugins.relative_paths_plugin import RelativePathsPlugin
from utils import UserError, reduce_json_validation_error_message


class Arguments:
    def __init__(self, options, documents, plugins):
        self.options = options
        self.documents = documents
        self.plugins = plugins


DEFAULT_TEMPLATE_PATH = '../doc_src/templates/default.html'
DEFAULT_CSS_FILE_PATH = '../doc/styles.css'
USE_HELP_TEXT = 'use -h for help'
PLACEHOLDERS_METADATA_ITEM = 'placeholders'

ARGUMENTS_DOCUMENT_LIST_SECTION = "document-list"
ARGUMENTS_PLUGINS_SECTION = "plugins"
ARGUMENTS_OPTIONS_SECTION = "options"

EXEC_NAME = 'md2html_py'
EXEC_VERSION = '0.1.3'

WORKING_DIR = Path(__file__).resolve().parent
MARKDOWN = markdown.Markdown(extensions=["extra", "toc", "mdx_emdash",
                                         "pymdownx.superfences", "admonition"])
PLUGINS = {'relative-paths': RelativePathsPlugin(), "page-flows": PageFlowsPlugin(),
           'page-variables': PageVariablesPlugin()}
CACHED_FILES = {}


def read_lines_from_file(file):
    with open(file, 'r') as file_handler:
        return file_handler.read()


def read_lines_from_cached_file(file):
    lines = CACHED_FILES.get(file)
    if lines is None:
        lines = read_lines_from_file(file)
        CACHED_FILES[file] = lines
    return lines


def read_lines_from_commented_file(file, comment_char='#'):
    """
    When reading replaces with spaces the content of those lines whose first non-blank symbol is
    `comment_char`. Then, when a parser points at an error, this error will be found at the
    pointed line and at the pointed position in the initial (commented) file.
    """
    lines = []
    with open(file, 'r') as file_handler:
        for line in file_handler:
            if line.strip().startswith(comment_char):
                lines.append(re.sub(r'[^\s]', ' ', line))
            else:
                lines.append(line)
    return ''.join(lines)


def strip_extension(path):
    return os.path.splitext(path)[0]


def first_not_none(*values):
    return next((v for v in values if v is not None), None)


def parse_metadata(metadata_section):
    errors = []
    new_metadata = {}
    try:
        metadata = json.loads(metadata_section)

        if not isinstance(metadata, dict):
            errors.append(f"The root element of the page metadata is of type "
                          f"'{type(metadata).__name__}', not an object, skipping.")
            return None, errors

        if 'title' in metadata:
            title_item = metadata['title']
            if isinstance(title_item, str):
                new_metadata['title'] = title_item
            else:
                errors.append(f"Title in page metadata is of type '{type(title_item).__name__}', "
                              f"not a string, skipping.")

        if PLACEHOLDERS_METADATA_ITEM in metadata:
            placeholders = metadata.get(PLACEHOLDERS_METADATA_ITEM)
            if isinstance(placeholders, dict):
                new_dict = {}
                new_metadata[PLACEHOLDERS_METADATA_ITEM] = new_dict
                for k, v in placeholders.items():
                    if isinstance(v, str):
                        new_dict[k] = v
                    else:
                        errors.append(f"Custom template placeholder '{k}' in page metadata is "
                                      f"of type '{type(v).__name__}', not a string, skipping.")
            else:
                errors.append(f"Custom template placeholders in page metadata is of type "
                              f"'{type(placeholders).__name__}', not an object, skipping.")

    except Exception as e:
        errors.append(f'Page metadata cannot be parsed: {type(e).__name__}: {e}')
        return None, errors

    return new_metadata, errors


def parse_md2html_arguments(*args):
    """
    Returns a tuple of (result_type, result), where result_type is one of the 'success',
    'help' or 'error'. If the result is not successful then all user information messages are
    already printed into console.
    """

    md2html_args = {}

    def formatter_creator(prog):
        return argparse.HelpFormatter(prog, width=80)

    # noinspection PyTypeChecker
    parser = argparse.ArgumentParser(description='Converts Markdown document into HTML document.',
                                     formatter_class=formatter_creator, add_help=False)
    parser.add_argument("-h", "--help", help="shows this help message and exits",
                        action='store_true')
    parser.add_argument("-i", "--input", help="input Markdown file name (mandatory)", type=str)
    parser.add_argument("--argument-file", help="argument file", type=str)
    parser.add_argument("-o", "--output", help="output HTML file name, defaults to input file name"
                                               "with '.html' extension", type=str)
    parser.add_argument("-t", "--title", help="the HTML page title, if omitted there will be an "
                                              "empty title", type=str)
    parser.add_argument("--template", help="custom template directory", type=str)
    parser.add_argument("--link-css", help="links CSS file, multiple entries allowed", type=str,
                        action='append')
    parser.add_argument("--include-css", help="includes content of the CSS file into HTML, "
                                              "multiple entries allowed", type=str, action='append')
    parser.add_argument("--no-css", help="creates HTML with no CSS. If no CSS-related arguments "
                                         "is specified, the default CSS will be included",
                        action='store_true')
    parser.add_argument("-f", "--force", help="rewrites HTML output file even if it was modified "
                                              "later than the input file", action='store_true')
    parser.add_argument("-v", "--verbose", help="outputs human readable information messages",
                        action='store_true')
    parser.add_argument("-r", "--report", help="if HTML file is generated, outputs the path of "
                                               "this file, incompatible with -v",
                        action='store_true')
    args = parser.parse_args(*args)

    if args.help:
        parser.print_help()
        return 'help', None

    if args.argument_file:
        md2html_args['argument_file'] = Path(args.argument_file)

    if args.input:
        md2html_args['input_file'] = args.input
    elif not args.argument_file:
        parser.print_usage()
        print(f'Input file is not specified ({USE_HELP_TEXT})')
        return 'error', None

    md2html_args['output_file'] = args.output

    md2html_args['title'] = args.title

    md2html_args['template'] = Path(args.template) if args.template else None

    md2html_args['no_css'] = True if args.no_css else False
    if args.no_css and (args.link_css or args.include_css):
        parser.print_usage()
        print(f'--no-css argument is not compatible with --link-css and --include-css '
              f'arguments ({USE_HELP_TEXT})')
        return 'error', None

    md2html_args['link_css'] = args.link_css if args.link_css else []
    md2html_args['include_css'] = [Path(item) for item in
                                   args.include_css] if args.include_css else []

    md2html_args['force'] = args.force
    md2html_args['verbose'] = args.verbose
    md2html_args['report'] = args.report

    if md2html_args['report'] and md2html_args['verbose']:
        parser.print_usage()
        print(f'--report and --verbose arguments are not compatible ({USE_HELP_TEXT})')
        return 'error', None

    return 'success', md2html_args


def load_json_argument_file(argument_file_string) -> dict:
    try:
        arguments_item = json.loads(argument_file_string)
    except JSONDecodeError as e:
        raise UserError(f"Error loading JSON argument file: {type(e).__name__}: {e}")
    try:
        schema = json.loads(
            read_lines_from_commented_file(WORKING_DIR.joinpath('args_file_schema.json')))
        validate(instance=arguments_item, schema=schema)
    except ValidationError as e:
        raise UserError(f"Error validating argument file content: {type(e).__name__}: " +
                        reduce_json_validation_error_message(str(e)))

    return arguments_item


def parse_argument_file_content(argument: dict, cli_args: dict) -> Arguments:

    documents = []
    plugins = []
    options = {}
    documents_page_flows = {}

    plugins_item = argument.get('plugins')
    page_flows_plugin = None
    # Even if all page flows are defined in the 'documents' section, at least empty 'page-flows'
    # plugin must be defined in order to activate page flows processing.
    page_flows_plugin_defined = False
    if plugins_item is not None:
        page_flows_plugin = PLUGINS.get("page-flows")
        page_flows_plugin_defined = "page-flows" in plugins_item

    if 'options' in argument:
        options = argument['options']
        if 'verbose' in options:
            if cli_args.get("report"):
                raise UserError("'verbose' parameter in 'options' section is incompatible "
                                "with '--report' command line argument.")
        else:
            options["verbose"] = cli_args["verbose"]
    else:
        options["verbose"] = cli_args["verbose"]

    defaults_item = argument.get('default')
    if defaults_item is None:
        defaults_item = {}

    documents_item = argument['documents']

    if 'no-css' in defaults_item and (
            'link-css' in defaults_item or 'include-css' in defaults_item):
        raise UserError(f"'no-css' parameter incompatible with one of the ['link-css', "
                        f"'include-css'] in the 'default' section.")

    for document_item in documents_item:
        document = {}

        v = first_not_none(cli_args.get('input_file'), document_item.get("input"),
                           defaults_item.get("input"))
        if v is not None:
            document['input_file'] = v
        else:
            raise UserError(f"Undefined input file for 'documents' item: {document_item}.")

        v = first_not_none(cli_args.get('output_file'), document_item.get("output"),
                           defaults_item.get("output"))
        document['output_file'] = v

        attr = 'title'
        document[attr] = first_not_none(cli_args.get(attr), document_item.get(attr),
                                        defaults_item.get(attr), '')
        attr = 'template'
        v = first_not_none(cli_args.get(attr), document_item.get(attr), defaults_item.get(attr))
        document[attr] = Path(v) if v is not None else None

        link_css = []
        include_css = []
        no_css = False
        if cli_args.get('no_css') or cli_args.get('link_css') or cli_args.get('include_css'):
            if cli_args.get('no_css'):
                no_css = True
            else:
                link_css.extend(first_not_none(cli_args.get('link_css'), []))
                include_css.extend(first_not_none(cli_args.get('include_css'), []))
        else:
            link_args = ["link-css", "add-link-css", "include-css", "add-include-css"]
            if 'no-css' in document_item and any(document_item.get(k) for k in link_args):
                q = '\''
                raise UserError(f"'no-css' parameter incompatible with one of "
                                f"[{', '.join([q + a + q for a in link_args])}] "
                                f"in `documents` item: {document_item}.")

            no_css = first_not_none(document_item.get('no-css'), defaults_item.get('no-css'), False)

            link_css.extend(first_not_none(document_item.get('link-css'),
                                           defaults_item.get('link-css'), []))
            link_css.extend(first_not_none(document_item.get('add-link-css'), []))
            include_css.extend(first_not_none(document_item.get('include-css'),
                                              defaults_item.get('include-css'), []))
            include_css.extend(first_not_none(document_item.get('add-include-css'), []))

            if link_css or include_css:
                no_css = False

        document['link_css'] = link_css
        document['include_css'] = include_css
        document['no_css'] = no_css

        attr = 'force'
        document[attr] = first_not_none(True if cli_args.get(attr) else None,
                                        document_item.get(attr), defaults_item.get(attr), False)
        attr = 'verbose'
        document[attr] = first_not_none(True if cli_args.get(attr) else None,
                                        document_item.get(attr), defaults_item.get(attr), False)
        attr = 'report'
        document[attr] = first_not_none(True if cli_args.get(attr) else None,
                                        document_item.get(attr), defaults_item.get(attr), False)

        if document['report'] and document['verbose']:
            raise UserError(f"Incompatible 'report' and 'verbose' parameters for 'documents' "
                            f"item: {document_item}.")

        enrich_document(document)

        if page_flows_plugin_defined:
            if ((1 if 'no-page-flows' in document_item else 0) +
                    (1 if 'page-flows' in document_item else 0) +
                    (1 if 'add-page-flows' in document_item else 0) > 1):
                raise UserError(f"Incompatible 'no-page-flows', 'page-flows' and 'add-page-flows' "
                                f"parameters for 'documents' item: {document_item}.")
            attr = 'page-flows'
            page_flows = first_not_none([] if document_item.get('no-page-flows') else
                                        document_item.get(attr), defaults_item.get(attr), [])
            for page_flow in page_flows:
                page_flow_list = documents_page_flows.setdefault(page_flow, [])
                page_flow_list.append({"link": document["output_file"], "title": document["title"]})
            add_page_flows = first_not_none(document_item.get("add-page-flows"), [])
            for page_flow in add_page_flows:
                page_flow_list = documents_page_flows.setdefault(page_flow, [])
                page_flow_list.append({"link": document["output_file"], "title": document["title"]})

        documents.append(document)

    if documents_page_flows and page_flows_plugin_defined:
        page_flows_plugin.accept_data(documents_page_flows)

    plugins_item = argument.get('plugins')
    if plugins_item is not None:
        for k, v in plugins_item.items():
            plugin = PLUGINS.get(k)
            if plugin:
                try:
                    if plugin.accept_data(v):
                        plugins.append(plugin)
                except UserError as e:
                    raise UserError(f"Error initializing plugin '{k}': {type(e).__name__}: {e}")

    return Arguments(options, documents, plugins)


def enrich_document(document):
    if not document['template']:
        document['template'] = WORKING_DIR.joinpath(DEFAULT_TEMPLATE_PATH)
    if not document['output_file']:
        document['output_file'] = str(Path(strip_extension(document['input_file']) + '.html')
                                      ).replace('\\', '/')
    if not document['no_css'] and not document['link_css'] and not document['include_css']:
        document['include_css'] = [WORKING_DIR.joinpath(DEFAULT_CSS_FILE_PATH)]


def md2html(document, plugins, metadata_handlers):
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

    md_lines = read_lines_from_file(input_file)
    for plugin in plugins:
        plugin.new_page()
    md_lines = apply_metadata_handlers(md_lines, metadata_handlers, output_location)

    substitutions = {'title': title}
    for plugin in plugins:
        substitutions.update(plugin.variables(document))

    if substitutions['title'] is None:
        substitutions['title'] = ''

    styles = []
    if link_css:
        styles.extend([f'<link rel="stylesheet" type="text/css" href="{item}">'
                       for item in link_css])
    if include_css:
        styles.extend(['<style>\n' + read_lines_from_file(item) + '\n</style>'
                       for item in include_css])
    substitutions['styles'] = '\n'.join(styles) if styles else ''

    # Methods `markdown.markdownFromFile()` and `Markdown.convertFile()` raise errors from
    # their inside implementation. So we are going to use methods `markdown.markdown()` and
    # `Markdown.convert()` instead. And anyway the first two methods read the md-file
    # completely before conversion, so they give no memory save.

    substitutions['content'] = MARKDOWN.convert(source=md_lines)

    substitutions['exec_name'] = EXEC_NAME
    substitutions['exec_version'] = EXEC_VERSION
    current_time = datetime.today()
    substitutions['generation_date'] = current_time.strftime('%Y-%m-%d')
    substitutions['generation_time'] = current_time.strftime('%H:%M:%S')

    result = chevron.render(read_lines_from_cached_file(template_file), substitutions)

    with open(output_file, 'w') as result_file:
        result_file.write(result)

    if verbose:
        print(f'Output file generated: {output_location}')
    if report:
        print(output_location)


def main():
    start_moment = time.monotonic()

    result_type, md2html_args = parse_md2html_arguments(sys.argv[1:])
    if result_type != 'success':
        sys.exit(1)

    argument_file = md2html_args.get('argument_file')
    if argument_file:
        argument_file_string = read_lines_from_commented_file(argument_file)
        try:
            argument_file_dict = load_json_argument_file(argument_file_string)
            arguments = parse_argument_file_content(argument_file_dict, md2html_args)
        except UserError as e:
            raise UserError(f"Error parsing argument file '{argument_file}': {type(e).__name__}: "
                            f"{e}")
    else:
        arguments = parse_argument_file_content({"documents": [{}]}, md2html_args)

    metadata_handlers = register_page_metadata_handlers(arguments.plugins)

    for document in arguments.documents:
        md2html(document, arguments.plugins, metadata_handlers)

    if arguments.options["verbose"]:
        end_moment = time.monotonic()
        print('Finished in: ' + str(timedelta(seconds=end_moment - start_moment)))


if __name__ == '__main__':
    try:
        main()
    except UserError as ue:
        print(str(ue))
        sys.exit(2)
