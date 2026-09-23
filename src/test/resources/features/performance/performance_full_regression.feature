@performance_testing @performance_full_regression
Feature: Full Phase 1 Performance Engine Validation

  #
  # This feature validates the architect-controlled PTAF Performance Engine end to end.
  # It is intended as a regression/smoke suite for core performance functionality.
  #

  @performance_get
  Scenario: Validate basic GET performance execution
    When we run GET performance test for path "/get" with name "basic_get_performance_test"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance jtl file path should be generated
    And performance run report root path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5
    And performance average response time should be less than 5000 ms
    And performance p95 response time should be less than 8000 ms

  @performance_get @performance_profile
  Scenario: Validate custom-profile GET performance execution
    When we run GET performance test for path "/get" with name "custom_profile_get_test" using 5 users ramp 5 seconds hold 10 seconds
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_post @performance_inline_json
  Scenario: Validate inline JSON POST performance execution
    When we run POST performance test for path "/post" with name "inline_post_performance_test" and json body "{\"name\":\"PTAF\",\"type\":\"performance\"}"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_post @performance_inline_json @performance_profile
  Scenario: Validate inline JSON POST with custom load profile
    When we run POST performance test for path "/post" with name "inline_post_profile_test" and json body "{\"name\":\"PTAF\",\"mode\":\"profile\"}" using 5 users ramp 5 seconds hold 10 seconds
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_put @performance_inline_json
  Scenario: Validate inline JSON PUT performance execution
    When we run PUT performance test for path "/put" with name "inline_put_performance_test" and json body "{\"update\":\"true\",\"source\":\"ptaf\"}"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_put @performance_inline_json @performance_profile
  Scenario: Validate inline JSON PUT with custom load profile
    When we run PUT performance test for path "/put" with name "inline_put_profile_test" and json body "{\"update\":\"profile\",\"enabled\":\"yes\"}" using 5 users ramp 5 seconds hold 10 seconds
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_delete
  Scenario: Validate DELETE performance execution
    When we run DELETE performance test for path "/delete" with name "delete_performance_test"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_delete @performance_profile
  Scenario: Validate DELETE performance execution with custom load profile
    When we run DELETE performance test for path "/delete" with name "delete_profile_test" using 5 users ramp 5 seconds hold 10 seconds
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_yaml @performance_post
  Scenario: Validate YAML-driven POST performance execution
    When we run YAML-driven POST performance test for path "/post" with name "yaml_post_performance_test" using yaml key "performance.payloads.createCustomer"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_yaml @performance_put
  Scenario: Validate YAML-driven PUT performance execution
    When we run YAML-driven PUT performance test for path "/put" with name "yaml_put_performance_test" using yaml key "performance.payloads.updateCustomer"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_auth @performance_bearer
  Scenario: Validate bearer token storage and authenticated GET execution
    When we store bearer token alias "valid_token" with value "sample_token_123"
    And we run authenticated GET performance test for path "/bearer" with name "authenticated_get_test" using bearer token alias "valid_token"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance total samples should be greater than 0

  @performance_auth @performance_bearer @performance_yaml
  Scenario: Validate authenticated YAML-driven POST execution
    When we store bearer token alias "api_token" with value "sample_token_456"
    And we run authenticated YAML-driven POST performance test for path "/post" with name "authenticated_yaml_post_test" using yaml key "performance.payloads.secureCreateCustomer" and bearer token alias "api_token"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance total samples should be greater than 0

  @performance_auth @performance_basic_auth
  Scenario: Validate basic auth GET performance execution
    When we run basic auth GET performance test for path "/basic-auth/admin/secret" with name "basic_auth_get_test" username "admin" password "secret"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should pass
    And performance total samples should be greater than 0
    And performance error percentage should be less than 5

  @performance_negative @performance_expected_failure
  Scenario: Validate invalid endpoint expected failure execution
    When we run GET performance test expecting failure for path "/invalid-performance-endpoint" with name "invalid_endpoint_failure_test"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should fail
    And performance execution should be in expected failure mode
    And performance total errors should be greater than 0
    And performance error percentage should be greater than 0

  @performance_negative @performance_expected_failure @performance_basic_auth
  Scenario: Validate invalid basic auth expected failure execution
    When we run basic auth GET performance test expecting failure for path "/basic-auth/admin/secret" with name "basic_auth_invalid_credentials_test" username "admin" password "wrong_password"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should fail
    And performance execution should be in expected failure mode
    And performance total errors should be greater than 0
    And performance error percentage should be greater than 0

  @performance_negative @performance_expected_failure @performance_yaml
  Scenario: Validate YAML-driven POST expected failure execution
    When we run YAML-driven POST performance test expecting failure for path "/status/500" with name "yaml_post_expected_failure_test" using yaml key "performance.payloads.createCustomer"
    Then performance result should be available
    And performance dashboard path should be generated
    And performance summary file path should be generated
    And performance readable summary file path should be generated
    And performance excel report should be generated
    And performance execution should fail
    And performance execution should be in expected failure mode
    And performance total errors should be greater than 0
    And performance error percentage should be greater than 0