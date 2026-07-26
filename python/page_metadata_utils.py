import re
from typing import Iterator, List, Union, Dict, Tuple

from models.document import Document
from models.page_metadata_handlers import PageMetadataHandlers
from plugins.md2html_plugin import Md2HtmlPlugin
from utils import UserError

METADATA_PATTERN = re.compile(r'^([\w_][\w\d_]*)([^\w\d_]*.*)$', re.DOTALL)
METADATA_START = "<!--"
METADATA_END = "-->"
METADATA_START_LEN = len(METADATA_START)
METADATA_END_LEN = len(METADATA_END)
METADATA_DELIMITERS_PATTERN = re.compile(METADATA_START.replace("|", "\\|") + '|' +
                                         METADATA_END.replace("|", "\\|"))
RECURSIVE_MAX_DEPTH = 100


class ParsingResultItem:

    def __init__(self, result, marker: str = None, metadata=None, metadata_section: str = None,
                 marker_key: Union[Tuple[str, bool], None] = None):
        """
        If `marker_key` is `None` the `result` is a direct substitution text, otherwise the page
        is going to be deferred and the `result` is the object that the plugin wants to use later
        (not necessary), when the page is processed of the next phase.
        """
        self.result = result
        self.marker: str = marker
        self.metadata = metadata
        self.metadata_section: str = metadata_section
        self.marker_key: Union[Tuple[str, bool], None] = marker_key


class MetadataHandlersApplicationResult:

    def __init__(self, parsingResults, deferPage):
        """
        `deferPage` is true is at least one `ParsingResultItem` is deferred, i.e. has the
            non-None `plugin`
        """
        self.parsingResults: List[ParsingResultItem] = parsingResults
        self.deferPage = deferPage


def register_page_metadata_handlers(plugins: List[Md2HtmlPlugin]) -> PageMetadataHandlers:
    marker_handlers = {}
    all_only_at_page_start = True
    for plugin in plugins:
        handlers = plugin.page_metadata_handlers()
        if handlers is not None:
            for handler, marker, only_at_page_start in handlers:
                all_only_at_page_start &= only_at_page_start
                key = marker.upper(), only_at_page_start
                marker_handlers.setdefault(key, []).append(handler)
    return PageMetadataHandlers(marker_handlers, all_only_at_page_start)


class MetadataMatchObject:
    def __init__(self, before: str, marker: str, metadata: str, metadata_block: str,
                 end_position: int):
        self.before = before
        self.marker = marker
        self.metadata = metadata
        self.metadata_block = metadata_block
        self.end_position = end_position


def metadata_finder(text: str) -> Iterator[MetadataMatchObject]:
    # When this was done completely in regex, it worked about 100 times longer.
    done = 0
    stack = []
    begin = 0
    for delimiter in METADATA_DELIMITERS_PATTERN.finditer(text):
        if delimiter[0] == METADATA_START:
            stack.append(delimiter.start())
        elif delimiter[0] == METADATA_END:
            if stack:
                begin = stack.pop()
            else:
                continue
        if not stack:
            end = delimiter.end() - METADATA_END_LEN
            match = METADATA_PATTERN.search(text[begin + METADATA_START_LEN:end])
            if match:
                yield MetadataMatchObject(text[done:begin], match.group(1),
                                          match.group(2), text[begin:end + METADATA_END_LEN],
                                          end + METADATA_END_LEN)
                done = end + METADATA_END_LEN


def apply_metadata_handlers(text, page_metadata_handlers: PageMetadataHandlers,
                            doc: Union[Document, None],
                            # In the extract-only mode only the plugins state are modified,
                            # the returned result is not supposed to be used
                            extract_only=False,
                            # Using a `dict` as there's no standard ordered set
                            visited_markers: Union[Dict[str, None], None] = None,
                            recursive_marker: Union[str, None] = None
                            ) -> Union[MetadataHandlersApplicationResult, None]:
    if recursive_marker:
        visited_markers = visited_markers or {}
        if recursive_marker in visited_markers:
            raise UserError(f"Cycle detected at marker: {recursive_marker}, "
                            f"the path is [{','.join(visited_markers)}]")
        visited_markers[recursive_marker] = None
        # Different plugins may have their peculiarities, so we cannot be completely sure
        # that ALL cycles are detected in ALL possible cases.
        if len(visited_markers) > RECURSIVE_MAX_DEPTH:
            cycle_path = '\n'.join(visited_markers)
            raise UserError(f"Cycle SUSPECTED with recursive depth {RECURSIVE_MAX_DEPTH} "
                            f"at marker: {recursive_marker}, the path is [{cycle_path}]")

    marker_handlers = page_metadata_handlers.marker_handlers
    new_md_lines_list = []
    last_position = 0
    replacement_done = False
    defer_page = False
    for matchObj in metadata_finder(text):
        first_non_blank = not bool(matchObj.before.strip())
        last_position = matchObj.end_position
        lookup_marker = matchObj.marker.upper()
        handlers = marker_handlers.get((lookup_marker, first_non_blank))
        if handlers is None and first_non_blank:
            handlers = marker_handlers.get((lookup_marker, False))
        replacement = ParsingResultItem(matchObj.metadata_block)
        if handlers:
            for h in handlers:
                accept_result = h.accept_page_metadata(
                    doc, lookup_marker,
                    matchObj.metadata, matchObj.metadata_block,
                    visited_markers)
                defer_page |= accept_result.defer
                replacement = (ParsingResultItem(result=accept_result.result,
                                                 marker=lookup_marker,
                                                 metadata=matchObj.metadata,
                                                 metadata_section=matchObj.metadata_block,
                                                 marker_key=(lookup_marker, False))
                               if accept_result.defer else ParsingResultItem(accept_result.result))
                replacement_done = True
        if not extract_only:
            new_md_lines_list.append(ParsingResultItem(matchObj.before))
            new_md_lines_list.append(replacement)
        if page_metadata_handlers.all_only_at_page_start:
            break

    if recursive_marker:
        del visited_markers[recursive_marker]

    if extract_only:
        return None
    else:
        if replacement_done:
            new_md_lines_list.append(ParsingResultItem(text[last_position:]))
            return MetadataHandlersApplicationResult(new_md_lines_list, defer_page)
        else:
            return MetadataHandlersApplicationResult([ParsingResultItem(text)], False)


def join_parsing_results(parsing_results: List[ParsingResultItem],
                         page_metadata_handlers: PageMetadataHandlers,
                         doc: Union[Document, None]) -> str:

    marker_handlers = page_metadata_handlers.marker_handlers
    result = []
    for item in parsing_results:
        replacement = item.result
        if item.marker_key:
            handlers = marker_handlers.get(item.marker_key)
            if not handlers:
                raise Exception(
                    f"Deferred metadata marker '{item.marker}' has no handler for phase-2 join "
                    f"(marker key: {item.marker_key!r}). Check plugin registration, "
                    f"e.g. only-at-page-start mismatch.")
            for h in handlers:
                accept_result = h.accept_page_metadata(
                    doc, item.marker, item.metadata, item.metadata_section,
                    # When we join parsing result, final substitution string should be returned
                    phase=2,
                    data_from_prev_phase=item.result)
                # Deferring is not acceptable as there will be no further processing
                if accept_result.defer:
                    raise UserError("Deferred result encountered when processing metadata "
                                    f"marker '{item.marker}' on phase 2. This may mean that "
                                    "this marker cannot be nested inside the other metadata"
                                    "block.")
                replacement = accept_result.result
        result.append(replacement)
    return ''.join(result)


def apply_and_merge_metadata_handlers(text, page_metadata_handlers: PageMetadataHandlers,
                                      doc: Union[Document, None], extract_only=False,
                                      visited_markers: Union[Dict[str, None], None] = None,
                                      recursive_marker: Union[str, None] = None
                            ) -> str:
    apply_metadata_result = apply_metadata_handlers(text, page_metadata_handlers, doc, extract_only,
                                                    visited_markers, recursive_marker)
    return join_parsing_results(apply_metadata_result.parsingResults, page_metadata_handlers, doc)

