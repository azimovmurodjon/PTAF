package com.ptaf.ui.action_performer;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.ptaf.ui.handlers.LocatorHandler;
import com.ptaf.ui.helpers.ElementLocatorHelper;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.ui.page_helper.PageHelper;
import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backward-compatible ElementActionImpl with resilient dynamic nested-iframe resolution.
 *
 * Behavior (no flags required):
 *  1) Build exactly as before (legacy) and return the locator (even if count==0).
 *  2) If that yields no matches, auto-fallbacks:
 *     - Relax numeric iframe attributes (e.g., frameborder='0px' -> frameborder$='px';
 *       data-model='123' -> [data-model]).
 *     - Presence fallback ([attr]) + nth() probing across siblings at each frame level
 *       (handles the "inner same frame, different number" case).
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    /** Probe breadth when multiple identical iframes exist. Adjust via -Dptaf.maxFrameIndex=5 if needed. */
    private static final int MAX_DYNAMIC_FRAME_INDEX =
            Integer.getInteger("ptaf.maxFrameIndex", 3);

    public ElementActionImpl(Page page) {
        super(page);
    }

    // --------------------------------------------------------------------------------------
    // Legacy public overloads (keep old call sites compiling)
    // --------------------------------------------------------------------------------------

    /** Legacy: page-only variant */
    public boolean performAction(Page page, String action, String element, String key, String value) {
        return performActionPage(page, action, element, key, value);
    }

    /** Legacy: frame-only variant */
    public boolean performAction(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performActionFrame(frameLocator, action, element, key, value);
    }

    /** Legacy: page + up to 3 iFrames (no explicit FrameLocator param) */
    public boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                 String action, String element, String key, String value) {
        return performActionPageFrame(page, iFrame, iFrame_2, iFrame_3, action, element, key, value, null);
    }

    // --------------------------------------------------------------------------------------
    // Interface methods (unchanged signatures)
    // --------------------------------------------------------------------------------------

    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {

        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        String[] locatorParts = fullLocatorString.split("\\s*>\\s*");

        try {
            // 1) Explicit FrameLocator takes precedence (legacy)
            if (frameLocator != null) {
                return buildChainInContext(frameLocator, locatorParts); // even if count==0
            }

            // 2) Frame params -> legacy chain
            if (iFrame != null && !iFrame.isEmpty()) {
                FrameLocator legacyChain = page.frameLocator(iFrame);
                if (iFrame_2 != null && !iFrame_2.isEmpty()) legacyChain = legacyChain.frameLocator(iFrame_2);
                if (iFrame_3 != null && !iFrame_3.isEmpty()) legacyChain = legacyChain.frameLocator(iFrame_3);

                Locator legacyBuilt = buildChainInContext(legacyChain, locatorParts);
                if (hasAtLeastOne(legacyBuilt)) return legacyBuilt;

                // ===== Automatic fallbacks for dynamic/numbered frames =====

                // A) Relax numeric selectors (e.g., '0px' -> $='px', '123' -> presence)
                FrameLocator relaxedChain = buildRelaxedFrameChain(page, iFrame, iFrame_2, iFrame_3);
                if (relaxedChain != null) {
                    Locator relaxedBuilt = buildChainInContext(relaxedChain, locatorParts);
                    if (hasAtLeastOne(relaxedBuilt)) {
                        logger.info("Resolved via relaxed frame selector(s).");
                        return relaxedBuilt;
                    }
                }

                // B) Presence selectors + nth() probing per level (covers "inner same frame")
                return presenceFallbackWithProbing(page, iFrame, iFrame_2, iFrame_3, locatorParts, legacyBuilt);
            }

            // 3) Page context only (legacy)
            return buildChainInContext(page, locatorParts);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    @Override
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return performActionInternal(page, null, null, null, action, element, key, value, null);
    }

    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performActionInternal(null, null, null, null, action, element, key, value, frameLocator);
    }

    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                          String action, String element, String key, String value, FrameLocator frameLocator) {
        return performActionInternal(page, iFrame, iFrame_2, iFrame_3, action, element, key, value, null);
    }

    @Override
    public boolean getElementHandlePage(Page page, String element, String key) {
        List<ElementHandle> elementHandles = getElementHandleList(page, element, key, null);
        return !elementHandles.isEmpty();
    }

    @Override
    public boolean getElementHandleFrame(FrameLocator frameLocator, String element, String key) {
        List<ElementHandle> elementHandles = getElementHandleList(null, element, key, frameLocator);
        return !elementHandles.isEmpty();
    }

    @Override
    public boolean assertElementTextPage(Page page, String element, String key, String expectedText) {
        return assertElementText(page, element, key, expectedText, null);
    }

    @Override
    public boolean assertElementTextFrame(FrameLocator frameLocator, String element, String key, String expectedText) {
        return assertElementText(null, element, key, expectedText, frameLocator);
    }

    @Override
    public String performActionPageWithReturn(Page page, String action, String element, String key, String value) {
        try {
            Locator targetLocator = getLocatorBasedOnPage(page, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for element: {} with key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageWithReturn for element '{}' and action '{}':", element, action, e);
            return null;
        }
    }

    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                                   String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for nested frame element: {} with key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageFrameWithReturn for element '{}' and action '{}':", element, action, e);
            return null;
        }
    }

    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        FileChooser fileChooser = page.waitForFileChooser(() ->
                page.click(getElement(element, key)));
        fileChooser.setFiles(Paths.get(getElement(element, file_name)));
    }

    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = getElement(element, key);
        String fileName = extractFileName(documentLinkName);
        System.out.println(fileName);
        try {
            page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(fileName)).click();
        } catch (Exception e) {
            logger.error("Failed to click on element by Role '{}'", element + key, e);
        }
    }

    public static String extractFileName(String filePath) {
        String[] parts = filePath.split("/");
        return parts[parts.length - 1];
    }

    @Override
    public String getElement(String element, String key) {
        try {
            return (String) YamlReader.get("elements." + element + "." + key);
        } catch (Exception e) {
            logger.error("Failed to retrieve selector for element '{}'", element + key, e);
            throw e;
        }
    }

    @Override
    public List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator) {
        List<ElementHandle> elementHandles = new ArrayList<>();
        try {
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            if (targetLocator != null) {
                elementHandles = targetLocator.elementHandles();
            } else {
                logger.error("Target locator for element '{}' could not be determined.", element);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve element handles for '{}'", element, e);
        }
        return elementHandles;
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        return elementLocatorHelper.getLocator(locatorValue);
    }

    // --------------------------------------------------------------------------------------
    // Internals
    // --------------------------------------------------------------------------------------

    private boolean performActionInternal(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                          String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // Context selection (legacy-compatible)
            if (frameLocator != null) {
                targetLocator = getLocator(null, null, null, element, key, null, frameLocator);
            } else if (page != null) {
                targetLocator = getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
            } else {
                throw new IllegalArgumentException("A Page or FrameLocator context is required.");
            }

            if (targetLocator == null) {
                throw new IllegalStateException("Failed to resolve a target Locator for element: " + element + " with key: " + key);
            }

            actionPerformer.waitForLocator(targetLocator);
            actionPerformer.performAction(page, action, targetLocator, value);
            return true;
        } catch (Exception e) {
            logger.error("Error while performing action '{}' on element '{}' with key '{}'", action, element, key, e);
            return false;
        }
    }

    private Locator buildChainInContext(Object context, String[] locatorParts) {
        Locator current = null;
        for (int i = 0; i < locatorParts.length; i++) {
            String part = locatorParts[i].trim();
            String locatorType = elementLocatorHelper.getLocatorType(part);
            String locator = elementLocatorHelper.getLocator(part);

            if (i == 0) {
                if (context instanceof Page) {
                    current = locatorHandler.getLocatorForType(locatorType, (Page) context, locator);
                } else {
                    current = locatorHandler.getLocatorForType(locatorType, (FrameLocator) context, locator);
                }
            } else {
                current = locatorHandler.getLocatorForType(locatorType, current, locator);
            }
        }
        return current; // legacy: even if count==0
    }

    private boolean hasAtLeastOne(Locator loc) {
        try {
            return loc != null && loc.count() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ---------- Dynamic frame selector relaxation & probing ----------

    /**
     * If a frame selector contains numeric-valued attributes, relax them:
     *  - attr='Npx'  -> attr$='px'
     *  - attr='123'  -> [attr]
     */
    private String relaxNumericAttrSelectors(String raw) {
        if (raw == null || raw.isEmpty()) return raw;

        String result = raw;

        // A: attr='Npx'  -> attr$='px'
        Pattern px = Pattern.compile("(?i)(\\biframe\\b[^\\[]*)\\[(\\w[\\w-]*)\\s*=\\s*['\"]?(\\d+)px['\"]?\\]");
        Matcher mPx = px.matcher(result);
        if (mPx.find()) {
            String before = mPx.group(1);
            String attr   = mPx.group(2);
            result = before + "[" + attr + "$='px']";
            return result;
        }

        // B: attr='123' (pure digits) -> presence
        Pattern digits = Pattern.compile("(?i)(\\biframe\\b[^\\[]*)\\[(\\w[\\w-]*)\\s*=\\s*['\"]?\\d+['\"]?\\]");
        Matcher mDigits = digits.matcher(result);
        if (mDigits.find()) {
            String before = mDigits.group(1);
            String attr   = mDigits.group(2);
            result = before + "[" + attr + "]";
            return result;
        }

        return result; // unchanged
    }

    private String presenceFallbackSelector(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        // ends-with px -> presence
        if (raw.matches("(?i).*\\[\\w[\\w-]*\\$='px'\\].*")) {
            return raw.replaceAll("(?i)\\[(\\w[\\w-]*)\\$='px'\\]", "[$1]");
        }
        // equals -> presence
        if (raw.matches("(?i).*\\[\\w[\\w-]*\\s*=\\s*[^\\]]+\\].*")) {
            return raw.replaceAll("(?i)\\[(\\w[\\w-]*)\\s*=\\s*[^\\]]+\\]", "[$1]");
        }
        return raw;
    }

    private FrameLocator buildRelaxedFrameChain(Page page, String f1, String f2, String f3) {
        String r1 = relaxNumericAttrSelectors(f1);
        FrameLocator fl = page.frameLocator(r1 != null ? r1 : f1);
        if (f2 != null && !f2.isEmpty()) {
            String r2 = relaxNumericAttrSelectors(f2);
            fl = fl.frameLocator(r2 != null ? r2 : f2);
        }
        if (f3 != null && !f3.isEmpty()) {
            String r3 = relaxNumericAttrSelectors(f3);
            fl = fl.frameLocator(r3 != null ? r3 : f3);
        }
        return fl;
    }

    /** Presence fallback + nth() probing across siblings at each level (1..3). */
    private Locator presenceFallbackWithProbing(Page page, String f1, String f2, String f3,
                                                String[] locatorParts, Locator legacyBuilt) {
        String p1 = presenceFallbackSelector(relaxNumericAttrSelectors(f1));
        String p2 = (f2 != null && !f2.isEmpty()) ? presenceFallbackSelector(relaxNumericAttrSelectors(f2)) : null;
        String p3 = (f3 != null && !f3.isEmpty()) ? presenceFallbackSelector(relaxNumericAttrSelectors(f3)) : null;

        // Level 1 only
        if (p1 != null && (p2 == null && p3 == null)) {
            for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                FrameLocator fl1 = page.frameLocator(p1).nth(i);
                Locator cand = buildChainInContext(fl1, locatorParts);
                if (hasAtLeastOne(cand)) {
                    logger.info("Resolved via presence+nth at level1: nth({})", i);
                    return cand;
                }
            }
            return legacyBuilt;
        }

        // Level 1 + 2
        if (p1 != null && p2 != null && p3 == null) {
            for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                FrameLocator fl1 = page.frameLocator(p1).nth(i);
                for (int j = 0; j <= MAX_DYNAMIC_FRAME_INDEX; j++) {
                    FrameLocator fl2 = fl1.frameLocator(p2).nth(j);
                    Locator cand = buildChainInContext(fl2, locatorParts);
                    if (hasAtLeastOne(cand)) {
                        logger.info("Resolved via presence+nth at level2: nth({},{})", i, j);
                        return cand;
                    }
                }
            }
            return legacyBuilt;
        }

        // Level 1 + 2 + 3 (inner same frame scenario)
        if (p1 != null && p2 != null && p3 != null) {
            for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                FrameLocator fl1 = page.frameLocator(p1).nth(i);
                for (int j = 0; j <= MAX_DYNAMIC_FRAME_INDEX; j++) {
                    FrameLocator fl2 = fl1.frameLocator(p2).nth(j);
                    for (int k = 0; k <= MAX_DYNAMIC_FRAME_INDEX; k++) {
                        FrameLocator fl3 = fl2.frameLocator(p3).nth(k);
                        Locator cand = buildChainInContext(fl3, locatorParts);
                        if (hasAtLeastOne(cand)) {
                            logger.info("Resolved via presence+nth at level3: nth({},{},{})", i, j, k);
                            return cand;
                        }
                    }
                }
            }
        }
        return legacyBuilt;
    }

    // --------------------------------------------------------------------------------------
    // Small helpers
    // --------------------------------------------------------------------------------------

    private boolean assertElementText(Page page, String element, String key, String expectedText, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            String actualText = targetLocator.first().textContent();
            boolean isTextMatching = expectedText.equals(actualText);
            logger.info("Asserting text on element '{}': expected '{}', actual '{}'", element, expectedText, actualText);
            if (!isTextMatching) {
                logger.error("Text mismatch: expected '{}' but found '{}'", expectedText, actualText);
            }
            return isTextMatching;
        } catch (Exception e) {
            logger.error("Error while asserting text on element '{}'", element, e);
            return false;
        }
    }

    private Locator getLocatorBasedOnPage(Page page, String element, String key) {
        return getLocator(null, null, null, element, key, page, null);
    }

    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String key) {
        return getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
    }

    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }
}