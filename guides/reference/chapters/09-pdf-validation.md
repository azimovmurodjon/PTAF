# PDF Automation and Validation Reference

This chapter describes the **current PDF capability implemented in `com.ptaf.pdf`** and the Cucumber glue in `PdfSteps`. It covers filesystem-based document selection, Playwright-backed download acquisition, text/structure/metadata/form/OCR/visual assertions, browserless execution, and report evidence. It does not describe a generic PDF testing strategy beyond what the repository implements. Apache PDFBox 2.0.30 is the document-processing dependency declared by the build. [1]

## 1. Module map and responsibilities

| Source area | Responsibility | Important execution detail |
|---|---|---|
| `com.ptaf.pdf.PdfUtils` | Opens PDFs with PDFBox; extracts normalized text, counts pages, checks the `%PDF-` header, and renders one page to PNG. | Public page numbers are **1-based**. Password overloads exist for text, page count, and rendering. [2] |
| `com.ptaf.pdf.PdfValidator` | Provides JUnit assertion wrappers for document/page text, regex, page count, header check, OCR, visual comparison, metadata, and forms. | Assertion failures are surfaced to the test runner; text failures include a maximum 1,200-character actual-text preview. [3] |
| `com.ptaf.pdf.PdfMeta` | Reads selected PDF Info-dictionary values and AcroForm field values into ordered maps. | It is read-only and uses PDFBox documents in try-with-resources blocks. [4] |
| `com.ptaf.pdf.PdfOcr` | Rasterizes one page and invokes the external `tesseract` command. | OCR is optional in validator assertions; it is neither bundled nor configured through YAML. [3][5] |
| `com.ptaf.pdf.PdfRenderDiff` | Compares a rendered image and a PNG baseline pixel-by-pixel. | It can write a transparent/red mismatch overlay when dimensions agree and the permitted ratio is exceeded. [6] |
| `com.ptaf.pdf.PdfStore` | Keeps the current PDF path in a thread-local map and can choose the newest matching file in a directory. | It tracks a path only; it does not download, parse, validate, or delete a PDF. [7] |
| `com.ptaf.stepdefinitions.PdfSteps` | Maps implemented Gherkin phrases to the module. | Most assertions call `PdfStore.ensureExists()` first. [8] |

## 2. Execution model and acquisition flow

### 2.1 Filesystem-first flow

The normal non-UI PDF feature flow is:

1. A feature selects a PDF explicitly by scanning a directory with `When I set last PDF from directory "…"`.
2. `PdfStore.setLastFromDirectory(directory, ".pdf")` checks that the path is a directory, lists its **direct** children, keeps regular files ending in the case-sensitive suffix `.pdf`, chooses the greatest `lastModified` value, converts it to an absolute path, and stores it in the current thread. [7][8]
3. Each validation step confirms that a path has been stored and that it still exists. It then delegates to the appropriate `PdfValidator` operation. [7][8]
4. The validator uses PDFBox for document operations, Java ImageIO/AWT for visual comparison, and—only for OCR—the system `tesseract` executable. [2][3][5][6]

The repository’s PDF feature follows this model. Its background selects the newest `.pdf` in `downloads`, checks existence and the header, then asserts a page count before exercising text scenarios. [9]

> **Selection rule:** “newest” is filesystem last-modified time, not creation time, download start time, or filename. If candidates have identical timestamps, the implementation documents that the selected file is arbitrary according to stream ordering. Files whose attributes cannot be read receive timestamp `0` and are deprioritized. [7]

### 2.2 UI download flow

`PdfSteps` also provides a browser-backed acquisition step:

```gherkin
When I download PDF from "<download-control>"."<locator-key>" saving to "<download-root>"
```

It gets the current Playwright `Page`, resolves the configured element through `ElementActionImpl`, invokes its `download` action, and stores the returned path as the last PDF. A null return is treated as an error. [8]

The underlying strict `download` action waits for a Playwright download event while clicking the target element. It creates a feature-title directory below the supplied root, saves the download there, preserves only the original extension, and returns the saved path. The filename is derived from the declared `Feature:` title and a microsecond timestamp, rather than retaining the server-suggested stem. [11][12]

For example, with a feature title such as `Download statement`, the artifact will be placed conceptually under:

```text
<download-root>/Download_statement/Download_statement_<timestamp>.pdf
```

The precise timestamp is intentionally nondeterministic. The resolver sanitizes non-alphanumeric title characters to underscores, limits the directory/file title component to 80 characters, and falls back to the feature filename stem or `unnamed_feature` if the source cannot be read. [12]

There is an `ActionPerformer` action named `download_optional`, but **there is no corresponding `PdfSteps` Gherkin expression**. That action returns `null` rather than failing if no download event is received within its action timeout. Do not assume the PDF download step is optional: it uses the strict `download` action. [8][11]

### 2.3 Input and baseline paths

| Artifact | Path supplied by | What the code does |
|---|---|---|
| PDF under test | `PdfStore` last path, set by the download step or directory-selection step | Requires it to exist before each exposed validation step. [7][8] |
| Directory input | Literal Gherkin string | Scans only immediate regular files; PDF filter is `.pdf`, case-sensitive. [7][8] |
| Download root | Literal Gherkin string passed to the download step | Creates a feature-specific subdirectory and writes a feature-titled artifact there. [8][11][12] |
| Expected visual baseline | Literal `expectedPng` in the visual step | Must be an image readable by ImageIO; routine usage should provide a PNG because the actual path is derived by replacing `.png`. [3][6][8] |
| Rendered actual | Derived from `expectedPngPath.replace(".png", ".actual.png")` | Written on every visual comparison attempt; parent directories are created. [2][3] |
| Difference image | Literal `diffOutPath` in the visual step | Written only for a same-dimension mismatch that exceeds `maxDiffRatio`; parent directories are created. [3][6] |
| OCR staging image | JVM temporary file (`ocr_page_<page>_*.png`) | Created by `File.createTempFile`; current source does **not** explicitly delete it. [5] |

The current general configuration also contains `downloadDocument: "src/test/downloads/"`, but `PdfSteps` does not read that key; both implemented PDF acquisition steps receive their paths from Gherkin. Treat `downloadDocument` as a general configuration value, not as a default for these steps. [8][13]

## 3. Browserless behavior and its boundary

The lifecycle hook class classifies a scenario tagged `@pdf`, or located below `features/pdf/`, as a non-UI file/database scenario. It marks the scenario browserless and returns before Playwright browser, context, and page creation. Browserless teardown clears lifecycle state without attempting browser cleanup. [10]

This makes directory-driven PDF validation suitable for a worker with no browser installation or display. The repository’s `PdfValidation.feature` is tagged `@pdf` and uses directory selection rather than a UI download. [9][10]

**Important boundary:** the `I download PDF from …` step is not usable inside that browserless PDF scenario. It calls `Hooks.getPage()`, and that accessor throws when no page was initialized. The download step is therefore appropriate only for a browser-backed feature that is not classified as `@pdf`; after the path has been stored, the same validation steps may be used. [8][10]

Browser-level settings (`browser`, `headless`, `videoCapture`, `ignoreHTTPSErrors`, and `maximize_browser`) therefore do not affect a directory-only `@pdf` test. They can affect a browser-backed download scenario. For Chromium, `headless=true` is implemented as the `--headless=old` launch argument; the `headless` JVM property overrides the YAML value, and an absent/blank value defaults to headed mode. [13][14]

## 4. Validation capabilities

### 4.1 Text, pages, and structure

`PdfUtils` extracts plain text through `PDFTextStripper`. Before public text methods return it—and before expected text is compared—the framework changes non-breaking spaces to spaces, collapses ordinary/zero-width/BOM whitespace to one space, and trims the result. This reduces whitespace-driven false negatives but means assertions cannot prove exact original spacing, line breaks, or invisible-character placement. PDFBox reading order can also vary with PDF structure. [2][3]

| Intent | Implemented validator behavior | Exposed Gherkin expression |
|---|---|---|
| Whole-document contains / does not contain | Normalized substring containment. | `Then the last PDF should contain "<text>"` / `should not contain` [3][8] |
| Several required strings | Reads full text once and asserts each normalized expected value. | `Then the last PDF should contain all:` with a one-column table [3][8] |
| Whole-document regex | Java `Pattern.compile(regex).matcher(text).find()`. | `Then the last PDF should match regex "<regex>"` [3][8] |
| Page contains / regex | Extracts only the requested 1-based page. | `Then page <number> of the last PDF should contain "<text>"` / `should match regex` [2][3][8] |
| Page count | PDFBox document page count equals expected integer. | `Then the last PDF should have <number> pages` [2][3][8] |
| Basic file check | Tests whether the bytes begin with `%PDF-`. | `Then the last PDF should be a valid PDF` [2][3][8] |
| Diagnostic text | Prints up to the requested number of normalized characters and fails if the result is empty. | `Then I print first <number> chars of last PDF` [8] |

The “valid PDF” wording is broader than the implemented check: `isPdfFile` reads the file bytes and checks only the `%PDF-` prefix. It is a useful smoke check, but it is not a complete parse, PDF/A check, signature check, encryption-policy check, malware scan, or semantic validation. Later text/page operations do open the document with PDFBox and can still fail. [2][3]

### 4.2 Metadata and AcroForms

`PdfMeta.documentInfo` returns a `LinkedHashMap` of these fixed Info-dictionary keys when a document information object is present: `Title`, `Author`, `Subject`, `Keywords`, `Creator`, `Producer`, `CreationDate`, `ModificationDate`, and `Trapped`. Null values become empty strings and date values are converted from `Date#getTime().toString()`. The metadata assertion checks normalized containment for one named key. [4][3]

`PdfMeta.formFields` obtains the AcroForm, iterates `form.getFields()`, and maps each returned field’s fully qualified name to `getValueAsString()` (or an empty string for null). The Gherkin assertion performs normalized equality. [4][3][8]

```gherkin
Then the last PDF metadata "Title" should contain "<expected-document-title>"
And the last PDF form field "<fully-qualified-field-name>" should equal "<expected-value>"
```

Password-aware overloads exist in `PdfMeta`, `PdfUtils`, and `PdfOcr`, but the current Gherkin glue exposes **no password parameter** for metadata, forms, text, page count, rendering, OCR, or visual comparison. The current form reader also iterates only the AcroForm’s top-level `getFields()` collection; do not assume recursive discovery of every nested child field without verifying the document and source behavior. [2][4][5][8]

### 4.3 OCR for image-only documents

Use OCR when text extraction is empty or inadequate because a page is scanned. `PdfOcr` renders one 1-based page at the requested DPI and runs:

```text
tesseract <temporary-png> stdout -l <language>
```

The command merges standard error into standard output, reads UTF-8 output, requires a zero exit status, and normalizes the returned string. Null/blank core API language values default to `eng`; the Gherkin step still requires a language string. [5][8]

```gherkin
Then OCR on page 1 of the last PDF with dpi 300.0 and lang "eng" should contain "<expected-scanned-text>"
```

OCR assertions are deliberately optional. They use JUnit assumptions to **skip** when `-Dpdf.ocr.enabled=false` or when `tesseract -v` cannot run successfully from `PATH`; enabled is the default, and only the case-insensitive literal `false` disables it. Once OCR starts, rendering or Tesseract failures are ordinary runtime failures, not skips. [3][5]

### 4.4 Rendered visual comparison

Visual testing renders one PDF page using `PDFRenderer` at the requested DPI, compares it to an expected image, and accepts the result when the differing-pixel fraction is less than or equal to `maxDiffRatio`. A pixel differs if **any** of the RGBA channel differences exceeds `channelTolerance`. [2][3][6]

```gherkin
Then page 1 of the last PDF rendered at 200.0 dpi should visually equal "<baseline-dir>/document-page-1.png" with tolerance 2 and max diff 0.005, diff out "<evidence-dir>/document-page-1.diff.png"
```

Choose a stable DPI and baseline produced under the same expected rendering conditions. The source suggests 150–200 DPI for readable display output and 300 DPI for OCR/high-fidelity capture; higher DPI also increases OCR CPU/disk cost. [2][5]

| Result condition | `DiffResult` / artifact behavior |
|---|---|
| Dimensions differ | Immediate non-match with `diffRatio=1.0`; **no diff image is written**, even if an output path was supplied. [6] |
| Dimensions match and ratio is within threshold | Match; the actual render remains on disk; no diff image is written. [3][6] |
| Dimensions match and ratio exceeds threshold | Non-match; a semi-transparent red overlay PNG is written only if `diffOutPath` is nonblank. The assertion message includes ratio and generated diff path when available. [3][6] |

The documented parameter ranges (`channelTolerance` 0–255 and ratio 0.0–1.0) are not validated by `PdfRenderDiff.compare`; choose sensible values in the feature. The implementation does not perform OCR-aware, layout-aware, semantic, or multi-page comparison in one step—each visual step evaluates one page. [6][8]

## 5. Safe, executable Gherkin patterns

The following expressions exactly match the current `PdfSteps` definitions while keeping business data and paths as placeholders.

```gherkin
@pdf
Feature: Validate a generated PDF from the filesystem

  Background:
    When I set last PDF from directory "<pdf-input-directory>"
    Then the last PDF should exist
    And the last PDF should be a valid PDF
    And the last PDF should have 2 pages

  Scenario: Verify text and selected page content
    Then the last PDF should contain all:
      | <document-heading> |
      | <required-label>   |
    And the last PDF should not contain "<forbidden-placeholder>"
    And page 1 of the last PDF should contain "<first-page-heading>"
    And page 2 of the last PDF should match regex "<safe-java-regex>"

  Scenario: Verify document attributes
    Then the last PDF metadata "Title" should contain "<expected-document-title>"
    And the last PDF form field "<fully-qualified-field-name>" should equal "<expected-value>"
```

For a browser-backed acquisition flow, keep the feature outside the `@pdf` browserless classification and ensure its locator is defined in the framework’s element YAML:

```gherkin
When I download PDF from "<download-control>"."<locator-key>" saving to "<download-root>"
Then the last PDF should exist
And the last PDF should have 1 pages
```

The repository’s existing PDF feature is a text-only example and demonstrates whole-document, page-specific, and regex assertions. Its concrete sample values should be regarded as fixture-specific rather than copied into reusable business tests. [9]

## 6. Configuration and execution selection

### 6.1 PDF-specific setting

| Setting | Source and default | Effect |
|---|---|---|
| JVM system property `pdf.ocr.enabled` | Not present in YAML; default behavior is enabled. | Set `-Dpdf.ocr.enabled=false` to skip OCR assertion steps without attempting Tesseract. Any other value leaves OCR enabled. [3] |

A safe command that disables optional OCR for whatever Cucumber scenarios are otherwise selected is:

```bash
mvn test -Dpdf.ocr.enabled=false
```

### 6.2 Related live runtime settings

| Key | Current configured value | Relevance to PDF work |
|---|---:|---|
| `browser` | `chrome` | Used only for browser-backed acquisition; not for an `@pdf` directory-validation scenario. [13][14] |
| `headless` | `"false"` | Controls UI download session visibility; JVM `-Dheadless=...` takes precedence. [13][14] |
| `videoCapture` | `"false"` | Controls Playwright video for browser-backed scenarios, not document validation evidence. [13][14] |
| `runtimeWait` | `0` | Used for Playwright default timeouts in browser-backed scenarios; it does not configure PDF parsing/OCR/diff timeouts. [10][13] |
| `reporting.per_feature_reports_enabled` | `true` | Enables per-feature reporting listener work. [13][15][20] |
| `reporting.per_feature_pdf_enabled` | `false` | Current setting does not emit the listener’s direct per-feature PDF. [13][15][20] |
| `reporting.per_feature_glass_pdf_enabled` | `true` | Current setting requests a separate Glass-style per-feature PDF. [13][15][20] |
| `reporting.per_feature_reports_output_dir` | `test-output/per-feature-reports` | Per-feature HTML root and direct-PDF root if enabled. [13][15] |
| `reporting.per_feature_glass_pdf_output_dir` | `test-output/per-feature-reports-glass` | Per-feature Glass-PDF root. [13][15] |

Configuration values are loaded from YAML files beneath the classpath folders `elements`, `queries`, `api_requests`, `config`, and `performance`; values are merged and accessed by dot-separated paths. `ConfigurationProperties` first tries `environments.<env>.<key>` using JVM `env` (default `QA`), then falls back to the plain key. [15][16]

### 6.3 Current runner discrepancy

The Maven Surefire configuration runs `src/test/resources/testng.xml`, which references `com.ptaf.runner.TestRunner`. That runner scans `src/test/resources/features` and its annotation currently selects `@eStore`, whereas the repository PDF feature is tagged `@pdf @text-only @green`. Consequently, a plain `mvn test` follows the configured suite but does **not** select the supplied PDF feature under the current source. [1][9][17][18]

No dedicated PDF Maven profile, PDF TestNG suite, or source-configured command-line tag override was found in the inspected POM, TestNG suite, or active runner. Select PDF scenarios through the project’s approved runner/tag-selection change or an execution mechanism added by the project owner; do not assume that `mvn test` is a PDF test command. [1][17][18]

## 7. Reports and evidence

### 7.1 Test result reports

The active TestNG runner registers console (`pretty`), HTML, JSON, rerun, Extent adapter, per-feature, and soft-assertion reporting plugins. It writes its direct Cucumber outputs below `target/cucumber-reports/` (HTML directory, JSON report, and rerun file). Maven Surefire additionally defines a `cucumber.plugin` system property that requests JSON and JUnit XML under `target/cucumber-reports/` plus `pretty`; inspect the actual run output if both plugin sources are relevant to your execution. [1][17]

`extent.properties` enables timestamped run folders under `test-output/`, Spark and Base64 HTML reports, a combined `PdfReport/FNB-PTAF-Report.pdf`, and an Excel report. It also declares the screenshot directory relative to that timestamped folder. [19]

With current YAML settings, `PerFeatureReportListener` creates per-feature HTML under `test-output/per-feature-reports` and requests a Glass-style PDF under `test-output/per-feature-reports-glass`. It derives filenames from the declared feature title plus its listener timestamp. The configuration comments say `{feature_file_name}`, but the listener source uses the parsed `Feature:` title; the code is the authoritative behavior. [13][20]

The listener records scenario results, steps, durations, failure messages, and image EmbedEvents. Direct per-feature PDF generation and Glass-PDF subprocess generation are separately conditional. Glass generation launches a new JVM, has a 120-second wait, verifies that the output PDF exists and is nonempty, and removes its temporary JSON and temporary screenshot-PNG inputs in `finally`. Failures of the optional report-generation branch are logged as warnings rather than rethrown from the listener. [20]

### 7.2 PDF validation artifacts are not automatic report attachments

The PDF module itself writes these inspection artifacts only when asked by a render/visual/OCR operation:

| Artifact | Produced by | Reporting behavior |
|---|---|---|
| Rendered actual PNG | `PdfUtils.renderPageToPng`, used by the visual validator | Persists beside/according to the expected-baseline-derived path; no source-visible automatic Cucumber EmbedEvent attaches it. [2][3] |
| Red-overlay diff PNG | `PdfRenderDiff.compare` on eligible visual mismatch | Available in the assertion message when written; no source-visible automatic report attachment. [3][6] |
| OCR temporary PNG | `PdfOcr.ocrPage` | Created in the JVM temp directory and not explicitly deleted by current code; no automatic report attachment. [5] |
| PDF text preview | `PdfValidator` failure or the print step | Written to failure message/standard output, not a standalone evidence file. [3][8] |

For auditable visual failures, supply a deterministic `diff out` path below a retained evidence directory and keep the rendered actual image with it. For text failures, preserve the runner report and console/Surefire output because the diagnostic preview is intentionally limited to 1,200 characters. [3][6]

## 8. Troubleshooting and source-visible limitations

| Symptom | Likely source-visible cause | Action grounded in the implementation |
|---|---|---|
| `No lastPdfPath set` or `PDF not found` | No acquisition step ran on the current thread, directory choice found the wrong input, or the artifact was deleted/moved. [7] | Add the directory-selection or strict browser download step before validation; confirm the resolved file exists. Remember that `PdfStore` is thread-local. |
| Unexpected input selected | Directory selection sees only direct regular files with lower-case `.pdf`; it uses modification time. [7] | Use a clean per-scenario directory or make the intended artifact unequivocally newest. Do not rely on an upper-case extension or same-timestamp ordering. |
| Browser setup is skipped / download step says page is closed or uninitialized | `@pdf` and `features/pdf/` classify the scenario as browserless, but the download step requires `Hooks.getPage()`. [8][10] | Use directory-based acquisition in `@pdf` features. For a UI download test, use a browser-backed feature and then validate its stored path. |
| `valid PDF` passes but a later operation fails | The exposed validity check is only a `%PDF-` header probe. [2][3] | Add a page-count, text, metadata, or render step that opens the document with PDFBox; inspect the nested runtime cause. |
| Text is empty or text assertion does not match the visible scan | `PDFTextStripper` sees no useful text layer; its reading order and normalization are not a visual layout guarantee. [2] | Use the OCR step for scanned content or visual comparison for layout. Avoid asserting significant original whitespace/newlines. |
| OCR step is skipped | OCR was disabled by `pdf.ocr.enabled=false` or `tesseract -v` was not successful on `PATH`. [3] | Remove/adjust the property when OCR is desired; install Tesseract for the execution identity and language data, and set `TESSDATA_PREFIX` when required. [5] |
| OCR fails rather than skips | Tesseract was detectable but rendering, language loading, temp-file access, or the OCR process failed. [5] | Inspect the wrapped cause; verify requested page/DPI, writable temp space, language data, and process permissions. OCR temp PNGs may remain for diagnosis. |
| Visual assertion fails with no diff PNG | Baseline and actual dimensions differ, or `diff out` was blank. Dimension mismatch returns immediately with ratio `1.0`. [6] | Regenerate/choose a baseline with the same render DPI/dimensions and provide a nonblank diff path. |
| Visual mismatch is unexpectedly noisy | Page rasterization, baseline rendering conditions, or strict tolerance/ratio differs. The code compares every RGBA channel of every pixel. [2][6] | Fix DPI first, then use deliberately justified channel tolerance and maximum ratio. Avoid baselines generated by a materially different renderer or font environment. |
| Metadata/form assertion reports an empty actual value | The requested fixed metadata key or top-level AcroForm field is absent; no matching form field defaults to empty string before the assertion. [3][4] | Inspect the actual PDF’s Info dictionary and field hierarchy; do not assume custom/XMP metadata or recursively nested fields are exposed by this implementation. |
| Old PDF appears to remain selected on a reused worker thread | `PdfStore` has `clear()`, but the inspected production/test code does not call it outside the store class. [7] | Set the intended current path at each scenario start; call `PdfStore.clear()` in custom lifecycle code only if the project chooses to manage reused threads explicitly. |
| `mvn test` runs no PDF scenarios | Default runner tag is `@eStore`; the supplied feature is `@pdf`. [9][17][18] | Change/select tags through the approved test execution configuration. The current POM does not define a separate PDF profile. [1] |

## 9. Boundaries of the implemented module

The module **does provide** extraction, normalized string/regex checks, 1-based page checks, page count, a header heuristic, selected Info metadata, top-level AcroForm values, optional external OCR, single-page PNG rendering, and pixel-based baseline comparison. [2][3][4][5][6]

It does **not expose through current Gherkin** password-bearing PDF validation despite underlying overloads; recursive form traversal; XMP/custom metadata assertions; digital-signature validation; PDF/A or accessibility/conformance validation; PDF creation/editing; multi-page visual comparison in one step; baseline lifecycle management; or automatic attachment of rendered/diff/OCR artifacts to Cucumber reports. These boundaries follow the inspected methods and step definitions, rather than an assumption that every PDFBox capability is available. [2][3][4][5][6][8]

## Related chapters

- **Framework setup, execution, and runners** — planned local chapter `01-framework-overview.md`.
- **Configuration and test-data resolution** — planned local chapter `02-configuration-and-test-data.md`.
- **UI browser automation and evidence** — planned local chapter `05-ui-browser-automation.md`.
- **Reporting and artifact management** — planned local chapter `10-reporting-and-evidence.md`.

## Source references

- [Maven build and Surefire configuration](../../../pom.xml) [1]
- [PDF utility operations](../../../src/main/java/com/ptaf/pdf/PdfUtils.java) [2]
- [PDF validator assertions](../../../src/main/java/com/ptaf/pdf/PdfValidator.java) [3]
- [Metadata and AcroForm reader](../../../src/main/java/com/ptaf/pdf/PdfMeta.java) [4]
- [OCR integration](../../../src/main/java/com/ptaf/pdf/PdfOcr.java) [5]
- [Visual-difference engine](../../../src/main/java/com/ptaf/pdf/PdfRenderDiff.java) [6]
- [Thread-local PDF storage](../../../src/main/java/com/ptaf/pdf/PdfStore.java) [7]
- [PDF Cucumber steps](../../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java) [8]
- [PDF feature example](../../../src/test/resources/features/pdf/PdfValidation.feature) [9]
- [Lifecycle and browserless classification](../../../src/main/java/com/ptaf/hooks/Hooks.java) [10]
- [Download action implementation](../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) [11]
- [Feature artifact naming](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) [12]
- [Active YAML configuration](../../../src/test/resources/config/config.yml) [13]
- [Browser factory](../../../src/main/java/com/ptaf/utils/BrowserFactory.java) [14]
- [Configuration accessors](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) [15]
- [YAML resource loader](../../../src/main/java/com/ptaf/utils/YamlReader.java) [16]
- [Active TestNG Cucumber runner](../../../src/test/java/com/ptaf/runner/TestRunner.java) [17]
- [TestNG suite](../../../src/test/resources/testng.xml) [18]
- [Extent report configuration](../../../src/test/resources/extent.properties) [19]
- [Per-feature report listener](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) [20]

### References

[1]: ../../../pom.xml "Maven build, dependencies, Surefire configuration, and profiles"
[2]: ../../../src/main/java/com/ptaf/pdf/PdfUtils.java "PDF text, page count, header, rendering, and normalization utilities"
[3]: ../../../src/main/java/com/ptaf/pdf/PdfValidator.java "High-level PDF assertions and OCR gating"
[4]: ../../../src/main/java/com/ptaf/pdf/PdfMeta.java "PDF Info metadata and AcroForm value extraction"
[5]: ../../../src/main/java/com/ptaf/pdf/PdfOcr.java "External Tesseract OCR for rendered PDF pages"
[6]: ../../../src/main/java/com/ptaf/pdf/PdfRenderDiff.java "Pixel-level image comparison and visual diff output"
[7]: ../../../src/main/java/com/ptaf/pdf/PdfStore.java "Thread-local current-PDF storage and newest-file selection"
[8]: ../../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java "Cucumber step definitions for PDF acquisition and validation"
[9]: ../../../src/test/resources/features/pdf/PdfValidation.feature "Current PDF text-validation feature"
[10]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Cucumber lifecycle, browserless classification, and Playwright page lifecycle"
[11]: ../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "Strict and optional Playwright download actions"
[12]: ../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-title-based artifact directories and filenames"
[13]: ../../../src/test/resources/config/config.yml "Active browser, download, and per-feature reporting settings"
[14]: ../../../src/main/java/com/ptaf/utils/BrowserFactory.java "Playwright launch and context configuration"
[15]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Configuration resolution and reporting accessors"
[16]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "Classpath YAML discovery, merge, and dot-path lookup"
[17]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Active TestNG Cucumber runner and output plugins"
[18]: ../../../src/test/resources/testng.xml "Maven TestNG suite entry point"
[19]: ../../../src/test/resources/extent.properties "Timestamped Extent report output configuration"
[20]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature HTML and PDF report generation"
