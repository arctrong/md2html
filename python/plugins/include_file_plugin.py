from pathlib import Path
from typing import Dict, Union

from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from page_metadata_utils import apply_metadata_handlers
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import read_lines_from_cached_file, UserError

MODULE_DIR = Path(__file__).resolve().parent


class IncludeFileData:
    def __init__(self, root_dir: str, trim: bool, recursive):
        self.root_dir: str = root_dir
        self.trim: bool = trim
        self.recursive: bool = recursive


class IncludeFilePlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.data: Dict[str, IncludeFileData] = {}
        self.all_metadata_handlers = None

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('include_file_schema.json'))

        for item in data:
            for marker in item["markers"]:
                marker = marker.upper()
                if marker in self.data:
                    raise UserError(f"Marker duplication (case-insensitively): {marker}")
                self.data[marker] = IncludeFileData(item["root-dir"], item.get("trim", True),
                                                    item.get("recursive", False))

    def is_blank(self) -> bool:
        return not bool(self.data)

    def accept_app_data(self, plugins: list, options: Options, metadata_handlers: PageMetadataHandlers):
        self.all_metadata_handlers = metadata_handlers

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in self.data.keys()]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str, metadata_section,
                             visited_markers: Union[Dict[str, None]] = None):
        marker = marker.upper()
        marker_data = self.data[marker]
        file_path = metadata_str.strip()
        include_file = Path(marker_data.root_dir).joinpath(file_path)
        try:
            content = read_lines_from_cached_file(include_file)
        except FileNotFoundError as e:
            raise UserError(f"Error processing page metadata block: {type(e).__name__}: {e}")
        if marker_data.trim:
            content = content.strip()

        if marker_data.recursive:
            content = apply_metadata_handlers(content, self.all_metadata_handlers, doc,
                                              visited_markers=visited_markers,
                                              recursive_marker=f"INCLUDE_FILE_PLUGIN:{include_file}")

        return content
