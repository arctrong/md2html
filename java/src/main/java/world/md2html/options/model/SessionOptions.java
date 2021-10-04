package world.md2html.options.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SessionOptions {
    private final boolean verbose;
    private final boolean legacyMode;
}
