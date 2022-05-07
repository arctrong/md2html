package world.md2html.options.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Document {

    private String inputLocation;
    private String outputLocation;
    private String title;
    private Path template;
    private List<Path> includeCss;
    private List<String> linkCss;
    private boolean noCss;
    private boolean force;
    private boolean verbose;
    private boolean report;

}
