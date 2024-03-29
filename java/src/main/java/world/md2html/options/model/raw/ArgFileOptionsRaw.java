package world.md2html.options.model.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = ArgFileOptionsRaw.ArgFileOptionsRawBuilder.class)
public class ArgFileOptionsRaw {
    boolean verbose;
    @JsonProperty("legacy-mode")
    boolean legacyMode;
}
