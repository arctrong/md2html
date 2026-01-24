package world.md2html.buildcache;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ListToSetDeserializer extends JsonDeserializer<SortedSet<String>> {
    @Override
    public SortedSet<String> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        List<String> list = p.readValueAs(new TypeReference<List<String>>() {});
        return list != null ? new TreeSet<>(list) : new TreeSet<>();
    }
}
