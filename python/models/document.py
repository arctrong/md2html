class Document:
    def __init__(self, input_file=None, output_file=None, title=None, code=None, template=None,
                 link_css=None, include_css=None, no_css=None, force=None, verbose=None,
                 report=None):
        self.input_file = input_file
        self.output_file = output_file
        self.title = title
        self.code = code
        self.template = template
        self.link_css = link_css
        self.include_css = include_css
        self.no_css = no_css
        self.force = force
        self.verbose = verbose
        self.report = report

    def copy(self, input_file=None, output_file=None, title=None, code=None, template=None,
             link_css=None, include_css=None, no_css=None, force=None, verbose=None,
             report=None):
        return Document(
            input_file=self.input_file if input_file is None else input_file,
            output_file=self.output_file if output_file is None else output_file,
            title=self.title if title is None else title,
            code=self.code if code is None else code,
            template=self.template if template is None else template,
            link_css=self.link_css if link_css is None else link_css,
            include_css=self.include_css if include_css is None else include_css,
            no_css=self.no_css if no_css is None else no_css,
            force=self.force if force is None else force,
            verbose=self.verbose if verbose is None else verbose,
            report=self.report if report is None else report
        )
