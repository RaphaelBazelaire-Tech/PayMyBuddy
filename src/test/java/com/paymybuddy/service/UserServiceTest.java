package com.paymybuddy.service;

import com.paymybuddy.controller.dto.RegisterDTO;
import com.paymybuddy.mapper.BankAccountMapper;
import com.paymybuddy.mapper.UserMapper;
import com.paymybuddy.model.BankAccountModel;
import com.paymybuddy.model.UserModel;
import com.paymybuddy.repository.BankAccountRepository;
import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.repository.entity.BankAccountEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @InjectMocks
    private UserService service;

    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    void setUp() {
        alice = new UserEntity();
        alice.setId(1);
        alice.setUsername("Alice");
        alice.setEmail("alice@paymybuddy.com");
        alice.setPassword("hashedPassword");
        alice.setBalance(new BigDecimal("100.00"));
        alice.setConnections(new HashSet<>());

        bob = new UserEntity();
        bob.setId(2);
        bob.setUsername("Bob");
        bob.setEmail("bob@paymybuddy.com");
        bob.setPassword("hashedBobPassword");
        bob.setBalance(new BigDecimal("50.00"));
        bob.setConnections(new HashSet<>());
    }

    @Nested
    public class FindByEmailTests {

        @Test
        public void findByEmailExistingShouldReturnMappedUser() {
            UserModel mapped = new UserModel();
            mapped.setId(1);
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(userMapper.toModel(alice)).thenReturn(mapped);

            UserModel result = service.findByEmail("alice@paymybuddy.com");

            assertThat(result).isSameAs(mapped);
        }

        @Test
        public void findByEmailUnknownShouldThrow() {
            when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByEmail("ghost@nowhere.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Utilisateur introuvable : ghost@nowhere.com");
        }
    }

    @Nested
    public class GetBankAccountsTests {

        @Test
        public void getBankAccountsWithAccountsShouldReturnMappedList() {
            BankAccountEntity acc1 = new BankAccountEntity();
            acc1.setId(10);

            BankAccountEntity acc2 = new BankAccountEntity();
            acc2.setId(11);

            BankAccountModel model1 = new BankAccountModel();
            model1.setId(10);

            BankAccountModel model2 = new BankAccountModel();
            model2.setId(11);

            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findByUser(alice)).thenReturn(List.of(acc1, acc2));
            when(bankAccountMapper.toModel(acc1)).thenReturn(model1);
            when(bankAccountMapper.toModel(acc2)).thenReturn(model2);

            List<BankAccountModel> result = service.getBankAccounts("alice@paymybuddy.com");

            assertThat(result).extracting(BankAccountModel::getId).containsExactly(10, 11);
        }

        @Test
        public void getBankAccountsNoAccountShouldReturnEmpty() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findByUser(alice)).thenReturn(List.of());

            assertThat(service.getBankAccounts("alice@paymybuddy.com")).isEmpty();
        }

        @Test
        public void getBankAccountsUnknownUserShouldThrow() {
            when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBankAccounts("ghost@nowhere.com"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    public class RegisterTests {

        private RegisterDTO validDto() {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("Charlie");
            dto.setEmail("charlie@paymybuddy.com");
            dto.setPassword("secret123");
            dto.setConfirmPassword("secret123");
            return dto;
        }

        @Test
        public void registerEmailAlreadyUsedShouldThrow() {
            RegisterDTO dto = validDto();
            when(userRepository.existsByEmail("charlie@paymybuddy.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Ce mail est déjà utilisé.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void registerPasswordsMismatchShouldThrow() {
            RegisterDTO dto = validDto();
            dto.setConfirmPassword("different");
            when(userRepository.existsByEmail("charlie@paymybuddy.com")).thenReturn(false);

            assertThatThrownBy(() -> service.register(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Les mots de passe ne correspondent pas.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void registerSuccessShouldSaveUserWithEncodedPassword() {
            RegisterDTO dto = validDto();
            when(userRepository.existsByEmail("charlie@paymybuddy.com")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");

            service.register(dto);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            UserEntity saved = captor.getValue();
            assertThat(saved.getUsername()).isEqualTo("Charlie");
            assertThat(saved.getEmail()).isEqualTo("charlie@paymybuddy.com");
            assertThat(saved.getPassword()).isEqualTo("encodedSecret");
            assertThat(saved.getBalance()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    public class AddConnectionTests {

        @Test
        public void addConnectionSelfAddShouldThrow() {
            assertThatThrownBy(() ->
                    service.addConnection("alice@paymybuddy.com", "ALICE@paymybuddy.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Vous ne pouvez pas vous ajouter vous-même.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void addConnectionFriendNotFoundShouldThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.addConnection("alice@paymybuddy.com", "ghost@nowhere.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Aucun utilisateur trouvé avec l'email : ghost@nowhere.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void addConnectionAlreadyAddedShouldThrow() {
            alice.getConnections().add(bob);
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(userRepository.findByEmail("bob@paymybuddy.com")).thenReturn(Optional.of(bob));

            assertThatThrownBy(() ->
                    service.addConnection("alice@paymybuddy.com", "bob@paymybuddy.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cette personne est déjà dans vos contacts.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void addConnectionSuccessShouldAddAndSave() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(userRepository.findByEmail("bob@paymybuddy.com")).thenReturn(Optional.of(bob));

            service.addConnection("alice@paymybuddy.com", "bob@paymybuddy.com");

            assertThat(alice.getConnections()).contains(bob);
            verify(userRepository).save(alice);
        }
    }

    @Nested
    public class RemoveConnectionTests {

        @Test
        public void removeConnectionExistingShouldRemoveAndSave() {
            alice.getConnections().add(bob);
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));

            service.removeConnection("alice@paymybuddy.com", 2);

            assertThat(alice.getConnections()).doesNotContain(bob);
            verify(userRepository).save(alice);
        }

        @Test
        public void removeConnectionNotInSetShouldNotThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));

            service.removeConnection("alice@paymybuddy.com", 99);

            assertThat(alice.getConnections()).isEmpty();
            verify(userRepository).save(alice);
        }
    }

    @Nested
    public class DepositTests {

        @Test
        public void depositNonPositiveShouldThrow() {
            assertThatThrownBy(() ->
                    service.deposit("alice@paymybuddy.com", BigDecimal.ZERO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Le montant du dépôt doit être positif.");

            assertThatThrownBy(() ->
                    service.deposit("alice@paymybuddy.com", new BigDecimal("-10")))
                    .isInstanceOf(RuntimeException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        public void depositSuccessShouldIncreaseBalanceAndSave() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));

            service.deposit("alice@paymybuddy.com", new BigDecimal("25.00"));

            assertThat(alice.getBalance()).isEqualByComparingTo("125.00");
            verify(userRepository).save(alice);
        }
    }

    @Nested
    public class WithdrawTests {

        private BankAccountEntity aliceBank() {
            BankAccountEntity acc = new BankAccountEntity();
            acc.setId(10);
            acc.setUser(alice);
            return acc;
        }

        @Test
        public void withdrawNonPositiveShouldThrow() {
            assertThatThrownBy(() ->
                    service.withdrawToBankAccount("alice@paymybuddy.com", 10, BigDecimal.ZERO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Le montant du virement doit être positif.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void withdrawBankAccountNotFoundShouldThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(10)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.withdrawToBankAccount("alice@paymybuddy.com", 10, new BigDecimal("20")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Compte bancaire introuvable.");
        }

        @Test
        public void withdrawBankAccountNotOwnedShouldThrow() {
            BankAccountEntity bobBank = new BankAccountEntity();
            bobBank.setId(20);
            bobBank.setUser(bob);

            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(20)).thenReturn(Optional.of(bobBank));

            assertThatThrownBy(() ->
                    service.withdrawToBankAccount("alice@paymybuddy.com", 20, new BigDecimal("10")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Ce compte bancaire ne vous appartient pas.");
        }

        @Test
        public void withdrawInsufficientBalanceShouldThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(10)).thenReturn(Optional.of(aliceBank()));

            assertThatThrownBy(() ->
                    service.withdrawToBankAccount("alice@paymybuddy.com", 10, new BigDecimal("100.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Le solde est insuffisant pour ce virement.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void withdrawSuccessShouldDecreaseBalanceAndSave() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(10)).thenReturn(Optional.of(aliceBank()));

            service.withdrawToBankAccount("alice@paymybuddy.com", 10, new BigDecimal("30.00"));

            assertThat(alice.getBalance()).isEqualByComparingTo("70.00");
            verify(userRepository).save(alice);
        }
    }

    @Nested
    public class AddBankAccountTests {

        @Test
        public void addBankAccountSuccessShouldNormalizeAndSave() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));

            service.addBankAccount(
                    "alice@paymybuddy.com",
                    "fr76 1234 5678 9012 3456 7890 123",
                    "bnpafrpp",
                    "BNP Paribas");

            ArgumentCaptor<BankAccountEntity> captor = ArgumentCaptor.forClass(BankAccountEntity.class);
            verify(bankAccountRepository).save(captor.capture());
            BankAccountEntity saved = captor.getValue();
            assertThat(saved.getIban()).isEqualTo("FR7612345678901234567890123");
            assertThat(saved.getBic()).isEqualTo("BNPAFRPP");
            assertThat(saved.getBankName()).isEqualTo("BNP Paribas");
            assertThat(saved.getUser()).isEqualTo(alice);
        }
    }

    @Nested
    public class DeleteBankAccountTests {

        @Test
        public void deleteBankAccountNotFoundShouldThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(10)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.deleteBankAccount("alice@paymybuddy.com", 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Compte bancaire introuvable.");

            verify(bankAccountRepository, never()).delete(any());
        }

        @Test
        public void deleteNotOwnedShouldThrow() {
            BankAccountEntity bobBank = new BankAccountEntity();
            bobBank.setId(20);
            bobBank.setUser(bob);

            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(20)).thenReturn(Optional.of(bobBank));

            assertThatThrownBy(() ->
                    service.deleteBankAccount("alice@paymybuddy.com", 20))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Ce compte bancaire ne vous appartient pas.");

            verify(bankAccountRepository, never()).delete(any());
        }

        @Test
        public void deleteSuccessShouldDelete() {
            BankAccountEntity aliceBank = new BankAccountEntity();
            aliceBank.setId(10);
            aliceBank.setUser(alice);

            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(bankAccountRepository.findById(10)).thenReturn(Optional.of(aliceBank));

            service.deleteBankAccount("alice@paymybuddy.com", 10);

            verify(bankAccountRepository).delete(aliceBank);
        }
    }

    @Nested
    public class UpdateUsernameTests {

        @Test
        public void updateUsernameSuccessShouldUpdateAndSave() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));

            service.updateUsername("alice@paymybuddy.com", "AliceNewName");

            assertThat(alice.getUsername()).isEqualTo("AliceNewName");
            verify(userRepository).save(alice);
        }
    }

    @Nested
    public class UpdatePasswordTests {

        @Test
        public void updatePasswordWrongCurrentShouldThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

            assertThatThrownBy(() ->
                    service.updatePassword("alice@paymybuddy.com", "wrong", "newPass", "newPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Les mots de passe ne correspondent pas.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void updatePasswordNewDifferentFromConfirmShouldThrow() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(passwordEncoder.matches("oldPass", "hashedPassword")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.updatePassword("alice@paymybuddy.com", "oldPass", "new1", "new2"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Les nouveaux mots de passe ne correspondent pas.");

            verify(userRepository, never()).save(any());
        }

        @Test
        public void updatePasswordSuccessShouldUpdateAndSave() {
            when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(alice));
            when(passwordEncoder.matches("oldPass", "hashedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

            service.updatePassword("alice@paymybuddy.com", "oldPass", "newPass", "newPass");

            assertThat(alice.getPassword()).isEqualTo("newHashed");
            verify(userRepository).save(alice);
        }
    }
}
