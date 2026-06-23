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
 */
public class MobilePermissionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MobilePermissionHandler.class);
    private final AppiumDriver driver;

    public MobilePermissionHandler(AppiumDriver driver) {
        if (driver == null) throw new IllegalArgumentException("Appium driver cannot be null.");
        this.driver = driver;
    }

    /** Attempts to allow the currently displayed permission popup. Returns true only when a button was clicked. */
    public boolean allowIfDisplayed() {
        return handleIfDisplayed("allow", null, MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /** Attempts to deny the currently displayed permission popup. Returns true only when a button was clicked. */
    public boolean denyIfDisplayed() {
        return handleIfDisplayed("deny", null, MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /** Attempts to click a permission/system-dialog button containing the requested visible text. */
    public boolean allowWithTextIfDisplayed(String text) {
        return handleIfDisplayed("allow", text, MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /** Handles multiple permission popups in sequence. The loop is bounded by mobile.permissions.max_popups_to_handle. */
    public int allowAllIfDisplayed() {
        int handled = 0;
        int max = Math.max(1, MobileConfigurationProperties.getPermissionMaxPopups());
        for (int i = 0; i < max; i++) {
            if (allowIfDisplayed()) handled++; else break;
        }
        logger.info("Handled [{}] mobile permission popup(s) using allow action.", handled);
        return handled;
    }

    /** Handles multiple deny-style permission popups in sequence. The loop is bounded by mobile.permissions.max_popups_to_handle. */
    public int denyAllIfDisplayed() {
        int handled = 0;
        int max = Math.max(1, MobileConfigurationProperties.getPermissionMaxPopups());
        for (int i = 0; i < max; i++) {
            if (denyIfDisplayed()) handled++; else break;
        }
        logger.info("Handled [{}] mobile permission popup(s) using deny action.", handled);
        return handled;
    }

    public boolean handleIfDisplayed(String action, String text, int timeoutSeconds) {
        String normalizedAction = action == null ? "allow" : action.trim().toLowerCase(Locale.ROOT);
        int timeout = Math.max(0, timeoutSeconds);
        if (MobileConfigurationProperties.capturePermissionEvidence()) {
            MobileEvidenceManager.captureNamedScreenshot(driver, "permission-before-" + normalizedAction);
        }
        for (By locator : candidateLocators(normalizedAction, text)) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(timeout))
                        .until(ExpectedConditions.elementToBeClickable(locator));
                String description = describe(element);
                element.click();
                logger.info("Clicked mobile permission/system dialog button [{}] using locator [{}].", description, locator);
                if (MobileConfigurationProperties.capturePermissionEvidence()) {
                    MobileEvidenceManager.captureNamedScreenshot(driver, "permission-after-" + normalizedAction);
                }
                return true;
            } catch (TimeoutException | NoSuchElementException ignored) {
                // Continue trying the next known platform locator. Absence of a popup is not a test failure.
            } catch (Exception e) {
                logger.debug("Permission locator [{}] was present but not actionable: {}", locator, e.getMessage());
            }
        }
        logger.info("No mobile permission/system popup found for action [{}] within [{}] second(s).", normalizedAction, timeout);
        return false;
    }

    private List<By> candidateLocators(String action, String requestedText) {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        boolean android = platform != null && platform.isAndroid();
        List<By> locators = new ArrayList<>();
        if (requestedText != null && !requestedText.trim().isEmpty()) {
            String safeText = escapeXPath(requestedText.trim());
            if (android) {
                locators.add(By.xpath("//*[contains(@text,'" + safeText + "') or contains(@content-desc,'" + safeText + "')]"));
            } else {
                locators.add(By.xpath("//*[contains(@name,'" + safeText + "') or contains(@label,'" + safeText + "') or contains(@value,'" + safeText + "')]"));
            }
        }
        if ("deny".equals(action) || "dismiss".equals(action)) {
            if (android) {
                locators.add(By.xpath("//*[contains(@resource-id,'permission_deny') or contains(@text,'Deny') or contains(@text,'DENY') or contains(@content-desc,'Deny') or contains(@text,'Don') or contains(@content-desc,'Don')]"));
            } else {
                locators.add(By.xpath("//*[contains(@name,'Don') or contains(@label,'Don') or contains(@name,'Deny') or contains(@label,'Deny') or contains(@name,'Not Now') or contains(@label,'Not Now') or contains(@name,'Cancel') or contains(@label,'Cancel')]"));
            }
        } else {
            if (android) {
                locators.add(By.xpath("//*[contains(@resource-id,'permission_allow') or contains(@text,'While using') or contains(@text,'Only this time') or contains(@text,'Allow') or contains(@text,'ALLOW') or contains(@content-desc,'Allow') or contains(@text,'OK')]"));
            } else {
                locators.add(By.xpath("//*[contains(@name,'Allow While Using') or contains(@label,'Allow While Using') or contains(@name,'Allow Once') or contains(@label,'Allow Once') or contains(@name,'Allow') or contains(@label,'Allow') or contains(@name,'OK') or contains(@label,'OK') or contains(@name,'Continue') or contains(@label,'Continue')]"));
            }
        }
        return locators;
    }

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
        return element.getTagName();
    }

    private String escapeXPath(String text) {
        return text.replace("'", "");
    }
}
