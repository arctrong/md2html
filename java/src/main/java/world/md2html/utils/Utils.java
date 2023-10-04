package world.md2html.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    @FunctionalInterface
    public interface FunctionWithException<T, R> {
        R apply(T arg) throws Exception;
    }

    @FunctionalInterface
    public interface SupplierWithException<R> {
        R apply() throws Exception;
    }

    public static final Map<Path, String> CACHED_FILES = new HashMap<>();

    public static boolean isNullOrFalse(Object object) {
        return object instanceof Boolean && (Boolean) object;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    private static final Pattern JSON_COMMENT_BLANKING_PATTERN = Pattern.compile("[^\\s]");
    private static final Pattern REGEX_MASKING_PATTERN =
            Pattern.compile("([?^\\\\$.|*+\\[\\](){}])");

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

    public static String getCachedString(Path path,
            FunctionWithException<Path, String> stringProvider) throws Exception {
        String lines = CACHED_FILES.get(path);
        if (lines == null) {
          //  try {
                lines = stringProvider.apply(path);
       //     } catch (Exception e) {
     //           throw new RuntimeException(e);
    //        }
            CACHED_FILES.put(path, lines);
        }
        return lines;
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
     * NOTE. JSON syntax does not allow comments. In this application comments are supported
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

    @SafeVarargs
    public static <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String formatNanoSeconds(long duration) {
        StringBuilder sb = new StringBuilder(20);
        long quotient = duration / 1_000_000;
        long remainder = quotient % 1_000;
        sb.insert(0, String.format(".%03d", remainder));
        quotient = quotient / 1_000;
        remainder = quotient % 60;
        sb.insert(0, String.format(":%02d", remainder));
        quotient = quotient / 60;
        remainder = quotient % 60;
        sb.insert(0, String.format(":%02d", remainder));
        quotient = quotient / 60;
        remainder = quotient % 24;
        sb.insert(0, String.format(" %02d", remainder));
        quotient = quotient / 24;
        sb.insert(0, String.format("%d", quotient));
        return sb.toString();
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
     * the relative location that being applied in the HTML page `page` will resolve to `resource`.
     * <br />
     * ATTENTION! This method wasn't tested with ABSOLUTE paths as any of the arguments.
     */

    public static String relativizeRelativeResource(String resource, String page)
            throws CheckedIllegalArgumentException {

        // TODO Consider refusing from `CheckedIllegalArgumentException` by replacing
        //  with `IllegalArgumentException`.

        page = page.replace('\\', '/');
        if (page.isEmpty() || page.endsWith("/")) {
            throw new CheckedIllegalArgumentException("Incorrect page location: " + page);
        }
        resource = resource.replace('\\', '/');
        if (resource.isEmpty() || resource.endsWith("/")) {
            throw new CheckedIllegalArgumentException("Incorrect relatively located resource: " +
                    resource);
        }
        Path basePath = Paths.get(page).getParent();
        Path result;
        if (basePath == null) {
            result = Paths.get(resource);
        } else {
            result = basePath.toAbsolutePath().relativize(Paths.get(resource).toAbsolutePath());
        }
        return result.normalize().toString().replace('\\', '/');
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
            throws CheckedIllegalArgumentException {
        page = page.replace('\\', '/');
        if (page.isEmpty() || page.endsWith("/")) {
            throw new CheckedIllegalArgumentException("Incorrect page location: " + page);
        }
        path = path.replace('\\', '/');
        if (!path.isEmpty() && !path.endsWith("/") || path.equals("/")) {
            throw new CheckedIllegalArgumentException("Incorrect relative path: " + path);
        }
        Path basePath = Paths.get(page).getParent();
        Path resultPath;
        if (basePath == null) {
            resultPath = Paths.get(path);
        } else {
            resultPath = basePath.toAbsolutePath().relativize(Paths.get(path).toAbsolutePath());
        }
        String result = resultPath.normalize().toString().replace('\\', '/');
        if (result.isEmpty() || result.equals("./") || result.equals(".")) {
            return "";
        } else if (result.endsWith("/")) {
            return result;
        } else {
            return result + "/";
        }
    }

    /**
     * This method assumes that the `object`'s `toString()` method returns value is like
     * `ClassName(field1=value1, field2=value2,...)`. It then returns the value like:
     * `{field1=value1, field2=value2,...}`.
     */
    public static String refineToString(Object object) {
        int prefixLength = object.getClass().getSimpleName().length() + 1;
        String string = object.toString();
        return "{" + string.substring(prefixLength, string.length() - 1) + "}";
    }

    public static <R> R supplyWithFileExceptionAsUserError(
            SupplierWithException<R> supplier,
            String stepDescription) {
        R result;
        try {
            result = supplier.apply();
        } catch (NoSuchFileException | FileNotFoundException e) {
            throw new UserError(String.format(stepDescription + ": " +
                    e.getClass().getSimpleName() + ": " +  e.getMessage()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static String maskRegexChars(String string) {
        return REGEX_MASKING_PATTERN.matcher(string).replaceAll("\\\\$1");
    }
}
