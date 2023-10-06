package world.md2html.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class SmartSubstringer {

    private final String startWith;
    private final String endWith;
    private final String startMarker;
    private final String endMarker;
    private final boolean empty;
    private final Pattern pattern;

    public SmartSubstringer(String startWith, String endWith, String startMarker,
                            String endMarker) {
        this.startWith = startWith;
        this.endWith = endWith;
        this.startMarker = startMarker;
        this.endMarker = endMarker;
        this.empty = isEmpty(startWith) && isEmpty(endWith) && isEmpty(startMarker) &&
                isEmpty(endMarker);
        String patternString = Stream.of(this.startWith, this.endWith, this.startMarker,
                        this.endMarker)
                .filter(StringUtils::isNotEmpty)
                .map(Utils::maskRegexChars)
                .collect(Collectors.joining("|"));
        this.pattern = Pattern.compile(patternString);
    }

    public SmartSubstringer smartCopy(String startWith, String endWith, String startMarker,
                                      String endMarker) {
        startWith = startWith == null ? this.startWith : startWith;
        endWith = endWith == null ? this.endWith : endWith;
        startMarker = startMarker == null ? this.startMarker : startMarker;
        endMarker = endMarker == null ? this.endMarker : endMarker;
        if (startWith.equals(this.startWith) && endWith.equals(this.endWith) &&
                startMarker.equals(this.startMarker) && endMarker.equals(this.endMarker)) {
            return this;
        } else {
            return new SmartSubstringer(startWith, endWith, startMarker, endMarker);
        }
    }

    public String substring(String string) {
        if (this.empty || string == null) {
            return string;
        }
        int startPosition = 0;
        int endPosition = string.length();
        boolean startWithFound = false;
        boolean endWithFound = false;
        boolean startMarkerFound = false;
        boolean endMarkerFound = false;

        Matcher found = this.pattern.matcher(string);
        while (found.find()) {
            if (found.group().equals(this.startWith) && !startWithFound && !startMarkerFound) {
                startWithFound = true;
                startPosition = found.start();
            } else if (found.group().equals(this.endWith) && !endWithFound && !endMarkerFound) {
                endWithFound = true;
                endPosition = found.end();
            } else if (found.group().equals(this.startMarker) && !startWithFound && !startMarkerFound) {
                startMarkerFound = true;
                startPosition = found.end();
            } else if (found.group().equals(this.endMarker) && !endWithFound && !endMarkerFound) {
                endMarkerFound = true;
                endPosition = found.start();
            }
        }
        if ((isNotEmpty(this.startWith) || isNotEmpty(this.startMarker)) &&
                !(startWithFound || startMarkerFound)) {
            return "";
        }
        if (startPosition > 0 || endPosition < string.length()) {
            if (startPosition <= endPosition) {
                return string.substring(startPosition, endPosition);
            }
            return "";
        } else {
            return string;
        }
    }
}
