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

    public By getLocatorForType(String locatorValue) {
        if (locatorValue == null || locatorValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile locator value cannot be blank.");
        }

        String locator = locatorValue.trim();

        // ------------------------------------------------------------------
        // Existing mobile/Appium prefixes. Do not change this behavior because
        // many existing projects depend on these exact prefixes.
        // ------------------------------------------------------------------
        if (locator.startsWith("ACCESSIBILITY_ID_")) return AppiumBy.accessibilityId(locator.substring("ACCESSIBILITY_ID_".length()));
        if (locator.startsWith("ANDROID_UIAUTOMATOR_")) return AppiumBy.androidUIAutomator(locator.substring("ANDROID_UIAUTOMATOR_".length()));
        if (locator.startsWith("IOS_PREDICATE_")) return AppiumBy.iOSNsPredicateString(locator.substring("IOS_PREDICATE_".length()));
        if (locator.startsWith("IOS_CLASS_CHAIN_")) return AppiumBy.iOSClassChain(locator.substring("IOS_CLASS_CHAIN_".length()));
        if (locator.startsWith("CLASS_NAME_")) return AppiumBy.className(locator.substring("CLASS_NAME_".length()));
        if (locator.startsWith("XPATH_")) return AppiumBy.xpath(locator.substring("XPATH_".length()));
        if (locator.startsWith("ID_")) return AppiumBy.id(locator.substring("ID_".length()));
        if (locator.startsWith("NAME_")) return AppiumBy.name(locator.substring("NAME_".length()));

        LocatorToken token = splitTypeAndValue(locator);
        String type = token.type().toUpperCase(Locale.ROOT);
        String value = token.value();

        // ------------------------------------------------------------------
        // Shared UI-style locator aliases for Appium browser/native contexts.
        // These aliases intentionally resolve to Selenium/Appium By locators,
        // not Playwright locators. They are additive only.
        // ------------------------------------------------------------------
        return switch (type) {
            case "CSS" -> By.cssSelector(value);
            case "TAG" -> By.tagName(value);
            case "CLASS" -> By.className(value);
            case "TESTID" -> By.cssSelector("[data-testid='" + cssEscape(value) + "'],[data-test='" + cssEscape(value) + "'],[testID='" + cssEscape(value) + "']");
            case "PLACEHOLDER" -> By.xpath("//*[@placeholder=" + xpathLiteral(value) + " or @value=" + xpathLiteral(value) + "]");
            case "LABEL" -> By.xpath("//*[@aria-label=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + "]");
            case "TITLE" -> By.xpath("//*[@title=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            case "ALTTEXT" -> By.xpath("//*[@alt=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            case "TEXT" -> By.xpath("//*[normalize-space(.)=" + xpathLiteral(value) + " or contains(normalize-space(.)," + xpathLiteral(value) + ") or @text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            case "LINKTEXT" -> By.xpath("//a[normalize-space(.)=" + xpathLiteral(value) + " or contains(normalize-space(.)," + xpathLiteral(value) + ")] | //*[@role='link' and (normalize-space(.)=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + ")]");
            case "BUTTON" -> By.xpath("//button[normalize-space(.)=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + " or @title=" + xpathLiteral(value) + "] | //*[@role='button' and (normalize-space(.)=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + " or @content-desc=" + xpathLiteral(value) + "]");
            case "TEXTBOX", "INPUT" -> By.xpath("//input[@name=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + " or @placeholder=" + xpathLiteral(value) + " or @title=" + xpathLiteral(value) + "] | //textarea[@name=" + xpathLiteral(value) + " or @aria-label=" + xpathLiteral(value) + " or @placeholder=" + xpathLiteral(value) + "] | //*[@role='textbox' and (@aria-label=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + ")] | //XCUIElementTypeTextField[@name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + " or @value=" + xpathLiteral(value) + "] | //android.widget.EditText[@text=" + xpathLiteral(value) + " or @content-desc=" + xpathLiteral(value) + "]");
            case "CHECKBOX" -> By.xpath("//*[@role='checkbox' and (@aria-label=" + xpathLiteral(value) + " or normalize-space(.)=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            case "RADIOBUTTON", "RADIO" -> By.xpath("//*[@role='radio' and (@aria-label=" + xpathLiteral(value) + " or normalize-space(.)=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            case "DROPDOWN", "COMBOBOX", "OPTION", "IMAGE", "HEADING", "TAB", "LIST", "LISTITEM", "TABLE", "ROW", "CELL", "DIALOG", "MENU", "MENUITEM" ->
                    By.xpath("//*[@role='" + roleName(type) + "' and (@aria-label=" + xpathLiteral(value) + " or normalize-space(.)=" + xpathLiteral(value) + ")] | //*[@text=" + xpathLiteral(value) + " or @name=" + xpathLiteral(value) + " or @label=" + xpathLiteral(value) + "]");
            default -> throw new IllegalArgumentException(buildUnsupportedLocatorMessage(locatorValue, type, value));
        };
    }

    private LocatorToken splitTypeAndValue(String locator) {
        int underscore = locator.indexOf('_');
        int space = locator.indexOf(' ');
        int separator;
        if (underscore >= 0 && space >= 0) separator = Math.min(underscore, space);
        else separator = Math.max(underscore, space);
        if (separator < 0) return new LocatorToken(locator, "");
        return new LocatorToken(locator.substring(0, separator).trim(), locator.substring(separator + 1).trim());
    }

    private String roleName(String type) {
        return switch (type) {
            case "DROPDOWN", "COMBOBOX" -> "combobox";
            case "RADIOBUTTON", "RADIO" -> "radio";
            case "LISTITEM" -> "listitem";
            case "MENUITEM" -> "menuitem";
            default -> type.toLowerCase(Locale.ROOT);
        };
    }

    private String buildUnsupportedLocatorMessage(String raw, String type, String value) {
        return "\n========== PTAF MOBILE LOCATOR TYPE NOT SUPPORTED ==========\n"
                + "Raw Value : " + raw + "\n"
                + "Type      : " + type + "\n"
                + "Value     : " + value + "\n"
                + "Supported : ACCESSIBILITY_ID_, ID_, XPATH_, CLASS_NAME_, NAME_, ANDROID_UIAUTOMATOR_, IOS_PREDICATE_, IOS_CLASS_CHAIN_, CSS_, CLASS_, TESTID_, PLACEHOLDER_, LABEL_, TITLE_, ALTTEXT_, TEXT_, LINKTEXT_, Button_, TEXTBOX_\n"
                + "Guidance  : Use a stable accessibility id for native mobile when possible. Use CSS_/XPATH_/TEXTBOX_/Button_ for real browser DOM automation.\n"
                + "===========================================================\n";
    }

    private String cssEscape(String value) {
        return value == null ? "" : value.replace("'", "\\'");
    }

    private String xpathLiteral(String value) {
        if (value == null) return "''";
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        String[] parts = value.split("'");
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", \"'\", ");
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    private record LocatorToken(String type, String value) { }
}
