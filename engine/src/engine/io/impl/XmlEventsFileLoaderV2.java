package engine.io.impl;

import engine.exceptions.FileValidationException;
import engine.io.EventsFileLoaderV2;
import engine.io.LoadedGameState;
import engine.model.CommissionType;
import engine.model.Event;
import engine.model.Option;
import engine.model.User;
import engine.model.trading.LmsrTradingMethod;
import engine.model.trading.OrderBookTradingMethod;
import engine.model.trading.TradingMethod;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XmlEventsFileLoaderV2 implements EventsFileLoaderV2 {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int OPTIONS_PER_EVENT = 2;

    @Override
    public LoadedGameState load(String path) throws FileValidationException {
        File file = new File(path);
        if (!file.isFile()) {
            throw new FileValidationException("File not found: \"" + path + "\".");
        }
        if (!path.toLowerCase().endsWith(".xml")) {
            throw new FileValidationException("File must have an .xml extension: \"" + path + "\".");
        }

        Document document = parse(file);
        List<String> problems = new ArrayList<>();

        List<RawEvent> rawEvents = parseAllEvents(document, problems);
        List<RawUser> rawUsers = parseAllUsers(document, problems);

        Map<Integer, String> eventIdToMmName = resolveMarketMakers(rawEvents, rawUsers, problems);

        if (!problems.isEmpty()) {
            throw new FileValidationException(String.join(" | ", problems));
        }

        Map<String, User> usersByName = new HashMap<>();
        List<User> users = new ArrayList<>();
        for (RawUser rawUser : rawUsers) {
            User user = new User(rawUser.name, rawUser.initialCash);
            usersByName.put(rawUser.name, user);
            users.add(user);
        }

        List<Event> events = new ArrayList<>();
        for (RawEvent rawEvent : rawEvents) {
            User marketMaker = usersByName.get(eventIdToMmName.get(rawEvent.id));
            events.add(new Event(rawEvent.id, rawEvent.name, rawEvent.description, rawEvent.commissionPercent,
                    rawEvent.commissionType, rawEvent.options, rawEvent.tradingMethod, marketMaker));
        }

        return new LoadedGameState(users, events);
    }

    private List<RawEvent> parseAllEvents(Document document, List<String> problems) {
        NodeList eventNodes = document.getElementsByTagName("GM-event");
        List<RawEvent> events = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            String label = "GM-event #" + (i + 1);
            try {
                RawEvent event = parseEvent(eventElement, label, seenIds);
                events.add(event);
            } catch (FileValidationException e) {
                problems.add(e.getMessage());
            }
        }
        if (events.isEmpty() && eventNodes.getLength() == 0) {
            problems.add("The file does not contain any GM-event elements.");
        }
        return events;
    }

    private RawEvent parseEvent(Element eventElement, String label, Set<Integer> seenIds)
            throws FileValidationException {
        String name = attribute(eventElement, "name", label);

        int id = intElement(eventElement, "id", label);
        if (!seenIds.add(id)) {
            throw new FileValidationException(label + " (\"" + name + "\"): duplicate event id " + id + ".");
        }

        String description = textElement(eventElement, "description", label);

        Element commissionElement = optionalChild(eventElement, "commission");
        if (commissionElement == null) {
            commissionElement = optionalChild(eventElement, "comision");
        }
        if (commissionElement == null) {
            throw new FileValidationException(label + ": missing required element \"commission\".");
        }
        int commissionPercent = parseInt(commissionElement.getTextContent(), label, "commission");
        if (commissionPercent < MIN_COMMISSION || commissionPercent > MAX_COMMISSION) {
            throw new FileValidationException(label + " (\"" + name + "\"): commission must be between "
                    + MIN_COMMISSION + " and " + MAX_COMMISSION + ", got " + commissionPercent + ".");
        }
        CommissionType commissionType;
        try {
            commissionType = CommissionType.fromXmlValue(commissionElement.getAttribute("type"));
        } catch (IllegalArgumentException e) {
            throw new FileValidationException(label + " (\"" + name
                    + "\"): commission type must be \"on-purchase\" or \"on-close\".");
        }

        List<String> optionNames = parseOptions(eventElement, label, name);
        List<Option> options = new ArrayList<>();
        for (String optionName : optionNames) {
            options.add(new Option(optionName));
        }

        TradingMethod tradingMethod = parseTradingMethod(eventElement, label, name, options.size());

        RawEvent event = new RawEvent();
        event.id = id;
        event.name = name;
        event.description = description;
        event.commissionPercent = commissionPercent;
        event.commissionType = commissionType;
        event.options = options;
        event.tradingMethod = tradingMethod;
        return event;
    }

    private List<String> parseOptions(Element eventElement, String label, String eventName)
            throws FileValidationException {
        Element optionsElement = requiredChild(eventElement, "GM-options", label);
        NodeList optionNodes = optionsElement.getElementsByTagName("GM-option");

        List<String> optionNames = new ArrayList<>();
        for (int i = 0; i < optionNodes.getLength(); i++) {
            String optionName = optionNodes.item(i).getTextContent().trim();
            if (optionName.isEmpty()) {
                throw new FileValidationException(label + " (\"" + eventName + "\"): GM-option #" + (i + 1)
                        + " is empty.");
            }
            optionNames.add(optionName);
        }
        if (optionNames.size() != OPTIONS_PER_EVENT) {
            throw new FileValidationException(label + " (\"" + eventName + "\"): must have exactly "
                    + OPTIONS_PER_EVENT + " options, found " + optionNames.size() + ".");
        }
        return optionNames;
    }

    private TradingMethod parseTradingMethod(Element eventElement, String label, String eventName, int optionCount)
            throws FileValidationException {
        Element methodElement = requiredChild(eventElement, "GM-method", label);

        Element lmsrElement = optionalChild(methodElement, "GM-LMSR");
        if (lmsrElement != null) {
            int b = intElement(lmsrElement, "b", label);
            if (b <= 0) {
                throw new FileValidationException(label + " (\"" + eventName
                        + "\"): b (liquidity) must be a positive integer, got " + b + ".");
            }
            return new LmsrTradingMethod(b, optionCount);
        }

        Element orderBookElement = optionalChild(methodElement, "GM-order-book");
        if (orderBookElement != null) {
            String initialValue = orderBookElement.hasAttribute("initial")
                    ? attribute(orderBookElement, "initial", label)
                    : attribute(orderBookElement, "inital", label);
            double initial = parseDouble(initialValue, label, "initial");
            double d = parseDouble(attribute(orderBookElement, "d", label), label, "d");
            String allowMintValue = attribute(orderBookElement, "allow-mint", label);
            boolean allowMint = Boolean.parseBoolean(allowMintValue);
            if (initial < 0) {
                throw new FileValidationException(label + " (\"" + eventName
                        + "\"): initial must not be negative, got " + initial + ".");
            }
            if (d <= 0) {
                throw new FileValidationException(label + " (\"" + eventName
                        + "\"): d (base value) must be a positive number, got " + d + ".");
            }
            return new OrderBookTradingMethod(initial, d, allowMint, optionCount);
        }

        throw new FileValidationException(label + " (\"" + eventName
                + "\"): GM-method must contain either GM-LMSR or GM-order-book.");
    }

    private List<RawUser> parseAllUsers(Document document, List<String> problems) {
        NodeList userNodes = document.getElementsByTagName("GM-user");
        List<RawUser> users = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (int i = 0; i < userNodes.getLength(); i++) {
            Element userElement = (Element) userNodes.item(i);
            String label = "GM-user #" + (i + 1);
            try {
                RawUser user = parseUser(userElement, label, seenNames);
                users.add(user);
            } catch (FileValidationException e) {
                problems.add(e.getMessage());
            }
        }
        if (users.isEmpty()) {
            problems.add("The file does not contain any GM-user elements.");
        }
        return users;
    }

    private RawUser parseUser(Element userElement, String label, Set<String> seenNames)
            throws FileValidationException {
        String name = attribute(userElement, "name", label);
        if (!seenNames.add(name)) {
            throw new FileValidationException(label + " (\"" + name + "\"): duplicate user name.");
        }

        double initialCash = parseDouble(textElement(userElement, "initial-cash", label), label, "initial-cash");
        if (initialCash <= 0) {
            throw new FileValidationException(label + " (\"" + name
                    + "\"): initial-cash must be greater than 0, got " + initialCash + ".");
        }

        List<Integer> mmEventIds = new ArrayList<>();
        Element mmElement = optionalChild(userElement, "GM-mareket-maker");
        if (mmElement == null) {
            mmElement = optionalChild(userElement, "GM-market-maker");
        }
        if (mmElement != null) {
            NodeList eventRefs = mmElement.getElementsByTagName("event");
            for (int i = 0; i < eventRefs.getLength(); i++) {
                Element eventRef = (Element) eventRefs.item(i);
                mmEventIds.add(parseInt(attribute(eventRef, "id", label), label, "event id"));
            }
        }

        RawUser user = new RawUser();
        user.name = name;
        user.initialCash = initialCash;
        user.mmEventIds = mmEventIds;
        return user;
    }

    private Map<Integer, String> resolveMarketMakers(List<RawEvent> events, List<RawUser> users,
                                                       List<String> problems) {
        Set<Integer> validEventIds = new HashSet<>();
        for (RawEvent event : events) {
            validEventIds.add(event.id);
        }

        Map<Integer, String> eventIdToMmName = new HashMap<>();
        for (RawUser user : users) {
            for (Integer eventId : user.mmEventIds) {
                if (!validEventIds.contains(eventId)) {
                    problems.add("User \"" + user.name + "\": market-maker reference to unknown event id " + eventId
                            + ".");
                    continue;
                }
                String existing = eventIdToMmName.put(eventId, user.name);
                if (existing != null) {
                    problems.add("Event id " + eventId + " has more than one market maker (\"" + existing
                            + "\" and \"" + user.name + "\").");
                }
            }
        }

        for (RawEvent event : events) {
            if (!eventIdToMmName.containsKey(event.id)) {
                problems.add(event.name + " (id " + event.id + "): no market maker assigned to this event.");
            }
        }
        return eventIdToMmName;
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

    private double parseDouble(String text, String context, String fieldName) throws FileValidationException {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new FileValidationException(context + ": \"" + fieldName + "\" must be a number, got \""
                    + text.trim() + "\".");
        }
    }

    private static class RawEvent {
        int id;
        String name;
        String description;
        int commissionPercent;
        CommissionType commissionType;
        List<Option> options;
        TradingMethod tradingMethod;
    }

    private static class RawUser {
        String name;
        double initialCash;
        List<Integer> mmEventIds;
    }
}
