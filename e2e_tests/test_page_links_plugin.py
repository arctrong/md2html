import unittest
from bs4 import BeautifulSoup
from pathlib import Path

import helpers as h


class Md2htmlPageLinksPluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('PageLinksPluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]
        
    def test_page_link_substitution(self):

        h.run_with_parameters(self.COMMON_PARAMS + ['-f', '--argument-file', 
            str(self.INPUT_DIR.joinpath('md2html_args.json'))])
            
        with open(Path(self.OUTPUT_DIR).joinpath('page1_single.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        self.assertEqual('Page 1', root.head.title.text)
        pars = root.body.findAll("p")
        self.assertEqual('page2_single.html', pars[1].a['href'])
        self.assertEqual('subdir/page4_glob.html', pars[3].a['href'])
        
        with open(Path(self.OUTPUT_DIR).joinpath('page2_single.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        self.assertEqual('Page 2', root.head.title.text)
        pars = root.body.findAll("p")
        self.assertEqual('page2_single.html', pars[1].a['href'])
        self.assertEqual('subdir/page4_glob.html', pars[3].a['href'])
        
        with open(Path(self.OUTPUT_DIR).joinpath('subdir/page3_glob.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        self.assertEqual('', root.head.title.text)
        pars = root.body.findAll("p")
        self.assertEqual('../page2_single.html', pars[1].a['href'])
        self.assertEqual('page4_glob.html', pars[3].a['href']) 

        with open(Path(self.OUTPUT_DIR).joinpath('subdir/page4_glob.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        self.assertEqual('Page 4', root.head.title.text)
        pars = root.body.findAll("p")
        self.assertEqual('../page2_single.html', pars[1].a['href'])
        self.assertEqual('page4_glob.html', pars[3].a['href'])


if __name__ == '__main__':
    unittest.main()
