package com.company.paymentoptimizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Order {
    private String id;
    private BigDecimal value;
    private List<String> promotions = new ArrayList<>();

    @JsonCreator
    public Order(@JsonProperty("id") String id,
                 @JsonProperty("value") BigDecimal value,
                 @JsonProperty("promotions") List<String> promotions) {

        this.id = id;
        this.value = value;
        this.promotions = promotions != null ? promotions : new ArrayList<>();
    }


}
