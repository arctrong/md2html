import argparse
import os
import sys
from pathlib import Path
from subprocess import check_output
import markdown
from string import Template
import re
import json

DEFAULT_TEMPLATE_DIR = '../md2html_templates/default'
TEMPLATE_FILE_NAME = 'template.html'
DEFAULT_CSS_FILE_PATH = '../html_resources/styles.css'
USE_HELP_TEXT = 'use -h for help'

WORKING_DIR = Path(__file__).resolve().parent
MARKDOWN_CONVERTER = markdown.Markdown(extensions=["extra", "toc", "mdx_emdash"])


def read_lines_from_file(file):
    with open(file, 'r') as file_handler:
        return file_handler.read()


def md2html(input_file, output_file, title, template_dir, link_css, include_css, force, verbose, report):

    if not force and output_file.exists():
        output_file_mtime = os.path.getmtime(output_file)
        input_file_mtime = os.path.getmtime(input_file)
        if output_file_mtime > input_file_mtime:
            if verbose:
                print(f'The output file is up-to-date. Skipping: {output_file}')
            return

    with open(input_file, 'r') as md_file:
        md_lines = md_file.read()

    # Trying to get title from metadata.
    # If adding other parameters, need to remove this condition.
    if title is None:
        match = re.search('^\\s*<!--METADATA\\s+(.*?)\\s*-->', md_lines, flags=re.IGNORECASE + re.DOTALL)
        if match:
            try:
                metadata = json.loads(match.group(1))
                title_item = metadata.get('title')
                if isinstance(title_item, str):
                    title = title_item
                elif verbose and title_item is not None:
                    print(f"WARNING: Title in page metadata is of type '{type(title_item).__name__}', "
                          f"not a string, skipping.")
            except Exception as e:
                if verbose:
                    print(f'WARNING: Page metadata cannot be parsed: {type(e).__name__}: {e}')

    if title is None:
        title = ''

    substitutions = {'title': title}
    if link_css:
        substitutions['styles'] = f'<link rel="stylesheet" type="text/css" href="{link_css}">'
    elif include_css:
        substitutions['styles'] = '<style>\n' + read_lines_from_file(include_css) + '\n</style>'
    else:
        substitutions['styles'] = ''

    # Methods `markdown.markdownFromFile()` and `Markdown.convertFile()` raise errors from their inside
    # implementation. So methods `markdown.markdown()` and `Markdown.convert()` are gonna be used.
    # And anyway the first two methods read the md-file completely before conversion, so they give
    # no memory save.

    substitutions['content'] = MARKDOWN_CONVERTER.convert(source=md_lines)

    template = Template(read_lines_from_file(template_dir.joinpath(TEMPLATE_FILE_NAME)))
    result = template.safe_substitute(substitutions)

    with open(output_file, 'w') as result_file:
        result_file.write(result)

    if verbose:
        print(f'Output file generated: {output_file}')
    if report:
        print(output_file)


def main():

    # print(sys.argv)

    def formatter_creator(prog):
        return argparse.HelpFormatter(prog, width=80)

    parser = argparse.ArgumentParser(description='Converts Markdown document into HTML document.',
                                     formatter_class=formatter_creator)
    parser.add_argument("-i", "--input", help="input Markdown file name (mandatory)", type=str, required=True)
    parser.add_argument("-o", "--output", help="output HTML file name, defaults to input file name with '.html' "
                                               "extension", type=str)
    parser.add_argument("-t", "--title", help="the HTML page title, if omitted there will be an empty title", type=str)
    parser.add_argument("--template", help="custom template directory", type=str)
    parser.add_argument("--link-css", help="links CSS file, if omitted includes the default CSS into HTML", type=str)
    parser.add_argument("--include-css", help="includes CSS file into HTML, if omitted includes the default CSS",
                        type=str)
    parser.add_argument("--no-css", help="creates HTML with no CSS", action='store_true')
    parser.add_argument("-f", "--force", help="rewrites HTML output file even if it was modified later than the input "
                                              "file", action='store_true')
    parser.add_argument("-v", "--verbose", help="outputs human readable information messages", action='store_true')
    parser.add_argument("-r", "--report", help="if HTML file is generated, outputs the path of this file, "
                                               "incompatible with -v", action='store_true')
    args = parser.parse_args()

    if args.input:
        input_file = Path(args.input)
    else:
        parser.print_usage()
        print(f'Input file is not specified ({USE_HELP_TEXT})')
        sys.exit(1)

    if args.output:
        output_file = Path(args.output)
    else:
        output_file = Path(os.path.splitext(input_file)[0] + '.html')

    if args.title:
        title = args.title
    else:
        title = None

    template_dir = Path(args.template) if args.template else WORKING_DIR.joinpath(DEFAULT_TEMPLATE_DIR)

    if args.link_css and args.include_css:
        parser.print_usage()
        print(f'--link-css and --include-css arguments are not compatible ({USE_HELP_TEXT})')
        sys.exit(1)
    if args.no_css and (args.link_css or args.include_css):
        parser.print_usage()
        print(f'--no-css argument is not compatible with --link-css and --include-css arguments ({USE_HELP_TEXT})')
        sys.exit(1)
    link_css = args.link_css
    if args.no_css:
        include_css = None
    else:
        include_css = Path(args.include_css) if args.include_css else \
            WORKING_DIR.joinpath(DEFAULT_CSS_FILE_PATH)

    force = args.force
    verbose = args.verbose
    report = args.report

    if report and verbose:
        parser.print_usage()
        print(f'--report and --verbose arguments are not compatible ({USE_HELP_TEXT})')
        sys.exit(1)

    md2html(input_file, output_file, title, template_dir, link_css, include_css, force, verbose, report)


if __name__ == '__main__':
    main()
