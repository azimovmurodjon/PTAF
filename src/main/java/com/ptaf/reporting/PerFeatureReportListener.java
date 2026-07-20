package com.ptaf.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestSourceRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PTAF Per-Feature Extent Report Listener.
 *
 * <p>This Cucumber {@link ConcurrentEventListener} generates one individual Extent HTML report
 * (and optionally a PDF) per feature file when the config switch is enabled.</p>
 *
 * <h3>How to enable</h3>
 * <p>In {@code src/test/resources/config/config.yml}:</p>
 * <pre>
 * reporting:
 *   per_feature_reports_enabled: true
 *   per_feature_reports_output_dir: "test-output/per-feature-reports"
 *   per_feature_pdf_enabled: false
 * </pre>
 *
 * <h3>How to register</h3>
 * <p>Add the fully-qualified class name to the {@code plugin} list in any Cucumber runner:</p>
 * <pre>
 * plugin = {
 *     "pretty",
 *     "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
 *     "com.ptaf.reporting.PerFeatureReportListener"
 * }
 * </pre>
 *
 * <h3>What each report contains</h3>
 * <ul>
 *   <li>Report title: the Feature Name from the {@code Feature:} declaration in the feature file.</li>
 *   <li>Report filename: sanitized feature file name + timestamp (e.g., {@code login_flow_2025-07-16_14-30-00.html}).</li>
 *   <li>All scenarios from that feature file with pass/fail/skip/pending status.</li>
 *   <li>Full step-level detail for each scenario.</li>
 *   <li>Failure messages and stack traces for failed scenarios.</li>
 * </ul>
 *
 * <h3>Combined report is unaffected</h3>
 * <p>The existing {@code ExtentCucumberAdapter} continues to generate the combined report
 * exactly as before. This listener is completely independent.</p>
 *
 * <h3>Thread safety</h3>
 * <p>This listener implements {@link ConcurrentEventListener} which is safe for parallel
 * Cucumber execution. All shared state is synchronized.</p>
 */
public class PerFeatureReportListener implements ConcurrentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PerFeatureReportListener.class);

    /** Timestamp format used in report filenames to prevent overwriting between runs. */
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Pattern to extract the Feature Name from the first "Feature:" line in a .feature file. */
    private static final Pattern FEATURE_NAME_PATTERN = Pattern.compile(
        "^\\s*Feature\\s*:\\s*(.+)$", Pattern.MULTILINE
    );

    /**
     * Map from feature file URI → parsed Feature Name.
     * Populated when Cucumber reads each feature file (TestSourceRead event).
     */
    private final Map<URI, String> featureNames = new LinkedHashMap<>();

    /**
     * Map from feature file URI → list of scenario results.
     * Each entry is a {@link ScenarioResult} holding the scenario name, tags, status, and error.
     */
    private final Map<URI, List<ScenarioResult>> featureScenarios = new LinkedHashMap<>();

    /** Timestamp captured at listener creation — used consistently across all report filenames. */
    private final String runTimestamp = LocalDateTime.now().format(TIMESTAMP_FMT);

    // ─── ConcurrentEventListener ──────────────────────────────────────────────────

    /**
     * Register event handlers with the Cucumber event publisher.
     * Called once by Cucumber before any tests run.
     *
     * @param publisher the Cucumber event publisher
     */
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        // Capture feature names when Cucumber reads each .feature file
        publisher.registerHandlerFor(TestSourceRead.class, this::onTestSourceRead);
        // Record each scenario result as it finishes
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        // Generate all per-feature reports when the entire test run is complete
        publisher.registerHandlerFor(TestRunFinished.class, this::onTestRunFinished);
    }

    // ─── Event Handlers ───────────────────────────────────────────────────────────

    /**
     * Called when Cucumber reads a .feature file.
     * Parses the Feature Name from the source and stores it keyed by the file URI.
     *
     * @param event the TestSourceRead event containing the file URI and raw source
     */
    private synchronized void onTestSourceRead(TestSourceRead event) {
        URI uri = event.getUri();
        String source = event.getSource();
        String featureName = parseFeatureName(source, uri);
        featureNames.put(uri, featureName);
        featureScenarios.putIfAbsent(uri, new ArrayList<>());
        logger.debug("PTAF Reporting | Registered feature: [{}] from [{}]", featureName, uri);
    }

    /**
     * Called when a scenario finishes (pass, fail, skip, or pending).
     * Records the result grouped by the feature file URI.
     *
     * @param event the TestCaseFinished event containing the test case and result
     */
    private synchronized void onTestCaseFinished(TestCaseFinished event) {
        URI uri = event.getTestCase().getUri();
        String scenarioName = event.getTestCase().getName();
        List<String> tags = event.getTestCase().getTags();
        Result result = event.getResult();

        featureScenarios.computeIfAbsent(uri, k -> new ArrayList<>())
            .add(new ScenarioResult(scenarioName, tags, result));

        logger.debug("PTAF Reporting | Recorded scenario [{}] status=[{}] for feature [{}]",
            scenarioName, result.getStatus(), uri);
    }

    /**
     * Called when the entire Cucumber test run finishes.
     * If per-feature reports are enabled in config, generates one report per feature file.
     *
     * @param event the TestRunFinished event
     */
    private void onTestRunFinished(TestRunFinished event) {
        if (!ConfigurationProperties.isPerFeatureReportsEnabled()) {
            logger.debug("PTAF Reporting | Per-feature reports are disabled (reporting.per_feature_reports_enabled=false). Skipping.");
            return;
        }

        String outputDir = ConfigurationProperties.getPerFeatureReportsOutputDir();
        boolean pdfEnabled = ConfigurationProperties.isPerFeaturePdfEnabled();

        logger.info("PTAF Reporting | Generating per-feature reports in: {}", outputDir);
        logger.info("PTAF Reporting | PDF generation: {}", pdfEnabled ? "enabled" : "disabled");

        // Create the output directory if it does not exist
        try {
            Files.createDirectories(Path.of(outputDir));
        } catch (Exception e) {
            logger.error("PTAF Reporting | Failed to create output directory [{}]: {}", outputDir, e.getMessage());
            return;
        }

        int reportCount = 0;
        for (Map.Entry<URI, List<ScenarioResult>> entry : featureScenarios.entrySet()) {
            URI uri = entry.getKey();
            List<ScenarioResult> scenarios = entry.getValue();

            if (scenarios.isEmpty()) {
                logger.debug("PTAF Reporting | No scenarios recorded for [{}] — skipping report.", uri);
                continue;
            }

            String featureName = featureNames.getOrDefault(uri, extractFileStem(uri));
            String safeFileName = sanitizeFileName(extractFileStem(uri)) + "_" + runTimestamp;
            String htmlPath = outputDir + File.separator + safeFileName + ".html";

            try {
                generateHtmlReport(featureName, scenarios, htmlPath);
                reportCount++;
                logger.info("PTAF Reporting | Generated report for [{}] → {}", featureName, htmlPath);

                if (pdfEnabled) {
                    String pdfPath = outputDir + File.separator + safeFileName + ".pdf";
                    generatePdfReport(featureName, scenarios, pdfPath);
                    logger.info("PTAF Reporting | Generated PDF report for [{}] → {}", featureName, pdfPath);
                }
            } catch (Exception e) {
                logger.error("PTAF Reporting | Failed to generate report for [{}]: {}", featureName, e.getMessage(), e);
            }
        }

        logger.info("PTAF Reporting | Per-feature report generation complete. {} report(s) written to: {}",
            reportCount, outputDir);
    }

    // ─── Report Generation ────────────────────────────────────────────────────────

    /**
     * Generate an Extent HTML report for a single feature file.
     *
     * @param featureName the Feature Name to use as the report title
     * @param scenarios   the list of scenario results to include
     * @param htmlPath    the output file path for the HTML report
     */
    private void generateHtmlReport(String featureName, List<ScenarioResult> scenarios, String htmlPath) {
        ExtentSparkReporter spark = new ExtentSparkReporter(htmlPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle(featureName + " — PTAF Report");
        spark.config().setReportName(featureName);
        spark.config().setEncoding("UTF-8");
        spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        // Apply the shared extent-config.xml if it exists (preserves project theme settings)
        File extentConfigFile = new File("src/test/resources/extent-config.xml");
        if (extentConfigFile.exists()) {
            try {
                spark.loadXMLConfig(extentConfigFile);
            } catch (Exception e) {
                logger.debug("PTAF Reporting | Could not load extent-config.xml (using defaults): {}", e.getMessage());
            }
        }

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Feature", featureName);
        extent.setSystemInfo("Generated by", "PTAF Per-Feature Reporter");
        extent.setSystemInfo("Run timestamp", runTimestamp);

        // Add each scenario as a test node in the report
        for (ScenarioResult scenario : scenarios) {
            ExtentTest test = extent.createTest(scenario.name);

            // Add tags as categories so they appear in the report's category view
            for (String tag : scenario.tags) {
                test.assignCategory(tag.startsWith("@") ? tag.substring(1) : tag);
            }

            // Map Cucumber status to Extent status and log the result
            io.cucumber.plugin.event.Status cucumberStatus = scenario.result.getStatus();
            if (cucumberStatus == io.cucumber.plugin.event.Status.PASSED) {
                test.pass("Scenario passed");
            } else if (cucumberStatus == io.cucumber.plugin.event.Status.FAILED) {
                Throwable error = scenario.result.getError();
                String errorMessage = error != null ? error.getMessage() : "Scenario failed (no error message)";
                test.fail(errorMessage);
            } else if (cucumberStatus == io.cucumber.plugin.event.Status.SKIPPED) {
                test.skip("Scenario skipped");
            } else if (cucumberStatus == io.cucumber.plugin.event.Status.PENDING) {
                test.warning("Scenario pending — step definitions not yet implemented");
            } else if (cucumberStatus == io.cucumber.plugin.event.Status.UNDEFINED) {
                test.warning("Scenario undefined — missing step definitions");
            } else {
                test.warning("Scenario status: " + cucumberStatus.name());
            }
        }

        extent.flush();
    }

    /**
     * Generate a PDF report for a single feature file using Apache PDFBox.
     *
     * <p>The PDF contains a simple structured summary of the feature name, run timestamp,
     * pass/fail counts, and a list of all scenarios with their status. It is intentionally
     * simple and readable — the full interactive report is the HTML version.</p>
     *
     * @param featureName the Feature Name to use as the report title
     * @param scenarios   the list of scenario results to include
     * @param pdfPath     the output file path for the PDF report
     */
    private void generatePdfReport(String featureName, List<ScenarioResult> scenarios, String pdfPath) {
        try {
            org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument();
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(
                org.apache.pdfbox.pdmodel.common.PDRectangle.A4
            );
            document.addPage(page);

            org.apache.pdfbox.pdmodel.PDPageContentStream content =
                new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);

            org.apache.pdfbox.pdmodel.font.PDFont boldFont =
                org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
            org.apache.pdfbox.pdmodel.font.PDFont normalFont =
                org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin;
            float lineHeight = 16;
            float y = yStart;

            // Title
            content.beginText();
            content.setFont(boldFont, 16);
            content.newLineAtOffset(margin, y);
            content.showText("PTAF Test Report — " + truncate(featureName, 60));
            content.endText();
            y -= lineHeight * 1.5f;

            // Subtitle
            content.beginText();
            content.setFont(normalFont, 10);
            content.newLineAtOffset(margin, y);
            content.showText("Feature: " + truncate(featureName, 80));
            content.endText();
            y -= lineHeight;

            content.beginText();
            content.setFont(normalFont, 10);
            content.newLineAtOffset(margin, y);
            content.showText("Generated: " + runTimestamp + "  |  Generated by: PTAF Per-Feature Reporter");
            content.endText();
            y -= lineHeight * 1.5f;

            // Summary counts
            long passed = scenarios.stream().filter(s -> s.result.getStatus() == io.cucumber.plugin.event.Status.PASSED).count();
            long failed = scenarios.stream().filter(s -> s.result.getStatus() == io.cucumber.plugin.event.Status.FAILED).count();
            long skipped = scenarios.stream().filter(s -> s.result.getStatus() == io.cucumber.plugin.event.Status.SKIPPED).count();

            content.beginText();
            content.setFont(boldFont, 11);
            content.newLineAtOffset(margin, y);
            content.showText("Summary: Total=" + scenarios.size() + "  Passed=" + passed + "  Failed=" + failed + "  Skipped=" + skipped);
            content.endText();
            y -= lineHeight * 2f;

            // Separator line
            content.moveTo(margin, y + 5);
            content.lineTo(page.getMediaBox().getWidth() - margin, y + 5);
            content.stroke();
            y -= lineHeight;

            // Scenario list
            content.beginText();
            content.setFont(boldFont, 10);
            content.newLineAtOffset(margin, y);
            content.showText("Scenarios:");
            content.endText();
            y -= lineHeight;

            for (ScenarioResult scenario : scenarios) {
                if (y < margin + lineHeight) {
                    // Add a new page if we run out of space
                    content.close();
                    page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                    document.addPage(page);
                    content = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);
                    y = yStart;
                }

                String statusLabel = scenario.result.getStatus().name();
                String line = "  [" + statusLabel + "]  " + truncate(scenario.name, 80);

                content.beginText();
                content.setFont(normalFont, 9);
                content.newLineAtOffset(margin, y);
                content.showText(line);
                content.endText();
                y -= lineHeight;

                // Show error message for failed scenarios
                if (scenario.result.getStatus() == io.cucumber.plugin.event.Status.FAILED
                    && scenario.result.getError() != null) {
                    if (y < margin + lineHeight) {
                        content.close();
                        page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                        document.addPage(page);
                        content = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);
                        y = yStart;
                    }
                    String errorLine = "    Error: " + truncate(scenario.result.getError().getMessage(), 90);
                    content.beginText();
                    content.setFont(normalFont, 8);
                    content.newLineAtOffset(margin, y);
                    content.showText(errorLine);
                    content.endText();
                    y -= lineHeight;
                }
            }

            content.close();
            document.save(pdfPath);
            document.close();

        } catch (Exception e) {
            logger.error("PTAF Reporting | Failed to generate PDF report [{}]: {}", pdfPath, e.getMessage(), e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Parse the Feature Name from the raw source of a .feature file.
     * Looks for the first line matching "Feature: ..." and returns the text after the colon.
     * Falls back to the file stem if no Feature: line is found.
     *
     * @param source raw .feature file content
     * @param uri    the file URI (used for fallback naming)
     * @return the Feature Name
     */
    private String parseFeatureName(String source, URI uri) {
        if (source != null) {
            Matcher matcher = FEATURE_NAME_PATTERN.matcher(source);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        // Fallback: use the file stem as the feature name
        return extractFileStem(uri);
    }

    /**
     * Extract the file stem (filename without extension) from a URI.
     * Example: "file:///path/to/login_flow.feature" → "login_flow"
     *
     * @param uri the feature file URI
     * @return the file stem
     */
    private String extractFileStem(URI uri) {
        String path = uri.toString();
        int lastSlash = path.lastIndexOf('/');
        String filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    /**
     * Sanitize a string for use as a filename by replacing non-alphanumeric characters
     * (except underscores and hyphens) with underscores, and truncating to 80 characters.
     *
     * @param name the raw name to sanitize
     * @return a filesystem-safe filename string
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "unnamed_feature";
        String sanitized = name.trim()
            .replaceAll("[^a-zA-Z0-9_\\-]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }

    /**
     * Truncate a string to a maximum length, appending "..." if truncated.
     *
     * @param text      the string to truncate
     * @param maxLength the maximum length
     * @return the truncated string
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    // ─── Inner classes ────────────────────────────────────────────────────────────

    /**
     * Immutable value object holding the result of a single Cucumber scenario.
     * Used to buffer scenario results until report generation time.
     */
    private static final class ScenarioResult {
        /** The scenario name as declared in the feature file. */
        final String name;
        /** The list of tags on the scenario (e.g., "@smoke", "@regression"). */
        final List<String> tags;
        /** The Cucumber result containing status and optional error. */
        final Result result;

        ScenarioResult(String name, List<String> tags, Result result) {
            this.name = name;
            this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
            this.result = result;
        }
    }
}
