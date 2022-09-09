package world.md2html.options;

import org.javatuples.Pair;
import world.md2html.ArgumentsHelper;
import world.md2html.Md2HtmlRunner;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.raw.ArgFileRaw;
import world.md2html.plugins.Md2HtmlPlugin;

import java.lang.reflect.Method;
import java.util.List;

import static world.md2html.options.argfile.ArgFileParsingHelper.readArgumentFileNode;

public class TestUtils {

//    private static final Method methodParseArgumentFile;
//    static {
//        try {
//            // The decision was made not to make the original method public as it must not
//            // be called from the outside.
//            // TODO There's a possible problem here: coverage and mutation tests (not used yet)
//            //  may not see this code invocation during tests.
//            methodParseArgumentFile = Md2HtmlRunner.class.getDeclaredMethod("parseArgumentFile",
//                    ArgFileRaw.class, CliOptions.class);
//            methodParseArgumentFile.setAccessible(true);
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static ArgFile parseArgumentFile(String argFileString, CliOptions cliOptions)
            throws ArgFileParseException {
        ArgFileRaw argFileRaw = readArgumentFileNode(argFileString);
        return ArgumentsHelper.parseArgumentFile(argFileRaw, cliOptions);
    }

}
