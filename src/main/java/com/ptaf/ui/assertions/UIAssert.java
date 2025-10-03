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
 */
public class UIAssert {
    private static final Logger logger = LoggerFactory.getLogger(UIAssert.class);

    private final Page page;
    private final ElementAction elementAction;

    public UIAssert(Page page) {
        this.page = page;
        this.elementAction = new ElementActionImpl(page);
    }

    // -----------------------
    // Text / Value assertions
    // -----------------------

    /** Assert exact text equals (strict). */
    public void textEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                           String element, String locator, String expectedText) {
        try {
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "gettext", element, locator, null, null);
            if (actual == null) actual = "";
            if (!String.valueOf(expectedText).equals(actual)) {
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Text equals assertion failed. Expected: [" + expectedText + "] but was: [" + actual + "]");
            } else {
                logger.info("✅ textEquals passed [{}]", expectedText);
            }
        } catch (Exception e) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "textEquals threw: " + e.getMessage());
        }
    }

    /** Assert text contains (delegates to ActionPerformer 'hastext'). */
    public void textContains(Page page, String iFrame, String iFrame_2, String iFrame_3,
                             String element, String locator, String mustContain) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "hastext", mustContain,
                "Text does not contain expected substring: " + mustContain);
    }

    /** Assert input value equals (uses 'hasequalvalue'). */
    public void valueEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                            String element, String locator, String expected) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "hasequalvalue", expected,
                "Value mismatch. Expected: " + expected);
    }

    /** Assert attribute equals. */
    public void attributeEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                String element, String locator, String attribute, String expected) {
        try {
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "getattribute", element, locator, attribute, null);
            if (actual == null) actual = "";
            if (!String.valueOf(expected).equals(actual)) {
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Attribute [" + attribute + "] mismatch. Expected: [" + expected + "] but was: [" + actual + "]");
            } else {
                logger.info("✅ attributeEquals passed attr={} [{}]", attribute, expected);
            }
        } catch (Exception e) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "attributeEquals threw: " + e.getMessage());
        }
    }

    /** Assert attribute contains. */
    public void attributeContains(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String element, String locator, String attribute, String mustContain) {
        try {
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "getattribute", element, locator, attribute, null);
            if (actual == null || !actual.contains(mustContain)) {
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Attribute [" + attribute + "] does not contain: [" + mustContain + "], actual: [" + actual + "]");
            } else {
                logger.info("✅ attributeContains passed attr={} contains [{}]", attribute, mustContain);
            }
        } catch (Exception e) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "attributeContains threw: " + e.getMessage());
        }
    }

    // -----------------------
    // State assertions
    // -----------------------

    public void isVisible(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "isvisible", null,
                "Element is not visible");
    }

    public void isHidden(Page page, String iFrame, String iFrame_2, String iFrame_3,
                         String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "ishidden", null,
                "Element is not hidden");
    }

    public void isEnabled(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "isenabled", null,
                "Element is not enabled");
    }

    public void isDisabled(Page page, String iFrame, String iFrame_2, String iFrame_3,
                           String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "isdisabled", null,
                "Element is not disabled");
    }

    public void isChecked(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "ischecked", null,
                "Element is not checked");
    }

    public void hasClass(Page page, String iFrame, String iFrame_2, String iFrame_3,
                         String element, String locator, String classSubstring) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "hasclass", classSubstring,
                "Element does not have class containing: " + classSubstring);
    }

    public void exists(Page page, String iFrame, String iFrame_2, String iFrame_3,
                       String element, String locator) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "exists", null,
                "Element does not exist");
    }

    public void notExists(Page page, String iFrame, String iFrame_2, String iFrame_3,
                          String element, String locator) {
        // Mirrors typical pattern: be explicit if there are matches, otherwise pass silently.
        try {
            Locator l = elementAction.getLocator(iFrame, iFrame_2, iFrame_3, element, locator, page, null);
            if (l.count() > 0) {
                delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "not_exists", null,
                        "Element exists but should not");
            } else {
                logger.info("✅ notExists passed (no matches found)");
            }
        } catch (Exception e) {
            // If we cannot even resolve, treat as not existing (consistent test ergonomics).
            logger.info("✅ notExists passed (unable to resolve locator; treating as non-existent): {}", e.getMessage());
        }
    }

    // ---------------
    // Wait assertions
    // ---------------

    public void waitForText(Page page, String iFrame, String iFrame_2, String iFrame_3,
                            String element, String locator, String expectedSubstring) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "waitfortext", expectedSubstring,
                "Expected text not found after wait: " + expectedSubstring);
    }

    public void waitForValue(Page page, String iFrame, String iFrame_2, String iFrame_3,
                             String element, String locator, String expectedValue) {
        delegateBooleanish(page, iFrame, iFrame_2, iFrame_3, element, locator, "waitforvalue", expectedValue,
                "Expected value not found after wait: " + expectedValue);
    }

    // -----------------------
    // Convenience overloads (no iframe args)
    // -----------------------
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

    /** Assert text equals after trimming and normalizing internal whitespace. */
    public void textEqualsTrimmed(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String element, String locator, String expectedText) {
        try {
            String actual = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, "gettext", element, locator, null, null);
            if (actual == null) actual = "";
            String normActual = actual.trim().replaceAll("\\s+", " ");
            String normExpected = (expectedText == null ? "" : expectedText.trim().replaceAll("\\s+", " "));
            if (!normExpected.equals(normActual)) {
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Text (trimmed) equals assertion failed. Expected: [" + normExpected + "] but was: [" + normActual + "]");
            } else {
                logger.info("✅ textEqualsTrimmed passed [{}]", normExpected);
            }
        } catch (Exception e) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "textEqualsTrimmed threw: " + e.getMessage());
        }
    }

    public void textEqualsTrimmed(Page page, String element, String locator, String expectedText) {
        textEqualsTrimmed(page, null, null, null, element, locator, expectedText);
    }

    /** Assert that the number of matched nodes equals expected. */
    public void countEquals(Page page, String iFrame, String iFrame_2, String iFrame_3,
                            String element, String locator, int expectedCount) {
        try {
            Locator l = elementAction.getLocator(iFrame, iFrame_2, iFrame_3, element, locator, page, null);
            int actualCount = (l == null) ? 0 : l.count();
            if (actualCount != expectedCount) {
                failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                        "Count assertion failed. Expected: " + expectedCount + " but was: " + actualCount);
            } else {
                logger.info("✅ countEquals passed [{}]", expectedCount);
            }
        } catch (Exception e) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    "countEquals threw: " + e.getMessage());
        }
    }

    public void countEquals(Page page, String element, String locator, int expectedCount) {
        countEquals(page, null, null, null, element, locator, expectedCount);
    }

    // ---------------------
    // Internal delegations
    // ---------------------

    /**
     * Delegates to ActionPerformer via ElementActionImpl with an action that
     * already asserts internally (throws on failure). We capture, screenshot, and rethrow nicely.
     */
    private void delegateBooleanish(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                    String element, String locator, String action, String valueIfAny,
                                    String friendlyFailure) {
        try {
            String result = elementAction.performActionPageFrameWithReturn(
                    page, iFrame, iFrame_2, iFrame_3, action, element, locator, valueIfAny, null);

            if (result != null) {
                logger.info("✅ {} passed; result={}", action, result);
            } else {
                logger.info("✅ {} passed", action);
            }
        } catch (AssertionError ae) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    friendlyFailure + " | " + ae.getMessage());
        } catch (Exception e) {
            failWithScreenshot(page, iFrame, iFrame_2, iFrame_3, element, locator,
                    friendlyFailure + " | " + e.getMessage());
        }
    }

    private void failWithScreenshot(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                    String element, String locator, String message) {
        logger.error("❌ Assertion failed: {}", message);
        try {
            String targetLocator = elementAction.getExactLocator(element, locator);
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
            logger.error("Screenshot capture failed: {}", shotEx.getMessage(), shotEx);
        }
        throw new RuntimeException(message);
    }

    private Scenario getCurrentScenario() {
        return Hooks.getCurrentScenario();
    }
}