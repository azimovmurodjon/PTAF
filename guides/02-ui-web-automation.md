# FNB-ETAF Regular Playwright Web UI Automation Guide

## Purpose and scope

This guide explains the **regular functional web UI automation** path in FNB-ETAF. It covers the Cucumber/TestNG execution path that uses the normal Playwright browser lifecycle, YAML-backed locators, reusable page and frame actions, screenshots, video recording, downloads, and functional reports. It is intended for authors of ordinary browser scenarios under `src/test/resources/features/`.

This is deliberately **not** a guide to browser load testing. The separate `ui_performance` module has its own runner, configuration, data, locators, browser workers, and reports. A normal feature is executed as a Cucumber scenario with the lifecycle below; it is not scheduled as a virtual-user journey. The distinction is essential because the normal lifecycle owns `BrowserFactory`, `Hooks`, `@LastScenario`, functional screenshots, videos, downloads, and Extent reporting.[1] [2]

> **Scope rule:** Use this guide for functional validation of browser behaviour. Use the isolated `ui_performance` profile only for approved concurrent-browser performance work. Do not mix `@performance*` tags or `features/performance/` paths into an ordinary UI feature, because the normal hook deliberately treats them as browserless.[3]

## Architecture and source locations

| Layer | Source location and package | Responsibility in the regular UI path |
|---|---|---|
| Build and default suite | [`pom.xml`](../../pom.xml); [`src/test/resources/testng.xml`](../../src/test/resources/testng.xml) | Maven compiles for Java 21 and Surefire launches the default TestNG suite. The default suite points to `com.ptaf.runner.TestRunner`; Surefire is configured for method parallelism with four threads and ignores test failures by default.[1] |
| Cucumber entry point | [`src/test/java/com/ptaf/runner/TestRunner.java`](../../src/test/java/com/ptaf/runner/TestRunner.java), package `com.ptaf.runner` | `TestRunner` extends `AbstractTestNGCucumberTests`, scans `src/test/resources/features`, and uses glue packages `com.ptaf.stepdefinitions` and `com.ptaf.hooks`. Its current selection is the `@eStore` tag expression.[4] |
| Browser lifecycle | [`src/main/java/com/ptaf/hooks/Hooks.java`](../../src/main/java/com/ptaf/hooks/Hooks.java), package `com.ptaf.hooks` | `Hooks` owns per-thread browser, context, page, scenario, and page-helper state. It creates the regular stack, identifies browserless scenarios, handles `@LastScenario`, and closes browser resources.[3] |
| Browser/context factory | [`src/main/java/com/ptaf/utils/BrowserFactory.java`](../../src/main/java/com/ptaf/utils/BrowserFactory.java), package `com.ptaf.utils` | Creates Chrome, Firefox, WebKit, or Edge browsers and contexts. It reads browser-related configuration, applies context HTTPS settings and optional HTTP credentials from JVM properties, and configures recording when enabled.[5] |
| Common page and frame facades | [`src/main/java/com/ptaf/ui/pages/PageCommonMethods.java`](../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java) and [`src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java`](../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java), package `com.ptaf.ui.pages` | Convert reusable Cucumber steps into page or nested-frame actions. `FrameCommonMethods` accepts up to three iframe selectors; page helpers operate in the active top-level page.[6] [7] |
| Action dispatcher | [`src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java`](../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) and [`src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java`](../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java), package `com.ptaf.ui.action_performer` | Resolves the YAML locator into a Playwright `Locator`, performs actions, applies bounded readiness waits, waits for `DOMContentLoaded` after navigation-like actions, and implements download/upload/screenshot actions.[8] [9] |
| Locator resolution | [`src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java`](../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java) and [`src/main/java/com/ptaf/ui/handlers/LocatorHandler.java`](../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java), packages `com.ptaf.ui.helpers` and `com.ptaf.ui.handlers` | Reads `elements.<group>.<key>` values, parses `TYPE_value` tokens, supports chained locators separated by `>`, and maps supported tokens to Playwright locators for pages, frames, or a parent locator.[10] [11] |
| Cucumber step bindings | [`src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`](../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java), [`NewPageCommonSteps.java`](../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java), and [`FrameCommonSteps.java`](../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java), package `com.ptaf.stepdefinitions` | Provide the Gherkin bindings for navigation, page actions, popup switching, screenshots, and predefined frame contexts. They obtain the active page/context from `Hooks`.[12] [13] [14] |
| Configuration and YAML repository | [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) and [`src/main/java/com/ptaf/utils/YamlReader.java`](../../src/main/java/com/ptaf/utils/YamlReader.java), package `com.ptaf.utils` | `ConfigurationProperties` resolves an environment-specific key first and then a common key. `YamlReader` merges YAML below `elements`, `queries`, `api_requests`, `config`, and `performance` into its in-memory map.[15] [16] |
| Evidence and reports | [`src/main/java/com/ptaf/utils/ScreenshotHandler.java`](../../src/main/java/com/ptaf/utils/ScreenshotHandler.java), [`FeatureArtifactNameResolver.java`](../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java), and [`src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`](../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) | Screenshots are attached to Cucumber results; downloads and videos are grouped using the declared `Feature:` title; the registered listener can produce per-feature Extent reports.[17] [18] [19] |

## Prerequisites

Use a **JDK 21** and Maven. The project compiler source and target are both 21, and the build declares Playwright 1.62.0, Cucumber 7.20.0, TestNG 7.10.2, and the Extent Cucumber adapter.[1] Ensure that the approved test environment, non-production test data, and any required corporate network access are available before attempting a live UI run.

Install the Playwright browser binaries before the first local browser execution. The repository README documents the following command:

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Build dependencies and compile the test sources without starting scenarios with:

```bash
mvn clean test-compile -DskipTests
```

Do not put passwords, access tokens, client identifiers, or unredacted private URLs in a feature file, locator YAML, committed data file, terminal output, or report name. `BrowserFactory` accepts HTTP basic-auth values only when both `-Dservice.username` and `-Dservice.password` JVM properties are supplied; keep their values in the approved secret-injection mechanism rather than in repository resources.[5]

## Regular UI lifecycle

### Normal scenario lifecycle

The normal lifecycle begins when Maven Surefire loads `src/test/resources/testng.xml`. That suite invokes `com.ptaf.runner.TestRunner`, whose Cucumber options search `src/test/resources/features`, `com.ptaf.stepdefinitions`, and `com.ptaf.hooks`.[1] [4] Cucumber creates `Hooks` for each scenario and runs `Hooks.setUp(Scenario)` before the feature steps.

For a regular UI scenario, `Hooks` resolves the browser named by the `browser` configuration key, invokes `BrowserFactory.createBrowser(...)`, constructs a `BrowserContext` through `BrowserFactory.createContextWithVideo(...)`, creates a `Page`, and stores browser/context/page state in `ThreadLocal` variables. It applies `runtimeWait` to the Playwright default action and navigation timeouts; an absent, invalid, or non-positive value falls back to 30 seconds. The hook then creates the scenario’s `PageCommonMethods` instance.[3]

The step definition receives the active `Page` and invokes a page or frame facade. `ElementActionImpl` reads a locator from the merged YAML repository, maps it to a Playwright `Locator`, and dispatches the requested action. `ActionPerformer` uses `time_to_wait_in_seconds` as the maximum element readiness wait. For click, select, check, press, and similar navigation-like operations, it subsequently attempts a bounded `DOMContentLoaded` wait; it does not wait for `NETWORKIDLE`.[8] [9]

At the end of a normal scenario, `Hooks.tearDown(Scenario)` invokes `PageCommonMethods.finalizeScenario()` for a passed browser scenario and then closes regular browser resources. Browser closure finalizes enabled videos before their feature-based names are assigned. A direct action failure is handled in the page/frame facade by capturing a full-page screenshot attachment, closing browser resources, and failing the flow unless soft assertions are enabled.[3] [6] [7] [17]

### `@LastScenario` is an explicit exception

A feature tagged `@LastScenario` shares its browser/context/page across the feature’s scenarios. `Hooks` counts the feature’s runnable scenarios and closes the shared browser after the final scenario. If the shared session fails unexpectedly, later scenarios in that feature are intentionally failed; an explicit `we close all browsers` step marks a deliberate close so a subsequent scenario may create a fresh stack.[3] [12]

For a new, independent functional feature, **do not add `@LastScenario`**. The normal per-scenario lifecycle is safer because it isolates state and simplifies evidence collection. Use `@LastScenario` only when browser state must be preserved across several ordered scenarios and that behaviour has been reviewed.

### Browserless classification

Before creating a Playwright stack, `Hooks` skips browser initialization for API, Appium mobile, file/database, and performance scenarios. API detection includes `@api`, `@api_*`, `@api-*`, `api` paths, and filenames containing `api`. Performance detection includes the explicit performance tags, tags beginning `@performance`, and only the real `features/performance/` location. Database/PDF/ZIP paths and their non-UI tags are also browserless. The special `@xml_ui` and `@csv_ui` tags are exceptions: they remain browser UI scenarios.[3]

A regular UI feature should therefore reside under `src/test/resources/features/` but outside the non-UI subfolders, and use a functional tag that does not match these browserless rules.

## Configuration files and key settings

### Regular UI configuration

The normal configuration file is [`src/test/resources/config/config.yml`](../../src/test/resources/config/config.yml). `ConfigurationProperties.getValue(...)` first looks under `environments.<value of -Denv, default QA>.<key>` and then falls back to the top-level key. This permits an environment-specific override without changing Java code.[15]

| Key | Consumed by | Source-backed behaviour |
|---|---|---|
| `browser` | `Hooks.createBrowserStack` | The regular hooks switch accepts `CHROME`, `FIREFOX`, `WEBKIT`, or `EDGE`; an unsupported normal browser value fails setup.[3] |
| `headless` | `BrowserFactory` | `-Dheadless=<true|false>` takes precedence. Otherwise the key is parsed as a boolean; a missing or blank value defaults to headed mode (`false`).[5] |
| `maximize_browser` or `maximizeBrowser` | `BrowserFactory` | For a headed desktop Chromium/Chrome/Edge launch, the factory uses `--start-maximized`. It does not apply that launch behaviour to Firefox, WebKit, headless operation, or a mobile profile.[5] |
| `ignoreHTTPSErrors` | `BrowserFactory` | The factory applies the setting to the browser context. For Chromium paths it also adds certificate-bypass launch arguments when true. Default is false when missing.[5] [15] |
| `time_to_wait_in_seconds` | `ActionPerformer` | Maximum wait for an element to become ready. If absent or invalid, the action dispatcher falls back to 30 seconds; zero or a negative parsed value means no extra action wait.[8] |
| `runtimeWait` | `Hooks` | Default page action and navigation timeout in seconds. A non-positive or invalid value falls back to 30 seconds.[3] |
| `videoCapture` | `BrowserFactory` | When true, desktop contexts record 1280×720 video. The output root is time-stamped under `test-output/captured-videos/`.[5] |
| `downloadDocument` | existing page download binding | `PageCommonSteps` reads this key in its `we click download on page …` binding. The lower-level download action treats the value it receives as an output directory.[12] [8] |
| `reporting.*` | `PerFeatureReportListener` | Controls per-feature HTML and PDF outputs. The default output paths used by the accessor are `test-output/per-feature-reports` and `test-output/per-feature-reports-glass` when the matching output key is absent.[15] [19] |
| `soft_assertions.enabled` and `soft_assertions.retry_seconds` | `PageCommonMethods`/`FrameCommonMethods` and `ActionPerformer` | With soft assertions enabled, a failed step is retried for the configured retry period, evidence is recorded, and the scenario fails at the end if failures remain. Element interaction waits use the shorter retry window; page-ready waits retain the full action timeout.[15] [8] |

Use a reviewed configuration that follows the actual key names. The example contains placeholders only and intentionally omits environment addresses and secrets.

```yaml
# src/test/resources/config/config.yml
browser: "chrome"
headless: "true"
maximize_browser: false
ignoreHTTPSErrors: "false"
time_to_wait_in_seconds: "<ACTION_WAIT_SECONDS>"
runtimeWait: <PAGE_TIMEOUT_SECONDS>
videoCapture: "true"
downloadDocument: "<APPROVED_DOWNLOAD_DIRECTORY>/"

# A feature’s navigation key may be any YAML key read through ConfigurationProperties.
target_url: "<APPROVED_TEST_ENVIRONMENT_URL>"

reporting:
  per_feature_reports_enabled: true
  per_feature_reports_output_dir: "test-output/per-feature-reports"
  per_feature_pdf_enabled: false
  per_feature_glass_pdf_enabled: false

soft_assertions:
  enabled: false
  retry_seconds: <RETRY_SECONDS>
```

### Reporting configuration

[`src/test/resources/extent.properties`](../../src/test/resources/extent.properties) configures the combined Extent adapter. It creates a timestamped base folder under `test-output/` and configures Spark HTML, Base64 HTML, PDF, and Excel reporter destinations beneath it. The normal TestNG runner additionally writes Cucumber pretty HTML, JSON, and rerun paths under `target/cucumber-reports/`.[20] [4]

### YAML locator loading

`YamlReader` scans and merges YAML from these regular resource roots: `elements`, `queries`, `api_requests`, `config`, and `performance`. Locator files must therefore be `.yml` or `.yaml` below `src/test/resources/elements/`. It reads `elements.<element-group>.<locator-key>`. Avoid duplicate top-level keys across files: values are merged and a later scalar value overwrites an earlier one.[16] [10]

## Build and exact execution commands

Run all commands from the repository root, `/home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23`.

```bash
# Download Playwright browser binaries when the machine has not been prepared.
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"

# Compile application and test code without executing scenarios.
mvn clean test-compile -DskipTests

# Run the normal TestNG suite configured in pom.xml and testng.xml.
mvn clean test

# Run the same normal suite while overriding only regular UI headless mode.
mvn clean test -Dheadless=true

# Build the project package after the normal default suite; Surefire has testFailureIgnore=true.
mvn clean install
```

`mvn clean test` is the exact normal-suite command because Surefire references `src/test/resources/testng.xml`; the suite names `com.ptaf.runner.TestRunner`; and that runner currently selects `@eStore`.[1] [4] The default Maven configuration is not a generic “run any tag” entry point. A newly created feature with a different tag will not be selected by that current runner until the runner’s Cucumber tag expression is intentionally changed through the normal reviewed implementation process. Do not claim a new feature has run merely because Maven completed with no selected scenarios.

The `ui_performance` command is intentionally different and must not be substituted for ordinary functional UI execution:

```bash
mvn clean test -Pui_performance
```

That profile changes Surefire to `src/test/resources/ui_performance/testng-ui_performance.xml`, disables normal TestNG method parallelism, and makes a performance failure fail the Maven run. It is documented only as a boundary, not as the way to run this guide’s features.[1] [2]

## Locator model and regular UI action patterns

### Locator YAML structure

Use a logical page/group name and a logical locator key. The group and key are what Gherkin passes to the common steps; the framework resolves them via `elements.<group>.<key>`.[10]

```yaml
# src/test/resources/elements/<feature>-elements.yml
elements:
  sample_page:
    username: "LABEL_<USERNAME_FIELD_LABEL>"
    password: "LABEL_<PASSWORD_FIELD_LABEL>"
    submit: "BUTTON_<SIGN_IN_BUTTON_NAME>"
    confirmation: "TESTID_<CONFIRMATION_TEST_ID>"
    result_row_action: "ROW_<ROW_ACCESSIBLE_NAME> > BUTTON_<ROW_ACTION_NAME>"
    body: "TAG_body"
```

A locator token is parsed at the first underscore or space into a type and value. `ElementActionImpl` splits a value containing `>` into a chain, creates the first locator from the page or frame, and resolves later terms relative to the previous locator.[9] [10]

| Locator form | Meaning in the current code |
|---|---|
| `CSS_<selector>`, `TAG_<selector>`, `XPATH_<selector>` | Passed to Playwright `locator(...)`. |
| `ID_<id>`, `NAME_<name>`, `CLASS_<class>` | Converted to `#id`, `[name='name']`, or `.class`. |
| `TEXT_<text>` | Resolves with `getByText(...)`. |
| `LABEL_<label>`, `PLACEHOLDER_<text>`, `TESTID_<id>`, `TITLE_<title>`, `ALTTEXT_<text>` | Resolves through the matching Playwright getter. |
| `BUTTON_<name>`, `LINKTEXT_<name>`, `TEXTBOX_<name>`, `CHECKBOX_<name>`, `RADIOBUTTON_<name>`, `DROPDOWN_<name>` | Resolves by ARIA role and accessible name. A type without a value is an unnamed role locator. |
| `ROW_<name> > BUTTON_<name>` | Resolves a row by role, then resolves a button relative to that row. |
| `ROLE_<role-name>` | Converts the value to a Playwright `AriaRole`; use a valid Playwright role name. |

`LocatorHandler` implements additional role names, including table/list/menu/tree/grid/landmark roles. Use the source as the definitive list when a special role is needed; unsupported locator types throw a descriptive error.[11]

### Actions available to feature authors

The usual page bindings in `PageCommonSteps` support click, double click, fill, select, check, hover, type, scroll, clear, visibility/checked/enabled/existence checks, text/value retrieval, value/text assertions, radio selection, screenshots, key presses, download, upload, and browser closure.[12] `NewPageCommonSteps` provides equivalent active-page actions after a popup/page switch and includes additional `not existed` and explicit value-verification forms.[13]

At the implementation layer, `ActionPerformer` also supports `selectmultiple`, `uncheck`, right-click, tap, input-value assignment, full-page screenshots, native maximize, focus/blur, drag, file selection, strict and optional downloads, DOM attribute/evaluation actions, waits, and validation operations. A feature should use a Cucumber binding that exists in the current step-definition class rather than assuming every dispatcher action has a public Gherkin phrase.[8]

> **Caution on the legacy page uncheck phrase:** `PageCommonSteps.weUncheckActionOnPage(...)` currently delegates to `pageCommonMethods.check(...)`, not `uncheck(...)`. Do not use `we uncheck on page …` to clear a selection. The active “new page” binding delegates to `FrameCommonMethods.uncheck(...)`, but a new feature should prefer reviewed, unambiguous step coverage rather than relying on the legacy phrase.[12] [13]

## Screenshots, videos, downloads, popups, and frames

### Screenshots

The explicit page step writes an element screenshot to `test-output/screenshots/<name>.png` and also attaches a screenshot to the Cucumber scenario when the page action succeeds. Use a simple, filesystem-safe name without path separators unless the required parent directory has been created. `ScreenshotHandler` captures full-page PNG bytes and attaches them to the scenario; it does not itself promise a standalone disk file for every failure.[12] [6] [17]

A normal fail-fast action error in `PageCommonMethods` records a full-page failure attachment and closes browser resources. With `soft_assertions.enabled: true`, the framework retries the step and records the soft failure while allowing later steps to execute; the reporting listener marks such steps failed in Extent output.[6] [15]

### Videos

Set `videoCapture: "true"` in the normal `config.yml` to record regular desktop contexts. `BrowserFactory` configures Playwright recording at 1280×720 under a time-stamped root. `Hooks` registers every page—including popup pages—so the handles are retained before a popup can close. After browser shutdown finalizes the recordings, the hook moves each file into a sanitized feature-title directory and allocates a collision-safe timestamped name.[5] [3] [18]

```text
test-output/captured-videos/<run-timestamp>/<Feature_Title>/
  <Feature_Title>_<unique-timestamp>.webm
```

This video behaviour belongs to the normal UI lifecycle. It is not the evidence policy or output structure of `ui_performance`.[2]

### Downloads and uploads

The strict `download` action waits for a Playwright download event caused by the target click. It creates a feature-title subdirectory below the supplied output root and saves a timestamped artifact with the browser-suggested extension. `download_optional` returns `null` rather than throwing if no download is emitted during the action timeout.[8] [18]

The existing Gherkin binding `we click download on page <group> locator <key>` reads `downloadDocument` and passes that configured value with a `.jpeg` suffix to the download facade. Treat this as an existing convention to verify in the target environment, not as a generic extension-selection API. The current upload binding is likewise an existing convenience step with a fixed source filename. For a new product workflow, do not place arbitrary local paths or client files in the feature; use approved non-sensitive fixture handling and reviewed step support.[12]

### Popups and frames

`FrameCommonSteps` supports `Given we navigate to <config-key> url` and a popup switch flow. `NewPageCommonSteps` also supports `Then we click <group> locator <key> and switch to popup`; it waits for the popup, waits for `DOMContentLoaded`, then calls `Hooks.setPage(...)` so subsequent “new page” steps use the popup.[14] [13]

`FrameCommonMethods` can resolve a target inside one, two, or three nested `FrameLocator` levels. The currently active generic frame bindings use predefined contexts in `NewPageCommonSteps` for Plaid (`iframe[title='Plaid Link']`), an AcceptUI popup frame, and an Atomic frame. Many older hard-coded frame-step declarations in `FrameCommonSteps` are commented out and are not available to a new feature. If a new application needs a different frame selector, that requires a reviewed step-definition implementation change; do not invent a Gherkin phrase and expect it to bind.[7] [13] [14]

## How to create a safe new regular UI feature

Create only the feature and its locator YAML when the existing common bindings cover the journey. Keep the feature declarative and keep environment URLs, selector strings, test data, and credentials out of it.

1. Add `<feature-name>.feature` under `src/test/resources/features/` or an appropriate **functional** subdirectory beneath it. Keep it outside `features/performance/`, `features/db/`, `features/pdf/`, and `features/mobile/`. Choose a unique functional tag such as `@ui_<feature>`; do not use a performance/API/mobile/file tag merely to label the work.[3]
2. Add `<feature-name>-elements.yml` under `src/test/resources/elements/` with an `elements:` root. Use meaningful group/key names and locator tokens supported by `LocatorHandler`. Prefer accessible role/label/test-id locators to brittle positional CSS or XPath where the application exposes them.[10] [11]
3. Add a **non-secret** navigation key to `src/test/resources/config/config.yml` or provide it through the framework’s environment-specific configuration structure. Reference only its key in Gherkin using `Given we navigate to <key> url`; the step calls `ConfigurationProperties.getBaseUrl(key)`.[14] [15]
4. Use existing bindings in `PageCommonSteps`, `NewPageCommonSteps`, or the active predefined-frame bindings. Do not add `@LastScenario` unless shared browser state is an explicit requirement. Do not add an explicit `we close all browsers` step to an ordinary isolated scenario; the hook cleans up automatically.[3] [12]
5. Confirm the runner-selection plan before treating the scenario as executable. The current default runner is fixed to `@eStore`. A distinct new tag must be selected through a reviewed runner change or other approved project execution configuration before it can run through the default TestNG suite.[4]
6. Start in a non-production target with placeholder-safe or approved synthetic data. Inspect the generated reports and evidence, then promote the feature only after locator stability and teardown behaviour are confirmed.

### Tested-style feature example

The syntax below matches current navigation and page-step bindings. It is a template only: every value is a placeholder, and no real endpoint, identity, credential, client value, or internal selector is shown.

```gherkin
@ui_sample_feature
Feature: <Feature_Name>

  Background: Open the approved test page
    Given we navigate to target_url url
    Then get title of page

  Scenario: A user submits the approved form
    Then we enter value on page sample_page locator username value "<TEST_USERNAME>"
    And we enter value on page sample_page locator password value "<TEST_PASSWORD>"
    And we click on page sample_page locator submit
    Then we verify on page sample_page of locator confirmation is visible
    And we capture screenshot on page sample_page locator confirmation name "<feature>_confirmation"
```

This pattern uses the feature’s configuration key, logical locator group/key names, reusable page actions, a visible outcome, and deliberate evidence. Avoid `Thread.sleep` helpers such as `we wait for some time` or `time out for <n> seconds` when a specific visible state can be asserted; those phrases exist but block the executing thread.[12]

## Feature, data, locator, payload, and query locations

| Resource type | Repository location | Regular UI usage and boundary |
|---|---|---|
| Functional features | [`src/test/resources/features/`](../../src/test/resources/features/) | Primary home for normal Cucumber UI features. Existing examples include [`google.feature`](../../src/test/resources/features/google.feature) and [`secondPageTest.feature`](../../src/test/resources/features/secondPageTest.feature).[21] [22] |
| UI locators | [`src/test/resources/elements/`](../../src/test/resources/elements/) | YAML locator repository. Use the `elements.<group>.<key>` structure; examples include [`homepage.yml`](../../src/test/resources/elements/homepage.yml) and [`google.yml`](../../src/test/resources/elements/google.yml).[10] [23] |
| Normal configuration | [`src/test/resources/config/config.yml`](../../src/test/resources/config/config.yml) | Browser settings, environment keys, download location, reporting settings, and soft-assertion settings. Do not commit private endpoint values or secrets.[15] |
| Generic data fixtures | [`src/test/resources/data/`](../../src/test/resources/data/) | Contains reusable data files such as CSV/XML examples. The normal page steps in scope do not automatically bind a CSV row to the sample feature; use only supported, approved data mechanisms.[24] |
| API request definitions | [`src/test/resources/api_requests/api_requests.yml`](../../src/test/resources/api_requests/api_requests.yml) | API-module resource. It is loaded by `YamlReader`, but it is not a normal UI locator or feature-data store.[16] [25] |
| Database queries | [`src/test/resources/queries/db_queries.yml`](../../src/test/resources/queries/db_queries.yml) | Database-module resource. It is loaded by `YamlReader`, but normal UI scenarios should not be reclassified as `@db` merely to access a query.[16] [26] |
| Performance payloads | [`src/test/resources/performance/payloads/`](../../src/test/resources/performance/payloads/) | Performance-module resources; not part of the regular UI browser lifecycle. |
| UI-performance resources | [`src/test/resources/ui_performance/`](../../src/test/resources/ui_performance/) | Dedicated performance configuration, CSV data, locators, features, and TestNG suites. They are not inputs to `BrowserFactory`, normal `Hooks`, or normal UI feature execution.[2] |

## Expected reports and artifacts

| Artifact | Expected location | Conditions and interpretation |
|---|---|---|
| Surefire TestNG results | `target/surefire-reports/` | Generated by Surefire for the default suite.[1] |
| Cucumber pretty/JSON/rerun outputs | `target/cucumber-reports/cucumber-pretty`, `target/cucumber-reports/CucumberTestReport.json`, and `target/cucumber-reports/rerun.txt` | Configured in `com.ptaf.runner.TestRunner`. Surefire also sets Cucumber JSON/XML plugin outputs under `target/cucumber-reports/`.[4] [1] |
| Combined Extent reports | `test-output/<run-timestamp>/SparkReport/Spark.html`, `Base64Report/Report.html`, `PdfReport/FNB-PTAF-Report.pdf`, and `ExcelReport/FNB-PTAF-Report.xlsx` | Controlled by `extent.properties`; the timestamped base folder prevents normal Extent run-folder overwrites.[20] |
| Per-feature report outputs | configured `reporting.per_feature_reports_output_dir` and `reporting.per_feature_glass_pdf_output_dir` | Produced only when the relevant reporting flags are enabled. The listener names reports after the declared feature title and records embedded screenshots.[15] [19] |
| Explicit element screenshots | `test-output/screenshots/<name>.png` | Produced by the existing page/new-page capture screenshot steps. The same action also attaches evidence to the Cucumber scenario.[12] [13] |
| Failure screenshot attachment | attached to Cucumber/Extent evidence | The normal fail-fast action handler uses `ScreenshotHandler` for a full-page PNG attachment. Treat it as report evidence, not a guaranteed standalone file.[6] [17] |
| Feature-grouped videos | `test-output/captured-videos/<run-timestamp>/<Feature_Title>/<Feature_Title>_<unique-timestamp>.webm` | Produced only when `videoCapture` is true and browser shutdown finalizes recordings.[5] [3] [18] |
| Feature-grouped downloads | `<configured-download-root>/<Feature_Title>/<Feature_Title>_<unique-timestamp>.<extension>` | Produced by the low-level strict/optional download action after a download event. The declared feature title replaces the source filename stem; the suggested extension is preserved.[8] [18] |

## Troubleshooting

| Symptom | Likely source-backed cause | Check and corrective action |
|---|---|---|
| Maven completes but the new feature did not run | Default `com.ptaf.runner.TestRunner` currently filters on `@eStore`. | Check the runner’s `tags` annotation and the feature tag. Establish an approved selection change before execution; do not interpret a zero-selection run as validation.[4] |
| `The page is closed or not initialized` | Hooks were not discovered, browser setup was skipped as non-UI, or the browser was already closed. | Confirm the runner glue includes `com.ptaf.hooks`; confirm the feature is not classified by a non-UI tag/path; inspect the first preceding failure.[3] [4] |
| `Unsupported browser type` during setup | The normal hooks’ browser switch only recognises Chrome, Firefox, WebKit, and Edge. | Correct the regular `browser` value to one of those values; use the dedicated mobile-browser facilities for mobile emulation rather than the regular desktop lifecycle.[3] [5] |
| Headless setting seems ignored | A JVM `-Dheadless` property has precedence over YAML. | Inspect the Maven/CI command and remove or correct the overriding property. A blank/missing YAML key defaults to headed mode.[5] |
| Element locator cannot be found or type is unknown | YAML path, locator group/key, `TYPE_value` token, or chain is malformed. | Verify `elements.<group>.<key>`, the file is beneath `src/test/resources/elements`, and the type is implemented by `LocatorHandler`. The resolver logs the failing YAML path and locator type context.[10] [11] [16] |
| A chained locator matches the wrong scope | The `>` chain resolves each subsequent locator relative to the preceding locator. | Make the first segment a stable container such as `ROW_<name>` and ensure every later segment is intended to be a child of that result.[9] [11] |
| The test waits longer or shorter than expected | `runtimeWait` applies Page defaults; `time_to_wait_in_seconds` applies dispatcher element waits; soft assertions can override element interaction waits. | Use the right setting for the problem. Keep hard sleeps out of normal scenarios when an explicit condition can be asserted.[3] [8] [15] |
| Video is absent or named unexpectedly | Recording is disabled, a page video was never finalized, or the feature title was sanitized for a path. | Set `videoCapture: "true"`, allow the hook to close the browser, and inspect `test-output/captured-videos/<run-timestamp>/`. Popups are intentionally collected as separate recordings under the same feature directory.[5] [3] [18] |
| Download fails immediately | Strict `download` expects a download event from the click. | Verify that the locator click produces a browser download, that the output directory is writable, and that the target is not rendering the document inline. Use optional-download behaviour only through approved existing support.[8] |
| Browser state leaks or later scenarios fail in an `@LastScenario` feature | The feature deliberately retains a shared browser and propagates unexpected shared-session failure. | Remove `@LastScenario` for independent tests. If shared state is required, preserve the intended ordering and use the explicit close mechanism only when a clean browser for a later scenario is intended.[3] [12] |
| Normal UI behaves like performance or no browser starts because a workspace name contains “performance” | The hook intentionally checks the **feature path**, not the enclosing workspace path, for performance classification. | Place an ordinary feature under the normal features root and inspect actual source tags/path; do not rely on the repository directory name as a classifier.[3] |

## Module boundaries

| Module | Relationship to regular UI automation |
|---|---|
| **`ui_performance`** | Isolated real-browser performance module. Its Maven profile replaces the Surefire suite, its `UiPerformanceRunner` and `com.ptaf.ui_performance.stepdefinitions` own execution, and its resources live under `src/test/resources/ui_performance/`. It does **not** use normal `Hooks`, `BrowserFactory`, ordinary locator files, normal feature data, `@LastScenario`, or normal functional reports.[1] [2] |
| **API and database automation** | Use their own step definitions and `api_requests`/`queries` resources. Normal hooks intentionally avoid starting a Playwright browser for API and database scenarios.[3] [16] |
| **JMeter/API performance** | The `performance` resource subtree and performance tags are browserless to regular hooks. Do not store regular UI payloads in this area or tag a functional browser scenario as performance.[3] [16] |
| **Mobile/Appium automation** | Native and real mobile browser tags are handled by `MobileHooks` and are browserless to the regular desktop hook. Playwright mobile-browser profile support is a separate facility and is not the regular desktop `Hooks.createBrowserStack` browser switch.[3] [5] |
| **Reporting and soft assertions** | Shared cross-cutting infrastructure. The ordinary UI runner registers `PerFeatureReportListener` and `SoftAssertionReportListener`; reporting configuration applies beyond UI but its browser screenshots/videos retain the normal UI lifecycle described here.[4] [15] [19] |

## References

<!-- Visible source-reference list -->
The sources below are visible and clickable in Markdown preview. Citation labels used in this guide point to the same source files.

- **[1]** [Maven build, Surefire configuration, and UI performance profile](../../pom.xml) — `../../pom.xml`
- **[2]** [Canonical FNB-ETAF UI performance load-testing guide](10-ui-performance-load-testing.md) — `10-ui-performance-load-testing.md`
- **[3]** [Regular browser lifecycle and browserless scenario classification](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`
- **[4]** [Default Cucumber TestNG runner for regular UI execution](../../src/test/java/com/ptaf/runner/TestRunner.java) — `../../src/test/java/com/ptaf/runner/TestRunner.java`
- **[5]** [Playwright browser and browser-context factory](../../src/main/java/com/ptaf/utils/BrowserFactory.java) — `../../src/main/java/com/ptaf/utils/BrowserFactory.java`
- **[6]** [Regular page interaction and failure handling facade](../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java) — `../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java`
- **[7]** [Regular nested-frame interaction facade](../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java) — `../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java`
- **[8]** [Playwright action dispatcher, waits, downloads, and screenshots](../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) — `../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java`
- **[9]** [YAML locator chaining and action routing](../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java) — `../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java`
- **[10]** [YAML element-path and locator token parser](../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java) — `../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java`
- **[11]** [Locator type to Playwright locator mapping](../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java) — `../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java`
- **[12]** [Regular top-level page Cucumber step definitions](../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`
- **[13]** [Popup, new-page, and predefined frame Cucumber step definitions](../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java`
- **[14]** [Navigation and popup frame Cucumber step definitions](../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java`
- **[15]** [Configuration accessors, environment override logic, reporting, and soft assertion settings](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — `../../src/main/java/com/ptaf/utils/ConfigurationProperties.java`
- **[16]** [Merged YAML resource-folder loader](../../src/main/java/com/ptaf/utils/YamlReader.java) — `../../src/main/java/com/ptaf/utils/YamlReader.java`
- **[17]** [Cucumber screenshot attachment handler](../../src/main/java/com/ptaf/utils/ScreenshotHandler.java) — `../../src/main/java/com/ptaf/utils/ScreenshotHandler.java`
- **[18]** [Feature-title artifact naming and directory grouping](../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) — `../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`
- **[19]** [Per-feature Extent report listener](../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) — `../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`
- **[20]** [Timestamped Extent report output configuration](../../src/test/resources/extent.properties) — `../../src/test/resources/extent.properties`
- **[21]** [Regular UI feature example](../../src/test/resources/features/google.feature) — `../../src/test/resources/features/google.feature`
- **[22]** [Regular UI popup, nested-frame, screenshot, and download example](../../src/test/resources/features/secondPageTest.feature) — `../../src/test/resources/features/secondPageTest.feature`
- **[23]** [Regular UI locator YAML example](../../src/test/resources/elements/homepage.yml) — `../../src/test/resources/elements/homepage.yml`
- **[24]** [Generic data resource directory](../../src/test/resources/data/) — `../../src/test/resources/data/`
- **[25]** [API request definition resource](../../src/test/resources/api_requests/api_requests.yml) — `../../src/test/resources/api_requests/api_requests.yml`
- **[26]** [Database query definition resource](../../src/test/resources/queries/db_queries.yml) — `../../src/test/resources/queries/db_queries.yml`

<!-- Internal citation definitions used by the in-text [n] links. Keep these definitions so citations remain clickable. -->
[1]: ../../pom.xml "Maven build, Surefire configuration, and UI performance profile"
[2]: 10-ui-performance-load-testing.md "Canonical FNB-ETAF UI performance load-testing guide"
[3]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Regular browser lifecycle and browserless scenario classification"
[4]: ../../src/test/java/com/ptaf/runner/TestRunner.java "Default Cucumber TestNG runner for regular UI execution"
[5]: ../../src/main/java/com/ptaf/utils/BrowserFactory.java "Playwright browser and browser-context factory"
[6]: ../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java "Regular page interaction and failure handling facade"
[7]: ../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java "Regular nested-frame interaction facade"
[8]: ../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "Playwright action dispatcher, waits, downloads, and screenshots"
[9]: ../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java "YAML locator chaining and action routing"
[10]: ../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java "YAML element-path and locator token parser"
[11]: ../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java "Locator type to Playwright locator mapping"
[12]: ../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java "Regular top-level page Cucumber step definitions"
[13]: ../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java "Popup, new-page, and predefined frame Cucumber step definitions"
[14]: ../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java "Navigation and popup frame Cucumber step definitions"
[15]: ../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Configuration accessors, environment override logic, reporting, and soft assertion settings"
[16]: ../../src/main/java/com/ptaf/utils/YamlReader.java "Merged YAML resource-folder loader"
[17]: ../../src/main/java/com/ptaf/utils/ScreenshotHandler.java "Cucumber screenshot attachment handler"
[18]: ../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-title artifact naming and directory grouping"
[19]: ../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature Extent report listener"
[20]: ../../src/test/resources/extent.properties "Timestamped Extent report output configuration"
[21]: ../../src/test/resources/features/google.feature "Regular UI feature example"
[22]: ../../src/test/resources/features/secondPageTest.feature "Regular UI popup, nested-frame, screenshot, and download example"
[23]: ../../src/test/resources/elements/homepage.yml "Regular UI locator YAML example"
[24]: ../../src/test/resources/data/ "Generic data resource directory"
[25]: ../../src/test/resources/api_requests/api_requests.yml "API request definition resource"
[26]: ../../src/test/resources/queries/db_queries.yml "Database query definition resource"
