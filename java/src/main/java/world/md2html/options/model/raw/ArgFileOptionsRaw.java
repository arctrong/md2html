package world.md2html.options.model.raw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ArgFileOptionsRaw {
    boolean verbose;
    @JsonProperty("cache-file")
    String cacheFile;
}
