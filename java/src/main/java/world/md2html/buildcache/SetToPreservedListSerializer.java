package world.md2html.buildcache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;

public class SetToPreservedListSerializer extends JsonSerializer<SortedSet<String>> {

    @Override
    public void serialize(SortedSet<String> value, JsonGenerator gen,
            SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeObject(Collections.emptyList());
        } else {
            gen.writeObject(new ArrayList<>(value));
        }
    }
}
