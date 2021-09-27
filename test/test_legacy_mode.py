import unittest
import os
from bs4 import BeautifulSoup
import helpers as h
import re


class LegacyModeTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.CLASS_NAME = cls.__name__
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)

    def test_legacy_mode(self):
        without_arg_file = h.execute_simple(f'{h.INPUT_DIR}/{self.CLASS_NAME}/legacy_source_text.txt', 
                                            f'{self.OUTPUT_DIR}/test_in_legacy_mode_without_arg_file.html',
                                            f'{h.INPUT_DIR}/{self.CLASS_NAME}/legacy_template.html',
                                            '--legacy-mode', 
                                            '--link-css', '../../../test_input/test_styles.css')

        output_file = f'{self.OUTPUT_DIR}/test_in_legacy_mode_with_arg_file.html'
        with_arg_file = h.execute(['--argument-file', f'{h.INPUT_DIR}/{self.CLASS_NAME}/argument_file.json',
                                  '--output', output_file, '--legacy-mode'], output_file)
        
        for root, test_name in [(without_arg_file, 'without_arg_file'), (with_arg_file, 'with_arg_file')]:
            with self.subTest(test_name=test_name):
                self.assertEqual('test title from metadata', root.head.title.text)

                self.assertIsNone(root.head.style)
                link = root.head.link
                self.assertEqual(['stylesheet'], link['rel'])
                self.assertEqual('text/css', link['type'])
                self.assertEqual('../../../test_input/test_styles.css', link['href'])
                
                pattern = re.compile('\d')
                paragraphs = root.body.find_all('p')
                self.assertEqual(6, len(paragraphs))
                self.assertEqual('Generator name: md2html_', paragraphs[0].text[0:24])
                self.assertEqual('Generator version: X.X.X', pattern.sub('X', paragraphs[1].text))
                self.assertEqual('Generation date: XXXX-XX-XX', pattern.sub('X', paragraphs[2].text))
                self.assertEqual('Generation time: XX:XX:XX', pattern.sub('X', paragraphs[3].text))
                self.assertEqual('Custom value: test custom value', paragraphs[4].text)
                self.assertEqual('Legacy content.', paragraphs[5].text)


if __name__ == '__main__':
    unittest.main()
