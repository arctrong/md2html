import json
from html import escape
from io import StringIO
from json import JSONDecodeError
from pathlib import Path

from jsonschema import validate, ValidationError

from argument_file_utils import complete_argument_file_processing, merge_and_canonize_argument_file
from cli_arguments_utils import CliArgDataObject
from output_utils import output_page
from models import Document, Options
from plugins.md2html_plugin import Md2HtmlPlugin, validate_data_with_file
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
                    content.write(f'<p class="{INDEX_LETTER_CLASS}" id="{index_letter_id}">'
                                  f'{current_letter}</p>\n')
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


class IndexPlugin(Md2HtmlPlugin):

    def __init__(self):
        with open(MODULE_DIR.joinpath('index_metadata_schema.json'), 'r') as schema_file:
            self.metadata_schema = json.load(schema_file)

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
        self.finalization_started = False

    def accept_data(self, data):
        validate_data_with_file(data, MODULE_DIR.joinpath('index_schema.json'))
        self.document_dict = {k: v for k, v in data.items()}
        self.index_cache_file = data["index-cache"]
        self.index_cache_relative = data.get("index-cache-relative", self.index_cache_relative)
        self.add_letters = data.get("letters", self.add_letters)
        self.add_letters_block = data.get("letters-block", self.add_letters_block)
        self.document_dict.pop('index-cache', None)
        self.document_dict.pop('index-cache-relative', None)
        self.document_dict.pop('letters', None)
        self.document_dict.pop('letters-block', None)

    def is_blank(self) -> bool:
        return False

    def initialize(self, argument_file: dict, cli_args: CliArgDataObject, plugins: list):
        argument_file = argument_file.copy()
        self.document_dict["input"] = "fictional.txt"
        argument_file['documents'] = [self.document_dict]
        canonized_argument_file = merge_and_canonize_argument_file(argument_file, cli_args)
        arguments, _ = complete_argument_file_processing(canonized_argument_file, plugins)
        self.document = arguments.documents[0]
        self.document.input_file = None

        if self.index_cache_relative:
            self.index_cache_file = str(Path(self.document.output_file).parent
                                        .joinpath(self.index_cache_file))
        index_cache_file = Path(self.index_cache_file)
        if index_cache_file.exists():
            with open(index_cache_file, 'r') as file:
                self.index_cache = json.load(file)
        else:
            self.index_cache = {}

    def page_metadata_handlers(self):
        return [(self, "INDEX", False)]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str,
                             metadata_section):
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

        anchors = self.index_cache[doc.output_file]
        self.current_anchor_number += 1
        anchor_text = f'<a name="{INDEX_ENTRY_ANCHOR_PREFIX}{self.current_anchor_number}"></a>'

        for entry in metadata:
            normalized_entry = entry.strip()
            anchors.append({"entry": normalized_entry,
                            "link": f'{self.current_link_page}#{INDEX_ENTRY_ANCHOR_PREFIX}'
                                    f'{self.current_anchor_number}', "title": doc.title})

        return anchor_text

    def new_page(self, doc: Document):
        if self.finalization_started:
            return

        self.index_cache[doc.output_file] = []
        self.cached_page_resets.add(doc.output_file)
        self.current_link_page = relativize_relative_resource(doc.output_file,
                                                              self.document.output_file)
        self.current_anchor_number = 0

    def finalize(self, plugins: dict, options: Options):
        self.finalization_started = True

        if not self.cached_page_resets:
            if self.document.verbose:
                print(f'Index file is up-to-date. Skipping: {self.document.output_file}')
            return

        for plugin in plugins.values():
            plugin.new_page(self.document)

        substitutions = {'content': _generate_content(self.index_cache, self.add_letters,
                                                      self.add_letters_block)}

        output_page(self.document, plugins, substitutions, options)

        with open(self.index_cache_file, 'w') as cache_file:
            json.dump(self.index_cache, cache_file, indent=2)

