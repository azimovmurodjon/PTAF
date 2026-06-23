@mobile @permissions @timeouts @sample
Feature: Enterprise native mobile permissions and timeout examples

  # This feature is intentionally generic. It validates that all new mobile
  # permission and timeout steps are mapped and demonstrates how project teams
  # should use these steps in real app flows.
  #
  # These permission steps are non-destructive and do not fail when no popup is
  # displayed. This allows the same scenario to run on clean devices, reused
  # devices, CI simulators, and cloud device farms.

  Scenario: Handle optional startup permissions and capture evidence
    When I allow all mobile permission popups if displayed
    When I capture mobile screenshot named "after-optional-permissions"

  Scenario: Use explicit timeout waits around known app checkpoints
    When I wait up to 10 seconds for mobile page theapp locator echoBoxMenu to be visible
    When I capture mobile screenshot named "echo-box-visible-after-explicit-wait"
