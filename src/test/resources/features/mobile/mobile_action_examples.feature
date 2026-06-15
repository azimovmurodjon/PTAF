@mobile @documentation @examples
Feature: Native mobile action examples

  # This file documents reusable native mobile Appium steps.
  # Keep this feature excluded from normal smoke/regression tags unless your app supports every example locator/action.

  Scenario: Example syntax for expanded mobile actions
    # Element interactions
    When I tap on mobile page theapp locator echoBoxMenu
    When I long press mobile page theapp locator echoBoxMenu for 1000 milliseconds
    When I double tap mobile page theapp locator echoBoxMenu
    When I tap mobile screen at x 200 y 400

    # Gestures and scrolling
    When I swipe mobile screen up
    When I swipe mobile screen down
    When I swipe mobile screen left
    When I swipe mobile screen right
    When I scroll mobile page theapp locator loginMenu into view with max 5 swipes
    When I scroll mobile screen to text "Login"
    When I pinch in mobile screen
    When I zoom out mobile screen

    # App/device behavior
    When I rotate mobile screen to "landscape"
    When I rotate mobile screen to "portrait"
    When I background mobile app for 5 seconds
    When I activate mobile app "com.company.app"
    When I terminate mobile app "com.company.app"
    When I open mobile deep link "myapp://home" for app "com.company.app"

    # Hybrid/webview and device utilities
    When I switch mobile context to "WEBVIEW_1"
    When I switch mobile context to native app
    When I set mobile clipboard text "sample text"
    Then I verify mobile clipboard text contains "sample"

    # File and permission utilities
    When I push local file "src/test/resources/testdata/sample.txt" to mobile path "/sdcard/Download/sample.txt"
    When I pull mobile file "/sdcard/Download/sample.txt" to local path "test-output/mobile-evidence/pulled/sample.txt"
    When I grant mobile permission "android.permission.CAMERA" for app "com.company.app"
    When I revoke mobile permission "android.permission.CAMERA" for app "com.company.app"
