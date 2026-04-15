package com.paymybuddy.repository;

import com.paymybuddy.model.BankAccount;
import com.paymybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount,Integer> {

    List<BankAccount> findByUser(User user);
}
