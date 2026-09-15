package engine.model.trading;

public class LmsrTradingMethod implements TradingMethod {

    private final int b;
    private final double[] sharesBought;

    public LmsrTradingMethod(int b, int optionCount) {
        this.b = b;
        this.sharesBought = new double[optionCount];
    }

    @Override
    public double getPrice(int optionIndex) {
        double numerator = Math.exp(sharesBought[optionIndex] / b);
        double denominator = sumExp(sharesBought);
        return numerator / denominator;
    }

    public double costOfPurchase(int optionIndex, int quantity) {
        double before = costFunction(sharesBought);
        double[] after = sharesBought.clone();
        after[optionIndex] += quantity;
        double afterCost = costFunction(after);
        return afterCost - before;
    }

    public void commitPurchase(int optionIndex, int quantity) {
        sharesBought[optionIndex] += quantity;
    }

    @Override
    public double openingCost() {
        return costFunction(new double[sharesBought.length]);
    }

    private double costFunction(double[] shares) {
        return b * Math.log(sumExp(shares));
    }

    private double sumExp(double[] shares) {
        double sum = 0;
        for (double q : shares) {
            sum += Math.exp(q / b);
        }
        return sum;
    }
}
