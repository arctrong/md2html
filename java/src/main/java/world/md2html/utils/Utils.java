package world.md2html.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {

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

}
