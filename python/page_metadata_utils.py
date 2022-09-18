import re
from typing import Iterator, List

from models.document import Document
from plugins.md2html_plugin import Md2HtmlPlugin

METADATA_PATTERN = re.compile(r'^([\w_][\w\d_]*)([^\w\d_]*.*)$', re.DOTALL)


class PageMetadataHandlers:
    def __init__(self, marker_handlers, all_only_at_page_start):
        self.marker_handlers = marker_handlers
        self.all_only_at_page_start = all_only_at_page_start


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
    """
    If this is done completely in regex, it works about 100 times longer.
    """
    done = 0
    current = 0
    while True:
        begin = text.find('<!--', current)
        if begin >= 0:
            end = text.find('-->', begin + 4)
            if end >= 0:
                match = METADATA_PATTERN.search(text[begin + 4:end])
                if match:
                    yield MetadataMatchObject(text[done:begin], match.group(1),
                                              match.group(2), text[begin:end + 3], end + 3)
                    done = end + 3
                current = end + 3
            else:
                return
        else:
            return


def apply_metadata_handlers(text, page_metadata_handlers: PageMetadataHandlers, doc: Document,
                            extract_only=False):
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
                replacement = h.accept_page_metadata(doc, matchObj.marker,
                                                     matchObj.metadata, matchObj.metadata_block)
                replacement_done = True
        if not extract_only:
            new_md_lines_list.append(matchObj.before)
            new_md_lines_list.append(replacement)
        if page_metadata_handlers.all_only_at_page_start:
            break
    if extract_only:
        return None
    else:
        if replacement_done:
            return ''.join(new_md_lines_list) + text[last_position:]
        else:
            return text
