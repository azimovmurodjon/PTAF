# API Automation Reference

## Purpose and boundary

This chapter documents the repository's **normal, functional API automation** path: the `com.ptaf.api` implementation, its Cucumber step layer, request-definition YAML, context lifecycle, response checks, browserless handling, and reports. It is intended for test authors, framework maintainers, reviewers, and CI owners who need to trace an API scenario from Gherkin to an HTTP response.

The path uses Playwright's `APIRequestContext` as an HTTP client. It is **not** browser-page automation and it is **not** the JMeter-based API load/performance engine. API performance/load design, thresholds, and JMeter artifacts belong to the planned `09-api-performance-testing.md` chapter and are intentionally not repeated here. The normal API performer measures and logs one request duration, but does not provide a load-test model. [4]

> **Security baseline:** keep real base URLs, credentials, bearer tokens, customer data, and sensitive request/response values out of feature files, YAML, shell history, logs, screenshots, and reports. Examples in this chapter use placeholders only.

## What is implemented

| Concern | Implemented responsibility | Primary source |
|---|---|---|
| Gherkin binding | `ApiSteps` exposes the supported request setup, dispatch, and response assertion expressions. | [1] |
| Test-facing facade | `ApiCommonMethods` delegates request work and makes JUnit 5 assertions for status, body text, headers, and JSONPath values. | [2] |
| Stateful API action | `ApiActionImpl` stores the next request's headers, path/query parameters, body, and last response in `ThreadLocal` state. | [3] |
| HTTP dispatch | `ApiActionPerformer` assembles Playwright `RequestOptions`, serializes a body with Gson, sends the request, and wraps the response. | [4] |
| API context | `ApiRequestHandler` lazily creates a per-thread Playwright instance and `APIRequestContext`, applying service base URL, SSL behavior, and optional bearer authentication. | [5] |
| Shared configuration | `YamlReader` merges YAML from selected resource folders; `ConfigurationProperties` resolves environment-specific overrides before root-level keys. | [6] [7] |
| Request metadata | `api_requests.yml` supplies reusable logical request keys with `method` and `endpoint`. | [9] |
| Browserless hook behavior | `Hooks` classifies API-only scenarios before desktop browser setup. | [8] |
| Response model | `ApiResponseWrapper` retains HTTP status, raw text body, and response headers. | [17] |

The API code is in `src/main/java/com/ptaf/api/`; Cucumber bindings are in `src/test/java/com/ptaf/stepdefinitions/`; API request definitions and API service configuration live under `src/test/resources/`. See [Source path navigation](#source-path-navigation) for clickable paths.

## Execution flow and state lifecycle

An API scenario moves through the following sequence.

1. **Cucumber binds Gherkin to `ApiSteps`.** The step class holds one `ApiCommonMethods` instance for the lifetime of that step-definition object. [1]
2. **Setup steps accumulate state for the next call.** Headers, path parameters, query parameters, and an optional body are forwarded to `ApiActionImpl`, where each is held in a `ThreadLocal`. The same thread also owns the last response. [2] [3]
3. **The dispatch step resolves YAML.** `ApiActionImpl.sendRequest(serviceName, requestKey)` reads `<requestKey>.method` and `<requestKey>.endpoint` from the merged YAML map. Missing either value causes `IllegalArgumentException`. [3] [6] [9]
4. **The handler obtains an API context.** On its first call in a thread, `ApiRequestHandler` creates `Playwright`, reads `api_services.<serviceName>.base_url`, parses `ignoreHTTPSErrors`, optionally reads the environment variable named by `auth_token_env`, and creates a Playwright `APIRequestContext`. [5] [7] [10]
5. **The performer builds and sends the HTTP request.** It replaces path placeholders, applies request headers and query parameters, serializes a non-null body with Gson, dispatches the configured verb, reads `response.text()`, and creates an `ApiResponseWrapper`. [4] [17]
6. **The last response becomes available to assertions.** Status, raw text body, headers, and JSONPath extraction are obtained from the last response for that same thread. [2] [3]
7. **Request-building state is cleared after a successful dispatch.** `ApiActionImpl` clears its header, path-parameter, query-parameter, and body state after `ApiActionPerformer.sendRequest(...)` returns. Set all required request inputs again before the next request. [3]

### Thread isolation and two important lifecycle limitations

Both request state and the Playwright API context are thread-local. This supports parallel scenarios without sharing headers, bodies, responses, or the context between threads. [3] [5]

However, the API context is **reused for later calls on the same thread**. `getContext(serviceName)` does not compare a later service name with the service that created the existing context. Therefore, do not assume that dispatching to a second configured service in the same thread creates a second base URL or a second automatic bearer-header configuration. Keep a thread's API calls scoped to one service unless the implementation is changed and verified. [5]

The handler provides `disposeContext()` to dispose the `APIRequestContext`, close Playwright, and remove its `ThreadLocal` entries. In the current Java sources, the handler is referenced by `ApiActionImpl`, but no caller of `disposeContext()` is present. The shared browserless hook teardown clears its own browserless scenario state and returns; it does not call this API cleanup method. Treat this as a source-visible lifecycle gap for long-running API suites. [5] [8]

## Browserless behavior

API scenarios are browserless from the perspective of the shared `Hooks` class. Before desktop browser initialization, a scenario is treated as API-only when any of the following is true:

- it has `@api`;
- it has a tag starting with `@api_` or `@api-`;
- its feature path contains `/api/`; or
- its feature file name contains `api`.

For such a scenario, `Hooks` logs the classification and skips the desktop browser, browser context, page, UI page helpers, and browser cleanup path. [8]

This does **not** mean that no Playwright object exists. `ApiRequestHandler` still creates a Playwright instance to own the `APIRequestContext`; it simply does not launch a browser or create a page. Consequently, normal API scenarios should not produce UI screenshots or Playwright browser-video artifacts through the browser hook. Cucumber and Extent reports still run because they are runner plugins, not browser features. [5] [8] [14] [16]

## Configuration and authentication

### API service keys

The active service configuration is [`src/test/resources/config/config.yml`][10]. The keys actually read by `ApiRequestHandler` are:

| Key | Required? | Runtime behavior |
|---|---:|---|
| `api_services.<service_name>.base_url` | Yes | Used as the API context's base URL. A missing or blank value fails context creation. |
| `api_services.<service_name>.auth_token_env` | No | When nonblank, its value is interpreted as an **environment-variable name**, not a token. The handler reads that variable and adds `Authorization: Bearer <value>` to context-level extra headers. Missing/blank environment values fail fast. |
| `ignoreHTTPSErrors` | No | Parsed with `Boolean.parseBoolean`; missing or unparseable content resolves to `false` through the accessor/default behavior. It is passed to the Playwright API context. |

The repository currently has `api_services` entries for a public sample service and an internal-CRM example; do not copy their endpoints or secrets into new scenarios. The safe shape for a new service is:

```yaml
api_services:
  <service_name>:
    base_url: "<service-base-url>"
    auth_token_env: "<TOKEN_ENVIRONMENT_VARIABLE_NAME>"

ignoreHTTPSErrors: "<true-or-false>"
```

For a protected service, populate the named variable through the approved secret mechanism before the test process starts. For example, the **shape** of a local shell assignment is:

```bash
export <TOKEN_ENVIRONMENT_VARIABLE_NAME>='<value-from-approved-secret-store>'
```

Do not replace this with a literal token in Gherkin. `ApiCommonMethods.setHeader` logs the supplied header name **and value** at INFO level, and the HTTP performer logs serialized bodies and response text at DEBUG level. Those logs can expose manually supplied credentials or sensitive test data. [2] [4] [5]

### Environment-specific lookup

`ConfigurationProperties.getValue(key)` first looks for `environments.<env>.<key>`, where the Java system property `env` defaults to `QA`; if that lookup is absent, it falls back to the root `key`. This applies to the nested API service paths passed by the handler as well as shared configuration keys. [7]

For example, a service lookup for `api_services.<service_name>.base_url` first checks `environments.<env>.api_services.<service_name>.base_url`, then the root `api_services.<service_name>.base_url`. This is lookup behavior, not a guarantee that such an override is currently defined in the resource. [7]

### TLS/SSL decision

`ignoreHTTPSErrors` is an API-context option, not a per-Gherkin-step option. Use `false` for strict certificate validation. A value of `true` allows the Playwright API context to ignore HTTPS certificate errors, which may be necessary in non-production environments but should be explicitly approved. [5] [7]

## API request YAML

### Location, schema, and lookup

Reusable request definitions are in [`src/test/resources/api_requests/api_requests.yml`][9]. The current file demonstrates grouped keys such as `jsonplaceholder_requests` and `crm_requests`. A Gherkin request key is a dot-separated path into that YAML structure.

```yaml
<request_group>:
  <request_name>:
    method: "<GET-or-POST-or-PUT-or-DELETE-or-PATCH>"
    endpoint: "/<resource>/{<path_parameter_name>}"
```

`method` and `endpoint` are both required. `ApiActionImpl` reads them as `<request_key>.method` and `<request_key>.endpoint`; a missing or incomplete definition throws `IllegalArgumentException`. [3] [9]

`YamlReader` scans `.yml` and `.yaml` files under the classpath folders `elements`, `queries`, `api_requests`, `config`, and `performance`, recursively merges the parsed maps, and resolves dot-separated key paths. A later file can overwrite a non-map value or recursively merge a map value with an earlier one. Keep top-level key names unique across those scanned folders unless an override is intentional and reviewed. [6]

### HTTP request assembly rules

| Input | How it is set | Exact implementation behavior | Authoring consequence |
|---|---|---|---|
| HTTP verb | YAML `method` | `GET`, `POST`, `PUT`, `DELETE`, and `PATCH` are supported case-insensitively. Any other verb throws `IllegalArgumentException`. | Keep normal functional definitions within these five verbs. |
| Endpoint | YAML `endpoint` | Used relative to the configured API context base URL. | Keep service base URL in configuration and endpoint path in request YAML. |
| Path parameter | Gherkin step / action method | Each `{name}` occurrence is literal string-replaced when a matching key exists; missing keys leave placeholders unchanged. Values are not URL-encoded. | Match the YAML placeholder exactly and provide URL-safe/pre-encoded values. |
| Query parameter | Gherkin step / action method | Applied through Playwright `RequestOptions`; values are converted with `String.valueOf(...)`. In programmatic use, `null` becomes the literal text `"null"`. | Do not rely on null omission; set a concrete value or omit the step. |
| Custom header | Gherkin step / action method | Stored in a map for the next request; a repeated exact key replaces its prior map value. | Never place tokens or sensitive values in this step because the facade logs the value. |
| Request body | Gherkin doc string or programmatic `Object` | Any non-null body is passed to `Gson.toJson(...)` then `RequestOptions.setData(...)`. | The Cucumber path passes a `String`; see the body-source caveat below. |
| Content type | Header map and performer | With a non-null body, `Content-Type: application/json` is added only when the map lacks the **exact** key `Content-Type`. | Use canonical `Content-Type` spelling when you set it; `Content-type` does not suppress the automatic default. |

The automatic bearer header configured at context creation and a scenario-supplied `Authorization` header are two separate layers. The framework code does not document a collision policy between them; avoid defining both for one request unless the intended Playwright behavior is verified in your environment. [4] [5]

## Body sources and payload caveat

The normal Cucumber API path has exactly one implemented body source: the doc string supplied to `And I set the request body to`. `ApiSteps` forwards that doc string unchanged to `ApiCommonMethods`, which forwards it to `ApiActionImpl` as a Java `String`. The performer then serializes that `String` with Gson. [1] [2] [3] [4]

That distinction matters: a doc string containing JSON-looking text is **not parsed into a JSON object** by the current step implementation. Gson serializes a Java string as a JSON string literal. Do not assume the feature example's multiline JSON text is transmitted as an object merely because it looks like JSON. If an endpoint requires a JSON object, this is a source-visible implementation limitation that must be addressed through a reviewed framework change or an appropriate programmatic path; it is not solved by changing indentation in Gherkin. [1] [4] [19]

`ApiActionImpl.setRequestBody(Object)` can accept a map, POJO, string, or another Gson-serializable object when called from Java. No normal API code reads a dedicated API payload directory, JSON fixture, CSV fixture, or multipart/file-upload source. The performance payload folders are owned by the separate performance module and are not consumed by `ApiSteps`, `ApiActionImpl`, or `ApiActionPerformer`. [3] [4]

## Gherkin vocabulary and safe examples

### Implemented step expressions

| Purpose | Exact Gherkin form | Validation/dispatch behavior |
|---|---|---|
| Add a request header | `Given I set the request header "{string}" to "{string}"` | Adds/replaces one header for the next request. |
| Add a path parameter | `And I set the path parameter "{string}" to "{string}"` | Replaces matching `{name}` in the YAML endpoint. |
| Add a query parameter | `And I set the query parameter "{string}" to "{string}"` | Adds one query parameter for the next request. |
| Set a body | `And I set the request body to` followed by a doc string | Stores raw doc-string text as a Java string. |
| Dispatch | `When I send a "{string}" request to the "{string}" service` | Resolves YAML, obtains context, and sends the request. |
| Assert status | `Then the response code should be {int}` | Exact integer equality through JUnit 5. |
| Assert body text | `And the response body should contain the text "{string}"` | Simple substring assertion. |
| Assert response header | `And the response header "{string}" should be "{string}"` | Header name lookup is case-insensitive; expected value is exact. |
| Assert JSONPath value | `And the value of the JSON path "{string}" should be "{string}"` | Jayway JSONPath result is converted to a string and compared exactly. |

These are the only API Gherkin bindings in `ApiSteps`. Do not invent inline HTTP methods, URLs, status ranges, authentication steps, JSON file body steps, response-schema steps, or retry steps without adding and validating corresponding implementation. [1]

### Safe GET example

The following uses placeholders and an existing request-key pattern without putting a real URL, identity, token, or production value in the feature.

```gherkin
@api
Feature: <API capability>

  Scenario: Retrieve a resource by identifier
    Given I set the request header "Accept" to "application/json"
    And I set the path parameter "<path_parameter_name>" to "<url-safe-test-identifier>"
    And I set the query parameter "<query_parameter_name>" to "<query-value>"
    When I send a "<request_group>.<request_name>" request to the "<service_name>" service
    Then the response code should be 200
    And the response header "Content-Type" should be "<expected-content-type>"
    And the response body should contain the text "<non-sensitive-expected-text>"
    And the value of the JSON path "$.<field_name>" should be "<expected-value>"
```

Use only the setup steps a particular endpoint needs. Every response assertion must occur after the dispatch step: before a request has been sent, `getLastResponse()` throws `IllegalStateException`. [1] [3]

### Body syntax example — and why it needs care

This is the supported syntax for passing body **text** from a feature:

```gherkin
And I set the request body to
  """
  <payload-text>
  """
```

It is safe as a grammar example, but it is not evidence that `<payload-text>` will be parsed as a JSON object. See [Body sources and payload caveat](#body-sources-and-payload-caveat) before using this path for POST, PUT, or PATCH requests. [1] [4]

## Response capture and validation

`ApiActionPerformer` reads the complete response through `response.text()` and creates `ApiResponseWrapper(status, body, headers)`. The wrapper exposes the numeric status, raw body string, and the response-header map; it does not parse JSON or make a defensive copy of the header map. Avoid unbounded/very large response bodies in this functional path because the body is loaded into memory as text. [4] [17]

| Check | What the framework actually compares | Practical guidance |
|---|---|---|
| Status | Expected `int` versus `getResponseStatusCode()` | Use exact expected status, including expected negative-test outcomes. |
| Body contains | `responseBody.contains(expectedText)` | Good for a small, non-sensitive text signal; it is not JSON-schema validation. |
| Response header | Case-insensitive header-name search, then exact value equality | Some servers append charset or parameters; baseline the entire expected returned value. |
| JSONPath value | `JsonPath.read(body, expression)`, then `Objects.toString(actual, null)` versus expected Gherkin string | Use only for JSON bodies and expect the string form of the result. |

An empty response body causes `getValueFromResponse` to return `null` after a warning. Invalid JSON, an invalid JSONPath expression, or an unparsable body is logged and rethrown as `RuntimeException`. A missing JSONPath normally leads to the subsequent exact string assertion failing. [2] [3]

## Execution entry points and source-visible discrepancies

### Default Maven suite is not the dedicated API selector

The Maven Surefire configuration uses `src/test/resources/testng.xml`. That suite names `com.ptaf.runner.TestRunner`, whose `@CucumberOptions` scans `com.ptaf.stepdefinitions` and `com.ptaf.hooks` but currently filters scenarios with `tags = "@eStore"`. Therefore the default command below runs the configured default TestNG suite; it does **not**, by source configuration alone, select the normal API suite. Surefire is also configured with `testFailureIgnore=true`, so a successful Maven process exit is not enough evidence of a passing test run. [11] [12] [13]

```bash
# Compile test sources without executing scenarios.
mvn -DskipTests test-compile

# Run the repository's current default TestNG suite (configured for @eStore, not API).
mvn clean test
```

### Dedicated JUnit API runner needs correction before reliance

`com.ptaf.runners.ApiTestRunner` has API-specific Cucumber options: it filters `@api`, searches feature files under `src/test/resources/features`, and configures `target/api-cucumber-reports.html`, Extent, timeline, and report-listener plugins. [14]

There is an important discrepancy: its glue is `com/ptaf/api/stepdefinitions`, while the implemented API steps are in package `com.ptaf.stepdefinitions` at `src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`. No matching `com.ptaf.api.stepdefinitions` directory exists in the current source tree. As written, the dedicated JUnit runner should not be treated as a validated path for the existing step class; unresolved bindings are a likely result. [1] [14]

The current `api_test.feature` has an API-identifying filename and will be browserless by hook classification, but it is not tagged `@api`. In contrast, `create_post_workflow_api.feature` is tagged `@api`. That difference matters because **browserless classification** and **runner selection** are separate mechanisms. [8] [18] [19]

## Generated reports and artifacts

Report output is configured by the runner plugins, Surefire, `extent.properties`, `config.yml`, and the per-feature listener. The table below describes **configured outputs** for a run that reaches the relevant runner/plugin; it does not guarantee artifacts when compilation, binding, setup, or report generation fails.

| Artifact | Configured path | Source-backed condition/notes |
|---|---|---|
| API-runner Cucumber HTML | `target/api-cucumber-reports.html` | Configured only by `ApiTestRunner`; the runner has the glue discrepancy above. [14] |
| Default TestNG Cucumber HTML | `target/cucumber-reports/cucumber-pretty` | Configured by `com.ptaf.runner.TestRunner`. [13] |
| Default TestNG Cucumber JSON | `target/cucumber-reports/CucumberTestReport.json` | Configured by `com.ptaf.runner.TestRunner`. [13] |
| Default TestNG rerun list | `target/cucumber-reports/rerun.txt` | Configured by `com.ptaf.runner.TestRunner`. [13] |
| Surefire Cucumber JSON/XML | `target/cucumber-reports/cucumber.json` and `target/cucumber-reports/cucumber.xml` | Added through Surefire's `cucumber.plugin` system property. [11] |
| Timeline output | `test-output-thread/` | Configured by the API JUnit and default TestNG Cucumber runners. [13] [14] |
| Combined Extent Spark HTML | `test-output/<timestamp>/SparkReport/Spark.html` | Enabled in `extent.properties`; base folder uses `dd-MMM-yy_HH-mm-ss`. [15] |
| Combined Extent Base64 HTML | `test-output/<timestamp>/Base64Report/Report.html` | Enabled in `extent.properties`. [15] |
| Combined Extent PDF | `test-output/<timestamp>/PdfReport/FNB-PTAF-Report.pdf` | Enabled in `extent.properties`. [15] |
| Combined Extent Excel | `test-output/<timestamp>/ExcelReport/FNB-PTAF-Report.xlsx` | Enabled in `extent.properties`. [15] |
| Per-feature HTML | `test-output/per-feature-reports/<feature-name>_<timestamp>.html` | Enabled by current `reporting.per_feature_reports_enabled`; listener names output from the `Feature:` declaration. [10] [16] |
| Per-feature Glass-style PDF | `test-output/per-feature-reports-glass/<feature-name>_<timestamp>.pdf` | Enabled by current `reporting.per_feature_glass_pdf_enabled`. [10] [16] |

The current configuration disables the listener's direct per-feature PDF option (`per_feature_pdf_enabled: false`) while enabling the separate Glass-style PDF option. The listener creates its output directories as needed and ignores the per-feature report path entirely when `per_feature_reports_enabled` is false. [10] [16]

Treat reports and logs as potentially sensitive. Browserless API classification means browser screenshots/videos are not expected, but an API report can still include step text, failures, and values that were placed in Gherkin; API logging can include headers, serialized request bodies, and raw response bodies. [2] [4] [8]

## Troubleshooting

| Symptom | Source-backed cause | Corrective action |
|---|---|---|
| `Base URL for API service ... was not found` | `api_services.<service>.base_url` was absent or blank after configuration lookup. | Confirm the Gherkin service key, the root YAML entry, and any intended `environments.<env>` override. Do not expose the real URL in defects or logs. [5] [7] |
| Token-environment-variable error | `auth_token_env` was nonblank but the named process environment variable was missing or empty. | Supply the variable through an approved secret mechanism before launching the test process; never inline the token in a feature. [5] |
| Request definition missing/incomplete | The request key did not resolve to both `method` and `endpoint`. | Match the Gherkin key exactly to the nested YAML key and retain both fields. [3] [9] |
| Unsupported HTTP method | YAML requested a method other than GET, POST, PUT, DELETE, or PATCH. | Use a supported method or extend the performer through a reviewed change. [4] |
| `{placeholder}` remains in the endpoint | No corresponding path parameter was set, or its name did not match. | Match the placeholder spelling exactly; pass a URL-safe/pre-encoded value. [4] |
| Query contains `null` | A programmatic caller supplied Java `null`; the performer applies `String.valueOf(null)`. | Omit the parameter or supply the intended explicit value. [4] |
| Unexpected content type or duplicate-looking header | The auto-default check recognizes only exact `Content-Type` casing; a differently cased key does not suppress it. | Use canonical `Content-Type` if setting it explicitly. Do not infer remote-server duplicate-header handling from this framework code. [4] |
| Object payload is rejected or arrives quoted | Cucumber doc-string payload reaches Gson as a Java `String`. | Do not assume JSON text is parsed. Escalate a suitable body-object/fixture capability as a framework change if required. [1] [4] |
| `No API request has been sent yet` | A response getter/assertion was used before dispatch in the current thread. | Build the request, send it, then assert. [3] |
| JSONPath assertion fails | The body is empty/non-JSON, the path is invalid/missing, or its string form differs from the expected Gherkin value. | First check status and an appropriate non-sensitive body signal; then correct the JSONPath/expected string. [2] [3] |
| Browser opens for a supposed API scenario | The scenario does not satisfy API tag/path/filename detection. | Apply `@api` at feature/scenario level and keep the API feature name/path unambiguous. [8] |
| Browserless scenario is not selected by the runner | Browserless classification does not select scenarios. The default TestNG runner filters `@eStore`; `api_test.feature` itself has no `@api` tag. | Reconcile suite selection intentionally; do not mistake hook classification for runner filtering. [13] [18] [19] |
| API steps are undefined under `ApiTestRunner` | Its glue package does not match the actual `ApiSteps` package. | Correct and validate the runner glue in a code change before relying on that runner. [1] [14] |
| Maven returns success despite failed scenarios | Surefire has `testFailureIgnore=true`. | Review Cucumber, Extent, per-feature, and Surefire report artifacts rather than the process exit code alone. [11] |
| Contexts persist across a long suite | `disposeContext()` exists but has no current Java caller. | Treat cleanup as a framework maintenance item; do not claim automatic API-context teardown. [5] [8] |

## Source path navigation

| Need to inspect | Repository path | Why it matters |
|---|---|---|
| Gherkin contract | [`src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`][1] | Exact step expressions and doc-string handling. |
| Assertion facade | [`src/main/java/com/ptaf/api/methods/ApiCommonMethods.java`][2] | Status/body/header/JSONPath checks and logging. |
| Request/response state | [`src/main/java/com/ptaf/api/implementation/ApiActionImpl.java`][3] | YAML lookup, request-state reset, last response, and JSONPath extraction. |
| HTTP assembly | [`src/main/java/com/ptaf/api/performer/ApiActionPerformer.java`][4] | Supported verbs, body serialization, parameters, headers, and response capture. |
| Context/auth lifecycle | [`src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java`][5] | Per-thread context, base URL, SSL, bearer token, and disposal API. |
| YAML resolution | [`src/main/java/com/ptaf/utils/YamlReader.java`][6] | Scanned folders, merging, and dot-path lookup. |
| Configuration resolution | [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`][7] | Environment override order and reporting key accessors. |
| Browserless decision | [`src/main/java/com/ptaf/hooks/Hooks.java`][8] | API detection and no-browser lifecycle. |
| Request catalog | [`src/test/resources/api_requests/api_requests.yml`][9] | Logical request key, verb, endpoint template. |
| API/report configuration | [`src/test/resources/config/config.yml`][10] | `api_services`, TLS, and per-feature reporting settings. |
| Maven/suite entry point | [`pom.xml`][11], [`src/test/resources/testng.xml`][12], and [`src/test/java/com/ptaf/runner/TestRunner.java`][13] | Default execution and its current non-API filter. |
| Dedicated API runner | [`src/test/java/com/ptaf/runners/ApiTestRunner.java`][14] | API selection/report configuration and glue discrepancy. |
| Report locations | [`src/test/resources/extent.properties`][15] and [`src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`][16] | Timestamped combined and per-feature artifact behavior. |

## Related chapters

The following chapters identify adjacent ownership boundaries in this guide library. Use the linked chapter that owns the behavior rather than duplicating its configuration or report instructions.

- `01-foundation-configuration-and-execution.md` — Maven, Cucumber, environment, and common framework configuration.
- `03-ui-web-automation.md` — browser/page/lifecycle and locator-driven UI automation.
- `05-database-automation.md` — database-specific configuration, queries, and steps.
- `09-api-performance-testing.md` — JMeter API load/performance scenarios and results; this chapter does not cover that engine in depth.
- `11-reporting-evidence-and-artifacts.md` — cross-module reporting/evidence conventions beyond the API artifacts listed here.

## Source references

- **[1]** [API Cucumber step definitions](../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java)
- **[2]** [API facade and response validations](../../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java)
- **[3]** [Stateful API action implementation](../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java)
- **[4]** [HTTP request assembly and dispatch](../../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java)
- **[5]** [Per-thread Playwright API context and authentication](../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java)
- **[6]** [Merged YAML resource loader](../../../src/main/java/com/ptaf/utils/YamlReader.java)
- **[7]** [Configuration lookup and environment overrides](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java)
- **[8]** [Shared browserless scenario lifecycle](../../../src/main/java/com/ptaf/hooks/Hooks.java)
- **[9]** [Reusable API request definitions](../../../src/test/resources/api_requests/api_requests.yml)
- **[10]** [API services and reporting configuration](../../../src/test/resources/config/config.yml)
- **[11]** [Maven dependencies and Surefire configuration](../../../pom.xml)
- **[12]** [Default Maven TestNG suite](../../../src/test/resources/testng.xml)
- **[13]** [Default TestNG Cucumber runner](../../../src/test/java/com/ptaf/runner/TestRunner.java)
- **[14]** [Dedicated JUnit API runner](../../../src/test/java/com/ptaf/runners/ApiTestRunner.java)
- **[15]** [Extent report output configuration](../../../src/test/resources/extent.properties)
- **[16]** [Per-feature report listener](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java)
- **[17]** [API response wrapper](../../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java)
- **[18]** [Current API GET feature example](../../../src/test/resources/features/api_test.feature)
- **[19]** [Current tagged API workflow example](../../../src/test/resources/features/create_post_workflow_api.feature)

## References

[1]: ../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java "API Cucumber step definitions"
[2]: ../../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java "API facade and response validations"
[3]: ../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java "Stateful API action implementation"
[4]: ../../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java "HTTP request assembly and dispatch"
[5]: ../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java "Per-thread Playwright API context and authentication"
[6]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "Merged YAML resource loader"
[7]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Configuration lookup and environment overrides"
[8]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Shared browserless scenario lifecycle"
[9]: ../../../src/test/resources/api_requests/api_requests.yml "Reusable API request definitions"
[10]: ../../../src/test/resources/config/config.yml "API services and reporting configuration"
[11]: ../../../pom.xml "Maven dependencies and Surefire configuration"
[12]: ../../../src/test/resources/testng.xml "Default Maven TestNG suite"
[13]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[14]: ../../../src/test/java/com/ptaf/runners/ApiTestRunner.java "Dedicated JUnit API runner"
[15]: ../../../src/test/resources/extent.properties "Extent report output configuration"
[16]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature report listener"
[17]: ../../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java "API response wrapper"
[18]: ../../../src/test/resources/features/api_test.feature "Current API GET feature example"
[19]: ../../../src/test/resources/features/create_post_workflow_api.feature "Current tagged API workflow example"
