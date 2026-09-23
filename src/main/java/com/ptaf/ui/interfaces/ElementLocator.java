package com.ptaf.ui.interfaces;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * ElementLocator is a small abstraction that hides the details of how locators are created
 * for Playwright-based UI interactions. Implementations of this interface are responsible
 * for converting a human-friendly locatorType + locator string into a Playwright Locator
 * object that can be used by test code (or other UI automation code) to interact with the page.
 *
 * <p>Purpose and usage notes for testers:
 * - This interface allows tests to be written in a way that refers to element types (like
 *   "XPATH", "CSS", "ID", "TEXT", "BUTTON") rather than constructing Playwright selectors
 *   directly in the test code.
 * - Tests should pass the appropriate {@code Page} or {@code FrameLocator} instance obtained
 *   from the Playwright fixture along with the locator type and the raw locator string.
 * - Implementations should return a non-null {@link Locator} that points to the element
 *   or elements represented by the input. If no such element exists, typical Playwright
 *   behavior is to provide a Locator that will fail when an interaction is attempted.
 *
 * <p>Common locatorType values (conventional examples; concrete implementations may support
 * additional or different values):
 * - "XPATH"  : locator is treated as an XPath expression.
 * - "CSS"    : locator is treated as a CSS selector.
 * - "ID"     : locator is treated as an element id (may be translated to a CSS selector like '#id').
 * - "TEXT"   : locator is treated as visible text content.
 * - "BUTTON" : locator is treated as a button text or button-specific selector.
 *
 * <p>Implementation guidance:
 * - Implementations SHOULD NOT perform explicit waits, assertions, or element interactions.
 *   Their job is solely to create and return the appropriate Locator object.
 * - Implementations MAY normalize/validate the {@code locator} parameter (for example escape
 *   single quotes in an XPath) but should not mutate the original input objects.
 * - Implementations SHOULD throw {@link IllegalArgumentException} for unsupported or malformed
 *   {@code locatorType} values as documented on the methods below.
 *
 * <p>Thread-safety:
 * - The thread-safety of implementations depends on the underlying Playwright {@link Page}
 *   and {@link FrameLocator} usage. Typically, Playwright objects are not shared across
 *   unrelated browser contexts/threads, so implementations should be used in a single-threaded
 *   context per Playwright Page/FrameLocator unless the broader test framework guarantees
 *   safe concurrent usage.
 *
 * @see com.microsoft.playwright.Page
 * @see com.microsoft.playwright.FrameLocator
 * @see com.microsoft.playwright.Locator
 */
public interface ElementLocator {

    /**
     * Locate an element on the given Playwright {@link Page} according to the specified locator type.
     *
     * <p>Examples:
     * - getLocatorForType("CSS", page, "div.my-class > a")  -> Locator for the CSS selector.
     * - getLocatorForType("XPATH", page, "//button[text()='Save']") -> Locator for XPath expression.
     *
     * <p>Responsibilities of implementations:
     * - Map the {@code locatorType} string to the correct way of building a Playwright selector.
     * - Return a {@link Locator} instance configured for the provided {@link Page}.
     * - Validate the {@code locatorType} and throw {@link IllegalArgumentException} for unknown values.
     *
     * <p>Important: this method should not perform interactions (clicks, fills) or synchronization/waiting.
     * It should only construct and return the appropriate Locator object.
     *
     * @param locatorType The type/category of locator to use (for example "XPATH", "CSS", "ID", "TEXT", "BUTTON").
     *                    Implementations may support more or less values; pass the exact string expected by the
     *                    concrete implementation.
     * @param page        The Playwright Page instance representing the browser page where the element resides.
     *                    Must not be null; implementations may throw {@link NullPointerException} if null is passed.
     * @param locator     The raw locator string appropriate for the locatorType (for example an XPath expression
     *                    or CSS selector). The string is not modified by this interface; implementations may
     *                    perform necessary escaping or normalization.
     * @return A Playwright {@link Locator} that points to the element(s) described by the inputs. The returned
     *         Locator is ready to be used for actions like click(), textContent(), etc.
     * @throws IllegalArgumentException if {@code locatorType} is unrecognized, not supported, or malformed.
     */
    Locator getLocatorForType(String locatorType, Page page, String locator);

    /**
     * Locate an element inside a frame (represented by Playwright {@link FrameLocator}) using the given locator type.
     *
     * <p>This is identical in purpose to {@link #getLocatorForType(String, Page, String)} but for contexts
     * where elements live within an iframe or other frame boundary and a {@link FrameLocator} has been
     * obtained from the {@link Page} or another frame.
     *
     * <p>Examples:
     * - getLocatorForType("CSS", frame, "input[name='email']") -> Locator for the CSS selector inside the frame.
     * - getLocatorForType("TEXT", frame, "Forgot password?") -> Locator for visible text inside the frame.
     *
     * <p>Responsibilities of implementations:
     * - Translate {@code locatorType} into an appropriate selector scoped to the provided frame.
     * - Return a {@link Locator} that is scoped to the frame context represented by {@code frame}.
     * - Throw {@link IllegalArgumentException} on unsupported or invalid {@code locatorType} values.
     *
     * <p>Important: Do not perform UI interactions or waiting here; only return the Locator object.
     *
     * @param locatorType The type/category of locator to use (for example "XPATH", "CSS", "ID", "TEXT", "BUTTON").
     *                    Must follow the supported values for the concrete implementation.
     * @param frame       The Playwright FrameLocator representing the frame that scopes the element lookup.
     *                    Must not be null; implementations may throw {@link NullPointerException} if null is passed.
     * @param locator     The raw locator string appropriate for the locatorType that will be applied inside the frame.
     * @return A Playwright {@link Locator} that is scoped to the frame and points to the element(s).
     * @throws IllegalArgumentException if {@code locatorType} is unrecognized, unsupported, or invalid.
     */
    Locator getLocatorForType(String locatorType, FrameLocator frame, String locator);
}
