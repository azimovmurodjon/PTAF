# FNB-ETAF Mobile Browser Automation

## Purpose and scope

This guide describes the repository's **Playwright mobile-browser emulation** layer. Its intended use is responsive web validation in a Playwright browser context configured with a named phone or tablet profile. A profile supplies the browser engine, CSS viewport, screen size, device-scale factor, mobile and touch flags, user agent, platform, category, and default orientation. It is therefore appropriate for browser-rendered web journeys and visual comparisons, not for device-native behavior. [1] [2]

> **Scope boundary:** Mobile-browser emulation does **not** boot an Android emulator, an iOS Simulator, a physical device, an installed application, or an Appium session. The Appium layer is separately responsible for native-app and real-device/browser scenarios. [3] [4]

### Current implementation status

The repository contains the emulation profile repository, execution controls, Playwright context application logic, a visual-comparison step, example resources, and baseline storage. However, the checked-in Playwright lifecycle does **not currently activate this layer end to end**. `com.ptaf.utils.BrowserFactory` has `createBrowser(String profileName)`, which resolves a profile and applies it to the current thread, but `com.ptaf.hooks.Hooks#createBrowserStack` accepts only the four desktop browser names and calls `createBrowser(BrowserTypeEnum)`. A profile name set in `config.yml` consequently reaches the `Hooks` desktop switch and fails as an unsupported browser type before the profile overload can be used. [5] [6]

There is also no committed runner selecting `@mobile_browser` or the `src/test/resources/features/mobile_browser` directory. The default Maven suite selects `com.ptaf.runner.TestRunner`, whose Cucumber options select `@eStore`; the dedicated `com.ptaf.runners.MobileTestRunner` instead selects the Appium-oriented `@theapp_smoke` scenarios under `features/mobile`. [7] [8] As a result, this guide documents the repository's actual configuration contract, reusable feature patterns, artifacts, and the integration limits that must be resolved before a true emulation run is possible. It does not represent a profile change as currently runnable when the source does not support it.

## Architecture and source locations

| Area | Exact package, class, or resource path | Responsibility and observed behavior |
|---|---|---|
| Profile resources | [`src/test/resources/mobile_browser/config/mobile-browser-profiles.yml`][1] | Defines named phone and tablet profiles. Each profile has `browser_engine`, `platform`, `device_category`, `orientation`, viewport and screen dimensions, `device_scale_factor`, `is_mobile`, `has_touch`, and `user_agent`. |
| Emulation execution resources | [`src/test/resources/mobile_browser/config/mobile-browser-execution.yml`][2] | Defines the `mobile_browser` switch, orientation override, evidence settings, visual-baseline settings, paths, and threshold. |
| Mobile-browser YAML loader | `com.ptaf.ui.mobilebrowser.MobileBrowserYamlReader` in [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java`][9] | Recursively loads `.yml` and `.yaml` files from the classpath `mobile_browser` folder and recursively merges maps. It is intentionally separate from the general `YamlReader`. |
| Profile lookup and model | `com.ptaf.ui.mobilebrowser.MobileBrowserProfileRepository` in [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java`][10] and `MobileBrowserProfile` in [`MobileBrowserProfile.java`][26] | Looks up profile names case-insensitively with collapsed whitespace, then exposes immutable device settings. Missing or malformed values receive documented defaults. |
| Execution settings accessor | `com.ptaf.ui.mobilebrowser.MobileBrowserExecutionConfig` in [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java`][11] | Reads `mobile_browser.*` controls from the mobile-browser YAML resources and supplies defaults. |
| Playwright profile application | `com.ptaf.utils.BrowserFactory` in [`src/main/java/com/ptaf/utils/BrowserFactory.java`][5] | `createBrowser(String)` chooses WebKit, Firefox, or Chromium from the profile. `createContextWithVideo` applies the active profile's viewport, screen, scale, mobile, touch, and user-agent values. |
| Current lifecycle entry point | `com.ptaf.hooks.Hooks` in [`src/main/java/com/ptaf/hooks/Hooks.java`][6] | Creates the normal Playwright `Browser`, `BrowserContext`, and `Page`. Its browser switch presently permits only `CHROME`, `FIREFOX`, `WEBKIT`, and `EDGE`; it does not call the profile-string overload. |
| Visual test glue | `com.ptaf.stepdefinitions.MobileBrowserVisualSteps` in [`src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java`][12] | Implements `Then I compare mobile browser page with visual baseline "{name}"` and delegates to the visual validator using the active `Page` and configured browser name. |
| Visual comparison | `com.ptaf.ui.mobilebrowser.MobileBrowserVisualValidator` in [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java`][13] | Takes a full-page PNG, creates or reads a profile-specific baseline, performs a pixel comparison, writes actual and diff PNGs, attaches configured images, and asserts the mismatch threshold. |
| Optional evidence utility | `com.ptaf.ui.mobilebrowser.MobileBrowserEvidenceManager` in [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java`][14] | Contains conditional full-page screenshot capture logic for a recognized profile. No checked-in lifecycle call site invokes this utility, so its screenshot controls are not presently automatic. |
| Feature and baseline examples | [`src/test/resources/features/mobile_browser/mobile_browser_visual_sample.feature`][15] and [`src/test/resources/baselines/mobile_browser/`][16] | The feature is a visual-step example. Baselines are organized beneath a sanitized browser-profile directory. |
| Shared web feature support | `com.ptaf.stepdefinitions.PageCommonSteps` [`PageCommonSteps.java`][27], `FrameCommonSteps` [`FrameCommonSteps.java`][17], `com.ptaf.ui.pages.PageCommonMethods` [`PageCommonMethods.java`][28], and `com.ptaf.ui.handlers.LocatorHandler` [`LocatorHandler.java`][23] | Supplies common Playwright page actions, navigation, YAML locator resolution, and locator-type mapping. These are web UI utilities, not mobile-native steps. |
| Build and current runners | [`pom.xml`][18], [`src/test/resources/testng.xml`][7], `com.ptaf.runner.TestRunner` [`TestRunner.java`][8], and `com.ptaf.runners.MobileTestRunner` [`MobileTestRunner.java`][29] | Maven uses Surefire with the TestNG suite by default. The committed runners do not select the emulation sample. |

## Browser emulation versus native and real mobile-browser automation

| Concern | Playwright mobile-browser emulation in this guide | Appium native app automation | Appium real mobile-browser automation |
|---|---|---|---|
| Driver and session | Playwright `Browser` and `BrowserContext` configured from a YAML profile. | `AppiumDriver` created by `MobileDriverManager.startDriver`. | `AppiumDriver` created by `MobileDriverManager.startBrowserDriver`. |
| Test target | A web page rendered by a desktop-hosted Playwright engine with mobile context properties. | An installed Android or iOS application. | Chrome on Android or Safari on iOS available on a device, emulator, or simulator. |
| Resource root | `src/test/resources/mobile_browser/` | `src/test/resources/mobile/` plus `src/main/java/com/ptaf/mobile/`. | `src/test/resources/mobile/config/mobile-browser-config.yml` and [`src/test/resources/features/mobile/appium_mobile_browser_google_search.feature`][30]. |
| Tags and lifecycle | Intended tag: `@mobile_browser`; it must remain a Playwright UI scenario. | `@mobile`, `@android`, `@ios`, or `@cross_platform` causes `MobileHooks` to start Appium. | `@appium_browser` or `@mobile_browser_real` causes `MobileHooks` to start an Appium browser session. |
| Locator model | Shared Playwright web `elements.*` locator YAML and `LocatorHandler`. | Platform-specific Appium locator handling. | `mobile_elements.*` with Android/iOS/common entries under `src/test/resources/mobile/elements/`. |

`Hooks` explicitly identifies `@mobile`, `@appium_browser`, and `@mobile_browser_real` as Appium scenarios and skips Playwright initialization for them. Do not add those tags to a Playwright emulation feature. Conversely, do not use the emulation profile YAML to configure an Appium session. [3] [4]

## Prerequisites

Use **JDK 21** and Maven. The Maven compiler source and target are both 21, and the project declares Playwright Java 1.62.0, Cucumber 7.20.0, TestNG 7.10.2, and Appium Java Client 9.4.0. [18]

Install the Playwright browser binaries before attempting any Playwright browser execution. The repository README provides this command:

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Use an authorized non-production test environment and a non-sensitive URL key in `config.yml`. `BrowserFactory` only applies HTTP basic authentication when both `-Dservice.username` and `-Dservice.password` are supplied; never place either value in a feature, profile, locator, baseline, report, or committed YAML file. [5]

Before a future emulation run, confirm that the selected profile exists in `mobile-browser-profiles.yml`, `mobile_browser.enabled` is true, the target browser engine is installed, and the code/runner integration limitations in [Current implementation status](#current-implementation-status) have been addressed. The profile repository normalizes profile names, but `Hooks` does not currently pass a profile name into it. [5] [10]

## Configuration files and key settings

### Global UI configuration

[`src/test/resources/config/config.yml`][19] is read by `com.ptaf.utils.ConfigurationProperties`. Its `browser` key is documented as the profile selector for mobile-browser emulation, while `headless`, `ignoreHTTPSErrors`, `runtimeWait`, `videoCapture`, and `maximize_browser` are general Playwright controls. [19] [20]

The following is an **intended profile-selection example**. It uses placeholders only and will not activate a mobile profile until `Hooks` is integrated with `BrowserFactory.createBrowser(String)`.

```yaml
# src/test/resources/config/config.yml
browser: "<mobile-browser-profile-name>"
headless: "<true-or-false>"
ignoreHTTPSErrors: "<true-or-false>"
runtimeWait: <positive-seconds>
maximize_browser: <true-or-false>
```

When a profile is active, `BrowserFactory` deliberately ignores `maximize_browser` so the device viewport remains profile-controlled. It resolves `headless` first from `-Dheadless`, then from `config.yml`, and otherwise defaults to headed mode. [5]

### Profile catalog

Profiles reside under the `mobile_browser_profiles` root in [`mobile-browser-profiles.yml`][1]. Profile lookup ignores case, trims and collapses whitespace, and returns no match for a missing name. For a custom profile, preserve the same schema and choose a unique human-readable key. The following is a schema example, not a production device definition.

```yaml
mobile_browser_profiles:
  <mobile-browser-profile-name>:
    browser_engine: "<chromium-or-webkit-or-firefox>"
    platform: "<platform-name>"
    device_category: "<phone-or-tablet>"
    orientation: "<portrait-or-landscape>"
    viewport_width: <css-viewport-width>
    viewport_height: <css-viewport-height>
    screen_width: <screen-width>
    screen_height: <screen-height>
    device_scale_factor: <device-pixel-ratio>
    is_mobile: <true-or-false>
    has_touch: <true-or-false>
    user_agent: "<mobile-user-agent>"
```

When an active profile reaches `BrowserFactory`, the selected engine determines the launched Playwright browser. The context receives the profile viewport, screen size, device scale factor, `isMobile`, `hasTouch`, and—when nonblank—user agent. An execution orientation of `portrait` or `landscape` swaps width and height when necessary; `profile` retains the profile geometry. [5]

### Mobile-browser execution controls

[`mobile-browser-execution.yml`][2] owns emulation-specific settings. The loader merges all YAML files below `src/test/resources/mobile_browser`; avoid duplicate keys across such files unless an override is deliberate. [9]

```yaml
mobile_browser:
  enabled: <true-or-false>
  orientation: "<profile-or-portrait-or-landscape>"
  evidence:
    output_directory: "<relative-evidence-directory>"
    screenshot_on_failure: <true-or-false>
    screenshot_on_pass: <true-or-false>
    screenshot_after_each_scenario: <true-or-false>
    attach_screenshots_to_report: <true-or-false>
    video_recording_enabled: <true-or-false>
    video_size_width: <video-width>
    video_size_height: <video-height>
  visual:
    enabled: <true-or-false>
    baseline_directory: "<relative-baseline-directory>"
    output_directory: "<relative-visual-output-directory>"
    mismatch_threshold_percent: <allowed-mismatch-percent>
    create_baseline_if_missing: <true-or-false>
    attach_artifacts_to_report: <true-or-false>
```

`MobileBrowserExecutionConfig` gives absent values defaults. The profile enablement switch defaults to true; orientation defaults to `profile`; screenshots-on-failure defaults to true; video defaults to false; and visual testing defaults to enabled. [11] The visual validator calculates mismatch as `mismatchedPixels × 100 / comparedPixels`. Therefore, the committed value `0.10` is compared as **0.10 percent**, not as a fraction of one. [2] [13]

For controlled baseline creation, permit `create_baseline_if_missing` only while establishing approved baselines. With it enabled, a missing expected image is created from the current page and the comparison returns without a mismatch assertion. With it disabled, a missing baseline fails the scenario. [13]

## Feature, locator, data, payload, and query locations

| Asset | Location | How this module uses it |
|---|---|---|
| Mobile-emulation feature files | `src/test/resources/features/mobile_browser/` | Contains the visual sample. A future Playwright emulation feature belongs here and should use `@mobile_browser`, not Appium tags. [15] |
| Shared web feature files | `src/test/resources/features/` | Existing generic Playwright scenarios and shared step patterns live here. A normal runner scans this tree, subject to its fixed tag expression. [8] |
| Playwright web locators | `src/test/resources/elements/*.yml` | General `YamlReader` scans `elements`, and `ElementLocatorHelper` resolves `elements.<logical-page>.<logical-locator>`. These are the appropriate locator resources for emulated web pages. [21] [22] |
| Appium mobile locators — out of scope | `src/test/resources/mobile/elements/` | Intended for the Appium mobile layer. They are not read by the Playwright `YamlReader` and must not be substituted for web locator YAML. [3] [21] |
| Mobile profile and control resources | `src/test/resources/mobile_browser/config/` | Read only by `MobileBrowserYamlReader`, not by the general `YamlReader`. [1] [2] [9] |
| Visual baselines | `src/test/resources/baselines/mobile_browser/<sanitized-profile>/<sanitized-baseline>.png` | Expected full-page image for `MobileBrowserVisualValidator`. Profile and baseline names have unsafe characters replaced with underscores. [13] [16] |
| General test data | `src/test/resources/data/` and `src/test/resources/testdata.xlsx` | General framework resources; no mobile-browser-specific data reader or data directory is present. |
| Performance payloads | `src/test/resources/performance/payloads/` | Belong to the performance module, not Playwright mobile-browser emulation. [21] |
| Database queries | `src/test/resources/queries/db_queries.yml` | Belong to database steps. `YamlReader` can load this directory, but the mobile-browser layer does not consume query definitions. [21] |
| API request definitions | `src/test/resources/api_requests/` | Loaded by the general YAML reader for API work; not a mobile-browser input. [21] |

A web locator definition uses the `TYPE_value` (or `TYPE value`) form. `ElementLocatorHelper` splits on the first underscore or space, and `LocatorHandler` maps the type to a Playwright locator. Supported examples include `CSS`, `XPATH`, `BUTTON`, `TEXTBOX`, `TEXT`, `TESTID`, `LABEL`, `PLACEHOLDER`, `ID`, `NAME`, and `CLASS`. [22] [23]

```yaml
# src/test/resources/elements/<page-file>.yml
elements:
  <logical-page>:
    <primary-action>: "TESTID_<test-id>"
    <form-field>: "LABEL_<accessible-label>"
    <status-message>: "CSS_<css-selector>"
```

Use stable, web-accessible selectors. A profile changes the context characteristics, not the locator repository or the Gherkin action vocabulary.

## Build and exact run commands

The following build check was validated against the repository on 2026-09-23 and completed successfully:

```bash
cd /home/ubuntu/PTAF_dev_ui_performance_video_fix_2026-09-23
mvn -DskipTests test-compile
```

The normal Maven suite command is:

```bash
mvn clean test
```

`pom.xml` directs Surefire to `src/test/resources/testng.xml`, which names `com.ptaf.runner.TestRunner`. That runner scans `src/test/resources/features`, uses `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and has `tags = "@eStore"`. It is a normal framework command, but it is **not a mobile-browser-emulation command**. [7] [8] [18]

The existing dedicated mobile command is:

```bash
mvn -Dtest=com.ptaf.runners.MobileTestRunner test
```

This runner is also **not** a Playwright emulation command: it scans `src/test/resources/features/mobile`, selects `@theapp_smoke`, and uses the Appium lifecycle for tagged mobile scenarios. Do not use it to validate `src/test/resources/features/mobile_browser/mobile_browser_visual_sample.feature`. [3] [29]

There is **no exact committed normal-run command that can execute the emulation sample as an emulated profile**. A working execution path requires both of the following production changes, which are deliberately outside this documentation task:

1. Update `com.ptaf.hooks.Hooks#createBrowserStack` to recognize a configured `MobileBrowserProfileRepository` profile and call `BrowserFactory.createBrowser(String)` before context creation.
2. Add or update a Playwright Cucumber runner/TestNG suite so it selects `@mobile_browser` and/or `src/test/resources/features/mobile_browser`, while retaining `com.ptaf.stepdefinitions` and `com.ptaf.hooks` glue.

Do not imply that setting `browser: "<mobile-browser-profile-name>"` and running `mvn clean test` solves these gaps; source inspection shows that it does not. [5] [6] [7] [8]

## Creating a new mobile-browser feature or test

### Source-supported feature pattern

The existing sample establishes the profile-specific visual step pattern. It assumes an active Playwright `Page`, which the current framework will supply only after the lifecycle integration described above exists. Use a descriptive baseline name, because the visual validator converts it to a filename. [12] [13] [15]

```gherkin
@mobile_browser @visual @smoke
Feature: <mobile-web-journey-name>

  Scenario: Compare the rendered mobile web page with its approved baseline
    Then I compare mobile browser page with visual baseline "<baseline-name>"
```

A broader web interaction pattern can reuse `PageCommonSteps` actions such as fill, click, visibility assertion, key press, and explicit screenshot. Keep it in the Playwright web locator model shown above. [27]

```gherkin
@mobile_browser @smoke
Feature: <responsive-mobile-web-journey>

  Scenario: Complete a responsive web action
    Given we navigate to <configured-url-key> url
    Then we verify on page <logical-page> of locator <start-control> is visible
    When we enter value on page <logical-page> locator <input-field> value "<test-value>"
    Then we click on page <logical-page> locator <primary-action>
    Then we verify on page <logical-page> of locator <expected-result> is visible
    Then I compare mobile browser page with visual baseline "<baseline-name>"
```

The navigation sentence is exact shared glue from `FrameCommonSteps`, but it presently calls `activePage.setViewportSize(1920, 1080)` after navigation. That is incompatible with retaining a mobile profile viewport. Do **not** treat the second example as a valid profile-preserving execution until that implementation is changed or an alternative profile-safe navigation step is provided. [17]

### Recommended creation sequence

After the lifecycle and runner gaps are resolved, add the profile only when its engine and device characteristics are required. Place the feature in `src/test/resources/features/mobile_browser/`, use `@mobile_browser`, and keep it free of `@mobile`, `@android`, `@ios`, `@appium_browser`, and `@mobile_browser_real`. Create or extend a file under `src/test/resources/elements/` for browser locators. Add a visual baseline through the controlled baseline-creation workflow, then turn off automatic baseline creation in CI. [1] [3] [13] [21]

A feature file alone is insufficient. Confirm that its runner reaches the feature, its tags do not trigger Appium, the profile name resolves, and the Playwright lifecycle calls the profile-based factory overload. This sequencing prevents a desktop run, a real-device run, and a mobile-emulation run from being mistaken for one another.

## Expected reports and artifacts

| Output | Expected location | Conditions and notes |
|---|---|---|
| Cucumber HTML, JSON, and rerun output from the TestNG runner | `target/cucumber-reports/cucumber-pretty`, `target/cucumber-reports/CucumberTestReport.json`, and `target/cucumber-reports/rerun.txt` | Configured by `com.ptaf.runner.TestRunner`. These appear only for scenarios selected by that runner. [8] |
| Extent combined reports | `test-output/<timestamp>/SparkReport/Spark.html`, `Base64Report/Report.html`, `PdfReport/FNB-PTAF-Report.pdf`, and `ExcelReport/FNB-PTAF-Report.xlsx` | Defined by `src/test/resources/extent.properties`. The Extent adapter must be present in the active runner. [24] |
| Per-feature reports | `test-output/per-feature-reports/<feature>_<timestamp>.html`; optional PDFs under the configured per-feature locations | Controlled by `reporting.*` in `config.yml` and `com.ptaf.reporting.PerFeatureReportListener`. The committed config enables per-feature HTML and Glass-style PDF generation. [19] [25] |
| Visual actual image | `<visual.output_directory>/<run-id>/<sanitized-profile>/<sanitized-baseline>-actual.png` | Written whenever the visual step runs with visual checks enabled. [13] |
| Visual diff image | `<visual.output_directory>/<run-id>/<sanitized-profile>/<sanitized-baseline>-diff.png` | Written only when an existing baseline is compared. It highlights differing pixels in red. [13] |
| Created or approved baseline | `<visual.baseline_directory>/<sanitized-profile>/<sanitized-baseline>.png` | Created on first comparison only if `create_baseline_if_missing` is true. This changes a tracked resource path and should be reviewed as a baseline update. [13] |
| Profile video | `test-output/mobile-browser-evidence/<run-id>/videos/` | Created only if an active profile reaches `BrowserFactory`, `video_recording_enabled` is true, and Playwright closes the browser/context to finalize the file. Current `Hooks` does not activate profiles. [5] [11] |
| Mobile-browser evidence screenshot | `<evidence.output_directory>/<run-id>/screenshots/<safe-scenario-name>.png` | This is the path implemented by `MobileBrowserEvidenceManager`, but no current lifecycle call invokes it. Its presence should not be expected without integration. [14] |

Visual attachments use `Scenario.attach` when configured. Whether they are visible in a final report also depends on the selected runner registering a compatible Cucumber/Extent reporting plugin. [8] [13] [25]

## Troubleshooting

| Symptom | Code-supported cause | Resolution or diagnostic |
|---|---|---|
| `Unsupported browser type: <profile>` during setup | `Hooks#createBrowserStack` switches only on `CHROME`, `FIREFOX`, `WEBKIT`, and `EDGE`; it never calls the profile-string factory method. | Do not retry as a configuration issue. Add the missing profile-detection/factory integration before attempting emulation. [5] [6] |
| `mvn clean test` runs no intended mobile-browser scenarios | The default TestNG suite points to `com.ptaf.runner.TestRunner`, whose tag expression is `@eStore`; no committed runner selects `@mobile_browser`. | Use the compilation check for source validation. Create or adjust an appropriate Playwright runner and suite before treating the visual feature as runnable. [7] [8] |
| A scenario starts Appium or asks for a device instead of Playwright | Its tags match `MobileHooks` mobile/Appium tags, including `@mobile`, `@appium_browser`, or `@mobile_browser_real`. | Remove those tags from an emulation scenario. Use them only for Appium native-app or real mobile-browser execution. [3] |
| The page becomes desktop-sized after navigation | The reusable `we navigate to <key> url` step calls `setViewportSize(1920, 1080)` after navigating. | Use a profile-safe navigation implementation before adding that step to an emulation journey. Do not attempt to correct this by changing the device profile dimensions. [17] |
| No automatic evidence screenshot appears | `MobileBrowserEvidenceManager` contains the capture method, but source search finds no invocation from `Hooks` or another lifecycle class. | Wire the manager into an appropriate Playwright teardown only after profile activation is implemented. Until then, use explicit shared screenshot steps for diagnostic work. [14] [17] |
| No video appears | Profile video configuration is evaluated only when a mobile profile is active; recording finalizes after browser closure. | First resolve the profile activation gap, then enable `mobile_browser.evidence.video_recording_enabled` and allow normal teardown to close the browser. [5] [11] |
| Visual check creates an image instead of failing on first run | `create_baseline_if_missing` is enabled. The validator writes the current screenshot as the expected baseline and returns. | Review and approve the created baseline, then disable automatic creation in CI so a missing expected image fails. [2] [13] |
| Visual mismatch appears unexpectedly | The validator compares full-page screenshots at pixel level. Viewport, device scale, orientation, page state, dynamic content, and font/rendering changes all affect the image. | Verify the selected profile and orientation, stabilize test data/page state, inspect `-actual.png` and `-diff.png`, then review the threshold only with an approved visual-testing policy. [5] [13] |
| Baseline cannot be found under the profile name shown in YAML | The validator sanitizes profile and baseline names, replacing characters outside `A–Z`, `a–z`, `0–9`, `.`, `_`, and `-` with underscores. | Use the sanitized directory and filename convention under `src/test/resources/baselines/mobile_browser/`. [13] |
| Locator cannot be resolved | Playwright web locators must be loaded under `elements.<logical-page>.<logical-locator>`. Appium `mobile_elements` are a different repository. | Check the exact logical names, `TYPE_value` syntax, and the resource root. Keep Appium locators out of the emulation feature. [21] [22] [23] |

## Module boundaries and ownership

The `com.ptaf.ui.mobilebrowser` package is a **Playwright browser-context support module**. It does not own business navigation, API payloads, database queries, Appium drivers, app packages, or native permissions. Its appropriate inputs are profile and execution YAML, an active Playwright `Page`, a configured browser/profile name, and optional visual baselines. Its output is a browser-context configuration plus visual/evidence artifacts when its utilities are actually integrated into the lifecycle. [5] [9] [11] [13]

The general web UI module owns reusable browser steps, page actions, locator parsing, and locator mappings. It reads `config` and `elements` using `com.ptaf.utils.YamlReader`. Mobile-browser YAML is not in that reader's folder list; `MobileBrowserYamlReader` owns it separately. Preserve this division rather than copying mobile-browser profile keys into generic UI locator YAML. [9] [17] [21] [22]

The Appium module owns `com.ptaf.hooks.MobileHooks`, `com.ptaf.mobile.*`, mobile capabilities, device/emulator/simulator sessions, native permissions, and real-device browser behavior. The real-browser feature and `mobile-browser-config.yml` are Appium assets even though they exercise web content. They must remain distinct from the Playwright emulation resources in this guide. [3] [4]

API, database, and performance modules have their own request, query, and payload resources. A responsive web feature can coordinate with those modules at a framework level, but the mobile-browser profile layer does not load or interpret those assets. [21]

## References

[1]: ../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml "Playwright mobile-browser emulation profiles"
[2]: ../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml "Mobile-browser execution controls"
[3]: ../../src/main/java/com/ptaf/hooks/MobileHooks.java "Appium mobile lifecycle hooks"
[4]: ../../src/test/resources/mobile/config/mobile-browser-config.yml "Appium real mobile-browser capability configuration"
[5]: ../../src/main/java/com/ptaf/utils/BrowserFactory.java "Playwright browser and mobile-profile factory"
[6]: ../../src/main/java/com/ptaf/hooks/Hooks.java "Playwright scenario lifecycle hooks"
[7]: ../../src/test/resources/testng.xml "Default TestNG suite"
[8]: ../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[9]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java "Mobile-browser YAML resource loader"
[10]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java "Mobile-browser profile repository"
[11]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java "Mobile-browser execution configuration accessors"
[12]: ../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java "Mobile-browser visual Cucumber steps"
[13]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java "Pixel-based mobile-browser visual validation"
[14]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java "Mobile-browser evidence screenshot manager"
[15]: ../../src/test/resources/features/mobile_browser/mobile_browser_visual_sample.feature "Mobile-browser visual feature sample"
[16]: ../../src/test/resources/baselines/mobile_browser/Galaxy_S25_Ultra_Chrome/visual-smoke-example.png "Example mobile-browser visual baseline"
[17]: ../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java "Shared Playwright navigation and frame steps"
[18]: ../../pom.xml "FNB-ETAF Maven build and test configuration"
[19]: ../../src/test/resources/config/config.yml "Global UI and reporting configuration"
[20]: ../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "Framework configuration accessor"
[21]: ../../src/main/java/com/ptaf/utils/YamlReader.java "General framework YAML reader"
[22]: ../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java "YAML element locator helper"
[23]: ../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java "Playwright locator-type handler"
[24]: ../../src/test/resources/extent.properties "Extent report output configuration"
[25]: ../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature reporting listener"
[26]: ../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java "Mobile-browser emulation profile model"
[27]: ../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java "Shared Playwright page Cucumber steps"
[28]: ../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java "Shared Playwright page actions"
[29]: ../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Appium-focused mobile Cucumber runner"
[30]: ../../src/test/resources/features/mobile/appium_mobile_browser_google_search.feature "Appium real mobile-browser feature example"
