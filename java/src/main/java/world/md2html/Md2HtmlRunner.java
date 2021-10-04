package world.md2html;

import world.md2html.options.model.ArgFileOptions;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.cli.CliArgumentsException;
import world.md2html.options.model.CliOptions;
import world.md2html.options.cli.CliParser;
import world.md2html.options.model.Document;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper;
import world.md2html.utils.UserError;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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

        ArgFileOptions argFileOptions = null;
        Path argumentFile = cliOptions.getArgumentFile();
        if (argumentFile != null) {
            try {
                String argumentFileString = Utils.readStringFromCommentedFile(argumentFile, "#",
                        StandardCharsets.UTF_8);
                argFileOptions = ArgFileParser.parse(argumentFileString, cliOptions);
            } catch (IOException e) {
                System.out.println("Error parsing argument file '" + argumentFile +
                        "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            } catch (ArgFileParseException e) {
                System.out.println("Error parsing argument file '" + argumentFile + "': " +
                        e.getMessage());
                System.exit(1);
            }
        } else {
            // When run without argument file, need implicitly added
            // plugin for page title extraction from the source text.
            String fakeArgFile = "{\"documents\": [{}], \"plugins\": {\"page-variables\": " +
                    "{\"VARIABLES\": {\"only-at-page-start\": true}";
            if (cliOptions.isLegacyMode()) {
                fakeArgFile += ", \"METADATA\": {\"only-at-page-start\": true}";
            }
            fakeArgFile += "}}}}";
            argFileOptions = ArgFileParser.parse(fakeArgFile, cliOptions);
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

        long end = System.nanoTime();
        System.out.println("Finished in: " + Utils.formatNanoSeconds(end - start));
    }

}
