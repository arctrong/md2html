from pathlib import Path
from typing import List, Dict, Union

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import relativize_relative_resource

MODULE_DIR = Path(__file__).resolve().parent


class PageLinksPlugin(Md2HtmlPlugin):
    def __init__(self):
        super().__init__()
        self.markers: List[str] = []
        self.page_links_handlers: List[PageLinkMetadataHandler] = []

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('page_links_schema.json'))
        markers = data.get("markers")
        self.markers = markers if markers else ["page"]

    def accept_document_list(self, docs: List[Document]):
        if not self.markers:
            return
        documents = {d.code: d.output_file for d in docs if d.code}
        if documents:
            pmh = PageLinkMetadataHandler(documents)
            self.page_links_handlers = [(pmh, m, False) for m in self.markers]

    def is_blank(self) -> bool:
        return not bool(self.page_links_handlers)

    def page_metadata_handlers(self):
        return self.page_links_handlers


class PageLinkMetadataHandler:
    def __init__(self, pages: Dict[str, str]):
        self.pages = pages

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str, metadata_section,
                             visited_markers: Union[Dict[str, None]] = None):

        metadata_str = metadata_str.strip()
        destination_page_output = self.pages.get(metadata_str)
        if destination_page_output is None:
            return metadata_section
        else:
            return relativize_relative_resource(destination_page_output, doc.output_file)
