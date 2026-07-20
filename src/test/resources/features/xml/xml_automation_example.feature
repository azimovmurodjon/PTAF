# =============================================================================
# PTAF XML Automation — Example Feature File
# =============================================================================
# This feature file demonstrates all XML step definitions available in PTAF.
# It uses the sample data file at:
#   src/test/resources/data/sample_order_response.xml
#
# HOW TO RUN:
#   mvn test -Dcucumber.filter.tags="@xml_example"
#
# TWO USAGE MODES:
#   1. File-based: Load XML from the filesystem (shown in Scenario 1 and 2)
#   2. UI-embedded: Load XML from a UI element (shown in Scenario 3)
#
# QUERYING STRATEGY:
#   - Simple node name: "status" → finds the first <status> element anywhere
#   - Full XPath:       "//orderResponse/status" → precise location in the tree
#   - XPath with filter: "//items/item[@id='1']/price" → find by attribute value
# =============================================================================

@xml @xml_example
Feature: XML Automation — File-based and UI-embedded

  # ---------------------------------------------------------------------------
  # Scenario 1: Load XML from a file and assert values using simple node names
  # ---------------------------------------------------------------------------
  # This is the simplest approach. Just provide the node name and PTAF finds
  # the first element with that name anywhere in the document.
  # ---------------------------------------------------------------------------
  @xml_file @smoke
  Scenario: Verify order response XML using simple node names
    Given I load XML file "src/test/resources/data/sample_order_response.xml"

    # Assert exact values using simple node names
    Then XML node "status" equals "SUCCESS"
    Then XML node "orderId" equals "ORD-2025-00123"
    Then XML node "currency" equals "USD"
    Then XML node "method" equals "CREDIT_CARD"

    # Assert partial values using contains
    Then XML node "message" contains "successfully"
    Then XML node "address" contains "Springfield"

    # Assert node existence
    Then XML node "trackingNumber" exists
    Then XML node "discountCode" does not exist

    # Assert that a node does NOT have a specific value
    Then XML node "status" does not equal "FAILED"

  # ---------------------------------------------------------------------------
  # Scenario 2: Load XML from a file and assert values using full XPath
  # ---------------------------------------------------------------------------
  # Use full XPath when you need to be precise about which element to find,
  # especially when the same node name appears multiple times in the document.
  # ---------------------------------------------------------------------------
  @xml_file @regression
  Scenario: Verify order items using XPath expressions
    Given I load XML file "src/test/resources/data/sample_order_response.xml"

    # Assert values using full XPath — precise navigation through the tree
    Then XML XPath "//orderResponse/status" equals "SUCCESS"
    Then XML XPath "//pricing/total" equals "189.53"
    Then XML XPath "//pricing/currency" equals "USD"
    Then XML XPath "//customer/name" equals "John Doe"
    Then XML XPath "//customer/email" equals "john.doe@example.com"
    Then XML XPath "//payment/transactionId" equals "TXN-789-XYZ"

    # XPath with attribute filter — find a specific item by its id attribute
    Then XML XPath "//items/item[@id='1']/description" equals "Wireless Headphones"
    Then XML XPath "//items/item[@id='2']/productCode" equals "PROD-002"
    Then XML XPath "//items/item[@id='3']/unitPrice" equals "19.99"

    # Assert attribute value directly
    Then XML node "//items/item[@id='1']" attribute "id" equals "1"

    # Assert node count — verify the order has exactly 3 items
    Then XML XPath "//items/item" count equals 3

    # Assert XPath contains
    Then XML XPath "//shipping/address" contains "Main Street"

  # ---------------------------------------------------------------------------
  # Scenario 3: Extract values and use them in later steps
  # ---------------------------------------------------------------------------
  @xml_file @regression
  Scenario: Extract XML values and use them in subsequent assertions
    Given I load XML file "src/test/resources/data/sample_order_response.xml"

    # Extract a value and store it under a variable name
    When I extract XML node "orderId" and store as "ORDER_ID"
    When I extract XML XPath "//payment/transactionId" and store as "TXN_ID"
    When I extract XML node "total" and store as "ORDER_TOTAL"

    # Use the stored values in later assertions
    Then XML node "orderId" equals stored value "ORDER_ID"
    Then XML node "total" equals stored value "ORDER_TOTAL"

    # Verify the extracted values are what we expect
    Then XML node "orderId" equals "ORD-2025-00123"
    Then XML node "total" equals "189.53"

  # ---------------------------------------------------------------------------
  # Scenario 4: Load XML from a UI element (UI-embedded mode)
  # ---------------------------------------------------------------------------
  # This scenario demonstrates loading XML that is displayed inside a UI element.
  # The UI element must contain well-formed XML text.
  #
  # IMPORTANT: This scenario requires a running application. The page and locator
  # below are examples — replace them with your actual page and locator names.
  # ---------------------------------------------------------------------------
  @xml_ui @regression
  Scenario: Verify API response XML displayed in a UI text area
    # First navigate to the page that shows the XML response
    Given I navigate to the application
    When I perform action "click" on page "ApiTestPage" locator "sendRequestButton"
    Then I should see element on page "ApiTestPage" locator "responseTextArea"

    # Load the XML content from the UI element
    Given I load XML from UI element on page "ApiTestPage" locator "responseTextArea"

    # Now assert values from the XML displayed in the UI
    Then XML node "status" equals "SUCCESS"
    Then XML node "orderId" exists
    Then XML XPath "//response/code" equals "200"
