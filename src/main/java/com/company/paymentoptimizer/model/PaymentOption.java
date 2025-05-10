package com.company.paymentoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Tymczasowa propozycja dla danego danego ordera, sluży do porównywania dostępnych dla danego Ordera metod
 */
@AllArgsConstructor
@Getter
@Setter
public class PaymentOption implements Comparable<PaymentOption> {
    private Order order;
    private PaymentMethod primaryMethod;
    private BigDecimal primaryAmount;
    private Optional<PaymentMethod> secondaryMethod;
    private BigDecimal secondaryAmount;
    private BigDecimal totalDiscount;

    /**
     * Konwetujemy potecjalna metode płatnoci na finalną alokację
     */
    public Allocation toAllocation() {
        if (secondaryMethod.isPresent()) {
            return new Allocation(
                    order,
                    primaryMethod,
                    primaryAmount,
                    totalDiscount,
                    Optional.of(new Allocation.SecondaryPayment(secondaryMethod.get(), secondaryAmount))
            );
        } else {
            return new Allocation(
                    order,
                    primaryMethod,
                    primaryAmount,
                    totalDiscount,
                    Optional.empty()
            );
        }
    }

    @Override
    public int compareTo(PaymentOption other) {
        //zawsze najpierw preferujemy wiekszy discount
        int comparison = other.totalDiscount.compareTo(this.totalDiscount);
        if (comparison != 0) {
            return comparison;
        }

        //potem jak równe to wybieramy tą co jest dokonana za pomoca PUNKTÓW
        boolean thisUsesPoints = primaryMethod.isPointsMethod();
        boolean otherUsesPoints = other.primaryMethod.isPointsMethod();

        if (thisUsesPoints && !otherUsesPoints) {
            return -1; //this przed other
        } else if (!thisUsesPoints && otherUsesPoints) {
            return 1;
        }

        //ostatecznie wybieramy tą co wiecej PUNKTÓW użyto
        return other.primaryAmount.compareTo(this.primaryAmount);

    }

}
