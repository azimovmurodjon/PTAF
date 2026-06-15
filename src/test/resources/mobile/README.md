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
