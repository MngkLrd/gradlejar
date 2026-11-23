package com.kirill.simplesorter.sorting;

import java.util.Locale;

/** Stable registry-path token matching with boundaries, no substring false positives. */
public final class PathTokenMatcher {
    private PathTokenMatcher() {}

    public static boolean matches(String path, String token) {
        if (path == null || token == null || token.isBlank()) return false;
        String p = path.toLowerCase(Locale.ROOT).replace('-', '_');
        String t = token.trim().toLowerCase(Locale.ROOT).replace('-', '_');

        if (t.endsWith("/") || t.endsWith("_")) {
            return p.startsWith(t) || p.contains("/" + t);
        }
        if (t.startsWith("/") || t.startsWith("_")) {
            return p.endsWith(t) || p.contains(t + "/");
        }
        int at = 0;
        while ((at = p.indexOf(t, at)) >= 0) {
            int end = at + t.length();
            if ((at == 0 || isSeparator(p.charAt(at - 1)))
                    && (end == p.length() || isSeparator(p.charAt(end)))) return true;
            at++;
        }
        return false;
    }

    private static boolean isSeparator(char c) {
        return c == '_' || c == '/' || c == '.' || c == '-';
    }
}
