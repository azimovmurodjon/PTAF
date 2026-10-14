package com.ptaf.mobile.hierarchy;

import com.ptaf.mobile.config.MobilePlatform;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Offline contract tests for the hierarchy-capture support. These tests do not need a device,
 * Appium server, application binary, Hooks lifecycle, or active locator resource.
 */
public class MobileUiHierarchyCaptureContractTest {

    private final MobileLocatorCandidateGenerator generator = new MobileLocatorCandidateGenerator();

    @Test
    public void shouldRedactVisibleValuesWhilePreservingStructure() {
        String raw = "<hierarchy><node class=\"android.widget.EditText\" resource-id=\"com.example:id/username\" "
                + "text=\"Example User\" content-desc=\"Username input\"/></hierarchy>";

        String sanitized = MobileHierarchyXmlSanitizer.sanitize(raw, true);

        Assert.assertTrue(sanitized.contains("resource-id=\"com.example:id/username\""));
        Assert.assertTrue(sanitized.contains("text=\"&lt;redacted&gt;\""));
        Assert.assertTrue(sanitized.contains("content-desc=\"&lt;redacted&gt;\""));
        Assert.assertFalse(sanitized.contains("Example User"));
        Assert.assertFalse(sanitized.contains("Username input"));
    }

    @Test
    public void shouldPreferStableAndroidAndIosLocatorStrategies() {
        String androidXml = "<hierarchy><node class=\"android.widget.Button\" resource-id=\"com.example:id/continue_button\" "
                + "content-desc=\"continue_button\" text=\"Continue\"/><node resource-id=\"com.example:id/token_input\"/></hierarchy>";
        String iosXml = "<AppiumAUT><XCUIElementTypeButton type=\"XCUIElementTypeButton\" name=\"continueButton\" label=\"Continue\"/></AppiumAUT>";

        List<MobileLocatorCandidate> android = generator.generate(androidXml, MobilePlatform.ANDROID);
        List<MobileLocatorCandidate> ios = generator.generate(iosXml, MobilePlatform.IOS);

        Assert.assertTrue(android.stream().anyMatch(candidate -> candidate.locator().equals("ACCESSIBILITY_ID_continue_button")));
        Assert.assertTrue(android.stream().anyMatch(candidate -> candidate.locator().equals("ID_com.example:id/continue_button")));
        Assert.assertTrue(ios.stream().anyMatch(candidate -> candidate.locator().equals("ACCESSIBILITY_ID_continueButton")));
        Assert.assertFalse(android.stream().anyMatch(candidate -> candidate.source().equals("text")));
        Assert.assertFalse(ios.stream().anyMatch(candidate -> candidate.source().equals("label")));
        Assert.assertFalse(android.stream().anyMatch(candidate -> candidate.locator().contains("token_input")));
    }

    @Test
    public void shouldMergePlatformDraftsWithoutReplacingExistingPlatformValue() throws Exception {
        Path output = Files.createTempDirectory("mobile-hierarchy-draft-").resolve("login_locator_candidates.yml");
        MobileLocatorDraftWriter writer = new MobileLocatorDraftWriter();

        writer.write(output, "login", MobilePlatform.ANDROID,
                List.of(new MobileLocatorCandidate("continueButton", "ID_com.example:id/continue_button", "resource-id", "high")),
                false);
        writer.write(output, "login", MobilePlatform.IOS,
                List.of(new MobileLocatorCandidate("continueButton", "ACCESSIBILITY_ID_continueButton", "name", "high")),
                false);
        writer.write(output, "login", MobilePlatform.ANDROID,
                List.of(new MobileLocatorCandidate("continueButton", "ID_should_not_replace", "resource-id", "high")),
                false);

        String yaml = Files.readString(output);
        Assert.assertTrue(yaml.contains("android: ID_com.example:id/continue_button"));
        Assert.assertTrue(yaml.contains("ios: ACCESSIBILITY_ID_continueButton"));
        Assert.assertFalse(yaml.contains("ID_should_not_replace"));
        Assert.assertTrue(yaml.contains("review_status: review_required"));
        Assert.assertTrue(yaml.contains("active_mobile_elements_modified: false"));
    }
}
