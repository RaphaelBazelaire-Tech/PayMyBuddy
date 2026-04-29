package com.paymybuddy.mapper;

import com.paymybuddy.model.BankAccountModel;
import com.paymybuddy.repository.entity.BankAccountEntity;
import org.springframework.stereotype.Component;

@Component
public class BankAccountMapper {

    public BankAccountModel toModel(BankAccountEntity bankAccountEntity) {
        if (bankAccountEntity == null) return null;

        return BankAccountModel.builder()
                .id(bankAccountEntity.getId())
                .iban(bankAccountEntity.getIban())
                .bic(bankAccountEntity.getBic())
                .bankName(bankAccountEntity.getBankName())
                .build();
    }
}
