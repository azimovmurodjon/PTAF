@mobile @appium_browser @google_mobile_browser @evidence
Feature: Appium real mobile browser Google search automation

  # This feature opens the real mobile browser available on the selected emulator/simulator.
  # Android uses Chrome through UiAutomator2. iOS uses Safari through XCUITest.
  # It is intentionally separate from Playwright mobile-browser emulation.

  Scenario: Search Google from the real mobile browser and capture evidence
    When I open mobile browser url "https://www.google.com"
    When I allow all mobile permission popups if displayed
    When I capture mobile screenshot named "01-google-opened-real-mobile-browser"
    When I wait up to 20 seconds for mobile page googleBrowser locator searchBox to be visible
    When I enter mobile value "PTAF automation framework" on page googleBrowser locator searchBox
    When I capture mobile screenshot named "02-google-search-text-entered"
    When I press Enter on mobile page googleBrowser locator searchBox
    When I wait up to 20 seconds for mobile page googleBrowser locator results to be visible
    When I capture mobile screenshot named "03-google-search-results"
    When I save mobile browser page source to "test-output/mobile-browser-appium/google-search-page-source.xml"
    Then mobile browser current url should contain "google"
