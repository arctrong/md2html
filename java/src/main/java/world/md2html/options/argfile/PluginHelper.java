package world.md2html.options.argfile;

import com.fasterxml.jackson.databind.JsonNode;
import world.md2html.Constants;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.*;
import java.util.function.Supplier;

public class PluginHelper {

    private PluginHelper() {
    }

    public static Map<String, Md2HtmlPlugin> instantiatePlugins(
            Map<String, JsonNode> pluginNodes) throws ArgFileParseException {

        Map<String, Md2HtmlPlugin> instantiatedPlugins = new HashMap<>();

        for (Map.Entry<String, JsonNode> pluginEntry : pluginNodes.entrySet()) {
            Supplier<Md2HtmlPlugin> pluginProvider =
                    Constants.PLUGIN_PROVIDERS.get(pluginEntry.getKey());
            if (pluginProvider != null) {
                try {
                    Md2HtmlPlugin plugin = pluginProvider.get();
                    plugin.acceptData(pluginEntry.getValue());
                    instantiatedPlugins.put(pluginEntry.getKey(), plugin);
                } catch (ArgFileParseException e) {
                    throw new ArgFileParseException("Error initializing plugin '" +
                            pluginEntry.getKey() + "': " + e.getMessage());
                }
            } else {
                throw new ArgFileParseException("Unknown plugin found: " + pluginEntry.getKey());
            }
        }

        return instantiatedPlugins;
    }

    public static void addExtraPluginData(Map<String, JsonNode> extraPluginData,
            Map<String, Md2HtmlPlugin> plugins) throws ArgFileParseException {

        for (Map.Entry<String, JsonNode> extraDataEntry : extraPluginData.entrySet()) {
            Md2HtmlPlugin plugin = plugins.get(extraDataEntry.getKey());
            if (plugin != null) {
                plugin.acceptData(extraDataEntry.getValue());
            }
        }
    }

    public static void completePluginsInitialization(ArgFileRaw argFileRaw, CliOptions cliOptions,
            Map<String, Md2HtmlPlugin> plugins) throws ArgFileParseException {

        Map<String, List<JsonNode>> extraPluginData = new HashMap<>();

        for (Md2HtmlPlugin plugin : plugins.values()) {
            Map<String, JsonNode> singleExtraPluginData = plugin.preInitialize(argFileRaw,
                    cliOptions, plugins);
            for (Map.Entry<String, JsonNode> singleData : singleExtraPluginData.entrySet()) {
                List<JsonNode> dataForPlugin = extraPluginData.computeIfAbsent(singleData.getKey(),
                        k -> new ArrayList<>());
                dataForPlugin.add(singleData.getValue());
            }
        }
        for (Map.Entry<String, Md2HtmlPlugin> plugin : plugins.entrySet()) {
            List<JsonNode> dataForPlugin = Optional.ofNullable(extraPluginData
                    .get(plugin.getKey())).orElse(Collections.singletonList(null));
            for (JsonNode data : dataForPlugin) {
                plugin.getValue().initialize(data);
            }
        }
    }

    public static void feedPluginsWithDocuments(Map<String, Md2HtmlPlugin> plugins,
            List<Document> documents) {
        for (Md2HtmlPlugin plugin : plugins.values()) {
            plugin.acceptDocumentList(documents);
        }
    }

    public static Map<String, Md2HtmlPlugin> filterNonBlankPlugins(Map<String,
            Md2HtmlPlugin> plugins) {
        Map<String, Md2HtmlPlugin> nonBlankPlugins = new HashMap<>();
        for (Map.Entry<String, Md2HtmlPlugin> plugin : plugins.entrySet()) {
            if (!plugin.getValue().isBlank()) {
                nonBlankPlugins.put(plugin.getKey(), plugin.getValue());
            }
        }
        return nonBlankPlugins;
    }

    public static void feedPluginsWithAppData(Map<String, Md2HtmlPlugin> plugins,
                                              ArgFile arguments) {
        for (Md2HtmlPlugin plugin : plugins.values()) {
            plugin.acceptAppData(arguments.getOptions(), arguments.getPlugins(),
                    arguments.getMetadataHandlers());
        }
    }
}
