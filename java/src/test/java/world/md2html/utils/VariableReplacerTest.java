package world.md2html.utils;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VariableReplacerTest {

    public Stream<Arguments> positiveTest() {
        return Stream.of(
                Arguments.of("simple", "start${1}middle${2}end",
                        asList("-A-", "-B-"), "start-A-middle-B-end"),
                Arguments.of("at start", "${1}end",
                        singletonList("-A-"), "-A-end"),
                Arguments.of("at end", "start${1}",
                        singletonList("-A-"), "start-A-"),
                Arguments.of("with spaces", "start${\n\t1 \n}end",
                        singletonList("-A-"), "start-A-end"),
                Arguments.of("not enough values", "start${1}middle${2} end",
                        singletonList("-A-"), "start-A-middle end"),
                Arguments.of("with masking in the middle", "start$${1}end",
                        emptyList(), "start${1}end"),
                Arguments.of("with masking at start", "$${1}end",
                        emptyList(), "${1}end"),
                Arguments.of("with masking at end", "start$$",
                        emptyList(), "start$"),
                Arguments.of("with marker at end", "start$",
                        emptyList(), "start$"),
                Arguments.of("no positions", "something",
                        singletonList("X"), "something"),
                Arguments.of("multi digit position", "start-${11}-end",
                        asList("1", "2", "3", "4", "5", "6", "7", "8", "9",
                                "10", "11ok"), "start-11ok-end")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void positiveTest(String name, String template, List<String> values,
                             String expected) throws VariableReplacer.VariableReplacerException {
        VariableReplacer replacer = new VariableReplacer(template);
        assertEquals(expected, replacer.replace(values));
    }

    public Stream<Arguments> negativeTest() {
        return Stream.of(
                Arguments.of("not a digit", "start${not-a-digit}end", emptyList(), "not-a-digit"),
                Arguments.of("position is zero", "start${0}end", emptyList(), "0"),
                Arguments.of("position too small", "start${-43}end", emptyList(), "-43"),
                Arguments.of("no closing brace", "start${1", emptyList(), "brace"),
                Arguments.of("no closing brace at the end", "start${", emptyList(), "brace")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void negativeTest(String name, String template, List<String> values,
                             String expected) {
        VariableReplacer.VariableReplacerException e =
                assertThrows(VariableReplacer.VariableReplacerException.class,
                () -> {
                    VariableReplacer replacer = new VariableReplacer(template);
                    assertEquals(expected, replacer.replace(values));
                });
        assertTrue(e.getMessage().contains(expected));
    }
}
