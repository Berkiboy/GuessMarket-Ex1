package engine.dto;

import java.util.List;

public class EventSummary {
    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final String commissionType;
    private final List<String> optionNames;
    private final boolean closed;

    public EventSummary(int id, String name, String description, int commissionPercent,
                         String commissionType, List<String> optionNames, boolean closed) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = optionNames;
        this.closed = closed;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommissionPercent() {
        return commissionPercent;
    }

    public String getCommissionType() {
        return commissionType;
    }

    public List<String> getOptionNames() {
        return optionNames;
    }

    public boolean isClosed() {
        return closed;
    }
}
