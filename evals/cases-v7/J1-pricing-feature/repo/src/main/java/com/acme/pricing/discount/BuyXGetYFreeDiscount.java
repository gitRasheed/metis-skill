package com.acme.pricing.discount;

/** For every (buy + free) units of the SKU on a line, the last {@code free} units cost nothing. */
public class BuyXGetYFreeDiscount extends Discount {

    private final String sku;
    private final int buy;
    private final int free;

    public BuyXGetYFreeDiscount(String code, String sku, int buy, int free) {
        super(code);
        this.sku = sku;
        this.buy = buy;
        this.free = free;
    }

    public String getSku() {
        return sku;
    }

    public int getBuy() {
        return buy;
    }

    public int getFree() {
        return free;
    }
}
