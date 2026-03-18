package com.ptaf.performance.config;

import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceProfile;

public class PerformanceConfigurationProperties {

    private static final String ROOT = "performance.";

    public static PerformanceProfile getDefaultProfile() {
        return new PerformanceProfile(
                PerformanceYamlReader.getInt(ROOT + "defaults.users", 1),
                PerformanceYamlReader.getInt(ROOT + "defaults.rampUpSeconds", 1),
                PerformanceYamlReader.getInt(ROOT + "defaults.holdSeconds", 1),
                PerformanceYamlReader.getInt(ROOT + "defaults.iterations", 1)
        );
    }

    public static PerformanceAssertionProfile getDefaultAssertionProfile() {
        return new PerformanceAssertionProfile(
                PerformanceYamlReader.getDouble(ROOT + "assertions.maxErrorPercent", 1.0),
                PerformanceYamlReader.getLong(ROOT + "assertions.maxAvgResponseTimeMs", 2000),
                PerformanceYamlReader.getLong(ROOT + "assertions.maxP95ResponseTimeMs", 3000)
        );
    }

    public static String getProtocol() {
        return PerformanceYamlReader.getString(ROOT + "defaults.protocol");
    }

    public static String getHost() {
        return PerformanceYamlReader.getString(ROOT + "defaults.host");
    }

    public static int getPort() {
        return PerformanceYamlReader.getInt(ROOT + "defaults.port", 443);
    }

    public static String getResultsFolder() {
        return PerformanceYamlReader.getString(ROOT + "reporting.resultsFolder");
    }

    public static String getDashboardFolder() {
        return PerformanceYamlReader.getString(ROOT + "reporting.dashboardFolder");
    }

    public static String getReportsBaseDirectory() {
        return "test-output/performance-reports";
    }
}