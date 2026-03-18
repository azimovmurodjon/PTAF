package com.ptaf.ui.helpers;

import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementLocatorHelper {
    private static final Logger logger = LoggerFactory.getLogger(ElementLocatorHelper.class);

    public String getElement(String element, String key) {
        String fullKey = "elements." + element + "." + key;

        try {
            Object raw = YamlReader.get(fullKey);
            if (raw == null) {
                String msg = buildCleanYamlError(
                        "YAML LOCATOR NOT FOUND",
                        fullKey,
                        element,
                        key,
                        "Value is null (missing key or wrong path)"
                );
                logger.error(msg);
                throw new IllegalArgumentException("YAML value is null for " + fullKey);
            }

            return String.valueOf(raw);

        } catch (Exception e) {
            String msg = buildCleanYamlError(
                    "YAML LOCATOR FAILURE",
                    fullKey,
                    element,
                    key,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            );

            logger.error(msg, e);
            throw e;
        }
    }

    // =========================
    // Clean professional formatter (no ASCII boxes)
    // =========================
    private String buildCleanYamlError(String title,
                                       String fullPath,
                                       String element,
                                       String key,
                                       String reason) {

        StringBuilder sb = new StringBuilder();
        sb.append("\n========== ").append(title).append(" ==========\n");
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

    /** TYPE from "TYPE_value" or "TYPE value" or just "TYPE" */
    public String getLocatorType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int us = token.indexOf('_');
        int sp = token.indexOf(' ');
        int sep = (us >= 0 && sp >= 0) ? Math.min(us, sp) : Math.max(us, sp);
        return (sep >= 0 ? token.substring(0, sep) : token).trim();
    }

    /** VALUE from "TYPE_value" or "TYPE value"; empty if none (unnamed role) */
    public String getLocator(String part) {
        if (part == null) return "";
        String token = part.trim();
        int us = token.indexOf('_');
        int sp = token.indexOf(' ');
        int sep = (us >= 0 && sp >= 0) ? Math.min(us, sp) : Math.max(us, sp);
        if (sep < 0) return "";
        return token.substring(sep + 1).trim();
    }

    public boolean hasExplicitValue(String part) {
        if (part == null) return false;
        return part.indexOf('_') >= 0 || part.indexOf(' ') >= 0;
    }

    public String[] splitTypeAndValue(String part) {
        String type = getLocatorType(part);
        String value = getLocator(part);
        return new String[]{type, value};
    }
}