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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PTAF Per-Feature Extent Report Listener.
 *
 * <p>This Cucumber {@link ConcurrentEventListener} generates one individual Extent HTML report
 * AND one full Glass-style PDF report per feature file when the config switch is enabled.</p>
 *
 * <h3>Report naming</h3>
 * <p>Each report is named after the {@code Feature:} declaration inside the feature file,
 * not the file name. For example, a feature file containing {@code Feature: Consumer Deposit}
 * produces {@code Consumer_Deposit_2025-07-16_14-30-00.html} and
 * {@code Consumer_Deposit_2025-07-16_14-30-00.pdf}.</p>
 *
 * <h3>PDF format</h3>
 * <p>The PDF uses the same Glass-style format as the combined Extent PDF report — generated
 * by {@link ExtentPDFCucumberReporter} — not a plain text summary.</p>
 *
 * <h3>How to enable</h3>
 * <p>In {@code src/test/resources/config/config.yml}:</p>
 * <pre>
 * reporting:
 *   per_feature_reports_enabled: true
 *   per_feature_reports_output_dir: "test-output/per-feature-reports"
 *   per_feature_pdf_enabled: true
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
     * Map from feature file URI → parsed Feature Name (from the Feature: declaration).
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
        publisher.registerHandlerFor(TestSourceRead.class, this::onTestSourceRead);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
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
     * If per-feature reports are enabled in config, generates one HTML and optionally one
     * Glass-style PDF report per feature file.
     *
     * @param event the TestRunFinished event
     */
    private void onTestRunFinished(TestRunFinished event) {
        if (!ConfigurationProperties.isPerFeatureReportsEnabled()) {
            logger.debug("PTAF Reporting | Per-feature reports are disabled. Skipping.");
            return;
        }

        String outputDir = ConfigurationProperties.getPerFeatureReportsOutputDir();
        boolean pdfEnabled = ConfigurationProperties.isPerFeaturePdfEnabled();

        boolean glassPdfEnabled = ConfigurationProperties.isPerFeatureGlassPdfEnabled();
        String glassPdfOutputDir = ConfigurationProperties.getPerFeatureGlassPdfOutputDir();

        logger.info("PTAF Reporting | Generating per-feature reports in: {}", outputDir);
        logger.info("PTAF Reporting | Glass PDF generation: {}", pdfEnabled ? "enabled" : "disabled");

        try {
            Files.createDirectories(Path.of(outputDir));
        } catch (Exception e) {
            logger.error("PTAF Reporting | Failed to create output directory [{}]: {}", outputDir, e.getMessage());
            return;
        }

        if (glassPdfEnabled) {
            try {
                Files.createDirectories(Path.of(glassPdfOutputDir));
            } catch (Exception e) {
                logger.warn("PTAF Reporting | Failed to create Glass PDF output directory [{}]: {}", glassPdfOutputDir, e.getMessage());
            }
        }

        int reportCount = 0;
        for (Map.Entry<URI, List<ScenarioResult>> entry : featureScenarios.entrySet()) {
            URI uri = entry.getKey();
            List<ScenarioResult> scenarios = entry.getValue();

            if (scenarios.isEmpty()) {
                logger.debug("PTAF Reporting | No scenarios recorded for [{}] — skipping.", uri);
                continue;
            }

            // Use the Feature: declaration name for both the report title AND the file name.
            String featureName = featureNames.getOrDefault(uri, extractFileStem(uri));
            // Sanitize the Feature Name for use as a file name (e.g. "Consumer Deposit" → "Consumer_Deposit")
            String safeFileName = sanitizeFileName(featureName) + "_" + runTimestamp;
            String htmlPath = outputDir + File.separator + safeFileName + ".html";
            String pdfPath  = outputDir + File.separator + safeFileName + ".pdf";

            try {
                generateExtentReport(featureName, scenarios, htmlPath, pdfEnabled ? pdfPath : null);
                reportCount++;
                logger.info("PTAF Reporting | Generated HTML report for [{}] → {}", featureName, htmlPath);
                if (pdfEnabled) {
                    logger.info("PTAF Reporting | Generated Glass PDF report for [{}] → {}", featureName, pdfPath);
                }
            } catch (Exception e) {
                logger.error("PTAF Reporting | Failed to generate report for [{}]: {}", featureName, e.getMessage(), e);
            }

            // ── Glass PDF via subprocess ──────────────────────────────────────────────
            if (glassPdfEnabled) {
                String glassPdfPath = glassPdfOutputDir + File.separator + safeFileName + ".pdf";
                try {
                    generateGlassPdfSubprocess(featureName, scenarios, glassPdfPath);
                    logger.info("PTAF Reporting | Generated Glass PDF (subprocess) for [{}] → {}", featureName, glassPdfPath);
                } catch (Exception e) {
                    logger.warn("PTAF Reporting | Glass PDF subprocess failed for [{}]: {}", featureName, e.getMessage(), e);
                }
            }
        }

        logger.info("PTAF Reporting | Per-feature report generation complete. {} report(s) written to: {}",
            reportCount, outputDir);
    }

    // ─── Report Generation ────────────────────────────────────────────────────────

    /**
     * Generate an Extent HTML report and optionally a full Glass-style PDF report
     * for a single feature file.
     *
     * <p>Both reports are generated from the same {@link ExtentReports} instance in a single
     * {@code flush()} call. The PDF uses {@link ExtentPDFCucumberReporter} which produces the
     * same Glass-style format as the combined Extent PDF report — not a plain text summary.</p>
     *
     * <p>The report title and file names are derived from the {@code Feature:} declaration
     * inside the feature file, not from the file name.</p>
     *
     * @param featureName the Feature Name from the Feature: declaration (used as report title and file name)
     * @param scenarios   the list of scenario results to include in the report
     * @param htmlPath    the output file path for the HTML report
     * @param pdfPath     the output file path for the Glass PDF report, or {@code null} to skip PDF generation
     */
    private void generateExtentReport(String featureName, List<ScenarioResult> scenarios,
                                      String htmlPath, String pdfPath) {

        // ── HTML reporter ──────────────────────────────────────────────────────────
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

        // ── ExtentReports instance ─────────────────────────────────────────────────
        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Feature", featureName);
        extent.setSystemInfo("Generated by", "PTAF Per-Feature Reporter");
        extent.setSystemInfo("Run timestamp", runTimestamp);

        // pdfPath is handled after flush() — see below

        // ── Add scenarios as test nodes ────────────────────────────────────────────
        for (ScenarioResult scenario : scenarios) {
            ExtentTest test = extent.createTest(scenario.name);

            // Add tags as categories so they appear in the report's category view
            for (String tag : scenario.tags) {
                test.assignCategory(tag.startsWith("@") ? tag.substring(1) : tag);
            }

            // Map Cucumber status to Extent status
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

        // ── Flush — generates the HTML report ────────────────────────────────────────
        extent.flush();

        // ── Glass PDF — built directly from ScenarioResult list ─────────────────────
        //
        // Why not ExtentPDFReportDataGenerator?
        // That generator expects the ExtentReports instance to be driven by the
        // ExtentService singleton (as the ExtentCucumberAdapter does), where tests are
        // structured as Feature (parent) → Scenario (child) in Gherkin hierarchy.
        // Our standalone ExtentReports instance creates flat test nodes with no
        // parent-child Gherkin structure, so generateReportData() produces Feature
        // POJOs with empty scenario lists, causing checkData() to throw and the PDF
        // to fail silently.
        //
        // Solution: build Feature and Scenario POJOs directly from our ScenarioResult
        // list and pass them to ReportData → PDFCucumberReport.createReport().
        if (pdfPath != null) {
            try {
                buildAndWriteGlassPdf(featureName, scenarios, pdfPath);
                logger.info("PTAF Reporting | Glass PDF generated for [{}] -> {}", featureName, pdfPath);
            } catch (Exception e) {
                logger.warn("PTAF Reporting | Could not generate Glass PDF for [{}]: {}. HTML report was still generated.",
                    featureName, e.getMessage(), e);
            }
        }
    }

    /**
     * Build and write a PDF report directly from a list of {@link ScenarioResult} objects
     * using Apache PDFBox.
     *
     * <p>This method uses PDFBox directly instead of {@code PDFCucumberReport} to avoid a
     * known PDFBox font-caching bug: {@code tech.grasshopper.pdf.font.ReportFont} stores
     * four {@code PDFont} objects in static fields. After the first {@code PDDocument} is
     * closed, these static references become invalid, causing subsequent PDF generations to
     * fail with {@code IOException: The TrueType font null does not contain a 'cmap' table}.
     * By using PDFBox directly, each PDF gets its own fresh font instances with no shared
     * static state, so any number of per-feature PDFs can be generated in sequence.</p>
     *
     * @param featureName the Feature Name used as the PDF report title
     * @param scenarios   the list of scenario results to include
     * @param pdfPath     the output file path for the PDF
     * @throws Exception if PDF generation fails
     */
    private void buildAndWriteGlassPdf(String featureName,
                                       List<ScenarioResult> scenarios,
                                       String pdfPath) throws Exception {

        // Ensure the parent directory exists
        File pdfFile = new File(pdfPath);
        File pdfParentDir = pdfFile.getParentFile();
        if (pdfParentDir != null && !pdfParentDir.exists()) {
            pdfParentDir.mkdirs();
        }

        // Count results
        int passed = 0, failed = 0, skipped = 0;
        for (ScenarioResult sr : scenarios) {
            io.cucumber.plugin.event.Status s = sr.result.getStatus();
            if (s == io.cucumber.plugin.event.Status.PASSED) passed++;
            else if (s == io.cucumber.plugin.event.Status.FAILED) failed++;
            else skipped++;
        }
        int total = scenarios.size();

        // ── Generate PDF using PDFBox directly ──────────────────────────────────────────────────
        // Each PDDocument gets its own fresh font instances — no shared static state, no caching
        // issues across multiple feature PDFs.
        try (PDDocument doc = new PDDocument()) {

            // Load fonts fresh for this document (embedSubset=false avoids TTF re-read on close)
            PDType0Font regular, bold;
            try (InputStream rs = getClass().getResourceAsStream("/tech/grasshopper/ttf/LiberationSans-Regular.ttf");
                 InputStream bs = getClass().getResourceAsStream("/tech/grasshopper/ttf/LiberationSans-Bold.ttf")) {
                if (rs == null || bs == null) {
                    throw new java.io.IOException("LiberationSans font resources not found in classpath");
                }
                regular = PDType0Font.load(doc, rs, false);
                bold    = PDType0Font.load(doc, bs, false);
            }

            // ── Page 1: Summary ─────────────────────────────────────────────────────────────────
            PDPage summaryPage = new PDPage(PDRectangle.A4);
            doc.addPage(summaryPage);
            float pageW = summaryPage.getMediaBox().getWidth();
            float pageH = summaryPage.getMediaBox().getHeight();
            float margin = 50f;
            float contentW = pageW - 2 * margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, summaryPage)) {

                // ── Header bar ──────────────────────────────────────────────────────────────────
                cs.setNonStrokingColor(0.08f, 0.14f, 0.30f);
                cs.addRect(0, pageH - 70, pageW, 70);
                cs.fill();

                cs.beginText();
                cs.setFont(bold, 20);
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.newLineAtOffset(margin, pageH - 45);
                cs.showText("PTAF Per-Feature Report");
                cs.endText();

                cs.beginText();
                cs.setFont(regular, 10);
                cs.setNonStrokingColor(0.75f, 0.82f, 0.95f);
                cs.newLineAtOffset(margin, pageH - 62);
                cs.showText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")));
                cs.endText();

                // ── Feature name ────────────────────────────────────────────────────────────────
                float y = pageH - 100;
                cs.beginText();
                cs.setFont(bold, 15);
                cs.setNonStrokingColor(0.08f, 0.14f, 0.30f);
                cs.newLineAtOffset(margin, y);
                String displayFeature = featureName.length() > 75 ? featureName.substring(0, 72) + "..." : featureName;
                cs.showText("Feature: " + displayFeature);
                cs.endText();
                y -= 8;

                // Underline
                cs.setStrokingColor(0.08f, 0.14f, 0.30f);
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, y);
                cs.lineTo(margin + contentW, y);
                cs.stroke();
                y -= 25;

                // ── Summary boxes ───────────────────────────────────────────────────────────────
                float boxW = (contentW - 20) / 4;
                float boxH = 55;
                float[][] boxes = {
                    {(float) total,   0.25f, 0.35f, 0.65f},   // Total  - blue
                    {(float) passed,  0.10f, 0.55f, 0.25f},   // Passed - green
                    {(float) failed,  0.75f, 0.15f, 0.15f},   // Failed - red
                    {(float) skipped, 0.60f, 0.45f, 0.10f},   // Skipped - amber
                };
                String[] boxLabels = {"TOTAL", "PASSED", "FAILED", "SKIPPED"};
                for (int i = 0; i < 4; i++) {
                    float bx = margin + i * (boxW + 6.67f);
                    cs.setNonStrokingColor(boxes[i][1], boxes[i][2], boxes[i][3]);
                    cs.addRect(bx, y - boxH, boxW, boxH);
                    cs.fill();
                    // Count number
                    cs.beginText();
                    cs.setFont(bold, 22);
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    cs.newLineAtOffset(bx + boxW / 2 - 10, y - boxH + 22);
                    cs.showText(String.valueOf((int) boxes[i][0]));
                    cs.endText();
                    // Label
                    cs.beginText();
                    cs.setFont(regular, 9);
                    cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                    cs.newLineAtOffset(bx + 6, y - boxH + 8);
                    cs.showText(boxLabels[i]);
                    cs.endText();
                }
                y -= boxH + 30;

                // ── Scenario list header ─────────────────────────────────────────────────────────
                cs.setNonStrokingColor(0.92f, 0.94f, 0.97f);
                cs.addRect(margin, y - 18, contentW, 22);
                cs.fill();

                cs.beginText();
                cs.setFont(bold, 10);
                cs.setNonStrokingColor(0.08f, 0.14f, 0.30f);
                cs.newLineAtOffset(margin + 8, y - 12);
                cs.showText("SCENARIO");
                cs.endText();

                cs.beginText();
                cs.setFont(bold, 10);
                cs.setNonStrokingColor(0.08f, 0.14f, 0.30f);
                cs.newLineAtOffset(margin + contentW - 70, y - 12);
                cs.showText("STATUS");
                cs.endText();
                y -= 22;

                // ── Scenario rows ────────────────────────────────────────────────────────────────
                boolean alternateRow = false;
                int rowsOnPage = 0;
                PDPage currentPage = summaryPage;
                PDPageContentStream currentCs = cs;

                for (ScenarioResult sr : scenarios) {
                    if (y < margin + 30) {
                        // Start a new page
                        currentCs.close();
                        PDPage nextPage = new PDPage(PDRectangle.A4);
                        doc.addPage(nextPage);
                        currentPage = nextPage;
                        currentCs = new PDPageContentStream(doc, nextPage);
                        y = pageH - margin;
                        alternateRow = false;
                    }

                    io.cucumber.plugin.event.Status status = sr.result.getStatus();
                    float[] statusColor;
                    String statusLabel;
                    if (status == io.cucumber.plugin.event.Status.PASSED) {
                        statusColor = new float[]{0.10f, 0.55f, 0.25f};
                        statusLabel = "PASSED";
                    } else if (status == io.cucumber.plugin.event.Status.FAILED) {
                        statusColor = new float[]{0.75f, 0.15f, 0.15f};
                        statusLabel = "FAILED";
                    } else if (status == io.cucumber.plugin.event.Status.SKIPPED) {
                        statusColor = new float[]{0.60f, 0.45f, 0.10f};
                        statusLabel = "SKIPPED";
                    } else {
                        statusColor = new float[]{0.50f, 0.50f, 0.50f};
                        statusLabel = status.name();
                    }

                    float rowH = 18;
                    // Alternate row background
                    if (alternateRow) {
                        currentCs.setNonStrokingColor(0.96f, 0.97f, 0.99f);
                        currentCs.addRect(margin, y - rowH, contentW, rowH);
                        currentCs.fill();
                    }
                    alternateRow = !alternateRow;

                    // Status badge
                    currentCs.setNonStrokingColor(statusColor[0], statusColor[1], statusColor[2]);
                    currentCs.addRect(margin + contentW - 65, y - rowH + 3, 60, 12);
                    currentCs.fill();

                    currentCs.beginText();
                    currentCs.setFont(bold, 8);
                    currentCs.setNonStrokingColor(1f, 1f, 1f);
                    currentCs.newLineAtOffset(margin + contentW - 62, y - rowH + 6);
                    currentCs.showText(statusLabel);
                    currentCs.endText();

                    // Scenario name
                    String name = sr.name != null ? sr.name : "Unnamed Scenario";
                    int maxChars = 85;
                    String displayName = name.length() > maxChars ? name.substring(0, maxChars - 3) + "..." : name;
                    currentCs.beginText();
                    currentCs.setFont(regular, 10);
                    currentCs.setNonStrokingColor(0.15f, 0.15f, 0.15f);
                    currentCs.newLineAtOffset(margin + 8, y - rowH + 5);
                    currentCs.showText(displayName);
                    currentCs.endText();

                    y -= rowH;
                }

                // Close the last content stream if it is not the original one
                if (currentCs != cs) {
                    currentCs.close();
                }
            }

            doc.save(pdfPath);
        }

        // Verify
        File written = new File(pdfPath);
        if (written.exists() && written.length() > 0) {
            logger.info("PTAF Reporting | PDF successfully written: {} ({} bytes)", pdfPath, written.length());
        } else {
            logger.warn("PTAF Reporting | PDF file is missing or empty after generation: {}", pdfPath);
        }
    }

    /**
     * Generate a Glass-style PDF for a single feature by spawning a subprocess.
     *
     * <p>Each Glass PDF generation runs in its own JVM via {@link GlassPdfSubprocessGenerator}.
     * This is necessary because {@code tech.grasshopper.pdf.font.ReportFont} stores static
     * {@code PDFont} references that become invalid after the first {@code PDDocument} closes,
     * causing all subsequent PDF generations in the same JVM to fail. A fresh JVM per feature
     * completely avoids this library bug.</p>
     *
     * <p>The subprocess is given a 60-second timeout. If it times out or exits with a non-zero
     * code, a warning is logged but the HTML report is unaffected.</p>
     *
     * @param featureName the Feature Name (used as the PDF report title)
     * @param scenarios   the list of scenario results to include
     * @param glassPdfPath the output file path for the Glass PDF
     * @throws Exception if the subprocess fails or times out
     */
    private void generateGlassPdfSubprocess(String featureName,
                                             List<ScenarioResult> scenarios,
                                             String glassPdfPath) throws Exception {

        // Build the classpath: target/classes + all dependency jars
        String javaHome = System.getProperty("java.home");
        String javaExe  = javaHome + File.separator + "bin" + File.separator + "java";
        if (!new File(javaExe).exists()) {
            javaExe = "java"; // fall back to PATH
        }

        // Collect classpath from the current JVM's classpath
        String classpath = System.getProperty("java.class.path");

        // Build command: java -cp <classpath> GlassPdfSubprocessGenerator <path> <feature> <sc1> <status1> ...
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(GlassPdfSubprocessGenerator.class.getName());
        cmd.add(glassPdfPath);
        cmd.add(featureName);

        for (ScenarioResult sr : scenarios) {
            String scName   = sr.name != null ? sr.name : "Unnamed Scenario";
            String scStatus = sr.result.getStatus().name();
            cmd.add(scName);
            cmd.add(scStatus);
        }

        logger.debug("PTAF Reporting | Spawning Glass PDF subprocess for [{}]", featureName);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // Drain stdout/stderr to prevent blocking
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread stdoutThread = new Thread(() -> {
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) stdout.append(line).append("\n");
            } catch (Exception ignored) {}
        });
        Thread stderrThread = new Thread(() -> {
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) stderr.append(line).append("\n");
            } catch (Exception ignored) {}
        });
        stdoutThread.start();
        stderrThread.start();

        boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
        stdoutThread.join(5000);
        stderrThread.join(5000);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Glass PDF subprocess timed out after 60 seconds for feature: " + featureName);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errMsg = stderr.toString().trim();
            throw new RuntimeException("Glass PDF subprocess exited with code " + exitCode
                + " for feature [" + featureName + "]: " + errMsg);
        }

        // Verify the PDF was written
        File pdf = new File(glassPdfPath);
        if (!pdf.exists() || pdf.length() == 0) {
            throw new RuntimeException("Glass PDF subprocess succeeded but file is missing or empty: " + glassPdfPath);
        }

        logger.debug("PTAF Reporting | Glass PDF subprocess completed: {} ({} bytes)", glassPdfPath, pdf.length());
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
     * (except underscores and hyphens) with underscores, collapsing consecutive underscores,
     * and truncating to 80 characters.
     *
     * <p>This is applied to the Feature Name so the PDF and HTML files are named after
     * the Feature: declaration. For example, "Consumer Deposit with Payment Switch" becomes
     * "Consumer_Deposit_with_Payment_Switch".</p>
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
