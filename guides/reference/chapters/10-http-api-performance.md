# HTTP and GraphQL API Performance Testing Reference

This chapter documents the current HTTP/API performance implementation in `com.ptaf.performance` and the tester-facing `PerformanceSteps`. It is a reference for creating, executing, interpreting, and troubleshooting API load scenarios. It covers the **current source implementation**, not a generic JMeter tutorial.

> **Safety rule:** keep credentials, JWTs, API keys, private hosts, and personal test data out of feature files, YAML, CSV, Excel, terminal output, and generated reports. Use logical aliases and placeholders such as `<runtime-token>` in examples.

## Ownership and architecture

The framework uses the **JMeter Java DSL** and its dashboard module (both versioned by `jmeter.dsl.version` in Maven) rather than `.jmx` plans. Testers express a request and load intent through Gherkin; builders and `PerformanceEngine` convert it into and synchronously run a JMeter DSL test plan. [1] [2] [3]

| Layer | Source path | Responsibility | Tester boundary |
|---|---|---|---|
| Cucumber API | [`src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java`][4] | Defines Gherkin for GET/POST/PUT/DELETE, payload sources, authentication, expected failures, artifacts, and metric checks. | Use these steps rather than constructing JMeter objects in feature files. |
| Request/profile assembly | [`src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java`][8] and [`PerformanceProfileBuilder.java`][9] | Applies default endpoint/profile values, resolves a body, validates request/auth combinations, and creates immutable models. | One request contains one HTTP sampler; one profile controls its thread group. |
| JMeter DSL plan | [`src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java`][2] | Builds HTTP defaults, shared headers, a single sampler, a thread group, JTL writer, and HTML reporter. | There is no tester-authored `.jmx` or multi-request transaction model in this path. |
| Execution and analysis | [`src/main/java/com/ptaf/performance/core/PerformanceEngine.java`][3] | Creates report folders; runs the plan; parses JTL samples; applies thresholds; writes summaries and a run workbook. | Use this primary engine path for full metrics and reporting. |
| SLA assertion | [`src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java`][13] | Fails normal scenarios when error rate, mean latency, or P95 latency is **greater than** its configured maximum. | No response-body assertion, throughput threshold, or status-code-specific assertion is implemented here. |
| Outputs | [`src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java`][16] and [`PerformanceExcelReportWriter.java`][15] | Writes scenario/run TXT files and recreates one multi-sheet XLSX workbook per run. | Treat generated files as potentially sensitive: they include target URL, payload-source details, and failure messages. |

The separate [`PerformanceExecutionManager`][22] is explicitly a lower-level fallback: it runs a prepared plan but returns placeholder detailed metrics. It is **not** the preferred `PerformanceEngine` flow and should not be used when JTL-based measurements and complete reporting are required. [22]

### End-to-end execution flow

1. A Gherkin `When` step builds a `PerformanceRequest`; its protocol, host, port, content type, and accept type begin with values from the performance configuration. [4] [8]
2. If no inline body was supplied, the request builder resolves a YAML, CSV, or Excel body in the fixed priority **YAML → CSV → Excel**. Inline content always wins. [8]
3. The step either uses the configured default profile or builds a custom duration profile. The engine initializes one timestamped run root and a numbered, sanitized scenario directory. [3] [9]
4. `PerformanceTestPlanBuilder` assembles a single `DslHttpSampler`, HTTP defaults, resolved shared headers, an iteration or duration thread group, `jtlWriter`, and `htmlReporter`. [2]
5. `DslTestPlan.run()` blocks until completion. The engine parses the produced JTL, calculates sample/error counts and latency statistics, and evaluates three thresholds. [3] [13]
6. It finalizes the scenario even when a normal assertion fails, writes summaries, appends run-level text/index entries, adds the result to the run report, and rewrites `performance-run-report.xlsx`. A normal failure is then rethrown; expected-failure mode is returned as a result instead. [3]

Performance-tagged scenarios are treated as browserless by the shared hooks, so the current hook logic skips Playwright initialization for these scenarios. [25]

## Configuration: actual YAML keys and semantics

The performance-specific reader loads exactly the classpath resource `performance/config/performance-config.yml` once, supports dot-separated lookup, and fails during static initialization if the file is missing or empty. Missing numeric keys use the accessor fallback; malformed numeric content throws a parsing exception. [7] [6]

The current resource is [`src/test/resources/performance/config/performance-config.yml`][5]. Its keys and current meanings are:

| YAML key | Current value | Used for |
|---|---:|---|
| `performance.defaults.protocol` | `https` | Default request protocol. Required by plan validation to be `http` or `https`. |
| `performance.defaults.host` | a public example host | Default DNS host. Replace it with an approved non-production host for real tests; do not place a full URL here. |
| `performance.defaults.port` | `443` | Default port; the Java fallback is also `443` when this key is absent. |
| `performance.defaults.users` | `5` | Default virtual users. |
| `performance.defaults.rampUpSeconds` | `5` | Seconds to ramp to the user count. |
| `performance.defaults.holdSeconds` | `10` | Hold duration for duration mode only. |
| `performance.defaults.iterations` | `1` | Default makes the default profile iteration-based. |
| `performance.assertions.maxErrorPercent` | `5.0` | Maximum sampled error percentage. |
| `performance.assertions.maxAvgResponseTimeMs` | `5000` | Maximum average elapsed time in milliseconds. |
| `performance.assertions.maxP95ResponseTimeMs` | `7000` | Maximum nearest-rank P95 elapsed time in milliseconds. |
| `performance.reporting.resultsFolder` | configured but not used by `PerformanceEngine` | Exposed by `PerformanceConfigurationProperties`; it does not control the primary engine output root. |
| `performance.reporting.dashboardFolder` | configured but not used by `PerformanceEngine` | Exposed by `PerformanceConfigurationProperties`; it does not control the primary engine dashboard path. |

### Profile modes

A `PerformanceProfile` has `users`, `rampUpSeconds`, `holdSeconds`, and `iterations`. The builder validates `users > 0`, non-negative times/counts, and one of these modes: [9]

| Mode | Condition | JMeter DSL call | Operational result |
|---|---|---|---|
| Iteration-based | `iterations > 0` | `rampTo(users, rampUp).holdIterating(iterations)` | Each virtual user performs the configured iteration count. `holdSeconds` is retained in the model/report but is not used to hold the DSL thread group. |
| Duration-based | `iterations == 0` **and** `holdSeconds > 0` | `rampToAndHold(users, rampUp, hold)` | Users ramp up and hold at target concurrency for the supplied duration. |

A custom Gherkin profile always sets `iterations` to `0`; therefore its `users / ramp / hold` syntax is duration-based. The uncustomized default is iteration-based because the current `iterations` value is positive. [4] [5]

## Endpoint and sampler rules

The endpoint is intentionally componentized as **protocol + host + port + relative path**. The plan builder validates a nonblank path/protocol/host, port above zero, and a supported protocol. It rejects a host containing `://`, `/`, `?`, or `#`, and rejects paths containing `://`; it normalizes a missing leading slash before constructing `protocol://host:port/path`. [2]

```gherkin
# Safe: each endpoint component is supplied by the framework configuration
When we run GET performance test for path "/api/v1/items" with name "List items baseline"
```

Do **not** put a full URL in the feature path or host configuration. Keep the host to a DNS name or IP and the route relative. This prevents malformed doubled host/port URLs. Although the validation message says the host must not contain a port, the current condition does not explicitly test for a colon; follow the component model regardless. [2]

### HTTP method/body behavior

| Method/body case | Builder behavior |
|---|---|
| `POST` with a nonblank body | Uses the DSL `post(body, resolvedContentType)` convenience method. |
| `PUT` or `PATCH` with a nonblank body | Sets the method, content type, and body. |
| `GET`, `DELETE`, or another method with a body | The body is attached only for `POST`, `PUT`, or `PATCH`; do not assume a GET/DELETE body will be sent. |
| Missing/unknown content type | The DSL conversion defaults to `application/json`; only JSON, XML/text XML, plain text, and form URL-encoded have explicit mappings. Unknown strings also map to JSON. |
| No explicit request name | The sampler label becomes normalized `METHOD PATH`. |

These rules are implemented in the plan builder; the supported Cucumber API exposes GET, POST, PUT, and DELETE steps. [2] [4]

## Headers and authentication aliases

`PerformanceTestPlanBuilder` resolves headers in this order: explicit request headers, then `Accept` and `Content-Type` only if those exact keys are absent, then bearer authorization, then basic authorization. Bearer/basic resolution overwrites the `Authorization` entry. Header-map lookup is ordinary Java map lookup, so use canonical `Accept`, `Content-Type`, and `Authorization` casing rather than case variants that could create duplicate HTTP header names. [2]

| Configuration | Current behavior and safe practice |
|---|---|
| `Accept` / `Content-Type` | Both request-builder defaults are `application/json`. Explicit headers with those canonical keys take precedence over the corresponding modeled values. [8] [2] |
| Bearer token alias | Store a runtime token under a logical alias, then refer only to the alias. The engine trims an optional `Bearer ` prefix when storing; plan construction adds exactly one `Authorization: Bearer <token>` header. Missing/blank aliases fail fast. [3] [2] |
| Basic auth | The builder forbids configuring both a bearer alias and a basic-auth username. Basic auth is UTF-8 `username:password` Base64 encoded and written to `Authorization`; do not embed credentials in committed Gherkin. [8] [2] |
| `PerformanceHeaderManager` | This reusable class can merge default/request headers with request values winning and can apply auth, but the current `PerformanceSteps` → `PerformanceEngine` route uses the plan builder's own header resolution instead. It is not a substitute for a secret store. [12] |
| `PerformanceAuthTokenManager` | This separate reusable manager stores token/optional expiry metadata but does not call an auth API. The primary engine maintains its own alias-to-token map, so do not assume the two stores are wired together. [23] [3] |

Safe alias pattern:

```gherkin
When we store bearer token alias "service_access" with value "<runtime-token>"
And we run authenticated GET performance test for path "/api/v1/account" with name "Account read" using bearer token alias "service_access"
Then performance execution should pass
```

The placeholder represents externally injected runtime data. It must not be replaced by a real token in version-controlled feature files. The generated summaries report the authentication **type**, but report payload-source details and failure messages still deserve normal artifact access controls. [4] [16]

## Payload resolution: inline, YAML, CSV, and Excel

`PerformanceRequestBuilder` resolves a body only when its inline body is blank. Priority is deterministic: **inline body → YAML key → CSV cell → Excel cell**. Its report metadata still identifies the configured external source when one was requested. [8]

| Source | How it is addressed | Current lookup behavior | Use with care |
|---|---|---|---|
| Inline | `withJsonBody` / POST or PUT Gherkin | Passed as supplied after trimming. | Suitable only for small, nonsensitive test bodies. Escape feature syntax correctly. |
| YAML | Dot-delimited `yaml key` | `YamlReader` scans and recursively merges YAML from resource folders including `performance`; scalar values are preserved, while YAML maps/lists are serialized with Gson into valid JSON. | Prefer this for readable JSON/GraphQL bodies. Later-loaded YAML data can overwrite matching keys. [10] [21] |
| CSV | file, first-column row identifier, header column | The reader tries classpath path, classpath path without leading slash, filesystem path, then `src/test/resources/<path>`. It uses the first row as headers and first cell as case-insensitive row ID. | Its splitter is simple comma splitting, not a full CSV parser; do not place comma-rich JSON unless the current parser's simple quoting behavior is proven for your data. [11] |
| Excel | filesystem path, first-sheet row identifier, header column | `ExcelReader` opens the supplied path, always reads sheet index 0, finds a case-insensitive match in column 0, and returns the requested header's cell as `toString()`. | The supplied path must exist at runtime. Reader failures log and return `null`; the request builder does not itself reject a missing body. [12] |

Safe YAML-driven example:

```gherkin
When we run YAML-driven POST performance test for path "/api/v1/widgets" with name "Create widget load" using yaml key "performance.payloads.createWidget"
Then performance execution should pass
And performance total samples should be greater than 0
```

The actual YAML payload registry currently contains the `performance.payloads` subtree in [`src/test/resources/performance/payloads/yaml/performance-payloads.yml`][21]. Add only synthetic, non-identifying data to such registries. The current CSV has a row-ID column and a request-body column; the current workbook has a first-sheet header/first-column layout compatible with `ExcelReader`. [11] [12]

## GraphQL over the HTTP performance path

There is **no GraphQL-specific sampler, parser, or assertion step**. GraphQL is exercised as an HTTP `POST` with an application/json body. The safe pattern is therefore the authenticated YAML-driven POST step, a relative GraphQL route, a logical token alias, and a YAML value that serializes to the JSON transport shape required by the API (commonly query/variables/operation metadata). The framework does not parse GraphQL queries or inspect GraphQL error fields. [4] [2] [10]

```gherkin
@performance_testing
Scenario: GraphQL operation under a controlled load profile
  When we store bearer token alias "graphql_access" with value "<runtime-token>"
  And we run authenticated YAML-driven POST performance test for path "/graphql" with name "Create widget GraphQL" using yaml key "performance.payloads.createWidgetGraphql" and bearer token alias "graphql_access"
  Then performance execution should pass
  And performance total samples should be greater than 0
  And performance error percentage should be less than 5
```

### Current GraphQL discrepancy

[`performance.feature`][20] references the YAML key `navigator_graphql_create_vhlm`, but that key is not present in the current performance YAML payload registry. The resolver throws when `YamlReader.get(key)` yields no value. Consequently, that tagged feature is not runnable as written until a safe, valid registry entry is provided or the feature is aligned with an existing key. Do not fix this documentation by copying a real GraphQL request, token, or private endpoint into source. [20] [21] [10]

## Failure semantics, thresholds, and HTTP interpretation

### Normal versus expected failure

A normal `runHttpTest` validates thresholds after JTL parsing. An exceeded threshold throws `AssertionError`; the engine writes scenario/run reports first and then rethrows, so the test fails. In expected-failure mode, assertion and other execution failures are captured in the result rather than rethrown. [3] [13]

| Mode/outcome | Status | Interpretation |
|---|---|---|
| Normal run, all threshold checks pass | `PASS` | Healthy normal outcome. |
| Normal run, assertion or execution fails | `FAIL` | Unexpected failure; investigation is required. |
| Expected-failure run, a failure occurs | `EXPECTED_FAIL_CONFIRMED` | Pass-like negative-validation result: expected behavior was observed. |
| Expected-failure run, no failure occurs | `EXPECTED_FAIL_NOT_TRIGGERED` | Failure-like negative-validation result: the intended failure did not happen. |
| Not executed | `SKIPPED` | Status exists for reporting, but this engine path does not set it during its normal flow. |

These meanings and pass/failure-like classifications come from `PerformanceExecutionStatus`. [14] The tester-facing expected-failure steps currently cover GET, basic-auth GET, and YAML POST. [4]

### Configured thresholds

The assertion engine checks the following in order and fails on the first actual value that is **strictly greater than** its configured maximum: error percentage, average response time, then P95 response time. Equal values pass. [13]

| Metric | Derived from JTL | Current default maximum | Gherkin result checks available |
|---|---|---:|---|
| Error percentage | `totalErrors × 100 / totalSamples` | `5.0` | less-than and greater-than checks |
| Average response time | rounded mean of elapsed milliseconds | `5000 ms` | less-than check |
| P95 response time | sorted nearest-rank P95 elapsed milliseconds | `7000 ms` | less-than check |

The engine recognizes CSV JTLs with `elapsed` and `success` columns or XML JTL `<httpSample>`/`<sample>` records. It calculates min/average/P95/max from elapsed values; missing, blank, or unparsable JTL content produces zero metrics. [3]

> **Source-visible discrepancy on zero thresholds:** `PerformanceAssertionProfile` documents a zero threshold as disabled, but `PerformanceAssertionEngine` and `PerformanceEngine.buildThresholdBreachSummary` compare actual values directly with `>`; a zero therefore behaves as “allow no positive value” in the primary assertion path rather than reliably disabling the check. Treat zero as ambiguous until the implementation is reconciled. [26] [13] [3]

### HTTP 4xx/5xx interpretation

The engine **does not parse, classify, or report HTTP status codes**. It counts a JTL sample as an error only when its JTL `success` flag is `false`; `totalErrors`, error percentage, threshold decisions, and expected-failure behavior use that flag. Therefore:

- A 4xx or 5xx response is visible to this framework only to the extent the JMeter result marks the sample unsuccessful.
- The produced `PerformanceExecutionResult`, TXT summaries, and workbook do not retain a status-code breakdown.
- A GraphQL HTTP 200 response containing an application-level `errors` array is not evaluated by current code. It can appear successful at this layer unless the sampler/JMeter outcome marks it unsuccessful.

Use an expected-failure scenario to test a deliberately failing HTTP route, and add explicit `total errors` / error-percentage Gherkin checks when the scenario must prove failures were sampled. Do not claim a particular 4xx/5xx classification from this engine alone. [3] [4]

## Artifacts and how to read them

The primary engine creates a shared timestamped root under `test-output-performance-reports/<dd-MMM-yy_HH-mm-ss>/`, then scenario folders named `<two-digit-sequence>_<sanitized-request-name>/`. Each scenario folder receives the following paths: [3]

| Scope | Artifact | Contents/use |
|---|---|---|
| Scenario | `results.jtl` | Raw JMeter results. Engine consumes elapsed/success fields to calculate metrics. |
| Scenario | `dashboard/` | HTML dashboard generated by the JMeter DSL reporter. |
| Scenario | `summary.txt` | Technical structured text: request, profile, thresholds, metrics, risk, result flags, and artifact paths. |
| Scenario | `readable-summary.txt` | Narrative stakeholder-oriented counterpart. |
| Run | `run-summary.txt` | Appended technical entry per scenario. |
| Run | `run-readable-summary.txt` | Appended narrative entry per scenario. |
| Run | `run-index.txt` | Compact appended scenario/artifact index. |
| Run | `performance-run-report.xlsx` | Recreated run workbook after each finalized scenario. |

The workbook contains `Executive_Summary`, `Scenario_Summary`, `Risk_Analysis`, `Anomalies`, `Readable_Report`, `Charts`, and `Glossary` sheets. It aggregates run status counts, latency and error/risk views, recommendations, anomalies, and charts; it is not a source of raw HTTP status codes. [15] [24]

**Operational caveat:** the configured `performance.reporting.resultsFolder` and `dashboardFolder`, and `PerformanceConfigurationProperties.getReportsBaseDirectory()` are not the paths used by `PerformanceEngine`, which instead hard-codes `test-output-performance-reports`. Collect the latter in CI unless the implementation changes. [5] [6] [3]

## Runner and command reference

The repository contains a dedicated JUnit Cucumber entry point, [`PerformanceTestRunner`][17], which selects `@performance_testing`, feature files in `src/test/resources/features/performance`, and the shared step/hook packages. Run that class **as a JUnit test from an IDE** to use the dedicated performance runner. [17]

The current Maven routing is importantly different:

```bash
# Default Maven route: executes src/test/resources/testng.xml.
# Its TestNG Cucumber runner selects @eStore, not @performance_testing.
mvn clean test

# Dedicated UI/browser performance profile: switches to the UI performance TestNG suite.
# It is not the HTTP/GraphQL API performance runner.
mvn clean test -Pui_performance
```

There is **no Maven profile named for HTTP/API performance** and no TestNG suite that names `com.ptaf.runners.PerformanceTestRunner` in the current `pom.xml` / default `testng.xml`. Accordingly, the repository does not provide a source-backed Maven command that selects the HTTP performance runner; do not present either command above as an API-performance invocation. This is a runner/configuration discrepancy: the JUnit runner comments allow Maven/Gradle generally, but Maven Surefire is explicitly configured with the TestNG suite containing `com.ptaf.runner.TestRunner`, whose tag expression is `@eStore`. [17] [18] [19] [1]

## Troubleshooting guide

| Symptom | Check and likely cause | Source-backed response |
|---|---|---|
| `Performance config file not found on classpath` | The fixed `performance/config/performance-config.yml` classpath resource is absent from the test runtime. | Restore/package the resource; the reader fails during static initialization. [7] |
| Invalid endpoint / malformed URL | A full URL was put into `host`, or a full URL was supplied as `path`. | Use separate protocol/host/port and a relative route such as `/api/v1/resource`. [2] |
| Profile validation failure | `users <= 0`, negative values, or both `iterations` and `holdSeconds` are nonpositive. | Use `iterations > 0`, or `iterations = 0` and `holdSeconds > 0`. [9] |
| Missing bearer token alias | Alias was not stored in the same `PerformanceEngine` instance, or its stored value is blank. | Store the safe runtime token before the request; use aliases consistently and never print token values. [3] [2] |
| Unexpected auth result | Both auth styles were attempted, or an explicit `Authorization` header was assumed to win. | Choose bearer **or** basic auth; know that resolved bearer/basic auth overwrites `Authorization`. [8] [2] |
| YAML payload not found | Key is absent, wrong, or not loaded from the merged resource folders. | Use an existing safe dot key; the resolver throws for a missing value. Validate GraphQL feature keys against the registry. [10] [21] |
| CSV payload cannot be read | File cannot be found through the four resolution locations, the row is not in column 0, or the header name differs. | Use a classpath/resource or valid filesystem path, first-column row identifier, and exact logical column; avoid complex quoted/comma data. [11] |
| Excel payload is blank or null | Path, first sheet, first-column ID, or exact header is wrong. | Provide a runtime-accessible file path; use sheet 0, case-insensitive row ID, and the exact header. Reader logs and returns `null` on failures. [12] |
| GraphQL request fails before load starts | Feature references a YAML key that is not in the current registry. | Align the key with a valid safe payload definition; current GraphQL feature has this mismatch. [20] [21] |
| Result says zero samples/metrics | JTL is missing, empty, lacks `elapsed`/`success` CSV columns, has no matching XML samples, or could not be parsed. | Inspect `results.jtl` and dashboard. Add `Then performance total samples should be greater than 0` because zeroed metrics alone can satisfy positive thresholds. [3] [4] |
| 4xx/5xx not shown in Excel | Status-code details are not parsed by the engine. | Inspect raw JTL/dashboard/server telemetry; use error-count/percentage checks, not a nonexistent status-code report. [3] [15] |
| Normal negative scenario stops the suite | A failing route was executed through a normal, not expected-failure, step. | Use one of the `expecting failure` steps and assert expected-failure mode plus sampled errors. [4] [14] |
| `mvn clean test` did not run performance scenarios | Default TestNG suite targets `@eStore`. | Run the dedicated JUnit `PerformanceTestRunner` in the IDE; the current POM has no API-performance profile. [18] [19] [17] |
| Artifact folders are not in configured reporting folder | Primary engine ignores the YAML reporting folder fields. | Collect `test-output-performance-reports`, which is the actual engine root. [3] [5] [6] |

## Related chapters

- `09-api-functional-testing.md` — request/response validation and non-performance API behavior.
- `11-test-data-and-payloads.md` — broader test-data governance and source maintenance.
- `12-ui-performance-testing.md` — the separate browser-based `ui_performance` Maven profile and artifacts.
- `13-test-reporting-and-ci-artifacts.md` — cross-suite report collection and retention.

## Source references

- [Maven dependencies and execution profiles][1]
- [JMeter DSL plan construction][2]
- [Primary performance execution, JTL metrics, outcomes, and paths][3]
- [Tester-facing Gherkin steps][4]
- [Performance YAML configuration][5]
- [Payload resolution and GraphQL-safe structured-YAML serialization][10]
- [Cucumber performance runner and current GraphQL feature][17] [20]
- [Excel run-report writer][15]

## References

[1]: ../../../pom.xml "Maven dependencies, Surefire configuration, and profiles"
[2]: ../../../src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java "JMeter DSL HTTP test-plan builder"
[3]: ../../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java "Primary HTTP performance engine"
[4]: ../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java "Cucumber HTTP performance steps"
[5]: ../../../src/test/resources/performance/config/performance-config.yml "Current performance YAML configuration"
[6]: ../../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java "Performance configuration accessors"
[7]: ../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java "Performance-specific YAML reader"
[8]: ../../../src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java "HTTP performance request builder"
[9]: ../../../src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java "Performance load-profile builder"
[10]: ../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java "Performance payload resolver"
[11]: ../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java "CSV payload reader"
[12]: ../../../src/main/java/com/ptaf/utils/ExcelReader.java "Excel payload reader"
[13]: ../../../src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java "Performance SLA assertion engine"
[14]: ../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java "Performance execution statuses"
[15]: ../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java "Performance Excel run-report writer"
[16]: ../../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java "Performance technical and readable summary writer"
[17]: ../../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java "Dedicated JUnit Cucumber performance runner"
[18]: ../../../src/test/resources/testng.xml "Default Maven TestNG suite"
[19]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[20]: ../../../src/test/resources/features/performance/performance.feature "Current GraphQL performance feature"
[21]: ../../../src/test/resources/performance/payloads/yaml/performance-payloads.yml "Current performance YAML payload registry"
[22]: ../../../src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java "Fallback performance execution manager"
[23]: ../../../src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java "Reusable performance token manager"
[24]: ../../../src/main/java/com/ptaf/performance/models/PerformanceRunReport.java "Run-level performance aggregate model"
[25]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Shared hooks and browserless performance routing"
[26]: ../../../src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java "Performance assertion threshold model"
