package com.example.bettingsystem.pojo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class BettingPO implements Serializable {
    private BigDecimal amount;
}
