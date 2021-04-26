package world.md2html.options;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.PrintWriter;

public class FeedbackHelper {

    private final HelpFormatter helpFormatter;
    private final String usage;
    private final Options options;
    private final String positionalArgs;

    public FeedbackHelper(String usage, Options options, String[] positionalArgs) {
        this.helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(80);
        helpFormatter.setOptionComparator(null);
        this.usage = usage;
        this.options = options;

        if (positionalArgs == null) { // no positional arguments
            this.positionalArgs = null;
        } else if (positionalArgs.length == 0) { // zero or more positional arguments
            this.positionalArgs = "[arg]...";
        } else {
            String comma = "";
            StringBuilder list = new StringBuilder();
            for (String s : positionalArgs) {
                list.append(comma).append("[").append(s).append("]");
                comma = ", ";
            }
            this.positionalArgs = list.toString();
        }
    }

    public void printHelp() {
        System.out.println();
        printUsage();
        System.out.println();
        PrintWriter pw = new PrintWriter(System.out);
        if (positionalArgs != null) {
            helpFormatter.printWrapped(pw, helpFormatter.getWidth(), "Positional arguments: " +
                    positionalArgs);
        }
        helpFormatter.printOptions(pw, helpFormatter.getWidth(), options,
                helpFormatter.getLeftPadding(), helpFormatter.getDescPadding());
        pw.flush();
    }

    public void printError(String message) {
        System.out.println();
        System.out.println("ERROR: " + message);
        System.out.println("Use -h for help.");
        System.out.println();
        printUsage();
    }

    private void printUsage() {
        PrintWriter pw = new PrintWriter(System.out);
        helpFormatter.printUsage(pw, helpFormatter.getWidth(), usage, options);
        if (positionalArgs != null) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < helpFormatter.getSyntaxPrefix().length(); ++i) {
                indent.append(" ");
            }
            String comma = options.getOptions().size() > 0 ? ", " : "";
            helpFormatter.printWrapped(pw, helpFormatter.getWidth(), indent + comma +
                    positionalArgs);
        }
        pw.flush();
    }

}
