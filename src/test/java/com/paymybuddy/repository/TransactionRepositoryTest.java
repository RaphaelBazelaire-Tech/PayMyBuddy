package com.paymybuddy.repository;

import com.paymybuddy.repository.entity.TransactionEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
public class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void findBySenderShouldReturnTransactionsOrderedByDateDesc() {
        UserEntity alice = persistUser("Alice", "alice@paymybuddy.com");
        UserEntity bob = persistUser("Bob", "bob@paymybuddy.com");

        LocalDateTime oldest = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime middle = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime newest = LocalDateTime.of(2025, 2, 1, 10, 0);

        persistTransaction(alice, bob, "10.00", middle);
        persistTransaction(alice, bob, "20.00", newest);
        persistTransaction(alice, bob, "5.00", oldest);

        List<TransactionEntity> result = transactionRepository.findBySenderOrderByDateTransactionDesc(alice);

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(TransactionEntity::getDateTransaction)
                .containsExactly(newest, middle, oldest);
    }

    @Test
    public void findBySenderUserWithNoTransactionShouldReturnEmptyList() {
        UserEntity alice = persistUser("Alice", "alice@paymybuddy.com");

        List<TransactionEntity> result = transactionRepository.findBySenderOrderByDateTransactionDesc(alice);

        assertThat(result).isEmpty();
    }

    @Test
    public void findBySenderShouldOnlyReturnSentTransactionsNotReceived() {
        UserEntity alice = persistUser("Alice", "alice@paymybuddy.com");
        UserEntity bob = persistUser("Bob", "bob@paymybuddy.com");

        LocalDateTime now = LocalDateTime.now();

        persistTransaction(alice, bob, "10.00", now);
        persistTransaction(bob, alice, "20.00", now);

        List<TransactionEntity> result = transactionRepository.findBySenderOrderByDateTransactionDesc(alice);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAmount()).isEqualByComparingTo("10.00");
    }

    private UserEntity persistUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashedPassword");
        user.setBalance(new BigDecimal("100.00"));
        return entityManager.persistAndFlush(user);
    }

    private TransactionEntity persistTransaction(UserEntity sender, UserEntity receiver, String amount, LocalDateTime date) {
        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setSender(sender);
        transactionEntity.setReceiver(receiver);
        transactionEntity.setAmount(new BigDecimal(amount));
        transactionEntity.setFee(new BigDecimal("0.50"));
        transactionEntity.setDateTransaction(date);
        return entityManager.persistAndFlush(transactionEntity);
    }
}
