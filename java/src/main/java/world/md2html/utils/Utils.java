package world.md2html.utils;

import com.eclipsesource.json.JsonObject;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import world.md2html.UserError;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    private static final Map<Path, String> CACHED_FILES = new HashMap<>();
    private static final Map<Path, Mustache> CACHED_MUSTACHE_RENDERERS = new HashMap<>();
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    private Utils() {
    }

    public static String stripExtension(String path) {
        int dotPos = path.lastIndexOf(".");
        int slashPos = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        if (dotPos - slashPos > 1) {
            return path.substring(0, dotPos);
        } else {
            return path;
        }
    }

    public static String readStringFromUtf8File(Path filePath) throws IOException {
        return readStringFromFile(filePath, StandardCharsets.UTF_8);
    }

    public static String readStringFromFile(Path filePath, Charset charset) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            char[] buffer = new char[8192];
            int readCount;
            while ((readCount = reader.read(buffer)) > -1) {
                result.append(buffer, 0, readCount);
            }
        }
        return result.toString();
    }

    public static String readStringFromCachedUtf8File(Path filePath) {
        return CACHED_FILES.computeIfAbsent(filePath, path -> {
            try {
                return readStringFromUtf8File(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String readStringFromCommentedFile(Path filePath, String commentChar,
            Charset charset) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith(commentChar)) {
                    // When ignoring a line, leaving an empty line instead. Then, when a
                    // parser points at an error, this error will be found at the pointed
                    // line in the initial (commented) file.
                    result.append("\n");
                } else {
                    result.append(line).append("\n");
                }
            }
        }
        return result.toString();
    }

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean jsonObjectContains(JsonObject jsonObject, String memberName) {
        return jsonObject.get(memberName) != null;
    }

    public static boolean jsonObjectContainsAny(JsonObject jsonObject, String... memberNames) {
        for (String mn : memberNames) {
            if (jsonObject.get(mn) != null) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    public static <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static Mustache createCachedMustacheRenderer(Path templateFile) {
        return CACHED_MUSTACHE_RENDERERS.computeIfAbsent(templateFile, path -> {
            try (Reader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(path.toFile()), StandardCharsets.UTF_8));
            ) {
                return MUSTACHE_FACTORY.compile(reader, templateFile.toString());
            } catch (IOException e) {
                throw new UserError(String.format("Error reading template file '\\s': \\s",
                        templateFile.toString(), e.getMessage()), e);
            }
        });
    }

    public static String formatNanoSeconds(long duration) {
        long quotient = duration / 1_000_000;
        long remainder = quotient % 1_000;
        String result = String.format(".%03d", remainder);
        quotient = quotient / 1_000;
        remainder = quotient % 60;
        result = String.format(":%02d", remainder) + result;
        quotient = quotient / 60;
        remainder = quotient % 60;
        result = String.format(":%02d", remainder) + result;
        quotient = quotient / 60;
        remainder = quotient % 24;
        result = String.format(" %02d", remainder) + result;
        quotient = quotient / 24;
        result = String.format("%d", quotient) + result;
        return result;
    }

}
