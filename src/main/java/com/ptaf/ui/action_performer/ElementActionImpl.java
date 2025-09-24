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

/**
 * ElementActionImpl is an implementation of the ElementAction interface that provides methods for
 * performing actions and assertions on web elements within an instance of a Playwright Page or FrameLocator.
 * It utilizes the ActionPerformer, LocatorHandler, and ElementLocatorHelper to manage interactions
 * with various elements on a web page.
 *
 * Backward-compatibility notes:
 * - Frame chain building and locator chaining match the original behavior:
 *   we return the final Locator even if its count==0 (no early failure).
 * - Unnamed role segments (e.g., "ROW_X > Button") are supported via ElementLocatorHelper + LocatorHandler.
 * - Optional dynamic sibling-frame probing (nth()) is OFF by default. Enable with:
 *      -Dptaf.dynamicFrameProbe=true
 *   and configure max with:
 *      -Dptaf.maxFrameIndex=3
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer(); // Handles action execution on Locators
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper(); // Assists in locating elements
    private final LocatorHandler locatorHandler = new LocatorHandler(); // Manages Locator creation based on type

    // -------- Optional dynamic-frame probing (disabled by default to preserve legacy behavior) --------
    private static final boolean DYNAMIC_PROBE_ENABLED =
            Boolean.parseBoolean(System.getProperty("ptaf.dynamicFrameProbe", "false"));
    private static final int MAX_DYNAMIC_FRAME_INDEX =
            Integer.getInteger("ptaf.maxFrameIndex", 3);

    public ElementActionImpl(Page page) {
        super(page);
    }

    /**
     * Retrieves a Locator for the specified element by determining its type and context.
     * This method orchestrates locator chaining. It parses locator strings with " > " to build complex chains.
     *
     * @param iFrame      top-level frame selector (may be null/empty)
     * @param iFrame_2    second-level frame selector
     * @param iFrame_3    third-level frame selector
     * @param element     YAML element group
     * @param key         YAML key within the element group
     * @param page        Page context (used when frameLocator is null)
     * @param frameLocator explicit FrameLocator context (takes precedence if provided)
     * @return The final chained Locator (may have count==0; matches legacy behavior)
     * @throws RuntimeException If any unexpected error occurs building the chain
     */
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {

        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        // Split the string by " > " to get the parts of the chain.
        String[] locatorParts = fullLocatorString.split("\\s*>\\s*");

        try {
            Object context = page;

            // 1) Respect explicit FrameLocator (exact legacy precedence)
            if (frameLocator != null) {
                context = frameLocator;
                Locator built = buildChainInContext(context, locatorParts);
                // Legacy behavior: return even if count==0
                return built;
            }

            // 2) If no explicit FrameLocator, build frame chain from iFrame/iFrame_2/iFrame_3 (legacy style)
            if (iFrame != null && !iFrame.isEmpty()) {
                FrameLocator fl = page.frameLocator(iFrame);
                if (iFrame_2 != null && !iFrame_2.isEmpty()) fl = fl.frameLocator(iFrame_2);
                if (iFrame_3 != null && !iFrame_3.isEmpty()) fl = fl.frameLocator(iFrame_3);
                context = fl;

                // Build with the provided chain first (legacy behavior)
                Locator built = buildChainInContext(context, locatorParts);

                // If optional dynamic probing is disabled OR we already found something, return immediately
                if (!DYNAMIC_PROBE_ENABLED) {
                    return built;
                }
                try {
                    if (built != null && built.count() > 0) {
                        return built;
                    }
                } catch (Exception ignore) {
                    // count() can throw in some edge cases; ignore to keep behavior stable
                }

                // Optional dynamic sibling-frame probing using nth(i)
                Locator probed = probeSiblingFrames(page, iFrame, iFrame_2, iFrame_3, locatorParts);
                return (probed != null) ? probed : built; // fall back to built even if empty
            }

            // 3) No frames -> build chain on page (legacy behavior)
            Locator built = buildChainInContext(page, locatorParts);
            return built;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Chain builders (preserve legacy semantics: no count checks here)
    // ---------------------------------------------------------------------------------------
    private Locator buildChainInContext(Object context, String[] locatorParts) {
        Locator currentLocator = null;
        for (int i = 0; i < locatorParts.length; i++) {
            String part = locatorParts[i].trim();
            String locatorType = elementLocatorHelper.getLocatorType(part);
            String locator = elementLocatorHelper.getLocator(part);

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
    }

    /**
     * Optional sibling-frame probing using nth(0..MAX_DYNAMIC_FRAME_INDEX).
     * Only used if DYNAMIC_PROBE_ENABLED==true and the straight chain produced no matches.
     * This does not alter legacy behavior if disabled.
     */
    private Locator probeSiblingFrames(Page page, String f1, String f2, String f3, String[] locatorParts) {
        try {
            if (f1 == null || f1.isEmpty()) return null;

            // level 1 only
            if ((f2 == null || f2.isEmpty()) && (f3 == null || f3.isEmpty())) {
                for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                    FrameLocator fl1 = page.frameLocator(f1).nth(i);
                    Locator candidate = buildChainInContext(fl1, locatorParts);
                    try {
                        if (candidate != null && candidate.count() > 0) {
                            logger.info("Dynamic frame probe: resolved at level1 nth({})", i);
                            return candidate;
                        }
                    } catch (Exception ignore) { /* keep probing */ }
                }
                return null;
            }

            // level 1 + 2
            if (f2 != null && !f2.isEmpty() && (f3 == null || f3.isEmpty())) {
                for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                    FrameLocator fl1 = page.frameLocator(f1).nth(i);
                    for (int j = 0; j <= MAX_DYNAMIC_FRAME_INDEX; j++) {
                        FrameLocator fl2 = fl1.frameLocator(f2).nth(j);
                        Locator candidate = buildChainInContext(fl2, locatorParts);
                        try {
                            if (candidate != null && candidate.count() > 0) {
                                logger.info("Dynamic frame probe: resolved at level2 nth({},{})", i, j);
                                return candidate;
                            }
                        } catch (Exception ignore) { /* keep probing */ }
                    }
                }
                return null;
            }

            // level 1 + 2 + 3
            if (f2 != null && !f2.isEmpty() && f3 != null && !f3.isEmpty()) {
                for (int i = 0; i <= MAX_DYNAMIC_FRAME_INDEX; i++) {
                    FrameLocator fl1 = page.frameLocator(f1).nth(i);
                    for (int j = 0; j <= MAX_DYNAMIC_FRAME_INDEX; j++) {
                        FrameLocator fl2 = fl1.frameLocator(f2).nth(j);
                        for (int k = 0; k <= MAX_DYNAMIC_FRAME_INDEX; k++) {
                            FrameLocator fl3 = fl2.frameLocator(f3).nth(k);
                            Locator candidate = buildChainInContext(fl3, locatorParts);
                            try {
                                if (candidate != null && candidate.count() > 0) {
                                    logger.info("Dynamic frame probe: resolved at level3 nth({},{},{})", i, j, k);
                                    return candidate;
                                }
                            } catch (Exception ignore) { /* keep probing */ }
                        }
                    }
                }
                return null;
            }

            return null;
        } catch (Exception e) {
            logger.warn("Dynamic frame probing failed: {}", e.getMessage());
            return null;
        }
    }

    // ------------------------- Existing public API below (UNCHANGED) -------------------------

    @Override
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return performAction(page, null, null, null, action, element, key, value, null);
    }

    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performAction(null, null, null, null, action, element, key, value, frameLocator);
    }

    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                          String action, String element, String key, String value, FrameLocator frameLocator) {
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

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
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
        }
        return false;
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

    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        return elementLocatorHelper.getLocator(locatorValue);
    }
}