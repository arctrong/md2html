import itertools
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Set, Union

from build_cache import build_cache_manager
from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from plugins.md2html_plugin import Md2HtmlPlugin, MetadataProcessingResult
from utils import UserError, relativize_relative_resource, UniqueIndexer, VariableReplacer

MODULE_DIR = Path(__file__).resolve().parent

DEFAULT_DEF_MARKERS = ["REFDEF"]
DEFAULT_REF_MARKERS = ["REF"]
DEFAULT_REFDEF_TEMPLATE = "<a name=\"${1}\"></a><span class=\"ref-def\">[${1}]</span>" \
                          "<sup>${2}</sup> ${3}"
DEFAULT_REF_TEMPLATE = "${3}<sup><a class=\"ref\" href=\"${2}\">[${1}]</a></sup>"
DEFAULT_BACK_REF_TEMPLATE = "<a class=\"ref\" href=\"${1}\">${2}</a>"

DEF_METADATA_PATTERN = re.compile(r'([^\s]+)\s+(.*)')


@dataclass(frozen=True)
class PageLocation:
    input_file: str
    output_file: str


@dataclass(frozen=True)
class Definition:
    page: PageLocation
    anchor_id: str
    ref_content: str


@dataclass(frozen=True)
class Reference:
    page: PageLocation
    anchor_id: str


def parse_ref_metadata(metadata):
    fields = metadata.split()
    if not 1 <= len(fields) <= 2:
        raise UserError(f"Metadata error: '{metadata}' - should contain 1 or 2 fields "
                        f"(source code and optionally format name) separated by spaces.")
    source_code = fields[0]
    format_name = fields[1] if len(fields) == 2 else ""
    return source_code, format_name


def parse_def_metadata(metadata):
    metadata = metadata.strip()
    matcher = DEF_METADATA_PATTERN.match(metadata)
    if not matcher:
        raise UserError(f"Metadata error: '{metadata}' - should contain source code and "
                        f"reference display value separated by spaces.")
    return matcher.group(1), matcher.group(2)


def find_replacer(replacers, format_name):
    replacer = replacers.get(format_name)
    if not replacer:
        replacer = replacers.get("")
    if not replacer:
        raise UserError(f"Template is not defined for format '{format_name}'.")
    return replacer


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


class BackReferencesPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        self.definitions: Dict[str, Definition] = {}
        self.references: Dict[str, List[Reference]] = {}
        self.def_markers = []
        self.ref_markers = []
        self.code_prefix = "backref_"
        self.def_templates: Dict[str, VariableReplacer] = {}    # format -> template
        self.ref_templates: Dict[str, VariableReplacer] = {}       # format -> template
        self.back_ref_templates: Dict[str, VariableReplacer] = {}  # format -> template
        self.unique_indexer: Optional[UniqueIndexer] = None
        self.backrefs_cache_file: Optional[str] = None
        self.backrefs_cache: Dict[str, List[dict]] = {}
        self._document_inputs: Set[str] = set()

    def accept_data(self, data):
        self.assure_accept_data_once()
        self.validate_data_with_file(data, MODULE_DIR.joinpath('back_references_schema.json'))

        def_markers = data.get("def-markers")
        self.def_markers.extend(DEFAULT_DEF_MARKERS if def_markers is None else
                                [m.upper() for m in def_markers])
        ref_markers = data.get("ref-markers")
        self.ref_markers.extend(DEFAULT_REF_MARKERS if ref_markers is None else
                                [m.upper() for m in ref_markers])
        code_prefix = data.get("code-prefix")

        if code_prefix:
            self.code_prefix = code_prefix

        raw_backrefs_cache_file = data.get("cache")
        if raw_backrefs_cache_file:
            self.backrefs_cache_file = raw_backrefs_cache_file.replace('\\', '/')

        self._set_templates(data)

    def _set_templates(self, data):
        default_refdef_template = data.get("refdef-template")
        self.def_templates[""] = VariableReplacer(
            default_refdef_template if default_refdef_template else DEFAULT_REFDEF_TEMPLATE)
        default_ref_template = data.get("ref-template")
        self.ref_templates[""] = VariableReplacer(
            default_ref_template if default_ref_template else DEFAULT_REF_TEMPLATE)
        default_back_ref_template = data.get("back-ref-template")
        self.back_ref_templates[""] = VariableReplacer(
            default_back_ref_template if default_back_ref_template else DEFAULT_BACK_REF_TEMPLATE)
        template_formats = data.get("formats")
        if template_formats:
            for format_code, formats in template_formats.items():
                for template_code, template in formats.items():
                    # There's only one template that may have alternative formats.
                    # For others, it looks like not making sense. But if in future we need to add
                    # other templates, this will be easy to do.
                    if template_code == "ref-template":
                        self.ref_templates[format_code] = VariableReplacer(template)

    def accept_document_list(self, docs: List[Document]):
        self._document_inputs = {
            _normalize_dependency_path(document.input_file) for document in docs
        }

    def accept_app_data(self, plugins: list, options: Options,
                        metadata_handlers: PageMetadataHandlers):
        if self._backrefs_cache_enabled():
            self._load_ref_cache()
            self._prune_backrefs_cache()
            self._populate_references_from_cache(self.references)

    def is_blank(self) -> bool:
        # This plugin has default configuration, so it should never be blank, but theoretically
        # the user may set empty markers and the plugin won't work in fact.
        return not (bool(self.def_markers) or bool(self.ref_markers))

    def page_metadata_handlers(self):
        return [(self, marker, False) for marker in itertools.chain(self.def_markers,
                                                                    self.ref_markers)]

    def accept_page_metadata(self, doc: Document, marker: str, metadata: str,
                             metadata_section: str,
                             visited_markers: Union[Dict[str, None], None] = None,
                             phase: int = 1, data_from_prev_phase=None
                             ) -> MetadataProcessingResult:
        marker = marker.upper()

        # TODO Consider defining separate metadata handlers for `def_markers` and `ref_markers`.
        #  This will make the methods smaller and avoid the lookups.
        if marker in self.def_markers:
            source_code, ref_content = parse_def_metadata(metadata)
            if phase == 1:
                if source_code in self.definitions:
                    raise UserError(f"Source '{source_code}' is defined multiple times")
                anchor_id = self.code_prefix + "def_" + source_code
                definition = Definition(_page_location_from_doc(doc), anchor_id, ref_content)
                self.definitions[source_code] = definition
                return MetadataProcessingResult(anchor_id, defer=True)
            elif phase == 2:
                anchor_id = data_from_prev_phase
                back_refs = self.references.get(source_code, [])
                back_ref_list = []
                back_ref_index = 0
                back_ref_replacer = find_replacer(self.back_ref_templates, "")
                for ref in back_refs:
                    back_ref_index += 1
                    ref_link = relativize_relative_resource(ref.page.output_file, doc.output_file)
                    back_ref_list.append(back_ref_replacer.replace(
                        [f"{ref_link}#{ref.anchor_id}", str(back_ref_index)]))
                back_ref_html = ", ".join(back_ref_list)
                replacer = find_replacer(self.def_templates, "")
                return MetadataProcessingResult(replacer.replace([
                    source_code, anchor_id, back_ref_html, ref_content]))
            else:
                raise Exception(f"Unknown phase: '{phase}'")

        elif marker in self.ref_markers:
            source_code, format_name = parse_ref_metadata(metadata)
            if phase == 1:
                ref_anchor_id = self.code_prefix + "ref_" + source_code
                ref_anchor_id = self.unique_indexer.get_unique(ref_anchor_id)
                ref_page = _page_location_from_doc(doc)

                self._store_reference(source_code, ref_page, ref_anchor_id)

                if source_code in self.definitions:
                    return MetadataProcessingResult(
                        self._generate_ref_html(source_code, ref_anchor_id, doc, format_name))
                else:
                    return MetadataProcessingResult(ref_anchor_id, defer=True)

            elif phase == 2:
                ref_anchor_id = data_from_prev_phase
                if source_code not in self.definitions:
                    raise UserError(f"Source '{source_code}' referenced but not defined")
                return MetadataProcessingResult(
                        self._generate_ref_html(source_code, ref_anchor_id, doc, format_name))
            else:
                raise Exception(f"Unknown phase: '{phase}'")
        else:
            raise Exception(f"Unknown marker: '{marker}'")

    def new_page(self, doc: Document):
        self.unique_indexer = UniqueIndexer()
        if doc.input_file is None:
            return
        page_input = _normalize_dependency_path(doc.input_file)
        self._remove_page_from_definitions(page_input)
        self._remove_referencing_page_from_cache(page_input)
        self._remove_referencing_page_from_references(page_input)

    def finalize(self):
        for source_code, definition in self.definitions.items():
            for ref in self.references.get(source_code, []):
                _record_dependency_pair(ref.page, definition.page)

        if self.backrefs_cache_file:
            self._save_backrefs_cache()
            build_cache_manager.record_standalone_derived_document(self.backrefs_cache_file)

    def _generate_ref_html(self, source_code, ref_anchor_id, doc, format_name):
        definition = self.definitions[source_code]
        link = relativize_relative_resource(definition.page.output_file, doc.output_file)
        replacer = find_replacer(self.ref_templates, format_name)
        return replacer.replace([source_code, ref_anchor_id, f"{link}#{definition.anchor_id}",
                                 definition.ref_content])

    def _backrefs_cache_enabled(self) -> bool:
        return self.backrefs_cache_file is not None

    def _load_ref_cache(self):
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
            json.dump(self.backrefs_cache, file, indent=2)

    def _prune_backrefs_cache(self):
        pruned = {}
        for source_code, referencing_pages in self.backrefs_cache.items():
            kept = [
                referencing_page for referencing_page in referencing_pages
                if referencing_page['input_file'] in self._document_inputs
            ]
            if kept:
                pruned[source_code] = kept
        self.backrefs_cache = pruned

    def _populate_references_from_cache(self, references):
        for source_code, referencing_pages in self.backrefs_cache.items():
            for referencing_page in referencing_pages:
                page = _page_location_from_cache_ref_item(referencing_page)
                for anchor_id in referencing_page['anchor_ids']:
                    references.setdefault(source_code, []).append(Reference(page, anchor_id))

    def _remove_page_from_definitions(self, page_input: str):
        for source_code, definition in list(self.definitions.items()):
            if definition.page.input_file == page_input:
                del self.definitions[source_code]

    def _remove_referencing_page_from_cache(self, page_input: str):
        if not self._backrefs_cache_enabled():
            return
        for source_code, referencing_pages in list(self.backrefs_cache.items()):
            referencing_pages = [
                referencing_page for referencing_page in referencing_pages
                if referencing_page['input_file'] != page_input
            ]
            if referencing_pages:
                self.backrefs_cache[source_code] = referencing_pages
            else:
                del self.backrefs_cache[source_code]

    def _remove_referencing_page_from_references(self, page_input: str):
        for source_code, references in list(self.references.items()):
            references = [
                reference for reference in references
                if reference.page.input_file != page_input
            ]
            if references:
                self.references[source_code] = references
            else:
                del self.references[source_code]

    def _store_reference(self, source_code: str, ref_page: PageLocation, ref_anchor_id: str):
        reference = Reference(ref_page, ref_anchor_id)
        self.references.setdefault(source_code, []).append(reference)
        if not self._backrefs_cache_enabled():
            return
        referencing_pages = self.backrefs_cache.setdefault(source_code, [])
        for referencing_page in referencing_pages:
            if referencing_page['input_file'] == ref_page.input_file:
                referencing_page['anchor_ids'].append(ref_anchor_id)
                return
        referencing_pages.append({
            'input_file': ref_page.input_file,
            'output_file': ref_page.output_file,
            'anchor_ids': [ref_anchor_id],
        })


def _normalize_loaded_cache(data: dict) -> Dict[str, List[dict]]:
    if not isinstance(data, dict):
        return {}
    normalized = {}
    for source_code, referencing_pages in data.items():
        if not isinstance(referencing_pages, list):
            continue
        source_entries = []
        for referencing_page in referencing_pages:
            if not isinstance(referencing_page, dict):
                continue
            input_file = referencing_page.get('input_file')
            output_file = referencing_page.get('output_file')
            anchor_ids = referencing_page.get('anchor_ids')
            if not input_file or not output_file or not anchor_ids:
                continue
            source_entries.append({
                'input_file': _normalize_dependency_path(input_file),
                'output_file': _normalize_dependency_path(output_file),
                'anchor_ids': list(anchor_ids),
            })
        if source_entries:
            normalized[source_code] = source_entries
    return normalized
