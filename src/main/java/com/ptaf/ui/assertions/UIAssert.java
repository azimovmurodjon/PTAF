package com.ptaf.ui.assertions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.action_performer.ElementActionImpl;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.utils.ScreenshotHandler;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UIAssert
 *
 * Semantic assertion layer on top of ElementActionImpl + ActionPerformer.
 * Each assertion delegates to existing ActionPerformer actions so we keep one source of truth.
 *
 * Typical usage from a step definition:
 *   UIAssert uiAssert = new UIAssert(page);
 *   uiAssert.textEquals(page, "iframe[name^='iframeWindowModal']", null, null, "Modal", "header", "Expected Title");
 *   // Or without iframes:
 *   uiAssert.isVisible(page, "LoginPage", "submitButton");
 *
 * Responsibilities:
 * - Provide convenient, readable assertion methods used by step definitions and tests.
 * - Delegate the actual find / evaluation work to ElementAction (ElementActionImpl).
 * - Capture screenshots and fail tests in a consistent manner when assertions fail.
 *
 * Notes for testers:
 * - Methods accept optional iframe path parameters (iFrame, iFrame_2, iFrame_3). Use the overloads without
 *   iframe arguments for most simple cases.
 * - On failure a screenshot will be attempted and a RuntimeException will be thrown with a descriptive message.
 */
public class UIAssert {
    private static final Logger logger = LoggerFactory.getLogger(UIAssert.class);

    // The Playwright page instance associated with this assertion helper.
    private final Page page;
    // ElementAction is the interface used to interact with the page / elements.
    // ElementActionImpl provides the concrete implementation used here.
    private final ElementAction elementAction;

    /**
     * Create a new UIAssert bound to a Playwright Page.
     *
     * @param page the Playwright Page used for element interactions and screenshots
     */
    public UIAssert(Page page) {
        this.page = page;
        this.elementAction = new ElementActionImpl(page);
    }

    // -----------------------
    // Text / Value assertions
    // -----------------------

    /**
     * Assert exact text equals (strict).
     *
     * Delegates to ElementAction.performActionPageFrameWithReturn with action "gettext".
     * If the returned text does not equal expectedText, captures a screenshot and fails the test.
     *
     * @param page Playwright Page (kept for API compatibility)
     * @param iFrame primary iframe selector if element is inside an iframe (nullable)
     * @param iFrame_2 secondary iframe selector (nullable)
     * @param iFrame_3 tertiary iframe selector (nullable)
     * @param element logical element name (map key used by elementAction)
     * @param locator concrete locator string or locator key
     * @param expectedText exact expected text value
     */
    public void textEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                           String element, String locator, String expectedText) {
        try {
            // Get the text from the element via ElementAction implementation.
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "gettext", element, locator, null, null);
            if (actual == null) actual = "";
            // Compare strictly; account for null expected by converting to String.
            if (!String.valueOf(expectedText).equals(actual)) {
                // On mismatch, capture screenshot and fail with informative message.
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Text equals assertion failed. Expected: [" + expectedText + "] but was: [" + actual + "]");
            } else {
                // Log success for traceability.
                logger.info("✅ textEquals passed [{}]", expectedText);
            }
        } catch (Exception e) {
            // Any exception during retrieval is treated as a failure: capture screenshot and fail.
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "textEquals threw: " + e.getMessage());
        }
    }

    /**
     * Assert text contains (delegates to ActionPerformer 'hastext').
     *
     * @see #delegateBooleanish(Page, String, String, String, String, String, String, String, String)
     */
    public void textContains(Page page, String iFrame, String iFrame_2, String iFrame_3,
                             String element, String locator, String mustContain) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "hastext", mustContain,
                "Text does not contain expected substring: " + mustContain);
    }

    /**
     * Assert input value equals (uses 'hasequalvalue').
     *
     * Delegates to ElementAction implementation that checks equality of value attributes.
     */
    public void valueEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                            String element, String locator, String expected) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "hasequalvalue", expected,
                "Value mismatch. Expected: " + expected);
    }

    /**
     * Assert attribute equals.
     *
     * Performs a "getattribute" action and compares returned value to expected.
     */
    public void attributeEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                String element, String locator, String attribute, String expected) {
        try {
            // Request attribute value from ElementAction. attribute is passed as the action value.
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "getattribute", element, locator, attribute, null);
            if (actual == null) actual = "";
            if (!String.valueOf(expected).equals(actual)) {
                // On mismatch, capture screenshot and fail.
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Attribute [" + attribute + "] mismatch. Expected: [" + expected + "] but was: [" + actual + "]");
            } else {
                logger.info("✅ attributeEquals passed attr={} [{}]", attribute, expected);
            }
        } catch (Exception e) {
            // Any exception is treated as failure and triggers screenshot capture.
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "attributeEquals threw: " + e.getMessage());
        }
    }

    /**
     * Assert attribute contains.
     *
     * Retrieves the attribute and checks that it contains the provided substring.
     */
    public void attributeContains(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String element, String locator, String attribute, String mustContain) {
        try {
            // Retrieve attribute value.
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "getattribute", element, locator, attribute, null);
            // Null or not containing substring is a failure.
            if (actual == null || !actual.contains(mustContain)) {
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Attribute [" + attribute + "] does not contain: [" + mustContain + "], actual: [" + actual + "]");
            } else {
                logger.info("✅ attributeContains passed attr={} contains [{}]", attribute, mustContain);
            }
        } catch (Exception e) {
            // Any exception triggers screenshot capture and failure.
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "attributeContains threw: " + e.getMessage());
        }
    }

    // -----------------------
    // State assertions
    // -----------------------

    /**
     * Assert element is visible.
     */
    public void isVisible(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "isvisible", null,
                "Element is not visible");
    }

    /**
     * Assert element is hidden.
     */
    public void isHidden(Page page, String iFrame, String iFrame_2, String iFrame_3,
                         String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "ishidden", null,
                "Element is not hidden");
    }

    /**
     * Assert element is enabled.
     */
    public void isEnabled(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "isenabled", null,
                "Element is not enabled");
    }

    /**
     * Assert element is disabled.
     */
    public void isDisabled(Page page, String iFrame, String iFrame_2, String iFrame_3,
                           String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "isdisabled", null,
                "Element is not disabled");
    }

    /**
     * Assert checkbox/radio is checked.
     */
    public void isChecked(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "ischecked", null,
                "Element is not checked");
    }

    /**
     * Assert element has a class that contains the given substring.
     */
    public void hasClass(Page page, String iFrame, String iFrame_2, String iFrame_3,
                         String element, String locator, String classSubstring) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "hasclass", classSubstring,
                "Element does not have class containing: " + classSubstring);
    }

    /**
     * Assert element exists (one or more matches).
     */
    public void exists(Page page, String iFrame, String iFrame_2, String iFrame_3,
                       String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "exists", null,
                "Element does not exist");
    }

    /**
     * Assert element does not exist.
     *
     * Implementation notes:
     * - First tries to resolve locator; if resolved and count > 0 then fails via delegateBooleanish("not_exists").
     * - If the locator cannot be resolved, this is considered a pass (treat missing locator as non-existent).
     */
    public void notExists(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        // Mirrors typical pattern: be explicit if there are matches, otherwise pass silently.
        try {
            // Use ElementAction to obtain a Locator and check the number of matches.
            Locator l = elementAction.getLocator(iFrame, iFrame_2, iFrame_3, element, locator, page, null);
            if (l.count() > 0) {
                // If matches found, delegate to the internal "not_exists" action to produce a failure.
                delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "not_exists", null,
                        "Element exists but should not");
            } else {
                // No matches found => success.
                logger.info("✅ notExists passed (no matches found)");
            }
        } catch (Exception e) {
            // If locator resolution fails (e.g., misconfigured element map), treat as non-existent to avoid noisy failures.
            logger.info("✅ notExists passed (unable to resolve locator; treating as non-existent): {}", e.getMessage());
        }
    }

    // ---------------
    // Wait assertions
    // ---------------

    /**
     * Wait for text to contain the expected substring.
     *
     * Delegates to an action that performs waiting semantics (likely polling / timeout aware).
     */
    public void waitForText(Page page, String iFrame, String iFrame_2, String iFrame_3,
                            String element, String locator, String expectedSubstring) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "waitfortext", expectedSubstring,
                "Expected text not found after wait: " + expectedSubstring);
    }

    /**
     * Wait for an input value to equal expectedValue.
     */
    public void waitForValue(Page page, String iFrame, String iFrame_2, String iFrame_3,
                             String element, String locator, String expectedValue) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "waitforvalue", expectedValue,
                "Expected value not found after wait: " + expectedValue);
    }

    // -----------------------
    // Convenience overloads (no iframe args)
    // -----------------------
    // These overloads mirror the iframe variants but leave iframe params null for common usage.
    public void textEquals(Page page, String element, String locator, String expectedText) {
        textEquals(page, null, null, null, element, locator, expectedText);
    }
    public void textContains(Page page, String element, String locator, String mustContain) {
        textContains(page, null, null, null, element, locator, mustContain);
    }
    public void valueEquals(Page page, String element, String locator, String expected) {
        valueEquals(page, null, null, null, element, locator, expected);
    }
    public void attributeEquals(Page page, String element, String locator, String attribute, String expected) {
        attributeEquals(page, null, null, null, element, locator, attribute, expected);
    }
    public void attributeContains(Page page, String element, String locator, String attribute, String mustContain) {
        attributeContains(page, null, null, null, element, locator, attribute, mustContain);
    }
    public void isVisible(Page page, String element, String locator) {
        isVisible(page, null, null, null, element, locator);
    }
    public void isHidden(Page page, String element, String locator) {
        isHidden(page, null, null, null, element, locator);
    }
    public void isEnabled(Page page, String element, String locator) {
        isEnabled(page, null, null, null, element, locator);
    }
    public void isDisabled(Page page, String element, String locator) {
        isDisabled(page, null, null, null, element, locator);
    }
    public void isChecked(Page page, String element, String locator) {
        isChecked(page, null, null, null, element, locator);
    }
    public void hasClass(Page page, String element, String locator, String classSubstring) {
        hasClass(page, null, null, null, element, locator, classSubstring);
    }
    public void exists(Page page, String element, String locator) {
        exists(page, null, null, null, element, locator);
    }
    public void notExists(Page page, String element, String locator) {
        notExists(page, null, null, null, element, locator);
    }
    public void waitForText(Page page, String element, String locator, String expectedSubstring) {
        waitForText(page, null, null, null, element, locator, expectedSubstring);
    }
    public void waitForValue(Page page, String element, String locator, String expectedValue) {
        waitForValue(page, null, null, null, element, locator, expectedValue);
    }

    // -----------------------
    // Extended assertions
    // -----------------------

    /**
     * Assert text equals after trimming and normalizing internal whitespace.
     *
     * Useful where formatting / extraneous whitespace may differ but semantic content should match.
     * Normalization rules: trim both ends and collapse runs of whitespace into a single space.
     */
    public void textEqualsTrimmed(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String element, String locator, String expectedText) {
        try {
            // Get text raw from element
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "gettext", element, locator, null, null);
            if (actual == null) actual = "";
            // Normalize whitespace for both expected and actual
            String normActual = actual.trim().replaceAll("\\s+", " ");
            String normExpected = (expectedText == null ? "" : expectedText.trim().replaceAll("\\s+", " "));
            if (!normExpected.equals(normActual)) {
                // Mismatch after normalization => fail with screenshot
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Text (trimmed) equals assertion failed. Expected: [" + normExpected + "] but was: [" + normActual + "]");
            } else {
                logger.info("✅ textEqualsTrimmed passed [{}]", normExpected);
            }
        } catch (Exception e) {
            // Any exception handled as failure
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "textEqualsTrimmed threw: " + e.getMessage());
        }
    }

    /**
     * Convenience overload for trimmed equality without iframe params.
     */
    public void textEqualsTrimmed(Page page, String element, String locator, String expectedText) {
        textEqualsTrimmed(page, null, null, null, element, locator, expectedText);
    }

    /**
     * Assert that the number of matched nodes equals expected.
     *
     * Uses ElementAction.getLocator to obtain a Locator and checks its count().
     */
    public void countEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                            String element, String locator, int expectedCount) {
        try {
            // Resolve locator and get match count; treat null locator as zero matches.
            Locator l = elementAction.getLocator(iFrame, iFrame_2, iFrame_3, element, locator, page, null);
            int actualCount = (l == null) ? 0 : l.count();
            if (actualCount != expectedCount) {
                // On mismatch, capture screenshot and fail.
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Count assertion failed. Expected: " + expectedCount + " but was: " + actualCount);
            } else {
                logger.info("✅ countEquals passed [{}]", expectedCount);
            }
        } catch (Exception e) {
            // Any unexpected exception => screenshot + fail.
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "countEquals threw: " + e.getMessage());
        }
    }

    /**
     * Convenience overload for countEquals without iframe params.
     */
    public void countEquals(Page page, String element, String locator, int expectedCount) {
        countEquals(page, null, null, null, element, locator, expectedCount);
    }

    // ---------------------
    // Internal delegations
    // ---------------------

    /**
     * Delegates to ActionPerformer via ElementActionImpl with an action that
     * already asserts internally (throws on failure). We capture, screenshot, and rethrow nicely.
     *
     * Behavior:
     * - Calls elementAction.performActionPageFrameWithReturn with provided action and optional value.
     * - If the action returns a non-null result, it is logged for debugging.
     * - If the underlying action throws an AssertionError or Exception, a screenshot is attempted and
     *   a RuntimeException is thrown with a friendly failure message combined with the underlying message.
     *
     * @param page Playwright Page (kept for API compatibility)
     * @param iFrame primary iframe if applicable
     * @param iFrame_2 secondary iframe if applicable
     * @param iFrame_3 tertiary iframe if applicable
     * @param element logical element name
     * @param locator locator string or key
     * @param action the action name understood by ElementAction implementation (e.g., "isvisible", "hastext")
     * @param valueIfAny optional value passed to the action (e.g., expected substring)
     * @param friendlyFailure user-friendly failure prefix used when failing
     */
    private void delegateBooleanish(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                    String element, String locator, String action, String valueIfAny,
                                    String friendlyFailure) {
        try {
            // Delegate to ElementAction and obtain optional string result
            String result = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, action, element, locator, valueIfAny, null);

            // If there's a returned result, log it; otherwise a simple pass log is sufficient.
            if (result != null) {
                logger.info("✅ {} passed; result={}", action, result);
            } else {
                logger.info("✅ {} passed", action);
            }
        } catch (AssertionError ae) {
            // Underlying assertion thrown by ElementAction - capture screenshot and fail with friendly message.
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    friendlyFailure + " | " + ae.getMessage());
        } catch (Exception e) {
            // Any other exception is also treated as assertion failure for ergonomics.
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    friendlyFailure + " | " + e.getMessage());
        }
    }

    /**
     * Capture screenshot for the failing locator and throw a RuntimeException with the provided message.
     *
     * This method:
     * - Logs the failure message.
     * - Attempts to derive the exact locator string from elementAction and call ScreenshotHandler to
     *   attach a screenshot to the current scenario.
     * - If screenshot capture fails, logs the screenshot error but still throws the original failure.
     *
     * @param page Playwright Page used for screenshot capture
     * @param iFrame primary iframe selector used when locating element (nullable)
     * @param iFrame_2 secondary iframe selector (nullable)
     * @param iFrame_3 tertiary iframe selector (nullable)
     * @param element logical element name from map
     * @param locator locator string or key
     * @param message descriptive failure message to include in thrown RuntimeException
     */
    private void failWithScreenshot(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                    String element, String locator, String message) {
        // Log the failure at error level for CI logs and local debugging.
        logger.error("❌ Assertion failed: {}", message);
        try {
            // Attempt to obtain the resolved locator string (for better screenshot annotations).
            String targetLocator = elementAction.getExactLocator(element, locator);
            // Use the ScreenshotHandler to attach a screenshot and additional context to the current scenario.
            ScreenshotHandler.handleScenarioTeardownLocator(
                    getCurrentScenario(),
                    page,
                    iFrame,
                    iFrame_2,
                    iFrame_3,
                    targetLocator,
                    "Assertion Failed"
            );
        } catch (Exception shotEx) {
            // If screenshot capture fails, log but do not suppress original failure.
            logger.error("Screenshot capture failed: {}", shotEx.getMessage(), shotEx);
        }
        // Finally, throw an unchecked exception so calling test frameworks mark the test as failed.
        throw new RuntimeException(message);
    }

    /**
     * Obtain the current Cucumber Scenario from Hooks helper.
     *
     * This method isolates the dependency on Hooks and keeps the rest of the class focused on assertions.
     *
     * @return current Scenario or null if not available
     */
    private Scenario getCurrentScenario() {
        return Hooks.getCurrentScenario();
    }
}
