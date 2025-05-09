package com.company.paymentoptimizer;

import com.company.paymentoptimizer.io.InputReader;
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
            InputReader inputReader = new InputReader();
            List<Order> orders = inputReader.readOrders(ordersPath);
            List<PaymentMethod> paymentMethods = inputReader.readPaymentMethods(paymentMethodsPath);
            System.out.println(orders);
            System.out.println(paymentMethods);


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
