# FNB-ETAF UI Performance Load Testing Guide

## Purpose and scope

The **UI Performance** module is the FNB-ETAF facility for exercising a configured browser journey with concurrent, real Chromium browser processes. It supports **load, stress, spike, and soak** profiles. The module is intentionally separate from the framework's normal UI lifecycle: Cucumber and TestNG invoke one isolated journey, while `UiPerformanceEngine` creates and coordinates the browser users, collects browser-journey measurements, evaluates thresholds, and writes standalone reports. The module measures rendered browser actions rather than HTTP endpoints. [1] [2] [3]

This guide covers only the isolated `com.ptaf.ui_performance` module and the bundled eStore Consumer Deposit journey resources. It does **not** describe the normal UI, API, database, mobile, or JMeter performance suites. Run live load only against an approved target with prepared test data and suitable workstation or CI capacity.

## Architecture and source locations

| Concern | Implementation and exact location | Responsibility |
|---|---|---|
| Maven entry point | [`pom.xml`][1], profile `ui_performance` | Replaces the default Surefire suite with the dedicated suite, disables Surefire parallelism, and fails the profile on test failure. |
| TestNG suite | [`src/test/resources/ui_performance/testng-ui_performance.xml`][4] | Invokes the UI Performance runner and offline contract tests. Its suite is non-parallel. |
| Cucumber runner | [`com.ptaf.ui_performance.runners.UiPerformanceRunner`][5] at `src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java` | Selects `src/test/resources/ui_performance/features`, dedicated glue, and `@ui_performance and not @template`; writes isolated Cucumber results. |
| Journey DSL | [`com.ptaf.ui_performance.stepdefinitions.UiPerformanceSteps`][6] at `src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java` | Builds a `UiPerformanceJourney` from the supported performance-only Gherkin steps, resolves route and locator names, and starts the engine. |
| Configuration | [`UiPerformanceYamlReader`][7] and [`UiPerformanceConfiguration`][8] in package `com.ptaf.ui_performance.config` | Reads only the dedicated classpath YAML; validates target construction, active profile, browser options, data mode, reporting, and limits. |
| Workload models | [`UiPerformanceExecutionPlan`][9], [`UiPerformanceStage`][10], [`UiPerformanceRunProfile`][11], and [`UiPerformanceTestType`][12] in package `com.ptaf.ui_performance.model` | Represent and validate the selected profile, its sequential stages, stage mode, safety maximum, browser settings, and the four allowed test types. |
| Concurrent browser executor | [`com.ptaf.ui_performance.core.UiPerformanceEngine`][2] | Owns virtual-user threading, one Playwright/Chromium process per worker, browser action execution, per-step timing, failure classification, threshold enforcement, and cleanup. |
| Synchronized-start coordinator | [`com.ptaf.ui_performance.core.UiPerformanceStartGate`][3] | Waits for every requested worker to prepare or record a launch failure before releasing prepared workers through one common signal. |
| eStore resources | [`ui_performance-config.yml`][13], [`estore_ui_performance.feature`][14], [`ui_performance-locators.yml`][15], and [`users.csv`][16] | Hold target/profile settings, the journey, separate locators, and virtual-user data. Keep sensitive values out of feature files and reports. |
| Data and locator access | [`UiPerformanceUserDataReader`][17] and [`UiPerformanceLocatorRepository`][18] | Read a simple UTF-8 CSV and the module-only locator YAML. |
| Reports | [`UiPerformanceReportManager`][19], [`UiPerformanceReportWriter`][20], [`UiPerformanceExistingReporterAdapter`][21], and [`UiPerformanceDurationFormatter`][22] | Create a timestamped run directory; produce browser reports, optional compatible existing-performance artifacts, and readable duration fields. |
| Executable contracts | [`UiPerformanceModuleContractTest`][23], [`UiPerformanceStartGateTest`][24], and [`UiPerformanceConcurrentBrowserIntegrationTest`][25] | Verify configuration, report/redaction behavior, start-gate release, and a local two-browser browser contract. |

## Prerequisites and operating guardrails

Use **JDK 21** and Maven. The build declares Java source and target level 21 and includes Playwright 1.62.0. Install the Playwright browser binaries before the first browser run. The repository README provides the following browser-install command: [1] [26]

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Before a live run, obtain target approval, confirm that the selected profile is appropriate for the environment, ensure the machine can launch the largest requested number of real browsers, and prepare data rows for the largest stage. The `ui_performance.enabled` flag is an explicit live-run guard; the engine rejects execution when it is false. A stage cannot request more users than `ui_performance.safety.max_virtual_users`. [8] [10]

Treat each virtual user as an actual Chromium process, not a lightweight HTTP client. Start with a small approved profile and increase the configured cap only after capacity review. A data-bearing profile with reuse disabled needs at least one usable CSV row for every user in its largest stage. [2] [17]

## Configuration files and key settings

All module settings are classpath resources below `src/test/resources/ui_performance`. `UiPerformanceYamlReader` loads `ui_performance/config/ui_performance-config.yml` by default. For a controlled CI/CD configuration variant, pass a **classpath resource** through `-Dui.performance.config=<classpath-resource>`; the reader does not accept an arbitrary filesystem path. [7]

### Main configuration

Edit [`src/test/resources/ui_performance/config/ui_performance-config.yml`][13] for the approved environment and workload. The following table describes the fields consumed by the code. Values in this guide are placeholders; do not put secrets, credentials, tokens, or unapproved targets in source control.

| YAML area | Key settings | Runtime effect and validation |
|---|---|---|
| `ui_performance.enabled` | `true` or `false` | Must be `true` for a live journey. The engine stops before browser launch when it is false. |
| `target` | `protocol`, bare `host`, `port`, `base_path`, `routes` | Builds the base URL from the first four values. A route is named and relative; it must not be a complete URL. Protocol is `http` or `https`; host may not contain protocol, path, or port. |
| `active_profile` and `profiles` | One of `load`, `stress`, `spike`, `soak`; each has `type` and `stages` | Selects and validates the workload plan. Stress and spike require at least two stages. Soak requires at least one duration-based stage. |
| stage fields | `name`, `users`, `ramp_up_seconds`, `hold_seconds`, `iterations_per_user` | Select stage concurrency and timing mode. `users` must be at least 1 and no more than the safety maximum. Values cannot be negative. |
| `browser` | `headless`, `ignore_https_errors`, `isolation`, `user_agent`, `action_timeout_ms`, `navigation_timeout_ms` | Launches Chromium using `headless`; applies the HTTPS-error option and user agent to each fresh context; applies action and navigation timeouts. `isolation` must be exactly `process` (case-insensitive). |
| `execution` | `synchronized_start_timeout_ms`, `between_iterations_ms` | Limits waiting for browser preparation and optionally pauses between repeat iterations. The start timeout and browser timeouts must be positive; the pause may be zero. |
| `data` | `use_csv`, `users_csv`, `allow_user_reuse` | Enables CSV substitutions and selects the classpath CSV. When CSV use is false, a journey containing `${column}` must fail before browser launch. Reuse defaults to false. |
| `evidence` | `capture_failure_screenshots`, `capture_console_errors` | Saves full-page screenshots only after failed iterations and writes browser console messages whose type is `error`. |
| `thresholds` | maximum failure rate, average journey duration, and P95 journey duration | The completed run fails if a measured value is greater than its configured threshold. |
| `safety` | `max_virtual_users` | The only maximum applied by the module; it must be at least 1. |
| `reporting` | output directory, existing-reporter switch, HTML/PDF/CSV/JSON switches | Chooses the timestamped report root and optional artifacts. The technical text summary is always written. |

A safe configuration shape is:

```yaml
ui_performance:
  enabled: <true-or-false>
  active_profile: <load-or-stress-or-spike-or-soak>
  target:
    protocol: <https-or-http>
    host: <approved-target-host>
    port: <approved-port>
    base_path: <optional-base-path>
    routes:
      journey_entry: <relative-route>
  profiles:
    load:
      type: load
      stages:
        - name: <stage-name>
          users: <positive-user-count>
          ramp_up_seconds: <non-negative-seconds>
          hold_seconds: 0
          iterations_per_user: <positive-count>
  browser:
    headless: <true-or-false>
    ignore_https_errors: <true-or-false>
    isolation: process
    user_agent: <approved-user-agent>
    action_timeout_ms: <positive-milliseconds>
    navigation_timeout_ms: <positive-milliseconds>
  execution:
    synchronized_start_timeout_ms: <positive-milliseconds>
    between_iterations_ms: <non-negative-milliseconds>
  data:
    use_csv: <true-or-false>
    users_csv: ui_performance/data/users.csv
    allow_user_reuse: <true-or-false>
  thresholds:
    maximum_failure_rate_percent: <0-to-100>
    maximum_average_journey_duration_ms: <positive-milliseconds>
    maximum_p95_journey_duration_ms: <positive-milliseconds>
  safety:
    max_virtual_users: <positive-user-cap>
  reporting:
    output_directory: test-output/ui_performance
    existing_performance_reporter_enabled: <true-or-false>
    html_enabled: <true-or-false>
    pdf_enabled: <true-or-false>
    csv_enabled: <true-or-false>
    json_enabled: <true-or-false>
```

### Profiles, stages, ramp, hold, and iterations

A stage is **iteration-based** when `iterations_per_user` is greater than zero. Every assigned virtual user runs that exact number of journey iterations; `hold_seconds` does not control the stopping condition in this mode. A stage is **duration-based** when `iterations_per_user: 0`; it then requires `hold_seconds >= 1` and repeats journeys until the shared stage deadline. For duration-based stages, the deadline is established as `ramp_up_seconds + hold_seconds` immediately after all workers have prepared their browsers. [2] [10]

The `load`, `stress`, `spike`, and `soak` names are validated types, not different browser implementations. The selected profile's stages run **sequentially**. Each stage independently selects its configured user count, creates its own worker pool, and completes before the next stage begins. [2] [9] [12]

Use the following placeholder-only examples as configuration patterns:

```yaml
# Fixed-count load: each user runs exactly <iterations> journeys.
load:
  type: load
  stages:
    - name: <normal-load>
      users: <users>
      ramp_up_seconds: 0
      hold_seconds: 0
      iterations_per_user: <iterations>

# Multi-stage stress: each duration stage repeats until its shared deadline.
stress:
  type: stress
  stages:
    - name: <lower-load>
      users: <lower-users>
      ramp_up_seconds: 0
      hold_seconds: <seconds>
      iterations_per_user: 0
    - name: <higher-load>
      users: <higher-users>
      ramp_up_seconds: <seconds>
      hold_seconds: <seconds>
      iterations_per_user: 0

# Immediate spike is a duration-based stage with zero ramp.
spike:
  type: spike
  stages:
    - name: <baseline>
      users: <baseline-users>
      ramp_up_seconds: 0
      hold_seconds: <seconds>
      iterations_per_user: 0
    - name: <spike>
      users: <spike-users>
      ramp_up_seconds: 0
      hold_seconds: <seconds>
      iterations_per_user: 0

# Soak requires at least one duration-based stage.
soak:
  type: soak
  stages:
    - name: <sustained-load>
      users: <users>
      ramp_up_seconds: <seconds>
      hold_seconds: <longer-approved-seconds>
      iterations_per_user: 0
```

### What “concurrent” means in this module

Concurrency is deliberately owned by `UiPerformanceEngine`, not by Cucumber scenario parallelism or the normal Maven Surefire configuration. The `ui_performance` Maven profile sets Surefire to `parallel=none` and `threadCount=1`; the dedicated TestNG suite is also non-parallel. This prevents outer test scheduling from becoming the load model. [1] [4] [5]

For each stage, the engine creates a fixed thread pool whose size equals `users`. Each virtual-user task creates its own `Playwright` instance, launches its own Chromium browser, and uses a separate browser context and page for every journey iteration. Browser launch happens before the timing of the first journey action. [2]

Every worker marks itself ready after it launches its browser, then waits at `UiPerformanceStartGate`. The engine waits up to `synchronized_start_timeout_ms` for all requested workers to prepare or report a browser-start failure, records the stage start, configures a duration deadline when applicable, and releases the common signal. With `ramp_up_seconds: 0`, prepared users proceed directly to their first action after this release. With a positive ramp, worker index `i` waits `round(ramp × 1000 × i / (users - 1))` milliseconds before its first iteration. The first-start spread in the stage reports is the difference between earliest and latest recorded first-iteration starts. [2] [3] [27]

This design makes the first configured browser action concurrent for a zero-ramp stage after browser preparation; it does not claim that every browser process starts at the same instant. Later iterations are independent worker loops and may diverge because of journey duration, the optional between-iteration pause, failures, or scheduling. The local integration contract checks that a two-user zero-ramp stage starts within one second, rather than asserting a zero-millisecond spread. [25]

## Build and run

Run all commands from the repository root:

```bash
cd /home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23
```

Compile and package without executing tests:

```bash
mvn clean package -DskipTests
```

Run the isolated UI Performance suite after the target, profile, data, thresholds, and machine capacity have been approved:

```bash
mvn clean test -Pui_performance
```

Run the same dedicated suite with an approved alternative **classpath** configuration resource packaged under `src/test/resources`:

```bash
mvn clean test -Pui_performance -Dui.performance.config=ui_performance/config/<approved-config>.yml
```

The active runner writes Cucumber output below `test-output/ui_performance/cucumber` and each engine invocation creates a timestamped subdirectory below the configured reporting output directory. A threshold breach is intentionally a failed Maven test in this profile, even though the reports have already been written. [1] [2] [5] [19]

## Create a new UI Performance journey

Create journeys with the dedicated DSL only. Do not add normal UI glue, normal hooks, normal pages, normal element files, or hard-coded target values to a UI Performance feature. `UiPerformanceJourney` explicitly validates that it has steps and an HTTP(S) base URL, while its class documentation states that these journeys cannot call normal UI step definitions, hooks, or lifecycle classes. [6] [28]

1. Add or update a named relative route in `ui_performance.target.routes` in [`ui_performance-config.yml`][13]. Keep protocol, host, port, and base path in `target`; do not place a full URL in the feature or route value.
2. Add a separate locator group and keys in [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`][15]. Values use the existing `TYPE_value` convention, for example `CSS_<selector>`, `XPATH_<selector>`, or `Button_<label>`. The resolver rejects absent groups or keys. [18]
3. If the journey fills or selects values from data, add the required column headers and enough approved rows to [`src/test/resources/ui_performance/data/users.csv`][16]. The first header must be `user_id`; at least one data column is required. The reader is a simple comma splitter, so quoted values containing commas are unsupported. Use placeholders such as `${field_name}` only through the data-field Gherkin steps. [17]
4. Add a `.feature` file under [`src/test/resources/ui_performance/features`][14] with `@ui_performance`, excluding `@template` if it must run through the default runner. Use a clear journey name and the configuration-first target and route steps.
5. Choose or add an approved profile in [`ui_performance-config.yml`][13], set its data capacity and thresholds, then execute the exact Maven command in the preceding section.

The supported action set is `NAVIGATE`, `FILL`, `SELECT_OPTION`, `CLICK`, `CLICK_AND_SWITCH_TO_POPUP`, and `VERIFY_VISIBLE`. The DSL maps these to navigation, locator fill/select/click, popup switching, and visible-state waits. Navigation waits for `DOMContentLoaded` and fails on response status 400 or higher. All locator actions require a `TYPE_value` locator definition. [2] [6] [29]

A tested-style feature pattern is:

```gherkin
@ui_performance
Feature: <business-journey> UI Performance

  Scenario: Concurrent users execute the approved browser journey
    Given UI performance journey "<journey-name>" uses configured target
    When UI performance journey navigates to configured route "<route-name>"
    Then UI performance journey verifies locator "<locator-group>" "<ready-key>" is visible
    And UI performance journey fills locator "<locator-group>" "<field-key>" with data field "<csv-column>"
    And UI performance journey selects locator "<locator-group>" "<select-key>" with data field "<csv-column>"
    And UI performance journey clicks locator "<locator-group>" "<submit-key>"
    And UI performance journey clicks locator "<locator-group>" "<popup-key>" and switches to popup
    Then UI performance journey verifies locator "<locator-group>" "<result-key>" is visible
    And the configured UI performance users execute the journey
    And the UI performance run produces a standalone performance report
```

Literal fill is available only for non-sensitive values. The step definition rejects a locator key containing `password`, `token`, `secret`, or `credential` when a literal is supplied. Keep sensitive data out of features and use a separately managed CSV field where a value is necessary. [6]

## Feature, data, locator, payload, and query locations

| Asset | UI Performance location | Use in this module |
|---|---|---|
| Features | `src/test/resources/ui_performance/features/` | Store the bundled eStore feature and new `@ui_performance` journeys here. |
| Main configuration | `src/test/resources/ui_performance/config/ui_performance-config.yml` | Store target composition, named routes, profiles, browser controls, evidence, thresholds, and reporting controls. |
| Optional configuration fixture | `src/test/resources/ui_performance/config/ui_performance-browser-contract.yml` | Local browser-contract configuration fixture; select it only through the classpath system property when running that contract. |
| User data | `src/test/resources/ui_performance/data/users.csv` | Store `user_id` plus the non-secret fields used by `${column}` substitutions. |
| Locators | `src/test/resources/ui_performance/locators/ui_performance-locators.yml` | Store UI Performance-only locator groups and `TYPE_value` definitions. |
| UI Performance Java | `src/main/java/com/ptaf/ui_performance/` | Contains the isolated config, core, data, metrics, model, and reporting packages. |
| UI Performance test Java | `src/test/java/com/ptaf/ui_performance/` | Contains the runner, DSL step definitions, and module contracts. |
| UI Performance payloads | No module-specific payload directory is read by the current code. | Do not assume that `src/test/resources/performance/payloads/` is part of a UI Performance journey. |
| Queries | No module-specific query location is read by the current code. | Do not assume that `src/test/resources/queries/` is part of a UI Performance journey. |

## Reports, evidence, and readable durations

`UiPerformanceReportManager` creates `<output_directory>/<sanitized-journey>_<yyyyMMdd_HHmmss>/`, including `failed-screenshots/` and `browser-console-errors/`. With the default reporting switches enabled, expect the following artifacts. [19] [20] [21]

| Artifact | Written when | Contents |
|---|---|---|
| `ui_performance-summary.html` | `html_enabled: true` | Browser-load dashboard with profile/stage results, journey percentiles, throughput, first-start spread, per-step timing summaries, and iteration evidence. |
| `ui_performance-summary.pdf` | `pdf_enabled: true` | PDF summary plus browser transaction timing page. |
| `ui_performance-stages.csv` | `csv_enabled: true` | One row per stage, including raw and readable timing fields, samples, errors, throughput, and first-start spread. |
| `ui_performance-iterations.csv` | `csv_enabled: true` | One row per virtual-user journey iteration, including raw/readable duration, step timing, status, sanitized diagnostic, and console-error count. |
| `ui_performance-step-timings.csv` | `csv_enabled: true` | Aggregated min, average, median, P90, P95, P99, and max timing for each successful recorded step. |
| `ui_performance-summary.json` | `json_enabled: true` | Machine-readable safe report map, with raw millisecond fields and readable companion fields. |
| `ui_performance-performance-summary.txt` | Always | Standalone technical text summary. |
| `failed-screenshots/*.png` | Evidence enabled and an iteration fails | Full-page screenshot of the active page when capture succeeds. |
| `browser-console-errors/*.log` | Console capture enabled and error messages occur | Captured browser `error` console messages. |
| `performance-run-report.xlsx`, `performance-reporter-index.txt`, `performance-reporter/<stage>/...` | `existing_performance_reporter_enabled: true` | Existing Performance reporter workbook and stage summaries. Each stage is represented as a real-browser scenario; the JTL-compatible CSV contains browser-journey samples, not endpoint samples. |
| `cucumber/cucumber.html`, `cucumber.json`, `cucumber.xml` | Dedicated runner | Cucumber execution artifacts written outside individual engine run folders. |

The report writer renders durations below one second as `N ms`, durations below one minute as seconds with millisecond precision where needed, and longer values with minute or hour components. For example, `23383` milliseconds is displayed as `23.383 s`, while CSV and JSON retain raw `*_ms` values. [20] [22]

Reports deliberately use the configured target **host**, not a full target URL. The HTML and text report generation exclude full URLs, credentials, tokens, input values, cookies, and session data. Failure messages and captured console errors pass through `UiPerformanceSensitiveTextSanitizer`, which redacts common password, token, authorization, bearer, access-token, and client-secret patterns and bounds diagnostic length. This is a reporting safeguard, not a reason to put secrets in CSV, configuration, or feature files. [2] [20] [30]

## Bundled eStore journey

The checked-in runnable journey is [`estore_ui_performance.feature`][14]. It creates an eStore Consumer Deposit application URL independently for each virtual user. The feature remains intentionally clean: it contains only the journey actions, while the target, user count, headless mode, timeouts, locators, data, thresholds, and report switches remain in the dedicated YAML and CSV resources.

The current journey uses the named `test_harness` route and `test_harness` locator group. Each worker waits for the harness, fills its assigned email and phone number, selects the Consumer Deposit product group and product, creates the application URL, and verifies the generated URL plus the available **Open URL** action. The engine records journey and step timings for every worker iteration.

For the default fixed-count load stage, the configuration is equivalent to the following pattern:

```yaml
profiles:
  load:
    type: load
    stages:
      - name: normal-load
        users: 2
        ramp_up_seconds: 0
        hold_seconds: 0
        iterations_per_user: 1
```

With `ramp_up_seconds: 0`, the configured workers prepare their independent browsers first and are released through the same start-gate signal. To increase simultaneous users, change `users` in the selected stage, provide enough approved CSV rows when reuse is disabled, and raise `safety.max_virtual_users` only after capacity and environment approval. Use `headless: true` for normal load execution; reserve `headless: false` for a small diagnostic run.

> **eStore target note:** the bundled configuration uses a desktop browser identity because the eStore edge returned HTTP 403 to the default `HeadlessChrome` identity during the checked validation. Do not copy a specific target, test-user value, or private configuration into a feature or report. Keep environment-specific values in the approved dedicated configuration.

## Troubleshooting

| Symptom | Source-supported cause | Resolution |
|---|---|---|
| `UI performance execution is disabled` | `ui_performance.enabled` is false. | Enable it only after approval and readiness checks. |
| Start-gate timeout before all browsers prepare | One or more processes did not prepare within `synchronized_start_timeout_ms`. | Increase the configured start timeout or reduce `users` for the machine. Check browser installation and available capacity. |
| A stage rejects the requested user count | `users` exceeds `safety.max_virtual_users`, or is below 1. | Correct the stage or explicitly raise the approved safety maximum. |
| Insufficient CSV users | CSV has fewer usable rows than the largest stage while reuse is false. | Add approved rows. Enable `allow_user_reuse` only for an approved journey where reuse is safe. |
| Unresolved `${column}` error or data placeholders with CSV disabled | A feature asks for a missing CSV column, or `use_csv: false` conflicts with a data placeholder. | Match the feature field name to the CSV header and enable CSV, or remove data-field actions from the data-free journey. |
| CSV row/header validation error | The first header is not `user_id`, headers are blank, a row has a different cell count, or a value contains an unsupported quoted comma. | Provide a simple UTF-8 comma-separated file with matching columns and no quoted comma-containing values. |
| `browser.isolation must be 'process'` | The module accepts only process isolation because its Playwright objects are not shared across worker threads. | Set `ui_performance.browser.isolation: process`; do not substitute a context or shared-browser mode. |
| Locator group/key not found or invalid locator definition | The separate locator YAML lacks the requested entry, or the value is not `TYPE_value`. | Add the correct group/key to the UI Performance locator resource and use a supported locator type handled by `LocatorHandler`. |
| Navigation failure, locator/state failure, or timeout | The engine records HTTP status 400+, waits for visible state, and classifies common failures as `NETWORK`, `TIMEOUT`, `LOCATOR_OR_UI_STATE`, or `BROWSER_OR_APPLICATION`. | Read the sanitized iteration diagnostic, browser console-error logs, optional failure screenshot, and per-step timings in the run folder. Verify target, route, locator, and timeout configuration. |
| Maven command fails after reports were created | Failure rate, average journey duration, or P95 duration exceeded the configured threshold. | Review the stage and iteration reports, adjust the approved workload or remediation target, and change thresholds only through the applicable approval process. |
| Expected artifact is absent | Its reporting/evidence switch is false, or no matching event occurred for conditional evidence. | Check the corresponding `reporting` or `evidence` key and the run-folder timestamp. The text summary is always attempted by the report writer. |

## Module boundaries with the rest of FNB-ETAF

The UI Performance runner scans only `com.ptaf.ui_performance.stepdefinitions`. It does not include `com.ptaf.hooks`, ordinary UI step definitions, mobile hooks, or Extent/PDF listeners. Its feature root and output locations are also module-specific. [5]

The normal UI runner is a separate runner with normal feature resources and glue that includes `com.ptaf.hooks`. In contrast, normal `Hooks` constructs the normal browser stack through `com.ptaf.utils.BrowserFactory`, applies normal configuration, and may manage video capture and feature lifecycle. None of that lifecycle is entered by the UI Performance runner. [31] [32] [33]

The module also keeps its settings away from the framework-wide YAML reader, normal UI locator files under `src/test/resources/elements`, API request resources, database queries, mobile resources, and JMeter configuration. Its only intentional reuse is **reporting adaptation**: `UiPerformanceExistingReporterAdapter` converts stage-level real-browser results into the existing Performance reporter formats. It does not execute JMeter and does not treat browser journeys as HTTP endpoint samples. [7] [15] [21]

> **Boundary rule:** use the normal Hooks/BrowserFactory pathway for ordinary functional UI execution. Use the `ui_performance` Maven profile and `com.ptaf.ui_performance` resources for controlled concurrent real-browser performance execution. Do not combine the two in one feature or runner.

## References

<!-- Visible source-reference list -->
The sources below are visible and clickable in Markdown preview. Citation labels used in this guide point to the same source files.

- **[1]** [Maven build configuration and ui_performance profile](../../pom.xml) — `../../pom.xml`
- **[2]** [UiPerformanceEngine concurrent browser execution](../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java) — `../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java`
- **[3]** [UiPerformanceStartGate synchronized release](../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java) — `../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java`
- **[4]** [Dedicated UI Performance TestNG suite](../../src/test/resources/ui_performance/testng-ui_performance.xml) — `../../src/test/resources/ui_performance/testng-ui_performance.xml`
- **[5]** [Dedicated UI Performance Cucumber runner](../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) — `../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`
- **[6]** [UI Performance Cucumber step definitions](../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java) — `../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`
- **[7]** [Isolated UI Performance YAML reader](../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java) — `../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java`
- **[8]** [UI Performance configuration accessors](../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java) — `../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java`
- **[9]** [UI Performance execution plan](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java`
- **[10]** [UI Performance stage validation](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java`
- **[11]** [UI Performance browser and threshold profile](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java`
- **[12]** [Supported UI Performance test types](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java`
- **[13]** [UI Performance eStore configuration](../../src/test/resources/ui_performance/config/ui_performance-config.yml) — `../../src/test/resources/ui_performance/config/ui_performance-config.yml`
- **[14]** [Bundled eStore UI Performance journey](../../src/test/resources/ui_performance/features/estore_ui_performance.feature) — `../../src/test/resources/ui_performance/features/estore_ui_performance.feature`
- **[15]** [UI Performance locator repository](../../src/test/resources/ui_performance/locators/ui_performance-locators.yml) — `../../src/test/resources/ui_performance/locators/ui_performance-locators.yml`
- **[16]** [UI Performance user-data CSV](../../src/test/resources/ui_performance/data/users.csv) — `../../src/test/resources/ui_performance/data/users.csv`
- **[17]** [UI Performance CSV user-data reader](../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java) — `../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java`
- **[18]** [UI Performance locator repository reader](../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java) — `../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java`
- **[19]** [UI Performance report directory manager](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java) — `../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java`
- **[20]** [UI Performance report writer](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java) — `../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java`
- **[21]** [Existing Performance reporter adapter for UI browser journeys](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java) — `../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java`
- **[22]** [Readable UI Performance duration formatter](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java) — `../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java`
- **[23]** [UI Performance module contract tests](../../src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java) — `../../src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java`
- **[24]** [UI Performance start-gate contract test](../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java) — `../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java`
- **[25]** [Local concurrent browser integration contract](../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java) — `../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java`
- **[26]** [PTAF project README and Playwright browser installation](../../ReadMe.md) — `../../ReadMe.md`
- **[27]** [UI Performance stage result and first-start spread](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java`
- **[28]** [UI Performance isolated journey model](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java`
- **[29]** [UI Performance supported browser step actions](../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java) — `../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java`
- **[30]** [UI Performance sensitive-text sanitizer](../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java) — `../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java`
- **[31]** [Normal framework Cucumber UI runner](../../src/test/java/com/ptaf/runners/TestRunner.java) — `../../src/test/java/com/ptaf/runners/TestRunner.java`
- **[32]** [Normal framework browser and scenario Hooks](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`
- **[33]** [Normal framework Playwright browser factory](../../src/main/java/com/ptaf/utils/BrowserFactory.java) — `../../src/main/java/com/ptaf/utils/BrowserFactory.java`

<!-- Internal citation definitions used by the in-text [n] links. Keep these definitions so citations remain clickable. -->
[1]: ../../pom.xml "Maven build configuration and ui_performance profile"
[2]: ../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java "UiPerformanceEngine concurrent browser execution"
[3]: ../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java "UiPerformanceStartGate synchronized release"
[4]: ../../src/test/resources/ui_performance/testng-ui_performance.xml "Dedicated UI Performance TestNG suite"
[5]: ../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java "Dedicated UI Performance Cucumber runner"
[6]: ../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "UI Performance Cucumber step definitions"
[7]: ../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java "Isolated UI Performance YAML reader"
[8]: ../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java "UI Performance configuration accessors"
[9]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java "UI Performance execution plan"
[10]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java "UI Performance stage validation"
[11]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java "UI Performance browser and threshold profile"
[12]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java "Supported UI Performance test types"
[13]: ../../src/test/resources/ui_performance/config/ui_performance-config.yml "UI Performance eStore configuration"
[14]: ../../src/test/resources/ui_performance/features/estore_ui_performance.feature "Bundled eStore UI Performance journey"
[15]: ../../src/test/resources/ui_performance/locators/ui_performance-locators.yml "UI Performance locator repository"
[16]: ../../src/test/resources/ui_performance/data/users.csv "UI Performance user-data CSV"
[17]: ../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java "UI Performance CSV user-data reader"
[18]: ../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java "UI Performance locator repository reader"
[19]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java "UI Performance report directory manager"
[20]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java "UI Performance report writer"
[21]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java "Existing Performance reporter adapter for UI browser journeys"
[22]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java "Readable UI Performance duration formatter"
[23]: ../../src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java "UI Performance module contract tests"
[24]: ../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java "UI Performance start-gate contract test"
[25]: ../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java "Local concurrent browser integration contract"
[26]: ../../ReadMe.md "PTAF project README and Playwright browser installation"
[27]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java "UI Performance stage result and first-start spread"
[28]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java "UI Performance isolated journey model"
[29]: ../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java "UI Performance supported browser step actions"
[30]: ../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java "UI Performance sensitive-text sanitizer"
[31]: ../../src/test/java/com/ptaf/runners/TestRunner.java "Normal framework Cucumber UI runner"
[32]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Normal framework browser and scenario Hooks"
[33]: ../../src/main/java/com/ptaf/utils/BrowserFactory.java "Normal framework Playwright browser factory"
