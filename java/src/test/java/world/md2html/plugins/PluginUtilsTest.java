package world.md2html.plugins;

import com.networknt.schema.JsonSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import world.md2html.options.model.Document;
import world.md2html.utils.JsonUtils;
import world.md2html.utils.UserError;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static world.md2html.utils.JsonUtils.loadJsonSchemaFromResource;
import static world.md2html.utils.Utils.mapOf;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PluginUtilsTest {

    private static final Document DUMMY_DOC = Document.builder()
            .input("dummy.txt").build();

    public static Stream<Arguments> listFromStringOrArray() {
        return Stream.of(
                Arguments.of("empty string", "", Collections.singletonList("")),
                Arguments.of("value as string", " some string ",
                        Collections.singletonList(" some string ")),
                Arguments.of("value empty list", "[]", Collections.emptyList()),
                Arguments.of("list of one value", "[\"one value\"]",
                        Collections.singletonList("one value")),
                Arguments.of("list of several values", "[\"value1\", \"value2\"]",
                        asList("value1", "value2"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void listFromStringOrArray(String name, String input, List<String> expected) {
        assertEquals(expected,
                JsonUtils.deJson(PluginUtils.listFromStringOrArray(DUMMY_DOC, input)));
    }

    public Stream<Arguments> mapFromStringOrObject() {
        return Stream.of(
                Arguments.of("empty string", "", "key", mapOf("key", "")),
                Arguments.of("empty string", "", "key", mapOf("key", "")),
        Arguments.of("value as string", " some string ", "key", mapOf("key", " some string ")),
        Arguments.of("value empty dict", "{}", "key", mapOf()),
                Arguments.of("dict of one value", "{\"key\": \"value\"}", "key",
                        mapOf("key", "value")),
        Arguments.of("dict of several values", "{\"k1\": \"v1\", \"k2\": 2, \"k3\": true}",
                "key", mapOf("k1", "v1", "k2", 2, "k3", true))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void mapFromStringOrObject(String name, String input, String key,
                               Map<String, Object> expected) {
        assertEquals(expected,
                JsonUtils.deJson(PluginUtils.mapFromStringOrObject(input, key)));
    }

    @Test
    void testDictFromStringOrArrayWithSchemaPositive() {
        JsonSchema metadataSchema =
                loadJsonSchemaFromResource("dictFromStringOrArrayTestSchema.json");
        assertEquals(mapOf("file", "readme.txt"),
                JsonUtils.deJson(PluginUtils.mapFromStringOrObject("{\"file\": \"readme.txt\"}",
                        "key", metadataSchema)));
    }

    @Test
    void testDictFromStringOrArrayWithSchemaNegative() {
        JsonSchema metadataSchema =
                loadJsonSchemaFromResource("dictFromStringOrArrayTestSchema.json");
        UserError e = assertThrows(UserError.class,
                () -> PluginUtils.mapFromStringOrObject("{\"filename\": \"readme.txt\"}",
                        "key", metadataSchema)
        );
        assertTrue(e.getMessage().contains("Validation error"));
    }
}
