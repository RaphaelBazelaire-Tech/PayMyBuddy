package com.paymybuddy.service;

import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private UserEntity userEntity;

    @BeforeEach
    public void setUp() {
        userEntity = new UserEntity();
        userEntity.setId(1);
        userEntity.setUsername("Alice");
        userEntity.setEmail("alice@paymybuddy.com");
        userEntity.setPassword("hashedPassword");
    }

    @Test
    public void loadUserByUsernameExistingUserShouldReturnUserDetails() {
        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(userEntity));

        UserDetails result = service.loadUserByUsername("alice@paymybuddy.com");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("alice@paymybuddy.com");
        assertThat(result.getPassword()).isEqualTo("hashedPassword");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    public void loadUserByUsernameUnknownEmailShouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@nowhere.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Aucun utilisateur trouvé avec l'email : ghost@nowhere.com");
    }
}
