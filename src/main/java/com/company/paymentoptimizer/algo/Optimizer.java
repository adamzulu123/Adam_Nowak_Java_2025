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
    private List<Allocation> allocations = new ArrayList<>();

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
        allocateFullCardOptions();
        allocateRemainingOrders();
        return allocations;
    }

    /**
     * Generowanie raportu
     */
    public String generateReport(List<Allocation> allocations) {
        StringBuilder report = new StringBuilder();
        Map<String, BigDecimal> paymentUsage = new HashMap<>();

        for (Allocation allocation : allocations) {
            report.append(allocation).append("\n\n");

            paymentUsage.merge(
                    allocation.getPrimaryMethod().getId(),
                    allocation.getAmountFromPrimaryMethod(),
                    BigDecimal::add
            );

            allocation.getSecondaryPayment().ifPresent(secondary ->
                    paymentUsage.merge(
                            secondary.paymentMethod().getId(),
                            secondary.amount(),
                            BigDecimal::add
                    )
            );
        }

        report.append("=== Result ===\n");
        paymentUsage.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    BigDecimal total = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                    report.append(entry.getKey()).append(": ").append(total).append("\n");
                });

        return report.toString();

    }


    /**
     * Zachłanne alokowanie orders które można opłacic kartą z promotions
     * @return
     */
    private void allocateFullCardOptions(){
        //List<Allocation> allocations = new ArrayList<>();
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

        //return allocations;
    }

    /**
     * Pomocnicza metoda do obliczania discount
     */
    private BigDecimal calculateDiscount(BigDecimal amount, int discountPercentage) {
        return amount.multiply(BigDecimal.valueOf(discountPercentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


    /**
     * Alokowanie pozostałych nie opłaconych zamówień.
     * Idea jest taka, że najpierw próbujemy opłacić zamówienie w całości punktami,
     * potem punkty + karta, a na koniec ratujemy się w całości kartą bez rabatów
     */
    public void allocateRemainingOrders(){
        Set<String> allocatedOrderIds = allocations.stream()
                .map(a -> a.getOrder().getId())
                .collect(Collectors.toSet());

        //List<Order> unallocatedOrders = new ArrayList<>(); //jak zachłanny zawiedzie --> nie dokończone ze wzgledu na czas

        orders.sort(Comparator.comparing(Order::getValue).reversed());

        for (Order order : orders) {
            if (!allocatedOrderIds.contains(order.getId())) {
                List<PaymentOption> potentialOrderOptions = new ArrayList<>();

                //1. PUNKTY 100%
                if (pointsMethod.canFullyCover(order.getValue())) {
                    BigDecimal discount = calculateDiscount(order.getValue(), pointsMethod.getDiscount());
                    BigDecimal amountAfterDiscount = order.getValue().subtract(discount);

                    potentialOrderOptions.add(new PaymentOption(
                            order,
                            pointsMethod,
                            amountAfterDiscount,
                            Optional.empty(),
                            BigDecimal.ZERO,
                            discount
                    ));
                }

                //2. Punkty (+10% wartości zamówienia) + Karta ==> 10% rabatu
                if (pointsMethod.getAvailable().compareTo(BigDecimal.ZERO) > 0) {
                    //musimy sprawdzić czy jesteśmy w stanie pokryć 10% zamówienia
                    BigDecimal minAmountForPoints = calculateDiscount(order.getValue(), 10);
                    BigDecimal pointsToUse = pointsMethod.getAvailable();

                    if (pointsToUse.compareTo(order.getValue()) > 0) {
                        pointsToUse = order.getValue();
                    }

                    if (pointsMethod.getAvailable().compareTo(minAmountForPoints) >= 0) {
                        for (PaymentMethod card : cardMethods) {
                            BigDecimal totalDiscount = calculateDiscount(order.getValue(), 10);
                            BigDecimal discountedTotal = order.getValue().subtract(totalDiscount);
                            BigDecimal remainingValue = discountedTotal.subtract(pointsToUse);

                            //jeśli kartą jesteśmy w stanie pokryć pozostałą część zamówienia
                            if (remainingValue.compareTo(BigDecimal.ZERO) > 0 && card.canFullyCover(remainingValue)) {
                                potentialOrderOptions.add(new PaymentOption(
                                        order,
                                        pointsMethod,
                                        pointsToUse,
                                        Optional.of(card),
                                        remainingValue,
                                        totalDiscount
                                ));
                            }
                        }
                    }
                }

                //3. Sama karta (ratunek jak juz pkt ani zniżek nie mamy)
                for (PaymentMethod card : cardMethods) {
                    if (card.canFullyCover(order.getValue())) {
                        potentialOrderOptions.add(new PaymentOption(
                                order,
                                card,
                                order.getValue(),
                                Optional.empty(),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        ));
                    }
                }

                //wybieramy najlepszą opcje dla zamówienia
                if (!potentialOrderOptions.isEmpty()) {
                    Collections.sort(potentialOrderOptions);
                    PaymentOption bestOption = potentialOrderOptions.getFirst();

                    //aktualizujemy limity
                    bestOption.getPrimaryMethod().addUsed(bestOption.getPrimaryAmount());
                    if (bestOption.getSecondaryMethod().isPresent()) {
                        bestOption.getSecondaryMethod().get().addUsed(bestOption.getSecondaryAmount());
                    }

                    //dodajemy do allocations
                    allocations.add(bestOption.toAllocation());
                    allocatedOrderIds.add(order.getId());
                }else{
                    System.out.println(order.getId());
                    //unallocatedOrders.add(order); //todo::
                }

            }
        }
        //todo: unallocatedOrders --> relokacja i poszukiwania innych możliwości lokalnie --> dużo wiecej kodu i czasu potrzebne

    }

    //todo:: do zrobienia ale czasu zabrakło działa za to algorytm zachłanny który powienien w znacznej wiekszosci
    //todo:: sytuacji znaleźć rozwiazanie -- to co niżej to własnie nie dokończone szykanie i relokowanie zapłaty
    //albo w ogóle można by wielowątkowo policzyc te 10 000 000 opcji i potem z nich wybierać
    //ale chyba ten co narazie greedy powienien rozwiązac wiekszośc problemów

    private boolean attemptReallocateFor(Order failedOrder) {
        List<PaymentOption> possibleOptions = generatePotentialPaymentMethods(failedOrder);

        if (possibleOptions.isEmpty()) return false;

        //sortujemy od najlepszej tej z największym rabatem
        Collections.sort(possibleOptions);

        //dla każde opcja sprawdzamy czy w ogole jestesmy w stanie to opłacic
        for (PaymentOption Option : possibleOptions) {

        }
        return true; //todo::

    }


    /**
     * Generujemy wszysktie potencjalne możliwości opłacenia nieopłalnego zamówienia na podstawie limitów zamówień
     * a nie tego co już zachłannie zrobiliśmy.
     */
    private List<PaymentOption> generatePotentialPaymentMethods(Order order){
        List<PaymentOption> options = new ArrayList<>();

        // 1. Opcja: 100% PUNKTY
        if (pointsMethod.getDiscount() > 0) {
            BigDecimal pointsNeeded = order.getValue();
            if (pointsNeeded.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = calculateDiscount(order.getValue(), pointsMethod.getDiscount());
                BigDecimal amountAfterDiscount = order.getValue().subtract(discount);

                options.add(new PaymentOption(
                        order,
                        pointsMethod,
                        amountAfterDiscount,
                        Optional.empty(),
                        BigDecimal.ZERO,
                        discount
                ));
            }
        }

        // 2. Opcja: Punkty (>=10%) + Karta
        BigDecimal minPointsAmount = calculateDiscount(order.getValue(), 10);
        BigDecimal discountFor10Percent = calculateDiscount(order.getValue(), 10);
        BigDecimal totalAfterDiscount = order.getValue().subtract(discountFor10Percent);

        for (PaymentMethod card : cardMethods) {
            BigDecimal cardAmount = totalAfterDiscount.subtract(minPointsAmount);
            if (cardAmount.compareTo(BigDecimal.ZERO) > 0) {
                options.add(new PaymentOption(
                        order,
                        pointsMethod,
                        minPointsAmount,
                        Optional.of(card),
                        cardAmount,
                        discountFor10Percent
                ));
            }
        }

        // 3. Opcja: 100% Karta z promocją
        for (PaymentMethod card : cardMethods) {
            if (order.getPromotions().contains(card.getId()) && card.getDiscount() > 0) {
                BigDecimal discount = calculateDiscount(order.getValue(), card.getDiscount());
                BigDecimal amountAfterDiscount = order.getValue().subtract(discount);

                options.add(new PaymentOption(
                        order,
                        card,
                        amountAfterDiscount,
                        Optional.empty(),
                        BigDecimal.ZERO,
                        discount
                ));
            }
        }

        // 4. Opcja: 100% Karta bez promocji
        for (PaymentMethod card : cardMethods) {
            if (!order.getPromotions().contains(card.getId()) || card.getDiscount() == 0) {
                options.add(new PaymentOption(
                        order,
                        card,
                        order.getValue(),
                        Optional.empty(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            }
        }

        return options;
    }

    private boolean tryReallocateResources(PaymentOption option) {
        return false; //todo:
    }






}
