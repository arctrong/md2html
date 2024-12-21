package world.md2html.utils;

import java.util.HashMap;
import java.util.Map;

public class UniqueIndexer {
    private final Map<String, Integer> uniqueMap = new HashMap<>();

    public String getUnique(String string) {
        Integer index = uniqueMap.get(string);
        if (index == null) {
            uniqueMap.put(string, 0);
        } else {
            index++;
            uniqueMap.put(string, index);
            string = string + "_" + index;
        }
        return string;
    }
}
