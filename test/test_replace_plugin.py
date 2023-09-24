import unittest
from typing import List

from bs4 import BeautifulSoup, Tag
from pathlib import Path

import helpers as h


class Md2htmlReplacePluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('ReplacePluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]
        
    def test_replace(self):

        h.run_with_parameters(
            self.COMMON_PARAMS +
            ['-f', '--argument-file', str(self.INPUT_DIR.joinpath('md2html_args.json'))])
            
        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
            
        self.assertEqual('ReplacePlugin', root.head.title.text)
        pars: List[Tag] = root.body.findAll("p")
        self.assertEqual('Test 1: pref1+VALUE+suff1 etc.', pars[0].text)
        self.assertEqual('Test 2: pref1+VALUE+suff1 etc.', pars[1].text)
        self.assertEqual('Test 3: pref2+VALUE1 +middle2++suff2 etc.', pars[2].text)
        self.assertEqual('Test 4: pref2+VALUE1+middle2+VALUE2+suff2 etc.', pars[3].text)
        self.assertEqual('Test 5: V2 m2 v1 m1 etc.', pars[4].text)
        self.assertEqual('<p>Test 6: V3 m3 <!--m1 v1--> etc.</p>', str(pars[5]))


if __name__ == '__main__':
    unittest.main()
