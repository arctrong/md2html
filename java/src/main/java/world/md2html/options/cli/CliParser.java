package world.md2html.options.cli;

import org.apache.commons.cli.*;
import world.md2html.options.model.CliOptions;
import world.md2html.utils.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static world.md2html.utils.Utils.isNullOrEmpty;

public class CliParser {

    private static final String HELP = "h";

    private static final String INPUT_ROOT = "input-root";
    private static final String INPUT = "i";
    private static final String INPUT_GLOB = "input-glob";

    private static final String SORT_BY_FILE_PATH = "sort-by-file-path";
    private static final String SORT_BY_VARIABLE = "sort-by-variable";
    private static final String SORT_BY_TITLE = "sort-by-title";

    private static final String OUTPUT_ROOT = "output-root";
    private static final String OUTPUT = "o";

    private static final String ARGUMENT_FILE = "argument-file";

    private static final String TITLE = "t";
    private static final String TITLE_FROM_VARIABLE = "title-from-variable";

    private static final String TEMPLATE = "template";

    private static final String LINK_CSS = "link-css";
    private static final String INCLUDE_CSS = "include-css";
    private static final String NO_CSS = "no-css";

    private static final String FORCE = "f";
    private static final String VERBOSE = "v";
    private static final String REPORT = "r";
    private static final String LEGACY_MODE = "legacy-mode";

    private static final int HELP_WIDTH = 80;

    private final String usage;

    public CliParser(String usage) {
        this.usage = usage;
    }

    private Options getCliOptions() {

        Options cliOptions = new Options();

        cliOptions.addOption(HELP, "help", false, "show this help message and exit");

        cliOptions.addOption(Option.builder(null).longOpt(INPUT_ROOT)
                .hasArg().numberOfArgs(1)
                .desc("root directory for input Markdown files. Defaults to current directory")
                .build());
        cliOptions.addOption(Option.builder(INPUT).longOpt("input")
                .hasArg().numberOfArgs(1)
                .desc("input Markdown file name: absolute or relative to the '--input-root' " +
                        "argument value")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(INPUT_GLOB)
                .hasArg().numberOfArgs(1)
                .desc("input Markdown file name pattern: absolute or relative to the" +
                        "'--input-root' argument value")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(SORT_BY_FILE_PATH)
                .hasArg(false)
                .desc("If '--input-glob' is used, the documents will be sorted by the input" +
                        "file path")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(SORT_BY_VARIABLE)
                .hasArg().numberOfArgs(1)
                .desc("If '--input-glob' is used, the documents will be sorted by the value" +
                        "of the specified page variable")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(SORT_BY_TITLE)
                .hasArg(false)
                .desc("If '--input-glob' is used, the documents will be sorted by their titles")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(OUTPUT_ROOT)
                .hasArg().numberOfArgs(1)
                .desc("root directory for output HTML files. Defaults to current directory")
                .build());
        cliOptions.addOption(Option.builder(OUTPUT).longOpt("output")
                .hasArg().numberOfArgs(1)
                .desc("output HTML file name: absolute or relative to '--output-root' " +
                        "argument value. Defaults to input file name with '.html' extension")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(ARGUMENT_FILE)
                .hasArg().numberOfArgs(1)
                .desc("argument file. Allows processing multiple documents with a single run. " +
                        "Also provides different adjustment possibilities and automations. " +
                        "If omitted, the single file will be processed")
                .build());
        cliOptions.addOption(Option.builder(TITLE).longOpt("title")
                .hasArg().numberOfArgs(1)
                .desc("the HTML page title")
                .build());
        // TODO Clarify how it works if GLOBs are not used.
        cliOptions.addOption(Option.builder(null).longOpt(TITLE_FROM_VARIABLE)
                .hasArg().numberOfArgs(1)
                .desc("If specified then the program will take the title from the page metadata " +
                        "at the step of making up the input file list")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(TEMPLATE)
                .hasArg().numberOfArgs(1)
                .desc("template that will be used for HTML documents generation")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(LINK_CSS)
                .hasArg().numberOfArgs(1)
                .desc("links CSS file, multiple entries allowed")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(INCLUDE_CSS)
                .hasArg().numberOfArgs(1)
                .desc("includes content of the CSS file into HTML, multiple entries allowed")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(NO_CSS)
                .hasArg(false)
                .desc("creates HTML with no CSS. If no CSS-related arguments is specified, " +
                        "the default CSS will be included")
                .build());
        cliOptions.addOption(Option.builder(FORCE).longOpt("force")
                .hasArg(false)
                .desc("rewrites HTML output file even if it was modified later than the input file")
                .build());
        cliOptions.addOption(Option.builder(VERBOSE).longOpt("verbose")
                .hasArg(false)
                .desc("outputs human readable information messages")
                .build());
        cliOptions.addOption(Option.builder(REPORT).longOpt("report")
                .hasArg(false)
                .desc("turns on formalized output that may be further automatically processed. " +
                        "Only if HTML file is generated, the path of this file will be output." +
                        "Incompatible with -v")
                .build());
        cliOptions.addOption(Option.builder(null).longOpt(LEGACY_MODE)
                .hasArg(false)
                .desc("Allows processing documentation projects prepared for version of the " +
                        "program prior to 1.0.0. It's still recommended to migrate the " +
                        "documentation projects to the newer version")
                .build());

        return cliOptions;
    }

    public CliOptions parse(String[] args) throws CliArgumentsException {

        Options cliOptions = getCliOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(cliOptions, args);
        } catch (ParseException e) {
            throw errorAsException(cliOptions, e.getMessage());
        }

        if (commandLine.hasOption(HELP) || args.length < 1) {
            throw helpAsException(cliOptions);
        }

        String[] restArgs = commandLine.getArgs();
        if (restArgs.length > 0) {
            throw errorAsException(cliOptions, "Positional arguments are not acceptable: "
                    + Arrays.toString(restArgs));
        }

        CliOptions.CliOptionsBuilder cliOptionsBuilder = CliOptions.builder();

        String argumentFile = commandLine.getOptionValue(ARGUMENT_FILE);
        cliOptionsBuilder.argumentFile(argumentFile);

        cliOptionsBuilder.inputRoot(commandLine.getOptionValue(INPUT_ROOT));
        cliOptionsBuilder.outputRoot(commandLine.getOptionValue(OUTPUT_ROOT));

        String input = commandLine.getOptionValue(INPUT);
        cliOptionsBuilder.input(input);
        String inputGlob = commandLine.getOptionValue(INPUT_GLOB);
        cliOptionsBuilder.inputGlob(inputGlob);
        cliOptionsBuilder.output(commandLine.getOptionValue(OUTPUT));

        if (input != null && inputGlob != null) {
            throw errorAsException(cliOptions,
                    "Both input file GLOB and input file name are defined");
        }
        if (argumentFile == null && input == null && inputGlob == null) {
            throw errorAsException(cliOptions,
                    "None of the input file name or input file GLOB is specified");
        }

        String sortByVariable = commandLine.getOptionValue(SORT_BY_VARIABLE);
        boolean sortByFilePath = commandLine.hasOption(SORT_BY_FILE_PATH);
        boolean sortByTitle = commandLine.hasOption(SORT_BY_TITLE);

        int sort_count = (isNullOrEmpty(sortByVariable) ? 0 : 1) +
                (sortByFilePath ? 1 : 0) + (sortByTitle ? 1 : 0);
        if (sort_count > 1) {
            throw errorAsException(cliOptions,
                    "The options --sort-by-file-path, --sort-by-variable and --sort-by-title " +
                            "are not compatible");
        }
        cliOptionsBuilder.sortByVariable(sortByVariable);
        cliOptionsBuilder.sortByFilePath(sortByFilePath);
        cliOptionsBuilder.sortByTitle(sortByTitle);

        cliOptionsBuilder.title(commandLine.getOptionValue(TITLE));
        cliOptionsBuilder.titleFromVariable(commandLine.getOptionValue(TITLE_FROM_VARIABLE));

        cliOptionsBuilder.template(commandLine.getOptionValue(TEMPLATE));

        List<String> linkCss = commandLine.hasOption(LINK_CSS) ?
                Arrays.asList(commandLine.getOptionValues(LINK_CSS)) : null;
        List<String> includeCss = commandLine.hasOption(INCLUDE_CSS) ?
                Arrays.asList(commandLine.getOptionValues(INCLUDE_CSS)) : null;
        boolean noCss = commandLine.hasOption(NO_CSS);
        if (noCss && (linkCss != null || includeCss != null)) {
            throw errorAsException(cliOptions, "Option '" + NO_CSS +
                    "' is not compatible with options '" + LINK_CSS + "' and '" + " '" +
                    INCLUDE_CSS + "'");
        }
        cliOptionsBuilder.linkCss(linkCss);
        cliOptionsBuilder.includeCss(includeCss);
        cliOptionsBuilder.noCss(noCss);

        cliOptionsBuilder.force(commandLine.hasOption(FORCE));
        boolean verbose = commandLine.hasOption(VERBOSE);
        boolean report = commandLine.hasOption(REPORT);
        if (report && verbose) {
            throw errorAsException(cliOptions,
                    "--report and --verbose arguments are not compatible");
        }
        cliOptionsBuilder.verbose(verbose);
        cliOptionsBuilder.report(report);

        cliOptionsBuilder.legacyMode(commandLine.hasOption(LEGACY_MODE));

        return cliOptionsBuilder.build();
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
