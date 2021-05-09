package world.md2html;

import org.apache.commons.cli.ParseException;
import world.md2html.options.CliArgumentsException;
import world.md2html.options.CliParserHelper;
import world.md2html.options.Md2HtmlOptions;

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
        Md2Html.execute(md2HtmlOptions);
    }

}
