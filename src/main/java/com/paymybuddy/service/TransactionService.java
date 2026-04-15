package com.paymybuddy.service;

import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TransactionService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.005");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    public Transaction transfer(String senderEmail, Integer receiverId, BigDecimal amount, String description) {

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("L'expéditeur est introuvable."));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Le destinataire est introuvable."));

        boolean isConnection = sender.getConnections().stream()
                .anyMatch(connection -> connection.getId().equals(receiver.getId()));

        if (!isConnection) {
            throw new RuntimeException("Vous ne pouvez envoyer de l'argent qu'à vos contacts.");
        }

        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeducted = amount.add(fee);

        if (sender.getBalance().compareTo(totalDeducted) < 0) {
            throw new RuntimeException("Solde insuffisant. Il vous faut au moins " + totalDeducted + " € (montant + 0,5% de frais)");
        }

        sender.setBalance(sender.getBalance().subtract(totalDeducted));
        receiver.setBalance(receiver.getBalance().add(amount));

        userRepository.save(sender);
        userRepository.save(receiver);

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setDescription(description);
        transaction.setDateTransaction(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsBySender(User sender) {
        return transactionRepository.findBySenderOrderByDateTransactionDesc(sender);
    }
}
