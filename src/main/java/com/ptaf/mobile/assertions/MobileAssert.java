package com.ptaf.mobile.assertions;

import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import com.ptaf.mobile.pages.MobileCommonMethods;
import org.junit.Assert;

/** Mobile assertion helpers for native Appium scenarios. */
public final class MobileAssert {
    private MobileAssert() { throw new IllegalStateException("Utility class"); }
    private static MobileCommonMethods methods() { return new MobileCommonMethods(MobileDriverManager.getDriver()); }

    public static void assertVisible(String page, String locator) {
        boolean visible = methods().isVisible(page, locator);
        if (!visible) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(MobileDriverManager.getDriver(), "not_visible_" + page + "_" + locator, "Expected mobile element to be visible: " + page + "." + locator);
        }
        Assert.assertTrue("Expected mobile element to be visible: " + page + "." + locator, visible);
    }

    public static void assertNotVisible(String page, String locator) {
        boolean visible = methods().isVisible(page, locator);
        if (visible) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(MobileDriverManager.getDriver(), "unexpected_visible_" + page + "_" + locator, "Expected mobile element not to be visible: " + page + "." + locator);
        }
        Assert.assertFalse("Expected mobile element not to be visible: " + page + "." + locator, visible);
    }

    public static void assertEnabled(String page, String locator) {
        boolean enabled = methods().isEnabled(page, locator);
        if (!enabled) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(MobileDriverManager.getDriver(), "not_enabled_" + page + "_" + locator, "Expected mobile element to be enabled: " + page + "." + locator);
        }
        Assert.assertTrue("Expected mobile element to be enabled: " + page + "." + locator, enabled);
    }

    public static void assertTextEquals(String page, String locator, String expected) {
        String actual = methods().getText(page, locator);
        if (actual == null || !actual.equals(expected)) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(MobileDriverManager.getDriver(), "text_not_equal_" + page + "_" + locator, "Expected text [" + expected + "] but was [" + actual + "]");
        }
        Assert.assertEquals("Unexpected mobile text", expected, actual);
    }

    public static void assertTextContains(String page, String locator, String expected) {
        String actual = methods().getText(page, locator);
        boolean contains = actual != null && actual.contains(expected);
        if (!contains) {
            MobileEvidenceManager.captureAssertionFailureScreenshot(MobileDriverManager.getDriver(), "text_not_contains_" + page + "_" + locator, "Expected text to contain [" + expected + "] but was [" + actual + "]");
        }
        Assert.assertTrue("Expected text to contain " + expected + " but was " + actual, contains);
    }
}
