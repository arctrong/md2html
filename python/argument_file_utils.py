import json
from json.decoder import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from constants import DEFAULT_TEMPLATE_PATH, DEFAULT_CSS_FILE_PATH, PLUGINS
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
    def __init__(self, options, documents, plugins):
        self.options = options
        self.documents = documents
        self.plugins = plugins


def parse_argument_file_content(argument_file_dict: dict, cli_args: dict) -> Arguments:

    documents = []
    plugins = []
    options = {}
    documents_page_flows = {}

    plugins_item = argument_file_dict.get('plugins')
    page_flows_plugin = None
    # Even if all page flows are defined in the 'documents' section, at least empty 'page-flows'
    # plugin must be defined in order to activate page flows processing.
    page_flows_plugin_defined = False
    if plugins_item is not None:
        page_flows_plugin = PLUGINS.get("page-flows")
        page_flows_plugin_defined = "page-flows" in plugins_item

    if 'options' in argument_file_dict:
        options = argument_file_dict['options']
        if 'verbose' in options:
            if cli_args.get("report"):
                raise UserError("'verbose' parameter in 'options' section is incompatible "
                                "with '--report' command line argument.")
        else:
            options["verbose"] = cli_args["verbose"]
    else:
        options["verbose"] = cli_args["verbose"]
    options['legacy_mode'] = first_not_none(cli_args.get('legacy_mode'),
                                            options.get('legacy_mode'), False)

    defaults_item = argument_file_dict.get('default')
    if defaults_item is None:
        defaults_item = {}

    documents_item = argument_file_dict['documents']

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

    plugins_item = argument_file_dict.get('plugins')
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
        document['template'] = MODULE_DIR.joinpath(DEFAULT_TEMPLATE_PATH)
    if not document['output_file']:
        document['output_file'] = str(Path(strip_extension(document['input_file']) + '.html')
                                      ).replace('\\', '/')
    if not document['no_css'] and not document['link_css'] and not document['include_css']:
        document['include_css'] = [MODULE_DIR.joinpath(DEFAULT_CSS_FILE_PATH)]
