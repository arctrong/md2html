package world.md2html;

import world.md2html.plugins.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class Constants {

    private Constants() {
    }

    public static final String EXEC_NAME = "md2html_java";
    public static final String EXEC_VERSION = "1.0.2";

    private static final String WORKING_DIR_ENV_VARIABLE_NAME = "MD2HTML_HOME";
    public static final Path WORKING_DIR;

    public static final Map<String, Supplier<Md2HtmlPlugin>> PLUGIN_PROVIDERS = new HashMap<>();

    static {
        PLUGIN_PROVIDERS.put("page-flows", PageFlowsPlugin::new);
        PLUGIN_PROVIDERS.put("relative-paths", RelativePathsPlugin::new);
        PLUGIN_PROVIDERS.put("page-variables", PageVariablesPlugin::new);
        PLUGIN_PROVIDERS.put("variables", VariablesPlugin::new);
        PLUGIN_PROVIDERS.put("index", IndexPlugin::new);
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
