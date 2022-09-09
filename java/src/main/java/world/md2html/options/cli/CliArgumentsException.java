package world.md2html.options.cli;

// TODO Consider extending `UserError`.
public class CliArgumentsException extends Exception {

    // TODO Consider removing this information (like in the Python version).
    public enum CliParsingExceptionType {
        /** Parsing error occurred. */
        ERROR,
        /** Help was requested. */
        HELP
    }

    private final CliParsingExceptionType exceptionType;
    private final String printText;

    public CliArgumentsException(String message, CliParsingExceptionType exceptionType,
            String printText) {
        super(message);
        this.exceptionType = exceptionType;
        this.printText = printText;
    }

    public CliParsingExceptionType getExceptionType() {
        return exceptionType;
    }

    public String getPrintText() {
        return printText;
    }

}
