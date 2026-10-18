package com.acme.pricing.service;

import com.acme.pricing.config.AppContext;
import com.acme.pricing.discount.BuyXGetYFreeDiscount;
import com.acme.pricing.discount.FixedAmountDiscount;
import com.acme.pricing.discount.PercentageDiscount;
import com.acme.pricing.model.AppliedDiscount;
import com.acme.pricing.model.Customer;
import com.acme.pricing.model.LineItem;
import com.acme.pricing.model.Quote;
import com.acme.pricing.model.QuoteRequest;
import com.acme.pricing.testing.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.acme.pricing.testing.Assert.assertEquals;
import static com.acme.pricing.testing.Assert.assertThrows;

public class PricingServiceImplTest {

    private final AppContext context = new AppContext();

    public PricingServiceImplTest() {
        context.customerRepository().save(new Customer("c-1", "Northwind Retail", "RETAIL"));
    }

    private Quote quote(LineItem... items) {
        return context.pricingService().quote(new QuoteRequest("c-1", List.of(items)));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Test
    public void quoteWithoutDiscountsIsSubtotal() {
        Quote quote = quote(new LineItem("SKU-A", 3, money("19.99")));

        assertEquals(money("59.97"), quote.subtotal());
        assertEquals(money("0.00"), quote.discountTotal());
        assertEquals(money("59.97"), quote.total());
        assertEquals(List.of(), quote.discounts());
    }

    @Test
    public void percentageDiscountAppliesAboveMinimum() {
        context.discountRepository().save(new PercentageDiscount("SPRING10", money("10"), money("50.00")));

        Quote quote = quote(new LineItem("SKU-A", 3, money("20.00")));

        assertEquals(List.of(new AppliedDiscount("SPRING10", money("6.00"))), quote.discounts());
        assertEquals(money("54.00"), quote.total());
    }

    @Test
    public void percentageDiscountSkippedBelowMinimum() {
        context.discountRepository().save(new PercentageDiscount("SPRING10", money("10"), money("50.00")));

        Quote quote = quote(new LineItem("SKU-A", 2, money("20.00")));

        assertEquals(List.of(), quote.discounts());
        assertEquals(money("40.00"), quote.total());
    }

    @Test
    public void discountAmountsRoundHalfEvenToCents() {
        context.discountRepository().save(new PercentageDiscount("SPRING10", money("10"), money("0.00")));

        Quote quote = quote(new LineItem("SKU-A", 1, money("12.25")));

        assertEquals(money("1.22"), quote.discountTotal());
        assertEquals(money("11.03"), quote.total());
    }

    @Test
    public void fixedAmountDiscountNeverExceedsSubtotal() {
        context.discountRepository().save(new FixedAmountDiscount("WELCOME25", money("25.00"), money("0.00")));

        Quote quote = quote(new LineItem("SKU-B", 1, money("15.00")));

        assertEquals(money("15.00"), quote.discountTotal());
        assertEquals(money("0.00"), quote.total());
    }

    @Test
    public void buyTwoGetOneFreeCountsCompleteGroupsOnly() {
        context.discountRepository().save(new BuyXGetYFreeDiscount("B2G1-SOCKS", "SKU-SOCKS", 2, 1));

        Quote quote = quote(new LineItem("SKU-SOCKS", 7, money("4.50")), new LineItem("SKU-A", 1, money("10.00")));

        assertEquals(List.of(new AppliedDiscount("B2G1-SOCKS", money("9.00"))), quote.discounts());
        assertEquals(money("32.50"), quote.total());
    }

    @Test
    public void discountsStackAgainstUndiscountedSubtotal() {
        context.discountRepository().save(new PercentageDiscount("SPRING10", money("10"), money("0.00")));
        context.discountRepository().save(new FixedAmountDiscount("WELCOME5", money("5.00"), money("0.00")));

        Quote quote = quote(new LineItem("SKU-A", 4, money("25.00")));

        assertEquals(List.of(
                new AppliedDiscount("SPRING10", money("10.00")),
                new AppliedDiscount("WELCOME5", money("5.00"))), quote.discounts());
        assertEquals(money("85.00"), quote.total());
    }

    @Test
    public void unknownCustomerIsRejected() {
        assertThrows(UnknownCustomerException.class,
                () -> context.pricingService().quote(new QuoteRequest("nobody", List.of())));
    }

    @Test
    public void nonPositiveQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> quote(new LineItem("SKU-A", 0, money("1.00"))));
    }
}
