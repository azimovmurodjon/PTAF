package com.ptaf.ui.handlers;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Utility that maps a simple "locator type" (e.g. "CSS", "BUTTON", "ID", "TEXTBOX", "ROLE", etc.)
 * to a Playwright Locator for three different contexts:
 * - Page (top-level)
 * - FrameLocator (inside an iframe)
 * - Locator (chained off an existing locator)
 *
 * <p>
 * Behaviour notes:
 * - This class does NOT change the semantics or naming of any Playwright calls; it is a thin
 *   mapping layer providing a consistent set of textual locator types used across tests.
 * - For ARIA roles (e.g. BUTTON, CHECKBOX, LINKTEXT, TEXTBOX, etc.) the handler supports both:
 *     - Named role: locatorType="BUTTON", locator="Submit"  -> matches a button with name "Submit"
 *     - Unnamed role: locatorType="BUTTON", locator="BUTTON" or locator=null or locator="" ->
 *         returns the role locator without a name filter (i.e. first matching role(s))
 * - For the special "ROLE" locatorType the locator parameter is expected to be the role name itself,
 *   e.g. locatorType="ROLE", locator="button" (case-insensitive).
 * - CSS-style shortcuts (ID/NAME/CLASS) are supported and mapped to locator strings appropriate for
 *   Playwright (e.g. "#id", "[name='val']", ".class").
 *
 * <p>
 * Examples:
 * - getLocatorForType("CSS", page, ".myclass") -> page.locator(".myclass")
 * - getLocatorForType("BUTTON", page, "Submit") -> page.getByRole(AriaRole.BUTTON, setName("Submit"))
 * - getLocatorForType("BUTTON", page, "BUTTON") -> page.getByRole(AriaRole.BUTTON)
 * - getLocatorForType("ROLE", page, "LINK") -> page.getByRole(AriaRole.LINK)
 *
 * <p>
 * Error handling:
 * - If an unknown locatorType is provided, an IllegalArgumentException is thrown containing
 *   a human-friendly multi-line message constructed by prettyUnknownType(...) explaining the
 *   context and what was received. This is intended to help testers quickly diagnose YAML/token
 *   misconfigurations like "TYPE_value".
 *
 * <p>
 * Important: This class is purely a mapping layer. Do not change method signatures or logic here
 * if you are trying to extend behaviour; instead update the mappings in each method.
 */
public class LocatorHandler {

    /**
     * Helper that determines whether the given locator string should be treated as "unnamed".
     *
     * <p>
     * We consider a locator unnamed if any of:
     * - locator == null
     * - locator is an empty string
     * - locator equals (case-insensitive) to the locatorType itself (e.g., "BUTTON")
     *
     * <p>
     * This supports usage patterns where a YAML token might be written as "Button" (only the type)
     * meaning "the first Button role inside the context" rather than a named element.
     *
     * @param locator the locator text provided (may be null)
     * @param locatorType the locator type text (e.g., "BUTTON") already known by the caller
     * @return true when the locator should be treated as unnamed and therefore the role-specific
     *         locator should be returned without a name filter
     */
    private boolean isUnnamed(String locator, String locatorType) {
        return locator == null
                || locator.isEmpty()
                || locator.equalsIgnoreCase(locatorType);
    }

    // ============================ PAGE CONTEXT ============================
    /**
     * Map a locatorType + locator to a Playwright Locator in the context of a Page.
     *
     * <p>
     * Supported locatorType values (case-insensitive) include:
     * - "CSS", "TAG", "XPATH"      -> page.locator(locator)
     * - Many ARIA role names such as "BUTTON", "CHECKBOX", "TEXTBOX", "LINKTEXT", "ROW", "CELL", etc.
     *   When the provided locator is "unnamed" (see isUnnamed), the role locator without a name
     *   filter is returned; otherwise a role locator with name(locator) is returned.
     * - "OPTION" uses setExact(true) when a name is provided to enforce exact matching.
     * - "BUTTONSUBMIT" will set setPressed(true) for the named-case to attempt to match pressed/submit
     *   style buttons where applicable.
     * - "TEXT" -> page.getByText(locator)
     * - "ROLE" -> locator is expected to be the role name, converted to AriaRole.valueOf(...)
     * - "ALTTEXT", "TITLE", "PLACEHOLDER", "LABEL", "TESTID" -> corresponding page getters
     * - "ID", "NAME", "CLASS" -> CSS-style short hands (#id, [name='..'], .class)
     *
     * @param locatorType textual type of locator (e.g., "CSS", "BUTTON", "ID", "ROLE")
     * @param page the Playwright Page to resolve the locator against
     * @param locator locator string value (name, selector, or role name depending on locatorType)
     * @return a Playwright Locator representing the requested element(s)
     * @throws IllegalArgumentException if locatorType is not recognized. The thrown message will
     *         include a detailed multi-line hint (via prettyUnknownType) to aid debugging.
     */
    public Locator getLocatorForType(String locatorType, Page page, String locator) {
        String t = locatorType.toUpperCase();

        switch (t) {
            // Generic selector styles: pass through to Playwright locator API
            case "CSS":
            case "TAG":
            case "XPATH":
                return page.locator(locator);

            // --- Roles (with or without name) ---
            // Each role branch: if unnamed => return role locator without name filter,
            // otherwise return role locator with name set to provided locator.
            case "BUTTON":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.BUTTON)
                        : page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(locator));
            case "LINKTEXT":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.LINK)
                        : page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(locator));
            case "OPTION":
                // For options we enforce exact matching when a name is provided.
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.OPTION)
                        : page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(locator).setExact(true));
            case "TEXTBOX":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TEXTBOX)
                        : page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(locator));
            case "CHECKBOX":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.CHECKBOX)
                        : page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(locator));
            case "RADIOBUTTON":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.RADIO)
                        : page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName(locator));
            case "DROPDOWN":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.COMBOBOX)
                        : page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName(locator));
            case "IMAGE":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.IMG)
                        : page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(locator));
            case "HEADING":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.HEADING)
                        : page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(locator));
            case "TAB":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TAB)
                        : page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(locator));
            case "LIST":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.LIST)
                        : page.getByRole(AriaRole.LIST, new Page.GetByRoleOptions().setName(locator));
            case "LISTBOX":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.LISTBOX)
                        : page.getByRole(AriaRole.LISTBOX, new Page.GetByRoleOptions().setName(locator));
            case "LISTITEM":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.LISTITEM)
                        : page.getByRole(AriaRole.LISTITEM, new Page.GetByRoleOptions().setName(locator));
            case "TABLE":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TABLE)
                        : page.getByRole(AriaRole.TABLE, new Page.GetByRoleOptions().setName(locator));
            case "ROW":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.ROW)
                        : page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(locator));
            case "CELL":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.CELL)
                        : page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(locator));
            case "BUTTONSUBMIT":
                // BUTTONSUBMIT: when named we apply setPressed(true) to attempt to identify submit-style buttons.
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.BUTTON)
                        : page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(locator).setPressed(true));
            case "SLIDER":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.SLIDER)
                        : page.getByRole(AriaRole.SLIDER, new Page.GetByRoleOptions().setName(locator));
            case "SPINBUTTON":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.SPINBUTTON)
                        : page.getByRole(AriaRole.SPINBUTTON, new Page.GetByRoleOptions().setName(locator));
            case "PROGRESSBAR":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.PROGRESSBAR)
                        : page.getByRole(AriaRole.PROGRESSBAR, new Page.GetByRoleOptions().setName(locator));
            case "ALERT":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.ALERT)
                        : page.getByRole(AriaRole.ALERT, new Page.GetByRoleOptions().setName(locator));
            case "ALERTDIALOG":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.ALERTDIALOG)
                        : page.getByRole(AriaRole.ALERTDIALOG, new Page.GetByRoleOptions().setName(locator));
            case "DIALOG":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.DIALOG)
                        : page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName(locator));
            case "NAVIGATION":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.NAVIGATION)
                        : page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName(locator));
            case "MENU":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.MENU)
                        : page.getByRole(AriaRole.MENU, new Page.GetByRoleOptions().setName(locator));
            case "MENUITEM":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.MENUITEM)
                        : page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(locator));
            case "MENUITEMCHECKBOX":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.MENUITEMCHECKBOX)
                        : page.getByRole(AriaRole.MENUITEMCHECKBOX, new Page.GetByRoleOptions().setName(locator));
            case "MENUITEMRADIO":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.MENUITEMRADIO)
                        : page.getByRole(AriaRole.MENUITEMRADIO, new Page.GetByRoleOptions().setName(locator));
            case "TREE":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TREE)
                        : page.getByRole(AriaRole.TREE, new Page.GetByRoleOptions().setName(locator));
            case "TREEITEM":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TREEITEM)
                        : page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(locator));
            case "GRID":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.GRID)
                        : page.getByRole(AriaRole.GRID, new Page.GetByRoleOptions().setName(locator));
            case "GRIDCELL":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.GRIDCELL)
                        : page.getByRole(AriaRole.GRIDCELL, new Page.GetByRoleOptions().setName(locator));
            case "SEPARATOR":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.SEPARATOR)
                        : page.getByRole(AriaRole.SEPARATOR, new Page.GetByRoleOptions().setName(locator));
            case "SWITCH":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.SWITCH)
                        : page.getByRole(AriaRole.SWITCH, new Page.GetByRoleOptions().setName(locator));
            case "STATUS":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.STATUS)
                        : page.getByRole(AriaRole.STATUS, new Page.GetByRoleOptions().setName(locator));
            case "BANNER":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.BANNER)
                        : page.getByRole(AriaRole.BANNER, new Page.GetByRoleOptions().setName(locator));
            case "FOOTER":
            case "CONTENTINFO":
                // These two map to the same ARIA role (CONTENTINFO)
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.CONTENTINFO)
                        : page.getByRole(AriaRole.CONTENTINFO, new Page.GetByRoleOptions().setName(locator));
            case "MAIN":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.MAIN)
                        : page.getByRole(AriaRole.MAIN, new Page.GetByRoleOptions().setName(locator));
            case "COMPLEMENTARY":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.COMPLEMENTARY)
                        : page.getByRole(AriaRole.COMPLEMENTARY, new Page.GetByRoleOptions().setName(locator));
            case "REGION":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.REGION)
                        : page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName(locator));
            case "ARTICLE":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.ARTICLE)
                        : page.getByRole(AriaRole.ARTICLE, new Page.GetByRoleOptions().setName(locator));
            case "FORM":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.FORM)
                        : page.getByRole(AriaRole.FORM, new Page.GetByRoleOptions().setName(locator));
            case "LOG":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.LOG)
                        : page.getByRole(AriaRole.LOG, new Page.GetByRoleOptions().setName(locator));
            case "MARQUEE":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.MARQUEE)
                        : page.getByRole(AriaRole.MARQUEE, new Page.GetByRoleOptions().setName(locator));
            case "TIMER":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TIMER)
                        : page.getByRole(AriaRole.TIMER, new Page.GetByRoleOptions().setName(locator));
            case "TOOLTIP":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TOOLTIP)
                        : page.getByRole(AriaRole.TOOLTIP, new Page.GetByRoleOptions().setName(locator));
            case "TOOLBAR":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.TOOLBAR)
                        : page.getByRole(AriaRole.TOOLBAR, new Page.GetByRoleOptions().setName(locator));
            case "PRESENTATION":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.PRESENTATION)
                        : page.getByRole(AriaRole.PRESENTATION, new Page.GetByRoleOptions().setName(locator));
            case "FIGURE":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.FIGURE)
                        : page.getByRole(AriaRole.FIGURE, new Page.GetByRoleOptions().setName(locator));

            // Text-based finder
            case "TEXT":
                return page.getByText(locator);

            // ROLE case: locator is expected to be a role name, e.g., "BUTTON", "LINK", etc.
            // Note: AriaRole.valueOf will throw IllegalArgumentException if role name is invalid;
            // that exception will bubble up to the caller.
            case "ROLE":
                return page.getByRole(AriaRole.valueOf(locator.toUpperCase()));

            // Other Playwright getters by attribute-like values
            case "ALTTEXT":
                return page.getByAltText(locator);
            case "TITLE":
                return page.getByTitle(locator);
            case "PLACEHOLDER":
                return page.getByPlaceholder(locator);
            case "LABEL":
                return page.getByLabel(locator);
            case "TESTID":
                return page.getByTestId(locator);

            // Simple CSS shortcuts: id, name, class
            case "ID":
                return page.locator("#" + locator);
            case "NAME":
                return page.locator("[name='" + locator + "']");
            case "CLASS":
                return page.locator("." + locator);

            default:
                // Unknown type -> provide a prettier, more transparent error that includes context.
                throw new IllegalArgumentException(prettyUnknownType("Unknown locator type", locatorType, locator, "PAGE"));
        }
    }

    // ============================ FRAME CONTEXT ============================
    /**
     * Map a locatorType + locator to a Playwright Locator in the context of a FrameLocator.
     *
     * <p>
     * This method mirrors the behavior of getLocatorForType(Page, ...), but resolves locators
     * against a FrameLocator instance (useful when interacting with elements inside iframes).
     *
     * @param locatorType textual type of locator
     * @param frame the FrameLocator context to resolve against
     * @param locator locator string value (name, selector, or role name depending on locatorType)
     * @return a Playwright Locator resolved from the frame context
     * @throws IllegalArgumentException when locatorType is unknown; the message will include
     *         detailed context info to help debugging.
     */
    public Locator getLocatorForType(String locatorType, FrameLocator frame, String locator) {
        String t = locatorType.toUpperCase();

        switch (t) {
            // Generic selector styles: pass through to FrameLocator API
            case "CSS":
            case "TAG":
            case "XPATH":
                return frame.locator(locator);

            // --- Roles (with or without name) ---
            case "BUTTON":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.BUTTON)
                        : frame.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName(locator));
            case "LINKTEXT":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.LINK)
                        : frame.getByRole(AriaRole.LINK, new FrameLocator.GetByRoleOptions().setName(locator));
            case "OPTION":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.OPTION)
                        : frame.getByRole(AriaRole.OPTION, new FrameLocator.GetByRoleOptions().setName(locator).setExact(true));
            case "TEXTBOX":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.TEXTBOX)
                        : frame.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName(locator));
            case "CHECKBOX":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.CHECKBOX)
                        : frame.getByRole(AriaRole.CHECKBOX, new FrameLocator.GetByRoleOptions().setName(locator));
            case "RADIOBUTTON":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.RADIO)
                        : frame.getByRole(AriaRole.RADIO, new FrameLocator.GetByRoleOptions().setName(locator));
            case "DROPDOWN":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.COMBOBOX)
                        : frame.getByRole(AriaRole.COMBOBOX, new FrameLocator.GetByRoleOptions().setName(locator));
            case "IMAGE":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.IMG)
                        : frame.getByRole(AriaRole.IMG, new FrameLocator.GetByRoleOptions().setName(locator));
            case "HEADING":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.HEADING)
                        : frame.getByRole(AriaRole.HEADING, new FrameLocator.GetByRoleOptions().setName(locator));
            case "TAB":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.TAB)
                        : frame.getByRole(AriaRole.TAB, new FrameLocator.GetByRoleOptions().setName(locator));
            case "LIST":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.LIST)
                        : frame.getByRole(AriaRole.LIST, new FrameLocator.GetByRoleOptions().setName(locator));
            case "LISTBOX":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.LISTBOX)
                        : frame.getByRole(AriaRole.LISTBOX, new FrameLocator.GetByRoleOptions().setName(locator));
            case "LISTITEM":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.LISTITEM)
                        : frame.getByRole(AriaRole.LISTITEM, new FrameLocator.GetByRoleOptions().setName(locator));
            case "TABLE":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.TABLE)
                        : frame.getByRole(AriaRole.TABLE, new FrameLocator.GetByRoleOptions().setName(locator));
            case "ROW":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.ROW)
                        : frame.getByRole(AriaRole.ROW, new FrameLocator.GetByRoleOptions().setName(locator));
            case "CELL":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.CELL)
                        : frame.getByRole(AriaRole.CELL, new FrameLocator.GetByRoleOptions().setName(locator));
            case "BUTTONSUBMIT":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.BUTTON)
                        : frame.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName(locator).setPressed(true));
            case "SLIDER":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.SLIDER)
                        : frame.getByRole(AriaRole.SLIDER, new FrameLocator.GetByRoleOptions().setName(locator));
            case "SPINBUTTON":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.SPINBUTTON)
                        : frame.getByRole(AriaRole.SPINBUTTON, new FrameLocator.GetByRoleOptions().setName(locator));
            case "PROGRESSBAR":
                return isUnnamed(locator, t) ? frame.getByRole(AriaRole.PROGRESSBAR)
                        : frame.getByRole(AriaRole.PROGRESSBAR, new FrameLocator.GetByRoleOptions().setName(locator));

            // Text-based finder inside frame
            case "TEXT":
                return frame.getByText(locator);

            // ROLE inside frame: locator is expected to be a role name
            case "ROLE":
                return frame.getByRole(AriaRole.valueOf(locator.toUpperCase()));

            // Other attribute-like getters
            case "ALTTEXT":
                return frame.getByAltText(locator);
            case "TITLE":
                return frame.getByTitle(locator);
            case "PLACEHOLDER":
                return frame.getByPlaceholder(locator);
            case "LABEL":
                return frame.getByLabel(locator);
            case "TESTID":
                return frame.getByTestId(locator);

            // CSS shortcuts in frame context
            case "ID":
                return frame.locator("#" + locator);
            case "NAME":
                return frame.locator("[name='" + locator + "']");
            case "CLASS":
                return frame.locator("." + locator);

            default:
                // Unknown type in frame context -> pretty error
                throw new IllegalArgumentException(prettyUnknownType("Unknown locator type", locatorType, locator, "FRAME"));
        }
    }

    // ============================ CHAINED CONTEXT (off an existing Locator) ============================
    /**
     * Map a locatorType + locator to a Playwright Locator in the context of an existing base Locator.
     *
     * <p>
     * Use this overload when you want to locate children or nested elements relative to an already
     * resolved Locator (e.g., rowLocator -> locate buttons inside that row).
     *
     * <p>
     * The behaviour mirrors the Page and FrameLocator versions:
     * - CSS/TAG/XPATH -> baseLocator.locator(selector)
     * - Role mapping behaves the same with Locator.GetByRoleOptions
     * - "TEXT", "ROLE", "ALTTEXT", "TITLE", "PLACEHOLDER", "LABEL", "TESTID" map to baseLocator.getBy...
     * - ID/NAME/CLASS are treated as CSS shortcuts and resolved relative to the baseLocator
     *
     * @param locatorType textual type of locator
     * @param baseLocator the Locator to resolve children from
     * @param locator locator string value used by the selected locatorType
     * @return a Playwright Locator resolved relative to the baseLocator
     * @throws IllegalArgumentException when locatorType is unknown; message includes helpful context.
     */
    public Locator getLocatorForType(String locatorType, Locator baseLocator, String locator) {
        String t = locatorType.toUpperCase();

        switch (t) {
            // Generic selector styles relative to a base locator
            case "CSS":
            case "TAG":
            case "XPATH":
                return baseLocator.locator(locator);

            // --- Roles (with or without name) ---
            case "BUTTON":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.BUTTON)
                        : baseLocator.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(locator));
            case "LINKTEXT":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.LINK)
                        : baseLocator.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(locator));
            case "OPTION":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.OPTION)
                        : baseLocator.getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName(locator).setExact(true));
            case "TEXTBOX":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.TEXTBOX)
                        : baseLocator.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName(locator));
            case "CHECKBOX":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.CHECKBOX)
                        : baseLocator.getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(locator));
            case "RADIOBUTTON":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.RADIO)
                        : baseLocator.getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName(locator));
            case "DROPDOWN":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.COMBOBOX)
                        : baseLocator.getByRole(AriaRole.COMBOBOX, new Locator.GetByRoleOptions().setName(locator));
            case "IMAGE":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.IMG)
                        : baseLocator.getByRole(AriaRole.IMG, new Locator.GetByRoleOptions().setName(locator));
            case "HEADING":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.HEADING)
                        : baseLocator.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName(locator));
            case "TAB":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.TAB)
                        : baseLocator.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName(locator));
            case "LIST":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.LIST)
                        : baseLocator.getByRole(AriaRole.LIST, new Locator.GetByRoleOptions().setName(locator));
            case "LISTBOX":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.LISTBOX)
                        : baseLocator.getByRole(AriaRole.LISTBOX, new Locator.GetByRoleOptions().setName(locator));
            case "LISTITEM":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.LISTITEM)
                        : baseLocator.getByRole(AriaRole.LISTITEM, new Locator.GetByRoleOptions().setName(locator));
            case "TABLE":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.TABLE)
                        : baseLocator.getByRole(AriaRole.TABLE, new Locator.GetByRoleOptions().setName(locator));
            case "ROW":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.ROW)
                        : baseLocator.getByRole(AriaRole.ROW, new Locator.GetByRoleOptions().setName(locator));
            case "CELL":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.CELL)
                        : baseLocator.getByRole(AriaRole.CELL, new Locator.GetByRoleOptions().setName(locator));
            case "BUTTONSUBMIT":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.BUTTON)
                        : baseLocator.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(locator).setPressed(true));
            case "SLIDER":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.SLIDER)
                        : baseLocator.getByRole(AriaRole.SLIDER, new Locator.GetByRoleOptions().setName(locator));
            case "SPINBUTTON":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.SPINBUTTON)
                        : baseLocator.getByRole(AriaRole.SPINBUTTON, new Locator.GetByRoleOptions().setName(locator));
            case "PROGRESSBAR":
                return isUnnamed(locator, t) ? baseLocator.getByRole(AriaRole.PROGRESSBAR)
                        : baseLocator.getByRole(AriaRole.PROGRESSBAR, new Locator.GetByRoleOptions().setName(locator));

            // Text-based finder relative to baseLocator
            case "TEXT":
                return baseLocator.getByText(locator);

            // ROLE relative to baseLocator: locator is expected to be a role name
            case "ROLE":
                return baseLocator.getByRole(AriaRole.valueOf(locator.toUpperCase()));

            // Other attribute-like getters relative to baseLocator
            case "ALTTEXT":
                return baseLocator.getByAltText(locator);
            case "TITLE":
                return baseLocator.getByTitle(locator);
            case "PLACEHOLDER":
                return baseLocator.getByPlaceholder(locator);
            case "LABEL":
                return baseLocator.getByLabel(locator);
            case "TESTID":
                return baseLocator.getByTestId(locator);

            // CSS-style shortcuts applied relative to baseLocator
            case "ID":
                return baseLocator.locator("#" + locator);
            case "NAME":
                return baseLocator.locator("[name='" + locator + "']");
            case "CLASS":
                return baseLocator.locator("." + locator);

            default:
                // Unknown chained locator type -> pretty error describing context CHAINED
                throw new IllegalArgumentException(prettyUnknownType("Unknown chained locator type", locatorType, locator, "CHAINED"));
        }
    }

    // ============================ PRETTY ERROR ============================
    /**
     * Build a multi-line, clearer error message describing which locator type failed and why.
     * This method is used to surface friendly errors to test authors when a YAML or token
     * contains an unsupported TYPE_value.
     *
     * @param title short title for the error (e.g., "Unknown locator type")
     * @param locatorType the original locatorType that was provided (may be invalid)
     * @param locator the locator text that accompanied the locatorType
     * @param context one of "PAGE", "FRAME", or "CHAINED" describing where the mapping was attempted
     * @return a formatted multi-line string explaining the failure and giving a brief hint
     */
    private String prettyUnknownType(String title, String locatorType, String locator, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== LOCATOR TYPE FAILURE (EXACT WHY) ==========\n");
        sb.append("Context    : ").append(context).append("\n");
        sb.append("Title      : ").append(title).append("\n");
        sb.append("Type       : ").append(locatorType).append("\n");
        sb.append("Locator    : ").append(locator).append("\n");
        sb.append("Hint       : Check YAML token TYPE_value. Example: CSS_.class OR XPATH_//div\n");
        sb.append("=====================================================\n");
        return sb.toString();
    }
}
