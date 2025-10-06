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
 * ElementActionImpl
 *
 * Backward-compatible element orchestration with enhanced, resilient iframe/modal handling.
 * NEW: Supports XPath-like indexed iframe selectors e.g. "(iframe[frameborder='0px'])[2]".
 *       We parse, convert to frameLocator(base).nth(index-1), verify target visibility,
 *       otherwise fall back to modal probing and legacy scanning.
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
    // Frame resolution (ENHANCED: index-aware + modal-aware probing)
    // ============================================================
    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        // Parse first chain segment (used for visibility probing)
        String full = elementLocatorHelper.getElement(element, key);
        String[] parts = normalizeAndSplitChain(full);
        String firstPart = parts[0].trim();
        String firstType = parseType(firstPart);
        String firstValue = parseValue(firstPart);

        // 1) Handle XPath-like index form: (iframe[...])[N]
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        if (idxSel != null) {
            // Try the specific index first (0-based)
            FrameLocator candidate = page.frameLocator(idxSel.base).nth(idxSel.indexZeroBased);
            Locator test = locatorHandler.getLocatorForType(firstType, candidate, firstValue);
            if (isLocatorVisibleQuick(test)) {
                logger.info("Resolved indexed iframe {}[{}] for target [{}:{}]", idxSel.base, idxSel.indexZeroBased, firstType, firstValue);
                return candidate;
            }
            // If not visible there, continue with modal/legacy fallback below.
        }

        // 2) Relax/normalize modal selector if needed
        String sel = relaxModalSelectorIfNeeded( (idxSel != null) ? idxSel.base : iframeSelector );

        // 3) If it's modal-like, probe all modal frames in z-index order
        if (sel != null && (sel.contains("iframeWindowModal") || sel.contains("frameborder"))) {
            String modalSelector = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";
            try {
                page.waitForSelector(modalSelector, new Page.WaitForSelectorOptions().setTimeout(1500));
            } catch (Throwable ignored) {}

            FrameLocator probed = probeModalFramesForTarget(page, modalSelector, firstType, firstValue);
            if (probed != null) return probed;
        }

        // 4) Legacy fallback scanning (kept for backward compatibility)
        Locator iframeLocator = page.locator(sel);
        int count = iframeLocator.count();

        for (int i = 0; i < count; i++) {
            FrameLocator fl = page.frameLocator(sel).nth(i);
            Locator testLocator = locatorHandler.getLocatorForType(firstType, fl, firstValue);
            if (testLocator.count() > 0 && testLocator.isVisible()) {
                return fl;
            }
        }

        // 5) Final fallback to first matching frame
        return page.frameLocator(sel);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        // Parse first chain segment (used for visibility probing)
        String full = elementLocatorHelper.getElement(element, key);
        String[] parts = normalizeAndSplitChain(full);
        String firstPart = parts[0].trim();
        String firstType = parseType(firstPart);
        String firstValue = parseValue(firstPart);

        // 1) Handle XPath-like index form: (iframe[...])[N]
        IndexedSelector idxSel = parseIndexedIframeSelector(iframeSelector);
        if (idxSel != null) {
            FrameLocator candidate = parentFrame.frameLocator(idxSel.base).nth(idxSel.indexZeroBased);
            Locator test = locatorHandler.getLocatorForType(firstType, candidate, firstValue);
            if (isLocatorVisibleQuick(test)) {
                logger.info("Resolved nested indexed iframe {}[{}] for target [{}:{}]", idxSel.base, idxSel.indexZeroBased, firstType, firstValue);
                return candidate;
            }
            // else continue
        }

        // 2) Relax/normalize modal selector if needed
        String sel = relaxModalSelectorIfNeeded( (idxSel != null) ? idxSel.base : iframeSelector );

        // 3) Modal probing in nested context
        if (sel != null && (sel.contains("iframeWindowModal") || sel.contains("frameborder"))) {
            String modalSelector = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";
            try {
                parentFrame.locator(modalSelector).first()
                        .waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.ATTACHED)
                                .setTimeout(1500));
            } catch (Throwable ignored) {}

            FrameLocator probed = probeModalFramesForTarget(parentFrame, modalSelector, firstType, firstValue);
            if (probed != null) return probed;
        }

        // 4) Legacy fallback scanning
        Locator iframeLocator = parentFrame.locator(sel);
        int count = iframeLocator.count();

        for (int i = 0; i < count; i++) {
            FrameLocator fl = parentFrame.frameLocator(sel).nth(i);
            Locator testLocator = locatorHandler.getLocatorForType(firstType, fl, firstValue);
            if (testLocator.count() > 0 && testLocator.isVisible()) {
                return fl;
            }
        }

        // 5) Final fallback
        return parentFrame.frameLocator(sel);
    }

    // ============================================================
    // Modal helpers: candidate ordering + visibility probing
    // ============================================================
    private List<Integer> getModalCandidateOrder(Page page, String modalSelector) {
        Locator iframes = page.locator(modalSelector);
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);

        order.sort((a, b) -> {
            int za = 0, zb = 0;
            try {
                Object va = iframes.nth(a).evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
                if (va instanceof Number) za = ((Number) va).intValue();
            } catch (Throwable ignored) {}
            try {
                Object vb = iframes.nth(b).evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
                if (vb instanceof Number) zb = ((Number) vb).intValue();
            } catch (Throwable ignored) {}
            return Integer.compare(zb, za);
        });
        return order;
    }

    private List<Integer> getModalCandidateOrder(FrameLocator parentFrame, String modalSelector) {
        Locator iframes = parentFrame.locator(modalSelector);
        int count = iframes.count();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) order.add(i);

        order.sort((a, b) -> {
            int za = 0, zb = 0;
            try {
                Object va = iframes.nth(a).evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
                if (va instanceof Number) za = ((Number) va).intValue();
            } catch (Throwable ignored) {}
            try {
                Object vb = iframes.nth(b).evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z,10); return isNaN(n) ? 0 : n; }");
                if (vb instanceof Number) zb = ((Number) vb).intValue();
            } catch (Throwable ignored) {}
            return Integer.compare(zb, za);
        });
        return order;
    }

    private FrameLocator probeModalFramesForTarget(Page page, String modalSelector, String firstType, String firstValue) {
        Locator modalEls = page.locator(modalSelector);
        List<Integer> order = getModalCandidateOrder(page, modalSelector);

        for (Integer idx : order) {
            try {
                Locator modalEl = modalEls.nth(idx);
                if (!modalEl.isVisible()) continue;

                FrameLocator fl = page.frameLocator(modalSelector).nth(idx);
                Locator test = locatorHandler.getLocatorForType(firstType, fl, firstValue);

                if (isLocatorVisibleQuick(test)) {
                    logger.info("Resolved modal frame index {} for target [{}:{}]", idx, firstType, firstValue);
                    return fl;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private FrameLocator probeModalFramesForTarget(FrameLocator parentFrame, String modalSelector, String firstType, String firstValue) {
        Locator modalEls = parentFrame.locator(modalSelector);
        List<Integer> order = getModalCandidateOrder(parentFrame, modalSelector);

        for (Integer idx : order) {
            try {
                Locator modalEl = modalEls.nth(idx);
                if (!modalEl.isVisible()) continue;

                FrameLocator fl = parentFrame.frameLocator(modalSelector).nth(idx);
                Locator test = locatorHandler.getLocatorForType(firstType, fl, firstValue);

                if (isLocatorVisibleQuick(test)) {
                    logger.info("Resolved nested modal frame index {} for target [{}:{}]", idx, firstType, firstValue);
                    return fl;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // ============================================================
    // Selector parsing helpers
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

    private boolean isLocatorVisibleQuick(Locator test) {
        try {
            test.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(1000));
            return true;
        } catch (Throwable t) {
            return false;
        }
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