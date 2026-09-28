# Reporting, Evidence, Artifacts, and Failure Diagnosis Reference

## Purpose and boundary

This chapter is the operational reference for **how FNB-ETAF turns an execution into reviewable evidence**: Cucumber result files, combined Extent outputs, per-feature HTML/PDF reports, screenshots, videos, downloads, mobile media, and API/UI-performance reports. It also maps common report symptoms to the class or configuration that owns them.

> **Evidence** is the original support for an outcome—such as a PNG, recording, downloaded file, console-error log, JTL file, visual diff, or Cucumber attachment. A **report** renders or indexes execution information and may embed that evidence.

This is deliberately a cross-cutting reference. It does not define UI locators, API request construction, mobile capabilities, load profiles, or visual-baseline approval rules. It records only behavior visible in the current implementation and resources. Report availability is **runner- and configuration-dependent**, not a universal guarantee.[1][2]

## Report-producing execution paths

### 1. Standard Cucumber/TestNG execution

The Maven default uses [`com.ptaf.runner.TestRunner`](../../../src/test/java/com/ptaf/runner/TestRunner.java), selected by [`testng.xml`](../../../src/test/resources/testng.xml), and Surefire adds a Cucumber JSON/JUnit plugin through Maven system properties.[1][2] The runner itself registers:

- console `pretty` output;
- built-in Cucumber HTML at `target/cucumber-reports/cucumber-pretty`;
- Cucumber JSON at `target/cucumber-reports/CucumberTestReport.json`;
- rerun entries at `target/cucumber-reports/rerun.txt`;
- the Extent Cucumber adapter;
- `PerFeatureReportListener`; and
- `SoftAssertionReportListener`.[2]

Surefire additionally supplies `json:target/cucumber-reports/cucumber.json` and `junit:target/cucumber-reports/cucumber.xml`, and places its own provider reports in `target/surefire-reports`.[1] Therefore, do not assume that one JSON filename is the sole CI input: the runner and Maven configuration name different Cucumber JSON outputs.

Use the normal suite only when its configured tags and target are approved:

```bash
mvn clean test
```

The standard lifecycle registers a scenario in `Hooks`, creates a Playwright browser/context/page for UI scenarios, executes steps, attaches eligible Cucumber media, closes browser resources, and lets registered Cucumber plugins flush their outputs.[2][9]

### 2. JUnit alternate runners

The API, HTTP-performance, and native-mobile JUnit runners each select their own feature/tags and explicit Cucumber plugin destinations. For example, the API runner emits `target/api-cucumber-reports.html` and a timeline directory, while the mobile runner emits `target/cucumber-reports/mobile-report.html`, `.json`, and `.xml`; both register Extent and the per-feature/soft-assertion listeners.[3][4] Run a selected JUnit runner only when that is the intended suite, for example:

```bash
mvn -Dtest=com.ptaf.runners.MobileTestRunner test
```

### 3. Isolated real-browser UI-performance execution

`mvn clean test -Pui_performance` changes Surefire to the dedicated UI-performance TestNG suite, disables scenario-level parallelism, and fails the build on threshold breach. The UI-performance engine—not TestNG—is responsible for concurrent browser users.[1]

```bash
mvn clean test -Pui_performance
```

Its runner registers only Cucumber `pretty`, HTML, JSON, and JUnit plugins under `test-output/ui_performance/cucumber/`; it intentionally does **not** load normal `com.ptaf.hooks`, normal step definitions, Extent, the per-feature listener, or the PDF listener.[5] Consequently, a UI-performance run has its own report family and cannot create standard Extent/per-feature reports merely because those switches are enabled elsewhere.

## Output inventory and ownership

| Output family | Canonical location or naming | Producing path | Important conditions and contents |
|---|---|---|---|
| Standard Cucumber HTML/JSON/rerun | `target/cucumber-reports/…` | Standard TestNG runner and Surefire | Runner plus Surefire contribute separate Cucumber plugin declarations; rerun file comes from the runner.[1][2] |
| Extent combined reports | `test-output/<dd-MMM-yy_HH-mm-ss>/SparkReport/Spark.html`, `Base64Report/Report.html`, `PdfReport/FNB-PTAF-Report.pdf`, `ExcelReport/FNB-PTAF-Report.xlsx` | Extent Cucumber adapter using `extent.properties` | Each adapter run receives a timestamped base folder. Spark, Base64, PDF, and Excel are enabled in the checked-in properties.[6] |
| Per-feature HTML | `test-output/per-feature-reports/<Feature_Title>_<yyyy-MM-dd_HH-mm-ss>.html` by current YAML | `PerFeatureReportListener` | Requires the listener plugin and `reporting.per_feature_reports_enabled: true`.[7][8] |
| Per-feature direct PDF | Same per-feature output directory, `.pdf` | `PerFeatureReportListener`/PDFBox | Requires `reporting.per_feature_pdf_enabled: true`; renders summary/scenario/step information from listener data.[7][8] |
| Per-feature Glass PDF | `test-output/per-feature-reports-glass/<Feature_Title>_<timestamp>.pdf` by current YAML | Listener subprocess and `GlassPdfSubprocessGenerator` | Requires `reporting.per_feature_glass_pdf_enabled: true`.[7][8] |
| Desktop web videos | Initially `test-output/captured-videos/<yyyyMMdd_HHmmss>/`; moved below a feature folder | `BrowserFactory` plus `Hooks` | `videoCapture: "true"` enables context recording. Finalization/renaming occurs after browser close.[9][10] |
| Explicit functional screenshots | Caller path; standard binding builds `test-output/screenshots/<name>.png` | `PageCommonSteps` → `PageCommonMethods` → `ActionPerformer` | The element PNG is persisted and the page method also attaches an element image for a passed screenshot action.[11][12] |
| Downloads | `<caller-supplied-root>/<Feature_Title>/<Feature_Title>_<microsecond-timestamp>.<original-extension>` | `ActionPerformer` and `FeatureArtifactNameResolver` | `download` is strict; `download_optional` returns `null` for no event or other download errors.[10][12] |
| Native Appium screenshots/video | `<mobile.evidence.output_directory>/<run-id>/…` | `MobileHooks` and `MobileEvidenceManager` | See the mobile layout and attachment rules below.[13][14] |
| Emulated mobile-browser screenshot helper | `<mobile_browser.evidence.output_directory>/<run-id>/screenshots/<scenario>.png` | `MobileBrowserEvidenceManager` | The class implements it, but the current source tree has no invocation of this helper; see the mobile-browser section below.[15] |
| Mobile-browser visual actual/diff | `test-output/mobile-browser-visual/<run-id>/<profile>/…` by current YAML | `MobileBrowserVisualValidator` | The baseline is under `src/test/resources/baselines/mobile_browser/<profile>/` by current YAML.[15][16] |
| API-performance run | `test-output-performance-reports/<dd-MMM-yy_HH-mm-ss>/` | `PerformanceEngine` | Scenario folders, JTL/dashboard/summaries, aggregate text, and a run Excel workbook are produced.[17][18][19] |
| UI-performance standalone report | `test-output/ui_performance/<journey>_<profile>_<yyyyMMdd_HHmmss>/` by current YAML | `UiPerformanceEngine`, manager, writer | Dedicated HTML/PDF/CSV/JSON/TXT plus optional adapter-generated workbook and stage artifacts.[20][21][22] |

## Combined Extent and Cucumber outputs

The Extent adapter reads [`extent.properties`](../../../src/test/resources/extent.properties). Its enabled reporters and configured paths are:

```properties
basefolder.name=test-output/
basefolder.datetimepattern=dd-MMM-yy_HH-mm-ss
extent.reporter.spark.start=true
extent.reporter.spark.out=SparkReport/Spark.html
extent.reporter.base64.start=true
extent.reporter.base64.out=Base64Report/Report.html
extent.reporter.pdf.start=true
extent.reporter.pdf.out=PdfReport/FNB-PTAF-Report.pdf
extent.reporter.excel.start=true
extent.reporter.excel.out=ExcelReport/FNB-PTAF-Report.xlsx
screenshot.dir=screenshots/
screenshot.rel.path=../screenshots/
```

The separate [`extent-config.xml`](../../../src/test/resources/extent-config.xml) supplies Extent display settings, including the dark theme, UTF-8, HTTPS asset protocol, timeline enabled, and Base64 thumbnail behavior.[6][23] This configuration does not itself register the adapter; registration is the responsibility of a runner's `plugin` list.

`src/test/resources/cucumber.properties` sets `cucumber.publish.enabled = false`, so this repository configuration does not publish Cucumber results through Cucumber publishing.[24]

### Attachments and soft assertions

Cucumber `Scenario.attach` calls are the bridge between an image/media byte array and plugin output. `PerFeatureReportListener` listens for `EmbedEvent`; for image media it adds a Base64 capture to the feature Extent node and buffers it against the active step for later report generation.[7] That is why an image must be attached while the scenario is active to appear inline in reports; a PNG merely written to disk is not automatically embedded.

With `soft_assertions.enabled: true`, a normal Cucumber step can appear passed because the implementation catches the failure. `SoftAssertionReportListener` examines unreported thread-local soft failures after each Gherkin step and marks the current Extent step failed. It does not stop execution and does not process hook steps.[25] The per-feature listener independently applies the same soft-failure status correction while building its own step results.[7]

## Per-feature reports: configuration, flow, and artifacts

### Configuration that exists

The active configuration values are in [`config.yml`](../../../src/test/resources/config/config.yml):

```yaml
reporting:
  per_feature_reports_enabled: true
  per_feature_reports_output_dir: "test-output/per-feature-reports"
  per_feature_pdf_enabled: false
  per_feature_glass_pdf_enabled: true
  per_feature_glass_pdf_output_dir: "test-output/per-feature-reports-glass"
```

`ConfigurationProperties` exposes precisely these keys and defaults: the enabled switches default to `false`, standard per-feature output defaults to `test-output/per-feature-reports`, and Glass output defaults to `test-output/per-feature-reports-glass`.[8] These values affect only the custom listener; they do not enable or disable the combined Extent adapter report.

### Event flow

When registered, `PerFeatureReportListener` is a thread-safe `ConcurrentEventListener` that performs the following sequence.[7]

1. On `TestSourceRead`, it extracts the first declared `Feature:` title and keys it to the feature URI. If none is found, it uses the feature file stem.
2. On scenario start, provided per-feature reporting is enabled, it lazily creates an `ExtentReports`/Spark reporter per feature, a feature parent node, and scenario children; scenario tags become Extent categories.
3. On each completed Gherkin step, it records status, duration, failure message, and soft-assertion corrections. Failed hooks are represented; passing hooks are deliberately omitted as noise.
4. On an image `EmbedEvent`, it places a Base64 image on the active Extent scenario and buffers it for the corresponding step. The listener relies on the observed Cucumber ordering that the embed event arrives before `TestStepFinished`.
5. At run finish, it first attempts to split the Extent adapter's feature tree through reflective access to `ExtentService`; if that cannot be accessed, it flushes its own per-feature Extent instances. It then produces each enabled PDF variant separately.

A filename derives from the **declared feature title**, not the `.feature` filename. The resolver replaces unsupported characters with `_`, collapses underscores, trims leading/trailing underscores, limits the name to 80 characters, and appends the listener's run timestamp. For example, a title such as `Account Opening / Approved Flow` becomes a safe `Account_Opening_Approved_Flow_<timestamp>` stem.[7]

### PDF variants and limits

The direct per-feature PDF is created with PDFBox. It has a summary page and scenario/step rows, status colors, timings, and truncated failed-step messages. The listener's direct PDF code does not render the buffered screenshot images into that PDF; image attachments are most directly available in Extent HTML and are included in the temporary JSON prepared for the Glass subprocess path.[7]

The optional Glass variant serializes scenario/step information and eligible Base64 screenshots to a temporary JSON file, writes temporary PNG files for step media, starts a new JVM for the generator, then removes those temporary files in `finally`.[7] It is intentionally isolated because the source identifies a static-font lifecycle issue in the report library.

> **Discrepancy to retain when troubleshooting:** the `generateGlassPdfSubprocess` Javadoc says the subprocess has a 60-second timeout, but the implementation actually waits for **120 seconds** before force-destroying it. The code, not the comment, is the operative behavior.[7]

A Glass PDF failure logs a warning and does not invalidate the feature HTML; therefore, a healthy HTML file with a missing Glass PDF is possible.[7]

## Screenshots, downloads, and video evidence

### Functional web screenshots

There are three distinct behaviors to keep separate:

1. **Explicit element screenshot:** the standard step binding below builds `test-output/screenshots/<name>.png`, invokes the `screenshot` action, then `PageCommonMethods` captures/attaches a passed-step locator screenshot if its local failure flag remains clear.[11][12]
2. **Explicit full-page screenshot:** `fullscreenshot`/`fullpagescreenshot` writes the requested file path using Playwright `Page.screenshot(fullPage=true)`. It is a filesystem capture; this action has no corresponding attachment call in `ActionPerformer`.[12]
3. **Failure attachment:** `PageCommonMethods.handleFailure` uses `ScreenshotHandler.handleScenarioTeardown` to attach a full-page PNG to Cucumber before attempting to close browser resources. `UIAssert.failWithScreenshot` instead attempts an element/iframe-aware attachment and then throws the original logical failure.[11][26]

A safe Gherkin example for an explicit, reviewable checkpoint is:

```gherkin
And we capture screenshot on page <element-group> locator <element-key> name "review-checkpoint"
```

Do not put customer values, account identifiers, tokens, or personal data in `<element-group>`, `<element-key>`, or screenshot names.

`ScreenshotHandler` attaches image bytes with MIME type `image/png` and catches/logs capture errors instead of throwing them. It has no disk-output policy of its own.[11] Thus, a screenshot may be absent even though the scenario correctly fails if the page/locator/frame is unavailable during capture.

> **Source-visible inconsistency:** `Hooks.tearDown` calls `PageCommonMethods.finalizeScenario()` only for a passed browser scenario, but `finalizeScenario()` currently captures a `"Passed Step"` image only when its `isFailed` flag is `true`. This does **not** establish reliable automatic pass-screenshot collection. Use the explicit screenshot step where a passed-state image is required.[9][26]

### Downloads and feature naming

`ActionPerformer` waits for a Playwright download event, creates a feature-title directory under the supplied root, and saves the file under a timestamped feature-title filename. `FeatureArtifactNameResolver` preserves only the source extension; it does not preserve the original filename stem. A timestamp with microseconds reduces collisions when one feature creates multiple artifacts.[10][12]

```text
<download-root>/
  <Sanitized_Feature_Title>/
    <Sanitized_Feature_Title>_yyyy-MM-dd_HH-mm-ss-SSSSSS.<extension>
```

The strict `download` action throws if the expected event does not occur. `download_optional` waits only through the action timeout and returns `null` after either a Playwright no-download failure or another exception.[12]

The standard binding reads `downloadDocument` from `config.yml`, but passes `filePath + ".jpeg"` as the download **root**. The action then treats that string as a directory before it makes the feature subdirectory. The binding's own comment describes a configured directory, so this `.jpeg` suffix is a source-visible mismatch to check before relying on the standard step.[11][12][27]

```gherkin
And we click download on page <element-group> locator <download-link-key>
```

Use an approved relative output root and treat downloaded material as potentially sensitive evidence.

### Desktop and emulated-mobile-browser videos

For ordinary desktop web contexts, `BrowserFactory` enables recording only when global `videoCapture` parses as `true`. It records initially below `test-output/captured-videos/<run-timestamp>/`; for an active mobile-browser profile, its video setting comes from `mobile_browser.evidence.video_recording_enabled` and initially uses `test-output/mobile-browser-evidence/<run-timestamp>/videos`.[9][10][16][27]

`Hooks` retains video handles for the initial page and popups. During `closeBrowserResources`, it closes the browser first, waits briefly for each recording to become available, creates a feature-title directory below the recording's parent, and moves each file to a feature-title/timestamp filename. Artifact-renaming problems are logged and do not change scenario status.[9][10]

> **Boundary:** `PageCommonMethods.closeBrowserOnFailure()` closes the browser directly, whereas feature-title video finalization is implemented in `Hooks.closeBrowserResources()`. The source therefore does not guarantee that every browser closed through the direct failure path will be renamed by the `Hooks` video-renaming routine.[9][26]

## Native mobile and mobile-browser evidence

### Native Appium evidence

`MobileHooks` is opt-in for scenarios tagged `@mobile`, `@android`, `@ios`, `@cross_platform`, `@appium_browser`, or `@mobile_browser_real`. It records the current scenario, starts the appropriate Appium driver, starts video if enabled, then on teardown captures configured screenshots, stops/saves video, closes the driver, and clears the scenario reference.[13]

The existing mobile evidence configuration is:

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

These are real keys exposed by `MobileConfigurationProperties`.[14][28]

| Native mobile artifact | Runtime layout and behavior |
|---|---|
| End-of-scenario screenshot | `{output_directory}/{RUN_ID}/target-output/screenshots/<safe-scenario>_<failure-or-scenario>.png`. A failed scenario attaches if `screenshot_on_failure` is true, even if `attach_screenshots_to_report` is false; pass/every-scenario capture follows its own switches.[13][14] |
| Named screenshot | `{output_directory}/{RUN_ID}/target-output/screenshots/<safe-name>.png`; attachment is conditional on `attach_screenshots_to_report` and an active scenario.[13] |
| Immediate assertion screenshot | `{output_directory}/{RUN_ID}/screenshots/<safe-scenario>_<failure-name>.png`; it logs the absolute path and attaches only when configured.[13] |
| Appium recording | `{output_directory}/{RUN_ID}/videos/<Sanitized_Feature_Title>/<Feature_Title>_<timestamp>.mp4`; only a `CanRecordScreen` driver can record. A passing video is discarded when `video_on_failure_only` is true.[10][13][14] |

Capture/save errors are non-fatal in `MobileEvidenceManager`; they are logged as warnings rather than rethrown.[13] An attached native screenshot/video still needs a runner/report renderer that supports Cucumber attachments to be viewable inline.

### Playwright mobile-browser emulation and visual comparisons

`MobileBrowserEvidenceManager` contains configuration-driven full-page screenshot logic for mobile profiles, including output naming and optional Cucumber attachment. It would write to `{mobile_browser.evidence.output_directory}/{RUN_ID}/screenshots/<safe-scenario>.png` when called.[15][16]

**Current source boundary:** no production or test source invokes `MobileBrowserEvidenceManager.captureScenarioScreenshotIfConfigured`. Its settings are implemented but are not wired into the checked-in lifecycle. Changing `screenshot_on_failure`, `screenshot_on_pass`, or `screenshot_after_each_scenario` alone therefore cannot be documented as producing automatic emulated-mobile screenshots in this revision.[15]

Visual comparison is wired through `MobileBrowserVisualSteps`:

```gherkin
Then I compare mobile browser page with visual baseline "<approved-baseline-name>"
```

That step passes the active page, scenario, configured browser profile, and baseline name to `MobileBrowserVisualValidator`.[29] The validator writes an actual image and, when a baseline exists, a diff image under the configured visual output root. It can create a missing baseline under `src/test/resources/baselines/mobile_browser/<profile>/` when `create_baseline_if_missing` is true, and it can attach baseline/actual/diff PNGs to Cucumber when configured.[15][16]

> **Review implication:** automatic baseline creation modifies the source-resource tree. Do not allow an unreviewed test environment to establish an approved visual baseline.

> **Configuration/comment discrepancy:** the implementation calculates mismatch as `mismatched / compared * 100` and compares it directly with `mismatch_threshold_percent`. With the current value `0.10`, the operative threshold is **0.10 percentage points**, not 10%; the accessor comment describes it as 10%. Validate the desired threshold against a controlled visual change before changing it.[15][16]

## API-performance reports

`PerformanceEngine` owns JMeter-DSL HTTP performance evidence. On its first scenario it creates one timestamped run folder below the hard-coded `test-output-performance-reports`, then creates ordered scenario folders such as `01_<sanitized-request-name>`.[17][18]

```text
test-output-performance-reports/
  <dd-MMM-yy_HH-mm-ss>/
    01_<sanitized-request-name>/
      results.jtl
      dashboard/
      summary.txt
      readable-summary.txt
    run-summary.txt
    run-readable-summary.txt
    run-index.txt
    performance-run-report.xlsx
```

For each scenario the engine builds a JMeter DSL test plan with the scenario's JTL, dashboard, and summary paths; parses JTL metrics; writes technical and readable summaries; appends aggregate run text/index entries; and rewrites the run-level Excel workbook.[17][18][19]

The Excel writer produces `performance-run-report.xlsx` with `Executive_Summary`, `Scenario_Summary`, `Risk_Analysis`, `Anomalies`, `Readable_Report`, `Charts`, and `Glossary` sheets, including tables and charts generated with Apache POI.[19]

The performance YAML does contain these reporting keys:

```yaml
performance:
  reporting:
    resultsFolder: <placeholder-results-root>
    dashboardFolder: <placeholder-dashboard-root>
```

`PerformanceConfigurationProperties` exposes them, but the current `PerformanceEngine` initializes its actual run root from its `DEFAULT_REPORTS_BASE_DIR` constant rather than these accessors. Treat `resultsFolder` and `dashboardFolder` as source-visible but **not controlling the engine's current run-root path** unless another caller uses them.[17][18][30]

A typical feature assertion vocabulary is available in the checked-in performance features, for example:

```gherkin
And performance dashboard path should be generated
And performance summary file path should be generated
And performance readable summary file path should be generated
And performance excel report should be generated
```

These assertions verify output paths after the performance execution; they do not create artifacts themselves.[31]

## UI-performance standalone reports and evidence

### Execution flow

The dedicated engine first checks `ui_performance.enabled`, validates the journey and data capacity, creates its timestamped run directory plus `failed-screenshots` and `browser-console-errors` children, executes stages sequentially with concurrent virtual-user browser processes, aggregates metrics, writes reports, optionally adapts results into the existing performance reporter, and **only then** enforces performance thresholds.[20][21][22]

This ordering is useful for diagnosis: a threshold-breach assertion reports the report directory, and reports should already have been written unless report generation itself failed.[20]

A safe configuration-first journey contains no literal URLs, selectors, credentials, or test values:

```gherkin
@ui_performance
Feature: <journey-title>

  Scenario: Configured concurrent browser journey
    Given UI performance journey "<journey-name>" uses configured target
    When UI performance journey navigates to configured route "<route-key>"
    Then UI performance journey verifies locator "<locator-group>" "<locator-key>" is visible
    And the configured UI performance users execute the journey
    And the UI performance run produces a standalone performance report
```

The dedicated step definitions intentionally resolve routes, locators, and data from separate resources. Literal values with locator keys containing `password`, `token`, `secret`, or `credential` are rejected; those values must use a separate data field.[32][33]

### Configuration keys that actually control it

The following keys are read by `UiPerformanceConfiguration` from [`ui_performance-config.yml`](../../../src/test/resources/ui_performance/config/ui_performance-config.yml):

| Configuration group | Keys | Reporting/evidence responsibility |
|---|---|---|
| Master/target | `ui_performance.enabled`, `target.protocol`, `target.host`, `target.port`, `target.base_path`, `target.routes.<name>` | Enables a live run and supplies the configured journey target. Reports retain only the resolved host as metadata.[20][33] |
| Evidence | `evidence.capture_failure_screenshots`, `evidence.capture_console_errors` | Enables per-iteration failure PNGs and browser console-error capture.[20][21] |
| Report formats | `reporting.output_directory`, `reporting.html_enabled`, `reporting.pdf_enabled`, `reporting.csv_enabled`, `reporting.json_enabled` | Controls run root and optional standalone formats. Technical TXT is written regardless of these four format switches.[21][33] |
| Existing reporter adapter | `reporting.existing_performance_reporter_enabled` | Enables the adapted Excel/stage-summary/JTL artifacts in the same UI-performance run folder.[20][22][33] |
| Thresholds | `thresholds.maximum_failure_rate_percent`, `thresholds.maximum_average_journey_duration_ms`, `thresholds.maximum_p95_journey_duration_ms` | Determines post-report assertion failure and adapter threshold assessment.[20][22][33] |

### Run directory contents

With all current standalone report switches enabled and the existing adapter enabled, a run directory contains the following families.[20][21][22]

```text
<output-directory>/<safe-journey>_<profile>_<yyyyMMdd_HHmmss>/
  failed-screenshots/
    <safe-stage>_vu-<n>_<safe-user-id>_iteration-<n>.png
  browser-console-errors/
    <safe-stage>_vu-<n>_<safe-user-id>_iteration-<n>.log
  ui_performance-summary.html                 # if html_enabled
  ui_performance-summary.pdf                  # if pdf_enabled
  ui_performance-summary.json                 # if json_enabled
  ui_performance-stages.csv                   # if csv_enabled
  ui_performance-iterations.csv               # if csv_enabled
  ui_performance-step-timings.csv             # if csv_enabled
  ui_performance-performance-summary.txt      # always written
  performance-run-report.xlsx                 # if existing reporter enabled
  performance-reporter-index.txt              # if existing reporter enabled
  performance-reporter/
    <safe-stage>/
      ui-browser-results.jtl
      summary.txt
      readable-summary.txt
```

The HTML exposes stage configuration/results, percentiles, throughput, transaction timing summaries, and each virtual-user iteration with a sanitized diagnostic. The CSV exports retain raw timing values; JSON includes safe report fields and a failure screenshot path; the PDF contains summary and transaction pages.[21] The adapter writes a JTL-compatible CSV named `.jtl`, but explicitly identifies each row as a **browser journey**, not a network endpoint/JMeter sample.[22]

### UI-performance sensitive-data handling

The standalone writer intentionally excludes full URLs, credentials, tokens, input values, cookies, and session data from the HTML/TXT narrative.[21] Failure messages and captured console-error text flow through `UiPerformanceSensitiveTextSanitizer`, which redacts common password/token/authorization/bearer pairs and selected query-string secret names, normalizes whitespace, and truncates at 500 characters.[20][34]

This is helpful but **not a general data-loss-prevention control**:

- virtual-user IDs are written into iteration CSV/HTML/JSON and failure-evidence filenames;
- PNG evidence can visibly contain test data;
- report JSON includes failure screenshot paths; and
- sanitizer patterns do not prove that every sensitive value form is recognized.[20][21][34]

Use non-personal opaque user IDs, approved synthetic rows, and a protected artifact store. Review screenshots, console logs, raw CSVs, and spreadsheets before sharing them.

## Artifact roots and hygiene

The current [`.gitignore`](../../../.gitignore) excludes `target/`, `test-output/`, `test-output-thread/`, `src/test/downloads/`, `test-output-performance-reports/`, and the UI-performance output root. It also excludes the local UI-performance user CSV (`users.local.csv`).[35] This lowers accidental version-control exposure, but it does not redact an artifact or stop it from being copied to CI, email, a shared drive, or an external report server.

Apply these operating rules:

1. **Use placeholders in features and examples.** Keep secrets, tokens, account numbers, personal data, and private endpoints out of Gherkin, artifact names, logs, and screenshots.
2. **Keep real UI-performance values outside source control.** The engine separates user fields into CSV-backed data and rejects sensitive literal fills in the journey DSL.[32][33]
3. **Use approved synthetic or masked data for screenshots and recordings.** Neither a Base64 Extent attachment nor a PNG/MP4 on disk is automatically sanitized.
4. **Inspect API-performance outputs before distribution.** Its technical/readable summaries write the full target URL and request/payload-source descriptors from the execution result; the source shown here does not apply the UI-performance sanitizer to those reports.[18][19]
5. **Do not publish a raw artifact directory by default.** Share the smallest approved set of reports/evidence, expire access, and remove retained diagnostics according to team policy.
6. **Treat visual baselines as controlled test assets.** Missing-baseline auto-creation can add a resource file; review it before committing or accepting it as expected behavior.[15][16]

## Failure diagnosis map

Start with the emitted file path and the selected runner/profile. Then use the map below to identify the first owning implementation rather than changing unrelated YAML or report files.

| Symptom | First owner to inspect | Relevant configuration / registration | What the source says to check |
|---|---|---|---|
| No standard Cucumber HTML, JSON, or rerun output | [`TestRunner`](../../../src/test/java/com/ptaf/runner/TestRunner.java) and [`pom.xml`](../../../pom.xml) | Runner `plugin` list; Surefire `cucumber.plugin`; `testng.xml` | Confirm the intended TestNG runner/suite was executed. Runner and Surefire specify different JSON paths.[1][2] |
| No combined Extent Spark/Base64/PDF/Excel directory | Selected runner and [`extent.properties`](../../../src/test/resources/extent.properties) | Extent adapter plugin; `basefolder.*`; `extent.reporter.*.start` | The adapter must be registered. Check the timestamped `test-output/<run>/` base, not only a fixed filename.[2][6] |
| UI-performance run has no Extent or per-feature reports | [`UiPerformanceRunner`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) | `-Pui_performance` runner plugins | Expected boundary: this runner has only built-in HTML/JSON/JUnit outputs and does not register Extent/listeners.[5] |
| Per-feature report is missing while combined Extent exists | [`PerFeatureReportListener`](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java), selected runner | Listener plugin plus `reporting.per_feature_reports_enabled` | Both registration and switch are required; inspect the configured output directory and listener logs.[2][7][8] |
| Per-feature name is unexpected | `PerFeatureReportListener` / `FeatureArtifactNameResolver` | Declared `Feature:` title | Reports/artifacts use sanitized declared feature title; missing title falls back to file stem, not scenario name.[7][10] |
| Per-feature HTML exists but one PDF variant is absent | `PerFeatureReportListener` | `per_feature_pdf_enabled`, `per_feature_glass_pdf_enabled`, Glass output dir | Direct and Glass PDFs are independently conditional. A Glass subprocess error is logged without invalidating HTML.[7][8] |
| Glass PDF times out or has no output | `PerFeatureReportListener` / `GlassPdfSubprocessGenerator` | `per_feature_glass_pdf_enabled` | Check subprocess stderr and output permission. Actual timeout is 120 seconds despite a stale 60-second Javadoc statement.[7] |
| Soft-failed step is green in Extent | [`SoftAssertionReportListener`](../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java) | `soft_assertions.enabled`; listener plugin | The listener only acts when enabled, only for Gherkin steps, and requires an active Extent current step.[25][27] |
| Screenshot file exists but is not inline in HTML/PDF | `ScreenshotHandler`, `PerFeatureReportListener`, selected reporter | `Scenario.attach` timing and image MIME type | Disk persistence alone does not attach. The per-feature listener processes image `EmbedEvent` attachments.[7][11] |
| Expected automatic passed screenshot is absent | `Hooks` and `PageCommonMethods` | No global pass-screenshot flag | Current teardown/finalize conditions are inconsistent; use explicit capture for required passed-state evidence.[9][26] |
| Failure screenshot is absent | `ScreenshotHandler`, `UIAssert`, `PageCommonMethods` | Active page/locator/frame must remain usable | Capture errors are swallowed/logged. A closed page or unresolved frame/locator can prevent the artifact while preserving the test failure.[11][26] |
| Download goes to an unexpected `.jpeg`-looking directory | `PageCommonSteps` then `ActionPerformer` | `downloadDocument` | The standard binding appends `.jpeg` before passing the path as a directory root; the download action then creates a feature subfolder.[11][12][27] |
| Strict download fails or optional download silently yields no file | `ActionPerformer` | `download` vs `download_optional`; action timeout | Strict waits for an event and throws. Optional catches both no-event and general errors and returns `null`.[12] |
| Video is absent | `BrowserFactory` and `Hooks` | Global `videoCapture`; mobile-browser `video_recording_enabled` | Ensure the right context type was created with recording enabled. Recording is finalized only after close.[9][10][16] |
| Video remains anonymous or not feature-named | `Hooks.renameRecordedVideos` | Browser must close through `Hooks.closeBrowserResources` | Review handle collection, post-close availability, move retry, and direct browser-close paths that bypass the renamer.[9] |
| Native mobile screenshot/video is absent | `MobileHooks` / `MobileEvidenceManager` | `mobile.evidence.*`; Appium `CanRecordScreen` support | Mobile capture is opt-in by tag; recording requires supported driver, and capture/save exceptions are non-fatal.[13][14] |
| Emulated mobile-browser automatic screenshot is absent | `MobileBrowserEvidenceManager` | `mobile_browser.evidence.screenshot_*` | The helper has no current caller. This is an implementation-wiring gap, not merely a Boolean setting.[15] |
| Visual comparison creates a baseline unexpectedly or fails threshold | `MobileBrowserVisualValidator` / `MobileBrowserVisualSteps` | `create_baseline_if_missing`, baseline/output dirs, `mismatch_threshold_percent` | Check whether baseline creation was allowed and remember the code compares percentage values directly; current `0.10` means 0.10%.[15][16][29] |
| API-performance artifacts are not under YAML `resultsFolder` | `PerformanceEngine` | Hard-coded `DEFAULT_REPORTS_BASE_DIR` | The active engine uses `test-output-performance-reports`, while YAML accessors expose unused-in-engine results/dashboard settings.[17][18][30] |
| API-performance Excel or aggregate summaries are missing | `PerformanceEngine`, `PerformanceSummaryWriter`, `PerformanceExcelReportWriter` | Writable run root; completed scenario result | Per-scenario summary, aggregate appends, then Excel rewrite happen during finalization. File I/O failures propagate as runtime failures.[18][19] |
| UI-performance format is absent | `UiPerformanceReportWriter` / `UiPerformanceConfiguration` | `html_enabled`, `pdf_enabled`, `csv_enabled`, `json_enabled` | Each named format is conditional; `ui_performance-performance-summary.txt` is unconditional.[21][33] |
| UI-performance workbook/stage JTL is absent | `UiPerformanceExistingReporterAdapter` | `existing_performance_reporter_enabled` | When disabled, the engine skips adapter artifacts; when enabled, inspect `performance-reporter/` and root workbook/index.[20][22][33] |
| UI-performance run fails after reports are written | `UiPerformanceEngine.enforceThresholds` | UI-performance `thresholds.*` | Threshold enforcement occurs after report writer and optional adapter, so use the reported directory for diagnosis.[20] |
| UI-performance report seems to expose sensitive data | `UiPerformanceReportWriter`, `UiPerformanceSensitiveTextSanitizer`, evidence capture | Sanitizer patterns; CSV user IDs; screenshot policy | Validate user IDs, screenshot content, console logs, raw exports, and unsupported secret formats; redaction is targeted rather than comprehensive.[20][21][34] |

## Related chapters

To avoid duplicating their future detailed ownership, read this chapter alongside these local future chapter filenames:

- `01-foundation-configuration-and-execution.md` — Maven, TestNG, runner, and configuration precedence.
- `02-ui-web-automation.md` — page actions, locators, browser lifecycle, and functional test authoring.
- `03-api-automation.md` — API request/authentication behavior and non-performance API testing.
- `05-mobile-native-automation.md` — Appium platforms, capabilities, and native test execution.
- `06-mobile-browser-automation.md` — emulation profiles and visual-regression test design.
- `09-api-performance-testing.md` — JMeter DSL request/load/assertion mechanics.
- `10-ui-performance-load-testing.md` — real-browser concurrency, journey DSL, profiles, and thresholds.

## Source references

- [Maven, Surefire, and UI-performance profile](../../../pom.xml)
- [Default TestNG Cucumber runner](../../../src/test/java/com/ptaf/runner/TestRunner.java)
- [Extent reporter properties](../../../src/test/resources/extent.properties)
- [Per-feature Cucumber listener](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java)
- [Web hooks and feature-video finalization](../../../src/main/java/com/ptaf/hooks/Hooks.java)
- [Feature artifact naming resolver](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java)
- [Native mobile evidence manager](../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java)
- [API-performance engine](../../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java)
- [UI-performance engine](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java)
- [UI-performance report writer](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java)

## References

[1]: ../../../pom.xml "Maven build, Surefire reporting configuration, and UI-performance profile"
[2]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[3]: ../../../src/test/java/com/ptaf/runners/ApiTestRunner.java "API Cucumber runner"
[4]: ../../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Native mobile Cucumber runner"
[5]: ../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java "Dedicated UI-performance Cucumber runner"
[6]: ../../../src/test/resources/extent.properties "Extent adapter reporter paths and switches"
[7]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature Extent HTML and PDF listener"
[8]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Framework reporting configuration accessors"
[9]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Web browser lifecycle and video finalization"
[10]: ../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-based artifact directory and filename resolver"
[11]: ../../../src/main/java/com/ptaf/utils/ScreenshotHandler.java "Cucumber screenshot attachment utility"
[12]: ../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "UI screenshot, full-page screenshot, and download actions"
[13]: ../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java "Native Appium screenshots and recordings"
[14]: ../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java "Native mobile evidence configuration accessors"
[15]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java "Playwright mobile-browser evidence helper"
[16]: ../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml "Mobile-browser evidence and visual configuration"
[17]: ../../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java "HTTP performance execution and report finalization"
[18]: ../../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java "Performance technical and readable text summaries"
[19]: ../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java "Run-level performance Excel workbook writer"
[20]: ../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java "Isolated real-browser UI-performance execution and evidence"
[21]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java "UI-performance standalone HTML, PDF, CSV, JSON, and text outputs"
[22]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java "UI-performance adaptation to existing performance reports"
[23]: ../../../src/test/resources/extent-config.xml "Extent report display configuration"
[24]: ../../../src/test/resources/cucumber.properties "Cucumber publication setting"
[25]: ../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java "Soft assertion Extent report correction"
[26]: ../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java "Page action failure and screenshot behavior"
[27]: ../../../src/test/resources/config/config.yml "Global UI, video, download, and per-feature reporting configuration"
[28]: ../../../src/test/resources/mobile/config/mobile-config.yml "Native mobile evidence configuration"
[29]: ../../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java "Mobile browser visual-comparison Gherkin binding"
[30]: ../../../src/test/resources/performance/config/performance-config.yml "HTTP performance defaults and reporting settings"
[31]: ../../../src/test/resources/features/performance/performance_full_regression.feature "Performance report assertion examples"
[32]: ../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "UI-performance Gherkin journey DSL and sensitive literal guard"
[33]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java "UI-performance configuration keys and validation"
[34]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java "UI-performance report diagnostic sanitizer"
[35]: ../../../.gitignore "Ignored generated-artifact roots and local UI-performance user data"
