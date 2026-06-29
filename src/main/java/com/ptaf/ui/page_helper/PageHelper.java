package com.ptaf.ui.page_helper;

import com.microsoft.playwright.Page;
import com.ptaf.ui.handlers.LocatorHandler;

/**
 * Utility wrapper around a Playwright {@link Page} that centralizes common page-level
 * helpers and exposes a shared {@link LocatorHandler} instance for resolving and
 * interacting with element locators.
 *
 * <p>
 * Purpose:
 * - Provide a thin abstraction on top of the raw Playwright Page so test code and higher-level
 *   page objects can share common utilities and locator resolution logic from a single place.
 * - Keep Playwright-specific interactions encapsulated while allowing tests and helpers to
 *   retrieve and use the underlying {@code Page} when needed.
 * </p>
 *
 * <p>
 * Usage notes for testers:
 * - Construct this class with the Playwright {@link Page} instance you obtain from your test
 *   fixture or setup code. Example:
 *     PageHelper helper = new PageHelper(page);
 * - Use {@code helper.page} to call Playwright APIs directly when required.
 * - Use the internal {@link LocatorHandler} (accessible via future accessor methods or package-level
 *   access) to resolve and manage locators consistently across tests.
 * - This class intentionally keeps logic minimal: it is a composition point for helpers and
 *   locator resolution rather than a full page object. Additional helper methods can be
 *   added here to centralize repetitive actions.
 * </p>
 *
 * <p>
 * Threading and lifecycle:
 * - The {@link Page} reference is stored as-is; ensure the Page's lifecycle (creation, navigation,
 *   closing) is managed by your test framework. Do not share a single PageHelper instance across
 *   tests that run in parallel against different browser contexts or pages unless the Page itself
 *   is intended to be shared.
 * </p>
 */
public class PageHelper {

    /**
     * The Playwright Page object representing the currently active browser tab or frame.
     *
     * <p>
     * Notes for testers:
     * - This field is public for convenience so test code and page objects can access the raw
     *   Playwright API without needing additional accessors. Access it directly when you need
     *   methods that are not wrapped by this helper.
     * - Do not reassign this field after construction; the {@link PageHelper} instance is intended
     *   to be initialized once with a valid {@link Page}.
     * </p>
     */
    public Page page;

    /**
     * Internal handler responsible for locating and managing element locators on the page.
     *
     * <p>
     * The {@link LocatorHandler} encapsulates locator definitions and provides utility methods
     * to resolve them into Playwright locators. It is instantiated once per {@link PageHelper}
     * to provide a consistent locator resolution strategy across all helper methods and tests
     * that reuse this instance.
     * </p>
     *
     * <p>
     * Visibility is kept private to enforce encapsulation; expose needed functionality via helper
     * methods if you want test code to interact with it in a controlled way.
     * </p>
     */
    private final LocatorHandler locatorHandler;

    /**
     * Create a new PageHelper tied to the provided Playwright {@link Page}.
     *
     * <p>
     * The constructor stores the supplied {@code page} reference and initializes a fresh
     * {@link LocatorHandler} instance for locator management.
     * </p>
     *
     * @param page The Playwright Page object this helper will operate on. Must be a valid,
     *             initialized Page obtained from Playwright. This constructor does not perform
     *             null checks or page validation, so callers should ensure they pass a valid
     *             instance.
     */
    public PageHelper(Page page) {
        // Store the Playwright Page reference for direct use by tests or helper methods.
        this.page = page;

        // Initialize the locator handler which centralizes locator definitions and helper logic.
        // Having a dedicated handler simplifies maintenance of selectors and provides a single
        // place to update locator strategies (CSS, XPath, test ids, etc.) if needed.
        this.locatorHandler = new LocatorHandler();
    }

    // Additional methods for page interaction can be added here.
    // When adding methods, prefer using the locatorHandler to resolve selectors and the
    // page field to execute Playwright actions. Keep methods small, focused, and well-documented
    // so testers can easily reuse them in test flows.
}
