package com.ptaf.ui.mobilebrowser;

import java.util.Objects;

/**
 * Immutable representation of a mobile or tablet browser profile used for Playwright emulation.
 *
 * <p>This class encapsulates a set of properties (viewport size, screen size, device scale factor,
 * user agent, platform, etc.) required to simulate a specific device in automated tests. Instances
 * are immutable; all fields are final and set in the constructor.</p>
 *
 * <p>Intended usage: testers create or obtain predefined instances of this profile and pass them to
 * browser/emulation setup code. The getters expose the raw values needed for Playwright or similar
 * APIs.</p>
 *
 * <p>Notes for testers:
 * - viewportWidth/viewportHeight are the CSS viewport size in pixels used by the browser context.
 * - screenWidth/screenHeight describe the full device screen dimensions in pixels.
 * - deviceScaleFactor is the device pixel ratio (DPR), e.g. 2.0 for many retina devices.
 * - mobile indicates whether the device should be treated as a mobile device (affects UA and layout).
 * - touch indicates whether the device supports touch input.</p>
 */
public class MobileBrowserProfile {
    // Basic identification
    private final String name;            // Human readable profile name, e.g. "iPhone 12"
    private final String browserEngine;   // Expected engine: "chromium", "webkit", "firefox" (case-insensitive)

    // User / platform information
    private final String userAgent;       // Full user agent string to present to the browser
    private final String platform;        // Operating system platform, e.g. "iOS", "Android"
    private final String deviceCategory;  // Category such as "phone" or "tablet"
    private final String orientation;     // Orientation string, e.g. "portrait" or "landscape"

    // Viewport and screen sizes (pixels)
    private final int viewportWidth;      // CSS viewport width in pixels used by the browser context
    private final int viewportHeight;     // CSS viewport height in pixels used by the browser context
    private final int screenWidth;        // Physical screen width in pixels
    private final int screenHeight;       // Physical screen height in pixels

    // Display and interaction characteristics
    private final double deviceScaleFactor;// Device pixel ratio (DPR), e.g. 1.0, 2.0
    private final boolean mobile;         // Whether the profile should be treated as a mobile device
    private final boolean touch;          // Whether the device supports touch events

    /**
     * Create a new immutable MobileBrowserProfile holding all required emulation parameters.
     *
     * @param name            Human-readable name for the profile (e.g. "Pixel 4", "iPad Pro").
     * @param browserEngine   Primary browser engine to emulate. Expected values (case-insensitive):
     *                        "chromium", "webkit", or "firefox". Other values are allowed but
     *                        convenience methods like usesChromium() will return false.
     * @param viewportWidth   Viewport width in CSS pixels (used by the browser context).
     * @param viewportHeight  Viewport height in CSS pixels (used by the browser context).
     * @param screenWidth     Full device screen width in physical pixels.
     * @param screenHeight    Full device screen height in physical pixels.
     * @param deviceScaleFactor Device pixel ratio (DPR), e.g. 1.0 for standard, 2.0 for retina.
     * @param mobile          True if the profile represents a mobile device (affects layout, UA hints).
     * @param touch           True if the device supports touch input (enables touch emulation).
     * @param userAgent       Full user agent string to present to the website under test.
     * @param platform        Platform name, e.g. "iOS", "Android", "Windows".
     * @param deviceCategory  Category such as "phone" or "tablet" to aid selection and reporting.
     * @param orientation     Orientation description, typically "portrait" or "landscape".
     *
     * <p>All parameters are stored as-is; this constructor does not perform normalization beyond
     * assignment. Nulls are allowed for string fields but convenience methods that compare strings
     * guard against null values.</p>
     */
    public MobileBrowserProfile(String name, String browserEngine, int viewportWidth, int viewportHeight, int screenWidth, int screenHeight,
                                double deviceScaleFactor, boolean mobile, boolean touch, String userAgent, String platform, String deviceCategory, String orientation) {
        // Identity fields
        this.name = name;
        this.browserEngine = browserEngine;

        // Viewport and screen geometry (pixels)
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Display and input capabilities
        this.deviceScaleFactor = deviceScaleFactor;
        this.mobile = mobile;
        this.touch = touch;

        // User agent and platform descriptors
        this.userAgent = userAgent;
        this.platform = platform;
        this.deviceCategory = deviceCategory;
        this.orientation = orientation;
    }

    /**
     * @return Human-readable profile name (may be null).
     */
    public String getName() { return name; }

    /**
     * @return Browser engine string supplied at construction (may be null). Common values:
     * "chromium", "webkit", "firefox".
     */
    public String getBrowserEngine() { return browserEngine; }

    /**
     * @return Viewport width in CSS pixels. Used to configure the browser context viewport.
     */
    public int getViewportWidth() { return viewportWidth; }

    /**
     * @return Viewport height in CSS pixels. Used to configure the browser context viewport.
     */
    public int getViewportHeight() { return viewportHeight; }

    /**
     * @return Full device screen width in physical pixels.
     */
    public int getScreenWidth() { return screenWidth; }

    /**
     * @return Full device screen height in physical pixels.
     */
    public int getScreenHeight() { return screenHeight; }

    /**
     * @return Device pixel ratio (DPR) such as 1.0, 2.0. This value helps simulate high-DPI screens.
     */
    public double getDeviceScaleFactor() { return deviceScaleFactor; }

    /**
     * @return True if the profile represents a mobile device. This flag typically causes the UA
     * and certain layout behaviors to reflect a mobile form factor.
     */
    public boolean isMobile() { return mobile; }

    /**
     * @return True if the device supports touch input. If true, test frameworks should enable touch
     * event emulation where applicable.
     */
    public boolean hasTouch() { return touch; }

    /**
     * @return User agent string to present to the web application under test (may be null).
     */
    public String getUserAgent() { return userAgent; }

    /**
     * @return Platform string such as "iOS" or "Android" (may be null).
     */
    public String getPlatform() { return platform; }

    /**
     * @return Device category such as "phone" or "tablet" (may be null).
     */
    public String getDeviceCategory() { return deviceCategory; }

    /**
     * @return Orientation direction such as "portrait" or "landscape" (may be null).
     */
    public String getOrientation() { return orientation; }

    /**
     * Convenience predicate: returns true if the configured browser engine is Chromium.
     *
     * <p>Comparison is case-insensitive and trims surrounding whitespace to tolerate minor input
     * differences. If browserEngine is null, returns false.</p>
     *
     * @return true if browserEngine equals "chromium" (ignoring case and surrounding whitespace)
     */
    public boolean usesChromium() { return browserEngine != null && browserEngine.trim().equalsIgnoreCase("chromium"); }

    /**
     * Convenience predicate: returns true if the configured browser engine is WebKit.
     *
     * <p>Comparison is case-insensitive and trims surrounding whitespace. If browserEngine is null,
     * returns false.</p>
     *
     * @return true if browserEngine equals "webkit" (ignoring case and surrounding whitespace)
     */
    public boolean usesWebKit() { return browserEngine != null && browserEngine.trim().equalsIgnoreCase("webkit"); }

    /**
     * Convenience predicate: returns true if the configured browser engine is Firefox.
     *
     * <p>Comparison is case-insensitive and trims surrounding whitespace. If browserEngine is null,
     * returns false.</p>
     *
     * @return true if browserEngine equals "firefox" (ignoring case and surrounding whitespace)
     */
    public boolean usesFirefox() { return browserEngine != null && browserEngine.trim().equalsIgnoreCase("firefox"); }
}
