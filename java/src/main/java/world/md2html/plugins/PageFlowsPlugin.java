package world.md2html.plugins;

import lombok.Getter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PageFlowsPlugin implements Md2HtmlPlugin {

    private Map<String, List<Page>> data;

    @Override
    public boolean acceptData() {
        return false;
    }

    @Getter
    private static class Page {
        private final String link;
        private final String title;
        private final boolean current;

        public Page(String link, String title, boolean current) {
            this.link = link;
            this.title = title;
            this.current = current;
        }

        public Page(String link, String title) {
            this.link = link;
            this.title = title;
            this.current = false;
        }
    }

    private static class PageFlow implements Iterable<Page> {

        @Getter private final List<Page> pages;
        @Getter private final Page previous;
        @Getter private final Page current;
        @Getter private final Page next;
        // For a logic-less template like Mustache the following calculated fields will help a lot.
        @Getter private final boolean has_navigation;
        @Getter private final boolean not_empty;

        private PageFlow(List<Page> pages, Page previous, Page current, Page next) {
            this.pages = pages;
            this.previous = previous;
            this.current = current;
            this.next = next;
            this.has_navigation = this.previous != null || this.next != null;
            this.not_empty = this.pages != null && !this.pages.isEmpty();
        }

        @Override
        public Iterator<Page> iterator() {
            return pages.iterator();
        }
    }

}
