package world.md2html.options.argfile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import world.md2html.options.model.Document;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.List;

@AllArgsConstructor
@Getter
public class ArgFileOptions {

    private final SessionOptions sessionOptions;
    private final List<Document> documents;
    private final List<Md2HtmlPlugin> md2HtmlPlugins;

}
