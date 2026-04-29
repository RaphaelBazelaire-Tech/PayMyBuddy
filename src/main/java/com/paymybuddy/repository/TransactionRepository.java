package com.paymybuddy.repository;

import com.paymybuddy.repository.entity.TransactionEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {

    List<TransactionEntity> findBySenderOrderByDateTransactionDesc(UserEntity sender);
}
