package world.md2html.testutils;

import world.md2html.options.model.Document;

import java.util.List;

public final class PluginTestUtils {

    public static final Document ANY_DOCUMENT = documentWithOutputLocation("whatever.html");

    private PluginTestUtils() {}

    public static <T, R extends T> R findFirstElementOfType(
            List<? extends T> elements,
            Class<R> requiredClass
    ) {
        for (T element : elements) {
            if (requiredClass.isAssignableFrom(element.getClass())) {
                return requiredClass.cast(element);
            }
        }
        return null;
    }

    public static Document documentWithOutputLocation(String outputLocation) {
        return Document.builder().output(outputLocation).build();
    }

}
