from typing import List

from models.document import Document
from models.options import Options
from models.page_metadata_handlers import PageMetadataHandlers
from plugins.md2html_plugin import Md2HtmlPlugin


class Arguments:
    def __init__(self, options: Options, documents: List[Document], plugins: List[Md2HtmlPlugin],
                 metadata_handlers: PageMetadataHandlers = None):
        self.options: Options = options
        self.documents: List[Document] = documents
        self.plugins: List[Md2HtmlPlugin] = plugins
        self.metadata_handlers: PageMetadataHandlers = metadata_handlers
