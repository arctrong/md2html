import unittest
from bs4 import BeautifulSoup
from pathlib import Path

import helpers as h


class Md2htmlIgnorePluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('IgnorePluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]
        
    def test_ignore_substitution(self):

        h.run_with_parameters(self.COMMON_PARAMS + ['-f', '--argument-file', 
            str(self.INPUT_DIR.joinpath('md2html_args.json'))])
            
        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
            
        self.assertEqual('IgnorePlugin', root.head.title.text)
        pars = root.body.findAll("p")
        self.assertEqual('page2.html', pars[0].a['href'])
        self.assertEqual('[Page 2](<!--page page2-->)', pars[1].code.text)


if __name__ == '__main__':
    unittest.main()
