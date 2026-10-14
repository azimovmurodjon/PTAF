package com.ptaf.mobile.hierarchy;

import com.ptaf.mobile.config.MobilePlatform;

import java.nio.file.Path;

/**
 * Immutable outcome of one explicit native-mobile UI hierarchy capture.
 *
 * <p>The raw hierarchy is written only to the configured test-output location. A locator
 * candidate file is a review artifact and is never written to the active
 * {@code src/test/resources/mobile/elements} directory.</p>
 */
public record MobileHierarchyCaptureResult(
        String screenName,
        MobilePlatform platform,
        Path hierarchyXmlPath,
        Path locatorDraftPath,
        int candidateCount,
        boolean redacted
) {
}
