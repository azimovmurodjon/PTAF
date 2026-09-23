package com.ptaf.ui_performance.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One data identity assigned to a virtual browser user.
 *
 * <p>The record stores data only in memory. Its {@link #toString()} method intentionally exposes
 * only the user identifier and available field names, never usernames, passwords, tokens, or other values.</p>
 */
public final class UiPerformanceUser {
    private final String id;
    private final Map<String, String> values;

    public UiPerformanceUser(String id, Map<String, String> values) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("UI performance user id cannot be blank.");
        }
        this.id = id.trim();
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public String getId() {
        return id;
    }

    public String getRequiredValue(String fieldName) {
        String value = values.get(fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required UI performance data field is missing for user id " + id + ": " + fieldName);
        }
        return value;
    }

    public Map<String, String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "UiPerformanceUser{id='" + id + "', fields=" + values.keySet() + "}";
    }
}
