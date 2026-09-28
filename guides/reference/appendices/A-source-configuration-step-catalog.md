# Appendix A — Source, Configuration, Runner, and Step Catalog

**Generated from the active repository:** `scripts/generate_reference_catalog.py`.

This appendix is an implementation navigation index. It is intentionally generated from the current source tree so a reviewer can locate the responsible class or resource without relying on memory. It does not replace the explanatory chapters in [`../README.md`](../Technical%20Reference.md).

> **How to use this catalog:** Start with the relevant chapter, then use the source link in this appendix to open the actual class, YAML resource, TestNG suite, or Gherkin feature. Source files remain authoritative whenever this appendix and implementation differ.

## Contents

- [Repository counts](#repository-counts)
- [Build and primary entry points](#build-and-primary-entry-points)
- [Configuration and resource catalog](#configuration-and-resource-catalog)
- [Feature catalog](#feature-catalog)
- [Step-definition catalog](#step-definition-catalog)
- [Production Java source catalog](#production-java-source-catalog)
- [Test and runner Java source catalog](#test-and-runner-java-source-catalog)
- [Maintenance and regeneration](#maintenance-and-regeneration)

## Repository counts

| Item | Count |
|---|---:|
| Production Java classes | 119 |
| Test Java classes | 25 |
| Configuration and suite resources | 33 |
| Gherkin feature files | 30 |
| Step-definition classes | 13 |
| Runner classes | 9 |

## Build and primary entry points

- [`pom.xml`](../../../pom.xml)
- [`src/test/resources/testng.xml`](../../../src/test/resources/testng.xml)
- [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../../src/test/resources/ui_performance/testng-ui_performance.xml)
- [`src/test/java/com/ptaf/runner/TestRunner.java`](../../../src/test/java/com/ptaf/runner/TestRunner.java)
- [`src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java)

See [Chapter 01](../chapters/01-foundation-build-runners.md) for the build and runner explanation. See [Chapter 11](../chapters/11-ui-performance-load-stress.md) for the isolated browser-load route.

## Configuration and resource catalog

The following list includes YAML, XML suite, and property resources below `src/test/resources`. YAML entries show only top-level keys because nested keys are documented in the dedicated chapters and source readers.

| Resource | Resource type | Top-level YAML keys or role |
|---|---|---|
| [`src/test/resources/api_requests/api_requests.yml`](../../../src/test/resources/api_requests/api_requests.yml) | YAML configuration | `jsonplaceholder_requests`, `crm_requests` |
| [`src/test/resources/config/config.yml`](../../../src/test/resources/config/config.yml) | YAML configuration | `browser`, `maximize_browser`, `headless`, `ignoreHTTPSErrors`, `time_to_wait_in_seconds`, `runtimeWait`, `videoCapture`, `excelDocumentLocation`, `downloadDocument`, `tool_qa_url`, `HARNESS_PREPROD_STAGE`, `database`, … |
| [`src/test/resources/cucumber.properties`](../../../src/test/resources/cucumber.properties) | Properties | Properties configuration; inspect file values and owning reader. |
| [`src/test/resources/data/sample_order_response.xml`](../../../src/test/resources/data/sample_order_response.xml) | XML configuration | XML suite or report configuration; inspect enabled listeners/suites. |
| [`src/test/resources/elements/eStore_elements.yml`](../../../src/test/resources/elements/eStore_elements.yml) | YAML configuration | `elements` |
| [`src/test/resources/elements/google.yml`](../../../src/test/resources/elements/google.yml) | YAML configuration | `elements` |
| [`src/test/resources/elements/homepage.yml`](../../../src/test/resources/elements/homepage.yml) | YAML configuration | `elements` |
| [`src/test/resources/elements/landingpage.yml`](../../../src/test/resources/elements/landingpage.yml) | YAML configuration | `elements` |
| [`src/test/resources/elements/login.yml`](../../../src/test/resources/elements/login.yml) | YAML configuration | `elements` |
| [`src/test/resources/elements/panda_page.yml`](../../../src/test/resources/elements/panda_page.yml) | YAML configuration | `elements` |
| [`src/test/resources/elements/personal_bank.yml`](../../../src/test/resources/elements/personal_bank.yml) | YAML configuration | `elements` |
| [`src/test/resources/extent-config.xml`](../../../src/test/resources/extent-config.xml) | XML configuration | XML suite or report configuration; inspect enabled listeners/suites. |
| [`src/test/resources/extent.properties`](../../../src/test/resources/extent.properties) | Properties | Properties configuration; inspect file values and owning reader. |
| [`src/test/resources/mobile/config/mobile-browser-config.yml`](../../../src/test/resources/mobile/config/mobile-browser-config.yml) | YAML configuration | `mobile_browser_appium` |
| [`src/test/resources/mobile/config/mobile-config.yml`](../../../src/test/resources/mobile/config/mobile-config.yml) | YAML configuration | `mobile` |
| [`src/test/resources/mobile/config/mobile-native-config.yml`](../../../src/test/resources/mobile/config/mobile-native-config.yml) | YAML configuration | `mobile` |
| [`src/test/resources/mobile/elements/fnb_elements.yml`](../../../src/test/resources/mobile/elements/fnb_elements.yml) | YAML configuration | `mobile_elements` |
| [`src/test/resources/mobile/elements/google_mobile_browser_elements.yml`](../../../src/test/resources/mobile/elements/google_mobile_browser_elements.yml) | YAML configuration | `mobile_elements` |
| [`src/test/resources/mobile/elements/mobile_permissions.yml`](../../../src/test/resources/mobile/elements/mobile_permissions.yml) | YAML configuration | `mobile_elements` |
| [`src/test/resources/mobile/elements/safari_browser_elements.yml`](../../../src/test/resources/mobile/elements/safari_browser_elements.yml) | YAML configuration | `mobile_elements` |
| [`src/test/resources/mobile/elements/theapp_elements.yml`](../../../src/test/resources/mobile/elements/theapp_elements.yml) | YAML configuration | `mobile_elements` |
| [`src/test/resources/mobile/elements/unified_locator_examples.yml`](../../../src/test/resources/mobile/elements/unified_locator_examples.yml) | YAML configuration | `mobile_elements` |
| [`src/test/resources/mobile_browser/config/mobile-browser-execution.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-execution.yml) | YAML configuration | `mobile_browser` |
| [`src/test/resources/mobile_browser/config/mobile-browser-profiles.yml`](../../../src/test/resources/mobile_browser/config/mobile-browser-profiles.yml) | YAML configuration | `mobile_browser_profiles` |
| [`src/test/resources/performance/config/performance-config.yml`](../../../src/test/resources/performance/config/performance-config.yml) | YAML configuration | `performance` |
| [`src/test/resources/performance/payloads/yaml/performance-payloads.yml`](../../../src/test/resources/performance/payloads/yaml/performance-payloads.yml) | YAML configuration | `performance` |
| [`src/test/resources/queries/db_queries.yml`](../../../src/test/resources/queries/db_queries.yml) | YAML configuration | `users`, `products`, `stored_procedures` |
| [`src/test/resources/testng.xml`](../../../src/test/resources/testng.xml) | XML configuration | XML suite or report configuration; inspect enabled listeners/suites. |
| [`src/test/resources/ui_performance/config/ui_performance-browser-contract.yml`](../../../src/test/resources/ui_performance/config/ui_performance-browser-contract.yml) | YAML configuration | `ui_performance` |
| [`src/test/resources/ui_performance/config/ui_performance-config.yml`](../../../src/test/resources/ui_performance/config/ui_performance-config.yml) | YAML configuration | `ui_performance` |
| [`src/test/resources/ui_performance/locators/ui_performance-locators.yml`](../../../src/test/resources/ui_performance/locators/ui_performance-locators.yml) | YAML configuration | `ui_performance_locators` |
| [`src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml`](../../../src/test/resources/ui_performance/testng-ui-performance-browser-contract.xml) | XML configuration | XML suite or report configuration; inspect enabled listeners/suites. |
| [`src/test/resources/ui_performance/testng-ui_performance.xml`](../../../src/test/resources/ui_performance/testng-ui_performance.xml) | XML configuration | XML suite or report configuration; inspect enabled listeners/suites. |

## Feature catalog

This table gives the feature-level tags and title. It is a discovery index, not a claim that a feature is included in the default runner. Confirm runner tag expressions before execution.

| Feature file | Feature tags | Feature title | First scenarios |
|---|---|---|---|
| [`src/test/resources/features/api_test.feature`](../../../src/test/resources/features/api_test.feature) | `(no feature-level tag)` | JSONPlaceholder API | `Retrieve a specific blog post and verify its title` |
| [`src/test/resources/features/automation_panda.feature`](../../../src/test/resources/features/automation_panda.feature) | `@Panda_Page` | Automation Panda Page | `Verify all existed pages` |
| [`src/test/resources/features/create_and_delete_user.feature`](../../../src/test/resources/features/create_and_delete_user.feature) | `@db` | User data management | `Create and then delete a new user in the database` |
| [`src/test/resources/features/create_post_workflow_api.feature`](../../../src/test/resources/features/create_post_workflow_api.feature) | `@api` | API Test for Blog Post Management on JSONPlaceholder | `Create, retrieve, and delete a new blog post` |
| [`src/test/resources/features/csv/csv_automation_example.feature`](../../../src/test/resources/features/csv/csv_automation_example.feature) | `@csv @csv_example` | CSV Automation — File-based and UI-embedded | `Verify transaction CSV data using column names`; `Verify CSV structure and column existence`; `Verify CSV data using column index`; … |
| [`src/test/resources/features/db/database_connection_health_check.feature`](../../../src/test/resources/features/db/database_connection_health_check.feature) | `@db` | Database Connection Health Check | `Validate SQL Server database connection` |
| [`src/test/resources/features/eStore.feature`](../../../src/test/resources/features/eStore.feature) | `@eStore @LastScenario` | Consumer Deposit with Payment Switch | `Consumer Deposit End to End Flow with one product verifying Payment Switch`; `Consumer Deposit End to End Flow with one product verifying Payment Switch`; `Consumer Deposit - Getting Started Page` |
| [`src/test/resources/features/eStore2.feature`](../../../src/test/resources/features/eStore2.feature) | `@eStore @LastScenario` | Consumer Deposit with Payment Switch 2 | `Consumer Deposit End to End Flow with one product verifying Payment Switch`; `Consumer Deposit - Getting Started Page` |
| [`src/test/resources/features/feature.feature`](../../../src/test/resources/features/feature.feature) | `@regression @login_page` | Login Page Feature | `Successful login`; `Successful login 2` |
| [`src/test/resources/features/frameTesting.feature`](../../../src/test/resources/features/frameTesting.feature) | `@FrameTesting` | Argo Teller | `Argo Connects SignOn` |
| [`src/test/resources/features/google.feature`](../../../src/test/resources/features/google.feature) | `@google` | Google Validation | `Search for wooden spoon` |
| [`src/test/resources/features/mobile/appium_mobile_browser_google_search.feature`](../../../src/test/resources/features/mobile/appium_mobile_browser_google_search.feature) | `@mobile @appium_browser @google_mobile_browser @evidence` | Appium real mobile browser Google search automation | `Search Google from the real mobile browser and capture evidence` |
| [`src/test/resources/features/mobile/fnb_cross_platform_smoke.feature`](../../../src/test/resources/features/mobile/fnb_cross_platform_smoke.feature) | `@mobile @cross_platform @fnb @fnb_smoke @evidence` | FNB Direct cross-platform native mobile launch and evidence workflow | `Validate FNB app launches and capture evidence on configured platform` |
| [`src/test/resources/features/mobile/theapp_cross_platform_workflow.feature`](../../../src/test/resources/features/mobile/theapp_cross_platform_workflow.feature) | `@mobile @cross_platform @theapp_smoke @evidence @smoke` | TheApp cross-platform native mobile workflow | `Validate Echo Box workflow on configured mobile platform`; `Validate Login screen opens on configured mobile platform` |
| [`src/test/resources/features/mobile_browser/mobile_browser_visual_sample.feature`](../../../src/test/resources/features/mobile_browser/mobile_browser_visual_sample.feature) | `@mobile_browser @visual @smoke` | Mobile browser visual validation sample | `Compare current mobile browser page against a named baseline` |
| [`src/test/resources/features/pdf/PdfValidation.feature`](../../../src/test/resources/features/pdf/PdfValidation.feature) | `@pdf @text-only @green` | Validate all textual content of the sample invoice PDF | `Whole document contains all important sections and values`; `Page 1 contains header, billing and line items`; `Page 2 contains payment details and thank you note`; … |
| [`src/test/resources/features/performance/happy_path_load_validation.feature`](../../../src/test/resources/features/performance/happy_path_load_validation.feature) | `@performance_testing_1` | Performance Happy Path Load Validation | `Happy path extreme local stress` |
| [`src/test/resources/features/performance/performance.feature`](../../../src/test/resources/features/performance/performance.feature) | `@performance_testing` | Navigator GraphQL performance validation | `Create VHLM through GraphQL under load` |
| [`src/test/resources/features/performance/performance_basic.feature`](../../../src/test/resources/features/performance/performance_basic.feature) | `(no feature-level tag)` | (no Feature declaration found) | (none detected) |
| [`src/test/resources/features/performance/performance_full_regression.feature`](../../../src/test/resources/features/performance/performance_full_regression.feature) | `@performance_testing @performance_full_regression` | Full Phase 1 Performance Engine Validation | `Validate basic GET performance execution`; `Validate custom-profile GET performance execution`; `Validate inline JSON POST performance execution`; … |
| [`src/test/resources/features/performance/performance_negative_validation.feature`](../../../src/test/resources/features/performance/performance_negative_validation.feature) | `(no feature-level tag)` | (no Feature declaration found) | (none detected) |
| [`src/test/resources/features/performance/performance_payload_driven.feature`](../../../src/test/resources/features/performance/performance_payload_driven.feature) | `(no feature-level tag)` | (no Feature declaration found) | (none detected) |
| [`src/test/resources/features/personal_bank/estore_Regression.feature`](../../../src/test/resources/features/personal_bank/estore_Regression.feature) | `@regression_365_test` | Personal Bank Regression Feature | `FNBSB-365_FE: Implement Co-Applicant Personal Details Page - Manual Entry`; `FNBSB-351_FE: Display Co-Applicants for Primary Applicant from Consumer when applicable via API`; `FNBSB-352_FE:  Implement ability to Add Beneficial Owners from Other Business Owners (Add Beneficial Owners Page)`; … |
| [`src/test/resources/features/personal_bank/personaBank_Checking.feature`](../../../src/test/resources/features/personal_bank/personaBank_Checking.feature) | `@regression @personal_bank @smoke` | Personal Bank Feature | `Apply for estyle checking account from the personal tab`; `Apply for estyle plus checking account from the personal tab`; `Apply for free style checking account from the personal tab`; … |
| [`src/test/resources/features/personal_bank/personaBank_Saving.feature`](../../../src/test/resources/features/personal_bank/personaBank_Saving.feature) | `@regression @personal_bank_business @smoke` | Personal Bank Feature | `Apply for FirstRate Savings account from the personal tab`; `Apply for FirstRate Money Market savings account from the personal tab`; `Apply for FirstRate Money Market savings account from the personal tab`; … |
| [`src/test/resources/features/personal_bank/personal_bank.feature`](../../../src/test/resources/features/personal_bank/personal_bank.feature) | `@regression @personal_bank_test` | Personal Bank Feature | `Apply for checking account from the personal tab 01`; `Apply for checking account from the personal tab 02` |
| [`src/test/resources/features/personal_bank/personal_bank_2.feature`](../../../src/test/resources/features/personal_bank/personal_bank_2.feature) | `@regression @personal_bank_test` | Personal Bank Feature | `Apply for checking account from the personal tab 1`; `Apply for checking account from the personal tab 2` |
| [`src/test/resources/features/secondPageTest.feature`](../../../src/test/resources/features/secondPageTest.feature) | `@secondPageTest
@LastScenario` | Second Page Test | `Opening New page Same Browser`; `Verify Nested Frame`; `Verify Nested Frame`; … |
| [`src/test/resources/features/xml/xml_automation_example.feature`](../../../src/test/resources/features/xml/xml_automation_example.feature) | `@xml @xml_example` | XML Automation — File-based and UI-embedded | `Verify order response XML using simple node names`; `Verify order items using XPath expressions`; `Extract XML values and use them in subsequent assertions`; … |
| [`src/test/resources/ui_performance/features/estore_ui_performance.feature`](../../../src/test/resources/ui_performance/features/estore_ui_performance.feature) | `@ui_performance @estore_ui_performance` | eStore Consumer Deposit UI Performance | `Concurrent users generate a Consumer Deposit application URL` |

## Step-definition catalog

The expressions below are copied from Cucumber annotations. Regular-expression syntax is shown exactly as implemented. Do not treat this table as a replacement for source-level parameter behavior; open the linked class before adding a new scenario.

### `src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `I set the request header {string} to {string}` |
| `And` | `I set the path parameter {string} to {string}` |
| `And` | `I set the query parameter {string} to {string}` |
| `And` | `I set the request body to` |
| `When` | `I send a {string} request to the {string} service` |
| `Then` | `the response code should be {int}` |
| `And` | `the response body should contain the text {string}` |
| `And` | `the response header {string} should be {string}` |
| `And` | `the value of the JSON path {string} should be {string}` |

### `src/test/java/com/ptaf/stepdefinitions/CsvSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/CsvSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `I load CSV file {string}` |
| `Given` | `I load CSV file {string} with delimiter {string}` |
| `Given` | `I load CSV from UI element on page {string} locator {string}` |
| `Then` | `CSV row {int} column {string} equals {string}` |
| `Then` | `CSV row {int} column {string} contains {string}` |
| `Then` | `CSV row {int} column {string} does not equal {string}` |
| `Then` | `CSV row {int} column index {int} equals {string}` |
| `Then` | `CSV row count equals {int}` |
| `Then` | `CSV row count is at least {int}` |
| `Then` | `CSV column {string} exists` |
| `Then` | `CSV column {string} does not exist` |
| `Then` | `all CSV rows have column {string} equals {string}` |
| `Then` | `CSV row {int} column {string} equals stored value {string}` |
| `When` | `I extract CSV row {int} column {string} and store as {string}` |

### `src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `I validate the database connection is successful` |
| `Given` | `the database does not contain a record for query {string} with parameters {string}` |
| `Then` | `I verify the database contains a record for query {string} with parameters {string}` |
| `Then` | `I verify the database does not contain a record for query {string} with parameters {string}` |
| `When` | `I insert a new record using query {string} with parameters {string}` |
| `When` | `I update a record using query {string} with parameters {string}` |
| `When` | `I delete {int} record\\(s) using query {string} with parameters {string}` |
| `Then` | `I verify single database value for query {string} with parameters {string} equals {string}` |
| `When` | `I execute database update query {string} with parameters {string} then {int} row\\(s) should be affected` |
| `Then` | `I verify database record for query {string} with parameters {string} contains:` |

### `src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Then` | `^we switch to popup (.*?) locator (.*?)$` |
| `Given` | `^we navigate to (.*?) url$` |
| `Then` | `^we click on main frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on main frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on main frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on main frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we enter random value on main frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on main frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on main frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on main frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on main frame (.*?) locator (.*?)$` |
| `Then` | `^we type on main frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on main frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on main frame (.*?) locator (.*?)$` |
| `Then` | `^we verify on main frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on main frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on main frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on main frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on main frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on main frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on main frame (.*?) of locator (.*?)$` |
| `Then` | `^we get value of elements on main frame (.*?) locator (.*?)$` |
| `Then` | `^we get list of elements on main frame (.*?) locator (.*?)$` |
| `When` | `we click radio on main frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on main frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on main frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) main frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we write to excel for testCase (.*?) column "(.*?)" main frame element (.*?) locator (.*?)$` |
| `Then` | `^we get text and contain on main frame (.*?) of locator (.*?)$` |
| `And` | `^we download on main frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for main frame (.*?) locator (.*?)$` |
| `Then` | `^we click on second frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on second frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on second frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on second frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on second frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on second frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on second frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on second frame (.*?) locator (.*?)$` |
| `Then` | `^we type on second frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on second frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on second frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on second frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on second frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on second frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on second frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on second frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on second frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on second frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get value of elements on second frame (.*?) locator (.*?)$` |
| `Then` | `^we get list of elements on second frame (.*?) locator (.*?)$` |
| `When` | `we click radio on second frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on second frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on second frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `And` | `^we download on second frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we enter using excel data on testCase (.*?) second frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we write to excel for testCase (.*?) column "(.*?)" second frame element (.*?) locator (.*?)$` |
| `Then` | `^we get text and contain on second frame (.*?) of locator (.*?)$` |
| `Then` | `^we select file: (.*?) for second frame (.*?) locator (.*?)$` |
| `Then` | `^we click on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on teller frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on teller frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we type on teller frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on teller frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on teller frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on teller frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on teller frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on teller frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on teller frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on teller frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on teller frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on teller frame (.*?) locator (.*?)$` |
| `When` | `we click radio on teller frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on teller frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on teller frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) teller frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on teller frame (.*?) of locator (.*?)$` |
| `And` | `^we download on teller frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for teller frame (.*?) locator (.*?)$` |
| `Then` | `^we click on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on  DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we type on DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is disabled` |
| `Then` | `^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we get text on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on DueDiligenceForm frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on DueDiligenceForm frame (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on DueDiligenceForm frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on DueDiligenceForm frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on DueDiligenceForm frame (.*?) of locator (.*?)$` |
| `And` | `^we download on DueDiligenceForm frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we click on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on pop-up DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on pop-up DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we type on pop-up DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on pop-up DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is disabled` |
| `Then` | `^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we get text on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on pop-up DueDiligenceForm frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on pop-up DueDiligenceForm frame (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on pop-up DueDiligenceForm frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on pop-up DueDiligenceForm frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) pop-up DueDiligenceForm frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on pop-up DueDiligenceForm frame (.*?) of locator (.*?)$` |
| `And` | `^we download on pop-up DueDiligenceForm frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for pop-up DueDiligenceForm (.*?) locator (.*?)$` |
| `Then` | `^we click on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on warning-box frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on warning-box frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we type on warning-box frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on warning-box frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on warning-box frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on warning-box frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on warning-box frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on warning-box frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on warning-box frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on warning-box frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on warning-box frame (.*?) locator (.*?)$` |
| `When` | `we click radio on warning-box frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on warning-box frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on warning-box frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) warning-box frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on warning-box frame (.*?) of locator (.*?)$` |
| `And` | `^we download on warning-box frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for warning-box frame (.*?) locator (.*?)$` |
| `Then` | `^we click on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we write to excel for testCase (.*?) column "(.*?)" pop-up frame element (.*?) locator (.*?)$` |
| `Then` | `^we double click on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on pop-up frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on pop-up frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we type on pop-up frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we verify on pop-up frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on pop-up frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on pop-up frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on pop-up frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on pop-up frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on pop-up frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on pop-up frame (.*?) locator (.*?)$` |
| `When` | `we click radio on pop-up frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on pop-up frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on pop-up frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) pop-up frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on pop-up frame (.*?) of locator (.*?)$` |
| `And` | `^we download on pop-up frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for pop-up frame (.*?) locator (.*?)$` |
| `Then` | `^we click on third frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on third frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on third frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on third frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on third frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on third frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on third frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on third frame (.*?) locator (.*?)$` |
| `Then` | `^we type on third frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on third frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on third frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on third frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on third frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on third frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on third frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on third frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on third frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on third frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on third frame (.*?) locator (.*?)$` |
| `When` | `we click radio on third frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on third frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on third frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) third frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we write to excel for testCase (.*?) column "(.*?)" third frame element (.*?) locator (.*?)$` |
| `Then` | `^we get text and contain on third frame (.*?) of locator (.*?)$` |
| `And` | `^we download on third frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for third frame (.*?) locator (.*?)$` |
| `Then` | `^we click on header frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on header frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on header frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on header frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on header frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on header frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on header frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on header frame (.*?) locator (.*?)$` |
| `Then` | `^we type on header frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on header frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on header frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on header frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on header frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on header frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on header frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on header frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on header frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on header frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on header frame (.*?) locator (.*?)$` |
| `When` | `we click radio on header frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on header frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on header frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) header frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on header frame (.*?) of locator (.*?)$` |
| `And` | `^we download on header frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for header frame (.*?) locator (.*?)$` |
| `Then` | `^we click on form frame (.*?) locator (.*?)$` |
| `Then` | `^we report list of selection on form frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on form frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on form frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on form frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on form frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on form frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on form frame (.*?) locator (.*?)$` |
| `Then` | `^we type on form frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on form frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on form frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on form frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on form frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on form frame (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on form frame (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on form frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on form frame (.*?) locator (.*?)$` |
| `Then` | `^we has value on form frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on form frame (.*?) locator (.*?)$` |
| `When` | `we click radio on form frame (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on form frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on form frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we enter using excel data on testCase (.*?) form frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text and contain on Form frame (.*?) of locator (.*?)$` |
| `And` | `^we download on form frame (.*?) locator (.*?) and file type is "(.*?)"$` |
| `Then` | `^we select file: (.*?) for Form frame (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on FNB-Online page (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we capture screenshot on Harland Clarke page (.*?) locator (.*?) name "(.*?)"$` |
| `Then` | `we navigate to Vault page locator (.*?) and capture screenshot$` |
| `Then` | `we navigate to Vault page from pop-up locator and capture screenshot$` |
| `Given` | `^get title of page$` |
| `And` | `^time out for (.*?) seconds$` |
| `And` | `^time out for (.*?) minutes$` |
| `And` | `^we wait for some time$` |
| `Then` | `^we enter using excel data on testCase "(.*?)" column name "(.*?)" value "(.*?)"$` |

### `src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Then` | `I compare mobile browser page with visual baseline {string}` |

### `src/test/java/com/ptaf/stepdefinitions/MobileSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/MobileSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `I start mobile application using platform {string}` |
| `Given` | `I start mobile browser using platform {string}` |
| `When` | `I open mobile browser url {string}` |
| `When` | `I press Enter on mobile page {word} locator {word}` |
| `When` | `I save mobile browser page source to {string}` |
| `Then` | `mobile browser current url should contain {string}` |
| `Then` | `mobile browser title should contain {string}` |
| `When` | `I tap on mobile page {word} locator {word}` |
| `When` | `I long press mobile page {word} locator {word} for {int} milliseconds` |
| `When` | `I double tap mobile page {word} locator {word}` |
| `When` | `I tap mobile screen at x {int} y {int}` |
| `When` | `I drag mobile page {word} locator {word} to page {word} locator {word}` |
| `When` | `I scroll mobile page {word} locator {word} into view with max {int} swipes` |
| `When` | `I scroll mobile screen to text {string}` |
| `When` | `I enter mobile value {string} on page {word} locator {word}` |
| `When` | `I clear mobile page {word} locator {word}` |
| `When` | `I hide mobile keyboard` |
| `When` | `I background mobile app for {int} seconds` |
| `When` | `I swipe mobile screen up` |
| `When` | `I swipe mobile screen down` |
| `When` | `I swipe mobile screen left` |
| `When` | `I swipe mobile screen right` |
| `When` | `I pinch in mobile screen` |
| `When` | `I zoom out mobile screen` |
| `When` | `I rotate mobile screen to {string}` |
| `When` | `I rotate mobile screen using configured orientation` |
| `When` | `I activate mobile app {string}` |
| `When` | `I terminate mobile app {string}` |
| `When` | `I open mobile deep link {string} for app {string}` |
| `When` | `I push local file {string} to mobile path {string}` |
| `When` | `I pull mobile file {string} to local path {string}` |
| `When` | `I set mobile clipboard text {string}` |
| `When` | `I switch mobile context to {string}` |
| `When` | `I switch mobile context to native app` |
| `When` | `I grant mobile permission {string} for app {string}` |
| `When` | `I revoke mobile permission {string} for app {string}` |
| `When` | `I wait up to {int} seconds for mobile page {word} locator {word} to be visible` |
| `When` | `I wait up to {int} seconds for mobile page {word} locator {word} to disappear` |
| `When` | `I pause mobile execution for {int} seconds` |
| `When` | `I allow mobile permission popup if displayed` |
| `When` | `I deny mobile permission popup if displayed` |
| `When` | `I allow mobile permission popup with text {string} if displayed` |
| `When` | `I allow all mobile permission popups if displayed` |
| `When` | `I deny all mobile permission popups if displayed` |
| `When` | `I handle mobile permission popup using action {string} if displayed` |
| `Then` | `I verify mobile page {word} locator {word} is visible` |
| `Then` | `I verify mobile page {word} locator {word} text contains {string}` |
| `When` | `I capture mobile screenshot named {string}` |
| `Then` | `I verify mobile clipboard text contains {string}` |

### `src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Then` | `^we click (.*?) locator (.*?) and switch to popup$` |
| `Then` | `^we click on new page (.*?) locator (.*?)$` |
| `Then` | `^we double click on new page (.*?) locator (.*?)$` |
| `Then` | `^we enter value on new page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on new page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on new page (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on new page (.*?) locator (.*?)$` |
| `Then` | `^we hover on new page (.*?) locator (.*?)$` |
| `Then` | `^we type on new page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on new page (.*?) locator (.*?)$` |
| `Then` | `^we clear value on new page (.*?) locator (.*?)$` |
| `Then` | `^we verify on new page (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on new page (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on new page (.*?) of locator (.*?) is enabled$` |
| `Then` | `^we get value on new page (.*?) locator (.*?)$` |
| `Then` | `^we verify element has value on new page (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on new page (.*?) of locator (.*?) is existed$` |
| `Then` | `^we verify on new page (.*?) of locator (.*?) is not existed$` |
| `Then` | `^we contain on new page (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on new page (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on new page (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on new page (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `When` | `^we click radio on new page (.*?) list locator (.*?)$` |
| `Then` | `^we get text and contain on new page (.*?) locator (.*?)$` |
| `Then` | `^get consumer tracking ID and contain on new page$` |
| `Then` | `^we click on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on plaid frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on plaid frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we type on plaid frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we verify on plaid frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on plaid frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on plaid frame (.*?) of locator (.*?) is enabled$` |
| `Then` | `^we get value on plaid frame (.*?) locator (.*?)$` |
| `Then` | `^we verify element has value on plaid frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on plaid frame (.*?) of locator (.*?) is existed$` |
| `Then` | `^we verify on plaid frame (.*?) of locator (.*?) is not existed$` |
| `Then` | `^we contain on plaid frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on plaid frame (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on plaid frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on plaid frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we click on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on pop frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on pop frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we type on pop frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we verify on pop frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on pop frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on pop frame (.*?) of locator (.*?) is enabled$` |
| `Then` | `^we get value on pop frame (.*?) locator (.*?)$` |
| `Then` | `^we verify element has value on pop frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on pop frame (.*?) of locator (.*?) is existed$` |
| `Then` | `^we verify on pop frame (.*?) of locator (.*?) is not existed$` |
| `Then` | `^we contain on pop frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on pop frame (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on pop frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on pop frame (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `Then` | `^we click on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we double click on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we enter value on atomic frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on atomic frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we hover on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we type on atomic frame (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we clear value on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we verify on atomic frame (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on atomic frame (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on atomic frame (.*?) of locator (.*?) is enabled$` |
| `Then` | `^we get value on atomic frame (.*?) locator (.*?)$` |
| `Then` | `^we verify element has value on atomic frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on atomic frame (.*?) of locator (.*?) is existed$` |
| `Then` | `^we verify on atomic frame (.*?) of locator (.*?) is not existed$` |
| `Then` | `^we contain on atomic frame (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on atomic frame (.*?) locator (.*?)$` |
| `And` | `^we capture screenshot on atomic frame (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on atomic frame (.*?) locator (.*?) key "(.*?)" keyboard$` |

### `src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `^we navigate to (.*?) url$` |
| `Then` | `^we click on page (.*?) locator (.*?)$` |
| `Then` | `^we double click on page (.*?) locator (.*?)$` |
| `Then` | `^we enter value on page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we select on page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we check on page (.*?) locator (.*?)$` |
| `Then` | `^we uncheck on page (.*?) locator (.*?)$` |
| `Then` | `^we hover on page (.*?) locator (.*?)$` |
| `Then` | `^we type on page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we scroll on page (.*?) locator (.*?)$` |
| `Then` | `^we clear value on page (.*?) locator (.*?) value "(.*?)"$` |
| `Then` | `^we verify on page (.*?) of locator (.*?) is visible$` |
| `Then` | `^we verify on page (.*?) of locator (.*?) is checked$` |
| `Then` | `^we verify on page (.*?) of locator (.*?) is enabled` |
| `Then` | `^we verify on page (.*?) of locator (.*?) is existed` |
| `Then` | `^we contain on page (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get text on page (.*?) locator (.*?)$` |
| `Then` | `^we get value on page (.*?) locator (.*?)$` |
| `Then` | `^we has value on page (.*?) of locator (.*?) value "(.*?)"$` |
| `Then` | `^we get list of elements on page (.*?) locator (.*?)$` |
| `Then` | `^we get text of elements on page (.*?) locator (.*?)$` |
| `When` | `we click radio on page (.*?) list locator (.*?)$` |
| `And` | `^we capture screenshot on page (.*?) locator (.*?) name "(.*?)"$` |
| `And` | `^we press on page (.*?) locator (.*?) key "(.*?)" keyboard$` |
| `And` | `^we click download on page (.*?) locator (.*?)$` |
| `And` | `^we select document to upload on page (.*?) locator (.*?)$` |
| `Given` | `^get title of page$` |
| `And` | `^we wait for some time$` |
| `And` | `^time out for (.*?) seconds$` |
| `Then` | `^Stop Execution` |
| `Then` | `^we close all browsers$` |

### `src/test/java/com/ptaf/stepdefinitions/PdfSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/PdfSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `When` | `I download PDF from {string}.{string} saving to {string}` |
| `When` | `I set last PDF from directory {string}` |
| `Then` | `the last PDF should exist` |
| `Then` | `the last PDF should be a valid PDF` |
| `Then` | `the last PDF should contain {string}` |
| `Then` | `the last PDF should not contain {string}` |
| `Then` | `the last PDF should contain all:` |
| `Then` | `the last PDF should match regex {string}` |
| `Then` | `page {int} of the last PDF should contain {string}` |
| `Then` | `page {int} of the last PDF should match regex {string}` |
| `Then` | `the last PDF should have {int} pages` |
| `Then` | `OCR on page {int} of the last PDF with dpi {float} and lang {string} should contain {string}` |
| `Then` | `page {int} of the last PDF rendered at {float} dpi should visually equal {string} with tolerance {int} and max diff {double}, diff out {string}` |
| `Then` | `the last PDF metadata {string} should contain {string}` |
| `Then` | `the last PDF form field {string} should equal {string}` |
| `Then` | `I print first {int} chars of last PDF` |

### `src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `When` | `^we run GET performance test for path "(.*?)" with name "(.*?)"$` |
| `When` | `^we run GET performance test for path "(.*?)" with name "(.*?)" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$` |
| `When` | `^we run POST performance test for path "(.*?)" with name "(.*?)" and json body "(.*?)"$` |
| `When` | `^we run POST performance test for path "(.*?)" with name "(.*?)" and json body "(.*?)" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$` |
| `When` | `^we run PUT performance test for path "(.*?)" with name "(.*?)" and json body "(.*?)"$` |
| `When` | `^we run PUT performance test for path "(.*?)" with name "(.*?)" and json body "(.*?)" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$` |
| `When` | `^we run DELETE performance test for path "(.*?)" with name "(.*?)"$` |
| `When` | `^we run DELETE performance test for path "(.*?)" with name "(.*?)" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$` |
| `When` | `^we run YAML-driven POST performance test for path "(.*?)" with name "(.*?)" using yaml key "(.*?)"$` |
| `When` | `^we run YAML-driven PUT performance test for path "(.*?)" with name "(.*?)" using yaml key "(.*?)"$` |
| `When` | `^we run CSV-driven POST performance test for path "(.*?)" with name "(.*?)" using csv file "(.*?)" row "(.*?)" column "(.*?)"$` |
| `When` | `^we run CSV-driven PUT performance test for path "(.*?)" with name "(.*?)" using csv file "(.*?)" row "(.*?)" column "(.*?)"$` |
| `When` | `^we run Excel-driven POST performance test for path "(.*?)" with name "(.*?)" using excel file "(.*?)" row "(.*?)" column "(.*?)"$` |
| `When` | `^we run Excel-driven PUT performance test for path "(.*?)" with name "(.*?)" using excel file "(.*?)" row "(.*?)" column "(.*?)"$` |
| `When` | `^we store bearer token alias "(.*?)" with value "(.*?)"$` |
| `When` | `^we run authenticated GET performance test for path "(.*?)" with name "(.*?)" using bearer token alias "(.*?)"$` |
| `When` | `^we run authenticated YAML-driven POST performance test for path "(.*?)" with name "(.*?)" using yaml key "(.*?)" and bearer token alias "(.*?)"$` |
| `When` | `^we run basic auth GET performance test for path "(.*?)" with name "(.*?)" username "(.*?)" password "(.*?)"$` |
| `When` | `^we run GET performance test expecting failure for path "(.*?)" with name "(.*?)"$` |
| `When` | `^we run basic auth GET performance test expecting failure for path "(.*?)" with name "(.*?)" username "(.*?)" password "(.*?)"$` |
| `When` | `^we run YAML-driven POST performance test expecting failure for path "(.*?)" with name "(.*?)" using yaml key "(.*?)"$` |
| `Then` | `^performance result should be available$` |
| `Then` | `^performance dashboard path should be generated$` |
| `Then` | `^performance summary file path should be generated$` |
| `Then` | `^performance readable summary file path should be generated$` |
| `Then` | `^performance jtl file path should be generated$` |
| `Then` | `^performance run report root path should be generated$` |
| `Then` | `^performance excel report should be generated$` |
| `Then` | `^performance execution should pass$` |
| `Then` | `^performance execution should fail$` |
| `Then` | `^performance execution should be in expected failure mode$` |
| `Then` | `^performance failure message should contain "(.*?)"$` |
| `Then` | `^performance average response time should be less than (\\d+) ms$` |
| `Then` | `^performance p95 response time should be less than (\\d+) ms$` |
| `Then` | `^performance error percentage should be less than (\\d+(?:\\.\\d+)?)$` |
| `Then` | `^performance error percentage should be greater than (\\d+(?:\\.\\d+)?)$` |
| `Then` | `^performance total errors should be greater than (\\d+)$` |
| `Then` | `^performance total samples should be greater than (\\d+)$` |
| `Then` | `^performance total scenario duration should be greater than (\\d+) ms$` |
| `Then` | `^performance risk score should be greater than (\\d+)$` |
| `Then` | `^performance risk score should be less than (\\d+)$` |
| `Then` | `^performance risk level should be "(.*?)"$` |
| `Then` | `^performance threshold breach summary should contain "(.*?)"$` |
| `Then` | `^performance recommended action should contain "(.*?)"$` |

### `src/test/java/com/ptaf/stepdefinitions/XmlSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/XmlSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `I load XML file {string}` |
| `Given` | `I load XML from UI element on page {string} locator {string}` |
| `Then` | `XML node {string} equals {string}` |
| `Then` | `XML XPath {string} equals {string}` |
| `Then` | `XML node {string} contains {string}` |
| `Then` | `XML XPath {string} contains {string}` |
| `Then` | `XML node {string} does not equal {string}` |
| `Then` | `XML node {string} exists` |
| `Then` | `XML node {string} does not exist` |
| `Then` | `XML XPath {string} count equals {int}` |
| `Then` | `XML node {string} attribute {string} equals {string}` |
| `Then` | `XML node {string} equals stored value {string}` |
| `When` | `I extract XML node {string} and store as {string}` |
| `When` | `I extract XML XPath {string} and store as {string}` |

### `src/test/java/com/ptaf/stepdefinitions/ZipSteps.java`

Source: [`src/test/java/com/ptaf/stepdefinitions/ZipSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `I unzip file {string}` |
| `Given` | `I unzip file {string} to directory {string}` |
| `When` | `I convert txt file {string} to CSV using delimiter {string}` |
| `When` | `I convert txt file {string} to CSV` |
| `Given` | `I load CSV from zip file {string}` |
| `Given` | `I load the first CSV file from zip` |
| `Given` | `I load XML from zip file {string}` |
| `Given` | `I load the first XML file from zip` |
| `Then` | `zip contains file {string}` |
| `Then` | `zip contains a {string} file` |
| `Then` | `I cleanup extracted zip files` |

### `src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`

Source: [`src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`](../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java)

| Keyword | Implemented Cucumber expression |
|---|---|
| `Given` | `UI performance journey {string} uses configured target` |
| `Given` | `UI performance journey {string} targets {string}` |
| `When` | `UI performance journey navigates to configured route {string}` |
| `When` | `UI performance journey navigates to {string}` |
| `And` | `UI performance journey fills locator {string} {string} with data field {string}` |
| `And` | `UI performance journey selects locator {string} {string} with data field {string}` |
| `And` | `UI performance journey fills locator {string} {string} with literal {string}` |
| `And` | `UI performance journey clicks locator {string} {string}` |
| `And` | `UI performance journey clicks locator {string} {string} and switches to popup` |
| `Then` | `UI performance journey verifies locator {string} {string} is visible` |
| `Then` | `the configured UI performance users execute the journey` |
| `Then` | `the UI performance run produces a standalone performance report` |
| `Then` | `the UI performance run produces a standalone report` |

## Production Java source catalog

This catalog groups production classes by package. The summary uses the class-level Javadoc when available; otherwise it identifies the declared type and directs the reader to the implementation.

### `com.ptaf.api.handlers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java`](../../../src/main/java/com/ptaf/api/handlers/ApiRequestHandler.java) | ApiRequestHandler manages the lifecycle of Playwright APIRequestContext instances.  Enterprise Framework Responsibility: This class centralizes API context creation for all API automation tests. It ensures that each execution thread receives an isolated APIReq |

### `com.ptaf.api.implementation`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/api/implementation/ApiActionImpl.java`](../../../src/main/java/com/ptaf/api/implementation/ApiActionImpl.java) | Implements the ApiAction interface to provide concrete methods for building, sending, and verifying API requests.  This class is intended to be used in a test automation flow where steps prepare parts of an HTTP request (headers, path/query parameters, body) a |

### `com.ptaf.api.interfaces`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/api/interfaces/ApiAction.java`](../../../src/main/java/com/ptaf/api/interfaces/ApiAction.java) | ApiAction defines the contract for performing high-level, reusable API operations. This interface abstracts away the complexities of building and sending HTTP requests, allowing tests to be written in a clean, readable, and stateful manner. Typical workflow fo |

### `com.ptaf.api.methods`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/api/methods/ApiCommonMethods.java`](../../../src/main/java/com/ptaf/api/methods/ApiCommonMethods.java) | ApiCommonMethods provides a high-level API for interacting with web services during tests. This class translates simple, readable method calls into API actions, which are then used in the step definition files.  Purpose for testers: - This class is a thin faca |

### `com.ptaf.api.performer`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/api/performer/ApiActionPerformer.java`](../../../src/main/java/com/ptaf/api/performer/ApiActionPerformer.java) | Utility class responsible for performing HTTP actions against an API using a Playwright APIRequestContext. This class centralizes the construction of requests (headers, query parameters, path parameters, and body serialization) and converts Playwright's APIRes |

### `com.ptaf.api.wrapper`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java`](../../../src/main/java/com/ptaf/api/wrapper/ApiResponseWrapper.java) | A wrapper class to store the results of an API call in a standardized format. This decouples the framework from Playwright's specific APIResponse object and provides easy access to the most important parts of the response.  Designed to be a simple, immutable c |

### `com.ptaf.csv`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/csv/CsvCommonMethods.java`](../../../src/main/java/com/ptaf/csv/CsvCommonMethods.java) | High-level CSV assertion and extraction methods for use by Cucumber step definitions. This class sits between the raw { CsvFileHandler} (which handles parsing and querying) and the { com.ptaf.stepdefinitions.CsvSteps} class (which maps Gherkin sentences to Jav |
| [`src/main/java/com/ptaf/csv/CsvContext.java`](../../../src/main/java/com/ptaf/csv/CsvContext.java) | Thread-local context holder for the CSV automation module. This class stores the current { CsvFileHandler} instance in a { ThreadLocal} so that each test thread (scenario) has its own isolated CSV data. This follows the same pattern used by { com.ptaf.xml.XmlC |
| [`src/main/java/com/ptaf/csv/CsvFileHandler.java`](../../../src/main/java/com/ptaf/csv/CsvFileHandler.java) | Core CSV parsing and querying engine for the PTAF CSV automation module. This class is responsible for:  Loading CSV content from a filesystem path or a raw string (e.g., extracted from a UI element). Parsing the CSV into a list of rows, each represented as a  |

### `com.ptaf.db.handlers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/db/handlers/DatabaseHandler.java`](../../../src/main/java/com/ptaf/db/handlers/DatabaseHandler.java) | DatabaseHandler manages database connection lifecycle for PTAF database automation.  Enterprise Framework Responsibility: This class centralizes database connection creation, reuse, and cleanup. It uses ThreadLocal to ensure each parallel test execution thread |

### `com.ptaf.db.implementation`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java`](../../../src/main/java/com/ptaf/db/implementation/DatabaseActionImpl.java) | DatabaseActionImpl provides the concrete implementation of DatabaseAction.  Enterprise Framework Responsibility: This class acts as the orchestration layer for database automation. It does not directly manage SQL Server JDBC details and does not directly execu |

### `com.ptaf.db.interfaces`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/db/interfaces/DatabaseAction.java`](../../../src/main/java/com/ptaf/db/interfaces/DatabaseAction.java) | DatabaseAction defines the contract for all high-level database automation actions.  Purpose: This interface is the public API used by higher-level test code (step definitions, validation utilities, and helper libraries) to interact with the database in a cons |

### `com.ptaf.db.pages`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java`](../../../src/main/java/com/ptaf/db/pages/DatabaseCommonMethods.java) | DatabaseCommonMethods provides high-level reusable database actions for test automation.  Enterprise Framework Responsibility: This class is the database equivalent of PageCommonMethods / FrameCommonMethods. It provides clean, business-readable methods that ca |

### `com.ptaf.db.performer`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java`](../../../src/main/java/com/ptaf/db/performer/DatabaseActionPerformer.java) | DatabaseActionPerformer contains the low-level database execution logic.  Enterprise Framework Responsibility: This class is responsible only for executing SQL statements against an active JDBC Connection. It does not create or close database connections. Conn |

### `com.ptaf.db.validators`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java`](../../../src/main/java/com/ptaf/db/validators/DatabaseConnectionValidator.java) | DatabaseConnectionValidator validates SQL Server database connectivity.  Enterprise Framework Responsibility: This class provides a controlled and reusable way to confirm that the PTAF database automation layer can successfully connect to the configured databa |

### `com.ptaf.hooks`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/hooks/DatabaseHooks.java`](../../../src/main/java/com/ptaf/hooks/DatabaseHooks.java) | DatabaseHooks manages database-specific cleanup after Cucumber scenarios.  Enterprise Framework Responsibility: This hook ensures that database connections opened during DB automation are closed safely after each database scenario. This prevents stale SQL Serv |
| [`src/main/java/com/ptaf/hooks/Hooks.java`](../../../src/main/java/com/ptaf/hooks/Hooks.java) | Retains the recording for every page created in the active context. This is necessary because popup pages can close before scenario teardown and would not be available from context.pages(). |
| [`src/main/java/com/ptaf/hooks/MobileHooks.java`](../../../src/main/java/com/ptaf/hooks/MobileHooks.java) | Mobile-specific Appium lifecycle hooks. This class is intentionally separate from the existing Playwright Hooks class so native mobile execution remains opt-in and backward-compatible. Platform resolution priority: <ol> Command-line override: { -Dmobile.platfo |

### `com.ptaf.mobile.assertions`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/assertions/MobileAssert.java`](../../../src/main/java/com/ptaf/mobile/assertions/MobileAssert.java) | Utility class that provides a set of assertion helpers tailored for native Appium mobile tests. Each helper method performs a boolean/text verification using MobileCommonMethods and, on failure, captures a failure screenshot via MobileEvidenceManager before de |

### `com.ptaf.mobile.config`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java`](../../../src/main/java/com/ptaf/mobile/config/MobileConfigurationProperties.java) | Central configuration access for Appium native mobile automation. This utility class provides typed accessors for mobile-related configuration values read via MobileYamlReader. All values are read lazily on demand and have sensible defaults so tests can run wi |
| [`src/main/java/com/ptaf/mobile/config/MobilePlatform.java`](../../../src/main/java/com/ptaf/mobile/config/MobilePlatform.java) | Supported native mobile platforms for the PTAF Appium module.  This enum represents the two mobile platforms that the PTAF (Portable Test Automation Framework) module currently supports: ANDROID and IOS. It provides utility methods for:   Parsing a platform fr |
| [`src/main/java/com/ptaf/mobile/config/MobileYamlReader.java`](../../../src/main/java/com/ptaf/mobile/config/MobileYamlReader.java) | Enterprise-safe YAML reader for PTAF native mobile automation resources. This reader intentionally loads only framework-owned YAML files from:  { src/test/resources/mobile/config} { src/test/resources/mobile/elements}  It must not recursively load all files un |

### `com.ptaf.mobile.drivers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java`](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverFactory.java) | Factory responsible for creating configured AppiumDriver instances for native apps and browser sessions on Android and iOS.  This class is configuration-driven: all session capabilities and behavior are read from MobileConfigurationProperties which is typicall |
| [`src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java`](../../../src/main/java/com/ptaf/mobile/drivers/MobileDriverManager.java) | Thread-local Appium driver manager to support parallel-safe Cucumber execution.  This utility class centralizes the lifecycle of AppiumDriver instances per-thread using ThreadLocal storage. Tests (or Cucumber scenarios) can call { #startDriver(MobilePlatform)} |

### `com.ptaf.mobile.evidence`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java`](../../../src/main/java/com/ptaf/mobile/evidence/MobileEvidenceManager.java) | Centralized screenshot and video evidence management for native Appium runs.  This utility class provides methods to: - Start and stop native screen recording (via Appium's CanRecordScreen) if enabled in configuration. - Capture screenshots on scenario end (pa |

### `com.ptaf.mobile.handlers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java`](../../../src/main/java/com/ptaf/mobile/handlers/MobileLocatorHandler.java) | Enterprise mobile locator resolver for PTAF Appium automation. This resolver is intentionally backward-compatible. All historical PTAF mobile locator prefixes continue to work exactly as before, including ACCESSIBILITY_ID_, ID_, XPATH_, CLASS_NAME_, ANDROID_UI |

### `com.ptaf.mobile.implementation`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java`](../../../src/main/java/com/ptaf/mobile/implementation/MobileActionImpl.java) | Default implementation of the MobileAction interface. This class is a thin delegate that forwards calls to MobileCommonMethods instantiated with the Appium driver returned by MobileDriverManager.getDriver(). It is intended to be used by test code and automatio |

### `com.ptaf.mobile.interfaces`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/interfaces/MobileAction.java`](../../../src/main/java/com/ptaf/mobile/interfaces/MobileAction.java) | MobileAction defines a collection of high-level, platform-agnostic actions that can be performed against a mobile application under test. This interface abstracts typical gestures, interactions, device-control operations, and utilities that test automation fra |

### `com.ptaf.mobile.pages`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java`](../../../src/main/java/com/ptaf/mobile/pages/MobileCommonMethods.java) | Reusable Appium mobile actions used by Cucumber step definitions. The class intentionally exposes enterprise-ready high-level mobile actions so testers can automate common native app behavior from Cucumber without writing Java code in project teams. |

### `com.ptaf.mobile.permissions`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java`](../../../src/main/java/com/ptaf/mobile/permissions/MobilePermissionHandler.java) | Enterprise permission and system-dialog handler for native Appium tests. Mobile applications frequently request operating-system permissions such as location, camera, photos, microphone, notifications, contacts, calendars, Bluetooth, and local network access.  |

### `com.ptaf.pdf`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/pdf/PdfMeta.java`](../../../src/main/java/com/ptaf/pdf/PdfMeta.java) | PdfMeta Utility class to extract metadata and AcroForm field values from PDF documents using PDFBox. Purpose: - Read PDF document metadata (Title, Author, Subject, etc.) and form fields (AcroForm). Why: - Tests frequently need to assert that a generated PDF co |
| [`src/main/java/com/ptaf/pdf/PdfOcr.java`](../../../src/main/java/com/ptaf/pdf/PdfOcr.java) | PdfOcr Purpose: - Provide a lightweight way to OCR scanned PDF pages without embedding a Java OCR library. - Renders a specific PDF page to a PNG file and invokes the external "tesseract" binary to extract text. Important notes for testers and integrators: - T |
| [`src/main/java/com/ptaf/pdf/PdfRenderDiff.java`](../../../src/main/java/com/ptaf/pdf/PdfRenderDiff.java) | PdfRenderDiff Purpose: - Pure-Java visual diff between two images (e.g., actual rendered page vs. baseline PNG). - Supports per-channel tolerance and max diff ratio. Why: - Some layouts/text flows are easier to validate visually than through text extraction. - |
| [`src/main/java/com/ptaf/pdf/PdfStore.java`](../../../src/main/java/com/ptaf/pdf/PdfStore.java) | PdfStore Purpose: - Thread-scoped storage for the "current" PDF path under test. - Provides utility to set that path from the newest file in a directory using pure Java NIO (no external libs). Why: - Step definitions often need to validate the same file across |
| [`src/main/java/com/ptaf/pdf/PdfUtils.java`](../../../src/main/java/com/ptaf/pdf/PdfUtils.java) | Utility helpers for working with PDF documents using Apache PDFBox.  This final utility class centralizes small, commonly-used operations for reading and rendering PDFs:  Extract text (entire document, single page or page ranges) Support for password-protected |
| [`src/main/java/com/ptaf/pdf/PdfValidator.java`](../../../src/main/java/com/ptaf/pdf/PdfValidator.java) | PdfValidator Purpose: - High-level, readable assertion methods for PDFs. - Delegates to PdfUtils/PdfRenderDiff/PdfMeta for the heavy lifting. - OCR assertions are OPTIONAL: they auto-skip when Tesseract is not available OR when OCR is disabled via system prope |

### `com.ptaf.performance.assertions`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java`](../../../src/main/java/com/ptaf/performance/assertions/PerformanceAssertionEngine.java) | Central SLA validation layer for performance executions. This engine validates the final execution result against the configured performance assertion profile and throws readable assertion messages when thresholds are exceeded. Reporting-safe goals:  keep vali |

### `com.ptaf.performance.auth`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java`](../../../src/main/java/com/ptaf/performance/auth/PerformanceAuthTokenManager.java) | Framework-owned authentication token manager for performance execution. This class is responsible for:  storing tokens by logical alias supporting token chaining across requests tracking expiration metadata when available injecting bearer tokens into Performan |

### `com.ptaf.performance.builders`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceProfileBuilder.java) | Architect-controlled builder for creating performance execution profiles. This builder supports two execution modes:  Iteration mode: iterations > 0 Duration mode: iterations == 0 and holdSeconds > 0   Testers should not manually define low-level execution log |
| [`src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceRequestBuilder.java) | Architect-controlled builder for performance requests. This builder hides request construction complexity from testers and step definitions. It provides a fluent API to assemble all parts of a performance request (method, URL, headers, payload, authentication, |
| [`src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java`](../../../src/main/java/com/ptaf/performance/builders/PerformanceTestPlanBuilder.java) | Builds JMeter DSL test plans from framework-owned request and profile models. This class is architect-controlled and hides JMeter DSL details from testers and step definitions. Responsibilities: - Convert a PerformanceRequest and PerformanceProfile into a DslT |

### `com.ptaf.performance.config`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java`](../../../src/main/java/com/ptaf/performance/config/PerformanceConfigurationProperties.java) | Centralized accessor for performance-related configuration properties. This class provides convenience methods to construct common configuration objects (PerformanceProfile and PerformanceAssertionProfile) and to retrieve individual configuration values such a |
| [`src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java`](../../../src/main/java/com/ptaf/performance/config/PerformanceYamlReader.java) | Utility for reading application performance configuration from a YAML file on the classpath. Behavior summary: - Loads a single YAML file located at { performance/config/performance-config.yml} from the classpath when this class is first referenced (static ini |

### `com.ptaf.performance.core`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/core/BasePerformanceEngine.java`](../../../src/main/java/com/ptaf/performance/core/BasePerformanceEngine.java) | Shared architect-owned base for performance engines. This base class centralizes reusable framework-owned dependencies for performance execution layers. Current responsibilities:  provide shared access to assertion engine provide shared access to test plan bui |
| [`src/main/java/com/ptaf/performance/core/PerformanceEngine.java`](../../../src/main/java/com/ptaf/performance/core/PerformanceEngine.java) | Central architect-controlled execution engine for PTAF performance testing. Responsibilities:  build and run JMeter DSL test plans parse JTL metrics evaluate assertion outcomes produce scenario-level result objects maintain one shared run-level report write TX |
| [`src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java`](../../../src/main/java/com/ptaf/performance/core/PerformanceExecutionManager.java) | Fallback execution manager for prepared performance test plans. This class is compatible with the richer { PerformanceExecutionResult} model and can be used when a caller already has a prepared test plan and only needs a framework-owned result object back. Imp |

### `com.ptaf.performance.headers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java`](../../../src/main/java/com/ptaf/performance/headers/PerformanceHeaderManager.java) | Framework-owned header manager for performance execution. This class centralizes HTTP header creation and merge behavior so that testers never manually build low-level header maps in feature logic. Responsibilities:  Store default framework headers Store reque |

### `com.ptaf.performance.models`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceAssertionProfile.java) | Defines SLA expectations for a performance run. Reporting-safe goals:  keep constructor compatibility normalize invalid threshold values provide small helper methods for reporting/risk logic    This immutable value object encapsulates three asserted thresholds |
| [`src/main/java/com/ptaf/performance/models/PerformanceExecutionResult.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionResult.java) | Standard framework-owned result object returned by the performance engine. This model supports:  technical execution metrics human-readable reporting context expected failure and actual failure tracking run-level and scenario-level reporting explicit execution |
| [`src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceExecutionStatus.java) | High-level execution status used for reporting, Excel dashboards, summaries, and leadership-readable result interpretation. These values are intentionally simple and business-readable so they can be used consistently across:  scenario summaries run-level repor |
| [`src/main/java/com/ptaf/performance/models/PerformanceProfile.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceProfile.java) | Defines the execution load profile for a performance run. This object is architect-controlled and reused by all performance scenarios. Reporting-safe goals:  keep constructor compatibility normalize invalid values provide small helper methods for report interp |
| [`src/main/java/com/ptaf/performance/models/PerformanceRequest.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceRequest.java) | Framework-owned immutable request model for performance execution. This object is constructed only through builder layers and consumed by architect-controlled engine/test-plan classes. Reporting-safe goals:  keep constructor compatibility normalize request fie |
| [`src/main/java/com/ptaf/performance/models/PerformanceRunReport.java`](../../../src/main/java/com/ptaf/performance/models/PerformanceRunReport.java) | Run-level performance report model. This object represents one full performance execution run and contains all scenario-level results that belong to the same run. Design goals: - keep reporting calculations centralized - remain backward-compatible for enterpri |

### `com.ptaf.performance.payloads`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java`](../../../src/main/java/com/ptaf/performance/payloads/CsvPayloadReader.java) | Framework-owned CSV payload reader. Resolution order for locating the CSV file: <ol> classpath resource (as provided) classpath resource (with leading '/' removed) direct filesystem path (absolute or relative) src/test/resources fallback (useful for tests) </o |
| [`src/main/java/com/ptaf/performance/payloads/PayloadSourceType.java`](../../../src/main/java/com/ptaf/performance/payloads/PayloadSourceType.java) | Supported payload source types for performance request bodies. This enum represents the different ways a request payload can be provided to the performance testing framework. The value chosen by the caller or configuration will determine how the framework inte |
| [`src/main/java/com/ptaf/performance/payloads/PerformancePayloadDefinition.java`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadDefinition.java) | Immutable payload definition that describes how to obtain request body content from different supported sources. This class encapsulates all possible metadata required to resolve a payload from one of several sources:  INLINE - raw body provided directly as a  |
| [`src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java`](../../../src/main/java/com/ptaf/performance/payloads/PerformancePayloadResolver.java) | Central resolver for performance request payloads.  This utility class provides a single entry point to obtain payload bodies used in performance tests. Payloads can originate from different sources: inline, YAML, CSV, or Excel. The resolver delegates retrieva |

### `com.ptaf.performance.reports`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/reports/PerformanceExcelFormatHelper.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelFormatHelper.java) | Central formatting helper for Excel performance reporting.  This utility class provides a single place to perform consistent, business-friendly formatting of numbers, durations, and text that are used across performance report Excel sheets. Methods are designe |
| [`src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceExcelReportWriter.java) | Writes a single Excel report for an entire performance run.  This class is responsible for turning a PerformanceRunReport model into a user-friendly, multi-sheet Excel workbook. Each sheet is laid out in a business-readable style and includes both numeric tabl |
| [`src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceReportManager.java) | Centralized report manager for all performance execution artifacts. This class is framework-owned and supports the current PTAF reporting model:  one shared run-level root folder per execution one scenario-level subfolder per performance scenario technical and |
| [`src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java`](../../../src/main/java/com/ptaf/performance/reports/PerformanceSummaryWriter.java) | Writes standardized summary artifacts for completed performance executions. This writer produces:  technical summary for QA / engineering readable summary for leadership / non-technical stakeholders   Reporting-safe goals:  keep artifact generation stable alig |

### `com.ptaf.performance.utils`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java`](../../../src/main/java/com/ptaf/performance/utils/PerformancePathResolver.java) | Resolves standardized output paths for the Performance Engine. This version is aligned with the current PTAF reporting structure:  one run-level root folder per execution one scenario-level subfolder per scenario fixed artifact names within each scenario folde |

### `com.ptaf.reporting`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java`](../../../src/main/java/com/ptaf/reporting/GlassPdfSubprocessGenerator.java) | Standalone main class that generates a single Glass-style PDF report for one feature. Invoked as a subprocess by { PerFeatureReportListener} so each PDF runs in its own JVM, avoiding the static font caching bug in { tech.grasshopper.pdf.font.ReportFont}. Argum |
| [`src/main/java/com/ptaf/reporting/PerFeatureReportListener.java`](../../../src/main/java/com/ptaf/reporting/PerFeatureReportListener.java) | PTAF Per-Feature Extent Report Listener. This Cucumber { ConcurrentEventListener} generates one individual Extent HTML report AND one full Glass-style PDF report per feature file when the config switch is enabled. <h3>Report naming</h3> Each report is named af |
| [`src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java`](../../../src/main/java/com/ptaf/reporting/SoftAssertionReportListener.java) | SoftAssertionReportListener — a purely additive Cucumber { ConcurrentEventListener} that makes soft-failed steps appear as FAILED (red) in Extent HTML and PDF reports. <h3>Why this is needed</h3> In soft assertion mode, { FrameCommonMethods.executeStep()} and  |

### `com.ptaf.softassert`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/softassert/SoftAssertionContext.java`](../../../src/main/java/com/ptaf/softassert/SoftAssertionContext.java) | Thread-local context that collects soft assertion failures during a scenario. <h3>Purpose</h3> When { soft_assertions.enabled: true} in { config.yml}, this class acts as the central collector for all step failures within a single scenario. Instead of stopping  |

### `com.ptaf.ui.action_performer`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java`](../../../src/main/java/com/ptaf/ui/action_performer/ActionPerformer.java) | ActionPerformer (optimized) What this version guarantees: - Uses your config time_to_wait_in_seconds as MAX timeout (e.g., 10s). - If element is already ready -> NO extra waiting; action runs immediately. - If element is not ready -> waits up to MAX timeout fo |
| [`src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java`](../../../src/main/java/com/ptaf/ui/action_performer/ElementActionImpl.java) | ElementActionImpl is an implementation of the ElementAction interface that provides methods for performing actions and assertions on web elements within an instance of a Playwright Page or FrameLocator. It leverages helper classes (ActionPerformer, LocatorHand |

### `com.ptaf.ui.assertions`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/assertions/UIAssert.java`](../../../src/main/java/com/ptaf/ui/assertions/UIAssert.java) | UIAssert Semantic assertion layer on top of ElementActionImpl + ActionPerformer. Each assertion delegates to existing ActionPerformer actions so we keep one source of truth. Typical usage from a step definition: UIAssert uiAssert = new UIAssert(page); uiAssert |

### `com.ptaf.ui.handlers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/handlers/LocatorHandler.java`](../../../src/main/java/com/ptaf/ui/handlers/LocatorHandler.java) | Utility that maps a simple "locator type" (e.g. "CSS", "BUTTON", "ID", "TEXTBOX", "ROLE", etc.) to a Playwright Locator for three different contexts: - Page (top-level) - FrameLocator (inside an iframe) - Locator (chained off an existing locator)  Behaviour no |

### `com.ptaf.ui.helpers`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java`](../../../src/main/java/com/ptaf/ui/helpers/ElementLocatorHelper.java) | Helper utility to locate element definitions from a YAML-backed configuration and to parse locator "type" and "value" tokens used throughout the UI tests. Responsibilities: - Retrieve a specific element property from YAML using the pattern "elements.{element}. |

### `com.ptaf.ui.interfaces`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/interfaces/ElementAction.java`](../../../src/main/java/com/ptaf/ui/interfaces/ElementAction.java) | The ElementAction interface defines a set of methods for interacting with web elements on a page or within frames using the Playwright framework. This interface provides a thin abstraction layer over Playwright's Page/Locator/Frame primitives to support common |
| [`src/main/java/com/ptaf/ui/interfaces/ElementLocator.java`](../../../src/main/java/com/ptaf/ui/interfaces/ElementLocator.java) | ElementLocator is a small abstraction that hides the details of how locators are created for Playwright-based UI interactions. Implementations of this interface are responsible for converting a human-friendly locatorType + locator string into a Playwright Loca |

### `com.ptaf.ui.mobilebrowser`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserEvidenceManager.java) | Evidence capture manager for Playwright mobile-browser emulation.  This utility class centralizes the logic for deciding when to capture screenshots from a Playwright Page instance during Cucumber scenario execution and for persisting those screenshots to the  |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserExecutionConfig.java) | Runtime execution controls for Playwright mobile-browser emulation.  This utility class centralizes access to configuration properties that control mobile-browser-specific behavior such as evidence collection (screenshots, video), visual regression settings, a |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfile.java) | Immutable representation of a mobile or tablet browser profile used for Playwright emulation. This class encapsulates a set of properties (viewport size, screen size, device scale factor, user agent, platform, etc.) required to simulate a specific device in au |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserProfileRepository.java) | Utility repository that reads mobile browser profile definitions from a YAML source and exposes them as tester-friendly { MobileBrowserProfile} objects. Profiles are read from a YAML map under the top-level key "mobile_browser_profiles". Each profile must be a |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserVisualValidator.java) | Utility class that provides pixel-based visual validation for Playwright mobile-browser profiles.  This class captures a full-page screenshot from a Playwright Page, compares it to a stored baseline image, writes artifacts (actual, diff and optionally baseline |
| [`src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java`](../../../src/main/java/com/ptaf/ui/mobilebrowser/MobileBrowserYamlReader.java) | Isolated YAML reader for Playwright mobile-browser emulation profiles. This utility class loads all YAML files found in the classpath folder "mobile_browser" at class initialization time and merges them into a single in-memory map structure. YAML contents are  |

### `com.ptaf.ui.page_helper`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/page_helper/PageHelper.java`](../../../src/main/java/com/ptaf/ui/page_helper/PageHelper.java) | Utility wrapper around a Playwright { Page} that centralizes common page-level helpers and exposes a shared { LocatorHandler} instance for resolving and interacting with element locators.  Purpose: - Provide a thin abstraction on top of the raw Playwright Page |

### `com.ptaf.ui.pages`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java`](../../../src/main/java/com/ptaf/ui/pages/FrameCommonMethods.java) | <h1>FrameCommonMethods</h1>  The <code>FrameCommonMethods</code> class provides utility methods for interacting with elements within iframes in a Playwright-based test automation framework. This class encapsulates common actions such as clicking, filling input |
| [`src/main/java/com/ptaf/ui/pages/PageCommonMethods.java`](../../../src/main/java/com/ptaf/ui/pages/PageCommonMethods.java) | <h1>PageCommonMethods</h1>  The <code>PageCommonMethods</code> class serves as a foundational utility for executing common web element interactions in a Playwright-powered test automation framework. This class encapsulates operations such as clicking, filling  |

### `com.ptaf.ui_performance.config`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceConfiguration.java) | Typed configuration accessors for the isolated UI performance module. Target, load shape, browser mode, limits, evidence, and reporting are read exclusively from { ui_performance/config/ui_performance-config.yml}. Cucumber only describes the journey and trigge |
| [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceLocatorRepository.java) | Resolves regular-style locator definitions from the UI performance-only locator repository. Values use the same { TYPE_value} convention as regular UI element YAML files, such as { CSS_#username}, { Button_Sign in}, or { xpath_//button[ The repository remains  |
| [`src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java`](../../../src/main/java/com/ptaf/ui_performance/config/UiPerformanceYamlReader.java) | Loads only the dedicated UI performance YAML file. This reader deliberately does not use the framework-wide { YamlReader}. Keeping a private configuration store prevents UI performance settings from being merged with normal UI, API, mobile, database, or JMeter |

### `com.ptaf.ui_performance.core`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java`](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceEngine.java) | Executes configurable load, stress, spike, and soak tests with concurrent real browsers. Cucumber/TestNG only triggers this engine. Concurrency is owned here. Each requested virtual user receives its own worker thread, Playwright instance, browser process, con |
| [`src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java`](../../../src/main/java/com/ptaf/ui_performance/core/UiPerformanceStartGate.java) | Coordinates a simultaneous first browser journey across configured UI performance virtual users. Each virtual user launches its independent browser first, marks itself ready, and waits here. The engine releases all ready users together only after every request |

### `com.ptaf.ui_performance.data`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java`](../../../src/main/java/com/ptaf/ui_performance/data/UiPerformanceUserDataReader.java) | Reads isolated UI performance user data from a simple UTF-8 CSV resource. The first column must be { user_id}. The remaining header names become data keys that can be referenced by journey steps using { ${column_name}}. Values are never logged or written to re |

### `com.ptaf.ui_performance.metrics`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java`](../../../src/main/java/com/ptaf/ui_performance/metrics/UiPerformanceMetrics.java) | Calculates browser-journey load metrics from completed real-browser samples. |

### `com.ptaf.ui_performance.model`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceExecutionPlan.java) | A named load, stress, spike, or soak schedule selected entirely from UI performance YAML. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceIterationResult.java) | One completed browser journey for one virtual user in one configured performance stage. The model records timing and sanitized evidence only. It never carries form values, passwords, tokens, cookies, or session data. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceJourney.java) | Browser journey executed independently by each virtual user. Journeys are assembled by the UI performance-only Cucumber glue. They cannot call normal UI step definitions, normal hooks, or normal page lifecycle classes. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunProfile.java) | Immutable runtime, browser, safety, and threshold settings for one UI performance run. The selected load shape is held by { UiPerformanceExecutionPlan}. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceRunResult.java) | Aggregates a complete load, stress, spike, or soak UI performance run. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStage.java) | One sequential stage in a real-browser UI performance profile. Within a stage, every requested user owns one Playwright instance and browser process. A zero ramp starts all prepared users together. A positive ramp distributes first actions across the ramp inte |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStageResult.java) | Final metrics and evidence for one configured UI performance stage. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceStep.java) | One intentionally small browser action in an isolated UI performance journey. Selectors are resolved from the separate UI performance locator repository before the step is created. Values may contain { ${column_name}} placeholders that are resolved in memory f |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceTestType.java) | Supported real-browser UI performance workload shapes. |
| [`src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java`](../../../src/main/java/com/ptaf/ui_performance/model/UiPerformanceUser.java) | One data identity assigned to a virtual browser user. The record stores data only in memory. Its { #toString()} method intentionally exposes only the user identifier and available field names, never usernames, passwords, tokens, or other values. |

### `com.ptaf.ui_performance.reporting`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceDurationFormatter.java) | Formats raw millisecond measurements for human-facing UI performance reports. Machine-readable files retain raw { _ms} fields. This formatter is used only where a tester is reading a report, so a journey measured as { 23383 ms} appears as { 23.383 s} instead. |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceExistingReporterAdapter.java) | Adapts real-browser UI performance stage results into the framework's existing performance report model. This is a reporting adapter only. It does not route browser execution through JMeter and does not misrepresent network samples as browser journeys. Each st |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportManager.java) | Creates a dedicated, timestamped report directory for each UI performance execution. |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceReportWriter.java) | Writes standalone browser-load reports with profile, stage, percentile, throughput, and transaction evidence. |
| [`src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java`](../../../src/main/java/com/ptaf/ui_performance/reporting/UiPerformanceSensitiveTextSanitizer.java) | Removes common credential and token patterns before errors are written to a shareable report. |

### `com.ptaf.utils`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/utils/BrowserFactory.java`](../../../src/main/java/com/ptaf/utils/BrowserFactory.java) | Utility class responsible for creating Playwright Browser and BrowserContext instances. This class centralizes browser creation logic including: - launching different browser types (Chromium, Firefox, WebKit, Edge) - applying mobile browser emulation profiles  |
| [`src/main/java/com/ptaf/utils/ConfigurationProperties.java`](../../../src/main/java/com/ptaf/utils/ConfigurationProperties.java) | ConfigurationProperties is a centralized utility class responsible for retrieving framework-level configuration values from the YAML configuration file.  Enterprise Framework Responsibility: This class provides one controlled access point for configuration val |
| [`src/main/java/com/ptaf/utils/ExcelReader.java`](../../../src/main/java/com/ptaf/utils/ExcelReader.java) | Utility class for reading data from an Excel file.  This class exposes a single public method { #getData(String, String, String)} which reads the first sheet of the provided Excel file and searches for a row whose first cell matches the provided test case name |
| [`src/main/java/com/ptaf/utils/ExcelToYaml.java`](../../../src/main/java/com/ptaf/utils/ExcelToYaml.java) | Utility class to convert Excel (XLSX) sheet data into YAML format.  Usage overview: - Call convertExcelToYaml(testcaseId, excelFilePath, yamlFilePath). - If testcaseId is null or equals "ALL" (case-insensitive), all rows from the first sheet are written to the |
| [`src/main/java/com/ptaf/utils/ExcelWriter.java`](../../../src/main/java/com/ptaf/utils/ExcelWriter.java) | Utility class for writing string values into Excel files using Apache POI.  This class provides a single public static method to write or overwrite data in a specific cell identified by a test case name (row) and a column name (header). It will create the head |
| [`src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java`](../../../src/main/java/com/ptaf/utils/FeatureArtifactNameResolver.java) | Resolves a Cucumber feature's declared { Feature:} name and creates consistent, filesystem-safe artifact filenames. Downloads and videos use the same feature-title source as the framework's per-feature reports. An artifact name follows the pattern { Feature_Na |
| [`src/main/java/com/ptaf/utils/PropertiesReader.java`](../../../src/main/java/com/ptaf/utils/PropertiesReader.java) | PropertiesReader is a utility class designed for loading and managing configuration data stored in .properties files. It reads all .properties files from a list of specified folders, merges the data into a single map, and provides methods for retrieving values |
| [`src/main/java/com/ptaf/utils/ScenarioUtil.java`](../../../src/main/java/com/ptaf/utils/ScenarioUtil.java) | ScenarioUtil is a utility class that provides methods for managing scenarios during test execution, particularly focusing on teardown processes. This class offers functionality to capture and attach screenshots to scenario reports in cases of test failures or  |
| [`src/main/java/com/ptaf/utils/ScreenshotHandler.java`](../../../src/main/java/com/ptaf/utils/ScreenshotHandler.java) | ScreenshotHandler is a utility class that centralizes logic for capturing and attaching screenshots to Cucumber Scenario reports. It is intended to be used during test teardown to provide visual evidence of the browser state when tests fail (or for any other s |
| [`src/main/java/com/ptaf/utils/YamlReader.java`](../../../src/main/java/com/ptaf/utils/YamlReader.java) | YamlReader is a utility class designed for loading and managing configuration data stored in YAML files. It now reads all YAML files from a list of specified folders, merges the data into a single map, and provides methods for retrieving values based on dot-se |

### `com.ptaf.xml`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/xml/XmlCommonMethods.java`](../../../src/main/java/com/ptaf/xml/XmlCommonMethods.java) | High-level XML assertion and extraction methods for use by Cucumber step definitions. This class sits between the raw { XmlFileHandler} (which handles parsing and XPath evaluation) and the { com.ptaf.stepdefinitions.XmlSteps} class (which maps Gherkin sentence |
| [`src/main/java/com/ptaf/xml/XmlContext.java`](../../../src/main/java/com/ptaf/xml/XmlContext.java) | Thread-local context holder for the XML automation module. This class stores the current { XmlFileHandler} instance in a { ThreadLocal} so that each test thread (scenario) has its own isolated XML document. This follows the same pattern used by the PTAF UI mod |
| [`src/main/java/com/ptaf/xml/XmlFileHandler.java`](../../../src/main/java/com/ptaf/xml/XmlFileHandler.java) | Core XML parsing and querying engine for the PTAF XML automation module. This class is responsible for:  Loading XML content from a filesystem path or a raw string (e.g., extracted from a UI element). Parsing the XML into a W3C { Document} object using the sta |

### `com.ptaf.zip`

| Source | Declared purpose |
|---|---|
| [`src/main/java/com/ptaf/zip/ZipContext.java`](../../../src/main/java/com/ptaf/zip/ZipContext.java) | ZipContext — ThreadLocal context holder for ZIP extraction state within FNB-ETAF. This class stores the result of the most recent ZIP extraction for the current test scenario thread. It follows the same pattern as { CsvContext} and { XmlContext} to ensure thre |
| [`src/main/java/com/ptaf/zip/ZipFileHandler.java`](../../../src/main/java/com/ptaf/zip/ZipFileHandler.java) | ZipFileHandler — Utility class for ZIP file operations within FNB-ETAF. This class provides the following capabilities:  <strong>Unzip:</strong> Extracts a ZIP file to a configurable target directory. Supports nested ZIPs (ZIPs inside ZIPs) with recursive extr |

## Test and runner Java source catalog

The following records test-side classes, including runners, step definitions, contract tests, and test support. They may be selected only by particular suites or profiles.

### `com.ptaf.runner`

| Source | Declared purpose |
|---|---|
| [`src/test/java/com/ptaf/runner/TestRunner.java`](../../../src/test/java/com/ptaf/runner/TestRunner.java) | TestNG Cucumber Runner — used by testng.xml and mvn clean test. This class integrates Cucumber with TestNG by extending { AbstractTestNGCucumberTests}. It is referenced by { src/test/resources/testng.xml} and is the entry point when running tests via Maven ({  |

### `com.ptaf.runners`

| Source | Declared purpose |
|---|---|
| [`src/test/java/com/ptaf/runners/ApiTestRunner.java`](../../../src/test/java/com/ptaf/runners/ApiTestRunner.java) | Test runner for API test scenarios.  This class is a JUnit entry point that instructs JUnit to run Cucumber feature files according to the options defined in the {  annotation. The class is intentionally empty — its purpose is solely to hold configuration meta |
| [`src/test/java/com/ptaf/runners/DatabaseTestRunner.java`](../../../src/test/java/com/ptaf/runners/DatabaseTestRunner.java) | DatabaseTestRunner is the dedicated Cucumber runner for database automation scenarios.  Enterprise Framework Responsibility: This runner is responsible for executing database validation scenarios only. Keeping DB execution separated from UI, API, PDF, and Perf |
| [`src/test/java/com/ptaf/runners/MobileTestRunner.java`](../../../src/test/java/com/ptaf/runners/MobileTestRunner.java) | Dedicated Appium test runner for executing native Android and iOS Cucumber scenarios. This class is intentionally empty and acts only as an entry point for JUnit to invoke Cucumber with a specific set of options. Tests are discovered and executed by the Cucumb |
| [`src/test/java/com/ptaf/runners/ParallelRun.java`](../../../src/test/java/com/ptaf/runners/ParallelRun.java) | Test runner that configures and launches Cucumber feature execution using TestNG. This class:  Specifies Cucumber options such as feature locations, step definition glue, reporting plugins, and tags to filter scenarios. Integrates Cucumber with TestNG by exten |
| [`src/test/java/com/ptaf/runners/PerformanceTestRunner.java`](../../../src/test/java/com/ptaf/runners/PerformanceTestRunner.java) | Test runner for performance-related Cucumber scenarios.  This class is intentionally empty and serves only as an entry point for JUnit to invoke Cucumber. The behavior of the test execution is configured through the { CucumberOptions} annotation below.  Who sh |
| [`src/test/java/com/ptaf/runners/Regression_Runner.java`](../../../src/test/java/com/ptaf/runners/Regression_Runner.java) | JUnit runner class for executing Cucumber feature files that are marked for regression testing. This class is intentionally minimal and acts solely as a configuration holder for Cucumber execution through JUnit. All behavior is defined via annotations; there a |
| [`src/test/java/com/ptaf/runners/TestRunner.java`](../../../src/test/java/com/ptaf/runners/TestRunner.java) | TestRunner is the JUnit entry point for executing Cucumber feature files in this project.  This class is intentionally empty and only serves as a configuration holder for Cucumber via annotations. JUnit discovers this runner and delegates to the Cucumber JUnit |

### `com.ptaf.stepdefinitions`

| Source | Declared purpose |
|---|---|
| [`src/test/java/com/ptaf/stepdefinitions/ApiSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ApiSteps.java) | ApiSteps contains the Gherkin step definitions used by Cucumber feature files to perform API testing in a human-readable, stateful manner. This class acts as a thin layer that translates Gherkin steps into calls to ApiCommonMethods. It does not implement HTTP  |
| [`src/test/java/com/ptaf/stepdefinitions/CsvSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/CsvSteps.java) | Cucumber step definitions for CSV automation in the PTAF framework. This class provides two categories of CSV steps: <h3>1. File-based CSV steps</h3> Load a CSV file from the filesystem and assert or extract values from it. <pre> Given I load CSV file "src/tes |
| [`src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/DatabaseSteps.java) | DatabaseSteps contains Cucumber step definitions for database validation and data setup.  Enterprise Framework Responsibility: This class provides business-readable Gherkin steps for database automation. It does not manage JDBC connections, build SQL Server co |
| [`src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/FrameCommonSteps.java) | Java source containing `FrameCommonSteps`. Read the source for the authoritative implementation contract. |
| [`src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileBrowserVisualSteps.java) | Cucumber step definitions related to visual regression testing of mobile browser pages. This class provides glue code between Cucumber scenarios and the visual validation infrastructure. It relies on: - Hooks.getPage() to provide the current Playwright Page in |
| [`src/test/java/com/ptaf/stepdefinitions/MobileSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/MobileSteps.java) | Cucumber step definitions for PTAF native mobile Appium automation.  This class exposes a set of high-level step definitions that map Gherkin steps to mobile interactions implemented by the MobileAction implementation and helper classes such as MobileDriverMan |
| [`src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/NewPageCommonSteps.java) | Step definitions used by Cucumber scenarios to interact with pages and frames using Playwright.  This class groups together a set of generic "action" step definitions that operate: - On the current active page (referred to as "new page" in step names) - Inside |
| [`src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PageCommonSteps.java) | Step definitions implementing common page interactions for Cucumber scenarios.  This class delegates almost all work to PageCommonMethods which encapsulates Playwright interactions. Steps are written in a generic way so they can be reused across feature files. |
| [`src/test/java/com/ptaf/stepdefinitions/PdfSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PdfSteps.java) | PdfSteps Purpose: - Minimal step glue to connect Gherkin to the reusable PDF library. Why: - Keeps steps thin and readable; heavy lifting stays in src/main classes. - Works with your existing Hooks and Playwright ActionPerformer. Notes for testers: - These ste |
| [`src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/PerformanceSteps.java) | High-level Cucumber step definitions for running performance tests and validating results.  This class exposes concise, tester-facing steps to: - run simple HTTP performance tests (GET/POST/PUT/DELETE), - run tests with custom load profiles (users, ramp-up, ho |
| [`src/test/java/com/ptaf/stepdefinitions/XmlSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/XmlSteps.java) | Cucumber step definitions for XML automation in the PTAF framework. This class provides two categories of XML steps: <h3>1. File-based XML steps</h3> Load an XML file from the filesystem and assert or extract values from it. <pre> Given I load XML file "src/te |
| [`src/test/java/com/ptaf/stepdefinitions/ZipSteps.java`](../../../src/test/java/com/ptaf/stepdefinitions/ZipSteps.java) | ZipSteps — Cucumber step definitions for ZIP file operations within FNB-ETAF. This class provides steps for:  Unzipping a downloaded or local ZIP file to a configurable extraction directory. Discovering files inside the ZIP by name or extension. Loading CSV or |

### `com.ptaf.ui_performance`

| Source | Declared purpose |
|---|---|
| [`src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceConcurrentBrowserIntegrationTest.java) | Local-only integration contract proving two real browsers begin one measured stage concurrently. |
| [`src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceModuleContractTest.java) | Offline contracts for profiles, configuration, timing reports, and existing Performance reporter reuse. |
| [`src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java`](../../../src/test/java/com/ptaf/ui_performance/UiPerformanceStartGateTest.java) | Offline concurrency contract for the UI performance shared start gate. |

### `com.ptaf.ui_performance.runners`

| Source | Declared purpose |
|---|---|
| [`src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java`](../../../src/test/java/com/ptaf/ui_performance/runners/UiPerformanceRunner.java) | Dedicated TestNG/Cucumber entry point for isolated UI performance scenarios. The runner deliberately scans only { com.ptaf.ui_performance.stepdefinitions}. It does not include { com.ptaf.hooks}, ordinary UI step definitions, mobile hooks, or Extent/PDF listene |

### `com.ptaf.ui_performance.stepdefinitions`

| Source | Declared purpose |
|---|---|
| [`src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java`](../../../src/test/java/com/ptaf/ui_performance/stepdefinitions/UiPerformanceSteps.java) | Cucumber glue for the isolated UI performance module. These steps form a small, explicit journey DSL. Targets, routes, locators, users, and thresholds remain in the dedicated UI performance YAML/CSV files. Feature files describe only the business flow and do n |

## Maintenance and regeneration

Run the following command from the repository root after adding or moving Java sources, YAML, XML, properties, feature files, runners, or step definitions:

```bash
python3 scripts/generate_reference_catalog.py
```

The script generates this appendix only. It does not alter framework behavior or configuration. Review the generated diff and verify that every new public user-facing feature has an explanatory chapter or an intentional cross-reference.

## Source references

- **[1]** [`scripts/generate_reference_catalog.py`](../../../scripts/generate_reference_catalog.py) — generator for this repository inventory.
- **[2]** [`pom.xml`](../../../pom.xml) — Maven build, plugins, profiles, and suite selection.
- **[3]** [`src/test/resources/testng.xml`](../../../src/test/resources/testng.xml) — default TestNG suite configuration.

## References

[1]: ../../../scripts/generate_reference_catalog.py "FNB-ETAF source navigation catalog generator"
[2]: ../../../pom.xml "FNB-ETAF Maven build and profile configuration"
[3]: ../../../src/test/resources/testng.xml "FNB-ETAF default TestNG suite"
