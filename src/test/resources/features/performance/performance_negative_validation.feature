#@performance_testing @performance_negative_testing
#Feature: Performance Negative Validation
#
#  Scenario: Basic auth should fail with invalid credentials
#    When we run basic auth GET performance test for path "/basic-auth/admin/secret" with name "basic_auth_invalid_credentials" username "admin" password "wrong_password"
#    Then performance error percentage should be greater than 0
#    And performance total errors should be greater than 0
#
#  Scenario: Invalid endpoint should produce failed requests
#    When we run GET performance test for path "/invalid-performance-endpoint" with name "invalid_endpoint_failure_test"
#    Then performance error percentage should be greater than 0
#    And performance total errors should be greater than 0