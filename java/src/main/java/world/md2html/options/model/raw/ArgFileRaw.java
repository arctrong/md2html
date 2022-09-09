package world.md2html.options.model.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ArgFileRaw.ArgFileRawBuilder.class)
public class ArgFileRaw {
    ArgFileOptionsRaw options;
    @JsonProperty("default")
    ArgFileDocumentRaw defaultSection;
    List<ArgFileDocumentRaw> documents;
    Map<String, JsonNode> plugins;
}