import argparse
from pathlib import Path


USE_HELP_TEXT = 'use -h for help'


class CliError(Exception):
    pass


class CliArgDataObject:

    def __init__(self):
        self.argument_file = None
        self.input_root = None
        self.output_root = None
        self.input_file = None
        self.input_glob = None
        self.sort_by_variable = None
        self.sort_by_file_path = None
        self.sort_by_title = None
        self.output_file = None
        self.title = None
        self.title_from_variable = None
        self.template = None
        self.no_css = None
        self.link_css = None
        self.include_css = None
        self.force = None
        self.verbose = None
        self.report = None
        self.legacy_mode = None


def parse_cli_arguments(*args) -> CliArgDataObject:
    """
    If the declarative constraints, defined in the used command line parser, are broken then
    the parser prints the error message to the console and exits the program.
    Further procedural checks behave the same way, by raising a `CliError`.
    """

    def formatter_creator(prog):
        return argparse.HelpFormatter(prog, width=80)

    # noinspection PyTypeChecker
    parser = argparse.ArgumentParser(description='Creates HTML documentation out of Markdown '
                                                 'texts.', formatter_class=formatter_creator,
                                     add_help=False)
    parser.add_argument("-h", "--help", help="shows this help message and exits",
                        action='store_true')

    parser.add_argument("--input-root", help="root directory for input Markdown files. "
                                             "Defaults to current directory", type=str)
    parser.add_argument("-i", "--input", help="input Markdown file name: absolute or relative "
                                              "to the '--input-root' argument value", type=str)
    parser.add_argument("--input-glob", help="input Markdown file name pattern: absolute or "
                                             "relative to the '--input-root' argument "
                                             "value", type=str)

    parser.add_argument("--sort-by-file-path", help="If '--input-glob' is used, the documents "
                                                    "will be sorted by the input file "
                                                    "path", action='store_true')
    parser.add_argument("--sort-by-variable", help="If '--input-glob' is used, the documents "
                                                   "will be sorted by the value of the specified "
                                                   "page variable", type=str)
    parser.add_argument("--sort-by-title", help="If '--input-glob' is used, the documents "
                                                "will be sorted by their "
                                                "titles", action='store_true')

    parser.add_argument("--output-root", help="root directory for output HTML files. Defaults "
                                              "to current directory", type=str)
    parser.add_argument("-o", "--output", help="output HTML file name: absolute or relative "
                                               "to '--output-root' argument value. Defaults to "
                                               "input file name with '.html' extension", type=str)

    parser.add_argument("--argument-file", help="argument file. Allows processing multiple "
                                                "documents with a single run. Also provides "
                                                "different adjustment possibilities and "
                                                "automations. If omitted, the single file "
                                                "will be processed", type=str)

    parser.add_argument("-t", "--title", help="the HTML page title", type=str)
    # TODO Clarify how it works if GLOBs are not used.
    parser.add_argument("--title-from-variable", help="If specified then the program will take "
                                                      "the title from the page metadata at the "
                                                      "step of making up the input file "
                                                      "list", type=str)

    parser.add_argument("--template", help="template that will be used for HTML documents "
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
                        help="turns on formalized output that may be further automatically "
                             "processed. Only if HTML file is generated, the path of this file "
                             "will be output. Incompatible with -v", action='store_true')
    parser.add_argument("--legacy-mode",
                        help="Allows processing documentation projects prepared for version of "
                             "the program prior to 1.0.0. It's still recommended to migrate the "
                             "documentation projects to the newer version", action='store_true')

    args = parser.parse_args(*args)

    if args.help:
        parser.print_help()
        raise CliError()

    cli_arg_data_object = CliArgDataObject()

    if args.argument_file:
        cli_arg_data_object.argument_file = Path(args.argument_file)

    cli_arg_data_object.input_root = args.input_root
    cli_arg_data_object.output_root = args.output_root

    if args.input:
        cli_arg_data_object.input_file = args.input
    if args.input_glob:
        cli_arg_data_object.input_glob = args.input_glob

    if cli_arg_data_object.input_file and cli_arg_data_object.input_glob:
        parser.print_usage()
        print(f'Both input file GLOB and input file name are defined ({USE_HELP_TEXT})')
        raise CliError()
    if not args.argument_file and not (cli_arg_data_object.input_file or
                                       cli_arg_data_object.input_glob):
        parser.print_usage()
        print(f'None of the input file name or input file GLOB is specified ({USE_HELP_TEXT})')
        raise CliError()

    if (1 if args.sort_by_file_path else 0) + (1 if args.sort_by_variable else 0) + (
            1 if args.sort_by_title else 0) > 1:
        parser.print_usage()
        print(f'The options --sort-by-file-path, --sort-by-variable and --sort-by-title are not '
              f'compatible ({USE_HELP_TEXT})')
        raise CliError()
    cli_arg_data_object.sort_by_variable = args.sort_by_variable
    cli_arg_data_object.sort_by_file_path = args.sort_by_file_path
    cli_arg_data_object.sort_by_title = args.sort_by_title

    cli_arg_data_object.output_file = args.output

    cli_arg_data_object.title = args.title
    cli_arg_data_object.title_from_variable = args.title_from_variable

    cli_arg_data_object.template = Path(args.template) if args.template else None

    cli_arg_data_object.no_css = True if args.no_css else False
    if args.no_css and (args.link_css or args.include_css):
        parser.print_usage()
        print(f'--no-css argument is not compatible with --link-css and --include-css '
              f'arguments ({USE_HELP_TEXT})')
        raise CliError()

    cli_arg_data_object.link_css = args.link_css if args.link_css else []
    cli_arg_data_object.include_css = args.include_css if args.include_css else []

    cli_arg_data_object.force = args.force
    cli_arg_data_object.verbose = args.verbose
    cli_arg_data_object.report = args.report

    if cli_arg_data_object.report and cli_arg_data_object.verbose:
        parser.print_usage()
        print(f'--report and --verbose arguments are not compatible ({USE_HELP_TEXT})')
        raise CliError()

    cli_arg_data_object.legacy_mode = args.legacy_mode

    return cli_arg_data_object
