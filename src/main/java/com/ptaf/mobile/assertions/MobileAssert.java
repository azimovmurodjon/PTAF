package com.ptaf.mobile.assertions;

import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import com.ptaf.mobile.pages.MobileCommonMethods;
import org.junit.Assert;

/**
 * Utility class that provides a set of assertion helpers tailored for native Appium mobile tests.
 *
 * <p>Each helper method performs a boolean/text verification using MobileCommonMethods and, on failure,
 * captures a failure screenshot via MobileEvidenceManager before delegating to a JUnit assertion.
 *
 * <p>Design notes:
 * - This class is a collection of static helpers and is not meant to be instantiated.
 * - Failures will capture evidence with a descriptive filename prefix and message to aid testers/debugging.
 *
 * Usage example:
 * MobileAssert.assertVisible("LoginPage", "loginButton");
 *
 * @see MobileCommonMethods
 * @see MobileEvidenceManager
 * @see MobileDriverManager
 */
public final class MobileAssert {
    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * <p>This class only contains static assertion helpers. Instantiation is prevented explicitly
     * to make the intent clear and to avoid misuse.
     *
     * @throws IllegalStateException always thrown to prevent instantiation
     */
    private MobileAssert() { throw new IllegalStateException("Utility class"); }

    /**
     * Factory helper that creates a MobileCommonMethods instance bound to the current mobile driver.
     *
     * <p>Using this method centralizes driver retrieval so all assertions operate on the same driver
     * instance managed by MobileDriverManager.
     *
     * @return a new MobileCommonMethods tied to the current driver
     */
    private static MobileCommonMethods methods() { return new MobileCommonMethods(MobileDriverManager.getDriver()); }

    /**
     * Asserts that the element identified by the given page and locator is visible.
     *
     * <p>If the element is not visible, a failure screenshot is captured with a filename prefix
     * "not_visible_{page}_{locator}" and a descriptive message, then a JUnit assertion fails.
     *
     * @param page    logical page name used by your mobile page definitions (used for locating the element)
     * @param locator element locator key on the specified page
     * @throws AssertionError if the element is not visible
     */
    public static void assertVisible(String page, String locator) {
        // Check visibility using the page helper methods
        boolean visible = methods().isVisible(page, locator);

        // On failure, capture evidence (screenshot + message) to help debugging
        if (!visible) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(
                MobileDriverManager.getDriver(),
                "not_visible_" + page + "_" + locator,
                "Expected mobile element to be visible: " + page + "." + locator
            );
        }

        // Delegate to JUnit assertion which throws AssertionError if false.
        Assert.assertTrue("Expected mobile element to be visible: " + page + "." + locator, visible);
    }

    /**
     * Asserts that the element identified by the given page and locator is not visible.
     *
     * <p>If the element is visible, a failure screenshot is captured with a filename prefix
     * "unexpected_visible_{page}_{locator}" and a descriptive message, then a JUnit assertion fails.
     *
     * @param page    logical page name used by your mobile page definitions
     * @param locator element locator key on the specified page
     * @throws AssertionError if the element is visible
     */
    public static void assertNotVisible(String page, String locator) {
        // Check visibility using the page helper methods
        boolean visible = methods().isVisible(page, locator);

        // If the element is unexpectedly visible, capture evidence to aid troubleshooting
        if (visible) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(
                MobileDriverManager.getDriver(),
                "unexpected_visible_" + page + "_" + locator,
                "Expected mobile element not to be visible: " + page + "." + locator
            );
        }

        // Use JUnit to assert false; will throw AssertionError on failure.
        Assert.assertFalse("Expected mobile element not to be visible: " + page + "." + locator, visible);
    }

    /**
     * Asserts that the element identified by the given page and locator is enabled (interactable).
     *
     * <p>If the element is not enabled, a failure screenshot is captured with a filename prefix
     * "not_enabled_{page}_{locator}" and a descriptive message, then a JUnit assertion fails.
     *
     * @param page    logical page name used by your mobile page definitions
     * @param locator element locator key on the specified page
     * @throws AssertionError if the element is not enabled
     */
    public static void assertEnabled(String page, String locator) {
        // Check enabled state using the page helper methods
        boolean enabled = methods().isEnabled(page, locator);

        // On failure, capture screenshot with context to help testers understand the state
        if (!enabled) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(
                MobileDriverManager.getDriver(),
                "not_enabled_" + page + "_" + locator,
                "Expected mobile element to be enabled: " + page + "." + locator
            );
        }

        // Assert using JUnit; failure throws AssertionError.
        Assert.assertTrue("Expected mobile element to be enabled: " + page + "." + locator, enabled);
    }

    /**
     * Asserts that the text of the element identified by the given page and locator equals the expected value.
     *
     * <p>Behavior:
     * - Retrieves the actual text via MobileCommonMethods#getText.
     * - If the actual text is null or does not equal the expected string, captures a failure screenshot
     *   with prefix "text_not_equal_{page}_{locator}" and includes expected/actual values in the message.
     * - Delegates to JUnit's Assert.assertEquals which will throw AssertionError on mismatch.
     *
     * @param page     logical page name used by your mobile page definitions
     * @param locator  element locator key on the specified page
     * @param expected the exact text expected to be present on the element
     * @throws AssertionError if the actual text is null or not equal to expected
     */
    public static void assertTextEquals(String page, String locator, String expected) {
        // Get the actual text from the element
        String actual = methods().getText(page, locator);

        // If mismatch or null, capture evidence with explicit expected/actual values
        if (actual == null || !actual.equals(expected)) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(
                MobileDriverManager.getDriver(),
                "text_not_equal_" + page + "_" + locator,
                "Expected text [" + expected + "] but was [" + actual + "]"
            );
        }

        // Use JUnit equality assertion (message is generic; details captured above)
        Assert.assertEquals("Unexpected mobile text", expected, actual);
    }

    /**
     * Asserts that the text of the element identified by the given page and locator contains the expected substring.
     *
     * <p>Behavior:
     * - Retrieves the actual text via MobileCommonMethods#getText.
     * - Determines containment (safely handles null actual values).
     * - If the expected substring is not contained, captures a failure screenshot with prefix
     *   "text_not_contains_{page}_{locator}" and includes expected/actual in the message.
     * - Delegates to JUnit's Assert.assertTrue which will throw AssertionError on failure.
     *
     * @param page     logical page name used by your mobile page definitions
     * @param locator  element locator key on the specified page
     * @param expected the substring expected to be present in the element's text
     * @throws AssertionError if actual text is null or does not contain expected substring
     */
    public static void assertTextContains(String page, String locator, String expected) {
        // Retrieve the actual text from the element (may be null)
        String actual = methods().getText(page, locator);

        // Safely check containment: ensure actual is not null then check contains
        boolean contains = actual != null && actual.contains(expected);

        // If not contained, capture screenshot with contextual message for testers
        if (!contains) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(
                MobileDriverManager.getDriver(),
                "text_not_contains_" + page + "_" + locator,
                "Expected text to contain [" + expected + "] but was [" + actual + "]"
            );
        }

        // Final assertion: will throw AssertionError if condition is false.
        Assert.assertTrue("Expected text to contain " + expected + " but was " + actual, contains);
    }
}
