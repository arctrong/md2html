from plugins.md2html_plugin import Md2HtmlPlugin
from plugins.md2html_plugin import PluginDataError
from utils import relativize_relative_resource
from collections.abc import Iterator


class PageFlow(Iterator):

    def __init__(self, pages, previous_page=None, current_page=None, next_page=None):
        self.pages = pages
        self.previous = previous_page
        self.current = current_page
        self.next = next_page

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
        if not isinstance(data, dict):
            raise PluginDataError(f"Plugin data is of type '{type(data).__name__}', not a dict.")
        result = {}
        for k, v in data.items():
            if not isinstance(v, list):
                raise PluginDataError( f"Page flow section '{k}' is of type "
                                       f"'{type(v).__name__}', not a list.")
            page_flow_items = []
            for item in v:
                if not isinstance(item, dict):
                    raise PluginDataError(f"Page flow section '{k}' item is of type "
                                          f"'{type(item).__name__}', not a list: {item}")
                if "link" not in item:
                    raise PluginDataError(f"Page flow section '{k}' does not contain 'link' "
                                          f"parameter: {item}")
                if "title" not in item:
                    raise PluginDataError(f"Page flow section '{k}' does not contain 'title' "
                                          f"parameter: {item}")
                page_flow_item = {"link": item["link"], "title": item["title"]}
                is_external = item.get("external")
                if is_external is not None:
                    if not isinstance(is_external, bool):
                        raise PluginDataError(f"Page flow section '{k}' item 'external' parameter "
                                              f"is of type '{type(is_external).__name__}', not a "
                                              f"boolean: {item}")
                    page_flow_item["external"] = is_external
                else:
                    page_flow_item["external"] = False
                page_flow_items.append(page_flow_item)
            result[k] = page_flow_items
        self.data = result

    def variables(self, doc: dict) -> dict:
        output_file = str(doc['output_file'])
        return {k: process_page_flow(v, output_file) for k, v in self.data.items()}
