package world.md2html.options.model;

import lombok.Builder;
import lombok.Value;
import world.md2html.plugins.Md2HtmlPlugin;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class ArgFile {
    SessionOptions options;
    List<Document> documents;
    List<Md2HtmlPlugin> plugins;
}
