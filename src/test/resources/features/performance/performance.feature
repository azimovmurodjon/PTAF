@performance_testing
Feature: Navigator GraphQL performance validation

  Scenario: Create VHLM through GraphQL under load
    When we store bearer token alias "NavigatorAuthentication" with value "<raw-JWT>"
    And we run authenticated YAML-driven POST performance test for path "/api/graphql" with name "Create VHLM GraphQL" using yaml key "navigator_graphql_create_vhlm" and bearer token alias "NavigatorAuthentication"
    Then performance execution should pass
