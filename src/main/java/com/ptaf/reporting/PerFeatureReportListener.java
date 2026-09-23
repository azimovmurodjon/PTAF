package com.ptaf.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestStepFinished;
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
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * Map from test case ID → ExtentTest node, built as scenarios start.
     * Used to associate step results and embedded screenshots with the correct scenario node.
     */
    private final Map<UUID, ExtentTest> activeTests = new ConcurrentHashMap<>();

    /**
     * Map from test case ID → feature URI, used to look up the right ExtentReports instance.
     */
    private final Map<UUID, URI> testCaseUriMap = new ConcurrentHashMap<>();

    /**
     * Map from feature URI → ExtentReports instance.
     * One ExtentReports per feature, created lazily when the first scenario of that feature starts.
     */
    private final Map<URI, ExtentReports> featureExtentMap = new ConcurrentHashMap<>();

    /**
     * Map from feature URI → HTML output path.
     * Set when the first scenario of each feature starts.
     */
    private final Map<URI, String> featureHtmlPathMap = new ConcurrentHashMap<>();

    /**
     * Map from test case ID → ScenarioResult, so onTestStepFinished and onEmbedEvent
     * can record step/screenshot data into the ScenarioResult for PDF generation.
     * Populated when a scenario starts (onTestCaseStarted) and removed when it finishes.
     */
    private final Map<UUID, ScenarioResult> activeScenarioResults = new ConcurrentHashMap<>();

    /**
     * Map from feature URI → Feature-level ExtentTest parent node.
     * Scenarios are added as children of this node so the HTML hierarchy matches
     * the combined Spark.html (Feature → Scenario → Step).
     */
    private final Map<URI, ExtentTest> featureParentNodes = new ConcurrentHashMap<>();

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
        publisher.registerHandlerFor(TestCaseStarted.class, this::onTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestStepFinished.class, this::onTestStepFinished);
        publisher.registerHandlerFor(EmbedEvent.class, this::onEmbedEvent);
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
        UUID id = event.getTestCase().getId();

        // Build the final ScenarioResult with the real result status.
        // Transfer accumulated steps and screenshots from the active scenario result.
        ScenarioResult finalSr = new ScenarioResult(scenarioName, tags, result);
        ScenarioResult activeSr = activeScenarioResults.remove(id);
        if (activeSr != null) {
            finalSr.steps.addAll(activeSr.steps);
            finalSr.screenshots.addAll(activeSr.screenshots);
        }
        featureScenarios.computeIfAbsent(uri, k -> new ArrayList<>()).add(finalSr);

        logger.debug("PTAF Reporting | Recorded scenario [{}] status=[{}] for feature [{}]",
            scenarioName, result.getStatus(), uri);
    }

    /**
     * Called when a scenario starts. Creates the ExtentTest node immediately so that
     * step results and screenshots can be attached as child nodes in real time.
     */
    private synchronized void onTestCaseStarted(TestCaseStarted event) {
        if (!ConfigurationProperties.isPerFeatureReportsEnabled()) return;

        URI uri = event.getTestCase().getUri();
        UUID id = event.getTestCase().getId();
        String scenarioName = event.getTestCase().getName();
        List<String> tags = event.getTestCase().getTags();

        // Lazily create the ExtentReports + HTML reporter for this feature
        if (!featureExtentMap.containsKey(uri)) {
            String featureName = featureNames.getOrDefault(uri, extractFileStem(uri));
            String safeFileName = sanitizeFileName(featureName) + "_" + runTimestamp;
            String outputDir = ConfigurationProperties.getPerFeatureReportsOutputDir();
            String htmlPath = outputDir + File.separator + safeFileName + ".html";

            try { Files.createDirectories(Path.of(outputDir)); } catch (Exception ignored) {}

            ExtentSparkReporter spark = new ExtentSparkReporter(htmlPath);
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle(featureName + " — PTAF Report");
            spark.config().setReportName(featureName);
            spark.config().setEncoding("UTF-8");
            spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");
            File extentConfigFile = new File("src/test/resources/extent-config.xml");
            if (extentConfigFile.exists()) {
                try { spark.loadXMLConfig(extentConfigFile); } catch (Exception ignored) {}
            }

            ExtentReports extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Feature", featureName);
            extent.setSystemInfo("Generated by", "PTAF Per-Feature Reporter");
            extent.setSystemInfo("Run timestamp", runTimestamp);

            featureExtentMap.put(uri, extent);
            featureHtmlPathMap.put(uri, htmlPath);

            // Create a Feature-level parent node so the HTML hierarchy matches the
            // combined Spark.html: Feature → Scenario → Step.
            ExtentTest featureNode = extent.createTest(
                com.aventstack.extentreports.gherkin.model.Feature.class, featureName);
            featureParentNodes.put(uri, featureNode);
        }

        // Create the scenario test node
        ExtentReports extent = featureExtentMap.get(uri);
        // Add scenario as a child of the Feature parent node (Gherkin hierarchy)
        ExtentTest featureNode = featureParentNodes.get(uri);
        ExtentTest test = featureNode != null
            ? featureNode.createNode(
                com.aventstack.extentreports.gherkin.model.Scenario.class, scenarioName)
            : extent.createTest(scenarioName);
        for (String tag : tags) {
            test.assignCategory(tag.startsWith("@") ? tag.substring(1) : tag);
        }

        activeTests.put(id, test);
        testCaseUriMap.put(id, uri);

        // Register a ScenarioResult placeholder so onTestStepFinished can record steps.
        // The final result (status) is set in onTestCaseFinished.
        // We use a temporary PASSED result here; it will be replaced in featureScenarios.
        ScenarioResult sr = new ScenarioResult(scenarioName, tags,
            new io.cucumber.plugin.event.Result(
                io.cucumber.plugin.event.Status.PASSED, java.time.Duration.ZERO, null));
        activeScenarioResults.put(id, sr);

        logger.debug("PTAF Reporting | Started scenario [{}] for feature [{}]", scenarioName, uri);
    }

    /**
     * Called when a step finishes. Adds the step as a child node under the scenario's ExtentTest.
     * Hook steps (Before/After) are included with their type as the keyword.
     */
    private void onTestStepFinished(TestStepFinished event) {
        if (!ConfigurationProperties.isPerFeatureReportsEnabled()) return;

        UUID id = event.getTestCase().getId();
        ExtentTest test = activeTests.get(id);
        if (test == null) return;

        io.cucumber.plugin.event.Status status = event.getResult().getStatus();
        Throwable stepError = event.getResult().getError();

        // ── Soft assertion override ───────────────────────────────────────────────
        // When soft_assertions.enabled=true, the Cucumber step status is PASSED even
        // though a failure was caught and recorded in SoftAssertionContext. We check
        // for new soft failures here and override the status to FAILED so the per-
        // feature report accurately reflects the real outcome.
        List<com.ptaf.softassert.SoftAssertionContext.SoftFailure> softFailures =
            com.ptaf.utils.ConfigurationProperties.isSoftAssertionsEnabled()
                ? com.ptaf.softassert.SoftAssertionContext.getUnreportedFailures()
                : java.util.Collections.emptyList();
        if (!softFailures.isEmpty() && status == io.cucumber.plugin.event.Status.PASSED) {
            status = io.cucumber.plugin.event.Status.FAILED;
            // Build a combined error message from all soft failures in this step
            StringBuilder softErrMsg = new StringBuilder();
            for (com.ptaf.softassert.SoftAssertionContext.SoftFailure sf : softFailures) {
                if (softErrMsg.length() > 0) softErrMsg.append("; ");
                softErrMsg.append(sf.errorMessage != null ? sf.errorMessage : "Soft assertion failed");
            }
            if (stepError == null) {
                stepError = new AssertionError(softErrMsg.toString());
            }
        }
        // Mark soft failures as reported so the next step starts fresh
        if (com.ptaf.utils.ConfigurationProperties.isSoftAssertionsEnabled()) {
            com.ptaf.softassert.SoftAssertionContext.markFailuresReported();
        }

        long durationMs = event.getResult().getDuration() != null
            ? event.getResult().getDuration().toMillis() : 0L;
        String durationStr = durationMs < 1000
            ? durationMs + " ms"
            : String.format("%.2f s", durationMs / 1000.0);

        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String keyword = step.getStep().getKeyword() != null ? step.getStep().getKeyword().trim() : "";
            String text = step.getStepText() != null ? step.getStepText() : "";
            String label = "<b>" + keyword + "</b> " + text + " <i>(" + durationStr + ")</i>";

            ExtentTest stepNode = test.createNode(label);
            switch (status) {
                case PASSED   -> stepNode.pass("Step passed");
                case FAILED   -> {
                    stepNode.fail(stepError != null ? stepError.getMessage() : "Step failed");
                }
                case SKIPPED  -> stepNode.skip("Step skipped");
                case PENDING  -> stepNode.warning("Step pending");
                default       -> stepNode.warning("Status: " + status.name());
            }

            // Also record into ScenarioResult for PDF generation
            ScenarioResult sr = activeScenarioResults.get(id);
            if (sr != null) {
                sr.steps.add(new StepResult(keyword, text, status,
                    stepError != null ? stepError.getMessage() : null, durationMs));
                // Pick up any screenshot that arrived via EmbedEvent before this TestStepFinished.
                // EmbedEvent fires before TestStepFinished, so we buffer it in pendingScreenshot
                // and attach it to the step we just added.
                if (sr.pendingScreenshot != null) {
                    StepResult addedStep = sr.steps.get(sr.steps.size() - 1);
                    addedStep.screenshotBase64 = sr.pendingScreenshot[0];
                    String statusWord = status == io.cucumber.plugin.event.Status.FAILED ? "Failure" : "Passed";
                    addedStep.screenshotName = "Screenshot of the " + statusWord + " Step: " + sr.name;
                    sr.pendingScreenshot = null; // consumed
                }
            }
        } else if (event.getTestStep() instanceof HookTestStep) {
            // Only log failed hooks — passing hooks are noise
            if (status == io.cucumber.plugin.event.Status.FAILED) {
                HookTestStep hook = (HookTestStep) event.getTestStep();
                String label = "<b>" + hook.getHookType().name() + "</b> hook failed <i>(" + durationStr + ")</i>";
                ExtentTest hookNode = test.createNode(label);
                Throwable err = event.getResult().getError();
                hookNode.fail(err != null ? err.getMessage() : "Hook failed");
            }
        }
    }

    /**
     * Called when a screenshot or other media is embedded during a step.
     * Attaches the image to the current scenario's ExtentTest node as a Base64 image.
     */
    private void onEmbedEvent(EmbedEvent event) {
        if (!ConfigurationProperties.isPerFeatureReportsEnabled()) return;

        UUID id = event.getTestCase().getId();
        ExtentTest test = activeTests.get(id);
        if (test == null) return;

        String mediaType = event.getMediaType();
        if (mediaType != null && mediaType.startsWith("image/")) {
            try {
                String base64 = Base64.getEncoder().encodeToString(event.getData());
                test.addScreenCaptureFromBase64String(base64,
                    event.getName() != null ? event.getName() : "Screenshot");
                // Also record into ScenarioResult for PDF generation.
                // Attach the screenshot to the LAST step in the active scenario result so that
                // both PASSED and FAILED steps can carry their own screenshot (e.g. explicit
                // "capture screenshot" actions on passed steps, or failure screenshots).
                ScenarioResult sr = activeScenarioResults.get(id);
                if (sr != null) {
                    // Keep the flat screenshots list for backward compatibility
                    String shotName = event.getName() != null ? event.getName() : "Screenshot";
                    sr.screenshots.add(new String[]{base64, shotName});
                    // EmbedEvent fires BEFORE TestStepFinished, so the step that triggered this
                    // screenshot has not yet been added to sr.steps. Buffer it as a pending
                    // screenshot; onTestStepFinished will pick it up and attach it to the step.
                    if (sr.pendingScreenshot == null) {
                        sr.pendingScreenshot = new String[]{base64, shotName};
                    }
                }
            } catch (Exception e) {
                logger.debug("PTAF Reporting | Could not attach screenshot: {}", e.getMessage());
            }
        }
    }

    /**
     * Called when the entire Cucumber test run finishes.
     * If per-feature reports are enabled in config, generates one HTML and optionally one
     * Glass-style PDF report per feature file.
     *
     * @param event the TestRunFinished event
     */
    /**
     * Called when the entire Cucumber test run finishes.
     * Flushes all per-feature ExtentReports instances (which were populated in real time
     * by onTestCaseStarted, onTestStepFinished, and onEmbedEvent) and optionally generates
     * PTAF direct PDFs and Glass-style PDFs for each feature.
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
        try { Files.createDirectories(Path.of(outputDir)); } catch (Exception ignored) {}
        if (glassPdfEnabled) {
            try { Files.createDirectories(Path.of(glassPdfOutputDir)); } catch (Exception ignored) {}
        }

        // ── Get the ExtentReports instance from ExtentService via reflection ──────────
        // ExtentCucumberAdapter populates ExtentService.INSTANCE with all Feature→Scenario→Step
        // data including screenshots, durations, and failure messages. We split this data
        // by feature to generate per-feature HTML reports that are 100% identical to the
        // combined Spark.html — no custom step capture needed.
        List<com.aventstack.extentreports.model.Test> allFeatureTests = null;
        try {
            java.lang.reflect.Field instanceField =
                com.aventstack.extentreports.service.ExtentService.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object serviceInstance = instanceField.get(null);
            if (serviceInstance != null) {
                // INSTANCE is an ExtentReportsLoader (inner class) that holds the ExtentReports
                for (java.lang.reflect.Field f : serviceInstance.getClass().getDeclaredFields()) {
                    if (com.aventstack.extentreports.ExtentReports.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        com.aventstack.extentreports.ExtentReports extentReports =
                            (com.aventstack.extentreports.ExtentReports) f.get(serviceInstance);
                        if (extentReports != null) {
                            allFeatureTests = extentReports.getReport().getTestList();
                            logger.info("PTAF Reporting | Found {} top-level feature tests in ExtentService.",
                                allFeatureTests.size());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("PTAF Reporting | Could not access ExtentService via reflection: {}. " +
                "Falling back to custom listener data.", e.getMessage());
        }

        // ── Reflect onTestCreated method for registering tests into new ExtentReports ──
        java.lang.reflect.Method onTestCreated = null;
        try {
            onTestCreated = com.aventstack.extentreports.ExtentReports.class.getSuperclass()
                .getDeclaredMethod("onTestCreated", com.aventstack.extentreports.model.Test.class);
            onTestCreated.setAccessible(true);
        } catch (Exception e) {
            logger.warn("PTAF Reporting | Could not reflect onTestCreated: {}", e.getMessage());
        }

        int reportCount = 0;

        // ── Primary path: split ExtentService data by feature ─────────────────────────
        if (allFeatureTests != null && onTestCreated != null && !allFeatureTests.isEmpty()) {
            final java.lang.reflect.Method finalOnTestCreated = onTestCreated;
            for (com.aventstack.extentreports.model.Test featureTest : allFeatureTests) {
                String featureName = featureTest.getName();
                String safeFileName = sanitizeFileName(featureName) + "_" + runTimestamp;
                String htmlPath = outputDir + File.separator + safeFileName + ".html";
                String pdfPath  = outputDir + File.separator + safeFileName + ".pdf";

                // Generate per-feature HTML (identical to combined Spark.html for this feature)
                try {
                    ExtentReports perFeature = new ExtentReports();
                    ExtentSparkReporter spark = new ExtentSparkReporter(htmlPath);
                    spark.config().setTheme(Theme.DARK);
                    spark.config().setDocumentTitle(featureName + " — PTAF Report");
                    spark.config().setReportName(featureName);
                    spark.config().setEncoding("UTF-8");
                    spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");
                    File extentConfigFile = new File("src/test/resources/extent-config.xml");
                    if (extentConfigFile.exists()) {
                        try { spark.loadXMLConfig(extentConfigFile); } catch (Exception ignored) {}
                    }
                    perFeature.attachReporter(spark);
                    perFeature.setSystemInfo("Feature", featureName);
                    perFeature.setSystemInfo("Generated by", "PTAF Per-Feature Reporter");
                    perFeature.setSystemInfo("Run timestamp", runTimestamp);

                    // Register feature + all Scenario/Step children recursively so the
                    // per-feature HTML is identical to the combined Spark.html (all steps,
                    // durations, screenshots, and error messages included).
                    registerTestTree(perFeature, finalOnTestCreated, featureTest);
                    perFeature.flush();
                    reportCount++;
                    logger.info("PTAF Reporting | Generated HTML for [{}] → {}", featureName, htmlPath);
                } catch (Exception e) {
                    logger.error("PTAF Reporting | Failed to generate HTML for [{}]: {}", featureName, e.getMessage(), e);
                }

                // PTAF direct PDF (uses ScenarioResult data from our listener)
                if (pdfEnabled) {
                    List<ScenarioResult> scenarios = featureScenarios.entrySet().stream()
                        .filter(entry -> featureNames.getOrDefault(entry.getKey(),
                            extractFileStem(entry.getKey())).equals(featureName))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(Collections.emptyList());
                    if (!scenarios.isEmpty()) {
                        try {
                            buildAndWriteGlassPdf(featureName, scenarios, pdfPath);
                            logger.info("PTAF Reporting | Generated PTAF PDF for [{}] → {}", featureName, pdfPath);
                        } catch (Exception e) {
                            logger.warn("PTAF Reporting | PTAF PDF failed for [{}]: {}", featureName, e.getMessage(), e);
                        }
                    }
                }

                // Glass PDF via subprocess
                if (glassPdfEnabled) {
                    List<ScenarioResult> scenarios = featureScenarios.entrySet().stream()
                        .filter(entry -> featureNames.getOrDefault(entry.getKey(),
                            extractFileStem(entry.getKey())).equals(featureName))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(Collections.emptyList());
                    String glassPdfPath = glassPdfOutputDir + File.separator + safeFileName + ".pdf";
                    if (!scenarios.isEmpty()) {
                        try {
                            generateGlassPdfSubprocess(featureName, scenarios, glassPdfPath);
                            logger.info("PTAF Reporting | Generated Glass PDF for [{}] → {}", featureName, glassPdfPath);
                        } catch (Exception e) {
                            logger.warn("PTAF Reporting | Glass PDF subprocess failed for [{}]: {}", featureName, e.getMessage(), e);
                        }
                    }
                }
            }
        } else {
            // ── Fallback path: use our own ExtentReports instances ─────────────────────
            logger.info("PTAF Reporting | Using fallback per-feature ExtentReports instances.");
            for (Map.Entry<URI, ExtentReports> entry : featureExtentMap.entrySet()) {
                URI uri = entry.getKey();
                ExtentReports extent = entry.getValue();
                List<ScenarioResult> scenarios = featureScenarios.getOrDefault(uri, Collections.emptyList());
                if (scenarios.isEmpty()) continue;

                String featureName = featureNames.getOrDefault(uri, extractFileStem(uri));
                String safeFileName = sanitizeFileName(featureName) + "_" + runTimestamp;
                String htmlPath = featureHtmlPathMap.getOrDefault(uri,
                    outputDir + File.separator + safeFileName + ".html");
                String pdfPath  = outputDir + File.separator + safeFileName + ".pdf";

                try {
                    extent.flush();
                    reportCount++;
                    logger.info("PTAF Reporting | Generated HTML (fallback) for [{}] → {}", featureName, htmlPath);
                } catch (Exception e) {
                    logger.error("PTAF Reporting | Failed to flush HTML for [{}]: {}", featureName, e.getMessage(), e);
                }

                if (pdfEnabled) {
                    try {
                        buildAndWriteGlassPdf(featureName, scenarios, pdfPath);
                    } catch (Exception e) {
                        logger.warn("PTAF Reporting | PTAF PDF failed for [{}]: {}", featureName, e.getMessage(), e);
                    }
                }

                if (glassPdfEnabled) {
                    String glassPdfPath = glassPdfOutputDir + File.separator + safeFileName + ".pdf";
                    try {
                        generateGlassPdfSubprocess(featureName, scenarios, glassPdfPath);
                    } catch (Exception e) {
                        logger.warn("PTAF Reporting | Glass PDF subprocess failed for [{}]: {}", featureName, e.getMessage(), e);
                    }
                }
            }
        }

        logger.info("PTAF Reporting | Per-feature report generation complete. {} report(s) written to: {}",
            reportCount, outputDir);
    }


    /**
     * Recursively registers a {@code model.Test} node and all its children into the given
     * {@link ExtentReports} instance via the protected {@code onTestCreated} method.
     * This ensures the per-feature HTML contains all Feature → Scenario → Step data,
     * including logs, screenshots, and durations — identical to the combined Spark.html.
     *
     * @param extent        the per-feature ExtentReports instance to register tests into
     * @param onTestCreated the reflected {@code AbstractProcessor.onTestCreated} method
     * @param test          the model.Test node to register (Feature, Scenario, or Step)
     * @throws Exception    if reflection invocation fails
     */
    private void registerTestTree(ExtentReports extent,
                                   java.lang.reflect.Method onTestCreated,
                                   com.aventstack.extentreports.model.Test test) throws Exception {
        onTestCreated.invoke(extent, test);
        for (com.aventstack.extentreports.model.Test child : test.getChildren()) {
            registerTestTree(extent, onTestCreated, child);
        }
    }

    // ─── Report Generation ────────────────────────────────────────────────────────

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

                    // ── Step rows under this scenario ────────────────────────────────────────────
                    for (StepResult step : sr.steps) {
                        if (y < margin + 20) {
                            currentCs.close();
                            PDPage nextPage = new PDPage(PDRectangle.A4);
                            doc.addPage(nextPage);
                            currentPage = nextPage;
                            currentCs = new PDPageContentStream(doc, nextPage);
                            y = pageH - margin;
                        }

                        float stepRowH = 14;
                        float[] stepColor;
                        String stepLabel;
                        if (step.status == io.cucumber.plugin.event.Status.PASSED) {
                            stepColor = new float[]{0.10f, 0.55f, 0.25f};
                            stepLabel = "✓";
                        } else if (step.status == io.cucumber.plugin.event.Status.FAILED) {
                            stepColor = new float[]{0.75f, 0.15f, 0.15f};
                            stepLabel = "✗";
                        } else {
                            stepColor = new float[]{0.60f, 0.45f, 0.10f};
                            stepLabel = "~";
                        }

                        // Step keyword + text
                        String stepText = step.keyword + " " + step.text;
                        int maxStep = 90;
                        String displayStep = stepText.length() > maxStep
                            ? stepText.substring(0, maxStep - 3) + "..." : stepText;
                        String durationStr = step.durationMs < 1000
                            ? step.durationMs + "ms"
                            : String.format("%.1fs", step.durationMs / 1000.0);

                        // Step status dot
                        currentCs.setNonStrokingColor(stepColor[0], stepColor[1], stepColor[2]);
                        currentCs.addRect(margin + 20, y - stepRowH + 4, 6, 6);
                        currentCs.fill();

                        currentCs.beginText();
                        currentCs.setFont(regular, 8);
                        currentCs.setNonStrokingColor(0.30f, 0.30f, 0.30f);
                        currentCs.newLineAtOffset(margin + 30, y - stepRowH + 4);
                        currentCs.showText(displayStep);
                        currentCs.endText();

                        // Duration on right
                        currentCs.beginText();
                        currentCs.setFont(regular, 7);
                        currentCs.setNonStrokingColor(0.55f, 0.55f, 0.55f);
                        currentCs.newLineAtOffset(margin + contentW - 40, y - stepRowH + 4);
                        currentCs.showText(durationStr);
                        currentCs.endText();

                        y -= stepRowH;

                        // Error message for failed steps
                        if (step.status == io.cucumber.plugin.event.Status.FAILED
                                && step.errorMessage != null && !step.errorMessage.isEmpty()) {
                            if (y < margin + 20) {
                                currentCs.close();
                                PDPage nextPage2 = new PDPage(PDRectangle.A4);
                                doc.addPage(nextPage2);
                                currentPage = nextPage2;
                                currentCs = new PDPageContentStream(doc, nextPage2);
                                y = pageH - margin;
                            }
                            String errMsg = step.errorMessage.length() > 100
                                ? step.errorMessage.substring(0, 97) + "..." : step.errorMessage;
                            currentCs.beginText();
                            currentCs.setFont(regular, 7);
                            currentCs.setNonStrokingColor(0.75f, 0.15f, 0.15f);
                            currentCs.newLineAtOffset(margin + 30, y - 10);
                            currentCs.showText("  " + errMsg);
                            currentCs.endText();
                            y -= 12;
                        }
                    }
                    y -= 4; // small gap between scenarios
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

        // ── Write JSON data file with full step/screenshot data ───────────────────
        // Using a temp JSON file avoids command-line argument length limits and
        // allows passing step text, durations, error messages, and screenshots.
        java.io.File jsonFile = java.io.File.createTempFile("ptaf_glass_", ".json");
        jsonFile.deleteOnExit();

        // Track temp PNG files written for screenshots - cleaned up in finally block
        List<java.io.File> tempPngFiles = new ArrayList<>();

        try {
            // Build JSON manually (no Jackson/Gson dependency needed for writing)
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"featureName\": ").append(jsonStr(featureName)).append(",\n");
            json.append("  \"startTime\": \"").append(java.time.LocalDateTime.now()).append("\",\n");
            json.append("  \"endTime\": \"").append(java.time.LocalDateTime.now()).append("\",\n");
            json.append("  \"scenarios\": [\n");

            for (int i = 0; i < scenarios.size(); i++) {
                ScenarioResult sr = scenarios.get(i);
                String scStatus = sr.result.getStatus().name();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                json.append("    {\n");
                json.append("      \"name\": ").append(jsonStr(sr.name)).append(",\n");
                json.append("      \"status\": \"").append(scStatus).append("\",\n");
                json.append("      \"startTime\": \"").append(now).append("\",\n");
                json.append("      \"endTime\": \"").append(now).append("\",\n");

                // Steps
                json.append("      \"steps\": [\n");
                for (int j = 0; j < sr.steps.size(); j++) {
                    StepResult step = sr.steps.get(j);
                    json.append("        {\n");
                    json.append("          \"keyword\": ").append(jsonStr(step.keyword)).append(",\n");
                    json.append("          \"text\": ").append(jsonStr(step.text)).append(",\n");
                    json.append("          \"status\": \"").append(step.status.name()).append("\",\n");
                    json.append("          \"durationMs\": ").append(step.durationMs).append(",\n");
                    json.append("          \"errorMessage\": ").append(jsonStr(step.errorMessage));
                    // Attach the step's own screenshot (PASSED or FAILED) if one was captured during this step.
                    // This handles both explicit "capture screenshot" actions on passed steps AND failure screenshots.
                    if (step.screenshotBase64 != null && !step.screenshotBase64.isBlank()) {
                        try {
                            byte[] pngBytes = java.util.Base64.getDecoder().decode(step.screenshotBase64);
                            java.io.File tmpPng = java.io.File.createTempFile("ptaf_shot_", ".png");
                            tmpPng.deleteOnExit();
                            java.nio.file.Files.write(tmpPng.toPath(), pngBytes);
                            tempPngFiles.add(tmpPng);
                            json.append(",\n          \"mediaPath\": ").append(jsonStr(tmpPng.getAbsolutePath()));
                            // Use the per-step label ("Screenshot of the Passed/Failure Step: <scenarioName>")
                            String label = step.screenshotName != null ? step.screenshotName
                                : (step.status == io.cucumber.plugin.event.Status.FAILED
                                    ? "Screenshot of the Failure Step: " + sr.name
                                    : "Screenshot of the Passed Step: " + sr.name);
                            json.append(",\n          \"screenshotName\": ").append(jsonStr(label));
                        } catch (Exception shotEx) {
                            logger.warn("PTAF Reporting | Could not write screenshot PNG: {}", shotEx.getMessage());
                        }
                    }
                    json.append("\n");
                    json.append("        }");
                    if (j < sr.steps.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("      ],\n");

                // Screenshots (base64 — only include if not too large, max 500KB each)
                json.append("      \"screenshots\": [\n");
                List<String[]> shots = sr.screenshots;
                int shotCount = 0;
                for (String[] shot : shots) {
                    if (shot[0] != null && shot[0].length() < 700_000) { // ~500KB base64
                        if (shotCount > 0) json.append(",\n");
                        json.append("        {\n");
                        json.append("          \"base64\": ").append(jsonStr(shot[0])).append(",\n");
                        json.append("          \"name\": ").append(jsonStr(shot.length > 1 ? shot[1] : "Screenshot")).append("\n");
                        json.append("        }");
                        shotCount++;
                    }
                }
                json.append("\n      ]\n");

                json.append("    }");
                if (i < scenarios.size() - 1) json.append(",");
                json.append("\n");
            }

            json.append("  ]\n}\n");

            java.nio.file.Files.writeString(jsonFile.toPath(), json.toString());

            // ── Build subprocess command ──────────────────────────────────────────
            String javaHome = System.getProperty("java.home");
            String javaExe  = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
            if (!new java.io.File(javaExe).exists()) javaExe = "java";

            String classpath = System.getProperty("java.class.path");

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExe);
            cmd.add("-cp");
            cmd.add(classpath);
            cmd.add(GlassPdfSubprocessGenerator.class.getName());
            cmd.add(glassPdfPath);
            cmd.add(jsonFile.getAbsolutePath());

            logger.debug("PTAF Reporting | Spawning Glass PDF subprocess for [{}]", featureName);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process process = pb.start();

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

            boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            stdoutThread.join(5000);
            stderrThread.join(5000);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Glass PDF subprocess timed out for feature: " + featureName);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errMsg = stderr.toString().trim();
                throw new RuntimeException("Glass PDF subprocess failed (exit " + exitCode
                    + ") for [" + featureName + "]: " + errMsg);
            }

            java.io.File pdf = new java.io.File(glassPdfPath);
            if (!pdf.exists() || pdf.length() == 0) {
                throw new RuntimeException("Glass PDF subprocess succeeded but file is missing/empty: " + glassPdfPath);
            }

            logger.debug("PTAF Reporting | Glass PDF subprocess completed: {} ({} bytes)", glassPdfPath, pdf.length());

        } finally {
            // Always clean up the temp JSON file and any temp PNG screenshot files
            try { jsonFile.delete(); } catch (Exception ignored) {}
            for (java.io.File tmpPng : tempPngFiles) {
                try { tmpPng.delete(); } catch (Exception ignored) {}
            }
        }
    }

    /** Escape a string for JSON output. Returns "null" if the input is null. */
    private static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                         .replace("\"", "\\\"")
                         .replace("\n", "\\n")
                         .replace("\r", "\\r")
                         .replace("\t", "\\t") + "\"";
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
        /** Step-level results captured by onTestStepFinished. */
        final List<StepResult> steps = new ArrayList<>();
        /** Base64-encoded screenshots attached during this scenario. */
        final List<String[]> screenshots = new ArrayList<>(); // [base64, name]
        /**
         * Screenshot that arrived via EmbedEvent BEFORE the corresponding TestStepFinished.
         * EmbedEvent fires before TestStepFinished, so we buffer the screenshot here and
         * attach it to the step when TestStepFinished fires.
         * Format: [base64, shotName]
         */
        String[] pendingScreenshot;

        ScenarioResult(String name, List<String> tags, Result result) {
            this.name = name;
            this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
            this.result = result;
        }
    }

    /**
     * Immutable value object holding the result of a single Cucumber step.
     */
    private static final class StepResult {
        final String keyword;
        final String text;
        final io.cucumber.plugin.event.Status status;
        final String errorMessage;
        final long durationMs;
        String screenshotBase64;  // set by onEmbedEvent when a screenshot is captured during this step
        String screenshotName;    // label for the screenshot (e.g. "Screenshot of the Passed Step: ...")

        StepResult(String keyword, String text, io.cucumber.plugin.event.Status status,
                   String errorMessage, long durationMs) {
            this.keyword = keyword != null ? keyword.trim() : "";
            this.text = text != null ? text : "";
            this.status = status;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }
    }
}
