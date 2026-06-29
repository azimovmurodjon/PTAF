package com.ptaf.ui.helpers;

import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility to locate element definitions from a YAML-backed configuration and to parse
 * locator "type" and "value" tokens used throughout the UI tests.
 *
 * <p>Responsibilities:
 * - Retrieve a specific element property from YAML using the pattern "elements.{element}.{key}".
 * - Provide clear, structured error messages and logging when lookup fails or returns null.
 * - Parse locator definitions that may be expressed as "TYPE_value", "TYPE value", or simply
 *   "TYPE" (where value may be absent).
 *
 * <p>This class does not modify any YAML; it only reads via YamlReader.get(fullKey).
 * Callers should handle exceptions thrown by YamlReader or this helper.
 */
public class ElementLocatorHelper {
    /**
     * SLF4J logger instance for structured logging of errors and diagnostics.
     */
    private static final Logger logger = LoggerFactory.getLogger(ElementLocatorHelper.class);

    /**
     * Retrieve an element property value from the YAML configuration.
     *
     * <p>The method constructs a full YAML path in the form:
     * "elements.{element}.{key}" and attempts to read it via YamlReader.get(fullKey).
     *
     * <p>Behavior:
     * - If the retrieved object is null, an error message is built and logged, and an
     *   IllegalArgumentException is thrown.
     * - If any exception occurs during retrieval, the exception is logged with a clean,
     *   structured message and then re-thrown to the caller.
     *
     * @param element the name of the element group/key under "elements" in the YAML
     * @param key the specific property key for the element (for example, "locator" or "type")
     * @return the String representation of the YAML value associated with the full path
     * @throws IllegalArgumentException if the YAML path exists but the value is null
     * @throws RuntimeException any exception thrown by YamlReader.get will be propagated
     */
    public String getElement(String element, String key) {
        // Build the YAML path in a standard predictable format
        String fullKey = "elements." + element + "." + key;

        try {
            // Attempt to read the raw value from YAML
            Object raw = YamlReader.get(fullKey);

            // If value is explicitly null, create a clear error message and fail fast.
            if (raw == null) {
                String msg = buildCleanYamlError(
                        "YAML LOCATOR NOT FOUND",
                        fullKey,
                        element,
                        key,
                        "Value is null (missing key or wrong path)"
                );
                // Log the structured error for easier debugging in CI logs
                logger.error(msg);
                // Throw an exception to surface the configuration problem to callers/testers
                throw new IllegalArgumentException("YAML value is null for " + fullKey);
            }

            // Return the YAML value as a String (preserves numeric/boolean textual representation)
            return String.valueOf(raw);

        } catch (Exception e) {
            // Any exception during YAML access is formatted cleanly and logged before rethrowing
            String msg = buildCleanYamlError(
                    "YAML LOCATOR FAILURE",
                    fullKey,
                    element,
                    key,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            );

            // Log message and stack trace to help diagnose underlying issues (IO, parse errors, etc.)
            logger.error(msg, e);
            // Re-throw the original exception so callers can decide how to handle it
            throw e;
        }
    }

    // =========================
    // Clean professional formatter (no ASCII boxes)
    // =========================

    /**
     * Build a concise, easy-to-read multi-line error message for YAML lookup failures.
     *
     * <p>This method is intentionally simple: it returns a multi-line string containing the title,
     * element, key, full path and the reason so logs are consistent and easily greppable.
     *
     * @param title short title indicating the nature of the error (e.g. "YAML LOCATOR FAILURE")
     * @param fullPath the complete YAML path that was attempted
     * @param element the element group attempted
     * @param key the specific key attempted
     * @param reason a short description/reason for the failure
     * @return a formatted multi-line string describing the error
     */
    private String buildCleanYamlError(String title,
                                       String fullPath,
                                       String element,
                                       String key,
                                       String reason) {

        StringBuilder sb = new StringBuilder();
        // Header with title to make it stand out in logs
        sb.append("\n========== ").append(title).append(" ==========\n");
        // Clear label/value pairs for easy visual scanning
        sb.append("Element   : ").append(element).append("\n");
        sb.append("Key       : ").append(key).append("\n");
        sb.append("FullPath  : ").append(fullPath).append("\n");
        sb.append("Reason    : ").append(reason).append("\n");
        sb.append("============================================\n");
        return sb.toString();
    }

    // =========================
    // Original logic (unchanged)
    // =========================

    /**
     * Extract the locator type token from a combined locator string.
     *
     * <p>Supported input formats (examples):
     * - "CSS_.my-class" -> returns "CSS"
     * - "xpath //div[1]" -> returns "xpath"
     * - "id" -> returns "id" (no separator present)
     *
     * <p>Rules:
     * - Trims leading/trailing whitespace before processing.
     * - Recognizes two separators: underscore '_' and space ' '. If both appear, the earliest
     *   separator (closest to the start) is used.
     * - If no separator exists, the whole token is treated as the type.
     *
     * @param part the combined locator string (may be null)
     * @return the locator type in the original case (empty string if input null or empty)
     */
    public String getLocatorType(String part) {
        if (part == null) return "";
        // Normalize whitespace
        String token = part.trim();

        // Find indexes of possible separators
        int us = token.indexOf('_'); // underscore separator index (-1 if not present)
        int sp = token.indexOf(' '); // space separator index (-1 if not present)

        // If both separators exist, use the earliest one; otherwise pick whichever exists (or -1)
        int sep = (us >= 0 && sp >= 0) ? Math.min(us, sp) : Math.max(us, sp);

        // If a separator was found, return substring before it; otherwise return entire token
        return (sep >= 0 ? token.substring(0, sep) : token).trim();
    }

    /**
     * Extract the locator value from a combined locator string.
     *
     * <p>Supported input formats (examples):
     * - "CSS_.my-class" -> returns ".my-class"
     * - "xpath //div[1]" -> returns "//div[1]"
     * - "id" -> returns "" (no explicit value provided)
     *
     * <p>Notes:
     * - If no separator ('_' or ' ') is present, this method returns an empty string. This
     *   supports the notion of an "unnamed role" where only type is provided.
     *
     * @param part the combined locator string (may be null)
     * @return the locator value portion (trimmed) or empty string if none
     */
    public String getLocator(String part) {
        if (part == null) return "";
        // Trim leading/trailing whitespace for robust parsing
        String token = part.trim();

        // Determine positions of separators '_' and ' '
        int us = token.indexOf('_');
        int sp = token.indexOf(' ');

        // Choose the earliest separator if both are present, otherwise pick the one that exists
        int sep = (us >= 0 && sp >= 0) ? Math.min(us, sp) : Math.max(us, sp);

        // If no separator found, there is no explicit value portion
        if (sep < 0) return "";

        // Return everything after the separator, trimmed of whitespace
        return token.substring(sep + 1).trim();
    }

    /**
     * Determine whether the given locator string contains an explicit value portion.
     *
     * <p>Returns true if either an underscore '_' or a space ' ' is present anywhere in the string.
     *
     * @param part the locator string to check (may be null)
     * @return true if an explicit value separator exists, false otherwise (including null input)
     */
    public boolean hasExplicitValue(String part) {
        if (part == null) return false;
        // If either separator appears, an explicit value exists
        return part.indexOf('_') >= 0 || part.indexOf(' ') >= 0;
    }

    /**
     * Split the provided locator string into a two-element array: [type, value].
     *
     * <p>This is a convenience method that delegates to getLocatorType and getLocator.
     * If the input is just a type, the returned array will contain the type and an empty string
     * as the value.
     *
     * @param part the combined locator string (may be null)
     * @return a String array of length 2: index 0 = type, index 1 = value (possibly empty)
     */
    public String[] splitTypeAndValue(String part) {
        String type = getLocatorType(part);
        String value = getLocator(part);
        return new String[]{type, value};
    }
}
