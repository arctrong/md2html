class Options:

    def __init__(self, verbose=False, legacy_mode=False):
        self.verbose = verbose
        self.legacy_mode = legacy_mode


class Document:

    def __init__(self, input_file=None, output_file=None, title=None, template=None, link_css=None,
                 include_css=None, no_css=None, force=None, verbose=None, report=None):
        self.input_file = input_file
        self.output_file = output_file
        self.title = title
        self.template = template
        self.link_css = link_css
        self.include_css = include_css
        self.no_css = no_css
        self.force = force
        self.verbose = verbose
        self.report = report


class Arguments:
    def __init__(self, options: Options, documents: list[Document]):
        self.options: Options = options
        self.documents: list[Document] = documents
