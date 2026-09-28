# Mobile Browser Automation Reference

## Purpose, terminology, and current status

FNB-ETAF contains **two deliberately separate mobile web automation paths**:

1. **Appium real mobile browser** — an Appium session drives Chrome on Android or Safari on iOS on an available emulator, simulator, or physical device. It uses the `com.ptaf.mobile` driver, action, locator, and evidence stack.
2. **Playwright mobile-browser emulation** — a Playwright `BrowserContext` is intended to use a named phone/tablet profile for engine selection, CSS viewport, screen size, device scale factor, mobile/touch flags, user agent, and visual comparison. It is responsive-web emulation, **not** a native simulator or device session. [3] [4]

> **Important current-state limitation — Playwright emulation is not wired end to end.** `BrowserFactory.createBrowser(String)` can resolve a mobile profile and launch its preferred Playwright engine, but the checked-in `Hooks#createBrowserStack` only accepts `CHROME`, `FIREFOX`, `WEBKIT`, and `EDGE`, then calls the desktop-enum overload. A profile name placed in `config.yml` therefore reaches the desktop switch and fails as an unsupported browser type; it does not activate the profile. In addition, no checked-in runner selects the `@mobile_browser` sample or the `features/mobile_browser` directory. This chapter distinguishes **implemented reusable support** from **currently runnable execution paths**. [3] [4] [18] [19] [20]

The build declares Playwright Java 1.62.0, Appium Java Client 9.4.0, Cucumber 7.20.0, TestNG 7.10.2, and Java 21 compilation settings. [1]

## Source map and responsibility boundaries

| Area | Main source path | Responsibility |
|---|---|---|
| Appium browser configuration | [`src/test/resources/mobile/config/mobile-browser-config.yml`][6] | Browser-session settings for Android Chrome and iOS Safari. |
| Shared Appium settings | [`src/test/resources/mobile/config/mobile-config.yml`][7] | Server endpoint, default platform, waits, permission handling, and Appium evidence controls. |
| Appium configuration access | [`MobileConfigurationProperties.java`][8] | Resolves shared values and split browser settings, preferring `mobile_browser_appium.*` over legacy `mobile.browser.*`. |
| Appium lifecycle | [`MobileHooks.java`][9], [`MobileDriverManager.java`][10], [`MobileDriverFactory.java`][11] | Selects platform, starts/owns thread-local browser drivers, applies capabilities, performs best-effort clean start, and quits sessions. |
| Real-browser actions and context recovery | [`MobileCommonMethods.java`][12] | Navigates, waits for iOS Safari `WEBVIEW`, uses the optional native address-bar fallback, and saves page source. |
| Real-browser Gherkin and locators | [`appium_mobile_browser_google_search.feature`][13], [`google_mobile_browser_elements.yml`][14], [`MobileSteps.java`][15] | Provides a real-browser scenario example, platform-aware locator data, and reusable Cucumber glue. |
| Playwright profile resources | [`mobile-browser-profiles.yml`][2], [`mobile-browser-execution.yml`][5] | Defines named device profiles and emulation-specific execution, evidence, orientation, and visual settings. |
| Profile/configuration support | [`MobileBrowserYamlReader.java`][21], [`MobileBrowserProfileRepository.java`][22], [`MobileBrowserExecutionConfig.java`][23] | Merges mobile-browser YAML, resolves profiles, and supplies typed settings/defaults. |
| Playwright profile/context support | [`BrowserFactory.java`][4] | Has the profile-aware launch overload and applies an active profile to a Playwright context. |
| Visual/evidence support | [`MobileBrowserVisualSteps.java`][25], [`MobileBrowserVisualValidator.java`][26], [`MobileBrowserEvidenceManager.java`][27] | Compares full-page PNGs and defines optional profile-specific screenshot capture. |
| Playwright lifecycle and runners | [`Hooks.java`][3], [`testng.xml`][18], [`com.ptaf.runner.TestRunner`][19], [`MobileTestRunner.java`][20] | Shows the current profile-activation and scenario-selection gaps. |

Do not use a Playwright emulation profile to configure Appium, and do not use Appium `mobile_elements` resources as Playwright web locators. The paths have different session types, lifecycle hooks, configuration readers, and locator handling. [3] [8] [9] [14] [24]

## Choosing the correct path

| Need | Use | What is actually exercised | Do not assume |
|---|---|---|---|
| Validate Android Chrome/Safari behavior on a device-like target, browser first-run state, device browser UI, or iOS web-context behavior | **Appium real mobile browser** | Appium `AndroidDriver`/`IOSDriver`, UiAutomator2/XCUITest, and the installed Chrome/Safari browser. [11] | That a CSS viewport/user-agent profile makes this a real-device test. |
| Responsive rendering, profile-specific viewport/touch/user-agent settings, or pixel-baseline comparison | **Playwright mobile-browser emulation** | A Playwright engine and `BrowserContext` settings when the profile-aware factory is reached. [4] | That it starts Android/iOS hardware, a simulator, Appium, or a native browser binary. [2] |
| Installed native app behavior, permissions, device APIs, or app package lifecycle | **Appium native mobile** | The native-app driver path, not browser mode. | That either browser path automates a native application. [9] [11] |

### Tag and hook routing

`MobileHooks` treats `@mobile`, `@android`, `@ios`, `@cross_platform`, `@appium_browser`, and `@mobile_browser_real` as mobile/Appium tags. Explicit `@appium_browser` and `@mobile_browser_real` start an Appium **browser** session; other mobile tags normally start a native-app session. `Hooks` recognizes the same Appium-oriented tags (and any feature under `features/mobile/`) and skips Playwright startup so a second browser is not created. [3] [9]

The `@mobile_browser` tag is deliberately **not** considered browserless by `Hooks`, because it is intended to remain a Playwright UI scenario. Keep Playwright-emulation features free of the Appium tags above. [3]

## Appium real mobile browser

### Session flow

For a tagged Appium browser scenario, the operational sequence is:

1. `MobileHooks` stores the Cucumber scenario for evidence, resolves the platform, selects browser mode, and calls `MobileDriverManager.startBrowserDriver(platform)`. [9]
2. Platform resolution is, in order: `-Dmobile.platform=<android-or-ios>`, `@android`/`@ios`, then `mobile.default_platform`. Supplying both platform tags is an error. [9] [7]
3. `MobileDriverManager` closes any existing thread-local driver, creates a browser driver through `MobileDriverFactory`, records the platform, and marks the thread as a browser session. [10]
4. The factory creates `AndroidDriver` with UiAutomator2 options for Android or `IOSDriver` with XCUITest options for iOS, applies browser capabilities, then performs best-effort clean-start handling and initial navigation when configured. [11]
5. Gherkin steps use `MobileActionImpl`/`MobileCommonMethods` with the active driver. On teardown, `MobileHooks` captures configured evidence, stops optional screen recording, quits the driver, and clears thread-local state. [9] [10] [15] [16]

The Appium server command recorded in the browser configuration is:

```bash
# Required only where the Appium environment and policy permit it.
appium --relaxed-security
```

`--relaxed-security` is material when Android `reset_app_data` is enabled because PTAF uses `mobile: shell` to issue `pm clear <browser-package>`. The implementation treats a failure as non-fatal and logs a warning; alternatively leave `reset_app_data: false`. [6] [11]

### Configuration: shared settings and browser capabilities

The mobile YAML reader loads only `mobile/config` and `mobile/elements` classpath folders, recursively deep-merges mapping values, and intentionally avoids YAML files inside mobile application bundles. Browser lookup first checks the split `mobile_browser_appium.<platform>.<key>` location and falls back to legacy `mobile.browser.<platform>.<key>`. [8] [28]

#### Shared Appium keys

| Key | Current value/role | Operational effect |
|---|---|---|
| `mobile.enabled` | `true` in the supplied shared config | Required by `MobileDriverManager`; false prevents driver start. |
| `mobile.appium_server_url` | Configured local Appium endpoint | Used to create the Appium session; malformed values fail driver creation. |
| `mobile.default_platform` | `android` | Used only when no command-line override or platform tag is present. |
| `mobile.explicit_wait_seconds` | `30` | Default explicit wait used by the mobile layer. |
| `mobile.implicit_wait_seconds` | `0` | Factory applies an implicit wait only when the configured value is greater than zero. |
| `mobile.new_command_timeout_seconds` | `300` | Sent to browser-driver options as the Appium new-command timeout. |
| `mobile.permissions.*` | popup timeout, maximum dialogs, evidence toggle | Controls optional permission-dialog handling, not Playwright emulation. |
| `mobile.evidence.*` | output, screenshots, native video/report attachment controls | Drives `MobileEvidenceManager` for both native-app and Appium-browser sessions. [7] [8] [9] [11] [16] |

#### `mobile_browser_appium` keys that the browser factory reads

| Platform | Keys in the committed browser config | Factory behavior |
|---|---|---|
| `android` | `automation_name`, `platform_name`, `device_name`, `platform_version`, `udid`, `orientation`, `browser_name`, `no_reset`, `full_reset`, `chromedriver_autodownload`, `chromedriver_executable`, `chromedriver_mapping_file`, `auto_grant_permissions` | Builds UiAutomator2 options, sets `browserName`, applies nonblank version/UDID/ChromeDriver paths, sets named booleans when present, and sends supported orientation. [6] [11] |
| `ios` | `automation_name`, `platform_name`, `device_name`, `platform_version`, `udid`, `orientation`, `browser_name`, `no_reset`, `full_reset`, `initial_url`, `auto_accept_alerts`, `auto_dismiss_alerts`, `include_safari_in_webviews`, `connect_hardware_keyboard`, `safari_allow_popups`, `safari_ignore_fraud_warning` | Builds XCUITest options, sets `browserName`, sends `safariInitialUrl` from `initial_url`, applies the listed Safari/alert booleans when present, and sends supported orientation. [6] [11] |
| Both | `clean_start_enabled`, `clear_cookies`, `close_existing_tabs`, `terminate_before_start`, `activate_after_cleanup`, plus browser package/bundle identifiers | Drives best-effort clean-start policy. Cookie deletion is attempted if enabled. Android app-data reset is implemented only for Android when `reset_app_data` is true. [6] [11] |

For iOS, `web_context_timeout_seconds` controls how long PTAF polls for and switches to a context containing `WEBVIEW`; `safari_native_navigation_fallback_enabled` controls the native Safari address-bar navigation fallback. [6] [12]

> **Source-visible clean-start limitation:** `close_existing_tabs` is logged but no tab-closing operation is implemented in `MobileDriverFactory`. Also, configured terminate/activate operations are intentionally skipped for native Android Chrome and iOS Safari to avoid invalidating their WebDriver session. iOS `reset_app_data` is present in YAML but the actual `mobile: shell pm clear` implementation is Android-only. Treat clean start as best effort, not as a guarantee of a blank browser profile. [6] [11]

### Browser navigation, Safari context handling, and page source

`When I open mobile browser url "<approved-test-url>"` calls `driver.get`, pauses briefly, checks the reported URL, retries with `driver.navigate().to` when the host does not match, and—only for an iOS browser session with fallback enabled—uses the native Safari address/search field. The fallback switches to `NATIVE_APP`, attempts configured and generic address-bar locators, types the URL, presses Return, waits, and lets the caller reacquire a web context. [12] [15]

For iOS Safari browser sessions, DOM operations call `ensureBrowserWebContextReadyIfNeeded()`. It waits up to `web_context_timeout_seconds`, looks for a context containing `WEBVIEW`, and switches to it if necessary. A timeout logs a diagnostic rather than throwing immediately; subsequent browser/DOM operations can still fail. The implementation’s diagnostic recommends checking simulator state, first-run Safari screens, WebKit automation, and `include_safari_in_webviews`. [6] [12]

The following source-supported Gherkin pattern uses placeholders only:

```gherkin
@mobile @appium_browser @evidence
Feature: Approved real mobile web journey

  Scenario: Navigate, validate, and retain review evidence
    When I open mobile browser url "<approved-test-url>"
    When I allow all mobile permission popups if displayed
    When I wait up to <timeout-seconds> seconds for mobile page <page-key> locator <field-key> to be visible
    When I enter mobile value "<safe-test-value>" on page <page-key> locator <field-key>
    When I press Enter on mobile page <page-key> locator <field-key>
    When I capture mobile screenshot named "<safe-artifact-name>"
    When I save mobile browser page source to "test-output/mobile-browser-appium/<safe-page-source-name>.xml"
    Then mobile browser current url should contain "<expected-host-fragment>"
```

The included real-browser feature demonstrates the same operations—URL navigation, optional permission handling, locator waits and entry, named screenshots, page-source retention, and URL assertion. Its locators are under `mobile_elements.googleBrowser` with Android, iOS, and common XPath values. [13] [14] [15]

`I save mobile browser page source to "<path>"` creates the parent directory when needed and writes `driver.getPageSource()` to that local path. Store only authorized, non-sensitive outputs: browser page source can include user-visible values or session-related markup. [12] [15]

### Appium evidence and reports

`MobileEvidenceManager` uses one timestamp-based run ID per JVM. Explicit named screenshots and end-of-scenario screenshots are PNGs, native screen recordings are MP4s, and configured screenshot attachments are emitted through `Scenario.attach`. Failure screenshots are force-attached when `mobile.evidence.screenshot_on_failure` is true. [7] [16]

| Artifact | Implemented path/policy | Notes |
|---|---|---|
| Explicit named screenshot | `<mobile.evidence.output_directory>/<run-id>/target-output/screenshots/<safe-name>.png` | Produced by `I capture mobile screenshot named ...`; repeated use of the same name in a run writes the same path. [16] |
| Scenario-end screenshot | `<mobile.evidence.output_directory>/<run-id>/target-output/screenshots/<safe-scenario>_<type>.png` | Captured on failure, pass, or every scenario according to the evidence switches. [16] |
| Assertion-failure screenshot | `<mobile.evidence.output_directory>/<run-id>/screenshots/<safe-scenario-and-failure>.png` | Uses a different subdirectory from named/scenario-end screenshots. [16] |
| Appium screen recording | `<mobile.evidence.output_directory>/<run-id>/videos/<safe-feature>/...mp4` | Created only if recording is enabled, the driver supports `CanRecordScreen`, and `video_on_failure_only` permits retention. [16] |
| Cucumber/Extent media | Embedded screenshot bytes; optional video bytes | Attachment depends on evidence flags and the active runner/report plugins. [16] [19] |

The per-feature report listener processes Cucumber `EmbedEvent` image attachments and adds them to HTML report nodes when per-feature reporting is enabled. Its behavior supports screenshots; it does not turn unattached filesystem artifacts into embedded report media. [19] [29]

## Playwright mobile-browser emulation

### Profile catalog and profile selection contract

Profiles live below `mobile_browser_profiles` in [`mobile-browser-profiles.yml`][2]. A profile contains the following fields:

| Field | Meaning when the profile-aware context path is used |
|---|---|
| `browser_engine` | Chooses WebKit, Firefox, or Chromium in `BrowserFactory.createBrowser(String)`; unrecognized engines fall through to Chromium. |
| `viewport_width`, `viewport_height` | CSS viewport dimensions applied to `BrowserContext`. |
| `screen_width`, `screen_height` | Screen dimensions applied to `BrowserContext`. |
| `device_scale_factor` | Browser-context device pixel ratio. |
| `is_mobile`, `has_touch` | Playwright mobile and touch emulation flags. |
| `user_agent` | Applied only when nonblank. |
| `platform`, `device_category`, `orientation` | Profile descriptors for selection/reporting; context orientation is controlled by the execution setting described below. [2] [4] [22] |

The supplied catalog contains named phone and tablet profiles for iOS/WebKit and Android/Chromium, including portrait and landscape device geometries. Select the **exact profile name** through the existing top-level `browser` key in `config.yml`; the profile repository matches case-insensitively, trims surrounding whitespace, and collapses internal whitespace. [2] [17] [22]

```yaml
# Intended selector; use a profile name that exists in mobile-browser-profiles.yml.
browser: "<mobile-browser-profile-name>"
```

A malformed or missing individual profile property does not necessarily fail profile parsing: `MobileBrowserProfileRepository` uses defaults such as 390×844 viewport, scale factor 3.0, Chromium, mobile/touch true, and portrait. A missing profile name itself produces no match, and `BrowserFactory.createBrowser(String)` throws an `IllegalArgumentException`. [4] [22]

### Intended profile-to-context execution flow

When invoked directly, the profile-aware factory performs this flow:

1. Resolve the `browser` profile name through `MobileBrowserProfileRepository` and require `mobile_browser.enabled` to be true.
2. Store the resolved profile in a thread-local and launch WebKit, Firefox, or Chromium according to `browser_engine`.
3. At `createContextWithVideo`, detect the active profile, retain a profile-controlled viewport rather than maximizing, and apply viewport, screen, scale factor, `isMobile`, `hasTouch`, and nonblank user agent.
4. Create a `Page` in that context; the normal shared web steps and visual validator consume the active page. [4] [22] [23]

The normal lifecycle currently performs a different flow: it reads `ConfigurationProperties.getBrowser()`, uppercases it, permits only the four desktop enum names, calls `createBrowser(BrowserTypeEnum)`, and clears the mobile-profile thread-local in that overload. Therefore the four intended steps are **not reached from the checked-in `Hooks` lifecycle**. [3] [4]

### Viewport, device behavior, orientation, and desktop controls

`mobile_browser.orientation` accepts `profile`, `portrait`, or `landscape` and defaults to `profile`. `portrait` swaps viewport and screen width/height when the profile is wider than tall; `landscape` swaps them when the profile is taller than wide. No rotation command is sent—the setting changes Playwright context dimensions. [4] [5] [23]

`maximize_browser` is intentionally ignored when an active mobile profile exists, both during Chromium launch and browser-context setup, so profile geometry remains authoritative. General `headless` is resolved first from `-Dheadless`, then `config.yml`, then defaults to false. `ignoreHTTPSErrors` is applied to the context, and Chromium receives certificate-bypass arguments when it is true. [4] [17]

> **Viewport discrepancy to avoid:** the reusable step `Given we navigate to <key> url` navigates and then calls `activePage.setViewportSize(1920, 1080)`. This can overwrite profile-responsive geometry. Do not classify a scenario using that step as profile-faithful until a profile-safe navigation path is supplied. [30]

### Mobile-browser execution YAML

`MobileBrowserYamlReader` loads every `.yml`/`.yaml` file beneath the classpath `mobile_browser` resource folder at class initialization and recursively merges maps; later scalar values replace earlier values. Keep mobile-browser keys in `src/test/resources/mobile_browser/` and avoid accidental duplicate overrides. [5] [21]

| Key | Committed setting | Effect/default in code |
|---|---|---|
| `mobile_browser.enabled` | `true` | Allows the profile-aware browser factory; missing defaults to true. |
| `mobile_browser.orientation` | `profile` | Profile, portrait, or landscape context geometry; missing defaults to `profile`. |
| `mobile_browser.evidence.output_directory` | `test-output/mobile-browser-evidence` | Used by the standalone mobile-browser screenshot utility; default is the same. |
| `mobile_browser.evidence.screenshot_on_failure` | `true` | Requests profile screenshot capture on scenario failure if the standalone utility is called. |
| `mobile_browser.evidence.screenshot_on_pass` / `screenshot_after_each_scenario` | `false` / `false` | Optional pass/all-scenario capture policies if that utility is called. |
| `mobile_browser.evidence.attach_screenshots_to_report` | `true` | Attaches standalone utility screenshots to the Cucumber scenario when called. |
| `mobile_browser.evidence.video_recording_enabled` | `false` | Enables Playwright context recording for an active mobile profile. |
| `mobile_browser.evidence.video_size_width` / `video_size_height` | `390` / `844` | Mobile recording resolution; malformed values fall back to these defaults. |
| `mobile_browser.visual.enabled` | `true` | Makes visual comparison active; false makes the visual validator a no-op. |
| `mobile_browser.visual.baseline_directory` | `src/test/resources/baselines/mobile_browser` | Root for profile-specific expected PNGs. |
| `mobile_browser.visual.output_directory` | `test-output/mobile-browser-visual` | Root for actual/diff artifacts. |
| `mobile_browser.visual.mismatch_threshold_percent` | `0.10` | Maximum allowed mismatch **percentage**; missing/malformed default is `0.10`. |
| `mobile_browser.visual.create_baseline_if_missing` | `true` | Writes the first actual image as the baseline instead of failing. |
| `mobile_browser.visual.attach_artifacts_to_report` | `true` | Attaches baseline, actual, and diff images when an existing baseline is compared. [5] [23] |

Because the validator calculates `mismatchedPixels * 100 / comparedPixels`, the committed `0.10` threshold means **0.10%**, not 10%. [5] [26]

### Visual validation

The available visual glue is exactly:

```gherkin
Then I compare mobile browser page with visual baseline "<approved-baseline-name>"
```

The step takes `Hooks.getPage()`, reads the configured `browser` value, and calls `MobileBrowserVisualValidator.compareCurrentPage(...)`. The visual sample is tagged `@mobile_browser @visual @smoke` and contains only this comparison step. [25] [31]

For an active profile, the validator:

1. Exits without comparison when `visual.enabled` is false; otherwise requires a non-null, open Playwright `Page`.
2. Captures a **full-page** PNG.
3. Sanitizes profile and baseline names by replacing characters outside `A–Z`, `a–z`, `0–9`, `.`, `_`, and `-` with `_`.
4. Writes the actual PNG and locates the expected baseline at `<baseline_directory>/<safe-profile>/<safe-baseline>.png`.
5. Creates a missing baseline and returns when `create_baseline_if_missing` is true; otherwise fails.
6. Compares every pixel over the larger image canvas, treating missing pixels as transparent; matching pixels are faded in the diff and mismatches are red.
7. Writes the diff, optionally attaches baseline/actual/diff images, then fails when the calculated percentage exceeds the threshold. [25] [26]

| Artifact | Implemented location |
|---|---|
| Expected baseline | `<visual.baseline_directory>/<safe-profile>/<safe-baseline>.png` |
| Actual full-page PNG | `<visual.output_directory>/<run-id>/<safe-profile>/<safe-baseline>-actual.png` |
| Pixel diff PNG | `<visual.output_directory>/<run-id>/<safe-profile>/<safe-baseline>-diff.png` |

The repository currently contains one baseline asset at [`Galaxy_S25_Ultra_Chrome/visual-smoke-example.png`][32]. The committed sample feature requests `sample-home-page`; those names do not match. If Playwright profile activation and runner selection are added while `create_baseline_if_missing: true` remains enabled, that sample would create a new `sample-home-page.png` baseline rather than use the existing `visual-smoke-example.png`. Review generated baselines as test assets; for CI, set baseline creation to false so missing approved baselines fail. [5] [26] [31] [32]

### Playwright mobile evidence and video

`MobileBrowserEvidenceManager` contains conditional full-page screenshot logic. It checks that the configured browser name resolves to a mobile profile, evaluates the three screenshot policy switches, writes `<evidence.output_directory>/<run-id>/screenshots/<safe-scenario>.png`, and optionally attaches it as `image/png`. Screenshot failures are logged and do not fail the scenario. [23] [27]

> **Source-visible integration gap:** no call to `MobileBrowserEvidenceManager.captureScenarioScreenshotIfConfigured(...)` occurs in the reviewed `Hooks` lifecycle. Consequently, the `mobile_browser.evidence.screenshot_*` settings are **not automatic in the committed Playwright execution path**. [3] [27]

If a profile does reach `BrowserFactory.createContextWithVideo` and mobile video recording is enabled, the context is configured to record at the mobile video size. The generic `Hooks` close path retains video handles, closes the browser, then waits briefly for recording finalization and moves files into a feature-named directory. [3] [4] [23]

> **Path discrepancy:** profile screenshots use the configurable `mobile_browser.evidence.output_directory`, but `BrowserFactory` currently constructs mobile video storage as `test-output/mobile-browser-evidence/<timestamp>/videos`, not from that configurable output-directory key. Account for this difference when collecting artifacts. [4] [23] [27]

## Runners and practical execution status

| Invocation/runner | Current selection | Mobile-browser consequence |
|---|---|---|
| `mvn clean test` | Surefire uses `src/test/resources/testng.xml`; that suite names `com.ptaf.runner.TestRunner`, whose fixed tag expression is `@eStore`. [1] [18] [19] | Does not select the Playwright `@mobile_browser` sample or the Appium real-browser sample. |
| `mvn -Dtest=com.ptaf.runners.MobileTestRunner test` | JUnit Cucumber runner scans `src/test/resources/features/mobile` but has fixed tag expression `@theapp_smoke`. [20] | Does not select the committed Appium real-browser feature, even though it is under `features/mobile`. |
| `mobile_browser_visual_sample.feature` | Exists under `src/test/resources/features/mobile_browser` with `@mobile_browser`. [31] | No committed runner selects it, and the Playwright profile lifecycle is incomplete. |
| `appium_mobile_browser_google_search.feature` | Exists under `src/test/resources/features/mobile` with `@mobile @appium_browser`. [13] | Appium hooks/driver support is implemented, but no checked-in runner is configured to select its tag. |

Accordingly, there is **no source-supported normal command that executes either mobile-browser sample as-is**. The Appium feature needs a runner/tag selection that includes its browser tag and an available configured device plus Appium server. The Playwright feature needs both a runner/tag selection and lifecycle integration that detects a profile and calls `BrowserFactory.createBrowser(String)`. These are implementation/configuration changes outside this reference chapter. [3] [4] [13] [18] [19] [20] [31]

## Troubleshooting

| Symptom | Source-grounded cause | Check or corrective action |
|---|---|---|
| `Unsupported browser type: <profile-name>` during Playwright setup | `Hooks#createBrowserStack` switches only the desktop names and does not call the profile-string factory overload. | Do not keep changing profile YAML. Add profile detection and call `BrowserFactory.createBrowser(String)` before context creation, then use a runner that selects the emulation feature. [3] [4] |
| `mvn clean test` does not execute mobile browser work | The active TestNG suite targets `@eStore`; no listed runner targets `@mobile_browser` or the Appium browser feature tag. | Inspect runner tags and suite class before treating a command as a mobile-browser run. [18] [19] [20] |
| Appium tries to run a native app rather than Chrome/Safari | The scenario lacks `@appium_browser`/`@mobile_browser_real`, or its mobile tags route it to native mode. | Use one real-browser tag and ensure the feature is not accidentally mixed with a Playwright-emulation tag strategy. [9] |
| Android browser session cannot clear app data | `reset_app_data` needs Android shell execution; failure is tolerated and often indicates missing `--relaxed-security`. | Start Appium according to authorized policy with the needed security flag, or set `reset_app_data: false`. [6] [11] |
| Safari visibly opens but DOM locators fail or a Runtime/WebElement context error occurs | iOS Safari has not exposed a `WEBVIEW` context, or a first-run/Start Page state prevents normal navigation. | Confirm `include_safari_in_webviews`, wait value, simulator Safari state, and WebKit/Appium setup. Review the context logs; use the configured native navigation fallback only for iOS browser sessions. [6] [12] |
| Cookies/tabs persist across runs | Browser clean start is best effort; `close_existing_tabs` is not executed, and native Chrome/Safari termination/activation is purposely skipped. | Reset the simulator/device/browser state through approved environment preparation; do not rely on clean-start flags alone. [6] [11] |
| Mobile page becomes desktop sized in emulation | Shared frame navigation explicitly sets 1920×1080. | Avoid that step for profile-sensitive validation until it is made profile-safe. [30] |
| No automatic Playwright mobile screenshot | The dedicated evidence utility has no current lifecycle call site. | Use explicit diagnostic screenshot support as appropriate, or wire the utility into profile-aware teardown after the lifecycle integration is completed. [3] [27] |
| Expected baseline is missing or an unexpected baseline appears | Names are sanitized; missing baseline creation is enabled by default; sample feature and supplied baseline names differ. | Check the safe profile/baseline path, approve intended image updates, and disable auto-creation in CI. [5] [26] [31] [32] |
| Visual diff is unexpectedly large | Full-page, pixel-exact comparison includes viewport, scale, orientation, content length, dynamic UI, fonts, and rendering changes; unequal dimensions are compared over the larger canvas. | Stabilize page state and profile/orientation, then inspect `-actual.png` and `-diff.png` before changing the threshold. [4] [26] |

## Boundaries and safe operating practices

- Use approved non-production URLs and placeholder values in feature data. Do not place credentials, tokens, personal data, or sensitive expected values in Gherkin, profiles, screenshots, page-source output, or baselines.
- Keep Appium browser settings under `src/test/resources/mobile/config/mobile-browser-config.yml`; keep Playwright responsive profiles and visual controls under `src/test/resources/mobile_browser/config/`. The two YAML readers are separate. [5] [6] [21] [28]
- For real-browser locators, use `mobile_elements.<page>.<locator>` platform-specific entries. For emulation, use the regular Playwright locator model and `LocatorHandler`; it maps CSS, XPath, ARIA roles, text, labels, test IDs, and related web selectors to Playwright locators. [14] [24]
- A Playwright mobile profile emulates selected browser-context characteristics. It does not establish device hardware, native app permissions, real Chrome/Safari state, or an Appium `WEBVIEW` session. Real-device browser validation remains an Appium responsibility. [4] [9] [11]

## Related chapters

- `01-foundation-configuration-and-execution.md` — build, configuration, and runner ownership.
- `02-ui-web-automation.md` — shared Playwright web actions and locator patterns.
- `05-mobile-native.md` — Appium native-app sessions, permissions, and device lifecycle.
- `11-reporting-evidence-and-artifacts.md` — report consumers and cross-module evidence retention.

## Source references

- [Maven dependencies and Surefire suite configuration][1]
- [Playwright profile catalog][2] and [browser/context factory][4]
- [Playwright lifecycle routing][3]
- [Playwright mobile execution controls][5]
- [Appium real-browser configuration][6] and [Appium driver factory][11]
- [Appium browser navigation and Safari recovery][12]
- [Current runners and suite][18] [19] [20]
- [Visual comparison step and implementation][25] [26]

## References

[1]: ../../../pom.xml "FNB-ETAF Maven dependencies and Surefire configuration"
[2]: ../../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml "Playwright mobile-browser profile catalog"
[3]: ../../../src/main/java/com/ptaf/hooks/Hooks.java "Playwright lifecycle, browser setup, routing, and video finalization"
[4]: ../../../src/main/java/com/ptaf/utils/BrowserFactory.java "Playwright browser/context factory with mobile-profile support"
[5]: ../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml "Playwright mobile-browser execution controls"
[6]: ../../../src/test/resources/mobile/config/mobile-browser-config.yml "Appium real mobile-browser configuration"
[7]: ../../../src/test/resources/mobile/config/mobile-config.yml "Shared Appium mobile configuration"
[8]: ../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java "Appium mobile and browser configuration accessors"
[9]: ../../../src/main/java/com/ptaf/hooks/MobileHooks.java "Appium mobile Cucumber lifecycle and platform selection"
[10]: ../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java "Thread-local Appium driver lifecycle"
[11]: ../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java "Appium native and real-browser driver construction"
[12]: ../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java "Real mobile-browser navigation, Safari context, and page-source actions"
[13]: ../../../src/test/resources/features/mobile/appium_mobile_browser_google_search.feature "Appium real mobile-browser feature example"
[14]: ../../../src/test/resources/mobile/elements/google_mobile_browser_elements.yml "Appium real mobile-browser locator example"
[15]: ../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java "Appium mobile Gherkin step definitions"
[16]: ../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java "Appium mobile evidence and recording manager"
[17]: ../../../src/test/resources/config/config.yml "Primary framework configuration including browser selector"
[18]: ../../../src/test/resources/testng.xml "Default TestNG suite"
[19]: ../../../src/test/java/com/ptaf/runner/TestRunner.java "Default TestNG Cucumber runner"
[20]: ../../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Dedicated JUnit mobile Cucumber runner"
[21]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java "Playwright mobile-browser YAML loader"
[22]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java "Mobile-browser profile resolution and defaults"
[23]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java "Typed mobile-browser execution controls"
[24]: ../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java "Playwright locator mapping"
[25]: ../../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java "Mobile-browser visual Gherkin glue"
[26]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java "Pixel-based mobile-browser visual validation"
[27]: ../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java "Mobile-browser Playwright screenshot utility"
[28]: ../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java "Appium mobile YAML reader"
[29]: ../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java "Per-feature Cucumber/Extent image attachment handling"
[30]: ../../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java "Shared Playwright navigation step"
[31]: ../../../src/test/resources/features/mobile_browser/mobile_browser_visual_sample.feature "Playwright mobile-browser visual sample"
[32]: ../../../src/test/resources/baselines/mobile_browser/Galaxy_S25_Ultra_Chrome/visual-smoke-example.png "Committed mobile-browser visual baseline asset"
