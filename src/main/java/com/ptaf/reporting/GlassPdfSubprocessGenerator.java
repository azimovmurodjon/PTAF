package com.ptaf.reporting;

import com.google.gson.Gson;
import tech.grasshopper.pdf.PDFCucumberReport;
import tech.grasshopper.pdf.data.DashboardData;
import tech.grasshopper.pdf.data.ExecutableData;
import tech.grasshopper.pdf.data.AttributeData;
import tech.grasshopper.pdf.data.ReportData;
import tech.grasshopper.pdf.pojo.cucumber.Feature;
import tech.grasshopper.pdf.pojo.cucumber.Scenario;
import tech.grasshopper.pdf.pojo.cucumber.Status;
import tech.grasshopper.pdf.pojo.cucumber.Step;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Standalone main class that generates a single Glass-style PDF report for one feature.
 *
 * <p>Invoked as a subprocess by {@link PerFeatureReportListener} so each PDF runs in its
 * own JVM, avoiding the static font caching bug in {@code tech.grasshopper.pdf.font.ReportFont}.</p>
 *
 * <p>Arguments: {@code <outputPdfPath> <jsonDataFilePath>}</p>
 */
public class GlassPdfSubprocessGenerator {

    // ── Inner POJOs for Gson deserialization (prefixed to avoid library name conflicts) ──

    static class GlassFeatureInput {
        String featureName;
        String startTime;
        String endTime;
        List<GlassScenarioInput> scenarios;
    }

    static class GlassScenarioInput {
        String name;
        String status;
        String startTime;
        String endTime;
        List<GlassStepInput> steps;
        List<GlassScreenshotInput> screenshots;
    }

    static class GlassStepInput {
        String keyword;
        String text;
        String status;
        long durationMs;
        String errorMessage;
        String mediaPath;       // absolute path to screenshot PNG file on disk (optional)
        String screenshotName;  // label shown above the screenshot (optional)
    }

    static class GlassScreenshotInput {
        String base64;
        String name;
        String filePath;  // absolute path to screenshot PNG file on disk (alternative to base64)
    }

    // ── Main entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: GlassPdfSubprocessGenerator <outputPdfPath> <jsonDataFilePath>");
            System.exit(1);
        }

        String outputPath = args[0];
        String jsonPath   = args[1];

        try {
            Gson gson = new Gson();
            GlassFeatureInput featureInput;
            try (FileReader reader = new FileReader(jsonPath)) {
                featureInput = gson.fromJson(reader, GlassFeatureInput.class);
            }

            if (featureInput == null || featureInput.featureName == null) {
                throw new IllegalArgumentException("Invalid or empty JSON data file: " + jsonPath);
            }

            generateGlassPdf(featureInput, outputPath);
            System.out.println("OK: " + outputPath);
            System.exit(0);
        } catch (Exception e) {
            System.err.println("FAIL: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    // ── PDF generation ────────────────────────────────────────────────────────────

    private static void generateGlassPdf(GlassFeatureInput fd, String outputPath) throws Exception {
        LocalDateTime runTime      = LocalDateTime.now();
        LocalDateTime featureStart = parseTime(fd.startTime, runTime);
        LocalDateTime featureEnd   = parseTime(fd.endTime,   runTime);

        List<GlassScenarioInput> scenarioInputs =
            fd.scenarios != null ? fd.scenarios : Collections.emptyList();

        // ── Build Feature POJO ────────────────────────────────────────────────────
        Feature feature = Feature.builder()
            .name(fd.featureName)
            .status(Status.PASSED)
            .startTime(featureStart).endTime(featureEnd)
            .scenarios(new ArrayList<>())
            .build();

        int fPassed = 0, fFailed = 0, fSkipped = 0;
        int totalSteps = 0, passedSteps = 0, failedSteps = 0, skippedSteps = 0;
        List<Scenario> scenarioPojos = new ArrayList<>();

        for (GlassScenarioInput sd : scenarioInputs) {
            Status scStatus   = parseStatus(sd.status);
            LocalDateTime scStart = parseTime(sd.startTime, featureStart);
            LocalDateTime scEnd   = parseTime(sd.endTime,   featureEnd);

            // ── Build Step POJOs ──────────────────────────────────────────────────
            List<Step> stepPojos = new ArrayList<>();
            List<GlassStepInput> steps = sd.steps != null ? sd.steps : Collections.emptyList();

            if (steps.isEmpty()) {
                // No step data — create one synthetic step for the scenario
                Step synth = Step.builder()
                    .name(sd.name != null ? sd.name : "Scenario")
                    .keyword(scStatus.name())
                    .status(scStatus)
                    .startTime(scStart).endTime(scEnd)
                    .rows(Collections.emptyList())
                    .before(Collections.emptyList())
                    .after(Collections.emptyList())
                    .build();
                stepPojos.add(synth);
                totalSteps++;
                if (scStatus == Status.PASSED) passedSteps++;
                else if (scStatus == Status.FAILED) failedSteps++;
                else skippedSteps++;
            } else {
                for (GlassStepInput step : steps) {
                    Status stepStatus = parseStatus(step.status);
                    long dMs = step.durationMs;
                    LocalDateTime stepEnd = scStart.plusNanos(dMs * 1_000_000L);

                    Step stepPojo = Step.builder()
                        .name(buildStepName(step.keyword, step.text))
                        .keyword(step.keyword != null ? step.keyword.trim() : "")
                        .status(stepStatus)
                        .startTime(scStart).endTime(stepEnd)
                        .rows(Collections.emptyList())
                        .before(Collections.emptyList())
                        .after(Collections.emptyList())
                        .build();
                    // Set error message separately so the library renders it as red text
                    if (step.errorMessage != null && !step.errorMessage.isBlank()) {
                        stepPojo.setErrorMessage(step.errorMessage);
                    }
                    // Attach screenshot to the step if a media file path is provided
                    if (step.mediaPath != null && !step.mediaPath.isBlank()
                            && new File(step.mediaPath).exists()) {
                        stepPojo.setMedia(Collections.singletonList(step.mediaPath));
                        // Set the screenshot label as output (shown as green text above the screenshot)
                        String label = step.screenshotName != null && !step.screenshotName.isBlank()
                            ? step.screenshotName : "Screenshot of the Failure Step";
                        stepPojo.setOutput(Collections.singletonList(label));
                    }
                    stepPojos.add(stepPojo);
                    totalSteps++;
                    if (stepStatus == Status.PASSED) passedSteps++;
                    else if (stepStatus == Status.FAILED) failedSteps++;
                    else skippedSteps++;
                }
            }

            // ── Build Scenario POJO ───────────────────────────────────────────────
            Scenario sc = Scenario.builder()
                .name(sd.name != null ? sd.name : "Unnamed Scenario")
                .status(scStatus)
                .startTime(scStart).endTime(scEnd)
                .feature(feature)
                .steps(stepPojos)
                .before(Collections.emptyList())
                .after(Collections.emptyList())
                .build();

            scenarioPojos.add(sc);
            switch (scStatus) {
                case PASSED  -> fPassed++;
                case FAILED  -> fFailed++;
                default      -> fSkipped++;
            }
        }

        // ── Update Feature counts ─────────────────────────────────────────────────
        feature.setScenarios(scenarioPojos);
        feature.setPassedScenarios(fPassed);
        feature.setFailedScenarios(fFailed);
        feature.setSkippedScenarios(fSkipped);
        feature.setTotalScenarios(scenarioPojos.size());
        feature.setStatus(fFailed > 0 ? Status.FAILED
            : (!scenarioPojos.isEmpty() && fSkipped == scenarioPojos.size()
               ? Status.SKIPPED : Status.PASSED));

        // ── Build ReportData ──────────────────────────────────────────────────────
        List<Feature> featureList = Collections.singletonList(feature);

        DashboardData dashboard = DashboardData.builder()
            .testRunStartTime(featureStart).testRunEndTime(featureEnd)
            .passedFeatures(fFailed == 0 && (scenarioPojos.isEmpty() || fSkipped < scenarioPojos.size()) ? 1 : 0)
            .failedFeatures(fFailed > 0 ? 1 : 0)
            .skippedFeatures(!scenarioPojos.isEmpty() && fSkipped == scenarioPojos.size() ? 1 : 0)
            .totalFeatures(1)
            .passedScenarios(fPassed).failedScenarios(fFailed)
            .skippedScenarios(fSkipped).totalScenarios(scenarioPojos.size())
            .passedSteps(passedSteps).failedSteps(failedSteps)
            .skippedSteps(skippedSteps).totalSteps(totalSteps)
            .build();

        List<Scenario> notPassedList = new ArrayList<>();
        for (Scenario sc : scenarioPojos) {
            if (sc.getStatus() != Status.PASSED) notPassedList.add(sc);
        }

        ReportData reportData = ReportData.builder()
            .features(featureList)
            .summaryData(dashboard)
            .featureData(tech.grasshopper.pdf.data.FeatureData.builder()
                .features(featureList).build())
            .scenarioData(tech.grasshopper.pdf.data.ScenarioData.builder()
                .scenarios(scenarioPojos).build())
            .executableData(ExecutableData.builder()
                .executables(Collections.emptyList()).build())
            .notPassedScenarioData(tech.grasshopper.pdf.data.ScenarioData.builder()
                .scenarios(notPassedList).build())
            .tagData(AttributeData.TagData.builder().tags(Collections.emptyList()).build())
            .deviceData(AttributeData.DeviceData.builder().devices(Collections.emptyList()).build())
            .authorData(AttributeData.AuthorData.builder().authors(Collections.emptyList()).build())
            .build();

        // ── Write PDF ─────────────────────────────────────────────────────────────
        File pdfFile = new File(outputPath);
        if (pdfFile.getParentFile() != null) pdfFile.getParentFile().mkdirs();

        PDFCucumberReport report = new PDFCucumberReport(reportData, pdfFile);
        report.createReport();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private static String buildStepName(String keyword, String text) {
        StringBuilder sb = new StringBuilder();
        if (keyword != null && !keyword.isBlank()) sb.append(keyword.trim()).append(" ");
        if (text != null) sb.append(text);
        return sb.toString().trim();
    }

    private static LocalDateTime parseTime(String s, LocalDateTime fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return LocalDateTime.parse(s); } catch (Exception e) { return fallback; }
    }

    private static Status parseStatus(String s) {
        if (s == null) return Status.UNDEFINED;
        switch (s.toUpperCase()) {
            case "PASSED":    return Status.PASSED;
            case "FAILED":    return Status.FAILED;
            case "SKIPPED":   return Status.SKIPPED;
            case "PENDING":   return Status.PENDING;
            case "AMBIGUOUS": return Status.AMBIGUOUS;
            default:          return Status.UNDEFINED;
        }
    }
}
