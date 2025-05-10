package com.company.paymentoptimizer.algo;

import com.company.paymentoptimizer.model.Allocation;
import com.company.paymentoptimizer.model.Order;
import com.company.paymentoptimizer.model.PaymentMethod;
import com.company.paymentoptimizer.model.PaymentOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Kluczowe aspekty algorytmu: 3 główne metody płatności (tradycyjne, PUNKTY, PKT + tradycyjne)
 * BRAK rabatów --> przy częsciowych płatnosciach kartą.
 * Najważniejsze zasady:
 * Jak płatność dokonana w całości kartą --> rabat procentowy przypisany do banku
 * Jak klient zapłaci conajmniej 10% wartości zamówienia (przed rabatami) za pomoca PUNKTÓW -->
 *          --> to ma dodatkowe 10% rabatu na całe zamówienie.
 *          :: czyli w tej sytuacji jak użyjemy lojalnościowego to już rabatu z kart nie naliczamy
 * Jak całość opłacona przez PUNKTY --> przypisany rabat dla PUNKTY (jak w przypadku 100% karta)
 * CEL ZADANIA:
 * Optymalny sposób zapłaty (taki dzięki któremu klient uzyska największy rabat) oraz minimalizacja płatności
 * kartami, bo preferowane sa PUNKTY (ale oczywiście wysokośc rabatu najważniejsza).
 * W dostarczanych plikach JSON mamy:
 * orders.json (do 10000 zamówień): id, value (kwota zamówienia), promotions (list promocji aktywnych na
 *      na podstawie potencjalnych metod płatnosci do danego zamówienia --> nazwa promocji ==  nazwa metody płatnosci)
 *      :: brak promotions nie wylucza płatności za PUNKTY (ich ilośc opisana w paymentMethods),
 *          czy kartą (tylko wtedy bez promocji za ta kartę).
 * paymentMethods.json (do 1000 metod): id (nazwa metody płatności lub "PUNKTY"), discount (procentowy rabat),
 *      limit (maksymalna kwota dostępna w danej metodzie płatności)
 *
 * Implementdacja: algorytm zachłanny
 * 1. Pełne płatności kartami z promotions w celu maksymalizacji zniżek
 * 2. Nieopłacone zamówenia mamy 3 warianty:
 *      a. 100% PKT --> rabat za pkt
 *      b. PKT (>= 10% kwoty) + KARTA --> 10 %
 *      c. 100 kartą --> brak rabatu
 * 3. Wybieramy opcję (największy rabat i preferujemy PKT w remisie)
 * 4. Aktualizujemy wykorzystanie limitów oraz zapisujemy do Allocation
 * 5. Wypisanie wyników.
 *
 */

public class Optimizer {

    private final List<Order> orders;
    private final List<PaymentMethod> paymentMethods;
    private final PaymentMethod pointsMethod;
    private final List<PaymentMethod> cardMethods;
    //private final List<Allocation> allocations = new ArrayList<>();

    //teraz mamy ładnie wyizolowane metody które się nie zmienia tak samo jak orders dlatego final
    public Optimizer(List<Order> orders, List<PaymentMethod> paymentMethods) {
        this.orders = new ArrayList<>(orders);
        this.paymentMethods = new ArrayList<>(paymentMethods);
        this.pointsMethod = paymentMethods.stream()
                .filter(PaymentMethod::isPointsMethod)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No points method found"));

        this.cardMethods = paymentMethods.stream()
                .filter(pm -> !pm.isPointsMethod())
                .collect(Collectors.toList());
    }

    /**
     * Głowna metoda która realizuje optmalizację
     * @return allocations
     */
    public List<Allocation> optimize() {
        List<Allocation> allocations = allocateFullCardOptions();

        return allocations;
    }


    /**
     * Zachłanne alokowanie orders które można opłacic kartą z promotions
     * @return
     */
    private List<Allocation> allocateFullCardOptions(){
        List<Allocation> allocations = new ArrayList<>();
        Set<String> allocatedOrders = new HashSet<>();

        List<PaymentOption> potentialCardOptions = new ArrayList<>();

        for (Order order : orders) {
            for (PaymentMethod card : cardMethods) {
                //jak karta jest w promotions oraz możemy nią pokryć całe zamówienie
                if (order.getPromotions().contains(card.getId()) && card.canFullyCover(order.getValue())) {
                    BigDecimal discount = calculateDiscount(order.getValue(), card.getDiscount());
                    BigDecimal amountAfterDiscount = order.getValue().subtract(discount);

                    potentialCardOptions.add(new PaymentOption(
                            order,
                            card,
                            amountAfterDiscount,
                            Optional.empty(),
                            BigDecimal.ZERO,
                            discount
                    ));
                }
            }
        }

        //sortujemy malejąco po wielkości rabatu
        Collections.sort(potentialCardOptions);

        //przydzielamy zamówenia do kart
        for (PaymentOption paymentOption : potentialCardOptions) {
            Order order = paymentOption.getOrder();
            PaymentMethod card = paymentOption.getPrimaryMethod();

            //upewniamy się że nie zaalokowaliśmy już danego order
            if (!allocatedOrders.contains(order.getId()) && card.canFullyCover(order.getValue())) {
                Allocation allocation = paymentOption.toAllocation();
                allocations.add(allocation);
                allocatedOrders.add(order.getId());
                //jeszcze zabieramy z dostępnych środków na karcie to za ile opłaciliśmy
                card.addUsed(paymentOption.getPrimaryAmount());
            }

        }

        return allocations;
    }

    /**
     * Pomocnicza metoda do obliczania discount
     */
    private BigDecimal calculateDiscount(BigDecimal amount, int discountPercentage) {
        return amount.multiply(BigDecimal.valueOf(discountPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


















}
