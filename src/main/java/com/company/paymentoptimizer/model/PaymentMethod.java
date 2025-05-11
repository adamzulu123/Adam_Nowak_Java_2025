package com.company.paymentoptimizer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PaymentMethod {
    private String id;
    private int discount;
    @Getter
    private BigDecimal limit;
    private BigDecimal used = BigDecimal.ZERO;

    @JsonCreator
    public PaymentMethod(@JsonProperty("id") String id,
                         @JsonProperty("discount") int discount,
                         @JsonProperty("limit") BigDecimal limit){

        this.id = id;
        this.discount = discount;
        this.limit = limit;
    }

    //helper methods

    public BigDecimal getAvailable() {
        return limit.subtract(used);
    }

    public boolean isPointsMethod(){
        return "PUNKTY".equals(id);
    }

    public void addUsed(BigDecimal amount){
        this.used = this.used.add(amount);
    }

    //sprawdza czy możemy w całości obłacic zamówienie
    public boolean canFullyCover(BigDecimal amount){
        return getAvailable().compareTo(amount) >= 0;
    }

    //jak zachłanny popełni bład to zawsze możemy wycofać opłatne zamówienia za pomoca danej metody
    public void rollbackUsed(BigDecimal amount) {
        this.used = this.used.subtract(amount);
    }




}
