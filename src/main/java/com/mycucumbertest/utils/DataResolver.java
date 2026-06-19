package com.mycucumbertest.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${...}} placeholders used in feature files / step arguments.
 *
 * Resolution order for a token {@code ${key.field}}:
 *   1st - testdata/users.json  (treats the part before the first dot as the
 *         user key and the remainder as the field, e.g. ${standardUser.username})
 *   2nd - config.properties / -D system property via {@link ConfigReader}
 *         (full dotted token, e.g. ${base.url})
 *
 * Strings without any {@code ${...}} token are returned unchanged, so plain
 * literals in the feature file still work.
 */
public final class DataResolver {

    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)\\}");

    private DataResolver() {}

    /** Replace every {@code ${...}} token in {@code raw}; pass literals through. */
    public static String resolve(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = TOKEN.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = resolveToken(m.group(1).trim());
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolveToken(String expr) {
        // 1) Try users.json as  userKey.field
        int dot = expr.indexOf('.');
        if (dot > 0) {
            String userKey = expr.substring(0, dot);
            String field = expr.substring(dot + 1);
            String v = TestUsers.field(userKey, field);
            if (v != null) {
                return v;
            }
        }
        // 2) Fall back to config.properties / system property
        String v = ConfigReader.get(expr);
        if (v != null) {
            return v;
        }
        throw new IllegalArgumentException(
                "Unresolved placeholder ${" + expr + "} — not in users.json or config.properties");
    }
}
