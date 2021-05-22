package world.md2html.pagemetadata;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Md2HtmlPageMetadataParserTest {

    /** This method is used for brevity and readability only. */
    private PageMetadataParsingResult parseMetadata(String metadataSection) {
        return Md2HtmlPageMetadataParser.parse(metadataSection);
    }

    @Test
    public void parse() {
    }

    @Test
    public void parse_notObject() {
        PageMetadataParsingResult result = parseMetadata("[]");
        assertNull(result.getPageMetadata());
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void parse_emptyString() {
        PageMetadataParsingResult result = parseMetadata("");
        assertNull(result.getPageMetadata());
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void parse_nullInput() {
        PageMetadataParsingResult result = parseMetadata(null);
        assertNull(result.getPageMetadata());
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_malformedJson() {
        PageMetadataParsingResult result = parseMetadata("not a Json");
        assertNull(result.getPageMetadata());
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void parse_emptyObject() {
        PageMetadataParsingResult result = parseMetadata("{}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertNull(metadata.getTitle());
        assertTrue(metadata.getCustomTemplatePlaceholders().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_unexpectedItems() {
        PageMetadataParsingResult result = parseMetadata("{\"unexpectedItem\": \"correct value\"}");
        assertTrue(result.isSuccess());
        assertNotNull(result.getPageMetadata());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_correctTitle() {
        PageMetadataParsingResult result = parseMetadata("{\"title\": \"My title\"}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertEquals("My title", metadata.getTitle());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_emptyTitle() {
        PageMetadataParsingResult result = parseMetadata("{\"title\": \"\"}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertEquals("", metadata.getTitle());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_unicodeEntitiesInStrings() {
        PageMetadataParsingResult result = parseMetadata("{\"title\":\"<!\\u002D-value-\\u002D>\"}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertEquals("<!--value-->", metadata.getTitle());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_incorrectTitleValue() {
        PageMetadataParsingResult result = parseMetadata("{ \"title\": 150 }");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertNull(metadata.getTitle());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void parse_incorrectTitleKey() {
        PageMetadataParsingResult result = parseMetadata("{\"Title\": \"correct title value\"}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertNull(metadata.getTitle());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_customTemplatePlaceholders_empty() {
        PageMetadataParsingResult result = parseMetadata("{\"custom_template_placeholders\": {}}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertTrue(metadata.getCustomTemplatePlaceholders().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_customTemplatePlaceholders_wrong() {
        PageMetadataParsingResult result =
                parseMetadata("{\"custom_template_placeholders\": \"not dict\" }");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        assertTrue(metadata.getCustomTemplatePlaceholders().isEmpty());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void parse_customTemplatePlaceholders_correctItems() {
        PageMetadataParsingResult result = parseMetadata("{\"custom_template_placeholders\": " +
                "{\"ph1\": \"val1\", \"ph2\": \"val2\"}}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        Map<String, String> cph = metadata.getCustomTemplatePlaceholders();
        assertEquals("val1", cph.get("ph1"));
        assertEquals("val2", cph.get("ph2"));
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void parse_customTemplatePlaceholders_incorrectItems() {
        PageMetadataParsingResult result = parseMetadata("{\"custom_template_placeholders\": " +
                        "{\"ph1\": 101, \"ph2\": \"val2\"}}");
        assertTrue(result.isSuccess());
        PageMetadata metadata = result.getPageMetadata();
        Map<String, String> cph = metadata.getCustomTemplatePlaceholders();
        assertFalse(cph.containsKey("ph1"));
        assertEquals("val2", cph.get("ph2"));
        assertFalse(result.getErrors().isEmpty());
    }

}