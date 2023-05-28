import unittest
from bs4 import BeautifulSoup
from pathlib import Path

import helpers as h


class Md2htmlWrapCodePluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('WrapCodePluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]

    def test_simple(self):

        h.run_with_parameters(
            self.COMMON_PARAMS +
            ['-f', '--argument-file', str(self.INPUT_DIR.joinpath('md2html_args.json'))]
        )

        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')

        self.assertEqual('Page 1', root.head.title.text)
        h1 = root.body.h1
        self.assertEqual('Page template', h1.text)

        p = h1.find_next_sibling('p')
        self.assertEqual('Page 1', p.text)

        p = p.find_next_sibling('p')
        elements = p.contents
        self.assertEqual('Link 1 to Java: ', elements[0])
        a = elements[1]
        self.assertEqual('source2.java', a.text)
        self.assertEqual('code/java/source2.java.html', a['href'])

        p = p.find_next_sibling('p')
        elements = p.contents
        self.assertEqual('Link 2 to Java: ', elements[0])
        a = elements[1]
        self.assertEqual('source2.java', a.text)
        self.assertEqual('code/java/source2.java.html', a['href'])

        p = p.find_next_sibling('p')
        elements = p.contents
        self.assertEqual('Link 1 to Shell: ', elements[0])
        a = elements[1]
        self.assertEqual('source1.shell.txt', a.text)
        self.assertEqual('code/shell/source1.shell.txt.html', a['href'])

        p = p.find_next_sibling('p')
        elements = p.contents
        self.assertEqual('Link 2 to Shell: ', elements[0])
        a = elements[1]
        self.assertEqual('source1.shell.txt', a.text)
        self.assertEqual('code/shell/source1.shell.txt.html', a['href'])

        with open(Path(self.OUTPUT_DIR).joinpath('code/java/source2.java.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')

        self.assertEqual('source2.java', root.head.title.text)
        h1 = root.body.h1
        self.assertEqual('Code template', h1.text)

        p = h1.find_next_sibling('p')
        self.assertEqual('Path: code/java/source2.java', p.text)
        p = p.find_next_sibling('p')
        self.assertEqual('File name: source2.java', p.text)

        self.assertEqual('some Java code', p.find_next_sibling('pre').code.text)

        with open(Path(self.OUTPUT_DIR).joinpath('code/shell/source1.shell.txt.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')

        self.assertEqual('source1.shell.txt', root.head.title.text)
        h1 = root.body.h1
        self.assertEqual('Code template', h1.text)

        p = h1.find_next_sibling('p')
        self.assertEqual('Path: code/shell/source1.shell.txt', p.text)
        p = p.find_next_sibling('p')
        self.assertEqual('File name: source1.shell.txt', p.text)

        self.assertEqual('some Shell script', p.find_next_sibling('pre').code.text)


if __name__ == '__main__':
    unittest.main()
