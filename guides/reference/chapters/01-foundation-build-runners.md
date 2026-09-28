# Foundation, Maven, TestNG, Cucumber, and Runner Reference

## Purpose and scope

This chapter is the execution reference for the FNB-ETAF test framework as it exists in the repository. It explains the build model, the default TestNG suite, Cucumber runner entry points, shared configuration loading, reporting outputs, and safe ways to diagnose an unexpected run. It covers the runner classes in both `com.ptaf.runner` and `com.ptaf.runners`, plus the isolated UI-performance runner.

> **Execution model in one sentence:** Maven Surefire is explicitly configured to use the TestNG provider and the default TestNG XML suite; that suite invokes one TestNG/Cucumber runner, which discovers tagged Gherkin scenarios and their glue. [1] [3] [4]

This is a reference to **current source and resources**, not an assertion that every runner is part of the default Maven command. In particular, several JUnit/Cucumber runners coexist in the codebase but are not named by the default `testng.xml` suite. [1] [3]

## Build foundation

### Project and runtime dependencies

[`pom.xml`](../../../pom.xml) defines Maven coordinates `com.fnb_ptaf:fnb_ptaf:1.0-SNAPSHOT`, compiles with Java 21 source and target settings, and uses UTF-8 source encoding. It manages Cucumber through the Cucumber BOM and includes Cucumber Java, Cucumber-TestNG, Cucumber-JUnit, TestNG, JUnit 4, and JUnit 5 dependencies at the same time. Playwright, Appium, SQL drivers, SnakeYAML, JMeter DSL, and reporting libraries are also build dependencies. [1]

That dependency mix enables multiple test styles, but it does **not** mean Maven will discover all styles in one default run. The choice of Surefire provider and suite file determines the default launch path.

| Layer | Current responsibility | Operational implication |
|---|---|---|
| Maven Compiler Plugin | Compiles Java 21 source and passes a Java compiler export argument. [1] | Use a JDK compatible with source/target 21 for a normal build. |
| Surefire 3.2.5 | Runs the test phase and is given the `surefire-testng` **plugin dependency**. [1] | The project deliberately avoids accidental JUnit Platform selection when JUnit 5 is present. |
| TestNG | Receives the configured suite XML and schedules the selected TestNG tests. [1] [3] | The default suite is authoritative for `mvn test` and `mvn clean test`. |
| Cucumber | Reads feature files, tag expressions, glue, and plugins from the invoked runner annotation. [4] | A runner is a configuration holder; feature files and glue hold test behavior. |
| Shade Plugin | Runs at `package`, attaches a `shaded` artifact, and excludes signature/module metadata that can cause packaging issues. [1] | `mvn package`/`mvn install` performs the normal test phase before packaging unless Maven is instructed otherwise. |

### Surefire configuration and its consequences

The main Surefire configuration specifies all of the following. [1]

| Setting | Current value or behavior | Meaning for a run |
|---|---|---|
| Provider | `org.apache.maven.surefire:surefire-testng` plugin dependency | Forces the TestNG provider instead of relying on provider auto-detection. |
| Suite | `src/test/resources/testng.xml` | Default Maven test execution starts from this suite. |
| Parallelism | `parallel=methods`, `threadCount=4`, `useUnlimitedThreads=false` | The normal Surefire/TestNG path is configured for up to four method threads. |
| Failure policy | `testFailureIgnore=true` | A test failure can be recorded in reports while the Maven process completes successfully. Do not treat exit status alone as a quality verdict. |
| Cucumber system property | `cucumber.plugin=json:target/cucumber-reports/cucumber.json,junit:target/cucumber-reports/cucumber.xml,pretty` | Surefire supplies additional Cucumber report destinations and console output configuration. |
| Surefire reports | `target/surefire-reports`, file output enabled | Inspect these files for Maven/TestNG-level summaries. |
| JVM access | `--add-opens` for `java.lang`, reflection, util, and I/O | Required reflection access is supplied to the forked test JVM. |

The explicit TestNG provider is significant. The POM documents that, without it, JUnit 5 on the classpath can cause Surefire to select the JUnit Platform provider and ignore `suiteXmlFiles`, leading to zero TestNG tests. [1]

### Default TestNG suite

[`src/test/resources/testng.xml`](../../../src/test/resources/testng.xml) declares a suite named **PTAF Mobile Test Suite**, with method-level parallelism and four threads. Despite that suite name, its sole listed class is `com.ptaf.runner.TestRunner`. [3]

```xml
<suite name="PTAF Mobile Test Suite" parallel="methods" thread-count="4">
  <test name="Mobile Browser Tests">
    <classes>
      <class name="com.ptaf.runner.TestRunner"/>
    </classes>
  </test>
</suite>
```

The XML name is therefore not evidence that the default command executes the dedicated Appium runner. The source-selected class is the TestNG/Cucumber runner described next. This naming/source-selection mismatch is a documentation and operations caution, not a different execution path. [3] [4]

## Default execution flow

### Normal Maven/TestNG/Cucumber path

1. `mvn test`, `mvn clean test`, or a lifecycle command that reaches the test phase starts Surefire. Surefire uses the TestNG provider and loads `src/test/resources/testng.xml`. [1]
2. TestNG instantiates `com.ptaf.runner.TestRunner`, the only class named in that suite. [3] [4]
3. The runner extends `AbstractTestNGCucumberTests`, so Cucumber scenarios are exposed to TestNG. Its Cucumber options search `src/test/resources/features`, scan `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and select only scenarios matching `@eStore`. [4]
4. Cucumber finds matching feature/scenario tags and calls the matching step definitions. The included `com.ptaf.hooks` package provides lifecycle hooks; the default runner deliberately includes it. [4] [15]
5. For UI work, the normal hooks resolve common configuration, create a Playwright browser/context/page when the scenario is not classified as browserless, apply `runtimeWait` as a timeout, and close resources during teardown. [15] [16]
6. Cucumber and reporting plugins write their configured artifacts. Surefire also writes its own test reports. Maven may still return success if scenarios fail because the normal Surefire profile uses `testFailureIgnore=true`. [1] [4]

### Browserless and specialized lifecycle decisions

The common hooks intentionally skip Playwright browser creation for performance, API, Appium-mobile, and selected file/database scenarios. The decision is tag/path based; for example, API detection includes `@api` and API-like paths, database detection includes `@db`/`@database` and the `features/db` path, and native/Appium scenarios include mobile tags. [15]

Native/Appium lifecycle is separately implemented in `MobileHooks`. It starts an Appium driver only for mobile-tagged scenarios and closes it in its `@After` hook. Platform resolution is command-line `-Dmobile.platform`, then `@android`/`@ios`, then `mobile.default_platform` in YAML. [18]

Database cleanup is separately implemented in `DatabaseHooks`: an `@After("@db or @database or @sql")` hook closes the database connection after database scenarios. [17]

## Runner catalog and coexistence

All runner classes below are intentionally light: annotations define Cucumber discovery, tag selection, and reporting; test logic belongs in glue and hooks. Exact selection remains runner-specific.

| Source path | Framework | Features / tag expression | Glue | Reports configured by the runner | Normal invocation status |
|---|---|---|---|---|---|
| [`com/ptaf/runner/TestRunner.java`](../../../src/test/java/com/ptaf/runner/TestRunner.java) | **TestNG + Cucumber** | `src/test/resources/features`; `@eStore` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks` | pretty; HTML and JSON in `target/cucumber-reports`; rerun list; Extent; per-feature and soft-assert listeners | **Default Maven suite entry point**. [3] [4] |
| [`com/ptaf/runners/TestRunner.java`](../../../src/test/java/com/ptaf/runners/TestRunner.java) | **JUnit 4 + Cucumber** | root features; `@eStore` | `com/ptaf/stepdefinitions`, `com/ptaf/hooks` | pretty; `target/cucumber-reports.html`; Extent; timeline; custom listeners | Present but not listed in default TestNG XML. [3] [5] |
| [`ApiTestRunner.java`](../../../src/test/java/com/ptaf/runners/ApiTestRunner.java) | **JUnit 4 + Cucumber** | root features; `@api` | `com/ptaf/api/stepdefinitions`, `com/ptaf/hooks` | pretty; `target/api-cucumber-reports.html`; Extent; timeline; custom listeners | Present but not listed in default TestNG XML. [6] |
| [`DatabaseTestRunner.java`](../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java) | **JUnit 4 + Cucumber** | `features/db`; `@db or @database or @sql` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks` | pretty; `database-report.html`, `.json`, `.xml` under `target/cucumber-reports`; custom listeners | Present but not listed in default TestNG XML. [7] |
| [`MobileTestRunner.java`](../../../src/test/java/com/ptaf/runners/MobileTestRunner.java) | **JUnit 4 + Cucumber** | `features/mobile`; `@theapp_smoke` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks` | pretty; mobile HTML/JSON/JUnit XML under `target/cucumber-reports`; Extent; custom listeners | Present but not listed in default TestNG XML. [8] |
| [`PerformanceTestRunner.java`](../../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java) | **JUnit 4 + Cucumber** | `features/performance`; `@performance_testing` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks` | pretty; `target/performance-cucumber-report.html`; Extent; custom listeners | Present but not listed in default TestNG XML. [9] |
| [`Regression_Runner.java`](../../../src/test/java/com/ptaf/runners/Regression_Runner.java) | **JUnit 4 + Cucumber** | root features; `@regression` | `com/ptaf/stepdefinitions`, `com/ptaf/hooks` | pretty; Extent; timeline; custom listeners | Present but not listed in default TestNG XML. [10] |
| [`ParallelRun.java`](../../../src/test/java/com/ptaf/runners/ParallelRun.java) | **TestNG + Cucumber** | root features; `@secondPageTest` | `com/ptaf/stepdefinitions`, `com/ptaf/hooks` | pretty; `target/cucumber-reports.html`; Extent; timeline; custom listeners | Its overridden TestNG data provider has `parallel=true`, but it is not named by the default suite. [11] |
| [`UiPerformanceRunner.java`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) | **TestNG + Cucumber** | `ui_performance/features`; `@ui_performance and not @template` | **only** `com.ptaf.ui_performance.stepdefinitions` | pretty; HTML/JSON/JUnit XML under `test-output/ui_performance/cucumber` | Selected only by the `ui_performance` Maven profile suite. [2] [12] |

### JUnit, TestNG, and Cucumber: safe mental model

- **Cucumber** is the BDD layer: it parses `.feature` files, evaluates tags, resolves glue, and dispatches Gherkin steps.
- **TestNG** is the default Maven-hosted runner because Surefire is forced to the TestNG provider and points at `testng.xml`. The default and parallel runners use `AbstractTestNGCucumberTests`. [1] [4] [11]
- **JUnit 4** hosts the JUnit/Cucumber runners via `@RunWith(Cucumber.class)`. JUnit 4 and JUnit 5 dependencies are both present, but the current listed Cucumber runners use JUnit 4 annotations. [1] [5] [6]
- **JUnit 5** is a dependency in the POM; no JUnit 5 Cucumber runner is selected by the default TestNG suite. [1] [3]

> **Boundary:** Do not assume that a JUnit runner comment showing `mvn -Dtest=… test` overrides the configured TestNG suite. The current POM mandates the TestNG provider and a suite XML, whereas the JUnit runners are not in that XML. The source does not define a dedicated Maven profile or alternate suite for each JUnit runner. Use an IDE JUnit launch for those classes, or make an approved build/suite change and validate it in CI. [1] [3] [5] [7] [8]

### Source-visible runner discrepancies

Two current API-runner details deserve verification before relying on it:

1. `ApiTestRunner` asks Cucumber to scan `com/ptaf/api/stepdefinitions`, but the current API step source is [`src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java). The API runner's glue path therefore does not match the shown API step package location. [6] [13]
2. The current [`api_test.feature`](../../../src/test/resources/features/api_test.feature) has no `@api` tag, while the API runner selects only `@api`. On the current sources, that feature is not selected by the runner's tag expression. [6] [14]

These are source-visible discrepancies, not evidence that a different feature or external configuration supplies the missing tag/glue. Resolve them through an approved code/configuration change before treating the API runner as a reliable Maven suite.

## Configuration entry points and keys

### Shared framework YAML

`ConfigurationProperties` is the common access facade. It resolves a requested key first from `environments.<env>.<key>`, where JVM property `env` defaults to `QA`, then falls back to the unqualified key. [19]

Its underlying `YamlReader` scans classpath folders `elements`, `queries`, `api_requests`, `config`, and `performance`, recursively parses `.yml`/`.yaml` files, and recursively merges YAML maps. It skips a missing folder with an informational message; parse/read errors are printed with the folder/file and exception details. Because maps are merged, a later loaded scalar can replace an earlier scalar with the same key. The source does not codify a stable cross-filesystem ordering for the directory walk, so identical keys across multiple files should be avoided unless that precedence is deliberately validated. [20]

The main shared file is [`src/test/resources/config/config.yml`](../../../src/test/resources/config/config.yml). It currently supplies these key families:

| Key or key family | Consumed purpose |
|---|---|
| `browser`, `maximize_browser`, `headless`, `ignoreHTTPSErrors` | Normal Playwright browser choice and launch/context behavior. `headless` can be overridden by JVM property `-Dheadless=<true|false>`. [16] [21] |
| `runtimeWait` | Common-hook timeout in seconds. A missing, invalid, or non-positive value becomes a 30-second timeout in the hook. [15] [19] |
| `videoCapture` | Enables normal Playwright video setup; generated recordings are handled at browser shutdown. [16] [21] |
| `excelDocumentLocation`, `downloadDocument` | General file/data location values exposed by `ConfigurationProperties`. [19] |
| environment URL keys | Values are obtained through the shared YAML facade; keep environment-specific targets outside feature literals where possible. [19] [21] |
| `database.*` | SQL Server mode, server/database information, authentication, TLS/trust, timeout, fetch, application-name, and password environment-variable name. [21] [22] |
| `api_services.<service>.base_url`, `api_services.<service>.auth_token_env` | API target and **environment-variable name**, not a token value. [21] |
| `reporting.per_feature_*` | Per-feature HTML/PDF/Glass-PDF controls and directories. [19] [21] |
| `soft_assertions.enabled`, `soft_assertions.retry_seconds` | Continue-on-failure behavior and retry duration. [19] [21] |
| `zip.extraction_dir`, `zip.cleanup_after_scenario`, `zip.recursive_unzip` | ZIP extraction location and cleanup behavior. [19] [21] |

The normal browser factory resolves `headless` in this order: JVM `-Dheadless`, shared YAML `headless`, then `false`. It also reads `maximize_browser` (with a `maximizeBrowser` fallback), `ignoreHTTPSErrors`, and `videoCapture`. It accepts optional HTTP basic-auth system properties only when both values are supplied; do not place those values in feature files or checked-in YAML. [16]

### Mobile configuration

`MobileYamlReader` is separate from the shared reader. It loads only framework-owned YAML below `mobile/config` and `mobile/elements`, then merges it into its own store. [`MobileConfigurationProperties`](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java) exposes typed access under `mobile.*`, including `mobile.enabled`, `mobile.appium_server_url`, `mobile.default_platform`, wait settings, `mobile.evidence.*`, and `mobile.permissions.*`. [23] [24]

For native/mobile-browser execution, the practical precedence for platform is:

```text
-Dmobile.platform=<android|ios>
        ↓
@android or @ios on the scenario
        ↓
mobile.default_platform in mobile-config.yml
```

That precedence is implemented by `MobileHooks`; a scenario carrying both platform tags fails as ambiguous. [18] The current mobile feature demonstrates safe platform-neutral tagging with `@mobile`, `@cross_platform`, and `@theapp_smoke`; it does not need embedded endpoint or credential values. [25]

### API performance configuration

`PerformanceYamlReader` loads exactly `performance/config/performance-config.yml` from the test classpath when first referenced. A missing or empty file fails fast. `PerformanceConfigurationProperties` reads `performance.defaults.*`, `performance.assertions.*`, and `performance.reporting.*` from that dedicated resource. [26] [27]

### Isolated UI-performance configuration

The UI-performance module deliberately does **not** use the shared YAML reader. `UiPerformanceYamlReader` loads only `ui_performance/config/ui_performance-config.yml`; a controlled classpath-resource override is available through `-Dui.performance.config=<classpath-resource>`. An empty override falls back to the default path, and a missing or invalid resource fails with an explicit configuration exception. [28]

The UI-performance configuration facade reads these actual keys under `ui_performance.*`. [29] [30]

| Key family | Role |
|---|---|
| `enabled` | Gate for a live run. `requireEnabled()` rejects execution when false. |
| `target.protocol`, `target.host`, `target.port`, `target.base_path`, `target.routes.<name>` | Target and named relative routes. Protocol must be HTTP/HTTPS; host must be a bare host; routes must be relative. |
| `active_profile`, `profiles.<profile>.type`, `profiles.<profile>.stages` | Selects load/stress/spike/soak stages. Stage keys are `name`, `users`, `ramp_up_seconds`, `hold_seconds`, and `iterations_per_user`. |
| `browser.headless`, `browser.ignore_https_errors`, `browser.isolation`, `browser.user_agent`, action/navigation timeouts | Controls the dedicated engine's Chromium process, contexts, and time limits. Isolation must be `process`. |
| `execution.synchronized_start_timeout_ms`, `execution.between_iterations_ms` | Shared-start preparation limit and repeat-iteration pause. |
| `data.use_csv`, `data.users_csv`, `data.allow_user_reuse` | CSV user-data mode, classpath CSV location, and explicit reuse decision. |
| `evidence.capture_failure_screenshots`, `evidence.capture_console_errors` | Dedicated failure screenshot and browser-console evidence. |
| `thresholds.maximum_failure_rate_percent`, `thresholds.maximum_average_journey_duration_ms`, `thresholds.maximum_p95_journey_duration_ms` | Post-run quality gates. |
| `safety.max_virtual_users` | Upper bound validated before browsers launch. |
| `reporting.output_directory`, `reporting.*_enabled`, `reporting.existing_performance_reporter_enabled` | Dedicated report directory and optional report formats/integration. |

The UI-performance engine supplies browser-user concurrency itself. For every virtual user it creates a worker thread, Playwright instance, Chromium process, context, page, and assigned user-data row. Stages execute sequentially; users within a stage wait at a shared start gate and then execute concurrently. [31]

## Safe launch options

Run commands from the repository root. These examples intentionally use placeholders and do not include credentials, tokens, personal data, or private target URLs.

| Goal | Safe command | What it actually selects |
|---|---|---|
| Compile and run the default suite | `mvn clean test` | Surefire → TestNG provider → `src/test/resources/testng.xml` → `com.ptaf.runner.TestRunner` → `@eStore`. [1] [3] [4] |
| Run default suite headlessly | `mvn clean test -Dheadless=true` | Same default suite, while the normal browser factory uses the JVM headless override. [16] |
| Use an environment-specific shared YAML branch | `mvn clean test -Denv=<ENVIRONMENT_NAME>` | `ConfigurationProperties` tries `environments.<ENVIRONMENT_NAME>.<key>` before the plain key. [19] |
| Run isolated UI performance | `mvn clean test -Pui_performance` | Profile swaps the suite to `testng-ui_performance.xml`, disables Surefire parallelism, and makes failures fail Maven. [1] [2] [12] |
| Use an approved alternate UI-performance classpath resource | `mvn clean test -Pui_performance -Dui.performance.config=ui_performance/config/<approved-file>.yml` | The isolated UI-performance reader uses the named classpath resource. [28] |
| Select native mobile platform for a runner/IDE launch | `mvn test -Dmobile.platform=<android|ios>` | Supplies the mobile hook's highest-precedence platform override; it does not by itself add `MobileTestRunner` to the default TestNG suite. [18] [3] |

### Targeted runner launch caveat

Several runner comments provide examples such as `mvn -Dtest=<RunnerClass> test`. Those examples are useful as **intent**, but the POM currently configures Surefire to run TestNG with a suite XML, and that XML names only `com.ptaf.runner.TestRunner`. [1] [3] [5] [7] [8]

For a JUnit runner, use an IDE launch explicitly configured as a JUnit test, or add an approved dedicated suite/profile. Do not assume a `-Dtest` selector gives the JUnit runner precedence over the configured TestNG suite without confirming the emitted Surefire report in the target environment.

## Gherkin and runner selection examples

### Safe tag and scenario shape

The default TestNG runner selects `@eStore`; the database runner accepts `@db or @database or @sql`; the mobile runner selects `@theapp_smoke`; and the isolated UI-performance runner selects `@ui_performance and not @template`. [4] [7] [8] [12]

Use tags to state scope, and keep secrets, session data, private targets, and real test values out of feature files. A safe isolated UI-performance pattern is:

```gherkin
@ui_performance
Feature: Approved browser journey

  Scenario: Concurrent users exercise the approved route
    Given UI performance journey "<journey name>" uses configured target
    When UI performance journey navigates to configured route "<route-name>"
    Then UI performance journey verifies locator "<group>" "<ready-locator>" is visible
    And UI performance journey fills locator "<group>" "<field>" with data field "<csv-column>"
    And the configured UI performance users execute the journey
    And the UI performance run produces a standalone performance report
```

The dedicated step definitions obtain targets, named routes, locators, thresholds, and CSV fields from isolated resources. They reject a literal fill when its locator name implies a password, token, secret, or credential; use a separate CSV data-field step for sensitive input instead. [30] The checked-in UI-performance feature follows this configuration-first form. [32]

## Generated artifacts and result interpretation

### Normal TestNG/Cucumber runs

| Artifact family | Typical current location | How to use it |
|---|---|---|
| Surefire/TestNG summary | `target/surefire-reports/` | Start here for test count, Maven/TestNG-level failures, and console-captured output. [1] |
| Cucumber reports from Surefire property | `target/cucumber-reports/cucumber.json` and `cucumber.xml` | Machine-readable JSON/JUnit-style results requested by Surefire. [1] |
| Default TestNG runner reports | `target/cucumber-reports/cucumber-pretty/`, `CucumberTestReport.json`, `rerun.txt` | Read the HTML/JSON for scenario detail; use `rerun.txt` only after reviewing why the scenario failed. [4] |
| Runner-specific reports | Paths such as `target/api-cucumber-reports.html`, `target/cucumber-reports/database-report.*`, or `target/cucumber-reports/mobile-report.*` | These exist only when the corresponding runner actually launches. [6] [7] [8] |
| Timeline output | `test-output-thread/` | Produced by runners that configure the `timeline:` plugin. [5] [6] [10] [11] |
| Combined Extent output | a timestamped directory below `test-output/` | Spark HTML, Base64 HTML, PDF, and Excel reporters are enabled in `extent.properties`. [33] |
| Per-feature output | configured `test-output/per-feature-reports` and optional Glass-PDF directory | The listener creates Feature-name/timestamp reports only when `reporting.per_feature_reports_enabled` is true. [19] [34] |
| Normal Playwright videos | timestamped `test-output/captured-videos/` when enabled | The normal hook finalizes and renames recordings after browser shutdown; evidence-renaming errors are non-fatal. [15] [16] |

A report showing failures is more trustworthy than Maven exit status for the default suite, because `testFailureIgnore=true` is explicit in the normal Surefire configuration. [1]

Soft assertions require special interpretation. When enabled, a step may continue after a captured failure; the soft-assertion listener attempts to mark that Gherkin step failed in Extent reporting, while normal mode remains fail-fast. [19] [35]

### Isolated UI-performance runs

For each UI-performance run, the report manager creates a timestamped journey directory under `ui_performance.reporting.output_directory`, including `failed-screenshots/` and `browser-console-errors/`. [29] [36]

When the matching report switches are enabled, the report writer produces:

- `ui_performance-summary.html`
- `ui_performance-summary.pdf`
- `ui_performance-summary.json`
- `ui_performance-stages.csv`
- `ui_performance-iterations.csv`
- `ui_performance-step-timings.csv`
- `ui_performance-performance-summary.txt` [37]

If `existing_performance_reporter_enabled` is true, the adapter also writes a performance-reporter index, stage-level summaries, a JTL-compatible browser-sample file, and an Excel workbook. Each adapted stage represents **real-browser journey load**, not an HTTP endpoint or JMeter execution. [31] [38]

Interpret the dedicated report in this order:

1. Check **total/passed/failed iterations**, failure rate, average, P95/P99, throughput, and first-start spread in the summary.
2. Review stage rows before aggregating a conclusion: a load/stress/spike/soak profile can have materially different stage results. [37]
3. Review `ui_performance-iterations.csv` and sanitized diagnostics for individual virtual-user failures; inspect failure screenshots and console logs only when enabled. [31] [37]
4. Compare failure rate, average duration, and P95 with the configured `thresholds.*` values. The engine writes reports **before** enforcing thresholds and then throws an assertion when any value is strictly greater than its configured limit. [31]

> **Important discrepancy to read correctly:** the HTML writer labels its summary `PASS` when zero iterations failed, but threshold enforcement is performed afterwards and can fail the TestNG/Maven run solely because average or P95 duration exceeds a threshold. Treat the configured threshold comparison and final test result as the release gate, not that HTML status label alone. [31] [37]

### Version-control boundary

`.gitignore` excludes `target/`, `test-output/`, `test-output-thread/`, `test-output-performance-reports/`, `test-output/ui_performance/`, downloads created under `src/test/downloads/`, and the local UI-performance user-data CSV. Reports and evidence are therefore local/generated artifacts unless intentionally exported through the approved evidence process. [39]

## Safe troubleshooting playbook

| Symptom | Evidence-led checks | Safe action |
|---|---|---|
| **`mvn test` reports zero tests** | Confirm Surefire has the `surefire-testng` plugin dependency and the suite path is `src/test/resources/testng.xml`; confirm that suite names `com.ptaf.runner.TestRunner`. [1] [3] | Do not add arbitrary dependencies first. Restore/verify the TestNG provider and suite reference, then inspect `target/surefire-reports/`. |
| **The expected runner did not execute** | Compare the requested runner with the sole default suite class. JUnit runner classes are not listed in `testng.xml`. [3] [5] | Launch the JUnit runner through the IDE or request a dedicated approved Maven suite/profile; verify the generated report path rather than trusting a command comment. |
| **The default run executes an unexpected subset** | The default TestNG runner contains `tags = "@eStore"`; the JUnit runners have different tag expressions. [4] [5] [7] [8] | Inspect tags on both Feature and Scenario. Change runner tags only through reviewed source change; do not silently edit unrelated suites. |
| **A failed default run returns Maven success** | `testFailureIgnore=true` is set in the default Surefire configuration. [1] | Evaluate Surefire/Cucumber/Extent artifacts and CI report parsing; use the isolated UI-performance profile when its fail-on-threshold semantics are required. |
| **Undefined API steps or no API scenarios** | `ApiTestRunner` glue differs from the current `ApiSteps` path, and the current API feature lacks the runner's required `@api` tag. [6] [13] [14] | Treat this as a source discrepancy. Correct glue/tag selection in a reviewed change before using it as a delivery gate. |
| **Browser setup fails or a browser is unsupported** | Normal hooks accept `CHROME`, `FIREFOX`, `WEBKIT`, and `EDGE` after uppercasing the configured `browser` value. [15] | Check `browser`, `headless`, `maximize_browser`, and `ignoreHTTPSErrors` in the shared configuration. Use `-Dheadless=<true|false>` for a temporary headless override rather than committing a local preference. [16] [21] |
| **Normal UI waits behave unexpectedly** | `runtimeWait` is read as seconds; missing, malformed, or non-positive values default to 30 seconds in hooks. [15] | Check the effective environment-specific and plain YAML keys. Do not raise global waits as a first response to a locator/application defect. |
| **Mobile platform is wrong** | Mobile hooks prioritize `-Dmobile.platform`, then platform tags, then YAML default; both `@android` and `@ios` are invalid together. [18] | Use one platform signal at a time and confirm the mobile YAML is on the test classpath. |
| **Database setup fails before a query** | Only `sqlserver` is accepted by the shown handler. SQL Server authentication requires `database.username` and an environment variable named by `database.password_env_variable`; Windows mode relies on the host identity. [22] | Validate host access and non-secret configuration names. Set the secret in the runtime environment, never in YAML, reports, or a command pasted into shared logs. |
| **UI-performance run stops before opening browsers** | The engine checks `enabled`, validates target/profile/isolation, validates CSV placeholder mode, and requires enough data rows unless reuse is explicitly allowed. [29] [31] | Confirm target approval, CSV row capacity for the selected profile, classpath override spelling, and workstation capacity before raising `max_virtual_users`. |
| **UI-performance report exists but the profile failed** | The engine writes reports before threshold enforcement; failures can be error-rate, average-duration, or P95-duration breaches. [31] | Preserve the run directory, review stage and iteration outputs, then adjust either the application or an approved, justified threshold—not the report after the fact. |
| **Reports or local user data appear as untracked/ignored files** | The ignore rules intentionally omit generated reports, videos, downloads, and a local UI-performance CSV. [39] | Export selected evidence through the approved process and keep secrets/local test data outside version control. |

## Related chapters

Planned companion chapters (local chapter filenames) are: `02-ui-web-automation.md`, `03-api-automation.md`, `04-database-automation.md`, `05-mobile-native-automation.md`, `06-mobile-browser-automation.md`, `09-api-performance-testing.md`, `10-ui-performance-load-testing.md`, and `11-reporting-evidence-and-artifacts.md`. Those chapters should own detailed module-specific steps and evidence procedures; this chapter owns the shared build, runner, configuration-loading, and execution boundary.

## Source references

- [pom.xml — dependencies, Surefire, and UI-performance profile](../../../pom.xml)
- [Default TestNG suite](../../../src/test/resources/testng.xml)
- [Default TestNG/Cucumber runner](../../../src/test/java/com/ptaf/runner/TestRunner.java)
- [JUnit and TestNG runner classes](../../../src/test/java/com/ptaf/runners/TestRunner.java)
- [Shared configuration facade and YAML loader](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java)
- [Shared YAML configuration](../../../src/test/resources/config/config.yml)
- [Normal lifecycle hooks and browser factory](../../../src/main/java/com/ptaf/hooks/Hooks.java)
- [Isolated UI-performance suite and runner](../../../src/test/resources/ui_performance/testng-ui_performance.xml)
- [UI-performance configuration, engine, and reports](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java)
- [Repository ignore rules](../../../.gitignore)

## References

[1]: ../../../pom.xml "Maven project model: dependencies, Surefire, shade plugin, and profiles"
[2]: ../../../src/test/resources/ui_performance/testng-ui_performance.xml "Dedicated UI performance TestNG suite"
[3]: ../../../src/test/resources/testng.xml "Default TestNG suite"
[4]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[5]: ../../../src/test/java/com/ptaf/runners/TestRunner.java "General JUnit Cucumber runner"
[6]: ../../../src/test/java/com/ptaf/runners/ApiTestRunner.java "API JUnit Cucumber runner"
[7]: ../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java "Database JUnit Cucumber runner"
[8]: ../../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Mobile JUnit Cucumber runner"
[9]: ../../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java "API performance JUnit Cucumber runner"
[10]: ../../../src/test/java/com/ptaf/runners/Regression_Runner.java "Regression JUnit Cucumber runner"
[11]: ../../../src/test/java/com/ptaf/runners/ParallelRun.java "Parallel TestNG Cucumber runner"
[12]: ../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java "Dedicated UI performance TestNG Cucumber runner"
[13]: ../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java "Current API step-definition source"
[14]: ../../../src/test/resources/features/api_test.feature "Current API feature"
[15]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Common Cucumber lifecycle hooks"
[16]: ../../../src/main/java/com/ptaf/utils/BrowserFactory.java "Normal Playwright browser factory"
[17]: ../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java "Database cleanup hook"
[18]: ../../../src/main/java/com/ptaf/hooks/MobileHooks.java "Mobile Appium lifecycle and platform resolution"
[19]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Shared typed configuration access"
[20]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "Shared YAML classpath reader"
[21]: ../../../src/test/resources/config/config.yml "Shared framework configuration"
[22]: ../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java "Database connection configuration consumer"
[23]: ../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java "Mobile YAML reader"
[24]: ../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java "Mobile typed configuration"
[25]: ../../../src/test/resources/features/mobile/theapp_cross_platform_workflow.feature "Current cross-platform mobile feature"
[26]: ../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java "Performance YAML reader"
[27]: ../../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java "Performance typed configuration"
[28]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java "Isolated UI performance YAML reader"
[29]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java "UI performance typed configuration"
[30]: ../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "UI performance Cucumber glue"
[31]: ../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java "UI performance concurrent-browser engine"
[32]: ../../../src/test/resources/ui_performance/features/estore_ui_performance.feature "UI performance Gherkin journey"
[33]: ../../../src/test/resources/extent.properties "Extent report output configuration"
[34]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature report listener"
[35]: ../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java "Soft assertion report listener"
[36]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java "UI performance run-directory manager"
[37]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java "UI performance report writer"
[38]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java "Existing performance report adapter"
[39]: ../../../.gitignore "Ignored generated artifacts and local UI performance data"
