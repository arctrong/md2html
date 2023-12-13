import unittest

from md2html import *
from plugins.page_flows_plugin import *
from .utils_for_tests import find_single_instance_of_type, parse_argument_file_for_test


def _generate_arg_file_with_plugins_section(page_count):
    arg_file_str = '\n{"documents": [ \n'
    comma = ''
    for i in [str(i) for i in range(1, page_count + 1)]:
        arg_file_str += comma + '    {"input": "page' + i + '.txt", "output": "page' \
                        + i + '.html", "title": "Title' + i + '"}'
        comma = ', \n'
    arg_file_str += '\n], "plugins": {"page-flows": {"sections": [ \n'
    comma = ''
    for i in [str(i) for i in range(1, page_count + 1)]:
        arg_file_str += comma + '    {"link": "page' + i + '.html", "title": "Title' + i + '"}'
        comma = ', \n'
    arg_file_str += '\n]}}}'
    return arg_file_str


def _generate_arg_file_with_documents_section(page_count):
    arg_file_str = '\n{"documents": [ \n'
    comma = ''
    for i in [str(i) for i in range(1, page_count + 1)]:
        arg_file_str += comma + '    {"input": "page' + i + '.txt", "output": "page' + i \
                        + '.html", "title": "Title' + i + '", "page-flows": ["sections"]}'
        comma = ', \n'
    arg_file_str += '\n], "plugins": {"page-flows": {}}}'
    return arg_file_str


def _find_single_plugin(plugins):
    return find_single_instance_of_type(plugins, PageFlowsPlugin)


class PageFlowsPluginTest(unittest.TestCase):

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt"}], "plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_pageSequence_inPluginsSection(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "about.md"}], '
            '"plugins": {"page-flows": {"sections": ['
            '{"link": "index.html", "title": "Home"},'
            '{"link": "about.html", "title": "About"},'
            '{"link": "other.html", "title": "Other"}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="about.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": True,
                              "external": False, "first": False, "last": False}, pages[1])
        self.assertDictEqual({"link": "other.html", "title": "Other", "current": False,
                              "external": False, "first": False, "last": True}, pages[2])

    def test_customAttributes(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "about.md"}], '
            '"plugins": {"page-flows": {"sections": ['
            '{"link": "other.html", "title": "Other", "custom_string": "custom string value", '
            '    "custom_number": 101.4, "custom_boolean": true}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="about.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(1, len(pages))
        self.assertEqual("custom string value", pages[0]["custom_string"])
        self.assertEqual(101.4, pages[0]["custom_number"])
        self.assertEqual(True, pages[0]["custom_boolean"])

    def test_pageSequence_inDocumentsSection(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '{"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '{"input": "about.txt", "title": "About", "page-flows": ["sections"]}, '
            '{"input": "no-page-flow.txt", "title": "No page flow"}'
            '], "plugins": {"page-flows": {}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="index.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": True,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False,
                              "external": False, "first": False, "last": True}, pages[1])

        doc = Document(output_file="no-page-flow.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False,
                              "external": False, "first": False, "last": True}, pages[1])

    def test_pageSequence_inBothDocumentsAndPluginsSections(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '    {"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '    {"input": "about.txt", "title": "About", "page-flows": ["sections"]}, '
            '    {"input": "other.txt", "title": "Other"}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "other.html", "title": "OtherLink"}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="other.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False,
                              "external": False, "first": False, "last": False}, pages[1])
        self.assertDictEqual({"link": "other.html", "title": "OtherLink", "current": True,
                              "external": False, "first": False, "last": True}, pages[2])

    def test_notActivated_withoutEmptyPluginDeclaration(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt", "output": "index.html", '
            '"title": "Home", "page-flows": ["sections"]}], '
            '"plugins": {}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        self.assertIsNone(_find_single_plugin(args.plugins))

    def test_severalPageFlows(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '    {"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '    {"input": "about.txt", "title": "About", "page-flows": ["sections"]}, '
            '    {"input": "narration.txt", "title": "Narration"}, '
            '    {"input": "other1.txt", "title": "Other1"}, '
            '    {"input": "other2.txt", "title": "Other2"}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "narration.html", "title": "Narration"}'
            '], "other_links": ['
            '    {"link": "other1.html", "title": "OtherLink1"},'
            '    {"link": "other2.html", "title": "OtherLink2"}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="narration.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False,
                              "external": False, "first": False, "last": False}, pages[1])
        self.assertDictEqual({"link": "narration.html", "title": "Narration", "current": True,
                              "external": False, "first": False, "last": True}, pages[2])

        doc = Document(output_file="other1.html")
        page_flow = plugin.variables(doc)["other_links"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "other1.html", "title": "OtherLink1", "current": True,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "other2.html", "title": "OtherLink2", "current": False,
                              "external": False, "first": False, "last": True}, pages[1])

    def test_sameDocumentInSeveralPageFlows(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '    {"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '    {"input": "about.txt", "title": "About", "page-flows": '
            '        ["sections", "other_links"]}, '
            '    {"input": "other.txt", "title": "Other", "page-flows": ["other_links"]}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "other.html", "title": "OtherLink"}'
            '], "other_links": ['
            '    {"link": "index.html", "title": "HomeLink"}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="other.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False,
                              "external": False, "first": False, "last": False}, pages[1])
        self.assertDictEqual({"link": "other.html", "title": "OtherLink", "current": True,
                              "external": False, "first": False, "last": True}, pages[2])
        page_flow = plugin.variables(doc)["other_links"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False,
                              "external": False, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "other.html", "title": "Other", "current": True,
                              "external": False, "first": False, "last": False}, pages[1])
        self.assertDictEqual({"link": "index.html", "title": "HomeLink", "current": False,
                              "external": False, "first": False, "last": True}, pages[2])

    def test_externalLinks(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '    {"input": "index.txt", "title": "Home"} '
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "index.html", "title": "HomeLinkExternal", "external": true}, '
            '    {"link": "index.html", "title": "HomeLink", "external": false}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="index.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "HomeLinkExternal", "current": False,
                              "external": True, "first": True, "last": False}, pages[0])
        self.assertDictEqual({"link": "index.html", "title": "HomeLink", "current": True,
                              "external": False, "first": False, "last": True}, pages[1])

    def test_navigations(self):
        with_plugins_section = load_json_argument_file(
            '\n{"documents": [ \n'
            '    {"input": "page1.txt", "output": "page1.html", "title": "Title1"}, \n'
            '    {"input": "page2.txt", "output": "page2.html", "title": "Title2"}, \n'
            '    {"input": "page3.txt", "output": "page3.html", "title": "Title3"} \n'
            '], "plugins": {"page-flows": {"sections": [ \n'
            '    {"link": "page1.html", "title": "Title1"}, \n'
            '    {"link": "page2.html", "title": "Title2"}, \n'
            '    {"link": "page3.html", "title": "Title3"} \n'
            ']}}}')
        with_documents_section = load_json_argument_file(
            '\n{"documents": [ \n'
            '{"input": "page1.txt", "output": "page1.html", "title": "Title1", '
            '    "page-flows": ["sections"]},  \n'
            '{"input": "page2.txt", "output": "page2.html", "title": "Title2", '
            '    "page-flows": ["sections"]},  \n'
            '{"input": "page3.txt", "output": "page3.html", "title": "Title3", '
            '    "page-flows": ["sections"]}  \n'
            '], "plugins": {"page-flows": {}}}')

        projection = ["link", "title", "current", "external"]

        for argument_file_dict, test_name in [(with_plugins_section, "with_plugins_section"),
                                              (with_documents_section, "with_documents_section")]:
            with self.subTest(test_name=test_name):
                args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
                plugin = _find_single_plugin(args.plugins)

                doc = Document(output_file="page1.html")
                page_flow = plugin.variables(doc)["sections"]
                self.assertTrue(page_flow.has_navigation)
                self.assertTrue(page_flow.not_empty)
                self.assertIsNone(page_flow.previous)
                self.assertDictEqual(
                    {"link": "page1.html", "title": "Title1", "current": True,
                     "external": False}, {k: page_flow.current[k] for k in projection})
                self.assertDictEqual(
                    {"link": "page2.html", "title": "Title2", "current": False,
                     "external": False}, {k: page_flow.next[k] for k in projection})

                doc = Document(output_file="page2.html")
                page_flow = plugin.variables(doc)["sections"]
                self.assertTrue(page_flow.has_navigation)
                self.assertTrue(page_flow.not_empty)
                self.assertDictEqual(
                    {"link": "page1.html", "title": "Title1", "current": False,
                     "external": False}, {k: page_flow.previous[k] for k in projection})
                self.assertDictEqual(
                    {"link": "page2.html", "title": "Title2", "current": True,
                     "external": False}, {k: page_flow.current[k] for k in projection})
                self.assertDictEqual(
                    {"link": "page3.html", "title": "Title3", "current": False,
                     "external": False}, {k: page_flow.next[k] for k in projection})

                doc = Document(output_file="page3.html")
                page_flow = plugin.variables(doc)["sections"]
                self.assertTrue(page_flow.has_navigation)
                self.assertTrue(page_flow.not_empty)
                self.assertDictEqual(
                    {"link": "page2.html", "title": "Title2", "current": False,
                     "external": False}, {k: page_flow.previous[k] for k in projection})
                self.assertDictEqual(
                    {"link": "page3.html", "title": "Title3", "current": True,
                     "external": False}, {k: page_flow.current[k] for k in projection})
                self.assertIsNone(page_flow.next)

    def test_navigations_generalized(self):

        projection = ["link", "title", "current", "external"]

        for page_count in range(1, 5):

            arg_file_str = _generate_arg_file_with_plugins_section(page_count)
            with_plugins_section = load_json_argument_file(arg_file_str)

            arg_file_str = _generate_arg_file_with_documents_section(page_count)
            with_documents_section = load_json_argument_file(arg_file_str)

            for argument_file_dict, test_name in [
                (with_plugins_section, "with_plugins_section"),
                (with_documents_section, "with_documents_section")
            ]:
                with self.subTest(test_name=test_name, page_count=page_count):
                    args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
                    plugin = _find_single_plugin(args.plugins)

                    for i in range(1, page_count + 1):
                        doc = Document(output_file=f"page{i}.html")
                        page_flow = plugin.variables(doc)["sections"]
                        self.assertEqual(page_count > 1, page_flow.has_navigation)
                        self.assertTrue(page_flow.not_empty)
                        if i < 2:
                            self.assertIsNone(page_flow.previous)
                        else:
                            self.assertDictEqual({
                                f"link": f"page{i - 1}.html", "title": f"Title{i - 1}",
                                "current": False, "external": False},
                                {k: page_flow.previous[k] for k in projection})
                        self.assertDictEqual({
                            "link": f"page{i}.html", "title": f"Title{i}", "current": True,
                            "external": False}, {k: page_flow.current[k] for k in projection})
                        if i > page_count - 1:
                            self.assertIsNone(page_flow.next)
                        else:
                            self.assertDictEqual({
                                "link": f"page{i + 1}.html", "title": f"Title{i + 1}",
                                "current": False, "external": False},
                                {k: page_flow.next[k] for k in projection})

    def test_relativisation(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": ['
            '    {"input": "root1.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "root2.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "doc/sub1.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "doc/sub2.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "doc/ch01/sub-sub.txt", "title": "whatever"}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "doc/ch01/sub-sub-1.html", "title": "whatever"},'
            '    {"link": "doc/ch01/sub-sub-2.html", "title": "whatever"}'
            ']}}}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="root1.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual("root1.html", pages[0]["link"])
        self.assertEqual("root2.html", pages[1]["link"])
        self.assertEqual("doc/sub1.html", pages[2]["link"])
        self.assertEqual("doc/ch01/sub-sub-1.html", pages[4]["link"])

        doc = Document(output_file="doc/sub1.html")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual("../root1.html", pages[0]["link"])
        self.assertEqual("sub1.html", pages[2]["link"])
        self.assertEqual("sub2.html", pages[3]["link"])
        self.assertEqual("ch01/sub-sub-1.html", pages[4]["link"])

        doc = Document(output_file="doc/ch01/sub-sub-1")
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual("../../root1.html", pages[0]["link"])
        self.assertEqual("../sub2.html", pages[3]["link"])
        self.assertEqual("sub-sub-1.html", pages[4]["link"])
        self.assertEqual("sub-sub-2.html", pages[5]["link"])

    def test_extended_format_simple(self):
        argument_file_dict = load_json_argument_file(
            '{'
            '   "documents": ['
            '       {"input": "page1.txt", "title": "whatever"}'
            '   ],'
            '   "plugins": {"page-flows": {"sections": { "title": "Sections", "groups": ["gr1"], '
            '          "items": ['
            '              {"link": "link1.html", "title": "title1"},'
            '              {"link": "link2.html", "title": "title2"}'
            '          ]'
            '   }}}'
            '}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="page1.html")
        page_flow = plugin.variables(doc)["sections"]
        self.assertEqual("Sections", page_flow.title)
        pages = [p for p in page_flow]
        self.assertEqual("link1.html", pages[0]["link"])
        self.assertEqual("title1", pages[0]["title"])
        self.assertEqual("link2.html", pages[1]["link"])
        self.assertEqual("title2", pages[1]["title"])

        group = plugin.variables(doc)["gr1"]
        self.assertEqual(1, len(group))
        pages = [p for p in group[0]]
        self.assertEqual("link1.html", pages[0]["link"])
        self.assertEqual("title1", pages[0]["title"])
        self.assertEqual("link2.html", pages[1]["link"])
        self.assertEqual("title2", pages[1]["title"])

    def test_extended_format_in_documents(self):
        argument_file_dict = load_json_argument_file(
            '{'
            '   "documents": ['
            '       {"input": "page1.txt", "title": "whatever", "page-flows": ["sections"]},'
            '       {"input": "page2.txt", "title": "whatever", "page-flows": ["sections"]}'
            '   ],'
            '   "plugins": {"page-flows": {"sections": { "title": "Sections", "groups": ["gr1"], '
            '          "items": ['
            '              {"link": "link1.html", "title": "whatever"}'
            '          ]'
            '   }}}'
            '}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="page1.html")
        page_flow = plugin.variables(doc)["sections"]
        self.assertEqual("Sections", page_flow.title)
        pages = [p for p in page_flow]
        self.assertEqual("page1.html", pages[0]["link"])
        self.assertEqual("page2.html", pages[1]["link"])
        self.assertEqual("link1.html", pages[2]["link"])

        group = plugin.variables(doc)["gr1"]
        self.assertEqual(1, len(group))
        pages = [p for p in group[0]]
        self.assertEqual("page1.html", pages[0]["link"])
        self.assertEqual("page2.html", pages[1]["link"])
        self.assertEqual("link1.html", pages[2]["link"])

    def test_extended_format_several_groups(self):
        argument_file_dict = load_json_argument_file(
            '{'
            '    "documents": ['
            '        {"input": "page1.txt", "title": "whatever"}'
            '    ],'
            '    "plugins": {"page-flows": {'
            '        "flow1": { "title": "Flow 1", "groups": ["gr1"], '
            '            "items": [{"link": "link1.html", "title": "whatever"}]'
            '        },'
            '        "flow2": { "title": "Flow 2", "groups": ["gr1", "gr2"], '
            '            "items": [{"link": "link2.html", "title": "whatever"}]'
            '        },'
            '        "flow3": { "title": "Flow 3", "groups": ["gr2"], '
            '            "items": [{"link": "link3.html", "title": "whatever"}]'
            '        }'
            '    }}'
            '}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)

        doc = Document(output_file="page1.html")

        group = plugin.variables(doc)["gr1"]
        self.assertEqual(2, len(group))
        self.assertEqual("Flow 1", group[0].title)
        self.assertEqual("Flow 2", group[1].title)
        pages = [p for p in group[0]]
        self.assertEqual("link1.html", pages[0]["link"])
        pages = [p for p in group[1]]
        self.assertEqual("link2.html", pages[0]["link"])

        group = plugin.variables(doc)["gr2"]
        self.assertEqual(2, len(group))
        self.assertEqual("Flow 2", group[0].title)
        self.assertEqual("Flow 3", group[1].title)
        pages = [p for p in group[0]]
        self.assertEqual("link2.html", pages[0]["link"])
        pages = [p for p in group[1]]
        self.assertEqual("link3.html", pages[0]["link"])

    def test_extended_format_duplicate_error(self):
        argument_file_dict = load_json_argument_file(
            '{'
            '   "documents": ['
            '       {"input": "page1.txt", "title": "whatever"}'
            '   ],'
            '   "plugins": {"page-flows": {"name1": { "title": "Sections", "groups": ["gr1", "name1"], '
            '          "items": ['
            '              {"link": "link1.html", "title": "title1"}'
            '          ]'
            '   }}}'
            '}')
        args = parse_argument_file_for_test(argument_file_dict, CliArgDataObject())
        plugin = _find_single_plugin(args.plugins)
        with self.assertRaises(UserError) as cm:
            plugin.variables(Document(output_file="page1.html"))
        self.assertTrue('Variable duplication' in str(cm.exception))

