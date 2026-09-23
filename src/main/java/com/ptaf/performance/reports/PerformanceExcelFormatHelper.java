package com.ptaf.performance.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Central formatting helper for Excel performance reporting.
 *
 * <p>
 * This utility class provides a single place to perform consistent, business-friendly
 * formatting of numbers, durations, and text that are used across performance report
 * Excel sheets. Methods are designed to be null-safe and to handle edge cases such as
 * NaN and infinite values to avoid noisy or invalid output in reports.
 * </p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Normalize and sanitize free-text values for safe display in cells.</li>
 *     <li>Format percentages, decimals, integers, scores and durations with predictable precision.</li>
 *     <li>Provide human-readable durations (ms/seconds/minutes/hours) and risk/status normalization.</li>
 *     <li>Defensive handling of invalid numeric inputs (NaN, Infinite, negative values).</li>
 * </ul>
 *
 * <p>All methods are static so this class is safe to use concurrently from multiple threads.</p>
 */
public final class PerformanceExcelFormatHelper {

    // DecimalFormat patterns used across formatting methods.
    // - WHOLE_NUMBER: grouping thousands, no decimal places (e.g., 1,234)
    // - ONE_DECIMAL: one decimal place (e.g., 1,234.5)
    // - TWO_DECIMALS: two decimal places (e.g., 1,234.56)
    // - THREE_DECIMALS: three decimal places (e.g., 1,234.567)
    private static final DecimalFormat WHOLE_NUMBER = new DecimalFormat("#,##0");
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#,##0.0");
    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("#,##0.00");
    private static final DecimalFormat THREE_DECIMALS = new DecimalFormat("#,##0.000");

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private PerformanceExcelFormatHelper() {
        // utility class
    }

    /**
     * Safely normalize free-form text for display in reports.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>null -> empty string</li>
     *     <li>trim leading/trailing whitespace</li>
     *     <li>convert CRLF, CR, LF to a single space (thereby preventing multi-line cells)</li>
     *     <li>collapse repeated whitespace characters into a single space</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <pre>
     * safeText(null) -> ""
     * safeText("  Hello\nWorld  ") -> "Hello World"
     * safeText("A   B") -> "A B"
     * </pre>
     *
     * @param value the raw text value (may be null)
     * @return cleaned, single-line text suitable for report cells
     */
    public static String safeText(String value) {
        if (value == null) {
            return "";
        }

        // Trim leading/trailing whitespace first.
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        // Replace newlines and carriage returns with spaces and collapse multiple spaces.
        return normalized
                .replace("\r\n", " ")         // Windows CRLF -> single space
                .replace('\n', ' ')          // Unix LF -> space
                .replace('\r', ' ')          // Old Mac CR -> space
                .replaceAll("\\s{2,}", " "); // collapse runs of whitespace into one space
    }

    /**
     * Returns the normalized text or a default if the input is absent/empty.
     *
     * @param value the raw text value (may be null)
     * @param defaultValue the string to return when the normalized input is empty
     * @return normalized text if present; otherwise defaultValue
     */
    public static String safeOrDefault(String value, String defaultValue) {
        String normalized = safeText(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    /**
     * Format a numeric value as a percentage string with two decimal places and a trailing '%'.
     *
     * <p>Note: This method does not assume the input is in fraction form. If you have 12.345
     * and want "12.35%", pass 12.345 directly.</p>
     *
     * <p>Invalid numbers (NaN/Infinite) are treated as 0.00%.</p>
     *
     * @param value percentage value to format
     * @return formatted percentage string like "12.34%"
     */
    public static String formatPercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.00%";
        }
        return TWO_DECIMALS.format(round(value, 2)) + "%";
    }

    /**
     * Format a numeric value as a decimal string with two decimal places.
     *
     * <p>Invalid numbers (NaN/Infinite) are treated as 0.00.</p>
     *
     * @param value decimal value to format
     * @return formatted decimal string like "1,234.56"
     */
    public static String formatDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.00";
        }
        return TWO_DECIMALS.format(round(value, 2));
    }

    /**
     * Format a numeric score with a preference for whole numbers where appropriate.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>NaN/Infinite -> "0"</li>
     *     <li>Round to nearest integer. If the rounded value is mathematically whole, show no decimals.</li>
     *     <li>Otherwise show one decimal place (to reduce noise but preserve precision).</li>
     * </ul>
     *
     * Examples:
     * <pre>
     * formatScore(4.0) -> "4"
     * formatScore(4.24) -> "4.2"
     * formatScore(Double.NaN) -> "0"
     * </pre>
     *
     * @param value score value
     * @return human-friendly formatted score
     */
    public static String formatScore(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }

        // Round to nearest integer to check whether it's whole after rounding.
        double rounded = round(value, 0);
        if (rounded == Math.floor(rounded)) {
            // Exact whole number after rounding -> show no fractional digits.
            return WHOLE_NUMBER.format(rounded);
        }
        // Non-whole numbers -> show one decimal place.
        return ONE_DECIMAL.format(round(value, 1));
    }

    /**
     * Format a long integer with grouping separators (thousands).
     *
     * @param value integer value
     * @return formatted integer string like "1,234"
     */
    public static String formatInteger(long value) {
        return WHOLE_NUMBER.format(value);
    }

    /**
     * Format an int integer with grouping separators (thousands).
     *
     * @param value integer value
     * @return formatted integer string like "1,234"
     */
    public static String formatInteger(int value) {
        return WHOLE_NUMBER.format(value);
    }

    /**
     * Convert milliseconds to a compact seconds string with appropriate precision:
     * <ul>
     *     <li>seconds >= 100 -> no decimals (e.g., "123 sec")</li>
     *     <li>10 <= seconds &lt; 100 -> one decimal (e.g., "12.3 sec")</li>
     *     <li>seconds &lt; 10 -> two decimals (e.g., "1.23 sec")</li>
     * </ul>
     *
     * <p>Negative inputs are clamped to 0.</p>
     *
     * @param milliseconds time in milliseconds
     * @return formatted seconds string appended with " sec"
     */
    public static String formatMillisecondsAsSeconds(long milliseconds) {
        if (milliseconds < 0) {
            milliseconds = 0;
        }

        double seconds = milliseconds / 1000.0;

        if (seconds >= 100) {
            // Large durations: whole seconds only
            return WHOLE_NUMBER.format(round(seconds, 0)) + " sec";
        }

        if (seconds >= 10) {
            // Medium durations: one decimal
            return ONE_DECIMAL.format(round(seconds, 1)) + " sec";
        }

        // Small durations: two decimals for finer granularity
        return TWO_DECIMALS.format(round(seconds, 2)) + " sec";
    }

    /**
     * Provide a detailed, human-readable representation of a millisecond duration.
     *
     * <p>Output format varies depending on magnitude:</p>
     * <ul>
     *     <li>Hours present: "Xh Ym Zs (N ms)"</li>
     *     <li>Minutes present (no hours): "Xm Ys (N ms)"</li>
     *     <li>Total &lt; 1000ms: "N ms"</li>
     *     <li>Exact whole seconds: "S sec (N ms)"</li>
     *     <li>Otherwise: "S.sss sec (N ms)" with three decimals for seconds</li>
     * </ul>
     *
     * <p>Negative inputs are clamped to 0.</p>
     *
     * @param milliseconds time in milliseconds
     * @return human-friendly duration string including an explicit ms total
     */
    public static String formatMillisecondsDetailed(long milliseconds) {
        if (milliseconds < 0) {
            milliseconds = 0;
        }

        long totalMs = milliseconds;
        long totalSeconds = totalMs / 1000;
        long remainingMs = totalMs % 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            // Hours present: include hours, minutes, seconds and the raw ms total for traceability.
            return String.format("%dh %dm %ds (%s ms)",
                    hours, minutes, seconds, WHOLE_NUMBER.format(totalMs));
        }

        if (minutes > 0) {
            // Minutes present but no hours.
            return String.format("%dm %ds (%s ms)",
                    minutes, seconds, WHOLE_NUMBER.format(totalMs));
        }

        if (totalMs < 1000) {
            // Sub-second durations should be presented in milliseconds.
            return WHOLE_NUMBER.format(totalMs) + " ms";
        }

        if (remainingMs == 0) {
            // Exact whole seconds: avoid unnecessary decimals.
            return seconds + " sec (" + WHOLE_NUMBER.format(totalMs) + " ms)";
        }

        // Non-exact seconds: show three decimal seconds for better precision.
        return THREE_DECIMALS.format(totalMs / 1000.0) + " sec (" + WHOLE_NUMBER.format(totalMs) + " ms)";
    }

    /**
     * Format a duration given in seconds into a compact, human-readable string.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>NaN/Infinite/negative -> treated as 0</li>
     *     <li>>= 3600 -> "Xh Ym Zs"</li>
     *     <li>>= 60 -> "Xm Ys"</li>
     *     <li>>= 10 -> one decimal place (e.g., "12.3 sec")</li>
     *     <li>&lt; 10 -> two decimals (e.g., "1.23 sec")</li>
     * </ul>
     *
     * @param seconds duration in seconds (may be fractional)
     * @return formatted duration string
     */
    public static String formatDurationSeconds(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) {
            seconds = 0;
        }

        if (seconds >= 3600) {
            long total = (long) Math.floor(seconds);
            long hours = total / 3600;
            long minutes = (total % 3600) / 60;
            long sec = total % 60;
            return String.format("%dh %dm %ds", hours, minutes, sec);
        }

        if (seconds >= 60) {
            long total = (long) Math.floor(seconds);
            long minutes = total / 60;
            long sec = total % 60;
            return String.format("%dm %ds", minutes, sec);
        }

        if (seconds >= 10) {
            return ONE_DECIMAL.format(round(seconds, 1)) + " sec";
        }

        return TWO_DECIMALS.format(round(seconds, 2)) + " sec";
    }

    /**
     * Format a metric label and its value, displaying "N/A" when the value is missing/empty.
     *
     * @param label the metric label (e.g., "Average Response")
     * @param value the metric value as a string (may be null/empty)
     * @return "label: value" or "label: N/A" when the value is absent
     */
    public static String formatNullableMetric(String label, String value) {
        String cleaned = safeText(value);
        if (cleaned.isEmpty()) {
            return label + ": N/A";
        }
        return label + ": " + cleaned;
    }

    /**
     * Normalize common risk level strings into a predictable display form.
     *
     * <p>Mapping applied (case-insensitive):</p>
     * <ul>
     *     <li>"low" -> "Low"</li>
     *     <li>"medium" -> "Medium"</li>
     *     <li>"high" -> "High"</li>
     *     <li>"critical" -> "Critical"</li>
     *     <li>empty/nil -> "Unknown"</li>
     *     <li>anything else -> returned as cleaned input (trimmed and whitespace-collapsed)</li>
     * </ul>
     *
     * @param riskLevel raw risk level text (may be null)
     * @return normalized risk level suitable for reports
     */
    public static String normalizeRiskLevel(String riskLevel) {
        String cleaned = safeText(riskLevel);
        if (cleaned.isEmpty()) {
            return "Unknown";
        }

        String lower = cleaned.toLowerCase();
        return switch (lower) {
            case "low" -> "Low";
            case "medium" -> "Medium";
            case "high" -> "High";
            case "critical" -> "Critical";
            default -> cleaned; // fallback to cleaned original (preserves any specific labeling)
        };
    }

    /**
     * Normalize execution status strings into an uppercase, underscore-separated token.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>Trim and collapse whitespace</li>
     *     <li>Replace spaces and hyphens with underscores</li>
     *     <li>Convert to upper-case</li>
     *     <li>Empty values -> "UNKNOWN"</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <pre>
     * normalizeExecutionStatus("in progress") -> "IN_PROGRESS"
     * normalizeExecutionStatus("failed-fast") -> "FAILED_FAST"
     * </pre>
     *
     * @param status raw status (may be null)
     * @return normalized status token
     */
    public static String normalizeExecutionStatus(String status) {
        String cleaned = safeText(status);
        if (cleaned.isEmpty()) {
            return "UNKNOWN";
        }

        return cleaned
                .replace(' ', '_')  // convert spaces to underscores
                .replace('-', '_')  // convert hyphens to underscores
                .toUpperCase();     // normalize to upper-case tokens
    }

    /**
     * Returns a safe double value for reporting: NaN or Infinite values are converted to 0.0.
     *
     * @param value input double
     * @return original value if finite, otherwise 0.0
     */
    public static double safeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }

    /**
     * Ensure a long value is non-negative by clamping negative inputs to 0.
     *
     * @param value input long
     * @return value if >= 0, otherwise 0
     */
    public static long safeLong(long value) {
        return Math.max(value, 0L);
    }

    /**
     * Ensure an int value is non-negative by clamping negative inputs to 0.
     *
     * @param value input int
     * @return value if >= 0, otherwise 0
     */
    public static int safeInt(int value) {
        return Math.max(value, 0);
    }

    /**
     * Round a double to the specified number of decimal places using HALF_UP rounding.
     *
     * <p>Internally uses BigDecimal.valueOf to preserve expected decimal rounding behavior.</p>
     *
     * @param value the value to round
     * @param scale number of decimal places to keep (>= 0)
     * @return the rounded value as a double
     */
    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
