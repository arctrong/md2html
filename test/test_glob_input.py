import unittest
from bs4 import BeautifulSoup
from pathlib import Path

import helpers as h


class Md2htmlGlobIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('GlobInputTest')
        h.run_with_parameters(['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR,
            '-f', '--argument-file', str(cls.INPUT_DIR.joinpath('md2html_args.json'))])
            
    def _read_output_file(self, file_name):
        with open(Path(self.OUTPUT_DIR).joinpath(file_name)) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        return root
    
    def test_glob_processing_and_ordering(self):
        root = self._read_output_file('single_doc.html')
        
        all_pages_links = root.div
        link = all_pages_links.a
        self.assertEqual('single_doc.html', link['href'])
        self.assertEqual('Single document', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir01/subdir01_page01.html', link['href'])
        self.assertEqual('sort-by-file-name: page01', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir01/subdir01_page02.html', link['href'])
        self.assertEqual('sort-by-file-name: page02', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir01/subdir01_page03.html', link['href'])
        self.assertEqual('sort-by-file-name: page03', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir02/subdir02_page03.html', link['href'])
        self.assertEqual('sort-by-variable: page03', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir02/subdir02_page02.html', link['href'])
        self.assertEqual('sort-by-variable: page02', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir02/subdir02_page01.html', link['href'])
        self.assertEqual('sort-by-variable: page01', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir03/subdir03_page02.html', link['href'])
        self.assertEqual('sort-by-title: Title09', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir03/subdir03_page01.html', link['href'])
        self.assertEqual('sort-by-title: Title12', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir03/subdir03_page03.html', link['href'])
        self.assertEqual('sort-by-title: Title15', link.text)

        subdir01_links = all_pages_links.next_sibling.next_sibling
        link = subdir01_links.a
        self.assertEqual('subdir01/subdir01_page01.html', link['href'])
        self.assertEqual('sort-by-file-name: page01', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir01/subdir01_page02.html', link['href'])
        self.assertEqual('sort-by-file-name: page02', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir01/subdir01_page03.html', link['href'])
        self.assertEqual('sort-by-file-name: page03', link.text)

        subdir02_links = subdir01_links.next_sibling.next_sibling
        link = subdir02_links.a
        self.assertEqual('subdir02/subdir02_page03.html', link['href'])
        self.assertEqual('sort-by-variable: page03', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir02/subdir02_page02.html', link['href'])
        self.assertEqual('sort-by-variable: page02', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir02/subdir02_page01.html', link['href'])
        self.assertEqual('sort-by-variable: page01', link.text)

        subdir03_links = subdir02_links.next_sibling.next_sibling
        link = subdir03_links.a
        self.assertEqual('subdir03/subdir03_page02.html', link['href'])
        self.assertEqual('sort-by-title: Title09', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir03/subdir03_page01.html', link['href'])
        self.assertEqual('sort-by-title: Title12', link.text)
        link = link.next_sibling.next_sibling.next_sibling
        self.assertEqual('subdir03/subdir03_page03.html', link['href'])
        self.assertEqual('sort-by-title: Title15', link.text)
        
    def test_file_generation(self):
        content = self._read_output_file('single_doc.html').p.strong
        self.assertEqual('This is a single document', content.text)
        
        content = self._read_output_file('subdir01/subdir01_page01.html').p.strong
        self.assertEqual('Subdirectory 1, page 1', content.text)
        content = self._read_output_file('subdir01/subdir01_page02.html').p.strong
        self.assertEqual('Subdirectory 1, page 2', content.text)
        content = self._read_output_file('subdir01/subdir01_page03.html').p.strong
        self.assertEqual('Subdirectory 1, page 3', content.text)
        
        content = self._read_output_file('subdir02/subdir02_page01.html').p.strong
        self.assertEqual('Subdirectory 2, page 1', content.text)
        content = self._read_output_file('subdir02/subdir02_page02.html').p.strong
        self.assertEqual('Subdirectory 2, page 2', content.text)
        content = self._read_output_file('subdir02/subdir02_page03.html').p.strong
        self.assertEqual('Subdirectory 2, page 3', content.text)

        content = self._read_output_file('subdir03/subdir03_page01.html').p.strong
        self.assertEqual('Subdirectory 3, page 1', content.text)
        content = self._read_output_file('subdir03/subdir03_page02.html').p.strong
        self.assertEqual('Subdirectory 3, page 2', content.text)
        content = self._read_output_file('subdir03/subdir03_page03.html').p.strong
        self.assertEqual('Subdirectory 3, page 3', content.text)






if __name__ == '__main__':
    unittest.main()
