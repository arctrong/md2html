import re
import unittest

import helpers as h


class Md2htmlTemplateIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)

    def test_empty_title(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/any_content.txt', 
                                f'{self.OUTPUT_DIR}/empty_title_test.html',
                                f'{h.INPUT_DIR}/test_template_title.html')
        
        self.assertEqual('', root.head.title.text)

    def test_not_empty_title_cli(self):
        output_file = f'{self.OUTPUT_DIR}/not_empty_title_cli_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/any_content.txt', '-o', output_file, 
                          '--template', f'{h.INPUT_DIR}/test_template_title.html',
                          '--title', 'test title from CLI'], 
                          output_file)
        
        self.assertEqual('test title from CLI', root.head.title.text)

    def test_not_empty_title_metadata(self):
        output_file = f'{self.OUTPUT_DIR}/not_empty_title_metadata_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/not_empty_title_metadata_test.txt',
                          '-o', output_file, '--legacy-mode',
                          '--template', f'{h.INPUT_DIR}/test_template_title.html'], 
                          output_file)
        
        self.assertEqual('test title from metadata', root.head.title.text)

    def test_not_empty_title_cli_overridden(self):
        output_file = f'{self.OUTPUT_DIR}/not_empty_title_cli_overridden_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/not_empty_title_metadata_test.txt',
                          '-o', output_file, 
                          '--template', f'{h.INPUT_DIR}/test_template_title.html',
                          '--title', 'test title from CLI overridden'], 
                          output_file)
        
        self.assertEqual('test title from CLI overridden', root.head.title.text)

    def test_no_css(self):
        output_file = f'{self.OUTPUT_DIR}/no_css_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/any_content.txt', '-o', output_file, 
                          '--template', f'{h.INPUT_DIR}/test_template_styles.html',
                          '--no-css'], output_file)
        
        self.assertIsNone(root.head.link)
        self.assertIsNone(root.head.style)

    def test_link_css(self):
        output_file = f'{self.OUTPUT_DIR}/link_css_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/any_content.txt', '-o', output_file, 
                          '--template', f'{h.INPUT_DIR}/test_template_styles.html',
                          '--link-css', '../../../test_input/test_styles.css'],
                          output_file)
        
        self.assertIsNone(root.head.style)
        link = root.head.link
        self.assertEqual(['stylesheet'], link['rel'])
        self.assertEqual('text/css', link['type'])
        self.assertEqual('../../../test_input/test_styles.css', link['href'])

    def test_include_css(self):
        output_file = f'{self.OUTPUT_DIR}/include_css_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/any_content.txt', '-o', output_file, 
                          '--template', f'{h.INPUT_DIR}/test_template_styles.html',
                          '--include-css', 
                          str(h.WORKING_DIR.joinpath('test_input/test_styles.css'))],
                          output_file)
        
        self.assertIsNone(root.head.link)
        style = root.head.style
        self.assertEqual('body {background-color: burlywood;}', style.contents[0].strip())

    def test_placeholders(self):
        output_file = f'{self.OUTPUT_DIR}/placeholders_test.html'
        root = h.execute(['-f', '-i', f'{h.INPUT_DIR}/placeholders_test.txt', '-o', output_file, 
                          '--template', f'{h.INPUT_DIR}/test_template_placeholders.html',
                          '--no-css', '--legacy-mode'],
                          output_file)
                          
        pattern = re.compile('\d')
        paragraphs = root.body.find_all('p')
        self.assertEqual(5, len(paragraphs))
        self.assertEqual('Generator name: md2html_', paragraphs[0].text[0:24])
        self.assertEqual('Generator version: X.X.X', pattern.sub('X', paragraphs[1].text))
        self.assertEqual('Generation date: XXXX-XX-XX', pattern.sub('X', paragraphs[2].text))
        self.assertEqual('Generation time: XX:XX:XX', pattern.sub('X', paragraphs[3].text))
        self.assertEqual('Custom value: test custom value', paragraphs[4].text)


if __name__ == '__main__':
    unittest.main()
