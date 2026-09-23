# FNB-ETAF PDF Automation and Validation Guide

## Purpose and scope

This guide describes the **FNB-ETAF PDF validation capability** implemented in this repository. It covers selecting or downloading a PDF, retaining the current document path, validating extracted text and structure, optionally applying optical character recognition (OCR), comparing a rendered page with a PNG baseline, checking document metadata and AcroForm fields, and locating the resulting test artifacts and reports.

The PDF package validates files that already exist on the local filesystem. It does not generate business PDFs, provide a PDF-specific YAML configuration section, or persist document state across test threads. The supplied feature is a browserless, local-file example that selects the newest `.pdf` in the repository-level `downloads/` directory. The framework also provides a UI download step, but it has a different lifecycle requirement described in [Creating a test scenario](#creating-a-test-scenario). [1] [2]

## Architecture and source locations

| Concern | Implementation and package | Repository location | Operational behavior |
|---|---|---|---|
| Cucumber PDF step contract | `com.ptaf.stepdefinitions.PdfSteps` | [src/test/java/com/ptaf/stepdefinitions/PdfSteps.java][1] | Binds Gherkin `When` and `Then` steps to download, selection, text, page, OCR, visual, metadata, form-field, and diagnostic operations. |
| Current-document state | `com.ptaf.pdf.PdfStore` | [src/main/java/com/ptaf/pdf/PdfStore.java][2] | Holds `lastPdfPath` in a `ThreadLocal`; it can select the newest matching file in a directory and fail early if no retained file exists. |
| PDF text, structure, and rendering | `com.ptaf.pdf.PdfUtils` | [src/main/java/com/ptaf/pdf/PdfUtils.java][3] | Uses PDFBox to read normalized text, count pages, test the `%PDF-` header, and render a one-based page number to PNG. |
| Assertions | `com.ptaf.pdf.PdfValidator` | [src/main/java/com/ptaf/pdf/PdfValidator.java][4] | Provides JUnit assertions over extraction, structure, OCR, visual comparison, metadata, and AcroForm values. |
| Metadata and forms | `com.ptaf.pdf.PdfMeta` | [src/main/java/com/ptaf/pdf/PdfMeta.java][5] | Reads standard document-information fields and top-level AcroForm field values into maps. |
| OCR | `com.ptaf.pdf.PdfOcr` | [src/main/java/com/ptaf/pdf/PdfOcr.java][6] | Renders a page and calls the system `tesseract` executable. |
| Visual comparison | `com.ptaf.pdf.PdfRenderDiff` | [src/main/java/com/ptaf/pdf/PdfRenderDiff.java][7] | Compares actual and expected images by RGBA channel tolerance and different-pixel ratio; it can write a red-overlay diff PNG. |
| PDF feature and input | Cucumber feature and local test PDF | [src/test/resources/features/pdf/PdfValidation.feature][8]; [downloads/sample_invoice.pdf][9] | The checked-in example selects the newest PDF in `downloads/` and performs text, page, page-count, and regex assertions. |
| Baseline images | PNG resources | [src/test/resources/baselines/][10] | Contains the PDF visual-comparison baseline area, including the checked-in invoice baseline. |
| UI-triggered download | `com.ptaf.ui.action_performer.ActionPerformer`, `com.ptaf.utils.FeatureArtifactNameResolver` | [ActionPerformer.java][11]; [FeatureArtifactNameResolver.java][12] | The strict `download` action saves a Playwright download beneath a feature-named subdirectory and returns its path to `PdfSteps`. |
| Lifecycle and non-UI classification | `com.ptaf.hooks.Hooks` | [src/main/java/com/ptaf/hooks/Hooks.java][13] | Treats `@pdf` scenarios and features located under `features/pdf/` as browserless; non-UI teardown does not create or close a Playwright browser. |
| Default Maven entry point | `com.ptaf.runner.TestRunner` and TestNG suite | [src/test/java/com/ptaf/runner/TestRunner.java][14]; [src/test/resources/testng.xml][15] | Maven Surefire uses the TestNG Cucumber runner in the configured suite. The runner currently declares `@eStore`; a PDF tag filter must therefore be supplied for the checked-in feature. |

## Prerequisites

Use **JDK 21** and Maven. The project compiler source and target levels are both 21, and the project declares Apache PDFBox `2.0.30` for PDF operations. [16]

A normal text-only PDF run does not require Playwright browsers. The `@pdf` tag causes `Hooks` to skip Playwright browser setup, so the checked-in local-file feature can run without a browser session. A scenario that uses the `I download PDF ...` step is different: it needs a live Playwright `Page`, an application download control, and the normal UI browser prerequisites. Do not label that UI-download scenario `@pdf`, because the hook will deliberately omit the page that the download step calls through `Hooks.getPage()`. [1] [13]

OCR is optional. When an OCR assertion is used, the host must make the `tesseract` command available on `PATH` and must have the requested language data installed. For non-standard language-data locations, the implementation notes support for `TESSDATA_PREFIX`. OCR may be explicitly disabled with `-Dpdf.ocr.enabled=false`. [4] [6]

The directory used to select, save, render, or diff files must be writable. A test that selects an existing document also needs an unambiguous input directory: the selection operation chooses the most recently modified matching file, not a filename supplied in Gherkin. [2]

## Configuration files and key settings

There is **no `pdf:` YAML section**. PDF assertions accept their operational values in Gherkin and use the retained `PdfStore` path. The following shared settings and files affect PDF executions or their reporting.

| File | Relevant setting or contract | Effect on PDF automation |
|---|---|---|
| [pom.xml][16] | Java 21; `org.apache.pdfbox:pdfbox:2.0.30`; Surefire TestNG suite; `testFailureIgnore=true` | Supplies the PDF parser/rendering library and runs the configured TestNG suite. A Maven process can complete successfully even when a test fails, so CI must inspect test reports rather than relying only on the process exit code. |
| [src/test/resources/testng.xml][15] | Class `com.ptaf.runner.TestRunner` | Selects the TestNG Cucumber runner used by Maven's default suite. |
| [src/test/java/com/ptaf/runner/TestRunner.java][14] | Feature root, glue packages, runner plugins, annotation tag `@eStore` | Discovers `src/test/resources/features` with `com.ptaf.stepdefinitions` and `com.ptaf.hooks`. Use the Cucumber tag system property below to select PDF scenarios instead of changing this runner. |
| [src/test/resources/config/config.yml][17] | `downloadDocument`; `reporting.*`; browser and video settings | `downloadDocument` is a general configured path, but `PdfSteps` does not read it automatically. The per-feature reporting switches control additional report artifacts for every feature type, including PDF features. Do not place secrets in this file or in a feature. |
| [src/test/resources/extent.properties][18] | Timestamped Extent base folder and Spark, Base64, PDF, Excel reporter outputs | Controls the combined Extent report tree under `test-output/`. |
| [src/test/resources/cucumber.properties][19] | `cucumber.publish.enabled = false` | Disables Cucumber publishing. |
| JVM system property | `pdf.ocr.enabled=false` | Disables OCR assertions; those assertions are skipped through JUnit assumptions rather than treated as failed. [4] |

> `downloadDocument` is not a PDF input selector. The local sample feature calls `When I set last PDF from directory "downloads"`, and the UI download step receives its destination directly in the step text. [1] [8] [17]

## Build and exact run commands

Run the following commands from the repository root, `/home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23`.

```bash
# Resolve dependencies and compile production and test sources without executing tests.
mvn clean test-compile -DskipTests

# Execute all scenarios selected by the @pdf tag, including the checked-in PDF feature.
mvn clean test -Dcucumber.filter.tags="@pdf"

# Execute the checked-in text-only PDF feature while disabling optional OCR globally.
mvn clean test -Dcucumber.filter.tags="@pdf" -Dpdf.ocr.enabled=false
```

The PDF feature currently has `@pdf`, `@text-only`, and `@green` tags. The default Maven runner annotation is `@eStore`, so calling `mvn test` without a Cucumber filter does not select that feature. The `cucumber.filter.tags` property is the run-time selection mechanism for the command shown above; it avoids modifying the runner annotation. [8] [14] [15]

For a new browser-backed download scenario tagged `<UI_DOWNLOAD_TAG>`, use a tag expression that selects that tag and **does not include `@pdf`**:

```bash
mvn clean test -Dcucumber.filter.tags="@<UI_DOWNLOAD_TAG>"
```

The default Surefire configuration writes additional Cucumber JSON and JUnit XML outputs under `target/cucumber-reports/` and places Surefire results under `target/surefire-reports/`. [16]

## Input, feature, locator, payload, and query locations

| Asset type | Location | PDF usage |
|---|---|---|
| PDF feature files | `src/test/resources/features/pdf/` | Store browserless/local PDF validation scenarios here. The existing feature is `PdfValidation.feature`. [8] |
| Local PDF inputs | `downloads/` | The supplied example reads the newest `*.pdf` directly beneath this directory. The sample is `downloads/sample_invoice.pdf`. [8] [9] |
| UI download output | A path supplied by `saving to "<DOWNLOAD_OUTPUT_ROOT>"` | The strict Playwright download action creates a sanitized feature-name subdirectory and a timestamped filename preserving the download extension. [1] [11] [12] |
| PDF visual baselines | `src/test/resources/baselines/` | Store reviewed expected PNGs here, or use another repository-approved baseline location supplied in the visual assertion. [10] |
| UI locators, only when downloading through UI | `src/test/resources/elements/*.yml` | `ElementActionImpl` resolves `elements.<element>.<key>` through `YamlReader`; add an appropriate locator entry before using the download step. [20] [21] |
| API request definitions | `src/test/resources/api_requests/` | Not consumed by `PdfSteps`. Use only if an API scenario independently retrieves or prepares a document. [21] |
| Database query definitions | `src/test/resources/queries/` | Not consumed by `PdfSteps`. Use only for separate database setup or verification steps. [21] |
| General performance payloads | `src/test/resources/performance/payloads/` | Not consumed by PDF validation. [21] |

`YamlReader` loads YAML only from `elements`, `queries`, `api_requests`, `config`, and `performance` classpath folders. A PDF-specific locator file belongs under `src/test/resources/elements/`; a new arbitrary resource folder is not automatically loaded by that class. [21]

## Creating a test scenario

### 1. Choose the document acquisition mode

Use **local-file validation** when a test PDF is already present in a controlled directory. Tag the feature or scenario `@pdf`; this is the mode used by the existing example. The selection step stores the absolute path of the newest regular file whose filename ends in lowercase `.pdf`.

Use **UI-triggered download validation** when the document must be obtained from a browser interaction. Give the scenario a UI-specific tag such as `@<UI_DOWNLOAD_TAG>`, but do not apply `@pdf` and do not place it under `features/pdf/`. Add the target element under `src/test/resources/elements/`, because the download step delegates locator lookup and the strict `download` action to the normal UI action layer. [1] [2] [11] [13] [20] [21]

### 2. Add locator data only for UI downloads

The locator resource uses the existing `elements` root and locator notation. Substitute only approved selectors; this is a structural example.

```yaml
# src/test/resources/elements/<document-elements-file>.yml
elements:
  <document_export_element>:
    <download_control_key>: "CSS_<APPROVED_DOWNLOAD_BUTTON_SELECTOR>"
```

The `PdfSteps` pattern accepts the element name and key separately:

```gherkin
When I download PDF from "<document_export_element>"."<download_control_key>" saving to "<DOWNLOAD_OUTPUT_ROOT>"
```

The strict action waits for a Playwright download event, saves the file, returns its saved path, and `PdfSteps` retains that path for later assertions. If that action returns no path, the step fails immediately. [1] [11]

### 3. Add a browserless local-file feature

Create a feature under `src/test/resources/features/pdf/`. This tested-style example uses placeholders only and follows the existing step definitions.

```gherkin
@pdf @text-only @<PDF_SUITE_TAG>
Feature: Validate a locally available PDF

  Background:
    When I set last PDF from directory "<PDF_INPUT_DIRECTORY>"
    Then the last PDF should exist
    And the last PDF should be a valid PDF
    And the last PDF should have <EXPECTED_PAGE_COUNT> pages

  Scenario: Validate required text and a page-specific value
    Then the last PDF should contain all:
      | <DOCUMENT_HEADING> |
      | <REQUIRED_VALUE>   |
    And page <PAGE_NUMBER> of the last PDF should contain "<PAGE_EXPECTED_TEXT>"
    And the last PDF should match regex "<DOCUMENT_REGEX>"
```

The existence check verifies only that the retained filesystem path exists. The valid-PDF assertion is a header heuristic that checks whether the file begins with `%PDF-`; it is not a complete integrity or conformance validation. Page numbers are one-based in extraction, OCR, and rendering operations. [2] [3] [4]

### 4. Add a UI-download feature when required

Keep this feature outside `src/test/resources/features/pdf/` and omit `@pdf`, so `Hooks` initializes Playwright. The download result becomes the current PDF for the rest of the scenario.

```gherkin
@<UI_DOWNLOAD_TAG>
Feature: Download and validate a PDF from the application

  Scenario: A document export contains the required content
    When I download PDF from "<document_export_element>"."<download_control_key>" saving to "<DOWNLOAD_OUTPUT_ROOT>"
    Then the last PDF should exist
    And the last PDF should be a valid PDF
    And the last PDF should contain "<REQUIRED_DOCUMENT_TEXT>"
    And page <PAGE_NUMBER> of the last PDF should match regex "<PAGE_REGEX>"
```

The shared `PdfStore` is thread-local. It avoids passing a path through every step and keeps parallel test threads separate, but the class exposes no automatic scenario-end clear operation. Avoid reusing a long-lived custom worker thread without clearing its PDF state in custom code. [2]

## Supported assertions and examples

### Text, page, and structure validation

The following steps are implemented in `PdfSteps` and use `PdfValidator`. Text extraction normalizes non-breaking spaces, whitespace runs, and selected zero-width characters before comparison. Regex matching is applied to normalized extracted text. [1] [3] [4]

```gherkin
Then the last PDF should contain "<REQUIRED_TEXT>"
And the last PDF should not contain "<FORBIDDEN_TEXT>"
And the last PDF should contain all:
  | <FIRST_REQUIRED_TEXT>  |
  | <SECOND_REQUIRED_TEXT> |
And the last PDF should match regex "<DOCUMENT_REGEX>"
And page <PAGE_NUMBER> of the last PDF should contain "<PAGE_TEXT>"
And page <PAGE_NUMBER> of the last PDF should match regex "<PAGE_REGEX>"
And the last PDF should have <EXPECTED_PAGE_COUNT> pages
Then I print first <CHARACTER_LIMIT> chars of last PDF
```

`I print first ... chars` writes the requested prefix to standard output and fails if extracted text is empty. It is a diagnostic aid, not an artifact writer. [1]

### Metadata and AcroForm fields

The metadata step checks a substring in one of the standard document-information values exposed as `Title`, `Author`, `Subject`, `Keywords`, `Creator`, `Producer`, `CreationDate`, `ModificationDate`, or `Trapped`. The form step compares a fully qualified top-level AcroForm field value after normalization. A missing key or field reads as an empty string and consequently fails unless the expected value is also empty. [4] [5]

```gherkin
Then the last PDF metadata "<METADATA_KEY>" should contain "<EXPECTED_METADATA_FRAGMENT>"
And the last PDF form field "<FORM_FIELD_NAME>" should equal "<EXPECTED_FIELD_VALUE>"
```

### Optional OCR validation

Use OCR for image-only or scanned pages when ordinary PDF text extraction is insufficient. The assertion renders the requested page at the supplied DPI, invokes Tesseract with the supplied language code, then compares normalized OCR output. The default language is `eng` if a direct library caller passes a blank value; pass a non-empty language code in Gherkin for clarity. [4] [6]

```gherkin
Then OCR on page <PAGE_NUMBER> of the last PDF with dpi <OCR_DPI> and lang "<TESSERACT_LANGUAGE>" should contain "<OCR_EXPECTED_TEXT>"
```

If OCR is disabled with `-Dpdf.ocr.enabled=false` or Tesseract cannot be invoked from `PATH`, the assertion is skipped rather than failed. A non-zero Tesseract exit after availability is established fails the operation. [4] [6]

### Visual comparison

Use visual comparison for layout-sensitive pages. Supply a reviewed expected PNG, a per-channel RGBA tolerance, the maximum acceptable fraction of differing pixels, and a writable diff destination.

```gherkin
Then page <PAGE_NUMBER> of the last PDF rendered at <RENDER_DPI> dpi should visually equal "<EXPECTED_PNG_PATH>" with tolerance <CHANNEL_TOLERANCE> and max diff <MAX_DIFFERING_PIXEL_RATIO>, diff out "<DIFF_PNG_PATH>"
```

The operation writes the rendered actual image beside the expected PNG by replacing `.png` with `.actual.png`. On a mismatch it writes the requested red-overlay diff PNG only when both image dimensions match and `diff out` is non-blank. An image-size mismatch returns a difference ratio of `1.0` and does not create a diff image. [4] [7]

## Expected reports and artifacts

The reports below are configured by the repository. PDF **test documents**, expected PNGs, rendered actuals, and pixel diffs are separate from the **PDF reports** produced by Extent and per-feature reporting.

| Output | Expected location or naming | Notes |
|---|---|---|
| Cucumber pretty HTML, JSON, and rerun list | `target/cucumber-reports/cucumber-pretty`, `target/cucumber-reports/CucumberTestReport.json`, `target/cucumber-reports/rerun.txt` | Declared by the default TestNG runner. [14] |
| Maven-configured Cucumber JSON and JUnit XML | `target/cucumber-reports/cucumber.json`; `target/cucumber-reports/cucumber.xml` | Set in the Surefire system properties. [16] |
| Surefire results | `target/surefire-reports/` | TestNG/Surefire execution output. [16] |
| Combined Extent reports | `test-output/<run-timestamp>/SparkReport/Spark.html`, `Base64Report/Report.html`, `PdfReport/FNB-PTAF-Report.pdf`, and `ExcelReport/FNB-PTAF-Report.xlsx` | Enabled in `extent.properties`; the timestamped base folder prevents normal runs from overwriting each other. [18] |
| Per-feature HTML | `test-output/per-feature-reports/<feature-name>_<timestamp>.html` | Enabled by the current `reporting.per_feature_reports_enabled` setting. [17] [22] |
| Per-feature Glass-style PDF | `test-output/per-feature-reports-glass/<feature-name>_<timestamp>.pdf` | Enabled by the current Glass PDF setting. The direct per-feature PDF switch is currently false. [17] [22] |
| UI-downloaded PDF | `<DOWNLOAD_OUTPUT_ROOT>/<sanitized-feature-name>/<sanitized-feature-name>_<timestamp>.pdf` | Written only by the strict UI download action. The source filename extension is preserved; the feature title replaces the original stem. [11] [12] |
| Rendered actual PNG | `<EXPECTED_PNG_PATH with .png replaced by .actual.png>` | Written by a visual assertion before comparison, including when the comparison passes. [4] |
| Visual diff PNG | `<DIFF_PNG_PATH>` | Written only for a same-dimension mismatch when a non-blank destination is supplied. [7] |
| OCR temporary PNG | JVM temporary directory | Created by `File.createTempFile`; the current OCR implementation does not explicitly delete it. [6] |

## Troubleshooting

| Symptom | Likely cause supported by the code | Resolution |
|---|---|---|
| `No lastPdfPath set. Download or set from directory first.` | A validation step ran before either the directory-selection step or a successful strict UI download. | Put one of those acquisition steps in the Background or before the first assertion. [1] [2] |
| `Not a directory` or `No files found in` | The selection path is not a directory, or it contains no regular files ending in lowercase `.pdf`. | Correct the directory path and ensure a PDF is directly in it. The suffix comparison is case-sensitive. [2] |
| The wrong local PDF is selected | More than one matching file is present; selection uses latest modification time. Equal modification times have no deterministic tie-breaking guarantee. | Isolate each scenario's input directory or ensure the intended file is uniquely newest before the run. [2] |
| UI download fails because the page is closed or uninitialized | The feature is tagged `@pdf` or sits under `features/pdf/`, so `Hooks` classifies it as browserless; the download step needs `Hooks.getPage()`. | Move the UI-download scenario outside the PDF feature directory and use a UI tag without `@pdf`. [1] [13] |
| UI download produces no file path | Locator resolution, click readiness, or the expected Playwright download event failed. `ElementActionImpl` returns `null` on an action error, then `PdfSteps` fails fast. | Confirm the element/key exists under `elements.*`, the control produces a download, and the output root is writable. [1] [11] [20] [21] |
| `Not a PDF` for a readable-looking file | The built-in validity check only looks for the `%PDF-` file header. | Confirm the download is a real PDF rather than an HTML/error payload, and use text/page assertions for deeper functional checks. [3] [4] |
| Text assertion misses visible text | The PDF may be image-only, text order may differ from its visual layout, or the target string differs after normalization. | Use page-level assertions for better isolation; use OCR for scanned content; use visual comparison for layout checks. [3] [4] [6] |
| OCR assertion is skipped | OCR is disabled or `tesseract -v` cannot return exit code zero. | Remove `-Dpdf.ocr.enabled=false`, install Tesseract on `PATH`, and install the requested language data. [4] [6] |
| OCR fails after starting | Rendering, writable-temp-directory access, language availability, or Tesseract execution failed. | Check the failure cause, Tesseract language support, `TESSDATA_PREFIX` where needed, and available temporary disk space. [6] |
| Visual comparison fails unexpectedly | Baseline dimensions or rendering differ, or configured tolerance/ratio is too strict for the rendering environment. | Inspect the `.actual.png` and requested diff image; confirm the expected baseline, page number, DPI, tolerance, and allowed ratio. [4] [7] |
| No diff PNG exists for a visual failure | The images differed in dimensions or the supplied diff destination was blank. | Use same-dimension expected images and supply a non-empty, writable `diff out` path. [7] |
| Maven exits successfully despite a scenario failure | The Surefire configuration has `testFailureIgnore=true`. | Treat the Cucumber, Surefire, and Extent reports as the result authority, or apply the repository's CI policy outside this guide. [16] |

## Module boundaries

The PDF package is intentionally narrow. It is a file-validation layer, not a replacement for UI, API, database, ZIP, mobile, or performance modules.

| Related module | Boundary with PDF validation |
|---|---|
| UI / Playwright | The only direct integration is the optional PDF download step: `PdfSteps` gets the current `Page`, then delegates to `ElementActionImpl` and the `download` action. PDF assertions themselves operate on a saved local path. [1] [11] |
| Lifecycle hooks | `Hooks` recognizes `@pdf` and `features/pdf/` as non-UI, avoiding browser initialization. This is correct for local file validation but incompatible with the UI-download step in the same browserless scenario. [13] |
| Locator resources | Locator YAML is needed only for UI download. It is not used when a local path is selected with `I set last PDF from directory ...`. [1] [20] [21] |
| API, database, and performance data | `PdfSteps` does not read API request YAML, SQL query YAML, or performance payloads. Those modules may prepare data independently, but no PDF step consumes their values directly. [1] [21] |
| Reporting | Cucumber, Surefire, Extent, and per-feature report generation observe the PDF scenario like other Cucumber scenarios. Their generated PDF reports are test evidence, not the PDF under test. [14] [16] [17] [18] [22] |
| Password-protected PDFs | The Java utility classes expose password overloads for text, pages, rendering, metadata, form fields, and OCR. The supplied Gherkin steps do not accept a password, so the documented Cucumber flow covers documents that can be opened without one. [1] [3] [5] [6] |

## Implementation limitations and operational cautions

The following points are explicit in the current code and should guide test design.

- `assertIsPdf` is a lightweight header check. It is not a full parser-based validity, signature, security, accessibility, or standards-conformance check. [3] [4]
- Text extraction uses PDFBox `PDFTextStripper`. Reading order can vary with document structure, and normalization deliberately collapses whitespace; assertions should target stable content rather than visual spacing. [3]
- `PdfMeta.formFields` iterates the fields returned by `PDAcroForm.getFields()`. Design form assertions against the fully qualified field names exposed there; the step does not fill form fields or modify documents. [5]
- Visual diff compares every pixel in memory and validates channels against the supplied tolerance. It does not validate that tolerance is within `0..255` or that the max ratio is within `0.0..1.0`; use sensible values in features. [7]
- Visual comparison writes the actual image by string replacement of `.png`. Supply an expected filename ending in lowercase `.png` to obtain the intended `.actual.png` name. [4]
- OCR launches an external process for every asserted page, can be CPU and I/O intensive, and leaves its temporary rendered PNG in the JVM temp directory unless an external cleanup process removes it. [6]
- The checked-in local selection feature can be affected by stale PDFs in `downloads/`. The directory is not automatically cleaned by `PdfStore` or `PdfSteps`. [2] [8]
- Parallel test execution keeps the retained PDF path thread-scoped, but shared directories, expected/actual image names, and manually chosen diff destinations still need unique scenario-safe paths. [2] [7] [15]

## References

[1]: ../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java "PdfSteps Cucumber bindings"
[2]: ../../src/main/java/com/ptaf/pdf/PdfStore.java "PdfStore thread-local document path management"
[3]: ../../src/main/java/com/ptaf/pdf/PdfUtils.java "PdfUtils PDFBox extraction, rendering, and normalization"
[4]: ../../src/main/java/com/ptaf/pdf/PdfValidator.java "PdfValidator assertion and OCR control methods"
[5]: ../../src/main/java/com/ptaf/pdf/PdfMeta.java "PdfMeta document-information and AcroForm extraction"
[6]: ../../src/main/java/com/ptaf/pdf/PdfOcr.java "PdfOcr Tesseract integration"
[7]: ../../src/main/java/com/ptaf/pdf/PdfRenderDiff.java "PdfRenderDiff image comparison implementation"
[8]: ../../src/test/resources/features/pdf/PdfValidation.feature "PDF validation feature example"
[9]: ../../downloads/sample_invoice.pdf "Sample local PDF input"
[10]: ../../src/test/resources/baselines/ "PDF visual comparison baselines"
[11]: ../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "Playwright strict download action"
[12]: ../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-based artifact naming"
[13]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Cucumber lifecycle and non-UI PDF classification"
[14]: ../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[15]: ../../src/test/resources/testng.xml "Default TestNG suite"
[16]: ../../pom.xml "Maven dependencies, compiler level, and Surefire configuration"
[17]: ../../src/test/resources/config/config.yml "Global framework and per-feature reporting configuration"
[18]: ../../src/test/resources/extent.properties "Timestamped Extent reporter configuration"
[19]: ../../src/test/resources/cucumber.properties "Cucumber publication configuration"
[20]: ../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java "Element locator resolution and returned action execution"
[21]: ../../src/main/java/com/ptaf/utils/YamlReader.java "Classpath YAML folder discovery and lookup"
[22]: ../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature HTML and PDF report generation"
