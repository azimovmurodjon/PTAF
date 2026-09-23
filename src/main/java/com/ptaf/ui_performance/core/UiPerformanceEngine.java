package com.ptaf.ui_performance.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitUntilState;
import com.ptaf.ui.handlers.LocatorHandler;
import com.ptaf.ui_performance.config.UiPerformanceConfiguration;
import com.ptaf.ui_performance.data.UiPerformanceUserDataReader;
import com.ptaf.ui_performance.metrics.UiPerformanceMetrics;
import com.ptaf.ui_performance.model.UiPerformanceIterationResult;
import com.ptaf.ui_performance.model.UiPerformanceJourney;
import com.ptaf.ui_performance.model.UiPerformanceRunProfile;
import com.ptaf.ui_performance.model.UiPerformanceRunResult;
import com.ptaf.ui_performance.model.UiPerformanceStage;
import com.ptaf.ui_performance.model.UiPerformanceStageResult;
import com.ptaf.ui_performance.model.UiPerformanceStep;
import com.ptaf.ui_performance.model.UiPerformanceUser;
import com.ptaf.ui_performance.reporting.UiPerformanceExistingReporterAdapter;
import com.ptaf.ui_performance.reporting.UiPerformanceDurationFormatter;
import com.ptaf.ui_performance.reporting.UiPerformanceReportManager;
import com.ptaf.ui_performance.reporting.UiPerformanceReportWriter;
import com.ptaf.ui_performance.reporting.UiPerformanceSensitiveTextSanitizer;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Executes configurable load, stress, spike, and soak tests with concurrent real browsers.
 *
 * <p>Cucumber/TestNG only triggers this engine. Concurrency is owned here. Each requested virtual user
 * receives its own worker thread, Playwright instance, browser process, context, page, and assigned
 * data row. This follows Playwright Java's thread-safety requirement and prevents one-by-one TestNG
 * execution from becoming the load model.</p>
 */
public final class UiPerformanceEngine {
    private static final LocatorHandler LOCATOR_HANDLER = new LocatorHandler();

    private final UiPerformanceReportManager reportManager = new UiPerformanceReportManager();
    private final UiPerformanceReportWriter reportWriter = new UiPerformanceReportWriter();
    private final UiPerformanceExistingReporterAdapter existingReporterAdapter = new UiPerformanceExistingReporterAdapter();

    /** Runs the complete profile selected in the separate UI performance YAML. */
    public UiPerformanceRunResult execute(UiPerformanceJourney journey) {
        UiPerformanceConfiguration.requireEnabled();
        journey.validateForExecution();

        UiPerformanceRunProfile profile = UiPerformanceConfiguration.getRunProfile();
        validateJourneyDataMode(journey);
        List<UiPerformanceUser> availableUsers = UiPerformanceConfiguration.isCsvUserDataEnabled()
                ? UiPerformanceUserDataReader.readUsers(UiPerformanceConfiguration.getUsersCsvPath())
                : UiPerformanceUserDataReader.createSyntheticUsers(profile.executionPlan().maximumVirtualUsers());
        ensureUserCapacity(availableUsers, profile.executionPlan().maximumVirtualUsers(),
                UiPerformanceConfiguration.isUserReuseAllowed());

        Path reportDirectory = reportManager.createRunDirectory(
                UiPerformanceConfiguration.getReportOutputDirectory(),
                journey.getName() + "_" + profile.executionPlan().profileName());
        Instant startedAt = Instant.now();
        long runStartedNanos = System.nanoTime();
        List<UiPerformanceStageResult> stageResults = new ArrayList<>();
        List<UiPerformanceIterationResult> allIterations = new ArrayList<>();

        for (UiPerformanceStage stage : profile.executionPlan().stages()) {
            UiPerformanceStageResult stageResult = executeStage(
                    stage, availableUsers, journey, profile, reportDirectory);
            stageResults.add(stageResult);
            allIterations.addAll(stageResult.getIterations());
        }

        Instant completedAt = Instant.now();
        long runDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - runStartedNanos);
        UiPerformanceMetrics metrics = UiPerformanceMetrics.from(allIterations, runDurationMs);
        UiPerformanceRunResult result = new UiPerformanceRunResult(
                "UI-PERF-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                        .withZone(java.time.ZoneOffset.UTC).format(startedAt),
                journey.getName(),
                URI.create(journey.getBaseUrl()).getHost(),
                startedAt,
                completedAt,
                profile,
                stageResults,
                allIterations,
                metrics,
                reportDirectory);

        reportWriter.write(result,
                UiPerformanceConfiguration.isHtmlReportEnabled(),
                UiPerformanceConfiguration.isPdfReportEnabled(),
                UiPerformanceConfiguration.isCsvReportEnabled(),
                UiPerformanceConfiguration.isJsonReportEnabled());
        if (UiPerformanceConfiguration.isExistingPerformanceReporterEnabled()) {
            existingReporterAdapter.write(result);
        }
        enforceThresholds(result);
        return result;
    }

    /** Fails before browser launch when a data-free profile still references a CSV placeholder. */
    private void validateJourneyDataMode(UiPerformanceJourney journey) {
        if (UiPerformanceConfiguration.isCsvUserDataEnabled()) {
            return;
        }
        boolean placeholderUsed = journey.getSteps().stream()
                .map(UiPerformanceStep::valueTemplate)
                .filter(value -> value != null)
                .anyMatch(value -> value.matches(".*\\$\\{[^}]+}.*"));
        if (placeholderUsed) {
            throw new IllegalStateException(
                    "UI performance journey uses a data-field placeholder while ui_performance.data.use_csv is false. "
                            + "Enable CSV data or remove the data-field steps from this journey.");
        }
    }

    /** Runs one stage; stages are sequential while users inside each stage are concurrent. */
    private UiPerformanceStageResult executeStage(UiPerformanceStage stage,
                                                  List<UiPerformanceUser> availableUsers,
                                                  UiPerformanceJourney journey,
                                                  UiPerformanceRunProfile profile,
                                                  Path reportDirectory) {
        List<UiPerformanceUser> assignedUsers = assignUsers(
                availableUsers, stage.virtualUsers(), UiPerformanceConfiguration.isUserReuseAllowed());
        UiPerformanceStartGate startGate = new UiPerformanceStartGate(stage.virtualUsers());
        AtomicLong stageDeadlineNanos = new AtomicLong(0L);
        ExecutorService executor = Executors.newFixedThreadPool(stage.virtualUsers());
        List<Future<List<UiPerformanceIterationResult>>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < stage.virtualUsers(); index++) {
                futures.add(executor.submit(new VirtualUserTask(
                        index, assignedUsers.get(index), journey, stage, profile,
                        reportDirectory, startGate, stageDeadlineNanos)));
            }
            startGate.awaitAllUsers(profile.synchronizedStartTimeoutMs());
            Instant stageStartedAt = Instant.now();
            long stageStartedNanos = System.nanoTime();
            if (stage.isDurationBased()) {
                stageDeadlineNanos.set(stageStartedNanos
                        + TimeUnit.SECONDS.toNanos(stage.rampUpSeconds() + stage.holdSeconds()));
            }
            startGate.release();

            List<UiPerformanceIterationResult> stageIterations = new ArrayList<>();
            for (Future<List<UiPerformanceIterationResult>> future : futures) {
                stageIterations.addAll(future.get());
            }
            long stageDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stageStartedNanos);
            Instant stageCompletedAt = Instant.now();
            return new UiPerformanceStageResult(
                    stage,
                    stageStartedAt,
                    stageCompletedAt,
                    stageIterations,
                    UiPerformanceMetrics.from(stageIterations, stageDurationMs));
        } catch (Exception exception) {
            startGate.release();
            throw new IllegalStateException("UI performance stage failed before report completion: " + stage.name(), exception);
        } finally {
            executor.shutdownNow();
        }
    }

    private void ensureUserCapacity(List<UiPerformanceUser> users, int maximumUsers, boolean allowReuse) {
        if (users.isEmpty()) {
            throw new IllegalArgumentException("UI performance user CSV contains no usable rows.");
        }
        if (users.size() < maximumUsers && !allowReuse) {
            throw new IllegalArgumentException(
                    "Selected UI performance profile needs " + maximumUsers + " unique users in its largest stage, but only "
                            + users.size() + " are available. Add rows to the separate CSV or explicitly enable reuse for an approved read-only journey.");
        }
    }

    private List<UiPerformanceUser> assignUsers(List<UiPerformanceUser> users, int virtualUsers, boolean allowReuse) {
        ensureUserCapacity(users, virtualUsers, allowReuse);
        List<UiPerformanceUser> assigned = new ArrayList<>();
        for (int index = 0; index < virtualUsers; index++) {
            assigned.add(users.get(index % users.size()));
        }
        return List.copyOf(assigned);
    }

    private void enforceThresholds(UiPerformanceRunResult result) {
        List<String> breaches = new ArrayList<>();
        if (result.getMetrics().failureRatePercent() > result.getProfile().maximumFailureRatePercent()) {
            breaches.add("failure rate " + format(result.getMetrics().failureRatePercent()) + "% exceeds "
                    + format(result.getProfile().maximumFailureRatePercent()) + "%");
        }
        if (result.getMetrics().averageJourneyDurationMs() > result.getProfile().maximumAverageJourneyDurationMs()) {
            breaches.add("average journey duration " + UiPerformanceDurationFormatter.format(result.getMetrics().averageJourneyDurationMs()) + " exceeds "
                    + UiPerformanceDurationFormatter.format(result.getProfile().maximumAverageJourneyDurationMs()));
        }
        if (result.getMetrics().p95JourneyDurationMs() > result.getProfile().maximumP95JourneyDurationMs()) {
            breaches.add("P95 journey duration " + UiPerformanceDurationFormatter.format(result.getMetrics().p95JourneyDurationMs()) + " exceeds "
                    + UiPerformanceDurationFormatter.format(result.getProfile().maximumP95JourneyDurationMs()));
        }
        if (!breaches.isEmpty()) {
            throw new AssertionError("UI performance thresholds were breached. Reports are available at "
                    + result.getReportDirectory() + ". " + String.join("; ", breaches));
        }
    }

    private final class VirtualUserTask implements Callable<List<UiPerformanceIterationResult>> {
        private final int userIndex;
        private final UiPerformanceUser user;
        private final UiPerformanceJourney journey;
        private final UiPerformanceStage stage;
        private final UiPerformanceRunProfile profile;
        private final Path reportDirectory;
        private final UiPerformanceStartGate startGate;
        private final AtomicLong stageDeadlineNanos;

        private VirtualUserTask(int userIndex,
                                UiPerformanceUser user,
                                UiPerformanceJourney journey,
                                UiPerformanceStage stage,
                                UiPerformanceRunProfile profile,
                                Path reportDirectory,
                                UiPerformanceStartGate startGate,
                                AtomicLong stageDeadlineNanos) {
            this.userIndex = userIndex;
            this.user = user;
            this.journey = journey;
            this.stage = stage;
            this.profile = profile;
            this.reportDirectory = reportDirectory;
            this.startGate = startGate;
            this.stageDeadlineNanos = stageDeadlineNanos;
        }

        @Override
        public List<UiPerformanceIterationResult> call() {
            List<UiPerformanceIterationResult> results = new ArrayList<>();
            boolean startGateMarked = false;
            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(profile.headless()));
                try {
                    // Browser startup is intentionally outside measured journey timings.
                    startGate.markVirtualUserReady();
                    startGateMarked = true;
                    startGate.awaitStartSignal();
                    delayForStageRamp();

                    if (stage.isDurationBased()) {
                        executeForDuration(browser, results);
                    } else {
                        executeForIterations(browser, results);
                    }
                } finally {
                    browser.close();
                }
            } catch (Exception exception) {
                if (!startGateMarked) {
                    startGate.markVirtualUserReady();
                }
                int failureCount = stage.isIterationBased()
                        ? Math.max(1, stage.iterationsPerUser() - results.size())
                        : 1;
                for (int offset = 0; offset < failureCount; offset++) {
                    results.add(failedResult(results.size() + 1, 0L, 0L, 0L,
                            Collections.emptyMap(), "BROWSER_LAUNCH_OR_RUNTIME", exception, null, List.of()));
                }
            }
            return results;
        }

        private void executeForIterations(Browser browser, List<UiPerformanceIterationResult> results) {
            for (int iteration = 1; iteration <= stage.iterationsPerUser(); iteration++) {
                results.add(executeIteration(browser, iteration));
                pauseBetweenIterations(iteration < stage.iterationsPerUser());
            }
        }

        private void executeForDuration(Browser browser, List<UiPerformanceIterationResult> results) {
            long deadlineNanos = stageDeadlineNanos.get();
            if (deadlineNanos <= 0L) {
                throw new IllegalStateException("UI performance stage deadline was not initialized: " + stage.name());
            }
            int iteration = 1;
            while (System.nanoTime() < deadlineNanos && !Thread.currentThread().isInterrupted()) {
                results.add(executeIteration(browser, iteration++));
                pauseBetweenIterations(System.nanoTime() < deadlineNanos);
            }
        }

        private void delayForStageRamp() {
            if (stage.rampUpSeconds() == 0 || stage.virtualUsers() == 1) {
                return;
            }
            long delayMillis = Math.round((stage.rampUpSeconds() * 1_000.0 * userIndex)
                    / (stage.virtualUsers() - 1));
            sleep(delayMillis, "during configured stage ramp-up");
        }

        private void pauseBetweenIterations(boolean shouldPause) {
            if (shouldPause && profile.betweenIterationsMs() > 0) {
                sleep(profile.betweenIterationsMs(), "between UI performance journey iterations");
            }
        }

        private void sleep(long delayMillis, String phase) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("UI performance virtual user was interrupted " + phase + ".", exception);
            }
        }

        private UiPerformanceIterationResult executeIteration(Browser browser, int iteration) {
            long startedAtEpochMs = System.currentTimeMillis();
            long startedNanos = System.nanoTime();
            Map<String, Long> timings = new LinkedHashMap<>();
            List<String> consoleErrors = Collections.synchronizedList(new ArrayList<>());
            Path screenshot = null;
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setIgnoreHTTPSErrors(profile.ignoreHttpsErrors())
                            .setUserAgent(profile.userAgent()));
            Page page = context.newPage();
            Page activePage = page;
            try {
                page.setDefaultTimeout(profile.actionTimeoutMs());
                page.setDefaultNavigationTimeout(profile.navigationTimeoutMs());
                if (profile.captureConsoleErrors()) {
                    page.onConsoleMessage(message -> collectConsoleError(message, consoleErrors));
                }
                for (UiPerformanceStep step : journey.getSteps()) {
                    long stepStarted = System.nanoTime();
                    activePage = executeStep(activePage, step, consoleErrors);
                    timings.put(step.name(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stepStarted));
                }
                long completedAtEpochMs = System.currentTimeMillis();
                writeConsoleErrorsIfNeeded(iteration, consoleErrors);
                return new UiPerformanceIterationResult(
                        stage.name(), user.getId(), iteration, true, startedAtEpochMs, completedAtEpochMs,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos), timings,
                        null, null, null, consoleErrors);
            } catch (Exception exception) {
                if (profile.captureFailureScreenshots()) {
                    screenshot = captureFailureScreenshot(activePage, iteration);
                }
                long completedAtEpochMs = System.currentTimeMillis();
                writeConsoleErrorsIfNeeded(iteration, consoleErrors);
                return failedResult(
                        iteration, startedAtEpochMs, completedAtEpochMs,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos), timings,
                        classify(exception), exception, screenshot, consoleErrors);
            } finally {
                context.close();
            }
        }

        private Page executeStep(Page page, UiPerformanceStep step, List<String> consoleErrors) {
            switch (step.action()) {
                case NAVIGATE -> {
                    Response response = page.navigate(resolveTarget(step.target()),
                            new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    if (response != null && response.status() >= 400) {
                        throw new IllegalStateException(
                                "UI performance navigation returned HTTP " + response.status() + ".");
                    }
                }
                case FILL -> resolveLocator(page, step.target()).first().fill(resolveTemplate(step.valueTemplate()));
                case SELECT_OPTION -> resolveLocator(page, step.target()).first().selectOption(
                        new SelectOption().setLabel(resolveTemplate(step.valueTemplate())));
                case CLICK -> resolveLocator(page, step.target()).first().click();
                case CLICK_AND_SWITCH_TO_POPUP -> {
                    Page popup = page.waitForPopup(
                            new Page.WaitForPopupOptions().setTimeout(profile.navigationTimeoutMs()),
                            () -> resolveLocator(page, step.target()).first().click());
                    popup.waitForLoadState();
                    popup.setDefaultTimeout(profile.actionTimeoutMs());
                    popup.setDefaultNavigationTimeout(profile.navigationTimeoutMs());
                    if (profile.captureConsoleErrors()) {
                        popup.onConsoleMessage(message -> collectConsoleError(message, consoleErrors));
                    }
                    return popup;
                }
                case VERIFY_VISIBLE -> resolveLocator(page, step.target()).first().waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(profile.actionTimeoutMs()));
            }
            return page;
        }

        private Locator resolveLocator(Page page, String definition) {
            if (definition == null || definition.isBlank()) {
                throw new IllegalArgumentException("UI performance locator definition cannot be blank.");
            }
            int separatorIndex = definition.indexOf('_');
            if (separatorIndex <= 0 || separatorIndex == definition.length() - 1) {
                throw new IllegalArgumentException("UI performance locator must use TYPE_value format: " + definition);
            }
            String locatorType = definition.substring(0, separatorIndex).trim();
            String locatorValue = definition.substring(separatorIndex + 1).trim();
            return LOCATOR_HANDLER.getLocatorForType(locatorType, page, locatorValue);
        }

        private Path captureFailureScreenshot(Page page, int iteration) {
            try {
                if (page == null || page.isClosed()) {
                    return null;
                }
                Path path = reportDirectory.resolve("failed-screenshots").resolve(
                        UiPerformanceReportManager.sanitize(stage.name()) + "_vu-" + (userIndex + 1) + "_"
                                + UiPerformanceReportManager.sanitize(user.getId()) + "_iteration-" + iteration + ".png");
                page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(path));
                return path;
            } catch (Exception ignored) {
                return null;
            }
        }

        private void collectConsoleError(ConsoleMessage message, List<String> errors) {
            if ("error".equalsIgnoreCase(message.type())) {
                errors.add(UiPerformanceSensitiveTextSanitizer.sanitize(message.text()));
            }
        }

        private void writeConsoleErrorsIfNeeded(int iteration, List<String> errors) {
            if (errors.isEmpty()) {
                return;
            }
            try {
                Path path = reportDirectory.resolve("browser-console-errors").resolve(
                        UiPerformanceReportManager.sanitize(stage.name()) + "_vu-" + (userIndex + 1) + "_"
                                + UiPerformanceReportManager.sanitize(user.getId()) + "_iteration-" + iteration + ".log");
                Files.write(path, errors, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // Evidence writing must never prevent browser cleanup or alter the iteration result.
            }
        }

        private UiPerformanceIterationResult failedResult(int iteration,
                                                          long startedAtEpochMs,
                                                          long completedAtEpochMs,
                                                          long durationMs,
                                                          Map<String, Long> timings,
                                                          String category,
                                                          Exception exception,
                                                          Path screenshot,
                                                          List<String> consoleErrors) {
            return new UiPerformanceIterationResult(
                    stage.name(), user.getId(), iteration, false, startedAtEpochMs, completedAtEpochMs,
                    durationMs, timings, category,
                    UiPerformanceSensitiveTextSanitizer.sanitize(exception.getMessage()),
                    screenshot == null ? null : screenshot.toString(), consoleErrors);
        }

        private String resolveTarget(String target) {
            if (target.startsWith("http://") || target.startsWith("https://")) {
                return target;
            }
            if (target.startsWith("/")) {
                return journey.getBaseUrl().replaceAll("/+$", "") + target;
            }
            return journey.getBaseUrl().replaceAll("/+$", "") + "/" + target;
        }

        private String resolveTemplate(String template) {
            if (template == null) {
                return null;
            }
            String resolved = template;
            for (Map.Entry<String, String> field : user.getValues().entrySet()) {
                resolved = resolved.replace("${" + field.getKey() + "}", field.getValue());
            }
            if (resolved.matches(".*\\$\\{[^}]+}.*")) {
                throw new IllegalArgumentException(
                        "UI performance data placeholder was not found for virtual user " + user.getId());
            }
            return resolved;
        }

        private String classify(Exception exception) {
            String message = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("timeout")) return "TIMEOUT";
            if (message.contains("net::") || message.contains("connection") || message.contains("dns")) return "NETWORK";
            if (message.contains("selector") || message.contains("locator") || message.contains("visible")
                    || message.contains("locator type")) return "LOCATOR_OR_UI_STATE";
            return "BROWSER_OR_APPLICATION";
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
