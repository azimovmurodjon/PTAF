# Configuration, YAML, Environment, Locator, and Data Source Reference

This chapter is the operational reference for the configuration and externalized test data that the current FNB-ETAF source actually reads. It covers the shared `config.yml` store, the distinct YAML readers used by mobile and performance modules, locator syntax, input-data readers, runtime overrides, and safe editing practices.

> **Scope boundary.** This is a source reference, not a proposal for a new configuration model. A key, fallback, command, and behavior is described only where it is present in the current repository sources or resources. Example values below are deliberately placeholders; do not place real credentials, production URLs, tokens, customer data, or personal data in feature files or YAML.

## 1. Configuration map and execution flow

### 1.1 Where configuration lives

Most editable resources live below `src/test/resources`; Java loads them from the test runtime classpath. The framework does **not** use one universal reader for every YAML file. The reader and resource family must match, as shown below.

| Resource family | Editable source location | Reader / consumer | Purpose and boundary |
|---|---|---|---|
| Shared framework configuration | [`src/test/resources/config/config.yml`](../../../src/test/resources/config/config.yml) | `YamlReader` through `ConfigurationProperties` | Browser, wait, download, database, API service, reporting, soft-assertion, and ZIP settings. |
| Desktop web element catalogues | [`src/test/resources/elements/`](../../../src/test/resources/elements/) | `YamlReader` and `ElementLocatorHelper` | `elements.<page>.<key>` locator definitions for normal Playwright UI automation. |
| API request catalogue | [`src/test/resources/api_requests/api_requests.yml`](../../../src/test/resources/api_requests/api_requests.yml) | `YamlReader`, `ApiActionImpl` | Named HTTP method/endpoint templates; service base URLs remain in `config.yml`. |
| SQL query catalogue | [`src/test/resources/queries/db_queries.yml`](../../../src/test/resources/queries/db_queries.yml) | `YamlReader`, `DatabaseActionImpl` | Named SQL statements used with prepared-statement parameters. |
| JMeter-style performance configuration and YAML payloads | [`src/test/resources/performance/`](../../../src/test/resources/performance/) | `PerformanceYamlReader` for its config; `YamlReader` for payload lookup | The dedicated reader loads **only** `performance/config/performance-config.yml`; the generic reader also scans the whole `performance` folder, allowing `performance.payloads.*` payload lookup. |
| Appium native/mobile configuration and elements | [`src/test/resources/mobile/config/`](../../../src/test/resources/mobile/config/) and [`mobile/elements/`](../../../src/test/resources/mobile/elements/) | `MobileYamlReader` | Intentionally restricted to these two directories so bundled mobile app/vendor YAML is not parsed. |
| Playwright mobile-browser emulation | [`src/test/resources/mobile_browser/config/`](../../../src/test/resources/mobile_browser/config/) | `MobileBrowserYamlReader` | Separate profiles and execution controls for responsive/device emulation, not native simulators. |
| Isolated real-browser UI performance | [`src/test/resources/ui_performance/`](../../../src/test/resources/ui_performance/) | `UiPerformanceYamlReader`, `UiPerformanceLocatorRepository`, `UiPerformanceUserDataReader` | A private configuration, locator, and CSV domain that does not merge with normal UI, API, DB, mobile, or JMeter performance YAML. |

The generic [`YamlReader`](../../../src/main/java/com/ptaf/utils/YamlReader.java) initializes once on first use. It walks only the classpath directories `elements`, `queries`, `api_requests`, `config`, and `performance`; it parses `.yml` and `.yaml` files, recursively merges YAML maps into one in-memory map, and resolves dot-separated paths such as `api_services.<service>.base_url`. A missing configured directory is informational. A bad YAML file is reported to `stderr` with folder, file, exception, and message, but the loader continues to the next file. A missing key or a scalar encountered where a map is needed returns `null` and emits an “exact why” diagnostic unless a caller suppresses it. [1]

### 1.2 Generic YAML lookup lifecycle

```text
First call to YamlReader / ConfigurationProperties / normal locator code
  -> static YamlReader initialization
  -> scan elements, queries, api_requests, config, performance on the classpath
  -> SnakeYAML parses each eligible file
  -> nested maps merge into one global in-memory map
  -> caller requests a dotted path
  -> map traversal returns scalar/map/list, or null on a missing/invalid segment
```

**Merge rule:** if both old and incoming values at a key are maps, their children are merged; otherwise the incoming value replaces the old one. This is useful for multiple files contributing under a root such as `elements`, but it means duplicate scalar paths can overwrite each other. The code walks files without an explicit sort, so do **not** intentionally create duplicate scalar keys and rely on a particular cross-file winner. [1]

**Reload rule:** all readers in this chapter load static data at class initialization. Edit the resource, then start a new JVM/test run; no source-visible file watcher or reload API refreshes an already initialized reader. [1][11][14][17]

### 1.3 `ConfigurationProperties`: the shared access gateway

[`ConfigurationProperties`](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) converts selected shared YAML values to strings and supplies typed convenience accessors. `getValue(key)` is the important common path:

1. Read Java system property `env`; default to `QA`.
2. Try `environments.<env>.<key>` in the generic YAML store, with missing-key diagnostics suppressed.
3. If no environment-specific value exists, read the ordinary `<key>` path.
4. Return `null` if neither exists; individual consumers may apply their own default or fail.

This is a **JVM system-property** overlay (`-Denv=<name>`), not an operating-system environment-variable lookup. The current `config.yml` contains no `environments:` block, so its presently effective values come from the ordinary keys unless a maintainer adds that block. Raw callers of `YamlReader.get(...)` do not get this environment overlay; for example, normal locator and query resolution use raw YAML lookup. [2]

A safe shape for an approved environment overlay is:

```yaml
# Add only approved, non-secret values.
environments:
  <ENVIRONMENT_NAME>:
    headless: "true"
    api_services:
      <service_name>:
        base_url: "https://<approved-non-production-host>"
```

Run it with a matching JVM property:

```bash
mvn test -Denv=<ENVIRONMENT_NAME>
```

The overlay must preserve the same nested path as the base key. For example, `api_services.<service>.base_url` belongs below `environments.<ENVIRONMENT_NAME>.api_services.<service>.base_url`, not below a differently named root. [2]

## 2. Shared `config/config.yml` key catalogue

The tables in this section catalogue keys that currently exist in [`config.yml`](../../../src/test/resources/config/config.yml). “Fallback / interpretation” names source-visible behavior, not a recommended setting. Values are intentionally not repeated from the checked-in file when they could be environment-specific.

### 2.1 UI, waits, files, and target aliases

| Key | Current consumer and responsibility | Fallback / operational detail |
|---|---|---|
| `browser` | `Hooks` reads it through `ConfigurationProperties.getBrowser()` to select a desktop browser or a configured Playwright mobile-browser profile. [2][6] | The browser factory supports desktop Chrome, Firefox, WebKit, and Edge. A matching mobile profile name is resolved separately. An unsupported name fails. |
| `maximize_browser` | `BrowserFactory` reads this key; it also accepts legacy camel-case `maximizeBrowser` when the snake-case key is blank. [6] | `true` adds `--start-maximized` only for headed desktop Chromium/Chrome/Edge. It is ignored for a mobile profile and cannot maximize a headless window; Firefox/WebKit follow the non-Chromium path. |
| `headless` | Browser launch mode for normal UI runs. [2][6] | **Special precedence:** nonblank `-Dheadless=<true|false>` wins; otherwise use the environment-aware shared key; otherwise default to `false`. Boolean parsing treats only `true` (case-insensitively) as true. |
| `ignoreHTTPSErrors` | Applies to normal browser contexts and API request contexts. Chromium launch also adds its SSL-bypass arguments when enabled. [2][6][7] | `ConfigurationProperties` defaults a missing/blank value to string `"false"`; consumers parse that as false. Use only in approved non-production test environments. |
| `time_to_wait_in_seconds` | `ActionPerformer` converts it to an action wait in milliseconds. [23] | Missing/malformed values fall back to 30,000 ms; zero/negative becomes zero (no extra wait). The normal UI element wait is a maximum, not a fixed sleep. |
| `runtimeWait` | `Hooks` sets Playwright default action and navigation timeouts from this value in seconds. [22] | Missing, invalid, zero, or negative values become 30 seconds in `Hooks`. This is distinct from `time_to_wait_in_seconds`. |
| `videoCapture` | Enables normal UI Playwright video recording in `BrowserFactory`. [2][6] | When true, desktop video goes under `test-output/captured-videos/<timestamp>/`; video finalization occurs when the context/browser closes. |
| `excelDocumentLocation` | Exposed by `ConfigurationProperties.getExcelDocumentLocation()`. [2] | The key exists, but a direct runtime caller of that accessor was not found in current Java sources. Performance Excel steps instead accept the file path in Gherkin. Treat this as a conventional project path, not a guaranteed global data source. |
| `downloadDocument` | Download Gherkin steps read it before delegating to the page helper. [24] | The current download step appends `.jpeg` to the configured string. The current upload example reads the key but passes a separately hard-coded filename, so it is not a general upload-file selector. |
| `tool_qa_url` | Present as a top-level URL alias in `config.yml`. [3] | No direct Java reference to this exact key was found. Do not assume a generic “navigate to application” step uses it without checking the relevant feature/step implementation. |
| `HARNESS_PREPROD_STAGE` | Present as a top-level URL alias in `config.yml`. [3] | No direct Java reference to this exact key was found. Keep any environment-specific URL approved and non-sensitive; do not copy it into documentation or public test data. |

> **Source-visible discrepancy — timeout compatibility.** `ConfigurationProperties.getRuntimeTimeoutMillis()` recognizes a legacy `runtimeTimeoutMillis` key and otherwise converts `runtimeWait` seconds to milliseconds. However, the current `Hooks` browser setup reads `runtimeWait` directly and does not call that helper. Therefore, adding only `runtimeTimeoutMillis` will not change the normal `Hooks` timeout path; `runtimeWait` is the operative shared key for that path. [2][22]

### 2.2 Database configuration

| Key | Current consumer and responsibility | Fallback / validation |
|---|---|---|
| `database.db_type` | `DatabaseHandler` selects the database implementation. [8] | Defaults to `sqlserver`; any other value currently throws because only SQL Server is supported. |
| `database.server_type` | `DatabaseConnectionValidator` logs it for diagnostic visibility. [8] | It is not used to construct the JDBC URL. |
| `database.server_name` | SQL Server host/server for JDBC URL construction. [8] | Required; a blank/missing value throws before connecting. |
| `database.port` | JDBC port. [8] | Defaults to `1433` when blank/missing. |
| `database.database_name` | Database name in the JDBC URL. [8] | Required; a blank/missing value throws before connecting. |
| `database.authentication` | Chooses `windows` or `sqlserver` authentication. [8] | Defaults to `windows`; another value throws. Windows mode adds `integratedSecurity=true` and relies on the runner account and host setup. |
| `database.encrypt` | JDBC encryption parameter. [8] | Defaults to `true` if blank/missing. |
| `database.trust_server_certificate` | JDBC trust-certificate parameter. [8] | Defaults to `true` if blank/missing. This is a transport setting, not a substitute for environment approval. |
| `database.login_timeout_seconds` | JDBC login timeout parameter. [8] | Defaults to `30` if blank/missing. |
| `database.query_timeout_seconds` | `DatabaseActionPerformer` calls `PreparedStatement.setQueryTimeout`. [25] | Parsed by the performer’s integer helper; its source-defined default applies when missing/invalid. A timeout is logged as guidance to review this key. |
| `database.fetch_size` | `DatabaseActionPerformer` calls `PreparedStatement.setFetchSize`. [25] | Parsed by the performer’s integer helper; its source-defined default applies when missing/invalid. |
| `database.application_name` | JDBC `applicationName` parameter. [8] | Defaults to `PTAF Automation Framework` if blank/missing. |
| `database.username` | SQL Server username, used only for `authentication: sqlserver`. [8] | Required in SQL Server-auth mode; it is not needed for Windows integrated authentication. |
| `database.password_env_variable` | **Name** of the OS environment variable holding the SQL Server password. [8] | Required in SQL Server-auth mode. `DatabaseHandler` calls `System.getenv` using this name and fails fast if it is absent/blank. The password itself must never be committed. |

The handler keeps one JDBC connection per thread and closes it through framework teardown. SQL query text is not kept in `config.yml`; reference a key in `queries/db_queries.yml`, whose `?` placeholders are bound by JDBC prepared statements. [8][13]

### 2.3 API services and request templates

| Key / path | Current consumer and responsibility | Fallback / validation |
|---|---|---|
| `api_services.<service>.base_url` | `ApiRequestHandler` uses it as the Playwright API request context base URL. [7] | Required and nonblank for the requested service; missing values throw before context creation. |
| `api_services.<service>.auth_token_env` | Names an OS environment variable from which the handler reads a bearer token. [7] | Blank/absent means no authorization header is added. A nonblank name whose OS variable is missing/blank causes a fail-fast exception. |
| `api_requests.<request>.method` | `ApiActionImpl` reads the configured HTTP method. [12] | Required with `endpoint`; an incomplete key produces an error that names the request definition catalogue. |
| `api_requests.<request>.endpoint` | `ApiActionImpl` reads an endpoint template, including `{placeholder}` parts. [12] | Path parameters set in earlier Gherkin steps are handed to the API performer for substitution. Request state is cleared after send, per thread. |

Safe API example:

```gherkin
Given I set the path parameter "recordId" to "<non-sensitive-test-id>"
When I send a "<request_group>.<request_name>" request to the "<service_name>" service
Then the response code should be 200
```

Keep reusable `method` and relative `endpoint` data in `api_requests.yml`; keep the base URL and the **environment-variable name** (not token value) in `config.yml`. This separation is enforced by the request handler’s configuration lookup. [7][12]

### 2.4 Reporting, soft assertions, and ZIP extraction

| Key | Current consumer and responsibility | Fallback / generated artifact |
|---|---|---|
| `reporting.per_feature_reports_enabled` | Enables the per-feature Extent listener. [2][19] | Defaults to `false`. The combined report is outside this switch’s control. |
| `reporting.per_feature_reports_output_dir` | Output directory for one timestamped HTML report per feature. [2][19] | Defaults to `test-output/per-feature-reports`; directories are created when possible. |
| `reporting.per_feature_pdf_enabled` | Enables the listener’s direct per-feature PDF. [2][19] | Defaults to `false`; effective only when per-feature reports are enabled. |
| `reporting.per_feature_glass_pdf_enabled` | Enables a separate Glass-style PDF subprocess path. [2][19] | Defaults to `false`; effective only when per-feature reports are enabled. |
| `reporting.per_feature_glass_pdf_output_dir` | Separate directory for Glass-style per-feature PDFs. [2][19] | Defaults to `test-output/per-feature-reports-glass`. Feature names are sanitized and timestamped in output filenames. |
| `soft_assertions.enabled` | Enables continue-on-failure behavior recorded for scenario-end failure reporting. [2] | Defaults to `false`; normal fail-fast behavior remains when false. Session/application crashes still have their own failure path. |
| `soft_assertions.retry_seconds` | Retry window for a failed soft assertion step. [2] | Defaults to 3 seconds; accessor clamps valid parsed values to 1–60 seconds and falls back to 3 on invalid text. |
| `zip.extraction_dir` | Base directory used by ZIP handling. [2] | Defaults to `test-output/extracted`; each archive is extracted into a directory named from the archive. |
| `zip.cleanup_after_scenario` | Whether ZIP extraction is removed after the scenario. [2] | Defaults to `true`; set false only for short-lived debugging evidence. |
| `zip.recursive_unzip` | Whether nested `.zip` files are recursively extracted. [2] | Defaults to `true`. |

## 3. Environment, command-line, and secret precedence

There is no single framework-wide “highest precedence” rule. Precedence depends on the consumer. The following table separates source-confirmed behaviors from assumptions.

| Concern | Source-confirmed precedence | Safe command/example |
|---|---|---|
| General shared configuration accessed through `ConfigurationProperties.getValue` | `environments.<-Denv value or QA>.<key>` → base `<key>` → caller-specific fallback/error. [2] | `mvn test -Denv=<ENVIRONMENT_NAME>` |
| Normal UI headless mode | `-Dheadless` when nonblank → shared environment-aware `headless` → `false`. [6] | `mvn test -Dheadless=true` |
| Native mobile platform | `-Dmobile.platform=android|ios` → scenario `@android` / `@ios` tag → `mobile.default_platform`. Conflicting platform tags fail. [20] | `mvn test -Dmobile.platform=android` |
| UI performance YAML resource | `-Dui.performance.config=<classpath-resource>` → `ui_performance/config/ui_performance-config.yml`. The override must resolve on the classpath. [15] | `mvn clean test -Pui_performance -Dui.performance.config=ui_performance/config/<approved-config>.yml` |
| API bearer token | `api_services.<service>.auth_token_env` supplies an **environment-variable name**; `System.getenv(name)` supplies the secret. [7] | Export/inject `<TOKEN_ENV_NAME>` only in the approved runner environment. |
| DB password | `database.password_env_variable` supplies an **environment-variable name**; `System.getenv(name)` supplies the secret in SQL Server-auth mode. [8] | Export/inject `<DB_PASSWORD_ENV_NAME>` only in the approved runner environment. |
| HTTP Basic credentials for normal browser contexts | Both JVM properties `service.username` and `service.password` must be nonblank before the browser context receives HTTP credentials. [6] | Prefer CI secret injection; do not put the actual properties into shell history, checked-in scripts, or feature files. |
| Chromium CI launch behavior | A nonblank OS `CI` variable adds `--no-sandbox` and `--disable-setuid-sandbox`. [6] | This is launch behavior only; it is not a configuration overlay. |

### 3.1 Secret-safe editing and execution

Use the following rules across all YAML families:

1. **Store a variable name, not a secret.** `auth_token_env` and `password_env_variable` are designed for the environment variable’s name. The consumers fail fast when the referenced secret is unavailable. [7][8]
2. **Do not put passwords, API tokens, cookies, authorization headers, private URLs, customer data, or live credentials** in YAML, feature files, CSV, Excel, report names, or documentation examples.
3. **Prefer an approved secret manager / CI secret injection.** If local testing is approved, set the variable in the local process environment and avoid printing it. The framework logs the configured variable name for API auth, not the token value. [7]
4. **Treat UI performance user CSV as sensitive test input.** The reader states that values are not logged or written into reports, but the local CSV itself still needs repository and access control appropriate to its contents. [16][18]
5. **Use a sanitized, approved target for load testing.** UI performance refuses an unapproved malformed target and reports only the host in its safe reporting map; that does not remove the need for test-environment approval. [15][18]

## 4. YAML family reference

### 4.1 Normal UI, API, queries, and generic performance payloads

#### Desktop elements: `elements.<page>.<key>`

Each file in [`src/test/resources/elements/`](../../../src/test/resources/elements/) contributes beneath the top-level `elements` map. Normal UI code obtains a value as `elements.<page>.<key>`; a missing value logs the attempted full path and throws `IllegalArgumentException`. [1][4]

```yaml
# src/test/resources/elements/<page>.yml
# Use a stable, non-sensitive logical page and key name.
elements:
  <page_key>:
    <save_button>: "Button_Save"
    <reference_field>: "TESTID_reference-input"
```

#### API request definitions

`api_requests.yml` uses logical groups, where each request has a `method` and `endpoint`. Endpoint placeholders use `{placeholder}` syntax and are paired with Gherkin path parameters. [3][12]

```yaml
<request_group>:
  get_record:
    method: "GET"
    endpoint: "/records/{recordId}"
```

#### SQL query definitions

`db_queries.yml` groups reusable SQL under logical keys. Parameterize values with JDBC `?` placeholders rather than string concatenation. `DatabaseActionImpl` retrieves the selected key through the generic YAML reader and delegates execution to prepared statements. [3][13]

```yaml
records:
  find_by_id: "SELECT status FROM records WHERE record_id = ?"
```

```gherkin
Then I verify the database contains a record for query "records.find_by_id" with parameters "<non-sensitive-id>"
```

The database step parser recognizes comma-separated values as booleans, integers/longs, decimals, `null`, or strings. Do not pass values containing commas through that step’s simple comma-separated parameter format without reviewing the step implementation. [21]

#### JMeter-style performance YAML

[`PerformanceYamlReader`](../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java) loads exactly one classpath resource: [`performance/config/performance-config.yml`](../../../src/test/resources/performance/config/performance-config.yml). It fails during initialization if that file is missing or empty. Its numeric helpers return a supplied default when a key is missing but propagate `NumberFormatException` when a present numeric value is malformed. [9]

| Confirmed path | Meaning |
|---|---|
| `performance.defaults.protocol` | Target protocol. |
| `performance.defaults.host` | Target host. |
| `performance.defaults.port` | Target port; accessor fallback is 443. |
| `performance.defaults.users` | Default virtual user count; accessor fallback is 1. |
| `performance.defaults.rampUpSeconds` | Default ramp duration; fallback 1. |
| `performance.defaults.holdSeconds` | Default hold duration; fallback 1. |
| `performance.defaults.iterations` | Default iterations per user; fallback 1. |
| `performance.reporting.resultsFolder` | Results output folder. |
| `performance.reporting.dashboardFolder` | Dashboard output folder. |
| `performance.assertions.maxErrorPercent` | Maximum error-percent assertion; fallback 1.0. |
| `performance.assertions.maxAvgResponseTimeMs` | Maximum average response time; fallback 2000 ms. |
| `performance.assertions.maxP95ResponseTimeMs` | Maximum P95 response time; fallback 3000 ms. |

The generic `YamlReader` also scans `performance/payloads/yaml/performance-payloads.yml`, so `PerformancePayloadResolver` can look up YAML body keys such as `performance.payloads.<payload_name>`. Scalars are returned as text; YAML maps/lists are serialized as JSON. [1][10]

### 4.2 Appium native mobile YAML

`MobileYamlReader` loads only `mobile/config` and `mobile/elements`, recursively filters to YAML inside those exact framework directories, deep-merges map roots, and fails initialization when an allowed YAML resource cannot be parsed. It intentionally avoids recursively scanning `mobile` as a whole because app bundles/vendor assets may contain unrelated YAML. [11]

#### Shared mobile controls: `mobile.*`

| Key | Purpose / current default when absent |
|---|---|
| `mobile.enabled` | Enables mobile automation; accessor default `true`. |
| `mobile.appium_server_url` | Appium server URL; accessor default `http://127.0.0.1:4723`. |
| `mobile.default_platform` | Default Android/iOS platform; accessor default Android. It is lower priority than `-Dmobile.platform` and `@android`/`@ios`. [20] |
| `mobile.explicit_wait_seconds` | Mobile explicit-wait default; accessor default 30. |
| `mobile.implicit_wait_seconds` | Mobile implicit-wait default; accessor default 0. |
| `mobile.new_command_timeout_seconds` | Appium new-command timeout; accessor default 120. |
| `mobile.permissions.popup_timeout_seconds` | Short optional permission-popup check timeout; accessor default 3. |
| `mobile.permissions.max_popups_to_handle` | Maximum permission popups handled in a loop; accessor default 5. |
| `mobile.permissions.capture_evidence` | Capture evidence around explicit permission handling; accessor default true. |
| `mobile.evidence.output_directory` | Native mobile evidence root; accessor default `test-output/mobile-evidence`. |
| `mobile.evidence.screenshot_on_failure` | Failure screenshot toggle; default true. |
| `mobile.evidence.screenshot_on_pass` | Passing-scenario screenshot toggle; default false. |
| `mobile.evidence.screenshot_after_each_scenario` | Per-scenario screenshot toggle; default false. |
| `mobile.evidence.attach_screenshots_to_report` | Screenshot report attachment toggle; default true. |
| `mobile.evidence.video_recording_enabled` | Native mobile video toggle; default false. |
| `mobile.evidence.video_on_failure_only` | Video-on-failure-only toggle; default false in the accessor. |
| `mobile.evidence.attach_video_to_report` | Video report attachment toggle; default false. |

These keys are exposed by [`MobileConfigurationProperties`](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java). The checked-in [`mobile-config.yml`](../../../src/test/resources/mobile/config/mobile-config.yml) is the appropriate place for shared Appium runtime settings, not platform-specific app/browser capabilities. [11][34]

#### Native application capabilities: `mobile.android.*` and `mobile.ios.*`

[`mobile-native-config.yml`](../../../src/test/resources/mobile/config/mobile-native-config.yml) provides the current native-app capability maps. `MobileConfigurationProperties` reads a named capability dynamically beneath `mobile.<platform>.<capability>`, so retain the nested platform map. Current keys include:

| Platform | Confirmed keys in the resource |
|---|---|
| `mobile.android.*` | `automation_name`, `platform_name`, `device_name`, `platform_version`, `udid`, `orientation`, `app`, `app_package`, `app_activity`, `app_wait_package`, `app_wait_activity`, `system_port`, `adb_exec_timeout`, `auto_grant_permissions`, `no_reset`, `full_reset`. |
| `mobile.ios.*` | `automation_name`, `platform_name`, `device_name`, `udid`, `platform_version`, `orientation`, `app`, `bundle_id`, `auto_accept_alerts`, `auto_dismiss_alerts`, `include_safari_in_webviews`, `connect_hardware_keyboard`, `xcode_org_id`, `xcode_signing_id`, `updated_wda_bundle_id`, `wda_local_port`, `wda_startup_retries`, `wda_startup_retry_interval`, `wda_launch_timeout`, `wda_connection_timeout`, `wait_for_idle_timeout`, `app_launch_state_timeout_sec`, `use_new_wda`, `show_xcode_log`, `enforce_app_install`, `no_reset`, `full_reset`. |

Keep signing identifiers, device IDs, and private artifact paths out of committed test data. The resource comments distinguish Android APK, iOS Simulator `.app`, and signed real-device IPA usage; verify device-farm requirements separately. [20]

#### Appium real-browser capabilities: `mobile_browser_appium.*`

[`mobile-browser-config.yml`](../../../src/test/resources/mobile/config/mobile-browser-config.yml) is for the real device/emulator Chrome or Safari session, not native app automation and not Playwright emulation. `MobileConfigurationProperties` prefers `mobile_browser_appium.<platform>.<capability>` and then falls back to legacy `mobile.browser.<platform>.<capability>` if the split key is absent. It similarly prefers `mobile_browser_appium.enabled` over legacy `mobile.browser.enabled`. [11]

| Platform | Confirmed keys in the split configuration |
|---|---|
| Root | `mobile_browser_appium.enabled` |
| Android | `automation_name`, `platform_name`, `device_name`, `platform_version`, `udid`, `orientation`, `browser_name`, `initial_url`, `clean_start_enabled`, `clear_cookies`, `close_existing_tabs`, `terminate_before_start`, `activate_after_cleanup`, `reset_app_data`, `no_reset`, `full_reset`, `browser_package`, `chromedriver_autodownload`, `chromedriver_executable`, `chromedriver_mapping_file`, `auto_grant_permissions`. |
| iOS | `automation_name`, `platform_name`, `device_name`, `platform_version`, `udid`, `orientation`, `browser_name`, `initial_url`, `clean_start_enabled`, `clear_cookies`, `close_existing_tabs`, `terminate_before_start`, `activate_after_cleanup`, `reset_app_data`, `no_reset`, `full_reset`, `browser_bundle_id`, `auto_accept_alerts`, `auto_dismiss_alerts`, `include_safari_in_webviews`, `connect_hardware_keyboard`, `safari_allow_popups`, `safari_ignore_fraud_warning`, `web_context_timeout_seconds`, `safari_native_navigation_fallback_enabled`. |

Use the documented Appium browser tags for real-browser scenarios. A source-visible rule in `MobileHooks` gives explicit `@appium_browser` or `@mobile_browser_real` tags priority in selecting Appium browser behavior. [20]

### 4.3 Playwright mobile-browser emulation YAML

`MobileBrowserYamlReader` separately loads YAML under classpath directory `mobile_browser`, recursively deep-merges maps, and fails early if resources cannot be read. It is not part of the generic `YamlReader` scan. [14]

| Root/path | Purpose and confirmed key set |
|---|---|
| `mobile_browser.*` in [`mobile-browser-execution.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml) | `enabled`, `orientation`, evidence keys `output_directory`, `screenshot_on_failure`, `screenshot_on_pass`, `screenshot_after_each_scenario`, `attach_screenshots_to_report`, `video_recording_enabled`, `video_size_width`, `video_size_height`, and visual keys `enabled`, `baseline_directory`, `output_directory`, `mismatch_threshold_percent`, `create_baseline_if_missing`, `attach_artifacts_to_report`. [14] |
| `mobile_browser_profiles.<profile-name>.*` in [`mobile-browser-profiles.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml) | `browser_engine`, `platform`, `device_category`, `orientation`, `viewport_width`, `viewport_height`, `screen_width`, `screen_height`, `device_scale_factor`, `is_mobile`, `has_touch`, `user_agent`. Profile lookup ignores case and collapses extra whitespace. Missing/malformed individual profile values fall back to reader defaults. [14] |

A normal `config.yml` `browser` value matching a profile name activates this emulation path. The browser factory applies profile viewport, screen size, scale factor, mobile/touch flags, and nonblank user agent. `mobile_browser.orientation` can retain profile orientation or force portrait/landscape by swapping dimensions. [6][14]

> **Source-visible discrepancy — mobile-browser evidence root.** `MobileBrowserExecutionConfig` exposes `mobile_browser.evidence.output_directory`, but `BrowserFactory` currently writes Playwright mobile-browser video to its fixed timestamped `test-output/mobile-browser-evidence/<timestamp>/videos` path rather than calling that accessor. Do not assume changing the YAML evidence output directory relocates video until the consumer is changed or verified. [6][14]

### 4.4 Isolated UI performance YAML, locators, and user data

The UI performance module deliberately does not use the framework-wide YAML store. [`UiPerformanceYamlReader`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java) loads one classpath resource selected by `-Dui.performance.config`, defaulting to [`ui_performance-config.yml`](../../../src/test/resources/ui_performance/config/ui_performance-config.yml). Its values are type-checked more strictly than the generic reader: malformed integer/long/double/boolean values throw useful `IllegalArgumentException`s. [15]

#### `ui_performance.*` configuration catalogue

| Confirmed path | Responsibility / validation |
|---|---|
| `ui_performance.enabled` | Explicit safety gate. `requireEnabled()` blocks execution when false. |
| `ui_performance.active_profile` | Chooses a named profile beneath `profiles`; accessor fallback `load`. |
| `ui_performance.target.protocol` | Must be `http` or `https`; fallback `https`. |
| `ui_performance.target.host` | Must be a bare host: no protocol, port, slash, or path. |
| `ui_performance.target.port` | Must be 1–65535; default derives from protocol. |
| `ui_performance.target.base_path` | Optional; if nonempty must start with `/`; a trailing slash is normalized away. |
| `ui_performance.target.routes.<route_name>` | Named route used by journey Gherkin; must be relative rather than an absolute URL. |
| `ui_performance.profiles.<profile>.type` | Load/stress/spike/soak type; default uses the selected profile name. |
| `ui_performance.profiles.<profile>.stages[]` | Stage maps use `name`, `users`, `ramp_up_seconds`, `hold_seconds`, `iterations_per_user`. Stages execute sequentially; users inside a stage run concurrently. |
| `ui_performance.browser.headless` | Browser headless flag; fallback true. This is independent of shared `config.yml` `headless`. |
| `ui_performance.browser.ignore_https_errors` | Browser-context HTTPS behavior; fallback false. |
| `ui_performance.browser.isolation` | Browser isolation value; fallback `process`. Current engine gives each virtual user a separate worker, Playwright instance, and browser process. |
| `ui_performance.browser.user_agent` | User agent for each UI performance browser context. |
| `ui_performance.browser.action_timeout_ms` | Action timeout; fallback 10,000 ms. |
| `ui_performance.browser.navigation_timeout_ms` | Navigation timeout; fallback 20,000 ms. |
| `ui_performance.execution.synchronized_start_timeout_ms` | Maximum preparation time before releasing a synchronized start gate; fallback 60,000 ms. |
| `ui_performance.execution.between_iterations_ms` | Pause between repeated iterations; fallback 0. |
| `ui_performance.data.use_csv` | Use isolated classpath CSV data; fallback true. If false, synthetic non-sensitive user identities are generated. |
| `ui_performance.data.users_csv` | Classpath CSV resource path; fallback `ui_performance/data/users.csv`. |
| `ui_performance.data.allow_user_reuse` | Controls reuse when a stage needs more users than available CSV rows; fallback false. |
| `ui_performance.evidence.capture_failure_screenshots` | Captures full-page failure screenshot under the run directory; fallback false. |
| `ui_performance.evidence.capture_console_errors` | Captures browser console errors as run evidence; fallback true. |
| `ui_performance.thresholds.maximum_failure_rate_percent` | Failure-rate threshold; fallback 20.0. |
| `ui_performance.thresholds.maximum_average_journey_duration_ms` | Average journey-duration threshold; fallback 5,000 ms. |
| `ui_performance.thresholds.maximum_p95_journey_duration_ms` | P95 journey-duration threshold; fallback 8,000 ms. |
| `ui_performance.safety.max_virtual_users` | Validation ceiling for the largest requested stage; fallback 10. There is no separate hidden compiled maximum. |
| `ui_performance.reporting.output_directory` | Base directory for timestamped run directories; fallback `test-output/ui_performance`. |
| `ui_performance.reporting.existing_performance_reporter_enabled` | Enables the adapter to the existing performance reporter; fallback true. |
| `ui_performance.reporting.html_enabled` / `pdf_enabled` / `csv_enabled` / `json_enabled` | Independently choose standalone HTML, PDF, CSV, and JSON outputs; each accessor fallback is true. |

A safe, generic journey shape is:

```gherkin
Given UI performance journey "<journey-name>" uses configured target
When UI performance journey navigates to configured route "<route-name>"
Then UI performance journey verifies locator "<locator-group>" "<locator-key>" is visible
And the configured UI performance users execute the journey
And the UI performance run produces a standalone performance report
```

The module profile is started with the Maven profile declared in the build:

```bash
mvn clean test -Pui_performance
```

That profile points Surefire at the dedicated UI performance TestNG suite, disables normal Surefire parallelism, and makes threshold breaches fail the dedicated run. [5][15][18]

#### UI performance locators and data

The UI performance locator repository reads only [`ui_performance/locators/ui_performance-locators.yml`](../../../src/test/resources/ui_performance/locators/ui_performance-locators.yml), requiring root `ui_performance_locators` and a group/key map. It shares locator **types** with ordinary UI locators but stays physically separate, so performance changes do not alter functional UI element files. [17]

Unlike the normal `ElementLocatorHelper`, the UI performance engine requires an underscore-separated `TYPE_value` definition: it finds the first `_`, rejects a blank type or value, then delegates to `LocatorHandler`. Do not use a space-only separator in this file. [17][18]

```yaml
ui_performance_locators:
  <journey_group>:
    <submit_action>: "Button_Submit"
    <status_field>: "CSS_[data-testid='status']"
```

The isolated user CSV is read as a classpath resource. Its first column must be `user_id`, it must have at least one additional named data column, every row must have the same column count, and blank/comment (`#`) lines are skipped. Values with quoted embedded commas are intentionally unsupported because the reader splits on commas. A journey can reference a header as `${header_name}`; a missing placeholder fails rather than silently sending an unresolved value. [16][18]

```csv
user_id,reference_code
<virtual-user-001>,<approved-non-sensitive-value>
<virtual-user-002>,<approved-non-sensitive-value>
```

When `allow_user_reuse: false`, supply at least as many rows as the largest selected stage; otherwise the engine stops before browser launch. If `use_csv: false`, do not use `${...}` journey values because the engine validates and rejects that combination. [16][18]

A separate browser-contract YAML resource exists at [`ui_performance-browser-contract.yml`](../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml). It is **not** the default configuration; select it (or another approved classpath resource) only with `-Dui.performance.config=...` when the UI performance reader is invoked. [15][26]

### 4.5 Generated UI performance artifacts

Each UI performance run creates `<reporting.output_directory>/<sanitized-journey>_<timestamp>/` plus `failed-screenshots/` and `browser-console-errors/` directories. When enabled, the report writer creates the following source-confirmed artifacts:

| Artifact | Controlled by |
|---|---|
| `ui_performance-summary.html` | `html_enabled` |
| `ui_performance-summary.pdf` | `pdf_enabled` |
| `ui_performance-stages.csv` | `csv_enabled` |
| `ui_performance-iterations.csv` | `csv_enabled` |
| `ui_performance-step-timings.csv` | `csv_enabled` |
| `ui_performance-summary.json` | `json_enabled` |
| `ui_performance-performance-summary.txt` | Always written by the standalone writer |
| Failure PNG and console-error log files | Only when the relevant evidence is captured and exists |

The report writer intentionally excludes full URLs, credentials, tokens, input values, cookies, and session data from its report content. Threshold breaches occur after report writing, so the failure message directs the user to the report directory. [18][27]

## 5. Locator conventions and boundaries

### 5.1 Normal Playwright desktop locators

For normal UI YAML, `ElementLocatorHelper` looks up `elements.<page>.<key>`. It accepts either `TYPE_value`, `TYPE value`, or a type with no value. It uses the earliest underscore or space as the separator, trims both portions, and returns an empty value for a type-only token. [4]

| Locator family | Supported normal UI syntax and resolver behavior |
|---|---|
| Raw selectors | `CSS_<selector>`, `TAG_<selector>`, and `XPATH_<selector>` pass the selector through to Playwright `locator(...)`. |
| Role-oriented locators | `BUTTON_<accessible name>`, `LINKTEXT_<name>`, `TEXTBOX_<name>`, `CHECKBOX_<name>`, `RADIOBUTTON_<name>`, `DROPDOWN_<name>`, `OPTION_<name>`, and many ARIA role names map to Playwright `getByRole`. A type-only/empty value makes an unnamed role lookup. `OPTION` uses exact-name matching; `BUTTONSUBMIT` applies the resolver’s pressed option for its named case. |
| Text / generic role | `TEXT_<visible text>` uses `getByText`; `ROLE_<aria-role-name>` converts the value to `AriaRole`. |
| Semantic getters | `ALTTEXT_`, `TITLE_`, `PLACEHOLDER_`, `LABEL_`, and `TESTID_` map to the corresponding Playwright getter. |
| CSS shortcuts | `ID_<id>` becomes `#<id>`; `NAME_<name>` becomes `[name='<name>']`; `CLASS_<class>` becomes `.<class>`. |

The page resolver includes the core roles above and broader ARIA roles such as headings, lists, tables, rows, cells, navigation, menus, trees, grids, status, landmarks, and more. Frame and chained-locator overloads mirror the principal selector, role, semantic, and shortcut behavior but should be validated for a less-common role before standardizing it; the source’s page switch is broader than its frame/chained switches. Unknown types throw a diagnostic containing the context (`PAGE`, `FRAME`, or `CHAINED`), received type/value, and a `TYPE_value` hint. [5]

Use stable accessible names, `data-testid` values, IDs, or targeted CSS where the application supports them. Avoid brittle indexes and customer-specific visible text. Keep selector values as YAML strings when they include `:`, `#`, brackets, quotes, or special characters.

```yaml
# Safe examples only; retain the selected convention consistently.
elements:
  <page_key>:
    submit: "Button_Submit"
    reference: "TESTID_reference-input"
    status: "CSS_[data-testid='status']"
    region: "XPATH_//section[@aria-label='Results']"
```

`ElementLocator` is deliberately a locator-construction abstraction: it should return a Playwright locator, not perform waits, assertions, or interactions. Waiting/action behavior belongs in the action layer. [28]

### 5.2 Appium mobile locators

For mobile YAML, `MobileCommonMethods.resolveLocator(page, locator)` first reads `mobile_elements.<page>.<locator>`. It supports platform-aware maps containing values such as `android`, `ios`, `default`, and `mobileBrowser`. In an Appium **browser** session only, a missing mobile entry may fall back to normal `elements.<page>.<locator>`; native app automation deliberately does not use that fallback. [29]

```yaml
mobile_elements:
  <page_key>:
    <login_button>:
      android: "ACCESSIBILITY_ID_login-button"
      ios: "ACCESSIBILITY_ID_login-button"
      default: "Button_Sign in"
```

[`MobileLocatorHandler`](../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java) preserves the explicit native prefixes `ACCESSIBILITY_ID_`, `ID_`, `XPATH_`, `CLASS_NAME_`, `NAME_`, `ANDROID_UIAUTOMATOR_`, `IOS_PREDICATE_`, and `IOS_CLASS_CHAIN_`. These explicit prefix checks are uppercase in the current code. It additionally parses friendly `TYPE_value` or `TYPE value` tokens case-insensitively for types including CSS, TAG, CLASS, TESTID, PLACEHOLDER, LABEL, TITLE, ALTTEXT, TEXT, LINKTEXT, BUTTON, TEXTBOX/INPUT, CHECKBOX, RADIOBUTTON/RADIO, DROPDOWN/COMBOBOX, OPTION, IMAGE, HEADING, TAB, LIST, LISTITEM, TABLE, ROW, CELL, DIALOG, MENU, and MENUITEM. [30]

> **Mobile locator rule of thumb:** prefer a stable native accessibility identifier for native app screens. Use UI-style CSS/XPath/text locators only where the session is actually interacting with a browser DOM or where that fallback is intentional. [30]

### 5.3 UI performance locator boundary

Use `ui_performance_locators.<group>.<key>` for UI performance journeys, not `elements.<page>.<key>`. The repository rejects blank group/key values and missing groups/locators with a clear exception. The engine requires underscore form `TYPE_value`, even though the normal UI helper also accepts a space separator. [17][18]

## 6. Input-data placement and reader behavior

### 6.1 Excel: `ExcelReader`

[`ExcelReader`](../../../src/main/java/com/ptaf/utils/ExcelReader.java) reads a filesystem path, not a classpath resource name. Its contract is deliberately narrow:

| Rule | Behavior |
|---|---|
| Workbook/sheet | Opens the supplied path with Apache POI and uses **only sheet index 0**. |
| Header row | Uses first row as header names; headers are trimmed but matching against requested `columnName` is case-sensitive. |
| Row match | Uses the first cell of each subsequent row and matches `testCaseName` case-insensitively after trimming. |
| Returned value | Returns the requested cell’s `Cell.toString()`; it does not normalize formatting or evaluate a formula for you. |
| Failure | Empty sheet, missing column, missing test-case row, null target cell, I/O, or parse errors are logged with file/test/column/reason and return `null`. |

Performance steps can resolve an Excel payload by file path, row identifier, and header name through `PerformancePayloadResolver`, which delegates to this reader. [10][31]

```gherkin
When we run Excel-driven POST performance test for path "/<resource>" with name "<safe-test-name>" using excel file "src/test/resources/performance/payloads/excel/<file>.xlsx" row "<row-id>" column "<payload-column>"
```

The repository also provides [`ExcelToYaml`](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java), a utility that converts the first worksheet to YAML. It treats row 0 as headers, supports `ALL` or one `testcase_id`, writes a YAML list/map to the caller-supplied destination, and does **not** create output when the requested test case is absent. It is a conversion utility, not a reader automatically invoked by normal UI execution. [32]

### 6.2 Performance CSV payloads

`PerformancePayloadResolver` supports inline, YAML, CSV, and Excel bodies. For CSV it delegates to `CsvPayloadReader`; the input is a file/classpath resource, first-column row identifier, and header name. [10][31]

`CsvPayloadReader` searches in this exact order: supplied classpath resource, classpath resource after removing a leading `/`, direct filesystem path, then `src/test/resources/<path>`. Its CSV has a header row; row identification uses the first cell case-insensitively; header matching is case-insensitive after normalization. It uses a simple comma split, so do not use complex quoted comma-containing payload fields without confirming a different parser is introduced. [31]

```gherkin
When we run CSV-driven POST performance test for path "/<resource>" with name "<safe-test-name>" using csv file "performance/payloads/csv/<file>.csv" row "<row-id>" column "<payload-column>"
```

### 6.3 Scenario CSV data

The general CSV steps load a project-relative filesystem path, typically under `src/test/resources/data/`. Rows and column indexes are 1-based data positions (header excluded); header-name matching for those general steps is case-sensitive. The CSV context is thread-local and cleared after each scenario. [33]

```gherkin
Given I load CSV file "src/test/resources/data/<approved-file>.csv"
Then CSV row 1 column "<column-name>" equals "<non-sensitive-expected-value>"
```

This differs from the isolated UI performance CSV reader, which requires a classpath resource and a `user_id` first header. Do not transpose one format’s rules onto the other. [16][33]

## 7. Tester-safe editing workflow

1. **Choose the correct owner directory first.** Put normal UI locators in `elements/`; native mobile settings in `mobile/config/`; mobile emulation profiles in `mobile_browser/config/`; UI load-test configuration, locators, and users under `ui_performance/`. A valid YAML file in the wrong directory may never be loaded by the intended reader. [1][11][14][15]
2. **Maintain exact YAML roots.** Use `elements`, `mobile_elements`, `mobile`, `mobile_browser`, `mobile_browser_profiles`, `performance`, `ui_performance`, or `ui_performance_locators` as required by the reader. A syntactically valid but mis-rooted document resolves to `null` or fails validation. [4][11][14][17]
3. **Preserve indentation and quote selector-heavy strings.** YAML parsing happens before test execution. Invalid YAML yields the generic reader’s file-specific diagnostic or causes dedicated readers to fail initialization. [1][11][14][15]
4. **Avoid duplicate generic scalar paths.** The generic reader merges maps and later scanned scalars replace earlier values; directory walking has no explicit ordering guarantee. [1]
5. **Do not edit `target/test-classes` copies.** Edit `src/test/resources`; build output is generated from source resources.
6. **Use logical keys in Gherkin.** API request names, SQL query names, and locator group/key pairs keep scenarios readable and prevent endpoints/SQL/selectors from spreading across feature files. [12][13][17]
7. **Keep test data non-sensitive and minimal.** Use placeholders or approved synthetic data. Remove stale downloaded/extracted/report data from working copies as appropriate for your project policy.
8. **Restart the test JVM after YAML changes.** All current readers cache configuration at class initialization. [1][11][14][15]
9. **Validate the smallest relevant run first.** Examples: `mvn test -Dheadless=true` for normal UI launch behavior; a tagged mobile scenario with `-Dmobile.platform=<platform>`; or the dedicated `mvn clean test -Pui_performance` only after target approval and capacity review. [5][6][20]

## 8. Troubleshooting matrix

| Symptom | Likely source-visible cause | Check / corrective action |
|---|---|---|
| `YAML GET FAILURE` names a failed path segment | Wrong root/key, a missing map segment, or a scalar where a nested map was expected. [1] | Compare the exact dotted key with the resource root and indentation. Check that the file is under a directory scanned by the correct reader. |
| Valid YAML change appears to have no effect | Reader was already initialized, wrong resource family was edited, or a direct `YamlReader` caller bypassed environment overlay. [1][2] | Restart the JVM; verify reader ownership; use `ConfigurationProperties` only where its environment overlay is expected. |
| Browser starts headed despite `config.yml` | `-Dheadless` has higher priority, or `headless` parses to false. [6] | Inspect the Maven/CI command for `-Dheadless`; then verify shared/environment YAML. |
| Normal UI timeout is not what `runtimeTimeoutMillis` suggests | `Hooks` reads `runtimeWait` directly. [2][22] | Set/repair `runtimeWait` in seconds for the normal browser-hook path. |
| API context fails before request is sent | Missing service `base_url`, missing request `method`/`endpoint`, or missing token OS variable named by `auth_token_env`. [7][12] | Confirm service/request logical names; ensure only the variable **name** is in YAML and its secret is injected into the runner. |
| DB connection fails in SQL Server-auth mode | Blank username/password variable name, absent OS password variable, unsupported `db_type`/authentication, or required server/database fields missing. [8] | Verify all required non-secret fields and runner access; never replace the environment-variable key with an actual password. |
| DB SQL key is “not found” | Query is not under a generic-reader scanned `queries` resource or the logical dotted key is wrong. [1][13] | Put it in `src/test/resources/queries/db_queries.yml` under the intended map and use the exact key. |
| Locator type failure | Token type is unsupported, wrong separator/value, or selector was put in the wrong context. [4][5] | Use a supported normal UI type; check `PAGE`/`FRAME`/`CHAINED` in the diagnostic. For UI performance, use underscore-only `TYPE_value`. |
| Native mobile locator is missing | The `mobile_elements.<page>.<locator>` entry is missing, or a web locator was expected to fall back during a native session. [29] | Add a platform-aware mobile locator. Only Appium browser sessions can fall back to normal `elements`. |
| UI performance CSV fails before browsers launch | CSV path is not classpath-visible, first header is not `user_id`, row shape differs, quoted comma data is present, or rows are insufficient with reuse disabled. [16][18] | Fix the CSV structure/path and add approved synthetic rows; keep `allow_user_reuse` false unless reuse is approved for the journey. |
| UI performance configuration does not change | The default resource remains selected or `-Dui.performance.config` does not name a classpath resource. [15] | Verify the property points to a packaged resource path, not an arbitrary local secret-bearing path. |
| UI performance run fails threshold assertions but has outputs | Thresholds are enforced after report writing. [18] | Open the created timestamped run directory; review safe summary/CSV/JSON evidence and adjust test capacity/thresholds only with approval. |

## Related chapters

These future chapter filenames are intentionally listed without links because they are planned scope boundaries rather than current files:

- `01-framework-orientation-and-execution.md` — project layout, runners, lifecycle, and supported run commands.
- `03-ui-and-playwright-interaction-reference.md` — UI actions, assertions, frames, waits, and browser lifecycle details.
- `04-api-and-database-automation-reference.md` — API request execution and database validation beyond their configuration catalogues.
- `05-mobile-automation-reference.md` — Appium sessions, device setup, and mobile interaction behavior.
- `06-performance-and-reporting-reference.md` — performance engines, execution mechanics, metrics, and report interpretation.
- `07-data-files-csv-xml-zip-reference.md` — CSV/XML/ZIP operations beyond the placement and reader rules in this chapter.

## Source references

- [Shared configuration resource](../../../src/test/resources/config/config.yml)
- [Generic YAML reader](../../../src/main/java/com/ptaf/utils/YamlReader.java)
- [Shared configuration accessors](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java)
- [Desktop locator helper and resolver](../../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java)
- [Desktop locator mapping](../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java)
- [Browser factory](../../../src/main/java/com/ptaf/utils/BrowserFactory.java)
- [API context and token handling](../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java)
- [Database connection handling](../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java)
- [JMeter performance configuration reader](../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java)
- [Performance payload resolver](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java)
- [Mobile YAML reader](../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java)
- [API request YAML consumer](../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java)
- [Database YAML query consumer](../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java)
- [Mobile-browser YAML reader and profiles](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java)
- [UI performance YAML reader/configuration](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java)
- [UI performance user CSV reader](../../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java)
- [UI performance locator repository](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java)
- [UI performance engine and reports](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java)
- [Per-feature reporting listener](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java)
- [Mobile hooks platform selection](../../../src/main/java/com/ptaf/hooks/MobileHooks.java)
- [Mobile configuration accessors](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java)
- [Excel and CSV data readers](../../../src/main/java/com/ptaf/utils/ExcelReader.java)

## References

[1]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "Generic YAML reader: scan, merge, lookup, and diagnostics"
[2]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Shared configuration accessors and environment-specific lookup"
[3]: ../../../src/test/resources/config/config.yml "Shared FNB-ETAF configuration resource"
[4]: ../../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java "Desktop YAML element lookup and locator-token parser"
[5]: ../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java "Playwright locator type mapping"
[6]: ../../../src/main/java/com/ptaf/utils/BrowserFactory.java "Browser launch, headless override, mobile profile, video, and HTTP credential handling"
[7]: ../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java "API context configuration and environment-token handling"
[8]: ../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java "SQL Server connection configuration and environment-password handling"
[9]: ../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java "Dedicated JMeter-style performance YAML reader"
[10]: ../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java "Performance payload resolver for inline, YAML, CSV, and Excel sources"
[11]: ../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java "Restricted mobile YAML reader"
[12]: ../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java "API request-definition YAML consumer"
[13]: ../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java "Database query YAML consumer"
[14]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java "Isolated Playwright mobile-browser YAML reader"
[15]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java "Isolated UI-performance YAML reader and classpath override"
[16]: ../../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java "UI-performance classpath CSV reader and validation"
[17]: ../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java "UI-performance locator repository"
[18]: ../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java "UI-performance execution, data validation, evidence, and threshold enforcement"
[19]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature Extent report configuration and artifacts"
[20]: ../../../src/main/java/com/ptaf/hooks/MobileHooks.java "Native mobile platform and browser-mode selection"
[21]: ../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java "Database Gherkin parameter parsing"
[22]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Normal UI browser lifecycle and runtimeWait handling"
[23]: ../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "Normal UI action wait configuration"
[24]: ../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java "Download and upload step configuration usage"
[25]: ../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java "Database statement timeout and fetch-size configuration"
[26]: ../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml "UI-performance browser-contract configuration resource"
[27]: ../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java "UI-performance report artifacts and sanitization"
[28]: ../../../src/main/java/com/ptaf/ui/interfaces/ElementLocator.java "Playwright locator-construction interface"
[29]: ../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java "Mobile locator lookup and browser-session fallback"
[30]: ../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java "Appium native and friendly locator resolver"
[31]: ../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java "Performance CSV payload location and lookup"
[32]: ../../../src/main/java/com/ptaf/utils/ExcelToYaml.java "Excel-to-YAML conversion utility"
[33]: ../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java "Scenario CSV steps, numbering, and lifecycle"
[34]: ../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java "Typed Appium mobile configuration accessors"
