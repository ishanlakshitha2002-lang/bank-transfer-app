
package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // Avoid inserting duplicate sample customers
        if (customerRepository.count() > 0) {
            return;
        }

        // Customer 1 - Kamal
        Customer kamal = new Customer();
        kamal.setAccountNumber("100001");
        kamal.setFullName("Kamal Perera");
        kamal.setEmail("kamal@gmail.com");
        kamal.setBalance(new BigDecimal("50000.00"));

        // Customer 2 - Nimali
        Customer nimali = new Customer();
        nimali.setAccountNumber("100002");
        nimali.setFullName("Nimali Silva");
        nimali.setEmail("nimali@gmail.com");
        nimali.setBalance(new BigDecimal("25000.00"));

        // Customer 3 - Saman
        Customer saman = new Customer();
        saman.setAccountNumber("100003");
        saman.setFullName("Saman Fernando");
        saman.setEmail("saman@gmail.com");
        saman.setBalance(new BigDecimal("10000.00"));

        customerRepository.save(kamal);
        customerRepository.save(nimali);
        customerRepository.save(saman);

        System.out.println("Sample customers inserted successfully!");
    }
}
