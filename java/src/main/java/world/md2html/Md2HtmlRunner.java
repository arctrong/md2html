package world.md2html;

import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.cli.CliArgumentsException;
import world.md2html.options.cli.CliParser;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.Document;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.utils.UserError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static world.md2html.options.argfile.ArgFileParsingHelper.readArgumentFileNode;
import static world.md2html.utils.Utils.formatNanoSeconds;
import static world.md2html.utils.Utils.readStringFromCommentedFile;

public class Md2HtmlRunner {

    public static void main(String[] args) throws Exception {
        try {
            execute(args);
        } catch (UserError ue) {
            System.out.println(ue.getMessage());
            System.exit(1);
        }
    }

    private static void execute(String[] args) throws IOException {
        long start = System.nanoTime();

        String usage = "java " + Md2Html.class.getSimpleName();
        CliOptions cliOptions;
        try {
            cliOptions = new CliParser(usage).parse(args);
        } catch (CliArgumentsException e) {
            throw new UserError(e.getPrintText());
        }

        String argumentFileString;
        String argumentFile = cliOptions.getArgumentFile();
        if (argumentFile == null) {
            argumentFileString =
                    "{\"documents\": [{}], \n" +
                    // When run without argument file, need to implicitly add
                    // plugin for page title extraction from the source text.
                    "\"plugins\": {\"page-variables\": \n" +
                    "    {\"VARIABLES\": {\"only-at-page-start\": true}}} \n" +
                    "}";
        } else {
            try {
                argumentFileString = readStringFromCommentedFile(Paths.get(argumentFile),
                        "#", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UserError("Error reading argument file '" + argumentFile + "': " +
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        ArgFile argFile;
        try {
            ArgFileRaw argFileRaw = readArgumentFileNode(argumentFileString);
            argFile = ArgumentsHelper.parseArgumentFile(argFileRaw, cliOptions);
        } catch (ArgFileParseException e) {
            throw new UserError("Error parsing argument file '" + argumentFile + "': " +
                    e.getMessage());
        }

        PageMetadataHandlersWrapper metadataHandlersWrapper =
                PageMetadataHandlersWrapper.fromPlugins(argFile.getPlugins());

        for (Document doc : argFile.getDocuments()) {
            try {
                Md2Html.execute(doc, argFile.getPlugins(), metadataHandlersWrapper,
                        argFile.getOptions());
            } catch(UserError e) {
                System.out.println("Error processing input file '" + doc.getInput() +
                        "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
        }

        for (Md2HtmlPlugin plugin : argFile.getPlugins()) {
            try {
                plugin.finalize(argFile.getOptions(), argFile.getPlugins());
            } catch (UserError ue) {
                throw new UserError("Error executing finalization action in plugin '" +
                        plugin.getClass().getSimpleName() + "': " + ue.getMessage());
            }
        }

        if (argFile.getOptions().isVerbose()) {
            long end = System.nanoTime();
            System.out.println("Finished in: " + formatNanoSeconds(end - start));
        }
    }

}
