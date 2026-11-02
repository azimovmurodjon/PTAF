@eStore @OpenAccount @Savings
Feature: eStore account opening flow

  Scenario: Open Account and select Savings
    Given we navigate to HARNESS_PREPROD_STAGE url
    Then get title of page
    Then we enter value on page TestHarness locator email_flt value "sanjayn@fnb-corp.com"
    When we enter value on page TestHarness locator phoneNumber_flt value "779-902-2577"
    And we select on page TestHarness locator product_group_flt value "Consumer Deposit"
    And we select on page TestHarness locator consumer_deposit_product_name value "23901 | Freestyle Checking | (DDA)"
    And we click on page TestHarness locator createURL_btn
    Then we click TestHarness locator openURL_btn and switch to popup
    And we click on new page eStoreAccounts locator open_account
    And we click on new page eStoreAccounts locator savings
    Then we verify on new page eStoreAccounts of locator confirmation_heading is visible
