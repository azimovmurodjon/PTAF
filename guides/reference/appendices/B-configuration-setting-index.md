# Appendix B — Configuration Setting Index

**Generated from the active repository:** `scripts/generate_configuration_index.py`.

This index lists settings that are currently checked into `src/test/resources`. It is deliberately safe for sharing: sensitive-looking values and environment- or user-specific identifiers are redacted. A setting’s exact implementation meaning is defined by the linked reader, model, or execution class, not by this table alone.

> **Use the chapter first:** Read [Chapter 02 — Configuration, YAML, Locators, and Data](../chapters/02-configuration-yaml-locators-data.md) for precedence and safe editing. Then use this appendix to find the exact file and setting key.

## Redaction rules

Keys whose name indicates passwords, tokens, secrets, credentials, authorization, cookies, API keys, connection strings, hostnames, URLs, endpoints, servers, users, emails, phones, or accounts are shown as placeholders. This prevents a documentation artifact from becoming a secret or personal-data distribution path.

## Resource index

| Resource | Format | Setting rows |
|---|---|---:|
| [`src/test/resources/api_requests/api_requests.yml`](../../../src/test/resources/api_requests/api_requests.yml) | YAML | 14 |
| [`src/test/resources/config/config.yml`](../../../src/test/resources/config/config.yml) | YAML | 39 |
| [`src/test/resources/cucumber.properties`](../../../src/test/resources/cucumber.properties) | Properties | 1 |
| [`src/test/resources/data/sample_order_response.xml`](../../../src/test/resources/data/sample_order_response.xml) | XML | 1 |
| [`src/test/resources/elements/eStore_elements.yml`](../../../src/test/resources/elements/eStore_elements.yml) | YAML | 899 |
| [`src/test/resources/elements/google.yml`](../../../src/test/resources/elements/google.yml) | YAML | 4 |
| [`src/test/resources/elements/homepage.yml`](../../../src/test/resources/elements/homepage.yml) | YAML | 16 |
| [`src/test/resources/elements/landingpage.yml`](../../../src/test/resources/elements/landingpage.yml) | YAML | 44 |
| [`src/test/resources/elements/login.yml`](../../../src/test/resources/elements/login.yml) | YAML | 12 |
| [`src/test/resources/elements/panda_page.yml`](../../../src/test/resources/elements/panda_page.yml) | YAML | 10 |
| [`src/test/resources/elements/personal_bank.yml`](../../../src/test/resources/elements/personal_bank.yml) | YAML | 248 |
| [`src/test/resources/extent-config.xml`](../../../src/test/resources/extent-config.xml) | XML | 1 |
| [`src/test/resources/extent.properties`](../../../src/test/resources/extent.properties) | Properties | 21 |
| [`src/test/resources/mobile/config/mobile-browser-config.yml`](../../../src/test/resources/mobile/config/mobile-browser-config.yml) | YAML | 47 |
| [`src/test/resources/mobile/config/mobile-config.yml`](../../../src/test/resources/mobile/config/mobile-config.yml) | YAML | 17 |
| [`src/test/resources/mobile/config/mobile-native-config.yml`](../../../src/test/resources/mobile/config/mobile-native-config.yml) | YAML | 43 |
| [`src/test/resources/mobile/elements/fnb_elements.yml`](../../../src/test/resources/mobile/elements/fnb_elements.yml) | YAML | 12 |
| [`src/test/resources/mobile/elements/google_mobile_browser_elements.yml`](../../../src/test/resources/mobile/elements/google_mobile_browser_elements.yml) | YAML | 6 |
| [`src/test/resources/mobile/elements/mobile_permissions.yml`](../../../src/test/resources/mobile/elements/mobile_permissions.yml) | YAML | 6 |
| [`src/test/resources/mobile/elements/safari_browser_elements.yml`](../../../src/test/resources/mobile/elements/safari_browser_elements.yml) | YAML | 2 |
| [`src/test/resources/mobile/elements/theapp_elements.yml`](../../../src/test/resources/mobile/elements/theapp_elements.yml) | YAML | 10 |
| [`src/test/resources/mobile/elements/unified_locator_examples.yml`](../../../src/test/resources/mobile/elements/unified_locator_examples.yml) | YAML | 11 |
| [`src/test/resources/mobile_browser/config/mobile-browser-execution.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml) | YAML | 16 |
| [`src/test/resources/mobile_browser/config/mobile-browser-profiles.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml) | YAML | 240 |
| [`src/test/resources/performance/config/performance-config.yml`](../../../src/test/resources/performance/config/performance-config.yml) | YAML | 12 |
| [`src/test/resources/performance/payloads/yaml/performance-payloads.yml`](../../../src/test/resources/performance/payloads/yaml/performance-payloads.yml) | YAML | 3 |
| [`src/test/resources/queries/db_queries.yml`](../../../src/test/resources/queries/db_queries.yml) | YAML | 10 |
| [`src/test/resources/testng.xml`](../../../src/test/resources/testng.xml) | XML | 2 |
| [`src/test/resources/ui_performance/config/ui_performance-browser-contract.yml`](../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml) | YAML | 35 |
| [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../../src/test/resources/ui_performance/config/ui_performance-config.yml) | YAML | 74 |
| [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`](../../../src/test/resources/ui_performance/locators/ui_performance-locators.yml) | YAML | 8 |
| [`src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml`](../../../src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml) | XML | 2 |
| [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../../src/test/resources/ui_performance/testng-ui_performance.xml) | XML | 4 |

## `src/test/resources/api_requests/api_requests.yml`

Source: [`src/test/resources/api_requests/api_requests.yml`](../../../src/test/resources/api_requests/api_requests.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `jsonplaceholder_requests.get_all_posts.method` | 14 | `str` | `GET` |
| `jsonplaceholder_requests.get_all_posts.endpoint` | 15 | `str` | `<environment- or user-supplied value>` |
| `jsonplaceholder_requests.get_single_post.method` | 17 | `str` | `GET` |
| `jsonplaceholder_requests.get_single_post.endpoint` | 18 | `str` | `<environment- or user-supplied value>` |
| `jsonplaceholder_requests.create_post.method` | 20 | `str` | `POST` |
| `jsonplaceholder_requests.create_post.endpoint` | 21 | `str` | `<environment- or user-supplied value>` |
| `jsonplaceholder_requests.update_post.method` | 23 | `str` | `PUT` |
| `jsonplaceholder_requests.update_post.endpoint` | 24 | `str` | `<environment- or user-supplied value>` |
| `jsonplaceholder_requests.delete_post.method` | 26 | `str` | `DELETE` |
| `jsonplaceholder_requests.delete_post.endpoint` | 27 | `str` | `<environment- or user-supplied value>` |
| `crm_requests.get_user_profile.method` | 32 | `str` | `<environment- or user-supplied value>` |
| `crm_requests.get_user_profile.endpoint` | 33 | `str` | `<environment- or user-supplied value>` |
| `crm_requests.create_new_lead.method` | 35 | `str` | `POST` |
| `crm_requests.create_new_lead.endpoint` | 36 | `str` | `<environment- or user-supplied value>` |

## `src/test/resources/config/config.yml`

Source: [`src/test/resources/config/config.yml`](../../../src/test/resources/config/config.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `browser` | 24 | `str` | `chrome` |
| `maximize_browser` | 29 | `bool` | `true` |
| `headless` | 35 | `str` | `false` |
| `ignoreHTTPSErrors` | 40 | `str` | `true` |
| `time_to_wait_in_seconds` | 44 | `str` | `10` |
| `runtimeWait` | 47 | `int` | `0` |
| `videoCapture` | 52 | `str` | `false` |
| `excelDocumentLocation` | 55 | `str` | `src/test/resources/testdata.xlsx` |
| `downloadDocument` | 58 | `str` | `src/test/downloads/` |
| `tool_qa_url` | 61 | `str` | `<environment- or user-supplied value>` |
| `HARNESS_PREPROD_STAGE` | 62 | `str` | `<environment- or user-supplied value>` |
| `database.db_type` | 72 | `str` | `<environment- or user-supplied value>` |
| `database.server_type` | 76 | `str` | `<environment- or user-supplied value>` |
| `database.server_name` | 80 | `str` | `<environment- or user-supplied value>` |
| `database.port` | 85 | `str` | `<environment- or user-supplied value>` |
| `database.database_name` | 89 | `str` | `<environment- or user-supplied value>` |
| `database.authentication` | 94 | `str` | `<environment- or user-supplied value>` |
| `database.encrypt` | 98 | `str` | `<environment- or user-supplied value>` |
| `database.trust_server_certificate` | 102 | `str` | `<environment- or user-supplied value>` |
| `database.login_timeout_seconds` | 105 | `str` | `<environment- or user-supplied value>` |
| `database.query_timeout_seconds` | 109 | `str` | `<environment- or user-supplied value>` |
| `database.fetch_size` | 113 | `str` | `<environment- or user-supplied value>` |
| `database.application_name` | 116 | `str` | `<environment- or user-supplied value>` |
| `database.username` | 120 | `str` | `<environment- or user-supplied value>` |
| `database.password_env_variable` | 121 | `str` | `<redacted>` |
| `api_services.jsonplaceholder.base_url` | 131 | `str` | `<environment- or user-supplied value>` |
| `api_services.jsonplaceholder.auth_token_env` | 134 | `str` | `<redacted>` |
| `api_services.internal_crm.base_url` | 138 | `str` | `<environment- or user-supplied value>` |
| `api_services.internal_crm.auth_token_env` | 143 | `str` | `<redacted>` |
| `reporting.per_feature_reports_enabled` | 162 | `bool` | `true` |
| `reporting.per_feature_reports_output_dir` | 167 | `str` | `test-output/per-feature-reports` |
| `reporting.per_feature_pdf_enabled` | 172 | `bool` | `false` |
| `reporting.per_feature_glass_pdf_enabled` | 179 | `bool` | `true` |
| `reporting.per_feature_glass_pdf_output_dir` | 184 | `str` | `test-output/per-feature-reports-glass` |
| `soft_assertions.enabled` | 208 | `bool` | `false` |
| `soft_assertions.retry_seconds` | 214 | `int` | `3` |
| `zip.extraction_dir` | 231 | `str` | `test-output/extracted` |
| `zip.cleanup_after_scenario` | 236 | `bool` | `true` |
| `zip.recursive_unzip` | 241 | `bool` | `true` |

## `src/test/resources/cucumber.properties`

Source: [`src/test/resources/cucumber.properties`](../../../src/test/resources/cucumber.properties)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `cucumber.publish.enabled` | 1 | `string` | `false` |

## `src/test/resources/data/sample_order_response.xml`

Source: [`src/test/resources/data/sample_order_response.xml`](../../../src/test/resources/data/sample_order_response.xml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `(no suite/class entry detected)` | — | `—` | Inspect XML source |

## `src/test/resources/elements/eStore_elements.yml`

Source: [`src/test/resources/elements/eStore_elements.yml`](../../../src/test/resources/elements/eStore_elements.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.TestHarness.body` | 3 | `str` | `CSS_body` |
| `elements.TestHarness.branch_name` | 4 | `str` | `CSS_#branchName` |
| `elements.TestHarness.branch_ID` | 5 | `str` | `CSS_#branchId` |
| `elements.TestHarness.email_flt` | 6 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.FormSelection` | 7 | `str` | `CSS_#selectForm` |
| `elements.TestHarness.ConsumerForm` | 8 | `str` | `XPATH_//select[@id='selectForm']/option[1]` |
| `elements.TestHarness.phoneNumber_flt` | 9 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.consumerZipCode_flt` | 10 | `str` | `CSS_#zipCode` |
| `elements.TestHarness.collateralZip_flt` | 11 | `str` | `CSS_#collateralZip` |
| `elements.TestHarness.cus_firstName_flt` | 12 | `str` | `CSS_#firstName` |
| `elements.TestHarness.cus_fastName_flt` | 13 | `str` | `CSS_#lastName` |
| `elements.TestHarness.cus_lastName_flt` | 14 | `str` | `CSS_#lastName` |
| `elements.TestHarness.product_group_flt` | 16 | `str` | `XPATH_(//label[text()='Product Group']/../div/select)[1]` |
| `elements.TestHarness.product_group` | 17 | `str` | `CSS_#productGroup-i0` |
| `elements.TestHarness.product_name` | 18 | `str` | `XPATH_(//label[text()='Business Loan Products']/../div/select)[1]` |
| `elements.TestHarness.consumer_deposit_product_name` | 19 | `str` | `XPATH_(//label[text()='Consumer Deposit Products']/../div/select)[1]` |
| `elements.TestHarness.createURL_btn` | 20 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.openURL_btn` | 21 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.calendar_icon_btn` | 22 | `str` | `Button_Choose date` |
| `elements.TestHarness.business_name` | 23 | `str` | `id_BusinessName` |
| `elements.TestHarness.ConsumerDepositProducts_flt` | 24 | `str` | `XPATH_//select[@id='consumerDepositProducts-i0']` |
| `elements.TestHarness.ConsumerLoanProducts_flt` | 25 | `str` | `CSS_#consumerLoanProducts-i0` |
| `elements.TestHarness.addAnotherProduct` | 26 | `str` | `XPATH_//label[text()='Add Another Product']` |
| `elements.TestHarness.environment_flt` | 27 | `str` | `CSS_selectEnvironment` |
| `elements.TestHarness.bankerAssisted` | 28 | `str` | `XPATH_//label[@for='isBankerAssisted']` |
| `elements.TestHarness.responsibilityCode` | 29 | `str` | `CSS_#responsibilityCode` |
| `elements.TestHarness.nmlsId` | 30 | `str` | `CSS_#nmlsNumber` |
| `elements.TestHarness.BusinessDepositProduct_flt` | 31 | `str` | `CSS_select#businessDepositProducts-i1` |
| `elements.TestHarness.ConsumerDepositProducts` | 32 | `str` | `XPATH_//select[@id='productGroup-i3']` |
| `elements.TestHarness.Consumer_product_group2` | 35 | `str` | `XPATH_//select[@id='consumerDepositProducts-i1']` |
| `elements.TestHarness.product_group_flt1` | 36 | `str` | `XPATH_//select[@id='productGroup-i0']` |
| `elements.TestHarness.product_group2_flt` | 37 | `str` | `XPATH_//select[@id='productGroup-i1']` |
| `elements.TestHarness.product_group3_flt` | 38 | `str` | `XPATH_//select[@id='productGroup-i2']` |
| `elements.TestHarness.product_group4_flt` | 39 | `str` | `XPATH_//select[@id='productGroup-i3']` |
| `elements.TestHarness.product_group5_flt` | 40 | `str` | `XPATH_//select[@id='productGroup-i4']` |
| `elements.TestHarness.product_group6_flt` | 41 | `str` | `XPATH_//select[@id='productGroup-i5']` |
| `elements.TestHarness.product_group7_flt` | 42 | `str` | `XPATH_//select[@id='productGroup-i6']` |
| `elements.TestHarness.product_group8_flt` | 43 | `str` | `XPATH_//select[@id='productGroup-i7']` |
| `elements.TestHarness.product_group9_flt` | 44 | `str` | `XPATH_//select[@id='productGroup-i8']` |
| `elements.TestHarness.product_group10_flt` | 45 | `str` | `XPATH_//select[@id='productGroup-i9']` |
| `elements.TestHarness.product_group11_flt` | 46 | `str` | `XPATH_//select[@id='productGroup-i10']` |
| `elements.TestHarness.appStatus` | 47 | `str` | `XPATH_//h2[text()='Your application is currently being reviewed.']` |
| `elements.TestHarness.lastNamePrimary` | 48 | `str` | `XPATH_//label[text()='Last Name']/following-sibling::input` |
| `elements.TestHarness.CD12Month` | 49 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[2]` |
| `elements.TestHarness.SpecialCD13Month` | 50 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[3]` |
| `elements.TestHarness.businessDepositSecondProduct` | 51 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[2]` |
| `elements.TestHarness.businessDepositThirdProduct` | 52 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[3]` |
| `elements.TestHarness.businessDepositFourthProduct` | 53 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[4]` |
| `elements.TestHarness.businessDepositFifthProduct` | 54 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[5]` |
| `elements.TestHarness.businessDepositSixthProduct` | 55 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[6]` |
| `elements.TestHarness.businessDepositSeventhProduct` | 56 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[7]` |
| `elements.TestHarness.businessDepositEighthProduct` | 57 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[8]` |
| `elements.TestHarness.businessDepositNinthProduct` | 58 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[9]` |
| `elements.TestHarness.businessDepositTenthProduct` | 59 | `str` | `XPATH_(//select[@data-ng-model='data.businessDepositProducts'])[10]` |
| `elements.TestHarness.businessLoansProduct` | 60 | `str` | `XPATH_//select[@id='businessLoanProducts-i2']` |
| `elements.TestHarness.businessLoansFirstProduct` | 61 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[1]` |
| `elements.TestHarness.businessLoansSecondProduct` | 62 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[2]` |
| `elements.TestHarness.BusinessLoanThirdProduct` | 63 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[3]` |
| `elements.TestHarness.BusinessLoanFourthProduct` | 64 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[4]` |
| `elements.TestHarness.BusinessLoanFifthProduct` | 65 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[5]` |
| `elements.TestHarness.BusinessLoanSixthProduct` | 66 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[6]` |
| `elements.TestHarness.BusinessLoanSeventhProduct` | 67 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[7]` |
| `elements.TestHarness.BusinessLoanEighthProduct` | 68 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[8]` |
| `elements.TestHarness.BusinessLoanNinthProduct` | 69 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[9]` |
| `elements.TestHarness.BusinessLoanTenthProduct` | 70 | `str` | `XPATH_(//select[@data-ng-model='data.businessLoanProducts'])[10]` |
| `elements.TestHarness.BusinessDepositProduct` | 71 | `str` | `XPATH_//select[@id='businessDepositProducts-i1']` |
| `elements.TestHarness.BusinessDepositProduct2` | 72 | `str` | `XPATH_//select[@id='businessDepositProducts-i4']` |
| `elements.TestHarness.consumer_loan_1` | 73 | `str` | `XPATH_//select[@id='consumerLoanProducts-i0']` |
| `elements.TestHarness.consumer_loan_2` | 74 | `str` | `XPATH_//select[@id='consumerLoanProducts-i1']` |
| `elements.TestHarness.consumer_loan_3` | 75 | `str` | `XPATH_//select[@id='consumerLoanProducts-i2']` |
| `elements.TestHarness.consumer_loan_4` | 76 | `str` | `XPATH_//select[@id='consumerLoanProducts-i3']` |
| `elements.TestHarness.consumer_loan_5` | 77 | `str` | `XPATH_//select[@id='consumerLoanProducts-i4']` |
| `elements.TestHarness.consumer_loan_6` | 78 | `str` | `XPATH_//select[@id='consumerLoanProducts-i5']` |
| `elements.TestHarness.consumer_loan_7` | 79 | `str` | `XPATH_//select[@id='consumerLoanProducts-i6']` |
| `elements.TestHarness.consumer_loan_8` | 80 | `str` | `XPATH_//select[@id='consumerLoanProducts-i7']` |
| `elements.TestHarness.consumer_loan_9` | 81 | `str` | `XPATH_//select[@id='consumerLoanProducts-i8']` |
| `elements.TestHarness.consumer_loan_10` | 82 | `str` | `XPATH_//select[@id='consumerLoanProducts-i9']` |
| `elements.TestHarness.consumer_loan_11` | 83 | `str` | `XPATH_//select[@id='consumerLoanProducts-i10']` |
| `elements.TestHarness.txtPromo` | 85 | `str` | `CSS_#productPromo-i0` |
| `elements.TestHarness.txtPromo1` | 86 | `str` | `CSS_#productPromo-i1` |
| `elements.TestHarness.txtaApplicaitonURL` | 87 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.ddpConsumerDepositProductName` | 88 | `str` | `XPATH_//label[text()='Consumer Deposit Products']/../div/select` |
| `elements.TestHarness.consumer_loan_product` | 89 | `str` | `XPATH_//label[text()='Consumer Loan Products']/../div/select` |
| `elements.TestHarness.consumer_deposit_product` | 90 | `str` | `XPATH_//label[text()='Consumer Deposit Products']/../div/select` |
| `elements.TestHarness.business_loan_product` | 91 | `str` | `XPATH_//label[text()='Business Loan Products']/../div/select` |
| `elements.TestHarness.business_deposit_product` | 92 | `str` | `XPATH_//label[text()='Business Deposit Products']/../div/select` |
| `elements.TestHarness.consumer_loan_product_2` | 93 | `str` | `XPATH_//select[@id='consumerLoanProducts-i4']` |
| `elements.TestHarness.crossSell` | 94 | `str` | `CSS_#crossSellProduct` |
| `elements.TestHarness.crossSell1` | 95 | `str` | `XPATH_//select[@id='crossSellProduct']` |
| `elements.TestHarness.CrossSellProduct` | 96 | `str` | `XPATH_//select[@id='crossSellProduct']` |
| `elements.TestHarness.debugMode` | 98 | `str` | `XPATH_//input[@id='debugMode']` |
| `elements.TestHarness.existingCustomer` | 99 | `str` | `XPATH_//input[@id='existingCustomer']` |
| `elements.TestHarness.existingChecking` | 100 | `str` | `XPATH_//input[@id='typeDDA']` |
| `elements.TestHarness.ddProductGroup` | 102 | `str` | `XPATH_//select[@id='productGroup-i#index#']` |
| `elements.TestHarness.ddProductNameConsumerDeposit` | 103 | `str` | `XPATH_//select[@id='consumerDepositProducts-i#index#']` |
| `elements.TestHarness.ddProductNameConsumerLoan` | 104 | `str` | `XPATH_//select[@id='consumerLoanProducts-i#index#']` |
| `elements.TestHarness.ddProductNameBusinessDeposit` | 106 | `str` | `XPATH_//select[@id='businessDepositProducts-i#index#']` |
| `elements.TestHarness.ddProductNameBusinessLoan` | 107 | `str` | `XPATH_//select[@id='businessLoanProducts-i#index#']` |
| `elements.TestHarness.txtPromoField` | 108 | `str` | `XPATH_//input[@id='productPromo-i#index#']` |
| `elements.TestHarness.ddProductGroup1` | 110 | `str` | `XPATH_//select[@id='productGroup-i0']` |
| `elements.TestHarness.ddProductGroup2` | 111 | `str` | `XPATH_//select[@id='productGroup-i1']` |
| `elements.TestHarness.ddProductGroup3` | 112 | `str` | `XPATH_//select[@id='productGroup-i2']` |
| `elements.TestHarness.txtPromoField1` | 114 | `str` | `XPATH_//input[@id='productPromo-i0']` |
| `elements.TestHarness.txtPromoField2` | 115 | `str` | `XPATH_//input[@id='productPromo-i1']` |
| `elements.TestHarness.txtPromoField3` | 116 | `str` | `XPATH_//input[@id='productPromo-i2']` |
| `elements.TestHarness.ddProductNameConsumerDeposit1` | 118 | `str` | `XPATH_//select[@id='consumerDepositProducts-i0']` |
| `elements.TestHarness.ddProductNameConsumerDeposit2` | 119 | `str` | `XPATH_//select[@id='consumerDepositProducts-i1']` |
| `elements.TestHarness.ddProductNameConsumerDeposit3` | 120 | `str` | `XPATH_//select[@id='consumerDepositProducts-i2']` |
| `elements.TestHarness.ddProductNameBusinessDeposit1` | 122 | `str` | `XPATH_//select[@id='consumerDepositProducts-i0']` |
| `elements.TestHarness.ddProductNameBusinessDeposit2` | 123 | `str` | `XPATH_//select[@id='businessDepositProducts-i1']` |
| `elements.TestHarness.ddProductNameBusinessDeposit3` | 124 | `str` | `XPATH_//select[@id='businessDepositProducts-i2']` |
| `elements.TestHarness.ddProductNameConsumerLoan1` | 126 | `str` | `XPATH_//select[@id='consumerLoanProducts-i0']` |
| `elements.TestHarness.ddProductNameConsumerLoan2` | 127 | `str` | `XPATH_//select[@id='consumerLoanProducts-i1']` |
| `elements.TestHarness.ddProductNameConsumerLoan3` | 128 | `str` | `XPATH_//select[@id='consumerLoanProducts-i2']` |
| `elements.TestHarness.ssnExisting` | 132 | `str` | `XPATH_//input[@id='Customers.Primary.Tin']` |
| `elements.TestHarness.phoneExisting` | 133 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.accountExisting` | 134 | `str` | `<environment- or user-supplied value>` |
| `elements.TestHarness.balanceExisting` | 135 | `str` | `XPATH_//input[@id='Customers.Primary.FnbAccountBalance']` |
| `elements.consumer_personal_info.body` | 138 | `str` | `CSS_body` |
| `elements.consumer_personal_info.email_flt` | 139 | `str` | `<environment- or user-supplied value>` |
| `elements.consumer_personal_info.business_name_txt` | 140 | `str` | `CSS_#BusinessName` |
| `elements.consumer_personal_info.dba_name_txt` | 141 | `str` | `CSS_#Dba` |
| `elements.consumer_personal_info.legalStructure_list` | 142 | `str` | `CSS_#LegalStructure` |
| `elements.consumer_personal_info.SoleProprietorship` | 143 | `str` | `OPTION_Sole Proprietorship` |
| `elements.consumer_personal_info.LLC` | 144 | `str` | `OPTION_Limited Liability Corporation (LLC)` |
| `elements.consumer_personal_info.non_profit_yes_radio` | 145 | `str` | `CSS_#NonProfityes` |
| `elements.consumer_personal_info.non_profit_no_radio` | 146 | `str` | `CSS_#NonProfitno` |
| `elements.consumer_personal_info.singleMemberLLC_yes_radio` | 147 | `str` | `CSS_#SingleMemberLLCyes` |
| `elements.consumer_personal_info.singleMemberLLC_no_radio` | 148 | `str` | `CSS_#SingleMemberLLCno` |
| `elements.consumer_personal_info.publicTradedCompany_chk` | 149 | `str` | `CSS_#CorporationSituations.PubliclyTradedCompany` |
| `elements.consumer_personal_info.financialInstitution_chk` | 150 | `str` | `CSS_#CorporationSituations.FinancialInstitution` |
| `elements.consumer_personal_info.insuranceCompanyByState_chk` | 151 | `str` | `CSS_#CorporationSituations.InsuranceCompanyRegulatedByState` |
| `elements.consumer_personal_info.noneOfThese_chk` | 152 | `str` | `CSS_#CorporationSituations.None` |
| `elements.consumer_personal_info.beneficialOwner_chk` | 153 | `str` | `CSS_#BusinessRelationships.BeneficialOwner` |
| `elements.consumer_personal_info.beneficialOwnershipPercent_txt` | 154 | `str` | `CSS_#BusinessRelationships.OwnershipPercentage` |
| `elements.consumer_personal_info.signer_chk` | 155 | `str` | `CSS_input[name='BusinessRelationships.Signer']` |
| `elements.consumer_personal_info.controlPerson_chk` | 156 | `str` | `CSS_#BusinessRelationships.ControlPerson` |
| `elements.consumer_personal_info.businessRole_list` | 157 | `str` | `CSS_#BusinessRole` |
| `elements.consumer_personal_info.Manager` | 158 | `str` | `OPTION_Manager` |
| `elements.consumer_personal_info.viewAndAccept_btn` | 159 | `str` | `XPATH_//button[text()='View and Accept']` |
| `elements.consumer_personal_info.iAccept_btn` | 160 | `str` | `CSS_#accept-button` |
| `elements.consumer_personal_info.next_btn` | 161 | `str` | `Button_Next` |
| `elements.consumer_personal_info.mobilePhone_flt` | 162 | `str` | `<environment- or user-supplied value>` |
| `elements.consumer_personal_info.DateOfBirth_name` | 163 | `str` | `id_Customers.Primary.DateOfBirth` |
| `elements.consumer_personal_info.prefill_info` | 164 | `str` | `CSS_#Path` |
| `elements.consumer_personal_info.ConsumerSSN_flt` | 167 | `str` | `xpath_input[id='Customers.Primary.Tin']` |
| `elements.consumer_personal_info.Street_Address_1` | 168 | `str` | `XPATH_(//label[text()='Street Address 1']/following::input)[1]` |
| `elements.consumer_personal_info.Consumer_SSN` | 169 | `str` | `XPATH_(//label[text()='Social Security Number']/following::input)[1]` |
| `elements.consumer_personal_info.first_Name` | 170 | `str` | `XPATH_(//label[text()='First Name']/following::input)[1]` |
| `elements.consumer_personal_info.middle_Name` | 171 | `str` | `XPATH_(//label[text()='Middle Name']/following::input)[1]` |
| `elements.consumer_personal_info.last_Name` | 172 | `str` | `XPATH_(//label[text()='Last Name']/following::input)[1]` |
| `elements.consumer_personal_info.view_and_accept` | 175 | `str` | `Button_View and Accept` |
| `elements.consumer_personal_info.i_accept_btn` | 176 | `str` | `Button_I Accept` |
| `elements.consumer_personal_info.canvas` | 177 | `str` | `XPATH_(//canvas)[1]` |
| `elements.consumer_personal_info.NextButton` | 179 | `str` | `TestID_continue` |
| `elements.consumer_personal_info.SaveButton` | 181 | `str` | `Button_Save` |
| `elements.consumer_personal_info.phoneNumber_txt` | 185 | `str` | `<environment- or user-supplied value>` |
| `elements.consumer_personal_info.dateOfBirth_txt` | 186 | `str` | `XPATH_//input[@name='Customers.Primary.DateOfBirth']` |
| `elements.consumer_personal_info.prefill_verify` | 187 | `str` | `XPATH_//div[contains(@class, 'GettingStartedPage_prefillCheckbox')]` |
| `elements.citizenship.body` | 190 | `str` | `CSS_body` |
| `elements.citizenship.citizen_yes_radio` | 191 | `str` | `CSS_Citizenship.CitizenResidentUsYes` |
| `elements.citizenship.citizen_no_radio` | 192 | `str` | `CSS_Citizenship.CitizenResidentUsNo` |
| `elements.citizenship.idType_inputlist` | 193 | `str` | `CSS_Identification.Type` |
| `elements.citizenship.idtype_driverslicense` | 194 | `str` | `OPTION_Driver's License` |
| `elements.citizenship.idNumber_txt` | 195 | `str` | `CSS_#Identification.Number` |
| `elements.citizenship.issueState_inputlist` | 196 | `str` | `CSS_Identification.State` |
| `elements.citizenship.issueState_PA` | 197 | `str` | `OPTION_PA` |
| `elements.citizenship.issueDate_txt` | 198 | `str` | `CSS_#c9865cff-61d9-47fe-b967-ba7663ec95d6` |
| `elements.citizenship.expiryDate_txt` | 199 | `str` | `CSS_#abfb0bae-a7d1-4733-863e-f5ee1a05230f` |
| `elements.citizenship.next_btn` | 200 | `str` | `Next` |
| `elements.citizenship.MailingAddressYes` | 201 | `str` | `CSS_Customers.Primary.HasDifferentMailingAddressNo` |
| `elements.citizenship.MailingAddressNo` | 202 | `str` | `CSS_Customers.Primary.HasDifferentMailingAddressYes` |
| `elements.citizenship.rdoAreYouUSCitizenYes` | 203 | `str` | `XPATH_//input[@id='Citizenship.CitizenResidentUsYes']` |
| `elements.citizenship.Are_you_a_citizen_No` | 204 | `str` | `XPATH_//input[@id='Citizenship.CitizenResidentUsNo']` |
| `elements.citizenship.ddIdType` | 205 | `str` | `XPATH_//input[@data-neuro-label='license--idType']` |
| `elements.citizenship.txtIdNumber` | 206 | `str` | `XPATH_//input[@id='Identification.Number']` |
| `elements.citizenship.txtDateOfIssued` | 207 | `str` | `XPATH_//input[@name='Identification.DateIssued']` |
| `elements.citizenship.txtDateOfExpiry` | 208 | `str` | `XPATH_//input[@name='Identification.DateExpired']` |
| `elements.citizenship.ddStateOfIssue` | 209 | `str` | `XPATH_//input[@id='Identification.State']` |
| `elements.citizenship.citizenshipLabel` | 211 | `str` | `Label_Yes` |
| `elements.citizenship.idType` | 212 | `str` | `Label_ID Type` |
| `elements.citizenship.idValue` | 213 | `str` | `Option_Driver's License` |
| `elements.citizenship.idNumber` | 214 | `str` | `Label_ID Number` |
| `elements.citizenship.stateOfIssue` | 215 | `str` | `Label_State of Issue` |
| `elements.citizenship.stateOfIssueValue` | 216 | `str` | `Option_PA` |
| `elements.citizenship.issueDate` | 217 | `str` | `Label_Issue Date` |
| `elements.citizenship.expDate` | 218 | `str` | `Label_Expiration Date` |
| `elements.citizenship.ConsumerSSN` | 219 | `str` | `XPATH_(//label[text()='Social Security Number']/following::input)[1]` |
| `elements.citizenship.NextButton` | 220 | `str` | `XPATH_//button[text()='Next']` |
| `elements.citizenship.SaveButton` | 221 | `str` | `Button_Save` |
| `elements.citizenship.FnbDashboardUrl` | 222 | `str` | `<environment- or user-supplied value>` |
| `elements.citizenship.country_of_citizenship` | 223 | `str` | `XPATH_//input[@id='Citizenship.CountryOfCitizenship']` |
| `elements.citizenship.Nocitizenship` | 224 | `str` | `XPATH_//label[contains(text(),'are you a citizen of the United Stat…` |
| `elements.citizenship.yescitizenship` | 225 | `str` | `XPATH_//label[contains(text(),'are you a citizen of the United Stat…` |
| `elements.citizenship.CountryOfCitizenship` | 226 | `str` | `XPATH_(//label[text()='Country of Citizenship']/following::input)[1]` |
| `elements.citizenship.yesUSGreenCard` | 227 | `str` | `XPATH_(//label[text()='Do you have a United States alien registrati…` |
| `elements.citizenship.noUSGreenCard` | 228 | `str` | `XPATH_(//label[text()='Do you have a United States alien registrati…` |
| `elements.citizenship.yesGovernmentIssuedVisas` | 229 | `str` | `XPATH_(//label[text()='Do you hold any of the following U.S. govern…` |
| `elements.citizenship.noGovernmentIssuedVisas` | 230 | `str` | `XPATH_(//label[text()='Do you hold any of the following U.S. govern…` |
| `elements.citizenship.yesResidentalien` | 231 | `str` | `XPATH_(//label[text()='Are you a resident alien for tax reporting p…` |
| `elements.citizenship.noResidentalien` | 232 | `str` | `XPATH_(//label[text()='Are you a resident alien for tax reporting p…` |
| `elements.citizenship.rdoConsumerAreYouUSCitizenYes` | 233 | `str` | `XPATH_//input[@id='Customers.Primary.CitizenResidentUsYes']` |
| `elements.citizenship.rdoConsumerAreYouUSCitizenNo` | 234 | `str` | `XPATH_//input[@id='Customers.Primary.CitizenResidentUsNo']` |
| `elements.citizenship.ddCountryOfCitizenship` | 235 | `str` | `XPATH_//input[@id='Customers.Primary.CountryOfCitizenship']` |
| `elements.citizenship.ddCountryOfCitizenshipValue` | 236 | `str` | `Option_Australia` |
| `elements.citizenship.rdoHasAlienRegistrationCardNo` | 237 | `str` | `XPATH_//input[@id='Customers.Primary.HasAlienRegistrationCardNo']` |
| `elements.citizenship.rdoHasGovernmentIssuedVisaNo` | 238 | `str` | `XPATH_//input[@id='Customers.Primary.HasGovernmentIssuedVisaNo']` |
| `elements.citizenship.rdoIsResidentAlienNo` | 239 | `str` | `XPATH_//input[@id='Customers.Primary.IsResidentAlienNo']` |
| `elements.citizenship.GovtValidVisaYes` | 240 | `str` | `XPATH_//input[@id='Customers.Primary.HasGovernmentIssuedVisaYes']` |
| `elements.citizenship.rdoCoAppAreYouUSCitizenYes` | 241 | `str` | `XPATH_//input[@type='radio' and contains(@id, 'CitizenResidentUsYes')]` |
| `elements.primaryResidence.noOfYearsPrimary` | 244 | `str` | `CSS_#CurrentAddressDuration.Years` |
| `elements.primaryResidence.mailingAddressYes` | 245 | `str` | `CSS_#MailingAddressSameAsCurrentAddressyes` |
| `elements.primaryResidence.next_btn` | 246 | `str` | `Next` |
| `elements.primaryResidence.no_Of_Years_Primary` | 248 | `str` | `CSS_input[name='CurrentAddressDuration.Years']` |
| `elements.primaryResidence.mailingAddress_Yes` | 249 | `str` | `Label_Yes` |
| `elements.primaryResidence.body` | 250 | `str` | `CSS_body` |
| `elements.primaryResidence.yearsOfResidence` | 251 | `str` | `XPATH_//label[text()='Years']/../following-sibling::div/input` |
| `elements.primaryResidence.mailingAddress` | 252 | `str` | `Label_Yes` |
| `elements.primaryResidence.residenceStatuslbl` | 253 | `str` | `Label_Residence Status` |
| `elements.primaryResidence.residenceStatus` | 254 | `str` | `Label_Residence Status` |
| `elements.primaryResidence.currentRentOrMor` | 255 | `str` | `Label_Current Rent/Mortgage Payment` |
| `elements.primaryResidence.residenceType` | 256 | `str` | `Option_Own` |
| `elements.primaryResidence.residenceStatusOwn` | 257 | `str` | `Option_Own` |
| `elements.primaryResidence.rent` | 258 | `str` | `XPATH_//label[text()='Current Rent/Mortgage Payment']/../following-…` |
| `elements.primaryResidence.noOfYears` | 259 | `str` | `CSS_input[name='Customers.Primary.AddressCurrent.Years']` |
| `elements.primaryResidence.rdoConsumerIsMailingAddressSameYes` | 260 | `str` | `XPATH_//input[@id='Customers.Primary.HasDifferentMailingAddressNo']` |
| `elements.primaryResidence.rdoConsumerIsMailingAddressSameNo` | 261 | `str` | `XPATH_//input[@id='Customers.Primary.HasDifferentMailingAddressYes']` |
| `elements.primaryResidence.rdoCoAppIsMailingAddressSameYes` | 262 | `str` | `XPATH_//input[contains(@name, 'HasDifferentMailingAddress') and @ty…` |
| `elements.primaryResidence.rdoCoAppIsMailingAddressSameNo` | 263 | `str` | `XPATH_//input[contains(@name, 'HasDifferentMailingAddress') and @ty…` |
| `elements.primaryResidence.save_btn` | 264 | `str` | `XPATH_//button[@aria-label='Save']` |
| `elements.primaryResidence.save_in_pop_up` | 265 | `str` | `XPATH_//button[@class='koji9Wfgg3Zabm1ZVx45 DJ7RwdyZ9mc25g9F3bBM'][…` |
| `elements.primaryResidence.NextButton` | 266 | `str` | `XPATH_//button[text()='Next']` |
| `elements.EmploymentIncome.body` | 269 | `str` | `CSS_body` |
| `elements.EmploymentIncome.employmentStatus` | 270 | `str` | `Label_Employment Status` |
| `elements.EmploymentIncome.employmentType` | 271 | `str` | `Option_Full Time` |
| `elements.EmploymentIncome.employmentTypeNoExp` | 272 | `str` | `Option_No Prior Work Experience` |
| `elements.EmploymentIncome.empName` | 273 | `str` | `Label_Employer Name` |
| `elements.EmploymentIncome.jobTitle` | 274 | `str` | `Label_Job Title` |
| `elements.EmploymentIncome.AnnualIncome` | 275 | `str` | `Label_Estimated Annual Income` |
| `elements.EmploymentIncome.OccupationCategory_flt` | 276 | `str` | `CSS_#Employment.OccupationCategory` |
| `elements.EmploymentIncome.OccupationCategory_Arch&Eng` | 277 | `str` | `OPTION_Architecture and Engineering` |
| `elements.EmploymentIncome.highRisk_None_Chk` | 278 | `str` | `CSS_#HighRiskQuestions.None` |
| `elements.EmploymentIncome.highRisk_marijuana_Chk` | 279 | `str` | `XPATH_//input[@id='HighRiskQuestions.MarijuanaBusiness']` |
| `elements.EmploymentIncome.OcpCategory` | 280 | `str` | `Label_Occupation Category` |
| `elements.EmploymentIncome.OcpType` | 281 | `str` | `Option_Management` |
| `elements.EmploymentIncome.payFreq` | 282 | `str` | `Label_Pay Frequency` |
| `elements.EmploymentIncome.payFreqSel` | 283 | `str` | `Option_Annual` |
| `elements.EmploymentIncome.paymentAmount` | 284 | `str` | `Label_Payment Amount` |
| `elements.EmploymentIncome.years` | 285 | `str` | `Label_Years` |
| `elements.EmploymentIncome.months` | 286 | `str` | `Label_Months` |
| `elements.EmploymentIncome.EmploymentYears_flt` | 287 | `str` | `CSS_#Customers.Primary.EmploymentCurrent.Years` |
| `elements.EmploymentIncome.EmploymentMonths_flt` | 288 | `str` | `CSS_#Customers.Primary.EmploymentCurrent.Months` |
| `elements.EmploymentIncome.nonOfTheAbove` | 289 | `str` | `XPATH_//input[@data-analytics-label='None of the Above']` |
| `elements.EmploymentIncome.noneOfTheAbove_BL` | 291 | `str` | `XPATH_//div[normalize-space()='None of the Above']` |
| `elements.EmploymentIncome.NextButtonBL` | 292 | `str` | `XPATH_//button[@aria-label='Next']` |
| `elements.EmploymentIncome.NextButton` | 294 | `str` | `XPATH_//button[text()='Next']` |
| `elements.EmploymentIncome.maritalStatus` | 295 | `str` | `Label_Marital Status` |
| `elements.EmploymentIncome.maritalStatusValue` | 296 | `str` | `Option_Married` |
| `elements.addCoApplicant.body` | 299 | `str` | `CSS_body` |
| `elements.addCoApplicant.addcoApp_radio` | 300 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AddCoapplicantY…` |
| `elements.addCoApplicant.Yes_CoApp` | 301 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AddCoapplicantY…` |
| `elements.addCoApplicant.No_CoApp` | 302 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AddCoapplicantNo']` |
| `elements.addCoApplicant.coApp_existingCust_radio` | 303 | `str` | `XPATH_(//input[@name='ShoppingCart.Products.Product[0].CoapplicantL…` |
| `elements.addCoApplicant.CoAppExistingFNBCusNo` | 304 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].IsCurr…` |
| `elements.addCoApplicant.addCoApp_btn` | 305 | `str` | `Button_Add Co-Applicant` |
| `elements.addCoApplicant.firstname_txt` | 306 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].FirstN…` |
| `elements.addCoApplicant.lastname_txt` | 307 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].LastNa…` |
| `elements.addCoApplicant.ssn_txt` | 308 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].Tin']` |
| `elements.addCoApplicant.emailAddress_txt` | 309 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.coAppSrt_address1` | 310 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].Addres…` |
| `elements.addCoApplicant.AccountPurpose` | 313 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.PersonalAccount` | 315 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.SourceOfFunds` | 316 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Fund…` |
| `elements.addCoApplicant.RetirementAccount` | 317 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.ExcessCashDepositAndWithdrawalsYes` | 318 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Acco…` |
| `elements.addCoApplicant.ExcessCashDepositAndWithdrawalsNo` | 319 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Acco…` |
| `elements.addCoApplicant.NoForeignWireTransfer` | 320 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Fore…` |
| `elements.addCoApplicant.YesForeignWireTransfer` | 321 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Fore…` |
| `elements.addCoApplicant.NoDomesticWireTransfer` | 322 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Dome…` |
| `elements.addCoApplicant.YesDomesticWireTransfer` | 323 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AmlDetails.Dome…` |
| `elements.addCoApplicant.OverdraftCoverageYes` | 324 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].KeepDefaultOver…` |
| `elements.addCoApplicant.OverdraftCoverageNo` | 325 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].KeepDefaultOver…` |
| `elements.addCoApplicant.OverdraftAgreementPage` | 326 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.addCoApplicant.SignatureBox` | 327 | `str` | `XPATH_//input[@id='Customers.Primary.SignedDisclosures.DynamicDiscl…` |
| `elements.addCoApplicant.AcceptButton` | 328 | `str` | `XPATH_//button[normalize-space()='I Accept']` |
| `elements.addCoApplicant.Nxt_Button` | 329 | `str` | `XPATH_//button[normalize-space()='Next']` |
| `elements.addCoApplicant.mobilePhone_flt` | 330 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.beneficialOwner_checkbox_flt` | 331 | `str` | `CSS_#BeneficialOwner` |
| `elements.addCoApplicant.signer_checkbox_flt` | 332 | `str` | `CSS_#Signer` |
| `elements.addCoApplicant.controlPerson_checkbox_flt` | 333 | `str` | `CSS_#ControlPerson` |
| `elements.addCoApplicant.businessRole/title_flt` | 334 | `str` | `CSS_#BusinessRole` |
| `elements.addCoApplicant.ownership_flt` | 335 | `str` | `CSS_#Ownership` |
| `elements.addCoApplicant.save_btn` | 336 | `str` | `Button_Save` |
| `elements.addCoApplicant.SignersOfDepositYes_checkbox_flt` | 337 | `str` | `CSS_#SignersOfDepositYes` |
| `elements.addCoApplicant.SignersOfDepositNo_checkbox_flt` | 338 | `str` | `CSS_#SignersOfDepositNo` |
| `elements.addCoApplicant.HasBusinessOwnershipYes_checkbox_flt` | 339 | `str` | `XPATH_//input[@id='HasBusinessOwnershipYes']` |
| `elements.addCoApplicant.HasBusinessOwnershipNo_checkbox_flt` | 340 | `str` | `CSS_#HasBusinessOwnershipNo` |
| `elements.addCoApplicant.IsForeignEntityYes_checkbox_flt` | 341 | `str` | `CSS_#IsForeignEntityYes` |
| `elements.addCoApplicant.IsForeignEntityNo_checkbox_flt` | 342 | `str` | `CSS_#IsForeignEntityNo` |
| `elements.addCoApplicant.UserProve_checkbox_flt` | 343 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.back_btn` | 344 | `str` | `Button_Back` |
| `elements.addCoApplicant.cancel_btn` | 345 | `str` | `Button_Cancel` |
| `elements.addCoApplicant.next_btn` | 346 | `str` | `XPATH_//button[text()='Next']` |
| `elements.addCoApplicant.add_CoApp_yes_btn` | 347 | `str` | `CSS_input[value='Yes']` |
| `elements.addCoApplicant.addCoAppNoBtn` | 348 | `str` | `XPATH_//input[@value='No']` |
| `elements.addCoApplicant.chkICertify` | 351 | `str` | `CSS_#UseProve` |
| `elements.addCoApplicant.addCoApp` | 352 | `str` | `Button_Add Co-Applicant` |
| `elements.addCoApplicant.CoAppAdd` | 353 | `str` | `XPATH_//div[text()='Add Co-Applicant']` |
| `elements.addCoApplicant.firstNameInPopup` | 354 | `str` | `CSS_input[name='FirstName']` |
| `elements.addCoApplicant.lastNameInPopup` | 355 | `str` | `CSS_input[name='LastName']` |
| `elements.addCoApplicant.emailInPopup` | 356 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.mobileInPopup` | 357 | `str` | `XPATH_input[name='MobilePhone']` |
| `elements.addCoApplicant.beneficialOwnerChkBox` | 358 | `str` | `XPATH_//input[@name='BeneficialOwner']\|//input[@name='ownershiptype']` |
| `elements.addCoApplicant.ownershipPercentage` | 359 | `str` | `Name_Ownership` |
| `elements.addCoApplicant.signerChkBox` | 360 | `str` | `XPATH_//input[@name='Signer']\|//input[@name='signer']` |
| `elements.addCoApplicant.conttrolPersonCheckBox` | 361 | `str` | `XPATH_//input[@name='ControlPerson']` |
| `elements.addCoApplicant.Guarantor_CheckBox` | 362 | `str` | `XPATH_//input[@id='Guarantor']` |
| `elements.addCoApplicant.businessRole` | 363 | `str` | `CSS_#BusinessRole` |
| `elements.addCoApplicant.manager` | 364 | `str` | `XPATH_//li[text()='Manager']` |
| `elements.addCoApplicant.saveInPopup` | 365 | `str` | `XPATH_//button[@type='submit']` |
| `elements.addCoApplicant.SecondSaveInPopup` | 366 | `str` | `XPATH_(//button[@type='submit'])[1]` |
| `elements.addCoApplicant.addAllSingerYes` | 367 | `str` | `XPATH_(//input[@data-analytics-label='Would you like all signers to…` |
| `elements.addCoApplicant.addAllbusSingerYes` | 368 | `str` | `XPATH_//input[@id='SignersOfDepositYes']` |
| `elements.addCoApplicant.addAllSingerNo` | 369 | `str` | `XPATH_(//input[@data-analytics-label='Would you like all signers to…` |
| `elements.addCoApplicant.addAllBusSingerNo` | 370 | `str` | `XPATH_//input[@id='SignersOfDepositNo']` |
| `elements.addCoApplicant.yesbusinessOwnerShip` | 371 | `str` | `XPATH_(//input[@data-analytics-label='Does another business have ow…` |
| `elements.addCoApplicant.nobusinessOwnerShip` | 372 | `str` | `XPATH_(//input[@data-analytics-label='Does another business have ow…` |
| `elements.addCoApplicant.yesForeignGovernment` | 373 | `str` | `XPATH_//input[@id='IsForeignEntityYes']` |
| `elements.addCoApplicant.noForeignGovernment` | 374 | `str` | `XPATH_//input[@id='IsForeignEntityNo']` |
| `elements.addCoApplicant.herebyCertify` | 376 | `str` | `Text_hereby certify, to the best of my knowledge, that the informat…` |
| `elements.addCoApplicant.NextButton` | 377 | `str` | `XPATH_//button[text()='Next']` |
| `elements.addCoApplicant.there_are_no_beneficial_owners_with_25_percent_checkbox` | 378 | `str` | `XPATH_//div[@role='dialog']//div//input[@type='checkbox']` |
| `elements.addCoApplicant.Continue_btn` | 379 | `str` | `XPATH_//button[normalize-space()='Continue']` |
| `elements.addCoApplicant.add_coapp_check_box` | 381 | `str` | `CSS_input[name='applicantCheckbox-0']` |
| `elements.addCoApplicant.add_coapp_check_box_2` | 382 | `str` | `XPATH_//input[@id='applicantCheckbox-1']` |
| `elements.addCoApplicant.business_name` | 383 | `str` | `XPATH_//input [@id='BusinessOwners.0.BusinessName']` |
| `elements.addCoApplicant.ownership_percentage` | 384 | `str` | `XPATH_//input [@id='BusinessOwners[0].Ownership']` |
| `elements.addCoApplicant.add_another_business` | 385 | `str` | `Button_Add Another Business` |
| `elements.addCoApplicant.hasBusinessOwnershipNo` | 386 | `str` | `XPATH_//input[@id='HasBusinessOwnershipNo']` |
| `elements.addCoApplicant.business_name_1` | 387 | `str` | `XPATH_//input [@id='BusinessOwners.1.BusinessName']` |
| `elements.addCoApplicant.ownership_percentage_1` | 388 | `str` | `XPATH_//input [@id='BusinessOwners[1].Ownership']` |
| `elements.addCoApplicant.does_another_business_have_25percent_or_more_in_this_business_no` | 389 | `str` | `XPATH_//input [@id='BusinessOwners[0].HasAdditionalOwnershipNo']` |
| `elements.addCoApplicant.does_another_business_have_25percent_or_more_in_this_business_no_1` | 390 | `str` | `XPATH_//input [@id='BusinessOwners[1].HasAdditionalOwnershipNo']` |
| `elements.addCoApplicant.would_you_like_all_signers_to_be_added_yes` | 391 | `str` | `XPATH_//input [@id='SignersOfDepositYes']` |
| `elements.addCoApplicant.yesAddCoApp` | 398 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.addCoApplicant.noAddCoApp` | 399 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.addCoApplicant.i_want_to_add_a_new_coapp` | 400 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[1].CoapplicantLock…` |
| `elements.addCoApplicant.requestedAmount` | 401 | `str` | `XPATH_//label[text()='Requested Amount']/..//following-sibling::div…` |
| `elements.addCoApplicant.LoanTermLabel` | 402 | `str` | `Label_Loan Term` |
| `elements.addCoApplicant.LoanTerm` | 403 | `str` | `Option_3 years` |
| `elements.addCoApplicant.LoanPurposeLabel` | 404 | `str` | `Label_Loan Purpose` |
| `elements.addCoApplicant.LoanPurpose` | 405 | `str` | `Option_Refinance` |
| `elements.addCoApplicant.colAddress` | 406 | `str` | `XPATH_(//label[text()='Please select the property you will be using…` |
| `elements.addCoApplicant.colCountry` | 407 | `str` | `XPATH_//label[text()='Enter the county where this property is locat…` |
| `elements.addCoApplicant.propertyTypeLabel` | 408 | `str` | `Label_Property Type` |
| `elements.addCoApplicant.propertyType` | 409 | `str` | `Option_Single Family` |
| `elements.addCoApplicant.propertyOcupLabel` | 410 | `str` | `Label_Property Occupancy` |
| `elements.addCoApplicant.PropertyOccupancy` | 411 | `str` | `Option_Second Home` |
| `elements.addCoApplicant.estimatedPropertyValue` | 412 | `str` | `XPATH_//label[text()='Estimated Property Value']/../following-sibli…` |
| `elements.addCoApplicant.mortgageHeader` | 413 | `str` | `XPATH_//div[contains(text(),'+ Add Mortgage Holder')]` |
| `elements.addCoApplicant.mortgageHeaderName` | 414 | `str` | `Name_MortgageHolderName` |
| `elements.addCoApplicant.mortgageBalance` | 415 | `str` | `Name_Balance` |
| `elements.addCoApplicant.monthlyMortgagePayment` | 416 | `str` | `Name_MonthlyPayment` |
| `elements.addCoApplicant.mortgagePayoff_Yes` | 417 | `str` | `XPATH_(//label[text()='Will this loan be used to pay off this mortg…` |
| `elements.addCoApplicant.saveMortgage` | 418 | `str` | `Button_Save` |
| `elements.addCoApplicant.existingMortgage_Yes` | 419 | `str` | `XPATH_(//label[text()='Do you have any existing mortgages on this p…` |
| `elements.addCoApplicant.checkingAccount_Yes` | 420 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.existingFNBCustomer_No` | 421 | `str` | `XPATH_//label[text()='No']/preceding-sibling::input` |
| `elements.addCoApplicant.firstname_txt_Co_app2` | 422 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[1].FirstN…` |
| `elements.addCoApplicant.lastname_txt_Co_app2` | 423 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[1].LastNa…` |
| `elements.addCoApplicant.ssn_txt_Co_app2` | 424 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[1].Tin']` |
| `elements.addCoApplicant.emailAddress_txt_Co_app2` | 425 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.coAppSrt_address1_Co_app2` | 426 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[1].Addres…` |
| `elements.addCoApplicant.addExistingCoApp` | 427 | `str` | `XPATH_(//input[@data-analytics-label='Please select your co-applica…` |
| `elements.addCoApplicant.view_and_accept` | 428 | `str` | `Button_View and Accept` |
| `elements.addCoApplicant.canvas` | 429 | `str` | `XPATH_(//canvas)[1]` |
| `elements.addCoApplicant.canvasPromo` | 430 | `str` | `XPATH_//div[@class='PdfPage_pageRow__82qzL']` |
| `elements.addCoApplicant.i_accept_btn` | 431 | `str` | `Button_I Accept` |
| `elements.addCoApplicant.yesAddCoapplicant` | 433 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.addCoApplicant.noAddCoapplicant` | 434 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.addCoApplicant.selectExistingCoapplicant` | 435 | `str` | `XPATH_(//label[text()='Please select your co-applicant:']/following…` |
| `elements.addCoApplicant.yesExistingFNBcustomer` | 438 | `str` | `XPATH_(//input[@data-analytics-label='Is the co-applicant on this a…` |
| `elements.addCoApplicant.noExistingFNBcustomer` | 439 | `str` | `XPATH_(//input[@data-analytics-label='Is the co-applicant on this a…` |
| `elements.addCoApplicant.selectCoApp` | 446 | `str` | `XPATH_(//input[@data-analytics-label='Please select your co-applica…` |
| `elements.addCoApplicant.specialCDFNBAccountCheck` | 447 | `str` | `<environment- or user-supplied value>` |
| `elements.addCoApplicant.purpose` | 448 | `str` | `Label_What is the purpose of this` |
| `elements.addCoApplicant.purposeType` | 449 | `str` | `Option_Personal` |
| `elements.addCoApplicant.source` | 450 | `str` | `Label_What is the source of funds` |
| `elements.addCoApplicant.sourceType` | 451 | `str` | `Option_Employment Income` |
| `elements.addCoApplicant.cashDeposit` | 452 | `str` | `XPATH_(//input[@data-analytics-label='Will this account have U.S. c…` |
| `elements.addCoApplicant.wireTransfer` | 453 | `str` | `XPATH_(//input[@data-analytics-label='Will there be monthly domesti…` |
| `elements.addCoApplicant.foreignWTransfer` | 454 | `str` | `XPATH_(//input[@data-analytics-label='Will there be monthly foreign…` |
| `elements.addCoApplicant.automaticCoverage` | 455 | `str` | `XPATH_//label[text()='Would you like to keep the coverage for check…` |
| `elements.addCoApplicant.signature` | 456 | `str` | `CSS_input[placeholder='Type your signature here']` |
| `elements.addCoApplicant.existingCoAppCheckBox1` | 458 | `str` | `XPATH_(//input[@type='checkbox'])[1]` |
| `elements.addCoApplicant.noCoApp` | 459 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.addCoApplicant.schoolName` | 460 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AccountDetails.…` |
| `elements.addCoApplicant.healthSavingsYear` | 461 | `str` | `XPATH_//input[@value='2025']` |
| `elements.addCoApplicant.workplaceCode` | 462 | `str` | `XPATH_//input[@data-analytics-label='Workplace Banking Employer Code']` |
| `elements.addCoApplicant.secondDepositCoApp` | 463 | `str` | `XPATH_(//input[@name='ShoppingCart.Products.Product[0].CoapplicantL…` |
| `elements.coAppGetStarted.mobilePhone` | 466 | `str` | `<environment- or user-supplied value>` |
| `elements.coAppGetStarted.dateofBirth` | 467 | `str` | `Placeholder_mm/dd/yyyy` |
| `elements.coAppGetStarted.viewAndAccept` | 468 | `str` | `XPATH_//button[text()='View and Accept']` |
| `elements.coAppGetStarted.canvas` | 469 | `str` | `XPATH_(//canvas)[1]` |
| `elements.coAppGetStarted.body` | 470 | `str` | `CSS_body` |
| `elements.coAppGetStarted.i_accept_btn` | 471 | `str` | `Button_I Accept` |
| `elements.coAppGetStarted.prefill` | 472 | `str` | `XPATH_//div[text()='Yes, pre-fill my information for me.']/../span` |
| `elements.coAppGetStarted.NextButton` | 473 | `str` | `XPATH_//button[text()='Next']` |
| `elements.coAppGetStarted.coAppMobilePhone` | 474 | `str` | `<environment- or user-supplied value>` |
| `elements.coAppGetStarted.ExistingCoApp` | 475 | `str` | `XPATH_(//input[@name='ShoppingCart.Products.Product[0].CoapplicantL…` |
| `elements.coAppGetStarted.dateOfBirth` | 476 | `str` | `Placeholder_mm/dd/yyyy` |
| `elements.coAppGetStarted.view_and_accept` | 477 | `str` | `Button_View and Accept` |
| `elements.coAppGetStarted.prefill_info` | 478 | `str` | `CSS_#Path` |
| `elements.coAppGetStarted.mobileNumber` | 481 | `str` | `XPATH_(//label[text()='Mobile Phone']/following::input)[1]` |
| `elements.coAppPersonalInfo.suffix` | 484 | `str` | `Label_Suffix` |
| `elements.coAppPersonalInfo.suffix_Text` | 485 | `str` | `Text_SR` |
| `elements.coAppPersonalInfo.ssn` | 486 | `str` | `Name_Tin` |
| `elements.coAppPersonalInfo.street_Address1` | 487 | `str` | `Name_AddressCurrent.Street1` |
| `elements.coAppPersonalInfo.street_Address2` | 488 | `str` | `Name_AddressCurrent.Street2` |
| `elements.coAppPersonalInfo.city` | 489 | `str` | `Name_AddressCurrent.City` |
| `elements.coAppPersonalInfo.state` | 490 | `str` | `Name_AddressCurrent.State` |
| `elements.coAppPersonalInfo.body` | 491 | `str` | `CSS_body` |
| `elements.coAppPersonalInfo.zip` | 492 | `str` | `Name_AddressCurrent.PostalCode` |
| `elements.coAppPersonalInfo.NextButton` | 493 | `str` | `XPATH_//button[text()='Next']` |
| `elements.coAppPersonalInfo.coAppFirstName` | 494 | `str` | `XPATH_(//label[text()='First Name']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppMiddleName` | 495 | `str` | `XPATH_(//label[text()='Middle Name']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppLastName` | 496 | `str` | `XPATH_(//label[text()='Last Name']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppEmail` | 497 | `str` | `<environment- or user-supplied value>` |
| `elements.coAppPersonalInfo.coAppSSN` | 498 | `str` | `XPATH_(//label[text()='Social Security Number']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppStreet_Address1` | 499 | `str` | `XPATH_(//label[text()='Street Address 1']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppStreet_Address2` | 500 | `str` | `XPATH_(//label[text()='Street Address 2']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppStreet_City` | 501 | `str` | `XPATH_(//label[text()='City']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppState` | 502 | `str` | `XPATH_(//label[text()='State']/following::input)[1]` |
| `elements.coAppPersonalInfo.coAppZIPCode` | 503 | `str` | `XPATH_//label[text()='ZIP Code']/following::input` |
| `elements.coAppPersonalInfo.coAppMobile` | 504 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].PhoneN…` |
| `elements.coAppPersonalInfo.CoAppDOB` | 505 | `str` | `XPATH_(//label[text()='Date of Birth']/following::input)[1]` |
| `elements.coAppPersonalInfo.residenceStatuslbl` | 506 | `str` | `Label_Residence Status` |
| `elements.coAppPersonalInfo.residenceType` | 507 | `str` | `Option_Own` |
| `elements.coAppPersonalInfo.mailingAddress` | 508 | `str` | `Label_Yes` |
| `elements.coAppPersonalInfo.ElectronicRecordsAgreement` | 509 | `str` | `XPATH_//button[text()='View and Accept']` |
| `elements.coAppPersonalInfo.canvas` | 510 | `str` | `XPATH_(//canvas)[1]` |
| `elements.coAppPersonalInfo.i_accept_btn` | 511 | `str` | `Button_I Accept` |
| `elements.coAppPersonalInfo.next_btn` | 512 | `str` | `Button_Next` |
| `elements.coAppPersonalInfo.prefill_info_no` | 513 | `str` | `CSS_#Path` |
| `elements.coAppPersonalInfo.save_btn` | 514 | `str` | `XPATH_//button[@aria-label='Save']` |
| `elements.coAppPersonalInfo.save_in_pop_up` | 515 | `str` | `XPATH_//button[@class='koji9Wfgg3Zabm1ZVx45 DJ7RwdyZ9mc25g9F3bBM'][…` |
| `elements.coAppCitizenship.body` | 518 | `str` | `CSS_body` |
| `elements.coAppCitizenship.citizenshipLabel` | 519 | `str` | `Label_Yes` |
| `elements.coAppCitizenship.USCitizenYes` | 520 | `str` | `Label_Yes` |
| `elements.coAppCitizenship.USCitizenNo` | 521 | `str` | `Label_No` |
| `elements.coAppCitizenship.idType` | 522 | `str` | `Label_ID Type` |
| `elements.coAppCitizenship.idValue` | 523 | `str` | `Option_Driver's License` |
| `elements.coAppCitizenship.DriverLicense` | 524 | `str` | `Option_Driver's License` |
| `elements.coAppCitizenship.idNumber` | 525 | `str` | `Label_ID Number` |
| `elements.coAppCitizenship.stateOfIssue` | 526 | `str` | `Label_State of Issue` |
| `elements.coAppCitizenship.stateOfIssueValue` | 527 | `str` | `Option_PA` |
| `elements.coAppCitizenship.issueDate` | 528 | `str` | `Label_Issue Date` |
| `elements.coAppCitizenship.expDate` | 529 | `str` | `Label_Expiration Date` |
| `elements.coAppCitizenship.ConsumerSSN` | 530 | `str` | `XPATH_(//label[text()='Social Security Number']/following::input)[1]` |
| `elements.coAppCitizenship.NextButton` | 531 | `str` | `XPATH_//button[text()='Next']` |
| `elements.coAppCitizenship.Nocitizenship` | 532 | `str` | `XPATH_//label[contains(text(),'are you a citizen of the United Stat…` |
| `elements.coAppCitizenship.yescitizenship` | 533 | `str` | `XPATH_//label[contains(text(),'are you a citizen of the United Stat…` |
| `elements.coAppCitizenship.CountryOfCitizenship` | 534 | `str` | `XPATH_(//label[text()='Country of Citizenship']/following::input)[1]` |
| `elements.coAppCitizenship.yesUSGreenCard` | 535 | `str` | `XPATH_(//label[text()='Do you have a United States alien registrati…` |
| `elements.coAppCitizenship.noUSGreenCard` | 536 | `str` | `XPATH_(//label[text()='Do you have a United States alien registrati…` |
| `elements.coAppCitizenship.yesGovernmentIssuedVisas` | 537 | `str` | `XPATH_(//label[text()='Do you hold any of the following U.S. govern…` |
| `elements.coAppCitizenship.noGovernmentIssuedVisas` | 538 | `str` | `XPATH_(//label[text()='Do you hold any of the following U.S. govern…` |
| `elements.coAppCitizenship.yesResidentalien` | 539 | `str` | `XPATH_(//label[text()='Are you a resident alien for tax reporting p…` |
| `elements.coAppCitizenship.noResidentalien` | 540 | `str` | `XPATH_(//label[text()='Are you a resident alien for tax reporting p…` |
| `elements.coAppCitizenship.coappRegretMsg` | 541 | `str` | `XPATH_//h3[contains(text(),'We regret we are unable to include')]` |
| `elements.coAppCitizenship.coAppDeclinedForDeposit` | 542 | `str` | `XPATH_//h3[contains(text(),'We regret we are unable to include the …` |
| `elements.coAppCitizenship.continueWithoutCoappMsg` | 545 | `str` | `XPATH_//p[contains(text(),'You may continue your application with t…` |
| `elements.coAppCitizenship.continueWithoutCoappMsg1` | 546 | `str` | `XPATH_//p[contains(text(),'You may continue your application withou…` |
| `elements.coAppCitizenship.declineDepositContinueLoan` | 547 | `str` | `XPATH_//p[contains(text(),'Unfortunately, you are not eligible for …` |
| `elements.coAppCitizenship.declineDepositHeader` | 548 | `str` | `XPATH_//h1[normalize-space()='We have updated the products in your …` |
| `elements.coAppResidenceAddress.noOfYearsPrimary` | 551 | `str` | `CSS_input[name='CurrentAddressDuration.Years']` |
| `elements.coAppResidenceAddress.mailingAddress` | 552 | `str` | `Label_Yes` |
| `elements.coAppResidenceAddress.body` | 553 | `str` | `CSS_body` |
| `elements.coAppResidenceAddress.NextButton` | 554 | `str` | `XPATH_//button[text()='Next']` |
| `elements.coAppResidenceAddress.noOfYearsCoapp` | 555 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].Addres…` |
| `elements.coAppResidenceAddress.residenceStatus` | 556 | `str` | `Label_Residence Status` |
| `elements.coAppResidenceAddress.rentOption` | 557 | `str` | `Option_Own` |
| `elements.coAppResidenceAddress.RentAmount` | 558 | `str` | `Label_Current Rent/Mortgage Payment` |
| `elements.coAppResidenceAddress.NumberOfYears` | 559 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].Addres…` |
| `elements.coAppResidenceAddress.SameMailingAddressYes` | 560 | `str` | `<environment- or user-supplied value>` |
| `elements.coAppResidenceAddress.SameMailingAddressNo` | 561 | `str` | `<environment- or user-supplied value>` |
| `elements.coAppResidenceAddress.mailingAddressYes` | 562 | `str` | `CSS_#MailingAddressSameAsCurrentAddressyes` |
| `elements.coAppResidenceAddress.next_btn` | 563 | `str` | `Next` |
| `elements.coAppResidenceAddress.no_Of_Years_Primary` | 564 | `str` | `CSS_input[name='CurrentAddressDuration.Years']` |
| `elements.coAppResidenceAddress.mailingAddress_Yes` | 565 | `str` | `Label_Yes` |
| `elements.coAppResidenceAddress.yearsOfResidence` | 566 | `str` | `XPATH_//label[text()='Years']/../following-sibling::div/input` |
| `elements.coAppResidenceAddress.residenceStatuslbl` | 567 | `str` | `Label_Residence Status` |
| `elements.coAppResidenceAddress.currentRentOrMor` | 568 | `str` | `Label_Current Rent/Mortgage Payment` |
| `elements.coAppResidenceAddress.residenceType` | 569 | `str` | `Option_Own` |
| `elements.coAppResidenceAddress.residenceStatusOwn` | 570 | `str` | `Option_Own` |
| `elements.coAppResidenceAddress.rent` | 571 | `str` | `XPATH_//label[text()='Current Rent/Mortgage Payment']/../following-…` |
| `elements.coAppResidenceAddress.noOfYears` | 572 | `str` | `CSS_input[name='Customers.Primary.AddressCurrent.Years']` |
| `elements.coAppResidenceAddress.rdoConsumerIsMailingAddressSameYes` | 573 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].HasDif…` |
| `elements.coAppResidenceAddress.rdoConsumerIsMailingAddressSameNo` | 574 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].HasDif…` |
| `elements.CoAppEmploymentIncome.body` | 578 | `str` | `CSS_body` |
| `elements.CoAppEmploymentIncome.employmentStatus` | 579 | `str` | `Label_Employment Status` |
| `elements.CoAppEmploymentIncome.EmpStatus_fullTime` | 580 | `str` | `OPTION_Full Time` |
| `elements.CoAppEmploymentIncome.EmpStatus_NoPriorWorkExp` | 581 | `str` | `OPTION_No Prior Work Experience` |
| `elements.CoAppEmploymentIncome.empName` | 582 | `str` | `Label_Employer Name` |
| `elements.CoAppEmploymentIncome.jobTitle` | 583 | `str` | `Label_Job Title` |
| `elements.CoAppEmploymentIncome.OcpCategory` | 584 | `str` | `Label_Occupation Category` |
| `elements.CoAppEmploymentIncome.OcpType` | 585 | `str` | `Option_Management` |
| `elements.CoAppEmploymentIncome.AnnualIncome` | 586 | `str` | `Label_Estimated Annual Income` |
| `elements.CoAppEmploymentIncome.NextButton` | 587 | `str` | `XPATH_//button[text()='Next']` |
| `elements.CoAppEmploymentIncome.nonOfTheAbove` | 588 | `str` | `XPATH_//input[@data-analytics-label='None of the Above']` |
| `elements.CoAppEmploymentIncome.payFreq` | 589 | `str` | `Label_Pay Frequency` |
| `elements.CoAppEmploymentIncome.payFreqSel` | 590 | `str` | `Option_Annual` |
| `elements.CoAppEmploymentIncome.paymentAmount` | 591 | `str` | `Label_Payment Amount` |
| `elements.CoAppEmploymentIncome.No_of_years_worked` | 592 | `str` | `CSS_input[data-analytics-label='Years']` |
| `elements.CoAppEmploymentIncome.employmentType` | 593 | `str` | `Option_Full Time` |
| `elements.CoAppEmploymentIncome.years` | 594 | `str` | `Label_Years` |
| `elements.CoAppEmploymentIncome.months` | 595 | `str` | `Label_Months` |
| `elements.proveValidation.body` | 599 | `str` | `CSS_body` |
| `elements.proveValidation.phoneNumber_txt` | 600 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.dateOfBirth_txt` | 601 | `str` | `XPATH_//input[@name='Customers.Primary.DateOfBirth']` |
| `elements.proveValidation.useProve_chk` | 602 | `str` | `XPATH_//input[@id='Customers.Primary.UseProve']` |
| `elements.proveValidation.view_and_accept` | 603 | `str` | `Button_View and Accept` |
| `elements.proveValidation.i_accept_btn` | 604 | `str` | `Button_I Accept` |
| `elements.proveValidation.canvas` | 605 | `str` | `XPATH_(//canvas)[1]` |
| `elements.proveValidation.canvasPromo` | 606 | `str` | `XPATH_//div[@class='PdfPage_pageRow__82qzL']` |
| `elements.proveValidation.prefill_info` | 607 | `str` | `CSS_#Path` |
| `elements.proveValidation.NextButton` | 608 | `str` | `XPATH_//button[text()='Next' or text()='Submit']` |
| `elements.proveValidation.coApp1_phnNum_txt` | 611 | `str` | `XPATH_//input[@name='Customers.AdditionalCustomers.Customer[0].Phon…` |
| `elements.proveValidation.coApp1_dob_txt` | 612 | `str` | `XPATH_//input[@name='Customers.AdditionalCustomers.Customer[0].Date…` |
| `elements.proveValidation.coApp2_phnNum_txt` | 613 | `str` | `XPATH_//input[@name='Customers.AdditionalCustomers.Customer[1].Phon…` |
| `elements.proveValidation.coApp2_dob_txt` | 614 | `str` | `XPATH_//input[@name='Customers.AdditionalCustomers.Customer[1].Date…` |
| `elements.proveValidation.Consumer_SSN` | 617 | `str` | `XPATH_(//label[text()='Social Security Number']/following::input)[1]` |
| `elements.proveValidation.Street_Address_1` | 618 | `str` | `XPATH_(//label[text()='Street Address 1']/following::input)[1]` |
| `elements.proveValidation.primaryCust_LN` | 619 | `str` | `XPATH_(//label[text()='Last Name']/following::input)[1]` |
| `elements.proveValidation.containerBusinessPatriotActNotice` | 620 | `str` | `XPATH_//div[contains(@class, 'PatriotActNotice_container')]` |
| `elements.proveValidation.containerConsumerPatriotActNotice` | 621 | `str` | `XPATH_//div[contains(@class, 'PatriotActNotice_container')]` |
| `elements.proveValidation.citizenship` | 623 | `str` | `Label_Yes` |
| `elements.proveValidation.idType` | 624 | `str` | `Label_ID Type` |
| `elements.proveValidation.idValue` | 625 | `str` | `Option_Driver's License` |
| `elements.proveValidation.idNumber` | 626 | `str` | `Label_ID Number` |
| `elements.proveValidation.stateOfIssue` | 627 | `str` | `Label_State of Issue` |
| `elements.proveValidation.stateOfIssueValue` | 628 | `str` | `Option_PA` |
| `elements.proveValidation.issueDate` | 629 | `str` | `Label_Issue Date` |
| `elements.proveValidation.expDate` | 630 | `str` | `Label_Expiration Date` |
| `elements.proveValidation.yearsOfResidence` | 632 | `str` | `XPATH_//label[text()='Years']/../following-sibling::div/input` |
| `elements.proveValidation.mailingAddress` | 633 | `str` | `Label_Yes` |
| `elements.proveValidation.employmentStatus` | 634 | `str` | `Label_Employment Status` |
| `elements.proveValidation.employmentType` | 635 | `str` | `Option_Full Time` |
| `elements.proveValidation.empName` | 636 | `str` | `Label_Employer Name` |
| `elements.proveValidation.jobTitle` | 637 | `str` | `Label_Job Title` |
| `elements.proveValidation.OcpCategory` | 638 | `str` | `Label_Occupation Category` |
| `elements.proveValidation.OcpType` | 639 | `str` | `Option_Management` |
| `elements.proveValidation.AnnualIncome` | 640 | `str` | `Label_Estimated Annual Income` |
| `elements.proveValidation.yesAddCoApp` | 642 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.proveValidation.yesSelectCoApp` | 643 | `str` | `XPATH_//input[@name='ShoppingCart.Products.Product[1].CoapplicantLo…` |
| `elements.proveValidation.noCoApp` | 645 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.proveValidation.purpose` | 646 | `str` | `Label_What is the purpose of this` |
| `elements.proveValidation.purposeType` | 647 | `str` | `Option_Personal` |
| `elements.proveValidation.source` | 648 | `str` | `Label_What is the source of funds` |
| `elements.proveValidation.sourceType` | 649 | `str` | `Option_Employment Income` |
| `elements.proveValidation.cashDeposit` | 650 | `str` | `XPATH_(//input[@data-analytics-label='Will this account have U.S. c…` |
| `elements.proveValidation.wireTransfer` | 651 | `str` | `XPATH_(//input[@data-analytics-label='Will there be monthly domesti…` |
| `elements.proveValidation.foreignWTransfer` | 652 | `str` | `XPATH_(//input[@data-analytics-label='Will there be monthly foreign…` |
| `elements.proveValidation.automaticCoverage` | 653 | `str` | `XPATH_//label[text()='Would you like to keep the coverage for check…` |
| `elements.proveValidation.signature` | 654 | `str` | `CSS_input[placeholder='Type your signature here']` |
| `elements.proveValidation.noExistingFNBCustomer` | 656 | `str` | `XPATH_(//input[@data-analytics-label='Is the co-applicant on this a…` |
| `elements.proveValidation.mobileNumber` | 658 | `str` | `XPATH_(//label[text()='Mobile Phone']/following::input)[1]` |
| `elements.proveValidation.dateOfBirth` | 659 | `str` | `Placeholder_mm/dd/yyyy` |
| `elements.proveValidation.firstNameCoApp` | 661 | `str` | `XPATH_//label[text()='First Name']/../following-sibling::div/input` |
| `elements.proveValidation.lastNameCoApp` | 662 | `str` | `XPATH_//label[text()='Last Name']/../following-sibling::div/input` |
| `elements.proveValidation.emailCoApp` | 663 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.Street_Address_2` | 664 | `str` | `XPATH_(//label[text()='Street Address 2']/following::input)[1]` |
| `elements.proveValidation.cityCoApp` | 665 | `str` | `XPATH_(//label[text()='City']/following::input)[1]` |
| `elements.proveValidation.stateCoApp` | 666 | `str` | `XPATH_(//label[text()='State']/following::input)[1]` |
| `elements.proveValidation.zipCoApp` | 667 | `str` | `XPATH_(//label[text()='ZIP Code']/following::input)[1]` |
| `elements.proveValidation.amountToFund` | 671 | `str` | `XPATH_(//h2[text()='Funding Amount']/../div/preceding::input)[1]` |
| `elements.proveValidation.amountToFundSecond` | 672 | `str` | `XPATH_(//h2[text()='Funding Amount']/../div/preceding::input)[2]` |
| `elements.proveValidation.amountToFund_emp_stu` | 673 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AccountDetails.…` |
| `elements.proveValidation.txtAmountToFund` | 674 | `str` | `XPATH_//input[contains(@id,'AmountToFund')]` |
| `elements.proveValidation.transferFromBank` | 676 | `str` | `TestId_Transfer from an account with another bank` |
| `elements.proveValidation.continue_btn` | 677 | `str` | `Button_Continue` |
| `elements.proveValidation.continueAsGuest` | 678 | `str` | `XPATH_//span[text()='Continue without phone number']` |
| `elements.proveValidation.searchBank` | 679 | `str` | `CSS_#search-input-input` |
| `elements.proveValidation.TDbank` | 680 | `str` | `XPATH_//h2[text()='TD Bank']` |
| `elements.proveValidation.ContinueLogin` | 681 | `str` | `XAPTH_//span[text()='Continue to login']/..` |
| `elements.proveValidation.SignIn` | 682 | `str` | `BUTTON_Sign in` |
| `elements.proveValidation.Getcode` | 683 | `str` | `BUTTON_Get code` |
| `elements.proveValidation.Submit` | 684 | `str` | `BUTTON_Submit` |
| `elements.proveValidation.bankNameRegions` | 685 | `str` | `Label_Regions Bank` |
| `elements.proveValidation.RegionsbankUserName` | 686 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.RegionsbankPassword` | 687 | `str` | `<redacted>` |
| `elements.proveValidation.submitPersonalAccnt` | 688 | `str` | `XPATH_//button[normalize-space()='Next']` |
| `elements.proveValidation.Continue` | 689 | `str` | `BUTTON_Continue` |
| `elements.proveValidation.cashManagementCheckBox` | 690 | `str` | `XPATH_//div[text()='Plaid Cash Management']/../parent::label/input` |
| `elements.proveValidation.termsAndCondCheckbox` | 691 | `str` | `XPATH_//div[contains(text(),'I have read and accept the Terms and C…` |
| `elements.proveValidation.Connectaccount` | 692 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.FinishWithoutSaving1` | 693 | `str` | `BUTTON_Finish without saving` |
| `elements.proveValidation.paymentRadioButton` | 694 | `str` | `Name_Payment.FundingAccountUsed.Id` |
| `elements.proveValidation.Credit_Debit_Card` | 696 | `str` | `TestId_Credit/Debit Card` |
| `elements.proveValidation.creditORDebit` | 697 | `str` | `XPATH_//button[@value='CREDIT_OR_DEBIT']` |
| `elements.proveValidation.Credit_debit_Frame` | 698 | `str` | `Text_× Card Information Card` |
| `elements.proveValidation.Credit_debit_Frame_B` | 699 | `str` | `Label_Card Number` |
| `elements.proveValidation.Credit_debit_Popup` | 701 | `str` | `Text_Card Number` |
| `elements.proveValidation.Card_NbrField` | 702 | `str` | `Label_Card Number` |
| `elements.proveValidation.Card_Nbr` | 703 | `str` | `Placeholder_5678 9012 3456` |
| `elements.proveValidation.cardNum` | 704 | `str` | `XPATH_//input[@id='cardNum']` |
| `elements.proveValidation.Card_Expiry` | 705 | `str` | `Placeholder_MM/YY` |
| `elements.proveValidation.Card_Code` | 706 | `str` | `Label_Card Code` |
| `elements.proveValidation.Card_FirstName` | 707 | `str` | `Textbox_firstName` |
| `elements.proveValidation.Card_LastName` | 708 | `str` | `Textbox_lastName` |
| `elements.proveValidation.Card_ZipCode` | 709 | `str` | `Textbox_zip` |
| `elements.proveValidation.ProcessCard` | 710 | `str` | `Button_Process Card` |
| `elements.proveValidation.submitButton` | 711 | `str` | `Button_Submit` |
| `elements.proveValidation.btnCCPaymentSubmit` | 712 | `str` | `XPATH_//button[@id='payButton']` |
| `elements.proveValidation.CardContinueBtn` | 713 | `str` | `Button_Continue` |
| `elements.proveValidation.certSSN` | 715 | `str` | `XPATH_(//h3[text()='Certify Social Security Number']/../div/div/lab…` |
| `elements.proveValidation.backupWithholding1` | 716 | `str` | `XPATH_//h3[text()='Backup Withholding']/../div[2]/div/label/span/input` |
| `elements.proveValidation.privacyPolicyLink` | 717 | `str` | `XPATH_//h2[text()='Privacy Policy']/../following-sibling::button/div` |
| `elements.proveValidation.depositAgreementAndFeeScheduleLink` | 718 | `str` | `XPATH_//h2[text()='Deposit Agreement and Fee Schedule']/../followin…` |
| `elements.proveValidation.freestyleCheckingLink` | 719 | `str` | `XPATH_//h2[contains(text(),'Freestyle Checking')]/../following-sibl…` |
| `elements.proveValidation.freestyle_product_2` | 720 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.proveValidation.lifestyleCheckingLink` | 722 | `str` | `XPATH_//h2[contains(text(),'Lifestyle Checking')]/../following-sibl…` |
| `elements.proveValidation.premierstyleCheckingLink` | 723 | `str` | `XPATH_//h2[contains(text(),'Premierstyle Checking')]/../following-s…` |
| `elements.proveValidation.estyleCheckingLink` | 724 | `str` | `XPATH_//h2[contains(text(),'eStyle')]/../following-sibling::button/div` |
| `elements.proveValidation.estyleplusCheckingLink` | 725 | `str` | `XPATH_//h2[contains(text(),'eStyle')]/../following-sibling::button/div` |
| `elements.proveValidation.employeeCheckingLink` | 726 | `str` | `XPATH_//h2[contains(text(),'Employee Checking')]/../following-sibli…` |
| `elements.proveValidation.studentCheckingLink` | 727 | `str` | `XPATH_//h2[contains(text(),'Student Checking')]/../following-siblin…` |
| `elements.proveValidation.cdLink` | 728 | `str` | `XPATH_//h2[contains(text(),'Certificate')]/../following-sibling::bu…` |
| `elements.proveValidation.FirstrateLink` | 729 | `str` | `XPATH_//h2[contains(text(),'FirstRate')]/../following-sibling::butt…` |
| `elements.proveValidation.HealthSavingsLink1` | 730 | `str` | `XPATH_(//h2[contains(text(),'Health Savings')]/../following-sibling…` |
| `elements.proveValidation.HealthSavingsLink2` | 731 | `str` | `XPATH_(//h2[contains(text(),'Health Savings')]/../following-sibling…` |
| `elements.proveValidation.WorkplaceLink` | 732 | `str` | `XPATH_//h2[contains(text(),'Workplace')]/../following-sibling::butt…` |
| `elements.proveValidation.foreignGovtOfficialNo` | 733 | `str` | `XPATH_(//input[@data-analytics-label='Are you currently or have you…` |
| `elements.proveValidation.immediateFamilyMemberNo` | 734 | `str` | `XPATH_(//input[@data-analytics-label='Are you an immediate family m…` |
| `elements.proveValidation.deplomatNo` | 735 | `str` | `XPATH_(//input[@data-analytics-label='Are you a diplomat?'])[2]` |
| `elements.proveValidation.diplomatNo` | 736 | `str` | `XPATH_(//input[@data-analytics-label='Are you a diplomat?'])[2]` |
| `elements.proveValidation.noDebitCardAccount` | 738 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.noDebitCardAccounts` | 739 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.yesDebitCardAccount` | 740 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.yesDebitCardAccounts` | 741 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.noVisaDebitFNameLName` | 742 | `str` | `XPATH_//label[contains(text(),'Would you like to include a Visa® De…` |
| `elements.proveValidation.noVisaDebitPrimary` | 744 | `str` | `XPATH_//input[@id='Customers.Primary.DebitCardRequestedNo']` |
| `elements.proveValidation.noVisaDebitCoApp` | 745 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].DebitC…` |
| `elements.proveValidation.yesVisaDebitCoApp` | 746 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].DebitC…` |
| `elements.proveValidation.orderChecks` | 749 | `str` | `Label_I don't want to order any` |
| `elements.proveValidation.orderChecksYes` | 750 | `str` | `XPATH_//button[contains (text(), 'Order Checks Now')]` |
| `elements.proveValidation.confirmationID` | 752 | `str` | `XPATH_//div[contains(text(),'Confirmation ID:')]` |
| `elements.proveValidation.continueApplication` | 753 | `str` | `XPATH_//button[text()='Continue Application']` |
| `elements.proveValidation.continue2business` | 754 | `str` | `XPATH_//button[text()='Continue Business Accounts']` |
| `elements.proveValidation.consumer_loan_declined` | 755 | `str` | `XPATH_(//ul[@class='AccountsCard_productList__muIWh']//div)[1]` |
| `elements.proveValidation.InsuranceNotice` | 756 | `str` | `XPATH_//h2[text()='Insurance Notice']/../following-sibling::button` |
| `elements.proveValidation.ImportantNoticesDisclosures` | 757 | `str` | `XPATH_//h2[text()='Important Notices & Disclosures']/../following-s…` |
| `elements.proveValidation.ImportantNoticesDisclosures_2` | 758 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.proveValidation.PrivPolicy` | 759 | `str` | `XPATH_//h2[text()='Privacy Policy']/../following-sibling::button` |
| `elements.proveValidation.Penguin_credit_card_view_and_accept` | 760 | `str` | `CSS_button#Customers.Primary.SignedDisclosures.DynamicDisclosures.D…` |
| `elements.proveValidation.FundNowYes` | 762 | `str` | `XPATH_//input[@value='true']` |
| `elements.proveValidation.schoolName` | 763 | `str` | `XPATH_//input[@id='ShoppingCart.Products.Product[0].AccountDetails.…` |
| `elements.proveValidation.selectFundingAccount` | 764 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.healthSavingsYear` | 765 | `str` | `XPATH_//input[@value='2024']` |
| `elements.proveValidation.workplaceCode` | 766 | `str` | `XPATH_//input[@data-analytics-label='Workplace Banking Employer Code']` |
| `elements.proveValidation.specialCDFNBAccountCheck` | 767 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.existingAccountNumber` | 768 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.existingAccountBalance` | 769 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.rdoFundNowYes` | 770 | `str` | `XPATH_//input[@id='Payment.FundNowtrue']` |
| `elements.proveValidation.rdoFundNowNo` | 771 | `str` | `XPATH_//input[@id='Payment.FundNowfalse']` |
| `elements.proveValidation.txtMobileNumber` | 772 | `str` | `CSS_#PhoneNumber` |
| `elements.proveValidation.txtDateOfBirth` | 773 | `str` | `CSS_[name='DateOfBirth']` |
| `elements.proveValidation.paymentSubmit` | 774 | `str` | `XPATH_//button[@name='Process Card']` |
| `elements.proveValidation.paymentButton` | 775 | `str` | `XPATH_//*[@id='payButton']` |
| `elements.proveValidation.iframeCreditCard` | 776 | `str` | `XPATH_//iframe[contains(@src, 'acceptMain.html')]` |
| `elements.proveValidation.txtCardNumber` | 777 | `str` | `CSS_#cardNum` |
| `elements.proveValidation.promoDisclaimer` | 778 | `str` | `XPATH_//h2[text()='Student Checking $100 Promo Offer Disclaimer']/.…` |
| `elements.proveValidation.certifyPromo` | 779 | `str` | `XPATH_//input[@id='Customers.Primary.PromoCertification']` |
| `elements.proveValidation.primaryDebitCard` | 780 | `str` | `XPATH_//input[@name='Customers.Primary.DebitCards.DebitCard[0].Chec…` |
| `elements.proveValidation.secondaryDebitCard` | 781 | `str` | `XPATH_//input[@name='Customers.Primary.DebitCards.DebitCard[0].Chec…` |
| `elements.proveValidation.acceptCrossSell` | 783 | `str` | `XPATH_//button[contains(text(), 'd like to apply for this account')]` |
| `elements.proveValidation.declineCrossSell` | 784 | `str` | `XPATH_//button[contains(text(), 'do not want to apply for this acco…` |
| `elements.proveValidation.saveApplication` | 785 | `str` | `XPATH_//button[contains(text(), 'Save')]` |
| `elements.proveValidation.crossSellAccept` | 786 | `str` | `XPATH_//button[contains(text(), 'see if I qualify')]` |
| `elements.proveValidation.crossSellContinue` | 787 | `str` | `XPATH_//button[@data-testid='continue']` |
| `elements.proveValidation.ExistingCoApp` | 788 | `str` | `XPATH_(//input[@id='ShoppingCart.Products.Product[0].CoapplicantLoc…` |
| `elements.proveValidation.acceptCounterOffer` | 791 | `str` | `XPATH_//button[contains(text(), 'd like to apply for an eStyle Acco…` |
| `elements.proveValidation.debitCardCount2` | 792 | `str` | `XPATH_//input[@value='2']` |
| `elements.proveValidation.btnRegionBank` | 793 | `str` | `XPATH_//button[@aria-label='Regions Bank']` |
| `elements.proveValidation.txtRegionBankUserId` | 794 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.txtRegionBankPassword` | 795 | `str` | `<redacted>` |
| `elements.proveValidation.btnRegionBankSubmit` | 796 | `str` | `XPATH_//button[contains(@id, 'button') and .//span[text()='Submit']]` |
| `elements.proveValidation.btnRegionBankContinue` | 797 | `str` | `XPATH_//button[contains(@id, 'button') and .//span[text()='Continue']]` |
| `elements.proveValidation.btnRegionBankFinishWithoutSaving` | 798 | `str` | `XPATH_//button[contains(@id, 'button') and .//span[text()='Finish w…` |
| `elements.proveValidation.rdoSelectActiveAccount` | 799 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.ssnExisting` | 801 | `str` | `XPATH_//input[@id='Customers.Primary.Tin']` |
| `elements.proveValidation.phoneExisting` | 802 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.accountExisting` | 803 | `str` | `<environment- or user-supplied value>` |
| `elements.proveValidation.balanceExisting` | 804 | `str` | `XPATH_//input[@id='Customers.Primary.FnbAccountBalance']` |
| `elements.proveValidation.secondDepositCoApp` | 806 | `str` | `XPATH_(//input[@name='ShoppingCart.Products.Product[0].CoapplicantL…` |
| `elements.proveValidation.popupSave` | 808 | `str` | `XPATH_//div[@class='Modal_buttonSection__sFKRj']//button[text()='Sa…` |
| `elements.proveValidation.resumePin` | 809 | `str` | `CSS_#Pin` |
| `elements.proveValidation.resume` | 810 | `str` | `XPATH_//button[@type='submit']` |
| `elements.Acknowledgement.body` | 813 | `str` | `CSS_body` |
| `elements.Acknowledgement.certSSN` | 815 | `str` | `XPATH_(//h3[text()='Certify Social Security Number']/../div/div/lab…` |
| `elements.Acknowledgement.consumerCertSSN` | 816 | `str` | `XPATH_(//h3[text()='Certify Social Security Number']/../div/div/lab…` |
| `elements.Acknowledgement.backupWithholding` | 817 | `str` | `XPATH_//h3[text()='Backup Withholding']/../div/div/label/span/input` |
| `elements.Acknowledgement.backupWithholding1` | 818 | `str` | `XPATH_//h3[text()='Backup Withholding']/../div[2]/div/label/span/input` |
| `elements.Acknowledgement.ConsumerBackupWithholding` | 819 | `str` | `XPATH_(//h3[text()='Backup Withholding']/../div/div/label/span/inpu…` |
| `elements.Acknowledgement.chkCertifySSN` | 820 | `str` | `XPATH_//input[@name='CertifySSN']` |
| `elements.Acknowledgement.chkBackupWithholding` | 821 | `str` | `XPATH_//input[@name='BackupWithholding']` |
| `elements.Acknowledgement.PrivacyPolicy` | 822 | `str` | `XPATH_//h2[normalize-space()='Privacy Policy']` |
| `elements.Acknowledgement.viewAndAccept1` | 823 | `str` | `XPATH_(//div[text()='View and Accept'])[1]` |
| `elements.Acknowledgement.canvas` | 824 | `str` | `XPATH_(//canvas)[1]` |
| `elements.Acknowledgement.i_accept_btn` | 825 | `str` | `Button_I Accept` |
| `elements.Acknowledgement.I_accept` | 826 | `str` | `CSS_accept-button` |
| `elements.Acknowledgement.depositAgreement` | 827 | `str` | `XPATH_//h2[text()='Deposit Agreement and Fee Schedule']` |
| `elements.Acknowledgement.depositAgreementAndFeeScheduleLink` | 828 | `str` | `XPATH_//h2[text()='Deposit Agreement and Fee Schedule']/../followin…` |
| `elements.Acknowledgement.unincorporatedAgreementLink` | 829 | `str` | `XPATH_//h2[text()='FNB Unincorporated Association Resolution']/../f…` |
| `elements.Acknowledgement.freestyleCheckingLink` | 830 | `str` | `XPATH_//h2[contains(text(),'Freestyle Checking')]/../following-sibl…` |
| `elements.Acknowledgement.lifestyleCheckingLink` | 831 | `str` | `XPATH_//h2[contains(text(),'Lifestyle Checking')]/../following-sibl…` |
| `elements.Acknowledgement.premierstyleCheckingLink` | 832 | `str` | `XPATH_//h2[contains(text(),'Premierstyle Checking')]/../following-s…` |
| `elements.Acknowledgement.estyleLink` | 833 | `str` | `XPATH_//h2[contains(text(),'eStyle')]/../following-sibling::button/div` |
| `elements.Acknowledgement.estyleplusLink` | 834 | `str` | `XPATH_//h2[contains(text(),'eStyle Plus')]/../following-sibling::bu…` |
| `elements.Acknowledgement.firstratemoneymarketLink` | 835 | `str` | `XPATH_//h2[contains(text(),'FirstRate Money Market')]/../following-…` |
| `elements.Acknowledgement.studentCheckingLink` | 836 | `str` | `XPATH_//h2[contains(text(),'Student Checking')]/../following-siblin…` |
| `elements.Acknowledgement.employeeCheckingLink` | 837 | `str` | `XPATH_//h2[contains(text(),'Employee Checking')]/../following-sibli…` |
| `elements.Acknowledgement.PenguinsCashBackAgreement` | 838 | `str` | `XPATH_//h2[contains(text(),'Penguins® Cash Back Credit Card')]/../f…` |
| `elements.Acknowledgement.viewAndAcpt2` | 839 | `str` | `XPATH_//h2[text()='Deposit Agreement and Fee Schedule']/../followin…` |
| `elements.Acknowledgement.viewAndAcpt3` | 840 | `str` | `XPATH_//h2[text()='FNB Corporate Resolution']/../following-sibling:…` |
| `elements.Acknowledgement.signTextBox` | 841 | `str` | `XPATH_//input[contains(@name,'Signature')]` |
| `elements.Acknowledgement.foreignGovtOfficialNo` | 842 | `str` | `XPATH_(//input[@data-analytics-label='Are you currently or have you…` |
| `elements.Acknowledgement.immediateFamilyMemberNo` | 843 | `str` | `XPATH_(//input[@data-analytics-label='Are you an immediate family m…` |
| `elements.Acknowledgement.deplomatNo` | 844 | `str` | `XPATH_(//input[@data-analytics-label='Are you a diplomat?'])[2]` |
| `elements.Acknowledgement.NextButton` | 845 | `str` | `XPATH_//button[text()='Next']` |
| `elements.Acknowledgement.fnbPartRes` | 846 | `str` | `XPATH_//h2[text()='FNB Partnership Resolution']` |
| `elements.Acknowledgement.fnbCorpRes` | 847 | `str` | `XPATH_//h2[text()='FNB Corporate Resolution']` |
| `elements.Acknowledgement.fnbSoleProp` | 848 | `str` | `XPATH_//h2[text()='FNB Sole Proprietorship Resolution']/../followin…` |
| `elements.Acknowledgement.viewAndAcpt4` | 849 | `str` | `XPATH_//h2[text()='FNB Partnership Resolution']/../following-siblin…` |
| `elements.Acknowledgement.viewAndAcptPromo` | 850 | `str` | `XPATH_(//div[contains(text(),'View and Accept')])` |
| `elements.Acknowledgement.viewAndAccept_LLCResolution` | 851 | `str` | `XPATH_//h2[text()='FNB LLC Authorization Resolution']/../following-…` |
| `elements.Acknowledgement.insuranceNotice` | 853 | `str` | `XPATH_//h2[text()='Insurance Notice']/../following-sibling::button/div` |
| `elements.Acknowledgement.insuranceNoticeAndDisclosures` | 854 | `str` | `XPATH_//h2[text()='Important Notices & Disclosures']/../following-s…` |
| `elements.Acknowledgement.Submit` | 855 | `str` | `BUTTON_Submit` |
| `elements.Acknowledgement.eStyleTruthSavings` | 856 | `str` | `XPATH_//h2[text()='eStyle Account Truth in Savings']/../following-s…` |
| `elements.Acknowledgement.CDTruthSavings` | 857 | `str` | `XPATH_//h2[text()='Business Certificate of Deposit Truth in Savings…` |
| `elements.Acknowledgement.SpecialCDTruthSavings` | 858 | `str` | `XPATH_//h2[text()='Business Special Certificate of Deposit Truth in…` |
| `elements.Acknowledgement.viewAndAccept_LifestyleChecking` | 859 | `str` | `XPATH_//h2[text()='Lifestyle Checking Truth in Savings']/../followi…` |
| `elements.Acknowledgement.viewAndAccept_PremierstyleChecking` | 860 | `str` | `XPATH_//h2[text()='Premierstyle Checking Truth //button[@id='Custom…` |
| `elements.Acknowledgement.viewAndAccept_FNB_SmartCashSMCreditCard` | 861 | `str` | `XPATH_(//div[@class='AcknowledgementsPage_disclosureSection__kDvVX'…` |
| `elements.Acknowledgement.viewAndAccept_eStyle` | 862 | `str` | `XPATH_//h2[text()='eStyle Account Truth in Savings']/../following-s…` |
| `elements.Acknowledgement.viewAndAccept_eStylePlus` | 863 | `str` | `XPATH_//h2[text()='eStylePlus Account Truth in Savings']/../followi…` |
| `elements.Acknowledgement.signature` | 864 | `str` | `CSS_input[placeholder='Type your signature here']` |
| `elements.Acknowledgement.btnMerchantServiceCredit500Promo` | 865 | `str` | `XPATH_//h2[contains(text(),'Earn a Merchant Service')]/../following…` |
| `elements.Acknowledgement.chkCertifyPromotionalOffer` | 866 | `str` | `XPATH_//input[@id='PromoCertification']` |
| `elements.Acknowledgement.FNBBUSINESSWINPromo` | 867 | `str` | `XPATh_//h2[contains(text(),'Earn a Merchant Services credit of up t…` |
| `elements.Acknowledgement.btnViewAndAccept_FreestyleChecking` | 868 | `str` | `XPATH_//h2[contains(text(), 'Freestyle Checking')]/parent::div/foll…` |
| `elements.Acknowledgement.btnViewAndAccept_LifestyleChecking` | 869 | `str` | `XPATH_//h2[contains(text(), 'Lifestyle Checking')]/parent::div/foll…` |
| `elements.Acknowledgement.btnViewAndAccept_PremierstyleChecking` | 871 | `str` | `XPATH_//h2[contains(text(), 'Premierstyle Checking')]/parent::div/f…` |
| `elements.Acknowledgement.btnViewAndAccept_ConsumerPromo_SAV2002504` | 872 | `str` | `XPATH_//h2[contains(text(), 'Earn $200')]/parent::div/following-sib…` |
| `elements.Acknowledgement.btnViewAndAccept_estylePlusChecking` | 873 | `str` | `XPATH_//h2[contains(text(), 'eStyle Plus')]/parent::div/following-s…` |
| `elements.Acknowledgement.btnViewAndAccept_firstRateMoneyMarket` | 874 | `str` | `XPATH_//h2[contains(text(), 'FirstRate Money Market')]/parent::div/…` |
| `elements.Acknowledgement.btnViewAndAccept_HealthSavingIndividual` | 875 | `str` | `XPATH_//h2[contains(text(), 'Health Savings - Individual')]/parent:…` |
| `elements.Acknowledgement.btnViewAndAccept_HealthSavingFamily` | 876 | `str` | `XPATH_//h2[contains(text(), 'Health Savings - Family')]/parent::div…` |
| `elements.Acknowledgement.btnViewAndAccept_workSpaceBankingSolution` | 877 | `str` | `XPATH_//h2[contains(text(), 'Workplace Banking Solutions')]/parent:…` |
| `elements.Acknowledgement.btnViewAndAccept_FNBUStudentChecking` | 878 | `str` | `XPATH_//h2[contains(text(), 'FNB-U Student Checking')]/parent::div/…` |
| `elements.Acknowledgement.btnViewAndAccept_FNBEmployeeChecking` | 879 | `str` | `XPATH_//h2[contains(text(), 'FNB Employee Checking')]/parent::div/f…` |
| `elements.Acknowledgement.btnViewAndAccept_HSAIndividual` | 880 | `str` | `XPATH_//h2[contains(text(), 'Health Savings Employee - Individual')…` |
| `elements.Acknowledgement.btnViewAndAccept_HSAFamily` | 881 | `str` | `XPATH_//h2[contains(text(), 'Health Savings Employee - Family')]/pa…` |
| `elements.Acknowledgement.btnViewAndAccept_FirstRateMMS` | 882 | `str` | `XPATH_//h2[contains(text(), 'FirstRate Money Market Special')]/pare…` |
| `elements.Acknowledgement.rdoCertifyPromoOffer` | 884 | `str` | `XPATH_//input[contains(@name, 'PromoCertification')]` |
| `elements.Acknowledgement.btnViewAndAccept_CDProduct` | 885 | `str` | `XPATH_//h2[contains(text(),'Certificate') or contains(text(),'Flexi…` |
| `elements.Acknowledgement.privacyPolicyLink` | 886 | `str` | `XPATH_//h2[contains(text(),'Privacy')]/parent::div/following-siblin…` |
| `elements.Acknowledgement.certificate_of_beneficial_owner` | 890 | `str` | `XPATH_//h2[contains(text(), 'Beneficial')]/parent::div/following-si…` |
| `elements.Acknowledgement.business_credit_card` | 891 | `str` | `XPATH_//h2[contains(text(),'Business Credit Card')]/parent::div/fol…` |
| `elements.Acknowledgement.penguinsCashBackLink` | 893 | `str` | `XPATH_//h2[contains(text(),'Penguins')]/parent::div/following-sibli…` |
| `elements.Acknowledgement.cdLink` | 897 | `str` | `XPATH_//h2[contains(text(),'Certificate')]/../following-sibling::bu…` |
| `elements.Acknowledgement.FirstrateLink` | 898 | `str` | `XPATH_//h2[contains(text(),'FirstRate')]/../following-sibling::butt…` |
| `elements.Acknowledgement.HealthSavingsLink1` | 899 | `str` | `XPATH_(//h2[contains(text(),'Health Savings')]/../following-sibling…` |
| `elements.Acknowledgement.HealthSavingsLink2` | 900 | `str` | `XPATH_(//h2[contains(text(),'Health Savings')]/../following-sibling…` |
| `elements.Acknowledgement.WorkplaceLink` | 901 | `str` | `XPATH_//h2[contains(text(),'Workplace')]/../following-sibling::butt…` |
| `elements.Acknowledgement.estyleCheckingLink` | 902 | `str` | `XPATH_//h2[contains(text(),'eStyle')]/../following-sibling::button/div` |
| `elements.Acknowledgement.estyleplusCheckingLink` | 903 | `str` | `XPATH_//h2[contains(text(),'eStyle')]/../following-sibling::button/div` |
| `elements.Acknowledgement.backupWithholding2` | 904 | `str` | `XPATH_//input[@id='Customers.Primary.WithholdingInformation.BackupW…` |
| `elements.Acknowledgement.certSSN2` | 905 | `str` | `XPATH_//input[@id='Customers.Primary.WithholdingInformation.Certify…` |
| `elements.debitCardOrderCheck.body` | 908 | `str` | `CSS_body` |
| `elements.debitCardOrderCheck.noDebitCardAccount` | 910 | `str` | `<environment- or user-supplied value>` |
| `elements.debitCardOrderCheck.noVisaDebitFNameLName` | 911 | `str` | `XPATH_//label[contains(text(),'Would you like to include a Visa® De…` |
| `elements.debitCardOrderCheck.WouldYouLikeDebitCardYes` | 912 | `str` | `XPATH_//label[@for='Customers.Primary.DebitCardRequestedYes']` |
| `elements.debitCardOrderCheck.rdoWouldYouLikeDebitCardNo` | 913 | `str` | `XPATH_//input[contains(@data-analytics-label, 'Visa') and @value='No']` |
| `elements.debitCardOrderCheck.rdoWouldYouLikeDebitCardYes` | 914 | `str` | `XPATH_//input[contains(@data-analytics-label, 'Visa') and @value='Y…` |
| `elements.debitCardOrderCheck.rdoWouldYouLikeDebitCardYesPrimary` | 915 | `str` | `XPATH_//input[contains(@data-analytics-label, 'Visa') and @value='Y…` |
| `elements.debitCardOrderCheck.rdoWouldYouLikeDebitCardNoPrimary` | 916 | `str` | `XPATH_//input[contains(@data-analytics-label, 'Visa') and @value='N…` |
| `elements.debitCardOrderCheck.rdoWouldYouLikeDebitCardYesCoapp` | 918 | `str` | `XPATH_//input[contains(@data-analytics-label, 'Visa') and @value='Y…` |
| `elements.debitCardOrderCheck.rdoWouldYouLikeDebitCardNoCoapp` | 919 | `str` | `XPATH_//input[contains(@data-analytics-label, 'Visa') and @value='N…` |
| `elements.debitCardOrderCheck.orderChecks` | 921 | `str` | `XPATH_//input[@id='Customers.Primary.SkipCheckOrdering']` |
| `elements.debitCardOrderCheck.NextButton` | 922 | `str` | `XPATH_//button[text()='Next' or text()='Submit']` |
| `elements.debitCardOrderCheck.SubmitButton` | 923 | `str` | `XPATH_//button[text()='Submit']` |
| `elements.debitCardOrderCheck.Submit` | 924 | `str` | `XPATH_//button[text()='Submit']` |
| `elements.debitCardOrderCheck.noDebitCardAccounts` | 928 | `str` | `<environment- or user-supplied value>` |
| `elements.debitCardOrderCheck.yesDebitCardAccount` | 929 | `str` | `<environment- or user-supplied value>` |
| `elements.debitCardOrderCheck.yesDebitCardAccounts` | 930 | `str` | `<environment- or user-supplied value>` |
| `elements.debitCardOrderCheck.noVisaDebitPrimary` | 933 | `str` | `XPATH_//input[@id='Customers.Primary.DebitCardRequestedNo']` |
| `elements.debitCardOrderCheck.noVisaDebitCoApp` | 934 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].DebitC…` |
| `elements.debitCardOrderCheck.yesVisaDebitCoApp` | 935 | `str` | `XPATH_//input[@id='Customers.AdditionalCustomers.Customer[0].DebitC…` |
| `elements.debitCardOrderCheck.orderChecksYes` | 938 | `str` | `XPATH_//button[contains (text(), 'Order Checks Now')]` |
| `elements.debitCardOrderCheck.debitCardCount2` | 939 | `str` | `XPATH_//input[@value='2']` |
| `elements.debitCardOrderCheck.orderChecksYesFirst` | 940 | `str` | `XPATH_(//button[contains (text(), 'Order Checks Now')])[1]` |
| `elements.debitCardOrderCheck.orderChecksYesSecond` | 941 | `str` | `XPATH_(//button[contains (text(), 'Order Checks Now')])[2]` |
| `elements.dashBoardPage.body` | 944 | `str` | `CSS_body` |
| `elements.dashBoardPage.ContinueApplication` | 945 | `str` | `XPATH_//span[normalize-space()='Start Application']` |
| `elements.dashBoardPage.ContinueApplication1` | 946 | `str` | `XPATH_(//span[text()='Start Application'])[1]` |
| `elements.dashBoardPage.ContinueApplication2` | 947 | `str` | `XPATH_(//span[text()='Start Application'])[2]` |
| `elements.dashBoardPage.ContinueApplication3` | 948 | `str` | `XPATH_(//span[text()='Start Application'])[3]` |
| `elements.dashBoardPage.Consumer_loan_Continue_btn` | 949 | `str` | `CSS_button[type='button']` |
| `elements.dashBoardPage.consumer_loan_consumer_deposit_business_deposit_continue_btn` | 950 | `str` | `CSS_button.styledButton_button__YqZjv.PrimaryButton_button__M0k3O` |
| `elements.dashBoardPage.CL_CD_BL_BD_Continue_btn` | 951 | `str` | `XPATH_//button[normalize-space()='Continue Business Accounts']` |
| `elements.dashBoardPage.DeclinedMsg` | 952 | `str` | `XPATH_//span[text()='DECLINED']` |
| `elements.dashBoardPage.confirmationID` | 953 | `str` | `XPATH_//div[contains(text(),'Confirmation ID:')]` |
| `elements.dashBoardPage.highRiskHardStop` | 954 | `str` | `XPATH_//h2[text()='We regret we are unable to continue opening your…` |
| `elements.dashBoardPage.HardStopMessage` | 956 | `str` | `XPATH_//h2[contains(text(),'We regret we are unable to continue ope…` |
| `elements.dashBoardPage.HardstopMesseage_after_coApp_form_new` | 958 | `str` | `XPATH_//h2[contains(text(),'We regret we are unable to continue ope…` |
| `elements.dashBoardPage.HardStopBankMessage` | 960 | `str` | `XPATH_//p[contains(text(),'We would be happy to assist you with an …` |
| `elements.dashBoardPage.PenguinsProductDenied` | 961 | `str` | `XPATH_//h2[text()='Penguins® Cash Back Credit Card']/../span[text()…` |
| `elements.dashBoardPage.eStyleProductDenied` | 962 | `str` | `XPATH_//h2[text()='eStyle']/../span[text()='DECLINED']` |
| `elements.dashBoardPage.FirstCheckingProductDenied` | 963 | `str` | `XPATH_//h2[text()='Business First Checking']/../span[text()='DECLIN…` |
| `elements.dashBoardPage.freeStyleProductDenied` | 964 | `str` | `XPATH_//h2[text()='Freestyle Checking']/../span[text()='DECLINED']` |
| `elements.dashBoardPage.sendPinNumber` | 965 | `str` | `Button_Send PIN Number` |
| `elements.dashBoardPage.pinTextBox` | 966 | `str` | `Name_Pin` |
| `elements.dashBoardPage.resumeButton` | 967 | `str` | `Button_Resume` |
| `elements.dashBoardPage.trackingCode` | 968 | `str` | `XPATH_//div[@class='TD1jCJeWvCwbvvVyVyCX']` |
| `elements.dashBoardPage.trackingCode1` | 969 | `str` | `XPATH_//div[contains(text(), 'Tracking')]` |
| `elements.dashBoardPage.dashboardCode` | 970 | `str` | `XPATH_//div[contains(text(),'Tracking Code:')]` |
| `elements.dashBoardPage.downloadReceipt` | 971 | `str` | `XPATH_//button[contains(text(),'Download')]` |
| `elements.dashBoardPage.accountOpened` | 972 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.consumerLoanStatus` | 973 | `str` | `XPATH_//span[normalize-space()='IN REVIEW']` |
| `elements.dashBoardPage.businessAppStatus` | 974 | `str` | `XPATH_//span[contains(text(),'PENDING')]` |
| `elements.dashBoardPage.routingNumber` | 975 | `str` | `XPATH_//div[contains(text(), '043318092')]` |
| `elements.dashBoardPage.atomicGetStarted` | 976 | `str` | `XPATH_//button[@role='launchAtomic']//div[contains(text(),'Get Star…` |
| `elements.dashBoardPage.paymentGetStarted` | 977 | `str` | `XPATH_//div[@class='PaymentSwitchCard_actionItem__beAJq']//div[@cla…` |
| `elements.dashBoardPage.PaymentSwitchGetStarted` | 978 | `str` | `XPATH_//button[@data-testid='launchPaymentSwitch']` |
| `elements.dashBoardPage.launchAtomic` | 979 | `str` | `XPATH_//button[@role='launchAtomic']` |
| `elements.dashBoardPage.continue_btn` | 980 | `str` | `LinkText_Continue` |
| `elements.dashBoardPage.launchMobileBankingModal` | 981 | `str` | `XPATH_//button[@role='launchMobileBanking']//div[contains(text(),'G…` |
| `elements.dashBoardPage.iosAppText` | 982 | `str` | `XPATH_(//p[contains(text(), 'Text Me')])[1]` |
| `elements.dashBoardPage.androidAppText` | 983 | `str` | `XPATH_(//p[contains(text(), 'Text Me')])[2]` |
| `elements.dashBoardPage.closeMobileModal` | 984 | `str` | `XPATH_//button[contains(text(), 'Close')]` |
| `elements.dashBoardPage.mobileActionComplete` | 985 | `str` | `XPATH_//span[contains(@class, 'MobileBankingNextStepCard_checkBubbl…` |
| `elements.dashBoardPage.paymentActionComplete` | 986 | `str` | `XPATH_//span[contains(@class, 'PaymentSwitchCard_checkBubble')]` |
| `elements.dashBoardPage.ddsComplete` | 988 | `str` | `XPATH_//span[contains(text(), 'COMPLETED')]` |
| `elements.dashBoardPage.getDdsStatus` | 989 | `str` | `XPATH_//span[@class='Pr_rTGf1EYMHzvDohlfS AtomicNextStepCard_bubble…` |
| `elements.dashBoardPage.resumeApplicationPin` | 990 | `str` | `XPATH_//input[@id='Pin']` |
| `elements.dashBoardPage.resumeSubmitPin` | 991 | `str` | `XPATH_//button[@type='submit']` |
| `elements.dashBoardPage.headingHappyPath` | 992 | `str` | `XPATH_//h1[@class='MK47dnkl5PxyugDxikDQ']` |
| `elements.dashBoardPage.subheadingHappyPath` | 994 | `str` | `XPATH_(//main//h2)[1]` |
| `elements.dashBoardPage.sectionHeadingHappyPath` | 995 | `str` | `XPATH_//h3[contains(text(), 'Accounts')]` |
| `elements.dashBoardPage.sectionHeadingDashboardConsumer` | 996 | `str` | `XPATH_//h3[contains(@class, 'typography_sectionHeading')]` |
| `elements.dashBoardPage.summaryHappyPath` | 997 | `str` | `XPATH_//p[contains(text(), 'Congratulations')]` |
| `elements.dashBoardPage.summaryDashboardConsumerLoanPart1` | 998 | `str` | `XPATH_//header//p[1]` |
| `elements.dashBoardPage.summaryDashboardConsumerLoanPart2` | 999 | `str` | `XPATH_//header//p[2]` |
| `elements.dashBoardPage.getAccountNumber` | 1000 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.ContinueBusiness_btn` | 1001 | `str` | `BUTTON_Continue Business Accounts` |
| `elements.dashBoardPage.dashboardCoApp` | 1002 | `str` | `XPATH_//h3[contains (text(), 'Co-Applicant')]` |
| `elements.dashBoardPage.dashboardDebit` | 1003 | `str` | `XPATH_//div[contains (text(), 'Debit Card')]` |
| `elements.dashBoardPage.dashboardChecks` | 1004 | `str` | `XPATH_//div[contains (text(), 'Checks')]` |
| `elements.dashBoardPage.btnContinueBusinessAccounts` | 1005 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.lblCTrackingCode` | 1006 | `str` | `XPATH_//div[contains(text(),'Confirmation ID')]` |
| `elements.dashBoardPage.lblCDashboardTrackingCode` | 1007 | `str` | `XPATH_//div[contains(text(),'Tracking Code')]` |
| `elements.dashBoardPage.txtPinNumber` | 1008 | `str` | `XPATH_//input[@id='Pin']` |
| `elements.dashBoardPage.onlineBankingLink` | 1009 | `str` | `XPATH_//a[contains (text(), 'visit Online Banking')]` |
| `elements.dashBoardPage.Consumer_SummaryReceipt` | 1010 | `str` | `XPATH_//button[text()='Download Your Personal Account Summary']` |
| `elements.dashBoardPage.sectionProductList` | 1011 | `str` | `XPATH_//ul[contains(@class, 'AccountsCard_productList')]` |
| `elements.dashBoardPage.lblProductName` | 1012 | `str` | `XPATH_//ul[contains(@class, 'AccountsCard_productList')]//li//h2` |
| `elements.dashBoardPage.lblAccountNumberValue` | 1013 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.lblRoutingNumberValue` | 1014 | `str` | `XPATH_//h3[text()='Routing Number']/ancestor::div[contains(@class, …` |
| `elements.dashBoardPage.lblAccountStatus` | 1015 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.lblFundingAmount` | 1016 | `str` | `XPATH_//h3[text()='Funding Amount']/ancestor::div[contains(@class, …` |
| `elements.dashBoardPage.continue_my_application_split_decision` | 1017 | `str` | `XPATH_//button[normalize-space()='Continue My Application']` |
| `elements.dashBoardPage.cancel_my_application_split_decision` | 1018 | `str` | `XPATH_//div[@class='qPTGC7AoJLD5nXTYRmQ0']` |
| `elements.dashBoardPage.lblProductName1` | 1019 | `str` | `XPATH_(//ul[contains(@class, 'AccountsCard_productList')]//li//h2)[1]` |
| `elements.dashBoardPage.lblProductName2` | 1020 | `str` | `XPATH_(//ul[contains(@class, 'AccountsCard_productList')]//li//h2)[2]` |
| `elements.dashBoardPage.lblAccountStatus1` | 1021 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.lblAccountStatus2` | 1022 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.lblAccountStatusCoApp` | 1023 | `str` | `<environment- or user-supplied value>` |
| `elements.dashBoardPage.hardstopHeading` | 1024 | `str` | `XPATH_//div[contains(@class, 'GenericPageBanner_contentWrapper')]//h2` |
| `elements.dashBoardPage.hardstopSubHeading` | 1025 | `str` | `XPATH_//h1[contains(@class, 'HardStopPage_pageTitle')]` |
| `elements.dashBoardPage.declinationLetter` | 1026 | `str` | `XPATH_//div[@data-testid='declination-letter']` |
| `elements.dashBoardPage.HardstopMesseage_after_coApp_form` | 1027 | `str` | `XPATH_//h3[contains(text(),'We regret we are unable to include')]` |
| `elements.dashBoardPage.hardStopBusiness_afterCoApp` | 1028 | `str` | `XPATH_//h2[contains(text(),'We regret we are unable to continue ope…` |
| `elements.dashBoardPage.coapp_name_on_dashboard` | 1029 | `str` | `XPATH_//h2[normalize-space()='Freestyle Checking (with ConsumerDepo…` |
| `elements.atomicModal.body` | 1032 | `str` | `CSS_body` |
| `elements.atomicModal.click` | 1033 | `str` | `XPATH_//p[contains(text(), 'Name')]` |
| `elements.atomicModal.continue_btn` | 1034 | `str` | `LinkText_Continue` |
| `elements.atomicModal.start` | 1035 | `str` | `XPATH_//span[contains(text(), 'Continue')]` |
| `elements.atomicModal.selectEmployer` | 1036 | `str` | `<environment- or user-supplied value>` |
| `elements.atomicModal.selectHomeDepot` | 1037 | `str` | `XPATH_//span[contains(text(), 'The Home Depot')]` |
| `elements.atomicModal.username` | 1038 | `str` | `<environment- or user-supplied value>` |
| `elements.atomicModal.loginContinue` | 1039 | `str` | `XPATH_//span[contains(text(), 'Continue')]` |
| `elements.atomicModal.password` | 1040 | `str` | `<redacted>` |
| `elements.atomicModal.submitDDS` | 1041 | `str` | `XPATH_//span[contains(text(), 'Confirm')]` |
| `elements.atomicModal.closeAtomicModal` | 1042 | `str` | `XPATH_//span[contains(text(), 'Go Back')]` |
| `elements.atomicModal.freestyleAccount` | 1043 | `str` | `<environment- or user-supplied value>` |
| `elements.atomicModal.lifestyleAccount` | 1044 | `str` | `<environment- or user-supplied value>` |
| `elements.atomicModal.selectExistingforDDS` | 1045 | `str` | `XPATH_//span[contains(text(), '••6891')]` |

## `src/test/resources/elements/google.yml`

Source: [`src/test/resources/elements/google.yml`](../../../src/test/resources/elements/google.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.google_page.body` | 3 | `str` | `CSS_body` |
| `elements.google_page.search_flt` | 4 | `str` | `CSS_#APjFqb` |
| `elements.google_page.google_search_btn` | 5 | `str` | `CSS_(//input[@name='btnK'])[2]` |
| `elements.google_page.footer_text` | 6 | `str` | `CSS_//span[normalize-space(text())='Applying AI towards science and…` |

## `src/test/resources/elements/homepage.yml`

Source: [`src/test/resources/elements/homepage.yml`](../../../src/test/resources/elements/homepage.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.homePage.body` | 3 | `str` | `tag_body` |
| `elements.homePage.logout_btn` | 4 | `str` | `LinkText_Log Out` |
| `elements.homePage.alert_frame_and_window` | 5 | `str` | `CSS_//h5[normalize-space(text())='Alerts, Frame & Windows']` |
| `elements.homePage.browser_window` | 6 | `str` | `CSS_//li[contains(.,'Browser Windows')]` |
| `elements.homePage.new_tab_btn` | 7 | `str` | `id_tabButton` |
| `elements.homePage.tab_semple_heading` | 8 | `str` | `id_sampleHeading` |
| `elements.homePage.frame_semple_heading` | 9 | `str` | `id_sampleHeading` |
| `elements.homePage.frame_btn` | 10 | `str` | `css_//span[normalize-space(text())='Frames']` |
| `elements.homePage.elements_tab` | 11 | `str` | `css_//h5[normalize-space(text())='Elements']` |
| `elements.homePage.update_and_download_tab` | 12 | `str` | `xpath_//span[normalize-space(text())='Upload and Download']` |
| `elements.homePage.download_btn` | 13 | `str` | `Button_Download` |
| `elements.homePage.upload_btn` | 14 | `str` | `id_uploadFile` |
| `elements.homePage.nested_frame_tab` | 15 | `str` | `xpath_(//li[@id='item-3'])[2]` |
| `elements.homePage.first_frame_txt` | 16 | `str` | `Text_Parent frame` |
| `elements.homePage.second_frame_txt` | 17 | `str` | `Text_Child Iframe` |
| `elements.homePage.header_image` | 18 | `str` | `css_(//div[@id='root']//img)[1]` |

## `src/test/resources/elements/landingpage.yml`

Source: [`src/test/resources/elements/landingpage.yml`](../../../src/test/resources/elements/landingpage.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.landing.personal_tab` | 3 | `str` | `CSS_a[title='Personal']` |
| `elements.landing.business_tab` | 4 | `str` | `CSS_a[title='Business']` |
| `elements.landing.eStore_tab` | 5 | `str` | `CSS_a[title='eStore®']` |
| `elements.landing.fnb_logo` | 6 | `str` | `CSS_img[title='First National Bank']` |
| `elements.landing.search_logo` | 7 | `str` | `CSS_img[alt='Search']` |
| `elements.landing.login_btn` | 8 | `str` | `CSS_.login-or-x` |
| `elements.landing.x_btn` | 9 | `str` | `CSS_.login-or-x` |
| `elements.landing.other_services` | 10 | `str` | `Other Services` |
| `elements.landing.personal_bank_login_frame_tab` | 11 | `str` | `Personal Banking` |
| `elements.landing.sign_up_hyperlink` | 12 | `str` | `Sign Up` |
| `elements.landing.ATMBranchIcon` | 13 | `str` | `LinkText_Locate Branch or ATM` |
| `elements.landing.ATMBranchLoc` | 14 | `str` | `Label_ATM & Branch Locator` |
| `elements.landing.SearchButton` | 15 | `str` | `Button_Search Search` |
| `elements.landing.SearchBox` | 16 | `str` | `Placeholder_How can we help you?` |
| `elements.landing.submitButton` | 17 | `str` | `Button_Submit` |
| `elements.landing.loginButton` | 18 | `str` | `Button_Login` |
| `elements.landing.loginBox` | 19 | `str` | `Text_Login Personal Banking Other Services Personal Mobile Login On…` |
| `elements.landing.GetStarted` | 20 | `str` | `LinkText_Get Started` |
| `elements.landing.GetStartedPage` | 21 | `str` | `Heading_Explore Products That Can Be` |
| `elements.landing.SeeItNow` | 22 | `str` | `LinkText_See it Now` |
| `elements.landing.GoToPersonal` | 23 | `str` | `LinkText_Go to Personal` |
| `elements.landing.GoToBusiness` | 24 | `str` | `LinkText_Go to Business` |
| `elements.landing.ATMBranchLocator` | 25 | `str` | `LinkText_Locator Icon Find a Branch/ATM` |
| `elements.landing.MobileAppsOption` | 26 | `str` | `LinkText_Mobile Phone with Apps` |
| `elements.landing.MobileAppsPage` | 27 | `str` | `Heading_Get Our Mobile Apps` |
| `elements.landing.ContactUs` | 28 | `str` | `LinkText_Contact Us` |
| `elements.landing.ContactUsPage` | 29 | `str` | `Label_Contact Us` |
| `elements.landing.Investor` | 30 | `str` | `LinkText_Investors` |
| `elements.landing.InvestorPage` | 31 | `str` | `Heading_Investor Information` |
| `elements.landing.Newsroom` | 32 | `str` | `LinkText_Newsroom` |
| `elements.landing.NewsroomPage` | 33 | `str` | `Heading_Newsroom` |
| `elements.landing.Careers` | 34 | `str` | `LinkText_Careers` |
| `elements.landing.CareersPage` | 35 | `str` | `Heading1_Careers` |
| `elements.landing.TermsOfUse` | 36 | `str` | `LinkText_Terms of Use` |
| `elements.landing.TermsOfUsePage` | 37 | `str` | `Heading1_Terms of Use` |
| `elements.landing.Privacy` | 38 | `str` | `LinkText_Privacy` |
| `elements.landing.PrivacyPage` | 39 | `str` | `Label_Privacy` |
| `elements.landing.SecurityCenter` | 40 | `str` | `LinkText_Security Center` |
| `elements.landing.SecurityCenterPage` | 41 | `str` | `Label_FNB Security Center` |
| `elements.landing.SiteMap` | 42 | `str` | `LinkText_Site Map` |
| `elements.landing.SiteMapPage` | 43 | `str` | `Label_Site Map` |
| `elements.landing.CorporateInformation` | 44 | `str` | `LinkText_Corporate Information` |
| `elements.landing.CorporateInformationPage` | 45 | `str` | `Heading_Corporate Information` |
| `elements.landing.list_of_element` | 46 | `str` | `CSS_div.field-wrap.field-wrap--stacked` |

## `src/test/resources/elements/login.yml`

Source: [`src/test/resources/elements/login.yml`](../../../src/test/resources/elements/login.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.login.body` | 3 | `str` | `CSS_body` |
| `elements.login.username` | 4 | `str` | `<environment- or user-supplied value>` |
| `elements.login.password` | 5 | `str` | `<redacted>` |
| `elements.login.loginButton` | 6 | `str` | `CSS_button[type='submit']` |
| `elements.login.login_text` | 7 | `str` | `CSS_//b[text()='Login ']` |
| `elements.login.otp_field` | 8 | `str` | `CSS_#Code` |
| `elements.login.otp_continue_btn` | 9 | `str` | `Button_Continue` |
| `elements.login.location_flt` | 10 | `str` | `CSS_#inputLocation` |
| `elements.login.cashbox_flt` | 11 | `str` | `CSS_#textCashbox` |
| `elements.login.ok_btn` | 12 | `str` | `Button_OK` |
| `elements.login.sign_on_btn` | 13 | `str` | `Button_Sign On` |
| `elements.login.yes_btn` | 14 | `str` | `Button_Yes` |

## `src/test/resources/elements/panda_page.yml`

Source: [`src/test/resources/elements/panda_page.yml`](../../../src/test/resources/elements/panda_page.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.panda_page.body` | 3 | `str` | `CSS_body` |
| `elements.panda_page.home_tab` | 4 | `str` | `CSS_//li[contains(.,'Home')]` |
| `elements.panda_page.about_tab` | 5 | `str` | `CSS_//a[normalize-space(text())='About']` |
| `elements.panda_page.contact_tab` | 6 | `str` | `CSS_//a[normalize-space(text())='Contact']` |
| `elements.panda_page.speaking_tab` | 7 | `str` | `CSS_//a[normalize-space(text())='Speaking']` |
| `elements.panda_page.teaching_tab` | 8 | `str` | `CSS_//a[normalize-space(text())='Teaching']` |
| `elements.panda_page.bdd_tab` | 9 | `str` | `CSS_//li[contains(.,'BDD')]` |
| `elements.panda_page.development_tab` | 10 | `str` | `CSS_//a[normalize-space(text())='Development']` |
| `elements.panda_page.testing_tab` | 11 | `str` | `CSS_//li[@id='menu-item-6835']/a[1]` |
| `elements.panda_page.python_tab` | 12 | `str` | `CSS_//li[@id='menu-item-6849']/a[1]` |

## `src/test/resources/elements/personal_bank.yml`

Source: [`src/test/resources/elements/personal_bank.yml`](../../../src/test/resources/elements/personal_bank.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `elements.personal.checking_and_savings` | 3 | `str` | `LinkText_Checking & Savings` |
| `elements.personal.browse_all_products` | 4 | `str` | `CSS_a[title='Checking: Browse All Products']` |
| `elements.personal.estyle_account` | 5 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.estyle_account_header` | 6 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.estyle_plus` | 7 | `str` | `LinkText_eStyle Plus Account eStyle` |
| `elements.personal.freestyle_account` | 8 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.lifestyle_account` | 9 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.premierstyle_account` | 10 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.add_to_cart` | 11 | `str` | `CSS_button[alt='Add To Cart']` |
| `elements.personal.cart_proceed_to_check` | 12 | `str` | `XPATH_//span[text()[normalize-space()='Proceed to Checkout']]` |
| `elements.personal.apply_now_check` | 13 | `str` | `XPATH_//input[@value='Apply Now']` |
| `elements.personal.checkout_btn` | 14 | `str` | `XPATH_(//button[text()[normalize-space()='Checkout']])[1]` |
| `elements.personal.first_name_field` | 15 | `str` | `CSS_#fname` |
| `elements.personal.fName_Validation_message` | 16 | `str` | `XPATH_//input[@data-fnb-validation-message='First Name can only con…` |
| `elements.personal.last_name_field` | 17 | `str` | `CSS_#lname` |
| `elements.personal.lName_Validation_message` | 18 | `str` | `XPATH_//input[@data-fnb-validation-message='Last Name can only cont…` |
| `elements.personal.organization_name_field` | 19 | `str` | `CSS_#business` |
| `elements.personal.email_field` | 20 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.email_Validation_message` | 21 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.zip_code_field` | 22 | `str` | `CSS_#zip` |
| `elements.personal.zip_Validation_message` | 23 | `str` | `XPATH_//input[@data-fnb-validation-message='Please enter a 5-Digit …` |
| `elements.personal.phone_number` | 24 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.phone_Validation_message` | 25 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.new_customer_check` | 26 | `str` | `XPATH_input[aria-label='New Customer ']` |
| `elements.personal.new_customer_Validation_message` | 27 | `str` | `XPATH_(//input[@data-fnb-validation-message='Please make a selectio…` |
| `elements.personal.continue_btn` | 28 | `str` | `Button_Continue` |
| `elements.personal.view_and_accept` | 29 | `str` | `Button_View and Accept` |
| `elements.personal.i_accept_btn` | 30 | `str` | `Button_I Accept` |
| `elements.personal.dateOfBirth` | 31 | `str` | `Placeholder_mm/dd/yyyy` |
| `elements.personal.prefill_info` | 32 | `str` | `CSS_#Path` |
| `elements.personal.continue_btn_window` | 33 | `str` | `TestId_continue` |
| `elements.personal.next_btn` | 34 | `str` | `Button_Next` |
| `elements.personal.dob_Validation_message` | 35 | `str` | `Text_Date of Birth is required` |
| `elements.personal.form_Accept_Validation_message` | 36 | `str` | `Text_You must review and accept` |
| `elements.personal.firstName` | 37 | `str` | `Label_First Name` |
| `elements.personal.middleName` | 38 | `str` | `Name_Customers.Primary.MiddleName` |
| `elements.personal.lastName` | 39 | `str` | `Name_Customers.Primary.LastName` |
| `elements.personal.suffix` | 40 | `str` | `CSS_input[name='Customers.Primary.Suffix']` |
| `elements.personal.suffix_Text` | 41 | `str` | `Text_SR` |
| `elements.personal.ssn` | 42 | `str` | `Name_Customers.Primary.Tin` |
| `elements.personal.ssn_Error` | 43 | `str` | `XPATH_//div[text()='Invalid SSN format or value.']` |
| `elements.personal.email` | 44 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.street_Address1` | 45 | `str` | `Name_Customers.Primary.AddressCurrent.Street1` |
| `elements.personal.street_Address2` | 46 | `str` | `Name_Customers.Primary.AddressCurrent.Street2` |
| `elements.personal.city` | 47 | `str` | `Name_Customers.Primary.AddressCurrent.City` |
| `elements.personal.state` | 48 | `str` | `CSS_input[name='Customers.Primary.AddressCurrent.State']` |
| `elements.personal.stateText` | 49 | `str` | `Option_PA` |
| `elements.personal.zip` | 50 | `str` | `XPATH_//*[@id='Customers.Primary.AddressCurrent.PostalCode']` |
| `elements.personal.continuebtn` | 51 | `str` | `CSS_button[data-testid='continue']` |
| `elements.personal.heading_pg3` | 52 | `str` | `XPATH_//h2[text()='FNB SmartCash']` |
| `elements.personal.applyForCreditCard` | 53 | `str` | `Button_Yes, I'd like to apply for` |
| `elements.personal.citizenship` | 54 | `str` | `Label_Yes` |
| `elements.personal.idType` | 55 | `str` | `Label_ID Type` |
| `elements.personal.idValue` | 56 | `str` | `Option_Driver's License` |
| `elements.personal.idNumber` | 57 | `str` | `Label_ID Number` |
| `elements.personal.stateOfIssue` | 58 | `str` | `Label_State of Issue` |
| `elements.personal.stateOfIssueValue` | 59 | `str` | `Option_PA` |
| `elements.personal.issueDate` | 60 | `str` | `Label_Issue Date` |
| `elements.personal.expDate` | 61 | `str` | `Label_Expiration Date` |
| `elements.personal.residenceStatus` | 62 | `str` | `Label_Residence Status` |
| `elements.personal.residenceType` | 63 | `str` | `Option_Own` |
| `elements.personal.currentRentOrMor` | 64 | `str` | `Label_Current Rent/Mortgage Payment` |
| `elements.personal.noOfYears` | 65 | `str` | `CSS_input[name='Customers.Primary.AddressCurrent.Years']` |
| `elements.personal.mailingAddress` | 66 | `str` | `Label_Yes` |
| `elements.personal.employmentStatus` | 67 | `str` | `Label_Employment Status` |
| `elements.personal.employmentType` | 68 | `str` | `Option_Full Time` |
| `elements.personal.empName` | 69 | `str` | `Label_Employer Name` |
| `elements.personal.jobTitle` | 70 | `str` | `Label_Job Title` |
| `elements.personal.OcpCategory` | 71 | `str` | `Label_Occupation Category` |
| `elements.personal.OcpType` | 72 | `str` | `Option_Management` |
| `elements.personal.payFreq` | 73 | `str` | `Label_Pay Frequency` |
| `elements.personal.payFreqSel` | 74 | `str` | `Option_Annual` |
| `elements.personal.paymentAmount` | 75 | `str` | `Label_Payment Amount` |
| `elements.personal.years` | 76 | `str` | `Label_Years` |
| `elements.personal.estimated_annualIncome` | 77 | `str` | `Label_Estimated Annual Income` |
| `elements.personal.months` | 78 | `str` | `Label_Months` |
| `elements.personal.coApp` | 79 | `str` | `XPATH_//label[text()='No']` |
| `elements.personal.coAppSecond` | 80 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to add a co-ap…` |
| `elements.personal.purpose` | 81 | `str` | `Label_What is the purpose of this` |
| `elements.personal.purposeType` | 82 | `str` | `Option_Personal` |
| `elements.personal.source` | 83 | `str` | `Label_What is the source of funds` |
| `elements.personal.sourceType` | 84 | `str` | `Option_Employment Income` |
| `elements.personal.cashDeposit` | 85 | `str` | `XPATH_(//input[@data-analytics-label='Will this account have U.S. c…` |
| `elements.personal.wireTransfer` | 86 | `str` | `XPATH_(//input[@data-analytics-label='Will there be monthly domesti…` |
| `elements.personal.foreignWTransfer` | 87 | `str` | `XPATH_(//input[@data-analytics-label='Will there be monthly foreign…` |
| `elements.personal.overdraft` | 88 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to keep the au…` |
| `elements.personal.automaticCoverage` | 89 | `str` | `XPATH_(//input[@data-analytics-label='Would you like to keep the au…` |
| `elements.personal.signature` | 90 | `str` | `CSS_input[placeholder='Type your signature here']` |
| `elements.personal.estyle` | 91 | `str` | `Label_eStyle` |
| `elements.personal.firstRateSaving` | 92 | `str` | `Label_FirstRate Savings` |
| `elements.personal.transferFromBank` | 93 | `str` | `TestId_Transfer from an account with another bank` |
| `elements.personal.personalPageVerification` | 94 | `str` | `Text_Login Personal Banking Other Services Destination My Default D…` |
| `elements.personal.searchBank` | 95 | `str` | `CSS_input[placeholder='Search for 12,000+ Institutions']` |
| `elements.personal.bankName` | 96 | `str` | `Label_Regional Acceptance Corporation` |
| `elements.personal.bankUserName` | 97 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.bankPassword` | 98 | `str` | `<redacted>` |
| `elements.personal.submitButton` | 99 | `str` | `Button_Submit` |
| `elements.personal.body` | 100 | `str` | `CSS_body` |
| `elements.personal.accountSelectionRB` | 101 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.creditOrDebit` | 102 | `str` | `XPATH_//button[@data-testid='Credit/Debit Card']` |
| `elements.personal.cardNum` | 103 | `str` | `XPATH_//input[@name='cardNum']` |
| `elements.personal.confirmSSN` | 104 | `str` | `XPATH_//input[@name='Customers.Primary.WithholdingInformation.Certi…` |
| `elements.personal.confirmWithholding` | 105 | `str` | `XPATH_//input[@name='Customers.Primary.WithholdingInformation.Backu…` |
| `elements.personal.viewAndAccept1` | 106 | `str` | `XPATH_(//div[text()='View and Accept'])[1]` |
| `elements.personal.viewAndAccept2` | 107 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.personal.viewAndAccept2_FS` | 108 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.personal.page2` | 109 | `str` | `XPATH_//span[text()='Page 2']` |
| `elements.personal.viewAndAccept3` | 110 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.personal.viewAndAccept4` | 111 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.personal.viewAndAccept4_FS` | 112 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.personal.foreignGovtOfficial` | 113 | `str` | `XPATH_(//input[@data-analytics-label='Are you currently or have you…` |
| `elements.personal.immediateFamilyMember` | 114 | `str` | `XPATH_(//input[@data-analytics-label='Are you an immediate family m…` |
| `elements.personal.deplomat` | 115 | `str` | `XPATH_(//input[@data-analytics-label='Are you a diplomat?'])[2]` |
| `elements.personal.canvas` | 116 | `str` | `XPATH_(//canvas)[1]` |
| `elements.personal.debitcard` | 117 | `str` | `Label_No` |
| `elements.personal.congratulations` | 118 | `str` | `XPATH_//h2[text()='Congratulations!']` |
| `elements.personal.confirmationID` | 119 | `str` | `XPATH_//div[contains(text(),'Confirmation ID:')]` |
| `elements.personal.accountNumber` | 120 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.orderChecks` | 121 | `str` | `Label_I don't want to order any` |
| `elements.personal.closeButton` | 123 | `str` | `XPATH_//button[text()='×']` |
| `elements.personal.coAppHeading` | 124 | `str` | `XPATH_//h1[contains(text(),'Co-applicants for')]` |
| `elements.personal.coAppContent1` | 125 | `str` | `XPATH_//p[text()='We will need to identify and collect more informa…` |
| `elements.personal.coAppHeading2` | 126 | `str` | `XPATH_//h3[text()='Beneficial Owners']` |
| `elements.personal.coAppContent2` | 127 | `str` | `XPATH_//p[text()='These individuals own 25% or more of the business.']` |
| `elements.personal.coAppHeading3` | 128 | `str` | `XPATH_//h3[text()='Signers']` |
| `elements.personal.coAppContent3` | 129 | `str` | `XPATH_//p[text()='You must identify at least 1 Signer for the appli…` |
| `elements.personal.coAppHeading4` | 130 | `str` | `XPATH_//h3[text()='Control Person']` |
| `elements.personal.coAppContent4` | 131 | `str` | `XPATH_//p[text()='You must identify 1 Control Person for the applic…` |
| `elements.personal.coAppHeading5` | 132 | `str` | `XPATH_//h2[text()='Co-Applicants']` |
| `elements.personal.coAppSubHeading` | 133 | `str` | `XPATH_//h3[text()='Select previous co-applicants that should be add…` |
| `elements.personal.coAppConent5` | 134 | `str` | `XPATH_//p[text()='Your previously entered co-applicants can be adde…` |
| `elements.personal.existingCoAppCheckBox1` | 135 | `str` | `XPATH_(//input[@field='[object Object]'])[1]` |
| `elements.personal.existingCoAppCheckBox2` | 136 | `str` | `XPATH_(//input[@field='[object Object]'])[2]` |
| `elements.personal.existingCoAppName1` | 137 | `str` | `XPATH_(//div[contains(@class,'Checkbox labelText')])[1]` |
| `elements.personal.existingCoAppName2` | 138 | `str` | `XPATH_(//div[contains(@class,'Checkbox labelText')])[2]` |
| `elements.personal.removeCoapp` | 139 | `str` | `CSS_button.styledButton_button__bqHSS.BlankButton_button__UQ-kO` |
| `elements.personal.areYouSure` | 140 | `str` | `Heading_Are you sure?` |
| `elements.personal.cancel` | 141 | `str` | `Button_Cancel` |
| `elements.personal.removeBOwner` | 142 | `str` | `Button_Remove Beneficial Owner` |
| `elements.personal.firstNameInPopup` | 143 | `str` | `CSS_input[name='FirstName']` |
| `elements.personal.lastNameInPopup` | 144 | `str` | `CSS_input[name='LastName']` |
| `elements.personal.emailInPopup` | 145 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.mobileInPopup` | 146 | `str` | `XPATH_input[name='MobilePhone']` |
| `elements.personal.beneficialOwnerChkBox` | 147 | `str` | `XPATH_input[name='ownershiptype']` |
| `elements.personal.signerChkBox` | 148 | `str` | `XPATH_input[name='signer']` |
| `elements.personal.conttrolPersonCheckBox` | 149 | `str` | `XPATH_input[name='controlPerson']` |
| `elements.personal.ownershipPersentage` | 150 | `str` | `XPATH_input[name='Ownership']` |
| `elements.personal.businessRole` | 151 | `str` | `CSS_#BusinessRole` |
| `elements.personal.president` | 152 | `str` | `XPATH_//li[text()='President']` |
| `elements.personal.vicePresident` | 153 | `str` | `XPATH_//li[text()='Vice President']` |
| `elements.personal.assistantVicePresident` | 154 | `str` | `XPATH_//li[text()='Assistant Vice President']` |
| `elements.personal.chairman` | 155 | `str` | `XPATH_//li[text()='Chairman']` |
| `elements.personal.chiefExecutiveOffer` | 156 | `str` | `XPATH_//li[text()='Chief Executive Officer']` |
| `elements.personal.chiefFinancialOfficer` | 157 | `str` | `XPATH_//li[text()='Chief Financial Officer']` |
| `elements.personal.chiefOperatingOfficer` | 158 | `str` | `XPATH_//li[text()='Chief Operating Officer']` |
| `elements.personal.chiefExcutiveVicePresident` | 159 | `str` | `XPATH_//input[@value='Executive Vice President']` |
| `elements.personal.generalPartner` | 160 | `str` | `XPATH_//li[text()='General Partner']` |
| `elements.personal.limitedPartner` | 161 | `str` | `XPATH_//li[text()='Limited Partner']` |
| `elements.personal.manager` | 162 | `str` | `XPATH_//li[text()='Manager']` |
| `elements.personal.managingMember` | 163 | `str` | `XPATH_//li[text()='Managing Member']` |
| `elements.personal.member` | 164 | `str` | `XPATH_//li[text()='Member']` |
| `elements.personal.owner` | 165 | `str` | `XPATH_//li[text()='Owner']` |
| `elements.personal.secretary` | 166 | `str` | `<redacted>` |
| `elements.personal.seniorVicePresident` | 167 | `str` | `XPATH_//li[text()='Senior Vice President']` |
| `elements.personal.tresurer` | 168 | `str` | `XPATH_//li[text()='Treasurer']` |
| `elements.personal.saveInPopup` | 169 | `str` | `XPATH_//button[@type='submit']` |
| `elements.personal.ownershipPercentage` | 170 | `str` | `XPATH_input[name='Ownership']` |
| `elements.personal.editButton` | 171 | `str` | `Button_Edit` |
| `elements.personal.editButton2` | 172 | `str` | `XPATH_(//div[text()='Edit'])[2]` |
| `elements.personal.editButton3` | 173 | `str` | `XPATH_(//div[text()='Edit'])[3]` |
| `elements.personal.addCoApp` | 174 | `str` | `Button_Add Co-Applicant` |
| `elements.personal.coAppSignerYes` | 175 | `str` | `CSS_#coapplicantSignersYes` |
| `elements.personal.coAppSignerNo` | 176 | `str` | `CSS_#coapplicantSignersNo` |
| `elements.personal.coAppOwnershipYes` | 177 | `str` | `CSS_#coapplicantOwnershipYes` |
| `elements.personal.coAppOwnershipNo` | 178 | `str` | `CSS_#coapplicantOwnershipNo` |
| `elements.personal.businessName` | 179 | `str` | `Label_Business Name` |
| `elements.personal.ownershipPercent` | 180 | `str` | `Label_Ownership %` |
| `elements.personal.crossButton` | 181 | `str` | `XPATH_//span[text()='✕']` |
| `elements.personal.browse_all_Saving` | 185 | `str` | `Label_Savings: Browse All Products` |
| `elements.personal.FirstRate_saving` | 186 | `str` | `LinkText_FirstRate Savings FirstRate` |
| `elements.personal.FirstRate_Savings_header` | 187 | `str` | `HeadingTextSetTure_FirstRate Savings` |
| `elements.personal.FirstRate` | 188 | `str` | `Label_FirstRate Savings` |
| `elements.personal.Credit_Debit_Card` | 189 | `str` | `TestId_Credit/Debit Card` |
| `elements.personal.Credit_debit_Frame` | 190 | `str` | `Text_× Card Information Card` |
| `elements.personal.Card_NbrField` | 191 | `str` | `Label_Card Number` |
| `elements.personal.Card_Nbr` | 192 | `str` | `Placeholder_5678 9012 3456` |
| `elements.personal.Card_Expiry` | 193 | `str` | `Placeholder_MM/YY` |
| `elements.personal.Card_Code` | 194 | `str` | `Label_Card Code` |
| `elements.personal.Card_FirstName` | 195 | `str` | `Textbox_firstName` |
| `elements.personal.Card_LastName` | 196 | `str` | `Textbox_lastName` |
| `elements.personal.Card_ZipCode` | 197 | `str` | `Textbox_zip` |
| `elements.personal.ProcessCard` | 198 | `str` | `Button_Process Card` |
| `elements.personal.CardContinueBtn` | 199 | `str` | `Button_Continue` |
| `elements.personal.FirstRateMoneyMkt` | 201 | `str` | `LinkText_FirstRate Money Market` |
| `elements.personal.FirstRateMoneyMktHd` | 202 | `str` | `HeadingTextSetTure_FirstRate Money Market` |
| `elements.personal.FirstRateMoney` | 203 | `str` | `Label_FirstRate Money Market` |
| `elements.personal.CheckOrder` | 204 | `str` | `Label_I don't want to order any` |
| `elements.personal.HealthSavAccount` | 206 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.HealthSavAccountHd` | 207 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.HSAaccountType` | 208 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.ApplyCheckingAc` | 209 | `str` | `Button_No, I do not want to apply` |
| `elements.personal.AnnualIncome` | 210 | `str` | `Label_Estimated Annual Income` |
| `elements.personal.TaxYear` | 211 | `str` | `Label_Current Year (2024)` |
| `elements.personal.HCAIndividual` | 212 | `str` | `Label_Health Savings - Individual` |
| `elements.personal.CertificateDeposit` | 215 | `str` | `LinkText_Certificates of Deposit (CDs` |
| `elements.personal.CertificateDepositHD` | 216 | `str` | `HeadingText_Certificates of Deposit` |
| `elements.personal.CertDipoType` | 217 | `str` | `CSS_select[aria-label='Select Option']` |
| `elements.personal.CertDipoAmount` | 218 | `str` | `Label_Certificate of Deposit - 48` |
| `elements.personal.bankNameRegions` | 219 | `str` | `Label_Regions Bank` |
| `elements.personal.transferFromBank1` | 220 | `str` | `TestId_Transfer from an account with another bank` |
| `elements.personal.RegionsbankUserName` | 221 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.RegionsbankPassword` | 222 | `str` | `<redacted>` |
| `elements.personal.viewAndAcceptCertDipo` | 223 | `str` | `XPATH_//button[@id='Customers.Primary.SignedDisclosures.DynamicDisc…` |
| `elements.personal.SaversGoalCertDip` | 225 | `str` | `LinkText_Savers Goal Certificates of` |
| `elements.personal.SaversGoalCertDipHD` | 226 | `str` | `HeadingText_Savers Goal Certificates of` |
| `elements.personal.SaversGoalDipoAmount` | 227 | `str` | `Label_Savers Goal CD` |
| `elements.personal.ownershipQuestion` | 228 | `str` | `Text_Does another business have 25` |
| `elements.personal.ownershipQuestionNo` | 229 | `str` | `CSS_#mainradio0Yes` |
| `elements.personal.ownershipQuestionYes` | 230 | `str` | `CSS_#mainradio0No` |
| `elements.personal.OwnershipQuestionYesMessage` | 231 | `str` | `Text_We will be collecting` |
| `elements.personal.OwnershipQuestionNoMessage` | 232 | `str` | `Text_We cannot proceed with your` |
| `elements.personal.addAnotherBusiness` | 233 | `str` | `Button_Add Another Business` |
| `elements.personal.businessName2` | 234 | `str` | `XPATH_//input[@id='businesses[1].businessName']` |
| `elements.personal.ownerhsip2` | 235 | `str` | `XPATH_//input[@id='businesses[1].ownership']` |
| `elements.personal.additionalBusinessQ` | 236 | `str` | `Text_Contact FNB if any additional` |
| `elements.personal.foreignGovYes` | 237 | `str` | `CSS_#coapplicantForeignGovYes` |
| `elements.personal.foreignGovNo` | 238 | `str` | `CSS_#coapplicantForeignGovNo` |
| `elements.personal.CertiDepositSpecial` | 241 | `str` | `LinkText_Offers Certificate of Deposit` |
| `elements.personal.add_to_cartSpDepo` | 242 | `str` | `Title_Add To Cart` |
| `elements.personal.CertiDepositSpecialHD` | 243 | `str` | `HeadingText_Certificate of Deposit` |
| `elements.personal.ExitingFnbAC` | 244 | `str` | `Label_I have an existing FNB` |
| `elements.personal.CertiDepoSpecialAmt` | 245 | `str` | `Label_Special Certificate of` |
| `elements.personal.FlexCD` | 247 | `str` | `LinkText_Flex CD Flex CD box Flexible` |
| `elements.personal.FlexCDHD` | 248 | `str` | `HeadingText_Flex CD` |
| `elements.personal.FlexCDHDAmt` | 249 | `str` | `Label_Flex CD` |
| `elements.personal.fNameError` | 250 | `str` | `Text_First Name is required` |
| `elements.personal.LNameError` | 251 | `str` | `Text_Last Name is required` |
| `elements.personal.EmailError` | 252 | `str` | `<environment- or user-supplied value>` |
| `elements.personal.MobileError` | 253 | `str` | `Text_Mobile Phone is required` |
| `elements.personal.OwnershipError` | 254 | `str` | `Text_Ownership % is required` |
| `elements.personal.BusinessRoleError` | 255 | `str` | `Text_Business Role/Title is` |
| `elements.personal.BOHeading` | 258 | `str` | `XPATH_//h1[contains(text(),'Additional Information about')]` |
| `elements.personal.BODescription` | 259 | `str` | `Text_Because this business owns 25` |
| `elements.personal.BOSubHeading` | 260 | `str` | `Heading_Beneficial Owners` |
| `elements.personal.addBO` | 261 | `str` | `Button_Add Beneficial Owner` |
| `elements.personal.foreignGovtYes` | 262 | `str` | `XPATH_//input[@id='ForeignGovtEntityYes']` |
| `elements.personal.foreignGovtNo` | 263 | `str` | `XPATH_//input[@id='ForeignGovtEntityNo']` |
| `elements.personal.disclosureChkBox` | 264 | `str` | `XPATH_//input[@id='DisclosureCheckbox']` |

## `src/test/resources/extent-config.xml`

Source: [`src/test/resources/extent-config.xml`](../../../src/test/resources/extent-config.xml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `(no suite/class entry detected)` | — | `—` | Inspect XML source |

## `src/test/resources/extent.properties`

Source: [`src/test/resources/extent.properties`](../../../src/test/resources/extent.properties)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `basefolder.name` | 11 | `string` | `test-output/` |
| `basefolder.datetimepattern` | 12 | `string` | `dd-MMM-yy_HH-mm-ss` |
| `extent.reporter.spark.start` | 22 | `string` | `true` |
| `extent.reporter.spark.out` | 24 | `string` | `SparkReport/Spark.html` |
| `extent.reporter.spark.config` | 25 | `string` | `src/test/resources/extent-config.xml` |
| `extent.reporter.spark.vieworder` | 26 | `string` | `dashboard,test,category,exception,author,device,log` |
| `extent.reporter.spark.theme` | 27 | `string` | `standard   # Options: standard, dark` |
| `extent.reporter.base64.start` | 32 | `string` | `true` |
| `extent.reporter.base64.out` | 33 | `string` | `Base64Report/Report.html` |
| `extent.reporter.pdf.start` | 38 | `string` | `true` |
| `extent.reporter.pdf.out` | 39 | `string` | `PdfReport/FNB-PTAF-Report.pdf` |
| `extent.reporter.excel.start` | 44 | `string` | `true` |
| `extent.reporter.excel.out` | 46 | `string` | `ExcelReport/FNB-PTAF-Report.xlsx` |
| `screenshot.dir` | 73 | `string` | `screenshots/` |
| `screenshot.rel.path` | 74 | `string` | `../screenshots/` |
| `systeminfo.os` | 79 | `string` | `Windows` |
| `systeminfo.user` | 80 | `string` | `<environment- or user-supplied value>` |
| `systeminfo.build` | 81 | `string` | `1.1` |
| `systeminfo.AppName` | 82 | `string` | `FNB-PTAF` |
| `systeminfo.Java` | 83 | `string` | `11` |
| `systeminfo.Environment` | 84 | `string` | `QA` |

## `src/test/resources/mobile/config/mobile-browser-config.yml`

Source: [`src/test/resources/mobile/config/mobile-browser-config.yml`](../../../src/test/resources/mobile/config/mobile-browser-config.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_browser_appium.enabled` | 20 | `bool` | `true` |
| `mobile_browser_appium.android.automation_name` | 23 | `str` | `UiAutomator2` |
| `mobile_browser_appium.android.platform_name` | 24 | `str` | `Android` |
| `mobile_browser_appium.android.device_name` | 25 | `str` | `Android Emulator` |
| `mobile_browser_appium.android.platform_version` | 26 | `str` | `(empty string)` |
| `mobile_browser_appium.android.udid` | 27 | `str` | `(empty string)` |
| `mobile_browser_appium.android.orientation` | 28 | `str` | `portrait` |
| `mobile_browser_appium.android.browser_name` | 29 | `str` | `Chrome` |
| `mobile_browser_appium.android.initial_url` | 30 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_appium.android.clean_start_enabled` | 34 | `str` | `true` |
| `mobile_browser_appium.android.clear_cookies` | 35 | `str` | `<redacted>` |
| `mobile_browser_appium.android.close_existing_tabs` | 36 | `str` | `false` |
| `mobile_browser_appium.android.terminate_before_start` | 37 | `str` | `false` |
| `mobile_browser_appium.android.activate_after_cleanup` | 38 | `str` | `false` |
| `mobile_browser_appium.android.reset_app_data` | 39 | `str` | `false` |
| `mobile_browser_appium.android.no_reset` | 40 | `bool` | `false` |
| `mobile_browser_appium.android.full_reset` | 41 | `bool` | `false` |
| `mobile_browser_appium.android.browser_package` | 42 | `str` | `com.android.chrome` |
| `mobile_browser_appium.android.chromedriver_autodownload` | 44 | `str` | `true` |
| `mobile_browser_appium.android.chromedriver_executable` | 45 | `str` | `(empty string)` |
| `mobile_browser_appium.android.chromedriver_mapping_file` | 46 | `str` | `(empty string)` |
| `mobile_browser_appium.android.auto_grant_permissions` | 47 | `str` | `true` |
| `mobile_browser_appium.ios.automation_name` | 50 | `str` | `XCUITest` |
| `mobile_browser_appium.ios.platform_name` | 51 | `str` | `iOS` |
| `mobile_browser_appium.ios.device_name` | 52 | `str` | `iPhone 17 Pro Max` |
| `mobile_browser_appium.ios.platform_version` | 53 | `str` | `(empty string)` |
| `mobile_browser_appium.ios.udid` | 54 | `str` | `(empty string)` |
| `mobile_browser_appium.ios.orientation` | 55 | `str` | `portrait` |
| `mobile_browser_appium.ios.browser_name` | 56 | `str` | `Safari` |
| `mobile_browser_appium.ios.initial_url` | 57 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_appium.ios.clean_start_enabled` | 63 | `str` | `true` |
| `mobile_browser_appium.ios.clear_cookies` | 64 | `str` | `<redacted>` |
| `mobile_browser_appium.ios.close_existing_tabs` | 65 | `str` | `true` |
| `mobile_browser_appium.ios.terminate_before_start` | 66 | `str` | `true` |
| `mobile_browser_appium.ios.activate_after_cleanup` | 67 | `str` | `true` |
| `mobile_browser_appium.ios.reset_app_data` | 68 | `str` | `false` |
| `mobile_browser_appium.ios.no_reset` | 69 | `bool` | `false` |
| `mobile_browser_appium.ios.full_reset` | 70 | `bool` | `false` |
| `mobile_browser_appium.ios.browser_bundle_id` | 71 | `str` | `com.apple.mobilesafari` |
| `mobile_browser_appium.ios.auto_accept_alerts` | 74 | `str` | `false` |
| `mobile_browser_appium.ios.auto_dismiss_alerts` | 75 | `str` | `false` |
| `mobile_browser_appium.ios.include_safari_in_webviews` | 76 | `str` | `true` |
| `mobile_browser_appium.ios.connect_hardware_keyboard` | 77 | `str` | `false` |
| `mobile_browser_appium.ios.safari_allow_popups` | 78 | `str` | `true` |
| `mobile_browser_appium.ios.safari_ignore_fraud_warning` | 79 | `str` | `true` |
| `mobile_browser_appium.ios.web_context_timeout_seconds` | 82 | `str` | `25` |
| `mobile_browser_appium.ios.safari_native_navigation_fallback_enabled` | 87 | `str` | `true` |

## `src/test/resources/mobile/config/mobile-config.yml`

Source: [`src/test/resources/mobile/config/mobile-config.yml`](../../../src/test/resources/mobile/config/mobile-config.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile.enabled` | 20 | `bool` | `true` |
| `mobile.appium_server_url` | 21 | `str` | `<environment- or user-supplied value>` |
| `mobile.default_platform` | 25 | `str` | `android` |
| `mobile.implicit_wait_seconds` | 32 | `int` | `0` |
| `mobile.explicit_wait_seconds` | 33 | `int` | `30` |
| `mobile.new_command_timeout_seconds` | 37 | `int` | `300` |
| `mobile.permissions.popup_timeout_seconds` | 41 | `int` | `3` |
| `mobile.permissions.max_popups_to_handle` | 43 | `int` | `5` |
| `mobile.permissions.capture_evidence` | 45 | `bool` | `true` |
| `mobile.evidence.output_directory` | 48 | `str` | `test-output/mobile-evidence` |
| `mobile.evidence.screenshot_on_failure` | 49 | `bool` | `true` |
| `mobile.evidence.screenshot_on_pass` | 50 | `bool` | `<redacted>` |
| `mobile.evidence.screenshot_after_each_scenario` | 51 | `bool` | `false` |
| `mobile.evidence.attach_screenshots_to_report` | 52 | `bool` | `true` |
| `mobile.evidence.video_recording_enabled` | 53 | `bool` | `false` |
| `mobile.evidence.video_on_failure_only` | 54 | `bool` | `true` |
| `mobile.evidence.attach_video_to_report` | 55 | `bool` | `false` |

## `src/test/resources/mobile/config/mobile-native-config.yml`

Source: [`src/test/resources/mobile/config/mobile-native-config.yml`](../../../src/test/resources/mobile/config/mobile-native-config.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile.android.automation_name` | 44 | `str` | `UiAutomator2` |
| `mobile.android.platform_name` | 45 | `str` | `Android` |
| `mobile.android.device_name` | 46 | `str` | `Android Emulator` |
| `mobile.android.platform_version` | 47 | `str` | `(empty string)` |
| `mobile.android.udid` | 48 | `str` | `(empty string)` |
| `mobile.android.orientation` | 49 | `str` | `portrait` |
| `mobile.android.app` | 51 | `str` | `src/test/resources/mobile/apps/TheApp.apk` |
| `mobile.android.app_package` | 52 | `str` | `(empty string)` |
| `mobile.android.app_activity` | 53 | `str` | `(empty string)` |
| `mobile.android.app_wait_package` | 55 | `str` | `(empty string)` |
| `mobile.android.app_wait_activity` | 56 | `str` | `(empty string)` |
| `mobile.android.system_port` | 57 | `str` | `(empty string)` |
| `mobile.android.adb_exec_timeout` | 58 | `str` | `(empty string)` |
| `mobile.android.auto_grant_permissions` | 59 | `str` | `true` |
| `mobile.android.no_reset` | 61 | `bool` | `false` |
| `mobile.android.full_reset` | 62 | `bool` | `false` |
| `mobile.ios.automation_name` | 65 | `str` | `XCUITest` |
| `mobile.ios.platform_name` | 66 | `str` | `iOS` |
| `mobile.ios.device_name` | 67 | `str` | `iPhone 15` |
| `mobile.ios.udid` | 68 | `str` | `(empty string)` |
| `mobile.ios.platform_version` | 69 | `str` | `(empty string)` |
| `mobile.ios.orientation` | 70 | `str` | `portrait` |
| `mobile.ios.app` | 76 | `str` | `src/test/resources/mobile/apps/ios-unzipped/TheApp.app` |
| `mobile.ios.bundle_id` | 77 | `str` | `(empty string)` |
| `mobile.ios.auto_accept_alerts` | 80 | `str` | `false` |
| `mobile.ios.auto_dismiss_alerts` | 81 | `str` | `false` |
| `mobile.ios.include_safari_in_webviews` | 84 | `str` | `false` |
| `mobile.ios.connect_hardware_keyboard` | 85 | `str` | `false` |
| `mobile.ios.xcode_org_id` | 86 | `str` | `(empty string)` |
| `mobile.ios.xcode_signing_id` | 87 | `str` | `(empty string)` |
| `mobile.ios.updated_wda_bundle_id` | 88 | `str` | `(empty string)` |
| `mobile.ios.wda_local_port` | 89 | `str` | `(empty string)` |
| `mobile.ios.wda_startup_retries` | 90 | `str` | `(empty string)` |
| `mobile.ios.wda_startup_retry_interval` | 91 | `str` | `(empty string)` |
| `mobile.ios.wda_launch_timeout` | 92 | `str` | `(empty string)` |
| `mobile.ios.wda_connection_timeout` | 93 | `str` | `(empty string)` |
| `mobile.ios.wait_for_idle_timeout` | 94 | `str` | `(empty string)` |
| `mobile.ios.app_launch_state_timeout_sec` | 95 | `str` | `(empty string)` |
| `mobile.ios.use_new_wda` | 96 | `str` | `(empty string)` |
| `mobile.ios.show_xcode_log` | 97 | `str` | `(empty string)` |
| `mobile.ios.enforce_app_install` | 98 | `str` | `(empty string)` |
| `mobile.ios.no_reset` | 100 | `bool` | `false` |
| `mobile.ios.full_reset` | 101 | `bool` | `false` |

## `src/test/resources/mobile/elements/fnb_elements.yml`

Source: [`src/test/resources/mobile/elements/fnb_elements.yml`](../../../src/test/resources/mobile/elements/fnb_elements.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_elements.fnb.appRoot.ios` | 4 | `str` | `XPATH_//XCUIElementTypeApplication` |
| `mobile_elements.fnb.appRoot.android` | 5 | `str` | `XPATH_//android.widget.FrameLayout \| //*[@package]` |
| `mobile_elements.fnb.anyVisibleElement.ios` | 7 | `str` | `XPATH_//*[self::XCUIElementTypeButton or self::XCUIElementTypeStati…` |
| `mobile_elements.fnb.anyVisibleElement.android` | 8 | `str` | `XPATH_//*[self::android.widget.Button or self::android.widget.TextV…` |
| `mobile_elements.fnb.possibleLoginButton.ios` | 10 | `str` | `XPATH_//*[contains(@name,'Log') or contains(@label,'Log') or contai…` |
| `mobile_elements.fnb.possibleLoginButton.android` | 11 | `str` | `XPATH_//*[contains(@text,'Log') or contains(@content-desc,'Log') or…` |
| `mobile_elements.fnb.possibleUserIdField.ios` | 13 | `str` | `<environment- or user-supplied value>` |
| `mobile_elements.fnb.possibleUserIdField.android` | 14 | `str` | `<environment- or user-supplied value>` |
| `mobile_elements.fnb.possiblePasswordField.ios` | 16 | `str` | `<redacted>` |
| `mobile_elements.fnb.possiblePasswordField.android` | 17 | `str` | `<redacted>` |
| `mobile_elements.fnb.possibleAllowButton.ios` | 19 | `str` | `XPATH_//*[contains(@name,'Allow') or contains(@label,'Allow') or co…` |
| `mobile_elements.fnb.possibleAllowButton.android` | 20 | `str` | `XPATH_//*[contains(@text,'Allow') or contains(@content-desc,'Allow'…` |

## `src/test/resources/mobile/elements/google_mobile_browser_elements.yml`

Source: [`src/test/resources/mobile/elements/google_mobile_browser_elements.yml`](../../../src/test/resources/mobile/elements/google_mobile_browser_elements.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_elements.googleBrowser.searchBox.android` | 12 | `str` | `XPATH_//*[@name='q' or @aria-label='Search' or contains(@title,'Sea…` |
| `mobile_elements.googleBrowser.searchBox.ios` | 13 | `str` | `XPATH_//*[@name='q' or @aria-label='Search' or contains(@title,'Sea…` |
| `mobile_elements.googleBrowser.searchBox.common` | 14 | `str` | `XPATH_//*[@name='q' or @aria-label='Search' or contains(@title,'Sea…` |
| `mobile_elements.googleBrowser.results.android` | 16 | `str` | `XPATH_//*[contains(.,'PTAF') or contains(.,'automation')]` |
| `mobile_elements.googleBrowser.results.ios` | 17 | `str` | `XPATH_//*[contains(.,'PTAF') or contains(.,'automation')]` |
| `mobile_elements.googleBrowser.results.common` | 18 | `str` | `XPATH_//*[contains(.,'PTAF') or contains(.,'automation')]` |

## `src/test/resources/mobile/elements/mobile_permissions.yml`

Source: [`src/test/resources/mobile/elements/mobile_permissions.yml`](../../../src/test/resources/mobile/elements/mobile_permissions.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_elements.permissions.allowButton.ios` | 12 | `str` | `XPATH_//*[contains(@name,'Allow') or contains(@label,'Allow') or co…` |
| `mobile_elements.permissions.allowButton.android` | 13 | `str` | `XPATH_//*[contains(@resource-id,'permission_allow') or contains(@te…` |
| `mobile_elements.permissions.allowWhileUsingButton.ios` | 15 | `str` | `XPATH_//*[contains(@name,'Allow While Using') or contains(@label,'A…` |
| `mobile_elements.permissions.allowWhileUsingButton.android` | 16 | `str` | `XPATH_//*[contains(@text,'While using') or contains(@text,'only whi…` |
| `mobile_elements.permissions.denyButton.ios` | 18 | `str` | `XPATH_//*[contains(@name,'Don') or contains(@label,'Don') or contai…` |
| `mobile_elements.permissions.denyButton.android` | 19 | `str` | `XPATH_//*[contains(@resource-id,'permission_deny') or contains(@tex…` |

## `src/test/resources/mobile/elements/safari_browser_elements.yml`

Source: [`src/test/resources/mobile/elements/safari_browser_elements.yml`](../../../src/test/resources/mobile/elements/safari_browser_elements.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_elements.safariBrowser.addressBar.ios` | 12 | `str` | `XPATH_//*[@name='URL' or @label='Address' or contains(@label,'Searc…` |
| `mobile_elements.safariBrowser.startPageCloseButton.ios` | 14 | `str` | `XPATH_//*[@name='Close' or @label='Close' or @name='Done' or @label…` |

## `src/test/resources/mobile/elements/theapp_elements.yml`

Source: [`src/test/resources/mobile/elements/theapp_elements.yml`](../../../src/test/resources/mobile/elements/theapp_elements.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_elements.theapp.echoBoxMenu` | 3 | `str` | `ACCESSIBILITY_ID_Echo Box` |
| `mobile_elements.theapp.loginMenu` | 4 | `str` | `ACCESSIBILITY_ID_Login Screen` |
| `mobile_elements.theapp.clipboardDemoMenu` | 5 | `str` | `ACCESSIBILITY_ID_Clipboard Demo` |
| `mobile_elements.theapp.echoInput` | 6 | `str` | `ACCESSIBILITY_ID_messageInput` |
| `mobile_elements.theapp.saveButton` | 7 | `str` | `ACCESSIBILITY_ID_messageSaveBtn` |
| `mobile_elements.theapp.savedMessage.android` | 9 | `str` | `XPATH_//*[contains(@text,'PTAF cross-platform native mobile test') …` |
| `mobile_elements.theapp.savedMessage.ios` | 10 | `str` | `ACCESSIBILITY_ID_savedMessage` |
| `mobile_elements.theapp.username` | 11 | `str` | `<environment- or user-supplied value>` |
| `mobile_elements.theapp.password` | 12 | `str` | `<redacted>` |
| `mobile_elements.theapp.loginButton` | 13 | `str` | `ACCESSIBILITY_ID_loginBtn` |

## `src/test/resources/mobile/elements/unified_locator_examples.yml`

Source: [`src/test/resources/mobile/elements/unified_locator_examples.yml`](../../../src/test/resources/mobile/elements/unified_locator_examples.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_elements.unifiedExamples.nativeLoginButton.android` | 15 | `str` | `ACCESSIBILITY_ID_loginButton` |
| `mobile_elements.unifiedExamples.nativeLoginButton.ios` | 16 | `str` | `ACCESSIBILITY_ID_loginButton` |
| `mobile_elements.unifiedExamples.nativeLoginButton.default` | 17 | `str` | `Button_Login` |
| `mobile_elements.unifiedExamples.browserSearchBox.mobileBrowser` | 21 | `str` | `TEXTBOX_Search` |
| `mobile_elements.unifiedExamples.browserSearchBox.android` | 22 | `str` | `XPATH_//*[@name='q' or @aria-label='Search']` |
| `mobile_elements.unifiedExamples.browserSearchBox.ios` | 23 | `str` | `XPATH_//*[@name='q' or @aria-label='Search']` |
| `mobile_elements.unifiedExamples.browserSearchBox.default` | 24 | `str` | `TEXTBOX_Search` |
| `mobile_elements.unifiedExamples.cssSearchBox.mobileBrowser` | 28 | `str` | `CSS_input[name='q']` |
| `mobile_elements.unifiedExamples.cssSearchBox.default` | 29 | `str` | `CSS_input[name='q']` |
| `mobile_elements.unifiedExamples.xpathResultText.mobileBrowser` | 32 | `str` | `XPATH_//*[contains(.,'PTAF') or contains(.,'automation')]` |
| `mobile_elements.unifiedExamples.xpathResultText.default` | 33 | `str` | `TEXT_PTAF` |

## `src/test/resources/mobile_browser/config/mobile-browser-execution.yml`

Source: [`src/test/resources/mobile_browser/config/mobile-browser-execution.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_browser.enabled` | 9 | `bool` | `true` |
| `mobile_browser.orientation` | 10 | `str` | `profile` |
| `mobile_browser.evidence.output_directory` | 13 | `str` | `test-output/mobile-browser-evidence` |
| `mobile_browser.evidence.screenshot_on_failure` | 14 | `bool` | `true` |
| `mobile_browser.evidence.screenshot_on_pass` | 15 | `bool` | `<redacted>` |
| `mobile_browser.evidence.screenshot_after_each_scenario` | 16 | `bool` | `false` |
| `mobile_browser.evidence.attach_screenshots_to_report` | 17 | `bool` | `true` |
| `mobile_browser.evidence.video_recording_enabled` | 18 | `bool` | `false` |
| `mobile_browser.evidence.video_size_width` | 19 | `int` | `390` |
| `mobile_browser.evidence.video_size_height` | 20 | `int` | `844` |
| `mobile_browser.visual.enabled` | 23 | `bool` | `true` |
| `mobile_browser.visual.baseline_directory` | 24 | `str` | `src/test/resources/baselines/mobile_browser` |
| `mobile_browser.visual.output_directory` | 25 | `str` | `test-output/mobile-browser-visual` |
| `mobile_browser.visual.mismatch_threshold_percent` | 26 | `float` | `0.1` |
| `mobile_browser.visual.create_baseline_if_missing` | 27 | `bool` | `true` |
| `mobile_browser.visual.attach_artifacts_to_report` | 28 | `bool` | `true` |

## `src/test/resources/mobile_browser/config/mobile-browser-profiles.yml`

Source: [`src/test/resources/mobile_browser/config/mobile-browser-profiles.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.browser_engine` | 10 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.platform` | 11 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.device_category` | 12 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.orientation` | 13 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.viewport_width` | 14 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.viewport_height` | 15 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.screen_width` | 16 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.screen_height` | 17 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.device_scale_factor` | 18 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.is_mobile` | 19 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.has_touch` | 20 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Safari.user_agent` | 21 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.browser_engine` | 23 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.platform` | 24 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.device_category` | 25 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.orientation` | 26 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.viewport_width` | 27 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.viewport_height` | 28 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.screen_width` | 29 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.screen_height` | 30 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.device_scale_factor` | 31 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.is_mobile` | 32 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.has_touch` | 33 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 17 Pro Max Chrome.user_agent` | 34 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.browser_engine` | 36 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.platform` | 37 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.device_category` | 38 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.orientation` | 39 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.viewport_width` | 40 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.viewport_height` | 41 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.screen_width` | 42 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.screen_height` | 43 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.device_scale_factor` | 44 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.is_mobile` | 45 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.has_touch` | 46 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 16 Pro Safari.user_agent` | 47 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.browser_engine` | 49 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.platform` | 50 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.device_category` | 51 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.orientation` | 52 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.viewport_width` | 53 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.viewport_height` | 54 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.screen_width` | 55 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.screen_height` | 56 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.device_scale_factor` | 57 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.is_mobile` | 58 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.has_touch` | 59 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone 15 Safari.user_agent` | 60 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.browser_engine` | 62 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.platform` | 63 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.device_category` | 64 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.orientation` | 65 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.viewport_width` | 66 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.viewport_height` | 67 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.screen_width` | 68 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.screen_height` | 69 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.device_scale_factor` | 70 | `int` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.is_mobile` | 71 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.has_touch` | 72 | `bool` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPhone SE Safari.user_agent` | 73 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.browser_engine` | 75 | `str` | `chromium` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.platform` | 76 | `str` | `Android` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.device_category` | 77 | `str` | `phone` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.orientation` | 78 | `str` | `portrait` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.viewport_width` | 79 | `int` | `412` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.viewport_height` | 80 | `int` | `915` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.screen_width` | 81 | `int` | `412` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.screen_height` | 82 | `int` | `915` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.device_scale_factor` | 83 | `float` | `3.5` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.is_mobile` | 84 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.has_touch` | 85 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy S25 Ultra Chrome.user_agent` | 86 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Galaxy S24 Chrome.browser_engine` | 88 | `str` | `chromium` |
| `mobile_browser_profiles.Galaxy S24 Chrome.platform` | 89 | `str` | `Android` |
| `mobile_browser_profiles.Galaxy S24 Chrome.device_category` | 90 | `str` | `phone` |
| `mobile_browser_profiles.Galaxy S24 Chrome.orientation` | 91 | `str` | `portrait` |
| `mobile_browser_profiles.Galaxy S24 Chrome.viewport_width` | 92 | `int` | `384` |
| `mobile_browser_profiles.Galaxy S24 Chrome.viewport_height` | 93 | `int` | `854` |
| `mobile_browser_profiles.Galaxy S24 Chrome.screen_width` | 94 | `int` | `384` |
| `mobile_browser_profiles.Galaxy S24 Chrome.screen_height` | 95 | `int` | `854` |
| `mobile_browser_profiles.Galaxy S24 Chrome.device_scale_factor` | 96 | `int` | `3` |
| `mobile_browser_profiles.Galaxy S24 Chrome.is_mobile` | 97 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy S24 Chrome.has_touch` | 98 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy S24 Chrome.user_agent` | 99 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.browser_engine` | 101 | `str` | `chromium` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.platform` | 102 | `str` | `Android` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.device_category` | 103 | `str` | `phone` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.orientation` | 104 | `str` | `portrait` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.viewport_width` | 105 | `int` | `412` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.viewport_height` | 106 | `int` | `915` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.screen_width` | 107 | `int` | `412` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.screen_height` | 108 | `int` | `915` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.device_scale_factor` | 109 | `int` | `3` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.is_mobile` | 110 | `bool` | `true` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.has_touch` | 111 | `bool` | `true` |
| `mobile_browser_profiles.Pixel 9 Pro Chrome.user_agent` | 112 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Pixel 8 Chrome.browser_engine` | 114 | `str` | `chromium` |
| `mobile_browser_profiles.Pixel 8 Chrome.platform` | 115 | `str` | `Android` |
| `mobile_browser_profiles.Pixel 8 Chrome.device_category` | 116 | `str` | `phone` |
| `mobile_browser_profiles.Pixel 8 Chrome.orientation` | 117 | `str` | `portrait` |
| `mobile_browser_profiles.Pixel 8 Chrome.viewport_width` | 118 | `int` | `412` |
| `mobile_browser_profiles.Pixel 8 Chrome.viewport_height` | 119 | `int` | `915` |
| `mobile_browser_profiles.Pixel 8 Chrome.screen_width` | 120 | `int` | `412` |
| `mobile_browser_profiles.Pixel 8 Chrome.screen_height` | 121 | `int` | `915` |
| `mobile_browser_profiles.Pixel 8 Chrome.device_scale_factor` | 122 | `float` | `2.625` |
| `mobile_browser_profiles.Pixel 8 Chrome.is_mobile` | 123 | `bool` | `true` |
| `mobile_browser_profiles.Pixel 8 Chrome.has_touch` | 124 | `bool` | `true` |
| `mobile_browser_profiles.Pixel 8 Chrome.user_agent` | 125 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.OnePlus 13 Chrome.browser_engine` | 127 | `str` | `chromium` |
| `mobile_browser_profiles.OnePlus 13 Chrome.platform` | 128 | `str` | `Android` |
| `mobile_browser_profiles.OnePlus 13 Chrome.device_category` | 129 | `str` | `phone` |
| `mobile_browser_profiles.OnePlus 13 Chrome.orientation` | 130 | `str` | `portrait` |
| `mobile_browser_profiles.OnePlus 13 Chrome.viewport_width` | 131 | `int` | `412` |
| `mobile_browser_profiles.OnePlus 13 Chrome.viewport_height` | 132 | `int` | `919` |
| `mobile_browser_profiles.OnePlus 13 Chrome.screen_width` | 133 | `int` | `412` |
| `mobile_browser_profiles.OnePlus 13 Chrome.screen_height` | 134 | `int` | `919` |
| `mobile_browser_profiles.OnePlus 13 Chrome.device_scale_factor` | 135 | `float` | `3.5` |
| `mobile_browser_profiles.OnePlus 13 Chrome.is_mobile` | 136 | `bool` | `true` |
| `mobile_browser_profiles.OnePlus 13 Chrome.has_touch` | 137 | `bool` | `true` |
| `mobile_browser_profiles.OnePlus 13 Chrome.user_agent` | 138 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPad Pro 13 Safari.browser_engine` | 140 | `str` | `webkit` |
| `mobile_browser_profiles.iPad Pro 13 Safari.platform` | 141 | `str` | `iOS` |
| `mobile_browser_profiles.iPad Pro 13 Safari.device_category` | 142 | `str` | `tablet` |
| `mobile_browser_profiles.iPad Pro 13 Safari.orientation` | 143 | `str` | `portrait` |
| `mobile_browser_profiles.iPad Pro 13 Safari.viewport_width` | 144 | `int` | `1032` |
| `mobile_browser_profiles.iPad Pro 13 Safari.viewport_height` | 145 | `int` | `1376` |
| `mobile_browser_profiles.iPad Pro 13 Safari.screen_width` | 146 | `int` | `1032` |
| `mobile_browser_profiles.iPad Pro 13 Safari.screen_height` | 147 | `int` | `1376` |
| `mobile_browser_profiles.iPad Pro 13 Safari.device_scale_factor` | 148 | `int` | `2` |
| `mobile_browser_profiles.iPad Pro 13 Safari.is_mobile` | 149 | `bool` | `true` |
| `mobile_browser_profiles.iPad Pro 13 Safari.has_touch` | 150 | `bool` | `true` |
| `mobile_browser_profiles.iPad Pro 13 Safari.user_agent` | 151 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPad Pro 11 Safari.browser_engine` | 153 | `str` | `webkit` |
| `mobile_browser_profiles.iPad Pro 11 Safari.platform` | 154 | `str` | `iOS` |
| `mobile_browser_profiles.iPad Pro 11 Safari.device_category` | 155 | `str` | `tablet` |
| `mobile_browser_profiles.iPad Pro 11 Safari.orientation` | 156 | `str` | `portrait` |
| `mobile_browser_profiles.iPad Pro 11 Safari.viewport_width` | 157 | `int` | `834` |
| `mobile_browser_profiles.iPad Pro 11 Safari.viewport_height` | 158 | `int` | `1194` |
| `mobile_browser_profiles.iPad Pro 11 Safari.screen_width` | 159 | `int` | `834` |
| `mobile_browser_profiles.iPad Pro 11 Safari.screen_height` | 160 | `int` | `1194` |
| `mobile_browser_profiles.iPad Pro 11 Safari.device_scale_factor` | 161 | `int` | `2` |
| `mobile_browser_profiles.iPad Pro 11 Safari.is_mobile` | 162 | `bool` | `true` |
| `mobile_browser_profiles.iPad Pro 11 Safari.has_touch` | 163 | `bool` | `true` |
| `mobile_browser_profiles.iPad Pro 11 Safari.user_agent` | 164 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPad Air Safari.browser_engine` | 166 | `str` | `webkit` |
| `mobile_browser_profiles.iPad Air Safari.platform` | 167 | `str` | `iOS` |
| `mobile_browser_profiles.iPad Air Safari.device_category` | 168 | `str` | `tablet` |
| `mobile_browser_profiles.iPad Air Safari.orientation` | 169 | `str` | `portrait` |
| `mobile_browser_profiles.iPad Air Safari.viewport_width` | 170 | `int` | `820` |
| `mobile_browser_profiles.iPad Air Safari.viewport_height` | 171 | `int` | `1180` |
| `mobile_browser_profiles.iPad Air Safari.screen_width` | 172 | `int` | `820` |
| `mobile_browser_profiles.iPad Air Safari.screen_height` | 173 | `int` | `1180` |
| `mobile_browser_profiles.iPad Air Safari.device_scale_factor` | 174 | `int` | `2` |
| `mobile_browser_profiles.iPad Air Safari.is_mobile` | 175 | `bool` | `true` |
| `mobile_browser_profiles.iPad Air Safari.has_touch` | 176 | `bool` | `true` |
| `mobile_browser_profiles.iPad Air Safari.user_agent` | 177 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPad Mini Safari.browser_engine` | 179 | `str` | `webkit` |
| `mobile_browser_profiles.iPad Mini Safari.platform` | 180 | `str` | `iOS` |
| `mobile_browser_profiles.iPad Mini Safari.device_category` | 181 | `str` | `tablet` |
| `mobile_browser_profiles.iPad Mini Safari.orientation` | 182 | `str` | `portrait` |
| `mobile_browser_profiles.iPad Mini Safari.viewport_width` | 183 | `int` | `744` |
| `mobile_browser_profiles.iPad Mini Safari.viewport_height` | 184 | `int` | `1133` |
| `mobile_browser_profiles.iPad Mini Safari.screen_width` | 185 | `int` | `744` |
| `mobile_browser_profiles.iPad Mini Safari.screen_height` | 186 | `int` | `1133` |
| `mobile_browser_profiles.iPad Mini Safari.device_scale_factor` | 187 | `int` | `2` |
| `mobile_browser_profiles.iPad Mini Safari.is_mobile` | 188 | `bool` | `true` |
| `mobile_browser_profiles.iPad Mini Safari.has_touch` | 189 | `bool` | `true` |
| `mobile_browser_profiles.iPad Mini Safari.user_agent` | 190 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.iPad 10th Gen Safari.browser_engine` | 192 | `str` | `webkit` |
| `mobile_browser_profiles.iPad 10th Gen Safari.platform` | 193 | `str` | `iOS` |
| `mobile_browser_profiles.iPad 10th Gen Safari.device_category` | 194 | `str` | `tablet` |
| `mobile_browser_profiles.iPad 10th Gen Safari.orientation` | 195 | `str` | `portrait` |
| `mobile_browser_profiles.iPad 10th Gen Safari.viewport_width` | 196 | `int` | `820` |
| `mobile_browser_profiles.iPad 10th Gen Safari.viewport_height` | 197 | `int` | `1180` |
| `mobile_browser_profiles.iPad 10th Gen Safari.screen_width` | 198 | `int` | `820` |
| `mobile_browser_profiles.iPad 10th Gen Safari.screen_height` | 199 | `int` | `1180` |
| `mobile_browser_profiles.iPad 10th Gen Safari.device_scale_factor` | 200 | `int` | `2` |
| `mobile_browser_profiles.iPad 10th Gen Safari.is_mobile` | 201 | `bool` | `true` |
| `mobile_browser_profiles.iPad 10th Gen Safari.has_touch` | 202 | `bool` | `true` |
| `mobile_browser_profiles.iPad 10th Gen Safari.user_agent` | 203 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.browser_engine` | 205 | `str` | `chromium` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.platform` | 206 | `str` | `Android` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.device_category` | 207 | `str` | `tablet` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.orientation` | 208 | `str` | `landscape` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.viewport_width` | 209 | `int` | `1480` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.viewport_height` | 210 | `int` | `924` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.screen_width` | 211 | `int` | `1480` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.screen_height` | 212 | `int` | `924` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.device_scale_factor` | 213 | `int` | `2` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.is_mobile` | 214 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.has_touch` | 215 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy Tab S10 Ultra Chrome.user_agent` | 216 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.browser_engine` | 218 | `str` | `chromium` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.platform` | 219 | `str` | `Android` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.device_category` | 220 | `str` | `tablet` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.orientation` | 221 | `str` | `landscape` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.viewport_width` | 222 | `int` | `1280` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.viewport_height` | 223 | `int` | `800` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.screen_width` | 224 | `int` | `1280` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.screen_height` | 225 | `int` | `800` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.device_scale_factor` | 226 | `int` | `2` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.is_mobile` | 227 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.has_touch` | 228 | `bool` | `true` |
| `mobile_browser_profiles.Galaxy Tab S9 Chrome.user_agent` | 229 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Pixel Tablet Chrome.browser_engine` | 231 | `str` | `chromium` |
| `mobile_browser_profiles.Pixel Tablet Chrome.platform` | 232 | `str` | `Android` |
| `mobile_browser_profiles.Pixel Tablet Chrome.device_category` | 233 | `str` | `tablet` |
| `mobile_browser_profiles.Pixel Tablet Chrome.orientation` | 234 | `str` | `landscape` |
| `mobile_browser_profiles.Pixel Tablet Chrome.viewport_width` | 235 | `int` | `1280` |
| `mobile_browser_profiles.Pixel Tablet Chrome.viewport_height` | 236 | `int` | `800` |
| `mobile_browser_profiles.Pixel Tablet Chrome.screen_width` | 237 | `int` | `1280` |
| `mobile_browser_profiles.Pixel Tablet Chrome.screen_height` | 238 | `int` | `800` |
| `mobile_browser_profiles.Pixel Tablet Chrome.device_scale_factor` | 239 | `int` | `2` |
| `mobile_browser_profiles.Pixel Tablet Chrome.is_mobile` | 240 | `bool` | `true` |
| `mobile_browser_profiles.Pixel Tablet Chrome.has_touch` | 241 | `bool` | `true` |
| `mobile_browser_profiles.Pixel Tablet Chrome.user_agent` | 242 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.browser_engine` | 244 | `str` | `chromium` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.platform` | 245 | `str` | `Android` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.device_category` | 246 | `str` | `tablet` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.orientation` | 247 | `str` | `landscape` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.viewport_width` | 248 | `int` | `1365` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.viewport_height` | 249 | `int` | `853` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.screen_width` | 250 | `int` | `1365` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.screen_height` | 251 | `int` | `853` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.device_scale_factor` | 252 | `int` | `2` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.is_mobile` | 253 | `bool` | `true` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.has_touch` | 254 | `bool` | `true` |
| `mobile_browser_profiles.Lenovo Tab P12 Chrome.user_agent` | 255 | `str` | `<environment- or user-supplied value>` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.browser_engine` | 257 | `str` | `chromium` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.platform` | 258 | `str` | `Android` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.device_category` | 259 | `str` | `tablet` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.orientation` | 260 | `str` | `landscape` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.viewport_width` | 261 | `int` | `1366` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.viewport_height` | 262 | `int` | `853` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.screen_width` | 263 | `int` | `1366` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.screen_height` | 264 | `int` | `853` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.device_scale_factor` | 265 | `int` | `2` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.is_mobile` | 266 | `bool` | `true` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.has_touch` | 267 | `bool` | `true` |
| `mobile_browser_profiles.Xiaomi Pad 7 Chrome.user_agent` | 268 | `str` | `<environment- or user-supplied value>` |

## `src/test/resources/performance/config/performance-config.yml`

Source: [`src/test/resources/performance/config/performance-config.yml`](../../../src/test/resources/performance/config/performance-config.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `performance.defaults.protocol` | 3 | `str` | `https` |
| `performance.defaults.host` | 4 | `str` | `<environment- or user-supplied value>` |
| `performance.defaults.port` | 5 | `int` | `443` |
| `performance.defaults.users` | 6 | `int` | `<environment- or user-supplied value>` |
| `performance.defaults.rampUpSeconds` | 7 | `int` | `5` |
| `performance.defaults.holdSeconds` | 8 | `int` | `10` |
| `performance.defaults.iterations` | 9 | `int` | `1` |
| `performance.reporting.resultsFolder` | 12 | `str` | `test-output-performance-test/results` |
| `performance.reporting.dashboardFolder` | 13 | `str` | `test-output-performance-test/dashboard` |
| `performance.assertions.maxErrorPercent` | 16 | `float` | `5.0` |
| `performance.assertions.maxAvgResponseTimeMs` | 17 | `int` | `5000` |
| `performance.assertions.maxP95ResponseTimeMs` | 18 | `int` | `7000` |

## `src/test/resources/performance/payloads/yaml/performance-payloads.yml`

Source: [`src/test/resources/performance/payloads/yaml/performance-payloads.yml`](../../../src/test/resources/performance/payloads/yaml/performance-payloads.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `performance.payloads.createCustomer` | 3 | `str` | `{   "firstName": "John",   "lastName": "Smith",   "email": "john.sm…` |
| `performance.payloads.updateCustomer` | 10 | `str` | `{   "customerId": 1001,   "status": "updated" }` |
| `performance.payloads.secureCreateCustomer` | 16 | `str` | `{   "firstName": "Secure",   "lastName": "User",   "role": "authent…` |

## `src/test/resources/queries/db_queries.yml`

Source: [`src/test/resources/queries/db_queries.yml`](../../../src/test/resources/queries/db_queries.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `users.get_user_by_email` | 11 | `str` | `<environment- or user-supplied value>` |
| `users.get_user_by_id` | 12 | `str` | `<environment- or user-supplied value>` |
| `users.get_all_active_users` | 13 | `str` | `<environment- or user-supplied value>` |
| `users.insert_new_user` | 14 | `str` | `<environment- or user-supplied value>` |
| `users.update_user_status` | 15 | `str` | `<environment- or user-supplied value>` |
| `users.delete_user_by_email` | 16 | `str` | `<environment- or user-supplied value>` |
| `products.get_product_by_sku` | 20 | `str` | `SELECT * FROM public.products WHERE sku = ?;` |
| `products.get_product_stock_level` | 21 | `str` | `SELECT stock_quantity FROM public.inventory WHERE product_id = ?;` |
| `products.update_stock_level` | 22 | `str` | `UPDATE public.inventory SET stock_quantity = ?, last_updated_at = N…` |
| `stored_procedures.calculate_customer_discount` | 27 | `str` | `{CALL CalculateCustomerDiscount(?)}` |

## `src/test/resources/testng.xml`

Source: [`src/test/resources/testng.xml`](../../../src/test/resources/testng.xml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `suite.name` | 2 | `XML attribute` | `PTAF Mobile Test Suite` |
| `test.class` | 5 | `XML class` | `com.ptaf.runner.TestRunner` |

## `src/test/resources/ui_performance/config/ui_performance-browser-contract.yml`

Source: [`src/test/resources/ui_performance/config/ui_performance-browser-contract.yml`](../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `ui_performance.enabled` | 2 | `bool` | `true` |
| `ui_performance.active_profile` | 3 | `str` | `load` |
| `ui_performance.target.protocol` | 5 | `str` | `http` |
| `ui_performance.target.host` | 6 | `str` | `<environment- or user-supplied value>` |
| `ui_performance.target.port` | 7 | `int` | `18765` |
| `ui_performance.target.base_path` | 8 | `str` | `(empty string)` |
| `ui_performance.target.routes.ready` | 10 | `str` | `/ready` |
| `ui_performance.execution.synchronized_start_timeout_ms` | 12 | `int` | `30000` |
| `ui_performance.execution.between_iterations_ms` | 13 | `int` | `0` |
| `ui_performance.profiles.load.type` | 16 | `str` | `load` |
| `ui_performance.profiles.load.stages[0].name` | 18 | `str` | `concurrent-contract` |
| `ui_performance.profiles.load.stages[0].users` | 19 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.load.stages[0].ramp_up_seconds` | 20 | `int` | `0` |
| `ui_performance.profiles.load.stages[0].hold_seconds` | 21 | `int` | `0` |
| `ui_performance.profiles.load.stages[0].iterations_per_user` | 22 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.browser.headless` | 24 | `bool` | `true` |
| `ui_performance.browser.ignore_https_errors` | 25 | `bool` | `false` |
| `ui_performance.browser.isolation` | 26 | `str` | `process` |
| `ui_performance.browser.action_timeout_ms` | 27 | `int` | `5000` |
| `ui_performance.browser.navigation_timeout_ms` | 28 | `int` | `5000` |
| `ui_performance.data.use_csv` | 30 | `bool` | `false` |
| `ui_performance.data.users_csv` | 31 | `str` | `<environment- or user-supplied value>` |
| `ui_performance.data.allow_user_reuse` | 32 | `bool` | `<environment- or user-supplied value>` |
| `ui_performance.evidence.capture_failure_screenshots` | 34 | `bool` | `false` |
| `ui_performance.evidence.capture_console_errors` | 35 | `bool` | `true` |
| `ui_performance.thresholds.maximum_failure_rate_percent` | 37 | `float` | `0.0` |
| `ui_performance.thresholds.maximum_average_journey_duration_ms` | 38 | `int` | `5000` |
| `ui_performance.thresholds.maximum_p95_journey_duration_ms` | 39 | `int` | `5000` |
| `ui_performance.safety.max_virtual_users` | 41 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.reporting.output_directory` | 43 | `str` | `target/ui-performance-browser-contract` |
| `ui_performance.reporting.existing_performance_reporter_enabled` | 44 | `bool` | `true` |
| `ui_performance.reporting.html_enabled` | 45 | `bool` | `true` |
| `ui_performance.reporting.pdf_enabled` | 46 | `bool` | `true` |
| `ui_performance.reporting.csv_enabled` | 47 | `bool` | `true` |
| `ui_performance.reporting.json_enabled` | 48 | `bool` | `true` |

## `src/test/resources/ui_performance/config/ui_performance-config.yml`

Source: [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../../src/test/resources/ui_performance/config/ui_performance-config.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `ui_performance.enabled` | 20 | `bool` | `true` |
| `ui_performance.active_profile` | 23 | `str` | `load` |
| `ui_performance.target.protocol` | 29 | `str` | `https` |
| `ui_performance.target.host` | 30 | `str` | `<environment- or user-supplied value>` |
| `ui_performance.target.port` | 31 | `int` | `443` |
| `ui_performance.target.base_path` | 32 | `str` | `(empty string)` |
| `ui_performance.target.routes.test_harness` | 34 | `str` | `/workspaces-preprod/servlet/SmartForm.html?formCode=testharness2` |
| `ui_performance.profiles.load.type` | 46 | `str` | `load` |
| `ui_performance.profiles.load.stages[0].name` | 48 | `str` | `normal-load` |
| `ui_performance.profiles.load.stages[0].users` | 49 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.load.stages[0].ramp_up_seconds` | 50 | `int` | `0` |
| `ui_performance.profiles.load.stages[0].hold_seconds` | 51 | `int` | `0` |
| `ui_performance.profiles.load.stages[0].iterations_per_user` | 52 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.stress.type` | 55 | `str` | `stress` |
| `ui_performance.profiles.stress.stages[0].name` | 57 | `str` | `stress-2-users` |
| `ui_performance.profiles.stress.stages[0].users` | 58 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.stress.stages[0].ramp_up_seconds` | 59 | `int` | `0` |
| `ui_performance.profiles.stress.stages[0].hold_seconds` | 60 | `int` | `60` |
| `ui_performance.profiles.stress.stages[0].iterations_per_user` | 61 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.stress.stages[1].name` | 62 | `str` | `stress-5-users` |
| `ui_performance.profiles.stress.stages[1].users` | 63 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.stress.stages[1].ramp_up_seconds` | 64 | `int` | `5` |
| `ui_performance.profiles.stress.stages[1].hold_seconds` | 65 | `int` | `60` |
| `ui_performance.profiles.stress.stages[1].iterations_per_user` | 66 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.stress.stages[2].name` | 67 | `str` | `stress-10-users` |
| `ui_performance.profiles.stress.stages[2].users` | 68 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.stress.stages[2].ramp_up_seconds` | 69 | `int` | `5` |
| `ui_performance.profiles.stress.stages[2].hold_seconds` | 70 | `int` | `60` |
| `ui_performance.profiles.stress.stages[2].iterations_per_user` | 71 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.spike.type` | 74 | `str` | `spike` |
| `ui_performance.profiles.spike.stages[0].name` | 76 | `str` | `baseline` |
| `ui_performance.profiles.spike.stages[0].users` | 77 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.spike.stages[0].ramp_up_seconds` | 78 | `int` | `0` |
| `ui_performance.profiles.spike.stages[0].hold_seconds` | 79 | `int` | `30` |
| `ui_performance.profiles.spike.stages[0].iterations_per_user` | 80 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.spike.stages[1].name` | 81 | `str` | `immediate-spike` |
| `ui_performance.profiles.spike.stages[1].users` | 82 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.spike.stages[1].ramp_up_seconds` | 83 | `int` | `0` |
| `ui_performance.profiles.spike.stages[1].hold_seconds` | 84 | `int` | `60` |
| `ui_performance.profiles.spike.stages[1].iterations_per_user` | 85 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.spike.stages[2].name` | 86 | `str` | `recovery` |
| `ui_performance.profiles.spike.stages[2].users` | 87 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.spike.stages[2].ramp_up_seconds` | 88 | `int` | `0` |
| `ui_performance.profiles.spike.stages[2].hold_seconds` | 89 | `int` | `30` |
| `ui_performance.profiles.spike.stages[2].iterations_per_user` | 90 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.soak.type` | 93 | `str` | `soak` |
| `ui_performance.profiles.soak.stages[0].name` | 95 | `str` | `sustained-5-users` |
| `ui_performance.profiles.soak.stages[0].users` | 96 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.profiles.soak.stages[0].ramp_up_seconds` | 97 | `int` | `10` |
| `ui_performance.profiles.soak.stages[0].hold_seconds` | 98 | `int` | `900` |
| `ui_performance.profiles.soak.stages[0].iterations_per_user` | 99 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.browser.headless` | 107 | `bool` | `true` |
| `ui_performance.browser.ignore_https_errors` | 108 | `bool` | `true` |
| `ui_performance.browser.isolation` | 110 | `str` | `process` |
| `ui_performance.browser.user_agent` | 112 | `str` | `<environment- or user-supplied value>` |
| `ui_performance.browser.action_timeout_ms` | 115 | `int` | `60000` |
| `ui_performance.browser.navigation_timeout_ms` | 116 | `int` | `90000` |
| `ui_performance.execution.synchronized_start_timeout_ms` | 120 | `int` | `120000` |
| `ui_performance.execution.between_iterations_ms` | 122 | `int` | `500` |
| `ui_performance.data.use_csv` | 129 | `bool` | `true` |
| `ui_performance.data.users_csv` | 130 | `str` | `<environment- or user-supplied value>` |
| `ui_performance.data.allow_user_reuse` | 132 | `bool` | `<environment- or user-supplied value>` |
| `ui_performance.evidence.capture_failure_screenshots` | 138 | `bool` | `true` |
| `ui_performance.evidence.capture_console_errors` | 139 | `bool` | `true` |
| `ui_performance.thresholds.maximum_failure_rate_percent` | 142 | `float` | `20.0` |
| `ui_performance.thresholds.maximum_average_journey_duration_ms` | 143 | `int` | `45000` |
| `ui_performance.thresholds.maximum_p95_journey_duration_ms` | 144 | `int` | `60000` |
| `ui_performance.safety.max_virtual_users` | 149 | `int` | `<environment- or user-supplied value>` |
| `ui_performance.reporting.output_directory` | 152 | `str` | `test-output/ui_performance` |
| `ui_performance.reporting.existing_performance_reporter_enabled` | 153 | `bool` | `true` |
| `ui_performance.reporting.html_enabled` | 154 | `bool` | `true` |
| `ui_performance.reporting.pdf_enabled` | 155 | `bool` | `true` |
| `ui_performance.reporting.csv_enabled` | 156 | `bool` | `true` |
| `ui_performance.reporting.json_enabled` | 157 | `bool` | `true` |

## `src/test/resources/ui_performance/locators/ui_performance-locators.yml`

Source: [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`](../../../src/test/resources/ui_performance/locators/ui_performance-locators.yml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `ui_performance_locators.test_harness.page_ready` | 8 | `str` | `CSS_body` |
| `ui_performance_locators.test_harness.email` | 9 | `str` | `<environment- or user-supplied value>` |
| `ui_performance_locators.test_harness.phone_number` | 10 | `str` | `<environment- or user-supplied value>` |
| `ui_performance_locators.test_harness.product_group` | 11 | `str` | `XPATH_(//label[text()='Product Group']/../div/select)[1]` |
| `ui_performance_locators.test_harness.product_name` | 12 | `str` | `XPATH_(//label[text()='Consumer Deposit Products']/../div/select)[1]` |
| `ui_performance_locators.test_harness.create_url` | 13 | `str` | `<environment- or user-supplied value>` |
| `ui_performance_locators.test_harness.generated_url` | 14 | `str` | `<environment- or user-supplied value>` |
| `ui_performance_locators.test_harness.open_url` | 15 | `str` | `<environment- or user-supplied value>` |

## `src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml`

Source: [`src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml`](../../../src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `suite.name` | 2 | `XML attribute` | `FNB-ETAF UI Performance Browser Contract` |
| `test.class` | 5 | `XML class` | `com.ptaf.ui_performance.UiPerformanceConcurrentBrowserIntegrationTest` |

## `src/test/resources/ui_performance/testng-ui_performance.xml`

Source: [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../../src/test/resources/ui_performance/testng-ui_performance.xml)

| Setting key or entry | Source line | Value type | Safe configured-value preview |
|---|---:|---|---|
| `suite.name` | 7 | `XML attribute` | `FNB-ETAF eStore UI Performance Suite` |
| `test.class` | 10 | `XML class` | `com.ptaf.ui_performance.runners.UiPerformanceRunner` |
| `test.class` | 12 | `XML class` | `com.ptaf.ui_performance.UiPerformanceModuleContractTest` |
| `test.class` | 14 | `XML class` | `com.ptaf.ui_performance.UiPerformanceStartGateTest` |

## Regeneration

Run the following command from the repository root after a resource configuration change:

```bash
python3 scripts/generate_configuration_index.py
```

The generator changes only this appendix. It does not alter runtime configuration, tests, or source code.

## Source references

- **[1]** [`scripts/generate_configuration_index.py`](../../../scripts/generate_configuration_index.py) — safe resource-setting index generator.
- **[2]** [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) — shared configuration accessor.
- **[3]** [`src/main/java/com/ptaf/utils/YamlReader.java`](../../../src/main/java/com/ptaf/utils/YamlReader.java) — shared YAML reader.

## References

[1]: ../../../scripts/generate_configuration_index.py "FNB-ETAF configuration setting index generator"
[2]: ../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java "FNB-ETAF shared configuration accessor"
[3]: ../../../src/main/java/com/ptaf/utils/YamlReader.java "FNB-ETAF shared YAML reader"
