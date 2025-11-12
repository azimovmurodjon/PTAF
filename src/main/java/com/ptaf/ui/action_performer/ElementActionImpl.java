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
 * ElementActionImpl (Topmost-Modal + Overlay-Guard + XPath index support)
 *
 * - Modal selection:
 *    * Relaxes strict modal names to starts-with: iframe[name^="iframeWindowModal"]
 *    * Always picks the topmost visible modal by z-index, and verifies the target chain exists inside
 *    * Works in nested contexts (outer modal contains inner modal)
 *
 * - Overlay guard:
 *    * Waits for #TranslucentOverlay__root to become hidden/absent or non-intercepting before clicking
 *
 * - Index-aware selectors:
 *    * Supports (iframe[...])[2] and (//iframe[@...])[2]
 *
 * - Backward compatible with your YAML chain + LocatorHandler role mapping
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private static final String MODAL_PREFIX_CSS = "iframe[name^='iframeWindowModal']";
    private static final String FRAMEBORDER_CSS = "iframe[frameborder='0px']";
    private static final String MODAL_CANDIDATES = MODAL_PREFIX_CSS + ", " + FRAMEBORDER_CSS;
    private static final String OVERLAY_ID = "#TranslucentOverlay__root";

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    public ElementActionImpl(Page page) { super(page); }

    // ============================================================
    // Core locator building (frames + element chain)
    // ============================================================
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {

        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        String[] locatorParts = normalizeAndSplitChain(fullLocatorString);
        Locator currentLocator;

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

            currentLocator = buildChain(context, locatorParts);
            return currentLocator;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    private Locator buildChain(Object context, String[] locatorParts) {
        Locator current = null;
        for (int i = 0; i < locatorParts.length; i++) {
            String part = locatorParts[i].trim();
            String locatorType = parseType(part);
            String locatorVal = parseValue(part);

            if (i == 0) {
                if (context instanceof Page) {
                    current = locatorHandler.getLocatorForType(locatorType, (Page) context, locatorVal);
                } else {
                    current = locatorHandler.getLocatorForType(locatorType, (FrameLocator) context, locatorVal);
                }
            } else {
                current = locatorHandler.getLocatorForType(locatorType, current, locatorVal);
            }
        }
        return current;
    }

    // ============================================================
    // Frame resolution (Index-aware + Modal-aware + Auto-probing)
    // ============================================================

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;

        String selNormalized = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = isModalLike(selNormalized);

        // Small non-fatal wait for candidates
        try {
            if (isXPathSelector(selNormalized)) {
                page.waitForSelector("xpath=//iframe", new Page.WaitForSelectorOptions().setTimeout(1200));
            } else {
                page.waitForSelector(looksModal ? MODAL_CANDIDATES : selNormalized,
                        new Page.WaitForSelectorOptions().setTimeout(1200));
            }
        } catch (Throwable ignored) {}

        // 1) Indexed first
        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(page, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                logger.info("Resolved indexed iframe {}[{}] (page)", idxSel.base, idxSel.indexZeroBased);
                return candidate;
            }
            logger.info("Indexed iframe {}[{}] didn't match, probing...", idxSel.base, idxSel.indexZeroBased);
        }

        // 2) Modal auto-probe (topmost visible by z-index, chain-aware)
        if (looksModal) {
            FrameLocator best = pickBestFrameByChainProbe(page, MODAL_CANDIDATES, element, key);
            if (best != null) {
                logger.info("Topmost visible modal selected (page): {}", MODAL_PREFIX_CSS);
                return best;
            }
        } else {
            FrameLocator best = pickBestFrameByChainProbe(page, selNormalized, element, key);
            if (best != null) return best;
        }

        // 3) Legacy fallback scan
        Locator ifrs = locatorForSelector(page, selNormalized);
        for (int i = 0, n = ifrs.count(); i < n; i++) {
            FrameLocator fl = frameLocatorForSelector(page, selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }

        // 4) Final fallback
        return frameLocatorForSelector(page, selNormalized);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        String selBase = (idxSel != null) ? idxSel.base : iframeSelector;

        String selNormalized = relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = isModalLike(selNormalized);

        try {
            if (isXPathSelector(selNormalized)) {
                parentFrame.locator("xpath=//iframe").first()
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(1200));
            } else {
                parentFrame.locator(looksModal ? MODAL_CANDIDATES : selNormalized).first()
                        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(1200));
            }
        } catch (Throwable ignored) {}

        if (idxSel != null) {
            FrameLocator candidate = frameLocatorForSelector(parentFrame, idxSel.base).nth(idxSel.indexZeroBased);
            if (isFinalChainVisibleInContext(candidate, element, key)) {
                logger.info("Resolved nested indexed iframe {}[{}]", idxSel.base, idxSel.indexZeroBased);
                return candidate;
            }
            logger.info("Nested indexed iframe {}[{}] didn't match, probing...", idxSel.base, idxSel.indexZeroBased);
        }

        if (looksModal) {
            FrameLocator best = pickBestFrameByChainProbe(parentFrame, MODAL_CANDIDATES, element, key);
            if (best != null) {
                logger.info("Topmost visible modal selected (nested): {}", MODAL_PREFIX_CSS);
                return best;
            }
        } else {
            FrameLocator best = pickBestFrameByChainProbe(parentFrame, selNormalized, element, key);
            if (best != null) return best;
        }

        Locator ifrs = locatorForSelector(parentFrame, selNormalized);
        for (int i = 0, n = ifrs.count(); i < n; i++) {
            FrameLocator fl = frameLocatorForSelector(parentFrame, selNormalized).nth(i);
            if (isFirstTokenVisibleInContext(fl, element, key)) return fl;
        }
        return frameLocatorForSelector(parentFrame, selNormalized);
    }

    private boolean isModalLike(String selNormalized) {
        return selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));
    }

    // ============================================================
    // Auto-probing by full chain (preferred), with z-index ordering
    // ============================================================

    private FrameLocator pickBestFrameByChainProbe(Page page, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(page, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        for (Integer idx : indicesByZIndexDesc(iframes)) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = frameLocatorForSelector(page, candidateSelector).nth(idx);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
        }
        // Weak fallback: first visible
        for (Integer idx : indicesByZIndexDesc(iframes)) {
            if (safeIsVisible(iframes.nth(idx))) return frameLocatorForSelector(page, candidateSelector).nth(idx);
        }
        return null;
    }

    private FrameLocator pickBestFrameByChainProbe(FrameLocator parentFrame, String candidateSelector, String element, String key) {
        Locator iframes = locatorForSelector(parentFrame, candidateSelector);
        int count = iframes.count();
        if (count == 0) return null;

        for (Integer idx : indicesByZIndexDesc(iframes)) {
            if (!safeIsVisible(iframes.nth(idx))) continue;
            FrameLocator fl = frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
            if (isFinalChainVisibleInContext(fl, element, key)) return fl;
        }
        for (Integer idx : indicesByZIndexDesc(iframes)) {
            if (safeIsVisible(iframes.nth(idx))) return frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
        }
        return null;
    }

    // ============================================================
    // Visibility & scoring helpers
    // ============================================================

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

    // ============================================================
    // Overlay guard
    // ============================================================

    private void waitForOverlayGone(Page page) {
        if (page == null) return;
        Locator overlay = page.locator(OVERLAY_ID);
        try {
            // Hidden or detached
            overlay.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(3000));
        } catch (Throwable ignored) {
            // If still present but transparent/non-intercepting, allow proceed
            try {
                page.waitForFunction("el => !el || getComputedStyle(el).pointerEvents === 'none' || getComputedStyle(el).opacity === '0'",
                        overlay, new Page.WaitForFunctionOptions().setTimeout(1500));
            } catch (Throwable ignored2) {}
        }
    }

    // ============================================================
    // Selector parsing & normalization (CSS + XPath)
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

    /**
     * Parse indexed iframe selector.
     * Supports:
     *   (iframe[...])[2]                    // CSS-like
     *   (//iframe[@attr='v'])[2]            // XPath
     */
    private IndexedSelector parseIndexedIframeSelector(String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        Pattern xPathIndexed = Pattern.compile("^\\(\\s*(//iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mx = xPathIndexed.matcher(s);
        if (mx.find()) {
            String base = mx.group(1);
            int oneBased = Integer.parseInt(mx.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        Pattern cssIndexed = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
        Matcher mc = cssIndexed.matcher(s);
        if (mc.find()) {
            String base = mc.group(1);
            int oneBased = Integer.parseInt(mc.group(2));
            return new IndexedSelector(base, Math.max(0, oneBased - 1));
        }

        return null;
    }

    /** If strict modal name like iframe[name="iframeWindowModal1857"] → relax to starts-with */
    private String relaxModalSelectorIfNeeded(String sel) {
        if (sel == null) return null;
        String s = sel.trim();

        // CSS strict → relax
        if (s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }
        return s;
    }

    private static final class IndexedSelector {
        final String base;
        final int indexZeroBased;
        IndexedSelector(String base, int indexZeroBased) {
            this.base = base;
            this.indexZeroBased = indexZeroBased;
        }
    }

    private boolean isXPathSelector(String sel) {
        if (sel == null) return false;
        String s = sel.trim();
        return s.startsWith("//") || s.startsWith("(");
    }

    private Locator locatorForSelector(Page page, String sel) {
        return isXPathSelector(sel) ? page.locator("xpath=" + sel) : page.locator(sel);
    }

    private Locator locatorForSelector(FrameLocator frame, String sel) {
        return isXPathSelector(sel) ? frame.locator("xpath=" + sel) : frame.locator(sel);
    }

    private FrameLocator frameLocatorForSelector(Page page, String sel) {
        return isXPathSelector(sel) ? page.frameLocator("xpath=" + sel) : page.frameLocator(sel);
    }

    private FrameLocator frameLocatorForSelector(FrameLocator frame, String sel) {
        return isXPathSelector(sel) ? frame.frameLocator("xpath=" + sel) : frame.frameLocator(sel);
    }

    private boolean safeIsVisible(Locator loc) {
        try { return loc.isVisible(); } catch (Throwable t) { return false; }
    }

    private List<Integer> indicesByZIndexDesc(Locator iframes) {
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);
        order.sort((a, b) -> {
            int zb = safeZIndex(iframes.nth(b));
            int za = safeZIndex(iframes.nth(a));
            return Integer.compare(zb, za);
        });
        return order;
    }

    private int safeZIndex(Locator iframeEl) {
        try {
            Object val = iframeEl.evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
            return (val instanceof Number) ? ((Number) val).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ============================================================
    // Action execution wrappers (public API unchanged)
    // ============================================================

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // short wait for modals to attach/animate
            if (page != null) {
                try {
                    page.waitForSelector(MODAL_CANDIDATES, new Page.WaitForSelectorOptions().setTimeout(500));
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
                throw new IllegalStateException("Failed to resolve target Locator for element: " + element + " key: " + key);
            }

            // Overlay guard BEFORE interaction
            waitForOverlayGone(page);

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
                logger.error("Locator not found for element: {} key: {}", element, key);
                return null;
            }
            waitForOverlayGone(page);
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageWithReturn for element '{}' action '{}':", element, action, e);
            return null;
        }
    }

    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                                   String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            Locator targetLocator = getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for nested frame element: {} key: {}", element, key);
                return null;
            }
            waitForOverlayGone(page);
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageFrameWithReturn for element '{}' action '{}':", element, action, e);
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