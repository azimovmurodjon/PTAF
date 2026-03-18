#@performance_testing @performance_payload_testing
#Feature: Performance Payload Driven Validation
#
#  Scenario: YAML-driven POST happy path
#    When we run YAML-driven POST performance test for path "/post" with name "yaml_post_happy_path" using yaml key "performance_payloads.create_customer"
#    Then performance dashboard path should be generated
#    And performance summary file path should be generated
#    And performance jtl file path should be generated
#
##  Scenario: CSV-driven POST happy path
##    When we run CSV-driven POST performance test for path "/post" with name "csv_post_happy_path" using csv file "performance/payloads/csv/customers.csv" row "customer_1" column "request_body"
##    Then performance dashboard path should be generated
##
##  Scenario: Excel-driven PUT happy path
##    When we run Excel-driven PUT performance test for path "/put" with name "excel_put_happy_path" using excel file "src/test/resources/performance/payloads/excel/performance_payloads.xlsx" row "customer_1" column "request_body"
##    Then performance dashboard path should be generated