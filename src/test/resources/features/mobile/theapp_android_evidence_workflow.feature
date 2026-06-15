@mobile @android @theapp_sample @evidence @smoke
Feature: TheApp Android sample workflow with PTAF evidence capture

  # Purpose:
  # This feature demonstrates a reliable native mobile workflow using the included TheApp APK.
  # Screenshots and videos are controlled from:
  # src/test/resources/mobile/config/mobile-config.yml
  #
  # Recommended evidence settings while validating this sample:
  # mobile.evidence.screenshot_on_failure: true
  # mobile.evidence.screenshot_on_pass: true
  # mobile.evidence.screenshot_after_each_scenario: false
  # mobile.evidence.video_recording_enabled: false
  #
  # Important:
  # Runtime orientation commands are intentionally not used in this smoke workflow because
  # some Android UiAutomator2 driver versions do not support `mobile: setDeviceOrientation`.
  # If you need orientation coverage, set `mobile.android.orientation` in mobile-config.yml
  # before the session starts, or run a separate optional orientation-specific scenario.

  Scenario: Validate Echo Box workflow and capture native mobile evidence
    Then I verify mobile page theapp locator echoBoxMenu is visible

    When I tap on mobile page theapp locator echoBoxMenu
    Then I verify mobile page theapp locator echoInput is visible

    When I enter mobile value "PTAF native mobile screenshot evidence sample" on page theapp locator echoInput
    When I hide mobile keyboard
    When I tap on mobile page theapp locator saveButton
    Then I verify mobile page theapp locator savedMessage is visible
    Then I verify mobile page theapp locator savedMessage text contains "PTAF native mobile screenshot evidence sample"

  Scenario: Validate menu navigation and evidence on a second mobile screen
    Then I verify mobile page theapp locator echoBoxMenu is visible
    Then I verify mobile page theapp locator loginMenu is visible

    When I tap on mobile page theapp locator loginMenu
    Then I verify mobile page theapp locator username is visible
    Then I verify mobile page theapp locator password is visible
    Then I verify mobile page theapp locator loginButton is visible
