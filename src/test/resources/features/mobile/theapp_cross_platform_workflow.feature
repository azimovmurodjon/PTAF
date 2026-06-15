@mobile @cross_platform @theapp_sample @evidence @smoke
Feature: TheApp cross-platform native mobile workflow

  # Purpose:
  # This feature is intentionally platform-neutral. It does not use @android or @ios tags.
  # The platform is selected from src/test/resources/mobile/config/mobile-config.yml:
  #
  #   mobile:
  #     default_platform: "android"
  #
  # or:
  #
  #   mobile:
  #     default_platform: "ios"
  #
  # Run the same feature on either platform with:
  # mvn test -Dtest=MobileTestRunner -Dcucumber.filter.tags="@cross_platform"

  Scenario: Validate Echo Box workflow on configured mobile platform
    Then I verify mobile page theapp locator echoBoxMenu is visible

    When I tap on mobile page theapp locator echoBoxMenu
    Then I verify mobile page theapp locator echoInput is visible

    When I enter mobile value "PTAF cross-platform native mobile test" on page theapp locator echoInput
    When I hide mobile keyboard
    When I tap on mobile page theapp locator saveButton

    Then I verify mobile page theapp locator savedMessage is visible
    Then I verify mobile page theapp locator savedMessage text contains "PTAF cross-platform native mobile test"

  Scenario: Validate Login screen opens on configured mobile platform
    Then I verify mobile page theapp locator echoBoxMenu is visible
    Then I verify mobile page theapp locator loginMenu is visible

    When I tap on mobile page theapp locator loginMenu

    Then I verify mobile page theapp locator username is visible
    Then I verify mobile page theapp locator password is visible
    Then I verify mobile page theapp locator loginButton is visible
