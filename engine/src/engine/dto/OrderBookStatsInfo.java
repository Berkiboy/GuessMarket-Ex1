package engine.dto;

public class OrderBookStatsInfo {
    private final Double last;
    private final Double bid;
    private final Double ask;
    private final Double mid;
    private final Double spread;

    public OrderBookStatsInfo(Double last, Double bid, Double ask, Double mid, Double spread) {
        this.last = last;
        this.bid = bid;
        this.ask = ask;
        this.mid = mid;
        this.spread = spread;
    }

    public Double getLast() {
        return last;
    }

    public Double getBid() {
        return bid;
    }

    public Double getAsk() {
        return ask;
    }

    public Double getMid() {
        return mid;
    }

    public Double getSpread() {
        return spread;
    }
}
