package com.ptaf.mobile.implementation;

import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.interfaces.MobileAction;
import com.ptaf.mobile.pages.MobileCommonMethods;

import java.util.Set;

/**
 * Default implementation of the MobileAction interface.
 *
 * <p>This class is a thin delegate that forwards calls to MobileCommonMethods instantiated
 * with the Appium driver returned by MobileDriverManager.getDriver(). It is intended to be used
 * by test code and automation frameworks to perform common mobile actions (taps, swipes,
 * type, clipboard operations, context switching, app lifecycle operations, etc.).
 *
 * <p>Notes for testers:
 * - All methods operate against the current thread's driver obtained from MobileDriverManager.
 * - The "page" and "locator" parameters typically correspond to keys used by the project's
 *   page/locator resolution strategy (e.g., page names and element identifiers).
 * - Most methods delegate directly to the underlying MobileCommonMethods which contain the
 *   real logic and interaction with the Appium driver. Exceptions and failures will surface
 *   from those underlying implementations.
 *
 * <p>Thread-safety: This class does not hold state and simply delegates per-invocation.
 * The thread-safety characteristics correspond to MobileDriverManager and MobileCommonMethods.
 */
public class MobileActionImpl implements MobileAction {
    /**
     * Create a new MobileCommonMethods instance bound to the current thread's driver.
     *
     * <p>This helper ensures every operation performed by this implementation uses the
     * Appium driver associated with the current test thread. The method is private to
     * keep the delegation centralized and concise.
     *
     * @return a MobileCommonMethods instance using the current thread's Appium driver
     */
    private MobileCommonMethods methods() { return new MobileCommonMethods(MobileDriverManager.getDriver()); }

    /**
     * Tap (single tap) on an element specified by page and locator.
     *
     * @param page    the page identifier (framework-specific) where the element is defined
     * @param locator the locator identifier (framework-specific) for the element to tap
     */
    public void tap(String page, String locator) { 
        // Delegate tap action to MobileCommonMethods bound to the current driver.
        methods().tap(page, locator); 
    }

    /**
     * Type text into an input element identified by page and locator.
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the input element
     * @param value   the text value to type into the element
     */
    public void type(String page, String locator, String value) { 
        // Delegate typing to the underlying helper which handles element focus and input.
        methods().type(page, locator, value); 
    }

    /**
     * Clear the contents of an input element.
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the input element to clear
     */
    public void clear(String page, String locator) { 
        // Delegate clear action.
        methods().clear(page, locator); 
    }

    /**
     * Retrieve the visible text of an element.
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element
     * @return the element's visible text (may be empty string if element contains no text)
     */
    public String getText(String page, String locator) { 
        // Delegate and return the retrieved text.
        return methods().getText(page, locator); 
    }

    /**
     * Wait until the specified element is visible using default timeout from configuration.
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element
     */
    public void waitForVisible(String page, String locator) { 
        // Delegate waiting logic to MobileCommonMethods.
        methods().waitForVisible(page, locator); 
    }

    /**
     * Wait until the specified element is visible with a custom timeout.
     *
     * @param page           the page identifier containing the element
     * @param locator        the locator identifier for the element
     * @param timeoutSeconds custom timeout in seconds to wait for visibility
     */
    public void waitForVisible(String page, String locator, int timeoutSeconds) { 
        // Delegate waiting with explicit timeout.
        methods().waitForVisible(page, locator, timeoutSeconds); 
    }

    /**
     * Wait until the specified element is not visible (hidden or removed) within the timeout.
     *
     * @param page           the page identifier containing the element
     * @param locator        the locator identifier for the element
     * @param timeoutSeconds maximum seconds to wait for the element to become not visible
     */
    public void waitForNotVisible(String page, String locator, int timeoutSeconds) { 
        // Delegate logic that waits for invisibility.
        methods().waitForNotVisible(page, locator, timeoutSeconds); 
    }

    /**
     * Pause execution for a given number of seconds.
     *
     * @param seconds the number of seconds to pause execution; used sparingly in tests
     */
    public void pause(int seconds) { 
        // Delegate to a controlled sleep/pause method.
        methods().pause(seconds); 
    }

    /**
     * Check whether an element is currently visible.
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element
     * @return true if element is visible; false otherwise
     */
    public boolean isVisible(String page, String locator) { 
        // Delegate visibility check and return boolean result.
        return methods().isVisible(page, locator); 
    }

    /**
     * Check whether an element is enabled (interactable).
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element
     * @return true if element is enabled; false otherwise
     */
    public boolean isEnabled(String page, String locator) { 
        // Delegate enabled state check.
        return methods().isEnabled(page, locator); 
    }

    /**
     * Check whether an element is selected (for selectable elements like checkboxes).
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element
     * @return true if element is selected; false otherwise
     */
    public boolean isSelected(String page, String locator) { 
        // Delegate selection state check.
        return methods().isSelected(page, locator); 
    }

    /**
     * Perform a long-press gesture on an element for a specified duration.
     *
     * @param page           the page identifier containing the element
     * @param locator        the locator identifier for the element
     * @param durationMillis duration of the long press in milliseconds
     */
    public void longPress(String page, String locator, long durationMillis) { 
        // Delegate long press gesture handling.
        methods().longPress(page, locator, durationMillis); 
    }

    /**
     * Perform a double-tap gesture on an element.
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element
     */
    public void doubleTap(String page, String locator) { 
        // Delegate double tap gesture.
        methods().doubleTap(page, locator); 
    }

    /**
     * Tap at absolute screen coordinates.
     *
     * @param x the x coordinate (pixels or driver-specific units)
     * @param y the y coordinate (pixels or driver-specific units)
     */
    public void tapAt(int x, int y) { 
        // Delegate coordinate-based tap.
        methods().tapAt(x, y); 
    }

    /**
     * Drag an element from one locator to another (within possibly different pages).
     *
     * @param fromPage    the source page identifier
     * @param fromLocator the source element locator identifier
     * @param toPage      the destination page identifier
     * @param toLocator   the destination element locator identifier
     */
    public void drag(String fromPage, String fromLocator, String toPage, String toLocator) { 
        // Delegate drag-and-drop action.
        methods().drag(fromPage, fromLocator, toPage, toLocator); 
    }

    /**
     * Scroll repeatedly until a specific element becomes visible or maxSwipes is exhausted.
     *
     * @param page      the page identifier to perform the scroll on
     * @param locator   the locator identifier for the element to reveal
     * @param maxSwipes maximum number of scroll/swipe attempts before giving up
     */
    public void scrollUntilVisible(String page, String locator, int maxSwipes) { 
        // Delegate scroll-until-visible behavior.
        methods().scrollUntilVisible(page, locator, maxSwipes); 
    }

    /**
     * Scroll the view until the specified text is visible.
     *
     * @param text the target text to find on screen while scrolling
     */
    public void scrollToText(String text) { 
        // Delegate text-based scrolling.
        methods().scrollToText(text); 
    }

    /**
     * Hide the on-screen keyboard if it is present.
     */
    public void hideKeyboard() { 
        // Delegate keyboard hiding logic.
        methods().hideKeyboard(); 
    }

    /**
     * Send the application to background for a specified number of seconds.
     *
     * @param seconds how long to background the app before bringing it back
     */
    public void backgroundApp(int seconds) { 
        // Delegate to background the application via the driver.
        methods().backgroundApp(seconds); 
    }

    /**
     * Perform an upward swipe gesture (commonly used for scrolling down content).
     */
    public void swipeUp() { 
        // Delegate swipe up.
        methods().swipeUp(); 
    }

    /**
     * Perform a downward swipe gesture (commonly used for scrolling up content).
     */
    public void swipeDown() { 
        // Delegate swipe down.
        methods().swipeDown(); 
    }

    /**
     * Perform a leftward swipe gesture.
     */
    public void swipeLeft() { 
        // Delegate swipe left.
        methods().swipeLeft(); 
    }

    /**
     * Perform a rightward swipe gesture.
     */
    public void swipeRight() { 
        // Delegate swipe right.
        methods().swipeRight(); 
    }

    /**
     * Perform a pinch-in gesture (zoom out).
     */
    public void pinchIn() { 
        // Delegate pinch-in gesture.
        methods().pinchIn(); 
    }

    /**
     * Perform a zoom-out gesture (pinch-out).
     */
    public void zoomOut() { 
        // Delegate zoom-out gesture.
        methods().zoomOut(); 
    }

    /**
     * Set the device orientation. The expected values are driver/platform dependent,
     * e.g., "PORTRAIT" or "LANDSCAPE".
     *
     * @param orientation orientation string understood by the underlying driver
     */
    public void setOrientation(String orientation) { 
        // Delegate orientation change request.
        methods().setOrientation(orientation); 
    }

    /**
     * Set device orientation based on the test framework's configured default.
     *
     * <p>This uses the configuration defined for the project rather than requiring callers
     * to supply an orientation explicitly.
     */
    public void setConfiguredOrientation() { 
        // Delegate to configured orientation handler.
        methods().setConfiguredOrientation(); 
    }

    /**
     * Activate (bring to foreground) the application identified by the given appId.
     *
     * @param appId application package name (Android) or bundle id (iOS)
     */
    public void activateApp(String appId) { 
        // Delegate app activation.
        methods().activateApp(appId); 
    }

    /**
     * Terminate the application identified by the given appId.
     *
     * @param appId application package name (Android) or bundle id (iOS)
     */
    public void terminateApp(String appId) { 
        // Delegate app termination.
        methods().terminateApp(appId); 
    }

    /**
     * Open a deep link URL for the specified application.
     *
     * @param url                  the deep link URL to open
     * @param appPackageOrBundleId the application package (Android) or bundle id (iOS) to route the link to
     */
    public void openDeepLink(String url, String appPackageOrBundleId) { 
        // Delegate deep link opening.
        methods().openDeepLink(url, appPackageOrBundleId); 
    }

    /**
     * Push a local file to the device/emulator at the specified remote path.
     *
     * @param remotePath  destination path on device/emulator
     * @param localPath   source path on the local machine
     */
    public void pushFile(String remotePath, String localPath) { 
        // Delegate file push operation.
        methods().pushFile(remotePath, localPath); 
    }

    /**
     * Pull a file from the device/emulator to the local output path.
     *
     * @param remotePath       path on the device/emulator to pull from
     * @param localOutputPath  local filesystem path to save the pulled file
     */
    public void pullFile(String remotePath, String localOutputPath) { 
        // Delegate file pull operation.
        methods().pullFile(remotePath, localOutputPath); 
    }

    /**
     * Set device clipboard content to the provided text.
     *
     * @param text the text to set in the device clipboard
     */
    public void setClipboard(String text) { 
        // Delegate to clipboard setter.
        methods().setClipboard(text); 
    }

    /**
     * Retrieve the current content of the device clipboard as text.
     *
     * @return clipboard content as a String (may be null or empty depending on platform)
     */
    public String getClipboard() { 
        // Delegate to clipboard getter and return the result.
        return methods().getClipboard(); 
    }

    /**
     * Get the set of available contexts (e.g., NATIVE_APP, WEBVIEW_*).
     *
     * @return a set of context names available in the current session
     */
    public Set<String> getContexts() { 
        // Delegate context retrieval.
        return methods().getContexts(); 
    }

    /**
     * Switch to a specific context by name (useful for webview/native transitions).
     *
     * @param contextName the name of the context to switch to (case-sensitive)
     */
    public void switchContext(String contextName) { 
        // Delegate context switching.
        methods().switchContext(contextName); 
    }

    /**
     * Convenience method to switch back to the native application context.
     */
    public void switchToNativeContext() { 
        // Delegate switching to native context.
        methods().switchToNativeContext(); 
    }

    /**
     * Grant a runtime permission to the specified application (platform-dependent).
     *
     * @param appId      application package name (Android) or bundle id (iOS)
     * @param permission permission string understood by the platform/driver
     */
    public void grantPermission(String appId, String permission) { 
        // Delegate permission granting to underlying implementation.
        methods().grantPermission(appId, permission); 
    }

    /**
     * Revoke a runtime permission from the specified application.
     *
     * @param appId      application package name (Android) or bundle id (iOS)
     * @param permission permission string understood by the platform/driver
     */
    public void revokePermission(String appId, String permission) { 
        // Delegate permission revocation.
        methods().revokePermission(appId, permission); 
    }

    /**
     * Open a URL in the device's default browser or in a webview, depending on configuration.
     *
     * @param url the URL to open
     */
    public void openUrl(String url) { 
        // Delegate URL opening.
        methods().openUrl(url); 
    }

    /**
     * Simulate pressing the Enter key on a specific element (useful to submit forms).
     *
     * @param page    the page identifier containing the element
     * @param locator the locator identifier for the element to send the Enter key to
     */
    public void pressEnter(String page, String locator) { 
        // Delegate Enter key press.
        methods().pressEnter(page, locator); 
    }

    /**
     * Get the current URL from a webview context.
     *
     * @return the current URL as a String (or an empty/null value if not applicable)
     */
    public String getCurrentUrl() { 
        // Delegate to underlying method to retrieve current URL.
        return methods().getCurrentUrl(); 
    }

    /**
     * Get the current page title from a webview context.
     *
     * @return the page title as a String (or an empty/null value if not applicable)
     */
    public String getTitle() { 
        // Delegate to underlying method to retrieve page title.
        return methods().getTitle(); 
    }

    /**
     * Save the current page source to a local file for debugging or analysis.
     *
     * @param outputPath the local file path where the page source should be saved
     */
    public void savePageSource(String outputPath) { 
        // Delegate page source saving.
        methods().savePageSource(outputPath); 
    }
}
