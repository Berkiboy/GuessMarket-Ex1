package engine.model.trading;

public class LmsrTradingMethod implements TradingMethod {

    private final int b;
    private final double[] sharesBought;

    public LmsrTradingMethod(int b, int optionCount) {
        this.b = b;
        this.sharesBought = new double[optionCount];
    }

    public int getB() {
        return b;
    }

    @Override
    public double getPrice(int optionIndex) {
        double max = maxScaled(sharesBought);
        double numerator = Math.exp(sharesBought[optionIndex] / b - max);
        return numerator / sumExpShifted(sharesBought, max);
    }

    public double costOfPurchase(int optionIndex, int quantity) {
        double before = costFunction(sharesBought);
        double[] after = sharesBought.clone();
        after[optionIndex] += quantity;
        return costFunction(after) - before;
    }

    public void commitPurchase(int optionIndex, int quantity) {
        sharesBought[optionIndex] += quantity;
    }

    @Override
    public double openingCost() {
        return costFunction(new double[sharesBought.length]);
    }

    @Override
    public double payoutPerShare() {
        return 1.0;
    }

    // C(q) = b * ln(sum(e^(q_i/b))), computed with the log-sum-exp shift so large quantities don't overflow.
    private double costFunction(double[] shares) {
        double max = maxScaled(shares);
        return b * (max + Math.log(sumExpShifted(shares, max)));
    }

    private double maxScaled(double[] shares) {
        double max = Double.NEGATIVE_INFINITY;
        for (double q : shares) {
            max = Math.max(max, q / b);
        }
        return max;
    }

    private double sumExpShifted(double[] shares, double shift) {
        double sum = 0;
        for (double q : shares) {
            sum += Math.exp(q / b - shift);
        }
        return sum;
    }
}
