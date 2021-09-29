import contextlib
import sys
import unittest
from pathlib import Path

from argument_file_utils import enrich_document

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


class NonWritable:
    def write(self, *args, **kwargs):
        pass


class CliArgParseTest(unittest.TestCase):

    def _assertMd2HtmlOptionsEquals(self, o1, o2):
        self.assertEqual(o1['input_file'], o2['input_file'])
        self.assertEqual(o1['output_file'], o2['output_file'])
        self.assertEqual(o1['title'], o2['title'])
        self.assertEqual(o1['template'], o2['template'])
        self.assertEqual(o1['include_css'], o2['include_css'])
        self.assertEqual(o1['link_css'], o2['link_css'])
        self.assertEqual(o1['force'], o2['force'])
        self.assertEqual(o1['verbose'], o2['verbose'])
        self.assertEqual(o1['report'], o2['report'])
        
    def _assert_cli_error(self, arguments: list, help_requested: bool = False):
        try:
            parse_cli_arguments(arguments)
            self.fail("Exception expected")
        except CliError as e:
            self.assertEqual(help_requested, e.help_requested)

    def test_parse_md2html_arguments_helpRequested(self):
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-h'], True)
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['--help'], True)
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-i \"whatever\"', '--help'], True)

    def test_parse_md2html_arguments_noInputFile(self):
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-t', 'whatever'])

    def test_parse_md2html_arguments_noInputFileWithArgumentFile(self):
        with contextlib.redirect_stdout(NonWritable()):
            parse_cli_arguments(['-t', 'whatever', '--argument-file=md2html_args.json'])

    def test_parse_md2html_arguments_minimalArgumentSet(self):
        md2html_args = parse_cli_arguments(['-i', '../doc/notes.md'])
        enrich_document(md2html_args)
        self.assertEqual('../doc/notes.md', md2html_args['input_file'])
        self.assertEqual('../doc/notes.html', md2html_args['output_file'])
        self.assertFalse(md2html_args['title'])
        # Template path depends on the environment and is not checked here.
        self.assertFalse(md2html_args['link_css'])
        self.assertTrue(md2html_args['include_css'])
        self.assertFalse(md2html_args['force'])
        self.assertFalse(md2html_args['verbose'])
        self.assertFalse(md2html_args['report'])

    def test_parse_md2html_arguments_maxArguments(self):
        # Short form
        md2html_args = parse_cli_arguments(
                ['-i', 'input.md', '-o', 'doc/output.htm', '-t', 'someTitle', '--template', '../templateDir',
                 '--link-css=someStyles.css', '-fv'])
        self.assertEqual('input.md', md2html_args['input_file'])
        self.assertEqual('doc/output.htm', md2html_args['output_file'])
        self.assertEqual('someTitle', md2html_args['title'])
        self.assertEqual(Path('../templateDir'), md2html_args['template'])
        self.assertEqual(1, len(md2html_args['link_css']))
        self.assertEqual('someStyles.css', md2html_args['link_css'][0])
        self.assertFalse(md2html_args['include_css'])
        self.assertTrue(md2html_args['force'])
        self.assertTrue(md2html_args['verbose'])
        self.assertFalse(md2html_args['report'])
        # Long form
        md2html_args1 = parse_cli_arguments(
            ['--input', 'input.md', '--output=doc/output.htm', '--title', 'someTitle', '--template', '../templateDir',
             '--link-css', 'someStyles.css', '--force', '--verbose'])
        self._assertMd2HtmlOptionsEquals(md2html_args, md2html_args1)

    def test_parse_md2html_arguments_includeCss(self):
        md2html_args = parse_cli_arguments(
            ['-i', 'input.md', '--include-css=styles1.css', '--include-css=styles2.css'])
        self.assertEqual(2, len(md2html_args['include_css']))
        self.assertTrue(Path('styles1.css') in md2html_args['include_css'])
        self.assertTrue(Path('styles2.css') in md2html_args['include_css'])
        self.assertFalse(md2html_args['link_css'])

    def test_parse_md2html_arguments_linkCss(self):
        md2html_args = parse_cli_arguments(
            ['-i', 'input.md', '--link-css=styles1.css', '--link-css=styles2.css'])
        self.assertEqual(2, len(md2html_args['link_css']))
        self.assertTrue('styles1.css' in md2html_args['link_css'])
        self.assertTrue('styles2.css' in md2html_args['link_css'])
        self.assertFalse(md2html_args['include_css'])

    def test_parse_md2html_arguments_linkAndIncludeCss(self):
        md2html_args = parse_cli_arguments(
            ['-i', 'input.md', '--link-css=styles1.css', '--include-css=styles2.css'])
        self.assertEqual(1, len(md2html_args['link_css']))
        self.assertEqual(1, len(md2html_args['include_css']))
        self.assertTrue('styles1.css' in md2html_args['link_css'])
        self.assertTrue(Path('styles2.css') in md2html_args['include_css'])

    def test_parse_md2html_arguments_defaultCss(self):
        md2html_args = parse_cli_arguments(['-i', 'input.md'])
        enrich_document(md2html_args)
        self.assertFalse(md2html_args['link_css'])
        self.assertEqual(1, len(md2html_args['include_css']))

    def test_parse_md2html_arguments_noCss(self):
        md2html_args = parse_cli_arguments(['-i', 'input.md', '--no-css'])
        self.assertFalse(md2html_args['link_css'])
        self.assertFalse(md2html_args['include_css'])

    def test_parse_md2html_arguments_wrongVerboseAndReportFlags(self):
        with contextlib.redirect_stdout(NonWritable()):
            self._assert_cli_error(['-i', 'readme.txt', '-vr'])

    def test_parse_md2html_arguments_wrongNoCssAndCss(self):
        for css_type in ["link-css", "include-css"]:
            with self.subTest(css_type=css_type):
                with contextlib.redirect_stdout(NonWritable()):
                    self._assert_cli_error(['-i', 'readme.txt', '--no-css', '--' + css_type, 'styles.css'])

    def test_parse_md2html_arguments_argumentFile(self):
        md2html_args = parse_cli_arguments(['--argument-file', 'md2html_args.json'])
        self.assertEqual(Path('md2html_args.json'), md2html_args['argument_file'])


if __name__ == '__main__':
    unittest.main()
