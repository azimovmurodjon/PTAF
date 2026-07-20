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
import java.util.ArrayList;
import java.util.List;

/**
 * ActionPerformer (optimized)
 *
 * What this version guarantees:
 * - Uses your config time_to_wait_in_seconds as MAX timeout (e.g., 10s).
 * - If element is already ready -> NO extra waiting; action runs immediately.
 * - If element is not ready -> waits up to MAX timeout for visibility.
 * - Keeps page-ready behavior after navigation-like actions, but avoids slow SPA stalls by:
 *      - waiting for DOMCONTENTLOADED only (fast + reliable)
 *      - NOT waiting for NETWORKIDLE by default (common slowdown on SPAs)
 *
 * NOTE:
 * - "download" (strict) keeps existing behavior: throws if no download.
 * - "download_optional" returns null if no download event occurs.
 */
public class ActionPerformer {

    /**
     * Thread-local override for the element interaction timeout used in soft assertion mode.
     *
     * <p>When soft assertions are enabled, {@code PageCommonMethods.executeStep()} sets this
     * value to {@code retry_seconds * 1000} before running each step, then clears it afterward.
     * This causes {@link #actionTimeoutMs()} to return the shorter retry timeout for element
     * interactions, while page load waits ({@link #waitForPageReady}) always use the full
     * configured timeout from {@code time_to_wait_in_seconds}.</p>
     *
     * <p>When not set (normal mode), this is null and {@link #actionTimeoutMs()} reads from config
     * as before — zero change to existing behavior.</p>
     */
    public static final ThreadLocal<Long> softAssertionTimeoutOverride = new ThreadLocal<>();

    private boolean isFileInput(Locator locator) {
        try {
            return Boolean.TRUE.equals(
                    locator.first().evaluate("el => el.tagName === 'INPUT' && el.type === 'file'")
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ActionPerformer.class);

    /**
     * Utility: replace spaces with newline characters.
     * Currently unused in the class but preserved for compatibility.
     *
     * @param text input text
     * @return text where every space is replaced with a newline
     */
    private static String formatTextForNewLine(String text) {
        return text.replaceAll(" ", "\n");
    }

    /**
     * Reads time_to_wait_in_seconds from configuration and returns it in milliseconds.
     * - If the property is missing or invalid, uses a safe default of 30_000 ms.
     * - If property value is 0 or negative, returns 0 meaning "no extra waiting" (fail-fast).
     *
     * This is the central place that controls the maximum wait applied by helper methods.
     *
     * @return configured timeout in milliseconds (non-negative)
     */
    private long actionTimeoutMs() {
        // In soft assertion mode, PageCommonMethods.executeStep() sets a thread-local override
        // to retry_seconds * 1000 so element interactions fail fast instead of waiting 60s.
        // This override is cleared after each step so subsequent steps use the full timeout.
        // Page load waits (waitForPageReady) use fullActionTimeoutMs() and are NOT affected.
        Long override = softAssertionTimeoutOverride.get();
        if (override != null) {
            return override;
        }
        String timeToWait = ConfigurationProperties.getValue("time_to_wait_in_seconds");
        try {
            int seconds = Integer.parseInt(timeToWait == null ? "0" : timeToWait.trim());
            return Math.max(0, seconds) * 1000L;
        } catch (Exception e) {
            // Safe fallback if config is missing or invalid
            return 30_000L;
        }
    }

    /**
     * Returns the full configured timeout from {@code time_to_wait_in_seconds} in milliseconds,
     * regardless of any soft assertion override. Used for page load waits that must always
     * use the full timeout so legitimate page navigations are not cut short.
     *
     * @return full configured timeout in milliseconds
     */
    private long fullActionTimeoutMs() {
        String timeToWait = ConfigurationProperties.getValue("time_to_wait_in_seconds");
        try {
            int seconds = Integer.parseInt(timeToWait == null ? "0" : timeToWait.trim());
            return Math.max(0, seconds) * 1000L;
        } catch (Exception e) {
            return 30_000L;
        }
    }

    // ============================================================
    // SMART WAIT HELPERS (MAX timeout, but instant if already ready)
    // ============================================================

    /**
     * Waits ONLY if needed:
     * - If locator.first() is already visible -> returns immediately (no wait).
     * - Otherwise waits up to time_to_wait for VISIBLE.
     */
    private void waitIfNeededVisible(Locator locator) {
        long timeout = actionTimeoutMs();
        if (timeout <= 0 || locator == null) return;

        if (isFileInput(locator)) return ;

        Locator first = locator.first();
        try {
            // Instant check (no wait)
            if (first.isVisible()) return;

            // Wait up to configured timeout ONLY if not visible
            first.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeout));
        } catch (Exception e) {
            Page page = null;
            try { page = locator.page(); } catch (Exception ignored) {}
            String debug = buildExactFailureMessage(page, "waitIfNeededVisible", null, locator, e);
            logger.error(debug, e);
            throw new RuntimeException(debug, e);
        }
    }

    /**
     * Clickable wait that DOES NOT impact runtime:
     * - If already visible+enabled -> instant return (no wait).
     * - Otherwise waits up to timeout for visible only.
     *
     * Why: Playwright click() already does actionable checks efficiently.
     * We avoid custom polling loops that slow runs.
     */
    private void waitIfNeededClickable(Locator locator) {
        long timeout = actionTimeoutMs();
        if (timeout <= 0 || locator == null) return;

        if (isFileInput(locator)) return ;

        Locator first = locator.first();
        try {
            // Instant check: if ready -> no wait
            if (first.isVisible() && first.isEnabled()) return;

            // Otherwise wait up to timeout for visible
            first.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeout));
        } catch (Exception e) {
            Page page = null;
            try { page = locator.page(); } catch (Exception ignored) {}
            String debug = buildExactFailureMessage(page, "waitIfNeededClickable", null, locator, e);
            logger.error(debug, e);
            throw new RuntimeException(debug, e);
        }
    }

    // ============================================================
    // PAGE READY (FAST + avoids SPA slowdowns)
    // ============================================================

    /**
     * Waits for the page to reach a "ready" state, but intentionally conservative:
     * - Waits for DOMContentLoaded only (fast + reliable)
     * - Does not wait for NETWORKIDLE by default (SPAs rarely reach network idle)
     *
     * If the configured timeout is zero or the page is null, this method returns immediately.
     *
     * @param page Playwright Page object
     */
    private void waitForPageReady(Page page) {
        // IMPORTANT: Use fullActionTimeoutMs() here, NOT actionTimeoutMs().
        // Page load waits must always use the full configured timeout (e.g. 60s) so that
        // legitimate page navigations are not cut short by the soft assertion retry window (e.g. 3s).
        // Only element interaction waits (waitIfNeededVisible, waitIfNeededClickable) use the
        // shorter soft assertion timeout via actionTimeoutMs().
        long timeout = fullActionTimeoutMs();
        if (timeout <= 0 || page == null) return;

        try {
            // Fast wait for DOM content loaded. This is typically enough after click/navigation actions.
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(timeout)
            );
        } catch (Exception ignored) {
            // We intentionally ignore load wait failures — we prefer to proceed rather than block tests.
        }

        // If you ever want NETWORKIDLE for specific apps, enable it here with a small cap:
        /*
        try {
            page.waitForLoadState(
                    LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(Math.min(timeout, 1000))
            );
        } catch (Exception ignored) {}
        */
    }

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    /**
     * Convenience wrapper that forwards to performAction and returns the String result.
     * Kept for backward compatibility where a "performActionWithReturn" naming was used.
     *
     * @param page Playwright Page instance (may be used for download or page state)
     * @param action action name (case-insensitive)
     * @param targetLocator target locator (may be null for some actions)
     * @param value optional value parameter used by many actions
     * @return String result for getter-like actions, path for downloads, or null for void actions
     */
    public String performActionWithReturn(Page page, String action, Locator targetLocator, String value) {
        return performAction(page, action, targetLocator, value);
    }

    /**
     * Main entry point: performs a variety of actions against the provided locator or page.
     *
     * Supported actions include common UI interactions (click, fill, select, check, hover, type, etc.),
     * file download/upload, assertions (isvisible, hastext, isempty), and other utilities.
     *
     * Important behavior notes:
     * - Actions do NOT change: logic and names are preserved for compatibility.
     * - Smart waits are applied: instant if element is already ready, otherwise wait up to configured timeout.
     * - For navigation-like interactions, a short page-ready wait (DOMContentLoaded) is applied.
     *
     * @param page Playwright Page object (used for downloads and page-level waits)
     * @param action action to perform (string)
     * @param targetLocator locator to act upon
     * @param value additional action parameter (meaning depends on action)
     * @return String result for getter actions or path for downloads; null for actions without return
     */
    public String performAction(Page page, String action, Locator targetLocator, String value) {
        try {
            switch (action.toLowerCase()) {

                // =============== SINGLE-ELEMENT ACTIONS (.first() added for backward compatibility) ===============
                case "click":
                    waitIfNeededClickable(targetLocator);
                    targetLocator.first().click();
                    waitForPageReady(page);
                    return null;

                case "fill":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().fill(value);
                    return null;

                case "select":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().selectOption(value);
                    waitForPageReady(page);
                    return null;

                case "selectmultiple":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().selectOption(value.split(","));
                    waitForPageReady(page);
                    return null;

                case "check":
                    waitIfNeededClickable(targetLocator);
                    targetLocator.first().check();
                    waitForPageReady(page);
                    return null;

                case "uncheck":
                    waitIfNeededClickable(targetLocator);
                    targetLocator.first().uncheck();
                    waitForPageReady(page);
                    return null;

                case "hover":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().hover();
                    return null;

                case "type":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().type(value);
                    return null;

                case "press":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().press(value);
                    waitForPageReady(page);
                    return null;

                case "dblclick":
                    waitIfNeededClickable(targetLocator);
                    targetLocator.first().dblclick();
                    waitForPageReady(page);
                    return null;

                case "rightclick":
                    waitIfNeededClickable(targetLocator);
                    targetLocator.first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
                    waitForPageReady(page);
                    return null;

                case "tap":
                    waitIfNeededClickable(targetLocator);
                    targetLocator.first().tap();
                    waitForPageReady(page);
                    return null;

                case "input":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().evaluate("(element, val) => element.value = val", value);
                    return null;

                case "screenshot":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(value)));
                    return null;

                case "scroll":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().evaluate("element => element.scrollIntoView({ behavior: 'smooth', block: 'center' })");
                    return null;

                case "focus":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().focus();
                    return null;

                case "blur":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().evaluate("element => element.blur()");
                    return null;

                case "clear":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().clear();
                    return null;

                case "drag": {
                    waitIfNeededVisible(targetLocator);
                    Locator dropTarget = targetLocator.page().locator(value).first();
                    waitIfNeededVisible(dropTarget);
                    targetLocator.first().dragTo(dropTarget);
                    waitForPageReady(page);
                    return null;
                }

                case "dragstart":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().dispatchEvent("dragstart");
                    return null;

                case "dragend":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().dispatchEvent("dragend");
                    return null;

                case "uploadfile":
                case "selectfile":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().setInputFiles(Paths.get(value));
                    return null;

                /**
                 * STRICT DOWNLOAD (existing behavior)
                 * - Waits for download event and throws if it doesn't happen.
                 * - Use this where download MUST happen.
                 *
                 * value = folder path to save downloads into
                 */
                case "download": {
                    waitIfNeededClickable(targetLocator);

                    // Wait for a download to be emitted as a result of clicking the locator.
                    Download download = page.waitForDownload(() -> targetLocator.first().click());

                    // Build a timestamped unique filename to avoid collisions
                    String timestamp = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

                    String originalFileName = download.suggestedFilename();
                    String uniqueFilename = timestamp + "-" + originalFileName;

                    Path savePath = Paths.get(value, uniqueFilename);
                    download.saveAs(savePath);

                    logger.info("File downloaded successfully and saved to: {}", savePath);
                    return savePath.toString();
                }

                /**
                 * OPTIONAL DOWNLOAD
                 * - Tries to download, but if no download event occurs it returns null (NO THROW).
                 * - Useful for cases where clicking may or may not trigger a download depending on context.
                 */
                case "download_optional": {
                    try {
                        waitIfNeededClickable(targetLocator);

                        // Wait for download with a bounded timeout equal to actionTimeoutMs()
                        Download downloadOpt = page.waitForDownload(
                                new Page.WaitForDownloadOptions().setTimeout(actionTimeoutMs()),
                                () -> targetLocator.first().click()
                        );

                        String timestampOpt = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

                        String uniqueFilename = timestampOpt + "-" + downloadOpt.suggestedFilename();
                        Path savePathOpt = Paths.get(value, uniqueFilename);
                        downloadOpt.saveAs(savePathOpt);

                        logger.info("Optional download success -> {}", savePathOpt);
                        return savePathOpt.toString();
                    } catch (PlaywrightException ex) {
                        // Playwright throws when no download event occurred in the given timeout
                        logger.info("Optional download skipped (no download event): {}", ex.getMessage());
                        return null;
                    } catch (Exception ex) {
                        // Any other failure is treated as skipped/failed optional download (no throw)
                        logger.info("Optional download skipped (download failed): {}", ex.getMessage());
                        return null;
                    }
                }

                case "file_chooser_for_upload":
                    waitIfNeededClickable(targetLocator);
                    // Triggers a click that is expected to open a file chooser
                    page.waitForFileChooser(() -> click(targetLocator.first()));
                    return null;

                case "setattribute":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().evaluate("(el, val) => el.setAttribute('value', val)", value);
                    return null;

                case "removeattribute":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().evaluate("(el, attr) => el.removeAttribute(attr)", value);
                    return null;

                case "evaluate":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().evaluate(value);
                    return null;

                case "waitforelement":
                    waitIfNeededVisible(targetLocator);
                    targetLocator.first().waitFor();
                    return null;

                case "waitforstate": {
                    long timeout = actionTimeoutMs();
                    targetLocator.first().waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.valueOf(value.toUpperCase()))
                            .setTimeout(timeout));
                    return null;
                }

                // =============== GETTER/VALIDATION ACTIONS (smart wait: instant if ready, else up to timeout) ===============
                case "getattribute":
                    waitIfNeededVisible(targetLocator);
                    return targetLocator.first().getAttribute(value);

                case "gettext":
                    waitIfNeededVisible(targetLocator);
                    return targetLocator.first().textContent();

                case "get_and_contain_text": {
                    waitIfNeededVisible(targetLocator);
                    String getText = targetLocator.first().textContent();
                    // This assertion seems redundant (contains itself) but is preserved for compatibility
                    assertCondition(getText != null && getText.contains(getText), "Element does not contain expected text.");
                    return getText;
                }

                case "getvalue":
                    waitIfNeededVisible(targetLocator);
                    return targetLocator.first().inputValue();

                case "returnelement":
                    return targetLocator.textContent();

                case "hasvalue": {
                    waitIfNeededVisible(targetLocator);
                    String currentValue = targetLocator.first().inputValue();
                    assertCondition(currentValue.equals(value), "Expected: " + value + ", but found: " + currentValue);
                    return currentValue;
                }

                case "order": {

                    waitIfNeededVisible(targetLocator);
                    List<String> actual = targetLocator.allTextContents();
                    List<String> expected = new ArrayList<>(actual);

                    if ("ascending".equalsIgnoreCase(value)) {
                        expected.sort(String.CASE_INSENSITIVE_ORDER);

                    } else if ("descending".equalsIgnoreCase(value)) {
                        expected.sort(String.CASE_INSENSITIVE_ORDER.reversed());

                    } else {
                        assertCondition(
                                false,
                                "Invalid sort order value: '" + value + "'. Expected 'ascending' or 'descending'."
                        );
                        return String.join(", ", actual);
                    }

                    assertCondition(
                            actual.equals(expected), "Expected: " + expected + "\n" + "Actual  : " + actual
                    );

                    return String.join(", ", actual);
                }

                case "isvisible": {
                    // zero-cost if already visible; otherwise attempt wait up to timeout then check
                    long timeout = actionTimeoutMs();
                    boolean visibleNow = targetLocator.first().isVisible();
                    if (!visibleNow && timeout > 0) {
                        try {
                            targetLocator.first().waitFor(new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE)
                                    .setTimeout(timeout));
                        } catch (Exception ignored) {}
                    }
                    boolean isVisible = targetLocator.first().isVisible();
                    assertCondition(isVisible, "Element is not visible.");
                    return String.valueOf(isVisible);
                }

                case "isenabled": {
                    // do not poll; instant check only
                    waitIfNeededVisible(targetLocator);
                    boolean isEnabled = targetLocator.first().isEnabled();
                    assertCondition(isEnabled, "Element is not enabled.");
                    return String.valueOf(isEnabled);
                }

                case "ischecked":
                    waitIfNeededVisible(targetLocator);
                    boolean isChecked = targetLocator.first().isChecked();
                    assertCondition(isChecked, "Element is not checked.");
                    return String.valueOf(isChecked);

                case "isdisabled":
                    waitIfNeededVisible(targetLocator);
                    boolean isDisabled = targetLocator.first().isDisabled();
                    assertCondition(isDisabled, "Element is not disabled.");
                    return String.valueOf(isDisabled);

                case "ishidden": {
                    long timeout = actionTimeoutMs();
                    boolean hiddenNow = targetLocator.first().isHidden();
                    if (!hiddenNow && timeout > 0) {
                        try {
                            targetLocator.first().waitFor(new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.HIDDEN)
                                    .setTimeout(timeout));
                        } catch (Exception ignored) {}
                    }
                    boolean isHidden = targetLocator.first().isHidden();
                    assertCondition(isHidden, "Element is not hidden.");
                    return String.valueOf(isHidden);
                }

                case "hastext": {
                    waitIfNeededVisible(targetLocator);
                    String locatorText = targetLocator.first().textContent();
                    assertCondition(locatorText != null && locatorText.contains(value), "Text mismatch.");
                    return locatorText;
                }

                case "hasclass": {
                    waitIfNeededVisible(targetLocator);
                    String cls = targetLocator.first().getAttribute("class");
                    boolean hasClass = cls != null && cls.contains(value);
                    assertCondition(hasClass, "Class mismatch.");
                    return String.valueOf(hasClass);
                }

                case "hasequalvalue": {
                    waitIfNeededVisible(targetLocator);
                    String actualValue = targetLocator.first().inputValue();
                    assertCondition(actualValue.equals(value), "Value mismatch.");
                    return actualValue;
                }

                case "isempty": {
                    waitIfNeededVisible(targetLocator);
                    String inputValue = targetLocator.first().inputValue();
                    assertCondition(inputValue.isEmpty(), "Element is not empty.");
                    return inputValue;
                }

                case "waitfortext": {
                    waitIfNeededVisible(targetLocator);
                    String txt = targetLocator.first().textContent();
                    if (txt == null || !txt.contains(value)) {
                        throw new AssertionError("Text not found: " + value);
                    }
                    return txt;
                }

                case "waitforvalue": {
                    waitIfNeededVisible(targetLocator);
                    String val = targetLocator.first().inputValue();
                    if (!val.equals(value)) {
                        throw new AssertionError("Value mismatch.");
                    }
                    return val;
                }

                // =============== MULTI-ELEMENT ACTIONS (NO waits; keep behavior) ===============
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
            // Build a helpful debug message and rethrow as RuntimeException for upstream handling
            String debug = buildExactFailureMessage(page, action, value, targetLocator, e);
            logger.error(debug, e);
            throw new RuntimeException(debug, e);
        }
    }

    /**
     * Helper click method used by internal flows that need a synchronous click wrapper.
     * Applies the same smart clickable wait before performing the click.
     *
     * @param targetLocator locator to click
     */
    private void click(Locator targetLocator) {
        try {
            waitIfNeededClickable(targetLocator);
            targetLocator.first().click();
        } catch (Exception e) {
            // Log the concise error message and wrap into RuntimeException for callers
            logger.error("Error while clicking on target locator: {}", e.getMessage());
            throw new RuntimeException("Click action failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simple assertion helper that throws AssertionError with the supplied message when false.
     * Kept internal to centralize assertion behavior.
     *
     * @param condition boolean condition expected to be true
     * @param errorMessage message used for AssertionError if condition is false
     */
    private void assertCondition(boolean condition, String errorMessage) {
        if (!condition) {
            throw new AssertionError(errorMessage);
        }
    }

    /**
     * Public helper: waits for the first element matched by the locator to become visible.
     * - If already visible -> returns immediately (no wait cost).
     * - Otherwise waits up to actionTimeoutMs() for visibility, then returns or throws on failure.
     *
     * Useful in test scripts when you want to explicitly ensure an element is visible before continuing.
     *
     * @param locator Playwright locator
     */
    public void waitForLocator(Locator locator) {
        try {
            waitIfNeededVisible(locator);
        } catch (Exception e) {
            Page page = null;
            try { page = locator.page(); } catch (Exception ignored) {}
            String debug = buildExactFailureMessage(page, "waitForLocator", null, locator, e);
            logger.error(debug, e);
            throw new RuntimeException(debug, e);
        }
    }

    // ============================================================
    // EXACT FAILURE MESSAGE (ONLY BETTER OUTPUT)
    // ============================================================

    /**
     * Builds a detailed failure message including page URL/title, locator info, match count and the exception details.
     * This is intended to be more actionable for testers and developers when debugging flakiness.
     *
     * @param page Playwright page at the time of failure (may be null)
     * @param action action being executed when failure happened
     * @param value value passed for the action (may be null)
     * @param locator locator involved in the failure (may be null)
     * @param e exception that was thrown
     * @return detailed multi-line debug string
     */
    private String buildExactFailureMessage(Page page, String action, String value, Locator locator, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n========== PTAF FAILURE (EXACT WHY) ==========\n");
        sb.append("Action     : ").append(safe(action)).append("\n");
        sb.append("Value      : ").append(safe(value)).append("\n");

        try {
            if (page != null) {
                sb.append("URL        : ").append(safe(page.url())).append("\n");
                sb.append("Title      : ").append(safe(page.title())).append("\n");
            }
        } catch (Exception ignored) {}

        sb.append("Locator    : ").append(locatorToString(locator)).append("\n");
        try {
            if (locator != null) {
                int count = locator.count();
                sb.append("MatchCount : ").append(count).append("\n");
            }
        } catch (Exception ignored) {}

        sb.append("Exception  : ").append(e.getClass().getName()).append("\n");
        sb.append("Message    : ").append(safe(e.getMessage())).append("\n");
        sb.append("Full       : ").append(safe(e.toString())).append("\n");
        sb.append("=============================================\n");

        return sb.toString();
    }

    /**
     * Safely convert locator to string for logging; if locator.toString() fails, returns a fallback message.
     *
     * @param locator Playwright locator to stringify
     * @return string representation or informative fallback
     */
    private String locatorToString(Locator locator) {
        if (locator == null) return "null";
        try { return locator.toString(); }
        catch (Exception ex) { return "Locator(toString failed): " + ex.getMessage(); }
    }

    /**
     * Safe string helper: returns "null" for actual null values to avoid NPEs during logging.
     *
     * @param s input string
     * @return original string or literal "null" if input was null
     */
    private String safe(String s) {
        return s == null ? "null" : s;
    }
}
