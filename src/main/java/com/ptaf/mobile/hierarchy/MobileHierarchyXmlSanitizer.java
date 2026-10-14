package com.ptaf.mobile.hierarchy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Set;

/**
 * Parses and optionally redacts Appium XML hierarchy content before it is persisted as test evidence.
 *
 * <p>Redaction intentionally protects rendered values while retaining hierarchy structure,
 * element class, resource identifier, package/bundle information, bounds, and accessibility
 * metadata that testers need for locator analysis. The unsanitized XML is never logged.</p>
 */
public final class MobileHierarchyXmlSanitizer {
    private static final Set<String> VALUE_ATTRIBUTES = Set.of(
            "text", "value", "label", "hint", "password", "content-desc", "contentdescription", "name"
    );

    private MobileHierarchyXmlSanitizer() {
        throw new IllegalStateException("Utility class");
    }

    /** Parses a UI hierarchy XML document with external entity processing disabled. */
    public static Document parse(String hierarchyXml) {
        if (hierarchyXml == null || hierarchyXml.isBlank()) {
            throw new IllegalArgumentException("Mobile UI hierarchy XML cannot be blank.");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(hierarchyXml)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse the current mobile UI hierarchy XML.", exception);
        }
    }

    /**
     * Produces safe evidence XML. When redaction is enabled, populated user-facing attributes
     * are replaced with {@code <redacted>} while locator-relevant tree structure remains intact.
     */
    public static String sanitize(String hierarchyXml, boolean redactValues) {
        Document document = parse(hierarchyXml);
        if (redactValues) {
            redactNode(document.getDocumentElement());
        }
        return serialize(document);
    }

    private static void redactNode(Node node) {
        if (node instanceof Element element) {
            for (int index = 0; index < element.getAttributes().getLength(); index++) {
                Node attribute = element.getAttributes().item(index);
                String name = attribute.getNodeName().toLowerCase(Locale.ROOT);
                if (VALUE_ATTRIBUTES.contains(name) && !attribute.getNodeValue().isBlank()) {
                    attribute.setNodeValue("<redacted>");
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.TEXT_NODE && !child.getTextContent().trim().isEmpty()) {
                child.setTextContent("<redacted>");
            } else {
                redactNode(child);
            }
        }
    }

    private static String serialize(Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter output = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize sanitized mobile UI hierarchy XML.", exception);
        }
    }
}
