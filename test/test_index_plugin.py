import unittest

# import helpers as h

# SIMPLE_TEST_TEMPLATE = f'{h.INPUT_DIR}/test_template_simple.html'


class Md2htmlIndexPluginIntegralTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.OUTPUT_DIR = h.prepare_output_directory(cls.__name__)
    
    def test_formatting(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/formatting_test.txt', 
                                f'{self.OUTPUT_DIR}/formatting_test.html',
                                SIMPLE_TEST_TEMPLATE)
        
        paragraphs = root.body.find_all('p')
        self.assertEqual(1, len(paragraphs))
        paragraph_contents = paragraphs[0].contents
        self.assertEqual(7, len(paragraph_contents))
        self.assertEqual('Simple text with ', paragraph_contents[0])
        em_fragment = paragraph_contents[1]
        self.assertEqual('em', em_fragment.name)
        self.assertEqual('italic', em_fragment.text)
        self.assertEqual(', ', paragraph_contents[2])
        strong_fragment = paragraph_contents[3]
        self.assertEqual('strong', strong_fragment.name)
        self.assertEqual('bold', strong_fragment.text)
        self.assertEqual(' and ', paragraph_contents[4])
        code_fragment = paragraph_contents[5]
        self.assertEqual('code', code_fragment.name)
        self.assertEqual('monospaced', code_fragment.text)
        self.assertEqual(' fragments.', paragraph_contents[6])
        



if __name__ == '__main__':
    unittest.main()
