import glob
import json
from json.decoder import JSONDecodeError
from pathlib import Path
from typing import Tuple

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

    options['verbose'] = first_not_none(cli_args.verbose, options.get('verbose'), False)
    options['cache-file'] = first_not_none(options.get('cache-file'), None)

    if 'no-css' in defaults_item and (
            'link-css' in defaults_item or 'include-css' in defaults_item):
        raise UserError(f"'no-css' parameter incompatible with one of the ['link-css', "
                        f"'include-css'] in the 'default' section.")

    canonized_document_items = []
    for document_item in argument_file_dict['documents']:
        canonized_document_items.append(
            merge_and_canonize_document(document_item, defaults_item, cli_args))
    merged_and_canonized_argument_file['documents'] = canonized_document_items

    return merged_and_canonized_argument_file


def merge_and_canonize_document(document_item: dict, defaults_item: dict,
                                cli_args: CliArgDataObject) -> dict:
    """
    Merges document definitions with priority: CLI args > documents section > default section.
    """

    def get_value(cli_attr_name, doc_key, default_key=None, default_value=None):
        """Helper to get value with proper priority order."""
        cli_val = getattr(cli_args, cli_attr_name, None) if cli_attr_name else None
        doc_val = document_item.get(doc_key)
        def_key = default_key if default_key else doc_key
        def_val = defaults_item.get(def_key) if def_key else None
        return first_not_none(cli_val, doc_val, def_val, default_value)

    def get_bool_flag(cli_attr_name, doc_key, default_key=None, default_value=False):
        """Helper for boolean flags that can come from CLI."""
        cli_val = getattr(cli_args, cli_attr_name, None) if cli_attr_name else None
        cli_bool = True if cli_val else None
        doc_val = document_item.get(doc_key)
        def_key = default_key if default_key else doc_key
        def_val = defaults_item.get(def_key) if def_key else None
        return first_not_none(cli_bool, doc_val, def_val, default_value)

    input_file = get_value('input_file', 'input')
    input_glob = get_value('input_glob', 'input-glob')

    if input_glob and input_file:
        raise UserError(f"Both input file GLOB and input file name are defined "
                        f"for 'documents' item: {document_item}.")
    elif not input_glob and not input_file:
        raise UserError(f"None of the input file name or input file GLOB is specified "
                        f"for 'documents' item: {document_item}.")

    canonized = {
        'input': input_file,
        'input-glob': input_glob,
        'input-root': get_value('input_root', 'input-root', default_value=""),
        'output-root': get_value('output_root', 'output-root', default_value=""),
        'output': get_value('output_file', 'output'),
        'title': get_value('title', 'title'),
        'code': document_item.get('code'),
        'title-from-variable': get_value('title_from_variable', 'title-from-variable'),
        'code-from-variable': get_value(None, 'code-from-variable'),
        'template': get_value('template', 'template'),
        'force': get_bool_flag('force', 'force'),
        'verbose': get_bool_flag('verbose', 'verbose'),
    }

    if input_glob:
        sort_by_file_path = get_bool_flag('sort_by_file_path', 'sort-by-file-path')
        sort_by_variable = get_value('sort_by_variable', 'sort-by-variable')
        sort_by_title = get_bool_flag('sort_by_title', 'sort-by-title')

        active_sorts = []
        if sort_by_file_path:
            active_sorts.append("'sort-by-file-path'")
        if sort_by_variable:
            active_sorts.append("'sort-by-variable'")
        if sort_by_title:
            active_sorts.append("'sort-by-title'")

        if len(active_sorts) > 1:
            raise UserError(f"Incompatible sort options {', '.join(active_sorts)} for 'documents' "
                            f"item: {document_item}.")

        canonized.update({
            "sort-by-file-path": sort_by_file_path,
            "sort-by-variable": sort_by_variable,
            "sort-by-title": sort_by_title,
        })

    css_options = _merge_css_options(document_item, defaults_item, cli_args)
    canonized.update(css_options)

    if {'page-flows', 'add-page-flows'}.issubset(document_item):
        raise UserError(f"Incompatible 'page-flows' and 'add-page-flows' "
                        f"parameters in the 'documents' item: {document_item}.")
    page_flows = first_not_none(document_item.get('page-flows'),
                                defaults_item.get('page-flows'), [])
    add_page_flows = document_item.get("add-page-flows")
    if add_page_flows is not None:
        page_flows.extend(add_page_flows)
    canonized['page-flows'] = page_flows

    return canonized


def _merge_css_options(document_item: dict, defaults_item: dict,
                       cli_args: CliArgDataObject) -> dict:
    """Handles CSS option merging with proper priority (CLI > document > default)."""

    if cli_args.no_css or cli_args.link_css or cli_args.include_css:
        if cli_args.no_css:
            return {'no-css': True, 'link-css': [], 'include-css': []}
        return {
            'no-css': False,
            'link-css': cli_args.link_css or [],
            'include-css': cli_args.include_css or []
        }

    link_args = ["link-css", "add-link-css", "include-css", "add-include-css"]
    # TODO Looks like if any of the CSS options is defined in the command line then
    #  all CSS options are taken from the command line. Need to check whether it's correct.
    if 'no-css' in document_item and any(document_item.get(k) for k in link_args):
        q = '\''
        raise UserError(f"'no-css' parameter incompatible with any of "
                        f"[{', '.join([q + a + q for a in link_args])}] "
                        f"in `documents` item: {document_item}.")

    no_css = first_not_none(document_item.get('no-css'),
                            defaults_item.get('no-css'), False)

    link_css = list(first_not_none(document_item.get('link-css'),
                                   defaults_item.get('link-css'), []))
    link_css.extend(first_not_none(document_item.get('add-link-css'), []))

    include_css = list(first_not_none(document_item.get('include-css'),
                                      defaults_item.get('include-css'), []))
    include_css.extend(first_not_none(document_item.get('add-include-css'), []))

    if link_css or include_css:
        no_css = False

    return {
        'link-css': link_css,
        'include-css': include_css,
        'no-css': no_css
    }


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


def complete_arguments_processing(canonized_argument_file: dict, plugins) -> Tuple[Arguments, dict]:
    """
    Returns a tuple:

    - Arguments data object without plugins;
    - Definitions of the plugins that are defined outside the `plugins` section.

    This method does not instantiate plugins.
    """
    options_item = canonized_argument_file['options']
    options = Options(verbose=options_item['verbose'],
                      cache_file=options_item['cache-file']
                      )

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
