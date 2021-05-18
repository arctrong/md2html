import contextlib
import sys
import unittest
from pathlib import Path

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *


class NonWritable:
    def write(self, *args, **kwargs):
        pass


class ArgParseTest(unittest.TestCase):

    def _assertMd2HtmlOptionsEquals(self, o1, o2):
        self.assertEqual(o1['input_file'], o2['input_file'])
        self.assertEqual(o1['output_file'], o2['output_file'])
        self.assertEqual(o1['title'], o2['title'])
        self.assertEqual(o1['template_dir'], o2['template_dir'])
        self.assertEqual(o1['include_css'], o2['include_css'])
        self.assertEqual(o1['link_css'], o2['link_css'])
        self.assertEqual(o1['force'], o2['force'])
        self.assertEqual(o1['verbose'], o2['verbose'])
        self.assertEqual(o1['report'], o2['report'])

    def test_parse_md2html_arguments_minimalArgumentSet(self):
        result_type, md2html_args = parse_md2html_arguments(['-i', '../doc/notes.md'])
        self.assertEqual('success', result_type)
        self.assertEqual(Path('../doc/notes.md'), md2html_args['input_file'])
        self.assertEqual(Path('../doc/notes.html'), md2html_args['output_file'])
        self.assertFalse(md2html_args['title'])
        # Template path depends on the environment and is not checked here.
        self.assertFalse(md2html_args['link_css'])
        self.assertTrue(md2html_args['include_css'])
        self.assertFalse(md2html_args['force'])
        self.assertFalse(md2html_args['verbose'])
        self.assertFalse(md2html_args['report'])

    def test_parse_md2html_arguments_maxArguments(self):
        # Short form
        result_type, md2html_args = parse_md2html_arguments(
                ['-i', 'input.md', '-o', 'doc/output.htm', '-t', 'someTitle', '--template', '../templateDir',
                 '--link-css=someStyles.css', '-fv'])
        self.assertEqual('success', result_type)
        self.assertEqual(Path('input.md'), md2html_args['input_file'])
        self.assertEqual(Path('doc/output.htm'), md2html_args['output_file'])
        self.assertEqual('someTitle', md2html_args['title'])
        self.assertEqual(Path('../templateDir'), md2html_args['template_dir'])
        self.assertEqual(1, len(md2html_args['link_css']))
        self.assertEqual('someStyles.css', md2html_args['link_css'][0])
        self.assertFalse(md2html_args['include_css'])
        self.assertTrue(md2html_args['force'])
        self.assertTrue(md2html_args['verbose'])
        self.assertFalse(md2html_args['report'])
        # Long form
        result_type1, md2html_args1 = parse_md2html_arguments(
            ['--input', 'input.md', '--output=doc/output.htm', '--title', 'someTitle', '--template', '../templateDir',
             '--link-css', 'someStyles.css', '--force', '--verbose'])
        self.assertEqual('success', result_type1)
        self._assertMd2HtmlOptionsEquals(md2html_args, md2html_args1)

    def test_parse_md2html_arguments_includeCss(self):
        result_type, md2html_args = parse_md2html_arguments(
            ['-i', 'input.md', '--include-css=styles1.css', '--include-css=styles2.css'])
        self.assertEqual('success', result_type)
        self.assertEqual(2, len(md2html_args['include_css']))
        self.assertTrue(Path('styles1.css') in md2html_args['include_css'])
        self.assertTrue(Path('styles2.css') in md2html_args['include_css'])
        self.assertFalse(md2html_args['link_css'])

    def test_parse_md2html_arguments_linkCss(self):
        result_type, md2html_args = parse_md2html_arguments(
            ['-i', 'input.md', '--link-css=styles1.css', '--link-css=styles2.css'])
        self.assertEqual('success', result_type)
        self.assertEqual(2, len(md2html_args['link_css']))
        self.assertTrue('styles1.css' in md2html_args['link_css'])
        self.assertTrue('styles2.css' in md2html_args['link_css'])
        self.assertFalse(md2html_args['include_css'])

    def test_parse_md2html_arguments_linkAndIncludeCss(self):
        result_type, md2html_args = parse_md2html_arguments(
            ['-i', 'input.md', '--link-css=styles1.css', '--include-css=styles2.css'])
        self.assertEqual('success', result_type)
        self.assertEqual(1, len(md2html_args['link_css']))
        self.assertEqual(1, len(md2html_args['include_css']))
        self.assertTrue('styles1.css' in md2html_args['link_css'])
        self.assertTrue(Path('styles2.css') in md2html_args['include_css'])

    def test_parse_md2html_arguments_defaultCss(self):
        result_type, md2html_args = parse_md2html_arguments(['-i', 'input.md'])
        self.assertEqual('success', result_type)
        self.assertFalse(md2html_args['link_css'])
        self.assertEqual(1, len(md2html_args['include_css']))

    def test_parse_md2html_arguments_noCss(self):
        result_type, md2html_args = parse_md2html_arguments(['-i', 'input.md', '--no-css'])
        self.assertEqual('success', result_type)
        self.assertFalse(md2html_args['link_css'])
        self.assertFalse(md2html_args['include_css'])

    def test_parse_md2html_arguments_wrongVerboseAndReportFlags(self):
        with contextlib.redirect_stdout(NonWritable()):
            result_type, md2html_args = parse_md2html_arguments(['-i', 'readme.txt', '-vr'])
        self.assertEqual('error', result_type)

    def test_parse_md2html_arguments_wrongNoCssAndCss(self):
        with contextlib.redirect_stdout(NonWritable()):
            result_type, md2html_args = parse_md2html_arguments(
                ['-i', 'readme.txt', '--no-css', '--include-css', 'styles.css'])
        self.assertEqual('error', result_type)
        with contextlib.redirect_stdout(NonWritable()):
            result_type, md2html_args = parse_md2html_arguments(
                ['-i', 'readme.txt', '--no-css', '--link-css', 'styles.css'])
        self.assertEqual('error', result_type)


if __name__ == '__main__':
    unittest.main()
