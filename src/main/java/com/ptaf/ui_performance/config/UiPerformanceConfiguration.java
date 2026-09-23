package com.ptaf.ui_performance.config;

import com.ptaf.ui_performance.model.UiPerformanceExecutionPlan;
import com.ptaf.ui_performance.model.UiPerformanceRunProfile;
import com.ptaf.ui_performance.model.UiPerformanceStage;
import com.ptaf.ui_performance.model.UiPerformanceTestType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Typed configuration accessors for the isolated UI performance module.
 *
 * <p>Target, load shape, browser mode, limits, evidence, and reporting are read exclusively from
 * {@code ui_performance/config/ui_performance-config.yml}. Cucumber only describes the journey and
 * triggers the selected profile.</p>
 */
public final class UiPerformanceConfiguration {
    private static final String ROOT = "ui_performance.";

    private UiPerformanceConfiguration() {
        throw new IllegalStateException("Utility class");
    }

    /** Returns true only when a tester explicitly enables UI performance execution. */
    public static boolean isEnabled() {
        return UiPerformanceYamlReader.getBoolean(ROOT + "enabled", false);
    }

    /** Builds the target from separate protocol, bare host, port, and optional base path. */
    public static String getConfiguredTargetUrl() {
        String protocol = UiPerformanceYamlReader.getString(ROOT + "target.protocol", "https").toLowerCase(Locale.ROOT);
        String host = UiPerformanceYamlReader.getString(ROOT + "target.host", "");
        int port = UiPerformanceYamlReader.getInt(ROOT + "target.port", defaultPort(protocol));
        String basePath = UiPerformanceYamlReader.getString(ROOT + "target.base_path", "");

        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new IllegalArgumentException("UI performance target protocol must be http or https.");
        }
        if (host.isBlank() || host.contains("://") || host.contains("/") || host.contains(":")) {
            throw new IllegalArgumentException("UI performance target host must be a bare host name without protocol, port, or path.");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("UI performance target port must be between 1 and 65535.");
        }

        String normalizedBasePath = normalizePath(basePath, "UI performance target base_path");
        String portSegment = port == defaultPort(protocol) ? "" : ":" + port;
        return protocol + "://" + host.trim() + portSegment + normalizedBasePath;
    }

    /** Returns a clean relative route from the dedicated target.routes map. */
    public static String getConfiguredRoute(String routeName) {
        if (routeName == null || routeName.isBlank()) {
            throw new IllegalArgumentException("UI performance route name cannot be blank.");
        }
        String configured = UiPerformanceYamlReader.getString(ROOT + "target.routes." + routeName.trim(), "");
        if (configured.isBlank()) {
            throw new IllegalArgumentException("UI performance configured route was not found: " + routeName);
        }
        if (configured.startsWith("http://") || configured.startsWith("https://")) {
            throw new IllegalArgumentException("UI performance configured route must be relative. Configure protocol and host under ui_performance.target.");
        }
        return normalizePath(configured, "UI performance configured route " + routeName);
    }

    /** Returns only the non-sensitive host for report metadata. */
    public static String getConfiguredTargetHost() {
        try {
            return URI.create(getConfiguredTargetUrl()).getHost();
        } catch (Exception exception) {
            throw new IllegalStateException("UI performance target configuration is invalid.", exception);
        }
    }

    /** Reads and validates the named load, stress, spike, or soak profile selected in YAML. */
    public static UiPerformanceExecutionPlan getExecutionPlan() {
        String profileName = UiPerformanceYamlReader.getString(ROOT + "active_profile", "load");
        String profileRoot = ROOT + "profiles." + profileName + ".";
        UiPerformanceTestType type = UiPerformanceTestType.fromConfig(
                UiPerformanceYamlReader.getString(profileRoot + "type", profileName));
        List<?> rawStages = UiPerformanceYamlReader.getList(profileRoot + "stages");
        List<UiPerformanceStage> stages = new ArrayList<>();
        for (int index = 0; index < rawStages.size(); index++) {
            Object rawStage = rawStages.get(index);
            if (!(rawStage instanceof Map<?, ?> stageMap)) {
                throw new IllegalArgumentException("UI performance profile stage must be a YAML map: " + profileName + " stage " + (index + 1));
            }
            stages.add(parseStage(profileName, index, stageMap));
        }
        UiPerformanceExecutionPlan plan = new UiPerformanceExecutionPlan(profileName, type, stages);
        plan.validate(getMaxVirtualUsers());
        return plan;
    }

    /** Builds the complete browser runtime, threshold, and selected workload profile. */
    public static UiPerformanceRunProfile getRunProfile() {
        UiPerformanceRunProfile profile = new UiPerformanceRunProfile(
                getExecutionPlan(),
                UiPerformanceYamlReader.getLong(ROOT + "execution.synchronized_start_timeout_ms", 60_000L),
                UiPerformanceYamlReader.getLong(ROOT + "execution.between_iterations_ms", 0L),
                UiPerformanceYamlReader.getLong(ROOT + "browser.action_timeout_ms", 10_000L),
                UiPerformanceYamlReader.getLong(ROOT + "browser.navigation_timeout_ms", 20_000L),
                UiPerformanceYamlReader.getBoolean(ROOT + "browser.headless", true),
                UiPerformanceYamlReader.getBoolean(ROOT + "browser.ignore_https_errors", false),
                UiPerformanceYamlReader.getString(ROOT + "browser.isolation", "process"),
                UiPerformanceYamlReader.getString(ROOT + "browser.user_agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"),
                UiPerformanceYamlReader.getBoolean(ROOT + "evidence.capture_failure_screenshots", false),
                UiPerformanceYamlReader.getBoolean(ROOT + "evidence.capture_console_errors", true),
                getMaxVirtualUsers(),
                UiPerformanceYamlReader.getDouble(ROOT + "thresholds.maximum_failure_rate_percent", 20.0),
                UiPerformanceYamlReader.getLong(ROOT + "thresholds.maximum_average_journey_duration_ms", 5_000L),
                UiPerformanceYamlReader.getLong(ROOT + "thresholds.maximum_p95_journey_duration_ms", 8_000L)
        );
        profile.validate();
        return profile;
    }

    /** Maximum users is tester-controlled in YAML; no hidden compiled maximum is applied. */
    public static int getMaxVirtualUsers() {
        return UiPerformanceYamlReader.getInt(ROOT + "safety.max_virtual_users", 10);
    }

    /** Backward-compatible accessor for the shared browser preparation timeout. */
    public static long getSynchronizedStartTimeoutMs() {
        return getRunProfile().synchronizedStartTimeoutMs();
    }

    public static String getUsersCsvPath() {
        return UiPerformanceYamlReader.getString(ROOT + "data.users_csv", "ui_performance/data/users.csv");
    }

    /**
     * Returns whether the journey requires CSV-backed virtual-user data.
     * Disable this for public/read-only journeys that do not use data-field placeholders.
     */
    public static boolean isCsvUserDataEnabled() {
        return UiPerformanceYamlReader.getBoolean(ROOT + "data.use_csv", true);
    }

    public static boolean isUserReuseAllowed() {
        return UiPerformanceYamlReader.getBoolean(ROOT + "data.allow_user_reuse", false);
    }

    public static String getReportOutputDirectory() {
        return UiPerformanceYamlReader.getString(ROOT + "reporting.output_directory", "test-output/ui_performance");
    }

    public static boolean isExistingPerformanceReporterEnabled() {
        return UiPerformanceYamlReader.getBoolean(ROOT + "reporting.existing_performance_reporter_enabled", true);
    }

    public static boolean isHtmlReportEnabled() { return UiPerformanceYamlReader.getBoolean(ROOT + "reporting.html_enabled", true); }
    public static boolean isPdfReportEnabled() { return UiPerformanceYamlReader.getBoolean(ROOT + "reporting.pdf_enabled", true); }
    public static boolean isCsvReportEnabled() { return UiPerformanceYamlReader.getBoolean(ROOT + "reporting.csv_enabled", true); }
    public static boolean isJsonReportEnabled() { return UiPerformanceYamlReader.getBoolean(ROOT + "reporting.json_enabled", true); }

    /** Guards a live run so the checked-in template never targets an application accidentally. */
    public static void requireEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "UI performance execution is disabled. Set ui_performance.enabled: true only after target approval, account preparation, profile review, and machine-capacity review.");
        }
    }

    private static UiPerformanceStage parseStage(String profileName, int index, Map<?, ?> stageMap) {
        String defaultName = profileName + "-stage-" + (index + 1);
        return new UiPerformanceStage(
                mapString(stageMap, "name", defaultName),
                mapInt(stageMap, "users", 1),
                mapInt(stageMap, "ramp_up_seconds", 0),
                mapInt(stageMap, "hold_seconds", 0),
                mapInt(stageMap, "iterations_per_user", 1));
    }

    private static String mapString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value).trim();
    }

    private static int mapInt(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("UI performance stage value must be an integer: " + key, exception);
        }
    }

    private static int defaultPort(String protocol) {
        return "http".equalsIgnoreCase(protocol) ? 80 : 443;
    }

    private static String normalizePath(String value, String description) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || "/".equals(trimmed)) {
            return "";
        }
        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException(description + " must begin with '/'.");
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
