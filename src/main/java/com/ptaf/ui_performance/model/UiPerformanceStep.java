package com.ptaf.ui_performance.model;

/**
 * One intentionally small browser action in an isolated UI performance journey.
 *
 * <p>Selectors are resolved from the separate UI performance locator repository before the step is
 * created. Values may contain {@code ${column_name}} placeholders that are resolved in memory for
 * the assigned virtual user and never written to reports.</p>
 */
public record UiPerformanceStep(String name, Action action, String target, String valueTemplate) {
    public enum Action {
        NAVIGATE,
        FILL,
        SELECT_OPTION,
        CLICK,
        CLICK_AND_SWITCH_TO_POPUP,
        VERIFY_VISIBLE
    }

    public UiPerformanceStep {
        if (name == null || name.isBlank() || action == null || target == null || target.isBlank()) {
            throw new IllegalArgumentException("UI performance steps require a name, action, and target.");
        }
    }
}
