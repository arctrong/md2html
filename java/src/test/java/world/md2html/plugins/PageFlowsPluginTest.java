package world.md2html.plugins;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.model.ArgFileOptions;
import world.md2html.options.model.Document;
import world.md2html.testutils.PluginTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.testutils.PluginTestUtils.documentWithOutputLocation;

class PageFlowsPluginTest {

    /**
     * The `PageFlowsPlugin.PageFlow` class is private and its instances are accessed using
     * the Bean notation. To reproduce this accessing method is these tests the internal
     * representation is used.
     */
    private static class PageFlow {

        // Now need for the pages here. They will be accessed via its `Iterable`
        // implementation.
        // @Getter private final List<Map<String, Object>> pages;
        @Getter private final Map<String, Object> previous;
        @Getter private final Map<String, Object> current;
        @Getter private final Map<String, Object> next;
        @Getter private final boolean has_navigation;
        @Getter private final boolean not_empty;

        @SuppressWarnings("unchecked")
        private PageFlow(Object pageFlow) {
            try {
                //this.pages = (List<Map<String, Object>>) property(pageFlow, "getPages");
                this.previous = (Map<String, Object>) property(pageFlow, "getPrevious");
                this.current = (Map<String, Object>) property(pageFlow, "getCurrent");
                this.next = (Map<String, Object>) property(pageFlow, "getNext");
                this.has_navigation = (boolean) property(pageFlow, "isHas_navigation");
                this.not_empty = (boolean) property(pageFlow, "isNot_empty");
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private Object property(Object object, String propertyName) throws NoSuchMethodException,
                InvocationTargetException, IllegalAccessException {
            return object.getClass().getMethod(propertyName).invoke(object);
        }
    }

    /**
     * The pages will be accessed via the page flow's `Iterable` interface implementation.
     * This method reproduces such behaviour.
     */
    private List<Map<String, Object>> extractPages(Object pageFlow) {
        List<Map<String, Object>> pages = new ArrayList<>();
        //noinspection unchecked
        for (Map<String, Object> page : (Iterable<Map<String, Object>>) pageFlow) {
            pages.add(page);
        }
        return pages;
    }

    private PageFlowsPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return (PageFlowsPlugin) PluginTestUtils.findSinglePlugin(plugins, PageFlowsPlugin.class);
    }

    private void assertPageEquals(String link, String title, boolean current, boolean external,
            Map<String, Object> page) {
        assertEquals(link, page.get("link"));
        assertEquals(title, page.get("title"));
        assertEquals(current, page.get("current"));
        assertEquals(external, page.get("external"));
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\"}], " +
                        "\"plugins\": {\"page-flows\": {}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void pageSequence_inPluginsSection() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-flows\": {\"sections\": [" +
                        "{\"link\": \"index.html\", \"title\": \"Home\"}," +
                        "{\"link\": \"about.html\", \"title\": \"About\"}," +
                        "{\"link\": \"other.html\", \"title\": \"Other\"}" +
                        "]}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        Document doc = documentWithOutputLocation("about.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, pages.get(0));
        assertPageEquals("about.html", "About", true, false, pages.get(1));
        assertPageEquals("other.html", "Other", false, false, pages.get(2));
    }

    @Test
    public void pageSequence_inDocumentsSection() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": [" +
                "{\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "{\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\"]}, " +
                "{\"input\": \"no-page-flow.txt\", \"title\": \"No page flow\"}" +
                "], \"plugins\": {\"page-flows\": {}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        
        Document doc = documentWithOutputLocation("index.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(2, pages.size());
        assertPageEquals("index.html", "Home", true, false, pages.get(0));
        assertPageEquals("about.html", "About", false, false, pages.get(1));

        doc = documentWithOutputLocation("no-page-flow.html");
        pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(2, pages.size());
        assertPageEquals("index.html", "Home", false, false, pages.get(0));
        assertPageEquals("about.html", "About", false, false, pages.get(1));
    }

    @Test
    public void pageSequence_inBothDocumentsAndPluginsSections() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": [" +
                "    {\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"other.txt\", \"title\": \"Other\"}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"other.html\", \"title\": \"OtherLink\"}" +
                "]}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        Document doc = documentWithOutputLocation("other.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, pages.get(0));
        assertPageEquals("about.html", "About", false, false, pages.get(1));
        assertPageEquals("other.html", "OtherLink", true, false, pages.get(2));
    }

    @Test
    public void notActivated_withoutEmptyPluginDeclaration() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(
                "{\"documents\": [{\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"Home\", \"page-flows\": [\"sections\"]}], " +
                        "\"plugins\": {}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());
        assertNull(plugin);
    }

    @Test
    public void severalPageFlows() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": [" +
                "    {\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"narration.txt\", \"title\": \"Narration\"}, " +
                "    {\"input\": \"other1.txt\", \"title\": \"Other1\"}, " +
                "    {\"input\": \"other2.txt\", \"title\": \"Other2\"}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"narration.html\", \"title\": \"Narration\"}" +
                "], \"other_links\": [" +
                "    {\"link\": \"other1.html\", \"title\": \"OtherLink1\"}," +
                "    {\"link\": \"other2.html\", \"title\": \"OtherLink2\"}" +
            "]}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        Document doc = documentWithOutputLocation("narration.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, pages.get(0));
        assertPageEquals("about.html", "About", false, false, pages.get(1));
        assertPageEquals("narration.html", "Narration", true, false, pages.get(2));

        doc = documentWithOutputLocation("other1.html");
        pages = extractPages(plugin.variables(doc).get("other_links"));
        assertEquals(2, pages.size());
        assertPageEquals("other1.html", "OtherLink1", true, false, pages.get(0));
        assertPageEquals("other2.html", "OtherLink2", false, false, pages.get(1));
    }

    @Test
    public void sameDocumentInSeveralPageFlows() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": [" +
                "    {\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\", \"other_links\"]}, " +
                "    {\"input\": \"other.txt\", \"title\": \"Other\", \"page-flows\": [\"other_links\"]}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"other.html\", \"title\": \"OtherLink\"}" +
                "], \"other_links\": [" +
                "    {\"link\": \"index.html\", \"title\": \"HomeLink\"}" +
                "]}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        Document doc = documentWithOutputLocation("other.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, pages.get(0));
        assertPageEquals("about.html", "About", false, false, pages.get(1));
        assertPageEquals("other.html", "OtherLink", true, false, pages.get(2));

        pages = extractPages(plugin.variables(doc).get("other_links"));
        assertEquals(3, pages.size());
        assertPageEquals("about.html", "About", false, false, pages.get(0));
        assertPageEquals("other.html", "Other", true, false, pages.get(1));
        assertPageEquals("index.html", "HomeLink", false, false, pages.get(2));
    }

    @Test
    public void externalLinks() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": [" +
            "    {\"input\": \"index.txt\", \"title\": \"Home\"} " +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"index.html\", \"title\": \"HomeLinkExternal\", \"external\": true}, " +
                "    {\"link\": \"index.html\", \"title\": \"HomeLink\", \"external\": false}" +
                "]}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        Document doc = documentWithOutputLocation("index.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(2, pages.size());
        assertPageEquals("index.html", "HomeLinkExternal", false, true, pages.get(0));
        assertPageEquals("index.html", "HomeLink", true, false, pages.get(1));
    }

    private static Stream<Arguments> navigation() {
        return Stream.of(
                Arguments.of("with_plugins_section", "\n{\"documents\": [ \n" +
                        "    {\"input\": \"page1.txt\", \"output\": \"page1.html\", \"title\": \"Title1\"}, \n" +
                        "    {\"input\": \"page2.txt\", \"output\": \"page2.html\", \"title\": \"Title2\"}, \n" +
                        "    {\"input\": \"page3.txt\", \"output\": \"page3.html\", \"title\": \"Title3\"} \n" +
                        "], \"plugins\": {\"page-flows\": {\"sections\": [ \n" +
                        "    {\"link\": \"page1.html\", \"title\": \"Title1\"}, \n" +
                        "    {\"link\": \"page2.html\", \"title\": \"Title2\"}, \n" +
                        "    {\"link\": \"page3.html\", \"title\": \"Title3\"} \n" +
                        "]}}}"),
                Arguments.of("with_documents_section", "\n{\"documents\": [ \n" +
                        "{\"input\": \"page1.txt\", \"output\": \"page1.html\", \"title\": \"Title1\", \"page-flows\": [\"sections\"]},  \n" +
                        "{\"input\": \"page2.txt\", \"output\": \"page2.html\", \"title\": \"Title2\", \"page-flows\": [\"sections\"]},  \n" +
                        "{\"input\": \"page3.txt\", \"output\": \"page3.html\", \"title\": \"Title3\", \"page-flows\": [\"sections\"]}  \n" +
                        "], \"plugins\": {\"page-flows\": {}}}")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void navigation(String testName, String argFileContent) throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(argFileContent, null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        Document doc = documentWithOutputLocation("page1.html");
        PageFlow pageFlow = new PageFlow(plugin.variables(doc).get("sections"));
        assertTrue(pageFlow.isHas_navigation());
        assertTrue(pageFlow.isNot_empty());
        assertNull(pageFlow.getPrevious());
        assertPageEquals("page1.html", "Title1", true, false, pageFlow.getCurrent());
        assertPageEquals("page2.html", "Title2", false, false, pageFlow.getNext());

        doc = documentWithOutputLocation("page2.html");
        pageFlow = new PageFlow(plugin.variables(doc).get("sections"));
        assertTrue(pageFlow.isHas_navigation());
        assertTrue(pageFlow.isNot_empty());
        assertPageEquals("page1.html", "Title1", false, false, pageFlow.getPrevious());
        assertPageEquals("page2.html", "Title2", true, false, pageFlow.getCurrent());
        assertPageEquals("page3.html", "Title3", false, false, pageFlow.getNext());

        doc = documentWithOutputLocation("page3.html");
        pageFlow = new PageFlow(plugin.variables(doc).get("sections"));
        assertTrue(pageFlow.isHas_navigation());
        assertTrue(pageFlow.isNot_empty());
        assertPageEquals("page2.html", "Title2", false, false, pageFlow.getPrevious());
        assertPageEquals("page3.html", "Title3", true, false, pageFlow.getCurrent());
        assertNull(pageFlow.getNext());
    }

    private static String generateArgFileWithPluginsSection(int pageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n{\"documents\": [ \n");
        String comma = "";
        for (int i = 1; i <= pageCount; ++i) {
            sb.append(comma).append("    {\"input\": \"page").append(i)
                    .append(".txt\", \"output\": \"page").append(i)
                    .append(".html\", \"title\": \"Title").append(i).append("\"}");
            comma = ", \n";
        }
        sb.append("\n], \"plugins\": {\"page-flows\": {\"sections\": [ \n");
        comma = "";
        for (int i = 1; i <= pageCount; ++i) {
            sb.append(comma).append("    {\"link\": \"page").append(i)
                    .append(".html\", \"title\": \"Title").append(i).append("\"}");
            comma = ", \n";
        }
        sb.append("\n]}}}");
        return sb.toString();
    }

    private static String generateArgFileWithDocumentsSection(int pageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n{\"documents\": [ \n");
        String comma = "";
        for (int i = 1; i <= pageCount; ++i) {
            sb.append(comma).append("    {\"input\": \"page").append(i)
                    .append(".txt\", \"output\": \"page").append(i)
                    .append(".html\", \"title\": \"Title").append(i)
                    .append("\", \"page-flows\": [\"sections\"]}");
            comma = ", \n";
        }
        sb.append("\n], \"plugins\": {\"page-flows\": {}}}");
        return sb.toString();
    }

    private static Stream<Arguments> navigation_generalized() {
        return IntStream.rangeClosed(1, 4).boxed().flatMap(pageCount -> Stream.of(
                Arguments.of("with_plugins_section", pageCount,
                        generateArgFileWithPluginsSection(pageCount)),
                Arguments.of("with_documents_section", pageCount,
                        generateArgFileWithDocumentsSection(pageCount))
        ));
    }

    @ParameterizedTest(name = "[{index}] {0}, pageCount={1}")
    @MethodSource
    public void navigation_generalized(String testName, int pageCount, String argFileContent)
            throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse(argFileContent, null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        for (int i = 1; i <= pageCount; ++i) {
            Document doc = documentWithOutputLocation("page" + i + ".html");
            PageFlow pageFlow = new PageFlow(plugin.variables(doc).get("sections"));
            assertEquals(pageCount > 1, pageFlow.isHas_navigation());
            assertTrue(pageFlow.isNot_empty());
            if (i < 2) { // for the first page, the previous page is always absent
                assertNull(pageFlow.getPrevious());
            } else {
                assertPageEquals("page" + (i - 1) + ".html", "Title" + (i - 1), false, false,
                        pageFlow.getPrevious());
            }
            // current page is always present
            assertPageEquals("page" + i + ".html", "Title" + i, true, false, pageFlow.getCurrent());
            if (i > pageCount - 1) { // for the last page, the next page is always absent
                assertNull(pageFlow.getNext());
            } else {
                assertPageEquals("page" + (i + 1) + ".html", "Title" + (i + 1), false, false,
                        pageFlow.getNext());
            }
        }
    }

    @Test
    public void relativisation() throws ArgFileParseException {
        ArgFileOptions argFileOptions = ArgFileParser.parse("{\"documents\": [" +
                "    {\"input\": \"root1.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"root2.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"doc/sub1.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"doc/sub2.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"doc/ch01/sub-sub.txt\", \"title\": \"whatever\"}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"doc/ch01/sub-sub-1.html\", \"title\": \"whatever\"}," +
                "    {\"link\": \"doc/ch01/sub-sub-2.html\", \"title\": \"whatever\"}" +
                "]}}}", null);
        PageFlowsPlugin plugin = findSinglePlugin(argFileOptions.getPlugins());

        Document doc = documentWithOutputLocation("root1.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals("root1.html", pages.get(0).get("link"));
        assertEquals("root2.html", pages.get(1).get("link"));
        assertEquals("doc/sub1.html", pages.get(2).get("link"));
        assertEquals("doc/ch01/sub-sub-1.html", pages.get(4).get("link"));

        doc = documentWithOutputLocation("doc/sub1.html");
        pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals("../root1.html", pages.get(0).get("link"));
        assertEquals("sub1.html", pages.get(2).get("link"));
        assertEquals("sub2.html", pages.get(3).get("link"));
        assertEquals("ch01/sub-sub-1.html", pages.get(4).get("link"));

        doc = documentWithOutputLocation("doc/ch01/sub-sub-1.html");
        pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals("../../root1.html", pages.get(0).get("link"));
        assertEquals("../sub2.html", pages.get(3).get("link"));
        assertEquals("sub-sub-1.html", pages.get(4).get("link"));
        assertEquals("sub-sub-2.html", pages.get(5).get("link"));
    }

}
