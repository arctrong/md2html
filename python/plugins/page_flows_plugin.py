from collections.abc import Iterator
from pathlib import Path

from plugins.md2html_plugin import Md2HtmlPlugin, validate_data
from utils import relativize_relative_resource

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
        if page["external"]:
            pages.append(page)
        else:
            is_current = page["link"] == output_file
            new_page = {"link": relativize_relative_resource(page["link"], output_file),
                        "title": page["title"],
                        "current": is_current}
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
        # noinspection PyTypeChecker
        self.data: dict = None

    def accept_data(self, data):
        validate_data(data, MODULE_DIR.joinpath('page_flows_schema.json'))
        result = {}
        for k, v in data.items():
            page_flow_items = []
            for item in v:

                # TODO Consider letting other arbitrary fields. Then they might be used in
                #  the template. Though this may be a problem in the Java version... but
                #  probably not as we are going to use a Map.

                page_flow_item = {"link": item["link"], "title": item["title"]}
                is_external = item.get("external")
                page_flow_item["external"] = is_external if is_external is not None else False
                page_flow_items.append(page_flow_item)
            result[k] = page_flow_items
        self.data = result

    def variables(self, doc: dict) -> dict:
        output_file = str(doc['output_file'])
        return {k: process_page_flow(v, output_file) for k, v in self.data.items()}
