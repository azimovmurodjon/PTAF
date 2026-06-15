package com.ptaf.mobile.handlers;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Converts PTAF mobile YAML locator strings into Selenium/Appium locators.
 */
public class MobileLocatorHandler {
    public By getLocatorForType(String locatorValue) {
        if (locatorValue == null || locatorValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile locator value cannot be blank.");
        }
        String locator = locatorValue.trim();
        if (locator.startsWith("ACCESSIBILITY_ID_")) return AppiumBy.accessibilityId(locator.substring("ACCESSIBILITY_ID_".length()));
        if (locator.startsWith("ID_")) return AppiumBy.id(locator.substring("ID_".length()));
        if (locator.startsWith("XPATH_")) return AppiumBy.xpath(locator.substring("XPATH_".length()));
        if (locator.startsWith("CLASS_NAME_")) return AppiumBy.className(locator.substring("CLASS_NAME_".length()));
        if (locator.startsWith("ANDROID_UIAUTOMATOR_")) return AppiumBy.androidUIAutomator(locator.substring("ANDROID_UIAUTOMATOR_".length()));
        if (locator.startsWith("IOS_PREDICATE_")) return AppiumBy.iOSNsPredicateString(locator.substring("IOS_PREDICATE_".length()));
        if (locator.startsWith("IOS_CLASS_CHAIN_")) return AppiumBy.iOSClassChain(locator.substring("IOS_CLASS_CHAIN_".length()));
        if (locator.startsWith("NAME_")) return AppiumBy.name(locator.substring("NAME_".length()));
        throw new IllegalArgumentException("Unsupported mobile locator type: " + locatorValue);
    }
}
