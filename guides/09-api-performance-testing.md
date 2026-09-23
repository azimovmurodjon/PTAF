# FNB-ETAF API Performance Testing

## Purpose and scope

This guide documents the **API performance module** in FNB-ETAF. The module drives HTTP load tests through the JMeter Java DSL, with Cucumber scenarios as the tester-facing interface. It supports GET, POST, PUT, and DELETE requests; default or scenario-specific load profiles; inline, YAML, CSV, and Excel payload selection; bearer-token aliases; Basic authentication; expected-failure execution; threshold validation; and run-level reporting.[1] [2]

This is a **separate execution path** from the framework's functional API automation. It does not consume reusable entries in `api_requests.yml`, API-service definitions in the shared configuration, UI locators, database query YAML, or the UI performance module. Its request defaults come from the performance configuration and its test runner is `com.ptaf.runners.PerformanceTestRunner`.[3] [4] [5]

> **Security rule — mandatory.** Target credentials, bearer tokens, client data, and private target endpoint values **must never be recorded in feature files, YAML, CSV, Excel workbooks, source files, command history, logs, JTL files, dashboards, text summaries, Excel reports, PDFs, or shared defect evidence.** Use only placeholders in version-controlled examples and reports. The current Cucumber bearer-token step accepts a literal value and the current performance module does not resolve a token from an environment variable; therefore, do **not** put a real token into that step. Do not run an authenticated target scenario until an approved secret-injection mechanism exists outside version-controlled test assets.[1] [6]

The existing GraphQL feature contains only a placeholder token. It is a usage illustration, not an authorization pattern to populate with a real token. This guide intentionally uses placeholder hosts, paths, aliases, payloads, and IDs throughout.

## Architecture and source locations

| Concern | Package, class, or resource | Supported behavior | Source |
|---|---|---|---|
| Build and load-test dependencies | Maven project; JMeter Java DSL and dashboard dependencies | Java source/target level is 21. The project declares `us.abstracta.jmeter:jmeter-java-dsl` and `jmeter-java-dsl-dashboard`, both at `${jmeter.dsl.version}`. | [7] |
| Performance runner | `com.ptaf.runners.PerformanceTestRunner` | JUnit 4 Cucumber runner. Discovers `src/test/resources/features/performance`, scans `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and selects `@performance_testing`. | [3] |
| Tester-facing Gherkin steps | `com.ptaf.stepdefinitions.PerformanceSteps` | Converts supported performance steps into `PerformanceRequest` and `PerformanceProfile` objects; retains the latest result for assertions. | [1] |
| Request and profile builders | `com.ptaf.performance.builders.PerformanceRequestBuilder`, `PerformanceProfileBuilder` | Apply configuration defaults, resolve a payload, validate request/profile shape, and produce immutable models. | [8] [9] |
| JMeter plan construction | `com.ptaf.performance.builders.PerformanceTestPlanBuilder` | Builds one HTTP sampler, a JMeter thread group, JTL writer, and HTML dashboard. Resolves headers and authentication immediately before the plan is run. | [10] |
| Execution and threshold evaluation | `com.ptaf.performance.core.PerformanceEngine`; `com.ptaf.performance.assertions.PerformanceAssertionEngine` | Runs the plan synchronously, parses JTL metrics, compares error rate, average response time, and P95 to configured thresholds, and writes scenario and run artifacts. | [11] [12] |
| Performance configuration | `com.ptaf.performance.config.PerformanceYamlReader`, `PerformanceConfigurationProperties`; `src/test/resources/performance/config/performance-config.yml` | Loads the performance configuration once from the classpath. Provides request defaults, load defaults, and assertion defaults. | [13] [14] |
| YAML payload resolution | `com.ptaf.performance.payloads.PerformancePayloadResolver`; `com.ptaf.utils.YamlReader`; `src/test/resources/performance/payloads/yaml/performance-payloads.yml` | Looks up a dot-separated key across the merged resource folders. Scalars remain text; YAML maps/lists are serialized to JSON. | [15] [16] [17] |
| CSV payload resolution | `com.ptaf.performance.payloads.CsvPayloadReader`; `src/test/resources/performance/payloads/csv/customers.csv` | Reads a header row, treats the first data column as the row identifier, and retrieves a named column. | [18] [19] |
| Excel payload resolution | `com.ptaf.utils.ExcelReader`; `src/test/resources/performance/payloads/excel/performance_payloads.xlsx` | Reads the first worksheet only; the first row is the header row and the first column is the row identifier. | [20] [21] |
| Bearer-token aliases and headers | `com.ptaf.performance.core.PerformanceEngine`; `com.ptaf.performance.headers.PerformanceHeaderManager`; `com.ptaf.performance.auth.PerformanceAuthTokenManager` | The runner path stores aliases in an engine-local concurrent map and resolves them to the `Authorization` header when the plan is built. | [11] [22] [23] |
| Performance artifacts | `com.ptaf.performance.reports.PerformanceSummaryWriter`, `PerformanceExcelReportWriter`; `com.ptaf.performance.utils.PerformancePathResolver` | Produces JTL, JMeter dashboard, scenario summaries, aggregate summaries/index, and a run-level Excel workbook. | [24] [25] [26] |
| Browser isolation | `com.ptaf.hooks.Hooks` | Performance-tagged Cucumber scenarios are treated as browserless; the shared Playwright browser stack is skipped. | [27] |

## Prerequisites and guardrails

The repository currently compiles for **Java 21** and uses Maven. Maven resolves the JMeter DSL, dashboard, SnakeYAML, Gson, Apache POI, Cucumber, JUnit 4, and reporting libraries through `pom.xml`.[7] The test operator also needs written authorization for the target environment and the proposed virtual-user volume. Start with a non-production target and a small profile.

Before any target execution, configure `protocol`, `host`, and `port` independently in the performance configuration. The host must be only a DNS name or IP address. It must not contain a scheme, port, slash, query string, fragment, or endpoint route. The feature step supplies a **relative** route. The plan builder rejects full URLs placed in either the host or path field.[10]

Use non-sensitive, synthetic test data. Do not use a client identifier, client payload, account data, production record, real username, password, token, or unredacted private hostname in a feature, payload resource, spreadsheet, scenario name, or report. A descriptive test name becomes a report folder name after sanitization, so it must also remain non-sensitive.[11] [26]

## Configuration files and settings

### Performance configuration

The performance module reads exactly `performance/config/performance-config.yml` from the test classpath when `PerformanceYamlReader` is initialized. A missing or empty file fails initialization. Values are accessed with dot notation.[13]

| YAML key | Used by | Current behavior when absent |
|---|---|---|
| `performance.defaults.protocol` | `PerformanceRequestBuilder` | No fallback; request validation rejects blank protocol. |
| `performance.defaults.host` | `PerformanceRequestBuilder` | No fallback; request validation rejects blank host. |
| `performance.defaults.port` | `PerformanceRequestBuilder` | Defaults to `443`. |
| `performance.defaults.users` | `PerformanceProfileBuilder` and default engine path | Defaults to `1`. |
| `performance.defaults.rampUpSeconds` | `PerformanceProfileBuilder` and default engine path | Defaults to `1`. |
| `performance.defaults.holdSeconds` | `PerformanceProfileBuilder` and default engine path | Defaults to `1`. |
| `performance.defaults.iterations` | `PerformanceProfileBuilder` and default engine path | Defaults to `1`. |
| `performance.assertions.maxErrorPercent` | `PerformanceAssertionEngine` | Defaults to `1.0`. |
| `performance.assertions.maxAvgResponseTimeMs` | `PerformanceAssertionEngine` | Defaults to `2000`. |
| `performance.assertions.maxP95ResponseTimeMs` | `PerformanceAssertionEngine` | Defaults to `3000`. |
| `performance.reporting.resultsFolder` and `performance.reporting.dashboardFolder` | Configuration accessors | Present in the YAML and exposed by `PerformanceConfigurationProperties`; the current `PerformanceEngine` instead creates run output under `test-output-performance-reports`. Do not assume these two keys redirect engine output. |

Use this **placeholder-only** shape when introducing a new target configuration. Replace placeholders only in an approved, non-versioned configuration process; never copy a target hostname or credential into this guide or a committed file.

```yaml
performance:
  defaults:
    protocol: https
    host: <target-host>
    port: 443
    users: 2
    rampUpSeconds: 5
    holdSeconds: 15
    iterations: 0

  reporting:
    resultsFolder: test-output-performance-test/results
    dashboardFolder: test-output-performance-test/dashboard

  assertions:
    maxErrorPercent: 1.0
    maxAvgResponseTimeMs: 2000
    maxP95ResponseTimeMs: 3000
```

The configured profile has two modes. When `iterations > 0`, each virtual user performs that number of iterations after ramp-up. When `iterations == 0`, `holdSeconds` must be greater than zero and the group ramps to the configured users and holds for that duration. Users must be greater than zero; ramp-up, hold, and iteration values cannot be negative.[9] [10]

The engine treats a measured value **greater than** the configured maximum as a threshold breach. It checks error percentage, then average response time, then P95 response time. A positive scenario rethrows a threshold assertion after writing its artifacts; an expected-failure scenario records the result as `EXPECTED_FAIL_CONFIRMED` when a failure is observed, or `EXPECTED_FAIL_NOT_TRIGGERED` when it is not.[11] [12]

### Payload resource locations and lookup behavior

| Source type | Location and feature syntax | Resolution rules and safe use |
|---|---|---|
| Inline JSON | In the `json body` step argument | Has highest precedence. Its body preview, truncated to 120 characters, becomes payload-source detail in performance result reporting. Use only synthetic, non-sensitive values. |
| YAML | `src/test/resources/performance/payloads/yaml/performance-payloads.yml`; `using yaml key "<dot-separated-key>"` | `YamlReader` recursively merges YAML under `elements`, `queries`, `api_requests`, `config`, and `performance`. The performance payload file shares the `performance` root with performance configuration. A scalar block is passed as written; a map/list is JSON-serialized. |
| CSV | `src/test/resources/performance/payloads/csv/customers.csv`; `using csv file "<path>" row "<id>" column "<header>"` | The header names are matched case-insensitively; the first column is the row ID. The reader uses simple comma splitting, not a full CSV parser. Do not place comma-containing JSON into CSV cells; use YAML or Excel for that payload. |
| Excel | `src/test/resources/performance/payloads/excel/performance_payloads.xlsx`; `using excel file "<path>" row "<id>" column "<header>"` | Only the first sheet is read. Header matching is exact after trimming; row-ID matching is case-insensitive. The reader logs and returns `null` on lookup failure rather than throwing to the builder. |

The payload builder resolution order is **inline body, YAML key, CSV source, then Excel source**. It chooses the first configured source only when no inline body is available.[8]

The supplied CSV has the expected conceptual columns: a row ID and `request_body`.[19] The supplied Excel workbook does **not** currently present separate row-ID and payload headers in the first worksheet, so it does not match the `ExcelReader` contract for the commented Excel example. Correct its first-sheet layout before enabling an Excel-driven scenario. The intended safe layout is:

| First column (row ID) | `request_body` |
|---|---|
| `<synthetic-row-id>` | `<synthetic-json-body>` |

> **Excel safety.** `ExcelReader` returns cell text and does not implement credential redaction or a formula-sanitization policy. Do not store secrets, client data, or formulas in payload workbooks. The performance Excel report writer writes Java strings using Apache POI's `setCellValue(String)` API, but it does not independently redact report strings. Keep test names, payload-source metadata, failure messages, and any values that could appear in reports non-sensitive.[20] [25]

### GraphQL pattern

There is no GraphQL-specific sampler or Cucumber step. GraphQL is executed as an authenticated or unauthenticated **HTTP POST** whose JSON request body is supplied through the standard payload mechanisms. A structured YAML mapping is the safer, maintainable pattern because `PerformancePayloadResolver` converts maps and lists to valid JSON rather than Java map syntax.[15]

```yaml
# src/test/resources/performance/payloads/yaml/performance-payloads.yml
performance:
  payloads:
    createEntityGraphql:
      query: |
        mutation CreateEntity($input: EntityInput!) {
          createEntity(input: $input) { id }
        }
      variables:
        input:
          name: "<synthetic-value>"
```

```gherkin
@performance_testing @performance_graphql
Feature: GraphQL API performance

  Scenario: Submit a GraphQL mutation under a small authorized load
    # The alias is a label only. Do not put a real token in this feature.
    # Run only when an approved external secret-injection implementation is available.
    When we run YAML-driven POST performance test for path "<graphql-relative-path>" with name "graphql_create_entity_baseline" using yaml key "performance.payloads.createEntityGraphql"
    Then performance execution should pass
    And performance total samples should be greater than 0
```

The checked-in GraphQL feature uses the authenticated YAML-driven POST grammar and refers to a YAML key. The current performance payload registry does not define that feature's referenced key, so payload resolution will fail unless a matching approved, placeholder-only definition is added. Do not solve that mismatch by embedding a production token or target body in the feature.[5] [15] [17]

### Authentication and bearer-token aliases

`PerformanceSteps` supports an alias-storage step, an authenticated GET step, and an authenticated YAML-driven POST step. `PerformanceEngine.storeBearerToken` trims whitespace, removes an optional leading `Bearer ` scheme, and keeps the normalized token in an engine-local `ConcurrentHashMap`. During plan creation, the alias is resolved into `Authorization: Bearer <token>`. A missing alias or blank resolved value fails plan construction.[1] [10] [11]

An alias is a convenience label, not a secret-management solution. The current Gherkin interface receives the token as a literal step parameter. Consequently, the following is documentation-only syntax and must remain placeholder-only:

```gherkin
# Never replace the placeholder in a committed feature file.
When we store bearer token alias "<token-alias>" with value "<externally-injected-token-not-recorded>"
And we run authenticated GET performance test for path "<relative-path>" with name "authorized_get_baseline" using bearer token alias "<token-alias>"
```

The request builder rejects a request that configures both bearer-token and Basic authentication. If Basic authentication is used by a controlled negative test, no real credential may be placed in the feature or report.[8]

## Endpoint, request, and load rules

The plan builder composes a request as:

```text
<protocol>://<host>:<port><relative-path>
```

The route is normalized to start with `/`. Valid protocols are only `http` and `https`. The host must not contain a URL scheme, slash, query, fragment, port, or endpoint route. The path must not contain a scheme. The engine does not URL-encode supplied components.[10]

POST with a nonblank body uses the DSL `post(body, contentType)` path. For PUT and PATCH, a body is attached when present. GET and DELETE steps in `PerformanceSteps` build bodyless requests. Supported content-type mappings are `application/json`, XML variants, `text/plain`, and form URL-encoded; unrecognized or blank values default to JSON.[1] [10]

A feature-specific profile such as `using <users> users ramp <seconds> seconds hold <seconds> seconds` uses duration mode because `PerformanceSteps` explicitly sets `iterations` to zero. Use this form for a timed load window. The default profile uses the `iterations` setting from YAML.[1] [9]

## Build and exact run commands

The default Surefire configuration executes the TestNG suite in `src/test/resources/testng.xml`; that suite does not select `PerformanceTestRunner`. Do **not** use plain `mvn test` as the API-performance execution command.[7] [28]

From the repository root, compile test classes and build a test-scope classpath:

```bash
mvn clean test-compile dependency:build-classpath \
  -Dmdep.includeScope=test \
  -Dmdep.outputFile=target/test-classpath.txt
```

Run the dedicated JUnit 4 Cucumber runner with the same Java module openings configured by Surefire:

```bash
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  -cp "target/test-classes:target/classes:$(tr -d '\n' < target/test-classpath.txt)" \
  org.junit.runner.JUnitCore com.ptaf.runners.PerformanceTestRunner
```

The runner executes every scenario carrying `@performance_testing` beneath `src/test/resources/features/performance`. Run only after confirming the target authorization, safe profile, placeholder-free secret handling, and absence of sensitive data in the scenario.[3]

For an IDE run configuration, execute `com.ptaf.runners.PerformanceTestRunner` as a **JUnit 4** test. Do not use the `ui_performance` Maven profile for API performance. That profile replaces the Surefire suite with the separate browser-performance module.[7]

## Creating a new API performance feature

A new test that uses an existing HTTP verb, payload source, and assertion does not need new Java code. Create a feature in `src/test/resources/features/performance/`, tag it `@performance_testing`, select a supported Gherkin step, and use a non-sensitive request name. The runner discovers that directory recursively by Cucumber configuration.[3] [1]

1. Establish safe default endpoint components and load/threshold values in `src/test/resources/performance/config/performance-config.yml`. Keep the host and all target-specific settings out of committed documentation and reports.
2. Add a synthetic body to the YAML payload registry when a body is needed. Prefer a structured YAML GraphQL body or a JSON block scalar. Confirm that the dot-separated lookup key matches the YAML tree exactly.
3. Add the feature under `src/test/resources/features/performance/`. Tag each runnable scenario or its feature with `@performance_testing`.
4. Begin with a low-volume custom profile. Use result, artifact, sample-count, and threshold assertions to make the test outcome explicit.
5. Execute through `PerformanceTestRunner`, inspect the JTL and summaries, and archive only sanitized artifacts.

A new verb-specific step, dynamic secret retrieval, custom header step, or a changed reporting/redaction requirement is outside the current Gherkin surface and requires a reviewed framework implementation change in the performance packages. Do not work around a missing capability by placing secrets in a feature or resource file.

### Tested-style Gherkin examples

The following examples use the exact step grammar implemented by `PerformanceSteps`; every value is deliberately a placeholder or synthetic value.[1]

```gherkin
@performance_testing @performance_get
Feature: Example GET performance validation

  Scenario: Baseline GET under a duration profile
    When we run GET performance test for path "<relative-path>" with name "baseline_get" using 2 users ramp 5 seconds hold 15 seconds
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance jtl file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 1
```

```gherkin
@performance_testing @performance_yaml
Feature: Example YAML payload performance validation

  Scenario: Create a synthetic entity under load
    When we run YAML-driven POST performance test for path "<relative-post-path>" with name "create_entity_load" using yaml key "performance.payloads.createEntity"
    Then performance execution should pass
    And performance average response time should be less than 2000 ms
    And performance p95 response time should be less than 3000 ms
```

```gherkin
@performance_testing @performance_expected_failure
Feature: Example expected-failure validation

  Scenario: Confirm a controlled negative route is reported
    When we run GET performance test expecting failure for path "<controlled-negative-relative-path>" with name "expected_failure_validation"
    Then performance execution should fail
    And performance execution should be in expected failure mode
    And performance total errors should be greater than 0
    And performance error percentage should be greater than 0
```

The expected-failure steps prevent the engine from rethrowing the observed failure as a normal positive-scenario failure. They do not make an unauthorised, destructive, or sensitive target safe to test.[1] [11]

## Feature, data, locator, payload, and query locations

| Asset type | Repository location | Relationship to API performance |
|---|---|---|
| API performance features | `src/test/resources/features/performance/` | Primary Cucumber feature directory for `PerformanceTestRunner`. Runnable scenarios need `@performance_testing`. |
| Performance configuration | `src/test/resources/performance/config/performance-config.yml` | Primary default endpoint, profile, and threshold source. |
| YAML payloads | `src/test/resources/performance/payloads/yaml/performance-payloads.yml` | Performance payload registry; resolved by dot-separated key through the shared YAML reader. |
| CSV payloads | `src/test/resources/performance/payloads/csv/customers.csv` | Optional CSV body source; simple parser constraints apply. |
| Excel payloads | `src/test/resources/performance/payloads/excel/performance_payloads.xlsx` | Optional Excel body source; must obey first-sheet/header/row-ID contract. |
| Functional API request definitions | `src/test/resources/api_requests/api_requests.yml` | Used by the functional API module; not consumed by `PerformanceRequestBuilder`. |
| Functional API service configuration | `src/test/resources/config/config.yml` | Functional API service definitions; not used as performance endpoint defaults. |
| Database queries | `src/test/resources/queries/db_queries.yml` | Database module asset; not an API performance payload or assertion source. |
| UI locators | `src/test/resources/elements/` | UI module asset; not read by API performance scenarios. |

## Reports and artifacts

`PerformanceEngine` makes one timestamped run root under:

```text
test-output-performance-reports/<dd-MMM-yy_HH-mm-ss>/
```

Each scenario receives a numbered, sanitized child directory. The main engine names its artifacts as follows.[11] [26]

| Scope | Expected artifact | Purpose |
|---|---|---|
| Scenario | `results.jtl` | Raw JMeter sample result file parsed by the engine for samples, errors, min/average/P95/max response time. |
| Scenario | `dashboard/` | JMeter HTML dashboard generated by the DSL dashboard reporter. |
| Scenario | `summary.txt` | Technical performance summary. |
| Scenario | `readable-summary.txt` | Stakeholder-oriented narrative summary. |
| Run | `run-summary.txt` | Aggregate technical summary appended per scenario. |
| Run | `run-readable-summary.txt` | Aggregate readable summary appended per scenario. |
| Run | `run-index.txt` | Compact scenario index with artifact paths and status. |
| Run | `performance-run-report.xlsx` | Run-level Excel workbook. Its sheets are `Executive_Summary`, `Scenario_Summary`, `Risk_Analysis`, `Anomalies`, `Readable_Report`, `Charts`, and `Glossary`. |
| Cucumber runner | `target/performance-cucumber-report.html` | Cucumber HTML report explicitly configured by `PerformanceTestRunner`. |
| Extent adapter | `test-output/<timestamp>/SparkReport/Spark.html`, plus configured Base64 HTML, PDF, and Excel files | Combined Extent outputs configured in `extent.properties`. |
| Per-feature listener | `test-output/per-feature-reports/<feature>_<timestamp>.html`; optional PDFs per `config.yml` | Per-feature reports are enabled by the shared reporting configuration. |

**Sanitize before handling or sharing.** The technical and readable summaries include a full target URL, request/path metadata, payload-source details, and failure text. Inline bodies can become truncated payload-source details. The code normalizes report text but does not provide a general secret-redaction layer. Treat every generated artifact as potentially sensitive, restrict access, and redact or destroy it according to the approved evidence process.[8] [11] [24] [25]

## Diagnostics and troubleshooting

| Symptom | Likely code-supported cause | Check and corrective action |
|---|---|---|
| `Performance config file not found on classpath` or empty-config error | `PerformanceYamlReader` loads only `performance/config/performance-config.yml`. | Verify the exact resource path under `src/test/resources`, then run `mvn clean test-compile` so it is copied to `target/test-classes`. |
| `Performance request host must contain only the DNS host or IP address` | A complete URL, port, path, query, or fragment was placed in `host`. | Split the target into protocol, host, port, and a relative Gherkin path. Never put an unredacted private target in a committed feature or report. |
| `Performance request path must be relative` | A full URL was placed in the step path. | Use only `<relative-path>` beginning with `/`. |
| Invalid profile error | Users are not positive, a time/count is negative, or both `iterations` and `holdSeconds` are non-positive. | For a timed run, set `iterations: 0` and a positive hold; for iteration mode, set `iterations > 0`. |
| `No bearer token found for alias` | Alias was not stored in the same `PerformanceEngine` instance or is blank. | Do not inject a literal real token into the feature. Use an approved external secret mechanism before enabling the authenticated scenario. |
| `YAML payload value not found for key` | The dot-separated key is absent from the shared merged YAML map. | Match the YAML hierarchy exactly. Confirm that the payload file is under the scanned `performance` directory and that it parses. |
| CSV column/row lookup error or malformed payload | Header/row is absent or the payload contains commas that the simple reader splits. | Use the first column as the row ID; use a comma-free CSV value only, or move JSON payloads to YAML/Excel. |
| Excel scenario has no intended body | `ExcelReader` reads only sheet 0, requires a separate exact header and first-column row ID, and returns `null` after logging failures. | Correct the first-sheet schema. In particular, repair the provided workbook's combined header layout before enabling its commented example. |
| Zero samples or zeroed timing metrics | JTL is missing, empty, malformed, or lacks CSV `elapsed` and `success` fields; XML parsing recognizes `<httpSample>`/`<sample>` elements. | Inspect `results.jtl` first, then the dashboard. Treat zeroed metrics as a diagnostic condition, not evidence of a successful target run. |
| Positive scenario fails after reports are written | Any configured threshold was exceeded or execution failed. | Read `summary.txt`, `readable-summary.txt`, JTL, and the `Anomalies`/`Risk_Analysis` sheets. The engine writes reports before rethrowing a normal assertion failure. |
| Expected-failure scenario is marked not triggered | The load run completed without an observed failure. | Confirm the scenario was intentionally negative and has `performance execution should be in expected failure mode`; do not turn it into a target-disruptive test. |
| Plain `mvn test` does not run this suite | Surefire uses the configured TestNG XML rather than the dedicated JUnit Cucumber runner. | Use the exact compile/classpath/JUnitCore commands in this guide, or run `PerformanceTestRunner` as JUnit 4 in the IDE. |

## Module boundaries

The performance module is intentionally a load-generation and measurement layer. `PerformanceEngine` owns JMeter plan execution, JTL parsing, threshold checks, and its own artifacts. It does not invoke functional API request definitions or the Playwright API handler.[4] [8] [11]

The functional API module is under `com.ptaf.api` and is configured by `com.ptaf.runners.ApiTestRunner`. It uses features under the broader `src/test/resources/features` tree, `src/test/resources/api_requests/api_requests.yml`, and shared `config/config.yml` service definitions. Use that module for request/response functional assertions and API workflow verification; use the JMeter DSL module for authorized concurrency, latency, error-rate, and P95 measurement.[4] [29] [30]

The database module owns `src/test/resources/queries/db_queries.yml`; the UI and mobile modules own locator resources under `src/test/resources/elements/` and mobile locator folders. These are not read by `PerformanceRequestBuilder`. The UI performance module is independently packaged under `com.ptaf.ui_performance` and has its own Maven profile, TestNG suites, browser configuration, and report artifacts. Do not mix its browser concurrency controls with API JMeter load controls.[4] [7]

## References

[1]: ../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java "Performance Cucumber step definitions"
[2]: ../../ReadMe.md "PTAF framework overview and API automation introduction"
[3]: ../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java "Dedicated performance Cucumber runner"
[4]: ../../src/test/java/com/ptaf/runners/ApiTestRunner.java "Functional API Cucumber runner"
[5]: ../../src/test/resources/features/performance/performance.feature "Checked-in GraphQL performance feature"
[6]: ../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java "Performance engine token storage and execution"
[7]: ../../pom.xml "Maven build, JMeter DSL dependencies, Surefire configuration, and UI performance profile"
[8]: ../../src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java "Performance request construction and payload precedence"
[9]: ../../src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java "Performance load profile construction and validation"
[10]: ../../src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java "JMeter DSL HTTP plan construction and endpoint validation"
[11]: ../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java "Performance execution, JTL parsing, statuses, and run artifacts"
[12]: ../../src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java "Performance threshold assertion engine"
[13]: ../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java "Performance configuration classpath reader"
[14]: ../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java "Performance configuration properties"
[15]: ../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java "YAML, CSV, Excel, and inline payload resolver"
[16]: ../../src/main/java/com/ptaf/utils/YamlReader.java "Shared recursive YAML resource reader"
[17]: ../../src/test/resources/performance/payloads/yaml/performance-payloads.yml "Performance YAML payload registry"
[18]: ../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java "CSV payload reader"
[19]: ../../src/test/resources/performance/payloads/csv/customers.csv "Performance CSV payload resource"
[20]: ../../src/main/java/com/ptaf/utils/ExcelReader.java "Excel payload reader"
[21]: ../../src/test/resources/performance/payloads/excel/performance_payloads.xlsx "Performance Excel payload resource"
[22]: ../../src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java "Performance HTTP header manager"
[23]: ../../src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java "Performance authentication token manager"
[24]: ../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java "Performance technical and readable summary writer"
[25]: ../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java "Performance Excel report writer"
[26]: ../../src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java "Performance report path conventions"
[27]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Shared Cucumber hooks and browserless performance tags"
[28]: ../../src/test/resources/testng.xml "Default TestNG suite"
[29]: ../../src/test/resources/api_requests/api_requests.yml "Functional API request definitions"
[30]: ../../src/test/resources/config/config.yml "Shared framework and functional API configuration"
