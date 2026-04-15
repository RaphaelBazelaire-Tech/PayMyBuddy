package com.paymybuddy.service;

import com.paymybuddy.controller.dto.RegisterDTO;
import com.paymybuddy.model.BankAccount;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.BankAccountRepository;
import com.paymybuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + email));
    }

    public void register(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Ce mail est déjà utilisé.");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas.");
        }

        User user = new User();
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

        User currentUser = findByEmail(userEmail);
        User friendUser = userRepository.findByEmail(friendEmail)
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
        User currentUser = findByEmail(userEmail);
        currentUser.getConnections().removeIf(connection -> connection.getId().equals(friendId));
        userRepository.save(currentUser);
    }

    public void deposit(String userEmail, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant du dépôt doit être positif.");
        }

        User user = findByEmail(userEmail);
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
    }

    public void withdrawToBankAccount(String email, Integer bankAccountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant du virement doit être positif.");
        }
        User user = findByEmail(email);

        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
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
        User user = findByEmail(email);
        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setIban(iban.toUpperCase().replaceAll("\\s+", ""));
        bankAccount.setBic(bic.toUpperCase());
        bankAccount.setBankName(bankName);
        bankAccountRepository.save(bankAccount);
    }

    public void deleteBankAccount(String email, Integer bankAccountId) {
        User user = findByEmail(email);
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Compte bancaire introuvable."));

        if (!bankAccount.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Ce compte bancaire ne vous appartient pas.");
        }
        bankAccountRepository.delete(bankAccount);
    }

    public void updateUsername(String email, String newUsername) {
        User user = findByEmail(email);
        user.setUsername(newUsername);
        userRepository.save(user);
    }

    public void updatePassword(String email, String currentPassword, String newPassword, String confirmPassword) {
        User user = findByEmail(email);

        if (!passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Les nouveaux mots de passe ne correspondent pas.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
