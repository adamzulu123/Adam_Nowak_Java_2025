package com.company.paymentoptimizer.algo;

import com.company.paymentoptimizer.io.InputReader;
import com.company.paymentoptimizer.model.Allocation;
import com.company.paymentoptimizer.model.Order;
import com.company.paymentoptimizer.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptimizerTest {

    private PaymentMethod pointsMethod;
    private PaymentMethod bankCard1;
    private PaymentMethod bankCard2;
    private InputReader inputReader;

    @BeforeEach
    void setUp() {
        pointsMethod = new PaymentMethod("PUNKTY", 15, new BigDecimal("1000.00"));
        bankCard1 = new PaymentMethod("CARD1", 5, new BigDecimal("500.00"));
        bankCard2 = new PaymentMethod("CARD2", 8, new BigDecimal("800.00"));
        inputReader = new InputReader();
    }

    @Test
    void shouldAllocateFullCardOptionWithHighestDiscount() {
        // Given
        Order order = new Order("order1", new BigDecimal("300.00"), Arrays.asList("CARD1", "CARD2"));
        List<Order> orders = Collections.singletonList(order);
        List<PaymentMethod> paymentMethods = Arrays.asList(pointsMethod, bankCard1, bankCard2);

        // When
        Optimizer optimizer = new Optimizer(orders, paymentMethods);
        List<Allocation> result = optimizer.optimize();

        // Then
        assertEquals(1, result.size());
        Allocation allocation = result.getFirst();
        assertEquals("order1", allocation.getOrder().getId());
        assertEquals("CARD2", allocation.getPrimaryMethod().getId());
        assertEquals(new BigDecimal("276.00"), allocation.getAmountFromPrimaryMethod());
        assertFalse(allocation.getSecondaryPayment().isPresent());
        assertEquals(new BigDecimal("24.00"), allocation.getDiscountValue());
    }

    @Test
    void shouldAllocateFullPointsPaymentWhenAvailable() {
        // Given
        Order order = new Order("order1", new BigDecimal("200.00"), Collections.emptyList());
        List<Order> orders = Collections.singletonList(order);
        List<PaymentMethod> paymentMethods = Arrays.asList(pointsMethod, bankCard1);

        // When
        Optimizer optimizer = new Optimizer(orders, paymentMethods);
        List<Allocation> result = optimizer.optimize();

        // Then
        assertEquals(1, result.size());
        Allocation allocation = result.getFirst();
        assertEquals("PUNKTY", allocation.getPrimaryMethod().getId());
        assertEquals(new BigDecimal("170.00"), allocation.getAmountFromPrimaryMethod());
        assertEquals(new BigDecimal("30.00"), allocation.getDiscountValue());
    }

    @Test
    void shouldAllocatePointsAndCardWithSpecialDiscount() {
        // Given
        Order order = new Order("order1", new BigDecimal("400.00"), Collections.emptyList());
        List<Order> orders = Collections.singletonList(order);

        // zmniejszamy limit pkt
        PaymentMethod limitedPoints = new PaymentMethod("PUNKTY", 15, new BigDecimal("50.00"));
        List<PaymentMethod> paymentMethods = Arrays.asList(limitedPoints, bankCard1, bankCard2);

        // When
        Optimizer optimizer = new Optimizer(orders, paymentMethods);
        List<Allocation> result = optimizer.optimize();

        // Then
        assertEquals(1, result.size());
        Allocation allocation = result.getFirst();
        assertEquals("PUNKTY", allocation.getPrimaryMethod().getId());
        assertEquals(new BigDecimal("50.00"), allocation.getAmountFromPrimaryMethod());
        assertTrue(allocation.getSecondaryPayment().isPresent());
        assertEquals(new BigDecimal("40.00"), allocation.getDiscountValue());
    }

    @Test
    void shouldHandleNoAvailablePaymentMethods() {
        // Given
        Order order = new Order("order1", new BigDecimal("2000.00"), Collections.emptyList());
        List<Order> orders = Collections.singletonList(order);

        // za małe limity
        PaymentMethod smallPoints = new PaymentMethod("PUNKTY", 15, new BigDecimal("10.00"));
        PaymentMethod smallCard = new PaymentMethod("CARD1", 5, new BigDecimal("100.00"));
        List<PaymentMethod> paymentMethods = Arrays.asList(smallPoints, smallCard);

        // When
        Optimizer optimizer = new Optimizer(orders, paymentMethods);
        List<Allocation> result = optimizer.optimize();

        // Then
        // Oczekujemy że nie będzie allocacji dla tego zamówienia - czyli zamówienie nie opłacone
        assertEquals(0, result.size());
    }

    @Test
    void shouldThrowExceptionWhenNoPointsMethodProvided() {
        // Given
        Order order = new Order("order1", new BigDecimal("100.00"), Collections.emptyList());
        List<Order> orders = Collections.singletonList(order);
        List<PaymentMethod> paymentMethods = Arrays.asList(bankCard1, bankCard2);

        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            // When
            new Optimizer(orders, paymentMethods);
        });
    }

    @Test
    void shouldHandleZeroValueOrder() {
        // Given
        Order zeroOrder = new Order("order1", BigDecimal.ZERO, List.of("CARD1"));
        List<Order> orders = Collections.singletonList(zeroOrder);
        List<PaymentMethod> paymentMethods = Arrays.asList(pointsMethod, bankCard1);

        // When
        Optimizer optimizer = new Optimizer(orders, paymentMethods);
        List<Allocation> result = optimizer.optimize();

        // Then
        assertEquals(1, result.size());
        Allocation allocation = result.getFirst();
        assertEquals(BigDecimal.ZERO.setScale(2), allocation.getAmountFromPrimaryMethod());
        assertEquals(BigDecimal.ZERO.setScale(2), allocation.getDiscountValue());
    }

    @Test
    void shouldOptimizeMultipleOrdersWithDifferentPromotions() {
        // Given
        Order order1 = new Order("order1", new BigDecimal("100.00"), List.of("CARD1"));
        Order order2 = new Order("order2", new BigDecimal("200.00"), List.of("CARD2"));
        Order order3 = new Order("order3", new BigDecimal("300.00"), Collections.emptyList());
        List<Order> orders = Arrays.asList(order1, order2, order3);

        List<PaymentMethod> paymentMethods = Arrays.asList(
                new PaymentMethod("PUNKTY", 15, new BigDecimal("400.00")),
                new PaymentMethod("CARD1", 5, new BigDecimal("500.00")),
                new PaymentMethod("CARD2", 8, new BigDecimal("500.00"))
        );

        // When
        Optimizer optimizer = new Optimizer(orders, paymentMethods);
        List<Allocation> result = optimizer.optimize();

        // Then
        assertEquals(3, result.size());

        // zamówienie pierwsze opłacone przez CARD1
        Optional<Allocation> allocation1 = result.stream()
                .filter(a -> a.getOrder().getId().equals("order1"))
                .findFirst();
        assertTrue(allocation1.isPresent());
        assertEquals("CARD1", allocation1.get().getPrimaryMethod().getId());

        // zamówienie 2 opłacone przez CARD2
        Optional<Allocation> allocation2 = result.stream()
                .filter(a -> a.getOrder().getId().equals("order2"))
                .findFirst();
        assertTrue(allocation2.isPresent());
        assertEquals("CARD2", allocation2.get().getPrimaryMethod().getId());

        // ostatnie przez PUNKTY
        Optional<Allocation> allocation3 = result.stream()
                .filter(a -> a.getOrder().getId().equals("order3"))
                .findFirst();
        assertTrue(allocation3.isPresent());
        assertEquals("PUNKTY", allocation3.get().getPrimaryMethod().getId());
    }

    //test do porówania przykładowych orders i paymentMethods z polecenia
    @Test
    void testExampleOrdersAndPaymentMethods() throws URISyntaxException, IOException {
        URL resource = getClass().getClassLoader().getResource("orders.json");
        String filePath = Paths.get(resource.toURI()).toString();
        List<Order> orders = inputReader.readOrders(filePath);

        URL payments = getClass().getClassLoader().getResource("paymentmethods.json");
        String filePath2 = Paths.get(payments.toURI()).toString();
        List<PaymentMethod> methods = inputReader.readPaymentMethods(filePath2);

        Optimizer optimizer = new Optimizer(orders, methods);
        List<Allocation> actualAllocations = optimizer.optimize();

        // oczekiwane wyniki
        Order order1 = findOrderById(orders, "ORDER1");
        Order order2 = findOrderById(orders, "ORDER2");
        Order order3 = findOrderById(orders, "ORDER3");
        Order order4 = findOrderById(orders, "ORDER4");

        PaymentMethod punkty = findMethodById(methods, "PUNKTY");
        PaymentMethod mZysk = findMethodById(methods, "mZysk");
        PaymentMethod bos = findMethodById(methods, "BosBankrut");

        List<Allocation> expected = List.of(
                new Allocation(order3, mZysk, new BigDecimal("135.00"), new BigDecimal("15.00"), Optional.empty()),
                new Allocation(order2, bos, new BigDecimal("190.00"), new BigDecimal("10.00"), Optional.empty()),
                new Allocation(order1, punkty, new BigDecimal("85.00"), new BigDecimal("15.00"), Optional.empty()),
                new Allocation(order4, punkty, new BigDecimal("15.00"), new BigDecimal("5.00"),
                        Optional.of(new Allocation.SecondaryPayment(mZysk, new BigDecimal("30.00"))))
        );

        assertEquals(expected, actualAllocations);
    }

    // pomocnicze metody
    private Order findOrderById(List<Order> orders, String id) {
        return orders.stream().filter(o -> o.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing order: " + id));
    }

    private PaymentMethod findMethodById(List<PaymentMethod> methods, String id) {
        return methods.stream().filter(m -> m.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing method: " + id));
    }



}
