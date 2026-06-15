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

    public File takeScreenshot() { return driver.getScreenshotAs(OutputType.FILE); }
    public void swipeUp() { swipe(0.5, 0.8, 0.5, 0.2); }
    public void swipeDown() { swipe(0.5, 0.2, 0.5, 0.8); }
    public void swipeLeft() { swipe(0.8, 0.5, 0.2, 0.5); }
    public void swipeRight() { swipe(0.2, 0.5, 0.8, 0.5); }
    public void pinchIn() { pinchOrZoom(0.70, 0.30); }
    public void zoomOut() { pinchOrZoom(0.30, 0.70); }

    public WebElement findVisibleElement(String page, String locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(MobileConfigurationProperties.getExplicitWaitSeconds()));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(resolveLocator(page, locator)));
    }

    @SuppressWarnings("unchecked")
    public By resolveLocator(String page, String locator) {
        String key = "mobile_elements." + page + "." + locator;
        Object raw = com.ptaf.mobile.config.MobileYamlReader.get(key);
        if (raw == null) throw new IllegalArgumentException("Mobile locator not found in YAML for key: " + key);

        String locatorValue;
        if (raw instanceof Map<?, ?> rawMap) {
            MobilePlatform platform = MobileDriverManager.getPlatform();
            String platformKey = platform == null ? MobileConfigurationProperties.getDefaultPlatform().name().toLowerCase(Locale.ROOT) : platform.name().toLowerCase(Locale.ROOT);
            Object platformValue = rawMap.get(platformKey);
            if (platformValue == null) platformValue = rawMap.get("default");
            if (platformValue == null) platformValue = rawMap.get("common");
            if (platformValue == null) {
                throw new IllegalArgumentException("Mobile locator key [" + key + "] is platform-specific but has no value for platform [" + platformKey + "]. Available keys: " + rawMap.keySet());
            }
            locatorValue = String.valueOf(platformValue);
        } else {
            locatorValue = String.valueOf(raw);
        }

        try {
            return locatorHandler.getLocatorForType(locatorValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to resolve mobile locator [" + key + "] with value [" + locatorValue + "]", e);
        }
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
