# FNB-ETAF Technical Usage Guide: CSV, XML, TXT Conversion, and ZIP Extraction

## Purpose and scope

This guide documents the repository’s **file-automation modules** for validating CSV and XML content, extracting ZIP archives, and converting delimited TXT files found in a ZIP into CSV. It is written for contributors creating or maintaining Cucumber scenarios in FNB-ETAF.

The implementation supports two ways to load CSV/XML: a filesystem path or visible text obtained through the UI module. ZIP handling is scenario-scoped: an archive is extracted, discovered files are held in the ZIP context, and the extracted CSV/XML can then be loaded into the existing CSV/XML contexts. TXT-to-CSV conversion is available only after extraction. The guide covers the behavior present in the current codebase; it does not introduce a new download-to-ZIP handoff, test fixture, parser, or Gherkin syntax.

> **Scope boundary:** The modules validate and transform files available to the test process. They do not fetch remote files, construct API payloads, execute SQL, or upload data. UI downloads and UI text extraction are integrations supplied by the Playwright/UI layers, not capabilities of the CSV, XML, or ZIP packages themselves.

## Architecture and source locations

| Layer | Package, class, or resource | Responsibility and verified behavior |
|---|---|---|
| CSV parser | [`com.ptaf.csv.CsvFileHandler`][csv-handler] | Loads UTF-8 filesystem content or a string; parses comma-delimited data by default, supports a one-character delimiter, exposes rows/header queries, and uses 1-based data-row and column indexes. |
| CSV façade and state | [`com.ptaf.csv.CsvCommonMethods`][csv-common], [`com.ptaf.csv.CsvContext`][csv-context] | Provides assertions/extraction and keeps the active handler in a `ThreadLocal`. The Cucumber `@After` hook clears both state and the scenario variable store. |
| CSV bindings | [`com.ptaf.stepdefinitions.CsvSteps`][csv-steps] | Binds file loading, custom-delimiter loading, UI-text loading, assertions, and extraction to Gherkin. |
| XML parser | [`com.ptaf.xml.XmlFileHandler`][xml-handler] | Parses a filesystem file or string into a DOM and evaluates simple-node or XPath queries. Its `DocumentBuilderFactory` disables DOCTYPE declarations and external entities. |
| XML façade and state | [`com.ptaf.xml.XmlCommonMethods`][xml-common], [`com.ptaf.xml.XmlContext`][xml-context] | Provides assertions/extraction and stores the active document in a `ThreadLocal`, cleared by the XML Cucumber `@After` hook. |
| XML bindings | [`com.ptaf.stepdefinitions.XmlSteps`][xml-steps] | Binds file/UI loading, value/existence/count/attribute assertions, and extraction to Gherkin. |
| ZIP/TXT engine | [`com.ptaf.zip.ZipFileHandler`][zip-handler] | Extracts a ZIP, recursively discovers files by extension, optionally recurses into nested ZIPs, converts a delimited TXT file to comma-separated CSV, and removes an extraction directory. |
| ZIP scenario state and bindings | [`com.ptaf.zip.ZipContext`][zip-context], [`com.ptaf.stepdefinitions.ZipSteps`][zip-steps] | Holds the extraction result in a `ThreadLocal`; exposes unzip, discovery, TXT conversion, CSV/XML loading from the archive, and cleanup steps. |
| Configuration | [`com.ptaf.utils.ConfigurationProperties`][configuration-properties], [`config/config.yml`][config-yml] | Reads `zip.*` settings and general framework values through the YAML reader, including an environment-specific override attempt before the base key. |
| YAML and locator integration | [`com.ptaf.utils.YamlReader`][yaml-reader], [`elements/`][elements-dir] | Merges YAML from `elements`, `queries`, `api_requests`, `config`, and `performance`. UI-backed file steps resolve the supplied logical page/locator through the existing UI locator system. |
| UI text/download integration | [`PageCommonMethods`][page-common-methods], [`ActionPerformer`][action-performer], [`PageCommonSteps`][page-common-steps] | UI-backed CSV/XML calls `gettext`. The existing strict `download` action waits for a Playwright download, writes it under a feature-named directory, and preserves the download extension. |
| Runner and reports | [`com.ptaf.runner.TestRunner`][test-runner], [`testng.xml`][testng], [`pom.xml`][pom], [`extent.properties`][extent-properties] | Maven’s default suite invokes the Cucumber/TestNG runner; runner plugins and Surefire configure Cucumber, TestNG, and report outputs. |

## Prerequisites

Use **JDK 21** and Maven from the repository root. The Maven compiler source/target are both 21, and the project’s Cucumber/TestNG dependencies and Surefire configuration are declared in [`pom.xml`][pom]. File-only CSV/XML/ZIP scenarios do not need a browser when appropriately tagged: the hooks classify `@csv_file`, `@xml_file`, and `@zip` as non-UI data scenarios. UI-embedded CSV/XML scenarios do require the configured Playwright browser because their step definitions call `Hooks.getPage()`.[Maven build and Surefire configuration][pom] [Browserless file-scenario classification][hooks]

Before working with an archive, ensure the archive path is readable by the build user. For a filesystem fixture, the established data location is `src/test/resources/data/`. The ZIP module accepts either absolute paths or paths relative to the Maven working directory (normally the project root).[ZIP handler behavior][zip-handler]

For UI-backed content or downloads, configure a browser and any required non-secret environment setup in [`src/test/resources/config/config.yml`][config-yml]. Do not place credentials, tokens, client data, or private service URLs in a feature, locator file, or committed test fixture.

## Configuration files and key settings

### ZIP configuration

The ZIP bindings obtain their defaults through `ConfigurationProperties`. The following keys are the module-specific controls in [`src/test/resources/config/config.yml`][config-yml].[ZIP configuration access][configuration-properties]

| Key | Current configured value | Default when absent | Effect |
|---|---:|---:|---|
| `zip.extraction_dir` | `test-output/extracted` | `test-output/extracted` | Base directory; an archive is extracted below a subdirectory named after its filename without `.zip`. |
| `zip.cleanup_after_scenario` | `true` | `true` | The `ZipSteps` `@After` hook deletes the current extraction directory, then clears ZIP context. |
| `zip.recursive_unzip` | `true` | `true` | Causes nested `.zip` files encountered during extraction to be extracted into a sibling subdirectory using the nested archive stem. |

A scenario can override only the extraction *directory* with `Given I unzip file "<ZIP_PATH>" to directory "<EXTRACTION_ROOT>"`; recursive behavior still comes from `zip.recursive_unzip`. There is no YAML setting for CSV delimiter, header presence, XML XPath behavior, or TXT output filename. Those behaviors are controlled by the applicable step and implementation.[ZIP configuration access][configuration-properties] [ZIP Cucumber steps][zip-steps]

A safe, placeholder-only configuration pattern is:

```yaml
zip:
  extraction_dir: "<TEST_OUTPUT_ROOT>/extracted"
  cleanup_after_scenario: true
  recursive_unzip: false
```

### Other relevant framework settings

| Setting or resource | Use in this scope |
|---|---|
| `downloadDocument` in [`config.yml`][config-yml] | Used by the existing `we click download on page …` UI step. It is not read by the ZIP package. |
| `reporting.*` in [`config.yml`][config-yml] | Controls per-feature report generation and output directories; the current file enables per-feature HTML and Glass-style PDF output. |
| [`extent.properties`][extent-properties] | Configures timestamped Extent Spark, Base64 HTML, PDF, Excel, and screenshot output locations. |
| [`elements/*.yml`][elements-dir] | Holds logical page/locator mappings used only when loading CSV/XML text from the UI or triggering a UI download. |
| [`YamlReader`][yaml-reader] | Defines the YAML resource folders loaded on the test classpath. File payloads for this module are not required to be YAML. |

Configuration access first looks for `environments.<env>.<key>`, where the JVM `env` property defaults to `QA`, and then falls back to the plain key. Keep any environment-specific file paths writable and isolated from source fixtures.[ZIP configuration access][configuration-properties]

## Build and exact run commands

Run all commands from `/home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23`.

```bash
# Resolve dependencies and compile production/test source without executing the suite.
mvn clean test-compile

# Execute the existing file-only CSV examples.
mvn clean test -Dcucumber.filter.tags="@csv_example and @csv_file"

# Execute the existing file-only XML examples.
mvn clean test -Dcucumber.filter.tags="@xml_example and @xml_file"

# After adding a ZIP feature tagged @zip, execute ZIP scenarios only.
mvn clean test -Dcucumber.filter.tags="@zip"
```

The default Maven Surefire suite is [`src/test/resources/testng.xml`][testng], which invokes `com.ptaf.runner.TestRunner`; its feature root is `src/test/resources/features` and its glue includes `com.ptaf.stepdefinitions` and `com.ptaf.hooks`. The tag system property narrows execution to the examples above. The default runner annotation itself is currently set to `@eStore`, so omitting `-Dcucumber.filter.tags=…` does **not** select the CSV/XML examples.[Default Cucumber runner][test-runner] [CSV example feature][csv-feature]

> **Build-result caveat:** Surefire is configured with `testFailureIgnore=true`. Inspect the generated Cucumber/TestNG/Extent artifacts rather than relying only on Maven’s process exit code when using the default profile.[Maven build and Surefire configuration][pom]

## How to create a new feature/test

1. **Choose the test type and location.** Put a CSV feature under `src/test/resources/features/csv/`, an XML feature under `src/test/resources/features/xml/`, and a new archive workflow under `src/test/resources/features/zip/` (create this directory; it is not currently present). Keep reusable non-sensitive data under `src/test/resources/data/`.
2. **Tag the scenario correctly.** Use `@csv_file`, `@xml_file`, or `@zip` for an entirely file-based scenario so the hooks skip browser initialization. Use `@csv_ui` or `@xml_ui` only when a browser is required to obtain visible text.[Browserless file-scenario classification][hooks]
3. **Load before asserting.** CSV and XML assertion/extraction steps require their corresponding context to be populated first. ZIP discovery, conversion, and ZIP-based loaders require a prior unzip step in the same scenario.[ZIP Cucumber steps][zip-steps]
4. **Use exact names.** CSV header matching is case-sensitive; CSV row and column indexes are 1-based. XML simple names return the first matching element; use a full XPath beginning with `/` when the target must be precise. ZIP filename lookup is case-insensitive, but if duplicate names exist in different archive directories, the first discovered match is used.[CSV handler behavior][csv-handler] [XML handler behavior][xml-handler]
5. **Keep archive output disposable.** Use an extraction root under `test-output`, enable automatic cleanup for ordinary runs, and retain output only intentionally for diagnosis. Do not direct an extraction to a source or shared directory.
6. **Run a narrow tag expression** using the commands above and review the expected artifacts described below.

### Tested-style CSV and XML feature patterns

The following steps use only implemented bindings and placeholders. They model the file-only examples located in [`features/csv/csv_automation_example.feature`][csv-feature] and [`features/xml/xml_automation_example.feature`][xml-feature].

```gherkin
@csv @csv_file @<TAG>
Feature: Validate a delimited export

  Scenario: Validate a CSV fixture by header and row
    Given I load CSV file "src/test/resources/data/<CSV_FILE>.csv"
    Then CSV column "<HEADER_NAME>" exists
    Then CSV row count is at least <MINIMUM_ROWS>
    Then CSV row 1 column "<HEADER_NAME>" equals "<EXPECTED_VALUE>"
    When I extract CSV row 1 column "<HEADER_NAME>" and store as "<CSV_VARIABLE>"
    Then CSV row 1 column "<HEADER_NAME>" equals stored value "<CSV_VARIABLE>"
```

```gherkin
@xml @xml_file @<TAG>
Feature: Validate an XML fixture

  Scenario: Validate a precise XML value
    Given I load XML file "src/test/resources/data/<XML_FILE>.xml"
    Then XML node "<UNIQUE_NODE_NAME>" exists
    Then XML XPath "<XPATH_EXPRESSION>" equals "<EXPECTED_VALUE>"
    Then XML XPath "<COLLECTION_XPATH>" count equals <EXPECTED_COUNT>
    When I extract XML XPath "<XPATH_EXPRESSION>" and store as "<XML_VARIABLE>"
    Then XML node "<UNIQUE_NODE_NAME>" does not equal "<UNEXPECTED_VALUE>"
```

For custom CSV separators, the implemented binding uses the first character of the supplied delimiter; the literal `"\\t"` is converted to a tab. Header presence defaults to `true`. Although `CsvFileHandler` offers `setHasHeaders(false)` to Java callers, there is no existing Gherkin step that changes this setting, so feature authors should not assume a headerless-file switch exists.[CSV handler behavior][csv-handler] [CSV Cucumber steps][csv-steps]

```gherkin
@csv @csv_file @<TAG>
Feature: Validate a custom-delimiter export

  Scenario: Load a custom-delimited file
    Given I load CSV file "src/test/resources/data/<DELIMITED_FILE>.txt" with delimiter "<DELIMITER>"
    Then CSV row 1 column index 1 equals "<EXPECTED_FIRST_FIELD>"
```

### Tested-style ZIP and TXT-to-CSV workflow

No ZIP feature or ZIP fixture is currently supplied in `src/test/resources`; use the following as a new, placeholder-only test pattern. It combines only the steps present in `ZipSteps`, `CsvSteps`, and `XmlSteps`.[ZIP Cucumber steps][zip-steps]

```gherkin
@zip @<TAG>
Feature: Validate archive contents

  Scenario: Extract, convert, and validate report files
    Given I unzip file "<ZIP_PATH>" to directory "test-output/<RUN_LABEL>/extracted"
    Then zip contains file "<DELIMITED_TEXT_FILE>.txt"
    Then zip contains a "xml" file
    When I convert txt file "<DELIMITED_TEXT_FILE>.txt" to CSV using delimiter "<DELIMITER>"
    Given I load CSV from zip file "<DELIMITED_TEXT_FILE>.csv"
    Then CSV column "<CSV_HEADER>" exists
    Then CSV row 1 column "<CSV_HEADER>" equals "<EXPECTED_CSV_VALUE>"
    Given I load XML from zip file "<XML_FILE>.xml"
    Then XML XPath "<XPATH_EXPRESSION>" equals "<EXPECTED_XML_VALUE>"
    Then I cleanup extracted zip files
```

The convenience step `When I convert txt file "<TEXT_FILE>.txt" to CSV` uses `|` as the delimiter. TXT conversion writes `<TEXT_FILE>.csv` next to the TXT input, trims each split field, joins fields with commas, and quotes an output field containing a comma, quote, or newline. It uses a new CSV handler with the usual comma delimiter when loaded through a ZIP step. Conversion can overwrite an existing same-basename CSV in the extraction directory; choose unique archive content names.[ZIP handler behavior][zip-handler]

## Module behavior and input/output safety

### CSV behavior

`CsvFileHandler` reads filesystem data as UTF-8. Blank lines are skipped. By default, the first parsed row becomes the headers; remaining rows are data. Each nonblank physical line is parsed independently, supports delimiter characters outside double quotes, and converts doubled double quotes inside quoted fields to a single quote character. Because parsing is line-by-line, do not rely on a quoted field spanning physical lines despite the general quoted-field handling described in code comments.[CSV handler behavior][csv-handler]

Every loaded CSV has isolated `ThreadLocal` context. A loaded document and extracted CSV variables are cleared after each scenario by `CsvSteps`. Assertions fail clearly when the context is absent, a 1-based row/index is out of range, a required header is absent, or a named variable was not extracted first.[CSV Cucumber steps][csv-steps]

### XML behavior and safety

A query beginning with `/` is evaluated as XPath; any other query is converted to `//<query>`, which returns the first matching element for value lookup. `count equals` accepts an XPath expression, while attribute assertions inspect the first matching node. A malformed XPath supplied to an existence check is logged and treated as absent; malformed XPath in value/count/attribute operations raises a runtime exception.[XML handler behavior][xml-handler]

XML parsing explicitly rejects a DOCTYPE and disables external general/parameter entities and entity expansion. This reduces XXE exposure when XML text comes from a UI element or other untrusted source. XML state and extracted variables are per thread and are cleared after each scenario.[XML handler behavior][xml-handler]

### ZIP extraction, discovery, and cleanup safety

The ZIP extractor checks each entry’s canonical path against the canonical target directory and skips an entry that would escape it, mitigating ZIP-slip path traversal. It creates parent directories as necessary, groups discovered files by lowercase extension (`noext` for extensionless files), and can recursively extract nested archives.[ZIP handler behavior][zip-handler]

`ThreadLocal` ZIP context isolates references between parallel scenario threads, but it does **not** make a shared filesystem target unique. Extraction uses `<targetDir>/<zip-stem>`; two parallel scenarios with the same archive filename and target root can collide. Use distinct archive names or a distinct per-run/per-scenario extraction root, particularly when Maven’s default TestNG suite runs methods in parallel.[Maven build and Surefire configuration][pom] [ZIP handler behavior][zip-handler]

Cleanup recursively deletes the extraction directory held by the current ZIP context. It catches/logs I/O cleanup errors rather than failing the scenario. Configure only a dedicated disposable output directory; neither the cleanup method nor the custom-target Gherkin step constrains deletion to `test-output`.[ZIP handler behavior][zip-handler]

Recursive extraction has no configured depth, file-count, compressed-size, or expanded-size limit. Treat untrusted or unusually large nested archives as out of scope for this helper unless the execution environment has appropriate input controls.

## UI text and file-download integration

### UI-embedded CSV/XML

`Given I load CSV from UI element on page "<PAGE>" locator "<LOCATOR>"` and its XML equivalent obtain the active Playwright `Page`, construct `PageCommonMethods`, and call `gettext`. The locator identifiers must exist in a YAML mapping under [`src/test/resources/elements/`][elements-dir]. The resulting text must be nonblank; CSV must be parseable under the CSV rules and XML must be well-formed. This integration reads visible text through the UI layer—it does **not** read an `<input>`/`<textarea>` value attribute directly. The XML binding explicitly advises using a value-reading step for such elements if text is not exposed.[CSV Cucumber steps][csv-steps] [XML Cucumber steps][xml-steps]

```yaml
# src/test/resources/elements/<PAGE_FILE>.yml
# Placeholder locator form only; use the repository’s established YAML locator syntax.
elements:
  <PAGE>:
    <CSV_OR_XML_TEXT_LOCATOR>: "<LOCATOR_EXPRESSION>"
    <DOWNLOAD_TRIGGER_LOCATOR>: "<LOCATOR_EXPRESSION>"
```

### Existing browser-download behavior and ZIP boundary

The existing UI step is:

```gherkin
And we click download on page <PAGE> locator <DOWNLOAD_TRIGGER_LOCATOR>
```

It gets `downloadDocument` from `config.yml`, passes that value with a `.jpeg` suffix to `PageCommonMethods.download`, and the strict `download` action waits for the Playwright download event. `ActionPerformer` treats the passed value as a **directory**, creates a feature-title subdirectory, and saves a feature-title/timestamp filename while preserving the source file extension.[UI download binding][page-common-steps] [Playwright download action][action-performer]

The current `PageCommonMethods.download` method returns `void`, and `PageCommonSteps` does not store the `ActionPerformer` return value. Consequently, the standard UI download step has **no implemented Gherkin variable or direct handoff** that passes the dynamic saved ZIP path into `Given I unzip file "…"`. A ZIP scenario can unzip a known filesystem path, but a fully dynamic browser-download-to-ZIP chain requires implementation beyond this guide’s scope. Do not assume the ZIP-step Javadoc’s illustrative “path returned by the UI download step” is an available feature binding.[ZIP Cucumber steps][zip-steps] [UI page helper][page-common-methods] [Playwright download action][action-performer]

## Feature, data, locator, payload, and query locations

| Asset category | Current location | Relationship to this module |
|---|---|---|
| CSV examples | [`src/test/resources/features/csv/csv_automation_example.feature`][csv-feature] | Existing tagged CSV file/UI example suite. |
| XML examples | [`src/test/resources/features/xml/xml_automation_example.feature`][xml-feature] | Existing tagged XML file/UI example suite. |
| ZIP features | `src/test/resources/features/zip/` | Not currently present; create it for archive scenarios. |
| File fixtures | [`src/test/resources/data/`][data-dir] | Contains the repository CSV/XML examples. No ZIP/TXT fixture was found here. |
| UI locators | [`src/test/resources/elements/`][elements-dir] | YAML page/locator maps for UI-backed text loading and UI download triggers. |
| Global/ZIP configuration | [`src/test/resources/config/config.yml`][config-yml] | Contains the `zip` block, download setting, reporting configuration, and broader framework settings. |
| API request definitions | [`src/test/resources/api_requests/api_requests.yml`][api-requests] | API module resource; not consumed by CSV/XML/ZIP handlers. |
| Database queries | [`src/test/resources/queries/db_queries.yml`][db-queries] | Database module resource; not consumed by CSV/XML/ZIP handlers. |
| Performance payloads | [`src/test/resources/performance/payloads/`][performance-payloads] | Performance module resource; not consumed by CSV/XML/ZIP handlers. |
| UI-performance data/locators | [`src/test/resources/ui_performance/data/`][ui-performance-data] and [`locators/`][ui-performance-locators] | Separate UI-performance module resources; not read by the data-file packages. |

## Expected reports and artifacts

| Artifact | Expected location | Notes |
|---|---|---|
| Cucumber pretty HTML | `target/cucumber-reports/cucumber-pretty` | Configured in `com.ptaf.runner.TestRunner`. |
| Cucumber JSON / rerun list | `target/cucumber-reports/CucumberTestReport.json`; `target/cucumber-reports/rerun.txt` | Configured in the runner. |
| Surefire report files | `target/surefire-reports/` | Configured in Surefire. |
| Additional Cucumber JSON/XML | `target/cucumber-reports/cucumber.json`; `target/cucumber-reports/cucumber.xml` | Set in Surefire system properties. |
| Extent outputs | `test-output/<timestamp>/SparkReport/Spark.html`, `Base64Report/Report.html`, `PdfReport/FNB-PTAF-Report.pdf`, `ExcelReport/FNB-PTAF-Report.xlsx` | Timestamped root and reporter paths come from `extent.properties`. |
| Per-feature reports | `test-output/per-feature-reports/` and `test-output/per-feature-reports-glass/` | Controlled by the `reporting.*` configuration. |
| ZIP extraction | `test-output/extracted/<ZIP_STEM>/` by default | Created when a ZIP scenario runs; normally removed after the scenario because cleanup is enabled. |
| UI download | Under the configured download root in a feature-name subdirectory | Dynamic feature-title/timestamp filename; source extension is preserved. |

Reports describe scenario execution, not a separate CSV/XML/ZIP-specific report. ZIP files and converted CSV files are working artifacts, not automatically embedded report attachments by the file modules.[Extent configuration][extent-properties] [Default Cucumber runner][test-runner] [Playwright download action][action-performer]

## Troubleshooting

| Symptom | Likely code-level cause | Resolution |
|---|---|---|
| `No CSV data is loaded` or `No XML document is loaded` | An assertion/extraction precedes a load step, or a prior scenario’s context was cleared. | Put the appropriate `Given I load …` step before dependent assertions; keep dependencies within one scenario. |
| CSV header not found | Header matching is case-sensitive, or the first row was not the intended header. | Verify exact header spelling/case and load with the correct delimiter. There is no Gherkin headerless toggle. |
| CSV row/index out of range | Numbers are 1-based and rows exclude the header when headers are enabled. | Correct the index and validate the data-row count first. |
| CSV values split incorrectly | The wrong delimiter was used. | Use `I load CSV file "…" with delimiter "<DELIMITER>"`; use `"\\t"` for tab. |
| XML parsing fails | Input is blank, malformed, or contains a disallowed DOCTYPE/external-entity construct. | Provide well-formed XML without external entity resolution; for UI scenarios, confirm the element exposes XML as text. |
| XML simple node returns an unexpected value | A simple query resolves to the first matching element anywhere in the document. | Replace it with a precise XPath beginning with `/` or `//`. |
| XML existence assertion says absent for malformed XPath | `nodeExists` handles XPath evaluation errors as `false`. | Validate the XPath independently; use a value/count assertion when a malformed XPath should fail loudly. |
| `Cannot … — no ZIP file has been extracted` | A ZIP-dependent step ran before `I unzip file`. | Unzip in the same scenario before discovery, conversion, or ZIP-based CSV/XML load steps. |
| ZIP file or extracted member is not found | The path/name is wrong, the archive is not `.zip`, or the expected member was not discovered. | Use a project-root-relative or absolute archive path; add `Then zip contains file "<FILE>"` before loading. |
| Parallel ZIP scenarios interfere | Same `zip-stem` and extraction root map to the same directory. | Set a unique target directory through the explicit unzip-to-directory step, or use unique archive names. |
| Converted CSV is missing or replaces data | TXT member name was wrong, conversion failed, or a same-basename CSV already existed. | Assert the TXT member first and choose a unique TXT basename; conversion writes beside the TXT file. |
| UI CSV/XML loader sees no text | The locator is wrong, text is not visible, or the element stores content in `value`. | Check the locator YAML and application state. For input/textarea value content, use a value-reading UI capability; the provided loader uses `gettext`. |
| Browser download cannot be passed to ZIP step | The current UI download binding does not expose the saved dynamic path to Gherkin. | Validate a known path in a separate file workflow, or implement an approved handoff outside this documentation task. |
| Maven appears successful despite failed scenarios | Default Surefire uses `testFailureIgnore=true`. | Read Cucumber, Surefire, and Extent reports for actual scenario status. |

## Module boundaries with the rest of FNB-ETAF

The file modules deliberately reuse the framework rather than duplicate it. `CsvSteps` and `XmlSteps` depend on the UI module only for the optional text-extraction path; their filesystem path does not need Playwright. `ZipSteps` depends on the CSV and XML handlers to load extracted members but does not duplicate their assertion logic. The hooks package controls whether a scenario receives a browser, and the runner/reporting packages own suite selection and evidence generation.[Browserless file-scenario classification][hooks] [Default Cucumber runner][test-runner] [ZIP Cucumber steps][zip-steps]

API request definitions, database query YAML, performance payloads, mobile assets, and UI-performance resources are separate modules. A scenario may conceptually compare values between modules, but the CSV and XML extracted-variable stores are separate `HashMap` instances owned by their respective step classes; no implemented cross-module placeholder substitution or cross-store assertion is provided by these bindings. Use explicit module steps or a new approved integration when a workflow needs data transfer beyond the steps documented here.[CSV common methods][csv-common] [XML handler behavior][xml-handler][xml-common]

## References

<!-- Visible source-reference list -->
The sources below are visible and clickable in Markdown preview. Citation labels used in this guide point to the same source files.

- **[csv-handler]** [csv-handler](../../src/main/java/com/ptaf/csv/CsvFileHandler.java) — `../../src/main/java/com/ptaf/csv/CsvFileHandler.java`
- **[csv-common]** [csv-common](../../src/main/java/com/ptaf/csv/CsvCommonMethods.java) — `../../src/main/java/com/ptaf/csv/CsvCommonMethods.java`
- **[csv-context]** [csv-context](../../src/main/java/com/ptaf/csv/CsvContext.java) — `../../src/main/java/com/ptaf/csv/CsvContext.java`
- **[csv-steps]** [csv-steps](../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java`
- **[xml-handler]** [xml-handler](../../src/main/java/com/ptaf/xml/XmlFileHandler.java) — `../../src/main/java/com/ptaf/xml/XmlFileHandler.java`
- **[xml-common]** [xml-common](../../src/main/java/com/ptaf/xml/XmlCommonMethods.java) — `../../src/main/java/com/ptaf/xml/XmlCommonMethods.java`
- **[xml-context]** [xml-context](../../src/main/java/com/ptaf/xml/XmlContext.java) — `../../src/main/java/com/ptaf/xml/XmlContext.java`
- **[xml-steps]** [xml-steps](../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java`
- **[zip-handler]** [zip-handler](../../src/main/java/com/ptaf/zip/ZipFileHandler.java) — `../../src/main/java/com/ptaf/zip/ZipFileHandler.java`
- **[zip-context]** [zip-context](../../src/main/java/com/ptaf/zip/ZipContext.java) — `../../src/main/java/com/ptaf/zip/ZipContext.java`
- **[zip-steps]** [zip-steps](../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java`
- **[configuration-properties]** [configuration-properties](../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — `../../src/main/java/com/ptaf/utils/ConfigurationProperties.java`
- **[yaml-reader]** [yaml-reader](../../src/main/java/com/ptaf/utils/YamlReader.java) — `../../src/main/java/com/ptaf/utils/YamlReader.java`
- **[page-common-methods]** [page-common-methods](../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java) — `../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java`
- **[action-performer]** [action-performer](../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) — `../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java`
- **[page-common-steps]** [page-common-steps](../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java) — `../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`
- **[hooks]** [hooks](../../src/main/java/com/ptaf/hooks/Hooks.java) — `../../src/main/java/com/ptaf/hooks/Hooks.java`
- **[feature-artifact-name-resolver]** [feature-artifact-name-resolver](../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) — `../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`
- **[test-runner]** [test-runner](../../src/test/java/com/ptaf/runner/TestRunner.java) — `../../src/test/java/com/ptaf/runner/TestRunner.java`
- **[testng]** [testng](../../src/test/resources/testng.xml) — `../../src/test/resources/testng.xml`
- **[pom]** [pom](../../pom.xml) — `../../pom.xml`
- **[config-yml]** [config-yml](../../src/test/resources/config/config.yml) — `../../src/test/resources/config/config.yml`
- **[extent-properties]** [extent-properties](../../src/test/resources/extent.properties) — `../../src/test/resources/extent.properties`
- **[csv-feature]** [csv-feature](../../src/test/resources/features/csv/csv_automation_example.feature) — `../../src/test/resources/features/csv/csv_automation_example.feature`
- **[xml-feature]** [xml-feature](../../src/test/resources/features/xml/xml_automation_example.feature) — `../../src/test/resources/features/xml/xml_automation_example.feature`
- **[data-dir]** [data-dir](../../src/test/resources/data/) — `../../src/test/resources/data/`
- **[elements-dir]** [elements-dir](../../src/test/resources/elements/) — `../../src/test/resources/elements/`
- **[api-requests]** [api-requests](../../src/test/resources/api_requests/api_requests.yml) — `../../src/test/resources/api_requests/api_requests.yml`
- **[db-queries]** [db-queries](../../src/test/resources/queries/db_queries.yml) — `../../src/test/resources/queries/db_queries.yml`
- **[performance-payloads]** [performance-payloads](../../src/test/resources/performance/payloads/) — `../../src/test/resources/performance/payloads/`
- **[ui-performance-data]** [ui-performance-data](../../src/test/resources/ui_performance/data/) — `../../src/test/resources/ui_performance/data/`
- **[ui-performance-locators]** [ui-performance-locators](../../src/test/resources/ui_performance/locators/) — `../../src/test/resources/ui_performance/locators/`

<!-- Internal citation definitions used by the in-text [n] links. Keep these definitions so citations remain clickable. -->
[csv-handler]: ../../src/main/java/com/ptaf/csv/CsvFileHandler.java
[csv-common]: ../../src/main/java/com/ptaf/csv/CsvCommonMethods.java
[csv-context]: ../../src/main/java/com/ptaf/csv/CsvContext.java
[csv-steps]: ../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java
[xml-handler]: ../../src/main/java/com/ptaf/xml/XmlFileHandler.java
[xml-common]: ../../src/main/java/com/ptaf/xml/XmlCommonMethods.java
[xml-context]: ../../src/main/java/com/ptaf/xml/XmlContext.java
[xml-steps]: ../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java
[zip-handler]: ../../src/main/java/com/ptaf/zip/ZipFileHandler.java
[zip-context]: ../../src/main/java/com/ptaf/zip/ZipContext.java
[zip-steps]: ../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java
[configuration-properties]: ../../src/main/java/com/ptaf/utils/ConfigurationProperties.java
[yaml-reader]: ../../src/main/java/com/ptaf/utils/YamlReader.java
[page-common-methods]: ../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java
[action-performer]: ../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java
[page-common-steps]: ../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java
[hooks]: ../../src/main/java/com/ptaf/hooks/Hooks.java
[feature-artifact-name-resolver]: ../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java
[test-runner]: ../../src/test/java/com/ptaf/runner/TestRunner.java
[testng]: ../../src/test/resources/testng.xml
[pom]: ../../pom.xml
[config-yml]: ../../src/test/resources/config/config.yml
[extent-properties]: ../../src/test/resources/extent.properties
[csv-feature]: ../../src/test/resources/features/csv/csv_automation_example.feature
[xml-feature]: ../../src/test/resources/features/xml/xml_automation_example.feature
[data-dir]: ../../src/test/resources/data/
[elements-dir]: ../../src/test/resources/elements/
[api-requests]: ../../src/test/resources/api_requests/api_requests.yml
[db-queries]: ../../src/test/resources/queries/db_queries.yml
[performance-payloads]: ../../src/test/resources/performance/payloads/
[ui-performance-data]: ../../src/test/resources/ui_performance/data/
[ui-performance-locators]: ../../src/test/resources/ui_performance/locators/
