package world.md2html;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {

    public static final String EXEC_NAME = "md2html_java";
    public static final String EXEC_VERSION = "0.1.2";

    private static final String WORKING_DIR_ENV_VARIABLE_NAME = "MD2HTML_HOME";
    public static final Path WORKING_DIR;
    static {
        String workingDirStr = System.getenv(Constants.WORKING_DIR_ENV_VARIABLE_NAME);
        if (workingDirStr == null) {
            throw new RuntimeException("Environment variable is not set: " +
                    Constants.WORKING_DIR_ENV_VARIABLE_NAME);
        }
        WORKING_DIR = Paths.get(workingDirStr);
    }

    private Constants() {
    }

}
