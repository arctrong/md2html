package world.md2html.options;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class FeedbackHelper {

    private final HelpFormatter helpFormatter;
    private final String usage;
    private final Options options;

    public FeedbackHelper(String usage, Options options) {
        this.helpFormatter = new HelpFormatter();
        this.usage = usage;
        this.options = options;
    }

    public void printHelp() {
        helpFormatter.printHelp(usage, options, true);
    }

    public void printError(String message) {
        System.out.println();
        System.out.println("ERROR: " + message);
        System.out.println("Use -h for help.");
        System.out.println();

        helpFormatter.printHelp(usage, options, true);
    }

}
