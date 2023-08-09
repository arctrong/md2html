package world.md2html.plugins;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.testutils.PluginTestUtils;
import world.md2html.utils.UserError;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static world.md2html.options.TestUtils.parseArgumentFile;
import static world.md2html.testutils.PluginTestUtils.documentWithOutputLocation;

class PageFlowsPluginTest {

    /**
     * The `PageFlowsPlugin.PageFlow` class is private and its instances are accessed using
     * the Bean notation. To reproduce this accessing method in these tests the internal
     * representation is used.
     */
    private static class TestPageFlow {

        @Getter private final String title;
        @Getter private final Map<String, Object> previous;
        @Getter private final Map<String, Object> current;
        @Getter private final Map<String, Object> next;
        @Getter private final boolean has_navigation;
        @Getter private final boolean not_empty;

        @SuppressWarnings("unchecked")
        private TestPageFlow(Object pageFlow) {
            try {
                this.title = (String) property(pageFlow, "getTitle");
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

    private enum PagePosition {
        FIRST, LAST, BETWEEN
    }

    private static final CliOptions DUMMY_CLI_OPTIONS = CliOptions.builder().build();

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

    private static List<TestPageFlow> convertPageFlowGroup(Object group) {
        //noinspection unchecked
        return ((List<Object>) group).stream().map(TestPageFlow::new).collect(Collectors.toList());
    }

    private PageFlowsPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins) {
        return PluginTestUtils.findFirstElementOfType(plugins, PageFlowsPlugin.class);
    }

    private void assertPageEquals(String link, String title, boolean current, boolean external,
            Map<String, Object> page) {
        assertEquals(link, page.get("link"));
        assertEquals(title, page.get("title"));
        assertEquals(current, page.get("current"));
        assertEquals(external, page.get("external"));
    }

    private void assertPageEquals(String link, String title, boolean current, boolean external,
            PagePosition pagePosition, Map<String, Object> page) {
        assertEquals(link, page.get("link"));
        assertEquals(title, page.get("title"));
        assertEquals(current, page.get("current"));
        assertEquals(external, page.get("external"));
        assertEquals(pagePosition == PagePosition.FIRST, page.get("first"));
        assertEquals(pagePosition == PagePosition.LAST, page.get("last"));
    }

    @Test
    public void notActivated() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\"}], " +
                        "\"plugins\": {\"page-flows\": {}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void pageSequence_inPluginsSection() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-flows\": {\"sections\": [" +
                        "{\"link\": \"index.html\", \"title\": \"Home\"}," +
                        "{\"link\": \"about.html\", \"title\": \"About\"}," +
                        "{\"link\": \"other.html\", \"title\": \"Other\"}" +
                        "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);
        Document doc = documentWithOutputLocation("about.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("about.html", "About", true, false, PagePosition.BETWEEN, pages.get(1));
        assertPageEquals("other.html", "Other", false, false, PagePosition.LAST, pages.get(2));
    }

    @Test
    public void customAttributes() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"about.md\"}], " +
                        "\"plugins\": {\"page-flows\": {\"sections\": [" +
                        "{\"link\": \"other.html\", \"title\": \"Other\"," +
                        "    \"custom_string\": \"custom string value\", " +
                        "    \"custom_number\": 101.4, \"custom_boolean\": true}" +
                        "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);
        Document doc = documentWithOutputLocation("about.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(1, pages.size());
        assertEquals("custom string value", pages.get(0).get("custom_string"));
        assertEquals(101.4, pages.get(0).get("custom_number"));
        assertEquals(true, pages.get(0).get("custom_boolean"));
    }

    @Test
    public void pageSequence_inDocumentsSection() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": [" +
                "{\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "{\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\"]}, " +
                "{\"input\": \"no-page-flow.txt\", \"title\": \"No page flow\"}" +
                "], \"plugins\": {\"page-flows\": {}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);
        
        Document doc = documentWithOutputLocation("index.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(2, pages.size());
        assertPageEquals("index.html", "Home", true, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("about.html", "About", false, false, PagePosition.LAST, pages.get(1));

        doc = documentWithOutputLocation("no-page-flow.html");
        pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(2, pages.size());
        assertPageEquals("index.html", "Home", false, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("about.html", "About", false, false, PagePosition.LAST, pages.get(1));
    }

    @Test
    public void pageSequence_inBothDocumentsAndPluginsSections() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": [" +
                "    {\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"other.txt\", \"title\": \"Other\"}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"other.html\", \"title\": \"OtherLink\"}" +
                "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("other.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("about.html", "About", false, false, PagePosition.BETWEEN, pages.get(1));
        assertPageEquals("other.html", "OtherLink", true, false, PagePosition.LAST, pages.get(2));
    }

    @Test
    public void notActivated_withoutEmptyPluginDeclaration() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(
                "{\"documents\": [{\"input\": \"index.txt\", \"output\": \"index.html\", " +
                        "\"title\": \"Home\", \"page-flows\": [\"sections\"]}], " +
                        "\"plugins\": {}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);
        assertNull(plugin);
    }

    @Test
    public void severalPageFlows() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": [" +
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
            "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("narration.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("about.html", "About", false, false, PagePosition.BETWEEN, pages.get(1));
        assertPageEquals("narration.html", "Narration", true, false, PagePosition.LAST, pages.get(2));

        doc = documentWithOutputLocation("other1.html");
        pages = extractPages(plugin.variables(doc).get("other_links"));
        assertEquals(2, pages.size());
        assertPageEquals("other1.html", "OtherLink1", true, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("other2.html", "OtherLink2", false, false, PagePosition.LAST, pages.get(1));
    }

    @Test
    public void sameDocumentInSeveralPageFlows() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": [" +
                "    {\"input\": \"index.txt\", \"title\": \"Home\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"about.txt\", \"title\": \"About\", \"page-flows\": [\"sections\", \"other_links\"]}, " +
                "    {\"input\": \"other.txt\", \"title\": \"Other\", \"page-flows\": [\"other_links\"]}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"other.html\", \"title\": \"OtherLink\"}" +
                "], \"other_links\": [" +
                "    {\"link\": \"index.html\", \"title\": \"HomeLink\"}" +
                "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("other.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(3, pages.size());
        assertPageEquals("index.html", "Home", false, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("about.html", "About", false, false, PagePosition.BETWEEN, pages.get(1));
        assertPageEquals("other.html", "OtherLink", true, false, PagePosition.LAST, pages.get(2));

        pages = extractPages(plugin.variables(doc).get("other_links"));
        assertEquals(3, pages.size());
        assertPageEquals("about.html", "About", false, false, PagePosition.FIRST, pages.get(0));
        assertPageEquals("other.html", "Other", true, false, PagePosition.BETWEEN, pages.get(1));
        assertPageEquals("index.html", "HomeLink", false, false, PagePosition.LAST, pages.get(2));
    }

    @Test
    public void externalLinks() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": [" +
            "    {\"input\": \"index.txt\", \"title\": \"Home\"} " +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"index.html\", \"title\": \"HomeLinkExternal\", \"external\": true}, " +
                "    {\"link\": \"index.html\", \"title\": \"HomeLink\", \"external\": false}" +
                "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("index.html");
        List<Map<String, Object>> pages = extractPages(plugin.variables(doc).get("sections"));
        assertEquals(2, pages.size());
        assertPageEquals("index.html", "HomeLinkExternal", false, true, PagePosition.FIRST, pages.get(0));
        assertPageEquals("index.html", "HomeLink", true, false, PagePosition.LAST, pages.get(1));
    }

    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public void navigation(String testName, String argFileContent) throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(argFileContent, DUMMY_CLI_OPTIONS)
                .getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("page1.html");
        TestPageFlow pageFlow = new TestPageFlow(plugin.variables(doc).get("sections"));
        assertTrue(pageFlow.isHas_navigation());
        assertTrue(pageFlow.isNot_empty());
        assertNull(pageFlow.getPrevious());
        assertPageEquals("page1.html", "Title1", true, false, pageFlow.getCurrent());
        assertPageEquals("page2.html", "Title2", false, false, pageFlow.getNext());

        doc = documentWithOutputLocation("page2.html");
        pageFlow = new TestPageFlow(plugin.variables(doc).get("sections"));
        assertTrue(pageFlow.isHas_navigation());
        assertTrue(pageFlow.isNot_empty());
        assertPageEquals("page1.html", "Title1", false, false, pageFlow.getPrevious());
        assertPageEquals("page2.html", "Title2", true, false, pageFlow.getCurrent());
        assertPageEquals("page3.html", "Title3", false, false, pageFlow.getNext());

        doc = documentWithOutputLocation("page3.html");
        pageFlow = new TestPageFlow(plugin.variables(doc).get("sections"));
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

    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public void navigation_generalized(String testName, int pageCount, String argFileContent)
            throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile(argFileContent, DUMMY_CLI_OPTIONS)
                .getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        for (int i = 1; i <= pageCount; ++i) {
            Document doc = documentWithOutputLocation("page" + i + ".html");
            TestPageFlow pageFlow = new TestPageFlow(plugin.variables(doc).get("sections"));
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
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{\"documents\": [" +
                "    {\"input\": \"root1.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"root2.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"doc/sub1.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"doc/sub2.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}, " +
                "    {\"input\": \"doc/ch01/sub-sub.txt\", \"title\": \"whatever\"}" +
                "], \"plugins\": {\"page-flows\": {\"sections\": [" +
                "    {\"link\": \"doc/ch01/sub-sub-1.html\", \"title\": \"whatever\"}," +
                "    {\"link\": \"doc/ch01/sub-sub-2.html\", \"title\": \"whatever\"}" +
                "]}}}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

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

    @Test
    public void test_extended_format_simple() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{" +
                "   \"documents\": [" +
                "       {\"input\": \"page1.txt\", \"title\": \"whatever\"}" +
                "   ]," +
                "   \"plugins\": {\"page-flows\": {\"sections\": { \"title\": \"Sections\", \"groups\": [\"gr1\"], " +
                "          \"items\": [" +
                "              {\"link\": \"link1.html\", \"title\": \"title1\"}," +
                "              {\"link\": \"link2.html\", \"title\": \"title2\"}" +
                "          ]" +
                "   }}}" +
                "}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("page1.html");
        Object pageFlowObject = plugin.variables(doc).get("sections");
        TestPageFlow pageFlow = new TestPageFlow(pageFlowObject);
        assertEquals("Sections", pageFlow.getTitle());
        List<Map<String, Object>> pages = extractPages(pageFlowObject);
        assertEquals("link1.html", pages.get(0).get("link"));
        assertEquals("title1", pages.get(0).get("title"));
        assertEquals("link2.html", pages.get(1).get("link"));
        assertEquals("title2", pages.get(1).get("title"));

        //noinspection unchecked
        List<Object> groupObject = (List<Object>) plugin.variables(doc).get("gr1");
        pages = extractPages(groupObject.get(0));
        assertEquals("link1.html", pages.get(0).get("link"));
        assertEquals("title1", pages.get(0).get("title"));
        assertEquals("link2.html", pages.get(1).get("link"));
        assertEquals("title2", pages.get(1).get("title"));
    }

    @Test
    public void test_extended_format_in_documents() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{" +
                "   \"documents\": [" +
                "       {\"input\": \"page1.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}," +
                "       {\"input\": \"page2.txt\", \"title\": \"whatever\", \"page-flows\": [\"sections\"]}" +
                "   ]," +
                "   \"plugins\": {\"page-flows\": {\"sections\": { \"title\": \"Sections\", \"groups\": [\"gr1\"], " +
                "          \"items\": [" +
                "              {\"link\": \"link1.html\", \"title\": \"whatever\"}" +
                "          ]" +
                "   }}}" +
                "}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("page1.html");
        Object pageFlowObject = plugin.variables(doc).get("sections");
        TestPageFlow pageFlow = new TestPageFlow(pageFlowObject);
        assertEquals("Sections", pageFlow.getTitle());
        List<Map<String, Object>> pages = extractPages(pageFlowObject);
        assertEquals("page1.html", pages.get(0).get("link"));
        assertEquals("page2.html", pages.get(1).get("link"));
        assertEquals("link1.html", pages.get(2).get("link"));

        //noinspection unchecked
        List<Object> groupObject = (List<Object>) plugin.variables(doc).get("gr1");
        pages = extractPages(groupObject.get(0));
        assertEquals(1, groupObject.size());
        assertEquals("page1.html", pages.get(0).get("link"));
        assertEquals("page2.html", pages.get(1).get("link"));
        assertEquals("link1.html", pages.get(2).get("link"));
    }

    @Test
    public void test_extended_format_several_groups() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{" +
                "    \"documents\": [" +
                "        {\"input\": \"page1.txt\", \"title\": \"whatever\"}" +
                "    ]," +
                "    \"plugins\": {\"page-flows\": {" +
                "        \"flow1\": { \"title\": \"Flow 1\", \"groups\": [\"gr1\"], " +
                "            \"items\": [{\"link\": \"link1.html\", \"title\": \"whatever\"}]" +
                "        }," +
                "        \"flow2\": { \"title\": \"Flow 2\", \"groups\": [\"gr1\", \"gr2\"], " +
                "            \"items\": [{\"link\": \"link2.html\", \"title\": \"whatever\"}]" +
                "        }," +
                "        \"flow3\": { \"title\": \"Flow 3\", \"groups\": [\"gr2\"], " +
                "            \"items\": [{\"link\": \"link3.html\", \"title\": \"whatever\"}]" +
                "        }" +
                "    }}" +
                "}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);

        Document doc = documentWithOutputLocation("page1.html");

        Object groupAsObject = plugin.variables(doc).get("gr1");
        //noinspection unchecked
        List<Object> groupAsList = (List<Object>) groupAsObject;
        assertEquals(2, groupAsList.size());
        List<TestPageFlow> group = convertPageFlowGroup(groupAsObject);
        assertEquals("Flow 1", group.get(0).getTitle());
        assertEquals("Flow 2", group.get(1).getTitle());
        List<Map<String, Object>> pages = extractPages(groupAsList.get(0));
        assertEquals("link1.html", pages.get(0).get("link"));
        pages = extractPages(groupAsList.get(1));
        assertEquals("link2.html", pages.get(0).get("link"));

        groupAsObject = plugin.variables(doc).get("gr2");
        //noinspection unchecked
        groupAsList = (List<Object>) groupAsObject;
        assertEquals(2, groupAsList.size());
        group = convertPageFlowGroup(groupAsObject);
        assertEquals("Flow 2", group.get(0).getTitle());
        assertEquals("Flow 3", group.get(1).getTitle());
        pages = extractPages(groupAsList.get(0));
        assertEquals("link2.html", pages.get(0).get("link"));
        pages = extractPages(groupAsList.get(1));
        assertEquals("link3.html", pages.get(0).get("link"));
    }

    @Test
    public void test_extended_format_duplicate_error() throws ArgFileParseException {
        List<Md2HtmlPlugin> plugins = parseArgumentFile("{" +
                "   \"documents\": [" +
                "       {\"input\": \"page1.txt\", \"title\": \"whatever\"}" +
                "   ]," +
                "   \"plugins\": {\"page-flows\": {\"name1\": { \"title\": \"Sections\", \"groups\": [\"gr1\", \"name1\"], " +
                "          \"items\": [" +
                "              {\"link\": \"link1.html\", \"title\": \"title1\"}" +
                "          ]" +
                "   }}}" +
                "}", DUMMY_CLI_OPTIONS).getPlugins();
        PageFlowsPlugin plugin = findSinglePlugin(plugins);
        Document doc = documentWithOutputLocation("page1.html");
        UserError e = assertThrows(UserError.class, () -> plugin.variables(doc));
        assertTrue(e.getMessage().contains("Variable duplication"));
    }
}
