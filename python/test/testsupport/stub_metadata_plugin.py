from dataclasses import dataclass
from typing import Dict, Union

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin, MetadataProcessingResult


@dataclass(frozen=True)
class MarkerSpec:
    marker: str
    only_at_page_start: bool = False

    @staticmethod
    def anywhere(marker: str) -> 'MarkerSpec':
        return MarkerSpec(marker, False)

    @staticmethod
    def at_page_start(marker: str) -> 'MarkerSpec':
        return MarkerSpec(marker, True)


class StubMetadataPlugin(Md2HtmlPlugin):

    def __init__(self, *marker_specs: MarkerSpec):
        super().__init__()
        self._handler_infos = [
            (self, spec.marker, spec.only_at_page_start) for spec in marker_specs
        ]

    def accept_data(self, data):
        raise NotImplementedError("Not used in stub plugin tests")

    def is_blank(self) -> bool:
        return not self._handler_infos

    def page_metadata_handlers(self):
        return self._handler_infos

    def accept_page_metadata(self, doc: Document, marker: str, metadata: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        return MetadataProcessingResult(metadata)
