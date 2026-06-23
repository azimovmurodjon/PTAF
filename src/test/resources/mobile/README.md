# PTAF Native Mobile Appium Automation

This folder contains native Android and iOS automation resources for Appium. The design follows the same PTAF concept used by the rest of the framework: Cucumber feature files, Java step definitions, reusable framework classes, YAML-driven configuration, YAML-driven locators, screenshots, videos, and reports.

## Included Sample Apps

The project includes official Appium Pro TheApp release artifacts for immediate local validation.

| File | Purpose |
|---|---|
| `apps/TheApp.apk` | Android sample app for UiAutomator2 execution. |
| `apps/TheApp.app.zip` | iOS simulator sample app archive. |
| `apps/ios-unzipped/TheApp.app` | Unzipped iOS simulator `.app` referenced by `mobile-config.yml`. |

## Main Configuration

All native mobile runtime behavior is controlled from:

```text
src/test/resources/mobile/config/mobile-config.yml
```

| Config Key | Purpose |
|---|---|
| `mobile.appium_server_url` | Appium 2 server endpoint. |
| `mobile.default_platform` | Default platform when not selected by tag. |
| `mobile.evidence.screenshot_on_failure` | Saves/attaches screenshots only when a scenario fails. |
| `mobile.evidence.screenshot_on_pass` | Saves/attaches screenshots for passed scenarios. |
| `mobile.evidence.screenshot_after_each_scenario` | Saves/attaches screenshots for every scenario regardless of result. |
| `mobile.evidence.video_recording_enabled` | Starts/stops Appium native screen recording for each scenario. |
| `mobile.evidence.video_on_failure_only` | Keeps videos only for failed scenarios when recording is enabled. |
| `mobile.android.orientation` | `portrait` or `landscape` for Android sessions. |
| `mobile.ios.orientation` | `portrait` or `landscape` for iOS simulator sessions. |
| `mobile.ios.bundle_id` | Leave blank when launching the included `.app`; Appium will detect the bundle ID. |

## How to Run Locally on macOS

Start Appium 2 and install the required drivers.

```bash
appium driver install uiautomator2
appium driver install xcuitest
appium
```

For Android, start an emulator and confirm it is visible.

```bash
adb devices
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@android"
```

For iOS, start an iOS Simulator and confirm it is visible.

```bash
xcrun simctl list devices
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@ios"
```

## iOS Bundle ID Guidance

The sample config intentionally keeps `ios.bundle_id` blank because the framework launches by `.app` path. If your real application requires an explicit bundle ID, get it from the app itself.

```bash
/usr/libexec/PlistBuddy -c "Print CFBundleIdentifier" src/test/resources/mobile/apps/ios-unzipped/TheApp.app/Info.plist
```

Then place the printed value into `mobile-config.yml`.

## Orientation Usage

Set orientation from configuration:

```yaml
android:
  orientation: "landscape"

ios:
  orientation: "portrait"
```

You can also rotate inside a scenario:

```gherkin
When I rotate mobile screen to "landscape"
When I rotate mobile screen to "portrait"
```

## Evidence Output

Native mobile screenshots and videos are written under:

```text
test-output/mobile-evidence/<run-id>/
```

Evidence can also be attached to Cucumber/Extent reports depending on the flags in `mobile-config.yml`. Keep full video recording disabled by default for daily local runs because video can slow down emulators and make reports very large.

## Enterprise Team Guidance

For large teams, do not hard-code local absolute paths. Keep application paths relative to the project root, store locators in YAML, and control runtime behavior through configuration files. This keeps the framework reusable across many projects without requiring code changes for each team.

## Expanded Native Mobile Actions

The native Appium module now includes a broader action library so testers can automate common Android and iOS behavior directly from feature files.

| Action Type | Example Step |
|---|---|
| Tap element | `When I tap on mobile page theapp locator echoBoxMenu` |
| Long press | `When I long press mobile page theapp locator echoBoxMenu for 1000 milliseconds` |
| Double tap | `When I double tap mobile page theapp locator echoBoxMenu` |
| Coordinate tap | `When I tap mobile screen at x 200 y 400` |
| Drag and drop | `When I drag mobile page source locator item to page target locator dropZone` |
| Scroll to element | `When I scroll mobile page theapp locator loginMenu into view with max 5 swipes` |
| Scroll to text | `When I scroll mobile screen to text "Login"` |
| Background app | `When I background mobile app for 5 seconds` |
| Pinch/zoom | `When I pinch in mobile screen` and `When I zoom out mobile screen` |
| Deep link | `When I open mobile deep link "myapp://home" for app "com.company.app"` |
| Context switch | `When I switch mobile context to "WEBVIEW_1"` and `When I switch mobile context to native app` |
| Clipboard | `When I set mobile clipboard text "sample"` and `Then I verify mobile clipboard text contains "sample"` |
| File transfer | `When I push local file "local/path.txt" to mobile path "/sdcard/path.txt"` |
| Permissions | `When I grant mobile permission "android.permission.CAMERA" for app "com.company.app"` |

These actions are connected to the same Cucumber/Extent reporting flow through `MobileHooks` and `MobileEvidenceManager`. Screenshot and video behavior remains controlled from `mobile-config.yml`.

## Enterprise Permission and System Dialog Handling

Native mobile apps often display operating-system dialogs for location, camera, photos, microphone, notifications, Bluetooth, contacts, calendar, and similar permissions. These popups are rendered by Android or iOS system UI rather than by the app screen itself, so PTAF provides both capability-level automation and explicit Cucumber steps for controlled test flows.

| Strategy | Recommended Use | Configuration or Step |
|---|---|---|
| Android automatic permission grant | Fast smoke tests after fresh install | `android.auto_grant_permissions: "true"` |
| iOS automatic alert handling | Very simple flows where all alerts can be accepted or dismissed | `ios.auto_accept_alerts: "true"` or `ios.auto_dismiss_alerts: "true"` |
| Explicit permission steps | Enterprise tests that need screenshots, reports, and selective handling | `When I allow mobile permission popup if displayed` |

The explicit permission steps are designed to be **safe when no popup exists**. They do not fail if the device has already granted permissions, which allows the same feature to run on clean devices, reused devices, local emulators, real devices, and CI device farms.

```gherkin
When I allow mobile permission popup if displayed
When I deny mobile permission popup if displayed
When I allow mobile permission popup with text "Allow While Using App" if displayed
When I allow all mobile permission popups if displayed
When I deny all mobile permission popups if displayed
When I handle mobile permission popup using action "allow" if displayed
```

Permission behavior is controlled from `mobile-config.yml`.

```yaml
mobile:
  permissions:
    popup_timeout_seconds: 3
    max_popups_to_handle: 5
    capture_evidence: true
```

When `capture_evidence` is enabled, PTAF captures before/after screenshots for explicit permission handling and attaches them to the active Cucumber report when report attachment is enabled. This gives reviewers a clear audit trail of permission behavior without requiring project teams to write Java code.

## Timeout and Wait Steps

Use explicit wait steps when an application screen needs additional time to render after login, permission handling, deep links, network calls, or animation-heavy flows.

| Step | Purpose |
|---|---|
| `When I wait up to 10 seconds for mobile page theapp locator echoBoxMenu to be visible` | Waits for a known checkpoint to appear. |
| `When I wait up to 10 seconds for mobile page theapp locator loadingSpinner to disappear` | Waits for loading indicators to finish. |
| `When I pause mobile execution for 2 seconds` | Last-resort pause for transitions that do not expose reliable locators. |

Prefer locator-based waits over fixed pauses. The pause step exists for rare cases only and should not be the default synchronization strategy.

## App Artifact Compatibility Rules

Mobile automation cannot make a device-only iOS build run on an iOS Simulator. This is an Apple build-target constraint, not a PTAF or Appium issue. Appium capabilities describe the target session, but the application binary must still match the platform where it is installed.

| Target | Correct App Artifact | Config Example |
|---|---|---|
| Android Emulator / Device | `.apk` | `android.app: "src/test/resources/mobile/apps/YourApp.apk"` |
| iOS Simulator | simulator-built `.app` | `ios.app: "src/test/resources/mobile/apps/YourSimulatorApp.app"` |
| Real iPhone / iPad | signed `.ipa` | `ios.app: "src/test/resources/mobile/apps/YourApp.ipa"` with `ios.udid` |

For iOS simulator testing, request a build compiled for the `iphonesimulator` SDK. For real-device IPA testing, configure `ios.udid`, signing-related WebDriverAgent values if required, and ensure the IPA provisioning profile includes the target device.

## Recommended Native Mobile Smoke Pattern

A stable enterprise smoke test should launch the app, handle optional permission prompts, verify a real screen checkpoint, and capture named screenshots for auditability.

```gherkin
@mobile @cross_platform @smoke @evidence
Scenario: Validate app launch and permission handling
  When I allow all mobile permission popups if displayed
  When I capture mobile screenshot named "01-after-permissions"
  When I wait up to 20 seconds for mobile page myapp locator homeScreen to be visible
  When I capture mobile screenshot named "02-home-screen"
```

Keep locators in `src/test/resources/mobile/elements`. Use platform-aware locator values when Android and iOS expose different accessibility identifiers for the same logical element.

```yaml
mobile_elements:
  myapp:
    homeScreen:
      android: "ACCESSIBILITY_ID_home_screen"
      ios: "ACCESSIBILITY_ID_homeScreen"
```

## Troubleshooting Quick Reference

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Simulator architecture is not supported` | iOS device build is being run on simulator. | Use simulator `.app` or run IPA on a real device. |
| Appium tries to open the wrong bundle ID | App path and `bundle_id` do not belong to the same app. | Match app path and bundle ID, or leave `bundle_id` blank when launching by `.app`. |
| YAML parser fails inside `.app` bundle | Framework scanned vendor YAML under `mobile/apps`. | Ensure `MobileYamlReader` loads only `mobile/config` and `mobile/elements`. |
| Android SDK path missing on Windows | `ANDROID_HOME` or `ANDROID_SDK_ROOT` is malformed. | Set both variables to the full SDK path, including drive letter. |
| Permission popup blocks test | Permission not auto-granted or prompt text differs. | Use explicit permission steps or update permission locators/config. |
| No screenshot on session startup failure | Appium session was never created. | Fix server/device/app capability issue first; screenshots require a live driver. |

## References

[1]: https://appium.io/docs/en/2.0/guides/caps/ "Appium Capabilities Documentation"
[2]: https://www.browserstack.com/docs/app-automate/appium/advanced-features/handle-permission-pop-ups "BrowserStack Appium Permission Pop-up Handling"

Appium defines capabilities as session-start parameters that describe the platform, automation driver, app, device, timeout, reset behavior, and other requested session characteristics.[1] Permission-popup handling may be automated through capabilities such as Android `autoGrantPermissions` and iOS `autoAcceptAlerts`/`autoDismissAlerts`, while selective permission scenarios can be handled by clicking platform-specific popup buttons during execution.[2]


## Appium Real Mobile Browser Automation

PTAF also supports automating the real browser installed on an emulator or simulator through Appium. This is different from Playwright mobile browser emulation. Appium browser mode opens Chrome on Android or Safari on iOS and drives the real browser through the mobile automation stack.

| Platform | Real Browser | Driver | Typical Use |
|---|---|---|---|
| Android emulator/device | Chrome | UiAutomator2 + ChromeDriver | Validate real Android Chrome behavior. |
| iOS simulator/device | Safari | XCUITest | Validate real iOS Safari behavior. |

Configure browser settings in `mobile-config.yml` under `mobile.browser`. Keep `mobile.browser.enabled` disabled by default so normal native app automation remains unchanged. Browser-mode scenarios should use `@appium_browser` or `@mobile_browser_real`.

```yaml
mobile:
  browser:
    enabled: false
    android:
      browser_name: "Chrome"
      chromedriver_autodownload: "true"
    ios:
      browser_name: "Safari"
      initial_url: "https://www.google.com"
```

Run Android Chrome on emulator:

```bash
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@google_mobile_browser" -Dmobile.platform=android
```

Run iOS Safari on simulator:

```bash
mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@google_mobile_browser" -Dmobile.platform=ios
```

The sample feature is:

```text
src/test/resources/features/mobile/appium_mobile_browser_google_search.feature
```

The sample opens Google, enters a query, presses Enter, captures screenshots, saves page source, and validates the current URL. Screenshot and report attachment behavior uses the same mobile evidence settings used by native app tests.


## Separated Mobile Configuration Files

PTAF now supports a cleaner enterprise configuration layout. Native application automation and Appium real mobile browser automation can be configured in separate files under `src/test/resources/mobile/config`.

| File | Purpose |
|---|---|
| `mobile-native-config.yml` | Native Android APK, iOS simulator `.app`, and real-device iOS `.ipa` automation. |
| `mobile-browser-config.yml` | Real Chrome/Safari browser sessions through Appium. |
| `mobile-config.yml` | Backward-compatible legacy combined file. Existing teams can keep using it while migrating. |

The framework reads all YAML files under `mobile/config` and gives priority to the split `mobile_browser_appium` browser configuration when it exists. This allows browser automation settings to evolve without accidentally changing native app capabilities.

For native iOS application automation, alert handling can be controlled directly in the native `ios` block:

```yaml
ios:
  auto_accept_alerts: "false"
  auto_dismiss_alerts: "false"
```

For iOS Safari browser automation, use the browser-specific file and keep Safari settings under `mobile_browser_appium.ios`.

## Unified Locator Support for Appium Native and Real Mobile Browser Automation

PTAF now supports a unified mobile locator resolver for Appium-based automation. This enhancement is additive and backward-compatible: existing mobile locators such as `ACCESSIBILITY_ID_`, `ID_`, `XPATH_`, `IOS_PREDICATE_`, `IOS_CLASS_CHAIN_`, and `ANDROID_UIAUTOMATOR_` continue to work exactly as before. The resolver also understands the simpler locator style commonly used by the Playwright UI module, such as `Button_Login`, `TEXTBOX_Search`, `TEXT_Save`, `CSS_.search`, `CLASS_result`, `TESTID_submit`, `PLACEHOLDER_Search`, and `LABEL_Email`.

The recommended enterprise rule is simple. For **native mobile apps**, teams should continue to prefer stable accessibility IDs, resource IDs, iOS predicates, and Android UiAutomator locators. For **Appium real mobile browser automation**, teams may reuse web-style locators and, when appropriate, existing `elements.<page>.<key>` storage from the Playwright UI side.

| Execution Type | Recommended Locator Style | Example |
|---|---|---|
| Native Android/iOS app | Stable accessibility/resource IDs | `ACCESSIBILITY_ID_loginButton` |
| Native iOS advanced | iOS predicate/class chain | `IOS_PREDICATE_name == 'Login'` |
| Native Android advanced | Android UiAutomator | `ANDROID_UIAUTOMATOR_new UiSelector().text("Login")` |
| Appium real Chrome/Safari browser | Web-style DOM locator | `CSS_input[name='q']` |
| Shared/simple alias | UI-style locator alias | `TEXTBOX_Search`, `Button_Login`, `TEXT_PTAF` |

Example:

```yaml
mobile_elements:
  loginPage:
    submitButton:
      android: "ACCESSIBILITY_ID_submitButton"
      ios: "ACCESSIBILITY_ID_submitButton"
      default: "Button_Submit"
```

For Appium real mobile browser mode, if a key is not found under `mobile_elements`, PTAF can fall back to existing Playwright-style `elements` storage. This makes it possible to reuse existing web page locators for real Chrome/Safari browser automation while keeping native app locators separate.

```yaml
elements:
  googleBrowser:
    searchBox: "TEXTBOX_Search"
    results: "TEXT_PTAF"
```

If locator resolution fails, PTAF now prints a detailed enterprise diagnostic showing the active mode, platform, page, key, primary lookup path, fallback lookup path, raw value, and recommended examples. This should make failures easier for large teams to troubleshoot without reading framework code.


### Appium Real Browser Clean-Start Behavior

For real mobile browser automation, repeatability is affected by existing Safari or Chrome tabs, cookies, start pages, and cached browser state. PTAF therefore supports a configurable clean-start sequence in `src/test/resources/mobile/config/mobile-browser-config.yml`.

```yaml
mobile_browser_appium:
  ios:
    clean_start_enabled: "true"
    clear_cookies: "true"
    close_existing_tabs: "true"
    terminate_before_start: "true"
    activate_after_cleanup: "true"
    browser_bundle_id: "com.apple.mobilesafari"

  android:
    clean_start_enabled: "true"
    clear_cookies: "true"
    terminate_before_start: "true"
    activate_after_cleanup: "true"
    reset_app_data: "false"
    browser_package: "com.android.chrome"
```

The cleanup is intentionally best-effort. Selenium cookie cleanup works only after a browser context is available. Android full app-data clearing may require Appium to be started with `--relaxed-security`. iOS Safari does not expose a universal Appium command to erase every tab and cache entry on all simulator versions, so PTAF restarts Safari and uses deterministic URL navigation, including the native address-bar fallback, to avoid stale Start Page behavior.
