# FNB-ETAF Native Mobile Appium Automation Guide

## Purpose and scope

This guide describes the **native mobile application** automation capability in FNB-ETAF. It covers Android native app sessions driven through UiAutomator2 and iOS native app sessions driven through XCUITest. The implementation is Appium-based, Cucumber scenarios opt in through mobile tags, and the framework supplies YAML-driven capabilities and locators, reusable Gherkin steps, platform resolution, evidence capture, and per-scenario driver cleanup. The Maven build declares `io.appium:java-client` version `9.4.0`, alongside Cucumber and JUnit support. [1]

The native module is deliberately separate from both Appium real-mobile-browser automation and Playwright responsive-browser emulation. A native scenario starts an AUT (application under test) session using the `mobile.android` or `mobile.ios` configuration. It does not use browser capabilities, browser cleanup, or Playwright device profiles. Use the real-browser module only for Chrome/Safari testing on a device or simulator; its boundary is documented in [Module boundaries](#module-boundaries-with-other-framework-modules). [2] [3]

> **Safety boundary:** This guide uses placeholders in all configuration and Gherkin examples. Keep application binaries, device identifiers, signing values, credentials, authentication data, and private endpoints outside feature files and out of committed documentation.

## Architecture and source locations

The native flow is resource-first. A tagged Cucumber scenario invokes `com.ptaf.hooks.MobileHooks`, which resolves the platform, opens a thread-local Appium driver, and starts optional recording. Steps in `com.ptaf.stepdefinitions.MobileSteps` delegate through `MobileActionImpl` to `MobileCommonMethods`; that class resolves a YAML locator and performs the Appium operation. Teardown captures configured evidence, stops recording, closes the session, and clears thread-local state. [4] [5] [6] [7]

| Concern | Package, class, or resource | Responsibility |
|---|---|---|
| Mobile lifecycle | [`com.ptaf.hooks.MobileHooks`](../../src/main/java/com/ptaf/hooks/MobileHooks.java) | Recognizes mobile tags; resolves Android/iOS; creates the native driver; starts/stops recording; captures end-of-scenario evidence; quits the driver. |
| Platform and configuration access | [`com.ptaf.mobile.config.MobilePlatform`](../../src/main/java/com/ptaf/mobile/config/MobilePlatform.java), [`MobileConfigurationProperties`](../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java), [`MobileYamlReader`](../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java) | Parses `ANDROID`/`IOS`, exposes mobile settings, and merges framework YAML only from `mobile/config` and `mobile/elements`. |
| Native driver creation | [`com.ptaf.mobile.drivers.MobileDriverFactory`](../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java), [`MobileDriverManager`](../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java) | Maps YAML to `UiAutomator2Options` or `XCUITestOptions`; creates Android/iOS sessions; maintains one driver, platform, and session-mode flag per thread. |
| Mobile action layer | [`com.ptaf.mobile.interfaces.MobileAction`](../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java), [`com.ptaf.mobile.implementation.MobileActionImpl`](../../src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java), [`com.ptaf.mobile.pages.MobileCommonMethods`](../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java) | Defines and implements feature-facing interaction, gesture, device, context, file, wait, and browser-support operations. `MobileActionImpl` is a thin delegate to `MobileCommonMethods`. |
| Locator resolution | [`com.ptaf.mobile.handlers.MobileLocatorHandler`](../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java), [`src/test/resources/mobile/elements/`](../../src/test/resources/mobile/elements/) | Converts a YAML locator value to `By`; selects platform-aware values from `mobile_elements.<page>.<key>`. |
| Assertions and permissions | [`com.ptaf.mobile.assertions.MobileAssert`](../../src/main/java/com/ptaf/mobile/assertions/MobileAssert.java), [`com.ptaf.mobile.permissions.MobilePermissionHandler`](../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java) | Performs visible/text assertions with failure evidence; safely handles optional OS permission dialogs. |
| Evidence | [`com.ptaf.mobile.evidence.MobileEvidenceManager`](../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java) | Writes PNG screenshots and optional MP4 screen recordings; attaches media to the active Cucumber scenario when configured. |
| Native step grammar | [`com.ptaf.stepdefinitions.MobileSteps`](../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java) | Exposes the Cucumber steps used by native features. |
| Native runner | [`com.ptaf.runners.MobileTestRunner`](../../src/test/java/com/ptaf/runners/MobileTestRunner.java) | JUnit Cucumber runner for `src/test/resources/features/mobile`; its checked-in annotation selects `@theapp_smoke`. |
| Native configuration | [`mobile-config.yml`](../../src/test/resources/mobile/config/mobile-config.yml), [`mobile-native-config.yml`](../../src/test/resources/mobile/config/mobile-native-config.yml) | Separates shared mobile behavior from native Android/iOS capabilities. |
| Sample native assets and locators | [`mobile/apps/`](../../src/test/resources/mobile/apps/), [`theapp_elements.yml`](../../src/test/resources/mobile/elements/theapp_elements.yml), [`fnb_elements.yml`](../../src/test/resources/mobile/elements/fnb_elements.yml) | Stores the checked-in Android APK, unpacked iOS simulator `.app`, checksum file, and sample page locator maps. |
| Feature patterns | [`features/mobile/`](../../src/test/resources/features/mobile/) | Contains `theapp_cross_platform_workflow.feature`, `fnb_cross_platform_smoke.feature`, and the real-browser sample. |

## Prerequisites

Use a JDK compatible with the repository build: the Maven compiler source and target are Java 21. Maven is required to resolve dependencies and run `MobileTestRunner`. [1]

Install and start an **Appium 2** server reachable from the configured `mobile.appium_server_url`. The native Android configuration selects UiAutomator2, while the iOS configuration selects XCUITest. Install the matching Appium driver before creating a session. [2] [8]

```bash
appium driver install uiautomator2
appium driver install xcuitest
appium
```

For Android execution, prepare an emulator or authorized Android device with the Android SDK and `adb` available. Confirm that the target appears before running a native feature:

```bash
adb devices
```

For iOS execution, use macOS with Xcode and an available iOS Simulator, or use an appropriately provisioned real device. The checked-in native configuration points to an unpacked simulator `.app`; it does not make a device-only IPA compatible with a simulator. Inspect available simulators with:

```bash
xcrun simctl list devices
```

The repository supplies sample assets for local validation: [`TheApp.apk`](../../src/test/resources/mobile/apps/TheApp.apk) for Android and [`ios-unzipped/TheApp.app`](../../src/test/resources/mobile/apps/ios-unzipped/TheApp.app) for an iOS simulator. The artifact directory also contains [`CHECKSUMS.sha256`](../../src/test/resources/mobile/apps/CHECKSUMS.sha256). The current native configuration points to those relative paths. [8] [9]

Before a project-specific run, confirm the following without committing sensitive values:

1. The Appium server is running and its URL is represented only in the local/native configuration.
2. The selected emulator, simulator, or real device matches `device_name`, optional `platform_version`, and optional `udid`.
3. The configured Android artifact is an `.apk`; the iOS simulator artifact is a simulator-built `.app`; and a real iPhone/iPad flow uses a signed `.ipa` with the required real-device and WebDriverAgent settings.
4. The app path is relative to the repository, and platform locators have been inspected in the target environment.

## Configuration files and key settings

`MobileYamlReader` loads and deep-merges only YAML files beneath `src/test/resources/mobile/config` and `src/test/resources/mobile/elements`. This intentional scope prevents YAML embedded in an app bundle or vendor resource from being parsed as framework configuration. Therefore, native configuration belongs in those two directories; do not place runtime mobile YAML inside `mobile/apps`. [6]

| File | Native role | Key settings to maintain |
|---|---|---|
| [`mobile-config.yml`](../../src/test/resources/mobile/config/mobile-config.yml) | Shared Appium/mobile behavior. | `mobile.enabled`, `appium_server_url`, `default_platform`, `implicit_wait_seconds`, `explicit_wait_seconds`, `new_command_timeout_seconds`, `permissions.*`, and `evidence.*`. |
| [`mobile-native-config.yml`](../../src/test/resources/mobile/config/mobile-native-config.yml) | Native application capabilities only. | `mobile.android.*` and `mobile.ios.*`, including `app`, device selection, reset behavior, orientation, Android package/activity/wait settings, and iOS bundle/WDA settings. |
| [`mobile-browser-config.yml`](../../src/test/resources/mobile/config/mobile-browser-config.yml) | **Not native app configuration.** | Appium Chrome/Safari browser settings under `mobile_browser_appium`; leave native app capabilities out of this file. |
| [`theapp_elements.yml`](../../src/test/resources/mobile/elements/theapp_elements.yml) | Example locator map for the sample native app. | `mobile_elements.theapp.<key>` entries, including a platform-specific `savedMessage`. |
| [`fnb_elements.yml`](../../src/test/resources/mobile/elements/fnb_elements.yml) | Example FNB native smoke locator map. | `mobile_elements.fnb.<key>` with separate Android and iOS paths for app-root and broad visible-element checks. |
| [`mobile_permissions.yml`](../../src/test/resources/mobile/elements/mobile_permissions.yml) | Documentation/sample locator map for OS dialogs. | The Java handler has built-in candidates, so features do not need to reference these keys unless a project needs stricter locator assertions. |
| [`config/config.yml`](../../src/test/resources/config/config.yml) | Cross-cutting report and soft-assertion settings. | `reporting.per_feature_*` controls the optional per-feature report listener; `soft_assertions.enabled` affects how mobile element lookup failures are deferred. |

### Shared configuration example

Use the checked-in files as the canonical key names. Replace placeholders locally; do not copy device IDs, signing identifiers, production application values, or private server values into feature files.

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

### Native capability example

The factory applies only nonblank optional strings and passes booleans such as reset and permission options to the platform-specific Appium options. Android uses `UiAutomator2Options`; iOS uses `XCUITestOptions`. The factory applies a valid `PORTRAIT` or `LANDSCAPE` capability and also attempts `mobile: setDeviceOrientation` after the session begins. [7]

```yaml
mobile:
  android:
    automation_name: "UiAutomator2"
    platform_name: "Android"
    device_name: "<android-device-name>"
    platform_version: "<android-platform-version-or-empty>"
    udid: "<android-udid-or-empty>"
    orientation: "<portrait-or-landscape>"
    app: "src/test/resources/mobile/apps/<android-app>.apk"
    app_package: "<android-package-or-empty>"
    app_activity: "<android-activity-or-empty>"
    app_wait_package: "<android-wait-package-or-empty>"
    app_wait_activity: "<android-wait-activity-or-empty>"
    system_port: "<android-system-port-or-empty>"
    adb_exec_timeout: "<milliseconds-or-empty>"
    auto_grant_permissions: <true-or-false>
    no_reset: <true-or-false>
    full_reset: <true-or-false>

  ios:
    automation_name: "XCUITest"
    platform_name: "iOS"
    device_name: "<ios-device-or-simulator-name>"
    platform_version: "<ios-version-or-empty>"
    udid: "<ios-udid-or-empty>"
    orientation: "<portrait-or-landscape>"
    app: "src/test/resources/mobile/apps/<ios-simulator-app-or-device-ipa>"
    bundle_id: "<ios-bundle-id-or-empty>"
    auto_accept_alerts: <true-or-false>
    auto_dismiss_alerts: <true-or-false>
    xcode_org_id: "<signing-team-id-or-empty>"
    xcode_signing_id: "<signing-identity-or-empty>"
    updated_wda_bundle_id: "<webdriveragent-bundle-id-or-empty>"
    wda_local_port: "<port-or-empty>"
    no_reset: <true-or-false>
    full_reset: <true-or-false>
```

### Platform selection and safe artifact selection

For a tagged native scenario, platform resolution is deterministic: `-Dmobile.platform=android|ios` has highest priority; then an `@android` or `@ios` scenario tag; then `mobile.default_platform`. A scenario with both platform tags is rejected as ambiguous. A cross-platform feature should use `@mobile` or `@cross_platform` without both platform tags, then choose the target through a command-line property or the shared configuration. [4]

| Target session | Appropriate artifact and configuration | Safe execution note |
|---|---|---|
| Android emulator or Android device | An `.apk` under `src/test/resources/mobile/apps/` (or another approved repository-relative path) in `mobile.android.app`. Optionally specify package/activity when the project requires them. | Keep `system_port` unique when the environment launches concurrent Android sessions. Avoid reset changes unless the test requires a clean install/state. |
| iOS Simulator | A build produced for the simulator, normally an unpacked `.app`, in `mobile.ios.app`. Leave `bundle_id` blank when launching by `.app` unless the project requires an installed-app launch. | Do not try to install a device IPA on a simulator. Match the chosen Simulator and optional `udid` to the built artifact. |
| Real iPhone or iPad | A signed `.ipa` in `mobile.ios.app`, with the device `udid` and applicable Xcode/WebDriverAgent settings. | The factory logs a warning if an IPA is configured without an iOS UDID. Keep signing IDs and WDA identifiers local and redacted in shared documentation. |

## Build and exact run commands

The mobile runner is a JUnit Cucumber runner, not the default TestNG suite. Its feature root is `src/test/resources/features/mobile`, its glue is `com.ptaf.stepdefinitions` and `com.ptaf.hooks`, and its checked-in tag expression is `@theapp_smoke`. Run it explicitly through Surefire. [5] [1]

```bash
# Compile the project and test sources without executing tests.
mvn clean test-compile -DskipTests

# Run the runner using its checked-in @theapp_smoke selection.
mvn test -Dtest=MobileTestRunner

# Run an approved cross-platform native suite on Android.
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@cross_platform" -Dmobile.platform=android

# Run the same approved cross-platform native suite on iOS.
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@cross_platform" -Dmobile.platform=ios

# Run an Android-only native suite selected by an explicit platform tag.
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@android" -Dmobile.platform=android

# Run an iOS-only native suite selected by an explicit platform tag.
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@ios" -Dmobile.platform=ios
```

The `cucumber.filter.tags` property is the repository-documented way to select a different mobile tag set at run time. Keep the runner minimal; use feature tags and command-line selection rather than adding execution logic to `MobileTestRunner`. [5] [8]

Do not start a native driver explicitly in a normal `@mobile`, `@android`, `@ios`, or `@cross_platform` feature. `MobileHooks` creates it before the scenario. The step `Given I start mobile application using platform "…"` exists for controlled/manual driver starts, but it calls `MobileDriverManager.startDriver`, which closes the thread's existing driver first. Using it inside an already hooked scenario unnecessarily replaces the session. [4] [10]

## Create a new native feature/test

Create a native test in the following order. This keeps application capability data, locators, and business-readable scenario flow separate.

1. **Add or validate the app artifact.** Place an approved Android APK or iOS simulator `.app` in `src/test/resources/mobile/apps/`, or use another approved repository-relative location. Update the correct platform block in `mobile-native-config.yml` only. Do not put environment-specific secrets or device-farm identifiers into feature text.
2. **Create platform-aware locator keys.** Add an app-specific YAML file under `src/test/resources/mobile/elements/`. It must have a `mobile_elements` root, a word-safe page key, and word-safe locator keys because the shipped Gherkin expressions use `{word}` for page and locator names. Provide an `android`, `ios`, `default`, or `common` value whenever platform differences require it. [11]
3. **Create the feature.** Add `src/test/resources/features/mobile/<approved-feature-name>.feature`. Tag it `@mobile` or `@cross_platform`; use exactly one of `@android`/`@ios` only when a scenario is platform-specific. The hook creates the application session automatically. [4]
4. **Use existing steps before adding Java.** `MobileSteps` already provides tap, type, clear, visibility/text assertions, gestures, waits, orientation, context switch, permissions, screenshots, clipboard, file transfer, and app lifecycle steps. Add Java only if no existing step represents the required behavior. [10]
5. **Execute a narrow tag first.** Run the approved tag on one platform. Review the Cucumber, Extent, screenshot, and optional video artifacts before widening the tag selection or enabling parallel infrastructure.

### Tested-style native feature template

The following is a template using the Gherkin forms implemented in `MobileSteps`. Replace every angle-bracket value with a project-approved, non-sensitive value before executing.

```gherkin
@mobile @cross_platform @<suite_tag>
Feature: <application_name> native mobile smoke

  Scenario: Verify the configured application reaches a stable screen
    When I allow all mobile permission popups if displayed
    When I wait up to <ready_timeout_seconds> seconds for mobile page <page_key> locator <ready_marker_key> to be visible
    Then I verify mobile page <page_key> locator <ready_marker_key> is visible
    When I capture mobile screenshot named "<checkpoint_name>"

  Scenario: Complete a simple native interaction
    When I wait up to <control_timeout_seconds> seconds for mobile page <page_key> locator <input_key> to be visible
    When I enter mobile value "<non_sensitive_test_value>" on page <page_key> locator <input_key>
    When I hide mobile keyboard
    When I tap on mobile page <page_key> locator <submit_key>
    Then I verify mobile page <page_key> locator <result_key> text contains "<expected_non_sensitive_text>"
```

### Tested-style locator template

For native apps, use stable accessibility identifiers, resource identifiers, Android UiAutomator selectors, or iOS predicates/class chains where available. The resolver supports a single string or a platform map. [11] [12]

```yaml
mobile_elements:
  <page_key>:
    <ready_marker_key>:
      android: "ACCESSIBILITY_ID_<android_accessibility_id>"
      ios: "ACCESSIBILITY_ID_<ios_accessibility_id>"
    <input_key>:
      android: "ID_<android_resource_id>"
      ios: "IOS_PREDICATE_name == '<ios_accessibility_name>'"
    <submit_key>:
      android: "ANDROID_UIAUTOMATOR_new UiSelector().description(\"<android_content_description>\")"
      ios: "IOS_CLASS_CHAIN_**/XCUIElementTypeButton[`name == \"<ios_button_name>\"`]"
    <result_key>:
      default: "XPATH_<approved_xpath>"
```

## Features, data, locators, payloads, and queries

The native module has dedicated feature, configuration, locator, and app-artifact locations. Its supplied step definitions do not consume a native payload, query, or external data-file convention. If a native scenario needs setup or verification through an API, database, XML, CSV, or performance module, keep that material in the owning module rather than creating an undocumented mobile payload/query location.

| Item | Native location/use | Boundary or owning location |
|---|---|---|
| Native feature files | [`src/test/resources/features/mobile/`](../../src/test/resources/features/mobile/) | Use `MobileTestRunner`; example native flows are [`theapp_cross_platform_workflow.feature`](../../src/test/resources/features/mobile/theapp_cross_platform_workflow.feature) and [`fnb_cross_platform_smoke.feature`](../../src/test/resources/features/mobile/fnb_cross_platform_smoke.feature). |
| Mobile step definitions | [`src/test/java/com/ptaf/stepdefinitions/MobileSteps.java`](../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java) | Reuse this grammar rather than duplicating generic mobile steps. |
| Native configuration | [`src/test/resources/mobile/config/`](../../src/test/resources/mobile/config/) | Shared settings are in `mobile-config.yml`; native capabilities are in `mobile-native-config.yml`. |
| Native locators | [`src/test/resources/mobile/elements/`](../../src/test/resources/mobile/elements/) | Store under `mobile_elements.<page>.<key>`. Sample files are [`theapp_elements.yml`](../../src/test/resources/mobile/elements/theapp_elements.yml), [`fnb_elements.yml`](../../src/test/resources/mobile/elements/fnb_elements.yml), and [`unified_locator_examples.yml`](../../src/test/resources/mobile/elements/unified_locator_examples.yml). |
| App artifacts | [`src/test/resources/mobile/apps/`](../../src/test/resources/mobile/apps/) | Use approved Android APKs, iOS simulator `.app` bundles, or appropriately signed real-device IPAs. |
| Native test data | No dedicated native data-file reader is implemented by `MobileSteps`. | Generic resources exist under [`src/test/resources/data/`](../../src/test/resources/data/); use them only through the module that owns their parsing/steps. |
| API request payloads | Not a native Appium input. | [`src/test/resources/api_requests/`](../../src/test/resources/api_requests/) and API steps own API request definitions. |
| Database queries | Not a native Appium input. | [`src/test/resources/queries/`](../../src/test/resources/queries/) and database steps own SQL query definitions. |
| Performance payloads | Not a native Appium input. | [`src/test/resources/performance/payloads/`](../../src/test/resources/performance/payloads/) belongs to the performance module. |

### Locator formats and selection rules

`MobileLocatorHandler` accepts these established Appium prefixes: `ACCESSIBILITY_ID_`, `ID_`, `XPATH_`, `CLASS_NAME_`, `NAME_`, `ANDROID_UIAUTOMATOR_`, `IOS_PREDICATE_`, and `IOS_CLASS_CHAIN_`. It also accepts UI-style aliases such as `CSS_`, `TAG_`, `CLASS_`, `TESTID_`, `PLACEHOLDER_`, `LABEL_`, `TITLE_`, `ALTTEXT_`, `TEXT_`, `LINKTEXT_`, `BUTTON_`, `TEXTBOX_`/`INPUT_`, and several role-oriented aliases. For a native AUT, prefer the explicit native forms; UI-style forms are compatibility support and are especially useful in real browser sessions. [12]

For an object-valued locator, the resolver selects `android` or `ios` for a native session, then falls back to `default` or `common`. It first reads `mobile_elements.<page>.<key>`. Only an Appium **browser** session may then fall back to the regular UI `elements.<page>.<key>` store, so native test authors must keep native AUT locators in `mobile_elements`. [11]

## Lifecycle, permissions, waits, and mobile feature patterns

### Lifecycle and evidence behavior

A scenario is mobile when it has one of these tags: `@mobile`, `@android`, `@ios`, `@cross_platform`, `@appium_browser`, or `@mobile_browser_real`. For native tags, `MobileHooks` stores the Cucumber scenario for evidence, resolves the platform, calls `MobileDriverManager.startDriver`, and starts video only when enabled. After the scenario, it optionally captures a final screenshot, stops recording, quits the Appium driver, and clears its scenario reference. [4]

`MobileDriverManager` stores the driver and chosen platform in `ThreadLocal` state. This makes each executing thread hold a separate Appium session. It does not remove the device-side requirement for distinct devices, or for distinct Android `system_port` values where concurrent sessions need them. [13] [7]

### Permissions

Use `auto_grant_permissions` in the Android capability block for simple, disposable smoke environments. For iOS, use the native `auto_accept_alerts` or `auto_dismiss_alerts` capability only when blanket alert behavior matches the test objective. For consent/permission behavior that must be visible in the scenario, use an explicit safe step. [7] [14]

```gherkin
When I allow mobile permission popup if displayed
When I deny mobile permission popup if displayed
When I allow mobile permission popup with text "<approved_button_text>" if displayed
When I allow all mobile permission popups if displayed
When I deny all mobile permission popups if displayed
When I handle mobile permission popup using action "<allow_or_deny_or_approved_label>" if displayed
```

The permission handler waits only for the configured `mobile.permissions.popup_timeout_seconds`, tries Android or iOS system-UI candidates, and returns without failing if no matching dialog is shown. `allow all` and `deny all` stop when no dialog is found and are bounded by `max_popups_to_handle`. If `capture_evidence` is enabled, the handler requests a before screenshot for its attempt and an after screenshot following a successful click. [14]

`I grant mobile permission "…" for app "…"` and its revoke counterpart call Appium's `mobile: changePermissions` command with an Android `appPackage`. Treat these as Android-specific steps. For iOS, use controlled alert capabilities or the safe system-dialog handling steps instead. [15] [10]

### Wait strategy

The shared configuration has a zero implicit wait and a framework explicit wait. Native actions that locate an element use `MobileCommonMethods.findVisibleElement`, which waits first for presence and then for visibility using `mobile.explicit_wait_seconds`. An action continues as soon as visibility is reached; it does not intentionally wait out the whole timeout. [2] [11]

Use checkpoint-based waits after navigation, login, deep links, permission handling, and state changes. Use a fixed pause only for a transition that exposes no reliable app state.

```gherkin
When I wait up to <seconds> seconds for mobile page <page_key> locator <ready_key> to be visible
When I wait up to <seconds> seconds for mobile page <page_key> locator <loading_indicator_key> to disappear
When I pause mobile execution for <seconds> seconds
```

### Supported native patterns

`MobileSteps` exposes the following tested-style patterns. They delegate to `MobileActionImpl` and `MobileCommonMethods`; values below remain placeholders by design. [10] [9]

| Pattern | Example Gherkin form |
|---|---|
| Tap, type, clear, assert | `When I tap on mobile page <page_key> locator <tap_key>`; `When I enter mobile value "<value>" on page <page_key> locator <input_key>`; `Then I verify mobile page <page_key> locator <result_key> is visible` |
| Gestures and scrolling | `When I long press mobile page <page_key> locator <key> for <milliseconds> milliseconds`; `When I scroll mobile page <page_key> locator <key> into view with max <count> swipes`; `When I swipe mobile screen up` |
| Keyboard and orientation | `When I hide mobile keyboard`; `When I rotate mobile screen to "<portrait_or_landscape>"`; `When I rotate mobile screen using configured orientation` |
| App/device operations | `When I background mobile app for <seconds> seconds`; `When I activate mobile app "<package_or_bundle_id>"`; `When I open mobile deep link "<deep_link>" for app "<package_or_bundle_id>"` |
| Hybrid context | `When I switch mobile context to "<webview_context_name>"`; `When I switch mobile context to native app` |
| Files and clipboard | `When I push local file "<local_path>" to mobile path "<device_path>"`; `When I pull mobile file "<device_path>" to local path "<local_path>"`; `Then I verify mobile clipboard text contains "<expected_text>"` |
| Evidence | `When I capture mobile screenshot named "<checkpoint_name>"` |

Coordinate taps, XPath, app lifecycle operations, file-system paths, clipboard functions, and deep links are supported but are inherently device/application dependent. Prefer semantic locators and a stable page checkpoint whenever the app provides one. [9] [10]

## Expected reports and artifacts

The mobile runner directly writes Cucumber artifacts to `target/cucumber-reports/`:

```text
target/cucumber-reports/
  mobile-report.html
  mobile-report.json
  mobile-report.xml
```

The runner also registers the Extent Cucumber adapter, `com.ptaf.reporting.PerFeatureReportListener`, and `com.ptaf.reporting.SoftAssertionReportListener`. The repository's `extent.properties` configures timestamped run folders beneath `test-output/` with Spark HTML, Base64 HTML, PDF, and Excel reports. Per-feature HTML/PDF behavior is controlled by the `reporting.per_feature_*` settings in `src/test/resources/config/config.yml`. [5] [16] [17]

Native evidence is independent of those report folders. `MobileEvidenceManager` creates one JVM run ID in `yyyyMMdd_HHmmss` form and writes under the configured evidence root, which defaults to `test-output/mobile-evidence`. Expected paths are:

```text
test-output/mobile-evidence/<run-id>/
  screenshots/<scenario>_<assertion>.png
  target-output/screenshots/<scenario_or_checkpoint>.png
  videos/<feature-name>/<feature-name>_<timestamp>.mp4
```

An assertion failure requests an immediate screenshot. On teardown, a failed scenario receives a final screenshot when `screenshot_on_failure` is enabled; passed and all-scenario capture behavior is controlled separately. Named screenshot steps always write a PNG if the driver can provide one. Screen recording is attempted only when `video_recording_enabled` is true and the driver implements `CanRecordScreen`. When `video_on_failure_only` is true, a passing scenario's recorded payload is discarded; otherwise a decoded MP4 is stored under the feature-name directory. Screenshot/video attachments to Cucumber are independently controlled by `attach_screenshots_to_report` and `attach_video_to_report`. [16] [18]

If cross-cutting soft assertions are enabled, `MobileCommonMethods` records certain element-lookup failures and allows execution to continue. `MobileHooks` turns the accumulated failures into a scenario failure during teardown; the mobile driver is still cleaned up in the `finally` path. [4] [11] [17]

## Troubleshooting

| Symptom | Repository-supported diagnosis and corrective action |
|---|---|
| `No Appium driver is available for this thread` | The scenario was not recognized as mobile, or a mobile step ran before setup. Add one supported native tag and run `MobileTestRunner`; do not invoke mobile steps from an untagged feature. [4] [13] |
| A cross-platform feature opens the wrong platform | Check priority: `-Dmobile.platform` overrides tags, and tags override `mobile.default_platform`. Remove either `@android` or `@ios` if both appear; that combination throws an ambiguity error. [4] |
| Native session cannot start | Validate the configured Appium URL, selected device/simulator, installed Appium driver, and repository-relative app path. Verify the artifact type matches the target: Android `.apk`, simulator `.app`, or signed real-device `.ipa`. [7] [8] |
| iOS IPA cannot start on a simulator | Use a simulator-built `.app`. The factory logs guidance when an IPA is set without an iOS UDID because that normally indicates a real-device artifact/configuration mismatch. [7] |
| Android app starts but waits at the wrong screen | Recheck `app_package`, `app_activity`, `app_wait_package`, and `app_wait_activity` if the project uses them. Prefer a post-launch locator wait over a fixed sleep. [7] [11] |
| Locator is missing or uses the wrong platform selector | Add the key below `mobile_elements.<page>.<key>` in `src/test/resources/mobile/elements/`. Verify the page/key are word-safe. Supply the current `android` or `ios` member, or a `default`/`common` fallback. Native sessions do not fall back to regular web `elements`. [11] |
| Locator type is rejected | Use a supported prefix such as `ACCESSIBILITY_ID_`, `ID_`, `XPATH_`, `ANDROID_UIAUTOMATOR_`, `IOS_PREDICATE_`, or `IOS_CLASS_CHAIN_`. The resolver's diagnostic lists the unsupported raw value and supported forms. [12] |
| Permission dialog blocks a test | Choose either capability-level handling or the safe explicit permission steps. Confirm that the configured popup timeout and maximum loop count fit the app's prompt sequence; an absent popup is intentionally non-failing. [14] |
| Screenshot is absent after startup failure | A screenshot requires a live Appium driver. Correct server, device, driver, or app capability issues first. Once a session exists, inspect `test-output/mobile-evidence/<run-id>/`. [16] |
| No video is present | Verify `video_recording_enabled`, driver recording support, and `video_on_failure_only`. A passing scenario produces no saved MP4 while failure-only retention is enabled. [16] |
| Tests become slow or flaky after adding sleeps | Restore locator-based waits. The framework's normal action lookup performs presence then visibility waits; retain pauses only where an app exposes no observable state. [11] |
| Multiple mobile tests interfere under parallel execution | The framework isolates driver references with `ThreadLocal`, but mobile infrastructure must also isolate device/simulator allocation and Android system ports. Run a narrow tag on one target first, then introduce approved parallel infrastructure. [13] [7] |

## Module boundaries with other framework modules

| Module | Relationship to native mobile Appium | Do not mix these responsibilities |
|---|---|---|
| Appium real mobile browser | Separate Appium session path, selected by `@appium_browser` or `@mobile_browser_real`, using [`mobile-browser-config.yml`](../../src/test/resources/mobile/config/mobile-browser-config.yml). It opens Android Chrome or iOS Safari and may reuse web locators. | Do not put `browserName`, ChromeDriver, Safari WebView, or clean-start settings in `mobile-native-config.yml`. Do not tag a native AUT scenario as a browser scenario. [3] [4] [7] |
| Playwright mobile-browser emulation | Uses the Playwright UI stack and device/browser profiles beneath [`src/test/resources/mobile_browser/`](../../src/test/resources/mobile_browser/). It does not boot an Appium device session. | Do not treat a Playwright viewport profile as native Android/iOS app automation. [19] |
| Standard web UI | Owns Playwright browser setup, ordinary web locators under [`src/test/resources/elements/`](../../src/test/resources/elements/), and browser evidence. | Native locators belong under `mobile_elements`, and native lifecycle comes from `MobileHooks`, not the web UI hook. [11] [19] |
| API, database, XML, CSV | May provide setup or verification at the framework level, with request/query/data resources in their own folders. | Do not add API payloads or SQL queries to the native mobile resource tree merely because a mobile workflow depends on them. [20] |
| UI performance | A dedicated real-browser load module with separate configuration, runner, data, locator, report, and feature roots. | Do not use native Appium tests as UI performance workers, and do not place native capabilities in `ui_performance` resources. [21] |
| Reporting and soft assertions | Mobile runner registers the normal Cucumber/Extent reports and the optional per-feature listener; `MobileEvidenceManager` contributes native media. | Do not assume Playwright video settings govern Appium recording; use `mobile.evidence.*`. [5] [16] |

## References

[1]: ../../pom.xml "FNB-ETAF Maven build: Java, Appium, Cucumber, JUnit, and Surefire configuration"
[2]: ../../src/test/resources/mobile/config/mobile-config.yml "Shared Appium mobile configuration"
[3]: ../../src/test/resources/mobile/config/mobile-browser-config.yml "Appium real mobile browser capability configuration"
[4]: ../../src/main/java/com/ptaf/hooks/MobileHooks.java "Native mobile Appium lifecycle hooks"
[5]: ../../src/test/java/com/ptaf/runners/MobileTestRunner.java "Dedicated Appium mobile Cucumber runner"
[6]: ../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java "Scoped mobile YAML reader"
[7]: ../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java "Appium native and browser driver factory"
[8]: ../../src/test/resources/mobile/README.md "PTAF native mobile Appium resource guide"
[9]: ../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java "Reusable Appium mobile actions, waits, and locator resolution"
[10]: ../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java "Native mobile Cucumber step definitions"
[11]: ../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java "Mobile locator lookup and explicit wait implementation"
[12]: ../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java "Mobile locator format resolver"
[13]: ../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java "Thread-local Appium driver lifecycle manager"
[14]: ../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java "Safe mobile permission and system-dialog handling"
[15]: ../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java "Mobile action contract and platform caveats"
[16]: ../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java "Native screenshot and screen-recording evidence manager"
[17]: ../../src/test/resources/config/config.yml "Framework reporting and soft-assertion configuration"
[18]: ../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java "Feature-based artifact naming utility"
[19]: ../../src/test/resources/mobile_browser/README.md "Playwright mobile browser emulation resources"
[20]: ../../ReadMe.md "PTAF framework resource layout and module overview"
[21]: ../../FNB-ETAF_UI_Performance_Automation_Guide.md "FNB-ETAF dedicated UI performance module guide"
