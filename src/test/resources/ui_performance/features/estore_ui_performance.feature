@ui_performance @estore_ui_performance
Feature: eStore Consumer Deposit UI Performance

  Scenario: Concurrent users generate a Consumer Deposit application URL
    Given UI performance journey "eStore Consumer Deposit" uses configured target
    When UI performance journey navigates to configured route "test_harness"
    Then UI performance journey verifies locator "test_harness" "page_ready" is visible
    And UI performance journey fills locator "test_harness" "email" with data field "email"
    And UI performance journey fills locator "test_harness" "phone_number" with data field "phone_number"
    And UI performance journey selects locator "test_harness" "product_group" with data field "product_group"
    And UI performance journey selects locator "test_harness" "product_name" with data field "product_name"
    And UI performance journey clicks locator "test_harness" "create_url"
    Then UI performance journey verifies locator "test_harness" "generated_url" is visible
    And UI performance journey verifies locator "test_harness" "open_url" is visible
    And the configured UI performance users execute the journey
    And the UI performance run produces a standalone performance report
