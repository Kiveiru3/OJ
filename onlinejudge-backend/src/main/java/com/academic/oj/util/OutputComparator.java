package com.academic.oj.util;

import java.util.ArrayList;
import java.util.List;

public final class OutputComparator {

    private OutputComparator() {
    }

    public static boolean equalsIgnorePresentation(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    public static String preview(String text, int maxLength) {
        String normalized = normalize(text);
        if (maxLength <= 0 || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...(truncated)";
    }

    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String unified = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = unified.split("\n", -1);
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String rawLine : rawLines) {
            lines.add(trimLineEnd(rawLine));
        }

        int end = lines.size() - 1;
        while (end >= 0 && lines.get(end).isEmpty()) {
            end--;
        }
        if (end < 0) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= end; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(lines.get(i));
        }
        return out.toString();
    }

    private static String trimLineEnd(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int end = text.length();
        while (end > 0) {
            char ch = text.charAt(end - 1);
            if (ch == ' ' || ch == '\t' || ch == '\f' || ch == '\u000B') {
                end--;
            } else {
                break;
            }
        }
        if (end == text.length()) {
            return text;
        }
        return text.substring(0, end);
    }
}

