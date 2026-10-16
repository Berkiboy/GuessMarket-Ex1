package engine.io.impl;

import engine.exceptions.FileValidationException;
import engine.io.EventDefinition;
import engine.io.EventsFileLoader;
import engine.model.CommissionType;
import engine.model.trading.LmsrTradingMethod;
import engine.model.trading.OrderBookTradingMethod;
import engine.model.trading.TradingMethod;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loader for the Exercise 3 schema: Guess-Market / GM-events / GM-event (no id element and no GM-users).
 * The schema tables and the XSD diagrams spell some names differently, so both spellings are accepted:
 * commission / comision and initial / inital.
 */
public class XmlEventsFileLoader implements EventsFileLoader {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int MIN_OPTIONS = 2;

    @Override
    public List<EventDefinition> load(String fileName, InputStream content) throws FileValidationException {
        if (fileName == null || fileName.isBlank()) {
            throw new FileValidationException("No file was selected.");
        }
        if (!fileName.trim().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new FileValidationException("The file must have an .xml extension: \"" + fileName + "\".");
        }

        Document document = parse(content);
        List<String> problems = new ArrayList<>();
        List<EventDefinition> events = parseAllEvents(document, problems);

        if (!problems.isEmpty()) {
            throw new FileValidationException(String.join("\n", problems));
        }
        return events;
    }

    private List<EventDefinition> parseAllEvents(Document document, List<String> problems) {
        NodeList eventNodes = document.getElementsByTagName("GM-event");
        List<EventDefinition> events = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        if (eventNodes.getLength() == 0) {
            problems.add("The file does not contain any GM-event elements.");
        }
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            String label = "GM-event #" + (i + 1);
            try {
                EventDefinition event = parseEvent(eventElement, label);
                if (!seenNames.add(event.name().toLowerCase(Locale.ROOT))) {
                    problems.add(label + ": the event name \"" + event.name()
                            + "\" appears more than once in the file (event names must be unique).");
                    continue;
                }
                events.add(event);
            } catch (FileValidationException e) {
                problems.add(e.getMessage());
            }
        }
        return events;
    }

    private EventDefinition parseEvent(Element eventElement, String label) throws FileValidationException {
        String name = attribute(eventElement, "name", label);
        String context = label + " (\"" + name + "\")";

        String description = textElement(eventElement, "description", context);

        Element commissionElement = optionalChild(eventElement, "commission");
        if (commissionElement == null) {
            commissionElement = optionalChild(eventElement, "comision");
        }
        if (commissionElement == null) {
            throw new FileValidationException(context + ": missing required element \"commission\".");
        }
        int commissionPercent = parseInt(commissionElement.getTextContent(), context, "commission");
        if (commissionPercent < MIN_COMMISSION || commissionPercent > MAX_COMMISSION) {
            throw new FileValidationException(context + ": commission must be between " + MIN_COMMISSION + " and "
                    + MAX_COMMISSION + ", got " + commissionPercent + ".");
        }
        CommissionType commissionType;
        try {
            commissionType = CommissionType.fromXmlValue(commissionElement.getAttribute("type").trim());
        } catch (IllegalArgumentException e) {
            throw new FileValidationException(context + ": commission type must be \"on-purchase\" or \"on-close\","
                    + " got \"" + commissionElement.getAttribute("type") + "\".");
        }

        List<String> optionNames = parseOptions(eventElement, context);
        TradingMethod tradingMethod = parseTradingMethod(eventElement, context, optionNames.size());

        return new EventDefinition(name, description, commissionPercent, commissionType, optionNames, tradingMethod);
    }

    private List<String> parseOptions(Element eventElement, String context) throws FileValidationException {
        Element optionsElement = requiredChild(eventElement, "GM-options", context);
        NodeList optionNodes = optionsElement.getElementsByTagName("GM-option");

        List<String> optionNames = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < optionNodes.getLength(); i++) {
            String optionName = optionNodes.item(i).getTextContent().trim();
            if (optionName.isEmpty()) {
                throw new FileValidationException(context + ": GM-option #" + (i + 1) + " is empty.");
            }
            if (!seen.add(optionName.toLowerCase(Locale.ROOT))) {
                throw new FileValidationException(context + ": the option \"" + optionName
                        + "\" appears more than once (option names must be unique within an event).");
            }
            optionNames.add(optionName);
        }
        if (optionNames.size() < MIN_OPTIONS) {
            throw new FileValidationException(context + ": an event must have at least " + MIN_OPTIONS
                    + " options, found " + optionNames.size() + ".");
        }
        return optionNames;
    }

    private TradingMethod parseTradingMethod(Element eventElement, String context, int optionCount)
            throws FileValidationException {
        Element methodElement = requiredChild(eventElement, "GM-method", context);

        Element lmsrElement = optionalChild(methodElement, "GM-LMSR");
        if (lmsrElement != null) {
            int b = parseInt(textElement(lmsrElement, "b", context), context, "b");
            if (b <= 0) {
                throw new FileValidationException(context + ": b (liquidity) must be a positive whole number, got "
                        + b + ".");
            }
            return new LmsrTradingMethod(b, optionCount);
        }

        Element orderBookElement = optionalChild(methodElement, "GM-order-book");
        if (orderBookElement != null) {
            String initialValue = orderBookElement.hasAttribute("initial")
                    ? attribute(orderBookElement, "initial", context)
                    : attribute(orderBookElement, "inital", context);
            double initial = parseDouble(initialValue, context, "initial");
            double d = parseDouble(attribute(orderBookElement, "d", context), context, "d");
            String allowMintValue = attribute(orderBookElement, "allow-mint", context);
            if (!allowMintValue.equalsIgnoreCase("true") && !allowMintValue.equalsIgnoreCase("false")) {
                throw new FileValidationException(context + ": allow-mint must be \"true\" or \"false\", got \""
                        + allowMintValue + "\".");
            }
            if (initial < 0) {
                throw new FileValidationException(context + ": initial must not be negative, got "
                        + format(initial) + ".");
            }
            if (d <= 0) {
                throw new FileValidationException(context + ": d (base value) must be a positive number, got "
                        + format(d) + ".");
            }
            return new OrderBookTradingMethod(initial, d, Boolean.parseBoolean(allowMintValue.toLowerCase()),
                    optionCount);
        }

        throw new FileValidationException(context + ": GM-method must contain either GM-LMSR or GM-order-book.");
    }

    private Document parse(InputStream content) throws FileValidationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            Document document = builder.parse(content);
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
            if (child.getParentNode() == parent) {
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

    private int parseInt(String text, String context, String fieldName) throws FileValidationException {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new FileValidationException(context + ": \"" + fieldName + "\" must be a whole number, got \""
                    + text.trim() + "\".");
        }
    }

    private double parseDouble(String text, String context, String fieldName) throws FileValidationException {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new FileValidationException(context + ": \"" + fieldName + "\" must be a number, got \""
                    + text.trim() + "\".");
        }
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
