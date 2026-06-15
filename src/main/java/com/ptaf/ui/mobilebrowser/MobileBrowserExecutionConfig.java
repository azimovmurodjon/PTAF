package com.ptaf.ui.mobilebrowser;

/** Runtime execution controls for Playwright mobile-browser emulation. */
public final class MobileBrowserExecutionConfig {
    private static final String ROOT = "mobile_browser.";
    private static final String EVIDENCE_ROOT = ROOT + "evidence.";
    private static final String VISUAL_ROOT = ROOT + "visual.";

    private MobileBrowserExecutionConfig() { throw new IllegalStateException("Utility class"); }

    public static boolean isEnabled() { return bool(ROOT + "enabled", true); }
    public static String getOrientationMode() { return str(ROOT + "orientation", "profile").trim().toLowerCase(); }
    public static String getEvidenceOutputDirectory() { return str(EVIDENCE_ROOT + "output_directory", "test-output/mobile-browser-evidence"); }
    public static boolean screenshotOnFailure() { return bool(EVIDENCE_ROOT + "screenshot_on_failure", true); }
    public static boolean screenshotOnPass() { return bool(EVIDENCE_ROOT + "screenshot_on_pass", false); }
    public static boolean screenshotAfterEachScenario() { return bool(EVIDENCE_ROOT + "screenshot_after_each_scenario", false); }
    public static boolean attachScreenshotsToReport() { return bool(EVIDENCE_ROOT + "attach_screenshots_to_report", true); }
    public static boolean videoRecordingEnabled() { return bool(EVIDENCE_ROOT + "video_recording_enabled", false); }
    public static int getVideoSizeWidth() { return integer(EVIDENCE_ROOT + "video_size_width", 390); }
    public static int getVideoSizeHeight() { return integer(EVIDENCE_ROOT + "video_size_height", 844); }

    public static boolean visualEnabled() { return bool(VISUAL_ROOT + "enabled", true); }
    public static String getVisualBaselineDirectory() { return str(VISUAL_ROOT + "baseline_directory", "src/test/resources/baselines/mobile_browser"); }
    public static String getVisualOutputDirectory() { return str(VISUAL_ROOT + "output_directory", "test-output/mobile-browser-visual"); }
    public static double getVisualMismatchThresholdPercent() { return dbl(VISUAL_ROOT + "mismatch_threshold_percent", 0.10); }
    public static boolean createBaselineIfMissing() { return bool(VISUAL_ROOT + "create_baseline_if_missing", true); }
    public static boolean attachVisualArtifactsToReport() { return bool(VISUAL_ROOT + "attach_artifacts_to_report", true); }

    private static String str(String key, String defaultValue) { Object value = MobileBrowserYamlReader.get(key); return value == null ? defaultValue : String.valueOf(value); }
    private static boolean bool(String key, boolean defaultValue) { Object value = MobileBrowserYamlReader.get(key); return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value)); }
    private static int integer(String key, int defaultValue) { Object value = MobileBrowserYamlReader.get(key); if (value == null) return defaultValue; try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return defaultValue; } }
    private static double dbl(String key, double defaultValue) { Object value = MobileBrowserYamlReader.get(key); if (value == null) return defaultValue; try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException e) { return defaultValue; } }
}
