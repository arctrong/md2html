from models.document import Document
from models.options import Options
from plugins.md2html_plugin import Md2HtmlPlugin


class Arguments:
    def __init__(self, options: Options, documents: list[Document], plugins: list[Md2HtmlPlugin]):
        self.options: Options = options
        self.documents: list[Document] = documents
        self.plugins: list[Md2HtmlPlugin] = plugins
