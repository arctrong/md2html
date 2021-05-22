package world.md2html.pagemetadata;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Md2HtmlPageMetadataExtractorTest {

    /**
     * This method is used for brevity and readability only.
     */
    private PageMetadataExtractionResult extractMetadata(String text) {
        return Md2HtmlPageMetadataExtractor.extract(text);
    }

    @Test
    public void extractPageMetadata_emptyInput() {
        assertFalse(extractMetadata(null).isSuccess());
        assertFalse(extractMetadata("").isSuccess());
    }

    @Test
    public void extractPageMetadata_noMetadata() {
        assertFalse(extractMetadata("no metadata").isSuccess());
        assertFalse(extractMetadata("<!---->").isSuccess());
        assertFalse(extractMetadata("<!-- -->").isSuccess());
        assertFalse(extractMetadata("not opened -->").isSuccess());
        assertFalse(extractMetadata("<!-- not closed").isSuccess());
        assertFalse(extractMetadata("<!-- not metadata -->").isSuccess());
        assertFalse(extractMetadata("<!--notMetadata -->").isSuccess());
        assertFalse(extractMetadata("<!-- metadata with space -->").isSuccess());
    }

    @Test
    public void extractPageMetadata_emptyMetadata() {
        PageMetadataExtractionResult metadata = extractMetadata("<!--metadata-->");
        assertTrue(metadata.isSuccess());
        assertEquals("", metadata.getMetadata());
    }

    @Test
    public void extractPageMetadata_start() {
        assertEquals(0, extractMetadata("<!--metadata-->").getStart());
        assertEquals(1, extractMetadata(" <!--metadata-->").getStart());
        assertEquals(1, extractMetadata("\n<!--metadata-->").getStart());
        assertEquals(1, extractMetadata("\r<!--metadata-->").getStart());
        assertEquals(2, extractMetadata("\r\n<!--metadata-->").getStart());
        assertEquals(5, extractMetadata("\n \t \n<!--metadata-->").getStart());
    }

    @Test
    public void extractPageMetadata_end() {
        assertEquals(15, extractMetadata("<!--metadata-->").getEnd());
        assertEquals(16, extractMetadata(" <!--metadata-->whatever").getEnd());
        assertEquals(16, extractMetadata(" <!--metadata-->\n").getEnd());
        assertEquals(16, extractMetadata(" <!--metadata-->\nwhatever").getEnd());
    }

    @Test
    public void extractPageMetadata_prepended() {
        assertTrue(extractMetadata(" \t \n <!--metadata -->").isSuccess());
        assertFalse(extractMetadata(" \t a \n <!--METADATA -->").isSuccess());
    }

    @Test
    public void extractPageMetadata_postpended() {
        assertTrue(extractMetadata("<!--metadata-->no_matter_what").isSuccess());
    }

    @Test
    public void extractPageMetadata_caseInsensitive() {
        assertTrue(extractMetadata("<!--metadata -->").isSuccess());
        assertTrue(extractMetadata("<!--meTAdaTA -->").isSuccess());
    }

    @Test
    public void extractPageMetadata_multiline() {
        PageMetadataExtractionResult metadata =
                extractMetadata("<!--metadata line1\nline2 -->");
        assertTrue(metadata.isSuccess());
        assertEquals(" line1\nline2 ", metadata.getMetadata());
    }

    @Test
    public void extractPageMetadata_withoutSpaces() {
        PageMetadataExtractionResult metadata =
                extractMetadata("<!--metadataMETADATA-->");
        assertTrue(metadata.isSuccess());
        assertEquals("METADATA", metadata.getMetadata());
    }

    @Test
    public void extractPageMetadata_withLiteralClose() {
        // In JSON strings `-->` may be represented as `-\u002D>`.
        PageMetadataExtractionResult metadata = extractMetadata("<!--metadata \"-->\" -->");
        assertTrue(metadata.isSuccess());
        assertEquals(" \"", metadata.getMetadata());
    }

    @Test
    public void jsonParser_unicodeEntitiesInStrings() {
        // This is an external library but we need to be sure about this certain case.
        JsonObject jsonObject = Json.parse("{\"key\":\"<!\\u002D-value-\\u002D>\"}").asObject();
        JsonValue value = jsonObject.get("key");
        assertEquals("<!--value-->", value.asString());
    }

}
