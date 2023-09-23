package world.md2html.utils;

import java.util.LinkedList;
import java.util.List;

public class VariableReplacer {

    public static class VariableReplacerException extends Exception {
        public VariableReplacerException(String message) {
            super(message);
        }
    }

    private static class Part {
        /**
         * Think the part as String if its int value is not greater than zero.
         */
        private final int intValue;
        private final String stringValue;

        public Part(int intValue, String stringValue) {
            this.intValue = intValue;
            this.stringValue = stringValue;
        }
    }

    private final char TOKEN_MARKER = '$';
    private final char TOKEN_START = '{';
    private final char TOKEN_END = '}';

    private final List<Part> parts = new LinkedList<>();

    public VariableReplacer(String template) throws VariableReplacerException {

        int state = 0;
        StringBuilder token = new StringBuilder();
        for (char c : template.toCharArray()) {
            if (state == 0) {
                if (c == TOKEN_MARKER) {
                    state = 1;
                } else {
                    token.append(c);
                }
            } else if (state == 1) {
                if (c == TOKEN_MARKER) {
                    token.append(TOKEN_MARKER);
                    state = 0;
                } else if (c == TOKEN_START) {
                    parts.add(new Part(-1, token.toString()));
                    token = new StringBuilder();
                    state = 2;
                } else {
                    token.append(c);
                    state = 0;
                }
            } else {
                if (c == TOKEN_END) {
                    String indexStr = token.toString().trim();
                    int index = 0;
                    try {
                        index = Integer.parseInt(indexStr);
                    } catch (NumberFormatException e) {
                        throw new VariableReplacerException("Replacement position is not a number: " +
                                indexStr);
                    }
                    if (index < 1) {
                        throw new VariableReplacerException("Replacement position is less that 1: " +
                                index);
                    }
                    parts.add(new Part(index, null));
                    token = new StringBuilder();
                    state = 0;
                } else {
                    token.append(c);
                }
            }
        }
        if (state > 1) {
            throw new VariableReplacerException("Matching closing brace not found: " +
                    TOKEN_END);
        }
        if (state == 1) {
            token.append(TOKEN_MARKER);
        }
        parts.add(new Part(-1, token.toString()));
    }

    public String replace(List<String> substitutions) {

        StringBuilder result = new StringBuilder();
        for (Part part : parts) {
            int pos = part.intValue;
            if (pos > 0) {
                if (substitutions.size() >= pos) {
                    result.append(substitutions.get(pos - 1));
                }
            } else {
                result.append(part.stringValue);
            }
        }
        return result.toString();
    }
}
