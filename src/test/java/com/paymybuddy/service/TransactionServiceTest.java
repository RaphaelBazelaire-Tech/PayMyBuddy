package com.paymybuddy.service;

import com.paymybuddy.mapper.TransactionMapper;
import com.paymybuddy.model.TransactionModel;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.repository.entity.TransactionEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService service;

    private UserEntity sender;
    private UserEntity receiver;

    @BeforeEach
    public void setUp() {
        sender = new UserEntity();
        sender.setId(1);
        sender.setEmail("alice@paymybuddy.com");
        sender.setBalance(new BigDecimal("100.00"));
        sender.setConnections(new HashSet<>());

        receiver = new UserEntity();
        receiver.setId(2);
        receiver.setEmail("bob@paymybuddy.com");
        receiver.setBalance(new BigDecimal("50.00"));
        receiver.setConnections(new HashSet<>());

        sender.getConnections().add(receiver);
    }

    @Test
    public void transferSenderNotFoundShouldThrow() {
        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(
                "alice@paymybuddy.com", 2, new BigDecimal("10.00"), "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("L'expéditeur est introuvable.");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void transferReceiverNotFoundShouldThrow() {
        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(
                "alice@paymybuddy.com", 2, new BigDecimal("10.00"), "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Le destinataire est introuvable.");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void transferReceiverNotInConnectionsShouldThrow() {
        UserEntity stranger = new UserEntity();
        stranger.setId(99);
        sender.getConnections().clear();

        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(userRepository.findById(99)).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> service.transfer(
                "alice@paymybuddy.com", 99, new BigDecimal("10.00"), "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vous ne pouvez envoyer de l'argent qu'à vos contacts.");

        verify(userRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void transferInsufficientBalanceShouldThrow() {
        sender.setBalance(new BigDecimal("5.00"));

        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> service.transfer(
                "alice@paymybuddy.com", 2, new BigDecimal("100.00"), "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Solde insuffisant")
                .hasMessageContaining("100.50");

        verify(userRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void transferSuccessShouldUpdateBalancesAndPersist() {
        BigDecimal amount = new BigDecimal("20.00");
        BigDecimal expectedFee = new BigDecimal("0.10");
        BigDecimal expectedSenderBalance = new BigDecimal("79.90");
        BigDecimal expectedReceiverBalance = new BigDecimal("70.00");

        TransactionEntity savedTx = new TransactionEntity();
        savedTx.setId(42);
        TransactionModel mappedModel = new TransactionModel();
        mappedModel.setId(42);

        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(receiver));
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedTx);
        when(transactionMapper.toModel(savedTx)).thenReturn(mappedModel);

        TransactionModel result = service.transfer(
                "alice@paymybuddy.com", 2, amount, "Remboursement pizza");

        assertThat(sender.getBalance()).isEqualByComparingTo(expectedSenderBalance);
        assertThat(receiver.getBalance()).isEqualByComparingTo(expectedReceiverBalance);

        verify(userRepository, times(1)).save(sender);
        verify(userRepository, times(1)).save(receiver);

        ArgumentCaptor<TransactionEntity> txCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity captured = txCaptor.getValue();

        assertThat(captured.getSender()).isEqualTo(sender);
        assertThat(captured.getReceiver()).isEqualTo(receiver);
        assertThat(captured.getAmount()).isEqualByComparingTo(amount);
        assertThat(captured.getFee()).isEqualByComparingTo(expectedFee);
        assertThat(captured.getDescription()).isEqualTo("Remboursement pizza");
        assertThat(captured.getDateTransaction()).isNotNull();

        assertThat(result).isSameAs(mappedModel);
    }

    @Test
    public void transferBalanceExactlyEqualsTotalShouldSucceed() {
        sender.setBalance(new BigDecimal("100.50"));

        TransactionEntity savedTx = new TransactionEntity();
        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(receiver));
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedTx);
        when(transactionMapper.toModel(savedTx)).thenReturn(new TransactionModel());

        service.transfer("alice@paymybuddy.com", 2, new BigDecimal("100.00"), "All-in");

        assertThat(sender.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    public void transferFeeRoundingShouldRoundHalfUp() {
        sender.setBalance(new BigDecimal("1000.00"));

        TransactionEntity savedTx = new TransactionEntity();
        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(receiver));
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedTx);
        when(transactionMapper.toModel(savedTx)).thenReturn(new TransactionModel());

        service.transfer("alice@paymybuddy.com", 2, new BigDecimal("33.33"), "Round test");

        ArgumentCaptor<TransactionEntity> txCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getFee()).isEqualByComparingTo("0.17");
    }

    @Test
    public void getTransactionsBySenderSenderNotFoundShouldThrow() {
        when(userRepository.findByEmail("ghost@nowhere.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactionsBySender("ghost@nowhere.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expéditeur introuvable.");
    }

    @Test
    public void getTransactionsBySenderSuccessShouldReturnMappedList() {
        TransactionEntity tx1 = new TransactionEntity();
        tx1.setId(1);
        TransactionEntity tx2 = new TransactionEntity();
        tx2.setId(2);
        TransactionModel model1 = new TransactionModel();
        model1.setId(1);
        TransactionModel model2 = new TransactionModel();
        model2.setId(2);

        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(transactionRepository.findBySenderOrderByDateTransactionDesc(sender))
                .thenReturn(List.of(tx1, tx2));
        when(transactionMapper.toModel(tx1)).thenReturn(model1);
        when(transactionMapper.toModel(tx2)).thenReturn(model2);

        List<TransactionModel> result = service.getTransactionsBySender("alice@paymybuddy.com");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TransactionModel::getId).containsExactly(1, 2);
    }

    @Test
    public void getTransactionsBySenderNoTransactionsShouldReturnEmptyList() {
        when(userRepository.findByEmail("alice@paymybuddy.com")).thenReturn(Optional.of(sender));
        when(transactionRepository.findBySenderOrderByDateTransactionDesc(sender))
                .thenReturn(List.of());

        List<TransactionModel> result = service.getTransactionsBySender("alice@paymybuddy.com");

        assertThat(result).isEmpty();
    }
}
