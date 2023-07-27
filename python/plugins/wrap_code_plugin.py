import os
from pathlib import Path
from typing import Any, Dict

from argument_file_utils import complete_arguments_processing, merge_and_canonize_argument_file
from cli_arguments_utils import CliArgDataObject
from models.document import Document
from models.options import Options
from output_utils import output_page, MARKDOWN
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import read_lines_from_cached_file, relativize_relative_resource, UserError

MODULE_DIR = Path(__file__).resolve().parent


class WrapCodeData:
    def __init__(self):
        self.style = ""
        self.variables = {}
        self.document_dict: dict = {}
        self.document_obj: Document = Document()


class WrapCodePlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.data: Dict[str, WrapCodeData] = {}
        self.processed_cache: Dict[str, str] = {}
        self.plugins_for_output = []
        self.app_options = None
        self.dry_run = False

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('wrap_code_schema.json'))

        for marker, data_dict in data.items():
            wrapped_document_data = WrapCodeData()
            wrapped_document_data.document_dict = {k: v for k, v in data_dict.items()}
            wrapped_document_data.document_dict.pop('style', None)
            wrapped_document_data.document_dict.pop('variables', None)
            wrapped_document_data.style = data_dict.get("style", wrapped_document_data.style)
            wrapped_document_data.variables = data_dict.get("variables", wrapped_document_data.variables)
            self.data[marker.upper()] = wrapped_document_data

    def pre_initialize(self, argument_file: dict, cli_args: CliArgDataObject,
                       plugins: list) -> Dict[str, Any]:

        for marker_data in self.data.values():
            this_argument_file = argument_file.copy()
            this_document_dict = marker_data.document_dict.copy()
            this_document_dict["input"] = "fictional"
            this_document_dict["output"] = "fictional"
            this_argument_file['documents'] = [this_document_dict]

            canonized_argument_file = merge_and_canonize_argument_file(this_argument_file, cli_args)
            arguments, _ = complete_arguments_processing(canonized_argument_file, plugins)

            document_obj = arguments.documents[0]
            document_obj.input_file = document_obj.input_file[:-9]
            document_obj.output_file = document_obj.output_file[:-9]
            marker_data.document_obj = document_obj

        return {}

    def is_blank(self) -> bool:
        return not bool(self.data)

    def accept_app_data(self, plugins: list, options: Options):
        self.plugins_for_output = plugins
        self.app_options = options

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in self.data.keys()]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str, metadata_section):
        marker = marker.upper()
        marker_data = self.data[marker]
        metadata_str = metadata_str.strip()
        document_obj = marker_data.document_obj

        input_file = Path(document_obj.input_file).joinpath(metadata_str)
        input_file_str = str(input_file).replace("\\", "/")
        cache_key = marker + "|" + input_file_str
        output_file_str = self.processed_cache.get(cache_key)
        if not output_file_str:
            output_file = Path(document_obj.output_file).joinpath(metadata_str + ".html")
            output_file_str = str(output_file).replace("\\", "/")

            need_to_generate = True
            if not document_obj.force and output_file.exists():
                output_file_mtime = os.path.getmtime(output_file)
                input_file_mtime = os.path.getmtime(input_file)
                if output_file_mtime > input_file_mtime:
                    if document_obj.verbose:
                        print(f'Wrapped output file is up-to-date. Skipping: {output_file_str}')
                        need_to_generate = False

            if need_to_generate and not self.dry_run:
                document_obj = document_obj.copy(input_file=input_file_str, output_file=output_file_str)

                try:
                    content = read_lines_from_cached_file(document_obj.input_file)
                except FileNotFoundError as e:
                    raise UserError(f"Error processing page metadata block: {type(e).__name__}: {e}")
                doc_content = ("````" + marker_data.style + "\n" + content + "\n" + "````")
                substitutions = {'content': MARKDOWN.convert(source=doc_content)}

                variables = marker_data.variables.copy()
                file_name = str(Path(metadata_str).name)
                variables.update({"title": file_name, "wrap_code_path": metadata_str,
                                  "wrap_code_file_name": file_name})

                output_page(document_obj, self.plugins_for_output, substitutions,
                            self.app_options, variables)

                if document_obj.verbose:
                    print(f'Wrapped output file generated: {document_obj.output_file}')
                if document_obj.report:
                    print(document_obj.output_file)

            self.processed_cache[cache_key] = output_file_str

        return relativize_relative_resource(output_file_str, doc.output_file)
