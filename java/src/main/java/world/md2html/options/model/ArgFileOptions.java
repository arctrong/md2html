package world.md2html.options.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.List;

@AllArgsConstructor
@Getter
public class ArgFileOptions {
    private final SessionOptions options;
    private final List<Document> documents;
    private final List<Md2HtmlPlugin> plugins;

}
