
package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 1. Find customer by account number
    @Query("SELECT c FROM Customer c WHERE c.accountNumber = :accountNumber")
    Optional<Customer> getByAccountNumber(
            @Param("accountNumber") String accountNumber
    );

    // 2. Count total accounts and emails
    @Query("SELECT COUNT(c) FROM Customer c")
    long countAccounts();

    @Query("SELECT COUNT(DISTINCT c.email) FROM Customer c")
    long countEmails();

    // 3. Find all customers ordered by name
    @Query("SELECT c FROM Customer c ORDER BY c.fullName ASC")
    List<Customer> findAllOrdered();

    // 4. Search customers by name
    @Query("SELECT c FROM Customer c WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    // 5. Calculate total balance
    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Customer c")
    BigDecimal getTotalBalance();

    // 6. Count customers using native SQL
    @Query(value = "SELECT COUNT(*) FROM customers", nativeQuery = true)
    long countCustomersNative();

    // 7. Debit money from an account
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Customer c SET c.balance = c.balance - :amount " +
           "WHERE c.accountNumber = :accountNumber AND c.balance >= :amount")
    int debitBalance(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount
    );

    // 8. Credit money to an account
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Customer c SET c.balance = c.balance + :amount " +
           "WHERE c.accountNumber = :accountNumber")
    int creditBalance(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount
    );
}
