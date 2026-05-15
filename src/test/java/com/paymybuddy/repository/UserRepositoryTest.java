package com.paymybuddy.repository;

import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void findByEmailExistingEmailShouldReturnUser() {
        UserEntity persisted = persistUser("Alice", "alice@paymybuddy.com");

        Optional<UserEntity> result = userRepository.findByEmail("alice@paymybuddy.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(persisted.getId());
        assertThat(result.get().getUsername()).isEqualTo("Alice");
        assertThat(result.get().getEmail()).isEqualTo("alice@paymybuddy.com");
    }

    @Test
    public void findByEmailUnknownEmailShouldReturnEmptyOptional() {
        persistUser("Alice", "alice@paymybuddy.com");

        Optional<UserEntity> result = userRepository.findByEmail("ghost@nowhere.com");

        assertThat(result).isEmpty();
    }

    @Test
    public void findByEmailEmptyDatabaseShouldReturnEmptyOptional() {
        Optional<UserEntity> result = userRepository.findByEmail("alice@paymybuddy.com");

        assertThat(result).isEmpty();
    }

    @Test
    public void findByEmailCaseSensitiveShouldNotMatchDifferentCase() {
        persistUser("Alice", "alice@paymybuddy.com");

        Optional<UserEntity> result = userRepository.findByEmail("ALICE@paymybuddy.com");

        assertThat(result).isEmpty();
    }

    @Test
    public void existsByEmailExistingEmailShouldReturnTrue() {
        persistUser("Alice", "alice@paymybuddy.com");

        assertThat(userRepository.existsByEmail("alice@paymybuddy.com")).isTrue();
    }

    @Test
    public void existsByEmailUnknownEmailShouldReturnFalse() {
        persistUser("Alice", "alice@paymybuddy.com");

        assertThat(userRepository.existsByEmail("ghost@nowhere.com")).isFalse();
    }

    @Test
    public void existsByEmailEmptyDatabaseShouldReturnFalse() {
        assertThat(userRepository.existsByEmail("any@paymybuddy.com")).isFalse();
    }

    private UserEntity persistUser(String username, String email) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashedPassword");
        user.setBalance(new BigDecimal("100.00"));
        return entityManager.persistAndFlush(user);
    }
}
