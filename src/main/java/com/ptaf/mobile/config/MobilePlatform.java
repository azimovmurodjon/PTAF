package com.ptaf.mobile.config;

/**
 * Supported native mobile platforms for the PTAF Appium module.
 *
 * <p>
 * This enum represents the two mobile platforms that the PTAF (Portable Test Automation Framework)
 * module currently supports: ANDROID and IOS. It provides utility methods for:
 * </p>
 * <ul>
 *     <li>Parsing a platform from a user-provided string (from).</li>
 *     <li>Convenience checks to determine if the current instance is Android or iOS.</li>
 * </ul>
 *
 * <p>
 * Notes on parsing behavior:
 * <ul>
 *     <li>Null or empty input defaults to {@link #ANDROID} (backwards-compatible default).</li>
 *     <li>Input is normalized by trimming whitespace, replacing hyphens '-' with underscores '_',
 *     and converting to upper-case before interpretation.</li>
 *     <li>Both "IPHONE" and "IPAD" are treated as {@link #IOS} for convenience.</li>
 *     <li>If the normalized value does not match any known enum constant, {@link java.lang.IllegalArgumentException}
 *     will be thrown by {@link java.lang.Enum#valueOf(Class, String)}.</li>
 * </ul>
 * </p>
 */
public enum MobilePlatform {
    /**
     * Android native platform.
     *
     * <p>Represents devices and capabilities intended for Android OS.</p>
     */
    ANDROID,

    /**
     * iOS native platform.
     *
     * <p>Represents devices and capabilities intended for Apple's iOS (iPhone/iPad).</p>
     */
    IOS;

    /**
     * Convert a string value to the corresponding {@link MobilePlatform} enum.
     *
     * <p>
     * The conversion performs a small amount of normalization so typical inputs are accepted:
     * </p>
     * <ol>
     *     <li>If {@code value} is {@code null} or empty (after trimming) this method returns {@link #ANDROID}
     *     as a default. This preserves backward compatibility with callers that do not specify a platform.</li>
     *     <li>Leading and trailing whitespace are trimmed.</li>
     *     <li>Hyphens ('-') are replaced with underscores ('_') to allow values like "i-phone" or "i-phone"
     *     to be interpreted correctly after normalization.</li>
     *     <li>The resulting string is converted to upper-case to match enum constant naming.</li>
     *     <li>The special names "IPHONE" and "IPAD" are explicitly mapped to {@link #IOS}.</li>
     *     <li>For any other normalized value, {@link java.lang.Enum#valueOf(Class, String)} is used, which
     *     will throw {@link java.lang.IllegalArgumentException} if the value does not match a known constant.</li>
     * </ol>
     *
     * @param value the input string representing the platform; may be null or contain whitespace/hyphens
     * @return the resolved {@link MobilePlatform}; never null
     * @throws java.lang.IllegalArgumentException if the normalized non-empty value does not match a valid platform
     */
    public static MobilePlatform from(String value) {
        // If no value provided, return the safe default (ANDROID) to avoid null-handling in callers.
        if (value == null || value.trim().isEmpty()) {
            return ANDROID;
        }

        // Normalize input:
        // - trim whitespace
        // - replace hyphens with underscores (so "i-phone" -> "i_phone")
        // - convert to upper-case to match enum constant names (ANDROID, IOS)
        String normalized = value.trim().replace("-", "_").toUpperCase();

        // Accept common synonyms for iOS devices explicitly.
        // Treat both "IPHONE" and "IPAD" as IOS for convenience.
        if ("IPHONE".equals(normalized) || "IPAD".equals(normalized)) {
            return IOS;
        }

        // For any other normalized value attempt to map to an enum constant.
        // Note: Enum.valueOf will throw IllegalArgumentException if no match is found.
        return MobilePlatform.valueOf(normalized);
    }

    /**
     * Convenience check to determine if this platform instance represents Android.
     *
     * @return {@code true} if this instance is {@link #ANDROID}, {@code false} otherwise
     */
    public boolean isAndroid() { return this == ANDROID; }

    /**
     * Convenience check to determine if this platform instance represents iOS.
     *
     * @return {@code true} if this instance is {@link #IOS}, {@code false} otherwise
     */
    public boolean isIos() { return this == IOS; }
}
