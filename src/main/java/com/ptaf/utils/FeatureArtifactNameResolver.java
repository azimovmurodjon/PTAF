package com.ptaf.utils;

import io.cucumber.java.Scenario;

import java.io.InputStream;
import java.net.URI;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a Cucumber feature's declared {@code Feature:} name and creates consistent,
 * filesystem-safe artifact filenames.
 *
 * <p>Downloads and videos use the same feature-title source as the framework's per-feature
 * reports. An artifact name follows the pattern
 * {@code Feature_Name_yyyy-MM-dd_HH-mm-ss-SSSSSS.ext}. Microseconds keep artifacts unique when
 * one feature produces several downloads, popups, or videos within the same second.</p>
 */
public final class FeatureArtifactNameResolver {

    private static final Pattern FEATURE_NAME_PATTERN = Pattern.compile(
            "(?m)^\\s*Feature\\s*:\\s*(.+?)\\s*$"
    );
    private static final DateTimeFormatter ARTIFACT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSSSSS");

    private FeatureArtifactNameResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Resolves the exact declared {@code Feature:} title for the supplied scenario.
     * If the source cannot be read, this method falls back to the feature-file stem, then to
     * {@code unnamed_feature}; it never falls back to the scenario name.
     *
     * @param scenario active Cucumber scenario
     * @return the declared Feature title or a safe fallback
     */
    public static String resolveFeatureName(Scenario scenario) {
        if (scenario == null || scenario.getUri() == null) {
            return "unnamed_feature";
        }

        return resolveFeatureName(scenario.getUri());
    }

    /**
     * Resolves the declared {@code Feature:} title from a Cucumber feature URI. This overload is
     * useful for artifact utilities that have the feature URI but not the full Scenario object.
     *
     * @param uri URI of the Cucumber feature file
     * @return the declared Feature title or a safe fallback
     */
    public static String resolveFeatureName(URI uri) {
        if (uri == null) {
            return "unnamed_feature";
        }

        try (InputStream stream = openFeatureStream(uri)) {
            if (stream != null) {
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                Matcher matcher = FEATURE_NAME_PATTERN.matcher(source);
                if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                    return matcher.group(1).trim();
                }
            }
        } catch (Exception ignored) {
            // Artifact saving must never fail because a feature source cannot be resolved.
        }

        return extractFeatureFileStem(uri);
    }

    /**
     * Creates a unique artifact path within {@code outputDirectory}. The extension from the
     * original file name is preserved, while the original file stem is deliberately replaced by
     * the declared Feature title.
     *
     * @param outputDirectory target artifact directory
     * @param scenario active Cucumber scenario
     * @param originalFileName source name used only to preserve the extension
     * @return feature-based target path
     */
    public static Path buildArtifactPath(Path outputDirectory, Scenario scenario, String originalFileName) {
        return outputDirectory.resolve(buildArtifactFileName(scenario, originalFileName));
    }

    /**
     * Creates a unique artifact path from a feature URI while preserving the source extension.
     *
     * @param outputDirectory target Feature-name artifact directory
     * @param featureUri URI of the Cucumber feature file
     * @param originalFileName source name used only to preserve the extension
     * @return feature-based target path
     */
    public static Path buildArtifactPath(Path outputDirectory, URI featureUri, String originalFileName) {
        return outputDirectory.resolve(buildArtifactFileName(featureUri, originalFileName));
    }

    /**
     * Creates and returns a filesystem-safe subdirectory named after the declared
     * {@code Feature:} title. All artifacts for the same feature are therefore grouped together
     * below their existing artifact-type output root.
     *
     * @param outputRoot configured downloads or videos directory
     * @param scenario active Cucumber scenario
     * @return the existing or newly-created Feature-name subdirectory
     * @throws IOException when the artifact directory cannot be created
     */
    public static Path createFeatureDirectory(Path outputRoot, Scenario scenario) throws IOException {
        return createFeatureDirectory(outputRoot, scenario != null ? scenario.getUri() : null);
    }

    /**
     * Creates and returns a filesystem-safe Feature-name subdirectory from a feature URI.
     *
     * @param outputRoot configured downloads or videos directory
     * @param featureUri URI of the Cucumber feature file
     * @return the existing or newly-created Feature-name subdirectory
     * @throws IOException when the artifact directory cannot be created
     */
    public static Path createFeatureDirectory(Path outputRoot, URI featureUri) throws IOException {
        Path featureDirectory = outputRoot.resolve(sanitizeFeatureName(resolveFeatureName(featureUri)));
        return Files.createDirectories(featureDirectory);
    }

    /**
     * Creates a feature-based artifact file name with a microsecond timestamp.
     *
     * @param scenario active Cucumber scenario
     * @param originalFileName source name used only to preserve the extension
     * @return filesystem-safe feature-title filename
     */
    public static String buildArtifactFileName(Scenario scenario, String originalFileName) {
        return buildArtifactFileName(scenario != null ? scenario.getUri() : null, originalFileName);
    }

    /**
     * Creates a feature-based artifact file name from a feature URI with a microsecond timestamp.
     *
     * @param featureUri URI of the feature file
     * @param originalFileName source name used only to preserve the extension
     * @return filesystem-safe feature-title filename
     */
    public static String buildArtifactFileName(URI featureUri, String originalFileName) {
        String safeFeatureName = sanitizeFeatureName(resolveFeatureName(featureUri));
        String timestamp = LocalDateTime.now().format(ARTIFACT_TIMESTAMP_FORMAT);
        return safeFeatureName + "_" + timestamp + extractExtension(originalFileName);
    }

    private static InputStream openFeatureStream(URI uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Files.newInputStream(Path.of(uri));
        }

        String resourcePath = uri.toString();
        if (resourcePath.startsWith("classpath:")) {
            resourcePath = resourcePath.substring("classpath:".length());
        }
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    }

    private static String extractFeatureFileStem(URI uri) {
        String path = uri == null ? "" : uri.toString();
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        int dot = filename.lastIndexOf('.');
        String stem = dot > 0 ? filename.substring(0, dot) : filename;
        return stem == null || stem.trim().isEmpty() ? "unnamed_feature" : stem.trim();
    }

    private static String sanitizeFeatureName(String featureName) {
        String sanitized = featureName == null ? "" : featureName.trim()
                .replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (sanitized.isEmpty()) {
            return "unnamed_feature";
        }
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }

    private static String extractExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "";
        }
        String filename = Path.of(originalFileName).getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot > 0 && dot < filename.length() - 1 ? filename.substring(dot) : "";
    }
}
