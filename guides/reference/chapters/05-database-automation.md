# Database Automation Reference

This chapter describes the **current database-automation implementation** in FNB-ETAF. It is a JDBC-based, Cucumber-facing layer intended to validate and, where deliberately configured, change database data without requiring a Playwright browser. The implemented connection path supports **Microsoft SQL Server only**; the database module is not a general multi-database abstraction at runtime. [1][2]

## Scope, ownership, and boundaries

The database stack separates test language, assertions, orchestration, connection ownership, and JDBC execution:

```text
Gherkin feature
  -> DatabaseSteps
    -> DatabaseCommonMethods
      -> DatabaseActionImpl
        -> DatabaseHandler (connection per execution thread)
        -> DatabaseActionPerformer (PreparedStatement execution)
          -> configured SQL Server
```

`DatabaseSteps` owns Cucumber bindings and simple feature-file parameter conversion. `DatabaseCommonMethods` owns readable, test-facing validations. `DatabaseActionImpl` resolves a logical query key and coordinates a connection with a performer. `DatabaseActionPerformer` owns prepared-statement execution, statement/result-set cleanup, parameter binding, timeouts, and result mapping. `DatabaseHandler` owns creation, reuse, and closure of the thread-local JDBC connection. [3][4][5][6][1]

The following boundaries are important:

- **Do not put raw SQL in feature files or step definitions.** The implementation resolves logical keys from the merged YAML resource set. [2][8]
- **Do not close a returned JDBC `Connection` directly.** Framework teardown must call `DatabaseHandler.closeConnection()` so the per-thread reference is removed as well as closed. [1]
- The performer closes only the `PreparedStatement` and `ResultSet` it creates; it **does not commit or roll back** changes. The handler builds and closes connections but does not set transaction mode or implement rollback. Therefore, update/insert/delete effects should be treated as persistent unless the database/environment supplies different transaction behavior. [5][1]
- The layer is browserless for database scenarios; it is not a UI-to-database comparison facility by itself. UI interaction belongs to the UI layer, while this layer accepts query keys, parameters, and expected values. [9][3]

## Repository map

| Concern | Current source/resource | Responsibility |
|---|---|---|
| Connection lifecycle | [`DatabaseHandler.java`](../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java) | Builds SQL Server URLs, creates/reuses a `ThreadLocal<Connection>`, and closes/removes it. [1] |
| Database action contract | [`DatabaseAction.java`](../../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java) | Defines SELECT, update, existence, single-row, and scalar-value operations. [7] |
| Action orchestration | [`DatabaseActionImpl.java`](../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java) | Resolves YAML keys, obtains the connection, delegates execution, and applies return conventions. [2] |
| JDBC performer | [`DatabaseActionPerformer.java`](../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java) | Configures `PreparedStatement`, binds values, executes SQL, and maps rows. [5] |
| Test-facing methods | [`DatabaseCommonMethods.java`](../../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java) | Provides assertions and update-count validation for Cucumber code. [3] |
| Connectivity validation | [`DatabaseConnectionValidator.java`](../../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java) | Uses the normal handler path and validates an open connection plus JDBC metadata. [6] |
| Cucumber steps | [`DatabaseSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java) | Binds the database Gherkin vocabulary and parses comma-separated parameters. [4] |
| Database cleanup hook | [`DatabaseHooks.java`](../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java) | Closes the database connection after tagged database scenarios. [10] |
| General lifecycle hook | [`Hooks.java`](../../../src/main/java/com/ptaf/hooks/Hooks.java) | Detects database/file scenarios and skips Playwright lifecycle work. [9] |
| Database runner | [`DatabaseTestRunner.java`](../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java) | Selects DB features/tags and emits dedicated Cucumber reports. [11] |
| Connection/report configuration | [`config.yml`](../../../src/test/resources/config/config.yml) | Holds the `database` and `reporting` settings. [12] |
| Query catalog | [`db_queries.yml`](../../../src/test/resources/queries/db_queries.yml) | Holds logical query keys and SQL templates. [13] |
| Existing health check | [`database_connection_health_check.feature`](../../../src/test/resources/features/db/database_connection_health_check.feature) | The only current DB feature; validates connection availability. [14] |

## Execution flow and connection lifecycle

1. Cucumber discovers a scenario through runner configuration. The dedicated runner searches `src/test/resources/features/db`, loads `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and accepts `@db`, `@database`, or `@sql`. [11]
2. The general `Hooks.@Before` records scenario state and detects a database/file scenario. A DB tag (`@db` or `@database`) or a feature path under `features/db` causes it to mark the scenario browserless and return before Playwright browser, context, page, screenshot, or video setup. [9]
3. Constructing `DatabaseSteps` creates `DatabaseCommonMethods` and its default `DatabaseActionImpl`, but **does not open a connection**. A connection is opened only when a health check or a query/update needs one. [4][3]
4. For the health-check step, `DatabaseConnectionValidator` calls `DatabaseHandler.getConnection()`, verifies that it is open, then obtains `DatabaseMetaData`. It returns `false` on an exception; its assertion wrapper converts that into an `AssertionError` for the step. [6]
5. For query/update steps, `DatabaseActionImpl` resolves the YAML key, normalizes varargs, calls `DatabaseHandler.getConnection()`, and delegates to the performer. [2]
6. `DatabaseHandler` stores a connection in a static `ThreadLocal`. If the current thread has no connection—or its stored connection is closed—it builds a new one. Parallel execution therefore receives an isolated connection per execution thread rather than sharing one JDBC object. [1]
7. The performer creates a prepared statement, applies timeout/fetch-size values, binds each argument at its 1-based JDBC position, executes, and automatically closes the statement and result set. It leaves the connection open for the scenario. [5]
8. After a tagged DB scenario, `DatabaseHooks.@After("@db or @database or @sql")` invokes `DatabaseHandler.closeConnection()`. This helper closes an open current-thread connection, logs close failures without rethrowing, and always removes the thread-local entry. The general hook separately clears only browserless scenario state. [10][1][9]

> **Operational implication:** a connection may be shared by several DB steps in the same scenario and current execution thread, but not across parallel threads. Do not assume any automatic commit/rollback boundary at scenario end; none is implemented in the handler or performer. [1][5]

## Database configuration

Configuration is retrieved through `ConfigurationProperties`, which first looks for an optional environment-specific override at `environments.<env>.<key>` (where `env` defaults to `QA`) and then falls back to the ordinary key. The checked-in configuration currently contains database settings at the ordinary `database.*` path. [15][12]

| Key that exists | Used by | Current behavior / default in code |
|---|---|---|
| `database.db_type` | `DatabaseHandler` | Must be `sqlserver` (case-insensitive); defaults to `sqlserver` when missing. Other values cause `IllegalArgumentException`. [1] |
| `database.server_type` | Validator logging only | Exists in YAML and is included in the validator's sanitized log. It is not used to construct the URL. [12][6] |
| `database.server_name` | `DatabaseHandler` | Required; becomes the SQL Server host portion of the JDBC URL. [1] |
| `database.port` | `DatabaseHandler` | Appended to the URL; defaults to `1433`. [1] |
| `database.database_name` | `DatabaseHandler` | Required; becomes `databaseName` in the URL. [1] |
| `database.authentication` | `DatabaseHandler` | Supports `windows` (default) and `sqlserver`. Other values fail fast. [1] |
| `database.encrypt` | `DatabaseHandler` | Included in the URL; defaults to `true`. [1] |
| `database.trust_server_certificate` | `DatabaseHandler` | Included in the URL; defaults to `true`. [1] |
| `database.login_timeout_seconds` | `DatabaseHandler` | Included as `loginTimeout`; defaults to `30`. [1] |
| `database.application_name` | `DatabaseHandler` | Included as `applicationName`; defaults to `PTAF Automation Framework`. [1] |
| `database.username` | `DatabaseHandler` | Required only for `sqlserver` authentication. [1] |
| `database.password_env_variable` | `DatabaseHandler` | Required only for `sqlserver` authentication; this is the **name** of an operating-system environment variable, not the password itself. [1] |
| `database.query_timeout_seconds` | `DatabaseActionPerformer` | Applied with `setQueryTimeout`; defaults to `60` if missing, blank, or non-numeric. [5] |
| `database.fetch_size` | `DatabaseActionPerformer` | Applied with `setFetchSize`; defaults to `500` if missing, blank, or non-numeric. [5] |

For `windows` authentication, the handler adds `integratedSecurity=true` and calls `DriverManager.getConnection(url)` without credentials. The source notes that some hosts require the appropriate SQL Server integrated-security native support. For `sqlserver` authentication, it reads the username from YAML and obtains the real password with `System.getenv(password_env_variable)`; missing username, variable-name, or environment value raises an explicit configuration error. [1]

### Safe configuration practice

- Keep the real password out of `config.yml`, feature files, report text, shell history, and source control. Supply only the **environment-variable name** in `database.password_env_variable`. [1][12]
- Treat database host names, database names, and user identifiers as environment-sensitive operational data. Use sanitized placeholders in examples and support material.
- The connectivity validator deliberately logs selected non-password configuration fields and product/driver metadata. It does not retrieve or log the password, but its safe-to-log list still identifies the target server/database environment. Use access-controlled CI logs. [6]
- The performer logs SQL text at DEBUG level and logs only the **parameter count**, not parameter values. Avoid embedding sensitive literals in YAML SQL or Gherkin, because step text can be retained in reports. [5][16]

## YAML query catalog and parameterization

`YamlReader` loads and merges `.yml`/`.yaml` files from the classpath folders `elements`, `queries`, `api_requests`, `config`, and `performance`. A dotted key traverses the resulting nested map. Consequently, the query catalog's top-level sections are addressed directly: for example, `users.get_user_by_email`, not a file-name-prefixed key. Missing intermediate key segments are written to stderr with an “exact why” diagnostic, and a missing database query key later produces `IllegalArgumentException`. [8][2]

The current query catalog defines logical sections for `users`, `products`, and `stored_procedures`. Each dynamic value must be represented by a `?` placeholder and supplied in matching order. [13]

```yaml
accounts:
  find_by_reference: "SELECT status FROM approved_schema.accounts WHERE reference = ?;"
  delete_test_record: "DELETE FROM approved_schema.accounts WHERE reference = ?;"
```

The database runtime uses `PreparedStatement` and binds values with `setObject(index + 1, value)`. It therefore separates values from SQL structure and reduces SQL-injection risk for values. It does **not** make dynamically constructed table names, column names, sort directions, or raw SQL safe; those must not be assembled from feature input. [5]

### Feature-file parameter parsing

All current DB steps accept parameters as one comma-separated string. `DatabaseSteps` trims and discards empty segments, then converts each remaining segment as follows. [4]

| Feature text after trimming | Bound Java value |
|---|---|
| `null` (any case) | `null` |
| `true` / `false` (any case) | `Boolean` |
| Whole number in `int` range | `Integer` |
| Larger whole number | `Long` |
| Simple decimal such as `12.50` | `BigDecimal` |
| Anything else | `String`, with one matching outer single- or double-quote pair removed |

Use one placeholder for each `?` in the SQL, in the same order. A comma in a string parameter cannot be represented safely by this parser because it is always a delimiter. Empty fields are discarded rather than bound as empty strings. Dates/timestamps have no special parser branch, so they are passed as strings unless a step or lower layer is extended. These are source-visible limits of `parseParameters`, not generic JDBC limits. [4]

### Source-visible dialect discrepancy

There is a material repository discrepancy to resolve before relying on the starter query catalog:

- `DatabaseHandler` accepts only `sqlserver`, and the Maven build contains the Microsoft SQL Server JDBC driver. [1][17]
- The checked-in `db_queries.yml` includes PostgreSQL-style schema qualification and time expressions, while also including a stored-procedure call whose syntax may vary by database. The file itself cautions that stored-procedure syntax differs by dialect. [13]

No dialect selection or query translation exists between the YAML catalog and the performer. Have the DBA/test owner review every catalog entry against the configured SQL Server dialect and target schema; do not assume the supplied query definitions execute unchanged. [1][5][13]

## Results, assertions, and mutation behavior

### Read results

A successful SELECT is returned as `List<Map<String,Object>>`. Each row is a `LinkedHashMap`, preserving result-column order; each map key is the JDBC **column label** (so SQL aliases matter), and each value is `ResultSet.getObject(...)`. No application-level conversion is performed. [5]

| Test-facing operation | Actual current semantics |
|---|---|
| `getRecords` / `performQuery` | Returns zero-to-many row maps. `DatabaseActionImpl` returns an empty list on `SQLException`, **the same shape** as a successful query with no rows. Inspect logs to distinguish them. [3][2] |
| `verifyRecordExists` | Fails with JUnit assertion failure if the query produces no row; it is backed by the same empty-list behavior. [3] |
| `verifyRecordDoesNotExist` | Fails if at least one row is returned. [3] |
| `getSingleRecord` | Returns `null` for no row; throws `IllegalStateException` for more than one row. Design the query to be unique. [2] |
| `getSingleValue` | Calls `getSingleRecord`, then returns the first inserted map value. It returns `null` for no record/value and is still strict about multiple returned **rows**. If one row has several columns, it logs a warning and returns only the first column. [2] |
| Data-table record check | Retrieves one row and compares each requested column name with `record.containsKey(...)`; key case/alias must match the JDBC column label exactly. Actual values are compared through `String.valueOf`, with textual `null` normalized to null. [4] |

### Mutations and cleanup patterns

`performUpdate` executes INSERT, UPDATE, or DELETE and returns the affected-row count. The action layer returns `-1` after a caught `SQLException`; `verifyRowsAffected` compares the returned count exactly with the expected number. The insert and update steps hard-code an expectation of one row, while the delete and generic update steps accept the expected count from Gherkin. [2][3][4]

Use **narrow, unique test-data predicates** and explicit cleanup. A safe lifecycle is:

1. Provision a unique, non-production test identifier through the team's approved data mechanism.
2. Verify it is absent with a read query before setup.
3. Insert or change only that record with a keyed YAML update.
4. Assert the expected value/count.
5. Delete exactly the record created by the scenario, then assert it is absent.

The framework closes the JDBC connection after the scenario but does not reverse data changes. A cleanup DELETE is therefore a data-management requirement, not merely a connection-cleanup concern. [10][1][5]

> **Documentation discrepancy:** `DatabaseHooks` comments describe rollback/commit as a possible handler responsibility, but the actual `DatabaseHandler.closeConnection()` implementation only closes and removes the connection. No commit or rollback call is present in the handler or performer. Treat the implementation, not the hook comment, as the operative behavior. [10][1][5]

## Supported Gherkin vocabulary

The existing health check is deliberately small and has the feature-level `@db` tag. [14]

```gherkin
@db
Feature: Database connection health check

  Scenario: Validate the configured database connection
    Given I validate the database connection is successful
```

The following is a **safe template** using existing step phrases and existing `users` query keys. Replace angle-bracket placeholders only through approved non-production test-data handling; do not put credentials, tokens, personal data, or production identifiers in a feature file.

```gherkin
@db
Feature: Test-record lifecycle

  Scenario: Verify and remove an allocated test record
    Given I validate the database connection is successful
    Then I verify the database contains a record for query "users.get_user_by_email" with parameters "<unique-test-email>"
    When I delete 1 record(s) using query "users.delete_user_by_email" with parameters "<unique-test-email>"
    Then I verify the database does not contain a record for query "users.get_user_by_email" with parameters "<unique-test-email>"
```

Other implemented bindings are:

| Intent | Step pattern |
|---|---|
| Assert absence before setup | `Given the database does not contain a record for query {string} with parameters {string}` |
| Assert presence | `Then I verify the database contains a record for query {string} with parameters {string}` |
| Insert exactly one row | `When I insert a new record using query {string} with parameters {string}` |
| Update exactly one row | `When I update a record using query {string} with parameters {string}` |
| Delete an explicit count | `When I delete {int} record(s) using query {string} with parameters {string}` |
| Assert an arbitrary update count | `When I execute database update query {string} with parameters {string} then {int} row(s) should be affected` |
| Assert scalar output | `Then I verify single database value for query {string} with parameters {string} equals {string}` |
| Assert named columns of one record | `Then I verify database record for query {string} with parameters {string} contains:` followed by a two-column data table |

These are exact step definitions, not proposals. There is no current Gherkin binding for raw SQL, query transactions, rollback, result-set iteration, named parameters, date coercion, or automatic test-data substitution. [4]

## Runner and suite entry points

### Dedicated database runner

`DatabaseTestRunner` is a **JUnit 4 Cucumber runner**. It points to `src/test/resources/features/db`, scans database steps/hooks, selects `@db or @database or @sql`, and sets `dryRun = false` with monochrome output. Run that class as a JUnit test in an IDE when you need the dedicated database feature directory and its reports. [11]

The runner source mentions a Maven Surefire-style candidate command:

```bash
mvn -Dtest=DatabaseTestRunner test
```

However, do **not** treat that as a verified dedicated-DB route in the current checkout. The active Maven Surefire configuration forces the TestNG provider and always names `src/test/resources/testng.xml`; that suite runs `com.ptaf.runner.TestRunner`, whose Cucumber tag expression is currently `@eStore`, not the database runner or DB tags. [11][17][18][19]

The default command therefore has a different, source-visible meaning:

```bash
mvn test
```

It invokes the configured TestNG suite, not `com.ptaf.runners.DatabaseTestRunner`. Additionally, the POM sets `testFailureIgnore` to `true`, so a Maven build result alone is not sufficient evidence that a database scenario executed or passed. [17][18][19]

To make database execution part of a Maven/CI flow, the project must explicitly select a compatible DB entry point (for example, a TestNG suite/runner that selects the database tags, or an intentionally configured JUnit route). That routing configuration is outside this reference chapter and is not supplied by the current default suite. This is an execution-boundary observation, not a request to alter the sources. [17][18][19]

### Tags and browserless behavior

- The dedicated runner and `DatabaseHooks` recognize `@db`, `@database`, and `@sql`. [11][10]
- The global browserless tag set lists `@db` and `@database`, but not `@sql`; it also recognizes any feature located under `features/db`. Thus dedicated DB features remain browserless by path, but an `@sql` scenario located elsewhere would not qualify through the tag set alone. This is a source-visible tag discrepancy. [9][11][10]
- Browserless execution skips Playwright initialization and browser cleanup. Therefore database-only tests do not create the normal Playwright page/context/browser or its browser-video evidence. Cucumber and database reports still run. [9][11]

## Reports and generated artifacts

For the dedicated runner, Cucumber writes these outputs under `target/cucumber-reports/`:

| Artifact | Generated by `DatabaseTestRunner` |
|---|---|
| `database-report.html` | Built-in Cucumber HTML plugin |
| `database-report.json` | Built-in Cucumber JSON plugin |
| `database-report.xml` | Built-in Cucumber JUnit XML plugin |
| Console step output | `pretty` plugin |

The same runner also registers `PerFeatureReportListener` and `SoftAssertionReportListener`. With the present reporting configuration, `reporting.per_feature_reports_enabled` is `true`; per-feature HTML is written to `test-output/per-feature-reports`, direct per-feature PDF is disabled, and Glass-style per-feature PDF is enabled with output at `test-output/per-feature-reports-glass`. The listener creates needed directories and names reports from the Gherkin `Feature:` title plus a timestamp. Report-generation failures are logged by the listener. [11][12][16]

`extent.properties` also enables Spark, Base64 HTML, PDF, and Excel reporter outputs below a timestamped `test-output/` run directory. Those are adapter settings, but the dedicated `DatabaseTestRunner` does **not** list `ExtentCucumberAdapter` in its plugin array. Do not promise those combined adapter artifacts for a dedicated database run unless the adapter is actually registered by the chosen execution route. The per-feature listener is explicitly registered and has its own fallback reporting path. [20][11][16]

Because report events contain Gherkin step text and the per-feature listener records step text, query parameters supplied in features may appear in report artifacts. Use opaque, non-sensitive test identifiers and protect report storage appropriately. [16][4]

## Troubleshooting

| Symptom | Evidence-based checks and response |
|---|---|
| `mvn test` does not run DB scenarios | Inspect `testng.xml`, `com.ptaf.runner.TestRunner`, and the POM: the current default route is TestNG plus `@eStore`, not the JUnit database runner. Verify actual executed scenarios in the generated reports rather than relying on Maven exit status. [17][18][19] |
| A DB scenario is not selected | Confirm the feature is under `src/test/resources/features/db` for the dedicated runner and is tagged `@db`, `@database`, or `@sql`. A health-check-only tag such as `@database_health_check` is not sufficient by itself. [11][14] |
| Playwright starts unexpectedly for an SQL-tagged case | Keep DB-only features under `features/db` or use `@db`/`@database`. `@sql` is recognized by the DB runner/hook but is absent from the global non-UI tag set. [9][10][11] |
| “Unsupported database.db_type” | The current handler accepts only `sqlserver`. Check `database.db_type`; adding a JDBC dependency alone does not add runtime support. [1][17] |
| Connection health check fails | Review the sanitized validator logs for server/database/authentication/encryption values, network/VPN reachability, SQL Server permissions, and authentication mode. For SQL authentication, verify the named password environment variable exists in the executing process. [6][1] |
| Integrated authentication fails on a host | Confirm the runner's OS identity has database permission and that required SQL Server integrated-security native support is available. The handler appends `integratedSecurity=true` only for `windows` mode. [1] |
| Query key cannot be found | Confirm the dotted logical key and YAML structure. `YamlReader` scans the `queries` resource folder and emits exact segment diagnostics; `DatabaseActionImpl` then throws if the key is absent or its value is blank. [8][2] |
| Query returns no records but logs show SQL failure | `performQuery` converts caught `SQLException` into an empty list. Check the logged exception and SQL/parameter count; an empty result alone is ambiguous. [2][5] |
| Update assertion reports `-1` rows | `performUpdate` returns `-1` after a caught `SQLException`. Find the preceding performer/action error; do not interpret `-1` as a legitimate affected-row count. [2][3] |
| “Expected a single database record” error | The query returned more than one row. Tighten the YAML predicate or use the multi-row path deliberately. `getSingleValue` is also strict because it first calls `getSingleRecord`. [2] |
| Data-table assertion says a column is missing | Use the JDBC column **label** exactly, including any alias/case emitted by the driver. The step checks map keys directly. [5][4] |
| Timeout or excessive retrieval | Review the SQL first, then `database.query_timeout_seconds` and `database.fetch_size`. Invalid/missing numeric settings fall back to 60 seconds and 500. [5] |
| Test records remain after a run | Connection closure is not data rollback. Add a narrowly targeted explicit cleanup query/step and confirm the affected count/absence. [1][5][10] |
| Expected browser screenshot/video is absent | Database scenarios are deliberately browserless; the global hook does not initialize a Playwright stack for them. Use Cucumber/DB reports and logs for evidence. [9][11] |

## Related chapters

The following planned chapters should cover adjacent concerns without duplicating this database contract:

- `01-framework-architecture.md` — framework modules, lifecycle conventions, and shared utilities.
- `02-cucumber-and-test-execution.md` — Cucumber discovery, tag strategy, runners, and suite design.
- `03-configuration-and-test-data.md` — configuration resolution, environment overrides, and safe test-data governance.
- `04-reporting-and-artifacts.md` — report adapters, report retention, screenshots, video, and CI evidence.
- `06-ui-automation.md` — Playwright/browser lifecycle and UI assertions.

## Source references

- [1] [`DatabaseHandler.java`](../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java)
- [2] [`DatabaseActionImpl.java`](../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java)
- [3] [`DatabaseCommonMethods.java`](../../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java)
- [4] [`DatabaseSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java)
- [5] [`DatabaseActionPerformer.java`](../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java)
- [6] [`DatabaseConnectionValidator.java`](../../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java)
- [7] [`DatabaseAction.java`](../../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java)
- [8] [`YamlReader.java`](../../../src/main/java/com/ptaf/utils/YamlReader.java)
- [9] [`Hooks.java`](../../../src/main/java/com/ptaf/hooks/Hooks.java)
- [10] [`DatabaseHooks.java`](../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java)
- [11] [`DatabaseTestRunner.java`](../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java)
- [12] [`config.yml`](../../../src/test/resources/config/config.yml)
- [13] [`db_queries.yml`](../../../src/test/resources/queries/db_queries.yml)
- [14] [`database_connection_health_check.feature`](../../../src/test/resources/features/db/database_connection_health_check.feature)
- [15] [`ConfigurationProperties.java`](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java)
- [16] [`PerFeatureReportListener.java`](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java)
- [17] [`pom.xml`](../../../pom.xml)
- [18] [`testng.xml`](../../../src/test/resources/testng.xml)
- [19] [`com.ptaf.runner.TestRunner`](../../../src/test/java/com/ptaf/runner/TestRunner.java)
- [20] [`extent.properties`](../../../src/test/resources/extent.properties)

## References

[1]: ../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java "DatabaseHandler — SQL Server URL construction and thread-local connection lifecycle"
[2]: ../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java "DatabaseActionImpl — query-key orchestration and return conventions"
[3]: ../../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java "DatabaseCommonMethods — test-facing database assertions"
[4]: ../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java "DatabaseSteps — Cucumber database step definitions and parameter parsing"
[5]: ../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java "DatabaseActionPerformer — PreparedStatement execution and result mapping"
[6]: ../../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java "DatabaseConnectionValidator — connectivity health checks"
[7]: ../../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java "DatabaseAction — database operation contract"
[8]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "YamlReader — merged YAML resource loading and dotted-key lookup"
[9]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Hooks — browserless scenario detection and lifecycle"
[10]: ../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java "DatabaseHooks — tagged database teardown"
[11]: ../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java "DatabaseTestRunner — dedicated Cucumber database runner"
[12]: ../../../src/test/resources/config/config.yml "config.yml — database and reporting configuration"
[13]: ../../../src/test/resources/queries/db_queries.yml "db_queries.yml — current externalized SQL catalog"
[14]: ../../../src/test/resources/features/db/database_connection_health_check.feature "Database connection health-check feature"
[15]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "ConfigurationProperties — environment-aware configuration lookup"
[16]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "PerFeatureReportListener — per-feature HTML and PDF reporting"
[17]: ../../../pom.xml "Maven dependencies and Surefire provider/suite configuration"
[18]: ../../../src/test/resources/testng.xml "Default TestNG suite"
[19]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[20]: ../../../src/test/resources/extent.properties "Extent adapter reporter settings"
