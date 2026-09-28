# Concurrent Real-Browser UI Performance Load Testing Reference

## Purpose and scope

This chapter documents the **isolated `ui_performance` implementation** used to exercise a configured browser journey with concurrent, real Playwright/Chromium users. It covers the dedicated eStore journey, workload profiles, data modes, synchronization, evidence, reporting, safety checks, and operational failure modes that are implemented in the current repository.

> **This is not the framework's normal UI test path.** The `ui_performance` Maven profile replaces the Surefire suite with its own TestNG suite; the dedicated Cucumber runner scans only `com.ptaf.ui_performance.stepdefinitions`. It deliberately does **not** load regular `Hooks`, ordinary UI glue, mobile hooks, normal listeners, or `BrowserFactory`. Browser-user concurrency is created by `UiPerformanceEngine`, not by TestNG or Cucumber scenario parallelism. [1][2][3]

The module is appropriate when the question is, for example, “How does the rendered eStore journey behave when several independent browsers perform it concurrently?” It measures browser journey and step timings, rather than HTTP-only endpoint timings. Its compatibility adapter can write established performance-report artifacts, but does not route the browser work through JMeter. [4]

### Quick facts

| Concern | Implemented behavior |
|---|---|
| Entry command | `mvn clean test -Pui_performance` selects the dedicated TestNG suite. [1] |
| Current feature | The tagged eStore feature builds a Consumer Deposit URL-generation journey from isolated target, locator, and data resources. [5] |
| Load model | Stages run **sequentially**; users within one stage run concurrently on a fixed thread pool sized to that stage. [3] |
| Browser model | Each virtual user gets a worker thread, `Playwright` instance, Chromium browser process, and data identity. Each iteration gets a new browser context and page. [3] |
| Synchronization | Browser startup completes before a common start gate releases prepared users; startup time is not part of an iteration's journey duration. [3][6] |
| Supported profile types | `load`, `stress`, `spike`, and `soak`. [7] |
| Current safety ceiling | The checked-in eStore configuration sets `ui_performance.safety.max_virtual_users` to **10**; this is a YAML-controlled ceiling, not a compiled hidden maximum. [8][9] |
| Primary outputs | Timestamped standalone HTML, PDF, CSV, JSON, and text reports, with optional existing-performance Excel/JTL-compatible outputs. [10][4] |

## Source layout and responsibilities

The isolated module is intentionally self-contained beneath `com.ptaf.ui_performance` and `src/test/resources/ui_performance`. It does use the existing `LocatorHandler` to interpret the repository's normal `TYPE_value` locator convention, but the locator repository itself remains separate from normal UI element YAML. [3][11]

### Production package classes

| Area | Classes | Responsibility |
|---|---|---|
| Configuration | `UiPerformanceYamlReader`, `UiPerformanceConfiguration`, `UiPerformanceLocatorRepository` | Load only the dedicated classpath YAML; validate and expose typed target/profile/browser/data/report settings; and resolve isolated grouped locator definitions. [12][9][11] |
| Execution | `UiPerformanceEngine`, `UiPerformanceStartGate` | Construct and run the concurrent workload, manage per-user Playwright/Chromium resources, collect results, write reports, enforce aggregate thresholds, and coordinate the first start. [3][6] |
| Data | `UiPerformanceUserDataReader` | Parse a simple UTF-8 CSV into in-memory user identities or create non-sensitive synthetic identities when CSV mode is off. [13] |
| Metrics | `UiPerformanceMetrics` | Compute total, pass/fail, error rate, minimum/average/median/percentiles/maximum, and completed-journey throughput. [14] |
| Workload models | `UiPerformanceExecutionPlan`, `UiPerformanceRunProfile`, `UiPerformanceStage`, `UiPerformanceTestType` | Represent and validate the selected named profile, supported type, ordered stages, runtime browser controls, safety ceiling, and thresholds. [15][16][31][7] |
| Journey and data models | `UiPerformanceJourney`, `UiPerformanceStep`, `UiPerformanceUser` | Hold the isolated browser DSL, one of six supported action types, and a user ID plus in-memory data fields without exposing values in `toString()`. [17][18][19] |
| Result models | `UiPerformanceIterationResult`, `UiPerformanceStageResult`, `UiPerformanceRunResult` | Retain sanitized iteration evidence, stage timing/metrics and first-start spread, and the complete run metadata/output directory. [20][21][22] |
| Reporting | `UiPerformanceReportManager`, `UiPerformanceReportWriter`, `UiPerformanceExistingReporterAdapter`, `UiPerformanceDurationFormatter`, `UiPerformanceSensitiveTextSanitizer` | Create a timestamped folder; write standalone reports; adapt browser stages to the existing performance reporting model; format readable durations; and redact selected credential/token patterns from text diagnostics. [10][4][23][24] |

### Dedicated test and resource assets

| Path | Role |
|---|---|
| [`src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) | Isolated TestNG/Cucumber entry point. It selects `@ui_performance and not @template`, publishes Cucumber HTML/JSON/JUnit output beneath `test-output/ui_performance/cucumber`, and leaves scenario parallelism disabled. [2] |
| [`src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`](../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java) | Small journey-building DSL; it creates the journey and triggers the engine, rather than operating a shared regular-UI page. [25] |
| [`src/test/resources/ui_performance/features/estore_ui_performance.feature`](../../../src/test/resources/ui_performance/features/estore_ui_performance.feature) | The packaged eStore Consumer Deposit journey. It contains business-flow steps, not a target URL, selector, account value, token, or credential. [5] |
| [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../../src/test/resources/ui_performance/config/ui_performance-config.yml) | Main real-browser configuration: target segmentation, profile stages, browser settings, data mode, evidence, thresholds, cap, and reporting switches. [8] |
| [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`](../../../src/test/resources/ui_performance/locators/ui_performance-locators.yml) | eStore-only grouped `TYPE_value` locators. Keeping this file separate prevents performance changes from changing normal functional UI locators. [26] |
| [`src/test/resources/ui_performance/data/users.csv`](../../../src/test/resources/ui_performance/data/users.csv) | Isolated user data resource. Do not place secrets or personal data in documentation or reports. [13] |
| [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../../src/test/resources/ui_performance/testng-ui_performance.xml) | The dedicated suite: runner plus offline module and start-gate contract tests, with TestNG `parallel="false"`. [27] |
| [`src/test/resources/ui_performance/config/ui_performance-browser-contract.yml`](../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml) and [`UiPerformanceConcurrentBrowserIntegrationTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java) | Local browser contract resources/test that verify two real browser users and a low first-start spread without using an external application. [28][29] |

## Isolation boundary: no regular Hooks or BrowserFactory

The regular framework has a separate [`Hooks`](../../../src/main/java/com/ptaf/hooks/Hooks.java) class and [`BrowserFactory`](../../../src/main/java/com/ptaf/utils/BrowserFactory.java). They are not part of this run. The profile comment explicitly says the dedicated profile does not alter or use normal TestNG parallelism, `BrowserFactory`, `Hooks`, or existing normal reports; the runner's glue scope likewise excludes `com.ptaf.hooks`. [1][2]

Consequences of that boundary are intentional:

- A regular UI scenario's browser/thread-local lifecycle cannot become the load model.
- The module creates and closes its own Playwright, browser, contexts, and pages inside the engine.
- Normal Extent/PDF listeners are not registered by the isolated runner; instead, the module writes its own standalone reports and can optionally adapt the result into the existing **performance** reporting format. [2][3][4]
- Cucumber describes one business journey once. It does not create one scenario per virtual user. The internal engine owns the concurrent user count. [2][3]

## Configuration contract

### Where configuration comes from

`UiPerformanceYamlReader` loads only `ui_performance/config/ui_performance-config.yml` from the classpath. The **only implemented JVM override** is `-Dui.performance.config=<classpath-resource>`; it changes the YAML resource location, not individual configuration keys. The override must still resolve from the classpath, and a missing or malformed resource fails early. There is no source support for arbitrary `-Dui_performance.*` key overrides. [12]

The feature is intentionally configuration-first: the given step builds a target from separate protocol, bare host, port, and optional base path; the navigation step names a relative route. This keeps environment URLs out of the feature. [9][25]

> **Safe operation:** Use an approved non-production target and prepared test data. Do not copy the configured endpoint, credentials, tokens, or user values into a feature, command history, commit message, or this chapter. `UiPerformanceJourney` rejects obvious sample-host placeholders, but approval remains an operator responsibility. [17][9]

### Actual configuration keys

The following are the keys read by the implementation. Values below describe the keys and current workload shape without reproducing the configured endpoint or data values.

| Configuration area | Keys | Meaning and source-enforced rules |
|---|---|---|
| Enable/profile | `ui_performance.enabled`; `active_profile` | `enabled` must be true for a live engine run; otherwise the engine stops before browser launch. `active_profile` selects a named entry below `profiles`. The checked-in eStore YAML currently enables the module and selects `load`. [9][8] |
| Target | `target.protocol`, `target.host`, `target.port`, `target.base_path`, `target.routes.<route_name>` | Protocol must be `http` or `https`; host must be a bare hostname (no scheme, slash, or port); port must be 1–65535; base path and configured routes must start with `/` when non-empty. Routes must be relative, not absolute URLs. [9] |
| Profile | `profiles.<profile>.type`, `stages[].name`, `users`, `ramp_up_seconds`, `hold_seconds`, `iterations_per_user` | Type is strictly load/stress/spike/soak. A stage needs at least one user, non-negative timing/count fields, and no more than `max_virtual_users`. `iterations_per_user > 0` is iteration-based; `0` is duration-based and requires `hold_seconds >= 1`. [15][16][7] |
| Browser | `browser.headless`, `ignore_https_errors`, `isolation`, `user_agent`, `action_timeout_ms`, `navigation_timeout_ms` | Headless is a YAML switch. HTTPS errors are controlled per new context. `isolation` must be exactly `process`; empty user-agent and non-positive action/navigation timeouts fail validation. [9][31] |
| Execution | `execution.synchronized_start_timeout_ms`, `between_iterations_ms` | The first is the maximum wait for all browser preparations before common release. The second is a pause only between repeated iterations and can be zero. [9][6] |
| Test data | `data.use_csv`, `users_csv`, `allow_user_reuse` | Determines CSV versus synthetic identities, the classpath CSV, and whether a stage can cycle through rows. [9][13] |
| Evidence | `evidence.capture_failure_screenshots`, `capture_console_errors` | Toggles full-page screenshots on failure and capture of browser messages whose type is `error`. [9][3] |
| Quality gates | `thresholds.maximum_failure_rate_percent`, `maximum_average_journey_duration_ms`, `maximum_p95_journey_duration_ms` | Valid failure-rate range is 0–100; average and P95 thresholds must be at least 1 ms. A value is a breach only when the observed metric is **greater than** its threshold. [9][16][3] |
| Capacity | `safety.max_virtual_users` | Tester-controlled cap used to validate every stage. The current eStore YAML cap is 10; the code applies no additional hidden cap. [8][9] |
| Reports | `reporting.output_directory`, `existing_performance_reporter_enabled`, `html_enabled`, `pdf_enabled`, `csv_enabled`, `json_enabled` | Sets the root directory, compatibility reporting switch, and standalone optional formats. The technical text summary is written regardless of the four format switches. [9][10] |

### Load, stress, spike, and soak profiles

The current YAML supplies the following schedules. These are **real browser counts**, not simulated HTTP threads. [8][3]

| Profile | Stages as currently configured | Operational interpretation |
|---|---|---|
| `load` | `normal-load`: 2 users, zero ramp, zero hold, 1 iteration per user | Two prepared browser users receive the common release and each runs once. [8] |
| `stress` | 2 users/0 s ramp/60 s hold; then 5 users/5 s/60 s; then 10 users/5 s/60 s; duration-based (`iterations_per_user: 0`) | Escalates concurrent browser pressure in three sequential stages. [8] |
| `spike` | 2-user baseline for 30 s; immediate 10-user spike for 60 s; 2-user recovery for 30 s; duration-based | Compares baseline, zero-ramp spike, and recovery in one sequential run. [8] |
| `soak` | 5 users, 10 s ramp, 900 s hold; duration-based | Sustained real-browser activity intended to expose stability issues over time. [8] |

Structural checks are also type-specific: stress and spike require at least two stages; soak requires at least one duration-based stage. [15]

### Headless execution and browser identity

The engine calls `playwright.chromium().launch(...)` and applies `headless` only to the launch options. It creates a context with `ignoreHTTPSerrors` and the configured `user_agent`, then a page for each iteration. [3]

The eStore resource currently defaults to headless mode, process isolation, a non-empty approved desktop-like user-agent, and elevated action/navigation timeouts. Its comments identify a response problem with the default `HeadlessChrome` identity. [8]

Two important implementation details prevent misleading interpretation:

1. **The browser binary is Chromium, not a selected local Chrome channel.** Although the user-agent is a browser identity sent to the application, the code does not set a Chromium channel or executable path. Treat the user-agent as an HTTP/browser identity control, not proof that a particular installed Chrome binary is being used. [3][8]
2. **`browser.isolation` is a validated contract, not a runtime selector.** `UiPerformanceRunProfile` rejects anything other than `process`, while the engine always creates one Playwright/Chromium browser per worker. [16][3]

Set `browser.headless: false` only for low-user diagnosis: every requested browser window becomes visible, which can change workstation resource pressure and timing. [8][9]

## Data modes and virtual-user assignment

### CSV-backed mode

With `data.use_csv: true`, the engine reads `data.users_csv` as a UTF-8 classpath CSV. The first header must be `user_id` (case-insensitive) and at least one additional nonblank data column is required. Empty lines and lines beginning with `#` are skipped; cells are trimmed. The parser intentionally does **not** support quoted comma-containing values. [13]

A feature's data-field step creates an in-memory `${column_name}` template. At execution time it is replaced from the assigned user's CSV row. User values are not placed in iteration results or standard report fields. [25][3][20]

Before any browser starts, the engine verifies that enough usable rows exist for the largest selected stage. When reuse is false (the current eStore setting), a profile needing *N* simultaneous users needs at least *N* distinct rows. When approved reuse is true, user rows are assigned cyclically using the row index modulo CSV size. [3][8]

### No-data mode

With `data.use_csv: false`, the engine creates as many synthetic identities as the selected profile's maximum virtual-user count, named with non-sensitive sequential IDs such as `virtual_user_001`. Each has an empty value map. [13][3]

A no-data journey **cannot** contain a `${...}` data placeholder. The engine validates this before browser launch and fails with a directed message if a CSV-style data field remains in the journey. This makes no-data mode suitable only for public/read-only or otherwise value-free flows. [3]

## eStore journey DSL and assets

`UiPerformanceSteps` exposes an intentionally small DSL. It builds a `UiPerformanceJourney`; it does not delegate to normal UI step definitions. Supported actions are `NAVIGATE`, `FILL`, `SELECT_OPTION`, `CLICK`, `CLICK_AND_SWITCH_TO_POPUP`, and `VERIFY_VISIBLE`. [25][18]

The packaged feature performs this sequence: navigate to the configured test-harness route; verify readiness; fill e-mail and phone fields from data; select product group and product name from data; click the URL-generation action; then verify generated/open URL controls. Locator group/key pairs resolve from the isolated locator YAML. [5][26]

A safe, configuration-first Gherkin pattern is:

```gherkin
@ui_performance
Scenario: Concurrent approved journey
  Given UI performance journey "<journey name>" uses configured target
  When UI performance journey navigates to configured route "<route name>"
  Then UI performance journey verifies locator "<group>" "<ready control>" is visible
  And UI performance journey fills locator "<group>" "<input>" with data field "<csv_column>"
  And UI performance journey clicks locator "<group>" "<action>"
  Then UI performance journey verifies locator "<group>" "<expected control>" is visible
  And the configured UI performance users execute the journey
  And the UI performance run produces a standalone performance report
```

Use data-field steps—not literal feature text—for sensitive values. The literal-fill step explicitly rejects locator keys containing `password`, `token`, `secret`, or `credential`; this is a key-name guard, so teams should still keep all secrets out of feature files as a general rule. [25]

Locator values are read from `ui_performance-locators.yml` and use `TYPE_value`. The engine separates at the first underscore and delegates type resolution to `LocatorHandler`; malformed definitions or missing group/key entries fail explicitly. Do not assume the UI-performance repository will pick up a normal `src/test/resources/elements` locator change. [11][3][26]

## Exact execution flow

1. **Maven selects the isolated suite.** `-Pui_performance` overrides Surefire's suite XML, turns off Surefire parallelism, uses one runner thread, and sets `testFailureIgnore` to false. [1]
2. **Cucumber builds one journey.** The runner scans only the isolated feature directory and glue package. The feature asks for the configured target/route and isolated locators; the final step calls `new UiPerformanceEngine().execute(...)`. [2][5][25]
3. **Preflight validation happens before browser launch.** The engine requires `enabled`, validates the journey and run profile, validates no-data mode, reads CSV or creates synthetic users, and checks row capacity/reuse policy. [3][9][16]
4. **A dedicated report folder is created.** The output path is `<output_directory>/<sanitized journey>_<profile>_<yyyyMMdd_HHmmss>`, with `failed-screenshots` and `browser-console-errors` subdirectories. [10][3]
5. **Stages execute in YAML order.** The engine finishes a stage and gathers all its futures before it starts the next stage. Each stage has a fixed-size executor equal to `stage.users`. [3]
6. **Each virtual user prepares an independent browser stack.** A task creates `Playwright`, launches Chromium, marks itself ready, and waits on the shared gate. Browser startup is deliberately outside the measured journey timing. [3][6]
7. **The common gate releases prepared users.** The coordinator waits up to `synchronized_start_timeout_ms` for every requested user to either prepare or finish a failed browser-start attempt, establishes the stage start/deadline, and releases the gate once. A zero-ramp stage begins first actions together; a positive ramp delays each worker based on its index across the configured ramp interval. [3][6]
8. **Each iteration gets a fresh context/page.** The browser persists for the virtual user's stage, but `browser.newContext()` and `context.newPage()` occur for every iteration. Defaults for action/navigation timeouts, user agent, TLS behavior, console listener, and steps apply to that context/page. The context closes in `finally`; the browser and Playwright close after the task. [3]
9. **Steps execute and are timed.** Navigation waits only for `DOMContentLoaded`, and HTTP status 400 or above becomes a failed journey. Select options use visible labels; popup clicks wait for and continue in the popup page; visibility waits for the visible selector state. [3]
10. **Results become reports, then gates are enforced.** The engine writes selected standalone reports, optionally writes existing-performance outputs, and finally throws an assertion if aggregate failure rate, average journey duration, or P95 duration exceeds its configured threshold. Thus reports remain available even when the Maven run fails a quality gate. [3][10][4]

### Iteration and duration semantics

For an iteration-based stage, each user executes exactly `iterations_per_user` journeys with an optional `between_iterations_ms` pause between them. For a duration-based stage (`iterations_per_user: 0`), each user repeats until the stage deadline or interruption. [3][16]

**Source-visible timing nuance:** the deadline is computed from common stage release as `ramp_up_seconds + hold_seconds`. A ramped user first sleeps its indexed ramp delay and then works until that common deadline. Therefore, early users can receive part of the ramp window plus hold time, while later users receive approximately the remaining hold window; the implementation is not a separate, equal per-user hold timer after every user completes its ramp. This is important when comparing stage sample counts. [3][16]

If browser startup or task runtime fails, the engine returns failed result(s) rather than silently dropping the virtual user. An iteration-based task records at least one failure and can record enough failures to cover remaining requested iterations; a duration-based task records one failure. Failure categories are `TIMEOUT`, `NETWORK`, `LOCATOR_OR_UI_STATE`, or `BROWSER_OR_APPLICATION` based on exception message matching. [3]

## Start gate and concurrency interpretation

`UiPerformanceStartGate` has a ready latch sized to the exact stage user count and a one-time release latch. Prepared users cannot take their first measured action until all have prepared or the coordinator times out. Its local contract test verifies that two users remain blocked before release and then are released together. [6][30]

The first-start spread reported per stage is the latest minus earliest nonzero timestamp among **iteration 1** results. It is useful corroboration of a zero-ramp start, not a guarantee of identical scheduling: operating-system scheduling, page creation, DNS, and application response still introduce variation after release. [21][10]

The local browser integration contract uses a temporary loopback web server and asserts two successful real browser journeys with first-start spread no greater than one second. It demonstrates the intended concurrency boundary without establishing a production performance baseline. [29][28]

## Metrics, thresholds, and readable durations

### What is calculated

| Metric | Calculation/meaning |
|---|---|
| Total/passed/failed journeys | Every collected iteration result, including failures. [14] |
| Failure rate | `failed / total * 100`. [14] |
| Journey distribution | Minimum, rounded arithmetic average, nearest-rank median/P90/P95/P99, and maximum over collected journey durations. [14] |
| Throughput | `total iterations / wall-clock run or stage duration`; displayed per second and per minute. It is completed-sample throughput, not an HTTP request-rate metric. [14] |
| Step/transaction summary | For each step name with recorded timings, min/average/median/P90/P95/P99/max; failed journeys may contribute timings for steps completed before failure. [10][3] |
| First-start spread | Earliest-to-latest first iteration start timestamp within a stage. [21] |

Percentiles use **nearest-rank** selection. The JSON and CSV retain raw millisecond fields; human-facing reports format under one second as `ms`, then seconds with three fractional digits, and longer values with minute/hour components. [14][23][10]

### Threshold behavior

The engine evaluates three aggregate run metrics after all stages finish:

- failure rate greater than `maximum_failure_rate_percent`;
- average journey duration greater than `maximum_average_journey_duration_ms`; or
- P95 journey duration greater than `maximum_p95_journey_duration_ms`. [3][9]

Equality is not a breach because the comparison is `>`. Since report generation occurs first, a threshold failure should be investigated from the generated folder named in the assertion message. [3]

> **Reporting nuance:** the engine's failing assertion uses **aggregate run** metrics. The existing-performance adapter also evaluates each **stage** independently when deciding its adapted scenario pass/fail/risk status. A workbook can therefore show a stage concern even where the aggregate threshold check does not throw, or vice versa. [3][4]

A second presentation nuance is that the standalone HTML's headline status is based on whether any iteration failed. A run with no iteration failures but an average/P95 timing breach can show `PASS` in that headline while Maven still fails after threshold enforcement. Check the configured threshold comparison and metrics rather than relying only on that headline. [10][3]

## Generated artifacts and evidence

The dedicated runner emits Cucumber outputs outside the run folder at:

```text
test-output/ui_performance/cucumber/cucumber.html
test-output/ui_performance/cucumber/cucumber.json
test-output/ui_performance/cucumber/cucumber.xml
```

For each engine execution, a timestamped run directory is created under `reporting.output_directory` (the current eStore configuration uses `test-output/ui_performance`). The following outputs are produced according to their switches. [2][8][10]

| Artifact | Written when | Contents |
|---|---|---|
| `ui_performance-summary.html` | `html_enabled` | Dashboard with profile/stage metrics, readable durations, transaction timing summary, and sanitized iteration diagnostics. [10] |
| `ui_performance-summary.pdf` | `pdf_enabled` | Summary/stage page plus a browser transaction timing page; line output is intentionally bounded. [10] |
| `ui_performance-stages.csv` | `csv_enabled` | Per-stage load shape, raw and readable distribution values, throughput, and first-start spread. [10] |
| `ui_performance-iterations.csv` | `csv_enabled` | Per-iteration stage/user/iteration/status/timestamps, raw/readable journey and step timings, sanitized diagnostic, and console error count. [10] |
| `ui_performance-step-timings.csv` | `csv_enabled` | Aggregated browser transaction timing distributions by step. [10] |
| `ui_performance-summary.json` | `json_enabled` | Safe report map with run metadata, raw/readable metrics, stages, transaction summary, and iteration records. [10] |
| `ui_performance-performance-summary.txt` | Always | Technical readable run/stage/transaction summary. [10] |
| `failed-screenshots/` | Folder always exists; files only when screenshots are enabled and an active page can be captured | Full-page PNG evidence named by sanitized stage, virtual-user position/ID, and iteration. [10][3] |
| `browser-console-errors/` | Folder always exists; files only when `error` console messages were captured | Per-iteration sanitized browser console error logs. Evidence-write errors are deliberately ignored so cleanup/results proceed. [10][3] |
| `performance-run-report.xlsx` and `performance-reporter-index.txt` | `existing_performance_reporter_enabled` | Existing performance-report workbook and an index connecting its stage scenarios to the browser dashboard. [4] |
| `performance-reporter/<stage>/summary.txt`, `readable-summary.txt`, `ui-browser-results.jtl` | Compatibility reporting enabled | One adapted performance scenario and JTL-compatible CSV per real-browser stage. It represents browser journey samples, not HTTP endpoint samples. [4] |

### Handling report data safely

Result models avoid form values, cookies, and session data; standard report metadata uses target host rather than a full target URL. Text diagnostics are pattern-redacted for common password/token/authorization/query-secret forms and truncated to 500 characters. [20][24][10]

This is **not** a universal evidence-redaction guarantee. `UiPerformanceSensitiveTextSanitizer` has no generic URL-redaction pattern, and a full-page failure screenshot can contain whatever the browser displayed. Store screenshots, console logs, CSVs, JSON, and workbooks as controlled test artifacts; use non-production test data and review access permissions before sharing. [24][3]

## Guardrails and operating procedure

### Before a live run

1. Confirm target-environment approval, expected workload profile, expected test data effects, and workstation capacity. The `requireEnabled()` message names all four as prerequisites. [9]
2. Confirm `active_profile` and every stage count against the actual capacity of the machine and target. The cap is a safeguard, not a capacity recommendation. The checked-in YAML's 10-user cap is described as a recommended first developer-workstation maximum. [8][9]
3. Use distinct prepared data rows for the largest stage unless reuse is explicitly approved for a read-only journey. [3][8]
4. Confirm that data-free mode has no placeholder steps, or keep CSV mode enabled. [3]
5. Review `headless`, user agent, TLS-error setting, and timeouts for the approved environment. Do not disable TLS validation merely to conceal a certificate issue. [8][3]
6. Verify the target is segmented correctly (`protocol`/bare `host`/`port`/`base_path` and relative named route), rather than embedding an endpoint in Gherkin. [9][25]

### Run command

From the repository root, invoke the dedicated profile:

```bash
mvn clean test -Pui_performance
```

This is the command stated by the runner/configuration and selects only the dedicated suite. It does not run normal UI scenarios as a substitute for a UI-performance run. [1][2][8]

For controlled CI, the source also supports this **schematic** classpath-resource selection form; substitute only an approved YAML resource already packaged on the test classpath:

```bash
mvn clean test -Pui_performance \
  -Dui.performance.config=<approved-classpath-yaml-resource>
```

Do not pass filesystem paths, credentials, or secrets as this property. The loader resolves it through the class loader and fails if it is not a valid resource. [12]

### After a run

1. Locate the timestamped run folder and inspect the HTML/text summary, per-stage CSV, and iteration CSV before changing load. [10]
2. Compare stage distribution, failure categories, first-start spread, and throughput; do not infer an API rate from browser throughput. [14][10]
3. If a quality gate failed, retain the report folder. It was written before threshold enforcement. [3]
4. Treat any failures or time regressions as an investigation item before raising `max_virtual_users` or choosing a higher profile. [4][8]

## Troubleshooting

| Symptom | Likely source-backed cause | Action |
|---|---|---|
| Normal tests ran, or the performance feature did not run | The dedicated profile/suite was not selected, or tag selection excludes the scenario. | Run `mvn clean test -Pui_performance`; verify the runner's feature path and `@ui_performance and not @template` tag expression. [1][2] |
| “UI performance execution is disabled” | `ui_performance.enabled` is false in the selected resource. | Enable only after target/data/profile/capacity approval. The checked-in eStore resource is currently enabled, but a CI override can select another YAML. [9][8][12] |
| “Config was not found on the classpath” | `ui.performance.config` points to an absent/invalid classpath resource. | Correct the resource name and ensure it is packaged under test resources; do not provide an arbitrary local path. [12] |
| Target validation error | Scheme/host/port/base path/route violates segmented target requirements. | Use a bare host, `http` or `https`, port 1–65535, and relative `/...` route names. [9] |
| Profile/stage validation failure | Unknown type; no stages; a negative value; a duration stage without hold; stress/spike with fewer than two stages; soak without duration mode; or stage users above cap. | Correct the selected profile YAML rather than changing engine code. [15][16][7] |
| “Needs N unique users” or CSV row/header error | Insufficient rows for the largest stage, reuse disabled, malformed simple CSV, missing resource, or invalid first header. | Add approved rows, keep one unique row per concurrent user where required, ensure first header is `user_id`, and avoid quoted comma values. [3][13] |
| Placeholder error with CSV disabled | A journey still contains `${column}` while `data.use_csv: false`. | Remove data-field steps for a genuinely data-free journey or re-enable CSV mode. [3] |
| Simultaneous start timeout | One or more browser workers could not complete preparation before `synchronized_start_timeout_ms`; high local load is a common operational cause. | Reduce stage users, increase only the synchronized-start timeout after diagnosing machine capacity, and check browser availability. [6][9] |
| HTTP 403 or application behavior differs in headless runs | The target may react to the default headless browser identity; the eStore YAML explicitly provides a desktop-like user agent for this reason. | Reconfirm the approved user agent and target policy; remember the engine still launches Playwright Chromium, not a selected Chrome executable. [8][3] |
| Navigation/action timeout | Page readiness, popup startup, selector availability, or network response exceeded configured maxima. | Check the failed iteration's category/console log/screenshot, then adjust the relevant timeout only with evidence. Navigation waits for DOM content loaded; visible assertions use the action timeout. [3][9] |
| Locator error or unexpected UI state | Missing isolated locator group/key, malformed `TYPE_value`, or stale UI-performance locator. | Update only `ui_performance-locators.yml` after verifying the target DOM; do not assume regular UI locator files are shared. [11][26][3] |
| Reports exist but Maven fails | Aggregate quality threshold breach occurs after report writing. | Read the metrics and threshold values in the run folder; a report folder is expected after this failure. [3][10] |
| HTML says PASS but the build failed for slow performance | HTML headline is based on failed iteration count; threshold enforcement separately evaluates aggregate average/P95/failure rate. | Inspect numeric metrics and threshold results, not only the HTML headline. [10][3] |
| Workbook flags one stage unexpectedly | Compatibility adapter evaluates thresholds per stage while engine pass/fail is aggregate. | Compare per-stage and aggregate metrics; document which gate governs the pipeline outcome. [4][3] |
| Screenshot or log contains more information than expected | Text redaction is pattern-based and screenshots capture full rendered pages. | Restrict artifact access, use synthetic/non-production data, and review evidence before circulation. [24][3] |

## Boundaries and limitations

- This module is a **real-browser** workload. Resource usage grows with actual Chromium processes, contexts, pages, and target rendering work; it is not a replacement for high-volume protocol-level load testing. [3][16]
- It supports only the six action types in `UiPerformanceStep`; it is not a generic replacement for the normal UI page-object/step ecosystem. [18][25]
- It executes one configured journey per engine invocation and one active profile at a time. Stages are sequential. [3][15]
- The CSV reader is deliberately simple and classpath-only. It does not parse quoted commas or fetch external user data. [13]
- The existing-performance JTL output is **JTL-compatible CSV of browser journey samples**. It should not be interpreted as a JMeter-generated network trace or one record per HTTP resource. [4]
- Browser startup is intentionally outside measured journey duration, while the duration-based stage window includes the configured ramp plus hold as described above. Interpret comparisons with that timing model in mind. [3]

## Related chapters

The chapters directory is empty in this checkout, so no local Markdown targets currently exist to link without creating broken links. When the planned reference set is added, read these local-relative chapter filenames alongside this one:

- `04-configuration-and-environments.md` — configuration ownership, target selection, and test data controls.
- `07-ui-automation-and-playwright.md` — regular functional UI lifecycle, Hooks, BrowserFactory, and locator conventions.
- `10-performance-testing-and-reporting.md` — protocol/API performance and established report model boundaries.
- `12-test-evidence-and-reporting.md` — report retention, sharing controls, and evidence interpretation.

## Source references

- [Maven UI-performance profile and isolated-suite replacement][1]
- [Dedicated runner and isolated Cucumber glue][2]
- [Concurrent real-browser engine][3]
- [Existing performance reporter adapter][4]
- [eStore UI-performance feature][5]
- [Synchronized start gate][6]
- [Supported test-type enum][7]
- [Main UI-performance YAML configuration][8]
- [Typed configuration accessors][9]
- [Standalone report writer][10]
- [UI-performance locator repository][11]
- [Dedicated YAML reader][12]
- [User CSV/synthetic-data reader][13]
- [Metrics calculation][14]
- [Execution-plan validation][15]
- [Stage/run-profile validation][16]
- [Journey validation][17]
- [Journey-step model][18]
- [Virtual-user model][19]
- [Iteration-result model][20]
- [Stage-result model][21]
- [Run-result model][22]
- [Readable-duration formatter][23]
- [Sensitive-text sanitizer][24]
- [UI-performance step definitions][25]
- [eStore UI-performance locators][26]
- [Dedicated TestNG suite][27]
- [Browser-contract YAML][28]
- [Concurrent-browser integration contract][29]
- [Start-gate contract test][30]
- [Run-profile validation][31]

## References

[1]: ../../../pom.xml "Maven project: UI-performance Surefire profile"
[2]: ../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java "Dedicated UI-performance Cucumber/TestNG runner"
[3]: ../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java "Concurrent real-browser UI-performance engine"
[4]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java "Existing performance reporter adapter"
[5]: ../../../src/test/resources/ui_performance/features/estore_ui_performance.feature "eStore UI-performance journey"
[6]: ../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java "UI-performance synchronized start gate"
[7]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java "Supported UI-performance test types"
[8]: ../../../src/test/resources/ui_performance/config/ui_performance-config.yml "UI-performance eStore configuration"
[9]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java "Typed UI-performance configuration"
[10]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java "Standalone UI-performance report writer"
[11]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java "Dedicated UI-performance locator repository"
[12]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java "Dedicated UI-performance YAML reader"
[13]: ../../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java "UI-performance CSV and synthetic-user reader"
[14]: ../../../src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java "UI-performance metrics calculations"
[15]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java "UI-performance execution plan"
[16]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java "UI-performance stage" 
[17]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java "UI-performance journey"
[18]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java "UI-performance step model"
[19]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java "UI-performance virtual-user model"
[20]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java "UI-performance iteration result"
[21]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java "UI-performance stage result"
[22]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java "UI-performance run result"
[23]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java "Readable duration formatter"
[24]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java "Sensitive text sanitizer"
[25]: ../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "UI-performance Cucumber step definitions"
[26]: ../../../src/test/resources/ui_performance/locators/ui_performance-locators.yml "eStore UI-performance locator resource"
[27]: ../../../src/test/resources/ui_performance/testng-ui_performance.xml "Dedicated UI-performance TestNG suite"
[28]: ../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml "Local UI-performance browser contract configuration"
[29]: ../../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java "Concurrent real-browser integration contract"
[30]: ../../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java "Synchronized start-gate contract test"
[31]: ../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java "UI-performance run-profile validation"
