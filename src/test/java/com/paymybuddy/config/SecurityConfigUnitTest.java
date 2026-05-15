package com.paymybuddy.config;

import com.paymybuddy.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SecurityConfigUnitTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    private SecurityConfig securityConfig;

    @BeforeEach
    public void setUp() {
        securityConfig = new SecurityConfig(userDetailsService);
    }

    @Test
    public void passwordEncoderShouldNotBeNull() {
        assertThat(securityConfig.passwordEncoder())
                .isNotNull();
    }

    @Test
    public void passwordEncoderShouldReturnBCryptInstance() {
        assertThat(securityConfig.passwordEncoder())
                .isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    public void passwordEncoderShouldProduceHashDifferentFromRawPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String raw = "MySecretPassword123!";
        String encoded = encoder.encode(raw);

        assertThat(encoded)
                .isNotNull()
                .isNotEqualTo(raw);
    }

    @Test
    void passwordEncoderShouldMatchCorrectPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String raw = "MySecretPassword123!";
        String encoded = encoder.encode(raw);

        assertThat(encoder.matches(raw, encoded))
                .isTrue();
    }

    @Test
    void passwordEncoderShouldNotMatchWrongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("MySecretPassword123");

        assertThat(encoder.matches("WrongPassword", encoded))
                .isFalse();
    }

    @Test
    public void passwordEncoderShouldProduceDifferentHashesDueToSalt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String raw = "SamePassword";

        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
