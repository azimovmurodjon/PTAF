package com.ptaf.ui_performance;

import com.ptaf.ui_performance.config.UiPerformanceConfiguration;
import com.ptaf.ui_performance.config.UiPerformanceLocatorRepository;
import com.ptaf.ui_performance.data.UiPerformanceUserDataReader;
import com.ptaf.ui_performance.metrics.UiPerformanceMetrics;
import com.ptaf.ui_performance.model.UiPerformanceExecutionPlan;
import com.ptaf.ui_performance.model.UiPerformanceIterationResult;
import com.ptaf.ui_performance.model.UiPerformanceRunProfile;
import com.ptaf.ui_performance.model.UiPerformanceRunResult;
import com.ptaf.ui_performance.model.UiPerformanceStage;
import com.ptaf.ui_performance.model.UiPerformanceStageResult;
import com.ptaf.ui_performance.model.UiPerformanceTestType;
import com.ptaf.ui_performance.reporting.UiPerformanceExistingReporterAdapter;
import com.ptaf.ui_performance.reporting.UiPerformanceDurationFormatter;
import com.ptaf.ui_performance.reporting.UiPerformanceReportWriter;
import com.ptaf.ui_performance.reporting.UiPerformanceSensitiveTextSanitizer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline contracts for profiles, configuration, timing reports, and existing Performance reporter reuse. */
public class UiPerformanceModuleContractTest {

    @Test
    public void loadsDedicatedTargetActiveProfileDataBrowserControlAndLocators() {
        Assert.assertTrue(UiPerformanceConfiguration.isEnabled(), "The requested eStore UI performance configuration must be runnable.");
        UiPerformanceRunProfile profile = UiPerformanceConfiguration.getRunProfile();
        Assert.assertEquals(profile.executionPlan().profileName(), "load");
        Assert.assertEquals(profile.executionPlan().testType(), UiPerformanceTestType.LOAD);
        Assert.assertEquals(profile.executionPlan().maximumVirtualUsers(), 2);
        Assert.assertEquals(profile.executionPlan().stages().getFirst().rampUpSeconds(), 0);
        Assert.assertEquals(profile.synchronizedStartTimeoutMs(), 120_000L);
        Assert.assertTrue(profile.headless(), "Headless execution must remain YAML controlled.");
        Assert.assertEquals(profile.browserIsolation(), "process");
        Assert.assertEquals(UiPerformanceConfiguration.getMaxVirtualUsers(), 10);
        Assert.assertEquals(UiPerformanceConfiguration.getConfiguredTargetUrl(), "https://staging.fnb.avoka-transact.com");
        Assert.assertEquals(UiPerformanceConfiguration.getConfiguredRoute("test_harness"),
                "/workspaces-preprod/servlet/SmartForm.html?formCode=testharness2");
        Assert.assertEquals(UiPerformanceUserDataReader.readUsers(UiPerformanceConfiguration.getUsersCsvPath()).size(), 10);
        Assert.assertEquals(UiPerformanceUserDataReader.createSyntheticUsers(3).size(), 3);
        Assert.assertEquals(UiPerformanceUserDataReader.createSyntheticUsers(3).getFirst().getId(), "virtual_user_001");
        Assert.assertTrue(UiPerformanceUserDataReader.createSyntheticUsers(3).getFirst().getValues().isEmpty());
        Assert.assertEquals(UiPerformanceLocatorRepository.getLocatorDefinition("test_harness", "create_url"), "Button_Create URL");
        Assert.assertEquals(UiPerformanceLocatorRepository.getLocatorDefinition("test_harness", "generated_url"),
                "CSS_#productSelectURL");
    }

    @Test
    public void validatesLoadStressSpikeSoakAndConfigurableSafetyLimit() {
        new UiPerformanceExecutionPlan("load", UiPerformanceTestType.LOAD,
                List.of(new UiPerformanceStage("load", 25, 0, 0, 1))).validate(25);
        new UiPerformanceExecutionPlan("stress", UiPerformanceTestType.STRESS,
                List.of(new UiPerformanceStage("low", 2, 0, 5, 0),
                        new UiPerformanceStage("high", 10, 5, 5, 0))).validate(25);
        new UiPerformanceExecutionPlan("spike", UiPerformanceTestType.SPIKE,
                List.of(new UiPerformanceStage("base", 2, 0, 5, 0),
                        new UiPerformanceStage("spike", 25, 0, 5, 0))).validate(25);
        new UiPerformanceExecutionPlan("soak", UiPerformanceTestType.SOAK,
                List.of(new UiPerformanceStage("sustained", 5, 5, 30, 0))).validate(25);

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
                () -> new UiPerformanceExecutionPlan("over-limit", UiPerformanceTestType.LOAD,
                        List.of(new UiPerformanceStage("too-large", 26, 0, 0, 1))).validate(25));
        Assert.assertTrue(exception.getMessage().contains("max_virtual_users"));
    }

    @Test
    public void writesFullBrowserPerformanceReportsAndExistingPerformanceWorkbook() throws Exception {
        String sanitized = UiPerformanceSensitiveTextSanitizer.sanitize("password=secret-token authorization=Bearer abc123");
        Assert.assertFalse(sanitized.contains("secret-token"));
        Assert.assertFalse(sanitized.contains("abc123"));

        Map<String, Long> firstTimings = new LinkedHashMap<>();
        firstTimings.put("Navigate login", 11_735L);
        firstTimings.put("Click login.submit", 5_555L);
        Map<String, Long> secondTimings = new LinkedHashMap<>();
        secondTimings.put("Navigate login", 10_924L);
        secondTimings.put("Click login.submit", 5_581L);
        long now = System.currentTimeMillis();
        List<UiPerformanceIterationResult> iterations = List.of(
                new UiPerformanceIterationResult("target-load", "user_001", 1, true,
                        now, now + 22_025, 22_025L, firstTimings, null, null, null, List.of()),
                new UiPerformanceIterationResult("target-load", "user_002", 1, false,
                        now + 4, now + 24_744, 24_740L, secondTimings, "TIMEOUT", sanitized, null, List.of())
        );
        UiPerformanceMetrics metrics = UiPerformanceMetrics.from(iterations, 1_000L);
        Assert.assertEquals(metrics.totalIterations(), 2);
        Assert.assertEquals(metrics.p90JourneyDurationMs(), 24_740L);
        Assert.assertEquals(metrics.p95JourneyDurationMs(), 24_740L);
        Assert.assertEquals(metrics.p99JourneyDurationMs(), 24_740L);
        Assert.assertEquals(metrics.failureRatePercent(), 50.0, 0.001);

        UiPerformanceStage stage = new UiPerformanceStage("target-load", 2, 0, 0, 1);
        UiPerformanceExecutionPlan plan = new UiPerformanceExecutionPlan("load", UiPerformanceTestType.LOAD, List.of(stage));
        UiPerformanceRunProfile profile = new UiPerformanceRunProfile(
                plan, 60_000L, 0L, 1_000L, 2_000L, true, false, "process",
                "Mozilla/5.0 Contract Browser",
                false, true, 10, 60.0, 60_000L, 60_000L);
        profile.validate();
        Path reportDirectory = Files.createTempDirectory("ui_performance-contract-");
        Instant started = Instant.now();
        UiPerformanceStageResult stageResult = new UiPerformanceStageResult(stage, started, started.plusSeconds(1), iterations, metrics);
        UiPerformanceRunResult result = new UiPerformanceRunResult(
                "UI-PERF-CONTRACT", "Offline Contract", "qa.example.internal",
                started, started.plusSeconds(1), profile, List.of(stageResult), iterations, metrics, reportDirectory);

        new UiPerformanceReportWriter().write(result, true, true, true, true);
        Path workbook = new UiPerformanceExistingReporterAdapter().write(result);

        assertNonEmpty(reportDirectory.resolve("ui_performance-summary.html"));
        assertNonEmpty(reportDirectory.resolve("ui_performance-summary.pdf"));
        assertNonEmpty(reportDirectory.resolve("ui_performance-stages.csv"));
        assertNonEmpty(reportDirectory.resolve("ui_performance-iterations.csv"));
        assertNonEmpty(reportDirectory.resolve("ui_performance-step-timings.csv"));
        assertNonEmpty(reportDirectory.resolve("ui_performance-performance-summary.txt"));
        assertNonEmpty(reportDirectory.resolve("ui_performance-summary.json"));
        assertNonEmpty(reportDirectory.resolve("performance-reporter-index.txt"));
        assertNonEmpty(workbook);
        assertNonEmpty(reportDirectory.resolve("performance-reporter/target-load/summary.txt"));
        assertNonEmpty(reportDirectory.resolve("performance-reporter/target-load/readable-summary.txt"));
        assertNonEmpty(reportDirectory.resolve("performance-reporter/target-load/ui-browser-results.jtl"));

        String html = Files.readString(reportDirectory.resolve("ui_performance-summary.html"));
        Assert.assertTrue(html.contains("Load profile and stage results"));
        Assert.assertTrue(html.contains("P90 / P95 / P99"));
        Assert.assertTrue(html.contains("First-start spread"));
        Assert.assertTrue(html.contains("23.383 s"));
        Assert.assertTrue(html.contains("11.735 s"));
        Assert.assertFalse(html.contains("23383 ms"));
        Assert.assertFalse(html.contains("secret-token"));
        String json = Files.readString(reportDirectory.resolve("ui_performance-summary.json"));
        Assert.assertTrue(json.contains("\"test_type\": \"Load\""));
        Assert.assertTrue(json.contains("transaction_timing_summary"));
        Assert.assertTrue(json.contains("\"average_journey_duration\": \"23.383 s\""));
        Assert.assertTrue(json.contains("\"journey_duration_ms\": 22025"));
        String jtl = Files.readString(reportDirectory.resolve("performance-reporter/target-load/ui-browser-results.jtl"));
        Assert.assertTrue(jtl.contains("UI-VU-user_001"));
        Assert.assertFalse(jtl.contains("secret-token"));

        try (XSSFWorkbook excel = new XSSFWorkbook(Files.newInputStream(workbook))) {
            Assert.assertNotNull(excel.getSheet("Executive_Summary"));
            Assert.assertNotNull(excel.getSheet("Scenario_Summary"));
            Assert.assertNotNull(excel.getSheet("Charts"));
        }
    }

    private static void assertNonEmpty(Path path) throws Exception {
        Assert.assertTrue(Files.size(path) > 0, "Expected non-empty report artifact: " + path);
    }

    @Test
    public void formatsHumanReadableDurationsWithoutDiscardingSubSecondPrecision() {
        Assert.assertEquals(UiPerformanceDurationFormatter.format(45L), "45 ms");
        Assert.assertEquals(UiPerformanceDurationFormatter.format(23_383L), "23.383 s");
        Assert.assertEquals(UiPerformanceDurationFormatter.format(62_500L), "1 min 2.500 s");
        Assert.assertEquals(UiPerformanceDurationFormatter.format(3_600_000L), "1 h 0 min 0 s");
    }
}
