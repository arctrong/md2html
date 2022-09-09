package world.md2html.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static world.md2html.utils.JsonUtils.OBJECT_MAPPER;

class JsonUtilsTest {

    @Test
    void objectNodeSetDefault() throws JsonProcessingException {
        ObjectNode node = (ObjectNode) OBJECT_MAPPER.readTree("{\"key1\": \"value1\"}");
        TextNode updatedNode = (TextNode)
            JsonUtils.objectNodeSetDefault(node, "key1", new TextNode("newValue"));
        assertEquals("value1", updatedNode.asText());
        assertEquals("value1", node.get("key1").asText());
        updatedNode = (TextNode)
                JsonUtils.objectNodeSetDefault(node, "key2", new TextNode("newValue"));
        assertEquals("newValue", updatedNode.asText());
        assertEquals("newValue", node.get("key2").asText());
    }

}
