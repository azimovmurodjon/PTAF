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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementActionImpl – fast modal + iframe handling
 * This is your working version, but with:
 *  - reduced waits for iframe detection and visibility
 *  - shorter overlay wait
 *  - all public API unchanged
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    // ====== Tunable timings (reduced from original) ======
    private static final String OVERLAY_CANDIDATES = "#TranslucentOverlay__root, .modal-backdrop, .k-overlay, .cdk-overlay-backdrop";
    private static final String MODAL_IFRAME_CSS = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

    // How long to wait for iframe presence (used in findFrameWithElement)
    private static final double IFRAME_WAIT_MS = 400.0;             // was 1500

    // How long to wait for overlays to disappear before clicking
    private static final long OVERLAY_WAIT_MS = 1200L;              // was 4000

    // Visibility waits during frame probing
    private static final double VISIBLE_WAIT_MS = 500.0;            // was 1200 / 1000

    // Poll interval inside waitForNoOverlay
    private static final long OVERLAY_POLL_INTERVAL_MS = 60L;       // was 100

    public ElementActionImpl(Page page) {
        super(page);
    }

    // ============================================================
    // Public API (unchanged signatures)
    // ============================================================

    @Override
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return this.performAction(page, null, null, null, action, element, key, value, null);
    }

    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return this.performAction(null, null, null, null, action, element, key, value, frameLocator);
    }

    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                          String action, String element, String key, String value, FrameLocator frameLocator) {
        return this.performAction(page, iFrame, iFrame_2, iFrame_3, action, element, key, value, null);
    }

    @Override
    public boolean getElementHandlePage(Page page, String element, String key) {
        List<ElementHandle> elementHandles = this.getElementHandleList(page, element, key, null);
        return !elementHandles.isEmpty();
    }

    @Override
    public boolean getElementHandleFrame(FrameLocator frameLocator, String element, String key) {
        List<ElementHandle> elementHandles = this.getElementHandleList(null, element, key, frameLocator);
        return !elementHandles.isEmpty();
    }

    @Override
    public boolean assertElementTextPage(Page page, String element, String key, String expectedText) {
        return this.assertElementText(page, element, key, expectedText, null);
    }

    @Override
    public boolean assertElementTextFrame(FrameLocator frameLocator, String element, String key, String expectedText) {
        return this.assertElementText(null, element, key, expectedText, frameLocator);
    }

    @Override
    public String performActionPageWithReturn(Page page, String action, String element, String key, String value) {
        try {
            Locator targetLocator = this.getLocatorBasedOnPage(page, element, key);
            if (targetLocator == null) {
                return null;
            } else {
                this.waitForNoOverlay(page, OVERLAY_WAIT_MS);
                this.actionPerformer.waitForLocator(targetLocator);
                return this.actionPerformer.performActionWithReturn(page, action, targetLocator, value);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                                   String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            Locator targetLocator = this.getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                return null;
            } else {
                this.waitForNoOverlay(page, OVERLAY_WAIT_MS);
                this.actionPerformer.waitForLocator(targetLocator);
                return this.actionPerformer.performActionWithReturn(page, action, targetLocator, value);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        FileChooser fileChooser = page.waitForFileChooser(() -> page.click(this.getElement(element, key)));
        fileChooser.setFiles(Paths.get(this.getElement(element, file_name)));
    }

    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = this.getElement(element, key);
        String fileName = extractFileName(documentLinkName);
        System.out.println(fileName);

        try {
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(fileName)).click();
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator) {
        List<ElementHandle> elementHandles = new ArrayList<>();

        try {
            Locator targetLocator = this.getLocator(null, null, null, element, key, page, frameLocator);
            if (targetLocator != null) {
                elementHandles = targetLocator.elementHandles();
            }
        } catch (Exception ignored) {
        }

        return elementHandles;
    }

    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = this.elementLocatorHelper.getElement(element, key);
        return this.parseValue(locatorValue);
    }

    // ============================================================
    // Core locator building (frames + element chain)
    // ============================================================

    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3,
                              String element, String key, Page page, FrameLocator frameLocator) {
        String fullLocatorString = this.elementLocatorHelper.getElement(element, key);
        String[] locatorParts = this.normalizeAndSplitChain(fullLocatorString);
        Locator currentLocator = null;

        try {
            Object context = page;
            if (frameLocator != null) {
                context = frameLocator;
            } else if (iFrame != null && !iFrame.isEmpty()) {
                FrameLocator fl = this.findFrameWithElement(page, iFrame, element, key);
                if (iFrame_2 != null && !iFrame_2.isEmpty()) {
                    fl = this.findFrameWithElement(fl, iFrame_2, element, key);
                }
                if (iFrame_3 != null && !iFrame_3.isEmpty()) {
                    fl = this.findFrameWithElement(fl, iFrame_3, element, key);
                }
                context = fl;
            }

            for (int i = 0; i < locatorParts.length; ++i) {
                String part = locatorParts[i].trim();
                String locatorType = this.parseType(part);
                String locator = this.parseValue(part);
                if (i == 0) {
                    if (context instanceof Page) {
                        currentLocator = this.locatorHandler.getLocatorForType(locatorType, (Page) context, locator);
                    } else {
                        currentLocator = this.locatorHandler.getLocatorForType(locatorType, (FrameLocator) context, locator);
                    }
                } else {
                    currentLocator = this.locatorHandler.getLocatorForType(locatorType, currentLocator, locator);
                }
            }

            return currentLocator;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    // ============================================================
    // Action execution + modal fallback (same logic, faster waits)
    // ============================================================

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;

        try {
            if (page != null) {
                try {
                    page.waitForSelector(MODAL_IFRAME_CSS,
                            new Page.WaitForSelectorOptions().setTimeout(200.0));
                } catch (Throwable ignored) {
                }
            }

            if (frameLocator != null) {
                targetLocator = this.getLocator(null, null, null, element, key, null, frameLocator);
            } else {
                if (page == null) {
                    throw new IllegalArgumentException("A Page or FrameLocator context is required.");
                }
                targetLocator = this.getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
            }

            if (targetLocator == null) {
                throw new IllegalStateException("Failed to resolve a target Locator for element: " + element + " with key: " + key);
            } else {
                if (page != null) {
                    this.waitForNoOverlay(page, OVERLAY_WAIT_MS);
                }
                this.actionPerformer.waitForLocator(targetLocator);
                this.actionPerformer.performAction(page, action, targetLocator, value);
                return true;
            }
        } catch (Exception ignoredMain) {
            // Fallback: try again on topmost visible modals, but with the same reduced waits
            if (page != null) {
                List<FrameLocator> visibleModals = this.getVisibleModalFramesTopFirst(page);
                if (!visibleModals.isEmpty()) {
                    for (FrameLocator fl : visibleModals) {
                        try {
                            Locator alt = this.getLocator(null, null, null, element, key, null, fl);
                            if (alt != null) {
                                this.waitForNoOverlay(page, OVERLAY_WAIT_MS);
                                this.actionPerformer.waitForLocator(alt);
                                this.actionPerformer.performAction(page, action, alt, value);
                                return true;
                            }
                        } catch (Exception ignoredAlt) {
                        }
                    }
                }
            }
            return false;
        }
    }

    // ============================================================
    // Frame resolution (index-aware + modal-aware + probing)
    // ============================================================

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = this.parseIndexedIframeSelector(iframeSelector);
        String selBase = idxSel != null ? idxSel.base : iframeSelector;
        String selNormalized = this.relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));

        try {
            if (this.isXPathSelector(selNormalized)) {
                page.waitForSelector("xpath=//iframe",
                        new Page.WaitForSelectorOptions().setTimeout(IFRAME_WAIT_MS));
            } else {
                page.waitForSelector(looksModal ? MODAL_IFRAME_CSS : selNormalized,
                        new Page.WaitForSelectorOptions().setTimeout(IFRAME_WAIT_MS));
            }
        } catch (Throwable ignored) {
        }

        FrameLocator best;
        if (looksModal &&
                ("iframe[name^=\"iframeWindowModal\"]".equals(selNormalized) || MODAL_IFRAME_CSS.equals(selNormalized))) {
            best = this.topmostVisibleModal(page);
            if (best != null) {
                return best;
            }
        }

        if (idxSel != null) {
            best = this.frameLocatorForSelector(page, idxSel.base).nth(idxSel.indexZeroBased);
            if (this.isFinalChainVisibleInContext(best, element, key)) {
                return best;
            }
        }

        if (looksModal) {
            best = this.pickBestFrameByChainProbe(page, MODAL_IFRAME_CSS, element, key);
            if (best != null) {
                return best;
            }
        } else {
            best = this.pickBestFrameByChainProbe(page, selNormalized, element, key);
            if (best != null) {
                return best;
            }
        }

        Locator iframeLocator = this.locatorForSelector(page, selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; ++i) {
            FrameLocator fl = this.frameLocatorForSelector(page, selNormalized).nth(i);
            if (this.isFirstTokenVisibleInContext(fl, element, key)) {
                return fl;
            }
        }

        return this.frameLocatorForSelector(page, selNormalized);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        IndexedSelector idxSel = this.parseIndexedIframeSelector(iframeSelector);
        String selBase = idxSel != null ? idxSel.base : iframeSelector;
        String selNormalized = this.relaxModalSelectorIfNeeded(selBase);
        boolean looksModal = selNormalized != null &&
                (selNormalized.contains("iframeWindowModal") || selNormalized.contains("frameborder"));

        try {
            if (this.isXPathSelector(selNormalized)) {
                parentFrame.locator("xpath=//iframe").first().waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.ATTACHED)
                                .setTimeout(IFRAME_WAIT_MS)
                );
            } else {
                parentFrame.locator(looksModal ? MODAL_IFRAME_CSS : selNormalized).first().waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.ATTACHED)
                                .setTimeout(IFRAME_WAIT_MS)
                );
            }
        } catch (Throwable ignored) {
        }

        FrameLocator best;
        if (looksModal &&
                ("iframe[name^=\"iframeWindowModal\"]".equals(selNormalized) || MODAL_IFRAME_CSS.equals(selNormalized))) {
            best = this.topmostVisibleModal(parentFrame);
            if (best != null) {
                return best;
            }
        }

        if (idxSel != null) {
            best = this.frameLocatorForSelector(parentFrame, idxSel.base).nth(idxSel.indexZeroBased);
            if (this.isFinalChainVisibleInContext(best, element, key)) {
                return best;
            }
        }

        if (looksModal) {
            best = this.pickBestFrameByChainProbe(parentFrame, MODAL_IFRAME_CSS, element, key);
            if (best != null) {
                return best;
            }
        } else {
            best = this.pickBestFrameByChainProbe(parentFrame, selNormalized, element, key);
            if (best != null) {
                return best;
            }
        }

        Locator iframeLocator = this.locatorForSelector(parentFrame, selNormalized);
        int count = iframeLocator.count();
        for (int i = 0; i < count; ++i) {
            FrameLocator fl = this.frameLocatorForSelector(parentFrame, selNormalized).nth(i);
            if (this.isFirstTokenVisibleInContext(fl, element, key)) {
                return fl;
            }
        }

        return this.frameLocatorForSelector(parentFrame, selNormalized);
    }

    // ============================================================
    // Topmost modal helpers (unchanged behavior)
    // ============================================================

    private FrameLocator topmostVisibleModal(Page page) {
        Locator ifr = page.locator(MODAL_IFRAME_CSS);
        int count = ifr.count();
        if (count == 0) {
            return null;
        } else {
            int bestIdx = -1;
            int bestZ = Integer.MIN_VALUE;

            for (int i = 0; i < count; ++i) {
                Locator n = ifr.nth(i);
                if (this.safeIsVisible(n)) {
                    int z = this.safeZIndex(n);
                    if (z > bestZ) {
                        bestZ = z;
                        bestIdx = i;
                    }
                }
            }

            return bestIdx >= 0 ? page.frameLocator(MODAL_IFRAME_CSS).nth(bestIdx) : null;
        }
    }

    private FrameLocator topmostVisibleModal(FrameLocator parent) {
        Locator ifr = parent.locator(MODAL_IFRAME_CSS);
        int count = ifr.count();
        if (count == 0) {
            return null;
        } else {
            int bestIdx = -1;
            int bestZ = Integer.MIN_VALUE;

            for (int i = 0; i < count; ++i) {
                Locator n = ifr.nth(i);
                if (this.safeIsVisible(n)) {
                    int z = this.safeZIndex(n);
                    if (z > bestZ) {
                        bestZ = z;
                        bestIdx = i;
                    }
                }
            }

            return bestIdx >= 0 ? parent.frameLocator(MODAL_IFRAME_CSS).nth(bestIdx) : null;
        }
    }

    private List<FrameLocator> getVisibleModalFramesTopFirst(Page page) {
        Locator ifr = page.locator(MODAL_IFRAME_CSS);
        int count = ifr.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            order.add(i);
        }

        order.sort((a, b) -> Integer.compare(this.safeZIndex(ifr.nth(b)), this.safeZIndex(ifr.nth(a))));
        List<FrameLocator> result = new ArrayList<>();
        for (Integer idx : order) {
            if (this.safeIsVisible(ifr.nth(idx))) {
                result.add(page.frameLocator(MODAL_IFRAME_CSS).nth(idx));
            }
        }
        return result;
    }

    // ============================================================
    // Probing helpers (same algorithm, shorter waits)
    // ============================================================

    private FrameLocator pickBestFrameByChainProbe(Page page, String candidateSelector, String element, String key) {
        Locator iframes = this.locatorForSelector(page, candidateSelector);
        int count = iframes.count();
        if (count == 0) {
            return null;
        } else {
            List<Integer> order = this.indicesByZIndexDesc(iframes);
            for (Integer idx : order) {
                if (this.safeIsVisible(iframes.nth(idx))) {
                    FrameLocator fl = this.frameLocatorForSelector(page, candidateSelector).nth(idx);
                    if (this.isFinalChainVisibleInContext(fl, element, key)) {
                        return fl;
                    }
                }
            }

            int bestIdx = -1;
            int bestCount = -1;
            int bestZ = Integer.MIN_VALUE;
            for (Integer idx : order) {
                FrameLocator fl = this.frameLocatorForSelector(page, candidateSelector).nth(idx);
                int countMatches = this.countFinalChainMatches(fl, element, key);
                int z = this.safeZIndex(iframes.nth(idx));
                if (countMatches > bestCount || (countMatches == bestCount && z > bestZ)) {
                    bestCount = countMatches;
                    bestZ = z;
                    bestIdx = idx;
                }
            }
            if (bestIdx >= 0) {
                return this.frameLocatorForSelector(page, candidateSelector).nth(bestIdx);
            }
            return null;
        }
    }

    private FrameLocator pickBestFrameByChainProbe(FrameLocator parentFrame, String candidateSelector, String element, String key) {
        Locator iframes = this.locatorForSelector(parentFrame, candidateSelector);
        int count = iframes.count();
        if (count == 0) {
            return null;
        } else {
            List<Integer> order = this.indicesByZIndexDesc(iframes);
            for (Integer idx : order) {
                if (this.safeIsVisible(iframes.nth(idx))) {
                    FrameLocator fl = this.frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
                    if (this.isFinalChainVisibleInContext(fl, element, key)) {
                        return fl;
                    }
                }
            }

            int bestIdx = -1;
            int bestCount = -1;
            int bestZ = Integer.MIN_VALUE;
            for (Integer idx : order) {
                FrameLocator fl = this.frameLocatorForSelector(parentFrame, candidateSelector).nth(idx);
                int countMatches = this.countFinalChainMatches(fl, element, key);
                int z = this.safeZIndex(iframes.nth(idx));
                if (countMatches > bestCount || (countMatches == bestCount && z > bestZ)) {
                    bestCount = countMatches;
                    bestZ = z;
                    bestIdx = idx;
                }
            }
            if (bestIdx >= 0) {
                return this.frameLocatorForSelector(parentFrame, candidateSelector).nth(bestIdx);
            }
            return null;
        }
    }

    private boolean isFinalChainVisibleInContext(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = this.buildLocatorInContext(frame, element, key);
            finalLocator.first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(VISIBLE_WAIT_MS)
            );
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int countFinalChainMatches(FrameLocator frame, String element, String key) {
        try {
            Locator finalLocator = this.buildLocatorInContext(frame, element, key);
            return finalLocator.count();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private boolean isFirstTokenVisibleInContext(FrameLocator frame, String element, String key) {
        String[] parts = this.normalizeAndSplitChain(this.elementLocatorHelper.getElement(element, key));
        if (parts.length == 0) return false;

        String first = parts[0].trim();
        String t = this.parseType(first);
        String v = this.parseValue(first);
        Locator test = this.locatorHandler.getLocatorForType(t, frame, v);

        try {
            test.first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(VISIBLE_WAIT_MS)
            );
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Locator buildLocatorInContext(FrameLocator frame, String element, String key) {
        String full = this.elementLocatorHelper.getElement(element, key);
        String[] chain = this.normalizeAndSplitChain(full);
        Locator current = null;

        for (int i = 0; i < chain.length; ++i) {
            String part = chain[i].trim();
            String type = this.parseType(part);
            String value = this.parseValue(part);
            if (i == 0) {
                current = this.locatorHandler.getLocatorForType(type, frame, value);
            } else {
                current = this.locatorHandler.getLocatorForType(type, current, value);
            }
        }

        return current;
    }

    // ============================================================
    // Small utilities
    // ============================================================

    private boolean safeIsVisible(Locator loc) {
        try {
            return loc.isVisible();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int safeZIndex(Locator iframeEl) {
        try {
            Object val = iframeEl.evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
            return val instanceof Number ? ((Number) val).intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private List<Integer> indicesByZIndexDesc(Locator iframes) {
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            order.add(i);
        }
        order.sort((a, b) -> Integer.compare(this.safeZIndex(iframes.nth(b)), this.safeZIndex(iframes.nth(a))));
        return order;
    }

    private String[] normalizeAndSplitChain(String raw) {
        if (raw == null) {
            return new String[0];
        } else {
            String normalized = raw.replace("&gt;", ">");
            return normalized.split("\\s*>\\s*");
        }
    }

    private String parseType(String part) {
        if (part == null) {
            return "";
        } else {
            String token = part.trim();
            int idx = token.indexOf('_');
            return (idx >= 0 ? token.substring(0, idx) : token).trim();
        }
    }

    private String parseValue(String part) {
        if (part == null) {
            return "";
        } else {
            String token = part.trim();
            int idx = token.indexOf('_');
            return (idx >= 0 ? token.substring(idx + 1) : "").trim();
        }
    }

    private IndexedSelector parseIndexedIframeSelector(String raw) {
        if (raw == null) {
            return null;
        } else {
            String s = raw.trim();
            Pattern xPathIndexed = Pattern.compile("^\\(\\s*(//iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
            Matcher mx = xPathIndexed.matcher(s);
            if (mx.find()) {
                String base = mx.group(1);
                int oneBased = Integer.parseInt(mx.group(2));
                return new IndexedSelector(base, Math.max(0, oneBased - 1));
            } else {
                Pattern cssIndexed = Pattern.compile("^\\(\\s*(iframe\\[[^\\]]+\\])\\s*\\)\\s*\\[(\\d+)\\]\\s*$", Pattern.CASE_INSENSITIVE);
                Matcher mc = cssIndexed.matcher(s);
                if (mc.find()) {
                    String base = mc.group(1);
                    int oneBased = Integer.parseInt(mc.group(2));
                    return new IndexedSelector(base, Math.max(0, oneBased - 1));
                } else {
                    return null;
                }
            }
        }
    }

    private String relaxModalSelectorIfNeeded(String sel) {
        if (sel == null) {
            return null;
        } else {
            String s = sel.trim();
            return s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]") ? "iframe[name^=\"iframeWindowModal\"]" : s;
        }
    }

    private boolean isXPathSelector(String sel) {
        if (sel == null) {
            return false;
        } else {
            String s = sel.trim();
            return s.startsWith("//") || s.startsWith("(");
        }
    }

    private Locator locatorForSelector(Page page, String sel) {
        return this.isXPathSelector(sel) ? page.locator("xpath=" + sel) : page.locator(sel);
    }

    private Locator locatorForSelector(FrameLocator frame, String sel) {
        return this.isXPathSelector(sel) ? frame.locator("xpath=" + sel) : frame.locator(sel);
    }

    private FrameLocator frameLocatorForSelector(Page page, String sel) {
        return this.isXPathSelector(sel) ? page.frameLocator("xpath=" + sel) : page.frameLocator(sel);
    }

    private FrameLocator frameLocatorForSelector(FrameLocator frame, String sel) {
        return this.isXPathSelector(sel) ? frame.frameLocator("xpath=" + sel) : frame.frameLocator(sel);
    }

    private void waitForNoOverlay(Page page, long timeoutMs) {
        try {
            Locator overlay = page.locator(OVERLAY_CANDIDATES);
            if (overlay.count() == 0) {
                return;
            }

            long end = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < end) {
                boolean anyVisible = false;
                int c = overlay.count();

                for (int i = 0; i < c; ++i) {
                    try {
                        if (overlay.nth(i).isVisible()) {
                            anyVisible = true;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }

                if (!anyVisible) {
                    return;
                }

                Thread.sleep(OVERLAY_POLL_INTERVAL_MS);
            }

            try {
                overlay.first().waitFor(
                        new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.HIDDEN)
                                .setTimeout(200.0)
                );
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignoredOuter) {
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
            throw e;
        }
    }

    private boolean assertElementText(Page page, String element, String key, String expectedText, FrameLocator frameLocator) {
        try {
            Locator targetLocator = this.getLocator(null, null, null, element, key, page, frameLocator);
            String actualText = targetLocator.first().textContent();
            return expectedText.equals(actualText);
        } catch (Exception ignored) {
            return false;
        }
    }

    private Locator getLocatorBasedOnPage(Page page, String element, String key) {
        return this.getLocator(null, null, null, element, key, page, null);
    }

    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String key) {
        return this.getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
    }

    @SuppressWarnings("unused")
    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return this.getLocator(null, null, null, element, key, null, frameLocator);
    }

    private static final class IndexedSelector {
        final String base;
        final int indexZeroBased;

        IndexedSelector(String base, int indexZeroBased) {
            this.base = base;
            this.indexZeroBased = indexZeroBased;
        }
    }
}