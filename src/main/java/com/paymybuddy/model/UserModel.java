package com.paymybuddy.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserModel {

    private Integer id;
    private String username;
    private String email;
    private BigDecimal balance;

    @Builder.Default
    private Set<UserModel> connections = new HashSet<>();
}
