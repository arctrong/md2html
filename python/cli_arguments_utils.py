import argparse
from pathlib import Path

USE_HELP_TEXT = 'use -h for help'


def parse_cli_arguments(*args):
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
    parser.add_argument("--argument-file",
                        help="argument file. Allows processing multiple documents with a single "
                             "run. Also provides different adjustment possibilities", type=str)
    parser.add_argument("-o", "--output", help="output HTML file name, defaults to input file name"
                                               " with '.html' extension", type=str)
    parser.add_argument("-t", "--title", help="the HTML page title, if omitted there will be an "
                                              "empty title", type=str)
    parser.add_argument("--template", help="template that will be used for HTML document "
                                           "generation", type=str)
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
    parser.add_argument("-r", "--report",
                        help="if HTML file is generated, outputs the path of this file, "
                             "incompatible with -v", action='store_true')
    parser.add_argument("--legacy-mode",
                        help="Allows processing documentation projects prepared for version of "
                             "the program prior to 1.0.0. Still it's recommended to migrate the "
                             "projects to the newer version", action='store_true')

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

    md2html_args['legacy_mode'] = args.legacy_mode

    return 'success', md2html_args
