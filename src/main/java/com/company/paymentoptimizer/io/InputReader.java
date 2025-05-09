package com.company.paymentoptimizer.io;

import com.company.paymentoptimizer.model.Order;
import com.company.paymentoptimizer.model.PaymentMethod;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Class responsible for reading input files (JSON)
 */
public class InputReader {
    public ObjectMapper mapper;

    public InputReader(){
        mapper = new ObjectMapper();

        //ignore fields that are not in Java class
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true); //allow comments in JSON

        //jeśli potrzebny custom BigDecimal deserializer to tutaj dodac jako SimpleModule
        //jak np potrzebujemy , zamiast . w liczbach
    }

    public List<Order> readOrders (String filepath) throws IOException {
        return Arrays.asList(mapper.readValue(new File(filepath), Order[].class));
    }

    public List<PaymentMethod> readPaymentMethods (String filepath) throws IOException {
        return Arrays.asList(mapper.readValue(new File(filepath), PaymentMethod[].class));
    }


}
