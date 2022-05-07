import json
from json.decoder import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from constants import DEFAULT_TEMPLATE_PATH, DEFAULT_CSS_FILE_PATH
from utils import UserError, read_lines_from_commented_json_file, \
    reduce_json_validation_error_message, first_not_none, strip_extension

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
            read_lines_from_commented_json_file(MODULE_DIR.joinpath('args_file_schema.json')))
        validate(instance=arguments_item, schema=schema)
    except ValidationError as e:
        raise UserError(f"Error validating argument file content: {type(e).__name__}: " +
                        reduce_json_validation_error_message(str(e)))
    return arguments_item


class Arguments:
    def __init__(self, options, documents):
        self.options: dict = options
        self.documents: list = documents


def add_documents_page_flows_data(plugins_page_flow_item, documents_page_flows):
    if plugins_page_flow_item is None or documents_page_flows is None:
        return
    for k, v in documents_page_flows.items():
        page_flow_items = plugins_page_flow_item.setdefault(k, [])
        new_items = v[:]
        for item in page_flow_items:
            new_items.append(item)
        plugins_page_flow_item[k] = new_items


def parse_argument_file_content(argument_file_dict: dict, cli_args: dict) -> Arguments:
    """
    This method makes modifications to the content of the `argument_file_dict` instance.
    These modifications assure presence of some fields and enrich the data.
    Some data may be added to the `plugins` section, but the plugins are not processed in this
    method for technical reasons (due to a circular import). The plugins may then be obtained
    by calling `argument_file_plugin_utils.process_plugins(...)` method.
    """

    options = argument_file_dict.setdefault('options', {})
    plugins_item = argument_file_dict.setdefault('plugins', {})

    page_flows_plugin_item = plugins_item.get("page-flows")

    if bool(options.get('verbose')) and bool(cli_args.get("report")):
        raise UserError("'verbose' parameter in 'options' section is incompatible "
                        "with '--report' command line argument.")

    options['verbose'] = first_not_none(options.get('verbose'), cli_args.get('verbose'), False)
    options['legacy_mode'] = first_not_none(cli_args.get('legacy_mode'),
                                            options.get('legacy-mode'), False)

    if options['legacy_mode']:
        page_variables = plugins_item.setdefault("page-variables", {})
        page_variables.setdefault("METADATA", {"only-at-page-start": True})

    defaults_item = argument_file_dict.get('default')
    if defaults_item is None:
        defaults_item = {}

    if 'no-css' in defaults_item and (
            'link-css' in defaults_item or 'include-css' in defaults_item):
        raise UserError(f"'no-css' parameter incompatible with one of the ['link-css', "
                        f"'include-css'] in the 'default' section.")

    documents = []
    documents_page_flows = {}
    for document_item in argument_file_dict['documents']:
        document = {}

        input_file = first_not_none(cli_args.get('input_file'), document_item.get("input"),
                                    defaults_item.get("input"))
        if input_file is None:
            raise UserError(f"Undefined input file for 'documents' item: {document_item}.")
        document['input_file'] = input_file

        document['output_file'] = first_not_none(cli_args.get('output_file'),
                                                 document_item.get("output"),
                                                 defaults_item.get("output"))
        document['input_root'] = first_not_none(cli_args.get('input_root'),
                                                document_item.get("input-root"),
                                                defaults_item.get('input-root'))
        document['output_root'] = first_not_none(cli_args.get('output_root'),
                                                 document_item.get("output-root"),
                                                 defaults_item.get('output-root'))

        document['title'] = first_not_none(cli_args.get('title'), document_item.get('title'),
                                           defaults_item.get('title'), '')

        template = first_not_none(cli_args.get('template'), document_item.get('template'),
                                  defaults_item.get('template'))
        document['template'] = Path(template) if template is not None else None

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

        document['force'] = first_not_none(True if cli_args.get('force') else None,
                                           document_item.get('force'),
                                           defaults_item.get('force'), False)
        document['verbose'] = first_not_none(True if cli_args.get('verbose') else None,
                                             document_item.get('verbose'),
                                             defaults_item.get('verbose'), False)
        document['report'] = first_not_none(True if cli_args.get('report') else None,
                                            document_item.get('report'),
                                            defaults_item.get('report'), False)

        if document['report'] and document['verbose']:
            raise UserError(f"Incompatible 'report' and 'verbose' parameters for 'documents' "
                            f"item: {document_item}.")

        enrich_document(document)

        # Even if all page flows are defined in the 'documents' section, at least empty
        # 'page-flows' plugin must be defined in order to activate page flows processing.
        if page_flows_plugin_item is not None:
            if ((1 if 'no-page-flows' in document_item else 0) +
                    (1 if 'page-flows' in document_item else 0) +
                    (1 if 'add-page-flows' in document_item else 0) > 1):
                raise UserError(f"Incompatible 'no-page-flows', 'page-flows' and 'add-page-flows' "
                                f"parameters for 'documents' item: {document_item}.")
            page_flows = first_not_none([] if document_item.get('no-page-flows') else
                                        document_item.get('page-flows'),
                                        defaults_item.get('page-flows'), [])
            for page_flow in page_flows:
                page_flow_list = documents_page_flows.setdefault(page_flow, [])
                page_flow_list.append({"link": document["output_file"], "title": document["title"]})
            add_page_flows = first_not_none(document_item.get("add-page-flows"), [])
            for page_flow in add_page_flows:
                page_flow_list = documents_page_flows.setdefault(page_flow, [])
                page_flow_list.append({"link": document["output_file"], "title": document["title"]})

        documents.append(document)

    add_documents_page_flows_data(page_flows_plugin_item, documents_page_flows)

    return Arguments(options, documents)


def enrich_document(document):
    if not document['template']:
        document['template'] = MODULE_DIR.joinpath(DEFAULT_TEMPLATE_PATH)
    if not document['output_file']:
        document['output_file'] = str(Path(strip_extension(document['input_file']) + '.html')
                                      ).replace('\\', '/')

    input_root = document['input_root']
    if input_root:
        document['input_file'] = str(Path(input_root).joinpath(document['input_file'])
                                     ).replace('\\', '/')
    output_root = document['output_root']
    if output_root:
        document['output_file'] = str(Path(output_root).joinpath(document['output_file'])
                                      ).replace('\\', '/')

    if not document['no_css'] and not document['link_css'] and not document['include_css']:
        document['include_css'] = [MODULE_DIR.joinpath(DEFAULT_CSS_FILE_PATH)]
