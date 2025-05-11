package com.company.paymentoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Finalny wynik optymalizacji dla danego zamówienia i na ich podstawie wygenerujemy <metoda> <wydane>
 */

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class Allocation {
    private Order order;
    private PaymentMethod primaryMethod;
    private BigDecimal amountFromPrimaryMethod;
    private BigDecimal discountValue;
    private Optional<SecondaryPayment> secondaryPayment;

    public record SecondaryPayment(PaymentMethod paymentMethod, BigDecimal amount) {
        @Override
        public String toString() {
            return paymentMethod.getId() + " (Amount: " + amount + ")";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Order: ").append(order.getId())
                .append(" (amount: ").append(order.getValue()).append(")\n");
        sb.append("Discount: ").append(discountValue).append("\n");
        sb.append("First payment method: ").append(primaryMethod.getId())
                .append(" (amount: ").append(amountFromPrimaryMethod).append(")");

        if (secondaryPayment.isPresent()) {
            sb.append("\nSecondary payment: ").append(secondaryPayment.get());
        }

        return sb.toString();
    }
}
