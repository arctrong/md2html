import contextlib
import unittest

from md2html import *


class NonWritable:
    def write(self, *args, **kwargs):
        pass


class CliArgParseTest(unittest.TestCase):

    def _assertMd2HtmlOptionsEquals(self, o1, o2):
        self.assertEqual(o1.input_root, o2.input_root)
        self.assertEqual(o1.output_root, o2.output_root)
        self.assertEqual(o1.input_file, o2.input_file)
        self.assertEqual(o1.output_file, o2.output_file)
        self.assertEqual(o1.title, o2.title)
        self.assertEqual(o1.template, o2.template)
        self.assertEqual(o1.include_css, o2.include_css)
        self.assertEqual(o1.link_css, o2.link_css)
        self.assertEqual(o1.force, o2.force)
        self.assertEqual(o1.verbose, o2.verbose)
        self.assertEqual(o1.report, o2.report)
        
    def _assert_cli_error(self, arguments: list):
        try:
            parse_cli_arguments(arguments)
            self.fail("Exception expected")
        except CliError as e:
            pass

    def test_helpRequested(self):
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-h'])
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['--help'])
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-i \"whatever\"', '--help'])

    def test_noInputFile(self):
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-t', 'whatever'])

    def test_noInputFileWithArgumentFile(self):
        with contextlib.redirect_stdout(NonWritable()):
            parse_cli_arguments(['-t', 'whatever', '--argument-file=md2html_args.json'])

    def test_minimalArgumentSet(self):
        md2html_args = parse_cli_arguments(['-i', '../doc/notes.md'])
        args = parse_argument_file({"documents": [{}]}, md2html_args)
        doc = args.documents[0]
        self.assertEqual('../doc/notes.md', doc.input_file)
        self.assertEqual('../doc/notes.html', doc.output_file)
        self.assertFalse(doc.title)
        # Template path depends on the environment and is not checked here.
        self.assertFalse(doc.link_css)
        self.assertTrue(doc.include_css)
        self.assertFalse(doc.force)
        self.assertFalse(doc.verbose)
        self.assertFalse(doc.report)

    def test_maxArguments(self):
        # Short form
        md2html_args = parse_cli_arguments(
                ['--input-root', 'input/root', '--output-root', 'output/root', 
                 '-i', 'input.md', '-o', 'doc/output.htm', '-t', 'someTitle', '--template',
                 '../templateDir', '--link-css=someStyles.css', '-fv'])
        self.assertEqual('input/root', md2html_args.input_root)
        self.assertEqual('input.md', md2html_args.input_file)
        self.assertEqual('output/root', md2html_args.output_root)
        self.assertEqual('doc/output.htm', md2html_args.output_file)
        self.assertEqual('someTitle', md2html_args.title)
        self.assertEqual(Path('../templateDir'), md2html_args.template)
        self.assertEqual(1, len(md2html_args.link_css))
        self.assertEqual('someStyles.css', md2html_args.link_css[0])
        self.assertFalse(md2html_args.include_css)
        self.assertTrue(md2html_args.force)
        self.assertTrue(md2html_args.verbose)
        self.assertFalse(md2html_args.report)
        # Long form
        md2html_args1 = parse_cli_arguments(
            ['--input-root', 'input/root', '--output-root', 'output/root', 
             '--input', 'input.md', '--output=doc/output.htm', '--title', 'someTitle', '--template',
             '../templateDir', '--link-css', 'someStyles.css', '--force', '--verbose'])
        self._assertMd2HtmlOptionsEquals(md2html_args, md2html_args1)

    def test_includeCss(self):
        md2html_args = parse_cli_arguments(
            ['-i', 'input.md', '--include-css=styles1.css', '--include-css=styles2.css'])
        self.assertEqual(2, len(md2html_args.include_css))
        self.assertTrue('styles1.css' in md2html_args.include_css)
        self.assertTrue('styles2.css' in md2html_args.include_css)
        self.assertFalse(md2html_args.link_css)

    def test_linkCss(self):
        md2html_args = parse_cli_arguments(
            ['-i', 'input.md', '--link-css=styles1.css', '--link-css=styles2.css'])
        self.assertEqual(2, len(md2html_args.link_css))
        self.assertTrue('styles1.css' in md2html_args.link_css)
        self.assertTrue('styles2.css' in md2html_args.link_css)
        self.assertFalse(md2html_args.include_css)

    def test_linkAndIncludeCss(self):
        md2html_args = parse_cli_arguments(
            ['-i', 'input.md', '--link-css=styles1.css', '--include-css=styles2.css'])
        self.assertEqual(1, len(md2html_args.link_css))
        self.assertEqual(1, len(md2html_args.include_css))
        self.assertTrue('styles1.css' in md2html_args.link_css)
        self.assertTrue('styles2.css' in md2html_args.include_css)

    def test_defaultCss(self):
        md2html_args = parse_cli_arguments(['-i', 'input.md'])
        args = parse_argument_file({"documents": [{}]}, md2html_args)
        doc = args.documents[0]
        self.assertFalse(doc.link_css)
        self.assertEqual(1, len(doc.include_css))

    def test_noCss(self):
        md2html_args = parse_cli_arguments(['-i', 'input.md', '--no-css'])
        self.assertFalse(md2html_args.link_css)
        self.assertFalse(md2html_args.include_css)

    def test_wrongVerboseAndReportFlags(self):
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-i', 'readme.txt', '-vr'])

    def test_wrongNoCssAndCss(self):
        for css_type in ["link-css", "include-css"]:
            with self.subTest(css_type=css_type):
                with contextlib.redirect_stdout(NonWritable()):
                    self._assert_cli_error(['-i', 'readme.txt', '--no-css', '--' + css_type,
                                            'styles.css'])

    def test_argumentFile(self):
        md2html_args = parse_cli_arguments(['--argument-file', 'md2html_args.json'])
        self.assertEqual(Path('md2html_args.json'), md2html_args.argument_file)
