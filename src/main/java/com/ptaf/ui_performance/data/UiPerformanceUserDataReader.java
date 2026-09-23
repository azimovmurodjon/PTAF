package com.ptaf.ui_performance.data;

import com.ptaf.ui_performance.model.UiPerformanceUser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads isolated UI performance user data from a simple UTF-8 CSV resource.
 *
 * <p>The first column must be {@code user_id}. The remaining header names become data keys that can
 * be referenced by journey steps using {@code ${column_name}}. Values are never logged or written to reports.</p>
 */
public final class UiPerformanceUserDataReader {
    private UiPerformanceUserDataReader() {
        throw new IllegalStateException("Utility class");
    }

    /** Loads users from the requested classpath CSV file. Quoted comma-containing values are intentionally unsupported. */
    public static List<UiPerformanceUser> readUsers(String classpathResource) {
        if (classpathResource == null || classpathResource.isBlank()) {
            throw new IllegalArgumentException("UI performance user CSV path cannot be blank.");
        }

        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource);
             BufferedReader reader = stream == null ? null : new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            if (reader == null) {
                throw new IllegalStateException("UI performance user CSV is not available on the classpath: " + classpathResource);
            }

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalStateException("UI performance user CSV has no header row: " + classpathResource);
            }
            String[] headers = split(headerLine);
            for (int index = 0; index < headers.length; index++) {
                headers[index] = headers[index].trim();
            }
            validateHeaders(headers, classpathResource);

            List<UiPerformanceUser> users = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank() || line.trim().startsWith("#")) {
                    continue;
                }
                String[] cells = split(line);
                if (cells.length != headers.length) {
                    throw new IllegalArgumentException("UI performance user CSV row " + rowNumber + " does not match the header column count.");
                }
                Map<String, String> fields = new LinkedHashMap<>();
                for (int index = 0; index < headers.length; index++) {
                    fields.put(headers[index], cells[index].trim());
                }
                users.add(new UiPerformanceUser(fields.remove(headers[0]), fields));
            }
            if (users.isEmpty()) {
                throw new IllegalStateException("UI performance user CSV contains no user rows: " + classpathResource);
            }
            return List.copyOf(users);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unable to read UI performance user CSV: " + classpathResource, exception);
        }
    }

    /**
     * Creates non-sensitive virtual-user identities for journeys that require no CSV fields.
     * Each user has an empty value map and remains independently identifiable in reports.
     */
    public static List<UiPerformanceUser> createSyntheticUsers(int userCount) {
        if (userCount < 1) {
            throw new IllegalArgumentException("UI performance synthetic user count must be at least 1.");
        }
        List<UiPerformanceUser> users = new ArrayList<>();
        for (int index = 1; index <= userCount; index++) {
            users.add(new UiPerformanceUser(String.format("virtual_user_%03d", index), Map.of()));
        }
        return List.copyOf(users);
    }

    private static String[] split(String line) {
        return line.split(",", -1);
    }

    private static void validateHeaders(String[] headers, String path) {
        if (headers.length < 2 || !"user_id".equalsIgnoreCase(headers[0].trim())) {
            throw new IllegalArgumentException("UI performance user CSV must begin with user_id and contain at least one data column: " + path);
        }
        for (String header : headers) {
            if (header == null || header.trim().isBlank()) {
                throw new IllegalArgumentException("UI performance user CSV contains an empty header: " + path);
            }
        }
    }
}
