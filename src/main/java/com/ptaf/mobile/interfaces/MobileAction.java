package com.ptaf.mobile.interfaces;

import java.util.Set;

public interface MobileAction {
    void tap(String page, String locator);
    void type(String page, String locator, String value);
    void clear(String page, String locator);
    String getText(String page, String locator);
    void waitForVisible(String page, String locator);
    boolean isVisible(String page, String locator);
    boolean isEnabled(String page, String locator);
    boolean isSelected(String page, String locator);
    void longPress(String page, String locator, long durationMillis);
    void doubleTap(String page, String locator);
    void tapAt(int x, int y);
    void drag(String fromPage, String fromLocator, String toPage, String toLocator);
    void scrollUntilVisible(String page, String locator, int maxSwipes);
    void scrollToText(String text);
    void hideKeyboard();
    void backgroundApp(int seconds);
    void swipeUp();
    void swipeDown();
    void swipeLeft();
    void swipeRight();
    void pinchIn();
    void zoomOut();
    void setOrientation(String orientation);
    void setConfiguredOrientation();
    void activateApp(String appId);
    void terminateApp(String appId);
    void openDeepLink(String url, String appPackageOrBundleId);
    void pushFile(String remotePath, String localPath);
    void pullFile(String remotePath, String localOutputPath);
    void setClipboard(String text);
    String getClipboard();
    Set<String> getContexts();
    void switchContext(String contextName);
    void switchToNativeContext();
    void grantPermission(String appId, String permission);
    void revokePermission(String appId, String permission);
}
