package com.ptaf.utils;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotType;
import com.ptaf.ui.action_performer.ElementActionImpl;
import com.ptaf.ui.interfaces.ElementAction;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * ScenarioUtil is a utility class that provides methods for managing
 * scenarios during test execution, particularly focusing on teardown processes.
 * This class offers functionality to capture and attach screenshots
 * to scenario reports in cases of test failures or specific conditions.
 */
public class ScenarioUtil {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioUtil.class);

    /**
     * Handles the teardown process for the given scenario.
     * This method attempts to capture a full-page screenshot
     * and attaches it to the scenario report, allowing for better
     * debugging in the event of test failures.
     *
     * @param scenario The Cucumber Scenario object providing context for logging
     *                 and information about the test case being executed.
     * @param page     The Playwright Page instance from which to capture the screenshot.
     * @param status   A string representing the status of the scenario (e.g., "failed", "passed"),
     *                 used for tagging the screenshot.
     */
    public static void handleScenarioTeardown(Scenario scenario, Page page, String status) {
        try {
            byte[] screenshot = page.locator("body").screenshot(new Locator.ScreenshotOptions()
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));
            scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());
            logger.info("Screenshot taken for {} scenario: {}", status, scenario.getName());
        } catch (Exception e) {
            logger.error("Error taking screenshot: {}", e.getMessage(), e);
        }
    }

    /**
     * Smaller/faster viewport screenshot attachment.
     */
    public static void handleScenarioTeardownFailier(Scenario scenario, Page page, String status) {
        try {
            byte[] screenshot = page.locator("body").screenshot(new Locator.ScreenshotOptions()
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));
            scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());
            logger.info("Screenshot captured and embedded for {} scenario: {}", status, scenario.getName());
        } catch (Exception e) {
            logger.error("Error capturing screenshot for scenario '{}': {}", scenario.getName(), e.getMessage(), e);
        }
    }

    /**
     * Element/iframe-targeted screenshot attachment.
     */
    public static void handleScenarioTeardownLocator(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String status) {
        ElementAction elementAction = new ElementActionImpl(page);
        try {
            byte[] screenshot = null;

            if (iFrame == null) {
                screenshot = page.locator(targetLocator).first().screenshot(new Locator.ScreenshotOptions()
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));
            } else if (iFrame != null && iFrame_2 == null && iFrame_3 == null) {
                screenshot = page.frameLocator(iFrame).locator(targetLocator).first().screenshot(new Locator.ScreenshotOptions()
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 == null) {
                screenshot = page.frameLocator(iFrame).frameLocator(iFrame_2).locator(targetLocator).first().screenshot(new Locator.ScreenshotOptions()
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 != null) {
                screenshot = page.frameLocator(iFrame).frameLocator(iFrame_2).frameLocator(iFrame_3).locator(targetLocator).first().screenshot(new Locator.ScreenshotOptions()
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));
            }

            if (screenshot != null) {
                scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());
                logger.info("Screenshot taken for {} scenario: {}", status, scenario.getName());
            } else {
                logger.warn("No screenshot captured for scenario: {}", scenario.getName());
            }

        } catch (Exception e) {
            logger.error("Error taking screenshot: {}", e.getMessage(), e);
        }
    }

    public static void reportAllDropdownOptionsMultiline(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String dropdownLocator) {
        try {
            Locator dropdown;

            if (iFrame == null) {
                page.waitForSelector(dropdownLocator, new Page.WaitForSelectorOptions().setTimeout(10000));
                dropdown = page.locator(dropdownLocator).first();
            } else if (iFrame != null && iFrame_2 == null && iFrame_3 == null) {
                FrameLocator frame = page.frameLocator(iFrame);
                frame.locator(dropdownLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                dropdown = frame.locator(dropdownLocator).first();
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 == null) {
                FrameLocator frame = page.frameLocator(iFrame).frameLocator(iFrame_2);
                frame.locator(dropdownLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                dropdown = frame.locator(dropdownLocator).first();
            } else {
                FrameLocator frame = page.frameLocator(iFrame).frameLocator(iFrame_2).frameLocator(iFrame_3);
                frame.locator(dropdownLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                dropdown = frame.locator(dropdownLocator).first();
            }

            dropdown.scrollIntoViewIfNeeded();

            byte[] screenshot = dropdown.screenshot(new Locator.ScreenshotOptions()
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));

            scenario.attach(screenshot, "image/jpeg", "Dropdown Screenshot: " + dropdownLocator);

            List<String> allOptions = dropdown.locator("option").allTextContents();

            if (allOptions.isEmpty()) {
                scenario.log("⚠️ No options found in dropdown: " + dropdownLocator);
            } else {
                scenario.log("All options from dropdown '" + dropdownLocator + "':");
                for (String option : allOptions) {
                    scenario.log("- " + option);
                }
            }

            logger.info("All dropdown options logged for scenario '{}': {}", scenario.getName(), allOptions);

        } catch (Exception e) {
            logger.error("Error capturing dropdown options or screenshot: {}", e.getMessage(), e);
            scenario.log("⚠️ Failed to capture dropdown or options due to: " + e.getMessage());
        }
    }

    // =====================================================================
    //                      STRING VALUE REPORTER (EXISTING ADD)
    // =====================================================================

    /** Attach and log any arbitrary string value. */
    public static void reportString(Scenario scenario, String title, String value) {
        try {
            String nonNullValue = (value == null) ? "<null>" : value;
            scenario.attach(nonNullValue.getBytes(StandardCharsets.UTF_8), "text/plain", title);
            scenario.log( title + ": " + nonNullValue);
            logger.info("String reported [{}]: {}", title, truncateForLog(nonNullValue));
        } catch (Exception e) {
            logger.error("Error attaching string value [{}]: {}", title, e.getMessage(), e);
            scenario.log("⚠️ Failed to attach string '" + title + "': " + e.getMessage());
        }
    }

    /**
     * Capture a string value from a target element, respecting up to three nested iframes.
     * - If the element is an input/textarea, returns its inputValue().
     * - Otherwise returns innerText() trimmed and normalized.
     *
     * @return captured string (never null; "<empty>" if blank, "<not-found>" if element missing)
     */
    public static String captureElementString(Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator) {
        try {
            Locator target;
            if (iFrame == null) {
                page.waitForSelector(targetLocator, new Page.WaitForSelectorOptions().setTimeout(10000));
                target = page.locator(targetLocator).first();
            } else if (iFrame != null && iFrame_2 == null && iFrame_3 == null) {
                FrameLocator f1 = page.frameLocator(iFrame);
                f1.locator(targetLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                target = f1.locator(targetLocator).first();
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 == null) {
                FrameLocator f2 = page.frameLocator(iFrame).frameLocator(iFrame_2);
                f2.locator(targetLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                target = f2.locator(targetLocator).first();
            } else {
                FrameLocator f3 = page.frameLocator(iFrame).frameLocator(iFrame_2).frameLocator(iFrame_3);
                f3.locator(targetLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                target = f3.locator(targetLocator).first();
            }

            boolean isEditable = false;
            try {
                isEditable = (boolean) target.evaluate("el => el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement");
            } catch (Exception ignored) { }

            String raw;
            if (isEditable) {
                try {
                    raw = target.inputValue();
                } catch (Exception e) {
                    raw = safeInnerText(target);
                }
            } else {
                raw = safeInnerText(target);
            }

            if (raw == null) return "<not-found>";
            String normalized = normalizeSpaces(raw.trim());
            return normalized.isEmpty() ? "<empty>" : normalized;

        } catch (Exception e) {
            logger.error("Error capturing element string from '{}': {}", targetLocator, e.getMessage(), e);
            return "<not-found>";
        }
    }

    /** Convenience: capture + attach/log a string value from an element (page/frames aware). */
    public static void reportElementString(Scenario scenario, Page page,
                                           String iFrame, String iFrame_2, String iFrame_3,
                                           String targetLocator, String label) {
        String value = captureElementString(page, iFrame, iFrame_2, iFrame_3, targetLocator);
        reportString(scenario, "Value: " + label, value);
    }

    // =====================================================================
    //                       NEW: STRUCTURED REPORTERS
    // =====================================================================

    /** Attach JSON (auto pretty-prints safely; falls back to raw if needed). */
    public static void reportJson(Scenario scenario, String title, String json) {
        try {
            String payload = (json == null) ? "null" : json;
            String pretty = safePrettyJson(payload);
            scenario.attach(pretty.getBytes(StandardCharsets.UTF_8), "application/json", title);
            scenario.log("📄 JSON attached: " + title);
            logger.info("JSON reported [{}], size={} chars", title, pretty.length());
        } catch (Exception e) {
            logger.error("Error attaching JSON [{}]: {}", title, e.getMessage(), e);
            scenario.log("⚠️ Failed to attach JSON '" + title + "': " + e.getMessage());
        }
    }

    /** Attach a simple one-column table rendered as numbered lines (for lists, options, etc.). */
    public static void reportTable(Scenario scenario, String title, List<String> rows) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(title == null ? "Table" : title).append(":\n");
            if (rows == null || rows.isEmpty()) {
                sb.append("<empty>\n");
            } else {
                for (int i = 0; i < rows.size(); i++) {
                    sb.append(i + 1).append(". ").append(rows.get(i)).append("\n");
                }
            }
            reportString(scenario, title == null ? "Table" : title, sb.toString());
        } catch (Exception e) {
            logger.error("Error attaching table [{}]: {}", title, e.getMessage(), e);
            scenario.log("⚠️ Failed to attach table '" + title + "': " + e.getMessage());
        }
    }

    /** Attach key–value pairs (configs, env, headers). */
    public static void reportKeyValues(Scenario scenario, String title, Map<String, ?> map) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(title == null ? "KeyValues" : title).append(":\n");
            if (map == null || map.isEmpty()) {
                sb.append("<empty>\n");
            } else {
                for (Map.Entry<String, ?> e : map.entrySet()) {
                    sb.append(e.getKey()).append(" = ").append(String.valueOf(e.getValue())).append("\n");
                }
            }
            reportString(scenario, title == null ? "KeyValues" : title, sb.toString());
        } catch (Exception e) {
            logger.error("Error attaching key-values [{}]: {}", title, e.getMessage(), e);
            scenario.log("⚠️ Failed to attach key-values '" + title + "': " + e.getMessage());
        }
    }

    /**
     * Value + Screenshot combo: captures element text/value AND its screenshot.
     * Works with pages and up to three nested frames, consistent with existing patterns.
     */
    public static void reportElementStringWithScreenshot(Scenario scenario, Page page,
                                                         String iFrame, String iFrame_2, String iFrame_3,
                                                         String targetLocator, String label) {
        try {
            // 1) Capture value
            String value = captureElementString(page, iFrame, iFrame_2, iFrame_3, targetLocator);
            reportString(scenario, "Value: " + (label == null ? targetLocator : label), value);

            // 2) Capture screenshot of the specific element
            Locator target;
            if (iFrame == null) {
                page.waitForSelector(targetLocator, new Page.WaitForSelectorOptions().setTimeout(10000));
                target = page.locator(targetLocator).first();
            } else if (iFrame != null && iFrame_2 == null && iFrame_3 == null) {
                FrameLocator f1 = page.frameLocator(iFrame);
                f1.locator(targetLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                target = f1.locator(targetLocator).first();
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 == null) {
                FrameLocator f2 = page.frameLocator(iFrame).frameLocator(iFrame_2);
                f2.locator(targetLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                target = f2.locator(targetLocator).first();
            } else {
                FrameLocator f3 = page.frameLocator(iFrame).frameLocator(iFrame_2).frameLocator(iFrame_3);
                f3.locator(targetLocator).waitFor(new Locator.WaitForOptions().setTimeout(10000));
                target = f3.locator(targetLocator).first();
            }

            byte[] screenshot = target.screenshot(new Locator.ScreenshotOptions()
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));

            // NOTE: Keep content-type consistent with your report usage.
            scenario.attach(screenshot, "image/png",
                    "Element Screenshot: " + (label == null ? targetLocator : label));
            logger.info("Reported value+image for '{}'", label == null ? targetLocator : label);

        } catch (Exception e) {
            logger.error("Error in reportElementStringWithScreenshot('{}'): {}", label, e.getMessage(), e);
            scenario.log("⚠️ Failed to attach value+screenshot for '" + (label == null ? targetLocator : label) + "': " + e.getMessage());
        }
    }

    // ----------------------- helpers -----------------------

    private static String safeInnerText(Locator locator) {
        try {
            String inner = locator.innerText();
            return (inner == null) ? "" : inner;
        } catch (Exception e) {
            try {
                String text = locator.textContent();
                return (text == null) ? "" : text;
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private static String normalizeSpaces(String s) {
        return s.replaceAll("[\\u00A0\\s]+", " "); // collapse whitespace & non-breaking spaces
    }

    private static String truncateForLog(String s) {
        final int max = 500;
        if (s == null) return "null";
        return (s.length() <= max) ? s : s.substring(0, max) + "…";
    }

    /** Minimal, dependency-free JSON pretty printer (tolerant; ignores quoting escapes). */
    private static String safePrettyJson(String input) {
        if (input == null) return "null";
        String s = input.trim();
        if (s.isEmpty()) return "<empty>";

        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    out.append(c);
                    // toggle if not escaped
                    boolean escaped = (i > 0 && s.charAt(i - 1) == '\\');
                    if (!escaped) inQuotes = !inQuotes;
                    break;
                case '{':
                case '[':
                    out.append(c);
                    if (!inQuotes) {
                        out.append('\n');
                        indent++;
                        appendIndent(out, indent);
                    }
                    break;
                case '}':
                case ']':
                    if (!inQuotes) {
                        out.append('\n');
                        indent = Math.max(0, indent - 1);
                        appendIndent(out, indent);
                        out.append(c);
                    } else {
                        out.append(c);
                    }
                    break;
                case ',':
                    out.append(c);
                    if (!inQuotes) {
                        out.append('\n');
                        appendIndent(out, indent);
                    }
                    break;
                case ':':
                    if (!inQuotes) {
                        out.append(": ");
                    } else {
                        out.append(c);
                    }
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    private static void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) sb.append("  ");
    }
}