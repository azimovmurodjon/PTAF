package com.ptaf.ui_performance.reporting;

import java.util.regex.Pattern;

/** Removes common credential and token patterns before errors are written to a shareable report. */
public final class UiPerformanceSensitiveTextSanitizer {
    private static final Pattern PASSWORD_PAIR = Pattern.compile("(?i)(password|passwd|pwd)\\s*([=:])\\s*[^\\s,;]+" );
    private static final Pattern TOKEN_PAIR = Pattern.compile("(?i)(token|authorization|bearer)\\s*([=:])\\s*(?:bearer\\s+)?[^\\s,;]+" );
    private static final Pattern QUERY_SECRET = Pattern.compile("(?i)(password|token|access_token|client_secret)=([^&\\s]+)");

    private UiPerformanceSensitiveTextSanitizer() {
        throw new IllegalStateException("Utility class");
    }

    /** Returns a bounded, sanitized diagnostic message that is safe to include in reports. */
    public static String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String sanitized = PASSWORD_PAIR.matcher(text).replaceAll("$1$2[REDACTED]");
        sanitized = TOKEN_PAIR.matcher(sanitized).replaceAll("$1$2[REDACTED]");
        sanitized = QUERY_SECRET.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = sanitized.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() > 500 ? sanitized.substring(0, 497) + "..." : sanitized;
    }
}
