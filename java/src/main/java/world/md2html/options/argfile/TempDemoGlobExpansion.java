package world.md2html.options.argfile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static world.md2html.options.argfile.ArgFileParsingHelper.expandGlob;

public class TempDemoGlobExpansion {

    public static void main(String[] args) throws IOException {

//        String glob = "**/*.txt";
//        Path path = Paths.get("C:\\dev\\md2html");
        String glob = "subdir01/**/*.txt";
        Path path = Paths.get("C:\\dev\\md2html\\test\\test_input\\GlobInputTest");

        List<Path> globPathList = new ArrayList<>(expandGlob(glob,
                path));

        globPathList.forEach(System.out::println);

        System.out.println("count=" + globPathList.size());


    }


}
