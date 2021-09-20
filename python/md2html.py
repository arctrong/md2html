import argparse
import os
import sys
from pathlib import Path
from subprocess import check_output
import markdown
from string import Template
import re
import json
from datetime import datetime

DEFAULT_TEMPLATE_PATH = '../doc_src/templates/default.html'
DEFAULT_CSS_FILE_PATH = '../doc/styles.css'
USE_HELP_TEXT = 'use -h for help'
PLACEHOLDERS_METADATA_ITEM = 'placeholders'

EXEC_NAME = 'md2html_py'
EXEC_VERSION = '0.1.3'

WORKING_DIR = Path(__file__).resolve().parent
MARKDOWN_CONVERTER = markdown.Markdown(extensions=["extra", "toc", "mdx_emdash",
                                                   "pymdownx.superfences", "admonition"])

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
    When reading ignores the content of those lines whose first non-blank symbol is `comment_char`,
    i.e. it leaves the empty line instead. Then, when a parser points at an error, this error will be
    found at the pointed line in the initial (commented) file.
    """
    lines = []
    with open(file, 'r') as file_handler:
        for line in file_handler:
            if line.strip().startswith(comment_char):
                lines.append(re.sub(r'[^\r\n]', '', line))
            else:
                lines.append(line)
    return ''.join([item for item in lines])


def strip_extension(path):
    return os.path.splitext(path)[0]


def first_not_none(*values):
    return next((v for v in values if v is not None), None)


def extract_metadata_section(text):
    match = re.search('^\\s*(<!--METADATA(.*?)-->)', text, flags=re.IGNORECASE + re.DOTALL)
    return (match.group(2), match.start(1), match.end(1)) if match else (None, 0, 0)


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
                        errors.append(f"Custom template placeholder '{k}' in page metadata is of type "
                                      f"'{type(v).__name__}', not a string, skipping.")
            else:
                errors.append(f"Custom template placeholders in page metadata is of type "
                              f"'{type(placeholders).__name__}', not an object, skipping.")

    except Exception as e:
        errors.append(f'Page metadata cannot be parsed: {type(e).__name__}: {e}')
        return None, errors

    return new_metadata, errors


def parse_md2html_arguments(*args):
    """
    Returns a tuple of (result_type, result), where result_type is one of the 'success', 'help' or 'error'.
    If the result is not successful then all user information messages are already printed into console.
    """

    md2html_args = {}

    def formatter_creator(prog):
        return argparse.HelpFormatter(prog, width=80)

    # noinspection PyTypeChecker
    parser = argparse.ArgumentParser(description='Converts Markdown document into HTML document.',
                                     formatter_class=formatter_creator, add_help=False)
    parser.add_argument("-h", "--help", help="shows this help message and exits", action='store_true')
    parser.add_argument("-i", "--input", help="input Markdown file name (mandatory)", type=str)
    parser.add_argument("--argument-file", help="argument file", type=str)
    parser.add_argument("-o", "--output", help="output HTML file name, defaults to input file name with '.html' "
                                               "extension", type=str)
    parser.add_argument("-t", "--title", help="the HTML page title, if omitted there will be an empty title", type=str)
    parser.add_argument("--template", help="custom template directory", type=str)
    parser.add_argument("--link-css", help="links CSS file, multiple entries allowed", type=str, action='append')
    parser.add_argument("--include-css", help="includes content of the CSS file into HTML, multiple entries allowed",
                        type=str, action='append')
    parser.add_argument("--no-css", help="creates HTML with no CSS. If no CSS-related arguments is specified, "
                                         "the default CSS will be included", action='store_true')
    parser.add_argument("-f", "--force", help="rewrites HTML output file even if it was modified later than the input "
                                              "file", action='store_true')
    parser.add_argument("-v", "--verbose", help="outputs human readable information messages", action='store_true')
    parser.add_argument("-r", "--report", help="if HTML file is generated, outputs the path of this file, "
                                               "incompatible with -v", action='store_true')
    args = parser.parse_args(*args)

    if args.help:
        parser.print_help()
        return 'help', None

    if args.argument_file:
        md2html_args['argument_file'] = Path(args.argument_file)

    if args.input:
        input_file = Path(args.input)
        md2html_args['input_file'] = input_file
    elif not args.argument_file:
        parser.print_usage()
        print(f'Input file is not specified ({USE_HELP_TEXT})')
        return 'error', None

    md2html_args['output_file'] = Path(args.output) if args.output else None

    md2html_args['title'] = args.title

    md2html_args['template'] = Path(args.template) if args.template else None

    md2html_args['no_css'] = True if args.no_css else False
    if args.no_css and (args.link_css or args.include_css):
        parser.print_usage()
        print(f'--no-css argument is not compatible with --link-css and --include-css arguments ({USE_HELP_TEXT})')
        return 'error', None

    md2html_args['link_css'] = args.link_css if args.link_css else []
    md2html_args['include_css'] = [Path(item) for item in args.include_css] if args.include_css else []

    md2html_args['force'] = args.force
    md2html_args['verbose'] = args.verbose
    md2html_args['report'] = args.report

    if md2html_args['report'] and md2html_args['verbose']:
        parser.print_usage()
        print(f'--report and --verbose arguments are not compatible ({USE_HELP_TEXT})')
        return 'error', None

    return 'success', md2html_args


def parse_argument_file(argument_file_string, cli_args):
    """
    Returns a tuple (success, error_message, document_list), where success is ether `True` or `False`.
    """

    document_list = []

    try:
        arguments_item = json.loads(argument_file_string)
    except Exception as e:
        return False, f'Argument file cannot be parsed: {type(e).__name__}: {e}', None

    if not isinstance(arguments_item, dict):
        return False, f"The root element is of type '{type(arguments_item).__name__}', not an object.", None

    if 'default' in arguments_item:
        defaults_item = arguments_item.get('default')
        if not isinstance(defaults_item, dict):
            return False, f"'default' section is of type '{type(defaults_item).__name__}', not an object.", None
    else:
        defaults_item = {}

    documents_item = arguments_item.get('documents')
    if documents_item is None:
        return False, f"'documents' section is absent.", None
    elif not isinstance(documents_item, list):
        return False, f"'documents' section is of type '{type(documents_item).__name__}', not a list.", None

    if 'no-css' in defaults_item and ('link-css' in defaults_item or 'include-css' in defaults_item):
        return (False, f"'no-css' parameter incompatible with one of the ['link-css', 'include-css'] "
                       f"in the 'default' section.", None)

    for document_item in documents_item:
        document = {}

        v = first_not_none(cli_args.get('input_file'), document_item.get("input"), defaults_item.get("input"))
        if v is not None:
            document['input_file'] = Path(v)
        else:
            return False, f"Undefined input file for 'documents' item: {document_item}.", None

        v = first_not_none(cli_args.get('output_file'), document_item.get("output"), defaults_item.get("output"))
        document['output_file'] = Path(v) if v is not None else None

        attr = 'title'
        document[attr] = first_not_none(cli_args.get(attr), document_item.get(attr), defaults_item.get(attr))

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
                return (False, f"'no-css' parameter incompatible with one of "
                               f"[{', '.join([q + a + q for a in link_args])}] "
                               f"in `documents` item: {document_item}.", None)

            no_css = first_not_none(document_item.get('no-css'), defaults_item.get('no-css'), False)

            link_css.extend(first_not_none(document_item.get('link-css'), defaults_item.get('link-css'), []))
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
            return False, f"Incompatible 'report' and 'verbose' parameters for 'documents' item: {document_item}.", None

        document_list.append(document)

    return True, None, document_list


def enrich_document_list(document_list):
    for document in document_list:
        if not document['template']:
            document['template'] = WORKING_DIR.joinpath(DEFAULT_TEMPLATE_PATH)
        if not document['output_file']:
            document['output_file'] = Path(strip_extension(document['input_file']) + '.html')
        if not document['no_css'] and not document['link_css'] and not document['include_css']:
            document['include_css'] = [WORKING_DIR.joinpath(DEFAULT_CSS_FILE_PATH)]


def md2html(**kwargs):
    input_file = kwargs['input_file']
    output_file = kwargs['output_file']
    title = kwargs['title']
    template_file = kwargs['template']
    link_css = kwargs['link_css']
    include_css = kwargs['include_css']
    force = kwargs['force']
    verbose = kwargs['verbose']
    report = kwargs['report']

    if not force and output_file.exists():
        output_file_mtime = os.path.getmtime(output_file)
        input_file_mtime = os.path.getmtime(input_file)
        if output_file_mtime > input_file_mtime:
            if verbose:
                print(f'The output file is up-to-date. Skipping: {output_file}')
            return

    md_lines = read_lines_from_file(input_file)

    substitutions = {'title': title}

    metadata_section, metadata_start, metadata_end = extract_metadata_section(md_lines)
    if metadata_section:
        md_lines = md_lines[:metadata_start] + md_lines[metadata_end:]
        metadata, errors = parse_metadata(metadata_section)
        if verbose:
            for error in errors:
                print('WARNING: ' + error)
        if metadata:
            if substitutions['title'] is None and metadata.get('title') is not None:
                substitutions['title'] = metadata['title']
            if metadata.get(PLACEHOLDERS_METADATA_ITEM):
                for k, v in metadata[PLACEHOLDERS_METADATA_ITEM].items():
                    substitutions[k] = v

    if substitutions['title'] is None:
        substitutions['title'] = ''

    styles = []
    if link_css:
        styles.extend([f'<link rel="stylesheet" type="text/css" href="{item}">' for item in link_css])
    if include_css:
        styles.extend(['<style>\n' + read_lines_from_file(item) + '\n</style>' for item in include_css])
    substitutions['styles'] = '\n'.join(styles) if styles else ''

    # Methods `markdown.markdownFromFile()` and `Markdown.convertFile()` raise errors from their inside
    # implementation. So we are going to use methods `markdown.markdown()` and `Markdown.convert()` instead.
    # And anyway the first two methods read the md-file completely before conversion, so they give
    # no memory save.

    substitutions['content'] = MARKDOWN_CONVERTER.convert(source=md_lines)

    substitutions['exec_name'] = EXEC_NAME
    substitutions['exec_version'] = EXEC_VERSION
    current_time = datetime.today()
    substitutions['generation_date'] = current_time.strftime('%Y-%m-%d')
    substitutions['generation_time'] = current_time.strftime('%H:%M:%S')

    template = Template(read_lines_from_cached_file(template_file))
    result = template.safe_substitute(substitutions)

    with open(output_file, 'w') as result_file:
        result_file.write(result)

    if verbose:
        print(f'Output file generated: {output_file}')
    if report:
        print(output_file)


def main():
    result_type, md2html_args = parse_md2html_arguments(sys.argv[1:])
    if result_type != 'success':
        sys.exit(1)

    argument_file = md2html_args.get('argument_file')
    if argument_file:
        argument_file_string = read_lines_from_commented_file(argument_file)
        success, error_message, document_list = parse_argument_file(argument_file_string, md2html_args)
        if not success:
            print(f"Error parsing argument file '{argument_file}': " + error_message)
            sys.exit(1)
    else:
        document_list = [md2html_args]

    enrich_document_list(document_list)

    for document in document_list:
        md2html(**document)


if __name__ == '__main__':
    main()
