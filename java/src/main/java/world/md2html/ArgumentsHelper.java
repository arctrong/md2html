package world.md2html;

import com.fasterxml.jackson.databind.JsonNode;
import org.javatuples.Pair;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.ArrayList;
import java.util.Map;

import static world.md2html.options.argfile.ArgFileParsingHelper.completeArgFileProcessing;
import static world.md2html.options.argfile.ArgFileParsingHelper.mergeAndCanonizeArgFileRaw;
import static world.md2html.options.argfile.PluginHelper.addExtraPluginData;
import static world.md2html.options.argfile.PluginHelper.completePluginsInitialization;
import static world.md2html.options.argfile.PluginHelper.feedPluginsWithAppData;
import static world.md2html.options.argfile.PluginHelper.feedPluginsWithDocuments;
import static world.md2html.options.argfile.PluginHelper.filterNonBlankPlugins;
import static world.md2html.options.argfile.PluginHelper.instantiatePlugins;

public class ArgumentsHelper {

    private ArgumentsHelper() {
    }

    public static ArgFile parseArgumentFile(ArgFileRaw argFileRaw,
            CliOptions cliOptions) throws ArgFileParseException {

        ArgFileRaw canonizedArgFileRaw = mergeAndCanonizeArgFileRaw(argFileRaw, cliOptions);
        Map<String, Md2HtmlPlugin> plugins = instantiatePlugins(canonizedArgFileRaw.getPlugins());

        // Plugins are not initialized yet, but 'page-variables' plugin will be used in the
        // following call. Still particularly this plugin is already fully functional.
        Pair<ArgFile, Map<String, JsonNode>> processingResult =
                completeArgFileProcessing(canonizedArgFileRaw, plugins);
        ArgFile arguments = processingResult.getValue0();
        Map<String, JsonNode> extraPluginData = processingResult.getValue1();

        addExtraPluginData(extraPluginData, plugins);
        completePluginsInitialization(argFileRaw, cliOptions, plugins);
        feedPluginsWithDocuments(plugins, arguments.getDocuments());
        plugins = filterNonBlankPlugins(plugins);

        ArgFile argFile = arguments.toBuilder().plugins(new ArrayList<>(plugins.values())).build();

        feedPluginsWithAppData(plugins, argFile);

        return argFile;
    }

}
