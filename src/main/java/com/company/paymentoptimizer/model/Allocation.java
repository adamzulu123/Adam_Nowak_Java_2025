package com.company.paymentoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Finalny wynik optymalizacji dla danego zamówienia i na ich podstawie wygenerujemy <metoda> <wydane>
 */

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
            return paymentMethod.getId() + " (kwota: " + amount + ")";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Zamówienie: ").append(order.getId())
                .append(" (wartość: ").append(order.getValue()).append(")\n");
        sb.append("Rabat: ").append(discountValue).append("\n");
        sb.append("Płatność podstawowa: ").append(primaryMethod.getId())
                .append(" (kwota: ").append(amountFromPrimaryMethod).append(")");

        if (secondaryPayment.isPresent()) {
            sb.append("\nPłatność dodatkowa: ").append(secondaryPayment.get());
        }

        return sb.toString();
    }
}
