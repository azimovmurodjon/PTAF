package com.ptaf.mobile.permissions;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Enterprise permission and system-dialog handler for native Appium tests.
 *
 * <p>Mobile applications frequently request operating-system permissions such as location,
 * camera, photos, microphone, notifications, contacts, calendars, Bluetooth, and local network
 * access. These dialogs are rendered by iOS/Android system UI rather than the application itself,
 * so project page locators are not always appropriate.</p>
 *
 * <p>This helper provides safe, reusable behavior for Cucumber steps. It intentionally does not
 * fail when a permission popup is absent. That design lets one cross-platform feature run on a
 * clean emulator/simulator, a reused device, a CI device farm, or a real device where permissions
 * may already be granted.</p>
 *
 * <p>Behavior summary:
 * - Attempts to find common OS-level permission dialog buttons and click them.
 * - Supports platform-specific locators for Android and iOS.
 * - Optionally captures screenshots before/after interaction based on configuration.
 * - Does not throw on missing dialogs; it returns boolean indicating whether a button was clicked.
 * - Can be instructed to handle a specific visible button text (useful for custom wording).</p>
 */
public class MobilePermissionHandler {
    // Logger used for informational/debug output. Visible to test logs.
    private static final Logger logger = LoggerFactory.getLogger(MobilePermissionHandler.class);

    // Appium driver used to query for system UI elements and to click buttons.
    private final AppiumDriver driver;

    /**
     * Construct a permission handler bound to the provided Appium driver.
     *
     * @param driver non-null AppiumDriver that drives the AUT (application under test) session.
     * @throws IllegalArgumentException if driver is null (driver is required).
     */
    public MobilePermissionHandler(AppiumDriver driver) {
        if (driver == null) throw new IllegalArgumentException("Appium driver cannot be null.");
        this.driver = driver;
    }

    /**
     * Attempts to allow the currently displayed permission popup.
     *
     * <p>This will try a set of common "allow" locators suitable for the current platform.
     * The method returns true only when a matching button was found and clicked. If no popup
     * is found within the configured timeout the method returns false (this is not considered
     * a test failure).</p>
     *
     * @return true if an "allow"-style button was found and clicked; false otherwise.
     */
    public boolean allowIfDisplayed() {
        return handleIfDisplayed("allow", null, MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /**
     * Attempts to deny the currently displayed permission popup.
     *
     * <p>Works similarly to allowIfDisplayed but searches for deny-style buttons.</p>
     *
     * @return true if a "deny"-style button was found and clicked; false otherwise.
     */
    public boolean denyIfDisplayed() {
        return handleIfDisplayed("deny", null, MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /**
     * Attempts to click a permission/system-dialog button containing the requested visible text.
     *
     * <p>Useful when the dialog contains a non-standard action button with a specific label
     * (for example "Allow Once", "While Using the App", or localized/custom text).</p>
     *
     * @param text visible text to search for inside the button element (case-sensitive match via XPath contains).
     * @return true if a button containing the given text was found and clicked; false otherwise.
     */
    public boolean allowWithTextIfDisplayed(String text) {
        return handleIfDisplayed("allow", text, MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /**
     * Handles multiple permission popups in sequence. The number of iterations is bounded by
     * mobile.permissions.max_popups_to_handle (minimum 1). Stops early if no dialog is found.
     *
     * <p>Useful for scenarios where an app may trigger sequential permission prompts (e.g. location
     * then notifications).</p>
     *
     * @return the number of permission popups that were handled (clicked).
     */
    public int allowAllIfDisplayed() {
        int handled = 0;
        int max = Math.max(1, MobileConfigurationProperties.getPermissionMaxPopups());
        for (int i = 0; i < max; i++) {
            if (allowIfDisplayed()) handled++; else break;
        }
        logger.info("Handled [{}] mobile permission popup(s) using allow action.", handled);
        return handled;
    }

    /**
     * Handles multiple deny-style permission popups in sequence. The number of iterations is bounded by
     * mobile.permissions.max_popups_to_handle (minimum 1). Stops early if no dialog is found.
     *
     * @return the number of permission popups that were handled (clicked).
     */
    public int denyAllIfDisplayed() {
        int handled = 0;
        int max = Math.max(1, MobileConfigurationProperties.getPermissionMaxPopups());
        for (int i = 0; i < max; i++) {
            if (denyIfDisplayed()) handled++; else break;
        }
        logger.info("Handled [{}] mobile permission popup(s) using deny action.", handled);
        return handled;
    }

    /**
     * Core handler that attempts to find and click a permission/system-dialog button.
     *
     * <p>Strategy:
     * - Normalize the requested action (allow/deny/dismiss).
     * - Optionally capture a screenshot before action (if configured).
     * - Build a prioritized list of candidate locators for the current platform and any provided text.
     * - Iterate over candidates, waiting up to timeoutSeconds for an element to be clickable.
     * - Click the first actionable element found and optionally capture an after screenshot.
     * - Do not throw if the popup is absent; log and return false instead.</p>
     *
     * @param action "allow", "deny", etc. If null defaults to "allow".
     * @param text optional visible text to search for within the button (may be null).
     * @param timeoutSeconds how many seconds to wait for each candidate locator to become clickable.
     * @return true when a click action was performed; false when no actionable element was found.
     */
    public boolean handleIfDisplayed(String action, String text, int timeoutSeconds) {
        // Normalize action string to lower-case and ensure non-null.
        String normalizedAction = action == null ? "allow" : action.trim().toLowerCase(Locale.ROOT);

        // Ensure non-negative timeout.
        int timeout = Math.max(0, timeoutSeconds);

        // If configured, capture a screenshot before interacting with a permission dialog.
        if (MobileConfigurationProperties.capturePermissionEvidence()) {
            MobileEvidenceManager.captureNamedScreenshot(driver, "permission-before-" + normalizedAction);
        }

        // Iterate through candidate locators in prioritized order and attempt to click the first actionable one.
        for (By locator : candidateLocators(normalizedAction, text)) {
            try {
                // Wait until the element is clickable. This handles brief animation/transition periods.
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(timeout))
                        .until(ExpectedConditions.elementToBeClickable(locator));

                // Create a human-readable description for logging (text, label, name, or tag).
                String description = describe(element);

                // Perform the click on the system dialog button.
                element.click();

                logger.info("Clicked mobile permission/system dialog button [{}] using locator [{}].", description, locator);

                // Capture screenshot after clicking if enabled.
                if (MobileConfigurationProperties.capturePermissionEvidence()) {
                    MobileEvidenceManager.captureNamedScreenshot(driver, "permission-after-" + normalizedAction);
                }
                return true;
            } catch (TimeoutException | NoSuchElementException ignored) {
                // The specific locator was not present or not clickable within the timeout. This is expected
                // in many flows (dialog not present, different button text, different platform). Continue trying
                // the next locator rather than failing the test.
            } catch (Exception e) {
                // If an unexpected exception occurs while attempting to interact with a present element,
                // log at debug level but continue trying other locators. This avoids flakiness for non-actionable
                // or stale elements.
                logger.debug("Permission locator [{}] was present but not actionable: {}", locator, e.getMessage());
            }
        }

        // No actionable permission dialog button was found for any of the candidate locators.
        logger.info("No mobile permission/system popup found for action [{}] within [{}] second(s).", normalizedAction, timeout);
        return false;
    }

    /**
     * Build a prioritized list of XPath locators that target common permission dialog buttons
     * for the current mobile platform (Android or iOS). The ordering is important — earlier
     * locators are given higher priority.
     *
     * <p>If requestedText is provided, a generic locator searching for that text/content-desc/name/label
     * is added first to increase the chance of matching custom/localized wording.</p>
     *
     * @param action normalized action hint ("allow", "deny", "dismiss", etc.). Used to choose between allow/deny candidate sets.
     * @param requestedText optional exact visible text to prefer when searching for an action button.
     * @return ordered list of By locators to try when searching for permission buttons.
     */
    private List<By> candidateLocators(String action, String requestedText) {
        // Determine platform to select correct attribute names (Android: @text/@content-desc/@resource-id, iOS: @name/@label/@value).
        MobilePlatform platform = MobileDriverManager.getPlatform();
        boolean android = platform != null && platform.isAndroid();

        List<By> locators = new ArrayList<>();

        // If a specific text was requested, add a generic contains() XPath that looks across common attributes.
        // This helps when the button uses a custom/localized label.
        if (requestedText != null && !requestedText.trim().isEmpty()) {
            String safeText = escapeXPath(requestedText.trim());
            if (android) {
                // Android: search in text or content-desc attributes.
                locators.add(By.xpath("//*[contains(@text,'" + safeText + "') or contains(@content-desc,'" + safeText + "')]"));
            } else {
                // iOS: search in name, label, or value attributes.
                locators.add(By.xpath("//*[contains(@name,'" + safeText + "') or contains(@label,'" + safeText + "') or contains(@value,'" + safeText + "')]"));
            }
        }

        // Add deny-style locators when the caller requested a deny/dismiss action.
        if ("deny".equals(action) || "dismiss".equals(action)) {
            if (android) {
                // Common Android deny button patterns. Includes resource-id hints and multiple textual variations
                // (Deny, DON/Don etc) to be resilient to capitalization and truncated strings.
                locators.add(By.xpath("//*[contains(@resource-id,'permission_deny') or contains(@text,'Deny') or contains(@text,'DENY') or contains(@content-desc,'Deny') or contains(@text,'Don') or contains(@content-desc,'Don')]"));
            } else {
                // iOS deny-like options: "Don't Allow" often appears as "Don" prefix in some localized strings,
                // also handle "Deny", "Not Now", "Cancel".
                locators.add(By.xpath("//*[contains(@name,'Don') or contains(@label,'Don') or contains(@name,'Deny') or contains(@label,'Deny') or contains(@name,'Not Now') or contains(@label,'Not Now') or contains(@name,'Cancel') or contains(@label,'Cancel')]"));
            }
        } else {
            // Default to allow-style locators (also used for actions other than deny/dismiss).
            if (android) {
                // Android allow patterns: resource-id hints, various textual variants including "While using", "Only this time", "Allow", "OK".
                locators.add(By.xpath("//*[contains(@resource-id,'permission_allow') or contains(@text,'While using') or contains(@text,'Only this time') or contains(@text,'Allow') or contains(@text,'ALLOW') or contains(@content-desc,'Allow') or contains(@text,'OK')]"));
            } else {
                // iOS allow patterns: "Allow While Using", "Allow Once", generic "Allow", "OK", "Continue".
                // We search both name and label attributes since iOS system dialogs expose either depending on element type.
                locators.add(By.xpath("//*[contains(@name,'Allow While Using') or contains(@label,'Allow While Using') or contains(@name,'Allow Once') or contains(@label,'Allow Once') or contains(@name,'Allow') or contains(@label,'Allow') or contains(@name,'OK') or contains(@label,'OK') or contains(@name,'Continue') or contains(@label,'Continue')]"));
            }
        }

        return locators;
    }

    /**
     * Attempt to create a concise human-readable description for a WebElement.
     *
     * <p>Checks common properties in order:
     * - text
     * - label attribute
     * - name attribute
     * - tag name (fallback)</p>
     *
     * <p>This is intended to produce useful log messages for testers when a button is clicked.</p>
     *
     * @param element WebElement that was found/clicked.
     * @return best-effort description string for logging.
     */
    private String describe(WebElement element) {
        try {
            String text = element.getText();
            if (text != null && !text.isBlank()) return text;
        } catch (Exception ignored) { }
        try {
            String label = element.getAttribute("label");
            if (label != null && !label.isBlank()) return label;
        } catch (Exception ignored) { }
        try {
            String name = element.getAttribute("name");
            if (name != null && !name.isBlank()) return name;
        } catch (Exception ignored) { }
        // As a last resort return the tag name so logs still show something identifying the element.
        return element.getTagName();
    }

    /**
     * Escape text for safe use inside an XPath contains() call.
     *
     * <p>Note: This is a conservative approach that simply removes single quotes from the
     * provided text to avoid breaking the XPath literal. It is intentionally simple to avoid
     * introducing complex quoting behavior. Testers should avoid passing values that contain
     * single quotes or update this method if more robust escaping is required.</p>
     *
     * @param text input text that will be embedded into an XPath expression.
     * @return sanitized text with single quotes removed.
     */
    private String escapeXPath(String text) {
        return text.replace("'", "");
    }
}
