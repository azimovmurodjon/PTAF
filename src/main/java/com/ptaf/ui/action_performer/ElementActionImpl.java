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

public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    private final ActionPerformer actionPerformer = new ActionPerformer();
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    private final LocatorHandler locatorHandler = new LocatorHandler();

    public ElementActionImpl(Page page) { super(page); }

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

    private FrameLocator findFrameWithElement(Page page, String iframeSelector, String element, String key) {
        String sel = relaxModalSelectorIfNeeded(iframeSelector);

        if (sel != null && (sel.contains("iframeWindowModal") || sel.contains("frameborder"))) {
            try {
                page.waitForSelector("iframe[name^='iframeWindowModal'], iframe[frameborder='0px']",
                        new Page.WaitForSelectorOptions().setTimeout(1500));
            } catch (Throwable ignored) {}
            FrameLocator active = resolveActiveModalFrame(page);
            if (active != null) return active;
        }

        Locator iframeLocator = page.locator(sel);
        int count = iframeLocator.count();

        for (int i = 0; i < count; i++) {
            FrameLocator fl = page.frameLocator(sel).nth(i);
            String fullLocatorString = elementLocatorHelper.getElement(element, key);

            String[] locatorParts = normalizeAndSplitChain(fullLocatorString);
            String part = locatorParts[0].trim();
            String locatorType = parseType(part);
            String locator = parseValue(part);

            Locator testLocator = locatorHandler.getLocatorForType(locatorType, fl, locator);
            if (testLocator.count() > 0 && testLocator.isVisible()) {
                return fl;
            }
        }

        return page.frameLocator(sel);
    }

    private FrameLocator findFrameWithElement(FrameLocator parentFrame, String iframeSelector, String element, String key) {
        String sel = relaxModalSelectorIfNeeded(iframeSelector);

        if (sel != null && (sel.contains("iframeWindowModal") || sel.contains("frameborder"))) {
            try {
                // No parentFrame.page(): wait via a Locator in this frame context
                parentFrame
                        .locator("iframe[name^='iframeWindowModal'], iframe[frameborder='0px']")
                        .first()
                        .waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.ATTACHED)
                                .setTimeout(1500));
            } catch (Throwable ignored) {}
            FrameLocator active = resolveActiveModalFrame(parentFrame);
            if (active != null) return active;
        }

        Locator iframeLocator = parentFrame.locator(sel);
        int count = iframeLocator.count();

        for (int i = 0; i < count; i++) {
            FrameLocator fl = parentFrame.frameLocator(sel).nth(i);
            String fullLocatorString = elementLocatorHelper.getElement(element, key);

            String[] locatorParts = normalizeAndSplitChain(fullLocatorString);
            String part = locatorParts[0].trim();
            String locatorType = parseType(part);
            String locator = parseValue(part);

            Locator testLocator = locatorHandler.getLocatorForType(locatorType, fl, locator);
            if (testLocator.count() > 0 && testLocator.isVisible()) {
                return fl;
            }
        }

        return parentFrame.frameLocator(sel);
    }

    private FrameLocator resolveActiveModalFrame(Page page) {
        String modalSelector = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

        Locator iframes = page.locator(modalSelector);
        int count = iframes.count();
        if (count == 0) return null;

        int bestIndex = -1;
        int bestZ = Integer.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            Locator el = iframes.nth(i);

            boolean visible = false;
            try { visible = el.isVisible(); } catch (Throwable ignored) {}
            if (!visible) continue;

            int zIndex = 0;
            try {
                Object val = el.evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z, 10); return isNaN(n) ? 0 : n; }");
                if (val instanceof Number) {
                    zIndex = ((Number) val).intValue();
                }
            } catch (Throwable ignored) {}

            if (zIndex >= bestZ) {
                bestZ = zIndex;
                bestIndex = i;
            }
        }

        if (bestIndex >= 0) {
            return page.frameLocator(modalSelector).nth(bestIndex);
        }

        for (int i = count - 1; i >= 0; i--) {
            try {
                if (iframes.nth(i).isVisible()) {
                    return page.frameLocator(modalSelector).nth(i);
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private FrameLocator resolveActiveModalFrame(FrameLocator parentFrame) {
        String modalSelector = "iframe[name^='iframeWindowModal'], iframe[frameborder='0px']";

        Locator iframes = parentFrame.locator(modalSelector);
        int count = iframes.count();
        if (count == 0) return null;

        int bestIndex = -1;
        int bestZ = Integer.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            Locator el = iframes.nth(i);

            boolean visible = false;
            try { visible = el.isVisible(); } catch (Throwable ignored) {}
            if (!visible) continue;

            int zIndex = 0;
            try {
                Object val = el.evaluate("e => { const z = getComputedStyle(e).zIndex; const n = parseInt(z, 10); return isNaN(n) ? 0 : n; }");
                if (val instanceof Number) {
                    zIndex = ((Number) val).intValue();
                }
            } catch (Throwable ignored) {}

            if (zIndex >= bestZ) {
                bestZ = zIndex;
                bestIndex = i;
            }
        }

        if (bestIndex >= 0) {
            return parentFrame.frameLocator(modalSelector).nth(bestIndex);
        }

        for (int i = count - 1; i >= 0; i--) {
            try {
                if (iframes.nth(i).isVisible()) {
                    return parentFrame.frameLocator(modalSelector).nth(i);
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private String relaxModalSelectorIfNeeded(String iframeSelector) {
        if (iframeSelector == null) return null;
        String s = iframeSelector.trim();
        if (s.matches("iframe\\[name\\s*=\\s*\"iframeWindowModal\\d+\"\\]")) {
            return "iframe[name^=\"iframeWindowModal\"]";
        }
        return s;
    }

    private String[] normalizeAndSplitChain(String raw) {
        if (raw == null) return new String[0];
        String normalized = raw.replace("&gt;", ">");
        return normalized.split("\\s*>\\s*");
    }

    private String parseType(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(0, idx) : token).trim();
    }

    private String parseValue(String part) {
        if (part == null) return "";
        String token = part.trim();
        int idx = token.indexOf('_');
        return (idx >= 0 ? token.substring(idx + 1) : "").trim();
    }

    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3,
                                  String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
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