package world.md2html.options.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@AllArgsConstructor
@Getter
public class CliOptions {

    private final Path argumentFile;
    private final String inputFile;
    private final String outputFile;
    private final String title;
    private final Path template;
    private final List<Path> includeCss;
    private final List<String> linkCss;
    private final boolean noCss;
    private final boolean force;
    private final boolean verbose;
    private final boolean report;
    private final boolean legacyMode;

}
