import glob
import json
from json.decoder import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from cli_arguments_utils import CliArgDataObject
from constants import DEFAULT_TEMPLATE_PATH, DEFAULT_CSS_FILE_PATH
from models.arguments import Arguments
from models.document import Document
from models.options import Options
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


def merge_and_canonize_argument_file(argument_file_dict: dict, cli_args: CliArgDataObject) -> dict:
    """
    Brings the argument file to a more canonical form:

    - merges arguments from the command line into the argument file;
    - applies the arguments from the default section to the documents;
    - canonizes some parameters that may be defined in different ways;
    - explicitly sets defaults values;
    - creates some default structures like empty collections;
    - doesn't change the plugins.

    This method works only with the argument file's content and doesn't use the context.
    So it, for example, doesn't resolve the GLOBs as it would need to read the file system.

    This method does not instantiate plugins.
    """

    options = argument_file_dict.get('options', {})
    defaults_item = argument_file_dict.get('default', {})

    merged_and_canonized_argument_file = {
        'options': options,
        'plugins': argument_file_dict.get('plugins', {})
    }

    if bool(options.get('verbose')) and bool(cli_args.report):
        raise UserError("'verbose' parameter in 'options' section is incompatible "
                        "with '--report' command line argument.")

    options['verbose'] = first_not_none(cli_args.verbose, options.get('verbose'), False)
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
                                                           defaults_item.get('input-root'),
                                                           "")
    canonized_document_item['output-root'] = first_not_none(cli_args.output_root,
                                                            document_item.get("output-root"),
                                                            defaults_item.get('output-root'),
                                                            "")
    canonized_document_item['title'] = first_not_none(cli_args.title,
                                                      document_item.get('title'),
                                                      defaults_item.get('title'))
    canonized_document_item['code'] = document_item.get('code')
    canonized_document_item['title-from-variable'] = first_not_none(
        cli_args.title_from_variable,
        document_item.get('title-from-variable'),
        defaults_item.get('title-from-variable'))
    canonized_document_item['code-from-variable'] = first_not_none(
        document_item.get('code-from-variable'),
        defaults_item.get('code-from-variable'))
    canonized_document_item['template'] = first_not_none(cli_args.template,
                                                         document_item.get('template'),
                                                         defaults_item.get('template'))
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
        # TODO Looks like if any of the CSS options is defined in the command line then
        #  all CSS options are taken from the command line. Need to check whether it's correct.
        if 'no-css' in document_item and any(document_item.get(k) for k in link_args):
            q = '\''
            raise UserError(f"'no-css' parameter incompatible with any of "
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

    # TODO Test the case when page metadata is absent. Check the same in the Java version.

    metadata_handlers = None
    page_variables_plugin = plugins.get("page-variables")
    if page_variables_plugin:
        metadata_handlers = register_page_metadata_handlers([page_variables_plugin])

    expanded_documents_item = []
    for document_item in documents_item:
        input_file_glob = document_item.get("input-glob")
        title_from_variable = document_item.get("title-from-variable")
        code_from_variable = document_item.get("code-from-variable")
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
                file_list.sort(key=lambda file_path: file_path)

            glob_document_items = []
            for file in file_list:
                glob_document_item = {k: v for k, v in document_item.items() if k != "input-glob"}
                glob_document_item["input"] = file

                if (title_from_variable or code_from_variable or
                        sort_by_variable) and metadata_handlers:
                    page_variables_plugin.new_page(None)
                    try:
                        input_file_string = read_lines_from_cached_file(
                            str(Path(input_root).joinpath(file)))
                    except FileNotFoundError as e:
                        raise UserError(f"Error processing GLOB path '{file}': {type(e).__name__}: {e}")
                    apply_metadata_handlers(input_file_string, metadata_handlers, None,
                                            extract_only=True)
                    page_variables = page_variables_plugin.variables(None)
                    if title_from_variable:
                        title = page_variables.get(title_from_variable)
                        if title:
                            glob_document_item["title"] = title
                    if code_from_variable:
                        code = page_variables.get(code_from_variable)
                        if code:
                            glob_document_item["code"] = code
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


def complete_arguments_processing(canonized_argument_file: dict, plugins) -> (Arguments, dict):
    """
    Returns a tuple:

    - Arguments data object without plugins;
    - Definitions of the plugins that are defined outside the `plugins` section.

    This method does not instantiate plugins.
    """
    options_item = canonized_argument_file['options']
    options = Options(verbose=options_item['verbose'],
                      legacy_mode=options_item['legacy-mode'])

    documents_page_flows_plugin = {}
    extra_plugin_items = {"page-flows": documents_page_flows_plugin}

    # TODO Consider to make `expand_document_globs` to only expand one GLOB document.
    #  This would make possible to enrich the expanded documents one by one and then
    #  to apply an optimization. An optimization may be done by keeping page variables
    #  in a "cache file" and not reading the files that were not changed (unless -f is
    #  specified). "cache-file" option should also be added to the "options" section.
    documents_item = expand_document_globs(canonized_argument_file['documents'], plugins)
    documents = []
    unique_codes = set()
    for document_item in documents_item:
        # Such check was probably done before but let it stay here as a safeguard.
        if document_item.get("input") is None:
            raise Exception(f"Undefined input file for 'documents' item: {document_item}.")

        _enrich_document(document_item)

        document_object = Document(input_file=document_item.get("input").replace("\\", "/"),
                                   output_file=document_item.get("output").replace("\\", "/"),
                                   title=first_not_none(document_item.get('title'), ""),
                                   code=document_item.get('code'),
                                   template=document_item.get('template'),
                                   link_css=document_item.get('link-css'),
                                   include_css=document_item.get('include-css'),
                                   no_css=document_item.get('no-css'),
                                   force=document_item.get('force'),
                                   verbose=document_item.get('verbose'),
                                   report=document_item.get('report'),
                                   )
        documents.append(document_object)

        if document_object.code in unique_codes:
            raise UserError(f"Duplicated document code '{document_object.code}' in: "
                            f"{document_object.input_file}")
        if document_object.code:
            unique_codes.add(document_object.code)

        for page_flow in first_not_none(document_item.get('page-flows'), []):
            page_flow_list = documents_page_flows_plugin.setdefault(page_flow, [])
            page_flow_list.append({"link": document_object.output_file,
                                   "title": document_object.title})

    return Arguments(options, documents, []), extra_plugin_items


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
