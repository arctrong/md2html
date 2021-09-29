import sys
import unittest
from pathlib import Path

sys.path.append(Path(__file__).resolve().parent.parent)
from md2html import *
from plugins.page_flows_plugin import *
from constants import PLUGINS


class PageFlowsPluginTest(unittest.TestCase):

    def _find_single_plugin(self, plugins):
        result = None
        for plugin in plugins:
            if isinstance(plugin, PageFlowsPlugin):
                if result is None:
                    result = plugin
                else:
                    self.fail(f"More than one plugins of type '{type(plugin).__name__}' found.")
        return result

    def test_notActivated(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "index.txt"}], '
            '"plugins": {}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        self.assertIsNone(self._find_single_plugin(plugins))

    def test_pageSequence_inPluginsSection(self):
        argument_file_dict = load_json_argument_file('{"documents": [{"input": "about.md"}], '
            '"plugins": {"page-flows": {"sections": ['
            '{"link": "index.html", "title": "Home"},'
            '{"link": "about.html", "title": "About"},'
            '{"link": "other.html", "title": "Other"}'
            ']}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "about.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": True, 
            "external": False}, pages[1])
        self.assertDictEqual({"link": "other.html", "title": "Other", "current": False, 
            "external": False}, pages[2])

    def test_pageSequence_inDocumentsSection(self):
        argument_file_dict = load_json_argument_file('{"documents": ['
            '{"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '{"input": "about.txt", "title": "About", "page-flows": ["sections"]}, '
            '{"input": "no-page-flow.txt", "title": "No page flow"}'
            '], "plugins": {"page-flows": {}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "index.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": True, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False, 
            "external": False}, pages[1])

        doc = {'output_file': "no-page-flow.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False, 
            "external": False}, pages[1])

    def test_pageSequence_inBothDocumentsAndPluginsSections(self):
        argument_file_dict = load_json_argument_file('{"documents": ['
            '    {"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '    {"input": "about.txt", "title": "About", "page-flows": ["sections"]}, '
            '    {"input": "other.txt", "title": "Other"}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "other.html", "title": "OtherLink"}'
            ']}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "other.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False, 
            "external": False}, pages[1])
        self.assertDictEqual({"link": "other.html", "title": "OtherLink", "current": True, 
            "external": False}, pages[2])

    def test_notActivated_withoutEmptyPluginDeclaration(self):
        argument_file_dict = load_json_argument_file(
            '{"documents": [{"input": "index.txt", "output": "index.html", '
            '"title": "Home", "page-flows": ["sections"]}], '
            '"plugins": {}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        self.assertIsNone(self._find_single_plugin(plugins))

    def test_severalPageFlows(self):
        argument_file_dict = load_json_argument_file('{"documents": ['
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
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "narration.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False, 
            "external": False}, pages[1])
        self.assertDictEqual({"link": "narration.html", "title": "Narration", "current": True, 
            "external": False}, pages[2])

        doc = {'output_file': "other1.html"}
        page_flow = plugin.variables(doc)["other_links"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "other1.html", "title": "OtherLink1", "current": True, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "other2.html", "title": "OtherLink2", "current": False, 
            "external": False}, pages[1])
        
    def test_sameDocumentInSeveralPageFlows(self):
        argument_file_dict = load_json_argument_file('{"documents": ['
            '    {"input": "index.txt", "title": "Home", "page-flows": ["sections"]}, '
            '    {"input": "about.txt", "title": "About", "page-flows": ["sections", "other_links"]}, '
            '    {"input": "other.txt", "title": "Other", "page-flows": ["other_links"]}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "other.html", "title": "OtherLink"}'
            '], "other_links": ['
            '    {"link": "index.html", "title": "HomeLink"}'
            ']}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "other.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "Home", "current": False, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False, 
            "external": False}, pages[1])
        self.assertDictEqual({"link": "other.html", "title": "OtherLink", "current": True, 
            "external": False}, pages[2])
        page_flow = plugin.variables(doc)["other_links"]
        pages = [p for p in page_flow]
        self.assertEqual(3, len(pages))
        self.assertDictEqual({"link": "about.html", "title": "About", "current": False, 
            "external": False}, pages[0])
        self.assertDictEqual({"link": "other.html", "title": "Other", "current": True, 
            "external": False}, pages[1])
        self.assertDictEqual({"link": "index.html", "title": "HomeLink", "current": False, 
            "external": False}, pages[2])
        
    def test_externalLinks(self):
        argument_file_dict = load_json_argument_file('{"documents": ['
            '    {"input": "index.txt", "title": "Home"} '
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "index.html", "title": "HomeLinkExternal", "external": true}, '
            '    {"link": "index.html", "title": "HomeLink", "external": false}'
            ']}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "index.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual(2, len(pages))
        self.assertDictEqual({"link": "index.html", "title": "HomeLinkExternal", "current": False, 
            "external": True}, pages[0])
        self.assertDictEqual({"link": "index.html", "title": "HomeLink", "current": True, 
            "external": False}, pages[1])

    def test_navigations(self):
        with_plugins_section = load_json_argument_file('\n{"documents": [ \n'
            '    {"input": "page1.txt", "output": "page1.html", "title": "Title1"}, \n'
            '    {"input": "page2.txt", "output": "page2.html", "title": "Title2"}, \n'
            '    {"input": "page3.txt", "output": "page3.html", "title": "Title3"} \n'
            '], "plugins": {"page-flows": {"sections": [ \n'
            '    {"link": "page1.html", "title": "Title1"}, \n'
            '    {"link": "page2.html", "title": "Title2"}, \n'
            '    {"link": "page3.html", "title": "Title3"} \n'
            ']}}}')
        with_documents_section = load_json_argument_file('\n{"documents": [ \n'
            '{"input": "page1.txt", "output": "page1.html", "title": "Title1", "page-flows": ["sections"]},  \n'
            '{"input": "page2.txt", "output": "page2.html", "title": "Title2", "page-flows": ["sections"]},  \n'
            '{"input": "page3.txt", "output": "page3.html", "title": "Title3", "page-flows": ["sections"]}  \n'
            '], "plugins": {"page-flows": {}}}')
            
        for argument_file_dict, test_name in [(with_plugins_section, "with_plugins_section"), 
                                              (with_documents_section, "with_documents_section")]:
            with self.subTest(test_name=test_name):
                plugins = parse_argument_file_content(argument_file_dict, {}).plugins
                plugin = self._find_single_plugin(plugins)
                
                doc = {'output_file': "page1.html"}
                page_flow = plugin.variables(doc)["sections"]
                self.assertTrue(page_flow.has_navigation)
                self.assertTrue(page_flow.not_empty)
                self.assertIsNone(page_flow.previous)
                self.assertDictEqual({"link": "page1.html", "title": "Title1", "current": True, 
                    "external": False}, page_flow.current)
                self.assertDictEqual({"link": "page2.html", "title": "Title2", "current": False, 
                    "external": False}, page_flow.next)
                
                doc = {'output_file': "page2.html"}
                page_flow = plugin.variables(doc)["sections"]
                self.assertTrue(page_flow.has_navigation)
                self.assertTrue(page_flow.not_empty)
                self.assertDictEqual({"link": "page1.html", "title": "Title1", "current": False, 
                    "external": False}, page_flow.previous)
                self.assertDictEqual({"link": "page2.html", "title": "Title2", "current": True, 
                    "external": False}, page_flow.current)
                self.assertDictEqual({"link": "page3.html", "title": "Title3", "current": False, 
                    "external": False}, page_flow.next)
     
                doc = {'output_file': "page3.html"}
                page_flow = plugin.variables(doc)["sections"]
                self.assertTrue(page_flow.has_navigation)
                self.assertTrue(page_flow.not_empty)
                self.assertDictEqual({"link": "page2.html", "title": "Title2", "current": False, 
                    "external": False}, page_flow.previous)
                self.assertDictEqual({"link": "page3.html", "title": "Title3", "current": True, 
                    "external": False}, page_flow.current)
                self.assertIsNone(page_flow.next)

    def _generate_arg_file_with_plugins_section(self, page_count):
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

    def _generate_arg_file_with_documents_section(self, page_count):
        arg_file_str = '\n{"documents": [ \n'
        comma = ''
        for i in [str(i) for i in range(1, page_count + 1)]:
            arg_file_str += comma + '    {"input": "page' + i + '.txt", "output": "page' + i \
                            + '.html", "title": "Title' + i + '", "page-flows": ["sections"]}'
            comma = ', \n'
        arg_file_str += '\n], "plugins": {"page-flows": {}}}'
        return arg_file_str

    def test_navigations_generalized(self):
        for page_count in range(1, 5):
        
            arg_file_str = self._generate_arg_file_with_plugins_section(page_count)
            with_plugins_section = load_json_argument_file(arg_file_str)
            
            arg_file_str = self._generate_arg_file_with_documents_section(page_count)
            with_documents_section = load_json_argument_file(arg_file_str)
        
            for argument_file_dict, test_name in [(with_plugins_section, "with_plugins_section"), 
                                                  (with_documents_section, "with_documents_section")]:
                with self.subTest(test_name=test_name, page_count=page_count):
                    plugins = parse_argument_file_content(argument_file_dict, {}).plugins
                    plugin = self._find_single_plugin(plugins)
                    
                    for i in range(1, page_count + 1):
                        doc = {'output_file': f"page{i}.html"}
                        page_flow = plugin.variables(doc)["sections"]
                        self.assertEqual(page_count > 1, page_flow.has_navigation)
                        self.assertTrue(page_flow.not_empty)
                        if i < 2:
                            self.assertIsNone(page_flow.previous)
                        else:
                            self.assertDictEqual({f"link": f"page{i - 1}.html", 
                                "title": f"Title{i - 1}", "current": False, "external": False}, 
                                page_flow.previous)
                        self.assertDictEqual({"link": f"page{i}.html", "title": f"Title{i}", 
                            "current": True, "external": False}, page_flow.current)
                        if i > page_count - 1:
                            self.assertIsNone(page_flow.next)
                        else:
                            self.assertDictEqual({"link": f"page{i + 1}.html", 
                                "title": f"Title{i + 1}", "current": False, "external": False}, 
                                page_flow.next)

    def test_relativisation(self):
        argument_file_dict = load_json_argument_file('{"documents": ['
            '    {"input": "root1.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "root2.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "doc/sub1.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "doc/sub2.txt", "title": "whatever", "page-flows": ["sections"]}, '
            '    {"input": "doc/ch01/sub-sub.txt", "title": "whatever"}'
            '], "plugins": {"page-flows": {"sections": ['
            '    {"link": "doc/ch01/sub-sub-1.html", "title": "whatever"},'
            '    {"link": "doc/ch01/sub-sub-2.html", "title": "whatever"}'
            ']}}}')
        plugins = parse_argument_file_content(argument_file_dict, {}).plugins
        plugin = self._find_single_plugin(plugins)
        
        doc = {'output_file': "root1.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual("root1.html", pages[0]["link"])
        self.assertEqual("root2.html", pages[1]["link"])
        self.assertEqual("doc/sub1.html", pages[2]["link"])
        self.assertEqual("doc/ch01/sub-sub-1.html", pages[4]["link"])

        doc = {'output_file': "doc/sub1.html"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual("../root1.html", pages[0]["link"])
        self.assertEqual("sub1.html", pages[2]["link"])
        self.assertEqual("sub2.html", pages[3]["link"])
        self.assertEqual("ch01/sub-sub-1.html", pages[4]["link"])
        
        doc = {'output_file': "doc/ch01/sub-sub-1"}
        page_flow = plugin.variables(doc)["sections"]
        pages = [p for p in page_flow]
        self.assertEqual("../../root1.html", pages[0]["link"])
        self.assertEqual("../sub2.html", pages[3]["link"])
        self.assertEqual("sub-sub-1.html", pages[4]["link"])
        self.assertEqual("sub-sub-2.html", pages[5]["link"])


if __name__ == '__main__':
    unittest.main()
