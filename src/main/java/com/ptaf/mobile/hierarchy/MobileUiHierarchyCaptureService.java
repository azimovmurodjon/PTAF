package com.ptaf.mobile.hierarchy;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import io.appium.java_client.AppiumDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Explicit, on-demand capture service for the active native Android or iOS UI hierarchy.
 *
 * <p>This service never changes {@code Hooks}, active mobile locators, or application actions.
 * It writes artifacts only below the configured test-output directory and creates a separate
 * review draft. It is therefore safe to add to working mobile automation without changing the
 * normal scenario lifecycle.</p>
 */
public final class MobileUiHierarchyCaptureService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private final MobileLocatorCandidateGenerator candidateGenerator;
    private final MobileLocatorDraftWriter draftWriter;

    public MobileUiHierarchyCaptureService() {
        this(new MobileLocatorCandidateGenerator(), new MobileLocatorDraftWriter());
    }

    MobileUiHierarchyCaptureService(MobileLocatorCandidateGenerator candidateGenerator, MobileLocatorDraftWriter draftWriter) {
        this.candidateGenerator = candidateGenerator;
        this.draftWriter = draftWriter;
    }

    /** Capture the current hierarchy and optionally generate a review-only locator draft. */
    public MobileHierarchyCaptureResult capture(AppiumDriver driver, MobilePlatform platform, String requestedScreenName) {
        if (!MobileConfigurationProperties.isHierarchyCaptureEnabled()) {
            throw new IllegalStateException("Mobile UI hierarchy capture is disabled in mobile-config.yml.");
        }
        if (driver == null) {
            throw new IllegalArgumentException("An active Appium driver is required to capture the current UI hierarchy.");
        }
        if (platform == null) {
            throw new IllegalStateException("The active mobile platform is not resolved. Start a native Android or iOS session first.");
        }

        String screenName = safeName(requestedScreenName, "currentScreen");
        String hierarchyXml = driver.getPageSource();
        if (hierarchyXml == null || hierarchyXml.isBlank()) {
            throw new IllegalStateException("Appium returned an empty UI hierarchy for screen [" + screenName + "].");
        }

        Path outputRoot = Path.of(MobileConfigurationProperties.getHierarchyCaptureOutputDirectory());
        String platformName = platform.name().toLowerCase();
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        Path hierarchyDirectory = outputRoot.resolve(screenName).resolve(platformName);
        Path hierarchyPath = hierarchyDirectory.resolve(screenName + "_" + platformName + "_" + timestamp + ".xml");
        boolean redactValues = MobileConfigurationProperties.redactHierarchyValues();

        try {
            Files.createDirectories(hierarchyDirectory);
            String persistedXml = MobileHierarchyXmlSanitizer.sanitize(hierarchyXml, redactValues);
            Files.writeString(hierarchyPath, persistedXml);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write mobile UI hierarchy artifact: " + hierarchyPath, exception);
        }

        Path draftPath = null;
        List<MobileLocatorCandidate> candidates = List.of();
        if (MobileConfigurationProperties.generateHierarchyLocatorDraft()) {
            candidates = candidateGenerator.generate(hierarchyXml, platform);
            draftPath = outputRoot.resolve(screenName)
                    .resolve("locator-drafts")
                    .resolve(screenName + "_locator_candidates.yml");
            draftWriter.write(
                    draftPath,
                    screenName,
                    platform,
                    candidates,
                    MobileConfigurationProperties.overwriteGeneratedHierarchyDraftValues()
            );
        }

        return new MobileHierarchyCaptureResult(
                screenName,
                platform,
                hierarchyPath,
                draftPath,
                candidates.size(),
                redactValues
        );
    }

    private String safeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
