@mobile @android @orientation_optional
Feature: Optional Android runtime orientation validation

  # Purpose:
  # This feature is intentionally separated from the main smoke/evidence workflow.
  # Some Android UiAutomator2 driver versions do not support runtime orientation changes
  # through the command currently used by the framework. If this optional scenario fails
  # with UnsupportedCommandException, keep orientation controlled from:
  # src/test/resources/mobile/config/mobile-config.yml

  Scenario: Optional runtime orientation check
    When I rotate mobile screen to "landscape"
    Then I verify mobile page theapp locator echoBoxMenu is visible
    When I rotate mobile screen to "portrait"
    Then I verify mobile page theapp locator echoBoxMenu is visible
