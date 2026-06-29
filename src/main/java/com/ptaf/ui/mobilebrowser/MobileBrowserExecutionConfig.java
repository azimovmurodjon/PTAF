package com.ptaf.ui.mobilebrowser;

/**
 * Runtime execution controls for Playwright mobile-browser emulation.
 *
 * <p>
 * This utility class centralizes access to configuration properties that control
 * mobile-browser-specific behavior such as evidence collection (screenshots, video),
 * visual regression settings, and general feature toggles. Configuration values are
 * read via {@link MobileBrowserYamlReader#get(String)} using a hierarchical key
 * convention:
 * </p>
 *
 * <ul>
 *   <li>Root prefix: {@code "mobile_browser."}</li>
 *   <li>Evidence settings: {@code "mobile_browser.evidence.*"}</li>
 *   <li>Visual settings: {@code "mobile_browser.visual.*"}</li>
 * </ul>
 *
 * <p>
 * Each accessor method in this class returns either a value from the configuration
 * source or a sensible default when the property is absent or malformed. All methods
 * are static convenience accessors; the class is not instantiable.
 * </p>
 *
 * <p>
 * Note for testers:
 * <ul>
 *   <li>Change configuration values by editing the YAML/properties used by {@code MobileBrowserYamlReader}.</li>
 *   <li>Defaults are provided so tests run with predictable behavior even when no external configuration is supplied.</li>
 * </ul>
 * </p>
 */
public final class MobileBrowserExecutionConfig {
    // Base key used for all mobile browser related configuration values.
    private static final String ROOT = "mobile_browser.";

    // Sub-root for evidence (screenshots, video) related configuration.
    private static final String EVIDENCE_ROOT = ROOT + "evidence.";

    // Sub-root for visual regression related configuration.
    private static final String VISUAL_ROOT = ROOT + "visual.";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>Utility classes should not be instantiated; this enforces that at runtime.</p>
     *
     * @throws IllegalStateException always when called to indicate the class should not be instantiated
     */
    private MobileBrowserExecutionConfig() { throw new IllegalStateException("Utility class"); }

    /**
     * Whether the mobile-browser emulation features are enabled.
     *
     * Key: {@code mobile_browser.enabled}
     * Default: {@code true}
     *
     * @return {@code true} if mobile-browser emulation should be used; {@code false} otherwise
     */
    public static boolean isEnabled() { return bool(ROOT + "enabled", true); }

    /**
     * Returns the orientation mode used for the emulated device.
     *
     * Key: {@code mobile_browser.orientation}
     * Default: {@code "profile"}
     *
     * <p>The returned value is trimmed and converted to lowercase to provide a consistent
     * comparison-friendly form (for example, "PORTRAIT" or "portrait" both become "portrait").</p>
     *
     * @return normalized orientation mode string (lowercase, trimmed)
     */
    public static String getOrientationMode() { return str(ROOT + "orientation", "profile").trim().toLowerCase(); }

    /**
     * Directory where evidence artifacts (screenshots, videos) should be written.
     *
     * Key: {@code mobile_browser.evidence.output_directory}
     * Default: {@code "test-output/mobile-browser-evidence"}
     *
     * @return the configured evidence output directory path
     */
    public static String getEvidenceOutputDirectory() { return str(EVIDENCE_ROOT + "output_directory", "test-output/mobile-browser-evidence"); }

    /**
     * Whether a screenshot should be taken whenever a test scenario fails.
     *
     * Key: {@code mobile_browser.evidence.screenshot_on_failure}
     * Default: {@code true}
     *
     * @return {@code true} to capture screenshots on failure
     */
    public static boolean screenshotOnFailure() { return bool(EVIDENCE_ROOT + "screenshot_on_failure", true); }

    /**
     * Whether a screenshot should be taken when a test scenario passes.
     *
     * Key: {@code mobile_browser.evidence.screenshot_on_pass}
     * Default: {@code false}
     *
     * @return {@code true} to capture screenshots on successful scenarios
     */
    public static boolean screenshotOnPass() { return bool(EVIDENCE_ROOT + "screenshot_on_pass", false); }

    /**
     * Whether a screenshot should be taken after every scenario regardless of outcome.
     *
     * Key: {@code mobile_browser.evidence.screenshot_after_each_scenario}
     * Default: {@code false}
     *
     * @return {@code true} to capture a screenshot after each scenario
     */
    public static boolean screenshotAfterEachScenario() { return bool(EVIDENCE_ROOT + "screenshot_after_each_scenario", false); }

    /**
     * Whether captured screenshots (evidence) should be attached to the test report.
     *
     * Key: {@code mobile_browser.evidence.attach_screenshots_to_report}
     * Default: {@code true}
     *
     * @return {@code true} to attach screenshots to the report
     */
    public static boolean attachScreenshotsToReport() { return bool(EVIDENCE_ROOT + "attach_screenshots_to_report", true); }

    /**
     * Whether video recording is enabled for scenarios.
     *
     * Key: {@code mobile_browser.evidence.video_recording_enabled}
     * Default: {@code false}
     *
     * @return {@code true} to enable video recording
     */
    public static boolean videoRecordingEnabled() { return bool(EVIDENCE_ROOT + "video_recording_enabled", false); }

    /**
     * Video recording width in pixels.
     *
     * Key: {@code mobile_browser.evidence.video_size_width}
     * Default: {@code 390}
     *
     * @return configured video width (pixels); returns default if not set or not a valid integer
     */
    public static int getVideoSizeWidth() { return integer(EVIDENCE_ROOT + "video_size_width", 390); }

    /**
     * Video recording height in pixels.
     *
     * Key: {@code mobile_browser.evidence.video_size_height}
     * Default: {@code 844}
     *
     * @return configured video height (pixels); returns default if not set or not a valid integer
     */
    public static int getVideoSizeHeight() { return integer(EVIDENCE_ROOT + "video_size_height", 844); }

    /**
     * Whether visual regression checks are enabled.
     *
     * Key: {@code mobile_browser.visual.enabled}
     * Default: {@code true}
     *
     * @return {@code true} to perform visual comparisons against baselines
     */
    public static boolean visualEnabled() { return bool(VISUAL_ROOT + "enabled", true); }

    /**
     * Directory that contains baseline images used for visual comparisons.
     *
     * Key: {@code mobile_browser.visual.baseline_directory}
     * Default: {@code "src/test/resources/baselines/mobile_browser"}
     *
     * @return the path to the visual baseline directory
     */
    public static String getVisualBaselineDirectory() { return str(VISUAL_ROOT + "baseline_directory", "src/test/resources/baselines/mobile_browser"); }

    /**
     * Directory where visual comparison output (diffs, artifacts) should be written.
     *
     * Key: {@code mobile_browser.visual.output_directory}
     * Default: {@code "test-output/mobile-browser-visual"}
     *
     * @return the path to the visual output directory
     */
    public static String getVisualOutputDirectory() { return str(VISUAL_ROOT + "output_directory", "test-output/mobile-browser-visual"); }

    /**
     * Threshold for visual mismatch expressed as a percentage.
     *
     * Key: {@code mobile_browser.visual.mismatch_threshold_percent}
     * Default: {@code 0.10} (i.e. 10%)
     *
     * <p>
     * Interpreting the value: typical visual comparison libraries accept a percentage/proportion
     * indicating how much difference is allowable before a test is considered failing. The default
     * is ten percent, represented here as {@code 0.10}. If your visual diff tool expects a value
     * between 0 and 1, use this directly; otherwise convert as needed.
     * </p>
     *
     * @return mismatch threshold as a double (default 0.10)
     */
    public static double getVisualMismatchThresholdPercent() { return dbl(VISUAL_ROOT + "mismatch_threshold_percent", 0.10); }

    /**
     * Whether a missing visual baseline image should be created automatically.
     *
     * Key: {@code mobile_browser.visual.create_baseline_if_missing}
     * Default: {@code true}
     *
     * @return {@code true} to create baseline images when missing
     */
    public static boolean createBaselineIfMissing() { return bool(VISUAL_ROOT + "create_baseline_if_missing", true); }

    /**
     * Whether visual artifacts (diff images, comparison outputs) should be attached to the report.
     *
     * Key: {@code mobile_browser.visual.attach_artifacts_to_report}
     * Default: {@code true}
     *
     * @return {@code true} to attach visual artifacts to the report
     */
    public static boolean attachVisualArtifactsToReport() { return bool(VISUAL_ROOT + "attach_artifacts_to_report", true); }

    /**
     * Helper to read a string configuration value.
     *
     * <p>
     * If the underlying reader returns {@code null} the provided {@code defaultValue} is returned.
     * Otherwise the value is converted to a {@link String} via {@code String.valueOf(Object)}.
     * </p>
     *
     * @param key the configuration key to read
     * @param defaultValue the fallback value when the key is absent
     * @return the configured string or the default if absent
     */
    private static String str(String key, String defaultValue) { Object value = MobileBrowserYamlReader.get(key); return value == null ? defaultValue : String.valueOf(value); }

    /**
     * Helper to read a boolean configuration value.
     *
     * <p>
     * If the underlying reader returns {@code null} the provided {@code defaultValue} is returned.
     * Otherwise {@link Boolean#parseBoolean(String)} is used which treats any case-insensitive
     * string equal to "true" as {@code true}; everything else yields {@code false}.
     * </p>
     *
     * @param key the configuration key to read
     * @param defaultValue the fallback boolean when the key is absent
     * @return parsed boolean or default when absent
     */
    private static boolean bool(String key, boolean defaultValue) { Object value = MobileBrowserYamlReader.get(key); return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value)); }

    /**
     * Helper to read an integer configuration value.
     *
     * <p>
     * If the underlying reader returns {@code null} the provided {@code defaultValue} is returned.
     * If the value is present but not a valid integer (NumberFormatException), the {@code defaultValue}
     * is also returned to preserve stability of the calling code.
     * </p>
     *
     * @param key the configuration key to read
     * @param defaultValue the fallback integer when the key is absent or invalid
     * @return parsed integer or default when absent/invalid
     */
    private static int integer(String key, int defaultValue) { Object value = MobileBrowserYamlReader.get(key); if (value == null) return defaultValue; try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return defaultValue; } }

    /**
     * Helper to read a double configuration value.
     *
     * <p>
     * If the underlying reader returns {@code null} the provided {@code defaultValue} is returned.
     * If the value is present but cannot be parsed as a double (NumberFormatException), the {@code defaultValue}
     * is returned to avoid throwing during configuration reads.
     * </p>
     *
     * @param key the configuration key to read
     * @param defaultValue the fallback double when the key is absent or invalid
     * @return parsed double or default when absent/invalid
     */
    private static double dbl(String key, double defaultValue) { Object value = MobileBrowserYamlReader.get(key); if (value == null) return defaultValue; try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException e) { return defaultValue; } }
}
