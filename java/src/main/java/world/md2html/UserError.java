package world.md2html;

/**
 * Error that was caused by correct processing of incorrect user input or actions.
 * The message must be more user friendly, and the application output may be simpler, e.g.
 * without a stack trace.
 */
public class UserError extends RuntimeException {

    public UserError(String message) {
        super(message);
    }

}
