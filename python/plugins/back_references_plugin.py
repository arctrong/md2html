import itertools
import re
from pathlib import Path
from typing import Dict, Union

from models.document import Document
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


def parse_ref_metadata(metadata):
    fields = metadata.split()
    if 1 > len(fields) > 2:
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


class BackReferencesPlugin(Md2HtmlPlugin):

    def __init__(self):
        super().__init__()
        # Maps source_code -> (defining_page_output_file, anchor_id, ref_content)
        self.source_definitions: Dict[str, tuple] = {}
        # Maps source_code -> list of (referencing_page_output_file, anchor_id)
        self.source_references: Dict[str, list] = {}
        self.def_markers = []
        self.ref_markers = []
        self.code_prefix = "backref_"
        self.refdef_templates: Dict[str, VariableReplacer] = {}    # format -> template
        self.ref_templates: Dict[str, VariableReplacer] = {}       # format -> template
        self.back_ref_templates: Dict[str, VariableReplacer] = {}  # format -> template
        self.unique_indexer: Union[UniqueIndexer, None] = None

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

        self._set_templates(data)

    def _set_templates(self, data):
        default_refdef_template = data.get("refdef-template")
        self.refdef_templates[""] = VariableReplacer(
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
                    # For others it looks like not making sense. But if in future we need to add
                    # other templates, this will be easy to do.
                    if template_code == "ref-template":
                        self.ref_templates[format_code] = VariableReplacer(template)

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

        if marker in self.def_markers:
            source_code, ref_content = parse_def_metadata(metadata)
            if phase == 1:
                if source_code in self.source_definitions:
                    raise UserError(f"Source '{source_code}' is defined multiple times")
                anchor_id = self.code_prefix + "def_" + source_code
                self.source_definitions[source_code] = (doc.output_file, anchor_id, ref_content)
                return MetadataProcessingResult(anchor_id, defer=True)
            elif phase == 2:
                anchor_id = data_from_prev_phase
                back_refs = self.source_references.get(source_code, [])
                back_ref_list = []
                back_ref_index = 0
                back_ref_replacer = find_replacer(self.back_ref_templates, "")
                for ref_page, anchor in back_refs:
                    back_ref_index += 1
                    ref_link = relativize_relative_resource(ref_page, doc.output_file)
                    back_ref_list.append(back_ref_replacer.replace(
                        [f"{ref_link}#{anchor}", str(back_ref_index)]))
                back_ref_html = ", ".join(back_ref_list)
                replacer = find_replacer(self.refdef_templates, "")
                return MetadataProcessingResult(replacer.replace([
                    source_code, anchor_id, back_ref_html, ref_content]))

        elif marker in self.ref_markers:
            source_code, format_name = parse_ref_metadata(metadata)
            if phase == 1:
                ref_anchor_id = self.code_prefix + "ref_" + source_code
                ref_anchor_id = self.unique_indexer.get_unique(ref_anchor_id)

                if source_code not in self.source_references:
                    self.source_references[source_code] = []
                self.source_references[source_code].append((doc.output_file, ref_anchor_id))

                if source_code in self.source_definitions:
                    return MetadataProcessingResult(
                        self._generate_ref_html(source_code, ref_anchor_id, doc, format_name))
                else:
                    return MetadataProcessingResult(ref_anchor_id, defer=True)

            elif phase == 2:
                ref_anchor_id = data_from_prev_phase
                if source_code not in self.source_definitions:
                    raise UserError(f"Source '{source_code}' referenced but not defined")
                return MetadataProcessingResult(
                        self._generate_ref_html(source_code, ref_anchor_id, doc, format_name))

    def new_page(self, doc: Document):
        self.unique_indexer = UniqueIndexer()

    def _generate_ref_html(self, source_code, ref_anchor_id, doc, format_name):
        def_page, def_anchor_id, ref_content = self.source_definitions[source_code]
        link = relativize_relative_resource(def_page, doc.output_file)
        replacer = find_replacer(self.ref_templates, format_name)
        return replacer.replace([source_code, ref_anchor_id, f"{link}#{def_anchor_id}",
                                 ref_content])

