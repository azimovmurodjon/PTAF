# PTAF Soft Assertions — Complete Guide

**Framework:** PTAF (Portable Test Automation Framework)  
**Feature:** Soft Assertion Mode (Continue-on-Failure)  
**Audience:** QA engineers and testers  
**Last Updated:** July 2025

---

## Table of Contents

1. [What Is Soft Assertion Mode?](#1-what-is-soft-assertion-mode)
2. [Normal Mode vs Soft Assertion Mode — Side by Side](#2-normal-vs-soft-assertion-mode)
3. [How to Enable and Configure](#3-how-to-enable-and-configure)
4. [How It Works — Step by Step](#4-how-it-works-step-by-step)
5. [What Gets Captured on Failure](#5-what-gets-captured-on-failure)
6. [The Failure Summary Report](#6-the-failure-summary-report)
7. [Supported Automation Types](#7-supported-automation-types)
8. [What Is NOT Soft-Handled](#8-what-is-not-soft-handled)
9. [When to Use Soft Assertions](#9-when-to-use-soft-assertions)
10. [When NOT to Use Soft Assertions](#10-when-not-to-use-soft-assertions)
11. [Configuration Reference](#11-configuration-reference)
12. [Practical Examples](#12-practical-examples)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. What Is Soft Assertion Mode?

In normal automation, when a step fails — for example, an element is not found or an assertion does not match — the test stops immediately, captures a screenshot, and closes the browser. This is called **fail-fast** behavior and it is the default in PTAF.

**Soft assertion mode** changes this behavior. When a step fails, the framework:

1. Captures a screenshot at the exact point of failure.
2. Records the failure with a timestamp and description.
3. **Continues to the next step** without stopping.
4. Keeps the browser or mobile driver open throughout the scenario.
5. At the end of the scenario, if any steps failed, the scenario is marked as **FAILED** with a full summary showing every failure, its timestamp, and its screenshot.

This allows a single test run to reveal all failures in a scenario at once, rather than stopping at the first one.

---

## 2. Normal Mode vs Soft Assertion Mode — Side by Side

| Behavior | Normal Mode (default) | Soft Assertion Mode |
|:---|:---|:---|
| **Config setting** | `soft_assertions.enabled: false` | `soft_assertions.enabled: true` |
| **Step fails** | Test stops immediately | Screenshot captured, test continues |
| **Browser/driver** | Closed immediately on failure | Kept open until end of scenario |
| **Remaining steps** | Skipped | All executed |
| **Scenario result** | FAILED at the first failure | FAILED at end if any steps failed |
| **Failure report** | One failure visible | All failures visible in one run |
| **Session crash** | Stops immediately | Stops immediately (not soft-handled) |
| **Passing scenario** | Same as soft mode | Same as normal mode |

---

## 3. How to Enable and Configure

Open `src/test/resources/config/config.yml` and find the `soft_assertions` section:

```yaml
soft_assertions:
  # Set to true to enable soft assertion mode.
  # Set to false (default) for normal fail-fast behavior.
  enabled: false

  # How many seconds to retry a failed step before giving up and moving on.
  # Only used when enabled: true.
  # Recommended range: 1 to 10 seconds. Default: 3 seconds.
  retry_seconds: 3
```

**To enable soft assertions:**

```yaml
soft_assertions:
  enabled: true
  retry_seconds: 3
```

**To return to normal behavior:**

```yaml
soft_assertions:
  enabled: false
```

When `enabled: false`, the code paths are identical to before this feature was added. There is zero performance impact and zero behavior change.

### Choosing the Right retry_seconds Value

The `retry_seconds` setting controls how long the framework waits for a failed step to recover before giving up and moving on. Keep this value low.

| Value | Recommended for |
|:---|:---|
| `1` | Very fast applications where elements appear almost instantly |
| `3` (default) | Most web and mobile applications — elements typically appear within 1–2 seconds |
| `5` | Applications with slow screen transitions or heavy API calls |
| `10` | Maximum recommended — only for very slow applications |

If an element has not appeared within 3 seconds, it is almost certainly a real failure. Waiting longer wastes time without improving reliability.

---

## 4. How It Works — Step by Step

Here is the exact sequence of events when a step fails in soft assertion mode:

**Step 1 — Step execution begins.**  
The framework attempts to execute the step normally (find the element, perform the action, assert the value).

**Step 2 — Step fails.**  
An exception is thrown (element not found, timeout, assertion mismatch, etc.).

**Step 3 — Screenshot captured immediately.**  
The framework captures a screenshot of the current state of the browser or mobile device at the exact moment of failure. This screenshot is attached to the Extent Report.

**Step 4 — Failure recorded.**  
The failure is recorded in the `SoftAssertionContext` with:
- A timestamp (HH:mm:ss.SSS format)
- A description of what failed (e.g., "Step execution failed on [LoginPage.loginButton]")
- The error message from the exception
- A note that the screenshot was captured

**Step 5 — Execution continues.**  
The browser or mobile driver is NOT closed. The framework moves to the next step in the scenario.

**Step 6 — Scenario completes.**  
All remaining steps are executed (some may pass, some may fail — each failure is recorded separately).

**Step 7 — Soft assertion flush.**  
In the Cucumber `@After` hook, the framework checks if any soft failures were recorded. If yes, it:
- Logs the full failure summary to the Cucumber/Extent report.
- Throws an `AssertionError` to mark the scenario as FAILED.
- The browser/driver is then closed as part of normal teardown.

If no soft failures were recorded (all steps passed), the scenario passes normally.

---

## 5. What Gets Captured on Failure

Every soft failure captures the following evidence:

**Screenshot:** A full-page screenshot of the browser or mobile device at the exact moment the step failed. The screenshot is named with the step description to make it easy to identify (e.g., `SoftFail_Step_execution_failed.png`).

**Failure record:** Each failure is stored with:
- **Timestamp** — the exact time the failure occurred (HH:mm:ss.SSS)
- **Step description** — what action was being attempted (e.g., "Step execution failed on [LoginPage.loginButton]")
- **Error message** — the raw exception message from the framework
- **Screenshot note** — confirmation that a screenshot was captured

All of this information appears in the failure summary at the end of the scenario.

---

## 6. The Failure Summary Report

When a scenario fails in soft assertion mode, the failure message in the Cucumber/Extent report looks like this:

```
╔══════════════════════════════════════════════════════════════╗
║         PTAF SOFT ASSERTION FAILURES — SCENARIO SUMMARY       ║
╚══════════════════════════════════════════════════════════════╝
  Total failures: 3

  [1] 14:32:05.123 — Step execution failed on [LoginPage.loginButton]
      Error    : Timeout 30000ms exceeded while waiting for locator
      Screenshot: captured (see report)

  [2] 14:32:18.456 — Step execution failed on [DashboardPage.welcomeHeader]
      Error    : Element not found: DashboardPage.welcomeHeader
      Screenshot: captured (see report)

  [3] 14:32:31.789 — Step execution failed
      Error    : Expected [SUCCESS] but got [PENDING]
      Screenshot: captured (see report)

  To investigate: review the screenshots attached to this report.
  To disable soft assertions: set soft_assertions.enabled: false in config.yml.
```

This summary is:
- Attached to the scenario in the Cucumber report.
- Visible in the Extent HTML report.
- Included in any per-feature PDF reports if `per_feature_pdf_enabled: true`.

---

## 7. Supported Automation Types

Soft assertion mode works for all four automation types in PTAF:

| Automation Type | Soft Assertions Supported | Screenshot Method |
|:---|:---|:---|
| **Desktop UI (Playwright)** | Yes | Playwright page screenshot |
| **Mobile Browser Simulation (Playwright)** | Yes | Playwright page screenshot |
| **Native Mobile App (Appium)** | Yes | Appium device screenshot via MobileEvidenceManager |
| **Native Mobile Browser (Appium)** | Yes | Appium device screenshot via MobileEvidenceManager |

No configuration changes are needed to enable soft assertions for a specific automation type. The `soft_assertions.enabled` switch applies globally to all types.

---

## 8. What Is NOT Soft-Handled

The following failure types are **never** soft-handled. They always stop the test immediately regardless of the `soft_assertions.enabled` setting:

**Session crashes:** If the Appium driver session is terminated unexpectedly (the app crashed, the device disconnected, the Appium server stopped), the test stops immediately. There is no point continuing because the driver is gone.

**Safari/iOS WebKit context failures:** If the iOS Safari WebKit context becomes unavailable during a browser session, the test stops immediately. These are infrastructure failures, not element-level failures.

**Browser session lost:** If the Playwright browser page is closed or the browser crashes, the test stops immediately.

**Java exceptions outside step execution:** Any exception thrown outside of the step execution wrapper (e.g., in setup code, in hooks) is not soft-handled.

This design is intentional. Soft assertions are for element-level failures (element not found, assertion mismatch, timeout waiting for visibility). Infrastructure failures should always stop the test because there is nothing meaningful to continue with.

---

## 9. When to Use Soft Assertions

Soft assertion mode is most valuable in the following situations:

**Smoke testing after a deployment:** You want to quickly check that all key flows still work after a new deployment. Running in soft assertion mode lets you see all failures in one pass rather than fixing them one at a time.

**Regression testing on a new environment:** When setting up tests on a new environment (staging, UAT), multiple elements may have different locators or the application may behave slightly differently. Soft assertions let you see all the differences at once.

**Data validation scenarios:** When validating a large number of fields on a page or in a document, soft assertions let you see all mismatches in one run rather than stopping at the first one.

**Exploratory test runs:** When you want to understand the current state of the application across many scenarios without stopping at every individual failure.

---

## 10. When NOT to Use Soft Assertions

Soft assertion mode is not appropriate in all situations:

**Dependent steps:** If step 3 fills in a form field and step 4 clicks submit, and step 3 fails (the field was not found), step 4 will also fail because the form was never filled. In this case, soft assertions will report two failures when there is really only one root cause. For tightly coupled step sequences, normal fail-fast mode gives cleaner results.

**Security and compliance tests:** Tests that verify security controls (login rejection, access denial, data masking) should use normal mode. If a security check fails, continuing the test could produce misleading results.

**Performance tests:** Performance tests are not affected by soft assertions (the performance runner does not use the same execution path), but it is good practice to keep performance tests in normal mode.

**CI/CD pipeline gate tests:** If your CI/CD pipeline uses test results as a deployment gate, normal mode gives faster feedback and cleaner failure signals.

**Recommendation:** Use `soft_assertions.enabled: false` (the default) for your primary CI/CD pipeline runs, and use `soft_assertions.enabled: true` for exploratory runs, environment validation, and regression discovery.

---

## 11. Configuration Reference

The full configuration section in `src/test/resources/config/config.yml`:

```yaml
soft_assertions:
  # Master switch for soft assertion mode.
  # false (default): normal fail-fast behavior — first failure stops the test.
  # true: continue-on-failure mode — all steps run, scenario fails at end with full summary.
  enabled: false

  # Retry duration in seconds.
  # When a step fails, the framework waits this long before giving up and moving on.
  # Only used when enabled: true.
  # Valid range: 1 to 60 seconds. Default: 3 seconds.
  # Keep this low — if an element has not appeared in 3 seconds it is almost certainly a real failure.
  retry_seconds: 3
```

**`ConfigurationProperties` methods (for advanced users):**

```java
// Check if soft assertion mode is enabled
boolean enabled = ConfigurationProperties.isSoftAssertionsEnabled();

// Get the configured retry duration in seconds
int retrySeconds = ConfigurationProperties.getSoftAssertionRetrySeconds();
```

---

## 12. Practical Examples

### Example 1 — Normal Mode (Default Behavior)

With `soft_assertions.enabled: false`:

```gherkin
@smoke @login
Scenario: Login flow
  Given I navigate to the application
  When I perform action "click" on page "LoginPage" locator "usernameField"
  And I perform action "fill" on page "LoginPage" locator "usernameField" with value "admin"
  And I perform action "fill" on page "LoginPage" locator "passwordField" with value "secret"
  And I perform action "click" on page "LoginPage" locator "loginButton"
  Then I should see element on page "DashboardPage" locator "welcomeHeader"
```

If `loginButton` is not found: screenshot captured → test stops → browser closes → scenario FAILED.

### Example 2 — Soft Assertion Mode

With `soft_assertions.enabled: true` and `retry_seconds: 3`:

```gherkin
@smoke @login
Scenario: Login flow
  Given I navigate to the application
  When I perform action "click" on page "LoginPage" locator "usernameField"
  And I perform action "fill" on page "LoginPage" locator "usernameField" with value "admin"
  And I perform action "fill" on page "LoginPage" locator "passwordField" with value "secret"
  And I perform action "click" on page "LoginPage" locator "loginButton"
  Then I should see element on page "DashboardPage" locator "welcomeHeader"
```

If `loginButton` is not found: screenshot captured → failure recorded → execution continues to next step → `welcomeHeader` also fails (because login never happened) → that failure is also recorded → scenario ends → FAILED with summary showing both failures.

### Example 3 — Data Validation with Soft Assertions

This is where soft assertions shine. Validating many fields at once:

```gherkin
@regression @data_validation
Scenario: Verify all fields on the order confirmation page
  Given I navigate to the application
  When I perform action "click" on page "OrderPage" locator "confirmOrderButton"
  Then I should see element on page "ConfirmationPage" locator "orderIdLabel"
  And I should see element on page "ConfirmationPage" locator "totalAmountLabel"
  And I should see element on page "ConfirmationPage" locator "statusLabel"
  And I should see element on page "ConfirmationPage" locator "deliveryDateLabel"
  And I should see element on page "ConfirmationPage" locator "paymentMethodLabel"
  And I should see element on page "ConfirmationPage" locator "shippingAddressLabel"
```

With normal mode: if `totalAmountLabel` is missing, the test stops and you never know whether the other 4 fields are present.

With soft assertion mode: all 6 assertions run. If 2 are missing, you see both failures in one report. You fix both at once and the next run passes.

### Example 4 — Mobile Native App with Soft Assertions

Soft assertions work identically for mobile native app automation:

```gherkin
@mobile @android @smoke
Scenario: Verify product page elements
  Given I start mobile app using platform "android"
  When I tap on mobile page "HomePage" locator "searchBar"
  And I enter mobile text "SearchPage" "searchBar" "headphones"
  And I tap on mobile page "SearchPage" locator "firstResult"
  Then I should see mobile element on page "ProductPage" locator "productTitle"
  Then I should see mobile element on page "ProductPage" locator "productPrice"
  Then I should see mobile element on page "ProductPage" locator "addToCartButton"
  Then I should see mobile element on page "ProductPage" locator "productRating"
```

With soft assertion mode: if `productRating` is missing (perhaps it was removed in a recent app update), the test continues, captures a screenshot, and reports the missing element at the end — without stopping the entire test.

---

## 13. Troubleshooting

| Problem | Likely Cause | Solution |
|:---|:---|:---|
| Soft assertions not working (test still stops on first failure) | `soft_assertions.enabled` is `false` or the YAML key is misspelled. | Check `config.yml`. The key must be exactly `soft_assertions.enabled: true`. |
| All steps after a failure are also failing | The steps are dependent — step N+1 relies on step N having succeeded. | This is expected behavior. Soft assertions reveal all failures, including cascading ones. Fix the root cause first. |
| Screenshots are not appearing in the report | The screenshot capture failed silently. | Check the test output directory. Ensure the `ScreenshotHandler` is configured correctly. |
| The scenario passes even though some steps failed | The soft failures were cleared before the `@After` hook ran. | This should not happen with the current implementation. If it does, check that `SoftAssertionContext.clear()` is not being called from a step definition. |
| `retry_seconds` has no effect | The setting is only used for the retry window before giving up. If the element never appears, the failure is recorded after `retry_seconds` and execution continues. | This is correct behavior. The retry window is not a polling loop — it is the standard Playwright/Appium explicit wait. |
| Mobile session crashes in soft assertion mode | The Appium driver session was terminated. | Session crashes are not soft-handled by design. The test stops immediately. Check the Appium server logs for the crash cause. |

---

*This document covers the PTAF Soft Assertions feature. The configuration keys, class names, and behavior described here are specific to the PTAF framework.*
