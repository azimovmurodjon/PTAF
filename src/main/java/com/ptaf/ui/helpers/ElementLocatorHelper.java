package com.ptaf.ui.helpers;

import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementLocatorHelper {
    private static final Logger logger = LoggerFactory.getLogger(ElementLocatorHelper.class);

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