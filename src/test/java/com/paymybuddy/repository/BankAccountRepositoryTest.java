package com.paymybuddy.repository;

import com.paymybuddy.repository.entity.BankAccountEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
public class BankAccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Test
    public void findByUserUserWithMultipleAccountsShouldReturnAllAccounts() {
        UserEntity alice = persistUser("Alice", "alice@paymybuddy.com");
        persistBankAccount(alice, "FR7610000000000000000000001", "BNPAFRPP", "BNP Paribas");
        persistBankAccount(alice, "FR7620000000000000000000002", "CRLYFRPP", "Crédit Lyonnais");

        List<BankAccountEntity> result = bankAccountRepository.findByUser(alice);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BankAccountEntity::getBankName)
                .containsExactlyInAnyOrder("BNP Paribas", "Crédit Lyonnais");
    }

    @Test
    public void findByUserUserWithNoAccountShouldReturnEmptyList() {
        UserEntity alice = persistUser("Alice", "alice@paymybuddy.com");

        List<BankAccountEntity> result = bankAccountRepository.findByUser(alice);

        assertThat(result).isEmpty();
    }

    @Test
    public void findByUserShouldOnlyReturnAccountsOfSpecificUser() {
        UserEntity alice = persistUser("Alice", "alice@paymybuddy.com");
        UserEntity bob = persistUser("Bob", "bob@paymybuddy.com");
        persistBankAccount(alice, "FR7610000000000000000000001", "BNPAFRPP", "BNP Paribas");
        persistBankAccount(bob, "FR7620000000000000000000002", "CRLYFRPP", "Crédit Lyonnais");

        List<BankAccountEntity> result = bankAccountRepository.findByUser(alice);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getBankName()).isEqualTo("BNP Paribas");
    }

    private UserEntity persistUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashedPassword");
        user.setBalance(new BigDecimal("100.00"));
        return entityManager.persistAndFlush(user);
    }

    private BankAccountEntity persistBankAccount(UserEntity user, String iban, String bic, String bankName) {
        BankAccountEntity account = new BankAccountEntity();
        account.setUser(user);
        account.setIban(iban);
        account.setBic(bic);
        account.setBankName(bankName);
        return entityManager.persistAndFlush(account);
    }
}
