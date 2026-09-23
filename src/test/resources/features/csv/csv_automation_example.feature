# =============================================================================
# PTAF CSV Automation — Example Feature File
# =============================================================================
# This feature file demonstrates all CSV step definitions available in PTAF.
# It uses the sample data file at:
#   src/test/resources/data/sample_transactions.csv
#
# The sample CSV has the following structure:
#   TransactionId, Date, Description, Amount, Currency, Status, Category, AccountNumber
#   TXN-001, 2025-07-01, Online Purchase - Amazon, 125.50, USD, APPROVED, Shopping, ACC-12345
#   TXN-002, 2025-07-02, Salary Deposit, 3500.00, USD, APPROVED, Income, ACC-12345
#   ... (5 rows total)
#
# HOW TO RUN:
#   mvn test -Dcucumber.filter.tags="@csv_example"
#
# ROW NUMBERING:
#   Row numbers are 1-based and refer to DATA rows only (not the header).
#   Row 1 = first data row (TXN-001), Row 2 = second data row (TXN-002), etc.
#
# COLUMN NAMES:
#   Column names are case-sensitive and must match the header row exactly.
# =============================================================================

@csv @csv_example
Feature: CSV Automation — File-based and UI-embedded

  # ---------------------------------------------------------------------------
  # Scenario 1: Load CSV from a file and assert cell values by column name
  # ---------------------------------------------------------------------------
  @csv_file @smoke
  Scenario: Verify transaction CSV data using column names
    Given I load CSV file "src/test/resources/data/sample_transactions.csv"

    # Assert the total number of data rows (excluding header)
    Then CSV row count equals 5

    # Assert specific cell values by row number and column name
    Then CSV row 1 column "TransactionId" equals "TXN-001"
    Then CSV row 1 column "Amount" equals "125.50"
    Then CSV row 1 column "Status" equals "APPROVED"
    Then CSV row 1 column "Category" equals "Shopping"

    Then CSV row 2 column "TransactionId" equals "TXN-002"
    Then CSV row 2 column "Amount" equals "3500.00"
    Then CSV row 2 column "Category" equals "Income"

    Then CSV row 5 column "TransactionId" equals "TXN-005"
    Then CSV row 5 column "Description" contains "ATM"

    # Assert that a cell does NOT equal a specific value
    Then CSV row 1 column "Status" does not equal "FAILED"

    # Assert that all rows have the same currency
    Then all CSV rows have column "Currency" equals "USD"
    Then all CSV rows have column "Status" equals "APPROVED"
    Then all CSV rows have column "AccountNumber" equals "ACC-12345"

  # ---------------------------------------------------------------------------
  # Scenario 2: Assert CSV structure — columns and row count
  # ---------------------------------------------------------------------------
  @csv_file @smoke
  Scenario: Verify CSV structure and column existence
    Given I load CSV file "src/test/resources/data/sample_transactions.csv"

    # Assert that required columns exist
    Then CSV column "TransactionId" exists
    Then CSV column "Amount" exists
    Then CSV column "Status" exists
    Then CSV column "Currency" exists
    Then CSV column "Category" exists
    Then CSV column "AccountNumber" exists

    # Assert that a column that should NOT be present is absent
    Then CSV column "InternalCode" does not exist
    Then CSV column "SecretKey" does not exist

    # Assert minimum row count
    Then CSV row count is at least 3

  # ---------------------------------------------------------------------------
  # Scenario 3: Assert cell values by column index (when no headers or index preferred)
  # ---------------------------------------------------------------------------
  @csv_file @regression
  Scenario: Verify CSV data using column index
    Given I load CSV file "src/test/resources/data/sample_transactions.csv"

    # Column indexes are 1-based:
    # Index 1 = TransactionId, 2 = Date, 3 = Description, 4 = Amount, 5 = Currency, 6 = Status
    Then CSV row 1 column index 1 equals "TXN-001"
    Then CSV row 1 column index 4 equals "125.50"
    Then CSV row 1 column index 6 equals "APPROVED"
    Then CSV row 2 column index 1 equals "TXN-002"
    Then CSV row 2 column index 4 equals "3500.00"

  # ---------------------------------------------------------------------------
  # Scenario 4: Extract CSV values and use them in later steps
  # ---------------------------------------------------------------------------
  @csv_file @regression
  Scenario: Extract CSV values and use them in subsequent assertions
    Given I load CSV file "src/test/resources/data/sample_transactions.csv"

    # Extract a value and store it under a variable name
    When I extract CSV row 1 column "TransactionId" and store as "FIRST_TXN_ID"
    When I extract CSV row 1 column "Amount" and store as "FIRST_AMOUNT"

    # Use the stored values in later assertions
    Then CSV row 1 column "TransactionId" equals stored value "FIRST_TXN_ID"
    Then CSV row 1 column "Amount" equals stored value "FIRST_AMOUNT"

    # Verify the extracted values are what we expect
    Then CSV row 1 column "TransactionId" equals "TXN-001"
    Then CSV row 1 column "Amount" equals "125.50"

  # ---------------------------------------------------------------------------
  # Scenario 5: Load CSV from a UI element (UI-embedded mode)
  # ---------------------------------------------------------------------------
  # This scenario demonstrates loading CSV that is displayed inside a UI element.
  # The UI element must contain CSV-formatted text content.
  #
  # IMPORTANT: This scenario requires a running application. Replace the page
  # and locator names below with your actual page and locator names.
  # ---------------------------------------------------------------------------
  @csv_ui @regression
  Scenario: Verify exported CSV data displayed in a UI text area
    # Navigate to the page that shows the CSV export
    Given I navigate to the application
    When I perform action "click" on page "ReportPage" locator "exportCsvButton"
    Then I should see element on page "ReportPage" locator "csvPreviewArea"

    # Load the CSV content from the UI element
    Given I load CSV from UI element on page "ReportPage" locator "csvPreviewArea"

    # Assert the structure and values of the exported CSV
    Then CSV column "TransactionId" exists
    Then CSV column "Amount" exists
    Then CSV row count is at least 1
    Then CSV row 1 column "Status" equals "APPROVED"

  # ---------------------------------------------------------------------------
  # Scenario 6: Load a semicolon-delimited CSV (common in European exports)
  # ---------------------------------------------------------------------------
  @csv_file @regression
  Scenario: Load and verify a semicolon-delimited CSV file
    # Use the delimiter parameter to specify semicolon as the separator
    Given I load CSV file "src/test/resources/data/sample_transactions.csv" with delimiter ","

    # The rest of the steps work exactly the same regardless of delimiter
    Then CSV row count equals 5
    Then CSV row 1 column "TransactionId" equals "TXN-001"
    Then CSV column "Amount" exists
