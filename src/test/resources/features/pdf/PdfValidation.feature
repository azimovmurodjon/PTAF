@pdf @text-only @green
Feature: Validate all textual content of the sample invoice PDF
  As a tester
  I want strong text assertions on the whole PDF and per-page
  So I can trust content is present and readable without OCR or visuals

  Background:
    # Pick the newest PDF from your downloads directory.
    # Make sure sample_invoice.pdf is in this folder and is the newest file.
    When I set last PDF from directory "downloads"
    Then the last PDF should exist
    And the last PDF should be a valid PDF
    And the last PDF should have 2 pages

  # ---- Whole-document coverage -------------------------------------------------
  @doc
  Scenario: Whole document contains all important sections and values
    Then the last PDF should contain all:
      | ACME CORP                                       |
      | 123 Market Street, Metropolis, NY 10001         |
      | Invoice #12345                                  |
      | Order Date:                                     |
      | Customer Name: John Smith                       |
      | Bill To:                                        |
      | John Smith                                      |
      | 456 Elm Ave                                     |
      | Springfield, NY 10002                           |
      | Items:                                          |
      | Widget A                                        |
      | Widget B                                        |
      | Subtotal: $350.00                               |
      | Tax (8%): $28.00                                |
      | Total $1,234.56                                 |
      | Payment Details                                 |
      | Total: $378.00                                  |
      | Thank you for your business!                    |

  # ---- Page 1: header, address, items, totals (big total uses comma) ----------
  @page1
  Scenario: Page 1 contains header, billing and line items
    Then page 1 of the last PDF should contain "ACME CORP"
    And page 1 of the last PDF should contain "Invoice #12345"
    And page 1 of the last PDF should contain "Order Date:"
    And page 1 of the last PDF should contain "Customer Name: John Smith"
    And page 1 of the last PDF should contain "Bill To:"
    And page 1 of the last PDF should contain "456 Elm Ave"
    And page 1 of the last PDF should contain "Springfield, NY 10002"
    And page 1 of the last PDF should contain "Items:"
    And page 1 of the last PDF should contain "Widget A"
    And page 1 of the last PDF should contain "Widget B"
    And page 1 of the last PDF should contain "Subtotal: $350.00"
    And page 1 of the last PDF should contain "Tax (8%): $28.00"
    And page 1 of the last PDF should contain "Total $1,234.56"

  # ---- Page 2: simple totals/thanks block (colon-style totals) ----------------
  @page2
  Scenario: Page 2 contains payment details and thank you note
    Then page 2 of the last PDF should contain "Payment Details"
    And page 2 of the last PDF should contain "Subtotal: $350.00"
    And page 2 of the last PDF should contain "Tax: $28.00"
    And page 2 of the last PDF should contain "Total: $378.00"
    And page 2 of the last PDF should contain "Thank you for your business!"

  # ---- Regex examples to prove numeric formats are there ----------------------
  @regex
  Scenario: Whole document matches currency formats via regex
    Then the last PDF should match regex "Subtotal\s*:\s*\$\s*350\.00"
    And the last PDF should match regex "Tax\s*\(8%\)\s*:\s*\$\s*28\.00"
    And the last PDF should match regex "Total\s*\$\s*1,234\.56"
    And the last PDF should match regex "Total\s*:\s*\$\s*378\.00"