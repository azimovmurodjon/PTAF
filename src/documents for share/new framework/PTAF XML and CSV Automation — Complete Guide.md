# PTAF XML and CSV Automation — Complete Guide

**Framework:** PTAF (Portable Test Automation Framework)  
**Module:** XML and CSV Automation  
**Audience:** QA engineers and testers — including those new to XML/CSV automation  
**Last Updated:** July 2025

---

## Table of Contents

1. [Introduction — What This Module Does](#1-introduction)
2. [XML Automation — Overview](#2-xml-automation-overview)
3. [XML Step Definitions — Complete Reference](#3-xml-step-definitions)
4. [XML — File-Based Usage (Load from Filesystem)](#4-xml-file-based-usage)
5. [XML — UI-Embedded Usage (Load from a UI Element)](#5-xml-ui-embedded-usage)
6. [XML — Querying Strategy (Node Name vs XPath)](#6-xml-querying-strategy)
7. [CSV Automation — Overview](#7-csv-automation-overview)
8. [CSV Step Definitions — Complete Reference](#8-csv-step-definitions)
9. [CSV — File-Based Usage (Load from Filesystem)](#9-csv-file-based-usage)
10. [CSV — UI-Embedded Usage (Load from a UI Element)](#10-csv-ui-embedded-usage)
11. [CSV — Column Names vs Column Indexes](#11-csv-column-names-vs-indexes)
12. [Variable Store — Extract and Reuse Values](#12-variable-store)
13. [Sample Data Files](#13-sample-data-files)
14. [Complete End-to-End Examples](#14-complete-examples)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Introduction — What This Module Does

The PTAF XML and CSV automation module allows you to validate, extract, and assert values from XML and CSV data sources directly inside your Cucumber feature files. It supports two usage modes for both formats.

**Mode 1 — File-based:** Load an XML or CSV file from the filesystem and assert values from it. This is used when you have a known data file (a configuration file, an API response saved to disk, a downloaded export, etc.) and you want to verify its contents as part of your test.

**Mode 2 — UI-embedded:** Find a UI element on the screen that contains XML or CSV text (for example, a textarea showing an API response, a code block displaying formatted data, or a pre-formatted text area), extract the text from it, and then assert values from the extracted content. This is used when the data you want to validate is displayed on the screen rather than stored in a file.

Both modes use the same assertion steps — the only difference is how the data is loaded. Once loaded, all query and assertion steps work identically regardless of whether the data came from a file or a UI element.

---

## 2. XML Automation — Overview

XML (Extensible Markup Language) is a structured data format used widely in API responses, configuration files, data exports, and web services. The PTAF XML module parses XML documents and allows you to query them using either simple node names or full XPath expressions.

**No external libraries are required.** The module uses only the standard Java SE XML APIs (`javax.xml.parsers`, `javax.xml.xpath`) which are built into every JDK installation.

**Key capabilities:**
- Load XML from a file path or from a UI element's text content
- Assert that a node value equals or contains an expected string
- Assert that a node exists or does not exist in the document
- Assert the count of nodes matching an XPath expression
- Assert attribute values on XML elements
- Extract node values into named variables for use in later steps
- Query using simple node names (e.g., `"status"`) or full XPath expressions (e.g., `"//order/status"`)

---

## 3. XML Step Definitions — Complete Reference

### Loading Steps

| Step | Description |
|:---|:---|
| `Given I load XML file {string}` | Load and parse an XML file from the filesystem. The path is relative to the project root (where `pom.xml` is). |
| `Given I load XML from UI element on page {string} locator {string}` | Extract text from a UI element and parse it as XML. The element must contain well-formed XML. |

### Assertion Steps — Value Equality

| Step | Description |
|:---|:---|
| `Then XML node {string} equals {string}` | Assert that the value of a node or XPath expression equals the expected value exactly. |
| `Then XML XPath {string} equals {string}` | Same as above but uses the keyword "XPath" for clarity when writing full XPath expressions. |
| `Then XML node {string} contains {string}` | Assert that the value of a node or XPath expression contains the expected substring. |
| `Then XML XPath {string} contains {string}` | Same as above but uses the keyword "XPath". |
| `Then XML node {string} does not equal {string}` | Assert that the value of a node does NOT equal the given value. |

### Assertion Steps — Node Existence

| Step | Description |
|:---|:---|
| `Then XML node {string} exists` | Assert that at least one node matching the query exists in the document. |
| `Then XML node {string} does not exist` | Assert that no node matching the query exists in the document. |

### Assertion Steps — Node Count

| Step | Description |
|:---|:---|
| `Then XML XPath {string} count equals {int}` | Assert that the number of nodes matching an XPath expression equals the expected count. |

### Assertion Steps — Attributes

| Step | Description |
|:---|:---|
| `Then XML node {string} attribute {string} equals {string}` | Assert that the value of an attribute on the first matching node equals the expected value. |

### Assertion Steps — Stored Values

| Step | Description |
|:---|:---|
| `Then XML node {string} equals stored value {string}` | Assert that the value of a node equals a previously stored variable value. |

### Extraction Steps

| Step | Description |
|:---|:---|
| `When I extract XML node {string} and store as {string}` | Extract the value of a node and store it under a named variable for use in later steps. |
| `When I extract XML XPath {string} and store as {string}` | Extract the value of an XPath expression and store it under a named variable. |

---

## 4. XML — File-Based Usage (Load from Filesystem)

### Step 1 — Place Your XML File

Put your XML file in the test resources directory. The recommended location is:

```
src/test/resources/data/your_file.xml
```

The file must be well-formed XML. It must have a single root element. Example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<orderResponse>
    <status>SUCCESS</status>
    <orderId>ORD-2025-00123</orderId>
    <customer>
        <name>John Doe</name>
        <email>john.doe@example.com</email>
    </customer>
    <pricing>
        <total>189.53</total>
        <currency>USD</currency>
    </pricing>
</orderResponse>
```

### Step 2 — Load the File in Your Feature File

```gherkin
Given I load XML file "src/test/resources/data/order_response.xml"
```

The path is relative to the project root (the directory where `pom.xml` is located). Both absolute and relative paths are supported.

### Step 3 — Assert Values

```gherkin
Then XML node "status" equals "SUCCESS"
Then XML node "orderId" equals "ORD-2025-00123"
Then XML node "currency" equals "USD"
Then XML node "name" contains "John"
Then XML node "errorCode" does not exist
```

### Step 4 — Use XPath for Precise Queries

When the same node name appears multiple times in the document, use a full XPath expression to be precise:

```gherkin
Then XML XPath "//orderResponse/status" equals "SUCCESS"
Then XML XPath "//customer/name" equals "John Doe"
Then XML XPath "//pricing/total" equals "189.53"
```

---

## 5. XML — UI-Embedded Usage (Load from a UI Element)

This mode is used when XML content is displayed inside a UI element — for example, an API testing page that shows the response body in a textarea, or a web application that renders XML in a code block.

### Step 1 — Add the Element to Your YAML Locator File

In your elements YAML file (e.g., `src/test/resources/elements/api_page.yml`), add the element that contains the XML:

```yaml
elements:
  ApiTestPage:
    responseTextArea:
      value: "XPATH_//textarea[@id='response-body']"
```

### Step 2 — Navigate to the Page and Trigger the XML Display

```gherkin
Given I navigate to the application
When I perform action "click" on page "ApiTestPage" locator "sendRequestButton"
Then I should see element on page "ApiTestPage" locator "responseTextArea"
```

### Step 3 — Load the XML from the UI Element

```gherkin
Given I load XML from UI element on page "ApiTestPage" locator "responseTextArea"
```

The framework finds the element, extracts its text content, and parses it as XML.

### Step 4 — Assert Values Exactly as You Would With a File

```gherkin
Then XML node "status" equals "SUCCESS"
Then XML XPath "//response/code" equals "200"
Then XML node "errorMessage" does not exist
```

---

## 6. XML — Querying Strategy (Node Name vs XPath)

The PTAF XML module supports two querying approaches. Understanding when to use each one is important for writing reliable tests.

### Simple Node Name (Recommended for Unique Nodes)

When you provide a query that does not start with `/` or `//`, the framework treats it as a simple node name and searches the entire document for the first element with that tag name.

```gherkin
Then XML node "status" equals "SUCCESS"
```

This is equivalent to the XPath expression `//status`. It finds the first `<status>` element anywhere in the document.

**Use this when:** The node name is unique in the document, or you only care about the first occurrence.

**Do not use this when:** The same node name appears multiple times (e.g., `<price>` inside multiple `<item>` elements) and you need a specific one.

### Full XPath Expression (Required for Precise Queries)

When you provide a query starting with `/` or `//`, the framework treats it as a full XPath expression.

```gherkin
Then XML XPath "//order/items/item[@id='1']/price" equals "29.99"
```

**Use this when:**
- The same node name appears multiple times and you need a specific one.
- You need to filter by an attribute value (e.g., `[@id='1']`).
- You need to navigate a specific path in the hierarchy.
- You need to count nodes.

### Common XPath Patterns

```xpath
# Find by exact path
//orderResponse/status

# Find by attribute value
//items/item[@id='1']/price

# Find by text content
//items/item[price='29.99']/description

# Find the second item
(//items/item)[2]

# Count all items
//items/item   (used with "count equals" step)

# Find by partial text
//items/item[contains(description, 'Headphone')]
```

---

## 7. CSV Automation — Overview

CSV (Comma-Separated Values) is a simple tabular data format used widely in data exports, test data files, reports, and configuration files. The PTAF CSV module parses CSV documents and allows you to query them by row number and column name or column index.

**No external libraries are required.** The module uses only standard Java SE APIs.

**Key capabilities:**
- Load CSV from a file path or from a UI element's text content
- Assert cell values by row number and column name
- Assert cell values by row number and column index
- Assert total row count
- Assert that a column header exists or does not exist
- Assert that all rows have the same value in a specific column
- Extract cell values into named variables for use in later steps
- Support for custom delimiters (semicolon, tab, pipe, etc.)

**Row numbering:** Row numbers are 1-based and refer to data rows only — not the header row. Row 1 is the first data row after the header.

**Column names:** Column names are case-sensitive and must match the header row exactly.

---

## 8. CSV Step Definitions — Complete Reference

### Loading Steps

| Step | Description |
|:---|:---|
| `Given I load CSV file {string}` | Load and parse a comma-separated CSV file from the filesystem. |
| `Given I load CSV file {string} with delimiter {string}` | Load a CSV file using a custom delimiter (e.g., `";"` for semicolon, `"\t"` for tab). |
| `Given I load CSV from UI element on page {string} locator {string}` | Extract text from a UI element and parse it as CSV. |

### Assertion Steps — Cell Value by Column Name

| Step | Description |
|:---|:---|
| `Then CSV row {int} column {string} equals {string}` | Assert that the cell at the given row and column name equals the expected value exactly. |
| `Then CSV row {int} column {string} contains {string}` | Assert that the cell contains the expected substring. |
| `Then CSV row {int} column {string} does not equal {string}` | Assert that the cell does NOT equal the given value. |

### Assertion Steps — Cell Value by Column Index

| Step | Description |
|:---|:---|
| `Then CSV row {int} column index {int} equals {string}` | Assert that the cell at the given row and 1-based column index equals the expected value. |

### Assertion Steps — Structure

| Step | Description |
|:---|:---|
| `Then CSV row count equals {int}` | Assert that the CSV contains exactly the expected number of data rows (excluding header). |
| `Then CSV row count is at least {int}` | Assert that the CSV contains at least the expected number of data rows. |
| `Then CSV column {string} exists` | Assert that a column with the given name exists in the CSV headers. |
| `Then CSV column {string} does not exist` | Assert that a column with the given name does NOT exist in the CSV headers. |

### Assertion Steps — All Rows

| Step | Description |
|:---|:---|
| `Then all CSV rows have column {string} equals {string}` | Assert that every data row has the same value in the specified column. |

### Assertion Steps — Stored Values

| Step | Description |
|:---|:---|
| `Then CSV row {int} column {string} equals stored value {string}` | Assert that a cell value equals a previously stored variable value. |

### Extraction Steps

| Step | Description |
|:---|:---|
| `When I extract CSV row {int} column {string} and store as {string}` | Extract a cell value and store it under a named variable for use in later steps. |

---

## 9. CSV — File-Based Usage (Load from Filesystem)

### Step 1 — Place Your CSV File

Put your CSV file in the test resources directory:

```
src/test/resources/data/your_file.csv
```

Example CSV file (`transactions.csv`):

```csv
TransactionId,Date,Description,Amount,Currency,Status
TXN-001,2025-07-01,Online Purchase,125.50,USD,APPROVED
TXN-002,2025-07-02,Salary Deposit,3500.00,USD,APPROVED
TXN-003,2025-07-03,Electricity Bill,89.75,USD,APPROVED
```

### Step 2 — Load the File in Your Feature File

```gherkin
Given I load CSV file "src/test/resources/data/transactions.csv"
```

### Step 3 — Assert Values

```gherkin
# Assert total row count (excluding header)
Then CSV row count equals 3

# Assert specific cell values
Then CSV row 1 column "TransactionId" equals "TXN-001"
Then CSV row 1 column "Amount" equals "125.50"
Then CSV row 1 column "Status" equals "APPROVED"

# Assert that a column exists
Then CSV column "Currency" exists
Then CSV column "InternalCode" does not exist

# Assert all rows have the same value in a column
Then all CSV rows have column "Currency" equals "USD"
Then all CSV rows have column "Status" equals "APPROVED"
```

### Step 4 — Load a Semicolon-Delimited File

For European-format CSV files that use semicolons instead of commas:

```gherkin
Given I load CSV file "src/test/resources/data/european_export.csv" with delimiter ";"
```

For tab-separated files (TSV):

```gherkin
Given I load CSV file "src/test/resources/data/export.tsv" with delimiter "\t"
```

---

## 10. CSV — UI-Embedded Usage (Load from a UI Element)

This mode is used when CSV content is displayed inside a UI element — for example, an export preview area, a report page showing data in plain text, or a textarea containing CSV output.

### Step 1 — Add the Element to Your YAML Locator File

```yaml
elements:
  ReportPage:
    csvPreviewArea:
      value: "XPATH_//textarea[@id='csv-output']"
```

### Step 2 — Navigate and Trigger the CSV Display

```gherkin
Given I navigate to the application
When I perform action "click" on page "ReportPage" locator "exportCsvButton"
Then I should see element on page "ReportPage" locator "csvPreviewArea"
```

### Step 3 — Load the CSV from the UI Element

```gherkin
Given I load CSV from UI element on page "ReportPage" locator "csvPreviewArea"
```

### Step 4 — Assert Values

```gherkin
Then CSV column "TransactionId" exists
Then CSV row count is at least 1
Then CSV row 1 column "Status" equals "APPROVED"
```

---

## 11. CSV — Column Names vs Column Indexes

### By Column Name (Recommended)

Column names are taken from the first row (header row) of the CSV file. They are case-sensitive.

```gherkin
Then CSV row 1 column "Amount" equals "125.50"
Then CSV row 2 column "Status" contains "APPROVED"
```

**Use this approach** when the CSV has a header row (which is the default for most CSV files).

### By Column Index (For Files Without Headers)

Column indexes are 1-based. Column 1 is the first column, column 2 is the second, and so on.

```gherkin
Then CSV row 1 column index 1 equals "TXN-001"
Then CSV row 1 column index 4 equals "125.50"
```

**Use this approach** when the CSV file has no header row, or when you prefer index-based access.

---

## 12. Variable Store — Extract and Reuse Values

Both the XML and CSV modules include a variable store that lets you extract a value in one step and use it in a later step within the same scenario. This is useful for cross-referencing values between different data sources or different parts of the same document.

### XML Variable Store

```gherkin
# Extract a value and store it
When I extract XML node "orderId" and store as "ORDER_ID"
When I extract XML XPath "//payment/transactionId" and store as "TXN_ID"

# Use the stored value in a later assertion
Then XML node "orderId" equals stored value "ORDER_ID"
```

### CSV Variable Store

```gherkin
# Extract a value and store it
When I extract CSV row 1 column "TransactionId" and store as "FIRST_TXN"

# Use the stored value in a later assertion
Then CSV row 1 column "TransactionId" equals stored value "FIRST_TXN"
```

### Cross-Module Usage

You can also use the variable store to cross-reference values between XML and CSV data in the same scenario. For example, verify that an order ID in an XML response matches a transaction ID in a CSV export:

```gherkin
# Load the XML API response
Given I load XML file "src/test/resources/data/api_response.xml"
When I extract XML node "orderId" and store as "ORDER_ID"

# Load the CSV export
Given I load CSV file "src/test/resources/data/export.csv"
Then CSV row 1 column "OrderReference" equals stored value "ORDER_ID"
```

**Important:** The variable store is cleared automatically at the end of each scenario. Variables do not persist between scenarios.

---

## 13. Sample Data Files

The module includes two sample data files in `src/test/resources/data/` that you can use to test the steps immediately.

### sample_order_response.xml

A complete order response XML with nested elements, attributes, and multiple sections (customer, items, pricing, payment, shipping). Use this to practice all XML step types.

### sample_transactions.csv

A 5-row transaction CSV with 8 columns (TransactionId, Date, Description, Amount, Currency, Status, Category, AccountNumber). Use this to practice all CSV step types.

To run the example scenarios:

```bash
mvn test -Dcucumber.filter.tags="@xml_example"
mvn test -Dcucumber.filter.tags="@csv_example"
```

---

## 14. Complete End-to-End Examples

### Example 1 — Validate an API Response XML File

```gherkin
@xml @api_validation @smoke
Feature: Order API Response Validation

  Scenario: Verify successful order response XML
    Given I load XML file "src/test/resources/data/order_response.xml"

    # Basic value assertions using simple node names
    Then XML node "status" equals "SUCCESS"
    Then XML node "orderId" equals "ORD-2025-00123"
    Then XML node "currency" equals "USD"

    # Precise assertions using full XPath
    Then XML XPath "//customer/name" equals "John Doe"
    Then XML XPath "//customer/email" equals "john.doe@example.com"
    Then XML XPath "//pricing/total" equals "189.53"
    Then XML XPath "//payment/method" equals "CREDIT_CARD"

    # Assert item count
    Then XML XPath "//items/item" count equals 3

    # Assert a specific item by attribute
    Then XML XPath "//items/item[@id='1']/description" equals "Wireless Headphones"
    Then XML XPath "//items/item[@id='1']/unitPrice" equals "59.99"

    # Assert node existence
    Then XML node "trackingNumber" exists
    Then XML node "errorCode" does not exist

    # Extract and verify
    When I extract XML node "orderId" and store as "ORDER_ID"
    Then XML node "orderId" equals stored value "ORDER_ID"
```

### Example 2 — Validate a Downloaded CSV Export

```gherkin
@csv @export_validation @regression
Feature: Transaction Export CSV Validation

  Scenario: Verify transaction export CSV structure and values
    Given I load CSV file "src/test/resources/data/sample_transactions.csv"

    # Verify structure
    Then CSV row count equals 5
    Then CSV column "TransactionId" exists
    Then CSV column "Amount" exists
    Then CSV column "Status" exists
    Then CSV column "InternalCode" does not exist

    # Verify specific rows
    Then CSV row 1 column "TransactionId" equals "TXN-001"
    Then CSV row 1 column "Amount" equals "125.50"
    Then CSV row 1 column "Status" equals "APPROVED"

    Then CSV row 2 column "TransactionId" equals "TXN-002"
    Then CSV row 2 column "Amount" equals "3500.00"
    Then CSV row 2 column "Category" equals "Income"

    # Verify all rows have the same currency and status
    Then all CSV rows have column "Currency" equals "USD"
    Then all CSV rows have column "Status" equals "APPROVED"

    # Extract and verify
    When I extract CSV row 1 column "TransactionId" and store as "FIRST_TXN"
    Then CSV row 1 column "TransactionId" equals stored value "FIRST_TXN"
```

### Example 3 — Load XML from a UI Element (API Response Viewer)

```gherkin
@xml @ui @api_response_viewer @regression
Feature: API Response XML Validation from UI

  Scenario: Verify XML response displayed in the API test tool
    Given I navigate to the application
    When I perform action "click" on page "ApiTestPage" locator "sendRequestButton"
    Then I should see element on page "ApiTestPage" locator "responseTextArea"

    # Load the XML from the UI element
    Given I load XML from UI element on page "ApiTestPage" locator "responseTextArea"

    # Assert values from the displayed XML
    Then XML node "status" equals "SUCCESS"
    Then XML XPath "//response/code" equals "200"
    Then XML node "errorMessage" does not exist
    When I extract XML node "requestId" and store as "REQ_ID"
```

### Example 4 — Load CSV from a UI Export Preview

```gherkin
@csv @ui @export_preview @regression
Feature: CSV Export Preview Validation

  Scenario: Verify CSV data shown in the export preview area
    Given I navigate to the application
    When I perform action "click" on page "ReportPage" locator "generateReportButton"
    Then I should see element on page "ReportPage" locator "csvPreviewArea"

    # Load the CSV from the UI element
    Given I load CSV from UI element on page "ReportPage" locator "csvPreviewArea"

    # Assert the exported data
    Then CSV column "TransactionId" exists
    Then CSV row count is at least 1
    Then CSV row 1 column "Status" equals "APPROVED"
    Then all CSV rows have column "Currency" equals "USD"
```

---

## 15. Troubleshooting

| Problem | Likely Cause | Solution |
|:---|:---|:---|
| `PTAF XML \| File not found: [path]` | The file path is incorrect or the file does not exist. | Verify the path is relative to the project root (where `pom.xml` is). Use forward slashes. |
| `PTAF XML \| Failed to parse XML string content` | The UI element contains text that is not valid XML. | Inspect the element in the browser. Ensure it contains well-formed XML with a single root element. |
| `PTAF XML \| No XML document is loaded` | You are trying to assert before loading a document. | Make sure `Given I load XML file "..."` or `Given I load XML from UI element ...` comes before any assertion steps. |
| `PTAF XML \| Assertion failed — value does not match` | The actual value in the document does not match the expected value. | Check the XML file for the actual value. Note that whitespace and case are significant. |
| `PTAF CSV \| File not found: [path]` | The file path is incorrect. | Same as XML — verify the path is relative to the project root. |
| `PTAF CSV \| Column [name] not found` | The column name does not match the header exactly. | Column names are case-sensitive. Check the header row of the CSV file for the exact spelling. |
| `PTAF CSV \| Row number [n] is out of range` | The row number is higher than the number of data rows. | Check the CSV file. Row numbers are 1-based and exclude the header row. |
| `PTAF CSV \| No CSV data is loaded` | You are trying to assert before loading data. | Make sure `Given I load CSV file "..."` or `Given I load CSV from UI element ...` comes before any assertion steps. |
| XML XPath returns empty string | The XPath expression does not match any node. | Test the XPath in an XML editor tool. Check for typos, namespace issues, or incorrect hierarchy. |

---

*This document covers the PTAF XML and CSV automation module. All step definitions, class names, and file paths are specific to the PTAF framework.*
