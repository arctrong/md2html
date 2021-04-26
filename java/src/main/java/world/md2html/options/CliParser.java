package world.md2html.options;

import org.apache.commons.cli.*;
import world.md2html.Md2Html;
import world.md2html.Md2HtmlWorkingDirHolder;
import world.md2html.utils.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CliParser {

    private static final String HELP_OPTION_NAME = "h";
    private static final String INPUT_FILE_OPTION_NAME = "i";
    private static final String OUTPUT_FILE_OPTION_NAME = "o";
    private static final String TITLE_OPTION_NAME = "t";
    private static final String TEMPLATES_DIR_OPTION_NAME = "templates";
    private static final String LINK_CSS_OPTION_NAME = "l";
    private static final String FORCE_OPTION_NAME = "f";
    private static final String VERBOSE_OPTION_NAME = "v";
    private static final String REPORT_OPTION_NAME = "r";

    private static final String DEFAULT_TEMPLATE_DIR = "templates";

    private final Options cliOptions;
    private final FeedbackHelper feedbackHelper;

    public CliParser() {
        String usage = "java " + Md2Html.class.getSimpleName();
        cliOptions = createCliOptions();
        feedbackHelper = new FeedbackHelper(usage, cliOptions, new String[]{"input", "output",
                "title"});
    }

    public CliParsingResult getMd2HtmlOptions(String[] args)
            throws ParseException, CliArgumentsException {

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(cliOptions, args);

        if (commandLine.hasOption(HELP_OPTION_NAME) || args.length < 1) {
            return new CliParsingResult(CliParsingResultType.HELP, null);
        }

        String[] restArgs = commandLine.getArgs();
        int restArgsCount = restArgs.length;

        if (restArgsCount != 0 && restArgsCount != 3) {
            throw new CliArgumentsException("Positional argument count must be exactly 0 or 3. " +
                    "Actual: " + restArgsCount);
        }

        if (restArgsCount > 0 && (commandLine.hasOption(INPUT_FILE_OPTION_NAME) ||
                commandLine.hasOption(OUTPUT_FILE_OPTION_NAME) ||
                commandLine.hasOption(TITLE_OPTION_NAME))) {
            throw new CliArgumentsException("Incompatible positional and named arguments");
        }

        Path inputFile;
        if (commandLine.hasOption(INPUT_FILE_OPTION_NAME)) {
            inputFile = Paths.get(commandLine.getOptionValue(INPUT_FILE_OPTION_NAME));
        } else if (restArgsCount > 0) {
            inputFile = Paths.get(restArgs[0]);
        } else {
            throw new CliArgumentsException("Input file is not specified");
        }

        Path outputFile;
        if (commandLine.hasOption(OUTPUT_FILE_OPTION_NAME)) {
            outputFile = Paths.get(commandLine.getOptionValue(
                    OUTPUT_FILE_OPTION_NAME));
        } else if (restArgsCount > 0) {
            outputFile = Paths.get(restArgs[1]);
        } else {
            outputFile = Paths.get(Utils.stripExtension(inputFile.toString()) + ".html");
        }

        String title;
        if (commandLine.hasOption(TITLE_OPTION_NAME)) {
            title = commandLine.getOptionValue(TITLE_OPTION_NAME);
        } else if (restArgsCount > 0) {
            title = restArgs[2];
        } else {
            title = "";
        }

        Path templateDir;
        if (commandLine.hasOption(TEMPLATES_DIR_OPTION_NAME)) {
            templateDir = Paths.get(commandLine.getOptionValue(
                    TEMPLATES_DIR_OPTION_NAME));
        } else {
            templateDir = Md2HtmlWorkingDirHolder.WORKING_DIR.resolve(DEFAULT_TEMPLATE_DIR);
        }

        String linkCss = "";
        if (commandLine.hasOption(LINK_CSS_OPTION_NAME)) {
            linkCss = commandLine.getOptionValue(LINK_CSS_OPTION_NAME);
        }

        boolean force = commandLine.hasOption(FORCE_OPTION_NAME);
        boolean verbose = commandLine.hasOption(VERBOSE_OPTION_NAME);
        boolean report = commandLine.hasOption(REPORT_OPTION_NAME);

        if (report && verbose) {
            throw new CliArgumentsException("--report and --verbose arguments are not compatible");
        }

        return new CliParsingResult(CliParsingResultType.SUCCESS,
                new Md2HtmlOptions(inputFile, outputFile, title, templateDir, linkCss, force,
                        verbose, report));
    }

    private Options createCliOptions() {

        Options cliOptions = new Options();

        cliOptions.addOption(HELP_OPTION_NAME, "help", false, "show this help message and exit");

        cliOptions.addOption(Option.builder(INPUT_FILE_OPTION_NAME).longOpt("input").hasArg()
                .numberOfArgs(1).desc("input Markdown file name (mandatory)").build());

        cliOptions.addOption(Option.builder(OUTPUT_FILE_OPTION_NAME).longOpt("output").hasArg()
                .numberOfArgs(1)
                .desc("output HTML file name, defaults to input file name with '.html' extension")
                .build());

        cliOptions.addOption(Option.builder(TITLE_OPTION_NAME).longOpt("title").hasArg()
                .numberOfArgs(1)
                .desc("the HTML page title, if omitted there will be an empty title").build());

        cliOptions.addOption(Option.builder(null).longOpt(TEMPLATES_DIR_OPTION_NAME).hasArg()
                .numberOfArgs(1).desc("custom template directory").build());

        cliOptions.addOption(Option.builder(LINK_CSS_OPTION_NAME).longOpt("link-css").hasArg()
                .numberOfArgs(1)
                .desc("links CSS file, if omitted includes the default CSS into HTML").build());

        cliOptions.addOption(Option.builder(FORCE_OPTION_NAME).longOpt("force").hasArg(false)
                .desc("rewrites HTML output file even if it was modified later than the input file")
                .build());

        cliOptions.addOption(Option.builder(VERBOSE_OPTION_NAME).longOpt("verbose").hasArg(false)
                .desc("outputs human readable information messages").build());

        cliOptions.addOption(Option.builder(REPORT_OPTION_NAME).longOpt("report").hasArg(false)
                .desc("if HTML file is generated, outputs the path of this file, incompatible " +
                        "with -v").build());

        return cliOptions;
    }

    public FeedbackHelper getFeedbackHelper() {
        return feedbackHelper;
    }

}
