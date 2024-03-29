import re
from typing import Iterator, List, Union, Dict

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


def register_page_metadata_handlers(plugins: List[Md2HtmlPlugin]) -> PageMetadataHandlers:
    marker_handlers = {}
    all_only_at_page_start = True
    for plugin in plugins:
        handlers = plugin.page_metadata_handlers()
        if handlers is not None:
            for handler, marker, only_at_page_start in handlers:
                if not only_at_page_start:
                    all_only_at_page_start = False
                key = marker.upper(), only_at_page_start
                value = marker_handlers.setdefault(key, [])
                value.append(handler)
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


def apply_metadata_handlers(text, page_metadata_handlers: PageMetadataHandlers, doc: Union[Document, None],
                            extract_only=False,
                            # Using a `dict` as there's no standard ordered set
                            visited_markers: Union[Dict[str, None], None] = None,
                            recursive_marker: Union[str, None] = None
                            ):
    if recursive_marker:
        visited_markers = visited_markers or {}
        if recursive_marker in visited_markers:
            raise UserError(f"Cycle detected at marker: {recursive_marker}, "
                            f"path is [{','.join(visited_markers)}]")
        visited_markers[recursive_marker] = None
        # Different plugin may have their peculiarities, so we cannot be completely sure
        # that ALL cycles are detected in ALL possible cases.
        if len(visited_markers) > RECURSIVE_MAX_DEPTH:
            cycle_path = '\n'.join(visited_markers)
            raise UserError(f"Cycle SUSPECTED with recursive depth {RECURSIVE_MAX_DEPTH} "
                            f"at marker: {recursive_marker}, path is [{cycle_path}]")

    marker_handlers = page_metadata_handlers.marker_handlers
    new_md_lines_list = []
    last_position = 0
    replacement_done = False
    for matchObj in metadata_finder(text):
        first_non_blank = not bool(matchObj.before.strip())
        last_position = matchObj.end_position
        lookup_marker = matchObj.marker.upper()
        handlers = marker_handlers.get((lookup_marker, first_non_blank))
        if handlers is None and first_non_blank:
            handlers = marker_handlers.get((lookup_marker, False))
        replacement = matchObj.metadata_block
        if handlers:
            for h in handlers:
                replacement = h.accept_page_metadata(doc, lookup_marker,
                                                     matchObj.metadata, matchObj.metadata_block,
                                                     visited_markers)
                replacement_done = True
        if not extract_only:
            new_md_lines_list.append(matchObj.before)
            new_md_lines_list.append(replacement)
        if page_metadata_handlers.all_only_at_page_start:
            break

    if recursive_marker:
        del visited_markers[recursive_marker]

    if extract_only:
        return None
    else:
        if replacement_done:
            return ''.join(new_md_lines_list) + text[last_position:]
        else:
            return text
