@db
Feature: Database Connection Health Check

  As a PTAF automation framework user
  I want to validate that the framework can connect to the configured SQL Server database
  So that DB automation failures can be separated from environment or access issues

  @database_health_check
  Scenario: Validate SQL Server database connection
    Given I validate the database connection is successful