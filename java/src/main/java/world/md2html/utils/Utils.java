package world.md2html.utils;

import com.eclipsesource.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    private static final Map<Path, String> CACHED_FILES = new HashMap<>();

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

}
