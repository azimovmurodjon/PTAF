package com.ptaf.mobile.config;

/**
 * Supported native mobile platforms for the PTAF Appium module.
 */
public enum MobilePlatform {
    ANDROID,
    IOS;

    public static MobilePlatform from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ANDROID;
        }
        String normalized = value.trim().replace("-", "_").toUpperCase();
        if ("IPHONE".equals(normalized) || "IPAD".equals(normalized)) {
            return IOS;
        }
        return MobilePlatform.valueOf(normalized);
    }

    public boolean isAndroid() { return this == ANDROID; }
    public boolean isIos() { return this == IOS; }
}
