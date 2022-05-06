package world.md2html.plugins;

import com.fasterxml.jackson.databind.node.ObjectNode;
import world.md2html.options.model.CliOptions;

import java.util.List;

public interface InitializationAction {

    void initialize(ObjectNode argFileNode, CliOptions cliOptions, List<Md2HtmlPlugin> plugins);

}
