package com.ptaf.ui_performance.reporting;

import java.util.Locale;

/**
 * Formats raw millisecond measurements for human-facing UI performance reports.
 *
 * <p>Machine-readable files retain raw {@code *_ms} fields. This formatter is used only
 * where a tester is reading a report, so a journey measured as {@code 23383 ms} appears as
 * {@code 23.383 s} instead.</p>
 */
public final class UiPerformanceDurationFormatter {

    private UiPerformanceDurationFormatter() {
        // Utility class.
    }

    /**
     * Returns an easily scannable duration using milliseconds below one second, seconds below
     * one minute, and minute/hour components for longer operations.
     *
     * @param milliseconds duration measured by the engine; negative values are treated as zero
     * @return a human-readable duration such as {@code 45 ms}, {@code 23.383 s}, or {@code 1 min 2.500 s}
     */
    public static String format(long milliseconds) {
        long safeMilliseconds = Math.max(0L, milliseconds);
        if (safeMilliseconds < 1_000L) {
            return safeMilliseconds + " ms";
        }

        long totalSeconds = safeMilliseconds / 1_000L;
        long remainingMilliseconds = safeMilliseconds % 1_000L;
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        String secondsText = remainingMilliseconds == 0L
                ? seconds + " s"
                : String.format(Locale.ROOT, "%d.%03d s", seconds, remainingMilliseconds);

        if (hours > 0L) {
            return hours + " h " + minutes + " min " + secondsText;
        }
        if (minutes > 0L) {
            return minutes + " min " + secondsText;
        }
        return secondsText;
    }
}
