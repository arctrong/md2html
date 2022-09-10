package world.md2html.options.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

// TODO Move to package `world.md2html.options.cli`.

@Value
@Builder()
public class CliOptions {

    String argumentFile;
    String inputRoot;
    String outputRoot;
    String input;
    String inputGlob;

    String sortByVariable;
    boolean sortByFilePath;
    boolean sortByTitle;

    String output;

    String title;
    String titleFromVariable;

    String template;

    List<String> includeCss;
    List<String> linkCss;
    boolean noCss;

    boolean force;
    boolean verbose;
    boolean report;
    boolean legacyMode;

}
