package world.md2html.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniqueIndexerTest {

    @Test
    void getUnique() {
        UniqueIndexer uniqueIndexer = new UniqueIndexer();
        String[] inputs = {"hello", "hello", "hello", "test", "test", "test"};
        String[] expectedOutputs = {"hello", "hello_1", "hello_2", "test", "test_1", "test_2"};

        String[] results = Arrays.stream(inputs)
                .map(uniqueIndexer::getUnique)
                .toArray(String[]::new);

        assertEquals(String.join(",", expectedOutputs), String.join(",", results));
    }
}