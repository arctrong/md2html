package world.md2html.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MustacheUtils {

    private static final Map<Path, Mustache> CACHED_MUSTACHE_RENDERERS = new HashMap<>();
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();
    private static final Pattern LEGACY_PLACEHOLDERS_REPLACEMENT_PATTERN =
            Pattern.compile("(^|[^$])\\$\\{([^}]+)\\}");
    private static final Pattern LEGACY_PLACEHOLDERS_UNESCAPED_REPLACEMENT_PATTERN =
            Pattern.compile("(^|[^$])\\$\\{(styles|content)\\}");

    public static Mustache createCachedMustacheRenderer(Path templateFile) throws IOException {
        Mustache result = CACHED_MUSTACHE_RENDERERS.get(templateFile);
        if (result == null) {
            try (Reader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(templateFile.toFile()), StandardCharsets.UTF_8))) {
                result = MUSTACHE_FACTORY.compile(reader, templateFile.toString());
                CACHED_MUSTACHE_RENDERERS.put(templateFile, result);
            }
        }
        return result;
    }

    public static Mustache createCachedMustacheRendererLegacy(Path templateFile)
            throws IOException {
        Mustache result = CACHED_MUSTACHE_RENDERERS.get(templateFile);
        if (result == null) {
            String template = Utils.readStringFromUtf8File(templateFile);
            Matcher matcher = LEGACY_PLACEHOLDERS_UNESCAPED_REPLACEMENT_PATTERN.matcher(template);
            template = matcher.replaceAll("$1{{{$2}}}");
            matcher = LEGACY_PLACEHOLDERS_REPLACEMENT_PATTERN.matcher(template);
            template = matcher.replaceAll("$1{{$2}}");
            Reader reader = new StringReader(template);
            result = MUSTACHE_FACTORY.compile(reader, templateFile.toString());
            CACHED_MUSTACHE_RENDERERS.put(templateFile, result);
        }
        return result;
    }
}
