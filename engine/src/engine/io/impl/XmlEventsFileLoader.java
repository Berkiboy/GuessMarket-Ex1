package engine.io.impl;

import engine.exceptions.FileValidationException;
import engine.io.EventsFileLoader;
import engine.model.CommissionType;
import engine.model.Event;
import engine.model.Option;
import engine.model.trading.LmsrTradingMethod;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// DOM-based parsing (no JAXB, no external dependency)
public class XmlEventsFileLoader implements EventsFileLoader {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int OPTIONS_PER_EVENT = 2;

    @Override
    public List<Event> load(String path) throws FileValidationException {
        File file = new File(path);
        if (!file.isFile()) {
            throw new FileValidationException("File not found: \"" + path + "\".");
        }
        if (!path.toLowerCase().endsWith(".xml")) {
            throw new FileValidationException("File must have an .xml extension: \"" + path + "\".");
        }

        Document document = parse(file);

        NodeList eventNodes = document.getElementsByTagName("GM-event");
        if (eventNodes.getLength() == 0) {
            throw new FileValidationException("The file does not contain any GM-event elements.");
        }

        List<String> problems = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            String eventLabel = "GM-event #" + (i + 1);
            try {
                Event event = parseEvent(eventElement, eventLabel, seenIds);
                events.add(event);
            } catch (FileValidationException e) {
                problems.add(e.getMessage());
            }
        }

        if (!problems.isEmpty()) {
            throw new FileValidationException(String.join(" | ", problems));
        }

        return events;
    }

    private Event parseEvent(Element eventElement, String eventLabel, Set<Integer> seenIds)
            throws FileValidationException {

        String name = attribute(eventElement, "name", eventLabel);

        int id = intElement(eventElement, "id", eventLabel);
        if (!seenIds.add(id)) {
            throw new FileValidationException(eventLabel + " (\"" + name + "\"): duplicate event id " + id + ".");
        }

        String description = textElement(eventElement, "description", eventLabel);

        // some official course files misspell this element as "comision" (single m); accept both
        Element commissionElement = optionalChild(eventElement, "commission");
        if (commissionElement == null) {
            commissionElement = optionalChild(eventElement, "comision");
        }
        if (commissionElement == null) {
            throw new FileValidationException(eventLabel + ": missing required element \"commission\".");
        }
        int commissionPercent = parseInt(commissionElement.getTextContent(), eventLabel, "commission");
        if (commissionPercent < MIN_COMMISSION || commissionPercent > MAX_COMMISSION) {
            throw new FileValidationException(eventLabel + " (\"" + name + "\"): commission must be between "
                    + MIN_COMMISSION + " and " + MAX_COMMISSION + ", got " + commissionPercent + ".");
        }
        String commissionTypeValue = commissionElement.getAttribute("type");
        CommissionType commissionType;
        try {
            commissionType = CommissionType.fromXmlValue(commissionTypeValue);
        } catch (IllegalArgumentException e) {
            throw new FileValidationException(eventLabel + " (\"" + name
                    + "\"): commission type must be \"on-purchase\" or \"on-close\", got \""
                    + commissionTypeValue + "\".");
        }

        List<String> optionNames = parseOptions(eventElement, eventLabel, name);
        List<Option> options = new ArrayList<>();
        for (String optionName : optionNames) {
            options.add(new Option(optionName));
        }

        int b = parseLmsrB(eventElement, eventLabel, name);

        return new Event(id, name, description, commissionPercent, commissionType, options,
                new LmsrTradingMethod(b, options.size()));
    }

    private List<String> parseOptions(Element eventElement, String eventLabel, String eventName)
            throws FileValidationException {
        Element optionsElement = requiredChild(eventElement, "GM-options", eventLabel);
        NodeList optionNodes = optionsElement.getElementsByTagName("GM-option");

        List<String> optionNames = new ArrayList<>();
        for (int i = 0; i < optionNodes.getLength(); i++) {
            String optionName = optionNodes.item(i).getTextContent().trim();
            if (optionName.isEmpty()) {
                throw new FileValidationException(eventLabel + " (\"" + eventName + "\"): GM-option #" + (i + 1)
                        + " is empty.");
            }
            optionNames.add(optionName);
        }

        if (optionNames.size() != OPTIONS_PER_EVENT) {
            throw new FileValidationException(eventLabel + " (\"" + eventName + "\"): must have exactly "
                    + OPTIONS_PER_EVENT + " options, found " + optionNames.size() + ".");
        }
        return optionNames;
    }

    private int parseLmsrB(Element eventElement, String eventLabel, String eventName) throws FileValidationException {
        Element methodElement = requiredChild(eventElement, "GM-method", eventLabel);
        Element lmsrElement = requiredChild(methodElement, "GM-LMSR", eventLabel);
        int b = intElement(lmsrElement, "b", eventLabel);
        if (b <= 0) {
            throw new FileValidationException(eventLabel + " (\"" + eventName
                    + "\"): b (liquidity) must be a positive integer, got " + b + ".");
        }
        return b;
    }

    private Document parse(File file) throws FileValidationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();
            return document;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new FileValidationException("The file is not a well-formed XML file: " + e.getMessage());
        }
    }

    private String attribute(Element element, String attributeName, String context) throws FileValidationException {
        String value = element.getAttribute(attributeName);
        if (value == null || value.trim().isEmpty()) {
            throw new FileValidationException(context + ": missing required attribute \"" + attributeName + "\".");
        }
        return value.trim();
    }

    private Element requiredChild(Element parent, String tagName, String context) throws FileValidationException {
        Element child = optionalChild(parent, tagName);
        if (child == null) {
            throw new FileValidationException(context + ": missing required element \"" + tagName + "\".");
        }
        return child;
    }

    private Element optionalChild(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getParentNode() == parent) { // getElementsByTagName searches the whole subtree, not just direct children
                return (Element) child;
            }
        }
        return null;
    }

    private String textElement(Element parent, String tagName, String context) throws FileValidationException {
        Element child = requiredChild(parent, tagName, context);
        String text = child.getTextContent().trim();
        if (text.isEmpty()) {
            throw new FileValidationException(context + ": element \"" + tagName + "\" must not be empty.");
        }
        return text;
    }

    private int intElement(Element parent, String tagName, String context) throws FileValidationException {
        Element child = requiredChild(parent, tagName, context);
        return parseInt(child.getTextContent(), context, tagName);
    }

    private int parseInt(String text, String context, String fieldName) throws FileValidationException {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new FileValidationException(context + ": \"" + fieldName + "\" must be a whole number, got \""
                    + text.trim() + "\".");
        }
    }
}
