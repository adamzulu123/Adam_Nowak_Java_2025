package com.company.paymentoptimizer.io;

import com.company.paymentoptimizer.model.Order;
import com.company.paymentoptimizer.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InputReaderTest {

    private InputReader inputReader;

    @BeforeEach
    public void setUp() {
        inputReader = new InputReader();
    }

    @Test
    void readOrdersFromJsonFile() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader()
                .getResource("orders.json");

        assertNotNull(resource, "orders.json not found");
        String filePath = Paths.get(resource.toURI()).toString();
        List<Order> orders = inputReader.readOrders(filePath);

        assertFalse(orders.isEmpty(), "orders list is empty");
        assertNotNull(orders.getFirst().getId());

        assertEquals(orders.getFirst().getValue(), new BigDecimal("100.00"));
    }

    @Test
    void readPaymentMethodsFromJsonFile() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader()
                .getResource("paymentmethods.json");

        assertNotNull(resource, "paymentmethods.json not found");

        String filePath = Paths.get(resource.toURI()).toString();
        List<PaymentMethod> methods = inputReader.readPaymentMethods(filePath);

        assertFalse(methods.isEmpty(), "Payment methods list should not be empty");
        assertNotNull(methods.getFirst().getId());

        assertEquals(methods.getFirst().getId(), "PUNKTY");
        assertEquals(methods.get(2).getDiscount(), 5);
    }

}
