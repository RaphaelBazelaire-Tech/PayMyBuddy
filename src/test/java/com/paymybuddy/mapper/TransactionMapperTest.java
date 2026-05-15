package com.paymybuddy.mapper;

import com.paymybuddy.model.TransactionModel;
import com.paymybuddy.repository.entity.TransactionEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionMapperTest {

    private TransactionMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new TransactionMapper(new UserMapper());
    }

    @Test
    public void toModelNullInputShouldReturnNull() {
        assertThat(mapper.toModel(null)).isNull();
    }

    @Test
    public void toModelValidEntityShouldMapAllFields() {
        UserEntity sender = createUser(1, "Alice", "alice@paymybuddy.com", "100.00");
        UserEntity receiver = createUser(2, "Bob", "bob@paymybuddy.com", "50.00");
        LocalDateTime txDate = LocalDateTime.of(2025, 1, 15, 12, 30);

        TransactionEntity entity = new TransactionEntity();
        entity.setId(10);
        entity.setSender(sender);
        entity.setReceiver(receiver);
        entity.setDescription("Remboursement pizza");
        entity.setAmount(new BigDecimal("15.00"));
        entity.setFee(new BigDecimal("0.75"));
        entity.setDateTransaction(txDate);

        TransactionModel result = mapper.toModel(entity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        assertThat(result.getDescription()).isEqualTo("Remboursement pizza");
        assertThat(result.getAmount()).isEqualByComparingTo("15.00");
        assertThat(result.getFee()).isEqualByComparingTo("0.75");
        assertThat(result.getDateTransaction()).isEqualTo(txDate);

        assertThat(result.getSender()).isNotNull();
        assertThat(result.getSender().getId()).isEqualTo(1);
        assertThat(result.getSender().getUsername()).isEqualTo("Alice");
        assertThat(result.getSender().getEmail()).isEqualTo("alice@paymybuddy.com");
        assertThat(result.getSender().getBalance()).isEqualByComparingTo("100.00");

        assertThat(result.getReceiver()).isNotNull();
        assertThat(result.getReceiver().getId()).isEqualTo(2);
        assertThat(result.getReceiver().getUsername()).isEqualTo("Bob");
    }

    @Test
    public void toModelShouldMapSenderAndReceiverAsShallow() {
        UserEntity sender = createUser(1, "Alice", "alice@paymybuddy.com", "100.00");
        UserEntity receiver = createUser(2, "Bob", "bob@paymybuddy.com", "50.00");

        sender.getConnections().add(createUser(3, "Charlie", "c@p.com", "20.00"));
        receiver.getConnections().add(createUser(4, "Dave", "d@p.com", "30.00"));

        TransactionEntity entity = new TransactionEntity();
        entity.setId(10);
        entity.setSender(sender);
        entity.setReceiver(receiver);
        entity.setAmount(new BigDecimal("15.00"));

        TransactionModel result = mapper.toModel(entity);

        assertThat(result.getSender().getConnections()).isEmpty();
        assertThat(result.getReceiver().getConnections()).isEmpty();
    }

    private UserEntity createUser(int id, String username, String email, String balance) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setBalance(new BigDecimal(balance));
        return user;
    }
}
