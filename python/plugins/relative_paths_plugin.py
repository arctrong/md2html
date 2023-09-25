from pathlib import Path
from typing import List, Dict, Union

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import relativize_relative_resource_path

MODULE_DIR = Path(__file__).resolve().parent


class RelativePathsPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.markers: List[str] = []
        self.paths: Dict[str, str] = {}
        self.relative_paths_handlers: List[RelativePathsMetadataHandler] = []

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('relative_paths_schema.json'))
        markers = data.get("markers")
        if markers and type(markers) == list:
            self.markers = markers
            self.paths = data["paths"]
        else:
            self.paths = data
        handler = RelativePathsMetadataHandler(self.paths)
        self.relative_paths_handlers = [(handler, m, False) for m in self.markers]

    def is_blank(self) -> bool:
        return not bool(self.paths)

    def page_metadata_handlers(self):
        return self.relative_paths_handlers

    def variables(self, doc: Document) -> dict:
        result = {}
        for k, v in self.paths.items():
            result[k] = relativize_relative_resource_path(v, doc.output_file)
        return result


class RelativePathsMetadataHandler:
    def __init__(self, paths: Dict[str, str]):
        self.paths = paths

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str, metadata_section,
                             visited_markers: Union[Dict[str, None]] = None):

        path = self.paths.get(metadata_str.strip())
        if path is None:
            return metadata_section
        else:
            return relativize_relative_resource_path(path, doc.output_file)
