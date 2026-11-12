
package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public BankService(
            CustomerRepository customerRepository,
            BankTransactionRepository transactionRepository) {

        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    // Transfer money between accounts
    @Transactional(rollbackFor = Exception.class)
    public void transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String description,
            boolean simulateFailure) throws Exception {

        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException(
                    "Sender and receiver accounts cannot be the same.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero.");
        }

        customerRepository.getByAccountNumber(fromAccount)
        .orElseThrow(() ->
                new IllegalArgumentException("Sender account not found."));

        customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() ->
                        new IllegalArgumentException("Receiver account not found."));

        int debited = customerRepository.debitBalance(fromAccount, amount);

        if (debited == 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance or sender account not found.");
        }

        // Simulate an error to test transaction rollback
        if (simulateFailure) {
            throw new RuntimeException("Simulated transfer failure.");
        }

        int credited = customerRepository.creditBalance(toAccount, amount);

        if (credited == 0) {
            throw new IllegalArgumentException("Receiver account not found.");
        }

        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);

        transactionRepository.save(transaction);
    }

    // Create a new customer
    @Transactional
    public Customer createCustomer(Customer customer) {

        if (customerRepository
                .getByAccountNumber(customer.getAccountNumber())
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Account number already exists.");
        }

        return customerRepository.save(customer);
    }
}
