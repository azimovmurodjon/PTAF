package com.ptaf.mobile.config;

/**
 * Central configuration access for Appium native mobile automation.
 *
 * <p>This utility class provides typed accessors for mobile-related configuration values
 * read via MobileYamlReader. All values are read lazily on demand and have sensible
 * defaults so tests can run without an explicit configuration file.</p>
 *
 * <p>Configuration keys are organized under a few logical roots:
 * <ul>
 *   <li>{@code mobile.*} - general mobile test settings</li>
 *   <li>{@code mobile.evidence.*} - screenshot / video evidence settings</li>
 *   <li>{@code mobile.permissions.*} - automated permission dialog handling</li>
 *   <li>{@code mobile.browser.*} and {@code mobile_browser_appium.*} - browser-mode specific settings</li>
 * </ul>
 *
 * <p>Do not instantiate – this is a pure static utility holder.</p>
 */
public final class MobileConfigurationProperties {
    // Root key for all mobile-specific configuration entries
    private static final String ROOT = "mobile.";

    // Nested root for evidence-related settings (screenshots, video, attachments)
    private static final String EVIDENCE_ROOT = ROOT + "evidence.";

    // Nested root for permission-handling settings (system permission dialogs)
    private static final String PERMISSIONS_ROOT = ROOT + "permissions.";

    // Legacy/legacy-nested browser configuration root (kept for backward compatibility)
    private static final String BROWSER_ROOT = ROOT + "browser.";

    // New split/enterprise configuration root for Appium browser sessions (preferred when present)
    private static final String APPIUM_BROWSER_ROOT = "mobile_browser_appium.";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>All members of this class are static; creating an instance indicates a programming error.</p>
     */
    private MobileConfigurationProperties() { throw new IllegalStateException("Utility class"); }

    /**
     * Whether mobile automation is enabled at all.
     *
     * @return true if mobile automation should be run; default is true.
     */
    public static boolean isEnabled() { return MobileYamlReader.getBoolean(ROOT + "enabled", true); }

    /**
     * The Appium server URL that tests should connect to.
     *
     * @return the configured Appium server URL or {@code "http://127.0.0.1:4723"} by default.
     */
    public static String getAppiumServerUrl() { return MobileYamlReader.getString(ROOT + "appium_server_url", "http://127.0.0.1:4723"); }

    /**
     * The default mobile platform to use when none is explicitly specified.
     *
     * <p>Returned as a {@link MobilePlatform} enum. The underlying configuration is a string
     * (for example "android" or "ios") and is resolved via {@link MobilePlatform#from(String)}.</p>
     *
     * @return the default platform; defaults to Android ("android").
     */
    public static MobilePlatform getDefaultPlatform() { return MobilePlatform.from(MobileYamlReader.getString(ROOT + "default_platform", "android")); }

    /**
     * Default explicit wait timeout used by the framework when waiting for elements/conditions.
     *
     * @return number of seconds to use for explicit waits; default is 30 seconds.
     */
    public static int getExplicitWaitSeconds() { return MobileYamlReader.getInt(ROOT + "explicit_wait_seconds", 30); }

    /**
     * Default implicit wait timeout configured on the driver.
     *
     * <p>Many teams prefer 0 implicit wait and rely on explicit waits instead; default is 0.</p>
     *
     * @return implicit wait timeout in seconds; default is 0.
     */
    public static int getImplicitWaitSeconds() { return MobileYamlReader.getInt(ROOT + "implicit_wait_seconds", 0); }

    /**
     * New command timeout passed to the Appium driver to determine how long the session will
     * remain alive without new commands.
     *
     * @return new command timeout in seconds; default is 120.
     */
    public static int getNewCommandTimeoutSeconds() { return MobileYamlReader.getInt(ROOT + "new_command_timeout_seconds", 120); }

    /**
     * Fetches a string capability value for a given platform from the mobile configuration.
     *
     * <p>This reads from keys under {@code mobile.{platform}.{capabilityName}}. The platform
     * enum name is converted to lower-case (e.g. {@code MobilePlatform.ANDROID} -> "android").</p>
     *
     * @param platform the platform to read the capability for (android / ios)
     * @param capabilityName the capability key name to retrieve
     * @param defaultValue the default value to return if the capability is not present
     * @return the configured capability string or the provided default when absent
     */
    public static String getCapability(MobilePlatform platform, String capabilityName, String defaultValue) {
        return MobileYamlReader.getString(ROOT + platform.name().toLowerCase() + "." + capabilityName, defaultValue);
    }

    /**
     * Fetches a boolean capability value for a given platform from the mobile configuration.
     *
     * <p>Same lookup logic as {@link #getCapability(MobilePlatform, String, String)} but returns
     * a boolean.</p>
     *
     * @param platform the platform to read the capability for
     * @param capabilityName the capability key name
     * @param defaultValue the default boolean to return when not configured
     * @return the configured boolean value or the provided default when absent
     */
    public static boolean getCapabilityBoolean(MobilePlatform platform, String capabilityName, boolean defaultValue) {
        return MobileYamlReader.getBoolean(ROOT + platform.name().toLowerCase() + "." + capabilityName, defaultValue);
    }

    /**
     * Retrieves the desired screen orientation for the given platform.
     *
     * <p>Value is normalized by trimming whitespace and converting to upper-case so it is
     * suitable for direct usage with Appium/driver APIs that expect uppercase orientation names.</p>
     *
     * @param platform the target platform
     * @return orientation string (e.g. "PORTRAIT" or "LANDSCAPE"); defaults to "PORTRAIT"
     */
    public static String getOrientation(MobilePlatform platform) {
        return getCapability(platform, "orientation", "portrait").trim().toUpperCase();
    }

    /**
     * Directory to write mobile evidence (screenshots, videos) into.
     *
     * @return configured evidence output directory; default is "test-output/mobile-evidence".
     */
    public static String getEvidenceOutputDirectory() { return MobileYamlReader.getString(EVIDENCE_ROOT + "output_directory", "test-output/mobile-evidence"); }

    /**
     * Whether to take a screenshot automatically when a test fails.
     *
     * @return true to capture screenshots on failure; default is true.
     */
    public static boolean screenshotOnFailure() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "screenshot_on_failure", true); }

    /**
     * Whether to take a screenshot automatically when a test passes.
     *
     * @return true to capture screenshots on successful tests; default is false.
     */
    public static boolean screenshotOnPass() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "screenshot_on_pass", false); }

    /**
     * Whether to take a screenshot after each scenario (Cucumber).
     *
     * @return true to capture screenshots after every scenario; default is false.
     */
    public static boolean screenshotAfterEachScenario() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "screenshot_after_each_scenario", false); }

    /**
     * Whether to attach captured screenshots to the test report.
     *
     * @return true to attach screenshots to report; default is true.
     */
    public static boolean attachScreenshotsToReport() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "attach_screenshots_to_report", true); }

    /**
     * Whether video recording is enabled for mobile test sessions.
     *
     * @return true if video recording should be used; default is false.
     */
    public static boolean videoRecordingEnabled() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "video_recording_enabled", false); }

    /**
     * Whether video recording should only be saved when a test fails.
     *
     * @return true to record video only on failure; default is false.
     */
    public static boolean videoOnFailureOnly() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "video_on_failure_only", false); }

    /**
     * Whether captured video should be attached to the test report.
     *
     * @return true to attach video to report; default is false.
     */
    public static boolean attachVideoToReport() { return MobileYamlReader.getBoolean(EVIDENCE_ROOT + "attach_video_to_report", false); }

    /**
     * Timeout used only for optional permission/system-dialog checks.
     *
     * <p>This timeout is intentionally short so that when no popup is present the tests do not
     * wait unnecessarily. It controls how long the permission-checking helper waits for a popup
     * before proceeding.</p>
     *
     * @return timeout in seconds for detecting permission popups; default is 3 seconds.
     */
    public static int getPermissionPopupTimeoutSeconds() { return MobileYamlReader.getInt(PERMISSIONS_ROOT + "popup_timeout_seconds", 3); }

    /**
     * Maximum number of permission dialogs that the framework should attempt to handle in a single loop.
     *
     * <p>Protects against accidental infinite loops if a screen repeatedly shows permission dialogs.</p>
     *
     * @return maximum number of popups to handle; default is 5.
     */
    public static int getPermissionMaxPopups() { return MobileYamlReader.getInt(PERMISSIONS_ROOT + "max_popups_to_handle", 5); }

    /**
     * When true, explicit permission handling steps capture before/after evidence and attach it to the Cucumber report.
     *
     * @return true to capture/attach permission handling evidence; default is true.
     */
    public static boolean capturePermissionEvidence() { return MobileYamlReader.getBoolean(PERMISSIONS_ROOT + "capture_evidence", true); }

    /**
     * Determines whether Appium should run in real mobile browser mode for browser tests.
     *
     * <p>There are two possible places where this flag may be configured:
     * <ol>
     *   <li>The new enterprise split config: {@code mobile_browser_appium.enabled}</li>
     *   <li>The legacy nested block: {@code mobile.browser.enabled}</li>
     * </ol>
     *
     * <p>The method first attempts to read the explicit split value (using {@link MobileYamlReader#get})
     * so the code can detect whether the setting was provided at all (null vs present). If a value is
     * present in the split config it is used. Otherwise the legacy {@code mobile.browser.enabled} key
     * is used as a fallback.</p>
     *
     * @return true when browser mode is enabled; default fallback is false.
     */
    public static boolean isBrowserModeEnabled() {
        // Try the new split/enterprise config first; get(...) returns null when absent.
        Object explicitSplitValue = MobileYamlReader.get(APPIUM_BROWSER_ROOT + "enabled");
        if (explicitSplitValue != null) return Boolean.parseBoolean(String.valueOf(explicitSplitValue));
        // Fall back to the legacy nested mobile.browser.enabled key with a default of false.
        return MobileYamlReader.getBoolean(BROWSER_ROOT + "enabled", false);
    }

    /**
     * Returns browser capability values with a split-config-first lookup strategy.
     *
     * <p>Lookup order:
     * <ol>
     *   <li>Enterprise split config: {@code mobile_browser_appium.{platform}.{capability}}</li>
     *   <li>Legacy nested config: {@code mobile.browser.{platform}.{capability}}</li>
     * </ol>
     *
     * <p>This ensures teams moving to the separated configuration file are supported while
     * keeping backward compatibility with existing nested configuration.</p>
     *
     * @param platform the platform (android/ios) to read the capability for
     * @param capabilityName the capability name
     * @param defaultValue value to return when no configuration is present in either location
     * @return the configured capability value or the provided default when absent
     */
    public static String getBrowserCapability(MobilePlatform platform, String capabilityName, String defaultValue) {
        String platformName = platform.name().toLowerCase();
        // Check the new split config location first; get(...) returns null if not defined.
        Object splitValue = MobileYamlReader.get(APPIUM_BROWSER_ROOT + platformName + "." + capabilityName);
        if (splitValue != null) return String.valueOf(splitValue);
        // Fall back to the legacy nested browser config
        return MobileYamlReader.getString(BROWSER_ROOT + platformName + "." + capabilityName, defaultValue);
    }

    /**
     * Returns boolean browser capability values with the same split-config-first fallback behavior as {@link #getBrowserCapability(MobilePlatform, String, String)}.
     *
     * @param platform the platform to read the boolean capability for
     * @param capabilityName the capability key name
     * @param defaultValue default boolean when not configured
     * @return the configured boolean value or the default
     */
    public static boolean getBrowserCapabilityBoolean(MobilePlatform platform, String capabilityName, boolean defaultValue) {
        String platformName = platform.name().toLowerCase();
        // Check the new split config first
        Object splitValue = MobileYamlReader.get(APPIUM_BROWSER_ROOT + platformName + "." + capabilityName);
        if (splitValue != null) return Boolean.parseBoolean(String.valueOf(splitValue));
        // Fall back to the legacy nested config
        return MobileYamlReader.getBoolean(BROWSER_ROOT + platformName + "." + capabilityName, defaultValue);
    }
}
