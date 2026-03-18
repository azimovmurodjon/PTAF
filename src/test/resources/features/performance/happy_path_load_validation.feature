@performance_testing_1
Feature: Performance Happy Path Load Validation
#
##  Scenario: Happy path smoke load
##    When we run GET performance test for path "/get" with name "happy_path_5_users" using 5 users ramp 5 seconds hold 10 seconds
##    Then performance dashboard path should be generated
##
##  Scenario: Happy path light load
##    When we run GET performance test for path "/get" with name "happy_path_25_users" using 25 users ramp 10 seconds hold 20 seconds
##    Then performance dashboard path should be generated
##
##  Scenario: Happy path moderate load
##    When we run GET performance test for path "/get" with name "happy_path_50_users" using 50 users ramp 15 seconds hold 30 seconds
##    Then performance dashboard path should be generated
#
#  Scenario: Happy path heavy load 100 users
#    When we run GET performance test for path "/get" with name "happy_path_100_users" using 100 users ramp 20 seconds hold 45 seconds
#    Then performance dashboard path should be generated
#
##  Scenario: Happy path machine stress check
##    When we run GET performance test for path "/get" with name "happy_path_200_users" using 200 users ramp 30 seconds hold 60 seconds
##    Then performance dashboard path should be generated
##
  Scenario: Happy path extreme local stress
    When we run GET performance test for path "/get" with name "happy_path_300_users" using 300 users ramp 45 seconds hold 90 seconds
    Then performance dashboard path should be generated