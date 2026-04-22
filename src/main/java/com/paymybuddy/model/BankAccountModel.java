package com.paymybuddy.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountModel {

    private Integer id;
    private String iban;
    private String bic;
    private String bankName;

}
