import json
from html import escape
from io import StringIO
from json import JSONDecodeError
from pathlib import Path
from typing import Any, Dict

from jsonschema import validate, ValidationError

from argument_file_utils import complete_arguments_processing, merge_and_canonize_argument_file
from cli_arguments_utils import CliArgDataObject
from models.document import Document
from models.options import Options
from output_utils import output_page
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import UserError, reduce_json_validation_error_message, relativize_relative_resource

MODULE_DIR = Path(__file__).resolve().parent

INDEX_ENTRY_ANCHOR_PREFIX = 'index_entry_'
INDEX_CONTENT_BLOCK_CLASS = "index-content"
INDEX_ENTRY_CLASS = "index-entry"
INDEX_LETTER_ID_PREFIX = "index_letter_"
INDEX_LETTER_CLASS = "index-letter"
INDEX_LETTERS_BLOCK_CLASS = "index_letters"


def _create_title_attr(title):
    return f' title="{escape(title)}"' if title else ''


def _generate_content(index_cache, add_letters, add_letters_block) -> str:
    index_entries = {}
    for entries in index_cache.values():
        for entry in entries:
            anchor_list = index_entries.setdefault(entry["entry"], [])
            anchor_list.append((entry["link"], entry["title"]))

    content = StringIO()
    letter_links = StringIO()
    current_letter = ""
    for term, links in sorted(index_entries.items(), key=lambda v: v[0].lower()):

        if add_letters or add_letters_block:
            letter = term[0:1].upper() if term else ""
            if letter != current_letter:
                current_letter = letter
                index_letter_id = INDEX_LETTER_ID_PREFIX + current_letter
                if add_letters:
                    content.write(f'<p class="{INDEX_LETTER_CLASS}">'
                                  f'<a id="{index_letter_id}"></a>{current_letter}</p>\n')
                else:
                    content.write(f'<a name="{index_letter_id}"></a>\n')
                if add_letters_block:
                    letter_links.write(f'<a href="#{index_letter_id}">{current_letter}</a> ')

        if len(links) > 1:
            links_string = StringIO()
            count = 0
            for link, title in links:
                count += 1
                links_string.write(f'{", " if count > 1 else ""}<a href="{link}"'
                                   f'{_create_title_attr(title)}>{count}</a>')
            content.write(f'<p class="{INDEX_ENTRY_CLASS}">{term}: '
                          f'{links_string.getvalue()}</p>\n')
        else:
            link, title = links[0]
            content.write(f'<p class="{INDEX_ENTRY_CLASS}">'
                          f'<a href="{link}"{_create_title_attr(title)}>{term}</a></p>\n')

    letter_links = letter_links.getvalue()
    return (f'<p class="{INDEX_LETTERS_BLOCK_CLASS}">{letter_links}</p>\n'
            if letter_links else '') + (f'<div class="{INDEX_CONTENT_BLOCK_CLASS}">\n'
                                        f'{content.getvalue()}\n</div>')


class IndexData:
    def __init__(self):
        self.index_cache_file = None
        self.index_cache_relative = False
        self.add_letters = False
        self.add_letters_block = False

        self.document_dict = None
        self.document = None

        self.current_link_page = ''
        self.current_anchor_number = 0
        self.index_cache = {}
        self.cached_page_resets = set()


class IndexPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        with open(MODULE_DIR.joinpath('index_metadata_schema.json'), 'r',
                  encoding="utf-8") as schema_file:
            self.metadata_schema = json.load(schema_file)
        self.index_data = {}
        self.finalization_started = False

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('index_schema.json'))
        for marker, data_dict in data.items():
            index_data = IndexData()
            index_data.document_dict = {k: v for k, v in data_dict.items()}
            index_data.document_dict.pop('index-cache', None)
            index_data.document_dict.pop('index-cache-relative', None)
            index_data.document_dict.pop('letters', None)
            index_data.document_dict.pop('letters-block', None)
            index_data.index_cache_file = data_dict["index-cache"]
            index_data.index_cache_relative = data_dict.get("index-cache-relative",
                                                            index_data.index_cache_relative)
            index_data.add_letters = data_dict.get("letters", index_data.add_letters)
            index_data.add_letters_block = data_dict.get("letters-block",
                                                         index_data.add_letters_block)
            self.index_data[marker.upper()] = index_data

    def is_blank(self) -> bool:
        return not bool(self.index_data)

    def pre_initialize(self, argument_file: dict, cli_args: CliArgDataObject,
                       plugins: list) -> Dict[str, Any]:
        argument_file = argument_file.copy()
        document_dicts = []
        for index_data in self.index_data.values():
            index_data.document_dict["input"] = "fictional.txt"
            document_dicts.append(index_data.document_dict)

        argument_file['documents'] = document_dicts
        canonized_argument_file = merge_and_canonize_argument_file(argument_file, cli_args)
        arguments, extra_plugin_data = complete_arguments_processing(canonized_argument_file,
                                                                     plugins)
        i = 0
        for index_data in self.index_data.values():
            index_data.document = arguments.documents[i]
            i += 1
            index_data.document.input_file = None

            if index_data.index_cache_relative:
                index_data.index_cache_file = str(Path(index_data.document.output_file).parent
                                                  .joinpath(index_data.index_cache_file))
            index_cache_file = Path(index_data.index_cache_file)
            if index_cache_file.exists():
                with open(index_cache_file, 'r', encoding="utf-8") as file:
                    index_data.index_cache = json.load(file)
            else:
                index_data.index_cache = {}

        return extra_plugin_data

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in self.index_data.keys()]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str,
                             metadata_section):
        index_data = self.index_data[marker.upper()]
        metadata_str = metadata_str.strip()
        if metadata_str.startswith('['):
            try:
                metadata = json.loads(metadata_str)
                validate(instance=metadata, schema=self.metadata_schema)
            except JSONDecodeError as e:
                raise UserError(f"Incorrect JSON in index entry: {type(e).__name__}: {str(e)}")
            except ValidationError as e:
                raise UserError(f"Error validating index entry: {type(e).__name__}: " +
                                reduce_json_validation_error_message(str(e)))
        else:
            metadata = [metadata_str]

        anchors = index_data.index_cache[doc.output_file]
        index_data.current_anchor_number += 1
        anchor_name = f'{INDEX_ENTRY_ANCHOR_PREFIX}{marker.lower()}_' \
                      f'{index_data.current_anchor_number}'
        anchor_text = f'<a name="{anchor_name}"></a>'

        for entry in metadata:
            normalized_entry = entry.strip()
            anchors.append({"entry": normalized_entry,
                            "link": f'{index_data.current_link_page}#{anchor_name}',
                            "title": doc.title})

        return anchor_text

    def new_page(self, doc: Document):
        if self.finalization_started:
            return
        for index_data in self.index_data.values():
            index_data.index_cache[doc.output_file] = []
            index_data.cached_page_resets.add(doc.output_file)
            index_data.current_link_page = relativize_relative_resource(
                doc.output_file, index_data.document.output_file)
            index_data.current_anchor_number = 0

    def finalize(self, plugins: list, options: Options):
        self.finalization_started = True

        for index_data in self.index_data.values():
            if not index_data.cached_page_resets:
                if index_data.document.verbose:
                    print(f'Index file is up-to-date. Skipping: {index_data.document.output_file}')
                return

            for plugin in plugins:
                plugin.new_page(index_data.document)

            substitutions = {'content': _generate_content(index_data.index_cache,
                                                          index_data.add_letters,
                                                          index_data.add_letters_block)}

            output_page(index_data.document, plugins, substitutions, options)

            with open(index_data.index_cache_file, 'w', encoding="utf-8") as cache_file:
                json.dump(index_data.index_cache, cache_file, indent=2)

            if index_data.document.verbose:
                print(f'Index file generated: {index_data.document.output_file}')
            if index_data.document.report:
                print(index_data.document.output_file)
