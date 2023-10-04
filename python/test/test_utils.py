import unittest

from utils import *


class UtilTest(unittest.TestCase):

    def test_strip_extension(self):
        self.assertEqual('file_with_one', strip_extension('file_with_one.extension'))
        self.assertEqual('file_with.two', strip_extension('file_with.two.extensions'))
        self.assertEqual('file_without_extension', strip_extension('file_without_extension'))
        self.assertEqual('.file_without_name', strip_extension('.file_without_name'))
        self.assertEqual('a', strip_extension('a.file_with_short_name'))
        self.assertEqual('file/with/path', strip_extension('file/with/path.txt'))
        self.assertEqual('with\\path_without_ext', strip_extension('with\\path_without_ext'))
        self.assertEqual('with/dotted.path/name', strip_extension('with/dotted.path/name.ext'))
        self.assertEqual('with\\dotted.path/name', strip_extension('with\\dotted.path/name'))
        self.assertEqual('with/dotted.path/.name', strip_extension('with/dotted.path/.name'))
        self.assertEqual('with\\path/a', strip_extension('with\\path/a.ext'))

    def test_strip_extension_Path_input(self):
        self.assertEqual('file_with.two', strip_extension('file_with.two.extensions'))

    def test_strip_extension_special_cases(self):
        with self.assertRaises(TypeError):
            strip_extension(None)
        self.assertEqual(Path(''),
                         Path(strip_extension(Path(''))))
        self.assertEqual('file_with.two',
                         strip_extension(Path('file_with.two.extensions')))
        self.assertEqual(Path('file/with/path'),
                         Path(strip_extension(Path('file/with/path.txt'))))
        self.assertEqual(Path('file\\with\\path'),
                         Path(strip_extension(Path('file\\with\\path.txt'))))

    def test_blank_comment_line(self):
        self.assertEqual('        ', blank_comment_line('#comment', '#'))
        self.assertEqual('        \n', blank_comment_line('#comment\n', '#'))
        self.assertEqual('        \r\n', blank_comment_line('#comment\r\n', '#'))
        self.assertEqual('         ', blank_comment_line(' #comment', '#'))
        self.assertEqual('not comment', blank_comment_line('not comment', '#'))
        self.assertEqual('not # comment', blank_comment_line('not # comment', '#'))
        self.assertEqual(' ', blank_comment_line('#', '#'))
        self.assertEqual(' \n', blank_comment_line('#\n', '#'))

    def test_first_not_none(self):
        self.assertEqual(1, first_not_none(None, 1, 2))
        self.assertEqual(None, first_not_none(None, None, None))
        self.assertEqual(None, first_not_none())
        self.assertEqual([], first_not_none([], [1, 2]))
        d = {'a': 1}
        self.assertEqual(1, first_not_none(d.get('b'), d.get('a')))

    def test_relativize_relative_resource_path(self):
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc/', '')
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc/', 'path/')
        with self.assertRaises(ValueError):
            #  noinspection PyTypeChecker
            relativize_relative_resource_path('doc/', None)
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('doc', 'index.html')
        with self.assertRaises(ValueError):
            relativize_relative_resource_path('/', 'index.html')

        self.assertEqual('../', relativize_relative_resource_path('../', 'index.html'))
        self.assertEqual('', relativize_relative_resource_path('', 'index.html'))
        self.assertEqual('doc/', relativize_relative_resource_path('doc/', 'index.html'))
        self.assertEqual('doc/pict/', relativize_relative_resource_path('doc/pict/', 'index.html'))

        self.assertEqual('../../', relativize_relative_resource_path('../', 'doc/index.html'))
        self.assertEqual('../', relativize_relative_resource_path('', 'doc/index.html'))
        self.assertEqual('', relativize_relative_resource_path('doc/', 'doc/index.html'))
        self.assertEqual('pict/', relativize_relative_resource_path('doc/pict/', 'doc/index.html'))

        self.assertEqual('../pict/',
                         relativize_relative_resource_path('pict/', 'doc/index.html'))
        self.assertEqual('../pict/doc/',
                         relativize_relative_resource_path('pict/doc/', 'doc/index.html'))
        self.assertEqual('../../pict/',
                         relativize_relative_resource_path('pict/', 'doc/chapter01/index.html'))
        self.assertEqual('../../pict/doc/',
                         relativize_relative_resource_path('pict/doc/', 'doc/chapter01/index.html'))

        self.assertEqual('', relativize_relative_resource_path('./', 'index.html'))
        self.assertEqual('../', relativize_relative_resource_path('./', 'doc/index.html'))

    def test_relativize_relative_resource(self):
        with self.assertRaises(ValueError):
            relativize_relative_resource('styles.css', '')
        with self.assertRaises(ValueError):
            relativize_relative_resource('styles.css', 'doc/')
        with self.assertRaises(ValueError):
            relativize_relative_resource('styles.css', None)
        with self.assertRaises(ValueError):
            relativize_relative_resource('', 'index.html')
        with self.assertRaises(ValueError):
            relativize_relative_resource('doc/', 'index.html')
        with self.assertRaises(ValueError):
            relativize_relative_resource(None, 'index.html')

        self.assertEqual('styles.css',
                         relativize_relative_resource('styles.css', 'index.html'))
        self.assertEqual('doc/styles.css',
                         relativize_relative_resource('doc/styles.css', 'index.html'))
        self.assertEqual('doc/pict/logo.png',
                         relativize_relative_resource('doc/pict/logo.png', 'index.html'))

        self.assertEqual('../../logo.png',
                         relativize_relative_resource('../logo.png', 'doc/index.html'))
        self.assertEqual('../logo.png',
                         relativize_relative_resource('logo.png', 'doc/index.html'))
        self.assertEqual('logo.png',
                         relativize_relative_resource('doc/logo.png', 'doc/index.html'))
        self.assertEqual('pict/logo.png',
                         relativize_relative_resource('doc/pict/logo.png', 'doc/index.html'))

        self.assertEqual('../pict/logo.png',
                         relativize_relative_resource('pict/logo.png', 'doc/index.html'))
        self.assertEqual('../pict/doc/logo.png',
                         relativize_relative_resource('pict/doc/logo.png', 'doc/index.html'))
        self.assertEqual('../../pict/logo.png',
                         relativize_relative_resource('pict/logo.png', 'doc/chapter01/index.html'))
        self.assertEqual('../../pict/doc/logo.png',
                         relativize_relative_resource('pict/doc/logo.png',
                                                      'doc/chapter01/index.html'))

        self.assertEqual('logo.png',
                         relativize_relative_resource('./logo.png', 'index.html'))
        self.assertEqual('../logo.png',
                         relativize_relative_resource('./logo.png', 'doc/index.html'))

    def test_variable_replacer_positive(self):
        for test_case in (
            ("simple", "start${1}middle${2}end", ["-A-", "-B-"], "start-A-middle-B-end"),
            ("at start", "${1}end", ["-A-"], "-A-end"),
            ("at end", "start${1}", ["-A-"], "start-A-"),
            ("with spaces", "start${\n\t1 \n}end", ["-A-"], "start-A-end"),
            ("not enough values", "start${1}middle${2} end", ["-A-"], "start-A-middle end"),
            ("with masking in the middle", "start$${1}end", [], "start${1}end"),
            ("with masking at start", "$${1}end", [], "${1}end"),
            ("with masking at end", "start$$", [], "start$"),
            ("with marker at end", "start$", [], "start$"),
            ("no positions", "something", ["X"], "something"),
            ("multi digit position", "start-${11}-end", ["1", "2", "3", "4", "5", "6", "7", "8", "9",
                                                         "10", "11ok"], "start-11ok-end"),
        ):
            with self.subTest(test_name=test_case[0]):
                replacer = VariableReplacer(test_case[1])
                self.assertEqual(test_case[3], replacer.replace(test_case[2]))

    def test_variable_replacer_negative(self):
        for test_case in (
            ("not a digit", "start${not-a-digit}end", [], "not-a-digit"),
            ("position is zero", "start${0}end", [], "0"),
            ("position too small", "start${-43}end", [], "-43"),
            ("no closing brace", "start${1", [], "brace"),
            ("no closing brace at the end", "start${", [], "brace"),
        ):
            with self.subTest(test_name=test_case[0]):
                with self.assertRaises(VariableReplacerError) as cm:
                    replacer = VariableReplacer(test_case[1])
                    replacer.replace(test_case[2])
                self.assertTrue(test_case[3] in str(cm.exception))

    # TODO delete later
    # def test_smart_substring(self):
    #     for case in (
    #         ("xxx SW yyy", "SW", "", "", "", "SW yyy"),
    #         ("xxx EW yyy", "", "EW", "", "", "xxx EW"),
    #         ("xxx SM yyy", "", "", "SM", "", " yyy"),
    #         ("xxx EM yyy", "", "", "", "EM", "xxx "),
    #         ("aaa SW bbb SM ccc", "SW", "", "SM", "", " ccc"),
    #         ("aaa SM bbb SW ccc", "SW", "", "SM", "", "SW ccc"),
    #         ("aaa EW bbb EM ccc", "", "EW", "", "EM", "aaa EW"),
    #         ("aaa EM bbb EW ccc", "", "EW", "", "EM", "aaa "),
    #         ("aaa SW bbb EW ccc", "SW", "EW", "", "", "SW bbb EW"),
    #         ("aaa SM bbb EM ccc", "", "", "SM", "EM", " bbb "),
    #         ("no start_with", "SW", "", "", "", ""),
    #         ("no start_marker", "", "", "SM", "", ""),
    #         ("no end_with", "", "EW", "", "", "no end_with"),
    #         ("no end_marker", "", "", "", "EM", "no end_marker"),
    #     ):
    #         with self.subTest(test_name=case[0]):
    #             self.assertEqual(case[5],
    #                              smart_substring(case[0], case[1], case[2], case[3], case[4]))

    def test_mask_regex_chars(self):
        for case in (
            ("no replacements", "no replacements"),
            (r"?^\$.|*+][)(}{", r"\?\^\\\$\.\|\*\+\]\[\)\(\}\{"),
            (r"[x]", r"\[x\]"),
        ):
            with self.subTest(test_name=case[0]):
                self.assertEqual(case[1], mask_regex_chars(case[0]))

    def test_smart_substringer(self):
        for case in (
            (" do not substring ", "", "", "", "", " do not substring "),
            ("xxx SW yyy", "SW", "", "", "", "SW yyy"),
            ("xxx EW yyy", "", "EW", "", "", "xxx EW"),
            ("xxx SM yyy", "", "", "SM", "", " yyy"),
            ("xxx EM yyy", "", "", "", "EM", "xxx "),
            ("xxx SW yyy EW zzz", "SW", "EW", "", "", "SW yyy EW"),
            ("xxx SM yyy EM zzz", "", "", "SM", "EM", " yyy "),
            ("aaa SW bbb SM ccc", "SW", "", "SM", "", "SW bbb SM ccc"),
            ("aaa SM bbb SW ccc", "SW", "", "SM", "", " bbb SW ccc"),
            ("aaa EW bbb EM ccc", "", "EW", "", "EM", "aaa EW"),
            ("aaa EM bbb EW ccc", "", "EW", "", "EM", "aaa "),
            ("no start_with", "SW", "", "", "", ""),
            ("no start_marker", "", "", "SM", "", ""),
            ("no end_with", "", "EW", "", "", "no end_with"),
            ("no end_marker", "", "", "", "EM", "no end_marker"),
            ("SW at start ", "SW", "", "", "", "SW at start "),
            ("SM at start ", "", "", "SM", "", " at start "),
            (" at end EW", "", "EW", "", "", " at end EW"),
            (" at end EM", "", "", "", "EM", " at end "),
            ("EW at start", "", "EW", "", "", "EW"),
            ("EM at start", "", "", "", "EM", ""),
            (" at end SW", "SW", "", "", "", "SW"),
            (" at end SM", "", "", "SM", "", ""),
            ("uuu EW after SW www", "SW", "EW", "", "", ""),
            ("uuu EM after SM www", "", "", "SM", "EM", ""),
            (r"start with ?^\$.|*+][)(}{ all RE chars", r"?^\$.|*+][)(}{", "", "", "",
             r"?^\$.|*+][)(}{ all RE chars"),
            (r"a }SM{ some RE chars ]EM[ b", "", "", r"}SM{", r"]EM[", " some RE chars "),
        ):
            with self.subTest(test_name=case[0]):
                substringer = SmartSubstringer(case[1], case[2], case[3], case[4])
                self.assertEqual(case[5], substringer.substring(case[0]))
