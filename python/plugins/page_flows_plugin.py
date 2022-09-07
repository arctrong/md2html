import json
from collections.abc import Iterator
from pathlib import Path

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin, validate_data_with_schema
from utils import relativize_relative_resource, first_not_none

MODULE_DIR = Path(__file__).resolve().parent


class PageFlow(Iterator):

    def __init__(self, pages, previous_page=None, current_page=None, next_page=None):
        self.pages = pages
        self.previous = previous_page
        self.current = current_page
        self.next = next_page
        # For a logic-less template like Mustache the following calculated fields will help a lot.
        self.has_navigation = self.previous is not None or self.next is not None
        self.not_empty = self.pages is not None and len(self.pages) > 0

    def __iter__(self):
        return self.pages.__iter__()

    def __next__(self):
        return self.pages.__next__()


def process_page_flow(page_flow, output_file):
    pages = []
    previous_page = None
    current_page = None
    next_page = None

    for page in page_flow:
        new_page = dict(page)
        if page["external"]:
            new_page["current"] = False
            pages.append(new_page)
        else:
            is_current = page["link"] == output_file
            new_page["link"] = relativize_relative_resource(page["link"], output_file)
            new_page["current"] = is_current
            pages.append(new_page)
            if current_page is None:
                if is_current:
                    current_page = new_page
                else:
                    previous_page = new_page
            elif next_page is None:
                next_page = new_page

    if current_page is None:
        previous_page = None

    return PageFlow(pages, previous_page, current_page, next_page)


class PageFlowsPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.initialize_done = False
        self.raw_data: dict = {}
        self.data: dict = {}
        with open(MODULE_DIR.joinpath('page_flows_schema.json'), 'r') as schema_file:
            self.data_schema = json.load(schema_file)

    def accept_data(self, data):
        validate_data_with_schema(data, self.data_schema)
        if self.raw_data:
            self.add_to_start(data)
        else:
            self.raw_data = data

    def add_to_start(self, data):
        if data is None:
            return
        for k, v in data.items():
            page_flow_items = self.raw_data.setdefault(k, [])
            new_items = v[:]
            for item in page_flow_items:
                new_items.append(item)
            self.raw_data[k] = new_items

    def add_to_end(self, data):
        if data is None:
            return
        for k, v in data.items():
            page_flow_items = self.raw_data.setdefault(k, [])
            for item in v:
                page_flow_items.append(item)

    def initialize_data(self):
        result = {}
        for k, v in self.raw_data.items():
            page_flow_items = []
            new_page = {}
            is_first = True
            for item in v:
                # TODO Consider letting other arbitrary fields. Then they might be used in
                #  the template. This is already done in Java version.

                new_page = {"link": item["link"], "title": item["title"],
                            "external": first_not_none(item.get("external"), False),
                            "first": is_first, "last": False}
                page_flow_items.append(new_page)
                is_first = False
            new_page["last"] = True
            result[k] = page_flow_items
        return result

    def initialize(self, extra_plugin_data):
        self.assure_initialize_once()
        if bool(extra_plugin_data):
            validate_data_with_schema(extra_plugin_data, self.data_schema)
            self.add_to_end(extra_plugin_data)
        self.data = self.initialize_data()

    def is_blank(self) -> bool:
        return not bool(self.data)

    def variables(self, doc: Document) -> dict:
        return {k: process_page_flow(v, doc.output_file) for k, v in self.data.items()}

    def assure_initialize_once(self):
        if self.initialize_done:
            raise Exception("Trying to initialize again.")
        self.initialize_done = True
