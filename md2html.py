import argparse
import os
import sys
from pathlib import Path
from subprocess import check_output
import markdown
from string import Template

DEFAULT_TEMPLATE_DIR = 'templates'
TEMPLATE_FILE_NAME = 'template.html'
CSS_FILE_NAME = 'styles.css'
USE_HELP_TEXT = 'use -h for help'

WORKING_DIR = Path(__file__).resolve().parent
MARKDOWN_CONVERTER = markdown.Markdown(extensions=["extra", "toc", "markdown_del_ins", "mdx_emdash"])


def read_lines_from_file(file):
    with open(file, 'r') as file_handler:
        return file_handler.read()


def md2html(input_file, output_file, title, template_dir, link_css, force, verbose, report):

    if not force and output_file.exists():
        output_file_mtime = os.path.getmtime(output_file)
        input_file_mtime = os.path.getmtime(input_file)
        if output_file_mtime > input_file_mtime:
            if verbose:
                print(f'The output file is up-to-date. Skipping: {output_file}')
            return

    substitutions = {'title': title}
    if link_css:
        substitutions['styles'] = f'<link rel="stylesheet" type="text/css" href="{link_css}">'
    else:
        substitutions['styles'] = '<style>\n' + read_lines_from_file(template_dir.joinpath(CSS_FILE_NAME)) \
                                  + '\n</style>'

    # Methods `markdown.markdownFromFile()` and `Markdown.convertFile()` raise errors from their inside
    # implementation. So methods `markdown.markdown()` and `Markdown.convert()` are gonna be used.
    # And anyway the first two methods read the md-file completely before conversion, so they give
    # no memory save.

    with open(input_file, 'r') as md_file:
        md_lines = md_file.read()
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

    parser = argparse.ArgumentParser(description='Converts Markdown document into HTML document.',
                                     epilog='Simplified argument set may be used: <input file name> <output file name> '
                                            '<page title>')
    parser.add_argument("-i", "--input", help="input Markdown file name (mandatory)", type=str)
    parser.add_argument("-o", "--output", help="output HTML file name, defaults to input file name with '.html' "
                                               "extension", type=str)
    parser.add_argument("-t", "--title", help="the HTML page title, if omitted there will be an empty title", type=str)
    parser.add_argument("--templates", help="custom template directory", type=str)
    parser.add_argument("-l", "--link-css", help="links CSS file, if omitted includes the default CSS into "
                                                 "HTML", type=str)
    parser.add_argument("-f", "--force", help="rewrites HTML output file even if it was modified later than the input "
                                              "file", action='store_true')
    parser.add_argument("-v", "--verbose", help="outputs human readable information messages", action='store_true')
    parser.add_argument("positionals", help="positionals for -i, -o, and -t, incompatible with corresponding named "
                                            "arguments", metavar='', type=str, nargs=argparse.REMAINDER)
    parser.add_argument("-r", "--report", help="if HTML file is generated, outputs the path of this file, "
                                               "incompatible with -v", action='store_true')
    args = parser.parse_args()

    len_rest = len(args.positionals)
    if len_rest not in [0, 3]:
        parser.print_usage()
        print(f'Positional argument count must be exactly 0 or 3 ({USE_HELP_TEXT})')
        sys.exit(1)

    if len_rest > 0 and (args.input or args.output or args.title):
        parser.print_usage()
        print(f'Incompatible positional and named arguments ({USE_HELP_TEXT})')
        sys.exit(1)

    if args.input:
        input_file = Path(args.input)
    elif len_rest > 0:
        input_file = Path(args.positionals[0])
    else:
        parser.print_usage()
        print(f'Input file is not specified ({USE_HELP_TEXT})')
        sys.exit(1)

    if args.output:
        output_file = Path(args.output)
    elif len_rest > 0:
        output_file = Path(args.positionals[1])
    else:
        output_file = Path(os.path.splitext(input_file)[0] + '.html')

    if args.title:
        title = args.title
    elif len_rest > 0:
        title = args.positionals[2]
    else:
        title = ''

    template_dir = Path(args.templates) if args.templates else WORKING_DIR.joinpath(DEFAULT_TEMPLATE_DIR)
    link_css = args.link_css
    force = args.force
    verbose = args.verbose
    report = args.report

    if report and verbose:
        parser.print_usage()
        print(f'--report and --verbose arguments are not compatible ({USE_HELP_TEXT})')
        sys.exit(1)

    md2html(input_file, output_file, title, template_dir, link_css, force, verbose, report)


if __name__ == '__main__':
    main()
