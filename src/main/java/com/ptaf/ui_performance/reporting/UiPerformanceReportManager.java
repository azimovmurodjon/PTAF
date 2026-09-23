package com.ptaf.ui_performance.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Creates a dedicated, timestamped report directory for each UI performance execution. */
public final class UiPerformanceReportManager {
    private static final DateTimeFormatter RUN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Creates the report directory and its failure-evidence child directory. */
    public Path createRunDirectory(String outputDirectory, String journeyName) {
        String safeJourneyName = sanitize(journeyName);
        Path directory = Path.of(outputDirectory).resolve(safeJourneyName + "_" + LocalDateTime.now().format(RUN_TIMESTAMP));
        try {
            Files.createDirectories(directory.resolve("failed-screenshots"));
            Files.createDirectories(directory.resolve("browser-console-errors"));
            return directory;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create UI performance report directory: " + directory, exception);
        }
    }

    /** Converts a journey name to a safe report-directory identifier. */
    public static String sanitize(String value) {
        return value == null || value.isBlank() ? "ui_performance-run" : value.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
