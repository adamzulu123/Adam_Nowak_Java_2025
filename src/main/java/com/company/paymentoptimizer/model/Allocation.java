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

    }
}
