package com.company.paymentoptimizer;

import com.company.paymentoptimizer.model.Order;
import com.company.paymentoptimizer.model.PaymentMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar paymentoptimizer.jar <input> <output>");
            System.exit(1);
        }

        String ordersPath = args[0];
        String paymentMethodsPath = args[1];

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Order> orders = objectMapper.readValue(new File(ordersPath), new TypeReference<List<Order>>() {});
            List<PaymentMethod> paymentMethods = objectMapper.readValue(new File(paymentMethodsPath),
                    new TypeReference<ArrayList<PaymentMethod>>() {});

            System.out.println("Orders:");
            for (Order order : orders) {
                System.out.println(order);
            }

            System.out.println("\n PaymentMethods:");
            for (PaymentMethod method : paymentMethods) {
                System.out.println(method);
            }

        } catch (IOException e){
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e){
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

    }
}
