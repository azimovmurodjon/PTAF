package com.ptaf.mobile.pages;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.handlers.MobileLocatorHandler;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import com.ptaf.softassert.SoftAssertionContext;
import com.ptaf.utils.ConfigurationProperties;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.appium.java_client.HidesKeyboard;
import io.appium.java_client.InteractsWithApps;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reusable Appium mobile actions used by Cucumber step definitions.
 *
 * <p>The class intentionally exposes enterprise-ready high-level mobile actions
 * so testers can automate common native app behavior from Cucumber without
 * writing Java code in project teams.</p>
 */
public class MobileCommonMethods {
    // SLF4J logger for structured logging of actions, errors, and soft assertion events.
    private static final Logger logger = LoggerFactory.getLogger(MobileCommonMethods.class);

    // Underlying Appium driver used by all actions.
    private final AppiumDriver driver;
    // Helper responsible for converting string locator descriptors into Selenium By objects.
    private final MobileLocatorHandler locatorHandler = new MobileLocatorHandler();

    /**
     * Create a wrapper around an AppiumDriver to expose reusable mobile actions.
     *
     * @param driver AppiumDriver instance (must not be null)
     * @throws IllegalArgumentException if the provided driver is null
     */
    public MobileCommonMethods(AppiumDriver driver) {
        if (driver == null) throw new IllegalArgumentException("Appium driver cannot be null.");
        this.driver = driver;
    }

    /* Simple element interactions - these forward to findVisibleElement which includes
       explicit waiting and browser context synchronization when required. */

    /**
     * Tap a visible element identified by page/locator keys.
     * In soft assertion mode, if the element was not found (null returned), the tap is skipped gracefully.
     */
    public void tap(String page, String locator) {
        WebElement el = findVisibleElement(page, locator);
        if (el != null) el.click();
    }

    /**
     * Type into an input element after clearing it first.
     * If value is null, an empty string is typed to avoid NPE.
     * In soft assertion mode, if the element was not found (null returned), the type is skipped gracefully.
     */
    public void type(String page, String locator, String value) {
        WebElement el = findVisibleElement(page, locator);
        if (el != null) { el.clear(); el.sendKeys(value == null ? "" : value); }
    }

    /**
     * Clear the input field's current value.
     * In soft assertion mode, if the element was not found (null returned), the clear is skipped gracefully.
     */
    public void clear(String page, String locator) {
        WebElement el = findVisibleElement(page, locator);
        if (el != null) el.clear();
    }

    /**
     * Return visible element text.
     * In soft assertion mode, if the element was not found (null returned), returns an empty string.
     */
    public String getText(String page, String locator) {
        WebElement el = findVisibleElement(page, locator);
        return el != null ? el.getText() : "";
    }

    /**
     * Check if an element is visible on screen.
     * Returns false when any exception occurs locating the element, or when soft assertion mode
     * returns null (element not found but failure already recorded).
     */
    public boolean isVisible(String page, String locator) {
        try {
            WebElement el = findVisibleElement(page, locator);
            return el != null && el.isDisplayed();
        } catch (Exception e) { return false; }
    }

    /**
     * Check if an element is enabled (interactable).
     * In soft assertion mode, if the element was not found (null returned), returns false.
     */
    public boolean isEnabled(String page, String locator) {
        WebElement el = findVisibleElement(page, locator);
        return el != null && el.isEnabled();
    }

    /**
     * Check if an element is selected (useful for checkboxes/radio buttons).
     * In soft assertion mode, if the element was not found (null returned), returns false.
     */
    public boolean isSelected(String page, String locator) {
        WebElement el = findVisibleElement(page, locator);
        return el != null && el.isSelected();
    }

    /**
     * Explicitly wait until element is visible.
     * In soft assertion mode, if the element was not found, the failure is already recorded.
     */
    public void waitForVisible(String page, String locator) { findVisibleElement(page, locator); }

    /**
     * Waits for a locator using an explicit timeout supplied from the feature file.
     *
     * This variation uses ExpectedConditions.visibilityOfElementLocated with a
     * custom timeout. It also contains diagnostic handling for iOS Safari WEBVIEW
     * contexts which can surface non-standard runtime errors when the webview is
     * not yet available.
     *
     * @param page locator page key
     * @param locator locator key on the page
     * @param timeoutSeconds maximum seconds to wait (non-negative)
     */
    public void waitForVisible(String page, String locator, int timeoutSeconds) {
        ensureBrowserWebContextReadyIfNeeded();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(timeoutSeconds, 0)));
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(resolveLocator(page, locator)));
        } catch (ClassCastException e) {
            // Some WebKit runtime failures manifest as ClassCastException - surface a helpful message.
            throw browserContextDiagnosticFailure(page, locator, e);
        } catch (RuntimeException e) {
            // Additional heuristic to detect Safari runtime context problems and provide diagnostics.
            if (looksLikeSafariRuntimeContextFailure(e)) {
                throw browserContextDiagnosticFailure(page, locator, e);
            }
            throw e;
        }
    }

    /**
     * Waits until a locator disappears or becomes invisible.
     * Useful for loaders, splash screens, and transient dialogs.
     *
     * @param page locator page key
     * @param locator locator key on the page
     * @param timeoutSeconds maximum seconds to wait (non-negative)
     */
    public void waitForNotVisible(String page, String locator, int timeoutSeconds) {
        ensureBrowserWebContextReadyIfNeeded();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(timeoutSeconds, 0)));
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(resolveLocator(page, locator)));
        } catch (ClassCastException e) {
            throw browserContextDiagnosticFailure(page, locator, e);
        } catch (RuntimeException e) {
            if (looksLikeSafariRuntimeContextFailure(e)) {
                throw browserContextDiagnosticFailure(page, locator, e);
            }
            throw e;
        }
    }

    /**
     * Pauses execution for a given number of seconds.
     *
     * Note: Prefer explicit waits (findVisibleElement/waitForVisible/etc.) in test code
     * but this method can be useful for rare cases such as long app transitions.
     *
     * @param seconds number of seconds to pause (negative treated as zero)
     */
    public void pause(int seconds) {
        try { Thread.sleep(Math.max(seconds, 0) * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Long press (press-and-hold) on element center for a given duration.
     *
     * @param page locator page key
     * @param locator locator key on the page
     * @param durationMillis how long to hold in milliseconds (minimum 500ms applied)
     */
    public void longPress(String page, String locator, long durationMillis) {
        WebElement element = findVisibleElement(page, locator);
        int x = element.getRect().getX() + element.getRect().getWidth() / 2;
        int y = element.getRect().getY() + element.getRect().getHeight() / 2;
        pressAt(x, y, Math.max(durationMillis, 500));
    }

    /**
     * Double-tap on an element by performing two quick tap actions at the element's center.
     *
     * @param page locator page key
     * @param locator locator key on the page
     */
    public void doubleTap(String page, String locator) {
        WebElement element = findVisibleElement(page, locator);
        int x = element.getRect().getX() + element.getRect().getWidth() / 2;
        int y = element.getRect().getY() + element.getRect().getHeight() / 2;
        tapAt(x, y);
        tapAt(x, y);
    }

    /**
     * Single touch tap at an (x,y) coordinate in the viewport.
     *
     * This uses the W3C Actions API with a single touch pointer.
     *
     * @param x x coordinate (pixels)
     * @param y y coordinate (pixels)
     */
    public void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    /**
     * Drag one element to another element's center.
     * Useful for reordering or drag-and-drop gestures.
     *
     * @param fromPage source page key
     * @param fromLocator source locator key
     * @param toPage destination page key
     * @param toLocator destination locator key
     */
    public void drag(String fromPage, String fromLocator, String toPage, String toLocator) {
        WebElement from = findVisibleElement(fromPage, fromLocator);
        WebElement to = findVisibleElement(toPage, toLocator);
        int startX = from.getRect().getX() + from.getRect().getWidth() / 2;
        int startY = from.getRect().getY() + from.getRect().getHeight() / 2;
        int endX = to.getRect().getX() + to.getRect().getWidth() / 2;
        int endY = to.getRect().getY() + to.getRect().getHeight() / 2;
        // Default drag duration is 800ms to mimic a natural swipe/drag.
        dragFromTo(startX, startY, endX, endY, 800);
    }

    /**
     * Scroll repeatedly (swipe up) until a target element becomes visible or until maxSwipes is reached.
     * If the element is not found after the attempts, a final findVisibleElement is called to throw a useful error.
     *
     * @param page locator page key
     * @param locator locator key on the page
     * @param maxSwipes maximum swipe attempts (must be >= 1)
     */
    public void scrollUntilVisible(String page, String locator, int maxSwipes) {
        int attempts = Math.max(maxSwipes, 1);
        for (int i = 0; i < attempts; i++) {
            if (isVisible(page, locator)) return;
            swipeUp();
        }
        // Final attempt will either return the element or raise an explicit error.
        findVisibleElement(page, locator);
    }

    /**
     * Scroll to an element by visible text. Platform-aware: uses different attributes on Android vs iOS.
     * This method constructs an XPath that searches common attributes for the provided text.
     *
     * @param text visible text to locate
     */
    public void scrollToText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to scroll cannot be blank.");
        }
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (platform != null && platform.isAndroid()) {
            driver.findElement(By.xpath("//*[contains(@text,'" + escapeXPath(text) + "') or contains(@content-desc,'" + escapeXPath(text) + "')]"));
        } else {
            driver.findElement(By.xpath("//*[contains(@name,'" + escapeXPath(text) + "') or contains(@label,'" + escapeXPath(text) + "') or contains(@value,'" + escapeXPath(text) + "')]"));
        }
    }

    /**
     * Hide the on-screen keyboard if the driver supports it.
     * Any exceptions are swallowed because keyboard absence is not a test failure in most cases.
     */
    public void hideKeyboard() { try { if (driver instanceof HidesKeyboard) ((HidesKeyboard) driver).hideKeyboard(); } catch (Exception ignored) { } }

    /**
     * Send app to background for a number of seconds.
     *
     * @param seconds seconds to background the app (minimum 1 second enforced)
     */
    public void backgroundApp(int seconds) {
        driver.executeScript("mobile: backgroundApp", Map.of("seconds", Math.max(seconds, 1)));
    }

    /**
     * Activate another app on the device by bundleId/package.
     *
     * @param appId application bundle id (iOS) or package (Android)
     */
    public void activateApp(String appId) {
        if (driver instanceof InteractsWithApps) { ((InteractsWithApps) driver).activateApp(appId); return; }
        throw new UnsupportedOperationException("Current Appium driver does not support activateApp.");
    }

    /**
     * Terminate another app on the device by bundleId/package.
     *
     * @param appId application bundle id (iOS) or package (Android)
     */
    public void terminateApp(String appId) {
        if (driver instanceof InteractsWithApps) { ((InteractsWithApps) driver).terminateApp(appId); return; }
        throw new UnsupportedOperationException("Current Appium driver does not support terminateApp.");
    }

    /**
     * Open a deep link into a target application. Uses platform-specific Appium mobile commands.
     *
     * @param url deep link URL
     * @param appPackageOrBundleId Android package name or iOS bundleId
     */
    public void openDeepLink(String url, String appPackageOrBundleId) {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (platform != null && platform.isAndroid()) {
            driver.executeScript("mobile: deepLink", Map.of("url", url, "package", appPackageOrBundleId));
        } else {
            driver.executeScript("mobile: launchApp", Map.of("bundleId", appPackageOrBundleId, "arguments", List.of(url)));
        }
    }

    /**
     * Push a local file into the device/simulator at a remote path.
     *
     * @param remotePath destination path on device
     * @param localPath local filesystem path to read file from
     */
    public void pushFile(String remotePath, String localPath) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(localPath));
            String encoded = Base64.getEncoder().encodeToString(bytes);
            driver.executeScript("mobile: pushFile", Map.of("remotePath", remotePath, "payload", encoded));
        } catch (Exception e) {
            throw new RuntimeException("Unable to push file to mobile device: " + localPath + " -> " + remotePath, e);
        }
    }

    /**
     * Pull a file from the device to a local output path.
     *
     * @param remotePath remote path on device
     * @param localOutputPath local filesystem destination
     */
    public void pullFile(String remotePath, String localOutputPath) {
        try {
            Object response = driver.executeScript("mobile: pullFile", Map.of("remotePath", remotePath));
            if (response == null) {
                throw new IllegalStateException("Appium returned no file content for: " + remotePath);
            }
            byte[] bytes = Base64.getDecoder().decode(String.valueOf(response));
            Path output = Path.of(localOutputPath);
            Files.createDirectories(output.getParent() != null ? output.getParent() : Path.of("."));
            Files.write(output, bytes);
        } catch (Exception e) {
            throw new RuntimeException("Unable to pull file from mobile device: " + remotePath, e);
        }
    }

    /**
     * Set plaintext clipboard on the device using Appium mobile command.
     *
     * @param text text to set (null treated as empty string)
     */
    public void setClipboard(String text) {
        driver.executeScript("mobile: setClipboard", Map.of("content", Base64.getEncoder().encodeToString((text == null ? "" : text).getBytes()), "contentType", "plaintext"));
    }

    /**
     * Retrieve plaintext clipboard from the device.
     *
     * @return clipboard text or empty string if Appium returns null
     */
    public String getClipboard() {
        Object response = driver.executeScript("mobile: getClipboard", Map.of("contentType", "plaintext"));
        return response == null ? "" : new String(Base64.getDecoder().decode(String.valueOf(response)));
    }

    /**
     * Retrieve available automation contexts (e.g., NATIVE_APP, WEBVIEW_...) from the driver.
     *
     * This is invoked reflectively to support multiple Appium client implementations.
     *
     * @return set of context names
     * @throws UnsupportedOperationException when context switching is not supported by the current driver
     */
    @SuppressWarnings("unchecked")
    public Set<String> getContexts() {
        try {
            return (Set<String>) driver.getClass().getMethod("getContextHandles").invoke(driver);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Current Appium driver does not support context switching.", e);
        }
    }

    /**
     * Switch driver context (reflective invocation of driver.context(name)).
     *
     * @param contextName context name to switch to (e.g., "NATIVE_APP" or "WEBVIEW_...")
     */
    public void switchContext(String contextName) {
        try {
            driver.getClass().getMethod("context", String.class).invoke(driver, contextName);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Current Appium driver does not support context switching.", e);
        }
    }

    /** Convenience method to return to native context. */
    public void switchToNativeContext() { switchContext("NATIVE_APP"); }

    /** Grant a runtime permission on Android using Appium changePermissions extension. */
    public void grantPermission(String appId, String permission) {
        driver.executeScript("mobile: changePermissions", Map.of("appPackage", appId, "permissions", permission, "action", "grant"));
    }

    /** Revoke a runtime permission on Android using Appium changePermissions extension. */
    public void revokePermission(String appId, String permission) {
        driver.executeScript("mobile: changePermissions", Map.of("appPackage", appId, "permissions", permission, "action", "revoke"));
    }

    /**
     * Set device orientation. Accepts "portrait" or "landscape" (case-insensitive).
     *
     * @param orientation "portrait" or "landscape"
     */
    public void setOrientation(String orientation) {
        String normalized = normalizeOrientation(orientation);
        driver.executeScript("mobile: setDeviceOrientation", Map.of("orientation", normalized));
    }

    /**
     * Set orientation based on configuration properties for the current platform.
     * This is useful when test suites define a default orientation per platform.
     */
    public void setConfiguredOrientation() {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (platform != null) setOrientation(MobileConfigurationProperties.getOrientation(platform));
    }

    /**
     * Open a URL in a real mobile browser session.
     *
     * This method includes multiple workarounds for flaky iOS Safari automation:
     * - Ensures the driver is in a WEBVIEW context when required.
     * - Retries navigation when initial driver.get does not appear to load the expected host.
     * - Optionally uses a native address-bar fallback to type the URL into Safari on simulators that ignore the first navigation call.
     *
     * The method logs progress to System.out so that test logs can indicate navigation attempts and fallback usage.
     *
     * @param url URL to open (non-empty)
     */
    public void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile browser URL cannot be blank.");
        }
        String targetUrl = url.trim();
        try {
            System.out.println("PTAF APPIUM REAL BROWSER | Opening URL: " + targetUrl);
            driver.get(targetUrl);
            pause(2);
            ensureBrowserWebContextReadyIfNeeded();
            String current = safeCurrentUrlForDiagnostics();
            if (!isExpectedUrlLoaded(current, targetUrl)) {
                System.out.println("PTAF APPIUM REAL BROWSER | First navigation did not report expected URL. Retrying. Current URL: " + current);
                driver.navigate().to(targetUrl);
                pause(2);
                ensureBrowserWebContextReadyIfNeeded();
                current = safeCurrentUrlForDiagnostics();
            }
            if (!isExpectedUrlLoaded(current, targetUrl) && shouldUseSafariNativeNavigationFallback()) {
                System.out.println("PTAF APPIUM REAL BROWSER | Safari still appears to be on Start Page or URL is unreadable. Executing native address-bar fallback navigation.");
                navigateSafariFromNativeAddressBar(targetUrl);
                ensureBrowserWebContextReadyIfNeeded();
                current = safeCurrentUrlForDiagnostics();
            }
            System.out.println("PTAF APPIUM REAL BROWSER | Current URL after navigation: " + current);
        } catch (Exception e) {
            throw new RuntimeException("PTAF Appium real mobile browser could not open URL [" + targetUrl + "]. Check Safari/Chrome availability, Appium browser session capabilities, network connectivity, first-run browser popups, iOS Safari Start Page state, and WEBVIEW context availability. Root cause: " + e.getMessage(), e);
        }
    }

    /** Press Enter/Return key on a focused element identified by page/locator. */
    public void pressEnter(String page, String locator) {
        findVisibleElement(page, locator).sendKeys(Keys.ENTER);
    }

    /** Get the current browser URL; ensures web context readiness first. */
    public String getCurrentUrl() { ensureBrowserWebContextReadyIfNeeded(); return driver.getCurrentUrl(); }

    /** Get the current browser title; ensures web context readiness first. */
    public String getTitle() { ensureBrowserWebContextReadyIfNeeded(); return driver.getTitle(); }

    /**
     * Save the current browser page source to a local file.
     *
     * @param outputPath filesystem path to write to (directories will be created if needed)
     */
    public void savePageSource(String outputPath) {
        try {
            ensureBrowserWebContextReadyIfNeeded();
            Path output = Path.of(outputPath);
            Files.createDirectories(output.getParent() != null ? output.getParent() : Path.of("."));
            Files.writeString(output, driver.getPageSource());
        } catch (Exception e) {
            throw new RuntimeException("Unable to save mobile browser page source to: " + outputPath, e);
        }
    }

    /**
     * Synchronizes Appium real mobile browser sessions with a usable web context.
     *
     * <p>iOS Safari can be visually open while Appium is still attached to the native Safari shell.
     * In that state, WebKit can return a Runtime-domain error object instead of a normal Selenium
     * WebElement. This method waits for a WEBVIEW context and switches to it before browser DOM
     * actions such as searching for Google's input field.</p>
     */
    private void ensureBrowserWebContextReadyIfNeeded() {
        if (!MobileDriverManager.isBrowserSession()) return;
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (platform == null || !platform.isIos()) return;

        int timeoutSeconds = parsePositiveInt(MobileConfigurationProperties.getBrowserCapability(platform, "web_context_timeout_seconds", "20"), 20);
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        Throwable lastFailure = null;

        while (System.currentTimeMillis() <= deadline) {
            try {
                Set<String> contexts = getContexts();
                System.out.println("PTAF APPIUM REAL BROWSER CONTEXT | Available contexts: " + contexts);
                String currentContext = getCurrentContextSafely();
                if (currentContext != null && currentContext.toUpperCase(Locale.ROOT).contains("WEBVIEW")) {
                    System.out.println("PTAF APPIUM REAL BROWSER CONTEXT | Already using context: " + currentContext);
                    return;
                }
                for (String context : contexts) {
                    if (context != null && context.toUpperCase(Locale.ROOT).contains("WEBVIEW")) {
                        switchContext(context);
                        System.out.println("PTAF APPIUM REAL BROWSER CONTEXT | Switched to context: " + context);
                        pause(1);
                        return;
                    }
                }
            } catch (Throwable t) {
                // Remember the last failure to include in logs if we time out.
                lastFailure = t;
            }
            pause(1);
        }

        String message = "PTAF Appium iOS Safari browser context was not ready. Safari may be open, but Appium did not expose a WEBVIEW context within "
                + timeoutSeconds + " seconds. Clear Safari/simulator state, verify Safari/WebKit automation, and confirm the simulator is not showing first-run screens.";
        if (lastFailure != null) {
            message += " Last context error: " + lastFailure.getMessage();
        }
        System.out.println("PTAF APPIUM REAL BROWSER CONTEXT WARNING | " + message);
    }

    /**
     * Check whether the test configuration enabled native Safari address-bar fallback for navigation.
     *
     * This returns true only for iOS browser sessions and when the configuration key
     * "safari_native_navigation_fallback_enabled" is set to true.
     *
     * @return true when fallback navigation is allowed, false otherwise
     */
    private boolean shouldUseSafariNativeNavigationFallback() {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (!MobileDriverManager.isBrowserSession() || platform == null || !platform.isIos()) return false;
        return Boolean.parseBoolean(MobileConfigurationProperties.getBrowserCapability(platform, "safari_native_navigation_fallback_enabled", "true"));
    }

    /**
     * Heuristic to determine whether a loaded URL string represents a successful page
     * load for the provided target URL (by comparing hosts and ignoring WebKit runtime errors).
     *
     * @param currentUrl URL reported by the driver (may be null/unreadable)
     * @param targetUrl original requested URL
     * @return true if the currentUrl likely represents the expected host/page
     */
    private boolean isExpectedUrlLoaded(String currentUrl, String targetUrl) {
        if (currentUrl == null || currentUrl.isBlank()) return false;
        String current = currentUrl.toLowerCase(Locale.ROOT);
        if (current.contains("runtime") || current.contains("unable to read") || current.contains("error=")) return false;
        String expectedHost = targetUrl.replace("https://", "").replace("http://", "").split("/")[0].toLowerCase(Locale.ROOT);
        return current.contains(expectedHost);
    }

    /**
     * Native Safari fallback used only for iOS real mobile browser automation.
     *
     * <p>Some iOS simulator/Safari combinations open Safari's Start Page but ignore
     * the first WebDriver URL navigation request. In that state the Google DOM does
     * not exist yet, so this method temporarily switches to NATIVE_APP, taps Safari's
     * address/search field, types the URL, presses Return, and lets the caller switch
     * back to WEBVIEW before DOM validation.</p>
     *
     * @param targetUrl URL to type into the native Safari address bar
     */
    private void navigateSafariFromNativeAddressBar(String targetUrl) {
        // Switch to native so we can interact with Safari's UI elements directly.
        switchToNativeContext();
        pause(1);
        // Some Safari Start Pages show a close overlay; attempt to remove it if present.
        tapSafariStartPageCloseIfPresent();

        WebElement addressBar = findSafariAddressBar();
        if (addressBar != null) {
            addressBar.click();
        } else {
            // If no recognized address bar control exists, tap near the bottom of the screen
            // where Safari's address/search control is likely to accept input.
            Dimension size = driver.manage().window().getSize();
            int x = size.width / 2;
            int y = Math.max(1, (int) (size.height * 0.93));
            System.out.println("PTAF APPIUM REAL BROWSER | Safari address bar locator not found. Tapping fallback coordinate: " + x + "," + y);
            tapAt(x, y);
        }
        pause(1);

        // Try sending keys to the active element. Some simulator states require a second attempt.
        try {
            WebElement active = driver.switchTo().activeElement();
            active.sendKeys(targetUrl);
            active.sendKeys(Keys.ENTER);
        } catch (Exception firstFailure) {
            try {
                WebElement addressBarRetry = findSafariAddressBar();
                if (addressBarRetry == null) throw firstFailure;
                addressBarRetry.sendKeys(targetUrl);
                addressBarRetry.sendKeys(Keys.ENTER);
            } catch (Exception secondFailure) {
                throw new RuntimeException("PTAF could not type URL into Safari native address/search field. Check iOS simulator Safari Start Page UI and safariBrowser.addressBar locator.", secondFailure);
            }
        }
        // Allow the native navigation some time to complete before switching back to WEBVIEW context.
        pause(4);
    }

    /**
     * If Safari Start Page overlay close button is configured and visible, click it.
     * This is defensive: the configuration may not exist or the button may not be visible.
     */
    private void tapSafariStartPageCloseIfPresent() {
        try {
            Object raw = com.ptaf.mobile.config.MobileYamlReader.get("mobile_elements.safariBrowser.startPageCloseButton.ios");
            if (raw == null) return;
            By closeLocator = locatorHandler.getLocatorForType(String.valueOf(raw));
            List<WebElement> closeButtons = driver.findElements(closeLocator);
            if (!closeButtons.isEmpty() && closeButtons.get(0).isDisplayed()) {
                System.out.println("PTAF APPIUM REAL BROWSER | Closing Safari Start Page overlay before URL fallback.");
                closeButtons.get(0).click();
                pause(1);
            }
        } catch (Exception ignored) {
            // The close button is optional; Safari may not show this overlay on every simulator.
        }
    }

    /**
     * Attempt to find Safari's native address/search field using configured locator(s)
     * or a set of reasonable XPath fallbacks. Returns the first visible match.
     *
     * @return visible address/search WebElement or null if none found
     */
    private WebElement findSafariAddressBar() {
        try {
            Object raw = com.ptaf.mobile.config.MobileYamlReader.get("mobile_elements.safariBrowser.addressBar.ios");
            if (raw != null) {
                By configured = locatorHandler.getLocatorForType(String.valueOf(raw));
                List<WebElement> configuredMatches = driver.findElements(configured);
                for (WebElement element : configuredMatches) {
                    if (element.isDisplayed()) return element;
                }
            }
        } catch (Exception ignored) { }

        // Fallback locators for various Safari versions/OS X accessibility attributes.
        List<By> fallbackLocators = List.of(
                By.xpath("//*[@name='URL']"),
                By.xpath("//*[contains(@value,'Search or enter website')]"),
                By.xpath("//*[contains(@label,'Search') or contains(@name,'Search')]"),
                By.xpath("//XCUIElementTypeTextField"),
                By.xpath("//XCUIElementTypeSearchField")
        );
        for (By locator : fallbackLocators) {
            try {
                List<WebElement> matches = driver.findElements(locator);
                for (WebElement element : matches) {
                    if (element.isDisplayed()) return element;
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    /**
     * Retrieve the driver's current context using reflection, returning null on failure.
     * This is used for diagnostics without throwing exceptions.
     */
    private String getCurrentContextSafely() {
        try {
            Object current = driver.getClass().getMethod("getContext").invoke(driver);
            return current == null ? null : String.valueOf(current);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Safely get the current URL for diagnostic logging. If the driver cannot provide the URL,
     * return a readable placeholder with the exception message.
     */
    private String safeCurrentUrlForDiagnostics() {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "<unable to read current URL: " + e.getMessage() + ">";
        }
    }

    /**
     * Heuristic identifying common error messages that indicate Safari/WebKit runtime
     * context problems where WebElements cannot be read in a browser session.
     */
    private boolean looksLikeSafariRuntimeContextFailure(Throwable throwable) {
        String message = String.valueOf(throwable == null ? "" : throwable.getMessage());
        return MobileDriverManager.isBrowserSession()
                && MobileDriverManager.getPlatform() != null
                && MobileDriverManager.getPlatform().isIos()
                && (message.contains("LinkedHashMap") || message.contains("Runtime") || message.contains("WEBVIEW") || message.contains("WebElement"));
    }

    /**
     * Build a RuntimeException containing helpful troubleshooting instructions when
     * an iOS Safari web context failure prevents locating web elements.
     */
    private RuntimeException browserContextDiagnosticFailure(String page, String locator, Throwable cause) {
        return new RuntimeException("PTAF Appium iOS Safari could not locate mobile browser element [" + page + "." + locator + "] because Safari web context was not ready or WebKit returned a Runtime-domain error. "
                + "Recommended checks: erase/restart the simulator, close first-run Safari popups, verify mobile-browser-config.yml has include_safari_in_webviews=true, and rerun with a clean Appium server. Root cause: "
                + cause.getMessage(), cause);
    }

    /**
     * Parse an integer string and return a positive integer or the provided default.
     *
     * This is defensive for configuration values that may be missing or malformed.
     */
    private int parsePositiveInt(String raw, int defaultValue) {
        try {
            int value = Integer.parseInt(String.valueOf(raw).trim());
            return value > 0 ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Take a screenshot and return it as a File object. */
    public File takeScreenshot() { return driver.getScreenshotAs(OutputType.FILE); }

    /* Convenience swipe wrappers that compute coordinates as percentages of screen. */
    public void swipeUp() { swipe(0.5, 0.8, 0.5, 0.2); }
    public void swipeDown() { swipe(0.5, 0.2, 0.5, 0.8); }
    public void swipeLeft() { swipe(0.8, 0.5, 0.2, 0.5); }
    public void swipeRight() { swipe(0.2, 0.5, 0.8, 0.5); }
    public void pinchIn() { pinchOrZoom(0.70, 0.30); }
    public void zoomOut() { pinchOrZoom(0.30, 0.70); }

    /**
     * Find a visible element using configured mobile locators and a smart two-phase explicit wait.
     *
     * <p><strong>How the wait works:</strong></p>
     * <ul>
     *   <li>Phase 1 — Presence: waits until the element exists in the UI hierarchy (handles slow
     *       screen transitions and heavy loading states where the element is not yet rendered).</li>
     *   <li>Phase 2 — Visibility: waits until the element is also visible on screen (non-zero size,
     *       not hidden). The action is executed immediately once visibility is confirmed.</li>
     * </ul>
     *
     * <p>The total wait timeout is controlled by {@code explicit_wait_seconds} in
     * {@code mobile-config.yml} (default: 30 seconds, recommended max: 60 seconds).
     * The wait skips immediately when the element becomes visible — it never waits the full
     * timeout unless the element never appears.</p>
     *
     * <p>This method also ensures the browser web context is ready before attempting
     * to locate elements in real browser sessions, and performs Safari-specific
     * diagnostic handling when WebKit returns runtime errors.</p>
     *
     * @param page    page key in the YAML locator configuration (e.g. "LoginPage")
     * @param locator locator key under the page (e.g. "loginButton")
     * @return visible WebElement ready for interaction
     */
    public WebElement findVisibleElement(String page, String locator) {
        ensureBrowserWebContextReadyIfNeeded();
        int timeoutSeconds = MobileConfigurationProperties.getExplicitWaitSeconds();
        By by = resolveLocator(page, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            // Phase 1: Wait until the element is present in the UI hierarchy.
            wait.until(ExpectedConditions.presenceOfElementLocated(by));
            // Phase 2: Wait until the element is also visible (rendered, non-zero size, not hidden).
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (ClassCastException e) {
            // ClassCastException is a session/context failure — not retried in soft assertion mode.
            throw browserContextDiagnosticFailure(page, locator, e);
        } catch (RuntimeException e) {
            if (looksLikeSafariRuntimeContextFailure(e)) {
                // Safari context failure — not retried in soft assertion mode.
                throw browserContextDiagnosticFailure(page, locator, e);
            }
            // For element-not-found and timeout failures, apply soft assertion handling if enabled.
            if (ConfigurationProperties.isSoftAssertionsEnabled()) {
                handleMobileSoftFailure(page, locator, e);
                // Return null — callers that receive null will skip the action gracefully.
                // This is safe because all callers (tap, type, etc.) call findVisibleElement
                // and the null return prevents the NPE by being caught in the calling method.
                return null;
            }
            throw e;
        }
    }

    /**
     * Handles an element-not-found failure in soft assertion mode for mobile automation.
     *
     * <p>Captures a screenshot using {@link MobileEvidenceManager}, records the failure
     * in {@link SoftAssertionContext}, and returns normally so execution continues to the
     * next step. The Appium driver session is NOT terminated.</p>
     *
     * <p>This method is ONLY called when {@code soft_assertions.enabled: true}.
     * Session crashes and Safari context failures are NOT handled here — they still throw.</p>
     *
     * @param page    the page key from the YAML locator file
     * @param locator the locator key from the YAML locator file
     * @param cause   the original exception
     */
    private void handleMobileSoftFailure(String page, String locator, RuntimeException cause) {
        String stepDesc = "findElement on [" + page + "." + locator + "]";
        logger.warn("PTAF Soft Assert (Mobile) | Element not found: [{}]. Capturing screenshot and continuing.", stepDesc);

        // Capture a screenshot at the point of failure
        String screenshotNote = null;
        try {
            MobileEvidenceManager.captureNamedScreenshot(
                driver,
                "SoftFail_" + page + "_" + locator
            );
            screenshotNote = "captured (see mobile evidence)";
        } catch (Exception screenshotEx) {
            logger.warn("PTAF Soft Assert (Mobile) | Could not capture failure screenshot: {}", screenshotEx.getMessage());
        }

        // Record the failure — scenario will fail at the end with a full summary
        SoftAssertionContext.recordFailure(
            stepDesc,
            cause != null ? cause.getMessage() : "(no error message)",
            screenshotNote
        );
        logger.warn("PTAF Soft Assert (Mobile) | Continuing to next step. Scenario will fail at end if failures remain.");
    }

    /**
     * Resolve a textual locator key into a Selenium By object.
     *
     * Locator resolution rules:
     * - First look for mobile_elements.<page>.<locator> in the mobile YAML configuration.
     * - If running in Appium browser mode and no mobile entry exists, optionally fall back
     *   to the shared web elements stored under elements.<page>.<locator>.
     * - Support platform-specific locator values encoded as maps with keys like "android", "ios", "default", "mobileBrowser", etc.
     *
     * @param page page key
     * @param locator locator key
     * @return By locator appropriate for the current platform/mode
     * @throws IllegalArgumentException when no locator can be resolved or resolution fails
     */
    @SuppressWarnings("unchecked")
    public By resolveLocator(String page, String locator) {
        String mobileKey = "mobile_elements." + page + "." + locator;
        String sharedWebKey = "elements." + page + "." + locator;
        Object raw = com.ptaf.mobile.config.MobileYamlReader.get(mobileKey);
        String sourceKey = mobileKey;

        // Enterprise browser-mode reuse: if an Appium real browser test does not have
        // a mobile_elements entry, allow it to reuse the existing Playwright UI
        // elements storage. Native app automation intentionally does not use this
        // fallback because native apps require platform-specific accessibility/resource ids.
        if (raw == null && MobileDriverManager.isBrowserSession()) {
            try {
                raw = com.ptaf.utils.YamlReader.get(sharedWebKey);
                sourceKey = sharedWebKey;
            } catch (Exception ignored) { }
        }

        if (raw == null) {
            throw new IllegalArgumentException(buildLocatorResolutionFailure(
                    "MOBILE LOCATOR NOT FOUND",
                    page,
                    locator,
                    mobileKey,
                    sharedWebKey,
                    null,
                    "No locator value exists for the requested key. Add it under mobile_elements or, for Appium browser mode, under existing elements."));
        }

        String locatorValue = extractPlatformAwareLocatorValue(raw, sourceKey, page, locator, sharedWebKey);

        try {
            return locatorHandler.getLocatorForType(locatorValue);
        } catch (Exception e) {
            throw new IllegalArgumentException(buildLocatorResolutionFailure(
                    "MOBILE LOCATOR RESOLUTION FAILURE",
                    page,
                    locator,
                    sourceKey,
                    sharedWebKey,
                    locatorValue,
                    e.getMessage()), e);
        }
    }

    /**
     * When raw locator configuration is a map, pick the most appropriate value
     * based on the current platform or browser-mode keys. Falls back to default/common keys.
     *
     * @param raw raw configuration object (either a string or a map)
     * @param sourceKey the key used to fetch raw (used for error messages)
     * @param page page key (for diagnostics)
     * @param locator locator key (for diagnostics)
     * @param sharedWebKey fallback web key (for diagnostics)
     * @return chosen locator value as string
     */
    private String extractPlatformAwareLocatorValue(Object raw, String sourceKey, String page, String locator, String sharedWebKey) {
        if (!(raw instanceof Map<?, ?> rawMap)) return String.valueOf(raw);

        // Determine platform key to select within the map: e.g., "android", "ios", or default value.
        MobilePlatform platform = MobileDriverManager.getPlatform();
        String platformKey = platform == null
                ? MobileConfigurationProperties.getDefaultPlatform().name().toLowerCase(Locale.ROOT)
                : platform.name().toLowerCase(Locale.ROOT);

        Object platformValue = null;
        // For browser-mode tests the YAML may include "mobileBrowser"/"browser"/"web" keys.
        if (MobileDriverManager.isBrowserSession()) {
            platformValue = rawMap.get("mobileBrowser");
            if (platformValue == null) platformValue = rawMap.get("browser");
            if (platformValue == null) platformValue = rawMap.get("web");
        }
        if (platformValue == null) platformValue = rawMap.get(platformKey);
        if (platformValue == null) platformValue = rawMap.get("default");
        if (platformValue == null) platformValue = rawMap.get("common");

        if (platformValue == null) {
            throw new IllegalArgumentException(buildLocatorResolutionFailure(
                    "PLATFORM-SPECIFIC MOBILE LOCATOR MISSING",
                    page,
                    locator,
                    sourceKey,
                    sharedWebKey,
                    String.valueOf(raw),
                    "No value exists for platform/mode [" + platformKey + "] and available keys are " + rawMap.keySet()));
        }
        return String.valueOf(platformValue);
    }

    /**
     * Build a detailed multi-line message to help troubleshoot locator resolution failures.
     *
     * This message includes mode, platform, page, locator keys, raw values and guidance so testers
     * can update YAML configuration appropriately.
     */
    private String buildLocatorResolutionFailure(String title, String page, String locator, String sourceKey, String fallbackKey, String rawValue, String reason) {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        return "\n========== PTAF " + title + " ==========\n"
                + "Mode       : " + (MobileDriverManager.isBrowserSession() ? "APPIUM_REAL_MOBILE_BROWSER" : "NATIVE_MOBILE_APP") + "\n"
                + "Platform   : " + (platform == null ? "<not resolved>" : platform.name()) + "\n"
                + "Page       : " + page + "\n"
                + "Key        : " + locator + "\n"
                + "Primary    : " + sourceKey + "\n"
                + "Fallback   : " + fallbackKey + "\n"
                + "Raw Value  : " + (rawValue == null ? "<null>" : rawValue) + "\n"
                + "Reason     : " + reason + "\n"
                + "Examples   : ACCESSIBILITY_ID_loginButton, XPATH_//input, CSS_.search, Button_Login, TEXTBOX_Search, TEXT_Save\n"
                + "Guidance   : For native mobile prefer ACCESSIBILITY_ID_/ID_/IOS_PREDICATE_/ANDROID_UIAUTOMATOR_. For Appium browser you may reuse UI-style elements when the page is a real web page.\n"
                + "===============================================\n";
    }

    /**
     * Perform a swipe gesture based on normalized percentages of the screen.
     *
     * @param startXPct start x percentage (0..1)
     * @param startYPct start y percentage (0..1)
     * @param endXPct end x percentage (0..1)
     * @param endYPct end y percentage (0..1)
     */
    private void swipe(double startXPct, double startYPct, double endXPct, double endYPct) {
        Dimension size = driver.manage().window().getSize();
        dragFromTo((int)(size.width * startXPct), (int)(size.height * startYPct), (int)(size.width * endXPct), (int)(size.height * endYPct), 700);
    }

    /**
     * Low-level press-and-hold at a coordinate using W3C Actions.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param durationMillis how long to hold
     */
    private void pressAt(int x, int y, long durationMillis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(new Pause(finger, Duration.ofMillis(durationMillis)));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    /**
     * Low-level drag from start coordinate to end coordinate over a duration.
     *
     * @param startX start x coordinate
     * @param startY start y coordinate
     * @param endX end x coordinate
     * @param endY end y coordinate
     * @param durationMillis duration of the move in milliseconds
     */
    private void dragFromTo(int startX, int startY, int endX, int endY, long durationMillis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(durationMillis), PointerInput.Origin.viewport(), endX, endY));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    /**
     * Perform a pinch or zoom gesture using two fingers moving in opposite directions.
     *
     * startFactor/endFactor are percentages of the smaller screen dimension (0..1)
     * that determine how far apart the two fingers start and end.
     *
     * @param startFactor how far apart fingers start (e.g., 0.70)
     * @param endFactor how far apart fingers end (e.g., 0.30)
     */
    private void pinchOrZoom(double startFactor, double endFactor) {
        Dimension size = driver.manage().window().getSize();
        int centerX = size.width / 2;
        int centerY = size.height / 2;
        int startOffset = (int) (Math.min(size.width, size.height) * startFactor / 2);
        int endOffset = (int) (Math.min(size.width, size.height) * endFactor / 2);
        PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
        PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
        Sequence s1 = new Sequence(finger1, 1);
        Sequence s2 = new Sequence(finger2, 1);
        s1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX - startOffset, centerY));
        s2.addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX + startOffset, centerY));
        s1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        s2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        s1.addAction(finger1.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), centerX - endOffset, centerY));
        s2.addAction(finger2.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), centerX + endOffset, centerY));
        s1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        s2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(s1, s2));
    }

    /**
     * Normalize orientation string to values accepted by Appium (uppercase PORTRAIT/LANDSCAPE).
     *
     * @param orientation input orientation (case-insensitive)
     * @return normalized orientation string
     * @throws IllegalArgumentException if input is null, blank or unsupported
     */
    private String normalizeOrientation(String orientation) {
        if (orientation == null || orientation.trim().isEmpty()) throw new IllegalArgumentException("Orientation must be portrait or landscape.");
        String normalized = orientation.trim().toUpperCase();
        if (!"PORTRAIT".equals(normalized) && !"LANDSCAPE".equals(normalized)) throw new IllegalArgumentException("Unsupported orientation: " + orientation + ". Use portrait or landscape.");
        return normalized;
    }

    /** Basic helper to sanitize single quotes for XPath building. */
    private String escapeXPath(String value) { return value.replace("'", ""); }
}
