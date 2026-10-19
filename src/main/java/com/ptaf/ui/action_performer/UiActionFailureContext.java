package com.ptaf.ui.action_performer;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Holds the most recent handled UI action failure for the current test thread.
 *
 * <p>The existing {@code ElementAction} public API returns {@code false} or {@code null}
 * for handled action failures. This context preserves the original throwable and actionable
 * diagnostic details until the page or frame facade creates the final Cucumber failure.
 * It prevents useful Playwright details from being replaced by a generic message while
 * preserving the established boolean/string return contracts.</p>
 *
 * <p>The stored failure contains only string snapshots of page and locator details. It does
 * not retain live Playwright objects after a failed action, so it remains safe when a page or
 * browser is closed during normal failure cleanup.</p>
 */
public final class UiActionFailureContext {

    private static final ThreadLocal<Failure> LAST_FAILURE = new ThreadLocal<>();

    private UiActionFailureContext() {
        throw new IllegalStateException("Utility class");
    }

    /** Clears any previous failure for the current action thread. */
    public static void clear() {
        LAST_FAILURE.remove();
    }

    /**
     * Stores a failure with the complete framework action context.
     *
     * @param action action name requested by the caller
     * @param element logical YAML element name
     * @param locatorKey YAML locator key
     * @param value action value when safe to display
     * @param page current page, if available
     * @param locator resolved Playwright locator, if available
     * @param frameContext readable iframe path, if used
     * @param cause original failure
     */
    public static void record(
            String action,
            String element,
            String locatorKey,
            String value,
            Page page,
            Locator locator,
            String frameContext,
            Throwable cause) {
        LAST_FAILURE.set(create(action, element, locatorKey, value, page, locator, frameContext, cause));
    }

    /**
     * Creates a safe diagnostic snapshot without retaining a live Page or Locator.
     *
     * @return immutable failure data that remains valid after browser cleanup
     */
    public static Failure create(
            String action,
            String element,
            String locatorKey,
            String value,
            Page page,
            Locator locator,
            String frameContext,
            Throwable cause) {
        boolean sensitive = isSensitive(element, locatorKey);
        return new Failure(
                action,
                element,
                locatorKey,
                value,
                frameContext,
                pageUrl(page),
                pageTitle(page),
                locatorText(locator),
                locatorMatchCount(locator),
                sensitive ? redactedCause(cause) : cause);
    }

    /**
     * Returns and clears the latest failure on the current thread.
     *
     * @return failure details, or {@code null} when no handled failure was recorded
     */
    public static Failure take() {
        Failure failure = LAST_FAILURE.get();
        LAST_FAILURE.remove();
        return failure;
    }

    /**
     * Returns the current failure without clearing it.
     * This is used only for adapter logging before the page or frame facade consumes it.
     */
    public static Failure peek() {
        return LAST_FAILURE.get();
    }

    /**
     * Builds one safe, complete diagnostic message for a failed UI action.
     *
     * <p>The message is used both in the console and as the Cucumber/Extent failure text.
     * It preserves the original cause separately, and it redacts action values for locator
     * keys or logical element names that indicate credentials, tokens, or secrets.</p>
     *
     * @param failure failure captured by the element-action adapter
     * @return readable diagnostic text suitable for a test report
     */
    public static String describe(Failure failure) {
        if (failure == null) {
            return "PTAF UI action failed, but no lower-level failure context was available.";
        }

        StringBuilder message = new StringBuilder();
        message.append("\n========== FNB-ETAF UI ACTION FAILURE ==========").append('\n');
        message.append("Action       : ").append(safe(failure.action())).append('\n');
        message.append("Element      : ").append(safe(failure.element())).append('\n');
        message.append("Locator key  : ").append(safe(failure.locatorKey())).append('\n');
        message.append("Input value  : ").append(formatValue(failure.value(), isSensitive(failure))).append('\n');
        message.append("Frame path   : ").append(safe(failure.frameContext())).append('\n');
        message.append("URL          : ").append(safe(failure.pageUrl())).append('\n');
        message.append("Title        : ").append(safe(failure.pageTitle())).append('\n');
        message.append("Locator      : ").append(safe(failure.locatorDescription())).append('\n');
        message.append("Match count  : ").append(safe(failure.locatorMatchCount())).append('\n');

        Throwable cause = failure.cause();
        message.append("Cause type   : ").append(cause == null ? "null" : cause.getClass().getName()).append('\n');
        message.append("Cause message: ").append(cause == null ? "null" : safe(cause.getMessage())).append('\n');
        message.append("==================================================");
        return message.toString();
    }

    /** Creates a final Cucumber/TestNG exception while preserving the original cause. */
    public static UiActionFailureException toException(Failure failure) {
        return new UiActionFailureException(describe(failure), failure == null ? null : failure.cause());
    }

    private static boolean isSensitive(Failure failure) {
        return isSensitive(failure.element(), failure.locatorKey());
    }

    private static boolean isSensitive(String element, String locatorKey) {
        String identifiers = (safe(element) + " " + safe(locatorKey)).toLowerCase();
        return identifiers.contains("password")
                || identifiers.contains("passwd")
                || identifiers.contains("secret")
                || identifiers.contains("token")
                || identifiers.contains("credential")
                || identifiers.contains("authorization")
                || identifiers.contains("auth");
    }

    private static Throwable redactedCause(Throwable cause) {
        if (cause == null) {
            return null;
        }
        return new IllegalStateException(
                "A sensitive UI action failed. Original exception type: " + cause.getClass().getName()
                        + ". The original message was redacted from framework console/report diagnostics.");
    }

    private static String formatValue(String value, boolean sensitive) {
        if (value == null) {
            return "null";
        }
        if (sensitive) {
            return "[REDACTED; length=" + value.length() + "]";
        }
        return "[" + value.replace("\n", "\\n").replace("\r", "\\r") + "] (length=" + value.length() + ")";
    }

    private static String locatorText(Locator locator) {
        if (locator == null) {
            return "unavailable (locator was not resolved)";
        }
        try {
            return locator.toString();
        } catch (Exception exception) {
            return "unavailable (" + safe(exception.getMessage()) + ")";
        }
    }

    private static String locatorMatchCount(Locator locator) {
        if (locator == null) {
            return "unavailable (locator was not resolved)";
        }
        try {
            return String.valueOf(locator.count());
        } catch (Exception exception) {
            return "unavailable (" + safe(exception.getMessage()) + ")";
        }
    }

    private static String pageUrl(Page page) {
        if (page == null) {
            return "unavailable (page was not supplied)";
        }
        try {
            return page.isClosed() ? "unavailable (page is closed)" : safe(page.url());
        } catch (Exception exception) {
            return "unavailable (" + safe(exception.getMessage()) + ")";
        }
    }

    private static String pageTitle(Page page) {
        if (page == null) {
            return "unavailable (page was not supplied)";
        }
        try {
            return page.isClosed() ? "unavailable (page is closed)" : safe(page.title());
        } catch (Exception exception) {
            return "unavailable (" + safe(exception.getMessage()) + ")";
        }
    }

    private static String safe(String value) {
        return value == null ? "null" : value;
    }

    /** Immutable diagnostic data retained between the element adapter and page/frame facade. */
    public record Failure(
            String action,
            String element,
            String locatorKey,
            String value,
            String frameContext,
            String pageUrl,
            String pageTitle,
            String locatorDescription,
            String locatorMatchCount,
            Throwable cause) {
    }
}
