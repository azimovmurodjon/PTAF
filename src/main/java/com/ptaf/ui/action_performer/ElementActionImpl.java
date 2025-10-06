package com.ptaf.ui.action_performer;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementActionImpl (Auto-Frame Selection)
 *
 * What’s new (without breaking existing code):
 * - Handles XPath-like indexed iframe selectors: (iframe[...])[N]
 *   → Tries that specific index first, then auto-probes others if needed.
 * - Modal-aware: for iframeWindowModal#### and iframe[frameborder='0px']
 *   → Orders by z-index, then probes the ENTIRE chained locator to find the frame
 *     where the final target is actually VISIBLE (or best match by count).
 * - Legacy fallback scanning retained for compatibility.
 *
 * You do NOT need to change step definitions or YAML.
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    public ElementActionImpl(Page page) { super(page); }

    // ============================================================
    // Core locator building across optional frames & chained parts
    // ============================================================
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {
        String fullLocatorString = elementLocatorHelper.getElement(element, key);
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

    // ============================================================
    // Frame resolution (Index-aware + Modal-aware + Auto-probing)
    // ============================================================

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        // 1) If XPath-like index selector present, try that index first
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);

        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;
        String selNormalized = relaxModalSelectorIfNeeded(selBase);

        boolean looksModal = selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));

        String candidateSelector = looksModal
                ? "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']"
                : selNormalized;

        // Short, non-fatal wait for candidate frames to attach
        try {
            page.waitForSelector(candidateSelector, new Page.WaitForSelectorOptions().setTimeout(1500));
        } catch (Throwable ignored) {}

        // 2) Try explicit index first (if provided)
        if (idxSel != null) {
            FrameLocator candidate = page.frameLocator(idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                logger.info("Resolved indexed iframe {}[{}] for element '{}':'{}'",
                        idxSel.base, idxSel.indexZeroBased, element, key);
                return candidate;
            } else {
                logger.info("Indexed iframe {}[{}] did not expose the target; auto-probing others...",
                        idxSel.base, idxSel.indexZeroBased);
            }
        }

        // 3) Auto-probe all candidates (modal-aware ordering + chain probing)
        FrameLocator best = pickBestFrameByChainProbe(page, candidateSelector, element, key);
        if (best != null) return best;

        // 4) Legacy fallback scanning (kept for compatibility)
        Locator iframeLocator = page.locator(selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; i++) {
            FrameLocator fl = page.frameLocator(selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }

        // 5) Final fallback to first matching frame
        return page.frameLocator(selNormalized);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        // 1) If XPath-like index selector present, try that index first
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);

        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;
        String selNormalized = relaxModalSelectorIfNeeded(selBase);

        boolean looksModal = selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));

        String candidateSelector = looksModal
                ? "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']"
                : selNormalized;

        // Short, non-fatal wait for candidate frames to attach in nested context
        try {
            parentFrame.locator(candidateSelector).first()
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.ATTACHED)
                            .setTimeout(1500));
        } catch (Throwable ignored) {}

        // 2) Try explicit index first (if provided)
        if (idxSel != null) {
            FrameLocator candidate = parentFrame.frameLocator(idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                logger.info("Resolved nested indexed iframe {}[{}] for element '{}':'{}'",
                        idxSel.base, idxSel.indexZeroBased, element, key);
                return candidate;
            } else {
                logger.info("Nested indexed iframe {}[{}] did not expose the target; auto-probing others...",
                        idxSel.base, idxSel.indexZeroBased);
            }
        }

        // 3) Auto-probe all candidates (modal-aware ordering + chain probing)
        FrameLocator best = pickBestFrameByChainProbe(parentFrame, candidateSelector, element, key);
        if (best != null) return best;

        // 4) Legacy fallback scanning
        Locator iframeLocator = parentFrame.locator(selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; i++) {
            FrameLocator fl = parentFrame.frameLocator(selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }

        // 5) Final fallback
        return parentFrame.frameLocator(selNormalized);
    }

    // ============================================================
    // Auto-probing by full chain (preferred), with z-index ordering
    // ============================================================

    private FrameLocator pickBestFrameByChainProbe(Page page, String candidateSelector, String element, String key) {
        Locator iframes = page.locator(candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        List<Integer> order = indicesByZIndexDesc(iframes);

        // 1) First pass: return as soon as final chain is VISIBLE
        for (Integer idx : order) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = page.frameLocator(candidateSelector).nth(idx);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
        }

        // 2) Second pass: pick best by match count (fallback)
        int bestIdx = -1;
        int bestCount = -1;
        int bestZ = Integer.MIN_VALUE;

        for (Integer idx : order) {
            FrameLocator fl = page.frameLocator(candidateSelector).nth(idx);
            int countMatches = countFinalChainMatches(fl, element, key);
            int z = safeZIndex(iframes.nth(idx));

            if (countMatches > bestCount || (countMatches == bestCount && z > bestZ)) {
                bestCount = countMatches;
                bestZ = z;
                bestIdx = idx;
            }
        }

        if (bestIdx >= 0) {
            logger.info("Picked frame index {} by best count={} zIndex={}", bestIdx, bestCount, bestZ);
            return page.frameLocator(candidateSelector).nth(bestIdx);
        }
        return null;
    }

    private FrameLocator pickBestFrameByChainProbe(FrameLocator parentFrame, String candidateSelector, String element, String key) {
        Locator iframes = parentFrame.locator(candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        List<Integer> order = indicesByZIndexDesc(iframes);

        // 1) First pass: return as soon as final chain is VISIBLE
        for (Integer idx : order) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = parentFrame.frameLocator(candidateSelector).nth(idx);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
        }

        // 2) Second pass: pick best by match count (fallback)
        int bestIdx = -1;
        int bestCount = -1;
        int bestZ = Integer.MIN_VALUE;

        for (Integer idx : order) {
            FrameLocator fl = parentFrame.frameLocator(candidateSelector).nth(idx);
            int countMatches = countFinalChainMatches(fl, element, key);
            int z = safeZIndex(iframes.nth(idx));

            if (countMatches > bestCount || (countMatches == bestCount && z > bestZ)) {
                bestCount = countMatches;
                bestZ = z;
                bestIdx = idx;
            }
        }

        if (bestIdx >= 0) {
            logger.info("Picked nested frame index {} by best count={} zIndex={}", bestIdx, bestCount, bestZ);
            return parentFrame.frameLocator(candidateSelector).nth(bestIdx);
        }
        return null;
    }

    // ============================================================
    // Visibility & scoring helpers
    // ============================================================

    /** Build FULL chained locator inside a given frame context and check VISIBLE quickly. */
    private boolean isFinalChainVisibleInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(1200));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Count matches for the FULL chained locator inside a frame. */
    private int countFinalChainMatches(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = buildLocatorInContext(frame, element, key);
            return finalLocator.count();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Quick probe: is FIRST token visible in this frame? (legacy compatibility) */
    private boolean isFirstTokenVisibleInContext(FrameLocator frame, String element, String key) {
        String[] parts = normalizeAndSplitChain(elementLocatorHelper.getElement(element, key));
        String first = parts[0].trim();
        String t = parseType(first);
        String v = parseValue(first);
        Locator test = locatorHandler.getLocatorForType(t, frame, v);
        try {
            test.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(1000));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Build the FULL chained locator starting from a frame context. */
    private Locator buildLocatorInContext(FrameLocator frame, String element, String key) {
        String full = elementLocatorHelper.getElement(element, key);
        String[] chain = normalizeAndSplitChain(full);

        Locator current = null;
        for (int i = 0; i < chain.length; i++) {
            String part = chain[i].trim();
            String type = parseType(part);
            String value = parseValue(part);
            if (i == 0) {
                current = locatorHandler.getLocatorForType(type, frame, value);
            } else {
                current = locatorHandler.getLocatorForType(type, current, value);
            }
        }
        return current;
    }

    private boolean safeIsVisible(Locator loc) {
        try { return loc.isVisible(); } catch (Throwable t) { return false; }
    }

    private int safeZIndex(Locator iframeEl) {
        try {
            Object val = iframeEl.evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
            return (val instanceof Number) ? ((Number) val).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private List<Integer> indicesByZIndexDesc(Locator iframes) {
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);

        order.sort((a, b) -> Integer.compare(safeZIndex(iframes.nth(b)), safeZIndex(iframes.nth(a))));
        return order;
    }

    // ============================================================
    // Selector parsing & normalization
    // ============================================================

    /** Accept both " > " and "&gt;"; normalize to '>' and split. */
    private String[] normalizeAndSplitChain(String raw) {
        if (raw == null) return new String[0];
        String normalized = raw.replace("&gt;", ">");
        return normalized.split("\\s*>\\s*");
    }

    /** Extract TYPE from "TYPE_value" or return token itself if no underscore. */
    private String parseType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(0, idx) : token).trim();
    }

    /** Extract value from "TYPE_value"; returns "" if no underscore (unnamed role). */
    private String parseValue(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(idx + 1) : "").trim();
    }

    /** If selector is like "(iframe[...])[2]" return base + zero-based index; else null. */
    private IndexedSelector parseIndexedIframeSelector(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // Pattern matches: (iframe[...])[N]  where N is 1-based integer
        Pattern p = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$");
        Matcher m = p.matcher(s);
        if (!m.find()) return null;
        String base = m.group(1);
        int oneBased = Integer.parseInt(m.group(2));
        int zeroBased = Math.max(0, oneBased - 1);
        return new IndexedSelector(base, zeroBased);
    }

    /** Relax strict modal name like iframe[name="iframeWindowModal1857"] -> iframe[name^="iframeWindowModal"] */
    private String relaxModalSelectorIfNeeded(String iframeSelector) {
        if (iframeSelector == null) return null;
        String s = iframeSelector.trim();
        if (s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }
        return s;
    }

    private static final class IndexedSelector {
        final String base;           // e.g. iframe[frameborder='0px']
        final int indexZeroBased;    // converted from 1-based
        IndexedSelector(String base, int indexZeroBased) {
            this.base = base;
            this.indexZeroBased = indexZeroBased;
        }
    }

    // ============================================================
    // Action execution wrappers (unchanged public surface)
    // ============================================================
    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // Small non-fatal wait for modals to attach, if any are opening
            if (page != null) {
                try {
                    page.waitForSelector("iframe[name^='iframeWindowModal'], iframe[frameborder='0px']",
                            new Page.WaitForSelectorOptions().setTimeout(500));
                } catch (Throwable ignored) {}
            }

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

    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        FileChooser fileChooser = page.waitForFileChooser(() -> page.click(getElement(element, key)));
        fileChooser.setFiles(Paths.get(getElement(element, file_name)));
    }

    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = getElement(element, key);
        String fileName = extractFileName(documentLinkName);
        System.out.println(fileName);
        try {
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(fileName)).click();
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
        return parseValue(locatorValue);
    }
}