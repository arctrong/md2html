package world.md2html.options.argfile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SessionOptions {
    private final boolean verbose;
    private final boolean legacyMode;
}
