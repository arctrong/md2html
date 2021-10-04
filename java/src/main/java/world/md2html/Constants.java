package world.md2html;

import world.md2html.plugins.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class Constants {

    private Constants() {
    }

    public static final String EXEC_NAME = "md2html_java";
    public static final String EXEC_VERSION = "1.0.0";

    private static final String WORKING_DIR_ENV_VARIABLE_NAME = "MD2HTML_HOME";
    public static final Path WORKING_DIR;

    public static final Map<String, Md2HtmlPlugin> PLUGINS = new HashMap<>();

    static {
        PLUGINS.put("page-flows", new PageFlowsPlugin());
        PLUGINS.put("relative-paths", new RelativePathsPlugin());
        PLUGINS.put("page-variables", new PageVariablesPlugin());
        PLUGINS.put("variables", new VariablesPlugin());
    }

    static {
        String workingDirStr = System.getenv(Constants.WORKING_DIR_ENV_VARIABLE_NAME);
        if (workingDirStr == null) {
            throw new RuntimeException("Environment variable is not set: " +
                    Constants.WORKING_DIR_ENV_VARIABLE_NAME);
        }
        WORKING_DIR = Paths.get(workingDirStr);
    }

}
