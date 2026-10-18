package com.acme.pricing.service;

import com.acme.pricing.discount.BuyXGetYFreeDiscount;
import com.acme.pricing.discount.Discount;
import com.acme.pricing.discount.FixedAmountDiscount;
import com.acme.pricing.discount.PercentageDiscount;
import com.acme.pricing.model.AppliedDiscount;
import com.acme.pricing.model.Customer;
import com.acme.pricing.model.LineItem;
import com.acme.pricing.model.Quote;
import com.acme.pricing.model.QuoteRequest;
import com.acme.pricing.repository.CustomerRepository;
import com.acme.pricing.repository.DiscountRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PricingServiceImpl implements PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CustomerRepository customerRepository;
    private final DiscountRepository discountRepository;

    public PricingServiceImpl(CustomerRepository customerRepository, DiscountRepository discountRepository) {
        this.customerRepository = customerRepository;
        this.discountRepository = discountRepository;
    }

    @Override
    public Quote quote(QuoteRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new UnknownCustomerException(request.customerId()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem item : request.items()) {
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for SKU " + item.sku());
            }
            subtotal = subtotal.add(item.lineAmount());
        }
        subtotal = MoneyRounding.toCents(subtotal);

        List<AppliedDiscount> applied = new ArrayList<>();
        BigDecimal discountTotal = MoneyRounding.toCents(BigDecimal.ZERO);
        for (Discount discount : discountRepository.findAll()) {
            BigDecimal amount = MoneyRounding.toCents(amountFor(discount, request.items(), subtotal));
            if (amount.signum() > 0) {
                applied.add(new AppliedDiscount(discount.getCode(), amount));
                discountTotal = discountTotal.add(amount);
            }
        }

        BigDecimal total = subtotal.subtract(discountTotal);
        if (total.signum() < 0) {
            total = MoneyRounding.toCents(BigDecimal.ZERO);
        }
        return new Quote(customer.id(), subtotal, List.copyOf(applied), discountTotal, total);
    }

    private BigDecimal amountFor(Discount discount, List<LineItem> items, BigDecimal subtotal) {
        if (discount instanceof PercentageDiscount percentage) {
            if (subtotal.compareTo(percentage.getMinimumSubtotal()) < 0) {
                return BigDecimal.ZERO;
            }
            return subtotal.multiply(percentage.getPercent()).divide(HUNDRED);
        } else if (discount instanceof FixedAmountDiscount fixed) {
            if (subtotal.compareTo(fixed.getMinimumSubtotal()) < 0) {
                return BigDecimal.ZERO;
            }
            return fixed.getAmount().min(subtotal);
        } else if (discount instanceof BuyXGetYFreeDiscount bogo) {
            BigDecimal amount = BigDecimal.ZERO;
            for (LineItem item : items) {
                if (item.sku().equals(bogo.getSku())) {
                    int freeUnits = (item.quantity() / (bogo.getBuy() + bogo.getFree())) * bogo.getFree();
                    amount = amount.add(item.unitPrice().multiply(BigDecimal.valueOf(freeUnits)));
                }
            }
            return amount;
        }
        throw new IllegalStateException("Unsupported discount type: " + discount.getClass().getName());
    }
}
