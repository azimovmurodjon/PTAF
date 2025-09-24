package com.ptaf.ui.handlers;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * LocatorHandler maps locator "types" to Playwright locators for Page, FrameLocator, or chained Locator.
 * Backward-compatible with patterns like "ROW_rowElement > Button_buttonName",
 * and now also supports unnamed roles like "ROW_rowElement > Button" (first button).
 */
public class LocatorHandler {

    /** Treat empty/null or same-as-type strings as "no name provided" (e.g., "Button"). */
    private boolean isUnnamed(String locator, String locatorType) {
        return locator == null
                || locator.isEmpty()
                || locator.equalsIgnoreCase(locatorType);
    }

    // ============================ PAGE CONTEXT ============================

    public Locator getLocatorForType(String locatorType, Page page, String locator) {
        String t = locatorType.toUpperCase();

        switch (t) {
            case "CSS":
            case "TAG":
            case "XPATH":
                return page.locator(locator);

            // --- Roles (with or without name) ---
            case "BUTTON":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.BUTTON)
                        : page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(locator));
            case "LINKTEXT":
                return isUnnamed(locator, t) ? page.getByRole(AriaRole.LINK)
                        : page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(locator));
            case "OPTION":
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

            case "TEXT":
                return page.getByText(locator);

            case "ROLE":
                // Here the "locator" is expected to be the role name (e.g., BUTTON)
                return page.getByRole(AriaRole.valueOf(locator.toUpperCase()));

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

            case "ID":
                return page.locator("#" + locator);
            case "NAME":
                return page.locator("[name='" + locator + "']");
            case "CLASS":
                return page.locator("." + locator);

            default:
                throw new IllegalArgumentException("Unknown locator type: " + locatorType);
        }
    }

    // ============================ FRAME CONTEXT ============================

    public Locator getLocatorForType(String locatorType, FrameLocator frame, String locator) {
        String t = locatorType.toUpperCase();

        switch (t) {
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

            case "TEXT":
                return frame.getByText(locator);

            case "ROLE":
                return frame.getByRole(AriaRole.valueOf(locator.toUpperCase()));

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

            case "ID":
                return frame.locator("#" + locator);
            case "NAME":
                return frame.locator("[name='" + locator + "']");
            case "CLASS":
                return frame.locator("." + locator);

            default:
                throw new IllegalArgumentException("Unknown locator type: " + locatorType);
        }
    }

    // ============================ CHAINED CONTEXT (off an existing Locator) ============================

    public Locator getLocatorForType(String locatorType, Locator baseLocator, String locator) {
        String t = locatorType.toUpperCase();

        switch (t) {
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

            case "TEXT":
                return baseLocator.getByText(locator);

            case "ROLE":
                return baseLocator.getByRole(AriaRole.valueOf(locator.toUpperCase()));

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

            // --- CSS-style shortcuts in chained context ---
            case "ID":
                return baseLocator.locator("#" + locator);
            case "NAME":
                return baseLocator.locator("[name='" + locator + "']");
            case "CLASS":
                return baseLocator.locator("." + locator);

            default:
                throw new IllegalArgumentException("Unknown chained locator type: " + locatorType);
        }
    }
}