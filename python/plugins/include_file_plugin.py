from pathlib import Path
from typing import Dict

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import read_lines_from_cached_file, UserError

MODULE_DIR = Path(__file__).resolve().parent


class IncludeFileData:
    def __init__(self, root_dir: str, trim: bool):
        self.root_dir: str = root_dir
        self.trim: bool = trim


class IncludeFilePlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.data: Dict[str, IncludeFileData] = {}

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('include_file_schema.json'))

        for item in data:
            for marker in item["markers"]:
                marker = marker.upper()
                if marker in self.data:
                    raise UserError(f"Marker duplication (case-insensitively): {marker}")
                self.data[marker] = IncludeFileData(item["root-dir"], item.get("trim", True))

    def is_blank(self) -> bool:
        return not bool(self.data)

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in self.data.keys()]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str, metadata_section):
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
        return content
