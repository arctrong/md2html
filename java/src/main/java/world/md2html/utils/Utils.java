package world.md2html.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static boolean isNullOrFalse(Object object) {
        return object instanceof Boolean && (Boolean) object;
    }

    /**
     * This function converts the given `JsonNode` into native Java representation. To reflect
     * JSON types and structures it uses certain Java types that are suitable in context of this
     * program. May be not suitable in other contexts.
     */
    public static Object deJson(JsonNode value) {
        switch (value.getNodeType()) {
            case ARRAY:
                List<Object> list = new ArrayList<>();
                value.forEach(item -> list.add(deJson(item)));
                return list;
            case NULL:
            case BINARY:
            case MISSING:
            case POJO:
                return null;
            case BOOLEAN:
                return value.asBoolean();
            case NUMBER:
                if (value.canConvertToExactIntegral()) {
                    return value.asInt();
                } else {
                    return value.asDouble();
                }
            case OBJECT:
                Map<String, Object> map = new HashMap<>();
                value.fields().forEachRemaining(entry -> map.put(entry.getKey(),
                        deJson(entry.getValue())));
                return map;
            case STRING:
                return value.asText();
        }
        return null;
    }

    public static class ResourceLocationException extends Exception {
        public ResourceLocationException(String message) {
            super(message);
        }
    }

//    private static final Map<Path, String> CACHED_FILES = new HashMap<>();

    private static final Map<Path, Mustache> CACHED_MUSTACHE_RENDERERS = new HashMap<>();
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    private static final Pattern JSON_COMMENT_BLANKING_PATTERN = Pattern.compile("[^\\s]");

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
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            return readStringFromReader(reader);
        }
    }

    private static String readStringFromReader(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[8192];
        int readCount;
        while ((readCount = reader.read(buffer)) > -1) {
            result.append(buffer, 0, readCount);
        }
        return result.toString();
    }

//    public static String readStringFromCachedUtf8File(Path filePath) {
//        return CACHED_FILES.computeIfAbsent(filePath, path -> {
//            try {
//                return readStringFromUtf8File(path);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }

    public static String blankCommentLine(String line, String commentChar) {
        if (line.trim().startsWith(commentChar)) {
            Matcher matcher = JSON_COMMENT_BLANKING_PATTERN.matcher(line);
            return matcher.replaceAll(" ");
        } else {
            return  line;
        }
    }

    /**
     * When reading replaces with spaces the content of those lines whose first non-blank
     * symbol is `commentChar`. Then, when a parser points at an error, this error will be
     * found at the pointed line and at the pointed position in the initial (commented) file.
     * <br />
     * NOTE. JSON syntax does not allow comments. In this application, this function was added
     * for convenience.
     */
    public static String readStringFromCommentedFile(Path filePath, String commentChar,
            Charset charset) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(blankCommentLine(line, commentChar)).append("\n");
            }
        }
        return result.toString();
    }

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

//    public static boolean jsonObjectContains(JsonObject jsonObject, String memberName) {
//        return jsonObject.get(memberName) != null;
//    }
//
//    public static boolean jsonObjectContainsAny(JsonObject jsonObject, String... memberNames) {
//        for (String mn : memberNames) {
//            if (jsonObject.get(mn) != null) {
//                return true;
//            }
//        }
//        return false;
//    }

    @SafeVarargs
    public static <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    public static <T> Optional<T> firstNotNullOptional(T... values) {
        for (T value : values) {
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

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

    public static String readStringFromResource(String resourceLocation) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                classLoader.getResourceAsStream(resourceLocation), StandardCharsets.UTF_8))) {
            return readStringFromReader(reader);
        }
    }

    /**
     * The `page` argument is an HTML page.
     * The `resource` argument is a relative location of an HTML page resource (like another page,
     * a picture, a CSS file etc.). So the both arguments cannot be empty or end with a '/'.
     * <br />
     * The method considers the both arguments being relative to the same location. It returns
     * the relative location that being applied on the HTML page `page` will resolve to `path`.
     * <br />
     * ATTENTION! This method wasn't tested with ABSOLUTE paths as any of the arguments.
     */
    public static String relativizeRelativeResource(String resource, String page)
            throws ResourceLocationException {
        page = page.replace('\\', '/');
        if (page.isEmpty() || page.endsWith("/")) {
            throw new ResourceLocationException("Incorrect page location: " + page);
        }
        resource = resource.replace('\\', '/');
        if (resource.isEmpty() || resource.endsWith("/")) {
            throw new ResourceLocationException("Incorrect relatively located resource: " +
                    resource);
        }
        Path basePath = Paths.get(page).getParent();
        if (basePath == null) {
            return resource;
        } else {
            return basePath.relativize(Paths.get(resource)).toString().replace('\\', '/');
        }
    }

    /**
     * The `page` argument is an HTML page, so it cannot be empty or end with a '/'.
     * The `path` argument is a relative path to a place where HTML page resources (like other
     * pages, pictures, CSS files etc.) can be allocated. So the `path` argument must end with
     * '/' or be empty so that it can be used in substitutions like `path + "styles.css"`.
     *
     * The method considers the both arguments being relative to the same location. It returns the
     * path that being applied from the HTML page `page` will lead to `path`. The result will match
     * the same requirements as the `path` argument matches, i.e. it will be empty or end with '/'.
     *
     * ATTENTION! This method wasn't tested with ABSOLUTE paths as any of the arguments.
     */
    public static String relativizeRelativePath(String path, String page)
            throws ResourceLocationException {
        page = page.replace('\\', '/');
        if (page.isEmpty() || page.endsWith("/")) {
            throw new ResourceLocationException("Incorrect page location: " + page);
        }
        path = path.replace('\\', '/');
        if (!path.isEmpty() && !path.endsWith("/") || path.equals("/")) {
            throw new ResourceLocationException("Incorrect relative path: " + path);
        }
        Path basePath = Paths.get(page).getParent();
        if (basePath == null) {
            return path;
        } else {
            String relativePath = basePath.relativize(Paths.get(path)).toString().replace('\\', '/');
            if (relativePath.isEmpty() || relativePath.equals("./") || relativePath.equals(".")) {
                return "";
            } else if (relativePath.endsWith("/")) {
                return relativePath;
            } else {
                return relativePath + "/";
            }
        }
    }

}
