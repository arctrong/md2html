package world.md2html;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Md2HtmlUtilsTest {

    @Test
    void extractPageMetadata_emptyInput() {
        assertFalse(Md2HtmlUtils.extractPageMetadataSection(null).isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("").isPresent());
    }

    @Test
    void extractPageMetadata_noMetadata() {
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("no metadata").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("<!---->").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("<!-- -->").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("not opened -->").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("<!-- not closed").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("<!-- not metadata -->").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("<!--notMetadata -->").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection("<!-- metadata with space -->")
                .isPresent());
    }

    @Test
    void extractPageMetadata_emptyMetadata() {
        Optional<String> metadata = Md2HtmlUtils.extractPageMetadataSection("<!--metadata-->");
        assertTrue(metadata.isPresent());
        assertEquals("", metadata.get());
    }

    @Test
    void extractPageMetadata_prepended() {
        assertTrue(Md2HtmlUtils.extractPageMetadataSection(" \t \n <!--metadata -->").isPresent());
        assertFalse(Md2HtmlUtils.extractPageMetadataSection(" \t a \n <!--METADATA -->")
                .isPresent());
    }

    @Test
    void extractPageMetadata_postpended() {
        assertTrue(Md2HtmlUtils.extractPageMetadataSection("<!--metadata-->no_matter_what")
                .isPresent());
    }


    @Test
    void extractPageMetadata_caseInsensitive() {
        assertTrue(Md2HtmlUtils.extractPageMetadataSection("<!--metadata -->").isPresent());
        assertTrue(Md2HtmlUtils.extractPageMetadataSection("<!--meTAdaTA -->").isPresent());
    }

    @Test
    void extractPageMetadata_multiline() {
        Optional<String> metadata =
                Md2HtmlUtils.extractPageMetadataSection("<!--metadata line1\nline2 -->");
        assertTrue(metadata.isPresent());
        assertEquals(" line1\nline2 ", metadata.get());
    }

    @Test
    void extractPageMetadata_withoutSpaces() {
        Optional<String> metadata =
                Md2HtmlUtils.extractPageMetadataSection("<!--metadataMETADATA-->");
        assertTrue(metadata.isPresent());
        assertEquals("METADATA", metadata.get());
    }

    @Test
    void extractPageMetadata_withLiteralClose() {
        // In JSON strings `-->` may be represented as `-\u002D>`.
        Optional<String> metadata =
                Md2HtmlUtils.extractPageMetadataSection("<!--metadata \"-->\" -->");
        assertTrue(metadata.isPresent());
        assertEquals(" \"", metadata.get());
    }

    @Test
    void jsonParser_unicodeEntitiesInStrings() {
        // This is an external library but we need to be sure about this certain case.
        JsonObject jsonObject = Json.parse("{\"key\":\"<!\\u002D-value-\\u002D>\"}").asObject();
        JsonValue value = jsonObject.get("key");
        assertEquals("<!--value-->", value.asString());
    }

}
