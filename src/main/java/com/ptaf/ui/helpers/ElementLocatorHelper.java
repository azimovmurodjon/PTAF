package com.ptaf.ui.helpers;

import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ElementLocatorHelper
 *
 * Responsibilities:
 *  - Fetch element strings from YAML (elements.{element}.{key}).
 *  - Parse a chain segment token into {type, value}, e.g.:
 *      "Button_Save" -> type=Button, value=Save
 *      "Button"      -> type=Button, value=""
 *
 * Backward compatible with existing "TYPE_value" usage and adds support for
 * name-optional segments like "Button", "ROW", "CELL", etc.
 */
public class ElementLocatorHelper {
    private static final Logger logger = LoggerFactory.getLogger(ElementLocatorHelper.class);

    /**
     * Returns the raw element string for the given YAML path:
     * elements.{element}.{key}
     * Example YAML:
     * elements:
     *   OrdersPage:
     *     rowFirstButton: "ROW_Order Row #42 > Button"
     */
    public String getElement(String element, String key) {
        try {
            Object raw = YamlReader.get("elements." + element + "." + key);
            if (raw == null) {
                throw new IllegalArgumentException("YAML value is null for elements." + element + "." + key);
            }
            return String.valueOf(raw);
        } catch (Exception e) {
            logger.error("Failed to retrieve YAML for elements.{}.{}: {}", element, key, e.getMessage());
            throw e;
        }
    }

    /**
     * Extracts the locator TYPE from a segment token.
     * Examples:
     *  - "Button_Save"  -> "Button"
     *  - "ROW_Order #1" -> "ROW"
     *  - "Button"       -> "Button" (no value provided)
     */
    public String getLocatorType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        String type = (idx >= 0) ? token.substring(0, idx) : token;
        return type.trim();
    }

    /**
     * Extracts the locator VALUE from a segment token.
     * Examples:
     *  - "Button_Save"  -> "Save"
     *  - "ROW_Order #1" -> "Order #1"
     *  - "Button"       -> "" (empty, meaning unnamed role)
     *
     * NOTE: Returning "" for the no-underscore case is intentional — it allows
     * LocatorHandler to treat this as an unnamed role (e.g., getByRole(ROLE) w/o name).
     */
    public String getLocator(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        if (idx < 0) {
            // No underscore => no explicit value (new style)
            return "";
        }
        // Everything after the first underscore is the value (keeps any additional underscores in value)
        String value = token.substring(idx + 1);
        return value.trim();
    }

    // -------- Optional helpers (not required by existing code, but handy) --------

    /** Returns true if the token has an explicit value (contains underscore). */
    public boolean hasExplicitValue(String part) {
        if (part == null) return false;
        return part.indexOf('_') >= 0;
    }

    /** Canonical split: returns {type, value}. If no value, returns empty string as value. */
    public String[] splitTypeAndValue(String part) {
        String type = getLocatorType(part);
        String value = getLocator(part);
        return new String[]{type, value};
    }
}