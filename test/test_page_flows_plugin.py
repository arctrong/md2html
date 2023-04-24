import unittest
from pathlib import Path

from bs4 import BeautifulSoup

import helpers as h


class Md2htmlPageFlowsPluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('PageFlowsPlugin')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]

    def test_old_syntax(self):
        h.run_with_parameters(self.COMMON_PARAMS + [
            '-f', '--argument-file',
            str(self.INPUT_DIR.joinpath('md2html_args_old_syntax.json'))
        ])

        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        page_flows_div = root.body.div
        h3 = page_flows_div.h3
        self.assertEqual('page_flow_1', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 1 - current', p.a.text)
        self.assertEqual('page1.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Google', p.a.text)
        self.assertEqual('https://www.google.com/', p.a["href"])

        h3 = p.next_sibling.next_sibling
        self.assertEqual('page_flow_2', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 2', p.a.text)
        self.assertEqual('page2.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])

        with open(Path(self.OUTPUT_DIR).joinpath('page2.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        page_flows_div = root.body.div
        h3 = page_flows_div.h3
        self.assertEqual('page_flow_1', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 1', p.a.text)
        self.assertEqual('page1.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Google', p.a.text)
        self.assertEqual('https://www.google.com/', p.a["href"])

        h3 = p.next_sibling.next_sibling
        self.assertEqual('page_flow_2', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 2 - current', p.a.text)
        self.assertEqual('page2.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])

        with open(Path(self.OUTPUT_DIR).joinpath('page3.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        page_flows_div = root.body.div
        h3 = page_flows_div.h3
        self.assertEqual('page_flow_1', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 1', p.a.text)
        self.assertEqual('page1.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3 - current', p.a.text)
        self.assertEqual('page3.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Google', p.a.text)
        self.assertEqual('https://www.google.com/', p.a["href"])

        h3 = p.next_sibling.next_sibling
        self.assertEqual('page_flow_2', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 2', p.a.text)
        self.assertEqual('page2.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3 - current', p.a.text)
        self.assertEqual('page3.html', p.a["href"])

    def test_new_syntax(self):
        h.run_with_parameters(self.COMMON_PARAMS + [
            '-f', '--argument-file',
            str(self.INPUT_DIR.joinpath('md2html_args_new_syntax.json'))
        ])

        with open(Path(self.OUTPUT_DIR).joinpath('new_syntax/page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        page_flows_div = root.body.div
        p = page_flows_div.p
        self.assertEqual('Page 1 - current', p.a.text)
        self.assertEqual('page1.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])

        h3 = p.next_sibling.next_sibling
        self.assertEqual('Page flow 2', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 2', p.a.text)
        self.assertEqual('page2.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Google', p.a.text)
        self.assertEqual('https://www.google.com/', p.a["href"])

        with open(Path(self.OUTPUT_DIR).joinpath('new_syntax/page2.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        page_flows_div = root.body.div
        p = page_flows_div.p
        self.assertEqual('Page 1', p.a.text)
        self.assertEqual('page1.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])

        h3 = p.next_sibling.next_sibling
        self.assertEqual('Page flow 2', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 2 - current', p.a.text)
        self.assertEqual('page2.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3', p.a.text)
        self.assertEqual('page3.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Google', p.a.text)
        self.assertEqual('https://www.google.com/', p.a["href"])

        with open(Path(self.OUTPUT_DIR).joinpath('new_syntax/page3.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        page_flows_div = root.body.div
        p = page_flows_div.p
        self.assertEqual('Page 1', p.a.text)
        self.assertEqual('page1.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3 - current', p.a.text)
        self.assertEqual('page3.html', p.a["href"])

        h3 = p.next_sibling.next_sibling
        self.assertEqual('Page flow 2', h3.text)
        p = h3.next_sibling.next_sibling
        self.assertEqual('Page 2', p.a.text)
        self.assertEqual('page2.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Page 3 - current', p.a.text)
        self.assertEqual('page3.html', p.a["href"])
        p = p.next_sibling.next_sibling
        self.assertEqual('Google', p.a.text)
        self.assertEqual('https://www.google.com/', p.a["href"])


if __name__ == '__main__':
    unittest.main()
