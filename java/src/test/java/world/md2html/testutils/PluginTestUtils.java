package world.md2html.testutils;

import world.md2html.options.model.Document;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.List;

public final class PluginTestUtils {

    public static final Document ANY_DOCUMENT = documentWithOutputLocation("whatever.html");

    private PluginTestUtils() {}

    public static <T extends Md2HtmlPlugin> T findSinglePlugin(List<? extends Md2HtmlPlugin> plugins,
            Class<T> pluginClass) {
        Md2HtmlPlugin result = null;
        for (Md2HtmlPlugin plugin : plugins) {
            if (pluginClass.equals(plugin.getClass())) {
                if (result == null) {
                    result = plugin;
                } else {
                    throw new IllegalArgumentException("More than one plugins of type'" +
                            pluginClass.getSimpleName() + "' found");
                }
            }
        }
        // TODO Try to remove this warning.
        return (T) result;
    }

    public static Document documentWithOutputLocation(String outputLocation) {
        return Document.builder().output(outputLocation).build();
    }

}
