package com.ptaf.ui.action_performer;

/**
 * Final failure surfaced to Cucumber/TestNG after UI evidence capture and cleanup.
 *
 * <p>The original Playwright or assertion throwable is preserved as this exception's cause,
 * so console output, Cucumber JSON, Extent reports, and per-feature reports all retain the
 * actual reason for failure instead of only a generic action message.</p>
 */
public class UiActionFailureException extends RuntimeException {

    public UiActionFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
