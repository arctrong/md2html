package world.md2html;

import com.fasterxml.jackson.databind.node.ObjectNode;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.cli.CliArgumentsException;
import world.md2html.options.cli.CliParser;
import world.md2html.options.model.ArgFileOptions;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.FinalizationAction;
import world.md2html.plugins.InitializationAction;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class Md2HtmlRunner {

    public static void main(String[] args) throws Exception {

        long start = System.nanoTime();

        String usage = "java " + Md2Html.class.getSimpleName();
        CliOptions cliOptions = null;
        try {
            cliOptions = new CliParser(usage).parse(args);
        } catch (CliArgumentsException e) {
            System.out.println(e.getPrintText());
            System.exit(1);
        }

        Path argumentFile = cliOptions.getArgumentFile();
        String argumentFileString = null;
        if (argumentFile != null) {
            try {
                argumentFileString = Utils.readStringFromCommentedFile(argumentFile, "#",
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("Error parsing argument file '" + argumentFile +
                        "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
        } else {
            // When run without argument file, need implicitly added
            // plugin for page title extraction from the source text.
            argumentFileString = "{\"documents\": [{}], \"plugins\": {\"page-variables\": " +
                    "{\"VARIABLES\": {\"only-at-page-start\": true}";
            if (cliOptions.isLegacyMode()) {
                argumentFileString += ", \"METADATA\": {\"only-at-page-start\": true}";
            }
            argumentFileString += "}}}}";
        }

        ArgFileOptions argFileOptions = null;
        ObjectNode argFileNode = null;
        try {
            argFileNode = ArgFileParser.readArgumentFileNode(argumentFileString);
            argFileOptions = ArgFileParser.parse(argFileNode, cliOptions);
        } catch (ArgFileParseException e) {
            System.out.println("Error parsing argument file '" + argumentFile + "': " +
                    e.getMessage());
            System.exit(1);
        }

        for (Md2HtmlPlugin plugin : argFileOptions.getPlugins()) {
            List<InitializationAction> initializationActions = plugin.initializationActions();
            if (initializationActions != null) {
                for (InitializationAction action : initializationActions) {
                    action.initialize(argFileNode, cliOptions, argFileOptions.getPlugins());
                }
            }
        }

        PageMetadataHandlersWrapper metadataHandlersWrapper =
                PageMetadataHandlersWrapper.fromPlugins(argFileOptions.getPlugins());

        for (Document doc : argFileOptions.getDocuments()) {
            try {
                Md2Html.execute(argFileOptions.getOptions(), doc, argFileOptions.getPlugins(),
                        metadataHandlersWrapper);
            } catch(UserError e) {
                System.out.println("Error processing input file '" + doc.getInputLocation() +
                        "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
        }

        for (Md2HtmlPlugin plugin : argFileOptions.getPlugins()) {
            List<FinalizationAction> finalizationActions = plugin.finalizationActions();
            if (finalizationActions != null) {
                try {
                    for (FinalizationAction finalizationAction : finalizationActions) {
                        finalizationAction.finalizePlugin();
                    }
                } catch (UserError e) {
                    System.out.println("Error executing finalization action for plugin '"
                            + plugin.getClass().getSimpleName() + "': " + e.getMessage());
                    System.exit(1);
                }
            }
        }

        if (argFileOptions.getOptions().isVerbose()) {
            long end = System.nanoTime();
            System.out.println("Finished in: " + Utils.formatNanoSeconds(end - start));
        }
    }

}
