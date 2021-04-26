package world.md2html.options;

public class CliParsingResult {

    private final CliParsingResultType resultType;
    private final Md2HtmlOptions options;

    public CliParsingResult(CliParsingResultType resultType, Md2HtmlOptions options) {
        this.resultType = resultType;
        this.options = options;
    }

    public CliParsingResultType getResultType() {
        return resultType;
    }

    public Md2HtmlOptions getOptions() {
        return options;
    }

}
