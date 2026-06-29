package com.ptaf.performance.config;

import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceProfile;

/**
 * Centralized accessor for performance-related configuration properties.
 *
 * <p>This class provides convenience methods to construct common configuration
 * objects (PerformanceProfile and PerformanceAssertionProfile) and to retrieve
 * individual configuration values such as protocol, host, port, and reporting
 * directories. All configuration values are read using {@code PerformanceYamlReader}
 * from keys under the "performance." root in the YAML configuration.
 *
 * <p>Intended to be used by test code, runners and any components that need a
 * standardized way to obtain performance test settings. The methods in this class
 * do not perform any computation or validation beyond delegating to the YAML reader.
 *
 * Note for testers:
 * - If a YAML key is missing, {@code PerformanceYamlReader} will return a fallback
 *   default when a default value is provided in the call (see individual methods).
 * - Keys used are documented in each accessor method Javadoc and inline comments.
 */
public class PerformanceConfigurationProperties {

    /**
     * Root prefix applied to all performance-related configuration keys in the YAML.
     * All keys referenced by this class are constructed by concatenating this root with
     * the specific sub-key (for example: "performance.defaults.users").
     */
    private static final String ROOT = "performance.";

    /**
     * Build and return the default {@link PerformanceProfile} using values read from YAML.
     *
     * <p>This method reads the following keys (with their fallback defaults if the key is absent):
     * - performance.defaults.users (default: 1)
     * - performance.defaults.rampUpSeconds (default: 1)
     * - performance.defaults.holdSeconds (default: 1)
     * - performance.defaults.iterations (default: 1)
     *
     * @return a {@link PerformanceProfile} populated from YAML or with defaults when keys are missing
     */
    public static PerformanceProfile getDefaultProfile() {
        return new PerformanceProfile(
                // Number of concurrent virtual users to simulate (fallback: 1)
                PerformanceYamlReader.getInt(ROOT + "defaults.users", 1),
                // Time in seconds to ramp up users (fallback: 1)
                PerformanceYamlReader.getInt(ROOT + "defaults.rampUpSeconds", 1),
                // Time in seconds to hold the load (fallback: 1)
                PerformanceYamlReader.getInt(ROOT + "defaults.holdSeconds", 1),
                // Number of iterations per user (fallback: 1)
                PerformanceYamlReader.getInt(ROOT + "defaults.iterations", 1)
        );
    }

    /**
     * Build and return the default {@link PerformanceAssertionProfile} using values read from YAML.
     *
     * <p>This method reads the following keys (with their fallback defaults if the key is absent):
     * - performance.assertions.maxErrorPercent (default: 1.0)
     * - performance.assertions.maxAvgResponseTimeMs (default: 2000)
     * - performance.assertions.maxP95ResponseTimeMs (default: 3000)
     *
     * <p>The returned object represents the thresholds used by performance assertions
     * in tests (for example: allowed error rate and response time limits).
     *
     * @return a {@link PerformanceAssertionProfile} populated from YAML or with defaults when keys are missing
     */
    public static PerformanceAssertionProfile getDefaultAssertionProfile() {
        return new PerformanceAssertionProfile(
                // Maximum allowed error percentage across requests (fallback: 1.0)
                PerformanceYamlReader.getDouble(ROOT + "assertions.maxErrorPercent", 1.0),
                // Maximum allowed average response time in milliseconds (fallback: 2000)
                PerformanceYamlReader.getLong(ROOT + "assertions.maxAvgResponseTimeMs", 2000),
                // Maximum allowed 95th percentile response time in milliseconds (fallback: 3000)
                PerformanceYamlReader.getLong(ROOT + "assertions.maxP95ResponseTimeMs", 3000)
        );
    }

    /**
     * Retrieve the protocol to use for performance tests (for example "http" or "https").
     *
     * <p>Key used: performance.defaults.protocol
     *
     * @return the protocol string as read from YAML, or {@code null} if not set
     */
    public static String getProtocol() {
        // No fallback default provided; the reader will return null if the key is absent
        return PerformanceYamlReader.getString(ROOT + "defaults.protocol");
    }

    /**
     * Retrieve the host to target for performance tests.
     *
     * <p>Key used: performance.defaults.host
     *
     * @return the host string as read from YAML, or {@code null} if not set
     */
    public static String getHost() {
        // No fallback default provided; the reader will return null if the key is absent
        return PerformanceYamlReader.getString(ROOT + "defaults.host");
    }

    /**
     * Retrieve the port to use for performance tests.
     *
     * <p>Key used: performance.defaults.port
     *
     * @return the port number as read from YAML, or 443 if the key is absent
     */
    public static int getPort() {
        // Default port fallback is 443 (commonly used for HTTPS)
        return PerformanceYamlReader.getInt(ROOT + "defaults.port", 443);
    }

    /**
     * Retrieve the folder path where raw results should be written.
     *
     * <p>Key used: performance.reporting.resultsFolder
     *
     * @return the results folder path as read from YAML, or {@code null} if not set
     */
    public static String getResultsFolder() {
        return PerformanceYamlReader.getString(ROOT + "reporting.resultsFolder");
    }

    /**
     * Retrieve the folder path where dashboard assets should be written.
     *
     * <p>Key used: performance.reporting.dashboardFolder
     *
     * @return the dashboard folder path as read from YAML, or {@code null} if not set
     */
    public static String getDashboardFolder() {
        return PerformanceYamlReader.getString(ROOT + "reporting.dashboardFolder");
    }

    /**
     * Return the base directory where performance reports are stored.
     *
     * <p>This value is currently hard-coded. Test suites and reporting tools should write
     * output under this directory (for example: "test-output/performance-reports").
     *
     * @return the hard-coded reports base directory path
     */
    public static String getReportsBaseDirectory() {
        return "test-output/performance-reports";
    }
}
