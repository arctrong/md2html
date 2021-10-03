package world.md2html.options.cli;

import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CliParser {

    private static final String HELP_OPTION_NAME = "h";
    private static final String INPUT_FILE_OPTION_NAME = "i";
    private static final String ARGUMENT_FILE_OPTION_NAME = "argument-file";
    private static final String OUTPUT_FILE_OPTION_NAME = "o";
    private static final String TITLE_OPTION_NAME = "t";
    private static final String TEMPLATES_DIR_OPTION_NAME = "template";
    private static final String LINK_CSS_OPTION_NAME = "link-css";
    private static final String INCLUDE_CSS_OPTION_NAME = "include-css";
    private static final String NO_CSS_OPTION_NAME = "no-css";
    private static final String FORCE_OPTION_NAME = "f";
    private static final String VERBOSE_OPTION_NAME = "v";
    private static final String REPORT_OPTION_NAME = "r";
    private static final String LEGACY_MODE_OPTION_NAME = "legacy-mode";

    private static final int HELP_WIDTH = 80;

    private final String usage;

    public CliParser(String usage) {
        this.usage = usage;
    }

    private Options getCliOptions() {

        Options cliOptions = new Options();

        cliOptions.addOption(HELP_OPTION_NAME, "help", false, "show this help message and exit");

        cliOptions.addOption(Option.builder(INPUT_FILE_OPTION_NAME).longOpt("input").hasArg()
                .numberOfArgs(1).desc("input Markdown file name (mandatory)").build());

        cliOptions.addOption(Option.builder(null).longOpt(ARGUMENT_FILE_OPTION_NAME)
                .hasArg().numberOfArgs(1).desc("argument file").build());

        cliOptions.addOption(Option.builder(OUTPUT_FILE_OPTION_NAME).longOpt("output").hasArg()
                .numberOfArgs(1)
                .desc("output HTML file name, defaults to input file name with '.html' extension")
                .build());

        cliOptions.addOption(Option.builder(TITLE_OPTION_NAME).longOpt("title").hasArg()
                .numberOfArgs(1)
                .desc("the HTML page title, if omitted there will be an empty title").build());

        cliOptions.addOption(Option.builder(null).longOpt(TEMPLATES_DIR_OPTION_NAME).hasArg()
                .numberOfArgs(1).desc("custom template directory").build());

        cliOptions.addOption(Option.builder(null).longOpt(LINK_CSS_OPTION_NAME).hasArg()
                .numberOfArgs(1)
                .desc("links CSS file, multiple entries allowed").build());

        cliOptions.addOption(Option.builder(null).longOpt(INCLUDE_CSS_OPTION_NAME).hasArg()
                .numberOfArgs(1)
                .desc("includes content of the CSS file into HTML, multiple entries allowed")
                .build());

        cliOptions.addOption(Option.builder(null).longOpt(NO_CSS_OPTION_NAME).hasArg(false)
                .desc("creates HTML with no CSS. If no CSS-related arguments is specified, "
                        + "the default CSS will be included").build());

        cliOptions.addOption(Option.builder(FORCE_OPTION_NAME).longOpt("force").hasArg(false)
                .desc("rewrites HTML output file even if it was modified later than the input file")
                .build());

        cliOptions.addOption(Option.builder(VERBOSE_OPTION_NAME).longOpt("verbose").hasArg(false)
                .desc("outputs human readable information messages").build());

        cliOptions.addOption(Option.builder(REPORT_OPTION_NAME).longOpt("report").hasArg(false)
                .desc("if HTML file is generated, outputs the path of this file, incompatible " +
                        "with -v").build());

        cliOptions.addOption(Option.builder(null).longOpt(LEGACY_MODE_OPTION_NAME).hasArg(false)
                .desc("Allows processing documentation projects prepared for version of " +
                        "the program prior to 1.0.0. Still it's recommended to migrate the " +
                        "projects to the newer version").build());

        return cliOptions;
    }

    public ClilOptions parse(String[] args) throws CliArgumentsException {

        Options cliOptions = getCliOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(cliOptions, args);
        } catch (ParseException e) {
            throw errorAsException(cliOptions, e.getMessage());
        }

        if (commandLine.hasOption(HELP_OPTION_NAME) || args.length < 1) {
            throw helpAsException(cliOptions);
        }

        String[] restArgs = commandLine.getArgs();
        if (restArgs.length > 0) {
            throw errorAsException(cliOptions, "Positional arguments are not accepted: "
                    + Arrays.toString(restArgs));
        }

        Path argumentFile = null;
        if (commandLine.hasOption(ARGUMENT_FILE_OPTION_NAME)) {
            argumentFile = Paths.get(commandLine.getOptionValue(ARGUMENT_FILE_OPTION_NAME));
        }

        String inputFile = commandLine.getOptionValue(INPUT_FILE_OPTION_NAME);
//        if (commandLine.hasOption(INPUT_FILE_OPTION_NAME)) {
//            inputFile = Paths.get();
//        } else if (!commandLine.hasOption(ARGUMENT_FILE_OPTION_NAME)) {
//            throw errorAsException(cliOptions, "Input file is not specified");
//        }

        String outputFile = commandLine.getOptionValue(OUTPUT_FILE_OPTION_NAME);

        String title = null;
        if (commandLine.hasOption(TITLE_OPTION_NAME)) {
            title = commandLine.getOptionValue(TITLE_OPTION_NAME);
        }

        Path templateFile = null;
        if (commandLine.hasOption(TEMPLATES_DIR_OPTION_NAME)) {
            templateFile = Paths.get(commandLine.getOptionValue(TEMPLATES_DIR_OPTION_NAME));
        }

        List<String> linkCss = null;
        List<Path> includeCss = null;
        boolean noCss = commandLine.hasOption(NO_CSS_OPTION_NAME);
        if (noCss) {
            if (commandLine.hasOption(LINK_CSS_OPTION_NAME) ||
                    commandLine.hasOption(INCLUDE_CSS_OPTION_NAME)) {
                throw errorAsException(cliOptions, "Option '" + NO_CSS_OPTION_NAME +
                        "' is not compatible with options '" + LINK_CSS_OPTION_NAME + "' and '"
                        + " '" + INCLUDE_CSS_OPTION_NAME + "'");
            }
        } else {
            if (commandLine.hasOption(LINK_CSS_OPTION_NAME)) {
                linkCss = Arrays.asList(commandLine.getOptionValues(LINK_CSS_OPTION_NAME));
            }
            if (commandLine.hasOption(INCLUDE_CSS_OPTION_NAME)) {
                includeCss = Arrays.stream(commandLine.getOptionValues(INCLUDE_CSS_OPTION_NAME))
                        .map(Paths::get).collect(Collectors.toList());
            }
        }

        boolean force = commandLine.hasOption(FORCE_OPTION_NAME);
        boolean verbose = commandLine.hasOption(VERBOSE_OPTION_NAME);
        boolean report = commandLine.hasOption(REPORT_OPTION_NAME);

        if (report && verbose) {
            throw errorAsException(cliOptions,
                    "--report and --verbose arguments are not compatible");
        }

        boolean legacy_mode = commandLine.hasOption(LEGACY_MODE_OPTION_NAME);

        return new ClilOptions(argumentFile, inputFile, outputFile, title, templateFile,
                includeCss, linkCss, noCss, force, verbose, report, legacy_mode);
    }

    private CliArgumentsException helpAsException(Options cliOptions) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        HelpFormatter formatter = createHelpFormatter();
        printWriter.println();
        printUsage(printWriter, formatter, cliOptions);
        printWriter.println();
        formatter.printOptions(printWriter, formatter.getWidth(), cliOptions,
                formatter.getLeftPadding(), formatter.getDescPadding());
        printWriter.close();
        return new CliArgumentsException(null, CliArgumentsException.CliParsingExceptionType.HELP,
                stringWriter.toString());
    }

    private CliArgumentsException errorAsException(Options cliOptions, String errorMessage) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println();
        printWriter.println("ERROR: " + errorMessage);
        printWriter.println();
        HelpFormatter formatter = createHelpFormatter();
        printUsage(printWriter, formatter, cliOptions);
        return new CliArgumentsException(errorMessage,
                CliArgumentsException.CliParsingExceptionType.ERROR,
                stringWriter.toString());
    }

    private HelpFormatter createHelpFormatter() {
        HelpFormatter hf = new HelpFormatter();
        hf.setOptionComparator(null);
        hf.setWidth(HELP_WIDTH);
        return hf;
    }

    private void printUsage(PrintWriter pw, HelpFormatter helpFormatter, Options options) {
        helpFormatter.printUsage(pw, helpFormatter.getWidth(), usage, options);
    }

}
