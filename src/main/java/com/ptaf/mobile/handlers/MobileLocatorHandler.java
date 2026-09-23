package com.ptaf.mobile.handlers;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.Locale;

/**
 * Enterprise mobile locator resolver for PTAF Appium automation.
 *
 * <p>This resolver is intentionally backward-compatible. All historical PTAF
 * mobile locator prefixes continue to work exactly as before, including
 * ACCESSIBILITY_ID_, ID_, XPATH_, CLASS_NAME_, ANDROID_UIAUTOMATOR_,
 * IOS_PREDICATE_, IOS_CLASS_CHAIN_, and NAME_.</p>
 *
 * <p>In addition, Appium native mobile and Appium real mobile browser flows can
 * now understand the same simple locator vocabulary commonly used by the
 * Playwright UI layer, such as Button_Login, TEXTBOX_Search, TEXT_Save,
 * CSS_.search, CLASS_result, TESTID_submit, PLACEHOLDER_Search, and LABEL_Email.
 * This gives teams one common locator language while preserving all existing
 * project locators.</p>
 */
public class MobileLocatorHandler {

    /**
     * Resolve a human-friendly PTAF locator string into a Selenium/Appium By locator.
     *
     * <p>Supported input styles:
     * - Explicit Appium prefixes that start with one of:
     *   ACCESSIBILITY_ID_, ID_, XPATH_, CLASS_NAME_, NAME_, ANDROID_UIAUTOMATOR_,
     *   IOS_PREDICATE_, IOS_CLASS_CHAIN_. These are handled with AppiumBy helpers.
     *
     * - Friendly UI-style prefixes (case-insensitive for the type portion),
     *   separated by an underscore or a space, e.g. "Button_Login" or "TEXT Save".
     *   Supported friendly types include: CSS, TAG, CLASS, TESTID, PLACEHOLDER, LABEL,
     *   TITLE, ALTTEXT, TEXT, LINKTEXT, BUTTON, TEXTBOX/INPUT, CHECKBOX, RADIOBUTTON/RADIO,
     *   DROPDOWN/COMBOBOX, OPTION, IMAGE, HEADING, TAB, LIST, LISTITEM, TABLE,
     *   ROW, CELL, DIALOG, MENU, MENUITEM.</p>
     *
     * <p>The method preserves existing behavior for Appium prefixed locators and
     * returns AppiumBy locators for those cases. For the friendly UI-style locators
     * it returns Selenium By locators that best represent the intent (mostly XPath
     * or CSS selectors). The resolution attempts to be tolerant and search common
     * DOM attributes used across web and native mobile automation (e.g. name, label,
     * aria-label, text, content-desc, placeholder).</p>
     *
     * @param locatorValue the raw locator string to resolve (must not be null/blank)
     * @return a Selenium/Appium By locator constructed from the input
     * @throws IllegalArgumentException if locatorValue is null/blank or if the type
     *                                  portion is not recognized/supported
     */
    public By getLocatorForType(String locatorValue) {
        if (locatorValue == null || locatorValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile locator value cannot be blank.");
        }

        String locator = locatorValue.trim();

        // ------------------------------------------------------------------
        // Existing mobile/Appium prefixes. Do not change this behavior because
        // many existing projects depend on these exact prefixes.
        // Each branch uses AppiumBy (native Appium helpers) and extracts the
        // suffix after the fixed prefix length.
        // ------------------------------------------------------------------
        if (locator.startsWith("ACCESSIBILITY_ID_")) return AppiumBy.accessibilityId(locator.substring("ACCESSIBILITY_ID_".length()));
        if (locator.startsWith("ANDROID_UIAUTOMATOR_")) return AppiumBy.androidUIAutomator(locator.substring("ANDROID_UIAUTOMATOR_".length()));
        if (locator.startsWith("IOS_PREDICATE_")) return AppiumBy.iOSNsPredicateString(locator.substring("IOS_PREDICATE_".length()));
        if (locator.startsWith("IOS_CLASS_CHAIN_")) return AppiumBy.iOSClassChain(locator.substring("IOS_CLASS_CHAIN_".length()));
        if (locator.startsWith("CLASS_NAME_")) return AppiumBy.className(locator.substring("CLASS_NAME_".length()));
        if (locator.startsWith("XPATH_")) return AppiumBy.xpath(locator.substring("XPATH_".length()));
        if (locator.startsWith("ID_")) return AppiumBy.id(locator.substring("ID_".length()));
        if (locator.startsWith("NAME_")) return AppiumBy.name(locator.substring("NAME_".length()));

        // Parse friendly "<TYPE>_<value>" or "<TYPE> <value>" style locators.
        LocatorToken token = splitTypeAndValue(locator);
        // Normalize type to upper-case for switch matching (uses Locale.ROOT for consistency).
        String type = token.type().toUpperCase(Locale.ROOT);
        String value = token.value();

        // ------------------------------------------------------------------
        // Shared UI-style locator aliases for Appium browser/native contexts.
        // These aliases intentionally resolve to Selenium/Appium By locators,
        // not Playwright locators. They are additive only.
        //
        // Note: Many of these cases use xpathLiteral(value) to ensure correctness
        // when value contains quotes and cssEscape(value) when embedding into CSS.
        // ------------------------------------------------------------------
        return switch (type) {
            case "CSS" -> By.cssSelector(value);
            case "TAG" -> By.tagName(value);
            case "CLASS" -> By.className(value);
            // TESTID: look for common test id attributes used across projects.
            case "TESTID" -> By.cssSelector("[data-testid='" + cssEscape(value) + "'],[data-test='" + cssEscape(value) + "'],[testID='" + cssEscape(value) + "']");
            // PLACEHOLDER: match either placeholder or input value attributes.
            case "PLACEHOLDER" -> By.xpath("//*[@placeholder=" + xpathLiteral(value) + " or @value=" + xpathLiteral(value) + "]");
            // LABEL: aria-label, label, or name
            case "LABEL" -> By.xpath("//*[@aria-label=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + "]");
            // TITLE: title, name, or label attributes
            case "TITLE" -> By.xpath("//*[@title=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            // ALTTEXT: alt, name, or label attributes
            case "ALTTEXT" -> By.xpath("//*[@alt=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            // TEXT: tries exact normalized text, contains(normalize-space(.)), and mobile-specific text/name/label attributes
            case "TEXT" -> By.xpath("//*[normalize-space(.)=" + xpathLiteral(value) + " or contains(normalize-space(.)," + xpathLiteral(value) + ") or @text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            // LINKTEXT: anchor text or elements with role='link'
            case "LINKTEXT" -> By.xpath("//a[normalize-space(.)=" + xpathLiteral(value) + " or contains(normalize-space(.)," + xpathLiteral(value) + ")] | //*[@role='link' and (normalize-space(.)=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + ")]");
            // BUTTON: <button>, role='button', or mobile-native button/text attributes
            case "BUTTON" -> By.xpath("//button[normalize-space(.)=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + " or @title=" + xpathLiteral(value) + "] | //*[@role='button' and (normalize-space(.)=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + " or @content-desc=" + xpathLiteral(value) + "]");
            // TEXTBOX / INPUT: input, textarea, role='textbox', XCUI text fields, and Android EditText
            case "TEXTBOX", "INPUT" -> By.xpath("//input[@name=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + " or @placeholder=" + xpathLiteral(value) + " or @title=" + xpathLiteral(value) + "] | //textarea[@name=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + " or @placeholder=" + xpathLiteral(value) + "] | //*[@role='textbox' and (@aria-label=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + ")] | //XCUIElementTypeTextField[@name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + " or @value=" + xpathLiteral(value) + "] | //android.widget.EditText[@text=" + xpathLiteral(value) + " or @content-desc=" + xpathLiteral(value) + "]");
            // CHECKBOX: role='checkbox' or various name/label/text matches
            case "CHECKBOX" -> By.xpath("//*[@role='checkbox' and (@aria-label=" + xpathLiteral(value) + " or normalize-space(.)=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            // RADIOBUTTON / RADIO: role='radio' and similar fallbacks
            case "RADIOBUTTON", "RADIO" -> By.xpath("//*[@role='radio' and (@aria-label=" + xpathLiteral(value) + " or normalize-space(.)=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            // Generic ARIA role-based lookups for many other element types.
            case "DROPDOWN", "COMBOBOX", "OPTION", "IMAGE", "HEADING", "TAB", "LIST", "LISTITEM", "TABLE", "ROW", "CELL", "DIALOG", "MENU", "MENUITEM" ->
                    By.xpath("//*[@role='" + roleName(type) + "' and (@aria-label=" + xpathLiteral(value) + " or normalize-space(.)=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            // Unknown type: produce a helpful error message describing supported types.
            default -> throw new IllegalArgumentException(buildUnsupportedLocatorMessage(locatorValue, type, value));
        };
    }

    /**
     * Split a locator into a type token and a value token.
     *
     * <p>Accepts two separator styles:
     * - underscore ('_'), e.g. "BUTTON_Login"
     * - single space (' '), e.g. "BUTTON Login"
     *
     * If neither separator is present the entire locator is returned as the type
     * and the value portion will be an empty string.
     *
     * @param locator the trimmed locator string to split
     * @return a LocatorToken containing the type (may be the full string) and the value (may be empty)
     */
    private LocatorToken splitTypeAndValue(String locator) {
        int underscore = locator.indexOf('_');
        int space = locator.indexOf(' ');
        int separator;
        // If both separators are present, choose the earlier one; otherwise choose whichever exists.
        if (underscore >= 0 && space >= 0) separator = Math.min(underscore, space);
        else separator = Math.max(underscore, space);
        // No separator found => treat entire locator as type
        if (separator < 0) return new LocatorToken(locator, "");
        // Return trimmed tokens for robust matching
        return new LocatorToken(locator.substring(0, separator).trim(), locator.substring(separator + 1).trim());
    }

    /**
     * Map certain friendly types to the canonical ARIA role name used in XPath queries.
     *
     * <p>For most input types this simply lower-cases the provided type, however
     * some common aliases are converted to their standard role names (e.g. DROPDOWN
     * or COMBOBOX -> combobox).</p>
     *
     * @param type the upper-cased locator type
     * @return the canonical ARIA role name to use in XPath role-based searches
     */
    private String roleName(String type) {
        return switch (type) {
            case "DROPDOWN", "COMBOBOX" -> "combobox";
            case "RADIOBUTTON", "RADIO" -> "radio";
            case "LISTITEM" -> "listitem";
            case "MENUITEM" -> "menuitem";
            default -> type.toLowerCase(Locale.ROOT);
        };
    }

    /**
     * Build a descriptive, multi-line error message when an unsupported locator
     * type is encountered.
     *
     * <p>The message includes the raw value, the detected type and value,
     * and a brief list of supported locator styles to help testers correct the input.</p>
     *
     * @param raw the original raw locator string supplied by the caller
     * @param type the resolved/upper-cased type portion
     * @param value the value portion or empty string
     * @return a detailed error message suitable for logging or throwing in an exception
     */
    private String buildUnsupportedLocatorMessage(String raw, String type, String value) {
        return "\n========== PTAF MOBILE LOCATOR TYPE NOT SUPPORTED ==========\n"
                + "Raw Value : " + raw + "\n"
                + "Type      : " + type + "\n"
                + "Value     : " + value + "\n"
                + "Supported : ACCESSIBILITY_ID_, ID_, XPATH_, CLASS_NAME_, NAME_, ANDROID_UIAUTOMATOR_, IOS_PREDICATE_, IOS_CLASS_CHAIN_, CSS_, CLASS_, TESTID_, PLACEHOLDER_, LABEL_, TITLE_, ALTTEXT_, TEXT_, LINKTEXT_, Button_, TEXTBOX_\n"
                + "Guidance  : Use a stable accessibility id for native mobile when possible. Use CSS_/XPATH_/TEXTBOX_/Button_ for real browser DOM automation.\n"
                + "===========================================================\n";
    }

    /**
     * Escape single quotes for embedding a value into a single-quoted CSS attribute selector.
     *
     * <p>Note: This is a simple helper intended for use in the TESTID css selector
     * generation and deliberately only escapes single quotes by prefixing them with a backslash.</p>
     *
     * @param value the raw value to escape (may be null)
     * @return an escaped string safe for usage inside single-quoted CSS attribute selectors
     */
    private String cssEscape(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }

    /**
     * Produce an XPath literal for an arbitrary string value.
     *
     * <p>XPath lacks a clean escaping mechanism for strings containing both single
     * and double quotes. This helper returns:
     * - "''" for null
     * - a single-quoted literal if the value contains no single quotes
     * - a double-quoted literal if the value contains no double quotes
     * - otherwise a concat(...) expression that pieces together single-quoted parts
     *   and embedded double-quote segments to represent the original string.</p>
     *
     * @param value the raw value to convert to an XPath literal (may be null)
     * @return a valid XPath literal representing the provided value
     */
    private String xpathLiteral(String value) {
        if (value == null) return "''";
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        // If we reach here the string contains both single and double quotes.
        // Build a concat(...) expression splitting on single quotes and inserting "'"
        // segments between the parts.
        String[] parts = value.split("'");
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", \"'\", ");
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Simple immutable tuple for returning a locator type and its value.
     *
     * <p>This record is package-private and used internally to avoid creating
     * multiple small classes. It holds the raw type token and the resolved
     * value portion (which may be empty).</p>
     */
    private record LocatorToken(String type, String value) { }
}
