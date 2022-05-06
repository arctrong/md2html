package world.md2html.options.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class CliOptions {

    private Path argumentFile;
    private String inputRoot;
    private String outputRoot;
    private String inputFile;
    private String outputFile;
    private String title;
    private Path template;
    private List<Path> includeCss;
    private List<String> linkCss;
    private boolean noCss;
    private boolean force;
    private boolean verbose;
    private boolean report;
    private boolean legacyMode;

}
