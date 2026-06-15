package com.ptaf.mobile.config;

/**
 * Central configuration access for Appium native mobile automation.
 */
public final class MobileConfigurationProperties {
    private static final String ROOT = "mobile.";
    private static final String EVIDENCE_ROOT = ROOT + "evidence.";

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
}
