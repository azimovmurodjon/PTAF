package com.ptaf.reporting;

import tech.grasshopper.pdf.PDFCucumberReport;
import tech.grasshopper.pdf.data.*;
import tech.grasshopper.pdf.pojo.cucumber.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Standalone main class that generates a single Glass-style PDF report for one feature.
 *
 * <p>This class is invoked as a subprocess by {@link PerFeatureReportListener} so that
 * each Glass PDF generation runs in its own JVM. This is necessary because
 * {@code tech.grasshopper.pdf.font.ReportFont} stores four {@code PDFont} objects in
 * static fields. After the first {@code PDDocument} is closed, these static references
 * become invalid, causing all subsequent PDF generations in the same JVM to fail with
 * {@code IOException: The TrueType font null does not contain a 'cmap' table}.
 * Running each generation in a fresh JVM completely avoids this library bug.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 *   java -cp &lt;classpath&gt; com.ptaf.reporting.GlassPdfSubprocessGenerator \
 *       &lt;outputPdfPath&gt; &lt;featureName&gt; &lt;scenario1Name&gt; &lt;scenario1Status&gt; ...
 * </pre>
 *
 * <p>Arguments:</p>
 * <ol>
 *   <li>Output PDF file path (absolute or relative)</li>
 *   <li>Feature name</li>
 *   <li>Scenario 1 name</li>
 *   <li>Scenario 1 status (PASSED, FAILED, SKIPPED, PENDING, UNDEFINED, AMBIGUOUS)</li>
 *   <li>Scenario 2 name (optional)</li>
 *   <li>Scenario 2 status (optional)</li>
 *   <li>... (pairs of name/status for each scenario)</li>
 * </ol>
 *
 * <p>Exit codes: 0 = success, 1 = error (message printed to stderr).</p>
 */
public class GlassPdfSubprocessGenerator {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: GlassPdfSubprocessGenerator <outputPath> <featureName> <sc1Name> <sc1Status> [<sc2Name> <sc2Status> ...]");
            System.exit(1);
        }

        String outputPath  = args[0];
        String featureName = args[1];

        // Parse scenario pairs: name, status, name, status, ...
        List<String[]> scenarioPairs = new ArrayList<>();
        for (int i = 2; i + 1 < args.length; i += 2) {
            scenarioPairs.add(new String[]{args[i], args[i + 1]});
        }

        try {
            generateGlassPdf(featureName, scenarioPairs, outputPath);
            System.out.println("OK: " + outputPath);
            System.exit(0);
        } catch (Exception e) {
            System.err.println("FAIL: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void generateGlassPdf(String featureName,
                                          List<String[]> scenarioPairs,
                                          String outputPath) throws Exception {
        LocalDateTime t = LocalDateTime.now();

        // ── Build Feature and Scenario POJOs ─────────────────────────────────────────
        Feature feature = Feature.builder()
            .name(featureName)
            .status(Status.PASSED)
            .startTime(t).endTime(t)
            .scenarios(new ArrayList<>())
            .build();

        int passed = 0, failed = 0, skipped = 0;
        List<Scenario> scenarioPojos = new ArrayList<>();

        for (String[] pair : scenarioPairs) {
            String scName   = pair[0];
            Status scStatus = parseStatus(pair[1]);

            Step step = Step.builder()
                .name(scName)
                .keyword(scStatus.name())
                .status(scStatus)
                .startTime(t).endTime(t)
                .rows(Collections.emptyList())
                .before(Collections.emptyList())
                .after(Collections.emptyList())
                .build();

            Scenario sc = Scenario.builder()
                .name(scName)
                .status(scStatus)
                .startTime(t).endTime(t)
                .feature(feature)
                .steps(Collections.singletonList(step))
                .before(Collections.emptyList())
                .after(Collections.emptyList())
                .build();

            scenarioPojos.add(sc);

            switch (scStatus) {
                case PASSED  -> passed++;
                case FAILED  -> failed++;
                default      -> skipped++;
            }
        }

        feature.setScenarios(scenarioPojos);
        feature.setPassedScenarios(passed);
        feature.setFailedScenarios(failed);
        feature.setSkippedScenarios(skipped);
        feature.setTotalScenarios(scenarioPojos.size());
        feature.setStatus(failed > 0 ? Status.FAILED
            : (skipped == scenarioPojos.size() ? Status.SKIPPED : Status.PASSED));

        // ── Build all required ReportData sub-objects ─────────────────────────────────
        List<Feature> featureList = Collections.singletonList(feature);

        DashboardData dashboard = DashboardData.builder()
            .testRunStartTime(t).testRunEndTime(t)
            .passedFeatures(failed == 0 && skipped < scenarioPojos.size() ? 1 : 0)
            .failedFeatures(failed > 0 ? 1 : 0)
            .skippedFeatures(skipped == scenarioPojos.size() ? 1 : 0)
            .totalFeatures(1)
            .passedScenarios(passed).failedScenarios(failed)
            .skippedScenarios(skipped).totalScenarios(scenarioPojos.size())
            .passedSteps(passed).failedSteps(failed)
            .skippedSteps(skipped).totalSteps(scenarioPojos.size())
            .build();

        List<Scenario> notPassedList = new ArrayList<>();
        for (Scenario sc : scenarioPojos) {
            if (sc.getStatus() != Status.PASSED) notPassedList.add(sc);
        }

        ReportData reportData = ReportData.builder()
            .features(featureList)
            .summaryData(dashboard)
            .featureData(FeatureData.builder().features(featureList).build())
            .scenarioData(ScenarioData.builder().scenarios(scenarioPojos).build())
            .executableData(ExecutableData.builder().executables(Collections.emptyList()).build())
            .notPassedScenarioData(ScenarioData.builder().scenarios(notPassedList).build())
            .tagData(AttributeData.TagData.builder().tags(Collections.emptyList()).build())
            .deviceData(AttributeData.DeviceData.builder().devices(Collections.emptyList()).build())
            .authorData(AttributeData.AuthorData.builder().authors(Collections.emptyList()).build())
            .build();

        // ── Ensure parent directory exists ────────────────────────────────────────────
        File pdfFile = new File(outputPath);
        if (pdfFile.getParentFile() != null) {
            pdfFile.getParentFile().mkdirs();
        }

        // ── Generate Glass PDF ────────────────────────────────────────────────────────
        // This runs in its own JVM so the static font fields in ReportFont are always
        // fresh — no cross-contamination from previous PDF generations.
        PDFCucumberReport report = new PDFCucumberReport(reportData, pdfFile);
        report.createReport();
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
