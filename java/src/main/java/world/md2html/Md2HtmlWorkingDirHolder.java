package world.md2html;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Md2HtmlWorkingDirHolder {

    private static final String WORKING_DIR_ENV_VARIABLE_NAME = "MD2HTML_PY_HOME";
    public static final Path WORKING_DIR;
    static {
        String workingDirStr = System.getenv(Md2HtmlWorkingDirHolder.WORKING_DIR_ENV_VARIABLE_NAME);
        if (workingDirStr == null) {
            throw new RuntimeException("Environment variable is not set: " +
                    Md2HtmlWorkingDirHolder.WORKING_DIR_ENV_VARIABLE_NAME);
        }
        WORKING_DIR = Paths.get(workingDirStr);
    }

    private Md2HtmlWorkingDirHolder() {
    }

}
