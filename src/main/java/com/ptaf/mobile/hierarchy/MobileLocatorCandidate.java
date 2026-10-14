package com.ptaf.mobile.hierarchy;

/**
 * A generated locator suggestion that requires tester review before use in an active
 * {@code mobile_elements} YAML file.
 *
 * @param key logical YAML key suggestion
 * @param locator framework-compatible locator value
 * @param source hierarchy attribute used to create the suggestion
 * @param confidence human-readable stability indication
 */
public record MobileLocatorCandidate(
        String key,
        String locator,
        String source,
        String confidence
) {
}
