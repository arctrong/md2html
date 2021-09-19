package world.md2html.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtilsTest {

    @Test
    void stripExtension() {
        assertEquals("", Utils.stripExtension(""));
        assertEquals("file_with_one", Utils.stripExtension("file_with_one.extension"));
        assertEquals("file_with.two", Utils.stripExtension("file_with.two.extensions"));
        assertEquals("file_without_extension", Utils.stripExtension("file_without_extension"));
        assertEquals(".file_without_name", Utils.stripExtension(".file_without_name"));
        assertEquals("a", Utils.stripExtension("a.file_with_short_name"));
        assertEquals("file/with/path", Utils.stripExtension("file/with/path.txt"));
        assertEquals("with\\path_without_ext", Utils.stripExtension("with\\path_without_ext"));
        assertEquals("with/dotted.path/name", Utils.stripExtension("with/dotted.path/name.ext"));
        assertEquals("with\\dotted.path/name", Utils.stripExtension("with\\dotted.path/name"));
        assertEquals("with/dotted.path/.name", Utils.stripExtension("with/dotted.path/.name"));
        assertEquals("with\\path/a", Utils.stripExtension("with\\path/a.ext"));
    }

    @Test
    void firstNotNull() {
        assertEquals(1, Utils.firstNotNull(null, 1, 2));
        assertNull(Utils.firstNotNull(null, null));
        assertNull(Utils.firstNotNull());
        Object o = new String[0];
        assertEquals(o, Utils.firstNotNull(o, new String[] {"1", "2"}));
    }

}
