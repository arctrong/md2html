import json
from datetime import datetime
from html import escape
from io import StringIO
from json import JSONDecodeError
from pathlib import Path

import chevron
from jsonschema import validate, ValidationError

from constants import EXEC_NAME, EXEC_VERSION
from plugins.md2html_plugin import Md2HtmlPlugin, validate_data
from utils import UserError, reduce_json_validation_error_message, relativize_relative_resource, \
    read_lines_from_cached_file, read_lines_from_file

MODULE_DIR = Path(__file__).resolve().parent


def _create_title_attr(title):
    return f' title="{escape(title)}"' if title else ''


def _generate_content(index_cache) -> str:
    index_entries = {}
    for entries in index_cache.values():
        for entry in entries:
            anchor_list = index_entries.setdefault(entry["entry"], [])
            anchor_list.append((entry["link"], entry["title"]))

    content = StringIO()
    for entry, links in sorted(index_entries.items()):
        if len(links) > 1:
            links_string = StringIO()
            count = 0
            for link, title in links:
                count += 1
                links_string.write(f'{", " if count > 1 else ""}<a href="{link}"'
                                   f'{_create_title_attr(title)}>{count}</a>')
            content.write(f'<p>{entry}:  {links_string.getvalue()}</p>\n')
        else:
            link, title = links[0]
            content.write(f'<p><a href="{link}"{_create_title_attr(title)}>{entry}</a></p>\n')

    return content.getvalue()


class IndexPlugin(Md2HtmlPlugin):

    INDEX_ENTRY_ANCHOR_PREFIX = 'index_entry_'

    def __init__(self):
        self.data = {}
        with open(MODULE_DIR.joinpath('index_metadata_schema.json'), 'r') as schema_file:
            self.metadata_schema = json.load(schema_file)

        self.document = None
        self.plugins = None

        self.current_page = None
        self.current_link_page = ''
        self.current_anchor_number = 0

        self.index_cache = {}
        self.cached_page_resets = set()

    def accept_data(self, data):
        validate_data(data, MODULE_DIR.joinpath('index_schema.json'))
        self.data = data

        index_cache_file = Path(self.data["index-cache"])
        if index_cache_file.exists():
            with open(index_cache_file, 'r') as file:
                self.index_cache = json.load(file)
        else:
            self.index_cache = {}

        return True

    def page_metadata_handlers(self):
        return [(self, "INDEX", False)]

    def accept_page_metadata(self, doc: dict, marker: str, metadata_str: str, metadata_section):
        output_file = doc["output_file"]

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

        if output_file in self.cached_page_resets:
            anchors = self.index_cache[output_file]
        else:
            anchors = []
            if anchors is not self.index_cache.setdefault(output_file, anchors):
                self.index_cache[output_file] = anchors
            self.cached_page_resets.add(output_file)

        anchor_text = StringIO()
        for entry in metadata:
            normalized_entry = entry.strip()
            (link_page, anchor_number) = self._get_next_link(output_file)
            anchors.append({"entry": normalized_entry,
                            "link": f'{link_page}#{self.INDEX_ENTRY_ANCHOR_PREFIX}{anchor_number}',
                            "title": doc.get('title')})
            anchor_text.write(f'<a name="{self.INDEX_ENTRY_ANCHOR_PREFIX}{anchor_number}"></a>')

        return anchor_text.getvalue()

    def new_page(self):
        pass

    def _get_next_link(self, output_file):
        if self.current_page != output_file:
            self.current_page = output_file
            self.current_link_page = relativize_relative_resource(self.current_page, self.data['output'])
            self.current_anchor_number = 0
        self.current_anchor_number += 1
        return self.current_link_page, self.current_anchor_number

    def get_additional_documents(self) -> list:
        document = {k: v for k, v in self.data.items()}
        document["input"] = "fictional.txt"
        return [document]

    def set_additional_documents_processed(self, documents, plugins, metadata_handlers, options):
        self.document = documents[0]
        self.plugins = plugins

    def after_all_page_processed_actions(self):
        return [self]

    def execute_after_all_page_processed(self):

        output_location = self.document['output_file']

        if not self.cached_page_resets:
            if self.document['verbose']:
                print(f'Index file is up-to-date. Skipping: {output_location}')
            return

        template_file = self.document['template']
        link_css = self.document['link_css']
        include_css = self.document['include_css']

        current_time = datetime.today()
        substitutions = {'title': self.document["title"],
                         'exec_name': EXEC_NAME, 'exec_version': EXEC_VERSION,
                         'generation_date': current_time.strftime('%Y-%m-%d'),
                         'generation_time': current_time.strftime('%H:%M:%S')}

        styles = []
        if link_css:
            styles.extend([f'<link rel="stylesheet" type="text/css" href="{item}">'
                           for item in link_css])
        if include_css:
            styles.extend(['<style>\n' + read_lines_from_file(item) + '\n</style>'
                           for item in include_css])
        substitutions['styles'] = '\n'.join(styles) if styles else ''

        for plugin in self.plugins:
            plugin.new_page()
            substitutions.update(plugin.variables(self.document))

        substitutions['content'] = _generate_content(self.index_cache)

        if substitutions['title'] is None:
            substitutions['title'] = ''

        template = read_lines_from_cached_file(template_file)

        try:
            result = chevron.render(template, substitutions)
        except chevron.ChevronError as e:
            raise UserError(f"Error processing template: {type(e).__name__}: {e}")

        with open(self.document["output_file"], 'w') as result_file:
            result_file.write(result)

        with open(self.data["index-cache"], 'w') as cache_file:
            json.dump(self.index_cache, cache_file, indent=2)

        if self.document['verbose']:
            print(f'Index file generated: {output_location}')
        if self.document['report']:
            print(output_location)
