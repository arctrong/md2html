package world.md2html.options;

import world.md2html.ArgumentsHelper;
import world.md2html.options.argfile.ArgFileParseException;
import world.md2html.options.model.ArgFile;
import world.md2html.options.model.CliOptions;
import world.md2html.options.model.raw.ArgFileRaw;

import static world.md2html.options.argfile.ArgFileParsingHelper.readArgumentFileNode;

public class TestUtils {

    public static ArgFile parseArgumentFile(String argFileString, CliOptions cliOptions)
            throws ArgFileParseException {
        ArgFileRaw argFileRaw = readArgumentFileNode(argFileString);
        return ArgumentsHelper.parseArgumentFile(argFileRaw, cliOptions);
    }

}
