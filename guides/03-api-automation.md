# FNB-ETAF API Automation Technical Usage Guide

## Purpose and scope

This guide describes the **browserless REST API automation path** that is implemented in this repository. It covers request definitions, service configuration, Cucumber steps, request assembly, Playwright API execution, response checks, reporting, and the boundaries between API automation and the other FNB-ETAF modules. It is intended for creating or maintaining API scenarios with the repository’s existing step vocabulary and resource conventions.

The implementation uses Playwright’s `APIRequestContext`, not a Playwright browser page. API scenario detection in `com.ptaf.hooks.Hooks` skips desktop browser initialization for scenarios tagged `@api` (and related `@api_`/`@api-` forms), for feature paths containing `/api/`, or for filenames containing `api`. The API client still creates a per-thread Playwright instance to own the API context. [1] [4]

This document does not describe UI locator authoring, database query authoring, native/mobile execution, or performance-load execution. It also intentionally does not reproduce configured private URLs, tokens, credentials, or client data.

## Architecture and source locations

| Layer | Package / class | Responsibility | Primary resource or entry point |
|---|---|---|---|
| Cucumber steps | `com.ptaf.stepdefinitions.ApiSteps` | Exposes the supported Gherkin steps for request setup, dispatch, and response checks. | [ApiSteps.java][5] |
| Test-facing facade | `com.ptaf.api.methods.ApiCommonMethods` | Delegates setup and execution to an `ApiAction`; performs JUnit 5 status, body, header, and JSONPath assertions. | [ApiCommonMethods.java][6] |
| Stateful action | `com.ptaf.api.implementation.ApiActionImpl` | Holds headers, path parameters, query parameters, body, and the most recent response in `ThreadLocal` state; resolves request keys from YAML. | [ApiActionImpl.java][7] |
| HTTP performer | `com.ptaf.api.performer.ApiActionPerformer` | Replaces endpoint placeholders, applies options, serializes bodies with Gson, dispatches supported verbs, and wraps the response. | [ApiActionPerformer.java][8] |
| API context handler | `com.ptaf.api.handlers.ApiRequestHandler` | Creates one `APIRequestContext` and one Playwright instance per thread; reads service configuration and optional bearer token from the process environment. | [ApiRequestHandler.java][9] |
| Response model | `com.ptaf.api.wrapper.ApiResponseWrapper` | Stores the status code, raw body text, and response-header map used by validations. | [ApiResponseWrapper.java][10] |
| Contract | `com.ptaf.api.interfaces.ApiAction` | Defines the high-level request-building, execution, and last-response API. | [ApiAction.java][11] |
| YAML loading | `com.ptaf.utils.YamlReader` and `com.ptaf.utils.ConfigurationProperties` | Merges YAML resources, resolves dot-separated keys, and applies optional `environments.<env>.` overrides before root values. | [YamlReader.java][12] [ConfigurationProperties.java][13] |
| API definitions | `src/test/resources/api_requests/api_requests.yml` | Holds logical request keys, HTTP methods, and endpoint templates. | [api_requests.yml][2] |
| Service configuration | `src/test/resources/config/config.yml` | Holds `api_services` entries, base URLs, environment-variable names for bearer tokens, and `ignoreHTTPSErrors`. | [config.yml][3] |
| Feature examples | `src/test/resources/features/api_test.feature` and `src/test/resources/features/create_post_workflow_api.feature` | Demonstrate the implemented step grammar and request workflow patterns. | [api_test.feature][14] [create_post_workflow_api.feature][15] |
| Browserless lifecycle | `com.ptaf.hooks.Hooks` | Detects API scenarios before browser setup and clears browserless hook state during teardown. | [Hooks.java][4] |
| Maven/TestNG entry point | `com.ptaf.runner.TestRunner` referenced by `src/test/resources/testng.xml` | Default Maven suite; scans `com.ptaf.stepdefinitions` and `com.ptaf.hooks`. | [TestRunner.java][16] [testng.xml][17] |

### Request lifecycle

A scenario first uses `ApiSteps` to accumulate request inputs. `ApiActionImpl` reads `<request_group>.<request_name>.method` and `.endpoint` from the merged YAML resource map, gets an API context for the supplied service key, and calls `ApiActionPerformer`. The performer applies path substitutions, headers, query parameters, and an optional body, then dispatches the configured HTTP method. The resulting status, text body, and headers are retained as the current thread’s last response for the assertion steps. [5] [7] [8]

Request state is **per thread**. After `ApiActionPerformer.sendRequest(...)` returns normally, `ApiActionImpl` clears the headers, path parameters, query parameters, and body for that thread. Set every value required for each request explicitly; a request failure before the method returns does not reach that state-clear call. The API context is also thread-local, but it is created lazily only once per thread. Consequently, do not mix different configured services in one execution thread and assume a new context will be created for the later service. [7] [9]

## Prerequisites

The build sets Java source and target to **21**, and Maven is the project build tool. The API path relies on Cucumber, TestNG, Playwright, Gson, SnakeYAML, Jayway JSONPath, and JUnit 5 assertions supplied by the project POM. [18]

Install a JDK compatible with Java 21 and Maven, then work from the repository root:

```bash
cd /home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23
java -version
mvn -version
mvn -DskipTests test-compile
```

API scenarios do **not** require Playwright browser binaries because `Hooks` classifies them as browserless and the API implementation uses `Playwright.request().newContext(...)`. They do require network access to the configured service and a valid configuration entry. [4] [9]

For a protected service, set the environment variable whose **name** is configured at `api_services.<service_name>.auth_token_env` before starting Maven. Keep the token value outside feature files, YAML, shell history, screenshots, and reports. A safe shell pattern is:

```bash
export <TOKEN_ENV_VAR_NAME>='<token-provided-by-your-secret-store>'
```

Do not put an `Authorization` value into a Gherkin header step. `ApiCommonMethods.setHeader` logs both the header name and value at INFO level; the performer also logs serialized bodies and full response bodies at DEBUG level. The configured environment-token route avoids placing the bearer token in test source, although users must still treat execution logs and API responses as sensitive. [6] [8] [9]

## Configuration files and settings

### Service settings

`src/test/resources/config/config.yml` is the service configuration source. `ConfigurationProperties.getValue(...)` first looks for `environments.<env>.<key>`, where `env` is the Java system property and defaults to `QA`; it falls back to the root key when no environment-specific value exists. [3] [13]

Use this placeholder-only structure when adding a service. It matches the keys consumed by `ApiRequestHandler`; do not copy a real endpoint or token into the guide, feature, or request YAML.

```yaml
api_services:
  <service_name>:
    base_url: "<base_url>"
    auth_token_env: "<TOKEN_ENV_VAR_NAME>"

ignoreHTTPSErrors: "<true_or_false>"
```

`base_url` must be present and nonblank. If `auth_token_env` is blank or absent, the context sends no automatic authorization header. If it is set, the handler reads the named environment variable, fails fast if that variable is missing or empty, and sends `Authorization: Bearer <token>`. The `ignoreHTTPSErrors` setting is passed to the Playwright API context. Set it deliberately: `true` permits certificate errors, while `false` enforces certificate validation. [9] [13]

### Request-definition settings

`src/test/resources/api_requests/api_requests.yml` is the authoritative location for reusable request keys. `YamlReader` scans and recursively merges `.yml` and `.yaml` resources beneath `elements`, `queries`, `api_requests`, `config`, and `performance`; values are retrieved by dot-separated keys. Therefore a request name in Gherkin must exactly match the nested YAML key. [2] [7] [12]

Use the existing definition shape, with placeholders rather than environment values:

```yaml
<request_group>:
  <request_name>:
    method: "<HTTP_METHOD>"
    endpoint: "/<resource>/{<path_parameter_name>}"
```

Both `method` and `endpoint` are required at runtime. A missing or incomplete request definition raises `IllegalArgumentException`. Endpoint replacement is literal: `{<path_parameter_name>}` is replaced only when a matching path parameter was set, and the replacement value is **not URL-encoded** by this implementation. Provide URL-safe path values and make the placeholder spelling agree with the Gherkin key. [7] [8]

### HTTP method, header, parameter, and payload behavior

| Concern | Implemented behavior | Authoring implication |
|---|---|---|
| HTTP verbs | `ApiActionPerformer` dispatches `GET`, `POST`, `PUT`, `DELETE`, and `PATCH` case-insensitively; another verb throws `IllegalArgumentException`. The current request YAML demonstrates GET, POST, PUT, and DELETE definitions. | Put the exact supported verb in `method`. Use a request definition rather than placing a URL in Gherkin. |
| Headers | Each header step adds or overwrites one header for the next request. | Set only non-secret request headers in a feature. Do not inline bearer tokens or client data. |
| Query parameters | Each query value is converted with `String.valueOf(...)` and applied through Playwright request options. A Java `null` becomes the text `"null"`. | Set query values explicitly and do not rely on null omission. |
| Path parameters | Placeholder replacement is a direct string operation. Unmatched placeholders remain in the endpoint. | Use matching names and pre-encode any value that requires URL encoding. |
| Body and content type | If a body is present, Gson serializes the body and sends it as request data. If the header map lacks the exact key `Content-Type`, the performer adds `Content-Type: application/json`. | Be precise with header casing. A raw Cucumber doc string is stored as a Java `String`, so Gson serializes it as a JSON string literal rather than parsing it into a JSON object. |
| Response capture | The performer reads `response.text()` and creates `ApiResponseWrapper(status, body, headers)`. | Keep response sizes appropriate for in-memory text capture; validate structured JSON with JSONPath only when the body is JSON. |

The existing workflow feature uses a doc string for a payload. That syntax is supported by `ApiSteps`, but the current implementation passes the doc string through as a `String` before Gson serialization. Do not infer that the framework parses or templatises JSON doc strings; it does neither in `ApiSteps` or `ApiActionImpl`. [5] [7] [8] [15]

## Build and execution

The default Surefire suite is `src/test/resources/testng.xml`, which invokes `com.ptaf.runner.TestRunner`. That runner includes the correct API step package, `com.ptaf.stepdefinitions`, and the shared hooks package. The POM sets `testFailureIgnore` to `true`; therefore inspect the generated reports rather than relying only on the Maven process exit code to establish scenario success. [16] [17] [18]

Run the tagged API examples through the default Maven/TestNG path with this exact command:

```bash
mvn clean test -Dcucumber.filter.tags="@api"
```

This command selects the currently tagged API workflow feature. Add `@api` to every new API feature or scenario so it is both selected by the command and classified as browserless. The existing `api_test.feature` has an API-identifying filename, so the hook treats it as browserless, but it is not tagged `@api` and is therefore not selected by the command above. [4] [14] [15]

The repository also contains `com.ptaf.runners.ApiTestRunner`, a JUnit Cucumber runner with API-specific report destinations. Its configured glue includes `com/ptaf/api/stepdefinitions`, but the implemented step class is packaged as `com.ptaf.stepdefinitions.ApiSteps`; no `com.ptaf.api.stepdefinitions` test package exists. For the source state documented here, use the Maven/TestNG command above rather than treating that JUnit runner as the validated API execution path. [5] [19]

## Create a new API feature and test

Create API scenarios in `src/test/resources/features/` or a child folder. Add the `@api` tag at feature or scenario level. Define the service once in `config.yml` and its request metadata once in `api_requests.yml`; do not repeat method or endpoint values in the feature. The feature should build one request, send it, and validate the retained response before configuring the next request.

The following is a placeholder-only template made exclusively from the step expressions implemented in `ApiSteps`. Substitute names that correspond to the service and request-definition keys you add. It is deliberately not a copy of a live endpoint or test record.

```gherkin
@api
Feature: <API capability>

  Scenario: <request outcome>
    Given I set the request header "<header_name>" to "<non_secret_header_value>"
    And I set the path parameter "<path_parameter_name>" to "<url_safe_value>"
    And I set the query parameter "<query_parameter_name>" to "<query_value>"
    And I set the request body to
      """
      <payload_text>
      """
    When I send a "<request_group>.<request_name>" request to the "<service_name>" service
    Then the response code should be <expected_status_code>
    And the response header "<response_header_name>" should be "<expected_header_value>"
    And the response body should contain the text "<expected_text>"
    And the value of the JSON path "<json_path>" should be "<expected_value>"
```

All setup steps are optional according to the request. For example, a simple GET needs only the path or query parameters that its definition requires, followed by the send and response assertions. Do not execute a response assertion before the `When I send...` step: `ApiActionImpl.getLastResponse()` throws when the current thread has not sent an API request. [5] [7]

### Supported Gherkin vocabulary

| Purpose | Exact step expression |
|---|---|
| Request header | `Given I set the request header "{string}" to "{string}"` |
| Path parameter | `And I set the path parameter "{string}" to "{string}"` |
| Query parameter | `And I set the query parameter "{string}" to "{string}"` |
| Body | `And I set the request body to` followed by a Cucumber doc string |
| Dispatch | `When I send a "{string}" request to the "{string}" service` |
| Status validation | `Then the response code should be {int}` |
| Body substring validation | `And the response body should contain the text "{string}"` |
| Header validation | `And the response header "{string}" should be "{string}"` |
| JSONPath validation | `And the value of the JSON path "{string}" should be "{string}"` |

The status check uses an exact integer comparison. The body check is a substring check. Response-header lookup is case-insensitive but its expected value must match exactly. JSONPath evaluation uses Jayway JSONPath, converts the actual result with `Objects.toString(actualValue, null)`, and compares it to the expected Gherkin string. Invalid JSON or an invalid JSONPath fails the step. [6] [7]

## Feature, data, locator, payload, and query locations

| Asset | Exact repository location | API relationship |
|---|---|---|
| API feature examples | `src/test/resources/features/api_test.feature`; `src/test/resources/features/create_post_workflow_api.feature` | Existing API step-grammar examples. Only the workflow feature carries `@api`. |
| New API features | `src/test/resources/features/` | Add new API `.feature` files here or beneath a focused child directory. Tag them `@api`. |
| API request definitions | `src/test/resources/api_requests/api_requests.yml` | Required method and endpoint templates, addressed from Gherkin by dot-separated request key. |
| API service configuration | `src/test/resources/config/config.yml` | Required `api_services.<service_name>` values and API SSL behavior. |
| API payload source | Gherkin doc string in the feature | There is no dedicated API payload-resource directory or payload-file reader in the current API code path. The doc string is stored and Gson-serialized as described above. |
| API data source | No dedicated API data-resource path is consumed by `ApiSteps`, `ApiActionImpl`, or `ApiActionPerformer` | Existing `src/test/resources/data/` contains other-module files; it is not read by this API implementation. |
| UI locators | `src/test/resources/elements/` | UI resource area. API request execution does not resolve locators. |
| Database queries | `src/test/resources/queries/db_queries.yml` | Database resource area. API request execution does not execute query definitions. |
| Performance payloads | `src/test/resources/performance/payloads/` | Performance-module resource area. It is separate from the API step path. |

## Expected reports and artifacts

A Maven API run goes through the default TestNG Cucumber runner. Surefire injects Cucumber JSON and JUnit report destinations, and the runner configures pretty output, Cucumber HTML/JSON output, a rerun file, Extent reporting, per-feature reporting, and soft-assertion reporting. [16] [18]

| Artifact | Configured location | Notes |
|---|---|---|
| Cucumber JSON | `target/cucumber-reports/cucumber.json` | Supplied by Surefire’s `cucumber.plugin` system property. |
| Cucumber JUnit XML | `target/cucumber-reports/cucumber.xml` | Supplied by the same Surefire property. |
| Runner HTML | `target/cucumber-reports/cucumber-pretty` | Configured by `com.ptaf.runner.TestRunner`. |
| Runner JSON | `target/cucumber-reports/CucumberTestReport.json` | Configured by `com.ptaf.runner.TestRunner`. |
| Failed-scenario rerun list | `target/cucumber-reports/rerun.txt` | Configured by `com.ptaf.runner.TestRunner`. |
| Combined Extent Spark HTML | `test-output/<timestamp>/SparkReport/Spark.html` | Extent reporter uses a timestamped base folder. |
| Combined Base64 HTML | `test-output/<timestamp>/Base64Report/Report.html` | Enabled in `extent.properties`. |
| Combined PDF | `test-output/<timestamp>/PdfReport/FNB-PTAF-Report.pdf` | Enabled in `extent.properties`. |
| Combined Excel workbook | `test-output/<timestamp>/ExcelReport/FNB-PTAF-Report.xlsx` | Enabled in `extent.properties`. |
| Per-feature HTML | `test-output/per-feature-reports/<feature_name>_<timestamp>.html` | Enabled by the current reporting configuration. |
| Per-feature Glass-style PDF | `test-output/per-feature-reports-glass/<feature_name>_<timestamp>.pdf` | Enabled by the current reporting configuration. |

Browserless API scenarios do not initialize the browser stack, so UI screenshots and browser-video artifacts are not expected from this path. API diagnostics are emitted through SLF4J; treat logs and reports as potentially sensitive because the API classes can log request headers, serialized payloads, and response text. [4] [6] [8] [20]

## Troubleshooting

| Symptom | Code-supported cause | Resolution |
|---|---|---|
| A new API feature opens a browser or is not selected by the command | The default Maven command filters on `@api`; browserless detection also depends on API tags/path/filename. | Add `@api` to the feature or scenario and rerun `mvn clean test -Dcucumber.filter.tags="@api"`. |
| `Base URL ... was not found in config.yml` | The service key passed in Gherkin does not resolve to a nonblank `api_services.<service_name>.base_url`. | Check the service key, the environment-specific override path, and the root configuration entry without printing the endpoint in test evidence. |
| Token environment-variable error | A configured `auth_token_env` name was missing from the Maven process environment or its value was blank. | Set the variable through the approved secret mechanism before Maven starts. Never replace it with an inline feature token. |
| `Request definition ... not found or is incomplete` | The request key does not resolve to both `.method` and `.endpoint` in the API request YAML. | Align the Gherkin key with the nested YAML key and retain both required fields. |
| Unsupported HTTP-method error | The configured method is outside GET, POST, PUT, DELETE, or PATCH. | Use one of the five verbs implemented by `ApiActionPerformer`. |
| Placeholder remains in endpoint or endpoint is malformed | No matching path parameter was set, or a value required URL encoding. | Match the YAML placeholder name exactly and provide a URL-safe value before dispatch. |
| Unexpected body or server rejection of a JSON-object payload | The Cucumber doc string reaches Gson as a Java `String`, which is JSON-serialized as a string literal. | Inspect the implementation behavior before baselining expectations; do not assume raw doc-string JSON is parsed into an object. |
| Content type is surprising or duplicated | Automatic JSON content type checks only for exact `Content-Type` map-key casing. | Use the canonical `Content-Type` header spelling when setting it explicitly, or let the performer add it when appropriate. |
| JSONPath check fails | The response is empty/non-JSON, the expression is invalid, the path is absent, or the string form differs from the expected value. | First assert the expected status and, if useful, a non-sensitive body substring; then use a valid JSONPath and expected string representation. |
| API contexts appear to persist in a long-running API suite | `ApiRequestHandler` provides `disposeContext()`, but no invocation is present in the repository’s Java sources. | Treat this as a lifecycle caveat when designing long API runs. Do not claim automatic API-context disposal from the current source state. |
| Maven appears successful even after a failed scenario | Surefire has `testFailureIgnore` enabled. | Review Cucumber and Extent artifacts for final scenario status. |
| API steps are undefined when using the JUnit API runner | `ApiTestRunner` glue does not match the actual `com.ptaf.stepdefinitions.ApiSteps` package. | Use the documented Maven/TestNG route unless the implementation is intentionally corrected and validated separately. |

## Module boundaries

| Framework module | Boundary with API automation |
|---|---|
| UI web automation | UI scenarios use browser/page setup, locator YAML under `elements`, and UI step definitions. API scenarios are classified browserless and use an API request context instead; they do not consume UI locators. [4] [21] |
| Mobile and mobile-browser automation | Mobile/Appium lifecycle is separately detected by `Hooks` and `MobileHooks`. API tags must not be applied to mobile scenarios because API classification bypasses browser setup. [4] |
| Database automation | Database tests use database-specific steps, hooks, configuration, and SQL query resources. API request logic does not run `queries/db_queries.yml`. [22] |
| Performance automation | Performance scenarios have dedicated tags and resource paths. The API request performer is a functional HTTP path, not the JMeter performance execution engine. [4] [18] |
| Reporting | API runs share Cucumber, Extent, per-feature, and soft-assertion report integrations. Browserless execution changes browser artifacts, not the report listeners. [16] [20] |
| Configuration utilities | API consumes `config.yml` and `api_requests` through the shared YAML/configuration utilities. Changes to merging or environment override behavior affect other modules that use the same utilities. [12] [13] |

## References

<!-- Visible source-reference list -->
The sources below are visible and clickable in Markdown preview. Citation labels used in this guide point to the same source files.

- **[1]** [Browserless scenario detection and shared lifecycle hooks](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`
- **[2]** [Reusable API request definitions](../../src/test/resources/api_requests/api_requests.yml) — `../../src/test/resources/api_requests/api_requests.yml`
- **[3]** [Framework configuration including API service settings](../../src/test/resources/config/config.yml) — `../../src/test/resources/config/config.yml`
- **[4]** [Browserless API scenario classification](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`
- **[5]** [Cucumber API step definitions](../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`
- **[6]** [API facade and JUnit response validations](../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java) — `../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java`
- **[7]** [Stateful API action implementation](../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java) — `../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java`
- **[8]** [HTTP request assembly and dispatch](../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java) — `../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java`
- **[9]** [Thread-local Playwright API context and authentication handling](../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java) — `../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java`
- **[10]** [API response wrapper](../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java) — `../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java`
- **[11]** [API action contract](../../src/main/java/com/ptaf/api/interfaces/ApiAction.java) — `../../src/main/java/com/ptaf/api/interfaces/ApiAction.java`
- **[12]** [Merged YAML resource loader](../../src/main/java/com/ptaf/utils/YamlReader.java) — `../../src/main/java/com/ptaf/utils/YamlReader.java`
- **[13]** [Configuration lookup and environment override utility](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — `../../src/main/java/com/ptaf/utils/ConfigurationProperties.java`
- **[14]** [API GET and response-validation example](../../src/test/resources/features/api_test.feature) — `../../src/test/resources/features/api_test.feature`
- **[15]** [Tagged API workflow and request-body example](../../src/test/resources/features/create_post_workflow_api.feature) — `../../src/test/resources/features/create_post_workflow_api.feature`
- **[16]** [Default TestNG Cucumber runner](../../src/test/java/com/ptaf/runner/TestRunner.java) — `../../src/test/java/com/ptaf/runner/TestRunner.java`
- **[17]** [Default Maven TestNG suite](../../src/test/resources/testng.xml) — `../../src/test/resources/testng.xml`
- **[18]** [Maven dependencies and Surefire configuration](../../pom.xml) — `../../pom.xml`
- **[19]** [JUnit API runner configuration](../../src/test/java/com/ptaf/runners/ApiTestRunner.java) — `../../src/test/java/com/ptaf/runners/ApiTestRunner.java`
- **[20]** [Per-feature report artifact generation](../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) — `../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`
- **[21]** [UI locator resource directory](../../src/test/resources/elements/) — `../../src/test/resources/elements/`
- **[22]** [Database query resource](../../src/test/resources/queries/db_queries.yml) — `../../src/test/resources/queries/db_queries.yml`

<!-- Internal citation definitions used by the in-text [n] links. Keep these definitions so citations remain clickable. -->
[1]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Browserless scenario detection and shared lifecycle hooks"
[2]: ../../src/test/resources/api_requests/api_requests.yml "Reusable API request definitions"
[3]: ../../src/test/resources/config/config.yml "Framework configuration including API service settings"
[4]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Browserless API scenario classification"
[5]: ../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java "Cucumber API step definitions"
[6]: ../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java "API facade and JUnit response validations"
[7]: ../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java "Stateful API action implementation"
[8]: ../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java "HTTP request assembly and dispatch"
[9]: ../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java "Thread-local Playwright API context and authentication handling"
[10]: ../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java "API response wrapper"
[11]: ../../src/main/java/com/ptaf/api/interfaces/ApiAction.java "API action contract"
[12]: ../../src/main/java/com/ptaf/utils/YamlReader.java "Merged YAML resource loader"
[13]: ../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Configuration lookup and environment override utility"
[14]: ../../src/test/resources/features/api_test.feature "API GET and response-validation example"
[15]: ../../src/test/resources/features/create_post_workflow_api.feature "Tagged API workflow and request-body example"
[16]: ../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[17]: ../../src/test/resources/testng.xml "Default Maven TestNG suite"
[18]: ../../pom.xml "Maven dependencies and Surefire configuration"
[19]: ../../src/test/java/com/ptaf/runners/ApiTestRunner.java "JUnit API runner configuration"
[20]: ../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature report artifact generation"
[21]: ../../src/test/resources/elements/ "UI locator resource directory"
[22]: ../../src/test/resources/queries/db_queries.yml "Database query resource"
