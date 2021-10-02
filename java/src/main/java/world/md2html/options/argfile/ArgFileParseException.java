package world.md2html.options.argfile;

public class ArgFileParseException extends Exception {

    public ArgFileParseException() {
    }

    public ArgFileParseException(String message) {
        super(message);
    }

    public ArgFileParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArgFileParseException(Throwable cause) {
        super(cause);
    }

    public ArgFileParseException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
