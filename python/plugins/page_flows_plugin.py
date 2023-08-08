import json
from collections.abc import Iterator
from pathlib import Path

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import relativize_relative_resource, first_not_none, UserError

MODULE_DIR = Path(__file__).resolve().parent


class PageFlow(Iterator):

    def __init__(self, title, pages, previous_page=None, current_page=None, next_page=None):
        self.title = title
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

    for page in page_flow["items"]:
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

    return PageFlow(page_flow["title"], pages, previous_page, current_page, next_page)


def unify_page_flow(page_flow):
    return {"title": page_flow.get("title") or "",
            "groups": page_flow.get("groups") or [],
            "items": page_flow.get("items") or []
            }


def unify_data(data):
    new_data = {}
    for k, v in data.items():
        if type(v) == list:
            new_data[k] = unify_page_flow({"items": v})
        elif type(v) == dict:
            new_data[k] = unify_page_flow(v)
        else:
            raise Exception(f"Wrong value type: {type(v).__name__}")
    return new_data


class PageFlowsPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.initialize_done = False
        self.raw_data: dict = {}
        self.page_flows: dict = {}
        with open(MODULE_DIR.joinpath('page_flows_schema.json'), 'r',
                  encoding="utf-8") as schema_file:
            self.data_schema = json.load(schema_file)

    def accept_data(self, data):
        self.validate_data_with_schema(data, self.data_schema)
        data = unify_data(data)
        if self.raw_data:
            self.add_to_start(data)
        else:
            self.raw_data = data

    def add_to_start(self, data):
        if data is None:
            return
        for k, v in data.items():
            if k in self.raw_data:
                self.raw_data[k]["items"] = v["items"] + self.raw_data[k]["items"]
            else:
                self.raw_data[k] = v

    def add_to_end(self, data):
        if data is None:
            return
        for k, v in data.items():
            if k in self.raw_data:
                self.raw_data[k]["items"].extend(v["items"])
            else:
                self.raw_data[k] = v

    def initialize_data(self):
        page_flows = {}
        for k, v in self.raw_data.items():
            page_flow_items = []
            new_page = {}
            is_first = True
            for item in v["items"]:
                # Allows other arbitrary fields. This fields then may be used in the templates.
                new_page = dict(item)
                new_page["external"] = first_not_none(item.get("external"), False)
                new_page["first"] = is_first
                new_page["last"] = False

                page_flow_items.append(new_page)
                is_first = False
            new_page["last"] = True
            new_v = dict(v)
            new_v["items"] = page_flow_items
            page_flows[k] = new_v
        self.page_flows = page_flows

    def initialize(self, extra_plugin_data):
        self.assure_initialize_once()
        if bool(extra_plugin_data):
            self.validate_data_with_schema(extra_plugin_data, self.data_schema)
            extra_plugin_data = unify_data(extra_plugin_data)
            self.add_to_end(extra_plugin_data)
        self.initialize_data()

    def is_blank(self) -> bool:
        return not bool(self.page_flows)

    def variables(self, doc: Document) -> dict:
        page_flows = {}
        page_flow_groups = {}
        for k, v in self.page_flows.items():
            page_flow = process_page_flow(v, doc.output_file)
            page_flows[k] = page_flow
            for group in v["groups"]:
                page_flow_group = page_flow_groups.setdefault(group, [])
                page_flow_group.append(page_flow)
        variables = page_flows
        for g, pfs in page_flow_groups.items():
            if g in variables:
                raise UserError(f"Variable duplication error in plugin '{type(self).__name__}': "
                                f"group name is '{g}'")
            else:
                variables[g] = pfs
        return variables

    def assure_initialize_once(self):
        if self.initialize_done:
            raise Exception("Trying to initialize again.")
        self.initialize_done = True
