package world.md2html;

import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageFlowsPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String EXEC_NAME = "md2html_java";
    public static final String EXEC_VERSION = "0.1.3";

    private static final String WORKING_DIR_ENV_VARIABLE_NAME = "MD2HTML_HOME";
    public static final Path WORKING_DIR;

    public static final Map<String, Md2HtmlPlugin> PLUGINS = new HashMap<>();

    static {
        PLUGINS.put("page-flows", new PageFlowsPlugin());

//        {'relative-paths': RelativePathsPlugin(),
//                'page-variables': PageVariablesPlugin(), "variables": VariablesPlugin()}


    }

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
