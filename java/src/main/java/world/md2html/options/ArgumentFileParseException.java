package world.md2html.options;

public class ArgumentFileParseException extends Exception {

    public ArgumentFileParseException() {
    }

    public ArgumentFileParseException(String message) {
        super(message);
    }

    public ArgumentFileParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArgumentFileParseException(Throwable cause) {
        super(cause);
    }

    public ArgumentFileParseException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
