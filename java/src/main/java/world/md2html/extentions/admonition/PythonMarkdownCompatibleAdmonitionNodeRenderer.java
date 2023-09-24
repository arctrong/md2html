package world.md2html.extentions.admonition;

import com.vladsch.flexmark.ext.admonition.AdmonitionBlock;
import com.vladsch.flexmark.ext.admonition.internal.AdmonitionOptions;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.html.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class PythonMarkdownCompatibleAdmonitionNodeRenderer implements NodeRenderer {

    public static AttributablePart ADMONITION_TITLE_PART = new AttributablePart("ADMONITION_TITLE_PART");

    final private AdmonitionOptions options;

    public PythonMarkdownCompatibleAdmonitionNodeRenderer(DataHolder options) {
        this.options = new AdmonitionOptions(options);
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(AdmonitionBlock.class, this::render));
        return set;
    }

    private void render(AdmonitionBlock node, NodeRendererContext context, HtmlWriter html) {

        String info = node.getInfo().toString().toLowerCase();

        String title;
        if (node.getTitle().isNull()) {
            title = this.options.qualifierTitleMap.get(info);
            if (title == null) {
                title = info.substring(0, 1).toUpperCase() + info.substring(1);
            }
        } else {
            title = node.getTitle().toString();
        }

        html.srcPos(node.getChars()).withAttr()
                .attr(Attribute.CLASS_ATTR, "admonition")
                .attr(Attribute.CLASS_ATTR, info)
                .tag("div", false).line();

        if (!title.isEmpty()) {
            html.withAttr(ADMONITION_TITLE_PART).withAttr()
                    .attr(Attribute.CLASS_ATTR, "admonition-title")
                    .tag("p")
                    .text(title).closeTag("p").line();
        }

        context.renderChildren(node);

        html.closeTag("div").line();
    }

    public static class Factory implements NodeRendererFactory {
        @NotNull
        @Override
        public NodeRenderer apply(@NotNull DataHolder options) {
            return new PythonMarkdownCompatibleAdmonitionNodeRenderer(options);
        }
    }

}
