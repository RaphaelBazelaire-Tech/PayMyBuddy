package com.paymybuddy.repository;

import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findBySenderOrderByDateTransactionDesc(User sender);
}
