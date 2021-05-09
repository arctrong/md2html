package world.md2html.options;

import java.nio.file.Path;

public class Md2HtmlOptions {

    private final Path inputFile;
    private final Path outputFile;
    private final String title;
    private final Path templateDir;
    private final Path includeCss;
    private final String linkCss;
    private final boolean force;
    private final boolean verbose;
    private final boolean report;

    public Md2HtmlOptions(Path inputFile, Path outputFile, String title, Path templateDir,
            Path includeCss, String linkCss, boolean force, boolean verbose, boolean report) {

        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.title = title;
        this.templateDir = templateDir;
        this.includeCss = includeCss;
        this.linkCss = linkCss;
        this.force = force;
        this.verbose = verbose;
        this.report = report;
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

    public Path getTemplateDir() {
        return templateDir;
    }

    public String getLinkCss() {
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

    public Path getIncludeCss() {
        return includeCss;
    }

}
