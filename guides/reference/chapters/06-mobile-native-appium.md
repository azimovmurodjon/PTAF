# Native Mobile Appium Automation Reference

## Purpose and boundary

This chapter is the implementation reference for **native Android and iOS application automation** in FNB-ETAF. Native execution creates an Appium session for the application under test (AUT): Android uses `UiAutomator2Options`/`AndroidDriver`, and iOS uses `XCUITestOptions`/`IOSDriver`. The Maven build includes Appium Java Client 9.4.0, Selenium 4.26.0, Cucumber, JUnit, and TestNG; the dedicated mobile runner itself is a JUnit Cucumber runner. [1][14]

This is **not** Playwright mobile-browser emulation. The Playwright mobile-browser resources describe browser profiles, viewport/device emulation, visual checks, and their own evidence settings; they do not start an Appium device session. [21] It is also distinct from Appium real-browser execution: the latter starts Chrome/Safari sessions through the same manager but uses browser capabilities and DOM-oriented locators. Native AUT scenarios must keep their capabilities under `mobile.android`/`mobile.ios` and their locators under `mobile_elements`; they do not fall back to the ordinary web `elements` store. [4][9][20]

> **Safe operating rule:** keep application binaries, signing material, device UDIDs, private Appium endpoints, credentials, authentication values, customer data, and irreversible test values out of feature files and shared documentation. Use approved local or secret-managed configuration and clearly non-production placeholders in tests.

## Source map and responsibilities

| Concern | Current source/resource | Responsibility in native execution |
|---|---|---|
| Entry point | `MobileTestRunner` [14] | Runs Cucumber features from `src/test/resources/features/mobile`, glue from `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and currently declares tag expression `@theapp_smoke`. |
| Scenario lifecycle | `MobileHooks` [3] | Detects mobile tags, resolves platform, starts the appropriate driver, starts optional recording, captures configured end-of-scenario evidence, and normally closes the session. |
| Configuration loading | `MobileYamlReader`, `MobileConfigurationProperties` [6][7] | Eagerly loads and deep-merges framework YAML beneath only `mobile/config` and `mobile/elements`; exposes typed mobile settings and defaults. |
| Session creation | `MobileDriverFactory`, `MobileDriverManager` [4][5] | Turns YAML capabilities into Android/iOS options, connects to Appium, and keeps driver/platform/session-mode state in `ThreadLocal`s. |
| Feature-facing behavior | `MobileSteps`, `MobileActionImpl`, `MobileCommonMethods` [9][13][23] | Provides Gherkin step bindings; delegates interactions, gestures, waits, app/device operations, context handling, file transfer, clipboard, and screenshots to the current driver. |
| Locators | `MobileLocatorHandler` and `mobile/elements/*.yml` [10][16][17] | Resolves `mobile_elements.<page>.<locator>` values, chooses a platform/mode entry, then produces Selenium/Appium `By` locators. |
| Assertions, permissions, and media | `MobileAssert`, `MobilePermissionHandler`, `MobileEvidenceManager` [11][12][23] | Captures assertion screenshots, safely attempts OS-dialog actions, stores media, and attaches eligible media to the Cucumber scenario. |

The reader deliberately does **not** scan the whole `mobile` tree. It filters YAML to paths containing `/mobile/config/` or `/mobile/elements/`, protecting framework startup from YAML found inside an app bundle or vendor resource. Put framework configuration and locator maps only in those approved resource locations. [7]

## Native execution flow

A conventional native scenario is launched and closed as follows:

1. Cucumber selects a feature under `src/test/resources/features/mobile`. Current native examples use `@mobile @cross_platform`; the repository also has a separate Appium-browser feature. [15][18][19]
2. `MobileHooks.setUpMobile` treats a scenario as mobile when it has at least one of `@mobile`, `@android`, `@ios`, `@cross_platform`, `@appium_browser`, or `@mobile_browser_real`. It stores the active `Scenario` in `MobileEvidenceManager`, resolves a platform, chooses native versus Appium-browser mode, creates a session, and requests video recording if enabled. [3]
3. For a native scenario, `MobileDriverManager.startDriver` first checks `mobile.enabled`, closes any pre-existing driver for the current thread, calls `MobileDriverFactory.createDriver`, and stores the resulting driver and platform in thread-local state. [5]
4. Feature steps in `MobileSteps` delegate through `MobileActionImpl` to a fresh `MobileCommonMethods` wrapper around `MobileDriverManager.getDriver()`. An element-oriented action resolves YAML, waits for presence and visibility, then performs its operation. [9][13]
5. In normal teardown, `MobileHooks.tearDownMobile` captures the configured final screenshot, stops optional recording, then always invokes `MobileDriverManager.closeDriver()` and clears the evidence scenario reference in a `finally` block. `closeDriver()` calls `quit()` but logs rather than rethrows a quit failure, then removes all driver-related thread locals. [3][5][11]

### Platform resolution

Platform selection is deterministic and applies to both native and Appium-browser scenarios:

| Priority | Source | Result |
|---:|---|---|
| 1 | `-Dmobile.platform=android` or `-Dmobile.platform=ios` | Command-line setting wins. |
| 2 | Scenario tag `@android` or `@ios` | The explicit single platform tag wins. |
| 3 | `mobile.default_platform` | YAML default is used when no override/tag is present. |

A scenario carrying both `@android` and `@ios` throws an `IllegalArgumentException`; a cross-platform feature should therefore use `@mobile` or `@cross_platform` without both platform tags and select the target through the property or YAML. `MobilePlatform.from` accepts case-insensitive normalized values and maps `IPHONE`/`IPAD` to `IOS`; an unknown nonblank value fails rather than silently choosing a platform. [3][6]

### Tags, runner, and commands

The shipped runner is intentionally separate from the default TestNG suite. The default `testng.xml` names `com.ptaf.runner.TestRunner`, not `MobileTestRunner`; invoke the mobile runner explicitly. The runner's checked-in `@CucumberOptions` currently selects `@theapp_smoke`, writes HTML/JSON/JUnit XML to `target/cucumber-reports`, and registers the Extent adapter plus the two reporting listeners. [14][19]

```bash
# Compile test code without executing tests.
mvn clean test-compile -DskipTests

# Run the mobile Cucumber runner using its checked-in tag expression.
mvn -Dtest=com.ptaf.runners.MobileTestRunner test

# Select the native target platform without exposing device identifiers.
mvn -Dtest=com.ptaf.runners.MobileTestRunner test -Dmobile.platform=android
mvn -Dtest=com.ptaf.runners.MobileTestRunner test -Dmobile.platform=ios
```

The `-Dmobile.platform` behavior is implemented by `MobileHooks`; the code source does not define a separate `mobile.tags` property. For a different Cucumber selection, review the runner annotation and the supported feature tags rather than assuming the default TestNG suite runs native scenarios. [3][14][19]

Do **not** put `Given I start mobile application using platform "…"` at the beginning of a normally tagged native scenario. Hooks have already created a driver, and that step calls `startDriver`, which intentionally closes any existing driver for the thread before replacing it. The step is available for a controlled/manual start, but it is not the normal hook-managed path. [3][5][13]

## Resources and configuration

### File ownership

| Resource | Owns | Do not use it for |
|---|---|---|
| `src/test/resources/mobile/config/mobile-config.yml` [2] | Shared mobile enablement, endpoint, default platform, waits, permission behavior, and native evidence behavior. | Native platform capabilities or real-browser-only settings. |
| `src/test/resources/mobile/config/mobile-native-config.yml` [8] | Android and iOS native application capabilities. | Chrome/Safari/browser cleanup configuration. |
| `src/test/resources/mobile/config/mobile-browser-config.yml` [20] | Separate Appium real Chrome/Safari configuration under `mobile_browser_appium`. | Native AUT launch capabilities. |
| `src/test/resources/mobile/elements/*.yml` [16][17] | Native page/screen locator maps under `mobile_elements`. | Credentials, test data, or application binaries. |
| `src/test/resources/mobile/apps/` [22] | Approved sample/application artifacts and checksums. | Configuration YAML; the YAML reader excludes this location by design. |
| `src/test/resources/config/config.yml` [18] | Cross-cutting reporting and `soft_assertions` settings. | Mobile capability values. |

`MobileYamlReader` deep-merges maps loaded from both permitted folders. Keep a key owned by one appropriate file and avoid duplicate scalar definitions across those files; the implementation does not document a stable user-facing precedence rule for duplicate files. [7]

### Shared keys that exist

These are the actual shared keys accessed by `MobileConfigurationProperties`. Defaults below are code defaults; the current values are visible in `mobile-config.yml`. [2][6]

| Key | Purpose and current implementation behavior |
|---|---|
| `mobile.enabled` | Gate checked by both native and Appium-browser driver starts; code default `true`. A false value causes `IllegalStateException`. |
| `mobile.appium_server_url` | Appium server URL; code default `http://127.0.0.1:4723`. A malformed URL is rethrown as `IllegalArgumentException`. |
| `mobile.default_platform` | Used only after command-line and tag selection; code default `android`. |
| `mobile.explicit_wait_seconds` | Used by ordinary element lookup's presence-plus-visibility wait; code default `30`. |
| `mobile.implicit_wait_seconds` | Applied once after session creation only when greater than zero; code default `0`. |
| `mobile.new_command_timeout_seconds` | Passed as Appium new-command timeout; code default `120`. |
| `mobile.permissions.popup_timeout_seconds` | Per-candidate timeout used by optional permission/system-dialog handling; code default `3`. |
| `mobile.permissions.max_popups_to_handle` | Upper bound for `allow all` / `deny all`; code default `5`, with at least one attempted iteration. |
| `mobile.permissions.capture_evidence` | Controls before/after named screenshots for explicit permission handling; code default `true`. |
| `mobile.evidence.output_directory` | Evidence root; code default `test-output/mobile-evidence`. |
| `mobile.evidence.screenshot_on_failure` | Captures a final teardown screenshot for a failed scenario; code default `true`. |
| `mobile.evidence.screenshot_on_pass` | Captures a final screenshot for a passed scenario; code default `false`. |
| `mobile.evidence.screenshot_after_each_scenario` | Captures a final screenshot regardless of status; code default `false`. |
| `mobile.evidence.attach_screenshots_to_report` | Attaches eligible PNG bytes to Cucumber; code default `true`. Failure teardown screenshots force attachment even if this is false. |
| `mobile.evidence.video_recording_enabled` | Requests Appium screen recording around each mobile scenario; code default `false`. |
| `mobile.evidence.video_on_failure_only` | Discards a passed scenario's returned recording payload when true; code default `false`. |
| `mobile.evidence.attach_video_to_report` | Attaches stored MP4 bytes to Cucumber; code default `false`. |

A safe shared configuration shape is:

```yaml
mobile:
  enabled: true
  appium_server_url: "<APPIUM_SERVER_URL>"
  default_platform: "<android-or-ios>"
  implicit_wait_seconds: 0
  explicit_wait_seconds: <seconds>
  new_command_timeout_seconds: <seconds>

  permissions:
    popup_timeout_seconds: <seconds>
    max_popups_to_handle: <count>
    capture_evidence: <true-or-false>

  evidence:
    output_directory: "test-output/mobile-evidence"
    screenshot_on_failure: <true-or-false>
    screenshot_on_pass: <true-or-false>
    screenshot_after_each_scenario: <true-or-false>
    attach_screenshots_to_report: <true-or-false>
    video_recording_enabled: <true-or-false>
    video_on_failure_only: <true-or-false>
    attach_video_to_report: <true-or-false>
```

### Native capabilities that exist

The factory uses standard typed option objects and sends an optional string capability only when its configuration value is nonblank. Generic optional scalar values are parsed as integer, long, or double when possible; optional booleans are parsed with `Boolean.parseBoolean`. A recognized orientation (`PORTRAIT`/`LANDSCAPE`) is sent as a capability, then the factory makes a best-effort `mobile: setDeviceOrientation` call after session creation. A runtime orientation-command failure is logged rather than fatal. [4]

| Platform | Keys actually read for a native session | Factory mapping / note |
|---|---|---|
| Android | `mobile.android.platform_name`, `automation_name`, `device_name`, `platform_version`, `app`, `app_package`, `app_activity`, `appActivity`, `udid`, `system_port`, `adb_exec_timeout`, `app_wait_activity`, `app_wait_package`, `auto_grant_permissions`, `orientation`, `no_reset`, `full_reset` | Builds `UiAutomator2Options`. `app_activity` is preferred, with `appActivity` as a backward-compatible fallback. `system_port` is a generic capability named `systemPort`. |
| iOS | `mobile.ios.platform_name`, `automation_name`, `device_name`, `platform_version`, `app`, `bundle_id`, `udid`, `xcode_org_id`, `xcode_signing_id`, `updated_wda_bundle_id`, `wda_local_port`, `wda_startup_retries`, `wda_startup_retry_interval`, `wda_launch_timeout`, `wda_connection_timeout`, `wait_for_idle_timeout`, `app_launch_state_timeout_sec`, `use_new_wda`, `show_xcode_log`, `auto_accept_alerts`, `auto_dismiss_alerts`, `include_safari_in_webviews`, `connect_hardware_keyboard`, `enforce_app_install`, `orientation`, `no_reset`, `full_reset` | Builds `XCUITestOptions`; WebDriverAgent and signing capabilities are only sent when nonblank. |

Use repository-relative artifact paths and placeholders for project-specific values:

```yaml
mobile:
  android:
    automation_name: "UiAutomator2"
    platform_name: "Android"
    device_name: "<ANDROID_DEVICE_OR_EMULATOR>"
    platform_version: ""
    udid: ""
    orientation: "portrait"
    app: "src/test/resources/mobile/apps/<APP>.apk"
    app_package: "<OPTIONAL_ANDROID_PACKAGE>"
    app_activity: "<OPTIONAL_ANDROID_ACTIVITY>"
    app_wait_package: ""
    app_wait_activity: ""
    system_port: ""
    adb_exec_timeout: ""
    auto_grant_permissions: "true"
    no_reset: false
    full_reset: false

  ios:
    automation_name: "XCUITest"
    platform_name: "iOS"
    device_name: "<IOS_SIMULATOR_OR_DEVICE>"
    platform_version: ""
    udid: ""
    orientation: "portrait"
    app: "src/test/resources/mobile/apps/<SIMULATOR_APP_OR_SIGNED_IPA>"
    bundle_id: ""
    auto_accept_alerts: "false"
    auto_dismiss_alerts: "false"
    xcode_org_id: ""
    xcode_signing_id: ""
    updated_wda_bundle_id: ""
    wda_local_port: ""
    no_reset: false
    full_reset: false
```

### Artifact and device constraints

The configuration comments and factory make the following native constraints explicit. [4][8][22]

| Target | Correct artifact | Operational constraint |
|---|---|---|
| Android emulator/device | `.apk` | Configure optional package/activity only when the AUT requires explicit launch details. Allocate a distinct `system_port` when parallel Android sessions require it. |
| iOS Simulator | Simulator-built `.app` | A device IPA is not a simulator artifact. When launching by `.app`, leave `bundle_id` blank unless the target launch flow requires an installed-app identifier. |
| Real iPhone/iPad | Signed `.ipa` | Supply `ios.udid` and, where the environment requires them, signing/WDA values. The factory logs a warning when an `.ipa` is configured without a UDID. |

Appium availability is an external prerequisite: the installed server must be reachable at `mobile.appium_server_url`, and the target Appium driver must match the requested automation name. The repository's resource README provides the local preparation commands below; run them only on an approved workstation/device environment. [22]

```bash
appium driver install uiautomator2
appium driver install xcuitest
appium

# Android target discovery
adb devices

# iOS Simulator discovery (macOS/Xcode host)
xcrun simctl list devices
```

## Feature authoring and Gherkin conventions

Place native features in `src/test/resources/features/mobile/`. The shipped native examples are a cross-platform sample-app workflow and an FNB launch/evidence smoke feature; the Google feature in the same folder is explicitly Appium real-browser automation, not a native AUT example. [15][19]

Use a mobile tag and, for cross-platform features, select the platform externally. The current step expressions use `{word}` for `page` and `locator`, so supply each as one token that exactly matches the YAML map key.

```gherkin
@mobile @cross_platform @<approved_suite_tag>
Feature: <application> native smoke

  Scenario: Reach a stable native screen without sensitive data
    When I allow all mobile permission popups if displayed
    When I wait up to <seconds> seconds for mobile page <screen_key> locator <ready_key> to be visible
    Then I verify mobile page <screen_key> locator <ready_key> is visible
    When I capture mobile screenshot named "<non_sensitive_checkpoint_name>"

  Scenario: Complete a low-risk native interaction
    When I enter mobile value "<non_sensitive_test_value>" on page <screen_key> locator <input_key>
    When I hide mobile keyboard
    When I tap on mobile page <screen_key> locator <submit_key>
    Then I verify mobile page <screen_key> locator <result_key> text contains "<expected_non_sensitive_text>"
```

`MobileSteps` implements the following groups. The phrases below are exact step patterns or safe substitutions for their arguments. [13]

| Goal | Available Gherkin pattern |
|---|---|
| Element interaction | `I tap on mobile page <page> locator <key>`; `I enter mobile value "<value>" on page <page> locator <key>`; `I clear mobile page <page> locator <key>`; `I press Enter on mobile page <page> locator <key>` |
| Assertions | `I verify mobile page <page> locator <key> is visible`; `I verify mobile page <page> locator <key> text contains "<text>"` |
| Synchronization | `I wait up to <seconds> seconds for mobile page <page> locator <key> to be visible`; `... to disappear`; `I pause mobile execution for <seconds> seconds` |
| Gestures | Long press, double tap, coordinate tap, drag, scroll-until-visible, scroll-to-text, swipe in four directions, pinch, and the existing `zoom out` step |
| Device/app | Hide keyboard, background app, rotate screen, activate/terminate app, open deep link, and switch context to a named context or `NATIVE_APP` |
| Device I/O | Push/pull a file, set/verify clipboard text, capture a named screenshot |
| Permissions | Allow/deny one or all optional popups, allow an approved label, or invoke a requested allow/deny action |

For hybrid apps, retrieve or otherwise identify the available `NATIVE_APP`/`WEBVIEW_*` context before switching; `MobileCommonMethods` uses reflective `getContextHandles` and `context(String)` calls and throws `UnsupportedOperationException` if the current driver does not support that interface. The `grant`/`revoke` permission methods call `mobile: changePermissions` with `appPackage`, so treat those feature steps as Android-oriented. [9][23]

## Locators and waits

### Locator-map contract

Native locator values are loaded from `mobile_elements.<page>.<locator>`. A value may be a single locator string or a map. For a native session, map selection is `android` or `ios`, then `default`, then `common`. The extra `mobileBrowser`/`browser`/`web` map choices are for Appium browser mode; only browser sessions may try `elements.<page>.<locator>` after no `mobile_elements` value exists. [9]

```yaml
mobile_elements:
  <screen_key>:
    <ready_key>:
      android: "ACCESSIBILITY_ID_<android_stable_id>"
      ios: "ACCESSIBILITY_ID_<ios_stable_id>"
    <input_key>:
      android: "ID_<android_resource_id>"
      ios: "IOS_PREDICATE_name == '<ios_accessibility_name>'"
    <submit_key>:
      android: "ANDROID_UIAUTOMATOR_new UiSelector().description(\"<android_description>\")"
      ios: "IOS_CLASS_CHAIN_**/XCUIElementTypeButton[`name == \"<ios_button_name>\"`]"
```

The checked-in sample map demonstrates both simple stable accessibility IDs and an Android/iOS map for one logical result. The FNB sample map contains broader XPath fallbacks suitable for a smoke check, but stable AUT-provided accessibility/resource identifiers should be preferred whenever available. [16][17]

### Locator language

| Locator form | Native use |
|---|---|
| `ACCESSIBILITY_ID_<value>` | Preferred first choice for a stable native accessibility identifier. |
| `ID_<value>` | Android/iOS element ID where exposed by the driver. |
| `ANDROID_UIAUTOMATOR_<selector>` | Android-specific UiAutomator selector. |
| `IOS_PREDICATE_<predicate>` | iOS predicate selector. |
| `IOS_CLASS_CHAIN_<chain>` | iOS class-chain selector. |
| `XPATH_<xpath>`, `CLASS_NAME_<name>`, `NAME_<name>` | Supported technical forms; use carefully because UI hierarchy and text XPath can be brittle. |
| `BUTTON_`, `TEXTBOX_`, `TEXT_`, `CSS_`, `TESTID_`, `LABEL_`, and related friendly forms | Additive compatibility forms. They resolve to Selenium/Appium `By` locators; they are usually more appropriate to real browser DOM automation than native AUT screens. |

`MobileLocatorHandler` rejects a blank or unsupported prefix with a diagnostic listing the raw value and supported styles. Its native guidance specifically recommends `ACCESSIBILITY_ID_`, `ID_`, `IOS_PREDICATE_`, or `ANDROID_UIAUTOMATOR_` when possible. [10]

### Wait model

The normal `findVisibleElement` path uses `mobile.explicit_wait_seconds` and waits in two phases: first element presence in the UI hierarchy, then visibility. It returns as soon as the condition is satisfied. Custom visible/invisible steps build a `WebDriverWait` with the passed nonnegative timeout. Keep the shared implicit wait at zero unless an environment has a demonstrated reason otherwise; the current resource configuration follows that pattern. [2][9]

Use a visible screen checkpoint after launch, navigation, permissions, deep links, and submissions. Reserve `I pause mobile execution ...` for the rare case in which the AUT exposes no observable condition.

> **Source-visible caveat:** `scrollUntilVisible` really does repeat `swipeUp()` and then performs a final lookup. In contrast, `scrollToText` currently builds a platform-aware XPath and calls `findElement`; it does not issue a scroll gesture. Do not rely on that latter step to move a list until its implementation is changed or the target is already visible. [9]

## Permissions and safe test data

`MobilePermissionHandler` is deliberately non-failing for an absent OS permission dialog. It tries platform-specific candidate locators for allow/deny actions and returns `false` if none becomes clickable within `mobile.permissions.popup_timeout_seconds`. `allow all`/`deny all` repeat only up to `max_popups_to_handle` and stop as soon as no popup is found. This supports a feature that runs on a clean device, a reused simulator, or a device farm without treating prior permission state as a failure. [12]

```gherkin
When I allow mobile permission popup if displayed
When I deny mobile permission popup if displayed
When I allow mobile permission popup with text "<approved_non_sensitive_button_label>" if displayed
When I allow all mobile permission popups if displayed
When I deny all mobile permission popups if displayed
```

Choose one permission strategy deliberately:

- **Android disposable smoke environment:** configure `mobile.android.auto_grant_permissions` when pre-granting does not invalidate the test objective.
- **iOS simple flows:** configure `mobile.ios.auto_accept_alerts` or `auto_dismiss_alerts` only if blanket handling is acceptable.
- **Consent/denial coverage:** keep automatic handling off and use explicit permission steps so screenshots and scenario history demonstrate the intended choice. [4][8][12]

Do not automate production transactions or embed passwords, card/account data, one-time passcodes, customer identifiers, signing identifiers, full internal URLs, or real device identifiers in a native feature. Use a purpose-created test account and test data owned by the relevant API/database/data module when setup or verification is necessary; `MobileSteps` itself implements no dedicated mobile payload, query, or test-data reader. [13]

## Screenshots, video, reports, and artifacts

`MobileEvidenceManager` assigns one JVM-wide run ID in `yyyyMMdd_HHmmss` format. It writes media below `mobile.evidence.output_directory` (current configuration: `test-output/mobile-evidence`) and uses a per-thread current Cucumber `Scenario` to log/attach evidence. [2][11]

| Trigger | Persisted artifact and attachment behavior |
|---|---|
| Named screenshot step | PNG in `<evidence-root>/<run-id>/target-output/screenshots/<safe-name>.png`; attached when an active scenario exists and `attach_screenshots_to_report` is true. |
| `MobileAssert` failure | Immediate PNG in `<evidence-root>/<run-id>/screenshots/<safe-scenario-and-assertion>.png`; logged to the scenario and attached when configured. |
| Scenario teardown | For a failed scenario with `screenshot_on_failure`, saves a `.../target-output/screenshots/<safe-scenario>_failure.png` and force-attaches it. For passes/always, saving depends on the corresponding evidence flags. |
| Recording start/stop | If enabled and the driver implements `CanRecordScreen`, starts before steps and stops during teardown. Returned Base64 data is decoded to an MP4 under `<evidence-root>/<run-id>/videos/<feature-derived-directory>/`; passed recordings are discarded when `video_on_failure_only` is true. |
| Cucumber runner reports | `target/cucumber-reports/mobile-report.html`, `.json`, and `.xml`; the runner also invokes the Extent adapter and listeners. |
| Extent adapter reports | `extent.properties` configures timestamped folders below `test-output/` for Spark, Base64, PDF, and Excel report outputs. [14][24] |

A screenshot/video request is best effort: evidence exceptions are logged and do not themselves fail a normal scenario. A startup failure before a driver exists cannot yield a mobile screenshot. Screen recording also depends on the active Appium driver supporting `CanRecordScreen`. [11]

The cross-cutting `soft_assertions.enabled` setting applies to native element lookup. When enabled, a normal locator timeout can be recorded with an immediate named screenshot and the action may continue; `MobileHooks` is intended to fail the scenario afterward if the context contains failures. [3][9][18]

> **Important source-visible discrepancy:** in the current `MobileHooks.tearDownMobile`, the soft-assertion summary is thrown *before* the later `try`/`finally` that captures final evidence, stops video, closes the driver, and clears the evidence scenario reference. Therefore, when soft assertions are enabled and failures exist, that teardown invocation does not reach the normal cleanup block. This is contrary to the nearby intent comments and should be treated as an implementation issue to verify/fix before relying on soft assertions in long-running mobile workers. [3]

## Native Appium troubleshooting

| Symptom | Check and corrective action grounded in the current implementation |
|---|---|
| `Mobile automation is disabled in mobile-config.yml` | Set `mobile.enabled` to an approved true value; both native and browser starts guard this key before opening a session. [5][6] |
| `No Appium driver is available for this thread` | Confirm a supported mobile tag caused `MobileHooks` to run and that mobile steps do not execute before a session starts. Avoid calling mobile steps from an untagged feature. [3][5] |
| Wrong platform starts | Inspect precedence: `-Dmobile.platform` overrides tags, and tags override `mobile.default_platform`. Remove the conflicting `@android`/`@ios` pair. [3] |
| Session creation fails immediately | Validate the Appium URL, active device/simulator, installed platform driver, artifact path, and capability spelling. Invalid server URLs are reported by the factory; disabled mobile execution is reported by the manager. [4][5] |
| iOS app fails on simulator | Ensure the artifact is a simulator-built `.app`, not a device `.ipa`. Supply a UDID and applicable signing/WDA settings for real-device IPA execution. [4][8][22] |
| Android launches the wrong activity or never reaches readiness | Recheck `app_package`, `app_activity` (or fallback `appActivity`), `app_wait_package`, and `app_wait_activity`. Then wait for a real post-launch locator rather than a fixed pause. [4][9] |
| Native locator is not found | Add/verify `mobile_elements.<page>.<locator>`, selected `android`/`ios` map member, and allowed locator prefix. Native sessions do not use the shared web-elements fallback. [9][10] |
| Locator is technically valid but fragile | Replace broad XPath/text matching with an AUT accessibility identifier, resource ID, iOS predicate, or UiAutomator selector. The resolver supports all, but its native guidance favors stable native selectors. [10] |
| Permission step does nothing | This may be correct: missing dialogs return normally. Check the selected platform, popup timing, label/localization, configured popup timeout, and maximum popup count. [12] |
| No final screenshot or MP4 | Verify a session exists, evidence flags are enabled, recording is supported, and failure-only retention is appropriate. For soft assertion runs, also account for the teardown discrepancy documented above. [3][11] |
| Parallel sessions interfere | Java driver references are thread-local, but device allocation is not. Provision separate devices/simulators and distinct Android `system_port` values where required; test a narrow tag serially before scaling. [4][5] |
| Orientation does not change | Use only `portrait`/`landscape`. The initial capability is sent when valid, but the post-session `mobile: setDeviceOrientation` command is explicitly best effort and may be unsupported by a driver/session. [4][9] |
| Context switch is unsupported | The current driver must expose the expected context API. Confirm available contexts and switch only after the hybrid webview appears; otherwise the wrapper throws an explicit unsupported-operation error. [9] |
| Gesture name does not match visual outcome | Verify on the target device. `zoomOut()` calls the two-finger helper with positions that spread apart; the interface itself notes that its inherited name should be verified against the implementation. [9][23] |

### Appium-browser and Playwright boundaries

This chapter does not document an Appium browser implementation, but the boundaries affect tag and resource choices:

- `@appium_browser` or `@mobile_browser_real` makes `MobileHooks` call `startBrowserDriver`, not `startDriver`. Browser sessions read separate `mobile_browser_appium` settings and may use browser DOM locators. [3][4][20]
- `MobileConfigurationProperties.isBrowserModeEnabled()` gives the split browser key priority over the legacy nested key. However, `isAppiumBrowserScenario()` already returns `true` for either explicit browser tag before evaluating that setting. In the current code, an explicit browser tag therefore starts a browser driver regardless of `mobile_browser_appium.enabled`; do not assume that flag alone is a runtime gate. [3][6]
- Playwright mobile-browser configuration is under `src/test/resources/mobile_browser/` and represents emulated browser profiles/evidence/visual settings, not a native AUT package or device session. [21]

## Related chapters

The following local chapter filenames are intended to be covered by their owning scopes; this chapter does not duplicate them:

- `01-framework-overview.md` — architecture and module map.
- `02-execution-and-configuration.md` — Maven, runners, environments, and cross-cutting properties.
- `04-ui-playwright-automation.md` — desktop and Playwright browser UI automation.
- `07-mobile-browser-automation.md` — Appium real browser and Playwright mobile-browser distinctions.
- `08-reporting-evidence-and-artifacts.md` — framework-wide reporting and artifact governance.

## Source references

- **[1]** [Maven build and test-plugin configuration](../../../pom.xml)
- **[2]** [Shared mobile configuration](../../../src/test/resources/mobile/config/mobile-config.yml)
- **[3]** [Mobile lifecycle hooks](../../../src/main/java/com/ptaf/hooks/MobileHooks.java)
- **[4]** [Appium driver factory](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java)
- **[5]** [Thread-local mobile driver manager](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java)
- **[6]** [Typed mobile configuration accessors](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java)
- **[7]** [Scoped mobile YAML reader](../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java)
- **[8]** [Native Android/iOS capabilities](../../../src/test/resources/mobile/config/mobile-native-config.yml)
- **[9]** [Mobile common actions, waits, context, and locator lookup](../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java)
- **[10]** [Mobile locator resolver](../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java)
- **[11]** [Mobile screenshot and video manager](../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java)
- **[12]** [Mobile permission/system-dialog handler](../../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java)
- **[13]** [Native mobile Cucumber step definitions](../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java)
- **[14]** [Dedicated mobile Cucumber runner](../../../src/test/java/com/ptaf/runners/MobileTestRunner.java)
- **[15]** [Cross-platform native sample feature](../../../src/test/resources/features/mobile/theapp_cross_platform_workflow.feature)
- **[16]** [Sample native locator map](../../../src/test/resources/mobile/elements/theapp_elements.yml)
- **[17]** [FNB native locator map](../../../src/test/resources/mobile/elements/fnb_elements.yml)
- **[18]** [Cross-cutting reporting and soft assertions configuration](../../../src/test/resources/config/config.yml)
- **[19]** [Default TestNG suite](../../../src/test/resources/testng.xml)
- **[20]** [Separate Appium real-browser configuration](../../../src/test/resources/mobile/config/mobile-browser-config.yml)
- **[21]** [Playwright mobile-browser execution configuration](../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml)
- **[22]** [Native mobile resource README and local prerequisites](../../../src/test/resources/mobile/README.md)
- **[23]** [Mobile action contract](../../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java)
- **[24]** [Extent adapter output configuration](../../../src/test/resources/extent.properties)

## References

[1]: ../../../pom.xml "FNB-ETAF Maven build and Surefire configuration"
[2]: ../../../src/test/resources/mobile/config/mobile-config.yml "Shared Appium mobile configuration"
[3]: ../../../src/main/java/com/ptaf/hooks/MobileHooks.java "Native mobile Appium lifecycle hooks"
[4]: ../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java "Appium native and browser driver factory"
[5]: ../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java "Thread-local Appium driver lifecycle manager"
[6]: ../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java "Typed native mobile configuration access"
[7]: ../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java "Scoped framework mobile YAML reader"
[8]: ../../../src/test/resources/mobile/config/mobile-native-config.yml "Native Android and iOS Appium capability configuration"
[9]: ../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java "Mobile Appium actions, waits, context, and locator resolution"
[10]: ../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java "Mobile locator format resolver"
[11]: ../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java "Native mobile screenshot and video evidence manager"
[12]: ../../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java "Mobile permission and system-dialog handler"
[13]: ../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java "Mobile Cucumber step definitions"
[14]: ../../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Dedicated Appium Cucumber runner"
[15]: ../../../src/test/resources/features/mobile/theapp_cross_platform_workflow.feature "Current cross-platform native mobile sample"
[16]: ../../../src/test/resources/mobile/elements/theapp_elements.yml "Current native sample locator map"
[17]: ../../../src/test/resources/mobile/elements/fnb_elements.yml "Current FNB native locator map"
[18]: ../../../src/test/resources/config/config.yml "Cross-cutting reporting and soft-assertion settings"
[19]: ../../../src/test/resources/testng.xml "Default TestNG suite definition"
[20]: ../../../src/test/resources/mobile/config/mobile-browser-config.yml "Appium real mobile browser configuration"
[21]: ../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml "Playwright mobile-browser execution settings"
[22]: ../../../src/test/resources/mobile/README.md "Native Appium resource setup guidance"
[23]: ../../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java "Mobile action interface contract"
[24]: ../../../src/test/resources/extent.properties "Extent report output configuration"
