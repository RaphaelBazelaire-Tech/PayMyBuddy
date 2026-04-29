package com.paymybuddy.service;

import com.paymybuddy.mapper.TransactionMapper;
import com.paymybuddy.model.TransactionModel;
import com.paymybuddy.repository.entity.TransactionEntity;
import com.paymybuddy.repository.entity.UserEntity;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.005");

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    public TransactionModel transfer(String senderEmail, Integer receiverId, BigDecimal amount, String description) {

        UserEntity sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("L'expéditeur est introuvable."));

        UserEntity receiver = userRepository.findById(receiverId)
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

        TransactionEntity transaction = new TransactionEntity();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setDescription(description);
        transaction.setDateTransaction(LocalDateTime.now());
        TransactionEntity saved = transactionRepository.save(transaction);

        return transactionMapper.toModel(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionModel> getTransactionsBySender(String senderEmail) {

        UserEntity sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Expéditeur introuvable."));

        return transactionRepository
                .findBySenderOrderByDateTransactionDesc(sender)
                .stream()
                .map(transactionMapper::toModel)
                .collect(Collectors.toList());
    }
}
