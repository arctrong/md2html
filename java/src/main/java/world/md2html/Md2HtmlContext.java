package world.md2html;

import lombok.experimental.UtilityClass;
import world.md2html.buildcache.BuildCacheManager;

@UtilityClass
public class Md2HtmlContext {

    // TODO Consider moving the collection of the initialized plugins here.

    private static final BuildCacheManager BUILD_CACHE_MANAGER = new BuildCacheManager();

    public static BuildCacheManager getBuildCacheManager() {
        return BUILD_CACHE_MANAGER;
    }
}
