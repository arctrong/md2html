package world.md2html.testutils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    private TestUtils() {
    }

    public static String relativeToCurrentDir(Path path) {
        String result = Paths.get("").toAbsolutePath().relativize(path).toString()
                .replace("\\", "/");
        if (result.isEmpty() || result.equals("./") || result.equals(".")) {
            return "";
        } else if (result.endsWith("/")) {
            return result;
        } else {
            return result + "/";
        }
    }

}
