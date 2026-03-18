package com.ptaf.performance.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helper class for converting raw performance numbers into business-readable values
 * for Excel reporting and text summaries.
 *
 * <p>Main responsibilities:
 * <ul>
 *   <li>Convert milliseconds into readable seconds text</li>
 *   <li>Format percentages with % sign</li>
 *   <li>Provide consistent decimal rounding</li>
 *   <li>Reduce confusion caused by long raw numeric values</li>
 * </ul>
 * </p>
 */
public final class PerformanceExcelFormatHelper {

    private PerformanceExcelFormatHelper() {
        // Utility class
    }

    /**
     * Converts milliseconds into readable seconds text.
     *
     * <p>Examples:
     * <ul>
     *   <li>25 ms   -> 0.03 sec</li>
     *   <li>250 ms  -> 0.25 sec</li>
     *   <li>1250 ms -> 1.25 sec</li>
     * </ul>
     * </p>
     *
     * @param milliseconds raw milliseconds
     * @return readable seconds text
     */
    public static String formatMillisecondsAsSeconds(long milliseconds) {
        BigDecimal seconds = BigDecimal.valueOf(milliseconds)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        return seconds.stripTrailingZeros().toPlainString() + " sec";
    }

    /**
     * Converts milliseconds into readable seconds text using fixed decimal places.
     *
     * @param milliseconds raw milliseconds
     * @param scale number of decimal places
     * @return readable seconds text
     */
    public static String formatMillisecondsAsSeconds(long milliseconds, int scale) {
        if (scale < 0) {
            scale = 2;
        }

        BigDecimal seconds = BigDecimal.valueOf(milliseconds)
                .divide(BigDecimal.valueOf(1000), scale, RoundingMode.HALF_UP);

        return seconds.toPlainString() + " sec";
    }

    /**
     * Formats raw milliseconds as a display value that shows both seconds and ms.
     *
     * <p>Example:
     * 250 -> 0.25 sec (250 ms)
     * </p>
     *
     * @param milliseconds raw milliseconds
     * @return combined display text
     */
    public static String formatMillisecondsDetailed(long milliseconds) {
        return formatMillisecondsAsSeconds(milliseconds) + " (" + milliseconds + " ms)";
    }

    /**
     * Formats a percent number with % sign using 2 decimal places by default.
     *
     * <p>Examples:
     * <ul>
     *   <li>0      -> 0%</li>
     *   <li>1.5    -> 1.5%</li>
     *   <li>12.345 -> 12.35%</li>
     * </ul>
     * </p>
     *
     * @param percent raw percent number
     * @return formatted percent text
     */
    public static String formatPercent(double percent) {
        BigDecimal value = BigDecimal.valueOf(percent)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        return value.toPlainString() + "%";
    }

    /**
     * Formats a percent number with custom decimal places.
     *
     * @param percent raw percent number
     * @param scale number of decimal places
     * @return formatted percent text
     */
    public static String formatPercent(double percent, int scale) {
        if (scale < 0) {
            scale = 2;
        }

        BigDecimal value = BigDecimal.valueOf(percent)
                .setScale(scale, RoundingMode.HALF_UP);

        return value.toPlainString() + "%";
    }

    /**
     * Formats a score-like decimal number using standard 2 decimal places.
     *
     * @param value raw value
     * @return formatted number
     */
    public static String formatDecimal(double value) {
        BigDecimal formatted = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        return formatted.toPlainString();
    }

    /**
     * Formats an integer or long as plain text.
     *
     * @param value raw value
     * @return plain string
     */
    public static String formatWholeNumber(long value) {
        return String.valueOf(value);
    }

    /**
     * Returns a safe text value for nullable strings.
     *
     * @param value raw value
     * @return non-null safe text
     */
    public static String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}