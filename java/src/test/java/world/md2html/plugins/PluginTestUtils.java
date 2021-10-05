package world.md2html.plugins;

import com.vladsch.flexmark.util.sequence.PrefixedSubSequence;
import world.md2html.options.model.Document;

import java.util.List;

public final class PluginTestUtils {

    private PluginTestUtils() {}

    public static Md2HtmlPlugin findSinglePlugin(List<Md2HtmlPlugin> plugins,
            Class<? extends Md2HtmlPlugin> pluginClass) {
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
        return result;
    }

    public static Document documentWithOutputLocation(String outputLocation) {
        return new Document(null, outputLocation, null, null, null, null, false, false, false,
                false);
    }

}
