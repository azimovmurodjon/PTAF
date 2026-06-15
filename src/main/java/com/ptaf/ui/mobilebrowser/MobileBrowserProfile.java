package com.ptaf.ui.mobilebrowser;

/** Immutable Playwright mobile/tablet browser emulation profile. */
public class MobileBrowserProfile {
    private final String name, browserEngine, userAgent, platform, deviceCategory, orientation;
    private final int viewportWidth, viewportHeight, screenWidth, screenHeight;
    private final double deviceScaleFactor;
    private final boolean mobile, touch;

    public MobileBrowserProfile(String name, String browserEngine, int viewportWidth, int viewportHeight, int screenWidth, int screenHeight,
                                double deviceScaleFactor, boolean mobile, boolean touch, String userAgent, String platform, String deviceCategory, String orientation) {
        this.name = name; this.browserEngine = browserEngine; this.viewportWidth = viewportWidth; this.viewportHeight = viewportHeight;
        this.screenWidth = screenWidth; this.screenHeight = screenHeight; this.deviceScaleFactor = deviceScaleFactor; this.mobile = mobile;
        this.touch = touch; this.userAgent = userAgent; this.platform = platform; this.deviceCategory = deviceCategory; this.orientation = orientation;
    }
    public String getName() { return name; }
    public String getBrowserEngine() { return browserEngine; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public double getDeviceScaleFactor() { return deviceScaleFactor; }
    public boolean isMobile() { return mobile; }
    public boolean hasTouch() { return touch; }
    public String getUserAgent() { return userAgent; }
    public String getPlatform() { return platform; }
    public String getDeviceCategory() { return deviceCategory; }
    public String getOrientation() { return orientation; }
    public boolean usesChromium() { return browserEngine != null && browserEngine.trim().equalsIgnoreCase("chromium"); }
    public boolean usesWebKit() { return browserEngine != null && browserEngine.trim().equalsIgnoreCase("webkit"); }
    public boolean usesFirefox() { return browserEngine != null && browserEngine.trim().equalsIgnoreCase("firefox"); }
}
