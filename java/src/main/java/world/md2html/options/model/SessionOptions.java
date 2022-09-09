package world.md2html.options.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SessionOptions {
    boolean verbose;
    boolean legacyMode;
}
