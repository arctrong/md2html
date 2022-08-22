import glob
import json
from json.decoder import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from cli_arguments_utils import CliArgDataObject
from constants import DEFAULT_TEMPLATE_PATH, DEFAULT_CSS_FILE_PATH
from models import Options, Document, Arguments
from page_metadata_utils import apply_metadata_handlers, register_page_metadata_handlers
from utils import UserError, reduce_json_validation_error_message, first_not_none, \
    strip_extension, read_lines_from_cached_file, read_lines_from_file

MODULE_DIR = Path(__file__).resolve().parent

ARGUMENTS_DOCUMENT_LIST_SECTION = "document-list"
ARGUMENTS_PLUGINS_SECTION = "plugins"
ARGUMENTS_OPTIONS_SECTION = "options"


def load_json_argument_file(argument_file_string) -> dict:
    try:
        arguments_item = json.loads(argument_file_string)
    except JSONDecodeError as e:
        raise UserError(f"Error loading JSON argument file: {type(e).__name__}: {e}")
    try:
        schema = json.loads(
            read_lines_from_file(MODULE_DIR.joinpath('args_file_schema.json')))
        validate(instance=arguments_item, schema=schema)
    except ValidationError as e:
        raise UserError(f"Error validating argument file content: {type(e).__name__}: " +
                        reduce_json_validation_error_message(str(e)))
    return arguments_item


# TODO Consider renaming this method
def merge_and_canonize_argument_file(argument_file_dict: dict, cli_args: CliArgDataObject) -> dict:
    """
    Makes changes to the argument file to bring it to a more canonical form:

    - merges arguments from the command line into the argument file;
    - applies the arguments from the default section to the documents;
    - canonizes some parameters that may be defined in different ways;
    - explicitly set defaults values;
    - creates some default structures like empty collections;
    - doesn't change the plugins.

    This method works only with the argument file's content and doesn't use the context.
    So it, for example, doesn't resolve the GLOBs as it would need to read the file system.
    """

    options = argument_file_dict.get('options', {})
    argument_file_dict.setdefault('plugins', {})
    defaults_item = argument_file_dict.get('default', {})

    merged_and_canonized_argument_file = {
        'options': options,
        'plugins': argument_file_dict.get('plugins', [])
    }

    if bool(options.get('verbose')) and bool(cli_args.report):
        raise UserError("'verbose' parameter in 'options' section is incompatible "
                        "with '--report' command line argument.")

    options['verbose'] = first_not_none(options.get('verbose'), cli_args.verbose, False)
    options['legacy-mode'] = first_not_none(cli_args.legacy_mode,
                                            options.get('legacy-mode'), False)

    if 'no-css' in defaults_item and (
            'link-css' in defaults_item or 'include-css' in defaults_item):
        raise UserError(f"'no-css' parameter incompatible with one of the ['link-css', "
                        f"'include-css'] in the 'default' section.")

    canonized_document_items = []
    for document_item in argument_file_dict['documents']:
        canonized_document_items.append(
            merge_and_canonize_document(document_item, defaults_item, cli_args))
    merged_and_canonized_argument_file['documents'] = canonized_document_items

    if merged_and_canonized_argument_file["options"]['legacy-mode']:
        page_variables = merged_and_canonized_argument_file["plugins"].setdefault(
            "page-variables", {})
        page_variables.setdefault("METADATA", {"only-at-page-start": True})

    return merged_and_canonized_argument_file


def merge_and_canonize_document(document_item: dict, defaults_item: dict,
                                cli_args: CliArgDataObject) -> dict:

    canonized_document_item = {}

    input_file = first_not_none(cli_args.input_file,
                                document_item.get("input"),
                                defaults_item.get("input"))
    input_glob = first_not_none(cli_args.input_glob,
                                document_item.get("input-glob"),
                                defaults_item.get("input-glob"))
    if input_glob and input_file:
        raise UserError(f"Both input file GLOB and input file name are defined "
                        f"for 'documents' item: {document_item}.")
    elif not input_glob and not input_file:
        raise UserError(f"None of the input file name or input file GLOB is specified "
                        f"for 'documents' item: {document_item}.")
    canonized_document_item['input'] = input_file
    canonized_document_item['input-glob'] = input_glob
    if input_glob:
        sort_by_file_path = first_not_none(
            True if cli_args.sort_by_file_path else None,
            document_item.get("sort-by-file-path"), defaults_item.get("sort-by-file-path"))
        sort_by_variable = first_not_none(
            cli_args.sort_by_variable, document_item.get("sort-by-variable"),
            defaults_item.get("sort-by-variable"))
        sort_by_title = first_not_none(
            True if cli_args.sort_by_title else None,
            document_item.get("sort-by-title"), defaults_item.get("sort-by-title"))

        sorts = []
        if sort_by_file_path:
            sorts.append("'sort-by-file-path'")
        if sort_by_variable:
            sorts.append("'sort-by-variable'")
        if sort_by_title:
            sorts.append("'sort-by-title'")
        if len(sorts) > 1:
            raise UserError(f"Incompatible sort options {', '.join(sorts)} for 'documents' "
                            f"item: {document_item}.")

        canonized_document_item["sort-by-file-path"] = sort_by_file_path
        canonized_document_item["sort-by-variable"] = sort_by_variable
        canonized_document_item["sort-by-title"] = sort_by_title
    canonized_document_item['output'] = first_not_none(cli_args.output_file,
                                                       document_item.get("output"),
                                                       defaults_item.get("output"))
    canonized_document_item['input-root'] = first_not_none(cli_args.input_root,
                                                           document_item.get("input-root"),
                                                           defaults_item.get('input-root'))
    canonized_document_item['output-root'] = first_not_none(cli_args.output_root,
                                                            document_item.get("output-root"),
                                                            defaults_item.get('output-root'))
    canonized_document_item['title'] = first_not_none(cli_args.title,
                                                      document_item.get('title'),
                                                      defaults_item.get('title'), '')
    canonized_document_item['title-from-variable'] = first_not_none(
        cli_args.title_from_variable,
        document_item.get('title-from-variable'),
        defaults_item.get('title-from-variable'), '')
    canonized_document_item['template'] = first_not_none(cli_args.template,
                                                         document_item.get('template'),
                                                         defaults_item.get('template'), '')
    link_css = []
    include_css = []
    no_css = False
    if cli_args.no_css or cli_args.link_css or cli_args.include_css:
        if cli_args.no_css:
            no_css = True
        else:
            link_css.extend(first_not_none(cli_args.link_css, []))
            include_css.extend(first_not_none(cli_args.include_css, []))
    else:
        link_args = ["link-css", "add-link-css", "include-css", "add-include-css"]
        if 'no-css' in document_item and any(document_item.get(k) for k in link_args):
            q = '\''
            raise UserError(f"'no-css' parameter incompatible with one of "
                            f"[{', '.join([q + a + q for a in link_args])}] "
                            f"in `documents` item: {document_item}.")

        no_css = first_not_none(document_item.get('no-css'), defaults_item.get('no-css'),
                                False)

        link_css.extend(first_not_none(document_item.get('link-css'),
                                       defaults_item.get('link-css'), []))
        link_css.extend(first_not_none(document_item.get('add-link-css'), []))
        include_css.extend(first_not_none(document_item.get('include-css'),
                                          defaults_item.get('include-css'), []))
        include_css.extend(first_not_none(document_item.get('add-include-css'), []))

        if link_css or include_css:
            no_css = False
    canonized_document_item['link-css'] = link_css
    canonized_document_item['include-css'] = include_css
    canonized_document_item['no-css'] = no_css
    canonized_document_item['force'] = first_not_none(True if cli_args.force else None,
                                                      document_item.get('force'),
                                                      defaults_item.get('force'), False)
    verbose = first_not_none(True if cli_args.verbose else None,
                             document_item.get('verbose'),
                             defaults_item.get('verbose'), False)
    report = first_not_none(True if cli_args.report else None,
                            document_item.get('report'),
                            defaults_item.get('report'), False)
    if verbose and report:
        raise UserError(f"Incompatible 'report' and 'verbose' parameters for 'documents' "
                        f"item: {document_item}.")
    canonized_document_item['verbose'] = verbose
    canonized_document_item['report'] = report
    # Page flows must be ignored if the 'page-flows' plugin is not defined.
    # But this is not checked here and must be checked at the following steps.
    if ((1 if 'page-flows' in document_item else 0) +
            (1 if 'add-page-flows' in document_item else 0) > 1):
        raise UserError(f"Incompatible 'page-flows' and 'add-page-flows' "
                        f"parameters in the 'documents' item: {document_item}.")
    page_flows = first_not_none(document_item.get('page-flows'),
                                defaults_item.get('page-flows'), [])
    canonized_document_item['page-flows'] = page_flows
    add_page_flows = document_item.get("add-page-flows")
    if add_page_flows is not None:
        for page_flow in add_page_flows:
            page_flows.append(page_flow)

    return canonized_document_item


def expand_document_globs(documents_item, plugins) -> list:
    """
    Expands the GLOBs, reads the necessary metadata, and resolves some overriding properties.
    """
    expanded_documents_item = []
    for document_item in documents_item:
        input_file_glob = document_item.get("input-glob")
        title_from_variable = document_item.get("title-from-variable")
        input_root = document_item.get("input-root")

        if input_file_glob:
            # in Python 3.10 `glob.glob` has additional argument `root_dir` that would allow
            # to avoid usage of `relative_to`. But here, as for now, Python 3.8 is used.
            file_list = glob.glob(str(Path(input_root).joinpath(input_file_glob)), recursive=True)
            file_list = [str(Path(f).relative_to(input_root)) for f in file_list]

            sort_by_file_path = document_item.get("sort-by-file-path")
            sort_by_variable = document_item.get("sort-by-variable")
            sort_by_title = document_item.get("sort-by-title")

            if sort_by_file_path:
                file_list.sort(key=lambda doc: doc)

            glob_document_items = []
            for file in file_list:
                glob_document_item = {k: v for k, v in document_item.items() if k != "input-glob"}
                glob_document_item["input"] = file

                if title_from_variable or sort_by_variable:
                    page_variables_plugin = plugins.get("page-variables")
                    if page_variables_plugin:
                        page_variables_plugin.new_page({})
                        metadata_handlers = register_page_metadata_handlers(
                            {"page-variables": page_variables_plugin})
                        input_file_string = read_lines_from_cached_file(
                            str(Path(input_root).joinpath(file)))
                        apply_metadata_handlers(input_file_string, metadata_handlers,
                                                glob_document_item, extract_only=True)
                        page_variables = page_variables_plugin.variables({})
                        if title_from_variable:
                            title = page_variables.get(title_from_variable)
                            if title:
                                glob_document_item["title"] = title
                        if sort_by_variable:
                            glob_document_item["SORT_ORDER"] = page_variables.get(sort_by_variable)

                glob_document_items.append(glob_document_item)

            if sort_by_variable:
                glob_document_items.sort(key=lambda doc: first_not_none(doc.get("SORT_ORDER"), ""))
            elif sort_by_title:
                glob_document_items.sort(key=lambda doc: first_not_none(doc.get("title"), ""))

            expanded_documents_item.extend(glob_document_items)
        else:
            expanded_documents_item.append(document_item)

    return expanded_documents_item


def complete_argument_file_processing(canonized_argument_file: dict, plugins) -> (Arguments, dict):

    options_item = canonized_argument_file['options']
    options = Options(verbose=options_item['verbose'],
                      legacy_mode=options_item['legacy-mode'])

    documents_page_flows_plugin = {}
    document_plugins_items = {"page-flows": documents_page_flows_plugin}

    documents_item = expand_document_globs(canonized_argument_file['documents'], plugins)
    documents = []
    for document_item in documents_item:
        # Such check was probably done before but let it stay here as a safeguard.
        if document_item.get("input") is None:
            raise Exception(f"Undefined input file for 'documents' item: {document_item}.")

        _enrich_document(document_item)

        for page_flow in first_not_none(document_item.get('page-flows'), []):
            page_flow_list = documents_page_flows_plugin.setdefault(page_flow, [])
            page_flow_list.append({"link": document_item["output"],
                                   "title": document_item["title"]})

        document_object = Document(input_file=document_item.get("input"),
                                   output_file=document_item.get("output"),
                                   title=document_item.get('title'),
                                   template=document_item.get('template'),
                                   link_css=document_item.get('link-css'),
                                   include_css=document_item.get('include-css'),
                                   no_css=document_item.get('no-css'),
                                   force=document_item.get('force'),
                                   verbose=document_item.get('verbose'),
                                   report=document_item.get('report'),
                                   )
        documents.append(document_object)

    return Arguments(options, documents), document_plugins_items


def _enrich_document(document):
    if not document['template']:
        document['template'] = MODULE_DIR.joinpath(DEFAULT_TEMPLATE_PATH)
    if not document['output']:
        document['output'] = str(Path(strip_extension(document['input']) + '.html')
                                 ).replace('\\', '/')

    input_root = document['input-root']
    if input_root:
        document['input'] = str(Path(input_root).joinpath(document['input'])
                                ).replace('\\', '/')

    output_root = document['output-root']
    if output_root:
        document['output'] = str(Path(output_root).joinpath(document['output'])
                                 ).replace('\\', '/')

    if not document['no-css'] and not document['link-css'] and not document['include-css']:
        document['include-css'] = [MODULE_DIR.joinpath(DEFAULT_CSS_FILE_PATH)]
