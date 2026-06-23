package com.ptaf.mobile.config;

/**
 * Central configuration access for Appium native mobile automation.
 */
public final class MobileConfigurationProperties {
    private static final String ROOT = "mobile.";
    private static final String EVIDENCE_ROOT = ROOT + "evidence.";
    private static final String PERMISSIONS_ROOT = ROOT + "permissions.";
    private static final String BROWSER_ROOT = ROOT + "browser.";
    private static final String APPIUM_BROWSER_ROOT = "mobile_browser_appium.";

    private MobileConfigurationProperties() { throw new IllegalStateException("Utility class"); }

    public static boolean isEnabled() { return MobileYamlReader.getBoolean(ROOT + "enabled", true); }
    public static String getAppiumServerUrl() { return MobileYamlReader.getString(ROOT + "appium_server_url", "http://127.0.0.1:4723"); }
    public static MobilePlatform getDefaultPlatform() { return MobilePlatform.from(MobileYamlReader.getString(ROOT + "default_platform", "android")); }
    public static int getExplicitWaitSeconds() { return MobileYamlReader.getInt(ROOT + "explicit_wait_seconds", 30); }
    public static int getImplicitWaitSeconds() { return MobileYamlReader.getInt(ROOT + "implicit_wait_seconds", 0); }
    public static int getNewCommandTimeoutSeconds() { return MobileYamlReader.getInt(ROOT + "new_command_timeout_seconds", 120); }

    public static String getCapability(MobilePlatform platform, String capabilityName, String defaultValue) {
        return MobileYamlReader.getString(ROOT + platform.name().toLowerCase() + "." + capabilityName, defaultValue);
    }

    public static boolean getCapabilityBoolean(MobilePlatform platform, String capabilityName, boolean defaultValue) {
        return MobileYamlReader.getBoolean(ROOT + platform.name().toLowerCase() + "." + capabilityName, defaultValue);
    }

    public static String getOrientation(MobilePlatform platform) {
        return getCapability(platform, "orientation", "portrait").trim().toUpperCase();
    }

    public static String getEvidenceOutputDirectory() { return MobileYamlReader.getString(EVIDENCE_ROOT + "output_directory", "test-output/mobile-evidence"); }
    public static boolean screenshotOnFailure() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "screenshot_on_failure", true); }
    public static boolean screenshotOnPass() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "screenshot_on_pass", false); }
    public static boolean screenshotAfterEachScenario() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "screenshot_after_each_scenario", false); }
    public static boolean attachScreenshotsToReport() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "attach_screenshots_to_report", true); }
    public static boolean videoRecordingEnabled() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "video_recording_enabled", false); }
    public static boolean videoOnFailureOnly() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "video_on_failure_only", false); }
    public static boolean attachVideoToReport() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "attach_video_to_report", false); }

    /** Timeout used only for optional permission/system-dialog checks. Keep short so tests do not wait unnecessarily when no popup is present. */
    public static int getPermissionPopupTimeoutSeconds() { return MobileYamlReader.getInt(PERMISSIONS_ROOT + "popup_timeout_seconds", 3); }

    /** Maximum number of permission dialogs that the framework should handle in one loop. Prevents accidental infinite loops. */
    public static int getPermissionMaxPopups() { return MobileYamlReader.getInt(PERMISSIONS_ROOT + "max_popups_to_handle", 5); }

    /** When true, explicit permission handling steps capture before/after evidence and attach it to the Cucumber report. */
    public static boolean capturePermissionEvidence() { return MobileYamlReader.getBoolean(PERMISSIONS_ROOT + "capture_evidence", true); }

    /** Enables Appium real mobile browser sessions for Android Chrome or iOS Safari. Native app mode remains the default. */
    public static boolean isBrowserModeEnabled() {
        Object explicitSplitValue = MobileYamlReader.get(APPIUM_BROWSER_ROOT + "enabled");
        if (explicitSplitValue != null) return Boolean.parseBoolean(String.valueOf(explicitSplitValue));
        return MobileYamlReader.getBoolean(BROWSER_ROOT + "enabled", false);
    }

    /**
     * Returns browser capability values from the enterprise split config first:
     * {@code mobile-browser-config.yml -> mobile_browser_appium.android/ios}.
     * It falls back to the legacy nested block {@code mobile.browser.android/ios}
     * so existing teams are not broken while moving to separated configuration files.
     */
    public static String getBrowserCapability(MobilePlatform platform, String capabilityName, String defaultValue) {
        String platformName = platform.name().toLowerCase();
        Object splitValue = MobileYamlReader.get(APPIUM_BROWSER_ROOT + platformName + "." + capabilityName);
        if (splitValue != null) return String.valueOf(splitValue);
        return MobileYamlReader.getString(BROWSER_ROOT + platformName + "." + capabilityName, defaultValue);
    }

    /** Returns boolean browser capability values with the same split-config-first fallback behavior. */
    public static boolean getBrowserCapabilityBoolean(MobilePlatform platform, String capabilityName, boolean defaultValue) {
        String platformName = platform.name().toLowerCase();
        Object splitValue = MobileYamlReader.get(APPIUM_BROWSER_ROOT + platformName + "." + capabilityName);
        if (splitValue != null) return Boolean.parseBoolean(String.valueOf(splitValue));
        return MobileYamlReader.getBoolean(BROWSER_ROOT + platformName + "." + capabilityName, defaultValue);
    }
}
