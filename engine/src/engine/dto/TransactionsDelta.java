package engine.dto;

import java.util.List;

/** Delta fetching of a user's account lines: the lines after the client's last known one, plus the new last. */
public class TransactionsDelta {
    private final int version;
    private final List<AccountTransactionInfo> entries;

    public TransactionsDelta(int version, List<AccountTransactionInfo> entries) {
        this.version = version;
        this.entries = entries;
    }

    /** Serial number of the newest line; the client sends it back as "from" on its next request. */
    public int getVersion() {
        return version;
    }

    public List<AccountTransactionInfo> getEntries() {
        return entries;
    }
}
