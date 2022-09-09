package world.md2html.options.model.raw;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ArgFileDocumentRaw.ArgFileDocumentRawBuilder.class)
public class ArgFileDocumentRaw {
    String template;
    @JsonProperty("input-root")
    String inputRoot;
    String input;
    @JsonProperty("input-glob")
    String inputGlob;
    @JsonProperty("sort-by-file-path")
    boolean sortByFilePath;
    @JsonProperty("sort-by-variable")
    String sortByVariable;
    @JsonProperty("sort-by-title")
    boolean sortByTitle;
    @JsonProperty("output-root")
    String outputRoot;
    String output;
    String title;
    @JsonProperty("title-from-variable")
    String titleFromVariable;
    @JsonProperty("no-css")
    boolean noCss;
    @JsonProperty("link-css")
    List<String> linkCss;
    @JsonProperty("include-css")
    List<String> includeCss;
    @JsonProperty("page-flows")
    List<String> pageFlows;
    @JsonProperty("add-link-css")
    List<String> addLinkCss;
    @JsonProperty("add-include-css")
    List<String> addIncludeCss;
    @JsonProperty("add-page-flows")
    List<String> addPageFlows;
    boolean verbose;
    boolean force;
    boolean report;
    // TODO Check how `@JsonIgnore` annotation works.
    @JsonIgnore
    String techSortBy;
}
