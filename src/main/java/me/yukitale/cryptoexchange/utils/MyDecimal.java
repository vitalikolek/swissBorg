package me.yukitale.cryptoexchange.utils;

import lombok.Getter;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MyDecimal {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.##################");
    private static final DecimalFormat USD_FORMAT = new DecimalFormat("#0.##");

    @Getter
    private final double value;
    private final boolean usd;

    public MyDecimal(Double value) {
        this.value = value == null ? 0 : value;
        this.usd = false;
    }

    public MyDecimal(Double value, boolean usd) {
        this.value = value == null ? 0 : value;
        this.usd = usd;
    }

    public MyDecimal multiple(Double amount) {
        return new MyDecimal(this.value * (amount == null ? 0 : amount));
    }

    public MyDecimal multiple(Double amount, boolean usd) {
        return new MyDecimal(this.value * (amount == null ? 0 : amount), usd);
    }

    @Override
    public String toString() {
        return Double.isNaN(this.value) ? "0" : this.usd ? USD_FORMAT.format(this.value).replace(",", ".") : DECIMAL_FORMAT.format(this.value).replace(",", ".");
    }

    public String toString(int n) {
        DecimalFormat decimalFormat = new DecimalFormat("#0." + "#".repeat(Math.max(0, n)));
        return Double.isNaN(this.value) ? "0" : this.usd ? USD_FORMAT.format(this.value).replace(",", ".") : decimalFormat.format(this.value).replace(",", ".");
    }

    public String toStringFloor(int n) {
        DecimalFormat decimalFormat = new DecimalFormat("#0." + "#".repeat(Math.max(0, n)));
        decimalFormat.setRoundingMode(RoundingMode.FLOOR);
        return Double.isNaN(this.value) ? "0" : this.usd ? USD_FORMAT.format(this.value).replace(",", ".") : decimalFormat.format(this.value).replace(",", ".");
    }

    public String toStringWithComma() {
        return Double.isNaN(this.value) ? "0" : this.usd ? USD_FORMAT.format(this.value) : DECIMAL_FORMAT.format(this.value);
    }
}
