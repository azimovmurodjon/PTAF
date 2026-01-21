package com.ptaf.ui.action_performer;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.ptaf.utils.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ActionPerformer (100% version)
 *
 * SAME behavior as your working code.
 * ONLY change: when it fails, it prints the exact failure details (more transparent).
 */
public class ActionPerformer {

    private static final Logger logger = LoggerFactory.getLogger(ActionPerformer.class);

    /** Utility to format text content by placing each word on a new line. */
    private static String formatTextForNewLine(String text) {
        return text == null ? null : text.replaceAll(" ", "\n");
    }

    /**
     * Reads time_to_wait from config (seconds) and returns milliseconds.
     * IMPORTANT: No hidden default. If missing/invalid -> 0ms (fast).
     */
    private long actionTimeoutMs() {
        String timeToWait = ConfigurationProperties.getValue("time_to_wait");
        try {
            int seconds = Integer.parseInt(timeToWait == null ? "0" : timeToWait.trim());
            return Math.max(0, seconds) * 1000L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * If timeout=0, Playwright would still use its own default timeouts in many calls.
     * We force near-no-wait so it fails fast as you requested.
     */
    private double effectiveTimeoutMs() {
        long t = actionTimeoutMs();
        return (t <= 0) ? 1 : t;
    }

    /**
     * Smart page load wait (conditional, fast):
     * - Only used after actions that might trigger navigation/reload.
     * - Uses DOMCONTENTLOADED + LOAD only (no NETWORKIDLE).
     * - Never throws (non-blocking).
     * - Returns immediately if already loaded.
     */
    private void smartWaitForLoadIfAny(Page page) {
        long t = actionTimeoutMs();
        if (t <= 0) return;

        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(t));
        } catch (Exception ignored) {}

        try {
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(t));
        } catch (Exception ignored) {}
    }

    public String performActionWithReturn(Page page, String action, Locator targetLocator, String value) {
        return performAction(page, action, targetLocator, value);
    }

    public String performAction(Page page, String action, Locator targetLocator, String value) {
        double t = effectiveTimeoutMs();

        try {
            switch (action.toLowerCase()) {

                // =======================
                // SINGLE-ELEMENT ACTIONS
                // =======================
                case "click":
                    targetLocator.first().click(new Locator.ClickOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "fill":
                    targetLocator.first().fill(value, new Locator.FillOptions().setTimeout(t));
                    return null;

                case "select":
                    targetLocator.first().selectOption(value, new Locator.SelectOptionOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "selectmultiple":
                    targetLocator.first().selectOption(value.split(","), new Locator.SelectOptionOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "check":
                    targetLocator.first().check(new Locator.CheckOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "uncheck":
                    targetLocator.first().uncheck(new Locator.UncheckOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "hover":
                    targetLocator.first().hover(new Locator.HoverOptions().setTimeout(t));
                    return null;

                case "type":
                    targetLocator.first().type(value, new Locator.TypeOptions().setTimeout(t));
                    return null;

                case "press":
                    targetLocator.first().press(value, new Locator.PressOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "dblclick":
                    targetLocator.first().dblclick(new Locator.DblclickOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "rightclick":
                    targetLocator.first().click(new Locator.ClickOptions()
                            .setButton(MouseButton.RIGHT)
                            .setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "tap":
                    targetLocator.first().tap(new Locator.TapOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;

                case "input":
                    targetLocator.first().evaluate("(element, val) => element.value = val", value);
                    return null;

                case "screenshot":
                    targetLocator.first().screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(value)));
                    return null;

                case "scroll":
                    targetLocator.first().evaluate("element => element.scrollIntoView({ behavior: 'smooth', block: 'center' })");
                    return null;

                case "focus":
                    targetLocator.first().focus(new Locator.FocusOptions().setTimeout(t));
                    return null;

                case "blur":
                    targetLocator.first().evaluate("element => element.blur()");
                    return null;

                case "clear":
                    // If your Playwright version doesn't support clear/options, replace with fill("", new FillOptions().setTimeout(t))
                    targetLocator.first().clear(new Locator.ClearOptions().setTimeout(t));
                    return null;

                case "drag": {
                    Locator target = targetLocator.page().locator(value).first();
                    targetLocator.first().dragTo(target, new Locator.DragToOptions().setTimeout(t));
                    smartWaitForLoadIfAny(page);
                    return null;
                }

                case "dragstart":
                    targetLocator.first().dispatchEvent("dragstart");
                    return null;

                case "dragend":
                    targetLocator.first().dispatchEvent("dragend");
                    return null;

                case "uploadfile":
                case "selectfile":
                    targetLocator.first().setInputFiles(Paths.get(value));
                    return null;

                // =======================
                // DOWNLOADS
                // =======================
                case "download": {
                    Download download = page.waitForDownload(
                            new Page.WaitForDownloadOptions().setTimeout(t),
                            () -> targetLocator.first().click(new Locator.ClickOptions().setTimeout(t))
                    );

                    String timestamp = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

                    String uniqueFilename = timestamp + "-" + download.suggestedFilename();
                    Path savePath = Paths.get(value, uniqueFilename);
                    download.saveAs(savePath);

                    logger.info("File downloaded successfully and saved to: {}", savePath);
                    return savePath.toString();
                }

                case "download_optional": {
                    try {
                        Download downloadOpt = page.waitForDownload(
                                new Page.WaitForDownloadOptions().setTimeout(t),
                                () -> targetLocator.first().click(new Locator.ClickOptions().setTimeout(t))
                        );

                        String timestampOpt = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

                        String uniqueFilenameOpt = timestampOpt + "-" + downloadOpt.suggestedFilename();
                        Path savePathOpt = Paths.get(value, uniqueFilenameOpt);
                        downloadOpt.saveAs(savePathOpt);

                        logger.info("Optional download success -> {}", savePathOpt);
                        return savePathOpt.toString();
                    } catch (PlaywrightException ex) {
                        logger.info("Optional download skipped (no download event): {}", ex.getMessage());
                        return null;
                    } catch (Exception ex) {
                        logger.info("Optional download skipped (download failed): {}", ex.getMessage());
                        return null;
                    }
                }

                case "file_chooser_for_upload":
                    page.waitForFileChooser(
                            new Page.WaitForFileChooserOptions().setTimeout(t),
                            () -> click(targetLocator.first(), t)
                    );
                    return null;

                // =======================
                // ATTRIBUTE / JS
                // =======================
                case "setattribute":
                    targetLocator.first().evaluate("(el, val) => el.setAttribute('value', val)", value);
                    return null;

                case "removeattribute":
                    targetLocator.first().evaluate("(el, attr) => el.removeAttribute(attr)", value);
                    return null;

                case "evaluate":
                    targetLocator.first().evaluate(value);
                    return null;

                // =======================
                // WAITS
                // =======================
                case "waitforelement":
                    targetLocator.first().waitFor(new Locator.WaitForOptions().setTimeout(t));
                    return null;

                case "waitforstate":
                    targetLocator.first().waitFor(new Locator.WaitForOptions()
                            .setTimeout(t)
                            .setState(WaitForSelectorState.valueOf(value.toUpperCase())));
                    return null;

                case "waitfortext":
                    targetLocator.first().waitFor(new Locator.WaitForOptions()
                            .setTimeout(t)
                            .setState(WaitForSelectorState.VISIBLE));
                    if (targetLocator.first().textContent() == null || !targetLocator.first().textContent().contains(value))
                        throw new AssertionError("Text not found: " + value);
                    return targetLocator.first().textContent();

                case "waitforvalue":
                    targetLocator.first().waitFor(new Locator.WaitForOptions()
                            .setTimeout(t)
                            .setState(WaitForSelectorState.VISIBLE));
                    if (!targetLocator.first().inputValue().equals(value))
                        throw new AssertionError("Value mismatch.");
                    return targetLocator.first().inputValue();

                // =======================
                // GETTERS / VALIDATIONS
                // =======================
                case "getattribute":
                    return targetLocator.first().getAttribute(value);

                case "gettext":
                    return targetLocator.first().textContent();

                case "get_and_contain_text": {
                    String txt = targetLocator.first().textContent();
                    assertCondition(txt != null && txt.contains(txt), "Element does not contain expected text.");
                    return txt;
                }

                case "getvalue":
                    return targetLocator.first().inputValue();

                case "returnelement":
                    return targetLocator.textContent();

                case "hasvalue": {
                    String currentValue = targetLocator.first().inputValue();
                    assertCondition(currentValue.equals(value), "Expected: " + value + ", but found: " + currentValue);
                    return currentValue;
                }

                case "isvisible": {
                    boolean isVisible = targetLocator.first().isVisible();
                    assertCondition(isVisible, "Element is not visible.");
                    return String.valueOf(isVisible);
                }

                case "isenabled": {
                    boolean isEnabled = targetLocator.first().isEnabled();
                    assertCondition(isEnabled, "Element is not enabled.");
                    return String.valueOf(isEnabled);
                }

                case "ischecked": {
                    boolean isChecked = targetLocator.first().isChecked();
                    assertCondition(isChecked, "Element is not checked.");
                    return String.valueOf(isChecked);
                }

                case "isdisabled": {
                    boolean isDisabled = targetLocator.first().isDisabled();
                    assertCondition(isDisabled, "Element is not disabled.");
                    return String.valueOf(isDisabled);
                }

                case "ishidden": {
                    boolean isHidden = targetLocator.first().isHidden();
                    assertCondition(isHidden, "Element is not hidden.");
                    return String.valueOf(isHidden);
                }

                case "hastext": {
                    String locatorText = targetLocator.first().textContent();
                    assertCondition(locatorText != null && locatorText.contains(value), "Text mismatch.");
                    return locatorText;
                }

                case "hasclass": {
                    String cls = targetLocator.first().getAttribute("class");
                    boolean hasClass = cls != null && cls.contains(value);
                    assertCondition(hasClass, "Class mismatch.");
                    return String.valueOf(hasClass);
                }

                case "hasequalvalue": {
                    String actualValue = targetLocator.first().inputValue();
                    assertCondition(actualValue.equals(value), "Value mismatch.");
                    return actualValue;
                }

                case "isempty": {
                    String inputValue = targetLocator.first().inputValue();
                    assertCondition(inputValue.isEmpty(), "Element is not empty.");
                    return inputValue;
                }

                // =======================
                // MULTI-ELEMENT ACTIONS
                // =======================
                case "exists": {
                    boolean exists = targetLocator.count() > 0;
                    assertCondition(exists, "Element does not exist.");
                    return String.valueOf(exists);
                }

                case "not_exists": {
                    boolean notExists = targetLocator.count() == 0;
                    assertCondition(notExists, "Element exists but should not.");
                    return String.valueOf(notExists);
                }

                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) {
            // SAME behavior, only better error printing:
            String debug = buildExactFailureMessage(page, action, value, targetLocator, t, e);

            // log full context + original stack
            logger.error(debug, e);

            // throw full message so Cucumber/Extent shows exact failure
            throw new RuntimeException(debug, e);
        }
    }

    private void click(Locator targetLocator, double timeoutMs) {
        try {
            targetLocator.first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
        } catch (Exception e) {
            logger.error("Error while clicking on target locator: {}", e.getMessage());
            throw new RuntimeException("Click action failed: " + e.getMessage(), e);
        }
    }

    private void assertCondition(boolean condition, String errorMessage) {
        if (!condition) throw new AssertionError(errorMessage);
    }

    /**
     * Waits for the first element matching the locator to be visible using time_to_wait.
     * - If time_to_wait=0 => fails immediately if not visible.
     */
    public void waitForLocator(Locator locator) {
        double t = effectiveTimeoutMs();
        try {
            locator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(t));
        } catch (Exception e) {
            Page page = null;
            try { page = locator.page(); } catch (Exception ignored) {}
            String debug = buildExactFailureMessage(page, "waitForLocator", null, locator, t, e);
            logger.error(debug, e);
            throw new RuntimeException(debug, e);
        }
    }

    // ============================================================
    // ONLY FOR PRINTING "EXACT WHY IT FAILED" (NO NEW FEATURES)
    // ============================================================

    private String buildExactFailureMessage(Page page, String action, String value, Locator locator, double timeoutMs, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n========== PTAF FAILURE (EXACT DETAILS) ==========\n");
        sb.append("Action      : ").append(safe(action)).append("\n");
        sb.append("Value       : ").append(safe(value)).append("\n");
        sb.append("Timeout(ms) : ").append((long) timeoutMs).append("\n");

        // Page info
        try {
            if (page != null) {
                sb.append("URL         : ").append(safe(page.url())).append("\n");
                sb.append("Title       : ").append(safe(page.title())).append("\n");
            }
        } catch (Exception ignored) {}

        // Locator info
        sb.append("Locator     : ").append(locatorToString(locator)).append("\n");

        // Count + first element quick state (this shows WHY: not found / hidden / disabled / etc.)
        try {
            if (locator != null) {
                int count = locator.count();
                sb.append("MatchCount  : ").append(count).append("\n");

                if (count > 0) {
                    Locator first = locator.first();
                    sb.append("FirstState  : ")
                            .append("visible=").append(safeBool(() -> first.isVisible())).append(", ")
                            .append("enabled=").append(safeBool(() -> first.isEnabled())).append(", ")
                            .append("hidden=").append(safeBool(() -> first.isHidden())).append(", ")
                            .append("disabled=").append(safeBool(() -> first.isDisabled())).append(", ")
                            .append("checked=").append(safeBool(() -> first.isChecked()))
                            .append("\n");

                    // Helpful snippets
                    String text = safe(() -> first.textContent());
                    String inputValue = safe(() -> first.inputValue());
                    if (text != null && !text.isBlank()) sb.append("Text        : ").append(limit(text, 400)).append("\n");
                    if (inputValue != null && !inputValue.isBlank()) sb.append("InputValue  : ").append(limit(inputValue, 400)).append("\n");
                }
            }
        } catch (Exception ex) {
            sb.append("DebugError  : Could not collect locator details: ").append(ex.getMessage()).append("\n");
        }

        // REAL exception content (Playwright message is here)
        sb.append("Exception   : ").append(e.getClass().getName()).append("\n");
        sb.append("Message     : ").append(safe(e.getMessage())).append("\n");
        sb.append("Full        : ").append(safe(e.toString())).append("\n");
        sb.append("==================================================\n");

        return sb.toString();
    }

    private String locatorToString(Locator locator) {
        if (locator == null) return "null";
        try { return locator.toString(); }
        catch (Exception e) { return "Locator(toString failed): " + e.getMessage(); }
    }

    private String safe(String s) {
        return s == null ? "null" : s;
    }

    private String safe(SupplierWithException<String> supplier) {
        try { return supplier.get(); } catch (Exception e) { return null; }
    }

    private boolean safeBool(SupplierWithException<Boolean> supplier) {
        try { return supplier.get(); } catch (Exception e) { return false; }
    }

    private String limit(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }
}