package com.ptaf.mobile.interfaces;

import java.util.Set;

/**
 * MobileAction defines a collection of high-level, platform-agnostic actions that can be
 * performed against a mobile application under test. This interface abstracts typical
 * gestures, interactions, device-control operations, and utilities that test automation
 * frameworks need to drive mobile apps (both Android and iOS).
 *
 * <p>
 * Conventions used in method signatures:
 * - "page" typically refers to a logical page or screen identifier (for example a page object
 *   name or feature name) used by the framework to resolve locators.
 * - "locator" is a string key that identifies a UI element on the specified page. The concrete
 *   meaning (XPath, accessibility id, resource id, etc.) depends on the underlying framework.
 * </p>
 *
 * <p>
 * Implementations of this interface should map these abstract operations to concrete mobile
 * driver calls (for example Appium, UiAutomator, XCUITest). Methods should be resilient and
 * provide informative failures for testers when elements are not found or actions cannot be
 * completed.
 * </p>
 */
public interface MobileAction {

    /**
     * Perform a tap (single short press) on the element identified by the given page and locator.
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element to tap
     */
    void tap(String page, String locator);

    /**
     * Type text into a text field or input control.
     *
     * <p>
     * The implementation should handle focusing the control as needed and may clear
     * existing text before typing if required by the framework's conventions.
     * </p>
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the input element
     * @param value   text to be entered into the element
     */
    void type(String page, String locator, String value);

    /**
     * Clear the content of the input element identified by the locator.
     *
     * <p>
     * This is typically implemented by selecting existing text and removing it or using
     * a clear() call on the underlying element. Implementations should ensure the element
     * is visible and enabled before attempting to clear.
     * </p>
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the input element to clear
     */
    void clear(String page, String locator);

    /**
     * Retrieve the visible text content of an element.
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element whose text will be returned
     * @return the visible text of the element or an empty string if not available
     */
    String getText(String page, String locator);

    /**
     * Wait until the element is visible using a default framework timeout.
     *
     * <p>
     * Implementations should poll for visibility and return once the element is visible.
     * If the element does not become visible within the default timeout, an implementation-specific
     * runtime exception or assertion should be raised to indicate failure.
     * </p>
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element to wait for
     */
    void waitForVisible(String page, String locator);

    /**
     * Wait until the element is visible or the specified timeout elapses.
     *
     * @param page           logical page or screen identifier where the locator is defined
     * @param locator        key or selector for the element to wait for
     * @param timeoutSeconds maximum time to wait in seconds before failing
     */
    void waitForVisible(String page, String locator, int timeoutSeconds);

    /**
     * Wait until the element is not visible (gone/hidden) or the specified timeout elapses.
     *
     * <p>
     * Useful for verifying that overlays, loaders or transient elements disappear.
     * </p>
     *
     * @param page           logical page or screen identifier where the locator is defined
     * @param locator        key or selector for the element expected to disappear
     * @param timeoutSeconds maximum time to wait in seconds before failing
     */
    void waitForNotVisible(String page, String locator, int timeoutSeconds);

    /**
     * Pause the test execution for a fixed number of seconds.
     *
     * <p>
     * Prefer explicit waits (waitForVisible, waitForNotVisible) over pause in tests, but
     * pause can be useful for debugging or when waiting for non-deterministic behaviors.
     * </p>
     *
     * @param seconds number of seconds to sleep/pause
     */
    void pause(int seconds);

    /**
     * Check whether the element is currently visible on screen.
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element to check
     * @return true if visible; false otherwise
     */
    boolean isVisible(String page, String locator);

    /**
     * Check whether the element is enabled (interactable).
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element to check
     * @return true if enabled; false otherwise
     */
    boolean isEnabled(String page, String locator);

    /**
     * Check whether the element is selected (for example a checkbox, radio button or selected list item).
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element to check
     * @return true if selected; false otherwise
     */
    boolean isSelected(String page, String locator);

    /**
     * Perform a long press (touch and hold) on the element for the specified duration.
     *
     * @param page           logical page or screen identifier where the locator is defined
     * @param locator        key or selector for the element to long-press
     * @param durationMillis duration of the press in milliseconds
     */
    void longPress(String page, String locator, long durationMillis);

    /**
     * Perform a double tap gesture on the specified element.
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element to double-tap
     */
    void doubleTap(String page, String locator);

    /**
     * Tap at an absolute location on the device screen.
     *
     * <p>
     * Coordinates are in device screen pixels (or in the coordinate system used by the driver).
     * This is useful for interacting with elements that are not easily accessible via locators.
     * </p>
     *
     * @param x horizontal coordinate (pixels)
     * @param y vertical coordinate (pixels)
     */
    void tapAt(int x, int y);

    /**
     * Drag an element from a source locator on one page to a target locator on another page.
     *
     * <p>
     * Implementations should perform a press+move+release gesture or use any platform-native
     * drag-and-drop API. If either element is not present or not interactable, the implementation
     * should raise an informative failure.
     * </p>
     *
     * @param fromPage    logical page/screen identifier for the source element
     * @param fromLocator key or selector for the source element
     * @param toPage      logical page/screen identifier for the destination element
     * @param toLocator   key or selector for the destination element
     */
    void drag(String fromPage, String fromLocator, String toPage, String toLocator);

    /**
     * Scroll/swipe until the element identified by the locator becomes visible or until the maximum
     * number of swipes is reached.
     *
     * @param page      logical page or screen identifier where the locator is defined
     * @param locator   key or selector for the element to reveal
     * @param maxSwipes maximum number of swipe attempts before giving up
     */
    void scrollUntilVisible(String page, String locator, int maxSwipes);

    /**
     * Scroll through the screen content until the given text is found (commonly used for lists).
     *
     * @param text visible text to scroll to
     */
    void scrollToText(String text);

    /**
     * Hide the on-screen keyboard if it is visible.
     *
     * <p>
     * Behavior can vary across platforms; implementations should attempt the platform-appropriate
     * method to dismiss the keyboard.
     * </p>
     */
    void hideKeyboard();

    /**
     * Send the application to the background for a specified duration, then resume the app.
     *
     * <p>
     * Useful to verify app behavior when backgrounded and restored. Some platforms may treat
     * backgrounding differently (suspending vs. keeping process alive).
     * </p>
     *
     * @param seconds number of seconds to background the app before bringing it back to foreground
     */
    void backgroundApp(int seconds);

    /**
     * Perform a swipe up gesture (typically to reveal lower content).
     */
    void swipeUp();

    /**
     * Perform a swipe down gesture (typically to reveal upper content).
     */
    void swipeDown();

    /**
     * Perform a swipe left gesture (commonly to move to the next item or dismiss).
     */
    void swipeLeft();

    /**
     * Perform a swipe right gesture (commonly to move to the previous item or reveal actions).
     */
    void swipeRight();

    /**
     * Perform a pinch-in gesture (zoom out by bringing fingers together).
     */
    void pinchIn();

    /**
     * Perform a zoom-out gesture (spread fingers apart to zoom in).
     *
     * Note: naming intentionally matches the existing method; verify expected behavior in implementation.
     */
    void zoomOut();

    /**
     * Set the device orientation explicitly.
     *
     * <p>
     * Common orientation values include: "PORTRAIT", "LANDSCAPE" (case-insensitive).
     * Implementations should map the provided string to the underlying platform driver values.
     * </p>
     *
     * @param orientation orientation string to set on the device
     */
    void setOrientation(String orientation);

    /**
     * Set the device orientation to a pre-configured orientation defined by the test framework
     * or environment. This allows tests to use a known default orientation without specifying it.
     */
    void setConfiguredOrientation();

    /**
     * Activate (bring to foreground) the application specified by its package name (Android)
     * or bundle identifier (iOS).
     *
     * @param appId package name or bundle id of the app to activate
     */
    void activateApp(String appId);

    /**
     * Terminate (stop) the application identified by package name or bundle id.
     *
     * @param appId package name or bundle id of the app to terminate
     */
    void terminateApp(String appId);

    /**
     * Open a deep link URL in the context of the specified application.
     *
     * <p>
     * The appPackageOrBundleId parameter may be required by some drivers to route the deep link
     * to the correct application. Implementations should ensure the URL is properly encoded and
     * that the deep link invocation is supported on the target platform.
     * </p>
     *
     * @param url                  deep link to open (for example myapp://path or https deep link)
     * @param appPackageOrBundleId optional package name or bundle id to target the deep link
     */
    void openDeepLink(String url, String appPackageOrBundleId);

    /**
     * Push a file from the local test machine to the device or emulator.
     *
     * @param remotePath path on the device/emulator filesystem where the file should be placed
     * @param localPath  path on the local test machine of the file to upload
     */
    void pushFile(String remotePath, String localPath);

    /**
     * Pull a file from the device/emulator to the local test machine.
     *
     * @param remotePath      path on the device/emulator to pull
     * @param localOutputPath destination path on the local machine to save the pulled file
     */
    void pullFile(String remotePath, String localOutputPath);

    /**
     * Set the system clipboard content to the provided text.
     *
     * <p>
     * Behavior may vary by platform and driver support. Useful for tests that need to paste
     * predictable content into fields.
     * </p>
     *
     * @param text text to set in the device clipboard
     */
    void setClipboard(String text);

    /**
     * Get the current text content of the system clipboard.
     *
     * @return clipboard contents as a String, or an empty string if unavailable
     */
    String getClipboard();

    /**
     * Retrieve the currently available automation contexts (for example "NATIVE_APP" and one or more webview contexts).
     *
     * <p>
     * Testers can use contexts to switch between native and webview automation as needed.
     * </p>
     *
     * @return a Set of context names available in the current session
     */
    Set<String> getContexts();

    /**
     * Switch the driver's context to the specified context name.
     *
     * <p>
     * For hybrid applications, context names commonly include "NATIVE_APP" and "WEBVIEW_<id>".
     * After switching to a webview context, web-based selectors may be usable.
     * </p>
     *
     * @param contextName name of the context to switch to
     */
    void switchContext(String contextName);

    /**
     * Convenience method to switch the automation context back to the native app context.
     *
     * <p>
     * Equivalent to switchContext("NATIVE_APP") for most drivers.
     * </p>
     */
    void switchToNativeContext();

    /**
     * Grant a runtime permission to the specified application (Android).
     *
     * <p>
     * On iOS, this may be a no-op or require a different approach (simulator configuration).
     * Implementations should be explicit about platform behavior in their documentation.
     * </p>
     *
     * @param appId      package name or bundle id of the app to grant permission to
     * @param permission permission string (for example "android.permission.CAMERA")
     */
    void grantPermission(String appId, String permission);

    /**
     * Revoke a runtime permission from the specified application (Android).
     *
     * @param appId      package name or bundle id of the app to revoke permission from
     * @param permission permission string to revoke
     */
    void revokePermission(String appId, String permission);

    /**
     * Open an arbitrary URL in the device browser or via the default handler.
     *
     * <p>
     * Use this to validate external links or workflows that switch to a browser.
     * </p>
     *
     * @param url the URL to open
     */
    void openUrl(String url);

    /**
     * Simulate pressing the Enter (Return) key while focused on the specified element.
     *
     * @param page    logical page or screen identifier where the locator is defined
     * @param locator key or selector for the element that should receive the Enter key
     */
    void pressEnter(String page, String locator);

    /**
     * Get the current URL when running in a webview context.
     *
     * @return the current URL as a String, or an empty string if unavailable or not in a webview
     */
    String getCurrentUrl();

    /**
     * Get the current page title when running in a webview context.
     *
     * @return the title of the current page, or an empty string if unavailable
     */
    String getTitle();

    /**
     * Save the current page source (DOM or native XML) to the specified local output path.
     *
     * <p>
     * Useful for debugging failing tests, capturing app state, or keeping artifacts for analysis.
     * Implementations should ensure the output directory exists or raise a clear error if it cannot write.
     * </p>
     *
     * @param outputPath full path (including filename) where the page source should be saved
     */
    void savePageSource(String outputPath);
}
