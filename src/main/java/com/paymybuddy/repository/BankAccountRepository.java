package com.paymybuddy.repository;

import com.paymybuddy.repository.entity.BankAccountEntity;
import com.paymybuddy.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, Integer> {

    List<BankAccountEntity> findByUser(UserEntity user);
}
