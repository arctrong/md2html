package world.md2html;

import world.md2html.buildcache.BuildCacheManager;
import world.md2html.plugins.IgnorePlugin;
import world.md2html.plugins.IncludeFilePlugin;
import world.md2html.plugins.IndexPlugin;
import world.md2html.plugins.Md2HtmlPlugin;
import world.md2html.plugins.PageFlowsPlugin;
import world.md2html.plugins.PageLinksPlugin;
import world.md2html.plugins.PageVariablesPlugin;
import world.md2html.plugins.RelativePathsPlugin;
import world.md2html.plugins.ReplacePlugin;
import world.md2html.plugins.VariablesPlugin;
import world.md2html.plugins.WrapCodePlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class Constants {

    private Constants() {
    }

    public static final String EXEC_NAME = "md2html_java";
    public static final String EXEC_VERSION = "1.0.8";

    private static final String WORKING_DIR_ENV_VARIABLE_NAME = "MD2HTML_HOME";
    public static final Path WORKING_DIR;

    public static final Map<String, Function<BuildCacheManager, Md2HtmlPlugin>> PLUGIN_PROVIDERS =
            new HashMap<>();

    static {
        PLUGIN_PROVIDERS.put("page-flows", mgr -> new PageFlowsPlugin());
        PLUGIN_PROVIDERS.put("relative-paths", mgr -> new RelativePathsPlugin());
        PLUGIN_PROVIDERS.put("page-variables", mgr -> new PageVariablesPlugin());
        PLUGIN_PROVIDERS.put("variables", mgr -> new VariablesPlugin());
        PLUGIN_PROVIDERS.put("index", IndexPlugin::new);
        PLUGIN_PROVIDERS.put("page-links", mgr -> new PageLinksPlugin());
        PLUGIN_PROVIDERS.put("ignore", mgr -> new IgnorePlugin());
        PLUGIN_PROVIDERS.put("wrap-code", WrapCodePlugin::new);
        PLUGIN_PROVIDERS.put("include-file", mgr -> new IncludeFilePlugin());
        PLUGIN_PROVIDERS.put("replace", mgr -> new ReplacePlugin());
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
