package world.md2html.options.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
public class Document {

    String input;
    String output;
    String title;
    String code;
    String template;
    List<String> includeCss;
    List<String> linkCss;
    boolean noCss;
    boolean force;
    boolean verbose;

}
