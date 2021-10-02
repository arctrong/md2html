package world.md2html;

/**
 * Error that was caused by correct processing of incorrect user input or actions.
 * The message must be more user friendly, and the application output may be simpler, e.g.
 * without a stack trace.
 */
public class UserError extends RuntimeException {
    public UserError() {
    }

    public UserError(String message) {
        super(message);
    }

    public UserError(String message, Throwable cause) {
        super(message, cause);
    }

    public UserError(Throwable cause) {
        super(cause);
    }

    public UserError(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
