import unittest

import helpers as h

SIMPLE_TEST_TEMPLATE = f'{h.INPUT_DIR}/test_template_simple.html'


class Md2htmlContentIntegralTest(unittest.TestCase):

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
        
    def test_br(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/br_test.txt', 
                                f'{self.OUTPUT_DIR}/br_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraphs = root.body.find_all('p')
        self.assertEqual(4, len(paragraphs))
        self.assertIsNone(paragraphs[0].br)
        self.assertIsNone(paragraphs[1].br)
        self.assertIsNotNone(paragraphs[2].br)

    def test_headers(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/headers_test.txt', 
                                f'{self.OUTPUT_DIR}/headers_test.html',
                                SIMPLE_TEST_TEMPLATE)
            
        header = root.body.contents[0].next_sibling
        for i in range(1, 7):
            self.assertEqual(f'h{i}', header.name)
            self.assertEqual(f'Level {i}', header.text)
            header = header.next_sibling.next_sibling

    def test_toc(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/toc_test.txt', 
                                f'{self.OUTPUT_DIR}/toc_test.html',
                                SIMPLE_TEST_TEMPLATE)
        
        item = root.body.ul.li
        for i in range(1, 7):
            self.assertEqual(f'Level {i}', item.a.text)
            item = item.li
    
    def test_links(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/links_test.txt', 
                                f'{self.OUTPUT_DIR}/links_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraphs = root.body.find_all('p')
        self.assertEqual(3, len(paragraphs))
        link = paragraphs[0].a
        self.assertEqual('link text', link.text)
        self.assertEqual('http://test_link', link['href'])
        link = paragraphs[1].a
        self.assertEqual('hinted link text', link.text)
        self.assertEqual('http://test_hinted_link', link['href'])
        self.assertEqual('test hint', link['title'])
        link = paragraphs[2].a
        self.assertEqual('http://test_link_without_text', link.text)
        self.assertEqual('http://test_link_without_text', link['href'])
    
    def test_images(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/images_test.txt',
                                f'{self.OUTPUT_DIR}/images_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraphs = root.body.find_all('p')
        self.assertEqual(3, len(paragraphs))
        image = paragraphs[0].img
        self.assertEqual('../../../test_input/image1.png', image['src'])
        self.assertEqual('', image['alt'])
        image = paragraphs[1].img
        self.assertEqual('../../../test_input/test_image_with_alt.svg', image['src'])
        self.assertEqual('ALT TEXT', image['alt'])
        image = paragraphs[2].img
        self.assertEqual('../../../test_input/image2.jpg', image['src'])
        self.assertEqual('', image['alt'])
        self.assertEqual('test hint', image['title'])
        
        
    def test_images_in_links(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/images_in_links_test.txt',
                                f'{self.OUTPUT_DIR}/images_in_links_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraphs = root.body.find_all('p')
        self.assertEqual(3, len(paragraphs))
        
        link = paragraphs[0].a
        self.assertEqual('http://test_link_simple', link['href'])
        self.assertEqual('link hint 1', link['title'])
        image = link.img
        self.assertEqual('../../../test_input/image1.png', image['src'])

        link = paragraphs[1].a
        self.assertEqual('http://test_link_with_alt', link['href'])
        self.assertEqual('link hint 2', link['title'])
        image = link.img
        self.assertEqual('../../../test_input/test_image_with_alt.svg', image['src'])
        self.assertEqual('ALT TEXT', image['alt'])

        link = paragraphs[2].a
        self.assertEqual('http://test_link_with_hint', link['href'])
        self.assertEqual('link hint 3', link['title'])
        image = link.img
        self.assertEqual('../../../test_input/image2.jpg', image['src'])
        self.assertEqual('image hint', image['title'])

    def test_lists(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/lists_test.txt',
                                f'{self.OUTPUT_DIR}/lists_test.html',
                                SIMPLE_TEST_TEMPLATE)
        
        paragraph = root.body.p
        self.assertEqual('The first list, unordered:', paragraph.text)
        ul = paragraph.next_sibling.next_sibling
        self.assertEqual('ul', ul.name)
        li = ul.li
        for i in range(1, 5):
            self.assertEqual(f'Level {i}', li.contents[0].strip())
            li = li.li
        
        paragraph = ul.next_sibling.next_sibling
        self.assertEqual('p', paragraph.name)
        self.assertEqual('The second list, ordered:', paragraph.text)
        ol = paragraph.next_sibling.next_sibling
        self.assertEqual('ol', ol.name)
        li1 = ol.li
        self.assertEqual('Level 1, item 1', li1.contents[0].strip())
        li2 = li1.li
        self.assertEqual('Level 2, item 1', li2.contents[0].strip())
        li2 = li2.next_sibling.next_sibling
        self.assertEqual('li', li2.name)
        self.assertEqual('Level 2, item 2', li2.contents[0].strip())
        li1 = li1.next_sibling.next_sibling
        self.assertEqual('li', li1.name)
        self.assertEqual('Level 1, item 2', li1.contents[0].strip())

        paragraph = ol.next_sibling.next_sibling
        self.assertEqual('p', paragraph.name)
        self.assertEqual('The third list, mixed:', paragraph.text)
        ol = paragraph.next_sibling.next_sibling
        self.assertEqual('ol', ol.name)
        li1 = ol.li
        self.assertEqual('Level 1, item 1', li1.contents[0].strip())
        li2 = li1.li
        self.assertEqual('Level 2, item 1', li2.contents[0].strip())
        li2 = li2.next_sibling.next_sibling
        self.assertEqual('li', li2.name)
        self.assertEqual('Level 2, item 2', li2.contents[0].strip())
        li1 = li1.next_sibling.next_sibling
        self.assertEqual('li', li1.name)
        self.assertEqual('Level 1, item 2', li1.contents[0].strip())

    def test_blockquotes(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/blockquotes_test.txt',
                                f'{self.OUTPUT_DIR}/blockquotes_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraph = root.body.p
        self.assertEqual('The following is a blockquote:', paragraph.text)
        paragraph = root.body.blockquote.p
        fragment = paragraph.contents[0]
        self.assertEqual('This is the ', fragment)
        fragment = fragment.next_sibling
        self.assertEqual('strong', fragment.name)
        self.assertEqual('content', fragment.text)
        fragment = fragment.next_sibling
        self.assertEqual(' of a ', fragment)
        fragment = fragment.next_sibling
        self.assertEqual('code', fragment.name)
        self.assertEqual('blockquote', fragment.text)
        fragment = fragment.next_sibling
        self.assertEqual('.', fragment)

    def test_fenced_blocks(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/fenced_blocks_test.txt',
                                f'{self.OUTPUT_DIR}/fenced_blocks_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraph = root.body.p
        self.assertEqual('Indented fenced block:', paragraph.text)
        pre = paragraph.next_sibling.next_sibling
        self.assertEqual('pre', pre.name)
        code = pre.code
        self.assertIsNone(code.get('class'))
        self.assertEqual('Indented line 1\nIndented line 2', code.text.strip())
        paragraph = pre.next_sibling.next_sibling
        self.assertEqual('Backticked fenced block:', paragraph.text)
        pre = paragraph.next_sibling.next_sibling
        self.assertEqual('pre', pre.name)
        code = pre.code
        self.assertIsNone(code.get('class'))
        self.assertEqual('Backticked line 1\nBackticked line 2', code.text.strip())
        paragraph = pre.next_sibling.next_sibling
        self.assertEqual('Styled fenced block:', paragraph.text)
        pre = paragraph.next_sibling.next_sibling
        self.assertEqual('pre', pre.name)
        code = pre.code
        self.assertEqual(['language-shell'], code.get('class'))
        self.assertEqual('Styled line 1\nStyled line 2', code.text.strip())

    def test_fenced_blocks_in_blockquotes(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/fenced_blocks_in_blockquotes_test.txt',
                                f'{self.OUTPUT_DIR}/fenced_blocks_in_blockquotes_test.html',
                                SIMPLE_TEST_TEMPLATE)
        
        paragraph = root.body.p
        self.assertEqual('Indented:', paragraph.text)
        blockquote = paragraph.next_sibling.next_sibling
        self.assertEqual('blockquote', blockquote.name)
        paragraph = blockquote.p
        self.assertEqual('Indented start.', paragraph.text)
        pre = paragraph.next_sibling.next_sibling
        self.assertEqual('pre', pre.name)
        code = pre.code
        self.assertIsNone(code.get('class'))
        self.assertEqual('Indented line 1\nIndented line 2', code.text.strip())
        paragraph = pre.next_sibling.next_sibling
        self.assertEqual('Indented end.', paragraph.text)
        
        paragraph = blockquote.next_sibling.next_sibling
        self.assertEqual('Backticked:', paragraph.text)
        blockquote = paragraph.next_sibling.next_sibling
        self.assertEqual('blockquote', blockquote.name)
        paragraph = blockquote.p
        self.assertEqual('Backticked start.', paragraph.text)
        pre = paragraph.next_sibling.next_sibling
        self.assertEqual('pre', pre.name)
        code = pre.code
        self.assertIsNone(code.get('class'))
        self.assertEqual('Backticked line 1\nBackticked line 2', code.text.strip())
        paragraph = pre.next_sibling.next_sibling
        self.assertEqual('Backticked end.', paragraph.text)

        paragraph = blockquote.next_sibling.next_sibling
        self.assertEqual('Styled:', paragraph.text)
        blockquote = paragraph.next_sibling.next_sibling
        self.assertEqual('blockquote', blockquote.name)
        paragraph = blockquote.p
        self.assertEqual('Styled start.', paragraph.text)
        pre = paragraph.next_sibling.next_sibling
        self.assertEqual('pre', pre.name)
        code = pre.code
        self.assertEqual(['language-shell'], code.get('class'))
        self.assertEqual('Styled line 1\nStyled line 2', code.text.strip())
        paragraph = pre.next_sibling.next_sibling
        self.assertEqual('Styled end.', paragraph.text)

    def test_fenced_blocks_in_lists(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/fenced_blocks_in_lists_test.txt',
                                f'{self.OUTPUT_DIR}/fenced_blocks_in_lists_test.html',
                                SIMPLE_TEST_TEMPLATE)

        li1 = root.body.ul.li
        self.assertEqual('Level 1, item 1', li1.contents[0].strip())
        code = li1.pre.code
        self.assertIsNone(code.get('class'))
        self.assertEqual('Backticked line 1, level 1\nBackticked line 2, level 1', code.text.strip())
        li2 = li1.ul.li
        self.assertEqual('Level 2, item 1-1', li2.contents[0].strip())
        code = li2.pre.code
        self.assertIsNone(code.get('class'))
        self.assertEqual('Backticked line 1, level 2\nBackticked line 2, level 2', code.text.strip())
        
        li1 = li1.next_sibling.next_sibling
        self.assertEqual('li', li1.name)
        
        self.assertEqual('Level 1, item 2', li1.contents[0].strip())
        code = li1.pre.code
        self.assertEqual(['language-shell'], code.get('class'))
        self.assertEqual('Styled line 1, level 1\nStyled line 2, level 1', code.text.strip())
        li2 = li1.ul.li
        self.assertEqual('Level 2, item 2-1', li2.contents[0].strip())
        code = li2.pre.code
        self.assertEqual(['language-shell'], code.get('class'))
        self.assertEqual('Styled line 1, level 2\nStyled line 2, level 2', code.text.strip())

    def test_tables(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/tables_test.txt',
                                f'{self.OUTPUT_DIR}/tables_test.html',
                                SIMPLE_TEST_TEMPLATE)

        table = root.body.table
        ths = table.thead.find_all('th')
        self.assertEqual('Default alignment', ths[0].text)
        self.assertEqual('Aligned to the center', ths[1].text)
        self.assertEqual('Aligned to the right', ths[2].text)
        tds = table.tbody.find_all('td')
        self.assertEqual('Default', tds[0].text)
        self.assertEqual('Centered', tds[1].text)
        self.assertEqual('Right', tds[2].text)
        self.assertIsNone(tds[0].get('align'))
        self.assertEqual('center', tds[1].get('align'))
        self.assertEqual('right', tds[2].get('align'))

    def test_admonitions(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/admonitions_test.txt',
                                f'{self.OUTPUT_DIR}/admonitions_test.html',
                                SIMPLE_TEST_TEMPLATE)

        paragraph = root.body.p
        self.assertEqual('The following is an admonition:', paragraph.text)
        admonition_block = root.body.div
        self.assertEqual(['admonition', 'note'], admonition_block.get('class'))
        admonition_title = admonition_block.p
        self.assertEqual(['admonition-title'], admonition_title.get('class'))
        self.assertEqual('Note', admonition_title.text)
        
        fragment = admonition_title.next_sibling.next_sibling
        self.assertEqual('p', fragment.name)
        fragment = fragment.contents[0]
        self.assertEqual('This is the ', fragment)
        fragment = fragment.next_sibling
        self.assertEqual('strong', fragment.name)
        self.assertEqual('content', fragment.text)
        fragment = fragment.next_sibling
        self.assertEqual(' of an ', fragment)
        fragment = fragment.next_sibling
        self.assertEqual('code', fragment.name)
        self.assertEqual('admonition', fragment.text)
        fragment = fragment.next_sibling
        self.assertEqual('.', fragment)

    def test_admonition_custom_header(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/admonition_custom_header.txt',
                                f'{self.OUTPUT_DIR}/admonition_custom_header.html',
                                SIMPLE_TEST_TEMPLATE)

        admonition_block = root.body.div
        self.assertEqual(['admonition', 'hint'], admonition_block.get('class'))
        admonition_title = admonition_block.p
        self.assertEqual('Custom header', admonition_title.text)
        content = admonition_title.next_sibling.next_sibling.contents[0]
        self.assertEqual('This admonition has a custom header.', content)

    def test_admonition_without_header(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/admonition_without_header.txt',
                                f'{self.OUTPUT_DIR}/admonition_without_header.html',
                                SIMPLE_TEST_TEMPLATE)

        admonition_block = root.body.div
        self.assertEqual(['admonition', 'danger'], admonition_block.get('class'))
        content = admonition_block.p.text
        self.assertEqual('This admonition has no header.', content)

    def test_admonition_with_fenced_block(self):
        root = h.execute_simple(f'{h.INPUT_DIR}/admonition_with_fenced_block.txt',
                                f'{self.OUTPUT_DIR}/admonition_with_fenced_block.html',
                                SIMPLE_TEST_TEMPLATE)

        admonition_block = root.body.div
        self.assertEqual(['admonition', 'attention'], admonition_block.get('class'))
        fenced_block = admonition_block.pre.code
        self.assertEqual(['language-code'], fenced_block.get('class'))
        self.assertEqual('print("Code fragment inside an admonition.")', fenced_block.text.strip())


if __name__ == '__main__':
    unittest.main()
