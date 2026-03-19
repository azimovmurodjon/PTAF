package com.ptaf.performance.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Central formatting helper for Excel performance reporting.
 *
 * Goals:
 * - Keep formatting business-friendly and consistent
 * - Avoid noisy decimal precision
 * - Preserve reusability across all report sheets
 * - Remain null-safe / enterprise-safe
 */
public final class PerformanceExcelFormatHelper {

    private static final DecimalFormat WHOLE_NUMBER = new DecimalFormat("#,##0");
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#,##0.0");
    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("#,##0.00");
    private static final DecimalFormat THREE_DECIMALS = new DecimalFormat("#,##0.000");

    private PerformanceExcelFormatHelper() {
        // utility class
    }

    public static String safeText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        return normalized
                .replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s{2,}", " ");
    }

    public static String safeOrDefault(String value, String defaultValue) {
        String normalized = safeText(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    public static String formatPercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.00%";
        }
        return TWO_DECIMALS.format(round(value, 2)) + "%";
    }

    public static String formatDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.00";
        }
        return TWO_DECIMALS.format(round(value, 2));
    }

    public static String formatScore(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }

        double rounded = round(value, 0);
        if (rounded == Math.floor(rounded)) {
            return WHOLE_NUMBER.format(rounded);
        }
        return ONE_DECIMAL.format(round(value, 1));
    }

    public static String formatInteger(long value) {
        return WHOLE_NUMBER.format(value);
    }

    public static String formatInteger(int value) {
        return WHOLE_NUMBER.format(value);
    }

    public static String formatMillisecondsAsSeconds(long milliseconds) {
        if (milliseconds < 0) {
            milliseconds = 0;
        }

        double seconds = milliseconds / 1000.0;

        if (seconds >= 100) {
            return WHOLE_NUMBER.format(round(seconds, 0)) + " sec";
        }

        if (seconds >= 10) {
            return ONE_DECIMAL.format(round(seconds, 1)) + " sec";
        }

        return TWO_DECIMALS.format(round(seconds, 2)) + " sec";
    }

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
            return String.format("%dh %dm %ds (%s ms)",
                    hours, minutes, seconds, WHOLE_NUMBER.format(totalMs));
        }

        if (minutes > 0) {
            return String.format("%dm %ds (%s ms)",
                    minutes, seconds, WHOLE_NUMBER.format(totalMs));
        }

        if (totalMs < 1000) {
            return WHOLE_NUMBER.format(totalMs) + " ms";
        }

        if (remainingMs == 0) {
            return seconds + " sec (" + WHOLE_NUMBER.format(totalMs) + " ms)";
        }

        return THREE_DECIMALS.format(totalMs / 1000.0) + " sec (" + WHOLE_NUMBER.format(totalMs) + " ms)";
    }

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

    public static String formatNullableMetric(String label, String value) {
        String cleaned = safeText(value);
        if (cleaned.isEmpty()) {
            return label + ": N/A";
        }
        return label + ": " + cleaned;
    }

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
            default -> cleaned;
        };
    }

    public static String normalizeExecutionStatus(String status) {
        String cleaned = safeText(status);
        if (cleaned.isEmpty()) {
            return "UNKNOWN";
        }

        return cleaned
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase();
    }

    public static double safeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }

    public static long safeLong(long value) {
        return Math.max(value, 0L);
    }

    public static int safeInt(int value) {
        return Math.max(value, 0);
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }
}