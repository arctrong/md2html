import unittest
from bs4 import BeautifulSoup
from pathlib import Path

import helpers as h


class Md2htmlIncludeFilePluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('IncludeFilePluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]

    def test_simple(self):

        h.run_with_parameters(
            self.COMMON_PARAMS +
            ['-f', '--argument-file', str(self.INPUT_DIR.joinpath('md2html_args.json'))]
        )

        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')

        self.assertEqual('IncludeFilePlugin test', root.head.title.text)
        h1 = root.body.h1
        self.assertEqual('IncludeFilePlugin test', h1.text)

        p = h1.find_next_sibling('p')
        self.assertEqual('Include Java: [some Java code]', p.text)

        p = p.find_next_sibling('p')
        self.assertEqual('Include Shell: [\nsome Shell script\n]', p.text)

        p = p.find_next_sibling('p')
        self.assertEqual('Include Code 1: [some source code]', p.text)

        p = p.find_next_sibling('p')
        self.assertEqual('Include Code 2: [some source code]', p.text)

        p = p.find_next_sibling('p')
        self.assertEqual('Include recursive: some text 1, [[some text 2]]', p.text)

        p = p.find_next_sibling('p')
        self.assertEqual('Include partially (text): [body]text inside body[/body]', p.text)
        
        p = p.find_next_sibling('p')
        self.assertEqual('Include partially (markers): Inclusion with markered substring', p.text)

        p = p.find_next_sibling('p')
        self.assertEqual('Include partially (per file, text): [body]text inside body[/body]', p.text)
        
        p = p.find_next_sibling('p')
        self.assertEqual('Include partially (per file, markers): Inclusion with markered substring', p.text)


if __name__ == '__main__':
    unittest.main()
