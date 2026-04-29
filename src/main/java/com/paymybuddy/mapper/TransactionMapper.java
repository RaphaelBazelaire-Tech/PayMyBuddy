package com.paymybuddy.mapper;

import com.paymybuddy.model.TransactionModel;
import com.paymybuddy.repository.entity.TransactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final UserMapper userMapper;

    public TransactionModel toModel(TransactionEntity transactionEntity) {
        if (transactionEntity == null) return null;

        return TransactionModel.builder()
                .id(transactionEntity.getId())
                .sender(userMapper.toModelShallow(transactionEntity.getSender()))
                .receiver(userMapper.toModelShallow(transactionEntity.getReceiver()))
                .description(transactionEntity.getDescription())
                .amount(transactionEntity.getAmount())
                .fee(transactionEntity.getFee())
                .dateTransaction(transactionEntity.getDateTransaction())
                .build();
    }
}
