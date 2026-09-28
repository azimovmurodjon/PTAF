# CSV, XML, TXT, ZIP, Excel, and File Utility Reference

## Purpose and scope

This chapter is the implementation reference for **file-resident test data and file utilities** in the current FNB-ETAF repository: CSV assertion/extraction, XML assertion/extraction, ZIP extraction and discovery, delimited TXT-to-CSV conversion, the Excel cell reader, and Excel-to-YAML utility. It also documents the limited integration points with ordinary Playwright UI actions and the normal Cucumber lifecycle.

The core CSV, XML, ZIP, TXT, and Excel readers work on files or in-memory content that is already available to the JVM. They do **not** fetch remote files, upload files, create a browser download, or transfer a dynamic path between Gherkin steps. Browser downloads, uploads, and UI-text loading belong to the Playwright/UI layer; protocol-performance payload resolution is a separate, browserless consumer of the CSV/Excel readers. [1] [4] [7] [10] [16] [19]

> **Safe-data rule.** Keep fixtures, archive contents, YAML, and feature files free of credentials, tokens, customer data, and private endpoints. Examples below use placeholders only.

## Source map and responsibilities

| Concern | Primary implementation | Inputs | Outputs/state | Boundary |
|---|---|---|---|---|
| CSV scenario module | [`CsvFileHandler`](../../../src/main/java/com/ptaf/csv/CsvFileHandler.java), [`CsvCommonMethods`](../../../src/main/java/com/ptaf/csv/CsvCommonMethods.java), [`CsvSteps`](../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java) | UTF-8 file path or nonblank text; optional one-character delimiter | Parsed headers/data rows in `CsvContext`; scenario-local extracted values | No file writing and no direct download/upload capability. |
| XML scenario module | [`XmlFileHandler`](../../../src/main/java/com/ptaf/xml/XmlFileHandler.java), [`XmlCommonMethods`](../../../src/main/java/com/ptaf/xml/XmlCommonMethods.java), [`XmlSteps`](../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java) | File path or nonblank XML string | Parsed DOM in `XmlContext`; scenario-local extracted values | No XML writer, schema validation, or remote retrieval. |
| ZIP/TXT module | [`ZipFileHandler`](../../../src/main/java/com/ptaf/zip/ZipFileHandler.java), [`ZipContext`](../../../src/main/java/com/ptaf/zip/ZipContext.java), [`ZipSteps`](../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java) | Local `.zip` path, extraction root, optional TXT delimiter | `<root>/<zip-stem>/` working directory, discovered-file map, optional adjacent `.csv` | Reuses CSV/XML handlers for archive members; does not download an archive. |
| Excel cell reader | [`ExcelReader`](../../../src/main/java/com/ptaf/utils/ExcelReader.java) | Workbook path, first-column row key, header name | A `String` cell representation or `null` | Java utility; no CSV/XML/ZIP Gherkin step calls it directly. |
| Excel-to-YAML utility | [`ExcelToYaml`](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java) | `.xlsx` path, `testcase_id` filter, destination path | YAML file when data is selected | Java utility; no Cucumber binding or Maven command is supplied by this module. |
| Performance payload adapters | [`PerformancePayloadResolver`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java), [`CsvPayloadReader`](../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java), [`PerformanceSteps`](../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java) | CSV/Excel payload path, row identifier, column | Request-body string passed to performance request construction | Separate from CSV assertion context and ordinary UI automation. |
| UI file boundary | [`PageCommonMethods`](../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java), [`ActionPerformer`](../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java), [`PageCommonSteps`](../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java) | Resolved YAML locator plus UI action/value | Playwright text, upload selection, or downloaded artifact | Requires an active browser; it is not an API of the data-file packages. |

The Maven build uses Java 21, Cucumber, Playwright, Apache POI/POI-OOXML, and SnakeYAML. Apache POI is the dependency used by both Excel utilities; the CSV and XML scenario handlers themselves use JDK APIs. [1] [2] [5] [14] [15]

## Execution classification: browserless versus browser-backed

The shared Cucumber hook evaluates tags before initializing Playwright. It treats `@csv_file`, `@xml_file`, and `@zip` as non-UI data tags. A feature path under `/features/zip/` is also file-only. By contrast, `@csv_ui` and `@xml_ui` explicitly prevent file-only classification, because those steps call `Hooks.getPage()` and read an element through the UI layer. [18]

| Scenario form | Recommended identifying tag(s) | Browser initialized by shared `Hooks`? | Why |
|---|---|---:|---|
| Filesystem CSV | `@csv_file` | No | The hook recognizes the tag as a non-UI data scenario. |
| Filesystem XML | `@xml_file` | No | The hook recognizes the tag as a non-UI data scenario. |
| ZIP/TXT/CSV/XML workflow | `@zip` | No | ZIP is a non-UI data tag; a `features/zip/` path also qualifies. |
| CSV or XML displayed by a web page | `@csv_ui` or `@xml_ui` | Yes | The loader obtains the current Playwright page and calls `gettext`. |
| CSV/Excel protocol-performance payload | `@performance_*` or a feature under `features/performance/` | No | Performance tags/feature path are recognized before the file-only classifier. |

The ordinary TestNG/Cucumber runner scans `src/test/resources/features` and includes both `com.ptaf.stepdefinitions` and `com.ptaf.hooks`. Its annotation currently selects `@eStore`; a narrow `-Dcucumber.filter.tags=...` invocation is therefore needed to select the checked-in CSV/XML examples. Surefire is configured for method-level parallelism with four threads and `testFailureIgnore=true`, so report status is more reliable than Maven exit status in the default profile. [1] [23] [24]

```bash
# Run from the repository root. These select existing, file-only example tags.
mvn clean test -Dcucumber.filter.tags="@csv_example and @csv_file"
mvn clean test -Dcucumber.filter.tags="@xml_example and @xml_file"

# Pattern for a newly added ZIP feature that carries @zip.
mvn clean test -Dcucumber.filter.tags="@zip"
```

## Configuration, storage, and path resolution

### Existing configuration keys

The ZIP module has exactly three module-specific configuration keys in `src/test/resources/config/config.yml`. `ConfigurationProperties` first looks for `environments.<env>.<key>`, with `env` defaulting to `QA`, then falls back to the plain key. `YamlReader` loads and merges `.yml`/`.yaml` files beneath the classpath folders `elements`, `queries`, `api_requests`, `config`, and `performance`. [11] [12] [13]

| Key | Current repository value | Default if absent | Consumed by | Effect |
|---|---|---|---|---|
| `zip.extraction_dir` | `test-output/extracted` | `test-output/extracted` | `ZipSteps` / `ConfigurationProperties` | Base extraction root. The handler creates `<root>/<zip-stem>/`. |
| `zip.cleanup_after_scenario` | `true` | `true` | `ZipSteps` `@After` hook | Deletes the current scenario's extraction directory when an archive was extracted. |
| `zip.recursive_unzip` | `true` | `true` | `ZipSteps` / `ZipFileHandler` | Recursively extracts nested `.zip` members. |
| `excelDocumentLocation` | `src/test/resources/testdata.xlsx` | No code default | General configuration accessor | Exposed by `getExcelDocumentLocation()`; it is not consulted by `ExcelReader.getData(...)`, which receives an explicit path. |
| `downloadDocument` | `src/test/downloads/` | No code default | Existing ordinary UI download step | A UI-layer value, not a ZIP setting and not used by `ZipFileHandler`. |

There is **no** YAML key in the current implementation for CSV header presence, CSV delimiter, XML query mode, TXT output filename, ZIP size/depth limits, or Excel sheet selection. The CSV Gherkin loader selects its delimiter per step; header handling remains enabled in that Gherkin path. [2] [4] [8] [10] [14]

A configuration-shaped example, with no operational values, is:

```yaml
zip:
  extraction_dir: "test-output/<RUN_LABEL>/extracted"
  cleanup_after_scenario: true
  recursive_unzip: false
```

### File locations and path semantics

| Item | Current location / resolution rule | Operational detail |
|---|---|---|
| CSV/XML example fixtures | `src/test/resources/data/` | The repository includes `sample_transactions.csv` and `sample_order_response.xml`; use non-sensitive fixtures in the same style. |
| CSV/XML feature examples | `src/test/resources/features/csv/` and `src/test/resources/features/xml/` | They demonstrate implemented bindings; the CSV and XML examples include both file and UI modes. [4] [7] |
| ZIP source | Absolute path or project-working-directory-relative path | The handler rejects absent files and names not ending in `.zip`. [8] |
| ZIP working output | `<targetDir>/<source ZIP filename without .zip>/` | The directory is created automatically. A custom target root is possible in Gherkin. [8] [10] |
| CSV/XML direct file paths | `Path.of(...)` relative to Maven's current working directory unless absolute | Both loaders validate nonblank paths and existence. [2] [5] |
| Performance CSV payload | Classpath as supplied; classpath with leading `/` removed; direct filesystem path; then `src/test/resources/<path>` | This resolution order applies **only** to `CsvPayloadReader`, not the CSV assertion handler. [25] |
| Excel reader path | Explicit `FileInputStream(filePath)` | No classpath fallback and no `excelDocumentLocation` lookup inside `ExcelReader`. [14] |

> **Parallel-storage caution.** `CsvContext`, `XmlContext`, and `ZipContext` isolate Java state with `ThreadLocal`; they do not make the filesystem unique. Two parallel ZIP scenarios using the same target root and archive stem write to the same `<root>/<stem>` directory. Use a unique target root or archive basename. [3] [6] [9] [8]

## CSV reference

### Execution flow and parser behavior

`Given I load CSV file "<PATH>"` creates a new `CsvFileHandler`, reads the file as UTF-8, parses it, and places the handler in the thread-local `CsvContext`. Assertions and extraction retrieve the current handler through `CsvCommonMethods`; `CsvSteps` clears the context and its instance variable store in an `@After` hook. [2] [3] [4]

The default parser treats the first nonblank physical line as headers and each following nonblank physical line as a data row. Header names are case-sensitive. Gherkin row numbers and column indexes are one-based; row 1 is the first **data** row, not the header. A custom delimiter binding accepts the first character of the supplied string, with the literal `"\\t"` mapped to a tab. [2] [4]

| Supported behavior | Exact implementation consequence |
|---|---|
| Default delimiter | Comma (`','`). |
| Delimiter alternatives | A Java caller can set any `char`; the Gherkin custom-delimiter step takes the first character only. |
| Header mode | `hasHeaders` defaults to `true`; a Java setter exists, but there is **no Gherkin binding** to set it to `false`. A headerless feature must not assume one. |
| Quoting | A delimiter inside double quotes does not split a field; doubled quotes inside a quoted field become one quote. |
| Empty fields | Consecutive delimiters produce empty values; missing fields at the end of a short row are also mapped to empty strings. |
| Blank lines | Skipped. |
| Multi-line quoted cells | The parser reads physical lines before parsing them. Despite comments mentioning newlines in quoted fields, it does not join lines; do not use quoted fields that span physical lines. |
| Output artifact | None. Loading is in-memory only. |

### CSV Gherkin bindings

All of the following are implemented by `CsvSteps`; placeholders are intentionally non-sensitive. [4]

```gherkin
@csv @csv_file @<TAG>
Feature: Validate an exported delimited file

  Scenario: Check content and retain a value in this scenario
    Given I load CSV file "src/test/resources/data/<EXPORT_FILE>.csv"
    Then CSV column "<HEADER>" exists
    Then CSV row count is at least <MINIMUM_ROW_COUNT>
    Then CSV row 1 column "<HEADER>" equals "<EXPECTED_VALUE>"
    Then CSV row 1 column "<TEXT_HEADER>" contains "<EXPECTED_SUBSTRING>"
    Then CSV row 1 column "<HEADER>" does not equal "<UNEXPECTED_VALUE>"
    When I extract CSV row 1 column "<HEADER>" and store as "<CSV_VARIABLE>"
    Then CSV row 1 column "<HEADER>" equals stored value "<CSV_VARIABLE>"
```

```gherkin
@csv @csv_file @<TAG>
Feature: Validate a custom-delimited file

  Scenario: Load a TSV or pipe export
    Given I load CSV file "src/test/resources/data/<DELIMITED_FILE>.txt" with delimiter "\\t"
    Then CSV row 1 column index 1 equals "<FIRST_FIELD>"

  Scenario: Check a pipe-delimited export
    Given I load CSV file "src/test/resources/data/<DELIMITED_FILE>.txt" with delimiter "|"
    Then all CSV rows have column "<STATUS_HEADER>" equals "<EXPECTED_STATUS>"
```

The UI-embedded route is separate and requires an active browser plus a valid logical YAML element group/key:

```gherkin
@csv @csv_ui @<TAG>
Feature: Validate CSV rendered by an application

  Scenario: Read visible CSV text
    # Navigation and triggering actions are application-specific.
    Given I load CSV from UI element on page "<ELEMENT_GROUP>" locator "<CSV_TEXT_KEY>"
    Then CSV row count is at least 1
    Then CSV column "<HEADER>" exists
```

The loader calls `PageCommonMethods.gettext`, which resolves the supplied values through the existing locator system and returns `textContent`. It requires nonblank text. It is suitable for visible text such as a `<pre>`/code-style region, but it does not read an `<input>` or `<textarea>` `value` attribute. [4] [21] [19]

### CSV failures and diagnosis

| Symptom | Source-visible cause | Corrective action |
|---|---|---|
| `No CSV data is loaded` | An assertion/extraction ran before a successful load or after cleanup. | Add a CSV load in the same scenario before dependent steps. |
| File not found | The file path is blank, wrong, or is resolved from an unexpected working directory. | Run from the repository root; use an existing relative path or an approved absolute path. |
| Column not found | Header matching is case-sensitive. | Copy the exact header spelling/case; confirm the selected delimiter. |
| Row/index out of range | Rows and indexes are one-based; the header is excluded in default header mode. | Assert/count data rows first and correct the index. |
| Values split incorrectly | The selected delimiter does not match the file. | Use the custom delimiter step; use `"\\t"` for a tab. |
| Unexpected result with quoted multi-line data | The implementation parses each physical line independently. | Pre-normalize the fixture/export or use a parser-enhancement request; do not rely on multiline fields. |
| UI loader reports no text | The element is absent/empty, locator mapping is wrong, or content is only in `value`. | Verify page state and YAML; use a value-reading UI mechanism for form controls. |

## XML reference

### Execution flow, query rules, and safety

A file load creates an `XmlFileHandler`, parses into a W3C `Document`, and stores it in `XmlContext`; a UI load parses raw UI text instead. `XmlCommonMethods` performs assertions and maintains extracted values for that step-definition instance. The XML `@After` hook clears the context and variable store after the scenario. [5] [6] [7]

A query beginning with `/` is treated as XPath. Any other query becomes `//<query>` and selects the first matching element for string-value lookup. Count steps accept XPath. Attribute assertions select the first matching node and return an empty string when the named attribute is absent; a missing matching node is an error. [5] [6] [7]

XML parsing uses a `DocumentBuilderFactory` configured to reject `DOCTYPE`, disable external general and parameter entities, and disable entity expansion. Therefore XML that relies on a DOCTYPE/external entity is deliberately rejected rather than resolved. [5]

### XML Gherkin bindings

```gherkin
@xml @xml_file @<TAG>
Feature: Validate an XML response artifact

  Scenario: Validate exact nodes and a collection
    Given I load XML file "src/test/resources/data/<RESPONSE_FILE>.xml"
    Then XML node "<UNIQUE_NODE>" exists
    Then XML node "<UNIQUE_NODE>" equals "<EXPECTED_VALUE>"
    Then XML node "<MESSAGE_NODE>" contains "<EXPECTED_SUBSTRING>"
    Then XML node "<ERROR_NODE>" does not exist
    Then XML XPath "<ITEM_XPATH>" count equals <EXPECTED_COUNT>
    Then XML node "<NODE_XPATH>" attribute "<ATTRIBUTE_NAME>" equals "<ATTRIBUTE_VALUE>"
    When I extract XML XPath "<VALUE_XPATH>" and store as "<XML_VARIABLE>"
    Then XML node "<CONFIRMATION_XPATH>" equals stored value "<XML_VARIABLE>"
```

```gherkin
@xml @xml_ui @<TAG>
Feature: Validate XML text displayed by an application

  Scenario: Parse visible XML text
    Given I load XML from UI element on page "<ELEMENT_GROUP>" locator "<XML_TEXT_KEY>"
    Then XML XPath "<STATUS_XPATH>" equals "<EXPECTED_STATUS>"
```

The visible-text step uses `gettext` and rejects blank content. `XmlSteps` explicitly notes that an input/textarea that exposes content only in its `value` attribute needs a value-reading UI step instead; the XML loader does not implement that fallback itself. [7] [21] [19]

### XML failures and diagnosis

| Symptom | Source-visible cause | Corrective action |
|---|---|---|
| `No XML document is loaded` | A query/assertion precedes loading. | Put a file or UI XML load step first in the same scenario. |
| Parse failure | File/string is blank, malformed, unreadable, or uses disallowed DOCTYPE/entity behavior. | Supply well-formed XML without external entity/DOCTYPE dependence. |
| Simple node returns the wrong value | A bare node name finds the first matching element anywhere. | Use an unambiguous XPath beginning with `/` or `//`. |
| `exists` says absent for a malformed XPath | `nodeExists` catches XPath evaluation errors and returns `false`. | Validate the XPath; use a value/count assertion when an invalid expression should fail loudly. |
| Attribute assertion differs from expectation | Only the first matching node is inspected; missing attributes return empty string. | Make the XPath specific and test the expected empty/nonempty outcome deliberately. |
| Stored variable missing | Values are only stored by the XML extraction steps and only for the scenario. | Extract before using and keep the dependent steps together. |

## ZIP extraction and TXT-to-CSV reference

### Archive execution flow

1. `I unzip file ...` reads `zip.extraction_dir` (or a provided root) and `zip.recursive_unzip`.
2. `ZipFileHandler` validates that the source exists and ends in `.zip`, creates `<target-root>/<zip-stem>/`, and streams archive entries into it.
3. Each extracted entry is checked against the canonical target path; an entry that would escape the target is skipped as a ZIP-slip safeguard.
4. The handler recursively scans the output and records files in a map keyed by lowercase extension; extensionless files use `noext`.
5. `ZipSteps` stores the result in `ZipContext`, making the discovered files available to later ZIP steps in the same scenario.
6. A TXT conversion may write an adjacent CSV; ZIP CSV/XML loader steps place a fresh handler into `CsvContext`/`XmlContext` for normal validation.
7. Explicit cleanup, or `ZipSteps`' `@After` hook when enabled, deletes the extracted scenario directory and clears ZIP context. [8] [9] [10] [11] [12]

Nested ZIP behavior has no depth, entry-count, compressed-size, or expanded-size limit in the current handler. Treat large/untrusted recursive archives as outside the helper's operational protections even though ZIP-slip traversal is checked. [8]

### ZIP Gherkin bindings and artifacts

```gherkin
@zip @<TAG>
Feature: Validate report archive content

  Scenario: Extract, convert, and validate
    Given I unzip file "<ARCHIVE_PATH>" to directory "test-output/<RUN_LABEL>/extracted"
    Then zip contains file "<REPORT_TEXT_FILE>.txt"
    Then zip contains a "xml" file
    When I convert txt file "<REPORT_TEXT_FILE>.txt" to CSV using delimiter "|"
    Given I load CSV from zip file "<REPORT_TEXT_FILE>.csv"
    Then CSV column "<CSV_HEADER>" exists
    Then CSV row 1 column "<CSV_HEADER>" equals "<EXPECTED_VALUE>"
    Given I load XML from zip file "<SUMMARY_FILE>.xml"
    Then XML XPath "<XPATH>" equals "<EXPECTED_XML_VALUE>"
    Then I cleanup extracted zip files
```

| Binding | Result/behavior |
|---|---|
| `Given I unzip file "<ZIP_PATH>"` | Extracts below configured `zip.extraction_dir/<zip-stem>/`. |
| `Given I unzip file "<ZIP_PATH>" to directory "<ROOT>"` | Extracts below `<ROOT>/<zip-stem>/`; recursive setting still comes from configuration. |
| `Then zip contains file "<NAME>"` | Case-insensitive filename lookup across the discovered map; duplicate names in different folders return the first discovered match. |
| `Then zip contains a "<EXTENSION>" file` | Tests lowercased extension after removing a leading period. |
| `When I convert txt file "<NAME>.txt" to CSV` | Uses pipe (`|`) by default. |
| `When I convert txt file "<NAME>.txt" to CSV using delimiter "<DELIMITER>"` | Replaces `\\t` with a tab, generates/re-registers `<NAME>.csv`. |
| `Given I load CSV from zip file "<NAME>.csv"` / `Given I load the first CSV file from zip` | Loads into `CsvContext`; standard CSV steps then apply. |
| `Given I load XML from zip file "<NAME>.xml"` / `Given I load the first XML file from zip` | Loads into `XmlContext`; standard XML steps then apply. |
| `Then I cleanup extracted zip files` | Deletes the current extraction directory immediately and clears ZIP context. |

The “first CSV/XML” variants simply return list element zero from discovery. Do not use them when the archive can contain more than one member of that extension, because discovery ordering is not an explicit test contract. [8] [10]

### TXT-to-CSV behavior

TXT conversion reads lines with `FileReader`, splits each line using the supplied delimiter as a regex-escaped separator, trims every field, joins fields with commas, and writes next to the input using the same basename and `.csv`. A field containing a comma, quote, or newline is double-quoted and embedded quotes are doubled. The converter preserves empty trailing fields (`split(..., -1)`). [8]

| Input condition | Output/result |
|---|---|
| `records.txt` using `|` | `records.csv` alongside the TXT file; use the no-delimiter Gherkin form or specify `"|"`. |
| Tab separator | Use `"\\t"` in Gherkin; `ZipSteps` converts it to an actual tab before conversion. |
| Existing same-basename CSV | The `FileWriter` overwrites it. Use distinct basenames if both artifacts must be retained. |
| Spaces around source values | Removed because each split field is trimmed. |
| Text delimiter inside a field | No CSV-style quote-aware input parser is used for TXT conversion; the delimiter still splits the line. |
| Generated CSV load | ZIP steps create a new normal comma-delimited `CsvFileHandler`, so column-header CSV assertions apply. |

### ZIP/TXT failures and diagnosis

| Symptom | Source-visible cause | Corrective action |
|---|---|---|
| `Cannot ... — no ZIP file has been extracted` | A discovery/conversion/archive-load step executed before unzip in this scenario. | Unzip first; ZIP context is scenario-thread local. |
| `ZIP file not found` / `File is not a ZIP archive` | Invalid project-relative/absolute path or source name lacks `.zip`. | Verify the path from the Maven working directory and archive extension. |
| Member not found | The name is absent after discovery or differs from expected content. | Use `zip contains file` first; review its available-file summary. |
| Wrong member selected | Duplicate case-insensitive filenames or use of a “first” step. | Give archive members unique names and load by explicit name. |
| Converted CSV absent/unexpected | TXT name is wrong, conversion failed, source delimiters are inconsistent, or same-basename CSV was overwritten. | Assert the TXT member first; select the actual delimiter; use unique names. |
| Leftover archive output | Cleanup disabled, skipped, or failed; cleanup catches/logs I/O errors instead of failing the scenario. | Use a disposable `test-output` root, preserve output only for diagnosis, and inspect logs. |
| Parallel collision | Same target root plus same ZIP stem. | Use a unique `to directory` root for every concurrent run/scenario. |

## Excel reader and Excel-to-YAML utility reference

### `ExcelReader`: exact lookup contract

`ExcelReader.getData(filePath, testCaseName, columnName)` opens a workbook via `WorkbookFactory`, always selects sheet index 0, treats row 0 as headers, and looks for a subsequent row whose first cell matches `testCaseName` case-insensitively after trimming. Header lookup uses the header text (trimmed) and is case-sensitive because `headerMap.containsKey(columnName)` is an exact lookup. If a target cell is found, it returns `Cell.toString()` without a `DataFormatter` transformation. [14]

| Input | Required contract | Return/error behavior |
|---|---|---|
| `filePath` | Direct path readable by `FileInputStream` | Any I/O or POI parsing failure is logged at `SEVERE`; method returns `null`. |
| Workbook sheet | Sheet index `0` only | No sheet-name or sheet-index argument exists. |
| Header row | First row | Header strings are trimmed before mapping. |
| Row key | First cell of each later row | Match is trimmed/case-insensitive. |
| Requested column | Exact header string supplied to `columnName` | Missing header logs a warning and returns `null`. |
| Target cell | Cell in the matched row | Missing target cell logs a warning and returns `null`. |
| Empty sheet / missing row | No usable value | Logs a formatted warning and returns `null`. |

The `excelDocumentLocation` configuration accessor is not an implicit data source: callers must pass the path. The reader does not provide Cucumber assertions, does not create a workbook, and does not expose a Gherkin “read Excel” step. [12] [14]

### Excel-backed performance request bodies

The current Gherkin bindings expose Excel cells only as the body of a protocol-performance POST or PUT request. They construct a request with `withExcelBody(excelFile, rowIdentifier, columnName)`, `application/json` content/accept types, then execute the performance engine. Resolver validation rejects blank file path, row identifier, or column before delegating to `ExcelReader`. [16] [17]

```gherkin
@performance_testing @performance_excel @<TAG>
Feature: Use a workbook-maintained request body

  Scenario: Resolve one first-sheet cell as a request payload
    When we run Excel-driven POST performance test for path "/<RESOURCE_PATH>" with name "<RUN_NAME>" using excel file "src/test/resources/<WORKBOOK>.xlsx" row "<ROW_KEY>" column "<BODY_COLUMN>"
```

This is browserless under the shared hook's performance classification, but it is **not** an Excel assertion feature. It turns the selected cell into the request body. The checked-in `performance_payload_driven.feature` contains CSV/Excel examples as commented lines, so it is a reference pattern rather than an active executable feature as currently committed. [17] [18] [16]

> **Source-visible discrepancy: `sheetName` is not honored by the current resolver path.** `PerformancePayloadDefinition` has an Excel factory overload that accepts `sheetName`, but `PerformancePayloadResolver.resolveExcel(...)` delegates to `ExcelReader.getData(filePath, rowIdentifier, columnName)`, whose reader always uses sheet 0. The existing Excel-driven Gherkin steps also have no sheet parameter. Do not assume a named sheet works until the resolver/reader contract changes. [16] [14]

### `ExcelToYaml`: programmatic conversion utility

`ExcelToYaml.convertExcelToYaml(testcaseId, excelFilePath, yamlFilePath)` uses `XSSFWorkbook`, so the implemented reader is specifically `.xlsx`-oriented. It reads sheet 0, uses row 0 for headers, and reads subsequent non-null rows into ordered maps. Blank/missing cells become `""`; whole numeric values become `Long`, other numeric values become `Double`, date-formatted cells become `Date.toString()`, and formula cells become their formula expression rather than a calculated value. [15]

If `testcaseId` is `null` or `ALL` (case-insensitive), it writes all rows as a YAML list. For a specific ID it looks for a `testcase_id` key whose value equals the supplied string, reorders that key first, and writes one YAML map. A nonmatching ID prints a message and creates no YAML output. The utility uses `FileWriter` and does not create parent directories; I/O errors print a stack trace. [15]

There is no Cucumber step, command-line main method, or active standard-runner invocation for this utility. Use it from approved Java code only, with a writable destination path; do not represent it as a supported feature-file command.

## Ordinary UI download/upload boundaries

### What is implemented

The normal UI action layer supports `uploadfile`/`selectfile` by calling Playwright `setInputFiles(Paths.get(value))`. Its strict `download` action waits for a browser download event, creates a sanitized feature-name subdirectory under the supplied value, and saves a feature-name/microsecond-timestamp artifact while preserving the downloaded file extension. `download_optional` returns `null` instead of throwing when no event is observed. [19] [22]

`PageCommonMethods.download(...)` delegates to the action layer but returns `void`. The ordinary Gherkin binding `And we click download on page <ELEMENT_GROUP> locator <DOWNLOAD_KEY>` reads `downloadDocument` and passes `filePath + ".jpeg"` to that method. Despite the step comment, `ActionPerformer` treats the value as a **directory root** and preserves the source download extension; consequently the current binding can create a `.jpeg`-named directory segment rather than a JPEG file. [20] [21] [19]

The ordinary upload binding `And we select document to upload on page <ELEMENT_GROUP> locator <UPLOAD_KEY>` currently passes a hard-coded filename to `selectFile`; it reads `downloadDocument` but does not use that local variable to compose the passed path. This is an implementation-specific legacy binding, not a safe parameterized upload interface. [20] [21] [19]

### Boundaries to keep explicit

- CSV/XML UI load steps can consume **visible text** after application navigation and an existing locator mapping, but they do not trigger a download. [4] [7] [21]
- ZIP steps accept a known local archive path; they do not consume a Playwright `Download` object or any UI-step variable. [10] [8]
- The ordinary download Gherkin step does not expose the dynamically saved path to a later Gherkin ZIP step because `PageCommonMethods.download` returns `void` and `PageCommonSteps` stores nothing. A dynamic “click download, then unzip the returned path” chain is therefore not currently available in standard Gherkin. [20] [21] [19]
- `ElementActionImpl.uploadFile(...)` has a different specialized API that waits for a file chooser and obtains its path from an element YAML key. That does not create a documented cross-module ZIP/CSV/XML handoff. [19]
- A file-only scenario should not use UI actions or UI text loaders; classification skips Playwright by design. Add `@csv_ui`/`@xml_ui` only when browser-backed content is genuinely required. [18]

## Generated artifacts and cleanup ownership

| Operation | Artifact/state | Typical location | Cleanup owner |
|---|---|---|---|
| Direct CSV/XML load | In-memory handler plus per-step-class variables | `CsvContext` / `XmlContext` only | CSV/XML `@After` methods clear context and variables. [3] [6] [4] [7] |
| ZIP extract | Archive member files and extension-index map | `test-output/extracted/<zip-stem>/` by default | `ZipSteps` explicit cleanup or `@After`, controlled by `zip.cleanup_after_scenario`. [8] [9] [10] |
| TXT conversion | `<txt-basename>.csv` beside extracted TXT | Inside the active extraction directory | Removed with ZIP extraction root when cleanup succeeds. [8] [10] |
| UI strict download | Preserved-extension artifact with sanitized feature name/timestamp | `<supplied-root>/<feature-name>/...` | No data-file cleanup; UI/download output management is separate. [19] [22] |
| Excel reader | Returned string or `null` | None | Workbook and input stream close by try-with-resources. [14] |
| Excel-to-YAML | YAML map/list document | Caller-provided `yamlFilePath` | Caller owns destination lifecycle. [15] |

## Source navigation

For a new contributor, trace operations in this order:

1. Start from the binding (`CsvSteps`, `XmlSteps`, or `ZipSteps`) to confirm exact Gherkin wording. [4] [7] [10]
2. Follow the binding to the high-level common method/context, then to the parser/handler for actual parse/query behavior. [2] [3] [5] [6] [8] [9]
3. For configuration, trace `ConfigurationProperties` into `YamlReader`, then inspect `config/config.yml`. [12] [13] [11]
4. For file-backed performance request bodies, follow `PerformanceSteps` → request builder/resolver → `CsvPayloadReader` or `ExcelReader`; do not conflate this reader with the scenario assertion context. [17] [16] [25] [14]
5. For browser-related work, trace `PageCommonSteps` → `PageCommonMethods` → `ActionPerformer`; then consult the feature-artifact resolver for output naming. [20] [21] [19] [22]
6. For why a browser did or did not start, inspect the shared hook's tag/path classifier before changing feature tags. [18]

## Related chapters

The following future local chapter filenames own adjacent concerns and should be read alongside this chapter without duplicating their scope:

- `01-foundation-configuration-and-execution.md` — Maven, runner, shared configuration, and lifecycle fundamentals.
- `02-ui-web-automation.md` — normal Playwright locator/action authoring, including UI upload/download behavior.
- `03-api-automation.md` — API request execution and API-only scenario conventions.
- `09-api-performance-testing.md` — protocol-performance profiles, request execution, and performance evidence beyond file-body lookup.
- `11-reporting-evidence-and-artifacts.md` — report formats and general evidence retention.

## Source references

- [1 — Maven dependencies, Java version, Surefire, and default suite](../../../pom.xml)
- [2 — CSV parser and file-loading implementation](../../../src/main/java/com/ptaf/csv/CsvFileHandler.java)
- [3 — CSV facade and `ThreadLocal` context](../../../src/main/java/com/ptaf/csv/CsvCommonMethods.java)
- [4 — CSV Cucumber step definitions and cleanup](../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java)
- [5 — XML parsing, XPath, and XXE safeguards](../../../src/main/java/com/ptaf/xml/XmlFileHandler.java)
- [6 — XML facade and `ThreadLocal` context](../../../src/main/java/com/ptaf/xml/XmlCommonMethods.java)
- [7 — XML Cucumber step definitions and cleanup](../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java)
- [8 — ZIP extraction, discovery, TXT conversion, and cleanup](../../../src/main/java/com/ptaf/zip/ZipFileHandler.java)
- [9 — ZIP scenario context](../../../src/main/java/com/ptaf/zip/ZipContext.java)
- [10 — ZIP Cucumber step definitions](../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java)
- [11 — Active shared and ZIP configuration](../../../src/test/resources/config/config.yml)
- [12 — Configuration accessors and defaults](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java)
- [13 — YAML discovery, merging, and dot-key lookup](../../../src/main/java/com/ptaf/utils/YamlReader.java)
- [14 — Excel first-sheet cell reader](../../../src/main/java/com/ptaf/utils/ExcelReader.java)
- [15 — Excel-to-YAML utility](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java)
- [16 — Performance payload definition and CSV/Excel resolution](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java)
- [17 — CSV/Excel-driven performance Gherkin bindings](../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java)
- [18 — Shared browserless classification and UI exceptions](../../../src/main/java/com/ptaf/hooks/Hooks.java)
- [19 — Playwright file action implementation](../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java)
- [20 — Ordinary UI download/upload Gherkin bindings](../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java)
- [21 — UI page delegation and text/download methods](../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java)
- [22 — Feature-based artifact naming](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java)
- [23 — Default TestNG/Cucumber runner options](../../../src/test/java/com/ptaf/runner/TestRunner.java)
- [24 — Default TestNG suite](../../../src/test/resources/testng.xml)
- [25 — Performance-specific CSV payload reader](../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java)

## References

[1]: ../../../pom.xml "Maven build, dependencies, Surefire, and profiles"
[2]: ../../../src/main/java/com/ptaf/csv/CsvFileHandler.java "CSV parser and query handler"
[3]: ../../../src/main/java/com/ptaf/csv/CsvCommonMethods.java "CSV common methods and scenario state"
[4]: ../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java "CSV Cucumber step definitions"
[5]: ../../../src/main/java/com/ptaf/xml/XmlFileHandler.java "XML parser, XPath evaluator, and secure factory"
[6]: ../../../src/main/java/com/ptaf/xml/XmlCommonMethods.java "XML common methods and scenario state"
[7]: ../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java "XML Cucumber step definitions"
[8]: ../../../src/main/java/com/ptaf/zip/ZipFileHandler.java "ZIP and TXT conversion handler"
[9]: ../../../src/main/java/com/ptaf/zip/ZipContext.java "ZIP thread-local extraction context"
[10]: ../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java "ZIP Cucumber step definitions"
[11]: ../../../src/test/resources/config/config.yml "Active framework configuration"
[12]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Configuration accessors and defaults"
[13]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "Merged YAML resource reader"
[14]: ../../../src/main/java/com/ptaf/utils/ExcelReader.java "Excel first-sheet cell reader"
[15]: ../../../src/main/java/com/ptaf/utils/ExcelToYaml.java "Excel-to-YAML converter"
[16]: ../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java "Performance CSV and Excel payload resolution"
[17]: ../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java "Performance CSV and Excel Cucumber steps"
[18]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Shared Cucumber browser lifecycle and browserless classification"
[19]: ../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "Playwright file upload and download actions"
[20]: ../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java "Ordinary page download and upload bindings"
[21]: ../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java "Page-level UI action delegation"
[22]: ../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-based artifact directory and filename utility"
[23]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[24]: ../../../src/test/resources/testng.xml "Default TestNG suite"
[25]: ../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java "Performance CSV payload reader"
