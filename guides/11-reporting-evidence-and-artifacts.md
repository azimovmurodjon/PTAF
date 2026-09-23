# FNB-ETAF Reporting, Evidence, and Artifact Guide

## Purpose and scope

This guide explains how the checked-in **FNB-ETAF** framework produces, names, retains, and consumes test evidence. It covers the combined Extent outputs, optional per-feature HTML and PDF outputs, Cucumber/TestNG/Surefire artifacts, desktop screenshots and videos, downloads and PDFs under test, native-mobile evidence, mobile-browser visual artifacts, HTTP performance reports, and the isolated real-browser UI-performance reports.

The guide is deliberately limited to behavior implemented in this repository. It does not authorize a target, change a runner, or guarantee an artifact that the active runner does not register. In particular, report generation is runner- and configuration-dependent. The isolated UI-performance runner intentionally does **not** load ordinary hooks or Extent/PDF listeners. [1] [2]

> **Evidence versus report:** an evidence artifact is a captured file or attachment, such as a PNG, video, download, browser-console log, JTL file, or visual diff. A report is a rendered or structured account of execution that may embed or link that evidence.

## Architecture and source locations

| Concern | Source location, package, or resource | What produces or controls it |
|---|---|---|
| Maven, Java, Surefire, and UI-performance profile | [`pom.xml`](../../pom.xml) | Java 21 compilation; Surefire TestNG provider; default `testng.xml`; Cucumber JSON/JUnit destinations; `ui_performance` profile. |
| Default Maven Cucumber run | [`com.ptaf.runner.TestRunner`](../../src/test/java/com/ptaf/runner/TestRunner.java), [`testng.xml`](../../src/test/resources/testng.xml) | `mvn clean test` executes the TestNG Cucumber runner, currently selecting `@eStore`; the runner registers Cucumber HTML/JSON/rerun, Extent, per-feature, and soft-assertion plugins. |
| Alternate JUnit Cucumber runners | [`com.ptaf.runners.TestRunner`](../../src/test/java/com/ptaf/runners/TestRunner.java), [`PerformanceTestRunner`](../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java), [`MobileTestRunner`](../../src/test/java/com/ptaf/runners/MobileTestRunner.java) | Configure alternate UI, HTTP-performance, and native-mobile report destinations and register the Extent/per-feature listeners. |
| Combined Extent report formats | [`extent.properties`](../../src/test/resources/extent.properties), [`extent-config.xml`](../../src/test/resources/extent-config.xml) | Extent Cucumber adapter creates timestamped Spark HTML, Base64 HTML, PDF, and Excel outputs when its plugin is registered. |
| Per-feature HTML/PDF reports | [`com.ptaf.reporting.PerFeatureReportListener`](../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java), [`com.ptaf.reporting.GlassPdfSubprocessGenerator`](../../src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java), [`config.yml`](../../src/test/resources/config/config.yml) | A `ConcurrentEventListener` splits Cucumber results by declared `Feature:` title, embeds image attachments, and writes per-feature HTML plus enabled PDF variants. |
| Soft assertion report status | [`com.ptaf.reporting.SoftAssertionReportListener`](../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java), [`com.ptaf.softassert.SoftAssertionContext`](../../src/main/java/com/ptaf/softassert/SoftAssertionContext.java) | Registered with the standard runners so soft failures are represented as failures in Extent/PDF reporting. |
| Desktop browser lifecycle, video, and safe renaming | [`com.ptaf.hooks.Hooks`](../../src/main/java/com/ptaf/hooks/Hooks.java), [`com.ptaf.utils.BrowserFactory`](../../src/main/java/com/ptaf/utils/BrowserFactory.java), [`com.ptaf.utils.FeatureArtifactNameResolver`](../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) | Browser context recording is configured at creation; videos are finalized after browser shutdown and moved to a feature-based directory and filename. |
| Functional UI screenshots and downloads | [`com.ptaf.utils.ScreenshotHandler`](../../src/main/java/com/ptaf/utils/ScreenshotHandler.java), [`com.ptaf.ui.pages.PageCommonMethods`](../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java), [`com.ptaf.ui.action_performer.ActionPerformer`](../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) | Failure or explicit screenshots are attached to Cucumber; downloads are saved below the caller-supplied root in a feature-named directory. |
| Downloaded-PDF state and validation | [`com.ptaf.stepdefinitions.PdfSteps`](../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java), [`com.ptaf.pdf.PdfStore`](../../src/main/java/com/ptaf/pdf/PdfStore.java), [`PdfValidation.feature`](../../src/test/resources/features/pdf/PdfValidation.feature) | Stores the current PDF per thread after a download or newest-file lookup, then validates that PDF independently of report-generated PDFs. |
| Native Appium evidence | [`com.ptaf.hooks.MobileHooks`](../../src/main/java/com/ptaf/hooks/MobileHooks.java), [`com.ptaf.mobile.evidence.MobileEvidenceManager`](../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java), [`MobileConfigurationProperties`](../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java) | Mobile hooks invoke configured end-of-scenario screenshot and screen-recording capture for Appium scenarios. |
| Playwright mobile-browser and visual evidence | [`MobileBrowserExecutionConfig`](../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java), [`MobileBrowserEvidenceManager`](../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java), [`MobileBrowserVisualValidator`](../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java) | Provides configuration and implementations for emulation screenshots, videos, profile-specific visual baselines, actual images, and diffs. The visual validator is reached through [`MobileBrowserVisualSteps`](../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java). |
| HTTP/JMeter performance reporting | [`com.ptaf.performance.core.PerformanceEngine`](../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java), [`PerformanceReportManager`](../../src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java), [`PerformanceSummaryWriter`](../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java), [`PerformanceExcelReportWriter`](../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java) | Creates a shared timestamped run directory, ordered scenario folders, JTL/dashboard/summary artifacts, aggregate summaries, and an Excel workbook. |
| Isolated browser-load performance reporting | [`com.ptaf.ui_performance.core.UiPerformanceEngine`](../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java), [`UiPerformanceReportManager`](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java), [`UiPerformanceReportWriter`](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java), [`UiPerformanceExistingReporterAdapter`](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java) | Creates a dedicated report directory, standalone UI-performance formats, optional failure evidence, and an adapter-generated performance workbook and per-stage artifacts. |

## Prerequisites

Use a **JDK 21** toolchain because the Maven compiler source and target are both `21`. Maven is required to resolve dependencies and invoke Surefire. Browser-based UI, mobile-browser emulation, and UI-performance runs also require the Playwright browser binaries; the repository README provides the following project command for their installation. [1] [3]

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Native mobile scenarios are distinct from Playwright emulation. They require a reachable Appium service plus the approved device/emulator and capabilities selected through the mobile configuration files. The framework resolves a native platform in this order: `-Dmobile.platform`, an `@android` or `@ios` tag, then `mobile.default_platform`. Do not place Appium credentials or private device endpoints in feature files or reports. [4]

Before producing or distributing evidence, ensure the executing account has write permission to the chosen output roots. Runtime output roots are ignored by Git in [`.gitignore`](../../.gitignore), including `target/`, `test-output/`, `test-output-performance-reports/`, `test-output-thread/`, `src/test/downloads/`, and UI-performance output.

## Configuration files and key settings

### Cross-cutting reporting configuration

[`src/test/resources/config/config.yml`](../../src/test/resources/config/config.yml) is read through `com.ptaf.utils.ConfigurationProperties`. The `reporting` section controls only the per-feature listener. Per-feature output requires both a runner that registers `com.ptaf.reporting.PerFeatureReportListener` and `reporting.per_feature_reports_enabled: true`. The combined Extent adapter report is independent of this switch. [5] [6]

```yaml
reporting:
  per_feature_reports_enabled: true
  per_feature_reports_output_dir: "test-output/per-feature-reports"
  per_feature_pdf_enabled: false
  per_feature_glass_pdf_enabled: true
  per_feature_glass_pdf_output_dir: "test-output/per-feature-reports-glass"
```

The configured values above are examples of local paths only. A per-feature HTML report is named from the declared `Feature:` title, sanitized to letters, digits, `_`, and `-`, truncated to 80 characters, and suffixed with `yyyy-MM-dd_HH-mm-ss`. The listener uses the same timestamp for every feature it processes in one listener instance. A missing feature title falls back to the feature-file stem. [6]

[`src/test/resources/extent.properties`](../../src/test/resources/extent.properties) controls the Extent Cucumber adapter. Its checked-in `basefolder.name=test-output/` and timestamp pattern produce a fresh `test-output/<dd-MMM-yy_HH-mm-ss>/` folder. Spark, Base64, PDF, and Excel reporters are enabled there, and the screenshot directory is relative to that run root. [7]

```properties
basefolder.name=test-output/
basefolder.datetimepattern=dd-MMM-yy_HH-mm-ss
extent.reporter.spark.start=true
extent.reporter.spark.out=SparkReport/Spark.html
extent.reporter.base64.start=true
extent.reporter.base64.out=Base64Report/Report.html
extent.reporter.pdf.start=true
extent.reporter.pdf.out=PdfReport/FNB-ETAF-Report.pdf
extent.reporter.excel.start=true
extent.reporter.excel.out=ExcelReport/FNB-ETAF-Report.xlsx
screenshot.dir=screenshots/
screenshot.rel.path=../screenshots/
```

`src/test/resources/cucumber.properties` disables Cucumber publishing. Surefire additionally provides `cucumber.plugin=json:target/cucumber-reports/cucumber.json,junit:target/cucumber-reports/cucumber.xml,pretty` for Maven test executions. [1] [8]

### Video, download, ZIP, and functional UI controls

The global configuration also supplies `videoCapture`, `downloadDocument`, and ZIP extraction settings. `BrowserFactory` reads `videoCapture` when it creates a desktop Playwright context. The `downloadDocument` value is a configured path but the active `download` action accepts its destination as the action value; it creates its own feature-based subdirectory below that caller-supplied path. [5] [9]

```yaml
videoCapture: "<true-or-false>"
downloadDocument: "<approved-download-root>/"

zip:
  extraction_dir: "test-output/extracted"
  cleanup_after_scenario: true
  recursive_unzip: true
```

When ZIP extraction is enabled through the ZIP steps, each archive is extracted below `<extraction_dir>/<archive-stem>/`. With `cleanup_after_scenario: true`, `ZipSteps` cleans that scenario’s extraction content; switch it to `false` only for controlled debugging and remove retained data afterward. [5]

### Mobile and mobile-browser evidence controls

Native Appium execution reads the shared settings in [`src/test/resources/mobile/config/mobile-config.yml`](../../src/test/resources/mobile/config/mobile-config.yml). The evidence block controls capture and report attachments. Separate native-app and real-mobile-browser capability files are [`mobile-native-config.yml`](../../src/test/resources/mobile/config/mobile-native-config.yml) and [`mobile-browser-config.yml`](../../src/test/resources/mobile/config/mobile-browser-config.yml). [4] [10]

```yaml
mobile:
  evidence:
    output_directory: "test-output/mobile-evidence"
    screenshot_on_failure: true
    screenshot_on_pass: false
    screenshot_after_each_scenario: false
    attach_screenshots_to_report: true
    video_recording_enabled: false
    video_on_failure_only: true
    attach_video_to_report: false
```

Playwright mobile-browser emulation reads [`src/test/resources/mobile_browser/config/mobile-browser-execution.yml`](../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml). Select a profile through `browser` in the global `config.yml`, with names defined in [`mobile-browser-profiles.yml`](../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml). [11]

```yaml
mobile_browser:
  enabled: true
  orientation: "profile"
  evidence:
    output_directory: "test-output/mobile-browser-evidence"
    screenshot_on_failure: true
    screenshot_on_pass: false
    screenshot_after_each_scenario: false
    attach_screenshots_to_report: true
    video_recording_enabled: false
    video_size_width: <approved-width>
    video_size_height: <approved-height>
  visual:
    enabled: true
    baseline_directory: "src/test/resources/baselines/mobile_browser"
    output_directory: "test-output/mobile-browser-visual"
    mismatch_threshold_percent: <approved-percent>
    create_baseline_if_missing: false
    attach_artifacts_to_report: true
```

Set `create_baseline_if_missing: false` for controlled validation so a missing expected image fails rather than becoming a new baseline. The code supports baseline creation when the switch is true; it writes directly into the baseline resource directory, so that mode should be used only in an approved baseline-authoring workflow. [12]

### HTTP performance and UI-performance controls

HTTP/JMeter performance defaults, optional result/dashboard keys, and thresholds are in [`src/test/resources/performance/config/performance-config.yml`](../../src/test/resources/performance/config/performance-config.yml). The active `PerformanceEngine` has its own hard-coded run-root `test-output-performance-reports`; do not assume the `performance.reporting.resultsFolder`, `dashboardFolder`, or `PerformanceConfigurationProperties.getReportsBaseDirectory()` values redirect that engine’s run folders. [13] [14]

```yaml
performance:
  defaults:
    protocol: "<protocol>"
    host: "<approved-host>"
    port: <approved-port>
    users: <approved-user-count>
    rampUpSeconds: <approved-ramp-seconds>
    holdSeconds: <approved-hold-seconds>
    iterations: <approved-iterations>
  assertions:
    maxErrorPercent: <approved-error-percent>
    maxAvgResponseTimeMs: <approved-average-ms>
    maxP95ResponseTimeMs: <approved-p95-ms>
```

The isolated UI-performance module uses only [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../src/test/resources/ui_performance/config/ui_performance-config.yml) for target composition, load profiles, browser settings, evidence, thresholds, and output formats. Its configuration rejects a `target.host` containing protocol, path, or port, and the default values in code are not a substitute for explicit target approval. [2]

```yaml
ui_performance:
  enabled: <true-or-false>
  active_profile: "<load-or-stress-or-spike-or-soak>"
  target:
    protocol: "<http-or-https>"
    host: "<approved-bare-host>"
    port: <approved-port>
    base_path: "<optional-base-path>"
    routes:
      approved_route: "/<relative-route>"
  evidence:
    capture_failure_screenshots: true
    capture_console_errors: true
  safety:
    max_virtual_users: <approved-maximum>
  reporting:
    output_directory: "test-output/ui_performance"
    existing_performance_reporter_enabled: true
    html_enabled: true
    pdf_enabled: true
    csv_enabled: true
    json_enabled: true
```

## Build and exact run commands

Run commands from the repository root. The first command compiles test code without executing tests. The default command uses the Surefire suite in `src/test/resources/testng.xml`; the current suite points to `com.ptaf.runner.TestRunner`. [1] [15]

```bash
cd /home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23
mvn clean test-compile -DskipTests
mvn clean test
```

Run the isolated UI-performance suite only through its Maven profile. That profile swaps the Surefire suite to `src/test/resources/ui_performance/testng-ui_performance.xml`, disables Surefire method parallelism, and lets `UiPerformanceEngine` own virtual-user concurrency. [1] [2]

```bash
mvn clean test -Pui_performance
```

The dedicated mobile runner documents this direct Maven form. It writes the mobile Cucumber HTML, JSON, and JUnit XML destinations described later in this guide. [16]

```bash
mvn -Dtest=com.ptaf.runners.MobileTestRunner test
```

The following class-targeted commands select the alternate JUnit Cucumber runners as configured in their annotations. Use them only after confirming their tags and feature paths match the intended suite; they do not change those annotation-level selections. [17] [18]

```bash
mvn -Dtest=com.ptaf.runners.PerformanceTestRunner test
mvn -Dtest=com.ptaf.runners.TestRunner test
```

## Create a new feature or test with evidence in mind

### Standard UI, API, database, PDF, or HTTP-performance feature

Create a `.feature` file under `src/test/resources/features/`, using a subdirectory where one already exists for the test type: `features/performance/`, `features/pdf/`, `features/mobile/`, `features/mobile_browser/`, or `features/db/`. The default TestNG runner discovers from the complete `features` tree, while the dedicated runners narrow that scope. Keep Gherkin values non-sensitive: resolve environments in YAML and credentials/tokens through approved external configuration rather than literals. [15] [17] [18]

Place supporting assets in the following resource locations:

| Asset | Repository location | Use |
|---|---|---|
| Standard web locators | [`src/test/resources/elements/`](../../src/test/resources/elements/) | Named element maps used by standard UI page/frame step definitions. |
| Native/mobile locators | [`src/test/resources/mobile/elements/`](../../src/test/resources/mobile/elements/) | Appium native and real-mobile-browser element definitions. |
| Mobile-browser baselines | [`src/test/resources/baselines/mobile_browser/`](../../src/test/resources/baselines/mobile_browser/) | Expected PNGs under a sanitized browser-profile directory. |
| API request definitions | [`src/test/resources/api_requests/api_requests.yml`](../../src/test/resources/api_requests/api_requests.yml) | API service/request metadata. |
| Database queries | [`src/test/resources/queries/db_queries.yml`](../../src/test/resources/queries/db_queries.yml) | Externalized database query definitions. |
| General sample data | [`src/test/resources/data/`](../../src/test/resources/data/) and [`testdata.xlsx`](../../src/test/resources/testdata.xlsx) | XML, CSV, and workbook-backed test data. |
| HTTP-performance payloads | [`src/test/resources/performance/payloads/`](../../src/test/resources/performance/payloads/) | CSV, Excel, and YAML payload source files. |
| UI-performance locators and users | [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`](../../src/test/resources/ui_performance/locators/ui_performance-locators.yml), [`data/users.csv`](../../src/test/resources/ui_performance/data/users.csv) | Dedicated locator repository and virtual-user data; do not put credentials in a committed sample file. |

Use this tested-style **placeholder-only** HTTP-performance pattern when the corresponding tags and runner are selected. The step texts match `com.ptaf.stepdefinitions.PerformanceSteps`; the configured host, thresholds, and load values remain outside this feature. [19]

```gherkin
@performance_testing
Feature: Approved service load evidence

  Scenario: Capture load artifacts for an approved route
    When we run GET performance test for path "/<approved-relative-path>" with name "<safe-test-name>" using <users> users ramp <ramp-seconds> seconds hold <hold-seconds> seconds
    Then performance dashboard path should be generated
    And performance summary file path should be generated
    And performance jtl file path should be generated
```

For a PDF obtained from a UI flow, use a controlled download directory and validate the downloaded document rather than confusing it with an Extent or per-feature report PDF. The `PdfSteps` download step stores the returned path in the current thread’s `PdfStore`. [20]

```gherkin
@pdf
Feature: Downloaded document evidence

  Scenario: Validate an approved PDF download
    When I download PDF from "<locator-group>"."<locator-key>" saving to "<approved-download-root>"
    Then the last PDF should exist
    And the last PDF should be a valid PDF
```

### Isolated UI-performance journey

Create UI-performance features only under [`src/test/resources/ui_performance/features/`](../../src/test/resources/ui_performance/features/). Keep routes in `ui_performance-config.yml`, selector definitions in `ui_performance-locators.yml`, and data fields in the dedicated CSV. `UiPerformanceSteps` intentionally rejects a sensitive literal where the locator key contains `password`, `token`, `secret`, or `credential`; use a data-field step instead. [2] [21]

```gherkin
@ui_performance
Feature: Approved browser journey evidence

  Scenario: Approved virtual users complete a configured journey
    Given UI performance journey "<journey-name>" uses configured target
    When UI performance journey navigates to configured route "<route-name>"
    Then UI performance journey verifies locator "<locator-group>" "<ready-locator>" is visible
    And UI performance journey fills locator "<locator-group>" "<field-locator>" with data field "<approved-data-column>"
    And UI performance journey clicks locator "<locator-group>" "<submit-locator>"
    And the configured UI performance users execute the journey
    And the UI performance run produces a standalone performance report
```

### Mobile-browser visual validation

A mobile-browser feature can call the visual step supplied by `MobileBrowserVisualSteps`. The baseline filename is sanitized and resolved below the configured baseline root and selected browser-profile directory. [12] [22]

```gherkin
@mobile_browser @visual
Feature: Approved responsive visual evidence

  Scenario: Compare an approved screen with its baseline
    Then I compare mobile browser page with visual baseline "<baseline-name>"
```

## Expected reports and evidence artifacts

### Combined Cucumber, Surefire, Extent, and per-feature output

The following paths are relative to the repository working directory. A file appears only when the selected runner/plugin and its configuration enable it.

| Artifact path or pattern | Produced by | Contents and naming behavior |
|---|---|---|
| `target/surefire-reports/` | Maven Surefire | TestNG/Surefire execution reports. The `pom.xml` sets this as `reportsDirectory`. |
| `target/cucumber-reports/cucumber.json` and `target/cucumber-reports/cucumber.xml` | Surefire `cucumber.plugin` system property | Cucumber JSON and JUnit XML destinations supplied during Maven test execution. |
| `target/cucumber-reports/cucumber-pretty` | `com.ptaf.runner.TestRunner` | Configured default-run Cucumber HTML destination. |
| `target/cucumber-reports/CucumberTestReport.json` and `target/cucumber-reports/rerun.txt` | `com.ptaf.runner.TestRunner` | Structured default-run Cucumber report and failed-scenario rerun list. |
| `target/cucumber-reports.html` and `test-output-thread/` | `com.ptaf.runners.TestRunner` | Alternate JUnit UI runner HTML and timeline destinations. |
| `target/performance-cucumber-report.html` | `com.ptaf.runners.PerformanceTestRunner` | Dedicated JUnit HTTP-performance runner HTML destination. |
| `target/cucumber-reports/mobile-report.html`, `mobile-report.json`, and `mobile-report.xml` | `com.ptaf.runners.MobileTestRunner` | Dedicated native-mobile Cucumber HTML, JSON, and JUnit XML. |
| `test-output/<extent-run>/SparkReport/Spark.html` | Extent Cucumber adapter | Combined Spark HTML; `<extent-run>` uses `dd-MMM-yy_HH-mm-ss`. |
| `test-output/<extent-run>/Base64Report/Report.html` | Extent Cucumber adapter | Combined Base64 HTML report. |
| `test-output/<extent-run>/PdfReport/FNB-PTAF-Report.pdf` | Extent Cucumber adapter | Combined Extent PDF report. |
| `test-output/<extent-run>/ExcelReport/FNB-PTAF-Report.xlsx` | Extent Cucumber adapter | Combined Extent Excel report. |
| `test-output/<extent-run>/screenshots/` | Extent adapter configuration | Screenshot location configured as the Extent run-relative screenshot directory. The framework’s direct `ScreenshotHandler` attaches PNG bytes to Cucumber; it does not itself write a file to this directory. |
| `test-output/per-feature-reports/<safe-feature>_<yyyy-MM-dd_HH-mm-ss>.html` | `PerFeatureReportListener` | One dark-theme HTML report per feature when `per_feature_reports_enabled` is true. Image Cucumber attachments are embedded as Base64. |
| `test-output/per-feature-reports/<safe-feature>_<yyyy-MM-dd_HH-mm-ss>.pdf` | `PerFeatureReportListener` | Direct PDFBox per-feature PDF only when `per_feature_pdf_enabled` is true. |
| `test-output/per-feature-reports-glass/<safe-feature>_<yyyy-MM-dd_HH-mm-ss>.pdf` | `PerFeatureReportListener` → `GlassPdfSubprocessGenerator` | Glass-style per-feature PDF only when both per-feature reports and Glass PDF are enabled. |

The per-feature listener creates its output directories. It attaches image `EmbedEvent` payloads to HTML report nodes and uses captured step/status data for the optional PDFs. Glass PDF generation writes temporary JSON and screenshot PNG files, starts a separate JVM, waits up to 120 seconds in the current implementation, verifies that the output PDF is non-empty, and deletes the temporary files in `finally`. Failures are logged while the per-feature HTML remains independent. [6] [23]

### Screenshots, video, downloads, PDFs, and mobile evidence

| Artifact path or pattern | Produced by | Important behavior |
|---|---|---|
| Cucumber/Extent image attachment, no direct disk path from `ScreenshotHandler` | `ScreenshotHandler.handleScenarioTeardown` called by `PageCommonMethods` on an action failure or soft failure | Captures a full-page PNG and calls `Scenario.attach`. Per-feature reports can consume that Cucumber image attachment. |
| `<path-supplied-by-feature>/` | Explicit `screenshot` or `fullscreenshot` action in `ActionPerformer` | Uses `Locator.screenshot(...setPath(...))` or `Page.screenshot(...setPath(...))`; the feature/action supplies the exact output path. Create an approved, non-sensitive output root rather than writing into resources. |
| `test-output/captured-videos/<yyyyMMdd_HHmmss>/<safe-feature>/<safe-feature>_<yyyy-MM-dd_HH-mm-ss-SSSSSS>.webm` | Desktop `BrowserFactory` recording plus `Hooks.closeBrowserResources` | Created only with `videoCapture: "true"`. Playwright first records in the timestamped directory; after browser closure the hook moves each finalized video into a feature subdirectory and assigns a unique feature-based filename. |
| `<approved-download-root>/<safe-feature>/<safe-feature>_<yyyy-MM-dd_HH-mm-ss-SSSSSS><original-extension>` | `ActionPerformer` `download` or `download_optional` | Waits for the download event, preserves the suggested-file extension, creates a safe feature directory, and saves without using the server filename as the stem. `download` throws if no event occurs; `download_optional` returns `null` on no event or an exception. |
| Thread-local PDF-under-test path | `PdfSteps` and `PdfStore` | A PDF download stores its returned path. `setLastFromDirectory` chooses the newest regular file matching `.pdf`, stores its absolute path for that thread, and validation steps call `ensureExists`. It is not a generated report-PDF registry. |
| `test-output/mobile-evidence/<yyyyMMdd_HHmmss>/target-output/screenshots/<safe-scenario>_<type>.png` | Native `MobileEvidenceManager.captureScenarioScreenshotIfConfigured` and named capture | `MobileHooks` calls final screenshot capture before closing an Appium driver. Failure capture can force attachment; other attachments use `attach_screenshots_to_report`. Assertion-failure capture uses a separate `screenshots/` child under the same run root. |
| `test-output/mobile-evidence/<yyyyMMdd_HHmmss>/videos/<safe-feature>/<safe-feature>_<yyyy-MM-dd_HH-mm-ss-SSSSSS>.mp4` | Native `MobileEvidenceManager.stopVideoIfEnabled` | Decodes Appium’s Base64 screen recording when enabled. A passing recording is discarded when `video_on_failure_only` is true. Attachment is controlled by `attach_video_to_report`. |
| `test-output/mobile-browser-evidence/<yyyyMMdd_HHmmss>/videos/<safe-feature>/<safe-feature>_<yyyy-MM-dd_HH-mm-ss-SSSSSS>.webm` | Mobile browser video setting in `BrowserFactory` plus standard `Hooks` close/rename | Applies when a selected Playwright mobile-browser profile has `video_recording_enabled: true`. |
| `test-output/mobile-browser-evidence/<yyyyMMdd_HHmmss>/screenshots/<safe-scenario>.png` | `MobileBrowserEvidenceManager` implementation | This is the path the class writes when its capture method is called. In the inspected source tree, no hook or step calls `MobileBrowserEvidenceManager.captureScenarioScreenshotIfConfigured`; therefore configuration alone does not establish automatic mobile-browser screenshot output. Video is wired separately through `BrowserFactory`. |
| `src/test/resources/baselines/mobile_browser/<safe-profile>/<safe-baseline>.png` | `MobileBrowserVisualValidator` only if baseline is absent and `create_baseline_if_missing` is true | A source-controlled expected image. Do not let CI create or commit baselines implicitly. |
| `test-output/mobile-browser-visual/<yyyyMMdd_HHmmss>/<safe-profile>/<safe-baseline>-actual.png` and `-diff.png` | `MobileBrowserVisualValidator` | Visual step always writes the actual image; when a baseline exists it also writes the pixel diff. Baseline, actual, and diff are attached only when `attach_artifacts_to_report` is true. |

`FeatureArtifactNameResolver` replaces unsafe feature-title characters, collapses duplicate underscores, trims surrounding underscores, caps the title portion at 80 characters, and adds microseconds to artifact filenames. This minimizes collisions across downloads, videos, and popup recordings. It retains the original source file’s extension but never uses the source stem for the destination name. [9]

### HTTP/JMeter performance artifacts

`PerformanceEngine` initializes one shared run directory under `test-output-performance-reports/<dd-MMM-yy_HH-mm-ss>/`. It assigns each performance scenario an ordered folder such as `01_<safe-request-name>/` and produces the following structure. [14]

```text
test-output-performance-reports/<run-timestamp>/
├── 01_<safe-request-name>/
│   ├── results.jtl
│   ├── dashboard/
│   ├── summary.txt
│   └── readable-summary.txt
├── run-summary.txt
├── run-readable-summary.txt
├── run-index.txt
└── performance-run-report.xlsx
```

The main performance engine writes scenario summaries, aggregates them, and rewrites `performance-run-report.xlsx` as results are added. The workbook is generated by `PerformanceExcelReportWriter`; its path is the current run root plus `performance-run-report.xlsx`. [14] [24]

### Isolated UI-performance artifacts

The UI-performance engine creates a directory at `test-output/ui_performance/<safe-journey>_<active-profile>_<yyyyMMdd_HHmmss>/`, or beneath the configured `ui_performance.reporting.output_directory`. The report manager immediately creates `failed-screenshots/` and `browser-console-errors/`. [25]

```text
test-output/ui_performance/<safe-journey>_<profile>_<timestamp>/
├── cucumber/
│   ├── cucumber.html
│   ├── cucumber.json
│   └── cucumber.xml
├── failed-screenshots/
│   └── <safe-stage>_vu-<number>_<safe-user-id>_iteration-<number>.png
├── browser-console-errors/
│   └── <safe-stage>_vu-<number>_<safe-user-id>_iteration-<number>.log
├── ui_performance-summary.html
├── ui_performance-summary.pdf
├── ui_performance-summary.json
├── ui_performance-stages.csv
├── ui_performance-iterations.csv
├── ui_performance-step-timings.csv
├── ui_performance-performance-summary.txt
├── performance-run-report.xlsx
├── performance-reporter-index.txt
└── performance-reporter/
    └── <safe-stage>/
        ├── summary.txt
        ├── readable-summary.txt
        └── ui-browser-results.jtl
```

The Cucumber `cucumber/` outputs are declared in `UiPerformanceRunner`. The standalone HTML/PDF/CSV/JSON files are each conditional on their respective `ui_performance.reporting.*_enabled` setting; the technical text summary is always written by `UiPerformanceReportWriter`. The Excel workbook, stage summaries, and browser-sample JTL are written only when `existing_performance_reporter_enabled` is true. Failure PNGs are written only when `capture_failure_screenshots` is true. Console logs are emitted only if error messages were collected while `capture_console_errors` is true. [2] [25] [26]

The UI-performance writer sends browser-console text and failure diagnostics through `UiPerformanceSensitiveTextSanitizer` before report serialization, and its standalone reports intentionally state that credentials, tokens, input values, cookies, session data, and full target URLs are excluded. The HTTP/JMeter `PerformanceSummaryWriter`, by contrast, includes a `Full Target URL` field in its summaries. Treat HTTP-performance output as potentially sensitive and restrict/redact it before distribution. [14] [27]

## Module boundaries

| Module | Boundary and reporting implication |
|---|---|
| Standard UI and common Cucumber | Uses `com.ptaf.hooks.Hooks`, `com.ptaf.stepdefinitions`, `BrowserFactory`, standard locator YAML, and the default/alternate Cucumber reporting plugins. Its videos, downloads, and attached screenshots participate in Cucumber/Extent behavior when the corresponding runner is active. |
| Native mobile/Appium | Uses `com.ptaf.hooks.MobileHooks`, `com.ptaf.mobile.*`, and `src/test/resources/mobile/*`. It is selected by mobile tags and avoids the Playwright browser stack in `Hooks`; native evidence is produced by `MobileEvidenceManager`. |
| Playwright mobile-browser emulation | Reuses the ordinary Playwright browser stack with profile-driven settings under `mobile_browser/`. It is not Appium native automation. Its visual comparison step is wired; its standalone screenshot manager exists but has no call site in the inspected source. |
| PDF validation | Uses `PdfSteps` and `PdfStore` for a downloaded document under test. It is separate from Extent’s combined PDF and per-feature PDF reports. |
| HTTP/JMeter performance | Uses `com.ptaf.performance.*` and `features/performance/`. `Hooks` recognizes performance scenarios as browserless, so its evidence is JTL/dashboard/summary/workbook output rather than UI browser capture. |
| UI performance | Uses only `com.ptaf.ui_performance.*` and `src/test/resources/ui_performance/*`. `UiPerformanceRunner` excludes normal hooks, ordinary UI glue, mobile hooks, and Extent/PDF listeners, so it writes the isolated Cucumber and performance artifacts documented above. |

## Safe artifact handling

Use only approved non-production test data. Do not encode a token, password, account identifier, client document, or private endpoint in a feature title, scenario name, baseline name, request name, filename, or committed sample. Feature titles become names for videos, downloads, and per-feature reports; scenario names become names for certain mobile screenshots. [6] [9]

Keep raw downloads, videos, screenshots, JTL files, console logs, and report bundles inside ignored output roots with access controlled by the execution environment. Review artifacts before attaching them to tickets or exporting them. A video or screenshot can contain application data even when its filename is sanitized. A downloaded PDF is application evidence and must follow the same retention and sharing controls as the source document.

Use the framework’s safe mechanisms rather than ad hoc string concatenation. `FeatureArtifactNameResolver` creates feature directories and unique feature-based filenames. `UiPerformanceReportManager.sanitize` removes unsafe characters from journey and stage directory components. `PdfStore` is thread-local, so PDF-validation steps should run in the same scenario/thread context that downloaded or selected the document. [9] [20] [25]

For any existing HTTP performance report, treat `summary.txt`, `readable-summary.txt`, the Excel workbook, JTL, and dashboard as restricted until reviewed because the current summary writer renders the full target URL. Do not use the current UI-performance redaction behavior as a guarantee for other report modules. [14] [27]

## Troubleshooting

| Symptom | Likely code-backed cause | Check or corrective action |
|---|---|---|
| No per-feature reports | The runner did not register `PerFeatureReportListener`, or `reporting.per_feature_reports_enabled` is false. | Use a registered runner, enable the switch, then inspect `test-output/per-feature-reports/`. |
| HTML exists but an optional per-feature PDF is absent | `per_feature_pdf_enabled` or `per_feature_glass_pdf_enabled` is false; Glass output also needs per-feature reports enabled. | Check `config.yml` switches and the separate Glass output directory. Review logs for subprocess failure or timeout. |
| Glass PDF fails but HTML exists | The listener isolates Glass generation in a subprocess; failures are logged and do not invalidate the independent HTML output. | Review runner logs and confirm the target output directory is writable. Do not inspect temporary JSON/PNG files as durable evidence because the listener deletes them. |
| Expected screenshot is not in an HTML report | The screenshot may not have been attached to the Cucumber `Scenario`, or the active runner may not use Extent/per-feature reporting. | For functional UI, confirm the action-failure/explicit capture path. For native mobile, check attachment switches. For visual tests, check `attach_artifacts_to_report`. |
| Mobile-browser screenshots are not created | The mobile-browser evidence manager has no caller in the inspected source, even though configuration keys and a write path exist. | Use the visual-validation step for wired image evidence, add a framework integration through approved development work, or do not promise automatic emulation screenshots. |
| Video is missing or remains anonymously named | Recording must be enabled before browser-context creation. The standard hooks rename only after the browser closes and the video is finalized. | Set the appropriate desktop/mobile-browser video switch, allow normal teardown/close, and inspect the timestamped recording directory plus feature subdirectory. |
| A download is missing | Strict `download` requires a Playwright download event. Optional download deliberately returns `null` if none occurs. | Confirm the locator triggers a download, use an approved writable root, and inspect `<download-root>/<safe-feature>/`. |
| PDF validation says no current PDF or file missing | The current thread has no `PdfStore` path, the configured directory is wrong, or the newest-file lookup found nothing. | Perform the download/store step first, use the same scenario thread, then check `PdfStore.ensureExists` prerequisites. |
| UI-performance run stops before browser execution | `ui_performance.enabled` is false, target settings are invalid, a CSV field is unresolved, or there are too few distinct users with reuse disabled. | Review the dedicated YAML, CSV headers/rows, route names, and configured `max_virtual_users`. |
| UI-performance report directory exists but a selected format is missing | HTML, PDF, CSV, and JSON are independently controlled. The existing performance adapter is also optional. | Check the four format flags and `existing_performance_reporter_enabled`; inspect the technical text summary, which is always written by the writer. |
| UI-performance run fails on thresholds but reports are available | `UiPerformanceEngine` writes reports before calling `enforceThresholds`. | Open the run directory named in the assertion message; review stages, iterations, failed screenshots, and console-error logs. |
| HTTP-performance reports appear in an unexpected root | The active `PerformanceEngine` uses `test-output-performance-reports`, not the similarly named configuration accessor’s `test-output/performance-reports`. | Collect artifacts from the engine’s hard-coded root and do not rely on unused-looking reporting YAML keys to relocate it. |

## References

[1]: ../../pom.xml "Maven build, Surefire configuration, and UI-performance profile"
[2]: ../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java "Isolated UI-performance Cucumber runner"
[3]: ../../ReadMe.md "PTAF repository README and Playwright prerequisite command"
[4]: ../../src/main/java/com/ptaf/hooks/MobileHooks.java "Appium mobile lifecycle hooks"
[5]: ../../src/test/resources/config/config.yml "Global framework configuration including reporting, video, download, and ZIP settings"
[6]: ../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature Extent HTML and PDF listener"
[7]: ../../src/test/resources/extent.properties "Extent adapter reporter formats and timestamped output layout"
[8]: ../../src/test/resources/cucumber.properties "Cucumber publishing configuration"
[9]: ../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Safe feature-based artifact directory and filename resolver"
[10]: ../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java "Native mobile screenshot and video evidence manager"
[11]: ../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml "Playwright mobile-browser execution and evidence configuration"
[12]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java "Mobile-browser visual baseline and diff artifact implementation"
[13]: ../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java "HTTP performance configuration accessor"
[14]: ../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java "HTTP/JMeter performance execution and report lifecycle"
[15]: ../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner and plugins"
[16]: ../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Dedicated mobile Cucumber runner and report outputs"
[17]: ../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java "Dedicated HTTP-performance Cucumber runner"
[18]: ../../src/test/java/com/ptaf/runners/TestRunner.java "Alternate JUnit Cucumber UI runner"
[19]: ../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java "HTTP performance Gherkin step definitions"
[20]: ../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java "Downloaded PDF step definitions and PdfStore integration"
[21]: ../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "Configuration-first UI-performance journey DSL"
[22]: ../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java "Mobile-browser visual comparison step definition"
[23]: ../../src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java "Glass-style per-feature PDF subprocess generator"
[24]: ../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java "Performance Excel workbook writer"
[25]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java "UI-performance run directory manager"
[26]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java "UI-performance adapter to existing performance reports"
[27]: ../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java "HTTP performance text and readable summary writer"
