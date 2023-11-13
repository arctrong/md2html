import json
from pathlib import Path
from typing import Dict, Union

from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from page_metadata_utils import apply_metadata_handlers
from plugins.md2html_plugin import Md2HtmlPlugin
from plugins.plugin_utils import dict_from_string_or_object
from utils import read_lines_from_cached_file, UserError, SmartSubstringer, strip_empty_lines

MODULE_DIR = Path(__file__).resolve().parent


class IncludeFileData:
    def __init__(self, root_dir: str, trim: Union[str, None], recursive: bool,
                 # start_with: str, end_with: str, start_marker: str, end_marker: str
                 subsringer: SmartSubstringer):
        self.root_dir: str = root_dir
        self.trim: Union[str, None] = trim
        self.recursive: bool = recursive
        self.subsringer: SmartSubstringer = subsringer


class IncludeFilePlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.data: Dict[str, IncludeFileData] = {}
        self.all_metadata_handlers = None
        with open(MODULE_DIR.joinpath('include_file_metadata_schema.json'), 'r',
                  encoding="utf-8") as schema_file:
            self.metadata_schema = json.load(schema_file)

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('include_file_schema.json'))

        for item in data:
            for marker in item["markers"]:
                marker = marker.upper()
                if marker in self.data:
                    raise UserError(f"Marker duplication (case-insensitively): {marker}")
                substringer = SmartSubstringer(start_with=item.get("start-with", ""),
                                               end_with=item.get("end-with", ""),
                                               start_marker=item.get("start-marker", ""),
                                               end_marker=item.get("end-marker", ""),
                                               )
                self.data[marker] = IncludeFileData(root_dir=item["root-dir"],
                                                    trim=item.get("trim", "all"),
                                                    recursive=item.get("recursive", False),
                                                    subsringer=substringer,
                                                    )

    def is_blank(self) -> bool:
        return not bool(self.data)

    def accept_app_data(self, plugins: list, options: Options, metadata_handlers: PageMetadataHandlers):
        self.all_metadata_handlers = metadata_handlers

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in self.data.keys()]

    def accept_page_metadata(self, doc: Document, marker: str, metadata_str: str, metadata_section,
                             visited_markers: Union[Dict[str, None]] = None):

        marker_data = self.data[marker]

        subsringer = marker_data.subsringer
        try:
            metadata = dict_from_string_or_object(metadata_str.strip(), "file", self.metadata_schema)
        except UserError as e:
            raise UserError(f"Error in inclusion: {str(e)}")

        subsringer = subsringer.smart_copy(
            metadata.get("start-with"),
            metadata.get("end-with"),
            metadata.get("start-marker"),
            metadata.get("end-marker"),
        )

        file_path = metadata.get("file").strip()
        include_file = Path(marker_data.root_dir).joinpath(file_path)
        try:
            content = read_lines_from_cached_file(include_file)
        except FileNotFoundError as e:
            raise UserError(f"Error processing page metadata block: {type(e).__name__}: {e}")

        content = subsringer.substring(content)

        trim = metadata.get("trim", marker_data.trim)
        if trim == "all":
            content = content.strip()
        elif trim == "empty-lines":
            content = strip_empty_lines(content)

        recursive = metadata.get("recursive", marker_data.recursive)
        if recursive:
            content = apply_metadata_handlers(content, self.all_metadata_handlers, doc,
                                              visited_markers=visited_markers,
                                              recursive_marker=f"INCLUDE_FILE_PLUGIN:{include_file}")

        return content
