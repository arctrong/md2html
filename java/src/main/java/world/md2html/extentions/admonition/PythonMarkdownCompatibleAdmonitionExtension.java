package world.md2html.extentions.admonition;

import com.vladsch.flexmark.ext.admonition.AdmonitionExtension;
import com.vladsch.flexmark.ext.admonition.internal.AdmonitionBlockParser;
import com.vladsch.flexmark.ext.admonition.internal.AdmonitionNodeFormatter;
import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Modification of {@link com.vladsch.flexmark.ext.admonition.AdmonitionExtension} that
 * generate the same HTML code as
 * <a href="https://python-markdown.github.io/extensions/admonition/">Python-Markdown
 * Admonition extension</a>.
 */
public class PythonMarkdownCompatibleAdmonitionExtension implements Parser.ParserExtension,
        HtmlRenderer.HtmlRendererExtension, Formatter.FormatterExtension {

    final public static DataKey<Integer> CONTENT_INDENT = new DataKey<>("ADMONITION.CONTENT_INDENT", 4);
    final public static DataKey<Boolean> ALLOW_LEADING_SPACE = new DataKey<>("ADMONITION.ALLOW_LEADING_SPACE", true);
    final public static DataKey<Boolean> INTERRUPTS_PARAGRAPH = new DataKey<>("ADMONITION.INTERRUPTS_PARAGRAPH", true);
    final public static DataKey<Boolean> INTERRUPTS_ITEM_PARAGRAPH = new DataKey<>("ADMONITION.INTERRUPTS_ITEM_PARAGRAPH", true);
    final public static DataKey<Boolean> WITH_SPACES_INTERRUPTS_ITEM_PARAGRAPH = new DataKey<>("ADMONITION.WITH_SPACES_INTERRUPTS_ITEM_PARAGRAPH", true);
    final public static DataKey<Boolean> ALLOW_LAZY_CONTINUATION = new DataKey<>("ADMONITION.ALLOW_LAZY_CONTINUATION", true);
    final public static DataKey<String> UNRESOLVED_QUALIFIER = new DataKey<>("ADMONITION.UNRESOLVED_QUALIFIER", "note");
    final public static DataKey<Map<String, String>> QUALIFIER_TYPE_MAP = new DataKey<>("ADMONITION.QUALIFIER_TYPE_MAP", AdmonitionExtension::getQualifierTypeMap);
    final public static DataKey<Map<String, String>> QUALIFIER_TITLE_MAP = new DataKey<>("ADMONITION.QUALIFIER_TITLE_MAP", AdmonitionExtension::getQualifierTitleMap);

    private PythonMarkdownCompatibleAdmonitionExtension() {
    }

    public static PythonMarkdownCompatibleAdmonitionExtension create() {
        return new PythonMarkdownCompatibleAdmonitionExtension();
    }

    @Override
    public void extend(Formatter.Builder formatterBuilder) {
        formatterBuilder.nodeFormatterFactory(new AdmonitionNodeFormatter.Factory());
    }

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {

    }

    @Override
    public void parserOptions(MutableDataHolder options) {

    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customBlockParserFactory(new AdmonitionBlockParser.Factory());
    }

    @Override
    public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
        if (htmlRendererBuilder.isRendererType("HTML")) {
            htmlRendererBuilder
                    .nodeRendererFactory(new PythonMarkdownCompatibleAdmonitionNodeRenderer.Factory());
        } else if (htmlRendererBuilder.isRendererType("JIRA")) {

        }
    }

}
