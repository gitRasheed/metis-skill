package hiddencheck;

import com.acme.pricing.config.AppContext;
import com.acme.pricing.discount.BuyXGetYFreeDiscount;
import com.acme.pricing.discount.FixedAmountDiscount;
import com.acme.pricing.discount.PercentageDiscount;
import com.acme.pricing.discount.VolumeDiscount;
import com.acme.pricing.discount.VolumeTier;
import com.acme.pricing.model.AppliedDiscount;
import com.acme.pricing.model.Customer;
import com.acme.pricing.model.LineItem;
import com.acme.pricing.model.Quote;
import com.acme.pricing.model.QuoteRequest;
import com.acme.pricing.model.SegmentDiscountRule;
import com.acme.pricing.repository.SegmentRuleRepository;
import com.acme.pricing.service.PricingService;
import com.acme.pricing.service.UnknownCustomerException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static hiddencheck.HiddenRunner.eq;

public class PricingHiddenChecks {

    private final AppContext ctx = new AppContext();

    public PricingHiddenChecks() {
        ctx.customerRepository().save(new Customer("retail-1", "Corner Shop", "RETAIL"));
        ctx.customerRepository().save(new Customer("whole-1", "Bulk Foods Ltd", "WHOLESALE"));
        ctx.customerRepository().save(new Customer("edu-1", "City College", "EDUCATION"));
    }

    private static BigDecimal m(String value) {
        return new BigDecimal(value);
    }

    private Quote quote(String customerId, LineItem... items) {
        PricingService service = ctx.pricingService();
        return service.quote(new QuoteRequest(customerId, List.of(items)));
    }

    private static BigDecimal amountFor(Quote quote, String code) {
        BigDecimal found = null;
        for (AppliedDiscount discount : quote.discounts()) {
            if (discount.code().equals(code)) {
                if (found != null) {
                    throw new AssertionError("code " + code + " applied more than once: " + quote.discounts());
                }
                found = discount.amount();
            }
        }
        return found;
    }

    private void standardVolume() {
        ctx.discountRepository().save(new VolumeDiscount("VOLUME", List.of(
                new VolumeTier(10, m("5")),
                new VolumeTier(50, m("10")))));
    }

    @Check
    public void volumeBelowFirstTierDoesNotApply() {
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 9, m("10.00")));
        eq(null, amountFor(q, "VOLUME"), "VOLUME at 9 units");
        eq(List.of(), q.discounts(), "discounts at 9 units");
        eq(m("90.00"), q.total(), "total at 9 units");
    }

    @Check
    public void volumeExactlyAtTenGetsFivePercent() {
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 10, m("10.00")));
        eq(m("5.00"), amountFor(q, "VOLUME"), "VOLUME at 10 units");
        eq(m("95.00"), q.total(), "total at 10 units");
    }

    @Check
    public void volumeJustBelowFiftyStaysOnFirstTier() {
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 49, m("2.00")));
        eq(m("4.90"), amountFor(q, "VOLUME"), "VOLUME at 49 units");
    }

    @Check
    public void volumeExactlyAtFiftyGetsTenPercentNotCumulative() {
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 50, m("2.00")));
        eq(m("10.00"), amountFor(q, "VOLUME"), "VOLUME at 50 units");
        eq(m("90.00"), q.total(), "total at 50 units");
    }

    @Check
    public void volumeTiersInAnyOrder() {
        ctx.discountRepository().save(new VolumeDiscount("VOLUME", List.of(
                new VolumeTier(50, m("10")),
                new VolumeTier(10, m("5")))));
        Quote high = quote("retail-1", new LineItem("SKU-A", 60, m("1.00")));
        Quote low = quote("retail-1", new LineItem("SKU-A", 20, m("1.00")));
        eq(m("6.00"), amountFor(high, "VOLUME"), "VOLUME at 60 units, unsorted tiers");
        eq(m("1.00"), amountFor(low, "VOLUME"), "VOLUME at 20 units, unsorted tiers");
    }

    @Check
    public void volumeCountsPerLineAndSumsIntoOneEntry() {
        standardVolume();
        Quote q = quote("retail-1",
                new LineItem("SKU-A", 6, m("10.00")),
                new LineItem("SKU-B", 6, m("10.00")),
                new LineItem("SKU-C", 10, m("4.00")),
                new LineItem("SKU-D", 50, m("1.00")));
        eq(m("7.00"), amountFor(q, "VOLUME"), "VOLUME per line (2.00 + 5.00)");
        eq(1, q.discounts().size(), "one applied entry");
        eq(m("210.00"), q.subtotal(), "subtotal");
        eq(m("203.00"), q.total(), "total");
    }

    @Check
    public void volumeRoundsHalfEvenToCents() {
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 10, m("2.45")));
        eq(m("1.22"), amountFor(q, "VOLUME"), "5% of 24.50 = 1.225");
        eq(m("23.28"), q.total(), "total");
    }

    @Check
    public void volumeStacksWithExistingDiscountsOnUndiscountedAmounts() {
        ctx.discountRepository().save(new PercentageDiscount("SPRING10", m("10"), m("0.00")));
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 50, m("4.00")));
        eq(m("20.00"), amountFor(q, "SPRING10"), "SPRING10");
        eq(m("20.00"), amountFor(q, "VOLUME"), "VOLUME");
        eq(m("40.00"), q.discountTotal(), "discount total");
        eq(m("160.00"), q.total(), "total");
    }

    @Check
    public void restrictedDiscountOnlyForListedSegment() {
        SegmentRuleRepository rules = ctx.segmentRuleRepository();
        ctx.discountRepository().save(new PercentageDiscount("TRADE15", m("15"), m("0.00")));
        rules.save(new SegmentDiscountRule("WHOLESALE", Set.of("TRADE15")));

        eq(m("15.00"), amountFor(quote("whole-1", new LineItem("SKU-A", 1, m("100.00"))), "TRADE15"), "wholesale");
        eq(null, amountFor(quote("retail-1", new LineItem("SKU-A", 1, m("100.00"))), "TRADE15"), "retail");
    }

    @Check
    public void unrestrictedDiscountStillAppliesToEveryone() {
        ctx.discountRepository().save(new PercentageDiscount("TRADE15", m("15"), m("0.00")));
        ctx.discountRepository().save(new FixedAmountDiscount("WELCOME5", m("5.00"), m("0.00")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("TRADE15")));

        Quote wholesale = quote("whole-1", new LineItem("SKU-A", 1, m("100.00")));
        Quote retail = quote("retail-1", new LineItem("SKU-A", 1, m("100.00")));
        eq(m("5.00"), amountFor(wholesale, "WELCOME5"), "WELCOME5 wholesale");
        eq(m("5.00"), amountFor(retail, "WELCOME5"), "WELCOME5 retail");
        eq(m("80.00"), wholesale.total(), "wholesale total");
        eq(m("95.00"), retail.total(), "retail total");
    }

    @Check
    public void segmentWithNoRuleGetsOnlyUnrestrictedDiscounts() {
        ctx.discountRepository().save(new PercentageDiscount("TRADE15", m("15"), m("0.00")));
        ctx.discountRepository().save(new PercentageDiscount("SPRING10", m("10"), m("0.00")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("TRADE15")));

        Quote edu = quote("edu-1", new LineItem("SKU-A", 1, m("100.00")));
        eq(List.of(new AppliedDiscount("SPRING10", m("10.00"))), edu.discounts(), "EDUCATION discounts");
    }

    @Check
    public void segmentWithEmptyRuleGetsOnlyUnrestrictedDiscounts() {
        ctx.discountRepository().save(new PercentageDiscount("TRADE15", m("15"), m("0.00")));
        ctx.discountRepository().save(new PercentageDiscount("SPRING10", m("10"), m("0.00")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("TRADE15")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("EDUCATION", Set.of()));

        Quote edu = quote("edu-1", new LineItem("SKU-A", 1, m("100.00")));
        eq(List.of(new AppliedDiscount("SPRING10", m("10.00"))), edu.discounts(), "EDUCATION discounts");
    }

    @Check
    public void savingRuleAgainReplacesIt() {
        ctx.discountRepository().save(new PercentageDiscount("TRADE15", m("15"), m("0.00")));
        ctx.discountRepository().save(new FixedAmountDiscount("WELCOME5", m("5.00"), m("0.00")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("TRADE15", "WELCOME5")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("EDUCATION", Set.of("TRADE15")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("WELCOME5")));

        Quote wholesale = quote("whole-1", new LineItem("SKU-A", 1, m("100.00")));
        eq(null, amountFor(wholesale, "TRADE15"), "TRADE15 after replacement");
        eq(m("5.00"), amountFor(wholesale, "WELCOME5"), "WELCOME5 after replacement");
    }

    @Check
    public void tieredAndSegmentCombined() {
        standardVolume();
        ctx.discountRepository().save(new PercentageDiscount("SPRING10", m("10"), m("0.00")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("VOLUME")));

        LineItem bulk = new LineItem("SKU-A", 50, m("3.00"));
        Quote wholesale = quote("whole-1", bulk);
        Quote retail = quote("retail-1", bulk);
        Quote edu = quote("edu-1", bulk);

        eq(m("15.00"), amountFor(wholesale, "VOLUME"), "wholesale VOLUME at 50");
        eq(m("15.00"), amountFor(wholesale, "SPRING10"), "wholesale SPRING10");
        eq(m("120.00"), wholesale.total(), "wholesale total");
        eq(null, amountFor(retail, "VOLUME"), "retail VOLUME");
        eq(m("135.00"), retail.total(), "retail total");
        eq(null, amountFor(edu, "VOLUME"), "education VOLUME");

        Quote wholesaleTen = quote("whole-1", new LineItem("SKU-A", 10, m("3.00")));
        eq(m("1.50"), amountFor(wholesaleTen, "VOLUME"), "wholesale VOLUME at 10");
    }

    @Check
    public void noRulesMeansExistingBehaviourForAllSegments() {
        standardVolume();
        ctx.discountRepository().save(new BuyXGetYFreeDiscount("B2G1", "SKU-S", 2, 1));
        for (String customer : List.of("retail-1", "whole-1", "edu-1")) {
            Quote q = quote(customer, new LineItem("SKU-S", 12, m("1.00")));
            eq(m("4.00"), amountFor(q, "B2G1"), customer + " B2G1");
            eq(m("0.60"), amountFor(q, "VOLUME"), customer + " VOLUME");
            eq(m("7.40"), q.total(), customer + " total");
        }
    }

    @Check
    public void existingDiscountsAndRoundingUnchanged() {
        ctx.discountRepository().save(new PercentageDiscount("SPRING10", m("10"), m("50.00")));
        ctx.discountRepository().save(new FixedAmountDiscount("WELCOME5", m("5.00"), m("0.00")));
        ctx.discountRepository().save(new BuyXGetYFreeDiscount("B2G1", "SKU-S", 2, 1));

        Quote small = quote("retail-1", new LineItem("SKU-A", 1, m("12.25")));
        eq(List.of(new AppliedDiscount("WELCOME5", m("5.00"))), small.discounts(), "below minimum");
        eq(m("7.25"), small.total(), "small total");

        Quote big = quote("retail-1", new LineItem("SKU-A", 5, m("12.25")), new LineItem("SKU-S", 3, m("2.00")));
        eq(m("67.25"), big.subtotal(), "subtotal");
        eq(List.of(
                new AppliedDiscount("SPRING10", m("6.72")),
                new AppliedDiscount("WELCOME5", m("5.00")),
                new AppliedDiscount("B2G1", m("2.00"))), big.discounts(), "applied discounts, in configured order");
        eq(m("13.72"), big.discountTotal(), "discount total");
        eq(m("53.53"), big.total(), "total");
    }

    @Check
    public void halfEvenNotHalfUp() {
        ctx.discountRepository().save(new PercentageDiscount("P10", m("10"), m("0.00")));
        eq(m("1.22"), quote("retail-1", new LineItem("SKU-A", 1, m("12.25"))).discountTotal(), "1.225 -> 1.22");
        eq(m("1.24"), quote("retail-1", new LineItem("SKU-A", 1, m("12.35"))).discountTotal(), "1.235 -> 1.24");
    }

    @Check
    public void totalsNeverNegativeAndZeroIsScaledToCents() {
        ctx.discountRepository().save(new FixedAmountDiscount("BIG", m("50.00"), m("0.00")));
        standardVolume();
        Quote q = quote("retail-1", new LineItem("SKU-A", 10, m("1.00")));
        eq(m("0.00"), q.total(), "total floor");
        Quote none = quote("retail-1");
        eq(m("0.00"), none.subtotal(), "empty subtotal");
        eq(m("0.00"), none.discountTotal(), "empty discount total");
    }

    @Check
    public void volumeUsesConfiguredTierNotDefaults() {
        ctx.discountRepository().save(new VolumeDiscount("BULK7", List.of(new VolumeTier(3, m("7")))));
        eq(null, amountFor(quote("retail-1", new LineItem("SKU-A", 2, m("10.00"))), "BULK7"), "BULK7 at 2 units");
        eq(m("2.10"), amountFor(quote("retail-1", new LineItem("SKU-A", 3, m("10.00"))), "BULK7"), "BULK7 at 3 units");
        eq(m("70.00"), amountFor(quote("retail-1", new LineItem("SKU-A", 100, m("10.00"))), "BULK7"), "BULK7 at 100 units");
    }

    @Check
    public void threeTierScheduleWithFractionalPercent() {
        ctx.discountRepository().save(new VolumeDiscount("PALLET", List.of(
                new VolumeTier(100, m("12.5")),
                new VolumeTier(5, m("2")),
                new VolumeTier(20, m("4")))));
        eq(null, amountFor(quote("retail-1", new LineItem("SKU-A", 4, m("10.00"))), "PALLET"), "4 units");
        eq(m("1.00"), amountFor(quote("retail-1", new LineItem("SKU-A", 5, m("10.00"))), "PALLET"), "5 units");
        eq(m("3.80"), amountFor(quote("retail-1", new LineItem("SKU-A", 19, m("10.00"))), "PALLET"), "19 units");
        eq(m("8.00"), amountFor(quote("retail-1", new LineItem("SKU-A", 20, m("10.00"))), "PALLET"), "20 units");
        eq(m("7.92"), amountFor(quote("retail-1", new LineItem("SKU-A", 99, m("2.00")),
                new LineItem("SKU-B", 2, m("0.50"))), "PALLET"), "99 units plus a small line");
        eq(m("25.00"), amountFor(quote("retail-1", new LineItem("SKU-A", 100, m("2.00"))), "PALLET"), "100 units");
        eq(m("37.50"), amountFor(quote("retail-1", new LineItem("SKU-A", 150, m("2.00"))), "PALLET"), "150 units");
    }

    @Check
    public void twoVolumeDiscountsEachApplyUnderTheirOwnCode() {
        standardVolume();
        ctx.discountRepository().save(new VolumeDiscount("CLEARANCE", List.of(new VolumeTier(3, m("1")))));
        Quote q = quote("retail-1", new LineItem("SKU-A", 10, m("10.00")));
        eq(m("5.00"), amountFor(q, "VOLUME"), "VOLUME");
        eq(m("1.00"), amountFor(q, "CLEARANCE"), "CLEARANCE");
        eq(2, q.discounts().size(), "two applied entries");
        eq(m("94.00"), q.total(), "total");
    }

    @Check
    public void restrictionsApplyToEveryDiscountType() {
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("P5", "F3", "BOGO", "VOL")));
        ctx.discountRepository().save(new PercentageDiscount("P5", m("5"), m("0.00")));
        ctx.discountRepository().save(new FixedAmountDiscount("F3", m("3.00"), m("0.00")));
        ctx.discountRepository().save(new BuyXGetYFreeDiscount("BOGO", "SKU-S", 1, 1));
        ctx.discountRepository().save(new VolumeDiscount("VOL", List.of(new VolumeTier(2, m("10")))));

        LineItem socks = new LineItem("SKU-S", 2, m("5.00"));
        Quote wholesale = quote("whole-1", socks);
        eq(List.of(
                new AppliedDiscount("P5", m("0.50")),
                new AppliedDiscount("F3", m("3.00")),
                new AppliedDiscount("BOGO", m("5.00")),
                new AppliedDiscount("VOL", m("1.00"))), wholesale.discounts(), "wholesale discounts");
        eq(m("0.50"), wholesale.total(), "wholesale total");

        Quote retail = quote("retail-1", socks);
        eq(List.of(), retail.discounts(), "retail discounts");
        eq(m("10.00"), retail.total(), "retail total");
    }

    @Check
    public void restrictedFixedAndBuyXGetYExcludedForOtherSegments() {
        ctx.discountRepository().save(new PercentageDiscount("P5", m("5"), m("0.00")));
        ctx.discountRepository().save(new FixedAmountDiscount("F3", m("3.00"), m("0.00")));
        ctx.discountRepository().save(new BuyXGetYFreeDiscount("BOGO", "SKU-S", 1, 1));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("F3", "BOGO")));

        Quote retail = quote("retail-1", new LineItem("SKU-S", 2, m("5.00")));
        eq(List.of(new AppliedDiscount("P5", m("0.50"))), retail.discounts(), "retail discounts");
        eq(m("9.50"), retail.total(), "retail total");
        Quote edu = quote("edu-1", new LineItem("SKU-S", 2, m("5.00")));
        eq(null, amountFor(edu, "F3"), "education F3");
        eq(null, amountFor(edu, "BOGO"), "education BOGO");
    }

    @Check
    public void replacingTheOnlyRuleThatMentionsACodeUnrestrictsIt() {
        ctx.discountRepository().save(new FixedAmountDiscount("F3", m("3.00"), m("0.00")));
        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of("F3")));
        LineItem item = new LineItem("SKU-A", 1, m("10.00"));

        eq(null, amountFor(quote("retail-1", item), "F3"), "retail while restricted");
        eq(m("3.00"), amountFor(quote("whole-1", item), "F3"), "wholesale while restricted");

        ctx.segmentRuleRepository().save(new SegmentDiscountRule("WHOLESALE", Set.of()));
        eq(m("3.00"), amountFor(quote("retail-1", item), "F3"), "retail after rule no longer mentions F3");
        eq(m("3.00"), amountFor(quote("whole-1", item), "F3"), "wholesale after rule no longer mentions F3");

        ctx.segmentRuleRepository().save(new SegmentDiscountRule("RETAIL", Set.of("F3")));
        eq(null, amountFor(quote("whole-1", item), "F3"), "wholesale after F3 moved to RETAIL");
        eq(m("3.00"), amountFor(quote("retail-1", item), "F3"), "retail after F3 moved to RETAIL");
    }

    @Check
    public void unknownCustomerStillRejected() {
        try {
            quote("ghost", new LineItem("SKU-A", 1, m("1.00")));
        } catch (UnknownCustomerException expected) {
            return;
        }
        throw new AssertionError("expected UnknownCustomerException");
    }
}
