package com.ptaf.ui.action_performer;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
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

/**
 * ElementActionImpl is an implementation of the ElementAction interface that provides methods for
 * performing actions and assertions on web elements within an instance of a Playwright Page or FrameLocator.
 * It utilizes the ActionPerformer, LocatorHandler, and ElementLocatorHelper to manage interactions
 * with various elements on a web page.
 *
 * Non-breaking enhancements:
 * - Chain split supports both " > " and "&gt;".
 * - Tokens without "_" are treated as name-optional role/locator types (e.g., "Button", "ROW").
 * - Dynamic iframe resolution: prefers :visible, iterates from last-to-first (topmost/newest),
 *   and uses robust fallbacks to reliably target the correct modal frame.
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer(); // Handles action execution on Locators
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper(); // Assists in locating elements
    private final LocatorHandler locatorHandler = new LocatorHandler(); // Manages Locator creation based on type

    public ElementActionImpl(Page page) {
        super(page);
    }

    /**
     * Central orchestrator for locator chaining (frames + element chain).
     */
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3, String element, String key, Page page, FrameLocator frameLocator) {
        String fullLocatorString = elementLocatorHelper.getElement(element, key);

        // SUPPORT BOTH: " > " and "&gt;" (non-breaking)
        String[] locatorParts = normalizeAndSplitChain(fullLocatorString);

        Locator currentLocator = null;

        try {
            Object context = page;

            if (frameLocator != null) {
                context = frameLocator;
            } else if (iFrame != null && !iFrame.isEmpty()) {
                FrameLocator fl = findFrameWithElement(page, iFrame, element, key);

                if (iFrame_2 != null && !iFrame_2.isEmpty()) {
                    fl = findFrameWithElement(fl, iFrame_2, element, key);
                }
                if (iFrame_3 != null && !iFrame_3.isEmpty()) {
                    fl = findFrameWithElement(fl, iFrame_3, element, key);
                }

                context = fl;
            }

            for (int i = 0; i < locatorParts.length; i++) {
                String part = locatorParts[i].trim();

                // Tolerant parsing (keeps old style working, adds name-optional support)
                String locatorType = parseType(part);
                String locator = parseValue(part);

                if (i == 0) {
                    if (context instanceof Page) {
                        currentLocator = locatorHandler.getLocatorForType(locatorType, (Page) context, locator);
                    } else {
                        currentLocator = locatorHandler.getLocatorForType(locatorType, (FrameLocator) context, locator);
                    }
                } else {
                    currentLocator = locatorHandler.getLocatorForType(locatorType, currentLocator, locator);
                }
            }

            return currentLocator;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    // ============================ FRAME RESOLUTION (UPDATED) ============================

    /**
     * Page-root frame resolution.
     * Strategy:
     *  1) Try provided selector with :visible
     *  2) Try provided selector as-is
     *  3) Try known modal fallbacks (visible first)
     *  4) Fallback to first match of original selector (legacy behavior)
     */
    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        // Best-effort attach wait; non-throwing
        try {
            page.waitForSelector(iframeSelector, new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.ATTACHED).setTimeout(2000));
        } catch (Exception ignored) {}

        // 1) Prefer visible variant
        FrameLocator fl = trySelectFrameBySelector(page, preferVisibleSelector(iframeSelector), element, key);
        if (fl != null) return fl;

        // 2) Original selector
        fl = trySelectFrameBySelector(page, iframeSelector, element, key);
        if (fl != null) return fl;

        // 3) Known modal fallbacks
        for (String fallback : modalIframeFallbacks()) {
            fl = trySelectFrameBySelector(page, fallback, element, key);
            if (fl != null) return fl;
        }

        // 4) Legacy fallback
        return page.frameLocator(iframeSelector);
    }

    /**
     * Nested-frame root resolution (FrameLocator parent).
     * Same strategy as page-root resolution.
     */
    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        // Best-effort attach wait; non-throwing
        try {
            parentFrame.locator(iframeSelector).first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED).setTimeout(2000));
        } catch (Exception ignored) {}

        // 1) Prefer visible variant
        FrameLocator fl = trySelectFrameBySelector(parentFrame, preferVisibleSelector(iframeSelector), element, key);
        if (fl != null) return fl;

        // 2) Original selector
        fl = trySelectFrameBySelector(parentFrame, iframeSelector, element, key);
        if (fl != null) return fl;

        // 3) Known modal fallbacks
        for (String fallback : modalIframeFallbacks()) {
            fl = trySelectFrameBySelector(parentFrame, fallback, element, key);
            if (fl != null) return fl;
        }

        // 4) Legacy fallback
        return parentFrame.frameLocator(iframeSelector);
    }

    // ============================ HELPERS (NON-BREAKING) ============================

    /** Accept both " > " and "&gt;" in YAML. We normalize to ">" and split. */
    private String[] normalizeAndSplitChain(String raw) {
        if (raw == null) return new String[0];
        String normalized = raw.replace("&gt;", ">");              // HTML entity to literal
        return normalized.split("\\s*>\\s*");                      // split on literal '>'
    }

    /** Backward compatible type parsing. */
    private String parseType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(0, idx) : token).trim();
    }

    /** Backward compatible value parsing. */
    private String parseValue(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(idx + 1) : "").trim();
    }

    /** If selector doesn't already include :visible, append it. */
    private String preferVisibleSelector(String selector) {
        if (selector == null || selector.isEmpty()) return selector;
        if (selector.contains(":visible")) return selector;
        return selector + ":visible";
    }

    /** Common modal iframe patterns as a last-resort fallback (non-breaking). */
    private String[] modalIframeFallbacks() {
        return new String[] {
                "iframe[name^='iframeWindowModal']:visible",
                "iframe[frameborder='0px']:visible",
                "iframe[name^='iframeWindowModal']",
                "iframe[frameborder='0px']"
        };
    }

    /** Scan frames (reverse order) for first where the first chain segment is visible; PAGE root. */
    private FrameLocator trySelectFrameBySelector(Page page, String iframeSelector, String element, String key) {
        if (iframeSelector == null || iframeSelector.isEmpty()) return null;
        Locator iframeLocator = page.locator(iframeSelector);
        int count = iframeLocator.count();
        if (count == 0) return null;

        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        String[] locatorParts = normalizeAndSplitChain(fullLocatorString);
        String part = locatorParts.length > 0 ? locatorParts[0].trim() : "";
        String locatorType = parseType(part);
        String locator = parseValue(part);

        // Iterate from last to first -> newest/topmost
        for (int i = count - 1; i >= 0; i--) {
            FrameLocator fl = page.frameLocator(iframeSelector).nth(i);
            try {
                Locator testLocator = locatorHandler.getLocatorForType(locatorType, fl, locator);
                if (testLocator.count() > 0 && testLocator.first().isVisible()) {
                    return fl;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** Scan frames (reverse order) for first where the first chain segment is visible; nested FRAME root. */
    private FrameLocator trySelectFrameBySelector(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        if (iframeSelector == null || iframeSelector.isEmpty()) return null;
        Locator iframeLocator = parentFrame.locator(iframeSelector);
        int count = iframeLocator.count();
        if (count == 0) return null;

        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        String[] locatorParts = normalizeAndSplitChain(fullLocatorString);
        String part = locatorParts.length > 0 ? locatorParts[0].trim() : "";
        String locatorType = parseType(part);
        String locator = parseValue(part);

        // Iterate from last to first -> newest/topmost
        for (int i = count - 1; i >= 0; i--) {
            FrameLocator fl = parentFrame.frameLocator(iframeSelector).nth(i);
            try {
                Locator testLocator = locatorHandler.getLocatorForType(locatorType, fl, locator);
                if (testLocator.count() > 0 && testLocator.first().isVisible()) {
                    return fl;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ============================ EXISTING LOGIC (UNCHANGED) ============================

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // Simplified context checking
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
        }

        return false;
    }

    @Override
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return performAction(page, null, null, null, action, element, key, value, null);
    }

    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performAction(null, null, null, null, action, element, key, value, frameLocator);
    }

    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        return performAction(page, iFrame, iFrame_2, iFrame_3, action, element, key, value, null);
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
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
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

    public String getElement(String element, String key) {
        try {
            return (String) YamlReader.get("elements." + element + "." + key);
        } catch (Exception e) {
            logger.error("Failed to retrieve selector for element '{}'", element + key, e);
            throw e;
        }
    }

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

    private Locator getLocatorBasedOnPage(Page page, String element, String key) {
        return getLocator(null, null, null, element, key, page, null);
    }

    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String key) {
        return getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
    }

    @SuppressWarnings("unused")
    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        // For compatibility: if someone passes a single token, return the value-part of it
        return parseValue(locatorValue);
    }
}