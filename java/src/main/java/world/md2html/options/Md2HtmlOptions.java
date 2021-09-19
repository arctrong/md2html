package world.md2html.options;

import java.nio.file.Path;
import java.util.List;

public class Md2HtmlOptions {

    private final Path argumentFile;
    private final Path inputFile;
    private final Path outputFile;
    private final String title;
    private final Path template;
    private final List<Path> includeCss;
    private final List<String> linkCss;
    private final boolean noCss;
    private final boolean force;
    private final boolean verbose;
    private final boolean report;

    public Md2HtmlOptions(Path argumentFile, Path inputFile, Path outputFile, String title,
            Path template, List<Path> includeCss, List<String> linkCss, boolean noCss,
            boolean force, boolean verbose, boolean report) {

        this.argumentFile = argumentFile;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.title = title;
        this.template = template;
        this.includeCss = includeCss;
        this.linkCss = linkCss;
        this.noCss = noCss;
        this.force = force;
        this.verbose = verbose;
        this.report = report;
    }

    public Path getArgumentFile() {
        return argumentFile;
    }

    public Path getInputFile() {
        return inputFile;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public String getTitle() {
        return title;
    }

    public Path getTemplate() {
        return template;
    }

    public List<String> getLinkCss() {
        return linkCss;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isReport() {
        return report;
    }

    public List<Path> getIncludeCss() {
        return includeCss;
    }

    public boolean isNoCss() {
        return noCss;
    }

}
