from pathlib import Path
from typing import List, Union, Dict

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin

MODULE_DIR = Path(__file__).resolve().parent


class IgnorePlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.markers: List[str] = []
        self.metadata_handlers = []

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('ignore_schema.json'))
        markers = data.get("markers")
        self.markers = markers if markers else ["ignore"]
        self.metadata_handlers = [(self, m, False) for m in self.markers]

    def is_blank(self) -> bool:
        return not bool(self.metadata_handlers)

    def page_metadata_handlers(self):
        return self.metadata_handlers

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None]] = None):
        content_start = metadata_section.find(metadata_str)
        prefix = metadata_section[:content_start - len(marker)]
        suffix = metadata_section[content_start + len(metadata_str):]
        return prefix + metadata_str.lstrip() + suffix
