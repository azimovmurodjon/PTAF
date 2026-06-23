package com.ptaf.mobile.pages;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.handlers.MobileLocatorHandler;
import io.appium.java_client.AppiumDriver;
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
    private final AppiumDriver driver;
    private final MobileLocatorHandler locatorHandler = new MobileLocatorHandler();

    public MobileCommonMethods(AppiumDriver driver) {
        if (driver == null) throw new IllegalArgumentException("Appium driver cannot be null.");
        this.driver = driver;
    }

    public void tap(String page, String locator) { findVisibleElement(page, locator).click(); }
    public void type(String page, String locator, String value) { WebElement e = findVisibleElement(page, locator); e.clear(); e.sendKeys(value == null ? "" : value); }
    public void clear(String page, String locator) { findVisibleElement(page, locator).clear(); }
    public String getText(String page, String locator) { return findVisibleElement(page, locator).getText(); }
    public boolean isVisible(String page, String locator) { try { return findVisibleElement(page, locator).isDisplayed(); } catch (Exception e) { return false; } }
    public boolean isEnabled(String page, String locator) { return findVisibleElement(page, locator).isEnabled(); }
    public boolean isSelected(String page, String locator) { return findVisibleElement(page, locator).isSelected(); }
    public void waitForVisible(String page, String locator) { findVisibleElement(page, locator); }

    /** Waits for a locator using an explicit timeout supplied from the feature file. */
    public void waitForVisible(String page, String locator, int timeoutSeconds) {
        ensureBrowserWebContextReadyIfNeeded();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(Math.max(timeoutSeconds, 0)));
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(resolveLocator(page, locator)));
        } catch (ClassCastException e) {
            throw browserContextDiagnosticFailure(page, locator, e);
        } catch (RuntimeException e) {
            if (looksLikeSafariRuntimeContextFailure(e)) {
                throw browserContextDiagnosticFailure(page, locator, e);
            }
            throw e;
        }
    }

    /** Waits until a locator disappears or becomes invisible. Useful for loaders, splash screens, and transient dialogs. */
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

    /** Pauses intentionally for rare application transitions. Prefer explicit waits whenever a stable locator is available. */
    public void pause(int seconds) {
        try { Thread.sleep(Math.max(seconds, 0) * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void longPress(String page, String locator, long durationMillis) {
        WebElement element = findVisibleElement(page, locator);
        int x = element.getRect().getX() + element.getRect().getWidth() / 2;
        int y = element.getRect().getY() + element.getRect().getHeight() / 2;
        pressAt(x, y, Math.max(durationMillis, 500));
    }

    public void doubleTap(String page, String locator) {
        WebElement element = findVisibleElement(page, locator);
        int x = element.getRect().getX() + element.getRect().getWidth() / 2;
        int y = element.getRect().getY() + element.getRect().getHeight() / 2;
        tapAt(x, y);
        tapAt(x, y);
    }

    public void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    public void drag(String fromPage, String fromLocator, String toPage, String toLocator) {
        WebElement from = findVisibleElement(fromPage, fromLocator);
        WebElement to = findVisibleElement(toPage, toLocator);
        int startX = from.getRect().getX() + from.getRect().getWidth() / 2;
        int startY = from.getRect().getY() + from.getRect().getHeight() / 2;
        int endX = to.getRect().getX() + to.getRect().getWidth() / 2;
        int endY = to.getRect().getY() + to.getRect().getHeight() / 2;
        dragFromTo(startX, startY, endX, endY, 800);
    }

    public void scrollUntilVisible(String page, String locator, int maxSwipes) {
        int attempts = Math.max(maxSwipes, 1);
        for (int i = 0; i < attempts; i++) {
            if (isVisible(page, locator)) return;
            swipeUp();
        }
        findVisibleElement(page, locator);
    }

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

    public void hideKeyboard() { try { if (driver instanceof HidesKeyboard) ((HidesKeyboard) driver).hideKeyboard(); } catch (Exception ignored) { } }

    public void backgroundApp(int seconds) {
        driver.executeScript("mobile: backgroundApp", Map.of("seconds", Math.max(seconds, 1)));
    }

    public void activateApp(String appId) {
        if (driver instanceof InteractsWithApps) { ((InteractsWithApps) driver).activateApp(appId); return; }
        throw new UnsupportedOperationException("Current Appium driver does not support activateApp.");
    }

    public void terminateApp(String appId) {
        if (driver instanceof InteractsWithApps) { ((InteractsWithApps) driver).terminateApp(appId); return; }
        throw new UnsupportedOperationException("Current Appium driver does not support terminateApp.");
    }

    public void openDeepLink(String url, String appPackageOrBundleId) {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (platform != null && platform.isAndroid()) {
            driver.executeScript("mobile: deepLink", Map.of("url", url, "package", appPackageOrBundleId));
        } else {
            driver.executeScript("mobile: launchApp", Map.of("bundleId", appPackageOrBundleId, "arguments", List.of(url)));
        }
    }

    public void pushFile(String remotePath, String localPath) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(localPath));
            String encoded = Base64.getEncoder().encodeToString(bytes);
            driver.executeScript("mobile: pushFile", Map.of("remotePath", remotePath, "payload", encoded));
        } catch (Exception e) {
            throw new RuntimeException("Unable to push file to mobile device: " + localPath + " -> " + remotePath, e);
        }
    }

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

    public void setClipboard(String text) {
        driver.executeScript("mobile: setClipboard", Map.of("content", Base64.getEncoder().encodeToString((text == null ? "" : text).getBytes()), "contentType", "plaintext"));
    }

    public String getClipboard() {
        Object response = driver.executeScript("mobile: getClipboard", Map.of("contentType", "plaintext"));
        return response == null ? "" : new String(Base64.getDecoder().decode(String.valueOf(response)));
    }

    @SuppressWarnings("unchecked")
    public Set<String> getContexts() {
        try {
            return (Set<String>) driver.getClass().getMethod("getContextHandles").invoke(driver);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Current Appium driver does not support context switching.", e);
        }
    }
    public void switchContext(String contextName) {
        try {
            driver.getClass().getMethod("context", String.class).invoke(driver, contextName);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Current Appium driver does not support context switching.", e);
        }
    }
    public void switchToNativeContext() { switchContext("NATIVE_APP"); }

    public void grantPermission(String appId, String permission) {
        driver.executeScript("mobile: changePermissions", Map.of("appPackage", appId, "permissions", permission, "action", "grant"));
    }

    public void revokePermission(String appId, String permission) {
        driver.executeScript("mobile: changePermissions", Map.of("appPackage", appId, "permissions", permission, "action", "revoke"));
    }

    public void setOrientation(String orientation) {
        String normalized = normalizeOrientation(orientation);
        driver.executeScript("mobile: setDeviceOrientation", Map.of("orientation", normalized));
    }

    public void setConfiguredOrientation() {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (platform != null) setOrientation(MobileConfigurationProperties.getOrientation(platform));
    }

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

    public void pressEnter(String page, String locator) {
        findVisibleElement(page, locator).sendKeys(Keys.ENTER);
    }

    public String getCurrentUrl() { ensureBrowserWebContextReadyIfNeeded(); return driver.getCurrentUrl(); }
    public String getTitle() { ensureBrowserWebContextReadyIfNeeded(); return driver.getTitle(); }

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

    private boolean shouldUseSafariNativeNavigationFallback() {
        MobilePlatform platform = MobileDriverManager.getPlatform();
        if (!MobileDriverManager.isBrowserSession() || platform == null || !platform.isIos()) return false;
        return Boolean.parseBoolean(MobileConfigurationProperties.getBrowserCapability(platform, "safari_native_navigation_fallback_enabled", "true"));
    }

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
     */
    private void navigateSafariFromNativeAddressBar(String targetUrl) {
        switchToNativeContext();
        pause(1);
        tapSafariStartPageCloseIfPresent();

        WebElement addressBar = findSafariAddressBar();
        if (addressBar != null) {
            addressBar.click();
        } else {
            Dimension size = driver.manage().window().getSize();
            int x = size.width / 2;
            int y = Math.max(1, (int) (size.height * 0.93));
            System.out.println("PTAF APPIUM REAL BROWSER | Safari address bar locator not found. Tapping fallback coordinate: " + x + "," + y);
            tapAt(x, y);
        }
        pause(1);

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
        pause(4);
    }

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

    private String getCurrentContextSafely() {
        try {
            Object current = driver.getClass().getMethod("getContext").invoke(driver);
            return current == null ? null : String.valueOf(current);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safeCurrentUrlForDiagnostics() {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "<unable to read current URL: " + e.getMessage() + ">";
        }
    }

    private boolean looksLikeSafariRuntimeContextFailure(Throwable throwable) {
        String message = String.valueOf(throwable == null ? "" : throwable.getMessage());
        return MobileDriverManager.isBrowserSession()
                && MobileDriverManager.getPlatform() != null
                && MobileDriverManager.getPlatform().isIos()
                && (message.contains("LinkedHashMap") || message.contains("Runtime") || message.contains("WEBVIEW") || message.contains("WebElement"));
    }

    private RuntimeException browserContextDiagnosticFailure(String page, String locator, Throwable cause) {
        return new RuntimeException("PTAF Appium iOS Safari could not locate mobile browser element [" + page + "." + locator + "] because Safari web context was not ready or WebKit returned a Runtime-domain error. "
                + "Recommended checks: erase/restart the simulator, close first-run Safari popups, verify mobile-browser-config.yml has include_safari_in_webviews=true, and rerun with a clean Appium server. Root cause: "
                + cause.getMessage(), cause);
    }

    private int parsePositiveInt(String raw, int defaultValue) {
        try {
            int value = Integer.parseInt(String.valueOf(raw).trim());
            return value > 0 ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public File takeScreenshot() { return driver.getScreenshotAs(OutputType.FILE); }
    public void swipeUp() { swipe(0.5, 0.8, 0.5, 0.2); }
    public void swipeDown() { swipe(0.5, 0.2, 0.5, 0.8); }
    public void swipeLeft() { swipe(0.8, 0.5, 0.2, 0.5); }
    public void swipeRight() { swipe(0.2, 0.5, 0.8, 0.5); }
    public void pinchIn() { pinchOrZoom(0.70, 0.30); }
    public void zoomOut() { pinchOrZoom(0.30, 0.70); }

    public WebElement findVisibleElement(String page, String locator) {
        ensureBrowserWebContextReadyIfNeeded();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(MobileConfigurationProperties.getExplicitWaitSeconds()));
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(resolveLocator(page, locator)));
        } catch (ClassCastException e) {
            throw browserContextDiagnosticFailure(page, locator, e);
        } catch (RuntimeException e) {
            if (looksLikeSafariRuntimeContextFailure(e)) {
                throw browserContextDiagnosticFailure(page, locator, e);
            }
            throw e;
        }
    }

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

    private String extractPlatformAwareLocatorValue(Object raw, String sourceKey, String page, String locator, String sharedWebKey) {
        if (!(raw instanceof Map<?, ?> rawMap)) return String.valueOf(raw);

        MobilePlatform platform = MobileDriverManager.getPlatform();
        String platformKey = platform == null
                ? MobileConfigurationProperties.getDefaultPlatform().name().toLowerCase(Locale.ROOT)
                : platform.name().toLowerCase(Locale.ROOT);

        Object platformValue = null;
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

    private void swipe(double startXPct, double startYPct, double endXPct, double endYPct) {
        Dimension size = driver.manage().window().getSize();
        dragFromTo((int)(size.width * startXPct), (int)(size.height * startYPct), (int)(size.width * endXPct), (int)(size.height * endYPct), 700);
    }

    private void pressAt(int x, int y, long durationMillis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(new Pause(finger, Duration.ofMillis(durationMillis)));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    private void dragFromTo(int startX, int startY, int endX, int endY, long durationMillis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(durationMillis), PointerInput.Origin.viewport(), endX, endY));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

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

    private String normalizeOrientation(String orientation) {
        if (orientation == null || orientation.trim().isEmpty()) throw new IllegalArgumentException("Orientation must be portrait or landscape.");
        String normalized = orientation.trim().toUpperCase();
        if (!"PORTRAIT".equals(normalized) && !"LANDSCAPE".equals(normalized)) throw new IllegalArgumentException("Unsupported orientation: " + orientation + ". Use portrait or landscape.");
        return normalized;
    }

    private String escapeXPath(String value) { return value.replace("'", ""); }
}
