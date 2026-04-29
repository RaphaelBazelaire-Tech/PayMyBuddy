package com.paymybuddy.service;

import com.paymybuddy.controller.dto.RegisterDTO;
import com.paymybuddy.mapper.BankAccountMapper;
import com.paymybuddy.mapper.UserMapper;
import com.paymybuddy.model.BankAccountModel;
import com.paymybuddy.model.UserModel;
import com.paymybuddy.repository.entity.BankAccountEntity;
import com.paymybuddy.repository.entity.UserEntity;
import com.paymybuddy.repository.BankAccountRepository;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final BankAccountMapper bankAccountMapper;

    @Transactional(readOnly = true)
    public UserModel findByEmail(String email) {
        UserEntity userEntity = findEntityByEmail(email);
        return userMapper.toModel(userEntity);
    }

    @Transactional(readOnly = true)
    public List<BankAccountModel> getBankAccounts(String email) {
        UserEntity userEntity = findEntityByEmail(email);
        return bankAccountRepository.findByUser(userEntity).stream()
                .map(bankAccountMapper::toModel)
                .collect(Collectors.toList());
    }

    public void register(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Ce mail est déjà utilisé.");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas.");
        }

        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setBalance(BigDecimal.ZERO);

        userRepository.save(user);
    }

    public void addConnection(String userEmail, String friendEmail) {
        if (userEmail.equalsIgnoreCase(friendEmail)) {
            throw new RuntimeException("Vous ne pouvez pas vous ajouter vous-même.");
        }

        UserEntity currentUser = findEntityByEmail(userEmail);
        UserEntity friendUser = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec l'email : " + friendEmail));

        boolean alreadyAdded = currentUser.getConnections().stream()
                .anyMatch(connection -> connection.getId().equals(friendUser.getId()));

        if (alreadyAdded) {
            throw new RuntimeException("Cette personne est déjà dans vos contacts.");
        }

        currentUser.getConnections().add(friendUser);
        userRepository.save(currentUser);
    }

    public void removeConnection(String userEmail, int friendId) {
        UserEntity currentUser = findEntityByEmail(userEmail);
        currentUser.getConnections().removeIf(connection -> connection.getId().equals(friendId));
        userRepository.save(currentUser);
    }

    public void deposit(String userEmail, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant du dépôt doit être positif.");
        }

        UserEntity user = findEntityByEmail(userEmail);
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
    }

    public void withdrawToBankAccount(String email, Integer bankAccountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant du virement doit être positif.");
        }
        UserEntity user = findEntityByEmail(email);

        BankAccountEntity bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Compte bancaire introuvable."));

        if (!bankAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Ce compte bancaire ne vous appartient pas.");
        }

        if (user.getBalance().compareTo(amount) <= 0) {
            throw new RuntimeException("Le solde est insuffisant pour ce virement.");
        }
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
    }

    public void addBankAccount(String email, String iban, String bic, String bankName) {
        UserEntity user = findEntityByEmail(email);
        BankAccountEntity bankAccount = new BankAccountEntity();
        bankAccount.setUser(user);
        bankAccount.setIban(iban.toUpperCase().replaceAll("\\s+", ""));
        bankAccount.setBic(bic.toUpperCase());
        bankAccount.setBankName(bankName);
        bankAccountRepository.save(bankAccount);
    }

    public void deleteBankAccount(String email, Integer bankAccountId) {
        UserEntity user = findEntityByEmail(email);
        BankAccountEntity bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Compte bancaire introuvable."));

        if (!bankAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Ce compte bancaire ne vous appartient pas.");
        }
        bankAccountRepository.delete(bankAccount);
    }

    public void updateUsername(String email, String newUsername) {
        UserEntity user = findEntityByEmail(email);
        user.setUsername(newUsername);
        userRepository.save(user);
    }

    public void updatePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        UserEntity user = findEntityByEmail(email);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Les nouveaux mots de passe ne correspondent pas.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    UserEntity findEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + email));
    }
}
