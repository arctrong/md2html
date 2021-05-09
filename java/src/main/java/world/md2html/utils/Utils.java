package world.md2html.utils;

import java.io.BufferedReader;
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
            return dotPos > 0 ? path.substring(0, dotPos) : path;
        } else {
            return path;
        }
    }

    public static String readStringFromUtf8File(Path filePath) throws Exception {
        return readStringFromFile(filePath, StandardCharsets.UTF_8);
    }

    public static String readStringFromFile(Path filePath, Charset charset) throws Exception {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = Files.newBufferedReader(filePath, charset);
        char[] buffer = new char[8192];
        int readCount;
        while ((readCount = reader.read(buffer)) > -1) {
            result.append(buffer, 0, readCount);
        }
        return result.toString();
    }

}
