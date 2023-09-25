package world.md2html.pagemetadata;

import org.junit.jupiter.api.Test;
import world.md2html.pagemetadata.PageMetadataHandlersWrapper.MetadataMatchObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataFinderTest {

    private void assertMetadataMatchObjectsEqual(MetadataMatchObject matchObject1,
                                                 MetadataMatchObject matchObject2) {
        assertEquals(matchObject1.getBefore(), matchObject2.getBefore());
        assertEquals(matchObject1.getMetadata(), matchObject2.getMetadata());
        assertEquals(matchObject1.getMetadataBlock(), matchObject2.getMetadataBlock());
        assertEquals(matchObject1.getEndPos(), matchObject2.getEndPos());
    }

    @Test
    public void trivial() {
        String pageContent = "    <!--metadata{\"key\": \"value\"}--> other " +
                "text <!--variables{\"question\": \"answer\"} --> some more text";
        List<MetadataMatchObject> matchObjects = new ArrayList<>();
        PageMetadataHandlersWrapper.metadataFinder(pageContent)
                .forEachRemaining(matchObjects::add);
        assertEquals(2, matchObjects.size());
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                        "    ", "metadata", "{\"key\": \"value\"}",
                        "<!--metadata{\"key\": \"value\"}-->", 35),
                matchObjects.get(0));
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                        " other text ", "variables", "{\"question\": \"answer\"} ",
                        "<!--variables{\"question\": \"answer\"} -->", 86),
                matchObjects.get(1));
    }

    @Test
    public void marginPositions() {
        String pageContent = "<!--m1 d1--> t2 <!--m2 d2--> t3 <!--m3 d3-->";
        List<MetadataMatchObject> matchObjects = new ArrayList<>();
        PageMetadataHandlersWrapper.metadataFinder(pageContent)
                .forEachRemaining(matchObjects::add);
        assertEquals(3, matchObjects.size());
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                "", "m1", " d1", "<!--m1 d1-->", 12), matchObjects.get(0));
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                " t2 ", "m2", " d2", "<!--m2 d2-->", 28), matchObjects.get(1));
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                " t3 ", "m3", " d3", "<!--m3 d3-->", 44), matchObjects.get(2));
    }

    @Test
    public void must_ignore_first_closing_delimiter() {
        String pageContent = "some text --><!--m1 v1-->";
        List<MetadataMatchObject> matchObjects = new ArrayList<>();
        PageMetadataHandlersWrapper.metadataFinder(pageContent)
                .forEachRemaining(matchObjects::add);
        assertEquals(1, matchObjects.size());
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                        "some text -->", "m1", " v1", "<!--m1 v1-->", 25),
                matchObjects.get(0));
    }

    @Test
    public void nested_metadata_must_preserve() {
        String pageContent = "<!--m1 d1 <!--m2 d2-->--> t2";
        List<MetadataMatchObject> matchObjects = new ArrayList<>();
        PageMetadataHandlersWrapper.metadataFinder(pageContent)
                .forEachRemaining(matchObjects::add);
        assertEquals(1, matchObjects.size());
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                        "", "m1", " d1 <!--m2 d2-->", "<!--m1 d1 <!--m2 d2-->-->", 25),
                matchObjects.get(0));
    }

    @Test
    public void must_tolerate_unclosed_blocks() {
        String pageContent = "<!--m1 d1--> t2 <!-- some text";
        List<MetadataMatchObject> matchObjects = new ArrayList<>();
        PageMetadataHandlersWrapper.metadataFinder(pageContent)
                .forEachRemaining(matchObjects::add);
        assertEquals(1, matchObjects.size());
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                        "", "m1", " d1", "<!--m1 d1-->", 12),
                matchObjects.get(0));
    }

    @Test
    public void must_tolerate_extra_end_delimiters() {
        String pageContent = "<!--m1 d1--> t2 --> some text -->";
        List<MetadataMatchObject> matchObjects = new ArrayList<>();
        PageMetadataHandlersWrapper.metadataFinder(pageContent)
                .forEachRemaining(matchObjects::add);
        assertEquals(1, matchObjects.size());
        assertMetadataMatchObjectsEqual(new MetadataMatchObject(
                        "", "m1", " d1", "<!--m1 d1-->", 12),
                matchObjects.get(0));
    }
}