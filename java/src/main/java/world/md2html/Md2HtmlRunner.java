package world.md2html;

import world.md2html.options.*;
import world.md2html.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class Md2HtmlRunner {

    public static void main(String[] args) throws Exception {
        String usage = "java " + Md2Html.class.getSimpleName();
        CliParserHelper cliParserHelper = new CliParserHelper(usage);
        Md2HtmlOptions md2HtmlOptions = null;
        try {
            md2HtmlOptions = cliParserHelper.parse(args);
        } catch (CliArgumentsException e) {
            System.out.println(e.getPrintText());
            System.exit(1);
        }

        List<Md2HtmlOptions> md2HtmlOptionsList;
        Path argumentFile = md2HtmlOptions.getArgumentFile();
        if (argumentFile != null) {
            String argumentFileString = Utils.readStringFromCommentedFile(argumentFile, "#",
                    StandardCharsets.UTF_8);
            md2HtmlOptionsList = ArgumentFileParser.parse(argumentFileString, md2HtmlOptions);
        } else {
            md2HtmlOptionsList = Collections.singletonList(md2HtmlOptions);
        }

        md2HtmlOptionsList =
                Md2HtmlOptionUtils.enrichDocumentMd2HtmlOptionsList(md2HtmlOptionsList);

        for (Md2HtmlOptions opt : md2HtmlOptionsList) {
            Md2Html.execute(opt);
        }
    }

}
