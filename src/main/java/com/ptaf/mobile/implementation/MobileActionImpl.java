package com.ptaf.mobile.implementation;

import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.interfaces.MobileAction;
import com.ptaf.mobile.pages.MobileCommonMethods;

import java.util.Set;

/** Default mobile action implementation backed by the current thread's Appium driver. */
public class MobileActionImpl implements MobileAction {
    private MobileCommonMethods methods() { return new MobileCommonMethods(MobileDriverManager.getDriver()); }
    public void tap(String page, String locator) { methods().tap(page, locator); }
    public void type(String page, String locator, String value) { methods().type(page, locator, value); }
    public void clear(String page, String locator) { methods().clear(page, locator); }
    public String getText(String page, String locator) { return methods().getText(page, locator); }
    public void waitForVisible(String page, String locator) { methods().waitForVisible(page, locator); }
    public boolean isVisible(String page, String locator) { return methods().isVisible(page, locator); }
    public boolean isEnabled(String page, String locator) { return methods().isEnabled(page, locator); }
    public boolean isSelected(String page, String locator) { return methods().isSelected(page, locator); }
    public void longPress(String page, String locator, long durationMillis) { methods().longPress(page, locator, durationMillis); }
    public void doubleTap(String page, String locator) { methods().doubleTap(page, locator); }
    public void tapAt(int x, int y) { methods().tapAt(x, y); }
    public void drag(String fromPage, String fromLocator, String toPage, String toLocator) { methods().drag(fromPage, fromLocator, toPage, toLocator); }
    public void scrollUntilVisible(String page, String locator, int maxSwipes) { methods().scrollUntilVisible(page, locator, maxSwipes); }
    public void scrollToText(String text) { methods().scrollToText(text); }
    public void hideKeyboard() { methods().hideKeyboard(); }
    public void backgroundApp(int seconds) { methods().backgroundApp(seconds); }
    public void swipeUp() { methods().swipeUp(); }
    public void swipeDown() { methods().swipeDown(); }
    public void swipeLeft() { methods().swipeLeft(); }
    public void swipeRight() { methods().swipeRight(); }
    public void pinchIn() { methods().pinchIn(); }
    public void zoomOut() { methods().zoomOut(); }
    public void setOrientation(String orientation) { methods().setOrientation(orientation); }
    public void setConfiguredOrientation() { methods().setConfiguredOrientation(); }
    public void activateApp(String appId) { methods().activateApp(appId); }
    public void terminateApp(String appId) { methods().terminateApp(appId); }
    public void openDeepLink(String url, String appPackageOrBundleId) { methods().openDeepLink(url, appPackageOrBundleId); }
    public void pushFile(String remotePath, String localPath) { methods().pushFile(remotePath, localPath); }
    public void pullFile(String remotePath, String localOutputPath) { methods().pullFile(remotePath, localOutputPath); }
    public void setClipboard(String text) { methods().setClipboard(text); }
    public String getClipboard() { return methods().getClipboard(); }
    public Set<String> getContexts() { return methods().getContexts(); }
    public void switchContext(String contextName) { methods().switchContext(contextName); }
    public void switchToNativeContext() { methods().switchToNativeContext(); }
    public void grantPermission(String appId, String permission) { methods().grantPermission(appId, permission); }
    public void revokePermission(String appId, String permission) { methods().revokePermission(appId, permission); }
}
