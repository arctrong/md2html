package world.md2html;

import world.md2html.options.argfile.ArgFileOptions;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.argfile.ArgFileParser;
import world.md2html.options.cli.CliArgumentsException;
import world.md2html.options.cli.CliParser;
import world.md2html.options.cli.ClilOptions;
import world.md2html.options.model.Document;
import world.md2html.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

public class Md2HtmlRunner {

    public static void main(String[] args) throws Exception {

        long start = System.nanoTime();

        String usage = "java " + Md2Html.class.getSimpleName();
        ClilOptions clilOptions = null;
        try {
            clilOptions = new CliParser(usage).parse(args);
        } catch (CliArgumentsException e) {
            System.out.println(e.getPrintText());
            System.exit(1);
        }

        ArgFileOptions argFileOptions = null;
        Path argumentFile = clilOptions.getArgumentFile();
        if (argumentFile != null) {
            try {
                String argumentFileString = Utils.readStringFromCommentedFile(argumentFile, "#",
                        StandardCharsets.UTF_8);
                argFileOptions = ArgFileParser.parse(argumentFileString, clilOptions);
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
            argFileOptions = ArgFileParser.parse("{\"documents\": [{}]}", clilOptions);
        }

        for (Document doc : argFileOptions.getDocuments()) {
            Md2Html.execute(doc, argFileOptions.getPlugins());
        }

        long end = System.nanoTime();
        System.out.println("Finished in: " + Utils.formatNanoSeconds(end - start));



    }

}
