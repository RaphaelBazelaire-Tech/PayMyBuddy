package com.paymybuddy.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionModel {

    private Integer id;
    private UserModel sender;
    private UserModel receiver;
    private String description;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime dateTransaction;
}
