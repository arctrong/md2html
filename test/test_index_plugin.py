import unittest
from bs4 import BeautifulSoup
from pathlib import Path

import helpers as h


anchor_attr = 'name'


class Md2htmlIndexPluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('IndexPluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]
    
    def test_index_page_generation(self):

        h.run_with_parameters(self.COMMON_PARAMS + ['-f', '--argument-file', 
            str(self.INPUT_DIR.joinpath('md2html_args.json'))])
        with open(Path(self.OUTPUT_DIR).joinpath('index_page.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
            
        entry_link = root.body.p.a
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor2 = href[1]
        self.assertEqual('a single entry as text', entry_link.text)
        
        entry_link = entry_link.parent.next_sibling.next_sibling.a
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor3b = href[1]
        self.assertEqual('b single entry as JSON', entry_link.text)
        
        entry_link = entry_link.parent.next_sibling.next_sibling.a
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor3c = href[1]
        self.assertEqual('c single entry as JSON', entry_link.text)
        
        self.assertEqual(page1_anchor3b, page1_anchor3c)

        entry_paragraph = entry_link.parent.next_sibling.next_sibling.contents
        self.assertEqual('d multiple entries in the same page: ', entry_paragraph[0])
        entry_link = entry_paragraph[1]
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor4 = href[1]
        self.assertEqual('1', entry_link.text)    
        self.assertEqual(', ', entry_paragraph[2])
        entry_link = entry_paragraph[3]
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor5 = href[1]
        self.assertEqual('2', entry_link.text)
 
        entry_paragraph = entry_link.parent.next_sibling.next_sibling.contents
        self.assertEqual('e multiple entries in different pages: ', entry_paragraph[0])
        entry_link = entry_paragraph[1]
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor6 = href[1]
        self.assertEqual('1', entry_link.text)    
        self.assertEqual(', ', entry_paragraph[2])
        entry_link = entry_paragraph[3]
        self.assertEqual('Page 2', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page2.html', href[0])
        page2_anchor1 = href[1]
        self.assertEqual('2', entry_link.text)

        entry_link = entry_link.parent.next_sibling.next_sibling.a
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor7 = href[1]
        self.assertEqual('F Entry with initial capital', entry_link.text)

        entry_paragraph = entry_link.parent.next_sibling.next_sibling.contents
        self.assertEqual('g multiple entries as text and JSON in the same page: ', entry_paragraph[0])
        entry_link = entry_paragraph[1]
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor1 = href[1]
        self.assertEqual('1', entry_link.text)    
        self.assertEqual(', ', entry_paragraph[2])
        entry_link = entry_paragraph[3]
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor8g = href[1]
        self.assertEqual('2', entry_link.text)

        entry_paragraph = entry_link.parent.next_sibling.next_sibling.contents
        self.assertEqual('h multiple entries as text and JSON in different page: ', entry_paragraph[0])
        entry_link = entry_paragraph[1]
        self.assertEqual('Page 1', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page1.html', href[0])
        page1_anchor8h = href[1]
        self.assertEqual('1', entry_link.text)    
        self.assertEqual(', ', entry_paragraph[2])
        entry_link = entry_paragraph[3]
        self.assertEqual('Page 2', entry_link['title'])
        href = entry_link['href'].split('#')
        self.assertEqual('page2.html', href[0])
        page2_anchor2 = href[1]
        self.assertEqual('2', entry_link.text)
        
        self.assertEqual(page1_anchor8g, page1_anchor8h)
        
        footer = entry_link.parent.next_sibling.next_sibling
        self.assertIn('Generator name: md2html_', footer.text)
        footer = footer.next_sibling.next_sibling
        self.assertEqual('Custom variable: custom value 1', footer.text)

        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
            
        anchor = root.body.p.a
        self.assertEqual(page1_anchor1, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('g multiple entries as text and JSON in the same page', descr.string)
        
        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor2, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('a single entry as text', descr.string) 

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor3b, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('b single entry as JSON', descr.string)
        descr = descr.next_sibling.next_sibling
        self.assertIn('c single entry as JSON', descr.string)

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor4, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('d multiple entries in the same page', descr.string)

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor5, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('d multiple entries in the same page', descr.string)

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor6, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('e multiple entries in different pages', descr.string)

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor7, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('F Entry with initial capital', descr.string)

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page1_anchor8g, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('g multiple entries as text and JSON in the same page', descr.string)
        descr = descr.next_sibling.next_sibling
        self.assertIn('h multiple entries as text and JSON in different page', descr.string)

        with open(Path(self.OUTPUT_DIR).joinpath('page2.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
            
        anchor = root.body.p.a
        self.assertEqual(page2_anchor1, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('e multiple entries in different pages', descr.string)

        anchor = anchor.parent.next_sibling.next_sibling.a
        self.assertEqual(page2_anchor2, anchor[anchor_attr])
        descr = anchor.next_sibling.next_sibling
        self.assertIn('h multiple entries as text and JSON in different page', descr.string)

    def test_partial_generation(self):

        # Generating index for two files (forcefully)
        h.run_with_parameters(self.COMMON_PARAMS + ['-f', '--argument-file', 
            str(self.INPUT_DIR.joinpath('md2html_args.json'))])
        with open(Path(self.OUTPUT_DIR).joinpath('index_page.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
            
        self.assertIsNotNone(root.body.find(name='a', string='a single entry as text'))
        self.assertIsNone(root.body.find(name='a', 
            string='v entry for testing partial regeneration'))
        
        # Generating index for three files (partially)
        h.run_with_parameters(self.COMMON_PARAMS + ['--argument-file', 
            str(self.INPUT_DIR.joinpath('md2html_args_partial.json'))])
        with open(Path(self.OUTPUT_DIR).joinpath('index_page.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')

        self.assertIsNotNone(root.body.find(name='a', string='a single entry as text'))
        self.assertIsNotNone(root.body.find(name='a', 
            string='v entry for testing partial regeneration'))


if __name__ == '__main__':
    unittest.main()
