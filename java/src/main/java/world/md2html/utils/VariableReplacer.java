package world.md2html.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VariableReplacer {

    private static final Pattern PLACEHOLDER_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public static class VariableReplacerException extends Exception {
        public VariableReplacerException(String message) {
            super(message);
        }
    }

    private static class NamedPlaceholder {
        private final String name;

        private NamedPlaceholder(String name) {
            this.name = name;
        }
    }

    private final char TOKEN_MARKER = '$';
    private final char TOKEN_START = '{';
    private final char TOKEN_END = '}';

    private final List<Object> parts = new LinkedList<>();

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
                    appendLiteral(token);
                    token = new StringBuilder();
                    state = 2;
                } else {
                    token.append(c);
                    state = 0;
                }
            } else {
                if (c == TOKEN_END) {
                    parts.add(parsePlaceholderToken(token.toString()));
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
        appendLiteral(token);
    }

    private void appendLiteral(StringBuilder token) {
        if (token.length() > 0) {
            parts.add(token.toString());
        }
    }

    private Object parsePlaceholderToken(String rawToken) throws VariableReplacerException {
        String placeholder = rawToken.trim();
        if (placeholder.isEmpty()) {
            throw new VariableReplacerException("Empty placeholder");
        }
        if (placeholder.chars().allMatch(Character::isDigit)) {
            int index = Integer.parseInt(placeholder);
            if (index < 1) {
                throw new VariableReplacerException("Replacement position is less that 1: " +
                        placeholder);
            }
            return index;
        }
        if (PLACEHOLDER_NAME_PATTERN.matcher(placeholder).matches()) {
            return new NamedPlaceholder(placeholder);
        }
        throw new VariableReplacerException("Invalid placeholder: " + placeholder);
    }

    public String replace(List<String> substitutions) {
        StringBuilder result = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Integer) {
                int pos = (Integer) part;
                if (substitutions.size() >= pos) {
                    result.append(substitutions.get(pos - 1));
                }
            } else if (part instanceof String) {
                result.append(part);
            }
        }
        return result.toString();
    }

    public String replace(Map<String, String> substitutions) {
        StringBuilder result = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof NamedPlaceholder) {
                String value = substitutions.get(((NamedPlaceholder) part).name);
                if (value != null) {
                    result.append(value);
                }
            } else if (part instanceof String) {
                result.append(part);
            }
        }
        return result.toString();
    }
}
