import unittest
from bs4 import BeautifulSoup, Comment, NavigableString
from pathlib import Path

import helpers as h


class Md2htmlRelativePathsPluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
        cls.INPUT_DIR = Path(h.INPUT_DIR).joinpath('RelativePathsPluginTest')
        cls.COMMON_PARAMS = ['--input-root', str(cls.INPUT_DIR), '--output-root', cls.OUTPUT_DIR]

    def test_relative_paths_new_syntax(self):

        h.run_with_parameters(self.COMMON_PARAMS + ['-f', '--argument-file',
            str(self.INPUT_DIR.joinpath('md2html_args.json'))])

        with open(Path(self.OUTPUT_DIR).joinpath('page1.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        self.assertEqual('Page 1 - relative-paths', root.head.title.text)
        pars = root.body.findAll("p")
        self.assertEqual('../../../test_input/RelativePathsPluginTest/pict1/image1.png',
                         pars[0].img['src'])
        self.assertEqual('../../../test_input/RelativePathsPluginTest/pict2/image2.jpg',
                         pars[1].img['src'])
        self.assertEqual('../../../test_input/RelativePathsPluginTest/pict1/image1.png',
                         pars[2].img['src'])
        self.assertEqual('../../../test_input/RelativePathsPluginTest/pict2/image2.jpg',
                         pars[3].img['src'])
        comment: NavigableString = root.find(text=lambda text: isinstance(text, Comment))
        self.assertEqual("p1 wrong_path", comment)

        with open(Path(self.OUTPUT_DIR).joinpath('subdir/page2_glob.html')) as html_file:
            root = BeautifulSoup(html_file, 'html.parser')
        pars = root.body.findAll("p")
        self.assertEqual('../../../../test_input/RelativePathsPluginTest/pict1/image1.png',
                         pars[0].img['src'])
        self.assertEqual('../../../../test_input/RelativePathsPluginTest/pict2/image2.jpg',
                         pars[1].img['src'])
        self.assertEqual('../../../../test_input/RelativePathsPluginTest/pict1/image1.png',
                         pars[2].img['src'])
        self.assertEqual('../../../../test_input/RelativePathsPluginTest/pict2/image2.jpg',
                         pars[3].img['src'])


if __name__ == '__main__':
    unittest.main()
