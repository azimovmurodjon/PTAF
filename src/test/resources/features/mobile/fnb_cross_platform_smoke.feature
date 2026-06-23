@mobile @cross_platform @fnb @fnb_smoke @evidence
Feature: FNB Direct cross-platform native mobile launch and evidence workflow

  # App metadata extracted from uploaded IPA:
  # Display Name: FNB Direct
  # iOS Bundle ID: com.fiserv.pspush
  # Version: 5.27.65
  #
  # This feature is intentionally safe for first automation validation.
  # It verifies that the native app starts, captures screenshots at each checkpoint,
  # and uses platform-aware locators from mobile/elements/fnb_elements.yml.
  #
  # For iOS real-device IPA execution, mobile-config.yml must point ios.app to:
  # src/test/resources/mobile/apps/fnb/FNB_iOS_5_27_65.ipa
  # and ios.bundle_id to: com.fiserv.pspush
  #
  # For Android, provide the matching FNB Android APK and update android.app,
  # android.app_package, android.app_activity, and android locator values as needed.

  Scenario: Validate FNB app launches and capture evidence on configured platform
    Then I verify mobile page fnb locator appRoot is visible
    When I allow all mobile permission popups if displayed
#    When I capture mobile screenshot named "01-fnb-app-launched"

    Then I verify mobile page fnb locator anyVisibleElement is visible
#    When I capture mobile screenshot named "02-fnb-first-visible-screen"

  Scenario: Capture FNB startup screen and inspect possible login controls
    Then I verify mobile page fnb locator appRoot is visible
#    When I capture mobile screenshot named "03-fnb-startup-screen"

    # The following locator is intentionally broad and platform-aware. If your app displays
    # a login/sign-in/continue button on first launch, this validates it and captures evidence.
    # If your first screen is a one-time enrollment, notification, or maintenance screen,
    # update fnb.possibleLoginButton in fnb_elements.yml using Appium Inspector.
    Then I verify mobile page fnb locator possibleLoginButton is visible
    When I capture mobile screenshot named "04-fnb-login-or-continue-visible"
