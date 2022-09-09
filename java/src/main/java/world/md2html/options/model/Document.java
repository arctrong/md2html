package world.md2html.options.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class Document {

    String input;
    String output;
    String title;
    String template;
    List<String> includeCss;
    List<String> linkCss;
    boolean noCss;
    boolean force;
    boolean verbose;
    boolean report;

}
