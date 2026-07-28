import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Set, Union, Tuple

from build_cache import build_cache_manager
from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from plugins.md2html_plugin import Md2HtmlPlugin, MetadataProcessingResult
from utils import UserError, relativize_relative_resource, UniqueIndexer, VariableReplacer, \
    VariableReplacerError

MODULE_DIR = Path(__file__).resolve().parent

DEFAULT_DEF_FORMAT = {
    "markers": ["REFDEF"],
    "template": """<a name="${anchor}"></a><span class="ref-def">[${code}]</span> ${content}""" \
                 "<sup>${back_refs_html}</sup>",
    "back-ref-template": "<a class=\"ref\" href=\"${href}\">${index}</a>",
    "back-ref-delimiter": ", ",
}

DEFAULT_REF_FORMAT = {
    "markers": ["REF"],
    "template": """${content}<sup><a name="${anchor}"></a><a class="ref" """
                """href="${href}">[${code}]</a></sup>""",
}

DEFAULT_CODE_PREFIX = "backref_"

DEF_METADATA_PATTERN = re.compile(r'([^\s]+)\s+(.*)')


@dataclass(frozen=True)
class PageLocation:
    input_file: str
    output_file: str


@dataclass
class DefFormatConfig:
    markers: List[str]
    template: VariableReplacer
    back_ref_template: VariableReplacer
    back_ref_delimiter: str


@dataclass
class RefFormatConfig:
    markers: List[str]
    template: VariableReplacer


@dataclass(frozen=True)
class Definition:
    page: PageLocation
    anchor_id: str
    ref_content: str
    def_format: DefFormatConfig


@dataclass(frozen=True)
class Reference:
    page: PageLocation
    anchor_id: str


def parse_ref_metadata(metadata) -> str:
    fields = metadata.split()
    if len(fields) != 1:
        raise UserError(f"Metadata error: '{metadata}' - the source code must be a single word "
                        f"(no spaces).")
    return fields[0]


def parse_def_metadata(metadata) -> Tuple[str, str]:
    metadata = metadata.strip()
    matcher = DEF_METADATA_PATTERN.match(metadata)
    if not matcher:
        raise UserError(f"Metadata error: '{metadata}' - should contain source code and "
                        f"reference display value separated by spaces.")
    return matcher.group(1), matcher.group(2)


def _normalize_dependency_path(path: str) -> str:
    return path.replace("\\", "/")


def _page_location_from_doc(doc: Document) -> PageLocation:
    return PageLocation(
        _normalize_dependency_path(doc.input_file),
        _normalize_dependency_path(doc.output_file),
    )


def _page_location_from_cache_ref_item(record: dict) -> PageLocation:
    return PageLocation(
        _normalize_dependency_path(record['input_file']),
        _normalize_dependency_path(record['output_file']),
    )


def _record_dependency_pair(ref: PageLocation, def_: PageLocation):
    build_cache_manager.record_dependency(ref.input_file, def_.input_file)
    build_cache_manager.record_dependency(def_.input_file, ref.input_file)


def _prepare_cache_for_save(by_page_cache: Dict[str, Dict[str, dict]]) -> dict:
    lists_cache = {}
    for source_code in sorted(by_page_cache.keys()):
        by_page = by_page_cache[source_code]
        lists_cache[source_code] = [
            by_page[input_file] for input_file in sorted(by_page.keys())
        ]
    return lists_cache


def _normalize_loaded_cache(data: dict) -> Dict[str, Dict[str, dict]]:
    normalized_dicts = {}
    for source_code, referencing_pages in data.items():
        by_input_file = {}
        for referencing_page in referencing_pages:
            normalized_input_file = _normalize_dependency_path(referencing_page.get('input_file'))
            by_input_file[normalized_input_file] = {
                'input_file': normalized_input_file,
                'output_file': _normalize_dependency_path(referencing_page.get('output_file')),
                "anchor_ids": list(referencing_page.get('anchor_ids')),
            }
        if by_input_file:
            normalized_dicts[source_code] = by_input_file
    return normalized_dicts


def _build_reverse_map_references_by_page_from_cache(backrefs_cache: Dict[str, Dict[str, dict]]
                                                     ) -> Dict[str, Set[str]]:
    reverse_map = {}
    for source_code, by_page in backrefs_cache.items():
        for page_input in by_page:
            reverse_map.setdefault(page_input, set()).add(source_code)
    return reverse_map


def _parse_template(template: str, field_name: str) -> VariableReplacer:
    try:
        return VariableReplacer(template)
    except VariableReplacerError as e:
        raise UserError(f"Invalid {field_name}: {e}") from e


def _parse_def_format(entry: dict) -> DefFormatConfig:
    merged = {**DEFAULT_DEF_FORMAT, **entry}
    raw_markers = merged.get("markers")
    if not raw_markers:
        raise UserError("Each def-format entry must declare at least one marker.")
    return DefFormatConfig(
        markers=[m.upper() for m in raw_markers],
        template=_parse_template(merged["template"], "template"),
        back_ref_template=_parse_template(merged["back-ref-template"], "back-ref-template"),
        back_ref_delimiter=merged["back-ref-delimiter"],
    )


def _parse_ref_format(entry: dict) -> RefFormatConfig:
    merged = {**DEFAULT_REF_FORMAT, **entry}
    raw_markers = merged.get("markers")
    if not raw_markers:
        raise UserError("Each ref-format entry must declare at least one marker.")
    return RefFormatConfig(
        markers=[m.upper() for m in raw_markers],
        template=_parse_template(merged["template"], "template"),
    )


def _parse_def_formats(data) -> List[DefFormatConfig]:
    raw_formats = data.get("def-formats")
    if raw_formats is None:
        return [_parse_def_format({})]
    return [_parse_def_format(entry) for entry in raw_formats]


def _parse_ref_formats(data) -> List[RefFormatConfig]:
    raw_formats = data.get("ref-formats")
    if raw_formats is None:
        return [_parse_ref_format({})]
    return [_parse_ref_format(entry) for entry in raw_formats]


class _DefMetadataHandler:

    def __init__(self, plugin: 'BackReferencesPlugin', format_config: DefFormatConfig):
        self._plugin = plugin
        self._format = format_config

    def accept_page_metadata(self, doc: Document, marker: str, metadata: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        plugin = self._plugin
        source_code, ref_content = parse_def_metadata(metadata)
        if phase == 1:
            if source_code in plugin.definitions:
                raise UserError(f"Source '{source_code}' is defined multiple times")
            anchor_id = plugin.code_prefix + "def_" + source_code
            definition = Definition(
                _page_location_from_doc(doc), anchor_id, ref_content, self._format)
            plugin._add_definition(definition, source_code)
            return MetadataProcessingResult(anchor_id, defer=True)
        if phase == 2:
            anchor_id = data_from_prev_phase
            def_format = plugin.definitions[source_code].def_format
            back_ref_list = []
            back_ref_index = 0
            for refs in plugin.references.get(source_code, {}).values():
                for ref in refs:
                    back_ref_index += 1
                    ref_link = relativize_relative_resource(
                        ref.page.output_file, doc.output_file)
                    back_ref_list.append(def_format.back_ref_template.replace({
                        "href": f"{ref_link}#{ref.anchor_id}",
                        "index": str(back_ref_index),
                    }))
            back_ref_html = def_format.back_ref_delimiter.join(back_ref_list)
            return MetadataProcessingResult(def_format.template.replace({
                "code": source_code,
                "anchor": anchor_id,
                "back_refs_html": back_ref_html,
                "content": ref_content,
            }))
        raise Exception(f"Unknown phase: '{phase}'")


class _RefMetadataHandler:

    def __init__(self, plugin: 'BackReferencesPlugin', format_config: RefFormatConfig):
        self._plugin = plugin
        self._format = format_config

    def accept_page_metadata(self, doc: Document, marker: str, metadata: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        plugin = self._plugin
        source_code = parse_ref_metadata(metadata)
        if phase == 1:
            ref_anchor_id = plugin.code_prefix + "ref_" + source_code
            ref_anchor_id = plugin.unique_indexer.get_unique(ref_anchor_id)
            ref_page = _page_location_from_doc(doc)

            plugin._add_reference(source_code, ref_page, ref_anchor_id)

            if source_code in plugin.definitions:
                return MetadataProcessingResult(
                    self._generate_ref_html(source_code, ref_anchor_id, doc))
            return MetadataProcessingResult(ref_anchor_id, defer=True)

        if phase == 2:
            ref_anchor_id = data_from_prev_phase
            if source_code not in plugin.definitions:
                raise UserError(f"Source '{source_code}' referenced but not defined")
            return MetadataProcessingResult(
                self._generate_ref_html(source_code, ref_anchor_id, doc))
        raise Exception(f"Unknown phase: '{phase}'")

    def _generate_ref_html(self, source_code, ref_anchor_id, doc):
        definition = self._plugin.definitions[source_code]
        link = relativize_relative_resource(definition.page.output_file, doc.output_file)
        return self._format.template.replace({
            "code": source_code,
            "anchor": ref_anchor_id,
            "href": f"{link}#{definition.anchor_id}",
            "content": definition.ref_content,
        })


class BackReferencesPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.definitions: Dict[str, Definition] = {}
        self.references: Dict[str, Dict[str, List[Reference]]] = {}
        self.def_formats: List[DefFormatConfig] = []
        self.ref_formats: List[RefFormatConfig] = []
        self.code_prefix = DEFAULT_CODE_PREFIX
        self.unique_indexer: Optional[UniqueIndexer] = None
        self.backrefs_cache_file: Optional[str] = None
        self.backrefs_cache: Dict[str, Dict[str, dict]] = {}
        self._document_inputs: Set[str] = set()
        # Reverse maps for partial resets by page input file to avoid full collection scans.
        self._reverse_map_definitions_by_page: Dict[str, Set[str]] = {}
        self._reverse_map_references_by_page: Dict[str, Set[str]] = {}

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('back_references_schema.json'))

        self.def_formats = _parse_def_formats(data)
        self.ref_formats = _parse_ref_formats(data)

        code_prefix = data.get("code-prefix")
        if code_prefix:
            self.code_prefix = code_prefix

        raw_backrefs_cache_file = data.get("cache")
        if raw_backrefs_cache_file:
            self.backrefs_cache_file = raw_backrefs_cache_file.replace('\\', '/')

    def accept_document_list(self, docs: List[Document]):
        self._document_inputs = {
            _normalize_dependency_path(document.input_file) for document in docs
        }

    def accept_app_data(self, plugins: list, options: Options,
                        metadata_handlers: PageMetadataHandlers):
        if self._backrefs_cache_enabled():
            self._load_backrefs_cache()
            self._prune_backrefs_cache()
            self._reverse_map_references_by_page = (
                _build_reverse_map_references_by_page_from_cache(self.backrefs_cache))
            self._populate_references_from_cache(self.references)

    def is_blank(self) -> bool:
        return not (bool(self.def_formats) or bool(self.ref_formats))

    def page_metadata_handlers(self):
        handlers = []
        for fmt in self.def_formats:
            handler = _DefMetadataHandler(self, fmt)
            handlers.extend([(handler, marker, False) for marker in fmt.markers])
        for fmt in self.ref_formats:
            handler = _RefMetadataHandler(self, fmt)
            handlers.extend([(handler, marker, False) for marker in fmt.markers])
        return handlers

    def _add_definition(self, definition: Definition, source_code: str):
        self.definitions[source_code] = definition
        self._reverse_map_definitions_by_page.setdefault(
            definition.page.input_file, set()).add(source_code)

    def new_page(self, doc: Document):
        self.unique_indexer = UniqueIndexer()
        if doc.input_file is None:
            return
        page_input = _normalize_dependency_path(doc.input_file)
        self._remove_page_from_definitions(page_input)
        self._remove_referencing_page(page_input)

    def finalize(self):
        for source_code, definition in self.definitions.items():
            for refs in self.references.get(source_code, {}).values():
                for ref in refs:
                    _record_dependency_pair(ref.page, definition.page)

        if self.backrefs_cache_file:
            self._save_backrefs_cache()
            build_cache_manager.record_standalone_derived_document(self.backrefs_cache_file)

    def _backrefs_cache_enabled(self) -> bool:
        return self.backrefs_cache_file is not None

    def _load_backrefs_cache(self):
        cache_file = Path(self.backrefs_cache_file)
        if cache_file.exists():
            with open(cache_file, 'r', encoding="utf-8") as file:
                data = json.load(file)
            self.backrefs_cache = _normalize_loaded_cache(data)
        else:
            self.backrefs_cache = {}

    def _save_backrefs_cache(self):
        cache_file = Path(self.backrefs_cache_file)
        cache_file.parent.mkdir(parents=True, exist_ok=True)
        with open(cache_file, 'w', encoding="utf-8") as file:
            json.dump(_prepare_cache_for_save(self.backrefs_cache), file, indent=2)

    def _prune_backrefs_cache(self):
        pruned = {}
        for source_code, referencing_pages in self.backrefs_cache.items():
            kept = {
                input_file: referencing_page
                for input_file, referencing_page in referencing_pages.items()
                if input_file in self._document_inputs
            }
            if kept:
                pruned[source_code] = kept
        self.backrefs_cache = pruned

    def _populate_references_from_cache(self, references):
        for source_code, referencing_pages in self.backrefs_cache.items():
            by_page = references.setdefault(source_code, {})
            for input_file, referencing_page in referencing_pages.items():
                page = _page_location_from_cache_ref_item(referencing_page)
                by_page[input_file] = [
                    Reference(page, anchor_id)
                    for anchor_id in referencing_page['anchor_ids']
                ]

    def _remove_page_from_definitions(self, page_input: str):
        for source_code in self._reverse_map_definitions_by_page.pop(page_input, ()):
            del self.definitions[source_code]

    def _remove_referencing_page(self, page_input: str):
        for source_code in self._reverse_map_references_by_page.pop(page_input, ()):
            self._remove_referencing_page_from_source(source_code, page_input)

    def _remove_referencing_page_from_source(self, source_code: str, page_input: str):
        references = self.references.get(source_code)
        if references is not None:
            references.pop(page_input, None)
            if not references:
                del self.references[source_code]

        if self._backrefs_cache_enabled():
            cache_references = self.backrefs_cache.get(source_code)
            if cache_references is not None:
                cache_references.pop(page_input, None)
                if not cache_references:
                    del self.backrefs_cache[source_code]

    def _add_reference(self, source_code: str, ref_page: PageLocation, ref_anchor_id: str):
        reference = Reference(ref_page, ref_anchor_id)
        self.references.setdefault(source_code, {}).setdefault(
            ref_page.input_file, []).append(reference)
        self._reverse_map_references_by_page.setdefault(
            ref_page.input_file, set()).add(source_code)

        if self._backrefs_cache_enabled():
            referencing_pages = self.backrefs_cache.setdefault(source_code, {})
            record = referencing_pages.get(ref_page.input_file)
            if record is None:
                referencing_pages[ref_page.input_file] = {
                    'input_file': ref_page.input_file,
                    'output_file': ref_page.output_file,
                    'anchor_ids': [ref_anchor_id],
                }
            else:
                record['anchor_ids'].append(ref_anchor_id)
