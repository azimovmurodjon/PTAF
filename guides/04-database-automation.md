# FNB-ETAF Database Automation Guide

## Purpose and scope

This guide explains how the FNB-ETAF database module executes direct Microsoft SQL Server validation and test-data operations through Cucumber. It covers the database runner, Gherkin steps, YAML query resources, SQL Server configuration, parameter binding, lifecycle handling, reporting, and creation of **browserless** database scenarios. It is intentionally limited to the current implementation in this repository. It does not describe UI locators, API request construction, performance workloads, or native/mobile execution.

The database path is designed to keep SQL and connection details outside feature files. A database feature calls a logical query key, such as `orders.select_by_reference`; the framework resolves that key from YAML, binds supplied values into JDBC `?` placeholders, and makes assertions against the returned data or affected-row count. The implemented connection handler accepts only `database.db_type: sqlserver` and supports Windows Integrated Authentication or SQL Server username/password authentication. [1] [2] [3]

> **Direct DB execution is browserless.** Database scenarios under `src/test/resources/features/db/`, or scenarios tagged `@db` or `@database`, are recognized by the shared hook as non-UI work. It returns before Playwright browser initialization. The dedicated database runner also limits discovery to the DB feature directory and selects `@db`, `@database`, or `@sql` scenarios. [4] [5]

## Architecture and source locations

The execution flow is:

```text
DatabaseTestRunner
  → Cucumber feature and DatabaseSteps
  → DatabaseCommonMethods
  → DatabaseAction (DatabaseActionImpl)
  → DatabaseHandler + DatabaseActionPerformer
  → JDBC PreparedStatement → SQL Server
```

`DatabaseHandler` owns a `ThreadLocal<Connection>`, so each execution thread receives a separate JDBC connection. `DatabaseHooks` closes and removes the current thread's connection after each DB-tagged scenario. Statements and result sets are closed by the performer; the connection itself remains open for the scenario until the DB teardown hook runs. [1] [3] [6]

| Responsibility | Package, class, or resource | Current behavior |
|---|---|---|
| DB-only Cucumber entry point | [`com.ptaf.runners.DatabaseTestRunner`][4] | Searches `src/test/resources/features/db`, uses DB steps and hooks, and selects `@db or @database or @sql`. |
| Gherkin bindings | [`com.ptaf.stepdefinitions.DatabaseSteps`][5] | Provides connection, existence, row-count, scalar-value, and column-value steps. |
| Test-facing DB helper | [`com.ptaf.db.pages.DatabaseCommonMethods`][7] | Applies JUnit assertions around records, scalar values, and affected-row counts. |
| DB abstraction | [`com.ptaf.db.interfaces.DatabaseAction`][8] | Defines query, update, existence, single-record, and single-value operations. |
| Query-key orchestration | [`com.ptaf.db.implementation.DatabaseActionImpl`][2] | Resolves SQL by logical YAML key, obtains a connection, and delegates statement execution. |
| Connection lifecycle | [`com.ptaf.db.handlers.DatabaseHandler`][1] | Builds SQL Server JDBC URLs, reads credentials where applicable, and owns thread-local connections. |
| JDBC execution | [`com.ptaf.db.performer.DatabaseActionPerformer`][3] | Creates `PreparedStatement` instances, sets timeout/fetch size, binds values with `setObject`, and maps rows by column label. |
| Connectivity smoke test | [`com.ptaf.db.validators.DatabaseConnectionValidator`][9] | Opens the normal framework connection, checks it is open, and reads JDBC metadata. |
| DB teardown | [`com.ptaf.hooks.DatabaseHooks`][6] | Runs after `@db`, `@database`, or `@sql` scenarios and calls `DatabaseHandler.closeConnection()`. |
| Shared browser lifecycle | [`com.ptaf.hooks.Hooks`][10] | Recognizes DB work as non-UI and skips Playwright setup and browser cleanup. |
| Configuration lookup | [`com.ptaf.utils.ConfigurationProperties`][11] and [`com.ptaf.utils.YamlReader`][12] | Reads `config.yml` and merged classpath YAML values using dot-separated keys. |

## Prerequisites

Use a **JDK 21** runtime because the Maven compiler configuration sets both source and target to 21. Maven is required to resolve the project dependencies and run the selected test class. The POM includes the Microsoft SQL Server JDBC driver and the PostgreSQL driver, but the implemented `DatabaseHandler` rejects any `database.db_type` other than `sqlserver`; do not infer PostgreSQL runtime support from the presence of that dependency or the example SQL resource. [13] [1]

The execution host needs network access to the approved SQL Server and an identity that has only the permissions required by the scenario. For Windows authentication, the JDBC URL includes `integratedSecurity=true`; the handler notes that some hosts also need the appropriate SQL Server JDBC native library available. For SQL Server authentication, configure a username and the **name** of an environment variable that contains the password. The password itself is obtained with `System.getenv` and must not be placed in YAML, features, reports, or shell history. [1]

Before writing mutations, confirm that the test database and schema are approved for automation. The implementation does not issue transaction commits or rollbacks around updates. Cleanup SQL must therefore be explicit when the test creates data. [3]

## Configuration and secure connection practice

### Resource loading and environment selection

Database connection settings live in [`src/test/resources/config/config.yml`][14]. Query definitions live in [`src/test/resources/queries/db_queries.yml`][15]. `YamlReader` scans the `elements`, `queries`, `api_requests`, `config`, and `performance` classpath folders, merges their YAML maps, and resolves nested keys through dot notation. `ConfigurationProperties.getValue` first attempts `environments.<env>.<key>`, where `<env>` is the `env` system property and defaults to `QA`; it then falls back to the unscoped key. [12] [11]

A query key must therefore be globally unambiguous across loaded YAML resources. If more than one loaded YAML file supplies the same non-map key, a later load can overwrite an earlier value. Keep DB keys grouped under a distinctive top-level mapping in the query resource.

### Database settings

The following settings are read by the implementation. The example is deliberately placeholder-only and does not represent a real server, database, account, or policy decision.

```yaml
# src/test/resources/config/config.yml
database:
  db_type: "sqlserver"
  server_type: "Database Engine"
  server_name: "<DB_HOST_OR_INSTANCE>"
  port: "<DB_PORT>"
  database_name: "<TEST_DATABASE>"
  authentication: "sqlserver" # or: windows
  encrypt: "<TRUE_OR_FALSE_PER_APPROVED_POLICY>"
  trust_server_certificate: "<TRUE_OR_FALSE_PER_APPROVED_POLICY>"
  login_timeout_seconds: "<LOGIN_TIMEOUT_SECONDS>"
  query_timeout_seconds: "<QUERY_TIMEOUT_SECONDS>"
  fetch_size: "<FETCH_SIZE>"
  application_name: "<AUTOMATION_APPLICATION_NAME>"
  username: "<DB_USERNAME>"
  password_env_variable: "PTAF_DB_PASSWORD"
```

| Setting | Consumption and validation |
|---|---|
| `database.db_type` | Defaults to `sqlserver`; any other value causes an `IllegalArgumentException`. |
| `database.server_name`, `database.database_name` | Required by `DatabaseHandler` before it creates the JDBC URL. |
| `database.port` | Defaults to `1433` when missing or blank. |
| `database.authentication` | Defaults to `windows`. Supported values are `windows` and `sqlserver`. Windows mode appends `integratedSecurity=true`; SQL Server mode passes the configured username and environment-supplied password to `DriverManager`. |
| `database.encrypt`, `database.trust_server_certificate`, `database.login_timeout_seconds`, `database.application_name` | Passed into the generated SQL Server JDBC URL. The handler defaults them to `true`, `true`, `30`, and `PTAF Automation Framework`, respectively, when blank. |
| `database.query_timeout_seconds`, `database.fetch_size` | Applied to every prepared statement. Missing, blank, or non-integer values fall back to 60 seconds and 500. |
| `database.username`, `database.password_env_variable` | Required only in `sqlserver` authentication mode. The latter contains the environment-variable **name**, not a password. |
| `database.server_type` | Read and logged by the connection validator's sanitized configuration summary; it does not participate in JDBC URL construction. |

For Windows Integrated Authentication, use `authentication: "windows"` and leave `username` and `password_env_variable` empty. For SQL Server authentication, supply the secret only in the host environment. A placeholder-only launch pattern is:

```bash
export PTAF_DB_PASSWORD='<PASSWORD_PROVIDED_BY_APPROVED_SECRET_STORE>'
mvn clean test -Dtest=DatabaseTestRunner
```

The handler avoids logging the completed JDBC URL and never logs the password. The connection validator does log non-password configuration fields, including server and database names, while it validates connectivity. Treat execution logs as sensitive operational artifacts and do not publish them outside the approved environment. [1] [9]

## Build and run

Run commands below assume the repository root is the current directory.

```bash
cd /home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23

# Compile main and test code without executing tests.
mvn -DskipTests test

# Execute only the dedicated database Cucumber runner.
mvn clean test -Dtest=DatabaseTestRunner
```

The second command is the runner's documented Maven Surefire selection pattern. The equivalent IDE action is to run `com.ptaf.runners.DatabaseTestRunner` as a JUnit test. [4]

Do not use the generic default Maven suite as the DB execution entry point. The Surefire configuration normally points at `src/test/resources/testng.xml`, while the database runner is a JUnit/Cucumber class and isolates DB features itself. The POM also sets `testFailureIgnore` to `true`; therefore, inspect the generated Cucumber/Extent outputs and Surefire results for the actual scenario status rather than relying only on a Maven process exit code. [13] [16]

No Playwright browser installation is required for a direct DB run. Keep the feature under `features/db` and use `@db` (recommended) or `@database`/`@sql`; the shared hook will bypass browser startup. [4] [10]

## Create a database scenario

### 1. Add a parameterized query resource

Add a logical key beneath a suitable top-level mapping in [`src/test/resources/queries/db_queries.yml`][15]. SQL must use JDBC `?` placeholders in the same positional order as the feature parameters. `DatabaseActionPerformer` always uses `PreparedStatement` and calls `setObject` for each parameter. [3]

```yaml
# Placeholder-only example: src/test/resources/queries/db_queries.yml
orders:
  select_by_reference: "SELECT order_reference, status FROM dbo.orders WHERE order_reference = ?"
  insert_test_order: "INSERT INTO dbo.orders (order_reference, status) VALUES (?, ?)"
  update_status_by_reference: "UPDATE dbo.orders SET status = ? WHERE order_reference = ?"
  delete_by_reference: "DELETE FROM dbo.orders WHERE order_reference = ?"
  count_by_reference: "SELECT COUNT(*) AS matching_records FROM dbo.orders WHERE order_reference = ?"
```

The checked-in query file contains sample SQL using PostgreSQL-oriented `public` schema and `NOW()` syntax, while the current connection implementation supports SQL Server only. Treat that resource as a key/placeholder example, not as a portability guarantee; write SQL that is valid for the configured SQL Server environment. [15] [1]

### 2. Add a browserless DB feature

Create the feature under `src/test/resources/features/db/`. Use a DB tag at feature or scenario level. The dedicated runner discovers only this directory, and its tag expression accepts `@db`, `@database`, and `@sql`. Do not add UI steps, UI locators, browser tags, or UI-only test setup to a direct DB scenario. [4]

```gherkin
@db
Feature: Order database validation

  Scenario: Create, verify, update, and remove an approved test order
    Given the database does not contain a record for query "orders.select_by_reference" with parameters "<TEST_ORDER_REFERENCE>"
    When I insert a new record using query "orders.insert_test_order" with parameters "<TEST_ORDER_REFERENCE>, <INITIAL_STATUS>"
    Then I verify database record for query "orders.select_by_reference" with parameters "<TEST_ORDER_REFERENCE>" contains:
      | order_reference | <TEST_ORDER_REFERENCE> |
      | status          | <INITIAL_STATUS>       |
    When I update a record using query "orders.update_status_by_reference" with parameters "<UPDATED_STATUS>, <TEST_ORDER_REFERENCE>"
    Then I verify single database value for query "orders.count_by_reference" with parameters "<TEST_ORDER_REFERENCE>" equals "1"
    When I delete 1 record(s) using query "orders.delete_by_reference" with parameters "<TEST_ORDER_REFERENCE>"
    Then I verify the database does not contain a record for query "orders.select_by_reference" with parameters "<TEST_ORDER_REFERENCE>"
```

The repository's smallest existing DB feature is the connection health check at [`src/test/resources/features/db/database_connection_health_check.feature`][17]. It is useful as a first scenario after configuration because it uses the same `DatabaseHandler` path as query execution. [5] [9]

### 3. Use the existing validation vocabulary

The following bindings are implemented by `com.ptaf.stepdefinitions.DatabaseSteps`; use them exactly as shown. [5]

| Intent | Supported Gherkin form | Assertion behavior |
|---|---|---|
| Connection smoke check | `Given I validate the database connection is successful` | Fails with `AssertionError` when connection creation, open-state verification, or metadata access fails. |
| Negative precondition or final check | `Given the database does not contain a record for query "<KEY>" with parameters "<PARAMETERS>"` | Requires zero returned rows. |
| Positive existence check | `Then I verify the database contains a record for query "<KEY>" with parameters "<PARAMETERS>"` | Requires at least one returned row. |
| Negative final check | `Then I verify the database does not contain a record for query "<KEY>" with parameters "<PARAMETERS>"` | Requires zero returned rows. |
| Insert | `When I insert a new record using query "<KEY>" with parameters "<PARAMETERS>"` | Requires exactly one affected row. |
| Update | `When I update a record using query "<KEY>" with parameters "<PARAMETERS>"` | Requires exactly one affected row. |
| Delete | `When I delete <COUNT> record(s) using query "<KEY>" with parameters "<PARAMETERS>"` | Requires the stated affected-row count. |
| Flexible mutation count | `When I execute database update query "<KEY>" with parameters "<PARAMETERS>" then <COUNT> row(s) should be affected` | Requires the stated affected-row count. |
| Scalar result | `Then I verify single database value for query "<KEY>" with parameters "<PARAMETERS>" equals "<EXPECTED>"` | Reads the first column of a single-row result and compares its string value. |
| Column-level record check | `Then I verify database record for query "<KEY>" with parameters "<PARAMETERS>" contains:` followed by two-column rows | Requires one result row, the specified column labels, and equal string values. |

A record assertion that expects one row is intentionally strict. `getSingleRecord` returns `null` for no rows and throws if a query returns more than one row. Design single-record queries with unique predicates. A scalar assertion uses the first column of that record; if the query returns several columns, the implementation logs a warning and uses the first one. [2] [7]

## Parameterization and query safety

Parameters in the built-in Gherkin DB steps are one comma-separated string. The parser trims items and converts them in this order: the literal `null` becomes SQL `NULL`; `true`/`false` become booleans; whole numbers become `Integer` or `Long`; decimal numbers become `BigDecimal`; every other value is a string after optional matching single or double outer quotes are removed. An empty parameter string, `""`, produces an empty parameter list. [5]

This parser has no escaping or quoting rule for embedded commas. A value containing a comma will be split into multiple parameters even if visually quoted. Do not use a comma-containing value with these built-in string-parameter steps; either choose a comma-free test value or implement an approved step extension before testing that data shape.

Always use `?` placeholders rather than interpolating feature values into SQL. The performer binds values positionally with `PreparedStatement.setObject`, which avoids direct string concatenation by the framework. Placeholder count and order remain the test author's responsibility. A missing query key or blank SQL value fails fast with `IllegalArgumentException`; a JDBC SELECT failure is returned upstream as an empty list, and a JDBC update failure as `-1`, so the subsequent assertion normally records the scenario failure. [2] [3]

For column-record checks, use the JDBC **column label**—including any SQL alias—exactly as it is returned by the driver. Rows are stored in insertion-ordered maps keyed by that label. Expected values are normalized only by trimming, treating `null` as null, and stripping matching outer quotes; comparison is otherwise a string equality check. [3] [5]

## Feature, data, locator, payload, and query locations

| Asset type | Repository location | Database-module use |
|---|---|---|
| Database features | [`src/test/resources/features/db/`][17] | Required location for features discovered by `DatabaseTestRunner`. |
| Database Gherkin bindings | [`src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`][5] | Reuse its existing DB step vocabulary; place new bindings here only when the existing vocabulary cannot express the requirement. |
| Database query YAML | [`src/test/resources/queries/db_queries.yml`][15] | Source of logical query keys and parameterized SQL. |
| Shared configuration YAML | [`src/test/resources/config/config.yml`][14] | Connection, statement-execution, and reporting switches. |
| General test data | [`src/test/resources/data/`][18] and `src/test/resources/testdata.xlsx` | Present for file/data-driven modules; the current DB steps do not read these resources. |
| UI locators | [`src/test/resources/elements/`][19] | Not used by direct DB scenarios. |
| API request payload definitions | [`src/test/resources/api_requests/`][20] | Not used by direct DB scenarios. |
| Performance payloads | [`src/test/resources/performance/payloads/`][21] | Not used by direct DB scenarios. |

## Reports and execution artifacts

`DatabaseTestRunner` configures readable console output plus three Cucumber reports for every DB run: `target/cucumber-reports/database-report.html`, `target/cucumber-reports/database-report.json`, and `target/cucumber-reports/database-report.xml`. When Maven runs the suite, its Surefire system property additionally requests `target/cucumber-reports/cucumber.json` and `target/cucumber-reports/cucumber.xml`. Surefire's own files are written under `target/surefire-reports`. [4] [13]

The DB runner also registers `PerFeatureReportListener`. With the current reporting settings, it generates one per-feature HTML report in `test-output/per-feature-reports` and one Glass-style per-feature PDF in `test-output/per-feature-reports-glass`; names derive from the Gherkin `Feature:` title and include a timestamp. The direct per-feature PDF switch is currently disabled. The combined Extent adapter is **not** registered in `DatabaseTestRunner`, so the combined Spark/Base64/PDF/Excel outputs configured in `extent.properties` are not a direct DB-runner output contract. [4] [14] [22] [23]

Direct DB scenarios do not initialize Playwright, and no DB step emits a browser screenshot or video. The per-feature listener can attach image events if Cucumber provides them, but direct DB scenarios should be expected to produce textual status, errors, timings, JSON/XML/HTML reports, and configured PDF artifacts rather than browser evidence. [10] [22]

## Troubleshooting

| Symptom | Source-backed checks and resolution |
|---|---|
| Connection health check fails | Confirm `database.server_name` and `database.database_name` are supplied, the selected authentication mode is `windows` or `sqlserver`, and the execution host has permitted network access. For SQL Server authentication, confirm the named environment variable is present and non-blank. Review the sanitized validation logs, not the password. [1] [9] |
| `Unsupported database.db_type` | Set the configured DB type to `sqlserver`. The current connection handler has no runtime branch for another engine. [1] |
| Windows authentication cannot connect | Confirm the process identity has database access. The URL uses `integratedSecurity=true`; where the JDBC driver requires it, make the matching native support available on the execution host. [1] |
| Query key is not found or YAML diagnostics appear | Confirm the dotted query key matches the YAML hierarchy and the file is under a classpath folder scanned by `YamlReader`, such as `queries`. The reader prints the failed key segment or YAML load failure to standard error. [12] [2] |
| Parameter binding or SQL error | Count SQL `?` placeholders and comma-delimited feature parameters in the same order. Verify type conversion rules and avoid comma-containing values with the built-in step parser. [5] [3] |
| DB scenario opens a browser | Put the feature under `src/test/resources/features/db/` and tag it `@db` or `@database`. Run `DatabaseTestRunner`, not the UI runner. The shared hook recognizes the DB path and DB tags as browserless. [4] [10] |
| Record check fails with no result or too many results | Use existence checks for zero-to-many results. For `I verify database record ... contains:`, make the query return exactly one row and use actual result column labels or aliases in the table. [2] [5] |
| Mutation has an unexpected row count | Verify the predicate identifies the intended test record and use the flexible update/delete binding when the approved expected count is not one. Add explicit cleanup because the performer does not commit or roll back transactions. [3] [5] |
| Maven looks successful despite a failed scenario | The POM sets Surefire `testFailureIgnore` to `true`. Inspect `database-report.html`, JSON/XML, per-feature artifacts, and `target/surefire-reports` for the test outcome. [13] [4] |
| Expected Extent combined report is absent | The DB runner does not register `ExtentCucumberAdapter`. Use its Cucumber outputs and configured per-feature reports, or change runner configuration only through an approved framework change. [4] [23] |

## Module boundaries

The database module owns **direct JDBC database verification and setup**. Feature files own scenario intent and call query keys; query YAML owns SQL; `DatabaseSteps` owns Gherkin translation; the DB packages own connection, execution, and result mapping. Feature files must not construct JDBC URLs, embed credentials, or hardcode SQL text.

The UI module owns Playwright pages, locators, screenshots, and video. It is intentionally bypassed for DB runs by `Hooks`. The API module owns HTTP request definitions under `api_requests` and is selected by `ApiTestRunner`; its resources are not a DB query source. Performance and mobile modules have their own config, payload, runner, and lifecycle paths. Shared configuration and the reporting listener are cross-cutting modules, but their presence does not make a DB scenario a UI test. [10] [24] [25]

When an end-to-end test needs both a UI action and a database assertion, treat it as a deliberate cross-module design decision. This guide covers only a browserless DB feature. A direct DB scenario should remain in the DB feature directory and execute through `DatabaseTestRunner`; UI assertions and locators belong to the UI runner and UI module.

## References

<!-- Visible source-reference list -->
The sources below are visible and clickable in Markdown preview. Citation labels used in this guide point to the same source files.

- **[1]** [DatabaseHandler](../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java) — `../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java`
- **[2]** [DatabaseActionImpl](../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java) — `../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java`
- **[3]** [DatabaseActionPerformer](../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java) — `../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java`
- **[4]** [DatabaseTestRunner](../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java) — `../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java`
- **[5]** [DatabaseSteps](../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`
- **[6]** [DatabaseHooks](../../src/main/java/com/ptaf/hooks/DatabaseHooks.java) — `../../src/main/java/com/ptaf/hooks/DatabaseHooks.java`
- **[7]** [DatabaseCommonMethods](../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java) — `../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java`
- **[8]** [DatabaseAction interface](../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java) — `../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java`
- **[9]** [DatabaseConnectionValidator](../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java) — `../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java`
- **[10]** [Shared Cucumber Hooks](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`
- **[11]** [ConfigurationProperties](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — `../../src/main/java/com/ptaf/utils/ConfigurationProperties.java`
- **[12]** [YamlReader](../../src/main/java/com/ptaf/utils/YamlReader.java) — `../../src/main/java/com/ptaf/utils/YamlReader.java`
- **[13]** [Maven build, JDBC dependencies, and Surefire configuration](../../pom.xml) — `../../pom.xml`
- **[14]** [Framework configuration](../../src/test/resources/config/config.yml) — `../../src/test/resources/config/config.yml`
- **[15]** [Reusable database query resource](../../src/test/resources/queries/db_queries.yml) — `../../src/test/resources/queries/db_queries.yml`
- **[16]** [Default TestNG suite](../../src/test/resources/testng.xml) — `../../src/test/resources/testng.xml`
- **[17]** [Database connection health-check feature](../../src/test/resources/features/db/database_connection_health_check.feature) — `../../src/test/resources/features/db/database_connection_health_check.feature`
- **[18]** [General data resources](../../src/test/resources/data/) — `../../src/test/resources/data/`
- **[19]** [UI locator resources](../../src/test/resources/elements/) — `../../src/test/resources/elements/`
- **[20]** [API request resource](../../src/test/resources/api_requests/api_requests.yml) — `../../src/test/resources/api_requests/api_requests.yml`
- **[21]** [Performance payload resources](../../src/test/resources/performance/payloads/) — `../../src/test/resources/performance/payloads/`
- **[22]** [PerFeatureReportListener](../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) — `../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`
- **[23]** [Extent adapter output configuration](../../src/test/resources/extent.properties) — `../../src/test/resources/extent.properties`
- **[24]** [UI Cucumber runner](../../src/test/java/com/ptaf/runners/TestRunner.java) — `../../src/test/java/com/ptaf/runners/TestRunner.java`
- **[25]** [API Cucumber runner](../../src/test/java/com/ptaf/runners/ApiTestRunner.java) — `../../src/test/java/com/ptaf/runners/ApiTestRunner.java`

<!-- Internal citation definitions used by the in-text [n] links. Keep these definitions so citations remain clickable. -->
[1]: ../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java "DatabaseHandler"
[2]: ../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java "DatabaseActionImpl"
[3]: ../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java "DatabaseActionPerformer"
[4]: ../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java "DatabaseTestRunner"
[5]: ../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java "DatabaseSteps"
[6]: ../../src/main/java/com/ptaf/hooks/DatabaseHooks.java "DatabaseHooks"
[7]: ../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java "DatabaseCommonMethods"
[8]: ../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java "DatabaseAction interface"
[9]: ../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java "DatabaseConnectionValidator"
[10]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Shared Cucumber Hooks"
[11]: ../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "ConfigurationProperties"
[12]: ../../src/main/java/com/ptaf/utils/YamlReader.java "YamlReader"
[13]: ../../pom.xml "Maven build, JDBC dependencies, and Surefire configuration"
[14]: ../../src/test/resources/config/config.yml "Framework configuration"
[15]: ../../src/test/resources/queries/db_queries.yml "Reusable database query resource"
[16]: ../../src/test/resources/testng.xml "Default TestNG suite"
[17]: ../../src/test/resources/features/db/database_connection_health_check.feature "Database connection health-check feature"
[18]: ../../src/test/resources/data/ "General data resources"
[19]: ../../src/test/resources/elements/ "UI locator resources"
[20]: ../../src/test/resources/api_requests/api_requests.yml "API request resource"
[21]: ../../src/test/resources/performance/payloads/ "Performance payload resources"
[22]: ../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "PerFeatureReportListener"
[23]: ../../src/test/resources/extent.properties "Extent adapter output configuration"
[24]: ../../src/test/java/com/ptaf/runners/TestRunner.java "UI Cucumber runner"
[25]: ../../src/test/java/com/ptaf/runners/ApiTestRunner.java "API Cucumber runner"
