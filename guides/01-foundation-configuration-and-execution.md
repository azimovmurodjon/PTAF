# FNB-ETAF Foundation: Configuration and Execution Guide

## Purpose and scope

This guide explains how the current **FNB-ETAF** Maven test project is assembled, how Cucumber and TestNG select scenarios, where configuration and test resources live, and how to extend the repository without changing the execution path of existing modules. It is deliberately limited to project foundation and execution concerns. It does not prescribe application-specific data, credentials, endpoints, or client settings.

The project compiles for **Java 21** and uses Maven Surefire with a TestNG provider. Normal Maven execution starts the TestNG suite at `src/test/resources/testng.xml`; that suite invokes the Cucumber/TestNG class `com.ptaf.runner.TestRunner`. A separate Maven profile, `ui_performance`, replaces only the Surefire suite file for the isolated real-browser UI-performance module.[1]

> **Terminology.** A *runner* is a Java class that provides Cucumber options. A *suite* is a TestNG XML file that identifies which test class TestNG starts. A *profile* is a Maven configuration selected with `-P`.

## Architecture and source locations

The following table is the operational map for the framework foundation. Paths are repository-relative; links are resolved from this guide.

| Concern | Source or resource location | Runtime responsibility |
|---|---|---|
| Maven project | [`pom.xml`](../../pom.xml) | Declares Java 21 compilation, framework dependencies, Surefire, Shade, the default TestNG suite, and the `ui_performance` profile. |
| Default Maven suite | [`src/test/resources/testng.xml`](../../src/test/resources/testng.xml) | Runs `com.ptaf.runner.TestRunner` with TestNG method parallelism and four threads. |
| Default TestNG/Cucumber runner | [`src/test/java/com/ptaf/runner/TestRunner.java`](../../src/test/java/com/ptaf/runner/TestRunner.java) — package `com.ptaf.runner` | Extends `AbstractTestNGCucumberTests`; scans `src/test/resources/features`, glue `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and selects `@eStore`. |
| Shared Cucumber lifecycle | [`src/main/java/com/ptaf/hooks/Hooks.java`](../../src/main/java/com/ptaf/hooks/Hooks.java) — package `com.ptaf.hooks` | Starts and closes the normal Playwright browser stack when applicable; deliberately bypasses it for identified API, performance, Appium, file, and database scenarios. |
| Global configuration facade | [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — package `com.ptaf.utils` | Resolves a requested key from `environments.<env>.<key>` first, then the global key. `env` defaults to `QA`. |
| Global YAML loader | [`src/main/java/com/ptaf/utils/YamlReader.java`](../../src/main/java/com/ptaf/utils/YamlReader.java) — package `com.ptaf.utils` | Recursively loads and merges YAML under the classpath folders `elements`, `queries`, `api_requests`, `config`, and `performance`; supports dot-separated lookup keys. |
| Global YAML configuration | [`src/test/resources/config/config.yml`](../../src/test/resources/config/config.yml) | Holds shared framework and environment-oriented settings consumed through `ConfigurationProperties`. |
| Standard feature suite | [`src/test/resources/features/`](../../src/test/resources/features/) | Contains functional, API, database, mobile, document, and protocol-level performance Gherkin features. |
| Step definitions | [`src/test/java/com/ptaf/stepdefinitions/`](../../src/test/java/com/ptaf/stepdefinitions/) — package `com.ptaf.stepdefinitions` | Contains the shared step definition classes, including `ApiSteps`, `DatabaseSteps`, `MobileSteps`, and `PerformanceSteps`. |
| Isolated UI-performance runner | [`src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`](../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) — package `com.ptaf.ui_performance.runners` | Scans only UI-performance features and `com.ptaf.ui_performance.stepdefinitions`; it excludes normal hooks, normal steps, and normal Extent/PDF listeners. |
| Isolated UI-performance suite | [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../src/test/resources/ui_performance/testng-ui_performance.xml) | Starts the isolated runner plus its offline module and start-gate contract tests. |
| Isolated UI-performance configuration | [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../src/test/resources/ui_performance/config/ui_performance-config.yml) | Provides the target components, load profile, browser controls, data settings, thresholds, safety cap, and report options for UI performance only. |

### Execution model

Surefire is explicitly configured with `surefire-testng`, rather than relying on Maven provider auto-detection. The default Surefire suite is `src/test/resources/testng.xml`, uses `parallel=methods`, a thread count of four, and has `testFailureIgnore=true`. Consequently, a normal build can complete its Maven lifecycle even when tests fail; always inspect the generated test reports.[1]

The `ui_performance` profile changes that behavior only for its own run. It selects `src/test/resources/ui_performance/testng-ui_performance.xml`, disables Surefire-level parallelism, uses one Surefire thread, and sets `testFailureIgnore=false`. The module’s own engine is responsible for concurrent browser users, so adding Maven-level concurrency to that profile would change the intended model.[1] [15]

## Prerequisites

Install a Java 21 JDK and Maven before using the project. The compiler source and target in the build descriptor are both 21; the older Java 11-or-later statement in the repository readme does not match the current Maven compiler configuration.[1] [2]

From the repository root, verify the active tools:

```bash
java -version
mvn --version
```

UI scenarios require the Playwright browser binaries that match the Java dependency. The repository readme documents the following browser-install command; execute it before running a browser-based suite on a new machine.[2]

```bash
mvn exec:java -e \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install"
```

Use a network path and test accounts that your organization has approved for the target environment. Do not add passwords, bearer tokens, API keys, or private host values to feature files, committed YAML, logs, or reports. The API handler obtains an optional authorization token from the environment variable named by configuration, and the database handler obtains the SQL Server password from a configured environment-variable name.[6] [7]

## Configuration files and key settings

### Shared configuration resolution

The actual shared configuration location is **`src/test/resources/config/config.yml`**, not a root-level `src/test/resources/config.yml`. `YamlReader` scans the classpath directory named `config` and `ConfigurationProperties` uses the merged result.[3] [4]

For a key such as `browser`, `ConfigurationProperties.getValue("browser")` first attempts `environments.<env>.browser`, where `<env>` is the JVM property `env` or `QA` when that property is absent. It then falls back to the global `browser` key. Values are accessed using dot-separated paths; this is also how API and database configuration sections are read.[3]

The following is a **structure-only** example. Every value is a placeholder and must be replaced by an approved non-sensitive setting. It illustrates keys used by the current configuration, browser, API, and database access classes.

```yaml
browser: "<BROWSER_NAME>"
headless: "<TRUE_OR_FALSE>"
ignoreHTTPSErrors: "<TRUE_OR_FALSE>"
runtimeWait: <SECONDS>
videoCapture: "<TRUE_OR_FALSE>"
maximize_browser: "<TRUE_OR_FALSE>"

api_services:
  <SERVICE_KEY>:
    base_url: "https://<APPROVED_HOST>"
    auth_token_env: "<TOKEN_ENVIRONMENT_VARIABLE_NAME>"

database:
  db_type: "sqlserver"
  authentication: "<windows_or_sqlserver>"
  server_name: "<APPROVED_DATABASE_HOST>"
  port: "<PORT>"
  database_name: "<DATABASE_NAME>"
  username: "<NON_SECRET_USERNAME>"
  password_env_variable: "<PASSWORD_ENVIRONMENT_VARIABLE_NAME>"

environments:
  <ENVIRONMENT_NAME>:
    browser: "<BROWSER_NAME>"
    headless: "<TRUE_OR_FALSE>"
```

`BrowserFactory` gives the JVM system property `headless` precedence over the resolved `headless` YAML value and otherwise defaults to headed operation when neither is supplied. It uses `ignoreHTTPSErrors`, `videoCapture`, and either `maximize_browser` or `maximizeBrowser` from the shared configuration.[5] The standard browser video directory is created below `test-output/captured-videos/<timestamp>` only when video capture is enabled.[5]

### Configuration inventory

| Area | Canonical location | Reader or consuming class | Important settings or format |
|---|---|---|---|
| Shared framework, UI, API, DB, reporting, and environment overrides | [`src/test/resources/config/config.yml`](../../src/test/resources/config/config.yml) | `com.ptaf.utils.YamlReader`; `com.ptaf.utils.ConfigurationProperties` | Environment override layout is `environments.<env>.<key>`. Browser controls include `browser`, `headless`, `ignoreHTTPSErrors`, and `runtimeWait`. |
| UI locators | [`src/test/resources/elements/`](../../src/test/resources/elements/) | Loaded by `com.ptaf.utils.YamlReader`; used by normal UI steps and page/action layers | YAML files are recursively merged with the global YAML store. Keep logical locator keys here rather than embedding selectors in functional features. |
| API service/request definitions | [`src/test/resources/api_requests/api_requests.yml`](../../src/test/resources/api_requests/api_requests.yml) | `YamlReader`; `com.ptaf.api.handlers.ApiRequestHandler`; `com.ptaf.api.methods.ApiCommonMethods` | Request keys are passed to API steps. A service base URL is read from `api_services.<service>.base_url`; `auth_token_env` names an environment variable rather than storing its token. |
| SQL query definitions | [`src/test/resources/queries/db_queries.yml`](../../src/test/resources/queries/db_queries.yml) | `YamlReader`; database action layer; `com.ptaf.db.pages.DatabaseCommonMethods` | Store SQL under logical keys and reference those keys from database features. |
| General test data | [`src/test/resources/data/`](../../src/test/resources/data/) and [`src/test/resources/testdata.xlsx`](../../src/test/resources/testdata.xlsx) | Feature-specific step implementations | Current checked-in resources include CSV/XML data under `data` and a workbook at the resource root. Use non-sensitive test data only. |
| API/JMeter-style performance controls | [`src/test/resources/performance/config/performance-config.yml`](../../src/test/resources/performance/config/performance-config.yml) | `com.ptaf.performance.config.PerformanceYamlReader` | Loaded directly as the single classpath resource `performance/config/performance-config.yml`, rather than through the global merge alone.[8] |
| API/JMeter-style performance payloads | [`src/test/resources/performance/payloads/`](../../src/test/resources/performance/payloads/) | `com.ptaf.performance.payloads.PerformancePayloadResolver` and related payload readers | Contains CSV, Excel, and YAML payload resources used by `PerformanceSteps`. |
| Native/Appium mobile controls | [`src/test/resources/mobile/config/`](../../src/test/resources/mobile/config/) | `com.ptaf.mobile.config.MobileConfigurationProperties` and `MobileYamlReader` | Supports `mobile.*`, evidence, permissions, and Appium browser-mode configuration. Platform can be overridden with `-Dmobile.platform=android` or `-Dmobile.platform=ios`.[10] |
| Playwright mobile-browser emulation | [`src/test/resources/mobile_browser/config/`](../../src/test/resources/mobile_browser/config/) | `com.ptaf.ui.mobilebrowser.MobileBrowserYamlReader`, `MobileBrowserExecutionConfig`, `MobileBrowserProfileRepository` | The isolated mobile-browser reader recursively loads YAML beneath `mobile_browser`; profiles are read from `mobile_browser_profiles`.[9] |
| UI-performance controls | [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../src/test/resources/ui_performance/config/ui_performance-config.yml) | `com.ptaf.ui_performance.config.UiPerformanceYamlReader`; `UiPerformanceConfiguration` | Read only by the isolated UI-performance module. Override the classpath resource with `-Dui.performance.config=<classpath-resource-path>` when using an approved alternate configuration.[13] |
| UI-performance locators and user rows | [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`](../../src/test/resources/ui_performance/locators/ui_performance-locators.yml) and [`src/test/resources/ui_performance/data/users.csv`](../../src/test/resources/ui_performance/data/users.csv) | `UiPerformanceLocatorRepository`; `UiPerformanceUserDataReader` | These resources are intentionally separate from normal locator YAML and normal data resources. |
| Cucumber publishing setting | [`src/test/resources/cucumber.properties`](../../src/test/resources/cucumber.properties) | Cucumber runtime | Disables Cucumber publishing. |
| Extent reporting | [`src/test/resources/extent.properties`](../../src/test/resources/extent.properties) and [`src/test/resources/extent-config.xml`](../../src/test/resources/extent-config.xml) | Extent Cucumber adapter | Configures timestamped report folders below `test-output/` and Spark, Base64, PDF, and Excel reporter outputs.[16] |

### Isolated UI-performance configuration

`UiPerformanceYamlReader` does **not** use the global `YamlReader`; it loads only `ui_performance/config/ui_performance-config.yml` from the test classpath. `UiPerformanceConfiguration` validates target protocol, bare host, port, and relative route format. It also requires `ui_performance.enabled: true` before a live run can proceed.[13] [14]

Use a configuration-first feature. Keep the target, routes, locators, user data, concurrency stages, thresholds, and reports in the isolated resource files. The following safe template reflects the supported layout without exposing a target or data value.

```yaml
ui_performance:
  enabled: <TRUE_OR_FALSE>
  active_profile: "<load_or_stress_or_spike_or_soak>"
  target:
    protocol: "https"
    host: "<APPROVED_BARE_HOST>"
    port: <PORT>
    base_path: "/<OPTIONAL_BASE_PATH>"
    routes:
      <ROUTE_NAME>: "/<RELATIVE_ROUTE>"
  profiles:
    load:
      type: "load"
      stages:
        - name: "<STAGE_NAME>"
          users: <USER_COUNT>
          ramp_up_seconds: <SECONDS>
          hold_seconds: <SECONDS>
          iterations_per_user: <ITERATIONS>
  browser:
    headless: <TRUE_OR_FALSE>
    ignore_https_errors: <TRUE_OR_FALSE>
    isolation: "process"
    action_timeout_ms: <MILLISECONDS>
    navigation_timeout_ms: <MILLISECONDS>
  data:
    use_csv: <TRUE_OR_FALSE>
    users_csv: "ui_performance/data/<USERS_FILE>.csv"
    allow_user_reuse: <TRUE_OR_FALSE>
  thresholds:
    maximum_failure_rate_percent: <PERCENT>
    maximum_average_journey_duration_ms: <MILLISECONDS>
    maximum_p95_journey_duration_ms: <MILLISECONDS>
  safety:
    max_virtual_users: <MAXIMUM_USERS>
  reporting:
    output_directory: "test-output/ui_performance"
    html_enabled: <TRUE_OR_FALSE>
    pdf_enabled: <TRUE_OR_FALSE>
    csv_enabled: <TRUE_OR_FALSE>
    json_enabled: <TRUE_OR_FALSE>
```

## Features, tags, runners, and suites

Cucumber tag expressions are part of each runner’s source configuration. Feature placement and tags must agree with the intended runner; a correct feature outside the runner’s feature path, or a missing required tag, is not selected.

| Execution path | Runner / suite | Feature root | Current tag expression | Glue / boundary |
|---|---|---|---|---|
| Default Maven execution | `com.ptaf.runner.TestRunner` through [`testng.xml`](../../src/test/resources/testng.xml) | `src/test/resources/features` | `@eStore` | `com.ptaf.stepdefinitions` and `com.ptaf.hooks`. |
| General JUnit UI runner | [`com.ptaf.runners.TestRunner`](../../src/test/java/com/ptaf/runners/TestRunner.java) | `src/test/resources/features` | `@eStore` | `com/ptaf/stepdefinitions`, `com/ptaf/hooks`. This class is an IDE-oriented JUnit entry point, not the class named in the default TestNG XML. |
| Database JUnit runner | [`com.ptaf.runners.DatabaseTestRunner`](../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java) | `src/test/resources/features/db` | `@db or @database or @sql` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks`; `DatabaseHooks` closes the thread-local DB connection for those tags.[7] |
| Mobile JUnit runner | [`com.ptaf.runners.MobileTestRunner`](../../src/test/java/com/ptaf/runners/MobileTestRunner.java) | `src/test/resources/features/mobile` | `@theapp_smoke` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks`. `MobileHooks` recognizes `@mobile`, `@android`, `@ios`, `@cross_platform`, `@appium_browser`, and `@mobile_browser_real` for Appium lifecycle handling.[10] |
| Protocol-level performance JUnit runner | [`com.ptaf.runners.PerformanceTestRunner`](../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java) | `src/test/resources/features/performance` | `@performance_testing` | `com.ptaf.stepdefinitions`, `com.ptaf.hooks`. Normal `Hooks` also treats `@performance*` scenarios as browserless. |
| Regression JUnit runner | [`com.ptaf.runners.Regression_Runner`](../../src/test/java/com/ptaf/runners/Regression_Runner.java) | `src/test/resources/features` | `@regression` | `com/ptaf/stepdefinitions`, `com/ptaf/hooks`. |
| Parallel TestNG runner | [`com.ptaf.runners.ParallelRun`](../../src/test/java/com/ptaf/runners/ParallelRun.java) | `src/test/resources/features` | `@secondPageTest` | TestNG data provider is declared `parallel = true`; glue includes standard steps and hooks. |
| Isolated UI performance | `com.ptaf.ui_performance.runners.UiPerformanceRunner` through [`testng-ui_performance.xml`](../../src/test/resources/ui_performance/testng-ui_performance.xml) | `src/test/resources/ui_performance/features` | `@ui_performance and not @template` | **Only** `com.ptaf.ui_performance.stepdefinitions`; does not load standard hooks or standard UI glue.[15] |

The repository also contains [`com.ptaf.runners.ApiTestRunner`](../../src/test/java/com/ptaf/runners/ApiTestRunner.java), a JUnit runner with the `@api` filter. Its declared glue is `com/ptaf/api/stepdefinitions`, whereas the checked-in `ApiSteps` class is in `com.ptaf.stepdefinitions`. Treat that runner as a configuration discrepancy rather than the supported default Maven API route until its glue path is aligned; do not assume `mvn test` invokes it.[11] [12]

## Build and exact run commands

Run commands from the repository root, `/home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23`.

### Compile and normal framework suite

```bash
# Compile production and test sources without running tests.
mvn clean test-compile -DskipTests

# Run the default Surefire TestNG suite: src/test/resources/testng.xml.
mvn clean test

# Run tests, package the artifact, execute the configured Shade goal, and install the artifacts locally.
mvn clean install
```

The default suite invokes `com.ptaf.runner.TestRunner`, which filters to `@eStore`; it is not a run-all-features command. The default Surefire configuration has `testFailureIgnore=true`, so use Surefire, Cucumber, and Extent outputs to determine test status rather than relying only on the Maven process exit code.[1] [11]

Use an approved environment override and a command-line headless override when appropriate:

```bash
mvn clean test -Denv=<ENVIRONMENT_NAME> -Dheadless=<true_or_false>
```

`-Denv` participates in `ConfigurationProperties` lookup, while `-Dheadless` takes precedence over the shared YAML value in `BrowserFactory`.[3] [5]

### Isolated UI-performance suite

```bash
# Run only the UI-performance profile and its dedicated TestNG suite.
mvn clean test -Pui_performance
```

This is the prescribed UI-performance command. The profile points Surefire at `src/test/resources/ui_performance/testng-ui_performance.xml`; that suite starts the dedicated runner, module contract test, and start-gate test.[1] [17]

The repository also provides a browser-contract configuration and suite. Use the following complete command only in an approved environment capable of running the browser contract:

```bash
mvn clean test -Pui_performance \
  -Dui.performance.config=ui_performance/config/ui_performance-browser-contract.yml \
  -Dsurefire.suiteXmlFiles=src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml
```

`UiPerformanceYamlReader` accepts the `ui.performance.config` system property only as a **classpath resource** path. The browser-contract suite starts `com.ptaf.ui_performance.UiPerformanceConcurrentBrowserIntegrationTest`.[13] [18]

### IDE execution

For the default Maven route, run [`src/test/resources/testng.xml`](../../src/test/resources/testng.xml) as a TestNG suite. For an isolated UI-performance run, run [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../src/test/resources/ui_performance/testng-ui_performance.xml) as a TestNG suite. The JUnit entry-point classes in `com.ptaf.runners` can be run directly from an IDE when their feature path, tag filter, and glue declaration match the module being exercised.

## How to create a new feature or test

### Add a feature within an existing module

First choose the existing module whose lifecycle and resource model match the test. Add functional UI, API, database, mobile, protocol-level performance, XML, CSV, PDF, or ZIP features under the appropriate subdirectory of `src/test/resources/features/`. Add a scenario tag that is selected by the runner you intend to use. Reuse a tested step definition from `com.ptaf.stepdefinitions` where possible; add a new step definition in that package only when the behavior belongs to the shared module.

For example, this is a placeholder-only database feature that uses the established query-key convention and database tag. It belongs under `src/test/resources/features/db/` and its SQL stays in `src/test/resources/queries/db_queries.yml`.

```gherkin
@db
Feature: <DOMAIN> database validation

  Scenario: Verify the approved record exists
    Given I validate the database connection is successful
    Then I verify the database contains a record for query "<QUERY_GROUP>.<QUERY_KEY>" with parameters "<APPROVED_TEST_VALUE>"
```

An API feature belongs under `src/test/resources/features/` or an appropriate API subdirectory and should use the `@api` tag so that normal hooks do not start a Playwright browser. Request definitions belong in `src/test/resources/api_requests/api_requests.yml`, and its base service is configured in `config/config.yml` under `api_services`.[4] [6] The following uses placeholders only:

```gherkin
@api
Feature: <SERVICE> API contract

  Scenario: Retrieve an approved resource
    Given I set the request header "Accept" to "application/json"
    And I set the path parameter "<PARAMETER_NAME>" to "<APPROVED_TEST_VALUE>"
    When I send a "<REQUEST_GROUP>.<REQUEST_KEY>" request to the "<SERVICE_KEY>" service
    Then the response code should be <EXPECTED_STATUS>
```

For a normal UI feature, place logical selectors in a YAML file under `src/test/resources/elements/` and reference the established shared UI steps. Do not put raw selectors, private URLs, passwords, tokens, or customer data in Gherkin.

### Add an isolated UI-performance journey

Place the feature under `src/test/resources/ui_performance/features/`, tag it `@ui_performance`, and do not tag it `@template` if it is approved for execution. Add routes to `ui_performance-config.yml`, selectors to `ui_performance-locators.yml`, and, when required, non-sensitive virtual-user fields to `ui_performance/data/users.csv`. `UiPerformanceSteps` supports configured targets and routes, locator-based fill/select/click/visibility steps, and data-field substitution.[14]

```gherkin
@ui_performance
Feature: <JOURNEY_NAME> UI performance

  Scenario: Approved users complete the configured journey
    Given UI performance journey "<JOURNEY_NAME>" uses configured target
    When UI performance journey navigates to configured route "<ROUTE_NAME>"
    And UI performance journey fills locator "<LOCATOR_GROUP>" "<FIELD_KEY>" with data field "<CSV_COLUMN>"
    And UI performance journey clicks locator "<LOCATOR_GROUP>" "<ACTION_KEY>"
    Then UI performance journey verifies locator "<LOCATOR_GROUP>" "<RESULT_KEY>" is visible
    And the configured UI performance users execute the journey
    And the UI performance run produces a standalone performance report
```

Use the `data field` form for sensitive values. The literal-fill step explicitly rejects locator keys containing `password`, `token`, `secret`, or `credential`, so sensitive values do not belong in the feature file.[14]

### Add a new module without affecting existing modules

A new domain that has distinct lifecycle, concurrency, data, or reporting requirements should be isolated rather than added to the default runner. The UI-performance module is the current model: it has its own Java packages, resource root, YAML reader, runner, TestNG suite, Maven profile, and output directory.[13] [15]

Follow this sequence:

1. Create a dedicated package, such as `src/test/java/com/ptaf/<module_id>/runners/` and `src/test/java/com/ptaf/<module_id>/stepdefinitions/`. Use a unique tag, such as `@<module_id>`, and a dedicated feature root at `src/test/resources/<module_id>/features/`.
2. Keep module configuration, locators, payloads, and data under `src/test/resources/<module_id>/`. Create a dedicated reader when the resources must remain isolated. The global `YamlReader` scans only `elements`, `queries`, `api_requests`, `config`, and `performance`; a new `<module_id>` folder is **not** automatically loaded.[4]
3. Create a dedicated TestNG suite, for example `src/test/resources/<module_id>/testng-<module_id>.xml`, that names only the new runner and any intentional module contract tests.
4. Add a dedicated Maven profile that overrides only `maven-surefire-plugin` `suiteXmlFiles` for that suite. Set module-specific parallelism and failure behavior inside that profile; do not alter the default Surefire configuration.
5. Configure the runner with its exact feature root, exclusive glue package, unique tag expression, and module-specific report locations. Include `com.ptaf.hooks` only when the new module intentionally requires the standard Playwright lifecycle. If it needs its own lifecycle, do not inherit shared hooks accidentally.
6. Keep `src/test/resources/testng.xml`, `com.ptaf.runner.TestRunner`, their `@eStore` filter, global resource locations, and existing report paths unchanged. The result is an additive command, `mvn clean test -P<module_id>`, while `mvn clean test` continues to execute the current default suite.

A safe TestNG suite template is:

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="FNB-ETAF <MODULE_NAME> Suite" verbose="1" parallel="false">
  <test name="<MODULE_NAME> tests">
    <classes>
      <class name="com.ptaf.<module_id>.runners.<ModuleRunnerClass>"/>
    </classes>
  </test>
</suite>
```

This additive boundary is important because the default suite does not discover arbitrary test classes; it names its runner explicitly. A new suite/profile therefore avoids changing existing execution by construction.[1] [17]

## Expected reports and artifacts

| Execution route | Expected outputs | Notes |
|---|---|---|
| All Surefire invocations | `target/surefire-reports/` | Surefire report directory configured by the build. |
| Default TestNG/Cucumber runner | `target/cucumber-reports/cucumber-pretty`, `target/cucumber-reports/CucumberTestReport.json`, and `target/cucumber-reports/rerun.txt` | Configured by `com.ptaf.runner.TestRunner`; Surefire also supplies Cucumber JSON and JUnit XML plugin destinations under `target/cucumber-reports/`.[1] [11] |
| Extent-enabled normal runners | A timestamped folder beneath `test-output/`, with Spark HTML, Base64 HTML, PDF, and Excel report paths configured in `extent.properties` | Applies where `ExtentCucumberAdapter` is included in the runner plugin list. |
| Database JUnit runner | `target/cucumber-reports/database-report.html`, `database-report.json`, and `database-report.xml` | Configured by `DatabaseTestRunner`.[12] |
| Mobile JUnit runner | `target/cucumber-reports/mobile-report.html`, `mobile-report.json`, and `mobile-report.xml` | Configured by `MobileTestRunner`. |
| Protocol-level performance JUnit runner | `target/performance-cucumber-report.html` | The runner also configures the Extent adapter.[12] |
| UI-performance Cucumber result | `test-output/ui_performance/cucumber/cucumber.html`, `cucumber.json`, and `cucumber.xml` | Separate from normal Cucumber outputs.[15] |
| UI-performance standalone result | Timestamped journey folder below the configured `test-output/ui_performance` directory | When enabled by YAML, the module writes its HTML, PDF, CSV, and JSON summary artifacts. Its module contract test verifies summary, stage, iteration, step-timing, and performance-summary outputs.[13] |
| Normal UI video evidence | `test-output/captured-videos/<timestamp>/` | Produced only when shared `videoCapture` is enabled; video filenames are organized using feature names during normal hook teardown.[5] [19] |

## Troubleshooting

| Symptom | Likely cause supported by the code | Resolution |
|---|---|---|
| Maven reports no expected scenarios in a normal run | Default `testng.xml` names only `com.ptaf.runner.TestRunner`, whose tag expression is `@eStore`. | Confirm the feature is below `src/test/resources/features/` and carries `@eStore`, or run the correct dedicated suite/profile instead. Do not assume default Maven execution runs all features. |
| A new YAML setting is `null` | Its file is outside the folders scanned by `YamlReader`, its dot path is wrong, or an environment override is absent. | Place globally shared settings under `elements`, `queries`, `api_requests`, `config`, or `performance`; otherwise use a dedicated reader. Check the key as `environments.<env>.<key>` and then as the global key.[3] [4] |
| Browser starts headed in CI/local execution unexpectedly | No `-Dheadless` override and no usable shared `headless` configuration was resolved. | Pass `-Dheadless=true` for the command or set the approved shared value. JVM property takes precedence.[5] |
| A database run leaves or cannot open a connection | Database settings are missing/invalid, an environment password variable is absent for SQL Server authentication, or the scenario lacks a recognized database tag. | Validate `database.*` configuration and its environment variable. Use `@db`, `@database`, or `@sql` so `DatabaseHooks` closes the thread-local connection.[7] |
| API execution starts a browser or fails authentication | The feature does not have an API-identifying tag, or the configured token environment variable is unset. | Use `@api` (or the supported API tag prefixes) and supply the named environment variable outside source control. `Hooks` classifies these API scenarios as browserless.[6] [20] |
| API JUnit runner cannot locate steps | `ApiTestRunner` declares `com/ptaf/api/stepdefinitions`, while checked-in `ApiSteps` is `com.ptaf.stepdefinitions`. | Use the default/common execution route where appropriate or align that runner’s glue in a future, separately reviewed change. Do not silently add duplicate step classes. |
| UI-performance run stops before browser work | The isolated configuration is disabled, invalid, not on the classpath, has an invalid target component, or a required route/locator is missing. | Check `ui_performance.enabled`, use a bare host and relative route, and verify the configured resource path. The isolated reader fails fast on missing or invalid configuration.[13] |
| UI-performance run uses normal hooks or normal reports | The default suite was used instead of the `ui_performance` profile, or the isolated runner was modified to scan shared glue. | Run `mvn clean test -Pui_performance`; retain the isolated feature path and glue declaration. |
| Maven completes although functional tests failed | Normal Surefire has `testFailureIgnore=true`. | Review `target/surefire-reports/`, Cucumber files, and configured Extent artifacts. The UI-performance profile deliberately changes this to `false`.[1] |

## Module boundaries

The framework has several deliberately different execution models. Keeping their boundaries explicit prevents accidental double browser initialization, conflicting configuration readers, and report collisions.

**Normal UI and shared functional automation** use `com.ptaf.runner.TestRunner`, the shared `com.ptaf.hooks.Hooks` lifecycle, globally merged YAML resources, and shared Cucumber/Extent reporting. The hooks initialize a Playwright browser only when the scenario is not classified as API, performance, Appium mobile, file/database, or document-oriented.[11] [20]

**API and database automation** use the shared step-definition package and global YAML configuration. API contexts are thread-local in `com.ptaf.api.handlers.ApiRequestHandler`; database connections are thread-local in `com.ptaf.db.handlers.DatabaseHandler`. API tokens and database passwords are read from environment variables named by configuration, not embedded in source resources.[6] [7]

**Mobile and mobile-browser automation** have their own configuration resource trees and lifecycle controls. `MobileHooks` starts Appium sessions only for its recognized mobile tags, while mobile-browser profile settings are read through the separate `MobileBrowserYamlReader`.[9] [10]

**Protocol-level performance** uses `com.ptaf.performance.*` and its dedicated `performance/config/performance-config.yml` reader. It is not the same module as UI performance; it exposes HTTP-performance steps through `com.ptaf.stepdefinitions.PerformanceSteps` and is treated as browserless by the normal hooks.[8] [12] [20]

**UI-performance automation** is intentionally standalone. Its runner excludes `com.ptaf.hooks`, shared UI step definitions, mobile hooks, and normal Extent/PDF listeners. Its data, configuration, locators, feature root, TestNG suite, report directory, and Maven profile are all separate. Do not merge UI-performance YAML into the global config, do not add its runner to the default `testng.xml`, and do not use the standard browser lifecycle in its runner.[13] [15]

## References

<!-- Visible source-reference list -->
The sources below are visible and clickable in Markdown preview. Citation labels used in this guide point to the same source files.

- **[1]** [FNB-ETAF Maven build descriptor](../../pom.xml) — `../../pom.xml`
- **[2]** [PTAF unified test automation framework readme](../../ReadMe.md) — `../../ReadMe.md`
- **[3]** [Shared configuration accessor](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — `../../src/main/java/com/ptaf/utils/ConfigurationProperties.java`
- **[4]** [Global YAML resource reader](../../src/main/java/com/ptaf/utils/YamlReader.java) — `../../src/main/java/com/ptaf/utils/YamlReader.java`
- **[5]** [Playwright browser and context factory](../../src/main/java/com/ptaf/utils/BrowserFactory.java) — `../../src/main/java/com/ptaf/utils/BrowserFactory.java`
- **[6]** [API request-context configuration and lifecycle](../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java) — `../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java`
- **[7]** [Database connection configuration and lifecycle](../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java) — `../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java`
- **[8]** [Protocol-level performance YAML reader](../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java) — `../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java`
- **[9]** [Mobile-browser YAML reader](../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java) — `../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java`
- **[10]** [Appium mobile Cucumber lifecycle hooks](../../src/main/java/com/ptaf/hooks/MobileHooks.java) — `../../src/main/java/com/ptaf/hooks/MobileHooks.java`
- **[11]** [Default TestNG Cucumber runner](../../src/test/java/com/ptaf/runner/TestRunner.java) — `../../src/test/java/com/ptaf/runner/TestRunner.java`
- **[12]** [JUnit and TestNG Cucumber runner package](../../src/test/java/com/ptaf/runners/) — `../../src/test/java/com/ptaf/runners/`
- **[13]** [Isolated UI-performance YAML reader](../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java) — `../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java`
- **[14]** [UI-performance Gherkin steps](../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java) — `../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`
- **[15]** [Isolated UI-performance TestNG Cucumber runner](../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) — `../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`
- **[16]** [Extent report output configuration](../../src/test/resources/extent.properties) — `../../src/test/resources/extent.properties`
- **[17]** [Default TestNG suite](../../src/test/resources/testng.xml) — `../../src/test/resources/testng.xml`
- **[18]** [UI-performance browser-contract TestNG suite](../../src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml) — `../../src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml`
- **[19]** [Feature-based artifact naming utility](../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) — `../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`
- **[20]** [Shared Cucumber browser and browserless scenario lifecycle](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`

<!-- Internal citation definitions used by the in-text [n] links. Keep these definitions so citations remain clickable. -->
[1]: ../../pom.xml "FNB-ETAF Maven build descriptor"
[2]: ../../ReadMe.md "PTAF unified test automation framework readme"
[3]: ../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Shared configuration accessor"
[4]: ../../src/main/java/com/ptaf/utils/YamlReader.java "Global YAML resource reader"
[5]: ../../src/main/java/com/ptaf/utils/BrowserFactory.java "Playwright browser and context factory"
[6]: ../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java "API request-context configuration and lifecycle"
[7]: ../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java "Database connection configuration and lifecycle"
[8]: ../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java "Protocol-level performance YAML reader"
[9]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java "Mobile-browser YAML reader"
[10]: ../../src/main/java/com/ptaf/hooks/MobileHooks.java "Appium mobile Cucumber lifecycle hooks"
[11]: ../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[12]: ../../src/test/java/com/ptaf/runners/ "JUnit and TestNG Cucumber runner package"
[13]: ../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java "Isolated UI-performance YAML reader"
[14]: ../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "UI-performance Gherkin steps"
[15]: ../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java "Isolated UI-performance TestNG Cucumber runner"
[16]: ../../src/test/resources/extent.properties "Extent report output configuration"
[17]: ../../src/test/resources/testng.xml "Default TestNG suite"
[18]: ../../src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml "UI-performance browser-contract TestNG suite"
[19]: ../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-based artifact naming utility"
[20]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Shared Cucumber browser and browserless scenario lifecycle"
