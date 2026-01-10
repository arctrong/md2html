package world.md2html.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MustacheUtils {

    private static final Map<Path, Mustache> CACHED_MUSTACHE_RENDERERS = new HashMap<>();
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    public static Mustache createCachedMustacheRenderer(Path templateFile) throws IOException {
        Mustache result = CACHED_MUSTACHE_RENDERERS.get(templateFile);
        if (result == null) {
            try (Reader reader = new BufferedReader(new InputStreamReader(
                    Files.newInputStream(templateFile), StandardCharsets.UTF_8))) {
                result = MUSTACHE_FACTORY.compile(reader, templateFile.toString());
                CACHED_MUSTACHE_RENDERERS.put(templateFile, result);
            }
        }
        return result;
    }
}
