# Regular Playwright Web UI Automation Reference

This chapter is the reference for **normal, single-user Playwright web UI automation** in FNB-ETAF: Cucumber scenarios drive browser interactions through the regular hooks, shared page methods, element YAML, and Playwright locators. It covers desktop web UI execution, tabs/popups, iframes, evidence, waits, and troubleshooting.

> **Boundary — this is not UI performance testing.** The dedicated `ui_performance` Maven profile uses its own TestNG suite and `com.ptaf.ui_performance` journey engine. It is deliberately isolated from the default suite, normal `BrowserFactory`, normal `Hooks`, and normal reports. Run normal UI with the default Maven test command; use `-Pui_performance` only for that separate load-oriented module. [1]

## 1. What runs, and where the code lives

The default Maven Surefire configuration selects `src/test/resources/testng.xml`, uses TestNG with method parallelism (`threadCount` 4), and sets `testFailureIgnore` to `true`. That suite invokes `com.ptaf.runner.TestRunner`; its Cucumber configuration searches `src/test/resources/features`, scans the normal step-definition and hook packages, and currently selects `@eStore`. The project declares Playwright 1.62.0, Cucumber 7.20.0, TestNG 7.10.2, and Java 21. [1] [2] [3]

| Layer | Responsibility | Primary source |
|---|---|---|
| Run entry | Default Surefire suite and TestNG Cucumber runner | [pom.xml][1], [testng.xml][2], [TestRunner.java][3] |
| Lifecycle | Per-thread browser/context/page/scenario ownership; non-UI exclusion; `@LastScenario` sharing; video finalization | [Hooks.java][4] |
| Browser creation | Launches Chrome/Firefox/WebKit/Edge; configures context, HTTPS handling, video, HTTP basic auth, and desktop maximize behavior | [BrowserFactory.java][5] |
| Configuration | Resolves values from merged YAML, with an environment-specific lookup attempt before a plain-key fallback | [ConfigurationProperties.java][6], [YamlReader.java][7] |
| Locator repository | Reads `elements.<group>.<key>` values from YAML | [ElementLocatorHelper.java][8], [eStore_elements.yml][10] |
| Locator/action engine | Turns repository tokens into Playwright `Locator`s and executes interactions/assertions | [LocatorHandler.java][11], [ElementActionImpl.java][12], [ActionPerformer.java][13] |
| UI facades | Main-document and frame-aware common methods, including failure/evidence handling | [PageCommonMethods.java][14], [FrameCommonMethods.java][15] |
| Cucumber glue | Ordinary-page actions, popup/new-page actions, and named frame variants | [PageCommonSteps.java][16], [NewPageCommonSteps.java][17], [FrameCommonSteps.java][18] |
| Evidence/reporting | Screenshot attachment, feature-name artifact naming, Extent output, and soft-failure report marking | [ScreenshotHandler.java][19], [FeatureArtifactNameResolver.java][20], [extent.properties][24], [SoftAssertionReportListener.java][23] |

### Normal execution commands

```bash
# Default normal TestNG/Cucumber execution. The runner's current tag expression is @eStore.
mvn clean test

# Normal UI, forcing the BrowserFactory headless override without editing config.yml.
mvn clean test -Dheadless=true

# Deliberately separate module; do not use this command for ordinary UI scenarios.
mvn clean test -Pui_performance
```

`-Dheadless` has priority over the YAML `headless` value. The current `config.yml` uses desktop Chrome, headed mode, `maximize_browser: true`, HTTPS-error ignoring, a 10-second action wait, `runtimeWait: 0`, and disabled video capture. Do not put credentials or private target URLs in feature files or documentation; reference a neutral configuration key instead. [5] [9]

## 2. Normal UI execution flow

For a browser-requiring scenario, the normal path is:

1. **Cucumber creates the scenario and calls `Hooks.setUp`.** The hook stores the `Scenario` and derives a feature key in thread-local state. It skips browser startup for recognised performance, API, Appium mobile, and file/database scenarios. Regular Playwright web UI scenarios continue. [4]
2. **The hook creates the Playwright stack.** It maps the configured desktop browser name (`CHROME`, `FIREFOX`, `WEBKIT`, or `EDGE`) to `BrowserFactory.BrowserTypeEnum`, launches a browser, creates a context, registers page/video tracking, creates the initial page, and stores browser, context, page, scenario, and `PageCommonMethods` as thread-local state. [4] [5]
3. **Timeouts are applied.** `runtimeWait` is interpreted as seconds for the page default and default-navigation timeouts. A missing, invalid, zero, or negative value falls back to 30 seconds; therefore the current `runtimeWait: 0` produces 30,000 ms despite the literal zero in YAML. [4] [9]
4. **A Gherkin step reaches a facade.** Ordinary `on page` steps delegate to `PageCommonMethods`; `on new page` and named frame steps delegate to `FrameCommonMethods`. The facade asks `ElementActionImpl` to resolve the YAML locator, then asks `ActionPerformer` to interact with the first matching Playwright locator. [11] [12] [13] [14] [15] [16]
5. **Normal completion closes resources.** Except for `@LastScenario` features, `Hooks.tearDown` closes the browser resources at the end of the scenario. A `@LastScenario` feature retains the same live stack across its runnable scenarios, then closes it after the final one. A failed shared session marks remaining scenarios for intentional failure unless a close was explicitly marked as deliberate. [4]
6. **Evidence is finalized after browser shutdown.** Before closing, hooks retain video handles for the initial page and later popups. After closing, they wait briefly for Playwright video files, create a feature-title directory, and rename each recording with a safe feature-title timestamp. [4] [20]

### `@LastScenario` operational rule

Use `@LastScenario` only when scenarios in the same feature intentionally continue browser/session state. The hook counts runnable scenarios in the feature file, reuses the browser when it remains alive, and closes after the count is reached. The explicit step `Then we close all browsers` marks the close as intentional before cleanup, so a later scenario can start a fresh stack rather than being treated as a lost shared session. [4] [16]

Do not assume this tag creates a cross-feature session or works as a general retry mechanism. It is feature-keyed, thread-local lifecycle logic. [4]

## 3. Configuration that affects regular UI

`ConfigurationProperties.getValue` first checks `environments.<env>.<key>` where the JVM `env` property defaults to `QA`, then falls back to the plain key. The current configuration file does **not** define an `environments` block, so its current effective values come from the plain keys shown below. [6] [9]

| Current key | Current value / normal meaning | Consumed by |
|---|---|---|
| `browser` | `chrome`; normal hooks only map `CHROME`, `FIREFOX`, `WEBKIT`, and `EDGE` | `Hooks`, `BrowserFactory` [4] [5] |
| `maximize_browser` | `true`; Chromium receives `--start-maximized` only for headed desktop browser runs | `BrowserFactory` [5] [9] |
| `headless` | `"false"`; overridden by `-Dheadless=true` or `-Dheadless=false` | `BrowserFactory` [5] [9] |
| `ignoreHTTPSErrors` | `"true"`; applied to context, and Chromium also gets certificate-bypass launch arguments | `BrowserFactory` [5] [6] |
| `time_to_wait_in_seconds` | `"10"`; maximum action-level wait | `ActionPerformer` [13] [9] |
| `runtimeWait` | `0`; hooks turn this into the 30-second fallback for page defaults/navigation | `Hooks` [4] [9] |
| `videoCapture` | `"false"`; enables normal desktop recording when true | `BrowserFactory` [5] [6] |
| `downloadDocument` | Configured base path used by the current ordinary-page download step | `PageCommonSteps` [16] [9] |
| `reporting.per_feature_reports_enabled` | `true`; enables feature-level reporting in the current configuration | `ConfigurationProperties`, report listener [6] [26] |
| `reporting.per_feature_reports_output_dir` | `test-output/per-feature-reports` | Per-feature report listener [6] [9] |
| `reporting.per_feature_pdf_enabled` | `false`; direct per-feature PDF generation is disabled | Per-feature report listener [6] [9] |
| `reporting.per_feature_glass_pdf_enabled` | `true`; the listener may generate the separately located Glass-style PDF | Per-feature report listener [6] [9] |
| `soft_assertions.enabled` | `false`; normal fail-fast path is active | `PageCommonMethods`, `FrameCommonMethods` [9] [14] [15] |
| `soft_assertions.retry_seconds` | `3`; used only when soft mode is enabled | `ConfigurationProperties`, UI facades [6] [14] [15] |

### Headless and maximize behavior

For Chromium-based desktop runs, the factory intentionally leaves Playwright's launch option at `headless=false` and adds `--headless=old` only when headless mode is requested; this avoids a documented conflict with Playwright's Headless Shell handling. In a CI environment (`CI` is nonblank), it adds `--no-sandbox` and `--disable-setuid-sandbox`. Edge uses channel `msedge` plus an Edge-specific compatibility argument. [5]

`maximize_browser` is **not** a generic viewport resize. In headed desktop Chromium it adds `--start-maximized` but deliberately retains Playwright's stable default desktop viewport to reduce responsive/frame disruption. It is skipped in headless mode and mobile-profile mode; Firefox and WebKit log that this factory path cannot apply Selenium-style native maximize. [5]

The action engine also recognises `maximize`, `maximizewindow`, and `maximisebrowser`, which use a Chrome DevTools Protocol window-bounds request through `Hooks.maximizeBrowserWindow`. It returns false rather than failing when native maximization is unavailable. There is no active ordinary-page Gherkin binding for this action in `PageCommonSteps`; expose it through tested custom glue only if needed. [4] [13] [16]

> **Source-visible discrepancy:** `PageCommonMethods` comments describe an automatic popup-maximize listener, but the reviewed `Hooks` and `BrowserFactory` paths register no corresponding `onPage` maximize listener. Rely on the factory launch behavior or the explicit action, not on automatic popup maximization. [4] [5] [14]

## 4. BrowserFactory and hook responsibilities

### BrowserFactory

`BrowserFactory` is the normal Playwright construction boundary. It creates a new `Playwright` instance internally, launches the selected browser engine, creates a context with `ignoreHTTPSErrors`, optionally applies HTTP basic credentials from the JVM properties `service.username` and `service.password` only when both are nonblank, and conditionally configures video recording. These property names are mechanisms only; never commit their values. [5]

When `videoCapture` is true for normal desktop UI, the context records 1280×720 video under `test-output/captured-videos/<launch-timestamp>`. The mobile-evidence branch is separate and outside this chapter's normal desktop scope. [5]

Although `BrowserFactory` has a separate overload for mobile profile names, normal `Hooks.createBrowserStack` selects only the four desktop enum values. The mobile-profile comments in `config.yml` therefore do not change ordinary desktop-hook execution. Treat desktop regular UI and mobile-browser emulation as different execution paths. [4] [5] [9]

### Hooks

`Hooks` owns the normal thread-local objects and exposes `getPage`, `getBrowser`, `getContext`, `getCurrentScenario`, and `setPage`. Calling a getter before setup—or after resources have been closed—throws an `IllegalStateException` rather than returning a usable object. `setPage` validates that a popup/new page is live, reapplies page defaults, and rebuilds the page-common facade for that page. [4]

The hooks deliberately avoid initializing a normal browser for tags identified as performance, API, Appium mobile, XML/CSV-only, database, ZIP, or PDF work. `@xml_ui` and `@csv_ui` are an explicit exception to the file-only exclusion, preserving UI browser setup for UI-embedded file work. [4]

## 5. Locators and UI-page design

### Repository contract

The YAML reader scans classpath folders named `elements`, `queries`, `api_requests`, `config`, and `performance`, recursively parses `.yml`/`.yaml` files, and merges their maps. A later loaded scalar overwrites an earlier scalar, so avoid duplicate top-level element-group/key paths across files. [7]

A regular UI lookup is always:

```text
elements.<element-group>.<locator-key>
```

For example, the existing element repository follows this shape (illustrative values only):

```yaml
elements:
  CheckoutPage:
    submitButton: "Button_Submit order"
    emailField: "CSS_#email"
    confirmation: "TestID_confirmation"
```

The current repository contains the same `elements` hierarchy and token style, including CSS, XPath, role, label, option, and test-ID entries. [8] [10]

### Token parsing and chained locators

`ElementLocatorHelper` splits a locator token at its **first** underscore or space, whichever appears first. Thus `CSS_#email`, `XPATH_//button`, and `Button_Submit` resolve as type/value pairs; a bare token such as `BUTTON` has an empty value. `ElementActionImpl` splits an element's complete locator string on `>` (with surrounding whitespace allowed), resolves the first segment from the page or frame, then resolves remaining segments relative to the preceding locator. [8] [12]

Use chained YAML only when each `>` truly means a locator boundary:

```yaml
elements:
  Results:
    firstRowAction: "CSS_.result-row > Button_Open"
```

Because the source uses `\s*>\s*` as the chain separator, a CSS selector whose intended meaning includes a spaced child combinator can be misinterpreted as a framework chain. Prefer a single robust selector or intentionally model the relationship as separate chain segments. [12]

### Supported locator families

`LocatorHandler` applies the same mapping in `Page`, `FrameLocator`, and chained `Locator` contexts. The exact mapping is the source of truth. [11]

| YAML token family | Resolution behavior |
|---|---|
| `CSS`, `TAG`, `XPATH` | Passed to Playwright `locator(...)` |
| `ID`, `NAME`, `CLASS` | Converted respectively to `#value`, `[name='value']`, `.value` |
| `TEXT`, `ALTTEXT`, `TITLE`, `PLACEHOLDER`, `LABEL`, `TESTID` | Mapped to the matching Playwright `getBy...` method |
| Named ARIA role tokens | `BUTTON`, `LINKTEXT`, `TEXTBOX`, `CHECKBOX`, `RADIOBUTTON`, `DROPDOWN`, `OPTION`, `TAB`, `ROW`, `CELL`, and many other listed ARIA roles map to `getByRole`; a supplied value becomes the accessible name |
| `ROLE` | The value itself must be a valid `AriaRole` name, for example `ROLE_BUTTON` |
| Bare role token | A missing/empty value, or a value equal to the token, produces an unnamed role locator; later action code commonly uses `.first()` |

`OPTION` uses exact accessible-name matching. `BUTTONSUBMIT` applies `setPressed(true)` for its named form. Unsupported locator types throw a formatted `LOCATOR TYPE FAILURE` with the current context (`PAGE`, `FRAME`, or `CHAINED`) and the supplied token. [11]

### Recommended page-object boundary

Keep feature files at the **logical group/key** level. Put selectors in YAML, not in Gherkin. The normal implementation flow is:

```text
Gherkin step
  -> PageCommonMethods or FrameCommonMethods
  -> ElementActionImpl
  -> ElementLocatorHelper reads elements.<group>.<key>
  -> LocatorHandler creates a Playwright Locator
  -> ActionPerformer waits and acts on locator.first()
```

`ElementActionImpl` is an orchestration layer: it resolves a `Page` or `FrameLocator` context, waits for the result through `ActionPerformer`, executes the action, and reports a boolean success to its facade. [8] [11] [12] [13]

## 6. Gherkin surface: ordinary pages, new pages, and frames

There is **no regular `UISteps.java` class** in the inspected normal web UI sources. The class named `UiPerformanceSteps` belongs to the separate `com.ptaf.ui_performance` module and is not normal UI glue. For normal UI, use `PageCommonSteps`, `NewPageCommonSteps`, and the limited active bindings in `FrameCommonSteps`. [16] [17] [18] [25]

### Safe ordinary-page example

The following uses placeholders and active ordinary-page bindings; the navigation value is a configuration key, not a literal target URL.

```gherkin
@web_smoke
Feature: Order confirmation

  Scenario: Submit an order
    Given we navigate to WEB_APP_URL_KEY url
    Then we enter value on page CheckoutPage locator emailField value "<test-email>"
    And we select on page CheckoutPage locator deliveryMethod value "standard"
    Then we click on page CheckoutPage locator submitButton
    And we verify on page ConfirmationPage of locator heading is visible
    And we contain on page ConfirmationPage of locator heading value "Thank you"
    And we capture screenshot on page ConfirmationPage locator pageBody name "confirmation"
```

The current ordinary-page bindings include click, double click, enter/fill, select, check, hover, type, scroll, clear, visibility/checked/enabled/existence checks, text/value retrieval, containment, keyboard press, screenshot capture, a hard-sleep convenience step, download, and explicit browser close. These bind to `PageCommonMethods`; the exact regular expressions are in `PageCommonSteps`. [16]

### Popup/new-tab flow

Use the actual popup binding when the click opens a new window or tab:

```gherkin
Then we click CheckoutPage locator openReceipt and switch to popup
Then we verify on new page ReceiptPage of locator heading is visible
And we capture screenshot on new page ReceiptPage locator pageBody name "receipt"
```

`NewPageCommonSteps` wraps the click in `currentPage.waitForPopup`, waits for popup `DOMContentLoaded`, and calls `Hooks.setPage(popupPage)`. Subsequent `new page` steps therefore target the popup. The hook also keeps its video handle before the popup can close. [4] [17]

**Limit:** reviewed normal glue does not offer a named tab index, a declarative switch-back-to-parent step, or a generic arbitrary-page selector. Add a small, reviewed custom step that retains the desired `Page` reference and calls `Hooks.setPage` only if the use case requires it. Do not use a stale `Page` after closing a popup. [4] [17]

### Frames

`FrameCommonMethods` and `ElementActionImpl` can resolve one, two, or three nested iframe selectors programmatically. The active `NewPageCommonSteps` provides recurring named one-frame variants for these fixed selectors:

| Gherkin prefix | Frame selector coded in glue | Typical active operations |
|---|---|---|
| `on plaid frame` | `iframe[title='Plaid Link']` | click, fill/type, select, check/uncheck, visibility, text/value, screenshots, key press |
| `on pop frame` | `//*[@id='AcceptUIContainer']/iframe` | Same family of operations |
| `on atomic frame` | `#atomic-transact-iframe` | Same family of operations |

For example:

```gherkin
Then we enter value on atomic frame ProviderLogin locator username value "<test-user>"
And we click on atomic frame ProviderLogin locator continueButton
Then we verify on atomic frame ProviderResult of locator completionMessage is visible
```

These frame names and selectors are code constants, not configurable YAML entries. `FrameCommonSteps` contains many older frame-oriented annotations as comments; its active bindings in the reviewed source are only navigation and one popup-switch form. Do not document a commented annotation as an available Gherkin step. [11] [14] [16] [17]

## 7. Actions, assertions, and wait semantics

### Action performer behavior

`ActionPerformer` applies a **smart maximum wait**: for visible/clickable interactions, it first tests whether `locator.first()` is already ready and otherwise waits up to `time_to_wait_in_seconds`. A zero or negative action wait means no additional wait; an absent or unparsable value falls back to 30 seconds. Navigation-like click, select, check/uncheck, press, double-click, right-click, tap, drag, and select-multiple operations then attempt a `DOMContentLOADED` wait using the full action timeout. That load-state wait deliberately ignores failures and does not wait for `NETWORKIDLE`. [13]

| Action group | Source-supported action names | Operational detail |
|---|---|---|
| Interact | `click`, `fill`, `select`, `check`, `uncheck`, `hover`, `type`, `press`, `dblclick`, `rightclick`, `tap`, `clear`, `focus`, `blur`, `scroll` | Acts on `locator.first()` after its relevant smart wait. `fill` replaces value; `type` sends keystrokes. [13] |
| Element/page manipulation | `input`, `drag`, `dragstart`, `dragend`, `setattribute`, `removeattribute`, `evaluate` | `input` writes `element.value` through JavaScript. `drag` expects its `value` to be a destination selector. [13] |
| Files | `uploadfile`, `selectfile`, `file_chooser_for_upload`, `download`, `download_optional` | File inputs call `setInputFiles`; strict download throws if no event; optional download returns `null` on no download/failure. [13] |
| Evidence/window | `screenshot`, `fullscreenshot`/`fullpagescreenshot`, `maximize` aliases | Element screenshot requires a value path; full page uses `page.screenshot(fullPage=true)`. [13] |
| Read/validate | `getattribute`, `gettext`, `getvalue`, `hasvalue`, `equalslisttext`, `order`, `isvisible`, `isenabled`, `ischecked`, `isdisabled`, `ishidden`, `hastext`, `hasclass`, `hasequalvalue`, `isempty`, `exists`, `not_exists` | Assertions throw if the condition is false. `order` accepts only `ascending` or `descending`. [13] |
| Explicit waits | `waitforelement`, `waitforstate`, `waitfortext`, `waitforvalue` | State expects a valid Playwright `WaitForSelectorState` name. Text/value checks are not a polling loop beyond the initial visibility wait. [13] |

### Prefer condition-driven waits

Use visibility/existence/text assertions after the condition that changes the UI. The project has `we wait for some time`, `time out for <seconds> seconds`, and `Stop Execution` steps, all using `Thread.sleep`; these are blocking fallbacks, not synchronization primitives. A normal action already performs its source-defined smart wait, and a popup step waits for popup plus `DOMContentLoaded`. [13] [16] [17]

### Assertion choices

For normal Gherkin, prefer the existing direct verification and containment steps. `UIAssert` is a Java-level semantic wrapper for exact/normalized text, attributes, state, count, and frame-aware assertions; it attempts a screenshot and throws its own `RuntimeException` when it detects a mismatch. It is not exposed as a reviewed ordinary Gherkin binding in the listed step classes. [21]

> **Source-visible caution:** `ElementActionImpl.performActionPageWithReturn` and its frame counterpart catch exceptions and return `null`. `UIAssert.delegateBooleanish` logs a `null` result as passed rather than treating it as a failure. This can mask an underlying action failure for those wrapper methods. Prefer active Cucumber assertions or test `UIAssert` carefully before relying on it for a gating result. [12] [21]

### Known facade/action discrepancies

The following are implementation facts, not recommended patterns:

| Area | Source-visible behavior | Practical guidance |
|---|---|---|
| Ordinary `uncheck` | `PageCommonSteps.weUncheckActionOnPage` calls `pageCommonMethods.check`, although `ActionPerformer` has a real `uncheck` action. New-page/frame variants call `uncheck`. | Do not rely on the ordinary `we uncheck on page ...` step to clear a checked control; use a corrected binding after a code fix or the new-page/frame path where appropriate. [13] [16] [17] |
| Ordinary `clear` regex | The ordinary binding regex captures a `value` argument, but the Java method accepts only `element` and `locator`. | Treat that exact ordinary clear step as unreliable until its expression/method arity is reconciled. [16] |
| `selectMultiple` | Facades pass `null` as the action value, while the action performs `value.split(",")`. | It cannot work through these wrappers without a value-passing correction. [13] [14] [15] |
| `waitForState` | Facades pass `null`, while the action calls `value.toUpperCase()`. | It needs a binding that supplies the requested state. [13] [14] [15] |
| Equal-value facade | `PageCommonMethods`/`FrameCommonMethods` call misspelled `hasqualvalue`; the action supports `hasequalvalue`. | Use the direct `hasvalue` route or correct the facade before using this helper. [13] [14] [15] |
| `drag` facade | The facade passes no destination selector, but the action uses `value` to locate the drop target. | Add a destination-aware binding before using drag-and-drop. [13] [14] [15] |
| `getvalue` facade | `PageCommonMethods.getvalue` performs the action but returns the logical element name, not the value. | Use it as a logging step only; implement a result-capturing binding if a later assertion needs the value. [14] |

## 8. Screenshots, downloads, videos, and reports

### Screenshots

The explicit ordinary-page screenshot step builds a file path as `test-output/screenshots/<name>.png`, invokes element screenshot capture, and attaches an element image to Cucumber as a passed-step artifact when the facade has not failed. New-page and named-frame screenshot steps use the same file-path convention, with frame-aware attachment. [14] [15] [16] [17] [19]

On normal fail-fast UI failure, `PageCommonMethods` captures a full-page screenshot through `ScreenshotHandler`, attempts browser cleanup, and throws. `ScreenshotHandler` attaches screenshot bytes to the Cucumber scenario as `image/png`; it catches screenshot errors so evidence failure does not hide the original failure. [14] [19]

A full-page action exists (`fullscreenshot`/`fullpagescreenshot`) and saves to the caller-supplied path, but no active `PageCommonSteps` binding exposes its documented generic form. If adding glue, ensure the parent directory exists and pass a neutral output path such as `test-output/screenshots/<safe-name>.png`. [13] [14] [16]

### Downloads and uploads

`download` is strict: it wraps a click in `page.waitForDownload`, creates a feature-name subdirectory inside the supplied output root, preserves the suggested file extension, and saves a feature-title/timestamp filename. `download_optional` uses the action timeout and returns `null` without throwing when no download occurs. Upload actions use `setInputFiles(Paths.get(value))`. [13] [20]

The active ordinary download step reads `downloadDocument` then appends `.jpeg` before supplying the result as the **directory** argument. Because the action treats its value as an output directory, verify this path before adopting that step: with the current configuration it resolves below the configured directory rather than naming a JPEG file. For reliable downloads, use a corrected binding that supplies an actual safe output directory; do not encode an expected extension by modifying the directory path. [9] [13] [16]

### Video recordings

Set `videoCapture: "true"` to enable regular desktop context recording. The context records at 1280×720 in a launch-timestamped `test-output/captured-videos/...` directory. Hooks record handles for the initial and popup pages; after `browser.close()`, they wait for finalization, create a sanitized declared-Feature-name directory, and move each `.webm` file to a feature-title plus microsecond timestamp name. Video rename/move failures are logged and intentionally do not change the scenario result. [4] [5] [20]

### Generated reports

The normal TestNG runner requests pretty console output, Cucumber HTML/JSON/rerun output below `target/cucumber-reports`, Extent adapter output, and the per-feature and soft-assertion listeners. Extent's current configuration creates a timestamped run directory under `test-output/` with Spark HTML, Base64 HTML, PDF, and Excel reporter paths. Per-feature HTML output is currently enabled under `test-output/per-feature-reports`; the separate Glass-style PDF switch is also enabled. [3] [9] [24] [26]

## 9. Soft assertions: available configuration, current caveat

With the current `soft_assertions.enabled: false`, normal UI is fail-fast: a facade failure records evidence, closes the page/browser path, and throws. When enabled, `PageCommonMethods` and `FrameCommonMethods` use a thread-local action-timeout override of `retry_seconds × 1000` for element waits, record the failure, capture evidence, and continue rather than close the browser. Page-load waits still use the full `time_to_wait_in_seconds` timeout. [9] [13] [14] [15]

`SoftAssertionContext` is thread-local and can collect timestamp, step description, message, and screenshot note. `SoftAssertionReportListener` marks a Gherkin step failed in the Extent report when new soft failures are present, but it explicitly does not throw or stop execution. [22] [23]

> **Important source-visible discrepancy:** comments in `config.yml`, `ConfigurationProperties`, and `SoftAssertionContext` describe end-of-scenario aggregation, clearing, and a final scenario failure. In the reviewed normal `Hooks` source, there is no call to `SoftAssertionContext.clear()`, `hasFailed()`, or `buildSummary()`, and no terminal `AssertionError` based on the collected failures. The inspected listener only adjusts Extent reporting. Consequently, do not assume soft mode reliably makes Cucumber/Maven fail at scenario end or clears collected failures between reused threads until this lifecycle is implemented and verified. [4] [6] [9] [22] [23]

## 10. Troubleshooting normal UI failures

| Symptom | Check in this order | Source-grounded diagnosis / response |
|---|---|---|
| Browser does not start | Runner glue; `browser`; config resources; browser name casing | The normal runner must include `com.ptaf.hooks`; hooks accept only desktop Chrome/Firefox/WebKit/Edge names in their normal switch. YAML loader diagnostics identify missing folder/file/path segments. [3] [4] [7] |
| Browser is unexpectedly headed/headless | Maven command; JVM property; `headless`; browser engine | `-Dheadless` wins over YAML. Chromium uses `--headless=old`; Firefox/WebKit use their normal Playwright headless option. [5] [9] |
| Browser does not maximize | Headless/mobile mode; engine; whether explicit maximize has binding | Launch maximize applies only to headed desktop Chromium. Firefox/WebKit warn; headless has no OS window. The native maximize action is CDP-dependent and no ordinary Gherkin binding currently exposes it. [4] [5] [13] |
| Action times out despite `runtimeWait` | Distinguish page defaults from action waits | Hooks use `runtimeWait`; smart action waits use `time_to_wait_in_seconds`. Current `runtimeWait: 0` falls back to 30 seconds, while action max is 10 seconds. [4] [9] [13] |
| SPA appears to keep loading | Check the post-click condition, not network idle | Navigation-like actions wait only for `DOMContentLoaded`; the action engine intentionally does not wait for `NETWORKIDLE`. Add a meaningful visible/text assertion. [13] |
| YAML locator is null or wrong | Element group/key; merged resource path; token syntax | The lookup must be `elements.<group>.<key>`. Logs identify missing YAML path. Use a supported type token and avoid accidental ` > ` chain splitting. [7] [8] [11] [12] |
| Locator matches the wrong control | Accessible name; role token; first-match behavior | The action layer commonly uses `.first()`. Make the YAML role name or selector sufficiently specific; `OPTION` is exact when named. [11] [13] |
| Popup actions use the original page | Popup binding and current page state | Use `... and switch to popup`; it waits for the popup and calls `Hooks.setPage`. There is no built-in switch-back step. [4] [17] |
| Frame element is not found | Correct named frame; iframe readiness; hierarchy | The active named glue uses fixed Plaid, Pop, and Atomic selectors. Programmatic locator resolution supports only the supplied first/second/third nested selector chain. [12] [15] [17] |
| Screenshot/video missing | Evidence settings; close path; output folders | Explicit screenshot attaches to Cucumber and writes its requested path. Video requires `videoCapture: true` and finalizes after browser close. Video naming errors do not fail the test. [4] [5] [19] [20] |
| Download path looks wrong | Active ordinary download binding | The binding appends `.jpeg` to the configured directory before strict download handling. Correct the binding/output root rather than expecting a file at that concatenated value. [9] [13] [16] |
| Later `@LastScenario` scenarios fail immediately | First failed/lost scenario; whether close was intentional | Shared-feature logic marks later scenarios failed after an unintentional lost browser. Use the explicit close only where a new stack is actually intended. [4] |
| Soft assertions look failed in HTML but build passes | Soft lifecycle implementation | The listener updates report status; the normal hook currently has no observed terminal aggregation/throw. Treat this as a known implementation gap. [4] [22] [23] |

The action performer emits a multi-line failure record containing action, value, current URL/title when available, locator string, match count, exception class, and message. Start diagnosis there, then validate YAML and the appropriate page/frame context before raising the timeout. [13]

## Related chapters

The following are **future local chapter filenames**; this chapter intentionally does not duplicate their ownership areas:

- `01-foundation-configuration-and-execution.md` — build, configuration, runner, and environment conventions.
- `04-api-automation.md` — HTTP/API test execution and non-UI behavior.
- `10-ui-performance-load-testing.md` — the isolated `ui_performance` journey/load engine.
- `11-reporting-evidence-and-artifacts.md` — framework-wide reporting and artifact governance.

## Source references

1. [Maven build, default suite, Playwright dependency, and isolated UI-performance profile][1]
2. [Default TestNG suite][2]
3. [Normal TestNG Cucumber runner][3]
4. [Normal Hooks lifecycle][4]
5. [BrowserFactory][5]
6. [ConfigurationProperties][6]
7. [YamlReader][7]
8. [ElementLocatorHelper][8]
9. [Current shared configuration][9]
10. [Current regular UI element repository][10]
11. [LocatorHandler][11]
12. [ElementActionImpl][12]
13. [ActionPerformer][13]
14. [PageCommonMethods][14]
15. [FrameCommonMethods][15]
16. [PageCommonSteps][16]
17. [NewPageCommonSteps][17]
18. [FrameCommonSteps][18]
19. [ScreenshotHandler][19]
20. [FeatureArtifactNameResolver][20]
21. [UIAssert][21]
22. [SoftAssertionContext][22]
23. [SoftAssertionReportListener][23]
24. [Extent reporter configuration][24]
25. [Dedicated UI-performance steps, boundary evidence][25]
26. [Per-feature Extent report listener][26]

## References

[1]: ../../../pom.xml "Maven build: dependencies, default Surefire configuration, and ui_performance profile"
[2]: ../../../src/test/resources/testng.xml "Default TestNG suite"
[3]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Normal TestNG Cucumber runner"
[4]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Normal Playwright browser, context, page, video, and scenario lifecycle"
[5]: ../../../src/main/java/com/ptaf/utils/BrowserFactory.java "Browser and BrowserContext construction"
[6]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Framework configuration accessors"
[7]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "Merged YAML resource loader"
[8]: ../../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java "Element repository lookup and locator-token parsing"
[9]: ../../../src/test/resources/config/config.yml "Current shared framework configuration"
[10]: ../../../src/test/resources/elements/eStore_elements.yml "Current regular UI element YAML repository"
[11]: ../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java "Locator type mappings for page, frame, and chained contexts"
[12]: ../../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java "YAML-to-locator resolution and action orchestration"
[13]: ../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java "Playwright interactions, waits, downloads, validation, and diagnostics"
[14]: ../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java "Normal page-action facade, evidence, and soft/fail-fast handling"
[15]: ../../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java "Frame-aware page-action facade"
[16]: ../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java "Ordinary-page Cucumber step definitions"
[17]: ../../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java "Popup, new-page, Plaid, Pop, and Atomic frame Cucumber steps"
[18]: ../../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java "Active generic navigation and popup frame Cucumber steps"
[19]: ../../../src/main/java/com/ptaf/utils/ScreenshotHandler.java "Cucumber screenshot attachment utility"
[20]: ../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-title artifact directory and filename resolver"
[21]: ../../../src/main/java/com/ptaf/ui/assertions/UIAssert.java "Java-level semantic UI assertion helper"
[22]: ../../../src/main/java/com/ptaf/softassert/SoftAssertionContext.java "Thread-local soft failure collector"
[23]: ../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java "Extent soft-failure report listener"
[24]: ../../../src/test/resources/extent.properties "Timestamped Extent report configuration"
[25]: ../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java "Dedicated UI performance Cucumber glue"
[26]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature Extent report listener"
