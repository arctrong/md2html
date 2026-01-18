package world.md2html.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This customised logging solution addresses the following requirements:
 * <ul>
 * <li> The `verbose` flag is known only after config parsing
 * <li> We need zero output before that
 * <li> we want STDOUT and message-only output
 * </ul>
 * Alternatives considered:
 * <ul>
 * <li> SLF4J + Logback - adds ~700 MB to the JAR, unnecessary production-ready features
 *     that are not required for a simple CLI app
 * <li> `org.slf4j:slf4j-simple` - too simple and unfriendly - STDERR only, no message-only
 *     support
 * <li> Custom logging solution or SLF4J facade implementation - probably worthless
 * </ul>
 */
public final class Logging {

    private static final String APP_LOGGER = "world.md2html";

    private Logging() {}

    public static Logger getLogger() {
        return Logger.getLogger(APP_LOGGER);
    }

    public static void init(boolean verbose) {
        try {
            LogManager manager = LogManager.getLogManager();
            Logger root = manager.getLogger("");

            // Disable EVERYTHING by default
            root.setLevel(Level.OFF);
            for (Handler h : root.getHandlers()) {
                root.removeHandler(h);
            }

            // If not verbose, done (total silence)
            if (!verbose) {
                return;
            }

            // Create STDOUT handler
            ConsoleHandler handler = new ConsoleHandler() {
                {
                    setOutputStream(System.out);
                }
            };
            handler.setLevel(Level.ALL);
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getMessage() + System.lineSeparator();
                }
            });

            // Configure application logger
            Logger app = Logger.getLogger(APP_LOGGER);
            app.setUseParentHandlers(false);
            app.setLevel(Level.ALL);
            app.addHandler(handler);

        } catch (Exception e) {
            // Logging must never break the app
            e.printStackTrace();
        }
    }
}
