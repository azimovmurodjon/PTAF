@performance_testing
Feature: Happy Path Load Validation

  Scenario: Happy path smoke load
    When we run GET performance test for path "/get" with name "happy_path_5_users" using 5 users ramp 5 seconds hold 10 seconds
    Then performance dashboard path should be generated
    And performance summary file path should be generated
    And performance jtl file path should be generated

  Scenario: Create customer under load
    When we run POST performance test for path "/post" with name "create_customer_10_users" and json body "{\"name\":\"John\"}" using 10 users ramp 5 seconds hold 15 seconds
    Then performance dashboard path should be generated

  Scenario: Authenticated GET performance test
    When we store bearer token alias "default" with value "your_token_here"
    And we run authenticated GET performance test for path "/get" with name "secure_get_5_users" using bearer token alias "default"
    Then performance dashboard path should be generated

  Scenario: Basic auth GET performance test
    When we run basic auth GET performance test for path "/basic-auth" with name "basic_auth_get_5_users" username "admin" password "secret"
    Then performance dashboard path should be generated

